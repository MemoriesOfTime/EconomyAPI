package me.onebone.economyapi.provider;

import com.smallaswater.easysqlx.sqlite.SQLiteHelper;
import com.smallaswater.easysqlx.sqlite.SQLiteHelper.DBTable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

/**
 * @author LT_Name
 */
public class SQLiteProvider implements Provider {
    private static final String TABLE_NAME = "Money";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PLAYER = "player";
    private static final String COLUMN_MONEY = "money";
    private static final String COLUMN_CURRENCY = "currency"; // 新增 currency 列
    private SQLiteHelper sqLiteHelper;
    private final HashMap<String, MoneyData> cache = new HashMap<>(); // Key 修改为 currencyName:playerName

    @Override
    public void init(File path) {
        try {
            this.sqLiteHelper = new SQLiteHelper(path.getAbsolutePath() + File.separator + "MoneyV3.db");
            if (!this.sqLiteHelper.exists(TABLE_NAME)) {
                this.sqLiteHelper.addTable(TABLE_NAME, DBTable.asDbTable(MoneyData.class));
            }
            MAIN_CONFIG.getCurrencyList().forEach(currencyName -> { // 初始化时加载所有货币的数据
                this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_CURRENCY + " = ?", new String[]{currencyName}, MoneyData.class)
                        .forEach(data -> this.cache.put(getCacheKey(currencyName, data.getPlayer()), data));
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void open() {
    }

    @Override
    public void save() {
        // nothing to do
    }

    @Override
    public void close() {
        if (this.sqLiteHelper != null) {
            this.sqLiteHelper.close();
        }
    }

    private String getCacheKey(String currencyName, String playerName) {
        return currencyName + ":" + playerName;
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        LinkedList<MoneyData> dataList = this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_PLAYER + " = ? AND " + COLUMN_CURRENCY + " = ?", new String[]{id, currencyName}, MoneyData.class);
        return !dataList.isEmpty();
    }

    @Override
    public boolean accountExists(String id) {
        return accountExists(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean removeAccount(String currencyName, String id) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        this.sqLiteHelper.remove(TABLE_NAME, COLUMN_PLAYER, id); // 移除账户时使用 player 作为 key，currency 通过 MoneyData 对象内部 currency 字段区分
        this.cache.remove(getCacheKey(currencyName, id)); // 从缓存中移除
        return true;
    }

    @Override
    public boolean removeAccount(String id) {
        return removeAccount(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean createAccount(String currencyName, String id, double defaultMoney) {
        if (accountExists(currencyName, id)) {
            return false;
        }
        MoneyData values = new MoneyData(id, defaultMoney);
        values.setCurrency(currencyName); // 设置 currencyName
        this.sqLiteHelper.add(TABLE_NAME, values);
        this.cache.put(getCacheKey(currencyName, id), values); // 添加到缓存
        return true;
    }

    @Override
    public boolean createAccount(String id, double defaultMoney) {
        return createAccount(MAIN_CONFIG.getDefaultCurrency().getName(), id, defaultMoney);
    }

    @Override
    public boolean setMoney(String currencyName, String id, double amount) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        MoneyData moneyData = this.getMoneyData(currencyName, id);
        moneyData.setMoney(amount);
        this.sqLiteHelper.set(TABLE_NAME, COLUMN_PLAYER, id, moneyData); // 使用 player 作为 key 更新，MoneyData 对象内部包含 currency 信息
        this.cache.put(getCacheKey(currencyName, id), moneyData); // 更新缓存
        return true;
    }

    @Override
    public boolean setMoney(String id, double amount) {
        return setMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public boolean addMoney(String currencyName, String id, double amount) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        this.setMoney(currencyName, id, this.getMoney(currencyName, id) + amount);
        return true;
    }

    @Override
    public boolean addMoney(String id, double amount) {
        return addMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public boolean reduceMoney(String currencyName, String id, double amount) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        this.setMoney(currencyName, id, this.getMoney(currencyName, id) - amount);
        return true;
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        return reduceMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public double getMoney(String currencyName, String id) {
        MoneyData data = this.getMoneyData(currencyName, id);
        if (data != null) {
            return data.getMoney();
        }
        return -1;
    }

    @Override
    public double getMoney(String id) {
        return getMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public LinkedHashMap<String, Double> getAll(String currencyName) {
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();
        MAIN_CONFIG.getCurrencyList().forEach(currency -> {
            if (currency.equals(currencyName)) {
                this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_CURRENCY + " = ?", new String[]{currencyName}, MoneyData.class)
                        .forEach(data -> map.put(data.getPlayer(), data.getMoney()));
            }
        });
        return map;
    }

    @Override
    public LinkedHashMap<String, Double> getAll() {
        return getAll(MAIN_CONFIG.getDefaultCurrency().getName());
    }

    @Override
    public String getName() {
        return "SQLite";
    }

    private MoneyData getMoneyData(String currencyName, String id) {
        String cacheKey = getCacheKey(currencyName, id);
        if (this.cache.containsKey(cacheKey)) {
            return this.cache.get(cacheKey);
        }
        LinkedList<MoneyData> dataList = this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_PLAYER + " = ? AND " + COLUMN_CURRENCY + " = ?", new String[]{id, currencyName}, MoneyData.class);
        if (!dataList.isEmpty()) {
            MoneyData data = dataList.getFirst();
            this.cache.put(cacheKey, data);
            return data;
        }
        return null;
    }

    public static class MoneyData {
        private long id;
        private String player;
        private double money;
        private String currency;

        public MoneyData() {
            //SQLiteHelper创建类需要无参数的构造方法
        }

        public MoneyData(String player, double money) {
            this.player = player;
            this.money = money;
        }

        public long getId() {
            return id;
        }

        public String getPlayer() {
            return player;
        }

        public double getMoney() {
            return money;
        }

        public void setMoney(double money) {
            this.money = money;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}