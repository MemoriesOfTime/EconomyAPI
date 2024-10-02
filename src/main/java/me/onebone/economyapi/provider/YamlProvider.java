package me.onebone.economyapi.provider;

/*
 * EconomyAPI: Core of economy system for Nukkit
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cn.nukkit.utils.Config;

import java.io.File;
import java.util.LinkedHashMap;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;

public class YamlProvider implements Provider {
    private final LinkedHashMap<String, Config> currenciesData = new LinkedHashMap<>();

    @Override
    public void init(File path) {
        MAIN_CONFIG.getCurrencyList().forEach(currencyName -> {
            Config file = new Config(new File(path, "money/" + currencyName + ".yml"), Config.YAML);
            file.set("version", 2);
            LinkedHashMap<String, Object> temp = (LinkedHashMap) file.getRootSection()
                    .computeIfAbsent("money", s -> new LinkedHashMap<>());
            temp.forEach((username, money) -> {
                if (money instanceof Integer) {
                    file.set(username, ((Integer) money).doubleValue());
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
        return createAccount(MAIN_CONFIG.getDefaultCurrency().getName(), defaultMoney);
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
