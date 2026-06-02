package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import me.onebone.economyapi.TestPluginSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeConfigTest {
    @TempDir
    Path dataFolder;

    @Test
    void tryUpgradeConfigVersionMigratesVersionOneConfigValues() {
        Config oldConfig = new Config(dataFolder.resolve("config.yml").toFile(), Config.YAML);
        oldConfig.set("money.monetary-unit", "coins");
        oldConfig.set("money.default", 25.5);
        oldConfig.set("money.max", 1000.0);
        oldConfig.set("data.auto-save-interval", 7);
        oldConfig.set("data.provider", "sqlite");
        oldConfig.save();
        Config newConfig = new Config(Config.YAML);
        TestPluginSupport.installPlugin(newConfig, dataFolder.toFile());

        assertTrue(UpgradeConfig.tryUpgradeConfigVersion(1));

        assertTrue(dataFolder.resolve("config.old.yml").toFile().isFile());
        assertEquals("coins", newConfig.getString("currencies.USD.monetary-unit"));
        assertEquals(25.5, newConfig.getDouble("currencies.USD.default"));
        assertEquals(1000.0, newConfig.getDouble("currencies.USD.max"));
        assertEquals(7, newConfig.getInt("data.auto-save-interval"));
        assertEquals("sqlite", newConfig.getString("data.provider"));
    }

    @Test
    void tryUpgradeConfigVersionIgnoresUnknownVersions() {
        TestPluginSupport.installPlugin(new Config(Config.YAML), dataFolder.toFile());

        assertFalse(UpgradeConfig.tryUpgradeConfigVersion(2));
    }

    @Test
    void oldMoneyDataExposesMigratedFields() {
        UpgradeConfig.OldMoneyData data = new UpgradeConfig.OldMoneyData("alice", 10);

        data.setMoney(12.5);

        assertEquals(0, data.getId());
        assertEquals("alice", data.getPlayer());
        assertEquals(12.5, data.getMoney());
    }
}
