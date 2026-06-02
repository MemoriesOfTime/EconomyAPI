package me.onebone.economyapi.provider;

import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyapi.config.EconomyAPIConfig;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

final class ProviderTestSupport {
    static final String USD = "USD";
    static final String EUR = "EUR";
    static final String MYSQL_TABLE_PREFIX = "test_";

    private ProviderTestSupport() {
    }

    static void installMainConfig() {
        installMainConfig(null);
    }

    static void installMainConfig(Config configRoot) {
        try {
            EconomyAPIConfig config = newConfigWithoutConstructor();
            setField(config, "config", configRoot);
            setField(config, "defaultCurrency", USD);
            setField(config, "autoSaveInterval", 10);
            setField(config, "provider", "yaml");

            Map<String, EconomyAPIConfig.Currency> currencies = new LinkedHashMap<>();
            currencies.put(USD, new EconomyAPIConfig.Currency(USD, "$", 1000.0, 9999999999.0, 10000));
            currencies.put(EUR, new EconomyAPIConfig.Currency(EUR, "EUR", 500.0, 5000000000.0, 11100));
            setField(config, "currencies", currencies);

            EconomyAPI.MAIN_CONFIG = config;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to configure EconomyAPIConfig for provider tests", e);
        }
    }

    static Config mysqlConfig(String host, int port, String database, String username, String password) {
        Config config = new Config(Config.YAML);
        ConfigSection mysqlSection = new ConfigSection(new LinkedHashMap<>() {{
            put("host", host);
            put("port", port);
            put("database", database);
            put("username", username);
            put("password", password);
            put("table-prefix", MYSQL_TABLE_PREFIX);
        }});
        config.set("sql.mysql", mysqlSection);
        return config;
    }

    private static EconomyAPIConfig newConfigWithoutConstructor() throws ReflectiveOperationException {
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        return (EconomyAPIConfig) unsafe.getClass()
                .getMethod("allocateInstance", Class.class)
                .invoke(unsafe, EconomyAPIConfig.class);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = EconomyAPIConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
