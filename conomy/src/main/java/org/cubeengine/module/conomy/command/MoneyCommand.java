/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cubeengine.module.conomy.command;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.*;
import org.cubeengine.module.conomy.BaseAccount;
import org.cubeengine.module.conomy.ConfigCurrency;
import org.cubeengine.module.conomy.Conomy;
import org.cubeengine.module.conomy.ConomyService;
import org.cubeengine.module.conomy.storage.BalanceModel;
import org.cubeengine.service.command.ContainerCommand;
import org.cubeengine.service.command.annotation.ParameterPermission;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserList;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransferResult;
import org.spongepowered.api.service.pagination.PaginationBuilder;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;
import static org.spongepowered.api.text.format.TextColors.GOLD;
import static org.spongepowered.api.text.format.TextColors.WHITE;

@Command(name = "money", desc = "Manage your money")
public class MoneyCommand extends ContainerCommand
{
    // TODO add in context to commands
    private final Conomy module;
    private ConomyService service;
    private I18n i18n;

    public MoneyCommand(Conomy module, ConomyService conomy, I18n i18n)
    {
        super(module);
        this.module = module;
        this.service = conomy;
        this.i18n = i18n;
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        return this.getCommand("balance").execute(invocation);
    }

    private BaseAccount.Unique getUserAccount(User user)
    {
        return service.createAccount(user.getUniqueId())
                .filter(a -> a instanceof BaseAccount.Unique)
                .map(BaseAccount.Unique.class::cast).orElse(null);
    }

    @Alias(value = {"balance", "moneybalance", "pmoney"})
    @Command(desc = "Shows your balance")
    public void balance(CommandSource context,
                        @Default User player,
                        @ParameterPermission @Flag(longName = "showhidden", name = "f") boolean showHidden)
    // TODO message when no user found "If you are out of money, better go work than typing silly commands in the console."
    {
        BaseAccount.Unique account = this.getUserAccount(player);
        if (account == null ||  (account.isHidden()
                                    && !showHidden
                                    && !account.getIdentifier().equals(context.getIdentifier())))
        {
            i18n.sendTranslated(context, NEGATIVE, "No account found for {user}!", player);
            return;
        }

        Map<Currency, BigDecimal> balances = account.getBalances();
        if (balances.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "No Balance for {user} found!", player);
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "{user}'s Balance:", player);
        for (Map.Entry<Currency, BigDecimal> entry : balances.entrySet())
        {
            context.sendMessage(Text.of(" - ", GOLD, entry.getKey().format(entry.getValue())));
        }
    }

    @Alias(value = {"toplist", "balancetop", "topmoney"})
    @Command(desc = "Shows the players with the highest balance.")
    public void top(CommandSource context,
                    @Optional @Label("[fromRank-]toRank") String range,
                    @ParameterPermission @Flag(longName = "showhidden", name = "f") boolean showHidden)
    {
        int fromRank = 1;
        int toRank = 10;
        if (range != null)
        {
            try
            {
                if (range.contains("-"))
                {
                    fromRank = Integer.parseInt(range.substring(0, range.indexOf("-")));
                    range = range.substring(range.indexOf("-") + 1);
                }
                toRank = Integer.parseInt(range);
            }
            catch (NumberFormatException e)
            {
                i18n.sendTranslated(context, NEGATIVE, "Invalid rank!");
                return;
            }
        }
        Collection<BalanceModel> models = this.service.getTopBalance(true, false, fromRank, toRank, showHidden);
        int i = fromRank;


        PaginationBuilder pagination = Sponge.getServiceManager().provideUnchecked(PaginationService.class).builder();
        pagination.paddingString("-");
        if (fromRank == 1)
        {
            pagination.title(i18n.getTranslation(context, POSITIVE, "Top Balance ({amount})", models.size()));
        }
        else
        {
            pagination.title(i18n.getTranslation(context, POSITIVE, "Top Balance from {integer} to {integer}",
                    fromRank, fromRank + models.size() - 1));
        }
        List<Text> texts = new ArrayList<>();
        for (BalanceModel balance : models)
        {
            Account account = service.getAccount(balance.getAccountID()).get();
            ConfigCurrency currency = service.getCurrency(balance.getCurrency());
            texts.add(Text.of(i++, WHITE, " - ",
                    DARK_GREEN, account.getDisplayName(), WHITE, ": ",
                    GOLD, currency.format(currency.fromLong(balance.getBalance()))));
        }
        pagination.contents(texts);
        pagination.sendTo(context);
    }

    @Alias(value = "pay")
    @Command(alias = "give", desc = "Transfer the given amount to another account.")
    public void pay(CommandSource context, @Label("*|<players>") UserList users, Double amount, @Named("as") User player)
    {
        if (amount < 0)
        {
            i18n.sendTranslated(context, NEGATIVE, "What are you trying to do?");
            return;
        }

        boolean asSomeOneElse = false;
        if (player != null)
        {
            if (!context.hasPermission(module.perms().COMMAND_PAY_ASOTHER.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to pay money as someone else!");
                return;
            }
            asSomeOneElse = true;
        }
        else
        {
            if (!(context instanceof User))
            {
                i18n.sendTranslated(context, NEGATIVE, "Please specify a player to use their account.");
                return;
            }
            player = (User)context;
        }
        BaseAccount.Unique source = getUserAccount(player);
        if (source == null)
        {
            if (asSomeOneElse)
            {
                i18n.sendTranslated(context, NEGATIVE, "{user} does not have an account!", player);
            }
            else
            {
                i18n.sendTranslated(context, NEGATIVE, "You do not have an account!");
            }
            return;
        }

        for (User user : users.list())
        {
            Account target = getUserAccount(user);
            if (target == null)
            {
                i18n.sendTranslated(context, NEGATIVE, "{user} does not have an account!", user);
                continue;
            }
            Currency cur = service.getDefaultCurrency();
            TransferResult result = source.transfer(target, cur, new BigDecimal(amount), causeOf(context));
            Text formatAmount = cur.format(result.getAmount());
            switch (result.getResult())
            {
                case SUCCESS:
                    if (asSomeOneElse)
                    {
                        i18n.sendTranslated(context, POSITIVE, "{txt#amount} transferred from {user}'s to {user}'s account!", formatAmount, player, user);
                    }
                    else
                    {
                        i18n.sendTranslated(context, POSITIVE, "{txt#amount} transferred to {user}'s account!", formatAmount, user);
                    }
                    if (user.isOnline())
                    {
                        i18n.sendTranslated(user.getPlayer().get(), POSITIVE, "{user} just paid you {txt#amount}!", player, formatAmount);
                    }
                    break;
                case ACCOUNT_NO_FUNDS:
                    if (asSomeOneElse)
                    {
                        i18n.sendTranslated(context, NEGATIVE, "{user} cannot afford {txt#amount}!", player.getName(), formatAmount);
                    }
                    else
                    {
                        i18n.sendTranslated(context, NEGATIVE, "You cannot afford {txt#amount}!", formatAmount);
                    }
                    break;
                default:
                    i18n.sendTranslated(context, NEGATIVE, "The Transaction was not successful!");
                    break;
            }
        }
    }

    private Cause causeOf(CommandSource context)
    {
        return Cause.of(NamedCause.source(context));
    }
}