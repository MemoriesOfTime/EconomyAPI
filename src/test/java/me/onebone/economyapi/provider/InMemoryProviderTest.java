package me.onebone.economyapi.provider;

import org.junit.jupiter.api.Test;

import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryProviderTest implements ProviderContractTest {
    @Override
    public Provider provider() {
        return new InMemoryProvider();
    }

    @Test
    void defaultTransferRollsBackSourceWhenTargetMutationFails() {
        InMemoryProvider provider = new InMemoryProvider();
        provider.createAccount(USD, "alice", 10);
        provider.createAccount(USD, "bob", 20);
        provider.failNextAdd();

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 7, 100);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void defaultTransferThrowsWhenRollbackFailsAfterTargetMutationFails() {
        InMemoryProvider provider = new InMemoryProvider();
        provider.createAccount(USD, "alice", 10);
        provider.createAccount(USD, "bob", 20);
        provider.failNextAdds(2);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.transferMoneyChecked(USD, "alice", "bob", 7, 100));

        assertEquals("Failed to roll back transfer from alice to bob in provider InMemory", exception.getMessage());
        assertEquals(3, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }
}
