package me.onebone.economyapi.command;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.lang.LangCode;
import cn.nukkit.utils.Config;
import me.onebone.economyapi.EconomyAPI;
import me.onebone.economyapi.TestPluginSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandBehaviorTest {
    private EconomyAPI plugin;

    @BeforeEach
    void setUp() {
        TestPluginSupport.installMainConfig();
        plugin = TestPluginSupport.installPlugin(new Config(Config.YAML), null);
        EconomyAPI.serverLangCode = LangCode.en_US;
    }

    @Test
    void constructorsRegisterCommandMetadata() {
        for (PluginCommand<EconomyAPI> command : commands()) {
            assertSame(plugin, command.getPlugin());
            assertNotNull(command.getDescription());
            assertFalse(command.getDescription().isBlank());
            assertNotNull(command.getUsage());
            assertFalse(command.getUsage().isBlank());
            assertTrue(command.getCommandParameters().containsKey("default"));
        }
    }

    @Test
    void disabledCommandsReturnFalseBeforeUsingSender() {
        for (PluginCommand<EconomyAPI> command : commands()) {
            assertFalse(command.execute(null, command.getName(), new String[0]));
        }
    }

    @Test
    void permissionDeniedCommandsSendPermissionMessage() {
        TestPluginSupport.setPluginEnabled(plugin, true);

        for (PluginCommand<EconomyAPI> command : permissionCheckedCommands()) {
            RecordingSender sender = new RecordingSender(false);

            assertFalse(command.execute(sender.proxy(), command.getName(), new String[0]));
            assertEquals(1, sender.messages.size());
        }
    }

    @Test
    void usageAndConsoleOnlyBranchesSendMessagesWithoutServerRuntime() {
        TestPluginSupport.setPluginEnabled(plugin, true);
        RecordingSender sender = new RecordingSender(true);

        assertTrue(new SetLangCommand(plugin).execute(sender.proxy(), "setlang", new String[0]));
        assertTrue(new MyMoneyCommand(plugin).execute(sender.proxy(), "mymoney", new String[0]));
        assertFalse(new GiveMoneyCommand(plugin).execute(sender.proxy(), "givemoney", new String[]{"alice"}));
        assertFalse(new TakeMoneyCommand(plugin).execute(sender.proxy(), "takemoney", new String[]{"alice"}));
        assertFalse(new SetMoneyCommand(plugin).execute(sender.proxy(), "setmoney", new String[]{"alice"}));
        assertFalse(new TopMoneyCommand(plugin).execute(sender.proxy(), "topmoney", new String[]{"not-a-page"}));
        assertFalse(new PayCommand(plugin).execute(sender.proxy(), "pay", new String[]{"alice", "1"}));

        assertEquals(7, sender.messages.size());
    }

    private List<PluginCommand<EconomyAPI>> commands() {
        return List.of(
                new MyMoneyCommand(plugin),
                new TopMoneyCommand(plugin),
                new GiveMoneyCommand(plugin),
                new TakeMoneyCommand(plugin),
                new PayCommand(plugin),
                new SetMoneyCommand(plugin),
                new SetLangCommand(plugin),
                new MigrateDataCommand(plugin)
        );
    }

    private List<PluginCommand<EconomyAPI>> permissionCheckedCommands() {
        return List.of(
                new MyMoneyCommand(plugin),
                new TopMoneyCommand(plugin),
                new GiveMoneyCommand(plugin),
                new TakeMoneyCommand(plugin),
                new PayCommand(plugin),
                new SetMoneyCommand(plugin),
                new SetLangCommand(plugin)
        );
    }

    private static final class RecordingSender {
        private final boolean hasPermission;
        private final List<Object> messages = new ArrayList<>();

        private RecordingSender(boolean hasPermission) {
            this.hasPermission = hasPermission;
        }

        private CommandSender proxy() {
            return (CommandSender) Proxy.newProxyInstance(
                    CommandSender.class.getClassLoader(),
                    new Class[]{CommandSender.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "sendMessage" -> {
                            messages.add(args[0]);
                            yield null;
                        }
                        case "sendCommandOutput" -> null;
                        case "hasPermission", "isPermissionSet" -> hasPermission;
                        case "addAttachment" -> null;
                        case "removeAttachment", "recalculatePermissions", "setOp" -> null;
                        case "getEffectivePermissions" -> Map.of();
                        case "isOp" -> hasPermission;
                        case "getServer" -> null;
                        case "getName" -> "Console";
                        case "isPlayer", "isEntity" -> false;
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> "RecordingSender";
                        default -> throw new UnsupportedOperationException("Unsupported CommandSender method: " + method.getName());
                    }
            );
        }
    }
}
