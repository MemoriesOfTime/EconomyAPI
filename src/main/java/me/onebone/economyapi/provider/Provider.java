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
