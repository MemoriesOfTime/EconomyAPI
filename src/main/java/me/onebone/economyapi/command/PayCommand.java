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
import cn.nukkit.lang.PluginI18n;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;
import me.onebone.economyapi.EconomyAPI;

import static me.onebone.economyapi.EconomyAPI.MAIN_CONFIG;
import static me.onebone.economyapi.EconomyAPI.serverLangCode;

public class PayCommand extends PluginCommand<EconomyAPI> {
    private final EconomyAPI plugin;

    public PayCommand(EconomyAPI plugin) {
        super("pay", plugin);

        this.setDescription("Pays to other player");
        this.setUsage("/pay <player> <amount>");
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
        if (!sender.hasPermission("economyapi.command.pay")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(new TranslationContainer("%commands.generic.ingame"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.getUsage()));
            return false;
        }
        String player = args[0];
        Player p = this.plugin.getServer().getPlayer(player);
        if (p != null) {
            if (sender == p) {
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "pay-failed-self"));
                return false;
            }

            player = p.getName();
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (!Double.isFinite(amount)) {
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-invalid-number"));
                return false;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "takemoney-must-be-number"));
            return false;
        }

        if (amount < 0.01) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "pay-too-low"));
            return false;
        }

        if (!this.plugin.hasAccount(player)) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", player));
            return false;
        }

        final String currencyName = args.length >= 3 ? args[2] : MAIN_CONFIG.getDefaultCurrency().getName();

        int result = this.plugin.reduceMoney((Player) sender, amount, currencyName);
        switch (result) {
            case EconomyAPI.RET_NO_ACCOUNT:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", player));
                break;
            case EconomyAPI.RET_CANCELLED:
            case EconomyAPI.RET_INVALID:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "pay-failed"));
                break;
            case EconomyAPI.RET_SUCCESS:
                this.plugin.addMoney(player, amount, currencyName, true);

                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "pay-success", EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit(currencyName), player));
                if (p != null) {
                    p.sendMessage(EconomyAPI.getI18n().tr(langCode, "money-paid", sender.getName(), EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit(currencyName)));
                }
                break;
        }
        return true;
    }

}
