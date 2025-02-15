package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import me.onebone.economyapi.EconomyAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}
