package me.onebone.economyapi.command;

/*
 * EconomyAPI: Core of economy system for Nukkit
 * Copyright (C) 2016  onebone <jyc00410@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.lang.LangCode;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;
import static me.onebone.economyapi.EconomyAPI.serverLangCode;

public class TakeMoneyCommand extends PluginCommand<EconomyAPI> {
    private final EconomyAPI plugin;

    public TakeMoneyCommand(EconomyAPI plugin) {
        super("takemoney", plugin);

        this.setDescription("Takes money from player");
        this.setUsage("/takemoney <player> <amount>");
        this.setAliases(new String[]{"withdraw"});
        this.plugin = plugin;

        // command parameters
        commandParameters.clear();
        commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newType("player", false, CommandParamType.TARGET),
                CommandParameter.newType("amount", false, CommandParamType.FLOAT),
                CommandParameter.newEnum("currencyName", true, MAIN_CONFIG.getCurrencyList().toArray(new String[0]))
        });
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.plugin.isEnabled()) return false;
        LangCode langCode = sender instanceof Player ? ((Player) sender).getLanguageCode() : serverLangCode;
        if (!sender.hasPermission("economyapi.command.takemoney")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.getUsage()));
            return false;
        }
        String player = args[0];

        Player p = this.plugin.getServer().getPlayer(player);
        if (p != null) {
            player = p.getName();
        }

        double amount = 0;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0 || !Double.isFinite(amount)) {
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-invalid-number"));
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-must-be-number"));
            return false;
        }


        final String finalPlayer = player;
        final double finalAmount = amount;
        final String currencyName = args.length >= 3 ? args[2] : MAIN_CONFIG.getDefaultCurrency().getName();

        EconomyAPI.getAsyncOperator().reduceMoney(player, amount, currencyName).thenAccept(result -> {
            switch (result) {
                case EconomyAPI.RET_INVALID:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-player-lack-of-money", finalPlayer, EconomyAPI.MONEY_FORMAT.format(finalAmount), EconomyAPI.MONEY_FORMAT.format(this.plugin.myMoney(finalPlayer, currencyName)), plugin.getMonetaryUnit(currencyName)));
                    break;
                case EconomyAPI.RET_NO_ACCOUNT:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", finalPlayer));
                    break;
                case EconomyAPI.RET_CANCELLED:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-failed", finalPlayer));
                    break;
                case EconomyAPI.RET_SUCCESS:
                    sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-took-money", finalPlayer, EconomyAPI.MONEY_FORMAT.format(finalAmount), plugin.getMonetaryUnit(currencyName)));
                    if (p != null) {
                        p.sendMessage(EconomyAPI.getI18n().tr(p.getLanguageCode(), "takemoney-money-taken", EconomyAPI.MONEY_FORMAT.format(finalAmount), plugin.getMonetaryUnit(currencyName)));
                    }
                    break;
            }
        });
        return true;
    }

}
