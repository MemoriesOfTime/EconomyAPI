package me.onebone.economyapi;

import cn.nukkit.IPlayer;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.scheduler.AsyncTask;
import me.onebone.economyapi.event.money.AddMoneyEvent;
import me.onebone.economyapi.event.money.ReduceMoneyEvent;
import me.onebone.economyapi.event.money.SetMoneyEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AsyncOperator {
    private static class MoneyLookupTask extends AsyncTask {
        private final String id;
        private final String currencyName;
        private final CompletableFuture<Double> future;

        public MoneyLookupTask(String id, String currencyName, CompletableFuture<Double> future) {
            this.id = id;
            this.currencyName = currencyName;
            this.future = future;
        }

        @Override
        public void onRun() {
            double money = EconomyAPI.getInstance().provider.getMoney(currencyName, id.toLowerCase());
            this.setResult(money);
        }

        @Override
        public void onCompletion(Server server) {
            future.complete((Double) this.getResult());
        }
    }

    /**
     * Returns money of player asynchronously
     *
     * @param player
     * @return CompletableFuture containing money of player. -1 if player does not exist.
     */
    public CompletableFuture<Double> myMoney(Player player) {
        return this.myMoney(player.getUniqueId());
    }

    public CompletableFuture<Double> myMoney(IPlayer player) {
        return this.myMoney(player.getUniqueId());
    }

    public CompletableFuture<Double> myMoney(UUID id) {
        checkAndConvertLegacy(id);
        return myMoneyInternal(id.toString());
    }

    public CompletableFuture<Double> myMoney(String id) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(this::myMoney)
                .orElseGet(() -> myMoneyInternal(id));
    }

    private CompletableFuture<Double> myMoneyInternal(String id) {
        return myMoneyInternal(id, EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName());
    }

    private static class MoneySetTask extends AsyncTask {
        private final String id;
        private final double amount;
        private final String currencyName;
        private final boolean force;
        private final CompletableFuture<Integer> future;

        public MoneySetTask(String id, double amount, String currencyName, boolean force, CompletableFuture<Integer> future) {
            this.id = id;
            this.amount = amount;
            this.currencyName = currencyName;
            this.force = force;
            this.future = future;
        }

        @Override
        public void onRun() {
            String lowerId = id.toLowerCase();
            if (amount < 0) {
                this.setResult(EconomyAPI.RET_INVALID);
                return;
            }

            SetMoneyEvent event = new SetMoneyEvent(lowerId, amount, currencyName);
            Server.getInstance().getPluginManager().callEvent(event);

            if (!event.isCancelled() || force) {
                if (EconomyAPI.getInstance().provider.accountExists(currencyName, lowerId)) {
                    double finalAmount = event.getAmount();
                    if (finalAmount <= EconomyAPI.getInstance().getMaxMoney(currencyName)) {
                        EconomyAPI.getInstance().provider.setMoney(currencyName, lowerId, finalAmount);
                        this.setResult(EconomyAPI.RET_SUCCESS);
                    } else {
                        this.setResult(EconomyAPI.RET_INVALID);
                    }
                } else {
                    this.setResult(EconomyAPI.RET_NO_ACCOUNT);
                }
            } else {
                this.setResult(EconomyAPI.RET_CANCELLED);
            }
        }

        @Override
        public void onCompletion(Server server) {
            future.complete((Integer) this.getResult());
        }
    }

    public CompletableFuture<Integer> setMoney(Player player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public CompletableFuture<Integer> setMoney(Player player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> setMoney(IPlayer player, double amount) {
        return this.setMoney(player, amount, false);
    }

    public CompletableFuture<Integer> setMoney(IPlayer player, double amount, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> setMoney(UUID id, double amount) {
        return setMoney(id, amount, false);
    }

    public CompletableFuture<Integer> setMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return setMoneyInternal(id.toString(), amount, force);
    }

    public CompletableFuture<Integer> setMoney(String id, double amount) {
        return this.setMoney(id, amount, false);
    }

    public CompletableFuture<Integer> setMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> setMoney(uuid1, amount, force))
                .orElseGet(() -> setMoneyInternal(id, amount, force));
    }

    private CompletableFuture<Integer> setMoneyInternal(String id, double amount, boolean force) {
        return setMoneyInternal(id, amount, EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName(), force);
    }


    private static class MoneyAddTask extends AsyncTask {
        private final String id;
        private final double amount;
        private final String currencyName;
        private final boolean force;
        private final CompletableFuture<Integer> future;

        public MoneyAddTask(String id, double amount, String currencyName, boolean force, CompletableFuture<Integer> future) {
            this.id = id;
            this.amount = amount;
            this.currencyName = currencyName;
            this.force = force;
            this.future = future;
        }

        @Override
        public void onRun() {
            String lowerId = id.toLowerCase();
            if (amount < 0) {
                this.setResult(EconomyAPI.RET_INVALID);
                return;
            }

            AddMoneyEvent event = new AddMoneyEvent(lowerId, amount, currencyName);
            Server.getInstance().getPluginManager().callEvent(event);

            if (!event.isCancelled() || force) {
                double money = EconomyAPI.getInstance().provider.getMoney(currencyName, lowerId);
                if (money != -1) {
                    if (money + amount > EconomyAPI.getInstance().getMaxMoney(currencyName)) {
                        this.setResult(EconomyAPI.RET_INVALID);
                    } else {
                        EconomyAPI.getInstance().provider.addMoney(currencyName, lowerId, amount);
                        this.setResult(EconomyAPI.RET_SUCCESS);
                    }
                } else {
                    this.setResult(EconomyAPI.RET_NO_ACCOUNT);
                }
            } else {
                this.setResult(EconomyAPI.RET_CANCELLED);
            }
        }

        @Override
        public void onCompletion(Server server) {
            future.complete((Integer) this.getResult());
        }
    }

    public CompletableFuture<Integer> addMoney(Player player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public CompletableFuture<Integer> addMoney(Player player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> addMoney(IPlayer player, double amount) {
        return this.addMoney(player, amount, false);
    }

    public CompletableFuture<Integer> addMoney(IPlayer player, double amount, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> addMoney(UUID id, double amount) {
        return addMoney(id, amount, false);
    }

    public CompletableFuture<Integer> addMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return addMoneyInternal(id.toString(), amount, force);
    }

    public CompletableFuture<Integer> addMoney(String id, double amount) {
        return this.addMoney(id, amount, false);
    }

    public CompletableFuture<Integer> addMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> addMoney(uuid1, amount, force))
                .orElseGet(() -> addMoneyInternal(id, amount, force));
    }

    private CompletableFuture<Integer> addMoneyInternal(String id, double amount, boolean force) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneyAddTask(id, amount, EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName(), force, future)
        );
        return future;
    }

    private static class MoneyReduceTask extends AsyncTask {
        private final String id;
        private final double amount;
        private final String currencyName;
        private final boolean force;
        private final CompletableFuture<Integer> future;

        public MoneyReduceTask(String id, double amount, String currencyName, boolean force, CompletableFuture<Integer> future) {
            this.id = id;
            this.amount = amount;
            this.currencyName = currencyName;
            this.force = force;
            this.future = future;
        }

        @Override
        public void onRun() {
            String lowerId = id.toLowerCase();
            if (amount < 0) {
                this.setResult(EconomyAPI.RET_INVALID);
                return;
            }

            ReduceMoneyEvent event = new ReduceMoneyEvent(lowerId, amount, currencyName);
            Server.getInstance().getPluginManager().callEvent(event);

            if (!event.isCancelled() || force) {
                double finalAmount = event.getAmount();
                double money = EconomyAPI.getInstance().provider.getMoney(currencyName, lowerId);
                if (money != -1) {
                    if (money - finalAmount < 0) {
                        this.setResult(EconomyAPI.RET_INVALID);
                    } else {
                        EconomyAPI.getInstance().provider.reduceMoney(currencyName, lowerId, finalAmount);
                        this.setResult(EconomyAPI.RET_SUCCESS);
                    }
                } else {
                    this.setResult(EconomyAPI.RET_NO_ACCOUNT);
                }
            } else {
                this.setResult(EconomyAPI.RET_CANCELLED);
            }
        }

        @Override
        public void onCompletion(Server server) {
            future.complete((Integer) this.getResult());
        }
    }

    public CompletableFuture<Integer> reduceMoney(Player player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public CompletableFuture<Integer> reduceMoney(Player player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> reduceMoney(IPlayer player, double amount) {
        return this.reduceMoney(player, amount, false);
    }

    public CompletableFuture<Integer> reduceMoney(IPlayer player, double amount, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, force);
    }

    public CompletableFuture<Integer> reduceMoney(UUID id, double amount) {
        return reduceMoney(id, amount, false);
    }

    public CompletableFuture<Integer> reduceMoney(UUID id, double amount, boolean force) {
        checkAndConvertLegacy(id);
        return reduceMoneyInternal(id.toString(), amount, force);
    }

    public CompletableFuture<Integer> reduceMoney(String id, double amount) {
        return this.reduceMoney(id, amount, false);
    }

    public CompletableFuture<Integer> reduceMoney(String id, double amount, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> reduceMoney(uuid1, amount, force))
                .orElseGet(() -> reduceMoneyInternal(id, amount, force));
    }

    private CompletableFuture<Integer> reduceMoneyInternal(String id, double amount, boolean force) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneyReduceTask(id, amount, EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName(), force, future)
        );
        return future;
    }


    // === mutiple currency

    public CompletableFuture<Double> myMoney(Player player, String currencyName) {
        return this.myMoney(player.getUniqueId(), currencyName);
    }

    public CompletableFuture<Double> myMoney(IPlayer player, String currencyName) {
        return this.myMoney(player.getUniqueId(), currencyName);
    }

    public CompletableFuture<Double> myMoney(UUID id, String currencyName) {
        checkAndConvertLegacy(id);
        return myMoneyInternal(id.toString(), currencyName);
    }

    public CompletableFuture<Double> myMoney(String id, String currencyName) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> myMoney(uuid1, currencyName))
                .orElseGet(() -> myMoneyInternal(id, currencyName));
    }

    private CompletableFuture<Double> myMoneyInternal(String id, String currencyName) {
        CompletableFuture<Double> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneyLookupTask(id, currencyName, future)
        );
        return future;
    }

    public CompletableFuture<Integer> setMoney(Player player, double amount, String currencyName) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> setMoney(Player player, double amount, String currencyName, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> setMoney(IPlayer player, double amount, String currencyName) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> setMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.setMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> setMoney(UUID id, double amount, String currencyName) {
        return setMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> setMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return setMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> setMoney(String id, double amount, String currencyName) {
        return this.setMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> setMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> setMoney(uuid1, amount, currencyName, force))
                .orElseGet(() -> setMoneyInternal(id, amount, currencyName, force));
    }

    private CompletableFuture<Integer> setMoneyInternal(String id, double amount, String currencyName, boolean force) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneySetTask(id, amount, currencyName, force, future)
        );
        return future;
    }

    // Add Money methods
    public CompletableFuture<Integer> addMoney(Player player, double amount, String currencyName) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> addMoney(Player player, double amount, String currencyName, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> addMoney(IPlayer player, double amount, String currencyName) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> addMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.addMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> addMoney(UUID id, double amount, String currencyName) {
        return addMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> addMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return addMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> addMoney(String id, double amount, String currencyName) {
        return this.addMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> addMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> addMoney(uuid1, amount, currencyName, force))
                .orElseGet(() -> addMoneyInternal(id, amount, currencyName, force));
    }

    private CompletableFuture<Integer> addMoneyInternal(String id, double amount, String currencyName, boolean force) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneyAddTask(id, amount, currencyName, force, future)
        );
        return future;
    }

    // Reduce Money methods
    public CompletableFuture<Integer> reduceMoney(Player player, double amount, String currencyName) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> reduceMoney(Player player, double amount, String currencyName, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> reduceMoney(IPlayer player, double amount, String currencyName) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, false);
    }

    public CompletableFuture<Integer> reduceMoney(IPlayer player, double amount, String currencyName, boolean force) {
        return this.reduceMoney(player.getUniqueId(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> reduceMoney(UUID id, double amount, String currencyName) {
        return reduceMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> reduceMoney(UUID id, double amount, String currencyName, boolean force) {
        checkAndConvertLegacy(id);
        return reduceMoneyInternal(id.toString(), amount, currencyName, force);
    }

    public CompletableFuture<Integer> reduceMoney(String id, double amount, String currencyName) {
        return this.reduceMoney(id, amount, currencyName, false);
    }

    public CompletableFuture<Integer> reduceMoney(String id, double amount, String currencyName, boolean force) {
        Optional<UUID> uuid = checkAndConvertLegacy(id);
        return uuid.map(uuid1 -> reduceMoney(uuid1, amount, currencyName, force))
                .orElseGet(() -> reduceMoneyInternal(id, amount, currencyName, force));
    }

    private CompletableFuture<Integer> reduceMoneyInternal(String id, double amount, String currencyName, boolean force) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        Server.getInstance().getScheduler().scheduleAsyncTask(
                EconomyAPI.getInstance(),
                new MoneyReduceTask(id, amount, currencyName, force, future)
        );
        return future;
    }

    // === other

    private void checkAndConvertLegacy(UUID uuid) {
        IPlayer player = Server.getInstance().getOfflinePlayer(uuid);
        if (player != null && player.getName() != null) {
            checkAndConvertLegacy(uuid, player.getName());
        }
    }

    private Optional<UUID> checkAndConvertLegacy(String id) {
        Optional<UUID> uuid = Server.getInstance().lookupName(id);
        uuid.ifPresent(uuid1 -> checkAndConvertLegacy(uuid1, id));
        return uuid;
    }

    private void checkAndConvertLegacy(UUID uuid, String name) {
        name = name.toLowerCase();
        if (!EconomyAPI.getInstance().provider.accountExists(name)) {
            return;
        }
        if (EconomyAPI.getInstance().provider.accountExists(uuid.toString())) {
            EconomyAPI.getInstance().provider.removeAccount(name);
            return;
        }
        double money = EconomyAPI.getInstance().provider.getMoney(name);
        EconomyAPI.getInstance().provider.createAccount(uuid.toString(), money);
        EconomyAPI.getInstance().provider.removeAccount(name);
    }
}