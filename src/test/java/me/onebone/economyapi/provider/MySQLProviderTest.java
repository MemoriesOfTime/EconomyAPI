package me.onebone.economyapi.provider;

import me.onebone.economyapi.TestPluginSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MySQLProviderTest {
    private MySQLProvider provider;

    @BeforeEach
    void setUp() {
        TestPluginSupport.installMainConfig();
        MySQLProvider.useManagerForTesting(null, "");
        provider = new MySQLProvider();
    }

    @Test
    void operationsReportMissingWhenManagerIsNotReady() {
        provider.open();
        provider.save();

        assertFalse(provider.accountExists(USD, "alice"));
        assertFalse(provider.createAccount(USD, "alice", 10));
        assertFalse(provider.setMoney(USD, "alice", 10));
        assertFalse(provider.addMoney(USD, "alice", 10));
        assertFalse(provider.reduceMoney(USD, "alice", 10));
        assertFalse(provider.removeAccount(USD, "alice"));
        assertEquals(-1, provider.getMoney(USD, "alice"));
        assertTrue(provider.getAll(USD).isEmpty());
        assertEquals("MySQL", provider.getName());

        provider.close();
    }

    @Test
    void defaultCurrencyOverloadsReportMissingWhenManagerIsNotReady() {
        assertFalse(provider.accountExists("alice"));
        assertFalse(provider.createAccount("alice", 10));
        assertFalse(provider.setMoney("alice", 10));
        assertFalse(provider.addMoney("alice", 10));
        assertFalse(provider.reduceMoney("alice", 10));
        assertFalse(provider.removeAccount("alice"));
        assertEquals(-1, provider.getMoney("alice"));
        assertTrue(provider.getAll().isEmpty());
    }

    @Test
    void checkedOperationsReportNoAccountWhenManagerIsNotReady() {
        assertEquals(Provider.RET_NO_ACCOUNT, provider.setMoneyChecked(USD, "alice", 10, 100));
        assertEquals(Provider.RET_NO_ACCOUNT, provider.addMoneyChecked(USD, "alice", 10, 100));
        assertEquals(Provider.RET_NO_ACCOUNT, provider.reduceMoneyChecked(USD, "alice", 10));
        assertEquals(Provider.RET_NO_ACCOUNT, provider.transferMoneyChecked(USD, "alice", "bob", 10, 100));
    }

    @Test
    void transferRejectsInvalidInputBeforeCheckingManagerReadiness() {
        assertEquals(Provider.RET_INVALID, provider.transferMoneyChecked(USD, "alice", "alice", 10, 100));
        assertEquals(Provider.RET_INVALID, provider.transferMoneyChecked(USD, "alice", "bob", -1, 100));
        assertEquals(Provider.RET_INVALID, provider.transferMoneyChecked(USD, "alice", "bob", Double.NaN, 100));
    }
}
