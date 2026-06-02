package me.onebone.economyapi.event;

import me.onebone.economyapi.TestPluginSupport;
import me.onebone.economyapi.event.account.CreateAccountEvent;
import me.onebone.economyapi.event.money.AddMoneyEvent;
import me.onebone.economyapi.event.money.ReduceMoneyEvent;
import me.onebone.economyapi.event.money.SetMoneyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EconomyEventTest {
    @BeforeEach
    void setUp() {
        TestPluginSupport.installMainConfig();
    }

    @Test
    void createAccountEventUsesDefaultCurrencyAndAllowsDefaultMoneyChanges() {
        CreateAccountEvent event = new CreateAccountEvent("alice", 100);

        event.setDefaultMoney(125.5);

        assertEquals("alice", event.getPlayer());
        assertEquals(125.5, event.getDefaultMoney());
        assertEquals(TestPluginSupport.USD, event.getCurrencyName());
        assertSame(CreateAccountEvent.handlerList, CreateAccountEvent.getHandlers());
    }

    @Test
    void createAccountEventKeepsExplicitCurrency() {
        CreateAccountEvent event = new CreateAccountEvent("alice", 100, TestPluginSupport.EUR);

        assertEquals(TestPluginSupport.EUR, event.getCurrencyName());
    }

    @Test
    void addMoneyEventStoresMutableAmountAndCurrency() {
        AddMoneyEvent event = new AddMoneyEvent("alice", 10);

        event.setAmount(12.25);

        assertEquals("alice", event.getPlayer());
        assertEquals(12.25, event.getAmount());
        assertEquals(TestPluginSupport.USD, event.getCurrencyName());
        assertSame(AddMoneyEvent.handlerList, AddMoneyEvent.getHandlers());
        assertNotNull(new AddMoneyEvent("alice", 10, TestPluginSupport.EUR).getCurrencyName());
    }

    @Test
    void reduceMoneyEventStoresMutableAmountAndCurrency() {
        ReduceMoneyEvent event = new ReduceMoneyEvent("alice", 10);

        event.setAmount(8.75);

        assertEquals("alice", event.getPlayer());
        assertEquals(8.75, event.getAmount());
        assertEquals(TestPluginSupport.USD, event.getCurrencyName());
        assertSame(ReduceMoneyEvent.handlerList, ReduceMoneyEvent.getHandlers());
        assertEquals(TestPluginSupport.EUR, new ReduceMoneyEvent("alice", 10, TestPluginSupport.EUR).getCurrencyName());
    }

    @Test
    void setMoneyEventStoresMutableAmountAndCurrency() {
        SetMoneyEvent event = new SetMoneyEvent("alice", 10);

        event.setAmount(99.5);

        assertEquals("alice", event.getPlayer());
        assertEquals(99.5, event.getAmount());
        assertEquals(TestPluginSupport.USD, event.getCurrencyName());
        assertSame(SetMoneyEvent.handlerList, SetMoneyEvent.getHandlers());
        assertEquals(TestPluginSupport.EUR, new SetMoneyEvent("alice", 10, TestPluginSupport.EUR).getCurrencyName());
    }
}
