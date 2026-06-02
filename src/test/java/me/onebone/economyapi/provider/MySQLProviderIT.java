package me.onebone.economyapi.provider;

import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.LogLevel;
import cn.nukkit.utils.Logger;
import com.smallaswater.easysqlx.mysql.manager.SqlManager;
import com.smallaswater.easysqlx.mysql.utils.DataType;
import com.smallaswater.easysqlx.mysql.utils.TableType;
import com.smallaswater.easysqlx.mysql.utils.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Proxy;
import java.util.Map;

import static me.onebone.economyapi.provider.ProviderTestSupport.EUR;
import static me.onebone.economyapi.provider.ProviderTestSupport.MYSQL_TABLE_PREFIX;
import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySQLProviderIT implements ProviderContractTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("economy")
            .withUsername("economy")
            .withPassword("economy");

    private SqlManager manager;
    private MySQLProvider provider;

    @BeforeAll
    void requireDocker() {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is required for MySQL provider tests");
    }

    @BeforeEach
    void setUp() throws Exception {
        ProviderTestSupport.installMainConfig(ProviderTestSupport.mysqlConfig(
                MYSQL.getHost(),
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT),
                MYSQL.getDatabaseName(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
        manager = new SqlManager(testPlugin(), new UserData(
                MYSQL.getUsername(),
                MYSQL.getPassword(),
                MYSQL.getHost(),
                MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT),
                MYSQL.getDatabaseName()
        ));
        MySQLProvider.useManagerForTesting(manager, MYSQL_TABLE_PREFIX);
        createProviderTables();
        provider = new MySQLProvider();
    }

    @Override
    public Provider provider() {
        return provider;
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.deleteTable(MYSQL_TABLE_PREFIX + USD);
            manager.deleteTable(MYSQL_TABLE_PREFIX + EUR);
            manager.disable();
        }
    }

    private Plugin testPlugin() {
        PluginDescription description = new PluginDescription(Map.of(
                "name", "EconomyAPITest",
                "main", "me.onebone.economyapi.TestPlugin",
                "version", "1.0.0",
                "api", "1.0.0",
                "prefix", "EconomyAPITest"
        ));
        return (Plugin) Proxy.newProxyInstance(
                Plugin.class.getClassLoader(),
                new Class[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getDescription" -> description;
                    case "getLogger" -> new NoOpPluginLogger((Plugin) proxy);
                    case "getName" -> "EconomyAPITest";
                    case "isEnabled" -> true;
                    case "isDisabled" -> false;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "EconomyAPITest";
                    case "onLoad", "onEnable", "onDisable" -> null;
                    default -> throw new UnsupportedOperationException("Unsupported test plugin method: " + method.getName());
                }
        );
    }

    private static class NoOpPluginLogger extends PluginLogger {
        NoOpPluginLogger(Plugin plugin) {
            super(plugin);
        }

        @Override
        public void log(LogLevel level, String message) {
        }

        @Override
        public void log(LogLevel level, String message, Throwable throwable) {
        }
    }

    @Test
    void createAccountStoresBalancesAsMoneyValues() {
        assertTrue(provider.createAccount(USD, "alice", 10.25));

        assertTrue(provider.accountExists(USD, "alice"));
        assertEquals(10.25, provider.getMoney(USD, "alice"));
    }

    private void createProviderTables() {
        for (String currencyName : EconomyCurrency.names()) {
            String tableName = MYSQL_TABLE_PREFIX + currencyName;
            if (manager.isExistTable(tableName)) {
                manager.deleteTable(tableName);
            }
            manager.createTable(
                    tableName,
                    new TableType("player", DataType.getUUID(), true),
                    new TableType("money", DataType.getBIGINT(), false)
            );
        }
    }

    private enum EconomyCurrency {
        USD,
        EUR;

        static String[] names() {
            return new String[]{USD.name(), EUR.name()};
        }
    }
}
