package me.onebone.economyapi;

import cn.nukkit.IPlayer;
import cn.nukkit.event.Event;
import me.onebone.economyapi.event.money.AddMoneyEvent;
import me.onebone.economyapi.event.money.ReduceMoneyEvent;
import me.onebone.economyapi.provider.InMemoryProvider;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyAPITransferTest {
    private static final String USD = "USD";

    @Test
    void transferRejectsSameAccountWithoutMutatingBalance() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });

        EconomyAPI.TransferResult result = api.transfer("alice", "alice", 100);

        assertEquals(EconomyAPI.RET_INVALID, result.result());
        assertEquals(100, result.amount());
        assertEquals(200, provider.getMoney(USD, "alice"));
    }

    @Test
    void transferRejectsNegativeAndNonFiniteAmounts() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });

        assertEquals(EconomyAPI.RET_INVALID, api.transfer("alice", "bob", -1).result());
        assertEquals(EconomyAPI.RET_INVALID, api.transfer("alice", "bob", Double.POSITIVE_INFINITY).result());
        assertEquals(EconomyAPI.RET_INVALID, api.transfer("alice", "bob", Double.NaN).result());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferReturnsCancelledWhenReduceEventIsCancelled() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setCancelled(), event -> {
        });

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_CANCELLED, result.result());
        assertEquals(100, result.amount());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferReturnsCancelledWhenAddEventIsCancelled() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setAmount(90), event -> event.setCancelled());

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_CANCELLED, result.result());
        assertEquals(90, result.amount());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferCanForceCancelledEvents() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setCancelled(), event -> event.setCancelled());

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100, true, true);

        assertEquals(EconomyAPI.RET_SUCCESS, result.result());
        assertEquals(100, result.amount());
        assertEquals(100, provider.getMoney(USD, "alice"));
        assertEquals(120, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferRejectsInvalidReduceEventAmountWithoutMutatingBalances() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setAmount(Double.NEGATIVE_INFINITY), event -> {
        });

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_INVALID, result.result());
        assertEquals(100, result.amount());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferRejectsInvalidAddEventAmountWithoutMutatingBalances() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setAmount(90), event -> event.setAmount(-1));

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_INVALID, result.result());
        assertEquals(-1, result.amount());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferRejectsAddEventAmountChangesWithoutMutatingBalances() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> event.setAmount(110));

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_INVALID, result.result());
        assertEquals(200, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferUsesReduceEventAmountWhenAddEventKeepsAmount() {
        InMemoryProvider provider = providerWithAccounts(200, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> event.setAmount(90), event -> {
        });

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 100);

        assertEquals(EconomyAPI.RET_SUCCESS, result.result());
        assertEquals(90, result.amount());
        assertEquals(110, provider.getMoney(USD, "alice"));
        assertEquals(110, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferReturnsProviderFailureWithoutPartialMutation() {
        InMemoryProvider provider = providerWithAccounts(4, 20);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });

        EconomyAPI.TransferResult result = api.transfer("alice", "bob", 5);

        assertEquals(EconomyAPI.RET_INVALID, result.result());
        assertEquals(5, result.amount());
        assertEquals(4, provider.getMoney(USD, "alice"));
        assertEquals(20, provider.getMoney(USD, "bob"));
    }

    @Test
    void transferResultRecordExposesValueSemantics() {
        EconomyAPI.TransferResult result = new EconomyAPI.TransferResult(EconomyAPI.RET_SUCCESS, 12.5);

        assertEquals(EconomyAPI.RET_SUCCESS, result.result());
        assertEquals(12.5, result.amount());
        assertEquals(new EconomyAPI.TransferResult(EconomyAPI.RET_SUCCESS, 12.5), result);
        assertEquals(new EconomyAPI.TransferResult(EconomyAPI.RET_SUCCESS, 12.5).hashCode(), result.hashCode());
        assertTrue(result.toString().contains("amount=12.5"));
    }

    @Test
    void saveAllDelegatesOnlyWhenProviderExists() {
        TestEconomyAPI api = new TestEconomyAPI(null, event -> {
        }, event -> {
        });

        api.saveAll();

        CountingProvider provider = new CountingProvider();
        api.provider = provider;
        api.saveAll();

        assertEquals(1, provider.saveCount);
    }

    @Test
    void onDisableSavesAndClosesProviderWhenPresent() {
        TestEconomyAPI api = new TestEconomyAPI(null, event -> {
        }, event -> {
        });
        CountingProvider provider = new CountingProvider();
        api.provider = provider;

        api.onDisable();

        assertEquals(1, provider.saveCount);
        assertEquals(1, provider.closeCount);
    }

    @Test
    void hasAccountRejectsNullIPlayer() {
        TestEconomyAPI api = new TestEconomyAPI(new InMemoryProvider(), event -> {
        }, event -> {
        });

        assertFalse(api.hasAccount((cn.nukkit.IPlayer) null));
        assertFalse(api.hasAccount((cn.nukkit.IPlayer) null, USD));
    }

    @Test
    void providerAccessorsExposeConfiguredProviderAndAsyncOperatorIsStable() {
        InMemoryProvider provider = new InMemoryProvider();
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });

        assertSame(provider, api.getProvider());
        assertTrue(api.addProvider("memory", InMemoryProvider.class));
        assertTrue(api.providerClass.containsKey("memory"));
        assertSame(EconomyAPI.getAsyncOperator(), EconomyAPI.getAsyncOperator());
    }

    @Test
    void currencyAccessorsReadMainConfig() {
        TestPluginSupport.installMainConfig();
        EconomyAPI api = new EconomyAPI();

        assertEquals("$", api.getMonetaryUnit());
        assertEquals(1000.0, api.getDefaultMoney());
        assertEquals(9999999999.0, api.getMaxMoney());
        assertEquals("EUR", api.getMonetaryUnit("EUR"));
        assertEquals(500.0, api.getDefaultMoney("EUR"));
        assertEquals(5000000000.0, api.getMaxMoney("EUR"));
    }

    @Test
    void directProviderReadMethodsUseProvider() {
        InMemoryProvider provider = providerWithAccounts(10, 20);
        provider.createAccount("EUR", "alice", 30);
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });

        LinkedHashMap<String, Double> defaultBalances = api.getAllMoney();
        LinkedHashMap<String, Double> eurBalances = api.getAllMoney("EUR");

        assertEquals(10, defaultBalances.get("alice"));
        assertEquals(30, eurBalances.get("alice"));
    }

    @Test
    void forcedLegacyMigrationRetriesWhenUuidWasCachedBeforeNameWasKnown() {
        TestPluginSupport.installMainConfig();
        InMemoryProvider provider = new InMemoryProvider();
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });
        UUID uuid = UUID.randomUUID();
        String uuidId = uuid.toString().toLowerCase();
        provider.createAccount(USD, "alice", 75);
        api.migratedPlayers.put(uuid, Boolean.TRUE);

        api.checkAndConvertLegacy(uuid, "Alice", true);

        assertFalse(provider.accountExists(USD, "alice"));
        assertTrue(provider.accountExists(USD, uuidId));
        assertEquals(75, provider.getMoney(USD, uuidId));
    }

    @Test
    void forcedLegacyMigrationKeepsLegacyBalanceWhenUuidAccountAlreadyExists() {
        TestPluginSupport.installMainConfig();
        InMemoryProvider provider = new InMemoryProvider();
        TestEconomyAPI api = new TestEconomyAPI(provider, event -> {
        }, event -> {
        });
        UUID uuid = UUID.randomUUID();
        String uuidId = uuid.toString().toLowerCase();
        provider.createAccount(USD, "alice", 75);
        provider.createAccount(USD, uuidId, 1000);
        api.migratedPlayers.put(uuid, Boolean.TRUE);

        api.checkAndConvertLegacy(uuid, "Alice", true);

        assertFalse(provider.accountExists(USD, "alice"));
        assertEquals(75, provider.getMoney(USD, uuidId));
    }

    @Test
    void uuidLegacyCheckDoesNotCacheWhenOfflineNameIsUnknown() {
        TestEconomyAPI api = new TestEconomyAPI(new InMemoryProvider(), event -> {
        }, event -> {
        }) {
            @Override
            IPlayer getOfflinePlayer(UUID uuid) {
                return null;
            }
        };
        UUID uuid = UUID.randomUUID();

        api.checkAndConvertLegacy(uuid);

        assertFalse(api.migratedPlayers.containsKey(uuid));
    }

    private InMemoryProvider providerWithAccounts(double aliceMoney, double bobMoney) {
        InMemoryProvider provider = new InMemoryProvider();
        provider.createAccount(USD, "alice", aliceMoney);
        provider.createAccount(USD, "bob", bobMoney);
        return provider;
    }

    private static class TestEconomyAPI extends EconomyAPI {
        private final Consumer<ReduceMoneyEvent> reduceListener;
        private final Consumer<AddMoneyEvent> addListener;

        private TestEconomyAPI(InMemoryProvider provider, Consumer<ReduceMoneyEvent> reduceListener, Consumer<AddMoneyEvent> addListener) {
            this.provider = provider;
            this.reduceListener = reduceListener;
            this.addListener = addListener;
        }

        private EconomyAPI.TransferResult transfer(String fromId, String toId, double amount) {
            return transferMoneyInternal(fromId, toId, amount, USD, false, false);
        }

        private EconomyAPI.TransferResult transfer(String fromId, String toId, double amount, boolean forceReduce, boolean forceAdd) {
            return transferMoneyInternal(fromId, toId, amount, USD, forceReduce, forceAdd);
        }

        @Override
        public double getMaxMoney(String currencyName) {
            return 1_000_000;
        }

        @Override
        void callMoneyEvent(Event event) {
            if (event instanceof ReduceMoneyEvent reduceMoneyEvent) {
                reduceListener.accept(reduceMoneyEvent);
            }
            if (event instanceof AddMoneyEvent addMoneyEvent) {
                addListener.accept(addMoneyEvent);
            }
        }
    }

    private static class CountingProvider extends InMemoryProvider {
        private int saveCount;
        private int closeCount;

        @Override
        public void save() {
            saveCount++;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }
}
