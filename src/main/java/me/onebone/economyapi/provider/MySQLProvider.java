package me.onebone.economyapi.provider;

import cn.nukkit.Server;
import cn.nukkit.utils.ConfigSection;
import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.common.data.SqlDataList;
import com.smallaswater.easysqlx.exceptions.MySqlLoginException;
import com.smallaswater.easysqlx.mysql.manager.SqlManager;
import com.smallaswater.easysqlx.mysql.utils.DataType;
import com.smallaswater.easysqlx.mysql.utils.TableType;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import me.onebone.economyapi.EconomyAPI;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

public class MySQLProvider implements Provider {
    private static volatile SqlManager manager;
    private static String TABLE_NAME_PREFIX = "";
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void initTablePrefix(String prefix) {
        MySQLProvider.TABLE_NAME_PREFIX = prefix;
    }

    static void useManagerForTesting(SqlManager manager, String tableNamePrefix) {
        MySQLProvider.manager = manager;
        MySQLProvider.TABLE_NAME_PREFIX = tableNamePrefix;
    }

    private static boolean isReady() {
        return MySQLProvider.manager != null;
    }

    private static String quoteTableName(String tableName) {
        return "`" + tableName.trim().replace("`", "``") + "`";
    }

    private static long toCents(double amount) {
        return Math.round(amount * 100);
    }

    @Override
    public void init(File path) {
        if (MySQLProvider.manager != null && MySQLProvider.manager.isEnable()) {
            EconomyAPI.getInstance().getLogger().warning("MySQL is already initialized.");
            return;
        }

        if (!MAIN_CONFIG.getConfig().exists("sql.mysql")) {
            throw new RuntimeException("MySQL is not configured.");
        }
        ConfigSection mysqlSection = MAIN_CONFIG.getConfig().getSection("sql.mysql");
        String host = mysqlSection.getString("host", "localhost");
        int port = mysqlSection.getInt("port", 3306);
        String database = mysqlSection.getString("database", "economy");
        String username = mysqlSection.getString("username", "root");
        String password = mysqlSection.getString("password", "root123456");
        String tablePrefix = mysqlSection.getString("table-prefix", "v1_");
        MySQLProvider.initTablePrefix(tablePrefix);

        try {
            SqlManager manager = new SqlManager(EconomyAPI.getInstance(), new UserData(
                    username, password, host, port, database
            ));
            MySQLProvider.manager = manager;

            MAIN_CONFIG.getCurrencyList().forEach(currencyName -> {
                manager.createTable(
                        TABLE_NAME_PREFIX + currencyName,
                        new TableType("player", DataType.getUUID(), true),
                        new TableType("money", DataType.getBIGINT(), false)
                );
            });
            EconomyAPI.getInstance().getLogger().info("MySQL initialized!");
        } catch (MySqlLoginException e) {
            EconomyAPI.getInstance().getLogger().error("MySQL connection failed.", e);
            Server.getInstance().getPluginManager().disablePlugin(EconomyAPI.getInstance());
        }
    }

    @Override
    public void open() {
        if (MySQLProvider.manager == null) return;
        if (!MySQLProvider.manager.isEnable()) {
            this.init(null);
        }
    }

