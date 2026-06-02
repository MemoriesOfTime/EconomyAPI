package me.onebone.economyapi.config;

import cn.nukkit.utils.Config;
import me.onebone.economyapi.TestPluginSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyAPIConfigTest {
    @Test
    void constructorLoadsCurrenciesAndDataSettings() {
        Config config = configWithUsdAndEur();
        config.set("data.default-currency", TestPluginSupport.EUR);
        config.set("data.auto-save-interval", 25);
        config.set("data.provider", "SQLite");
        TestPluginSupport.installPlugin(config, null);

        EconomyAPIConfig economyConfig = new EconomyAPIConfig();

        assertEquals(config, economyConfig.getConfig());
        assertEquals(TestPluginSupport.EUR, economyConfig.getDefaultCurrency().getName());
        assertEquals("EUR", economyConfig.getDefaultCurrency().getMonetaryUnit());
        assertEquals(500.0, economyConfig.getDefaultCurrency().getDefaultAmount());
        assertEquals(5000000000.0, economyConfig.getDefaultCurrency().getMaxAmount());
        assertEquals(11100, economyConfig.getDefaultCurrency().getExchangeRate());
        assertEquals(25, economyConfig.getAutoSaveInterval());
        assertEquals("sqlite", economyConfig.getProvider());
        assertTrue(economyConfig.getCurrencyList().containsAll(List.of(TestPluginSupport.USD, TestPluginSupport.EUR)));
    }

    @Test
    void missingDefaultCurrencyFailsFast() {
        Config config = configWithUsdAndEur();
        config.set("data.default-currency", "GBP");
        TestPluginSupport.installPlugin(config, null);
        EconomyAPIConfig economyConfig = new EconomyAPIConfig();

        IllegalStateException exception = assertThrows(IllegalStateException.class, economyConfig::getDefaultCurrency);

        assertTrue(exception.getMessage().contains("Default currency 'GBP' not found"));
    }

    @Test
    void missingRequestedCurrencyReportsAvailableCurrencies() {
        Config config = configWithUsdAndEur();
        TestPluginSupport.installPlugin(config, null);
        EconomyAPIConfig economyConfig = new EconomyAPIConfig();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> economyConfig.getCurrency("GBP"));

        assertTrue(exception.getMessage().contains("Currency 'GBP' not found"));
        assertTrue(exception.getMessage().contains(TestPluginSupport.USD));
    }

    @Test
    void missingDataSettingsUseDefaults() {
        Config config = configWithUsdAndEur();
        TestPluginSupport.installPlugin(config, null);

        EconomyAPIConfig economyConfig = new EconomyAPIConfig();

        assertEquals(TestPluginSupport.USD, economyConfig.getDefaultCurrency().getName());
        assertEquals(10, economyConfig.getAutoSaveInterval());
        assertEquals("yaml", economyConfig.getProvider());
    }

    private Config configWithUsdAndEur() {
        Config config = new Config(Config.YAML);
        config.set("currencies.USD.monetary-unit", "$");
        config.set("currencies.USD.default", 1000.0);
        config.set("currencies.USD.max", 9999999999.0);
        config.set("currencies.USD.exchange-rate", 10000);
        config.set("currencies.EUR.monetary-unit", "EUR");
        config.set("currencies.EUR.default", 500.0);
        config.set("currencies.EUR.max", 5000000000.0);
        config.set("currencies.EUR.exchange-rate", 11100);
        return config;
    }
}
