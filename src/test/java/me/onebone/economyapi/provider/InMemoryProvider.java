package me.onebone.economyapi.provider;

import java.io.File;
import java.util.LinkedHashMap;

import static me.onebone.economyapi.provider.ProviderTestSupport.USD;

public class InMemoryProvider implements Provider {
    private final LinkedHashMap<String, LinkedHashMap<String, Double>> balances = new LinkedHashMap<>();
    private int failedAdds;

    void failNextAdd() {
        failNextAdds(1);
    }

    void failNextAdds(int count) {
        failedAdds = count;
    }

    @Override
    public void init(File path) {
    }

    @Override
    public void open() {
    }

    @Override
    public void save() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean accountExists(String currencyName, String id) {
        return balances.getOrDefault(currencyName, new LinkedHashMap<>()).containsKey(id);
    }

    @Override
    public boolean accountExists(String id) {
        return accountExists(USD, id);
    }

    @Override
    public boolean removeAccount(String currencyName, String id) {
        LinkedHashMap<String, Double> currencyBalances = balances.get(currencyName);
        return currencyBalances != null && currencyBalances.remove(id) != null;
    }

    @Override
    public boolean removeAccount(String id) {
        return removeAccount(USD, id);
    }

    @Override
    public boolean createAccount(String currencyName, String id, double defaultMoney) {
        balances.computeIfAbsent(currencyName, ignored -> new LinkedHashMap<>());
        if (accountExists(currencyName, id)) {
            return false;
        }
        balances.get(currencyName).put(id, defaultMoney);
        return true;
    }

    @Override
    public boolean createAccount(String id, double defaultMoney) {
        return createAccount(USD, id, defaultMoney);
    }

    @Override
    public boolean setMoney(String currencyName, String id, double amount) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        balances.get(currencyName).put(id, amount);
        return true;
    }

    @Override
    public boolean setMoney(String id, double amount) {
        return setMoney(USD, id, amount);
    }

    @Override
    public boolean addMoney(String currencyName, String id, double amount) {
        if (failedAdds > 0) {
            failedAdds--;
            return false;
        }
        if (!accountExists(currencyName, id)) {
            return false;
        }
        balances.get(currencyName).put(id, getMoney(currencyName, id) + amount);
        return true;
    }

    @Override
    public boolean addMoney(String id, double amount) {
        return addMoney(USD, id, amount);
    }

    @Override
    public boolean reduceMoney(String currencyName, String id, double amount) {
        if (!accountExists(currencyName, id)) {
            return false;
        }
        balances.get(currencyName).put(id, getMoney(currencyName, id) - amount);
        return true;
    }

    @Override
    public boolean reduceMoney(String id, double amount) {
        return reduceMoney(USD, id, amount);
    }

    @Override
    public double getMoney(String currencyName, String id) {
        if (!accountExists(currencyName, id)) {
            return -1;
        }
        return balances.get(currencyName).get(id);
    }

    @Override
    public double getMoney(String id) {
        return getMoney(USD, id);
    }

    @Override
    public LinkedHashMap<String, Double> getAll(String currencyName) {
        return new LinkedHashMap<>(balances.getOrDefault(currencyName, new LinkedHashMap<>()));
    }

    @Override
    public LinkedHashMap<String, Double> getAll() {
        return getAll(USD);
    }

    @Override
    public String getName() {
        return "InMemory";
    }
}
