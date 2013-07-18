package com.hotmail.shinyclef.shinyraffle;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Author: Shinyclef
 * Date: 17/07/12
 * Time: 9:19 PM
 */

public class CmdExecutor implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase("raffle")
                || (command.getName().equalsIgnoreCase("raf")))
        {
            //make sure there's at least 1 arg
            if (args.length < 1)
                return false;

            String com = args[0].toLowerCase();

            if (com.equals("buy"))
            {
                return RaffleLogic.buy(sender, command, label, args);
            }

            else if (com.equals("check"))
            {
                return RaffleLogic.check(sender, command, label, args);
            }

            else if (com.equals("stats"))
            {
                return RaffleLogic.stats(sender, command, label, args);
            }

            else if (com.equals("add"))
            {
                return RaffleLogic.add(sender, command, label, args);
            }

            else if (com.equals("addextra"))
            {
                return RaffleLogic.addExtra(sender, command, label, args);
            }

            else if (com.equals("remove"))
            {
                return RaffleLogic.remove(sender, command, label, args);
            }

            else if (com.equals("removeextra"))
            {
                return RaffleLogic.removeExtra(sender, command, label, args);
            }

            else if (com.equals("draw"))
            {
                return RaffleLogic.draw(sender, command, label, args);
            }

            else if (com.equals("buylimit"))
            {
                return RaffleLogic.buyLimit(sender, command, label, args);
            }

            else if (com.equals("ticketprice"))
            {
                return RaffleLogic.ticketPrice(sender, command, label, args);
            }

            else if (com.equals("new"))
            {
                return RaffleLogic.newRaffle(sender, command, label, args);
            }

            else if (com.equals("donate"))
            {
                return RaffleLogic.donate(sender, command, label, args);
            }

            else if (com.equals("enable"))
            {
                return RaffleLogic.enable(sender, command, label, args);
            }

            else if (com.equals("disable"))
            {
                return RaffleLogic.disable(sender, command, label, args);
            }

            else if (com.equals("refund"))
            {
                return RaffleLogic.refund(sender, command, label, args);
            }

            else if (com.equals("account"))
            {
                return RaffleLogic.account(sender, command, label, args);
            }

            else if (com.equals("help"))
            {
                return RaffleLogic.help(sender, command, label, args);
            }

            else if (com.equals("status"))
            {
                return RaffleLogic.status(sender, command, label, args);
            }

            else if (com.equals("debug"))
            {
                return RaffleLogic.debug(sender, command, label, args);
            }
        }

        return false;
    }
}