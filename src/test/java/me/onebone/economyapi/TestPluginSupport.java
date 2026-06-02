package me.onebone.economyapi;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import me.onebone.economyapi.config.EconomyAPIConfig;

import java.io.File;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestPluginSupport {
    public static final String USD = "USD";
    public static final String EUR = "EUR";

    private TestPluginSupport() {
    }

    public static EconomyAPI installPlugin(Config config, File dataFolder) {
        EconomyAPI plugin = new EconomyAPI();
        setPluginBaseField(plugin, "config", config);
        if (dataFolder != null) {
            setPluginBaseField(plugin, "dataFolder", dataFolder);
        }
        setEconomyInstance(plugin);
        return plugin;
    }

    public static void setPluginEnabled(EconomyAPI plugin, boolean enabled) {
        setPluginBaseField(plugin, "isEnabled", enabled);
    }

    public static void installMainConfig() {
        installMainConfig(null);
    }

    public static void installMainConfig(Config configRoot) {
        try {
            EconomyAPIConfig config = newConfigWithoutConstructor();
            setEconomyConfigField(config, "config", configRoot);
            setEconomyConfigField(config, "defaultCurrency", USD);
            setEconomyConfigField(config, "autoSaveInterval", 10);
            setEconomyConfigField(config, "provider", "yaml");

            Map<String, EconomyAPIConfig.Currency> currencies = new LinkedHashMap<>();
            currencies.put(USD, new EconomyAPIConfig.Currency(USD, "$", 1000.0, 9999999999.0, 10000));
            currencies.put(EUR, new EconomyAPIConfig.Currency(EUR, "EUR", 500.0, 5000000000.0, 11100));
            setEconomyConfigField(config, "currencies", currencies);

            EconomyAPI.MAIN_CONFIG = config;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to configure EconomyAPIConfig for tests", e);
        }
    }

    private static EconomyAPIConfig newConfigWithoutConstructor() throws ReflectiveOperationException {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        return (EconomyAPIConfig) unsafe.getClass()
                .getMethod("allocateInstance", Class.class)
                .invoke(unsafe, EconomyAPIConfig.class);
    }

    private static void setEconomyConfigField(EconomyAPIConfig config, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = EconomyAPIConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(config, value);
    }

    private static void setPluginBaseField(EconomyAPI plugin, String fieldName, Object value) {
        try {
            Field field = PluginBase.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(plugin, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set PluginBase field " + fieldName, e);
        }
    }

    private static void setEconomyInstance(EconomyAPI plugin) {
        try {
            Field field = EconomyAPI.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, plugin);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to install EconomyAPI instance", e);
        }
    }
}
