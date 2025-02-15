package me.onebone.economyapi.provider;

import java.io.File;
import java.util.LinkedHashMap;

public interface Provider {

    void init(File path);

    void open();

    void save();

    void close();

    boolean accountExists(String currencyName, String id);

    boolean accountExists(String id);

    boolean removeAccount(String currencyName, String id);

    boolean removeAccount(String id);

    boolean createAccount(String currencyName, String id, double defaultMoney);

    boolean createAccount(String id, double defaultMoney);

    boolean setMoney(String currencyName, String id, double amount);

    boolean setMoney(String id, double amount);

    boolean addMoney(String currencyName, String id, double amount);

    boolean addMoney(String id, double amount);

    boolean reduceMoney(String currencyName, String id, double amount);

    boolean reduceMoney(String id, double amount);

    double getMoney(String currencyName, String id);

    double getMoney(String id);

    LinkedHashMap<String, Double> getAll(String currencyName);

    LinkedHashMap<String, Double> getAll();

    String getName();
}
