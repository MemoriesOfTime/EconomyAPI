package me.onebone.economyapi;

/*
 * EconomyAPI: Core of economy system for Nukkit
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.PluginI18n;
import cn.nukkit.lang.PluginI18nManager;
import cn.nukkit.plugin.PluginBase;
import me.onebone.economyapi.command.*;
import me.onebone.economyapi.config.EconomyAPIConfig;
import me.onebone.economyapi.config.UpgradeConfig;
import me.onebone.economyapi.event.account.CreateAccountEvent;
import me.onebone.economyapi.event.money.AddMoneyEvent;
import me.onebone.economyapi.event.money.ReduceMoneyEvent;
import me.onebone.economyapi.event.money.SetMoneyEvent;
import me.onebone.economyapi.provider.MySQLProvider;
import me.onebone.economyapi.provider.Provider;
import me.onebone.economyapi.provider.SQLiteProvider;
import me.onebone.economyapi.provider.YamlProvider;
import me.onebone.economyapi.task.AutoSaveTask;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.onebone.economyapi.config.UpgradeConfig.*;
import static me.onebone.economyapi.config.UpgradeConfig.tryUpgradeSQLiteData;

public class EconomyAPI extends PluginBase implements Listener {
    public static final int RET_NO_ACCOUNT = -3;
    public static final int RET_CANCELLED = -2;
    public static final int RET_NOT_FOUND = -1;
    public static final int RET_INVALID = 0;
    public static final int RET_SUCCESS = 1;
    public static EconomyAPIConfig MAIN_CONFIG;
    public static final DecimalFormat MONEY_FORMAT = new DecimalFormat();
    private static EconomyAPI instance;
    private static PluginI18n i18n;
    public static LangCode serverLangCode;
    protected Provider provider;
    protected final HashMap<String, Class<?>> providerClass = new HashMap<>();
    protected static AsyncOperator asyncOperator = new AsyncOperator();
    final ConcurrentHashMap<UUID, Boolean> migratedPlayers = new ConcurrentHashMap<>();

    static {
        MONEY_FORMAT.setMaximumFractionDigits(2);
    }

    private final String[] langList = new String[]{
            "ch", "cs", "def", "fr", "id", "it", "jp", "ko", "nl", "ru", "zh"
    };

    public static EconomyAPI getInstance() {
        return instance;
    }

    public static PluginI18n getI18n() {
        return i18n;
    }

    public boolean createAccount(Player player) {
        return this.createAccount(player, -1);
    }

    public boolean createAccount(Player player, double defaultMoney) {
        return this.createAccount(player, defaultMoney, false);
    }

    public boolean createAccount(Player player, double defaultMoney, boolean force) {
        checkAndConvertLegacy(player.getUniqueId(), player.getName(), true);
        return createAccountInternal(player.getUniqueId().toString(), defaultMoney, force);
    }

    public boolean createAccount(IPlayer player) {
        return this.createAccount(player, -1);
    }

    public boolean createAccount(IPlayer player, double defaultMoney) {
        return this.createAccount(player, defaultMoney, false);
    }

    public boolean createAccount(IPlayer player, double defaultMoney, boolean force) {
        checkAndConvertLegacy(player.getUniqueId(), player.getName(), true);
        return createAccountInternal(player.getUniqueId().toString(), defaultMoney, force);
    }

    public boolean createAccount(UUID id, double defaultMoney) {
        return this.createAccount(id, defaultMoney, false);
    }

    public boolean createAccount(UUID id, double defaultMoney, boolean force) {
        checkAndConvertLegacy(id);
        return createAccountInternal(id.toString(), defaultMoney, force);
    }

    public boolean createAccount(String id, double defaultMoney, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> createAccount(uuid1, defaultMoney, force))
                .orElse(createAccountInternal(id, defaultMoney, force));
    }

    private boolean createAccountInternal(String id, double defaultMoney, boolean force) {
        id = id.toLowerCase();
        CreateAccountEvent event = new CreateAccountEvent(id, defaultMoney);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            boolean failed = false;
            for (String currencyName : MAIN_CONFIG.getCurrencyList()) {
                double money = event.getDefaultMoney() == -1D ? getDefaultMoney(currencyName) : event.getDefaultMoney();
                failed = failed || !this.provider.createAccount(currencyName, id, money);
            }
            return !failed;
        }
        return false;
    }

    public LinkedHashMap<String, Double> getAllMoney() {
        return this.provider.getAll();
    }

    /**
     * Returns money of player
     *
     * @param player
     * @return Money of player. -1 if player does not exist.
     */
    public double myMoney(Player player) {
        return this.myMoney(player.getUniqueId());
    }

    public double myMoney(IPlayer player) {
        return this.myMoney(player.getUniqueId());
    }

    public double myMoney(UUID id) {
        checkAndConvertLegacy(id);
        return myMoneyInternal(id.toString());
    }

    public double myMoney(String id) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(this::myMoney).orElse(myMoneyInternal(id));
    }

    private double myMoneyInternal(String id) {
        return this.provider.getMoney(id.toLowerCase());
    }

    public int setMoney(Player player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public int setMoney(Player player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public int setMoney(IPlayer player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public int setMoney(IPlayer player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public int setMoney(UUID id, double amount) {
        return setMoney(id, amount, false);
    }

    public int setMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return setMoneyInternal(id.toString(), amount, force);
    }

    public int setMoney(String id, double amount) {
        return this.setMoney(id, amount, false);
    }

    public int setMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> setMoney(uuid1, amount, force))
                .orElse(setMoneyInternal(id, amount, force));
    }

    private int setMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }

        SetMoneyEvent event = new SetMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.setMoneyChecked(
                    MAIN_CONFIG.getDefaultCurrency().getName(), id, event.getAmount(), this.getMaxMoney());
        }
        return RET_CANCELLED;
    }

    public int addMoney(Player player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public int addMoney(Player player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public int addMoney(IPlayer player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public int addMoney(IPlayer player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public int addMoney(UUID id, double amount) {
        return addMoney(id, amount, false);
    }

    public int addMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return addMoneyInternal(id.toString(), amount, force);
    }

    public int addMoney(String id, double amount) {
        return this.addMoney(id, amount, false);
    }

    public int addMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> addMoney(uuid1, amount, force))
                .orElse(addMoneyInternal(id, amount, force));
    }

    private int addMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }
        AddMoneyEvent event = new AddMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.addMoneyChecked(
                    MAIN_CONFIG.getDefaultCurrency().getName(), id, event.getAmount(), this.getMaxMoney());
        }
        return RET_CANCELLED;
    }

    public int reduceMoney(Player player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public int reduceMoney(Player player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public int reduceMoney(IPlayer player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public int reduceMoney(IPlayer player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public int reduceMoney(UUID id, double amount) {
        return this.reduceMoney(id, amount, false);
    }

    public int reduceMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return reduceMoneyInternal(id.toString(), amount, force);
    }

    public int reduceMoney(String id, double amount) {
        return this.reduceMoney(id, amount, false);
    }

    public int reduceMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> reduceMoney(uuid1, amount, force))
                .orElse(reduceMoneyInternal(id, amount, force));
    }

    private int reduceMoneyInternal(String id, double amount, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }

        ReduceMoneyEvent event = new ReduceMoneyEvent(id, amount);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.reduceMoneyChecked(
                    MAIN_CONFIG.getDefaultCurrency().getName(), id, event.getAmount());
        }
        return RET_CANCELLED;
    }

    public boolean hasAccount(IPlayer player) {
        return player != null && hasAccount(player.getUniqueId());
    }

    public boolean hasAccount(UUID id) {
        return hasAccount(id.toString());
    }

    public boolean hasAccount(String id) {
        return provider.accountExists(checkAndConvertLegacy(id).map(UUID::toString).map(String::toLowerCase).orElse(id.toLowerCase()));
    }

    public boolean hasAccount(IPlayer player, String currencyName) {
        return player != null && hasAccount(player.getUniqueId(), currencyName);
    }

    public boolean hasAccount(UUID id, String currencyName) {
        checkAndConvertLegacy(id);
        return this.provider.accountExists(currencyName, id.toString().toLowerCase());
    }

    public boolean hasAccount(String id, String currencyName) {
        return this.provider.accountExists(
                currencyName,
                checkAndConvertLegacy(id).map(UUID::toString).map(String::toLowerCase).orElse(id.toLowerCase())
        );
    }

    public String getMonetaryUnit() {
        return MAIN_CONFIG.getDefaultCurrency().getMonetaryUnit();
    }

    public double getDefaultMoney() {
        return MAIN_CONFIG.getDefaultCurrency().getDefaultAmount();
    }

    public double getMaxMoney() {
        return MAIN_CONFIG.getDefaultCurrency().getMaxAmount();
    }

    // start 多货币方法
    public LinkedHashMap<String, Double> getAllMoney(String currencyName) {
        return this.provider.getAll(currencyName);
    }

    public double myMoney(Player player, String currencyName) {
        return this.myMoney(player.getUniqueId(), currencyName);
    }

    public double myMoney(IPlayer player, String currencyName) {
        return this.myMoney(player.getUniqueId(), currencyName);
    }

    public double myMoney(UUID id, String currencyName) {
        checkAndConvertLegacy(id);
        return myMoneyInternal(id.toString(), currencyName);
    }

    public double myMoney(String id, String currencyName) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> myMoney(uuid1, currencyName)).orElse(myMoneyInternal(id, currencyName));
    }

    private double myMoneyInternal(String id, String currencyName) {
        return this.provider.getMoney(currencyName, id.toLowerCase());
    }

    public int setMoney(Player player, double amount, String currencyName) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int setMoney(Player player, double amount, String currencyName, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int setMoney(IPlayer player, double amount, String currencyName) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int setMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int setMoney(UUID id, double amount, String currencyName) {
        return setMoney(id, amount, currencyName, false);
    }

    public int setMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return setMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public int setMoney(String id, double amount, String currencyName) {
        return this.setMoney(id, amount, currencyName, false);
    }

    public int setMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> setMoney(uuid1, amount, currencyName, force))
                .orElse(setMoneyInternal(id, amount, currencyName, force));
    }

    private int setMoneyInternal(String id, double amount, String currencyName, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }
        SetMoneyEvent event = new SetMoneyEvent(id, amount, currencyName);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.setMoneyChecked(currencyName, id, event.getAmount(), getMaxMoney(currencyName));
        }
        return RET_CANCELLED;
    }

    public int addMoney(Player player, double amount, String currencyName) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int addMoney(Player player, double amount, String currencyName, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int addMoney(IPlayer player, double amount, String currencyName) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int addMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int addMoney(UUID id, double amount, String currencyName) {
        return addMoney(id, amount, currencyName, false);
    }

    public int addMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return addMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public int addMoney(String id, double amount, String currencyName) {
        return this.addMoney(id, amount, currencyName, false);
    }

    public int addMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> addMoney(uuid1, amount, currencyName, force))
                .orElse(addMoneyInternal(id, amount, currencyName, force));
    }

    private int addMoneyInternal(String id, double amount, String currencyName, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }
        AddMoneyEvent event = new AddMoneyEvent(id, amount, currencyName);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.addMoneyChecked(currencyName, id, event.getAmount(), getMaxMoney(currencyName));
        }
        return RET_CANCELLED;
    }

    public int reduceMoney(Player player, double amount, String currencyName) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int reduceMoney(Player player, double amount, String currencyName, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int reduceMoney(IPlayer player, double amount, String currencyName) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public int reduceMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public int reduceMoney(UUID id, double amount, String currencyName) {
        return reduceMoney(id, amount, currencyName, false);
    }

    public int reduceMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return reduceMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public int reduceMoney(String id, double amount, String currencyName) {
        return this.reduceMoney(id, amount, currencyName, false);
    }

    public int reduceMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> reduceMoney(uuid1, amount, currencyName, force))
                .orElse(reduceMoneyInternal(id, amount, currencyName, force));
    }

    private int reduceMoneyInternal(String id, double amount, String currencyName, boolean force) {
        id = id.toLowerCase();
        if (amount < 0) {
            return RET_INVALID;
        }
        ReduceMoneyEvent event = new ReduceMoneyEvent(id, amount, currencyName);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            return this.provider.reduceMoneyChecked(currencyName, id, event.getAmount());
        }
        return RET_CANCELLED;
    }

    public int transferMoney(Player from, String to, double amount, String currencyName) {
        return transferMoney(from.getUniqueId(), to, amount, currencyName, false, false);
    }

    public int transferMoney(Player from, String to, double amount, String currencyName, boolean force) {
        return transferMoney(from.getUniqueId(), to, amount, currencyName, force, force);
    }

    public int transferMoney(UUID from, String to, double amount, String currencyName) {
        return transferMoney(from, to, amount, currencyName, false, false);
    }

    public int transferMoney(UUID from, String to, double amount, String currencyName, boolean force) {
        return transferMoney(from, to, amount, currencyName, force, force);
    }

    public int transferMoney(UUID from, String to, double amount, String currencyName, boolean forceReduce, boolean forceAdd) {
        return transferMoneyDetailed(from, to, amount, currencyName, forceReduce, forceAdd).result();
    }

    public TransferResult transferMoneyDetailed(UUID from, String to, double amount, String currencyName, boolean forceReduce, boolean forceAdd) {
        checkAndConvertLegacy(from);
        Optional<UUID> toUuid = checkAndConvertLegacy(to);
        String fromId = from.toString().toLowerCase();
        String toId = toUuid.map(UUID::toString).map(String::toLowerCase).orElse(to.toLowerCase());
        return transferMoneyInternal(fromId, toId, amount, currencyName, forceReduce, forceAdd);
    }

    TransferResult transferMoneyInternal(String fromId, String toId, double amount, String currencyName, boolean forceReduce, boolean forceAdd) {
        if (fromId.equals(toId) || !Double.isFinite(amount) || amount < 0) {
            return TransferResult.invalid(amount);
        }

        ReduceMoneyEvent reduceEvent = new ReduceMoneyEvent(fromId, amount, currencyName);
        callMoneyEvent(reduceEvent);
        if (reduceEvent.isCancelled() && !forceReduce) {
            return TransferResult.cancelled(amount);
        }

        double transferAmount = reduceEvent.getAmount();
        if (!Double.isFinite(transferAmount) || transferAmount < 0) {
            return TransferResult.invalid(amount);
        }

        AddMoneyEvent addEvent = new AddMoneyEvent(toId, transferAmount, currencyName);
        callMoneyEvent(addEvent);
        if (addEvent.isCancelled() && !forceAdd) {
            return TransferResult.cancelled(transferAmount);
        }

        double addAmount = addEvent.getAmount();
        if (!Double.isFinite(addAmount) || addAmount < 0) {
            return TransferResult.invalid(addAmount);
        }
        if (Double.compare(addAmount, transferAmount) != 0) {
            return TransferResult.invalid(addAmount);
        }

        int result = this.provider.transferMoneyChecked(currencyName, fromId, toId, transferAmount, getMaxMoney(currencyName));
        return new TransferResult(result, transferAmount);
    }

    void callMoneyEvent(Event event) {
        this.getServer().getPluginManager().callEvent(event);
    }

    public record TransferResult(int result, double amount) {
        private static TransferResult invalid(double amount) {
            return new TransferResult(RET_INVALID, amount);
        }

        private static TransferResult cancelled(double amount) {
            return new TransferResult(RET_CANCELLED, amount);
        }
    }

    public boolean createAccount(Player player, double defaultMoney, String currencyName) {
        return this.createAccount(player, defaultMoney, currencyName, false);
    }

    public boolean createAccount(Player player, double defaultMoney, String currencyName, boolean force) {
        checkAndConvertLegacy(player.getUniqueId(), player.getName(), true);
        return createAccountInternal(player.getUniqueId().toString(), defaultMoney, currencyName, force);
    }

    public boolean createAccount(IPlayer player, double defaultMoney, String currencyName) {
        return this.createAccount(player, defaultMoney, currencyName, false);
    }

    public boolean createAccount(IPlayer player, double defaultMoney, String currencyName, boolean force) {
        checkAndConvertLegacy(player.getUniqueId(), player.getName(), true);
        return createAccountInternal(player.getUniqueId().toString(), defaultMoney, currencyName, force);
    }

    public boolean createAccount(UUID id, double defaultMoney, String currencyName) {
        return this.createAccount(id, defaultMoney, currencyName, false);
    }

    public boolean createAccount(UUID id, double defaultMoney, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return createAccountInternal(id.toString(), defaultMoney, currencyName, force);
    }

    public boolean createAccount(String id, double defaultMoney, String currencyName) {
        return this.createAccount(id, defaultMoney, currencyName, false);
    }

    public boolean createAccount(String id, double defaultMoney, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> createAccount(uuid1, defaultMoney, currencyName, force))
                .orElse(createAccountInternal(id, defaultMoney, currencyName, force));
    }

    private boolean createAccountInternal(String id, double defaultMoney, String currencyName, boolean force) {
        id = id.toLowerCase();
        CreateAccountEvent event = new CreateAccountEvent(id, defaultMoney, currencyName);
        this.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() || force) {
            defaultMoney = event.getDefaultMoney() == -1D ? getDefaultMoney(currencyName) : event.getDefaultMoney();
            return this.provider.createAccount(currencyName, id, defaultMoney);
        }
        return false;
    }

    public String getMonetaryUnit(String currencyName) {
        return MAIN_CONFIG.getCurrency(currencyName).getMonetaryUnit();
    }

    public double getDefaultMoney(String currencyName) {
        return MAIN_CONFIG.getCurrency(currencyName).getDefaultAmount();
    }

    public double getMaxMoney(String currencyName) {
        return MAIN_CONFIG.getCurrency(currencyName).getMaxAmount();
    }
    // end 多货币方法

    public void saveAll() {
        if (this.provider != null) {
            this.provider.save();
        }
    }

    @Override
    public void onLoad() {
        instance = this;
        // 注册插件的 i18n
        i18n = PluginI18nManager.register(this);
        initServerLangCode();

        this.addProvider("yaml", YamlProvider.class);
        if (this.getServer().getPluginManager().getPlugin("EasySQLX") != null) {
            this.addProvider("sqlite", SQLiteProvider.class);
            this.addProvider("mysql", MySQLProvider.class);
        } else {
            this.getLogger().warning("EasySQLX is not found, SQLite and MySQL provider will not be available.");
        }
    }

    @Override
    public void onEnable() {
        if (Files.exists(Path.of(getDataFolder().toString(), "config.yml")) &&
                UpgradeConfig.tryUpgradeConfigVersion(EconomyAPI.getInstance().getConfig().getInt("version", 1))) {
            if (updateDoubleConfirmation()) {
                MAIN_CONFIG = new EconomyAPIConfig();
                if (tryUpgradeYamlData()) {
                    EconomyAPI.getInstance().getLogger().info("YAML data upgrade complete.");
                }
                if (tryUpgradeSQLiteData()) {
                    EconomyAPI.getInstance().getLogger().info("SQLite data upgrade complete.");
                }
            } else {
                return;
            }
        } else {
            EconomyAPI.getInstance().saveDefaultConfig();
            MAIN_CONFIG = new EconomyAPIConfig();
        }

        boolean success = this.initialize();

        if (success) {
            this.getServer().getPluginManager().registerEvents(this, this);
            this.getServer().getScheduler().scheduleDelayedRepeatingTask(new AutoSaveTask(this), MAIN_CONFIG.getAutoSaveInterval() * 1200, MAIN_CONFIG.getAutoSaveInterval() * 1200);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        this.createAccount(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        migratedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onDisable() {
        this.saveAll();
        if (this.provider != null) {
            this.provider.close();
        }
    }

    private boolean initialize() {
        this.registerCommands();
        return this.selectProvider();
    }

    private void registerCommands() {
        this.getServer().getCommandMap().register("economy", new MyMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new TopMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new GiveMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new TakeMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new PayCommand(this));
        this.getServer().getCommandMap().register("economy", new SetMoneyCommand(this));
        this.getServer().getCommandMap().register("economy", new MigrateDataCommand(this));
    }

    private boolean selectProvider() {
        Class<?> providerClass = this.providerClass.get(MAIN_CONFIG.getProvider());

        if (providerClass == null) {
            this.getLogger().critical("Invalid data provider was given.");
            return false;
        }

        try {
            this.provider = (Provider) providerClass.getDeclaredConstructor().newInstance();
            this.provider.init(this.getDataFolder());
        } catch (InstantiationException | IllegalAccessException e) {
            this.getLogger().critical("Invalid data provider was given.");
            return false;
        } catch (InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        this.provider.open();

        this.getLogger().info("Data provider was set to: " + provider.getName());
        return true;
    }

    public Provider getProvider() {
        return this.provider;
    }

    public boolean addProvider(String name, Class<? extends Provider> providerClass) {
        this.providerClass.put(name, providerClass);
        return true;
    }

    /**
     * Gets the async operator for economy operations.
     *
     * @return AsyncOperator instance for performing asynchronous economy operations
     * @example <pre>
     * EconomyAPI.getAsyncOperator().myMoney(player).thenAccept(money -> {
     *     player.sendMessage("Your balance: " + money);
     * });
     * </pre>
     */
    public static AsyncOperator getAsyncOperator() {
        return asyncOperator;
    }

    void checkAndConvertLegacy(UUID uuid) {
        if (migratedPlayers.containsKey(uuid)) {
            return;
        }
        IPlayer player = getOfflinePlayer(uuid);
        if (player != null && player.getName() != null) {
            checkAndConvertLegacy(uuid, player.getName());
        }
    }

    IPlayer getOfflinePlayer(UUID uuid) {
        return getServer().getOfflinePlayer(uuid);
    }

    Optional<UUID> checkAndConvertLegacy(String id) {
        Optional<UUID> uuid = getServer().lookupName(id);
        if (uuid.isEmpty()) {
            Player onlinePlayer = getServer().getPlayerExact(id);
            if (onlinePlayer != null) {
                uuid = Optional.of(onlinePlayer.getUniqueId());
            }
        }
        uuid.ifPresent(uuid1 -> checkAndConvertLegacy(uuid1, id));
        return uuid;
    }

    void checkAndConvertLegacy(UUID uuid, String name) {
        checkAndConvertLegacy(uuid, name, false);
    }

    void checkAndConvertLegacy(UUID uuid, String name, boolean force) {
        if (!force && migratedPlayers.containsKey(uuid)) {
            return;
        }
        if (name == null) {
            return;
        }
        name = name.toLowerCase();
        String uuidStr = uuid.toString().toLowerCase();
        for (String currencyName : MAIN_CONFIG.getCurrencyList()) {
            if (!provider.accountExists(currencyName, name)) {
                continue;
            }
            double money = provider.getMoney(currencyName, name);
            if (provider.accountExists(currencyName, uuidStr)) {
                if (!force || provider.setMoney(currencyName, uuidStr, money)) {
                    provider.removeAccount(currencyName, name);
                }
                continue;
            }
            if (provider.createAccount(currencyName, uuidStr, money)) {
                provider.removeAccount(currencyName, name);
            }
        }
        migratedPlayers.put(uuid, Boolean.TRUE);
    }

    private void initServerLangCode() {
        switch (Server.getInstance().getLanguage().getLang()) {
            case "eng" -> {
                serverLangCode = LangCode.en_US;
            }
            case "chs" -> {
                serverLangCode = LangCode.zh_CN;
            }
            case "deu" -> {
                serverLangCode = LangCode.de_DE;
            }
            case "rus" -> {
                serverLangCode = LangCode.ru_RU;
            }
            default -> {
                try {
                    serverLangCode = LangCode.valueOf(Server.getInstance().getLanguage().getLang());
                } catch (IllegalArgumentException e) {
                    serverLangCode = LangCode.en_US;
                }
            }
        }
    }
}
