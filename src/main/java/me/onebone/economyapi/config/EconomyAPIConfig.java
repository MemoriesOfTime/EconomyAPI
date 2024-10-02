package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import me.onebone.economyapi.EconomyAPI;
import java.util.HashMap;
import java.util.Map;

public class EconomyAPIConfig {
    private final Config config;
    private final Map<String, Currency> currencies = new HashMap<>();
    private final String defaultCurrency;

    public EconomyAPIConfig() {
        // 保存默认配置文件
        EconomyAPI.getInstance().saveDefaultConfig();
        config = EconomyAPI.getInstance().getConfig();

        // 读取多货币配置
        loadCurrencies();

        // 读取默认货币
        defaultCurrency = config.getString("data.default-currency", "USD");
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
                currencies.put(currencyName, new Currency(monetaryUnit, defaultAmount, maxAmount, exchangeRate));
            }
        }
    }

    // 获取默认货币
    public Currency getDefaultCurrency() {
        return currencies.get(defaultCurrency);
    }

    // 获取特定货币的信息
    public Currency getCurrency(String name) {
        return currencies.get(name);
    }

    // 货币类，存储每种货币的属性
    public static class Currency {
        private final String monetaryUnit;
        private final double defaultAmount;
        private final double maxAmount;
        private final int exchangeRate;

        public Currency(String monetaryUnit, double defaultAmount, double maxAmount, int exchangeRate) {
            this.monetaryUnit = monetaryUnit;
            this.defaultAmount = defaultAmount;
            this.maxAmount = maxAmount;
            this.exchangeRate = exchangeRate;
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
