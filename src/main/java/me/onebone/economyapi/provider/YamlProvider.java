package me.onebone.economyapi.provider;

import cn.nukkit.utils.Config;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

/**
 * @author onebone
 */
public class YamlProvider implements Provider {
    private final LinkedHashMap<String, Config> currenciesData = new LinkedHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void init(File path) {
        lock.writeLock().lock();
        try {
            MAIN_CONFIG.getCurrencyList().forEach(currencyName -> {
                Config file = new Config(new File(path, "money" + File.separator + currencyName + ".yml"), Config.YAML);
                file.set("version", 3);
                LinkedHashMap<String, Object> temp = (LinkedHashMap) file.getRootSection()
                        .computeIfAbsent("money", s -> new LinkedHashMap<>());
                temp.forEach((username, money) -> {
                    if (money instanceof Integer) {
                        file.set("money." + username, ((Integer) money).doubleValue());
                    } else if (money instanceof Double) {
                        file.set("money." + username, money);
                    } else if (money instanceof String) {
                        file.set("money." + username, Double.parseDouble(money.toString()));
                    }
                });
                file.save();
                currenciesData.put(currencyName, file);
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void open() {
        // nothing to do
    }

    @Override
    public void save() {
        lock.readLock().lock();
        try {
            currenciesData.values().forEach(Config::save);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            currenciesData.values().forEach(Config::save);
            currenciesData.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        lock.readLock().lock();
        try {
            if (!currenciesData.containsKey(currencyName)) return false;
            return currenciesData.get(currencyName).exists("money." + id);
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
            if (!currenciesData.containsKey(currencyName)) return false;
            Config data = currenciesData.get(currencyName);
            if (data.exists("money." + id)) {
                data.remove("money." + id);
                return true;
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
            if (!currenciesData.containsKey(currencyName)) return false;
            Config data = currenciesData.get(currencyName);
            if (data.exists("money." + id)) return false;
            data.set("money." + id, defaultMoney);
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
            if (!currenciesData.containsKey(currencyName)) return false;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return false;
            data.set("money." + id, amount);
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
            if (!currenciesData.containsKey(currencyName)) return false;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return false;
            data.set("money." + id, data.getDouble("money." + id) + amount);
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
            if (!currenciesData.containsKey(currencyName)) return false;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return false;
            data.set("money." + id, data.getDouble("money." + id) - amount);
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
            if (!currenciesData.containsKey(currencyName)) return -1;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return -1;
            return data.getDouble("money." + id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getMoney(String id) {
        return getMoney(MAIN_CONFIG.getDefaultCurrency().getName(), id);
    }

    public LinkedHashMap<String, Double> getAll(String currencyName) {
        lock.readLock().lock();
        try {
            LinkedHashMap<String, Double> result = new LinkedHashMap<>();
            if (!currenciesData.containsKey(currencyName)) return result;
            Object moneySection = currenciesData.get(currencyName).getRootSection().get("money");
            if (!(moneySection instanceof LinkedHashMap)) return result;
            LinkedHashMap<String, Object> temp = (LinkedHashMap) moneySection;
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
        } finally {
            lock.readLock().unlock();
        }
    }

    public LinkedHashMap<String, Double> getAll() {
        return getAll(MAIN_CONFIG.getDefaultCurrency().getName());
    }

    public String getName() {
        return "Yaml";
    }

    @Override
    public int setMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            if (!currenciesData.containsKey(currencyName)) return RET_NO_ACCOUNT;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return RET_NO_ACCOUNT;
            if (amount > maxMoney) return RET_INVALID;
            data.set("money." + id, amount);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int addMoneyChecked(String currencyName, String id, double amount, double maxMoney) {
        lock.writeLock().lock();
        try {
            if (!currenciesData.containsKey(currencyName)) return RET_NO_ACCOUNT;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return RET_NO_ACCOUNT;
            double money = data.getDouble("money." + id);
            if (money + amount > maxMoney) return RET_INVALID;
            data.set("money." + id, money + amount);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int reduceMoneyChecked(String currencyName, String id, double amount) {
        lock.writeLock().lock();
        try {
            if (!currenciesData.containsKey(currencyName)) return RET_NO_ACCOUNT;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + id)) return RET_NO_ACCOUNT;
            double money = data.getDouble("money." + id);
            if (money - amount < 0) return RET_INVALID;
            data.set("money." + id, money - amount);
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
            if (!currenciesData.containsKey(currencyName)) return RET_NO_ACCOUNT;
            Config data = currenciesData.get(currencyName);
            if (!data.exists("money." + fromId) || !data.exists("money." + toId)) return RET_NO_ACCOUNT;

            double fromMoney = data.getDouble("money." + fromId);
            if (fromMoney - amount < 0) return RET_INVALID;

            double toMoney = data.getDouble("money." + toId);
            if (toMoney + amount > maxMoney) return RET_INVALID;

            data.set("money." + fromId, fromMoney - amount);
            data.set("money." + toId, toMoney + amount);
            return RET_SUCCESS;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
