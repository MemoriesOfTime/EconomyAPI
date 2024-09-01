package me.onebone.economyapi.provider;

import cn.nukkit.item.ItemHelmetGold;
import com.smallaswater.easysqlx.common.data.SqlData;
import com.smallaswater.easysqlx.sqlite.SQLiteHelper;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * @author LT_Name
 */
public class SQLiteProvider implements Provider {

    private static final String TABLE_NAME = "Money";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_PLAYER = "player";
    private static final String COLUMN_MONEY = "money";

    private SQLiteHelper sqLiteHelper;

    private final HashMap<String, MoneyData> cache = new HashMap<>();

    @Override
    public void init(File path) {
        try {
            this.sqLiteHelper = new SQLiteHelper(path.getAbsolutePath() + "/Money.db");
            if (!this.sqLiteHelper.exists(TABLE_NAME)) {
                this.sqLiteHelper.addTable(TABLE_NAME, SQLiteHelper.DBTable.asDbTable(MoneyData.class));
            }
            this.sqLiteHelper.getAll(TABLE_NAME, MoneyData.class).forEach(data -> this.cache.put(data.getPlayer(), data));
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

    @Override
    public boolean accountExists(String id) {
        return this.sqLiteHelper.hasData(TABLE_NAME, COLUMN_PLAYER, id);
    }

    @Override
    public boolean removeAccount(String id) {
        if (this.accountExists(id)) {
            this.sqLiteHelper.remove(TABLE_NAME, COLUMN_PLAYER, id);
            return true;
        }
        return false;
    }

    @Override
    public boolean createAccount(String id, double defaultMoney) {
        if (!this.accountExists(id)) {
            MoneyData values = new MoneyData(id, defaultMoney);
            this.sqLiteHelper.add(TABLE_NAME, values);
            return true;
        }
        return false;
    }

    @Override
    public boolean setMoney(String id, double amount) {
        if (this.accountExists(id)) {
            MoneyData moneyData = this.getMoneyData(id);
            moneyData.setMoney(amount);
            this.sqLiteHelper.set(TABLE_NAME, COLUMN_PLAYER, id, moneyData);
            return true;
        }
        return false;
    }

    @Override
    public boolean addMoney(String id, double amount) {
        if (this.accountExists(id)) {
            this.setMoney(id, this.getMoney(id) + amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        if (this.accountExists(id)) {
            this.setMoney(id, this.getMoney(id) - amount);
            return true;
        }
        return false;
    }

    @Override
    public double getMoney(String id) {
        if (this.accountExists(id)) {
            return this.getMoneyData(id).getMoney();
        }
        return -1;
    }

    @Override
    public LinkedHashMap<String, Double> getAll() {
        LinkedHashMap<String, Double> map = new LinkedHashMap<>();
        for (MoneyData data : this.cache.values()) {
            map.put(data.getPlayer(), data.getMoney());
        }
        return map;
    }

    @Override
    public String getName() {
        return "SQLite";
    }

    private MoneyData getMoneyData(String id) {
        if (!this.cache.containsKey(id)) {
            MoneyData data = this.sqLiteHelper.get(TABLE_NAME, COLUMN_PLAYER, id, MoneyData.class);
            this.cache.put(id, data);
        }
        return this.cache.get(id);
    }

    public static class MoneyData {

        public long id;
        public String player;
        public double money;

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
    }
}
