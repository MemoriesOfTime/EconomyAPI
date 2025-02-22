package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.smallaswater.easysqlx.sqlite.SQLiteHelper;
import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyapi.provider.SQLiteProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

public class UpgradeConfig {
    public static boolean tryUpgradeConfigVersion(int oldVersion) {
        if (oldVersion == 1) {
            Path target = Paths.get(EconomyAPI.getInstance().getDataFolder().toString(), "config.old.yml");
            try {
                Files.move(
                        Paths.get(EconomyAPI.getInstance().getDataFolder().toString(), "config.yml"),
                        target);
            } catch (IOException ignored) {
            }
            Config oldConfig = new Config(target.toFile());
            Config config = EconomyAPI.getInstance().getConfig();
            config.set("currencies.USD.monetary-unit", oldConfig.getString("money.monetary-unit"));
            config.set("currencies.USD.default", oldConfig.getDouble("money.default"));
            config.set("currencies.USD.max", oldConfig.getDouble("money.max"));
            config.set("data.auto-save-interval", oldConfig.getInt("data.auto-save-interval"));
            config.set("data.provider", oldConfig.getString("data.provider"));
            return true;
        }
        return false;
    }

    protected static String TEMP_CURRENCY_NAME = "USD";

    public static boolean updateDoubleConfirmation() {
        System.out.println("An older version of the Money.yml configuration file has been detected. Would you like to upgrade?");
        System.out.println("The upgrade process will perform the following steps:");
        System.out.println("1. Copy the Money.yml file to a new file under the money directory (money/" + TEMP_CURRENCY_NAME + ".yml)");
        System.out.println("2. Modify the version of the new file to 3");
        System.out.println("3. Rename the old Money.yml file to Money.old.yml");
        System.out.print("Please confirm whether to upgrade (yes/no): ");
        if (new Scanner(System.in).nextLine().toLowerCase().startsWith("y")) {
            System.out.println("User confirmed the upgrade. Starting the upgrade process...");
        } else {
            System.out.println("The plugin version is too high and does not support this configuration file. Please use version 2.0.6 of EconomyAPI!");
            System.exit(1);
        }

        EconomyAPI.getInstance().saveDefaultConfig();
        EconomyAPI.getInstance().getConfig().getSection("currencies").set(
                TEMP_CURRENCY_NAME,
                new ConfigSection(new LinkedHashMap<>() {{
                    put("monetary-unit", "$");
                    put("default", 1000.0);
                    put("max", 9999999999.0);
                    put("exchange-rate", 10000);
                }})
        );
        EconomyAPI.getInstance().getConfig().set("data.default-currency", TEMP_CURRENCY_NAME);
        return true;
    }

    public static boolean tryUpgradeYamlData() {
        EconomyAPI economyAPI = EconomyAPI.getInstance();
        String dataFolder = economyAPI.getDataFolder().toString();

        Path sourceFile = Paths.get(dataFolder, "Money.yml");
        Path targetDir = Paths.get(dataFolder, "money");
        Path targetFile = targetDir.resolve(EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName() + ".yml");
        Path oldSourceFile = Paths.get(dataFolder, "Money.old.yml");

        if (!Files.exists(sourceFile)) {
            economyAPI.getLogger().warning("Money.yml file not found.");
            return false;
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

            Config file = new Config(targetFile.toFile(), Config.YAML);
            file.set("version", 3);
            file.save();

            Files.move(sourceFile, oldSourceFile, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            economyAPI.getLogger().warning("Failed to upgrade Money.yml data.", e); // 记录完整异常
            return false;
        }
    }

    public static boolean tryUpgradeSQLiteData() {
        if (!EconomyAPI.MAIN_CONFIG.getProvider().equals("sqlite")) {
            EconomyAPI.getInstance().getLogger().info("SQLite is not enabled, no upgrade is required.");
            return false;
        }
        Path sourceFile = Paths.get(EconomyAPI.getInstance().getDataFolder().toString(), "MoneyV3.db");
        Path targetFile = Paths.get(EconomyAPI.getInstance().getDataFolder().toString(), "Money.db.old");
        try {
            Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
        if (!Files.exists(sourceFile)) {
            EconomyAPI.getInstance().getLogger().warning("MoneyV3.db file not found.");
            return false;
        }
        String TABLE_NAME = "Money";
        try {
            SQLiteHelper oldSQLiteHelper = new SQLiteHelper(EconomyAPI.getInstance().getDataFolder().getAbsolutePath() + File.separator + "Money.old.db");
            if (!oldSQLiteHelper.exists(TABLE_NAME)) {
                System.out.println("Old data table 'Money' not found in Money.old.db, data migration skipped.");
                return false;
            }
            List<OldMoneyData> oldMoneyDataList = oldSQLiteHelper.getAll(TABLE_NAME, OldMoneyData.class);
            if (oldMoneyDataList == null) {
                System.out.println("Failed to read data from 'Money' table in Money.old.db, data migration aborted.");
                return false;
            }
            SQLiteHelper sqLiteHelper = new SQLiteHelper(EconomyAPI.getInstance().getDataFolder().getAbsolutePath() + File.separator + "MoneyV3.db");
            if (!sqLiteHelper.exists(TABLE_NAME)) {
                sqLiteHelper.addTable(TABLE_NAME, SQLiteHelper.DBTable.asDbTable(SQLiteProvider.MoneyData.class));
            }
            for (OldMoneyData data : oldMoneyDataList) {
                // 将 OldMoneyData 转换为 MoneyData
                SQLiteProvider.MoneyData moneyData = new SQLiteProvider.MoneyData(data.getPlayer(), data.getMoney());
                moneyData.setCurrency(EconomyAPI.MAIN_CONFIG.getDefaultCurrency().getName());

                sqLiteHelper.add(TABLE_NAME, moneyData); // 插入新的 MoneyData 对象
            }
            System.out.println("SQLite data migration complete!"); // 添加迁移完成的提示
            return true;
        } catch (ClassNotFoundException e) {
            EconomyAPI.getInstance().getLogger().warning("Failed to upgrade MoneyV3.db data, runtime error.", e);
            return false;
        } catch (SQLException e) {
            EconomyAPI.getInstance().getLogger().warning("Failed to upgrade Money.yml data, sql exception.", e);
            return false;
        }
    }


    public static class OldMoneyData {
        public long id;
        public String player;
        public double money;

        public OldMoneyData() {
        }

        public OldMoneyData(String player, double money) {
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
