package me.onebone.economyapi.provider;

import cn.nukkit.utils.Config;

import java.io.File;
import java.util.LinkedHashMap;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

/**
 * @author onebone
 */
public class YamlProvider implements Provider {
    private final LinkedHashMap<String, Config> currenciesData = new LinkedHashMap<>();

    @Override
    public void init(File path) {
        MAIN_CONFIG.getCurrencyList().forEach(currencyName -> {
            Config file = new Config(new File(path, "money" + File.separator + currencyName + ".yml"), Config.YAML);
            file.set("version", 3);
            LinkedHashMap<String, Object> temp = (LinkedHashMap) file.getRootSection()
                    .computeIfAbsent("money", s -> new LinkedHashMap<>());
            temp.forEach((username, money) -> {
                if (money instanceof Integer) {
                    file.set(username, ((Integer) money).doubleValue());
                } else if (money instanceof Double) {
                    file.set(username, money);
                } else if (money instanceof String) {
                    file.set(username, Double.parseDouble(money.toString()));
                }
            });
            file.save();
            currenciesData.put(currencyName, file);
        });
    }

    @Override
    public void open() {
        // nothing to do
    }

    @Override
    public void save() {
        currenciesData.values().forEach(cfg -> cfg.save());
    }

    @Override
    public void close() {
        this.save();
        currenciesData.clear();
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        if (!currenciesData.containsKey(currencyName)) return false;
        return currenciesData.get(currencyName).exists("money." + id);
    }

    @Override
    public boolean accountExists(String id) {
        return accountExists(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean removeAccount(String currencyName, String id) {
        if (!currenciesData.containsKey(currencyName)) return false;
        if (accountExists(currencyName, id)) {
            currenciesData.get(currencyName).remove("money." + id);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAccount(String id) {
        return removeAccount(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    @Override
    public boolean createAccount(String currencyName, String id, double defaultMoney) {
        if (!accountExists(currencyName, id)) {
            if (!currenciesData.containsKey(currencyName)) return false;
            currenciesData.get(currencyName).set("money." + id, defaultMoney);
            return true;
        }
        return false;
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
        currenciesData.get(currencyName).set("money." + id, amount);
        return false;
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
        Config data = currenciesData.get(currencyName);
        data.set("money." + id, data.getDouble("money." + id) + amount);
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
        Config data = currenciesData.get(currencyName);
        data.set("money." + id, data.getDouble("money." + id) - amount);
        return true;
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        return reduceMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id, amount);
    }

    @Override
    public double getMoney(String currencyName, String id) {
        if (!accountExists(currencyName, id)) {
            return -1;
        }
        return currenciesData.get(currencyName).getDouble("money." + id);
    }

    @Override
    public double getMoney(String id) {
        return getMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    public LinkedHashMap<String, Double> getAll(String currencyName) {
        LinkedHashMap<String, Double> result = new LinkedHashMap<>();
        if (!currenciesData.containsKey(currencyName)) return result;
        LinkedHashMap<String, Object> temp = (LinkedHashMap) currenciesData.get(currencyName).getRootSection()
                .computeIfAbsent("money", s -> new LinkedHashMap<>());
        temp.forEach((username, money) -> {
            if (money instanceof Integer) {
                result.put(username, ((Integer) money).doubleValue());
            } else if (money instanceof Double) {
                result.put(username, (Double) money);
            } else if (money instanceof String) {
                result.put(username, Double.parseDouble(money.toString()));
            }
        });
        return result;
    }

    public LinkedHashMap<String, Double> getAll() {
        return getAll(MAIN_CONFIG.getDefaultCurrency().getName());
    }

    public String getName() {
        return "Yaml";
    }
}
