package me.onebone.economyapi.provider;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static me.onebone.economyapi.provider.ProviderTestSupport.EUR;
import static me.onebone.economyapi.provider.ProviderTestSupport.USD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

interface ProviderContractTest {
    Provider provider();

    @Test
    default void createAccountStoresBalance() {
        Provider provider = provider();

        assertTrue(provider.createAccount(USD, "alice", 10));

        assertTrue(provider.accountExists(USD, "alice"));
        assertEquals(10, provider.getMoney(USD, "alice"));
    }

    @Test
    default void defaultCurrencyOverloadsOperateOnDefaultCurrencyAccount() {
        Provider provider = provider();

        assertTrue(provider.createAccount("alice", 10));
        assertTrue(provider.accountExists("alice"));
        assertEquals(10, provider.getMoney("alice"));

        assertTrue(provider.addMoney("alice", 5));
        assertEquals(15, provider.getMoney("alice"));
        assertTrue(provider.setMoney("alice", 12));
        assertTrue(provider.reduceMoney("alice", 2));
        assertEquals(10, provider.getMoney("alice"));
        assertEquals(10, provider.getAll().get("alice"));
        assertTrue(provider.removeAccount("alice"));
        assertFalse(provider.accountExists("alice"));
    }

    @Test
    default void providerNameIsPresent() {
        assertFalse(provider().getName().isBlank());
    }

    @Test
    default void createAccountRejectsDuplicateAccountWithoutChangingBalance() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);

        assertFalse(provider.createAccount(USD, "alice", 20));

        assertEquals(10, provider.getMoney(USD, "alice"));
    }

    @Test
    default void accountsAreIsolatedByCurrency() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);
        provider.createAccount(EUR, "alice", 20);

        assertTrue(provider.setMoney(USD, "alice", 30));

        assertEquals(30, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(EUR, "alice"));
    }

    @Test
    default void getAllReturnsOnlyRequestedCurrencyBalances() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);
        provider.createAccount(USD, "bob", 20);
        provider.createAccount(EUR, "carol", 30);

        LinkedHashMap<String, Double> usdBalances = provider.getAll(USD);

        assertEquals(2, usdBalances.size());
        assertEquals(10, usdBalances.get("alice"));
        assertEquals(20, usdBalances.get("bob"));
        assertFalse(usdBalances.containsKey("carol"));
    }

    @Test
    default void removeAccountDeletesOnlyRequestedCurrencyAccount() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);
        provider.createAccount(EUR, "alice", 20);

        assertTrue(provider.removeAccount(USD, "alice"));

        assertFalse(provider.accountExists(USD, "alice"));
        assertTrue(provider.accountExists(EUR, "alice"));
    }

    @Test
    default void setMoneyCheckedRejectsMissingAccount() {
        Provider provider = provider();

        int result = provider.setMoneyChecked(USD, "missing", 10, 100);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
    }

    @Test
    default void setMoneyCheckedRejectsAmountAboveMaximumWithoutMutatingBalance() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);

        int result = provider.setMoneyChecked(USD, "alice", 101, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
    }

    @Test
    default void setMoneyCheckedStoresAmountWithinMaximum() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);

        int result = provider.setMoneyChecked(USD, "alice", 100, 100);

        assertEquals(Provider.RET_SUCCESS, result);
        assertEquals(100, provider.getMoney(USD, "alice"));
    }

    @Test
    default void addMoneyCheckedRejectsMissingAccount() {
        Provider provider = provider();

        int result = provider.addMoneyChecked(USD, "missing", 10, 100);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
    }

    @Test
    default void addMoneyCheckedRejectsAmountThatWouldExceedMaximumWithoutMutatingBalance() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 95);

        int result = provider.addMoneyChecked(USD, "alice", 6, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(95, provider.getMoney(USD, "alice"));
    }

    @Test
    default void addMoneyCheckedAddsAmountWithinMaximum() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 95);

        int result = provider.addMoneyChecked(USD, "alice", 5, 100);

        assertEquals(Provider.RET_SUCCESS, result);
        assertEquals(100, provider.getMoney(USD, "alice"));
    }

    @Test
    default void reduceMoneyCheckedRejectsMissingAccount() {
        Provider provider = provider();

        int result = provider.reduceMoneyChecked(USD, "missing", 10);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
    }

    @Test
    default void reduceMoneyCheckedRejectsAmountThatWouldMakeBalanceNegativeWithoutMutatingBalance() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 4);

        int result = provider.reduceMoneyChecked(USD, "alice", 5);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(4, provider.getMoney(USD, "alice"));
    }

    @Test
    default void reduceMoneyCheckedSubtractsAmountWithinBalance() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);

        int result = provider.reduceMoneyChecked(USD, "alice", 4);

        assertEquals(Provider.RET_SUCCESS, result);
        assertEquals(6, provider.getMoney(USD, "alice"));
    }

    @Test
    default void transferMoneyCheckedRejectsNonFiniteAmounts() {
        Provider provider = providerWithAccounts(10, 10);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", Double.NaN, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
        assertEquals(10, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedRejectsNegativeAmounts() {
        Provider provider = providerWithAccounts(10, 10);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", -1, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
        assertEquals(10, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedRejectsSameAccount() {
        Provider provider = providerWithAccounts(10, 10);

        int result = provider.transferMoneyChecked(USD, "alice", "alice", 1, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
    }

    @Test
    default void transferMoneyCheckedRejectsMissingSourceAccount() {
        Provider provider = provider();
        provider.createAccount(USD, "bob", 10);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 1, 100);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
        assertEquals(10, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedRejectsMissingTargetAccount() {
        Provider provider = provider();
        provider.createAccount(USD, "alice", 10);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 1, 100);

        assertEquals(Provider.RET_NO_ACCOUNT, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
    }

    @Test
    default void transferMoneyCheckedRejectsInsufficientFundsWithoutMutatingBalances() {
        Provider provider = providerWithAccounts(4, 10);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 5, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(4, provider.getMoney(USD, "alice"));
        assertEquals(10, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedRejectsTargetOverflowWithoutMutatingBalances() {
        Provider provider = providerWithAccounts(10, 98);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 3, 100);

        assertEquals(Provider.RET_INVALID, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
        assertEquals(98, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedMovesMoneyBetweenAccounts() {
        Provider provider = providerWithAccounts(10, 20);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 7, 100);

        assertEquals(Provider.RET_SUCCESS, result);
        assertEquals(3, provider.getMoney(USD, "alice"));
        assertEquals(27, provider.getMoney(USD, "bob"));
    }

    @Test
    default void transferMoneyCheckedAllowsZeroAmountWithoutChangingBalances() {
        Provider provider = providerWithAccounts(10, 20);

        int result = provider.transferMoneyChecked(USD, "alice", "bob", 0, 100);

        assertEquals(Provider.RET_SUCCESS, result);
        assertEquals(10, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    private Provider providerWithAccounts(double aliceMoney, double bobMoney) {
        Provider provider = provider();
        provider.createAccount(USD, "alice", aliceMoney);
        provider.createAccount(USD, "bob", bobMoney);
        return provider;
    }
}