    @Override
    public void save() {
        // not required in MySQL.
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            if (MySQLProvider.manager == null) return;
            MySQLProvider.manager.disable();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        lock.readLock().lock();
        try {
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            if (MySQLProvider.manager.isExistTable(TABLE_NAME_PREFIX + currencyName)) {
                return MySQLProvider.manager.isExistsData(TABLE_NAME_PREFIX + currencyName, "player", id);
            }
            return false;
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
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            if (MySQLProvider.manager.isExistTable(TABLE_NAME_PREFIX + currencyName)) {
                return MySQLProvider.manager.deleteData(TABLE_NAME_PREFIX + currencyName, new SqlData("player", id));
            }
            return false;
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
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            if (accountExists(currencyName, id)) return false;
            long money = toCents(defaultMoney);
            String tableName = quoteTableName(TABLE_NAME_PREFIX + currencyName);
            String sql = "INSERT INTO " + tableName + " (player, money) VALUES (?, ?)";
            try (Connection connection = MySQLProvider.manager.getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setLong(2, money);
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                EconomyAPI.getInstance().getLogger().error("Failed to create account in MySQL", e);
                return false;
            }
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
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            long money = toCents(amount);
            return MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
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
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            Long current = getMoneyCents(currencyName, id);
            if (current == null) return false;
            long money = current + toCents(amount);
            return MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
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
            if (!isReady()) return false;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return false;
            Long current = getMoneyCents(currencyName, id);
            if (current == null) return false;
            long money = current - toCents(amount);
            return MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
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
            Long money = getMoneyCents(currencyName, id);
            return money == null ? -1 : money / 100.0;
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
            if (!isReady()) return map;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return map;
            SqlData emptyData = new SqlData();
            SqlDataList<SqlData> sqlDataList = MySQLProvider.manager.getData(TABLE_NAME_PREFIX + currencyName, "*", emptyData);
            if (sqlDataList == null) {
                return map;
            }
            for (SqlData sqlData : sqlDataList) {
                LinkedHashMap<String, Object> data = sqlData.getData();
                try {
                    String playerId = (String) data.get("player");
                    long moneyObj = (long) data.get("money");
                    map.put(playerId, moneyObj / 100.0);
                } catch (Exception e) {
                    EconomyAPI.getInstance().getLogger().error("Error processing SqlData: " + sqlData, e);
                }
            }
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
        return "MySQL";
    }

    @Override
    public int setMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            if (!isReady()) return RET_NO_ACCOUNT;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return RET_NO_ACCOUNT;
            if (!accountExists(currencyName, id)) return RET_NO_ACCOUNT;
            if (amount > maxMoney) return RET_INVALID;
            long money = toCents(amount);
            MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int addMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            if (!isReady()) return RET_NO_ACCOUNT;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return RET_NO_ACCOUNT;
            Long current = getMoneyCents(currencyName, id);
            if (current == null) return RET_NO_ACCOUNT;
            long amountCents = toCents(amount);
            long maxMoneyCents = toCents(maxMoney);
            long money = current + amountCents;
            if (money < current || money > maxMoneyCents) return RET_INVALID;
            MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int reduceMoneyChecked(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            if (!isReady()) return RET_NO_ACCOUNT;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return RET_NO_ACCOUNT;
            Long current = getMoneyCents(currencyName, id);
            if (current == null) return RET_NO_ACCOUNT;
            long amountCents = toCents(amount);
            if (current - amountCents < 0) return RET_INVALID;
            long money = current - amountCents;
            MySQLProvider.manager.setData(TABLE_NAME_PREFIX + currencyName, new SqlData("money", money), new SqlData("player", id));
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
            if (!isReady()) return RET_NO_ACCOUNT;
            if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return RET_NO_ACCOUNT;
            long amountCents = toCents(amount);
            long maxMoneyCents = toCents(maxMoney);
            String tableName = quoteTableName(TABLE_NAME_PREFIX + currencyName);
            try (Connection connection = MySQLProvider.manager.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    String firstLockId = fromId.compareTo(toId) <= 0 ? fromId : toId;
                    String secondLockId = firstLockId.equals(fromId) ? toId : fromId;

                    Long firstLockMoney = getMoneyForUpdate(connection, tableName, firstLockId);
                    Long secondLockMoney = getMoneyForUpdate(connection, tableName, secondLockId);
                    Long fromMoney = fromId.equals(firstLockId) ? firstLockMoney : secondLockMoney;
                    Long toMoney = toId.equals(firstLockId) ? firstLockMoney : secondLockMoney;
                    if (fromMoney == null || toMoney == null) {
                        connection.rollback();
                        return RET_NO_ACCOUNT;
                    }

                    long newFromMoney = fromMoney - amountCents;
                    if (newFromMoney < 0) {
                        connection.rollback();
                        return RET_INVALID;
                    }

                    long newToMoney = toMoney + amountCents;
                    if (newToMoney < toMoney || newToMoney > maxMoneyCents) {
                        connection.rollback();
                        return RET_INVALID;
                    }

                    updateMoney(connection, tableName, fromId, newFromMoney);
                    updateMoney(connection, tableName, toId, newToMoney);
                    connection.commit();
                    return RET_SUCCESS;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to transfer money in MySQL provider", e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Long getMoneyCents(String currencyName, String id) {
        if (!isReady()) return null;
        if (!MAIN_CONFIG.getCurrencyList().contains(currencyName)) return null;
        SqlDataList<SqlData> sqlDataList = MySQLProvider.manager.getData(TABLE_NAME_PREFIX + currencyName, "money", new SqlData("player", id));
        if (sqlDataList.isEmpty()) return null;
        return sqlDataList.get(0).getLong("money");
    }

    private Long getMoneyForUpdate(Connection connection, String tableName, String playerId) throws SQLException {
        String sql = "SELECT money FROM " + tableName + " WHERE player = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return resultSet.getLong("money");
            }
        }
    }

    private void updateMoney(Connection connection, String tableName, String playerId, long money) throws SQLException {
        String sql = "UPDATE " + tableName + " SET money = ? WHERE player = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, money);
            statement.setString(2, playerId);
            statement.executeUpdate();
        }
    }
}
