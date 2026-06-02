package me.onebone.economyapi.provider;

import com.smallaswater.easysqlx.sqlite.SQLiteHelper;
import com.smallaswater.easysqlx.sqlite.SQLiteHelper.DBTable;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

/**
 * @author LT_Name
 */
public class SQLiteProvider implements Provider {
    private static final String TABLE_NAME = "Money";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PLAYER = "player";
    private static final String COLUMN_MONEY = "money";
    private static final String COLUMN_CURRENCY = "currency";
    private SQLiteHelper sqLiteHelper;
    private final ConcurrentHashMap<String, MoneyData> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void init(File path) {
        lock.writeLock().lock();
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
        } finally {
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
        try {
            if (this.sqLiteHelper != null) {
                this.sqLiteHelper.close();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String getCacheKey(String currencyName, String playerName) {
        return currencyName + ":" + playerName;
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        lock.readLock().lock();
        try {
            LinkedList<MoneyData> dataList = this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_PLAYER + " = ? AND " + COLUMN_CURRENCY + " = ?", new String[]{id, currencyName}, MoneyData.class);
            return !dataList.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean accountExists(String id) {
        return accountExists(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean removeAccount(String currencyName, String id) {
        lock.writeLock().lock();
        try {
            if (!accountExists(currencyName, id)) {
                return false;
            }
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData != null) {
                this.sqLiteHelper.remove(TABLE_NAME, (int) moneyData.getId());
            }
            this.cache.remove(getCacheKey(currencyName, id));
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeAccount(String id) {
        return removeAccount(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean createAccount(String currencyName, String id, double defaultMoney) {
        lock.writeLock().lock();
        try {
            if (accountExists(currencyName, id)) {
                return false;
            }
            MoneyData values = new MoneyData(id, defaultMoney);
            values.setCurrency(currencyName); // 设置 currencyName
            this.sqLiteHelper.add(TABLE_NAME, values);
            MoneyData savedData = getMoneyDataFromStorage(currencyName, id);
            this.cache.put(getCacheKey(currencyName, id), savedData == null ? values : savedData); // 添加到缓存
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean createAccount(String id, double defaultMoney) {
        return createAccount(MAIN_CONFIG.getDefaultCurrency().getName(), id, defaultMoney);
    }

    @Override
    public boolean setMoney(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return false;
            moneyData.setMoney(amount);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean setMoney(String id, double amount) {
        return setMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public boolean addMoney(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return false;
            moneyData.setMoney(moneyData.getMoney() + amount);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean addMoney(String id, double amount) {
        return addMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public boolean reduceMoney(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return false;
            moneyData.setMoney(moneyData.getMoney() - amount);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        return reduceMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public double getMoney(String currencyName, String id) {
        lock.readLock().lock();
        try {
            MoneyData data = this.getMoneyData(currencyName, id);
            if (data != null) {
                return data.getMoney();
            }
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getMoney(String id) {
        return getMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public LinkedHashMap<String, Double> getAll(String currencyName) {
        lock.readLock().lock();
        try {
            LinkedHashMap<String, Double> map = new LinkedHashMap<>();
            MAIN_CONFIG.getCurrencyList().forEach(currency -> {
                if (currency.equals(currencyName)) {
                    this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_CURRENCY + " = ?", new String[]{currencyName}, MoneyData.class)
                            .forEach(data -> map.put(data.getPlayer(), data.getMoney()));
                }
            });
            return map;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public LinkedHashMap<String, Double> getAll() {
        return getAll(MAIN_CONFIG.getDefaultCurrency().getName());
    }

    @Override
    public String getName() {
        return "SQLite";
    }

    @Override
    public int setMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return RET_NO_ACCOUNT;
            if (amount > maxMoney) return RET_INVALID;
            moneyData.setMoney(amount);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int addMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return RET_NO_ACCOUNT;
            double newMoney = moneyData.getMoney() + amount;
            if (newMoney > maxMoney) return RET_INVALID;
            moneyData.setMoney(newMoney);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int reduceMoneyChecked(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            MoneyData moneyData = this.getMoneyData(currencyName, id);
            if (moneyData == null) return RET_NO_ACCOUNT;
            double newMoney = moneyData.getMoney() - amount;
            if (newMoney < 0) return RET_INVALID;
            moneyData.setMoney(newMoney);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_ID, String.valueOf(moneyData.getId()), moneyData);
            this.cache.put(getCacheKey(currencyName, id), moneyData);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int transferMoneyChecked(String currencyName, String fromId, String toId, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            if (!Double.isFinite(amount) || amount < 0) return RET_INVALID;
            if (fromId.equals(toId)) return RET_INVALID;
            try (Connection connection = openTransactionConnection()) {
                connection.setAutoCommit(false);
                try {
                    MoneyRecord fromRecord = getMoneyRecord(connection, currencyName, fromId);
                    MoneyRecord toRecord = getMoneyRecord(connection, currencyName, toId);
                    if (fromRecord == null || toRecord == null) {
                        connection.rollback();
                        return RET_NO_ACCOUNT;
                    }

                    double newFromMoney = fromRecord.money() - amount;
                    if (newFromMoney < 0) {
                        connection.rollback();
                        return RET_INVALID;
                    }

                    double newToMoney = toRecord.money() + amount;
                    if (newToMoney > maxMoney) {
                        connection.rollback();
                        return RET_INVALID;
                    }

                    updateMoney(connection, fromRecord.id(), newFromMoney);
                    updateMoney(connection, toRecord.id(), newToMoney);
                    connection.commit();

                    this.cache.put(getCacheKey(currencyName, fromId), new MoneyData(fromRecord.id(), fromId, newFromMoney, currencyName));
                    this.cache.put(getCacheKey(currencyName, toId), new MoneyData(toRecord.id(), toId, newToMoney, currencyName));
                    return RET_SUCCESS;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to transfer money in SQLite provider", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Connection openTransactionConnection() throws SQLException {
        return java.sql.DriverManager.getConnection("jdbc:sqlite:" + this.sqLiteHelper.getDbFilePath());
    }

    private MoneyRecord getMoneyRecord(Connection connection, String currencyName, String playerId) throws SQLException {
        String sql = "SELECT " + COLUMN_ID + ", " + COLUMN_MONEY + " FROM " + TABLE_NAME
                + " WHERE " + COLUMN_PLAYER + " = ? AND " + COLUMN_CURRENCY + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId);
            statement.setString(2, currencyName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new MoneyRecord(resultSet.getLong(COLUMN_ID), resultSet.getDouble(COLUMN_MONEY));
            }
        }
    }

    private void updateMoney(Connection connection, long rowId, double money) throws SQLException {
        String sql = "UPDATE " + TABLE_NAME + " SET " + COLUMN_MONEY + " = ? WHERE " + COLUMN_ID + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, money);
            statement.setLong(2, rowId);
            statement.executeUpdate();
        }
    }

    // 内部方法，调用方负责持有锁
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

    private MoneyData getMoneyDataFromStorage(String currencyName, String id) {
        LinkedList<MoneyData> dataList = this.sqLiteHelper.getDataByString(TABLE_NAME, COLUMN_PLAYER + " = ? AND " + COLUMN_CURRENCY + " = ?", new String[]{id, currencyName}, MoneyData.class);
        if (dataList.isEmpty()) {
            return null;
        }
        return dataList.getFirst();
    }

    public static class MoneyData {
        public long id;
        public String player;
        public double money;
        public String currency;

        public MoneyData() {
            //SQLiteHelper创建类需要无参数的构造方法
        }

        public MoneyData(String player, double money) {
            this.player = player;
            this.money = money;
        }

        public MoneyData(long id, String player, double money, String currency) {
            this.id = id;
            this.player = player;
            this.money = money;
            this.currency = currency;
        }

        public void setId(long id) {
            this.id = id;
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

    private record MoneyRecord(long id, double money) {
    }
}
