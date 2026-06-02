package me.onebone.economyapi.provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteProviderTest implements ProviderContractTest {
    @TempDir
    Path dataFolder;
    private SQLiteProvider provider;

    @BeforeEach
    void setUp() {
        ProviderTestSupport.installMainConfig();
        provider = new SQLiteProvider();
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
    void initCreatesDatabaseFile() {
        assertTrue(dataFolder.resolve("MoneyV3.db").toFile().isFile());
    }

    @Test
    void accountChangesPersistAfterCloseAndReopen() {
        provider.createAccount(USD, "alice", 10);
        provider.addMoneyChecked(USD, "alice", 5, 100);
        provider.close();

        SQLiteProvider reopened = new SQLiteProvider();
        reopened.init(dataFolder.toFile());

        assertTrue(reopened.accountExists(USD, "alice"));
        assertEquals(15, reopened.getMoney(USD, "alice"));

        reopened.close();
    }

    @Test
    void moneyDataStoresDatabaseFields() {
        SQLiteProvider.MoneyData data = new SQLiteProvider.MoneyData();

        data.setId(42);
        data.player = "alice";
        data.setMoney(12.5);
        data.setCurrency(USD);

        assertEquals(42, data.getId());
        assertEquals("alice", data.getPlayer());
        assertEquals(12.5, data.getMoney());
        assertEquals(USD, data.getCurrency());
    }

    @Test
    void moneyDataConstructorStoresAllFields() {
        SQLiteProvider.MoneyData data = new SQLiteProvider.MoneyData(42, "alice", 12.5, USD);

        assertEquals(42, data.getId());
        assertEquals("alice", data.getPlayer());
        assertEquals(12.5, data.getMoney());
        assertEquals(USD, data.getCurrency());
    }
}
