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

public class SetMoneyCommand extends PluginCommand<EconomyAPI> {
    private final EconomyAPI plugin;

    public SetMoneyCommand(EconomyAPI plugin) {
        super("setmoney", plugin);

        this.setDescription("Sets player's balance");
        this.setUsage("/setmoney <player> <amount> [currencyName]");
        this.setAliases(new String[]{"setbal", "setbalance"});
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
        if (!sender.hasPermission("economyapi.command.setmoney")) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.getUsage()));
            return true;
        }
        String player = args[0];

        Player p = this.plugin.getServer().getPlayer(player);
        if (p != null) {
            player = p.getName();
        }
        double amount = 0;
        try {
            amount = Double.parseDouble(args[1]);
            if (!Double.isFinite(amount)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "setmoney-invalid-number", amount, plugin.getMonetaryUnit()));
            return true;
        }

        String currencyName = MAIN_CONFIG.getDefaultCurrency().getName();
        if (args.length >= 3) {
            currencyName = args[2];
        }
        int result = this.plugin.setMoney(player, amount, currencyName);
        switch (result) {
            case EconomyAPI.RET_NO_ACCOUNT:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "player-never-connected", player));
                return true;
            case EconomyAPI.RET_CANCELLED:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "setmoney-failed"));
                return true;
            case EconomyAPI.RET_INVALID:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "reached-max", EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit(currencyName)));
                return true;
            case EconomyAPI.RET_SUCCESS:
                sender.sendMessage(EconomyAPI.getI18n().tr(langCode, "setmoney-setmoney", player, EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit(currencyName)));
                if (p != null) {
                    p.sendMessage(EconomyAPI.getI18n().tr(p.getLanguageCode(), "setmoney-set", EconomyAPI.MONEY_FORMAT.format(amount), plugin.getMonetaryUnit(currencyName)));
                }
                return true;
        }
        return true;
    }

}
