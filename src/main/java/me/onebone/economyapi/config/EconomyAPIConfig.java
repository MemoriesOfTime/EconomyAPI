package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyapi.provider.MySQLProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyAPIConfig {
    private final Config config;
    private final Map<String, Currency> currencies = new HashMap<>();
    private final String defaultCurrency;
    private final int autoSaveInterval;
    private final String provider;

    public EconomyAPIConfig() {
        config = EconomyAPI.getInstance().getConfig();

        // 读取多货币配置
        loadCurrencies();

        // 读取默认货币
        defaultCurrency = config.getString("data.default-currency", "USD");
        autoSaveInterval = config.getInt("data.auto-save-interval", 10);
        provider = config.getString("data.provider", "yaml");

        loadSqlConfig();
    }

    // 读取货币配置
    private void loadCurrencies() {
        if (config.exists("currencies")) {
            ConfigSection currencySection = config.getSection("currencies");
            for (String currencyName : currencySection.getKeys(false)) {
                ConfigSection section = currencySection.getSection(currencyName);
                String monetaryUnit = section.getString("monetary-unit", "$");
                double defaultAmount = section.getDouble("default", 1000.0);
                double maxAmount = section.getDouble("max", 9999999999.0);
                int exchangeRate = section.getInt("exchange-rate", 10000); // 默认USD为基准汇率

                // 将货币添加到货币列表
                currencies.put(currencyName, new Currency(currencyName, monetaryUnit, defaultAmount, maxAmount, exchangeRate));
            }
        }
    }

    private void loadSqlConfig() {
        if (provider == "mysql" && config.exists("data.mysql")) {
            ConfigSection mysqlSection = config.getSection("data.mysql");
            String host = mysqlSection.getString("host", "localhost");
            int port = mysqlSection.getInt("port", 3306);
            String database = mysqlSection.getString("database", "money");
            String username = mysqlSection.getString("username", "root");
            String password = mysqlSection.getString("password", "root123456");
            MySQLProvider.initTablePrefix(mysqlSection.getString("table-prefix", "v1_"));
            MySQLProvider.initSql(new UserData(
                    username, password, host, port, database
            ));
        }
    }

    public Config getConfig() {
        return config;
    }

    // 获取默认货币
    public Currency getDefaultCurrency() {
        return currencies.get(defaultCurrency);
    }

    // 获取特定货币的信息
    public Currency getCurrency(String name) {
        return currencies.get(name);
    }

    // 获取货币种类列表
    public List<String> getCurrencyList() {
        return currencies.keySet().stream().toList();
    }

    // 获取auto-save-interval
    public int getAutoSaveInterval() {
        return autoSaveInterval * 1200;
    }

    // 获取provider
    public String getProvider() {
        return provider.toLowerCase();
    }

    // 货币类，存储每种货币的属性
    public static class Currency {
        private final String name;
        private final String monetaryUnit;
        private final double defaultAmount;
        private final double maxAmount;
        private final int exchangeRate;

        public Currency(String name, String monetaryUnit, double defaultAmount, double maxAmount, int exchangeRate) {
            this.name = name;
            this.monetaryUnit = monetaryUnit;
            this.defaultAmount = defaultAmount;
            this.maxAmount = maxAmount;
            this.exchangeRate = exchangeRate;
        }

        public String getName() {
            return name;
        }

        public String getMonetaryUnit() {
            return monetaryUnit;
        }

        public double getDefaultAmount() {
            return defaultAmount;
        }

        public double getMaxAmount() {
            return maxAmount;
        }

        public int getExchangeRate() {
            return exchangeRate;
        }
    }
}
