package me.onebone.economyapi.provider;

import cn.nukkit.utils.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;

import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlProviderTest implements ProviderContractTest {
    @TempDir
    Path dataFolder;
    private YamlProvider provider;

    @BeforeEach
    void setUp() {
        ProviderTestSupport.installMainConfig();
        provider = new YamlProvider();
        provider.init(dataFolder.toFile());
    }

    @Override
    public Provider provider() {
        return provider;
    }

    @AfterEach
    void tearDown() {
        provider.close();
    }

    @Test
    void initCreatesMoneyFilesForConfiguredCurrencies() {
        assertTrue(dataFolder.resolve("money").resolve("USD.yml").toFile().isFile());
        assertTrue(dataFolder.resolve("money").resolve("EUR.yml").toFile().isFile());
        assertEquals(3, new Config(dataFolder.resolve("money").resolve("USD.yml").toFile(), Config.YAML).getInt("version"));
        assertEquals(3, new Config(dataFolder.resolve("money").resolve("EUR.yml").toFile(), Config.YAML).getInt("version"));
    }

    @Test
    void initNormalizesExistingNumericValuesAndExposesAllBalances() {
        provider.close();
        Config file = new Config(dataFolder.resolve("money").resolve("USD.yml").toFile(), Config.YAML);
        file.set("money.integer", 1);
        file.set("money.double", 2.5);
        file.set("money.string", "3.75");
        file.save();

        provider = new YamlProvider();
        provider.init(dataFolder.toFile());

        LinkedHashMap<String, Double> balances = provider.getAll(USD);
        assertEquals(1.0, balances.get("integer"));
        assertEquals(2.5, balances.get("double"));
        assertEquals(3.75, balances.get("string"));
    }

    @Test
    void accountChangesPersistAfterSaveAndReopen() {
        assertTrue(provider.createAccount(USD, "alice", 10));
        assertEquals(Provider.RET_SUCCESS, provider.addMoneyChecked(USD, "alice", 5, 100));
        provider.save();
        provider.close();

        YamlProvider reopened = new YamlProvider();
        reopened.init(dataFolder.toFile());

        assertTrue(reopened.accountExists(USD, "alice"));
        assertEquals(15, reopened.getMoney(USD, "alice"));
        reopened.close();
    }
}
