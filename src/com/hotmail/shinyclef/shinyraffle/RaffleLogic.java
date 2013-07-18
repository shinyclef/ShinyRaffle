package com.hotmail.shinyclef.shinyraffle;

import com.sun.xml.internal.ws.client.SenderException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import sun.awt.windows.ThemeReader;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Author: Shinyclef
 * Date: 22/06/13
 * Time: 1:15 PM
 */

public class RaffleLogic
{
    //general
    private static ShinyRaffle plugin;
    private static Economy economy = null;
    private static Configuration config = null;
    private static DecimalFormat twoDecimal = new DecimalFormat("0.00");
    private static Integer activeDraw;
    private static Integer donationID;
    private static Integer maxPlayerID;

    //config settings
    private static boolean active;
    private static boolean enabled;
    private static double fund;
    private static Map<String, Object> ticketMap = null;
    private static Map<String, Object> moneyMap = null;

    //active raffle DB variables
    private static int entrants;
    private static double ticketPrice;
    private static int ticketsSold;
    private static int buyLimit;
    private static double ticketFunded;
    private static double accountFunded;
    private static double donationFunded;
    private static double extraFunded;
    private static double totalPrizePool;

    //raffle stats
    private static double biggestPot;
    private static int rafflesQty;
    private static int totalTicketsSold;
    private static double totalMoneySpent;
    private static double totalDonated;

    public static void initializeVariables(ShinyRaffle thePlugin)
    {
        plugin = thePlugin;
        economy = plugin.getEconomy();
        config = plugin.getConfig();

        //get variables from config
        active = config.getBoolean("Active");
        enabled = config.getBoolean("Enabled");
        fund = config.getDouble("Raffle.Fund");
        donationFunded = config.getDouble("Raffle.DonationFunded");

        //get ticketMap from config
        try
        {
            ticketMap = config.getConfigurationSection("Raffle.PlayerTickets").getValues(false);
        }
        catch (NullPointerException ex)
        {
            ticketMap = new HashMap<String, Object>();
        }

        //get moneyMap from config
        try
        {
            moneyMap = config.getConfigurationSection("Raffle.PlayerMoneyEntered").getValues(false);
        }
        catch (NullPointerException ex)
        {
            moneyMap = new HashMap<String, Object>();
        }

        //Database.initializeDBVariables();
    }

    public static void initialiseDBVariables(ResultSet rs)
    {
        try
        {
            while (rs.next())
            {
                entrants = rs.getInt("Entrants");
                ticketPrice = rs.getDouble("TicketPrice");
                ticketsSold = rs.getInt("TicketsSold");
                buyLimit = rs.getInt("BuyLimit");
                ticketFunded = rs.getDouble("TicketFunded");
                accountFunded = rs.getDouble("AccountFunded");
                extraFunded = rs.getDouble("ExtraFunded");
                totalPrizePool = rs.getDouble("TotalPrizePool");
                biggestPot = rs.getDouble("BiggestPot");
                rafflesQty = rs.getInt("RafflesQty") - 1;
                totalTicketsSold = rs.getInt("TotalTicketsSold");
                totalMoneySpent = rs.getDouble("TotalMoneySpent");
                totalDonated = rs.getDouble("TotalDonated");
                activeDraw = rs.getInt("ActiveDraw");
                donationID = rs.getInt("MaxDonationID");
                maxPlayerID = rs.getInt("MaxPlayerID");

                if (activeDraw == null)
                {
                    activeDraw = 0;
                }

                if (donationID == null)
                {
                    donationID = 0;
                }

                if (maxPlayerID == null)
                {
                    maxPlayerID = 0;
                }
            }

            Database.disconnect();
        }
        catch (SQLException ex)
        {
            plugin.log.info("ShinyRaffle Error: " + ex.getMessage());
        }
    }

    public static boolean buy(CommandSender sender, Command command, String label, String[] args)
    {
        //anything other than 2 arg, stop
        if (args.length != 2)
            return false;

        //if raffle is disabled, notify and stop (unless admin team)
        if (!enabled)
        {
            if (!sender.hasPermission("rolyd.mod"))
            {
                sender.sendMessage(ChatColor.YELLOW + "Sorry, ticket purchases are currently" + ChatColor.RED +
                        " disabled" + ChatColor.YELLOW + ".");
                return true;
            }
            else
            {
                sender.sendMessage(ChatColor.YELLOW + "Reminder: Raffle is currently disabled for regular players.");
            }
        }

        //if command is accessed from console, stop
        if (!(sender instanceof Player))
        {
            sender.sendMessage("Sorry, this command cannot be used from console.");
            return true;
        }

        //if arg[1] is not a number, return false
        int buyQty = 0;
        try
        {
            buyQty = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            return false;
        }

        //negative check
        if (buyQty < 0)
        {
            sender.sendMessage(ChatColor.RED +
                    "I see what you did there! Sorry, the pot will not give you money! No negatives please! :P");
            return true;
        }

        //if player is trying to buy 0, notify and stop
        if (buyQty == 0)
        {
            sender.sendMessage(ChatColor.RED +
                    "Well, I guess you can say you successfully bought 0 tickets! Try buying more than 0 next time... :P");
            return true;
        }

        //if player cannot afford it, notify and stop
        double cost = buyQty * ticketPrice;
        if (!economy.has(sender.getName(), cost))
        {
            if (buyQty == 1)
            {
                sender.sendMessage(ChatColor.RED + "Sorry, 1 ticket costs " + ChatColor.WHITE + "$" +
                        twoDecimal.format(cost) + ChatColor.RED + ". You only have " + ChatColor.WHITE + "$" +
                        twoDecimal.format(economy.getBalance(sender.getName())) + ChatColor.RED + ".");
                return true;
            }
            if (buyQty > 1)
            {
                sender.sendMessage(ChatColor.RED + "Sorry, " + buyQty + " tickets cost " + ChatColor.WHITE +
                        "$" + twoDecimal.format(cost) + ChatColor.RED + ". You only have " + ChatColor.WHITE +
                        twoDecimal.format(economy.getBalance(sender.getName())) + ChatColor.RED + ".");
                return true;
            }
        }

        //if qty will not be over add the qty they bought and cost to their totals in the maps
        //also mark if new or old player
        if (ticketMap.get(sender.getName()) == null)
        {
            if (buyQty > buyLimit && buyLimit != 0)
            {
                sender.sendMessage(ChatColor.RED + "Sorry, you cannot buy more than the current ticket limit of " +
                        ChatColor.GOLD + buyLimit + ChatColor.RED + ".");
                return true;
            }
            ticketMap.put(sender.getName(), buyQty);
            moneyMap.put(sender.getName(), cost);
            entrants++;
        }
        else if ((Integer)ticketMap.get(sender.getName()) + buyQty > buyLimit && buyLimit != 0)
        {
            sender.sendMessage(ChatColor.RED + "Sorry, that would put your ticket count at " + ChatColor.GOLD +
                    ((Integer) ticketMap.get(sender.getName()) + buyQty) + ChatColor.RED +
                    " which is over the ticket limit of " + ChatColor.GOLD + buyLimit + ChatColor.RED + ".");
            return true;
        }
        else
        {
            ticketMap.put(sender.getName(), (Integer)ticketMap.get(sender.getName()) + buyQty);
            moneyMap.put(sender.getName(), (Double)moneyMap.get(sender.getName()) + cost);
        }

        //subtract cost from their account, and add it to the prize pool
        economy.withdrawPlayer(sender.getName(), cost);
        ticketFunded = ticketFunded + cost;
        recalculateTotalPrizePool();

        //update stats: tickets sold, total tickets sold, total money spent
        ticketsSold = ticketsSold + buyQty;
        totalTicketsSold = totalTicketsSold + buyQty;
        totalMoneySpent = totalMoneySpent + cost;

        //update config
        config.createSection("Raffle.PlayerTickets", ticketMap);
        config.createSection("Raffle.PlayerMoneyEntered", moneyMap);
        plugin.saveConfig();

        //update database
        Database.buy(entrants, buyQty, (Integer)ticketMap.get(sender.getName()),
                cost, (Double)moneyMap.get(sender.getName()), sender.getName());

        //notify player
        if (buyQty == 1)
            sender.sendMessage(ChatColor.GREEN + "1 ticket bought!");
        else
            sender.sendMessage(ChatColor.GREEN + "" + buyQty + " tickets bought!");

        //2nd line of notification
        int playerQty = (Integer) ticketMap.get(sender.getName());
        if (playerQty == 1)
            sender.sendMessage(ChatColor.GREEN + "You now have " + ChatColor.GOLD + "1" + ChatColor.GREEN +
                    " ticket and the pot is currently worth " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.GREEN +  ".");
        else
            sender.sendMessage(ChatColor.GREEN + "You now have " + ChatColor.GOLD + playerQty +
                    ChatColor.GREEN + " tickets and the pot is currently worth " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.GREEN + ".");

        return true;
    }

    public static boolean check(CommandSender sender, Command command, String label, String[] args)
    {
        //message if there is no active raffle
        if (!active)
        {
            sender.sendMessage(ChatColor.GREEN + "There is no raffle running right now. Donations for the"
                    + " next pot are currently worth " + ChatColor.GOLD + "$" + twoDecimal.format(totalPrizePool)
                    + ChatColor.GREEN + ".");
            return true;
        }

        //notify player of how many tickets they have and how much the pot is worth
        int playerQty = 0;
        if (ticketMap.get(sender.getName()) != null)
        {
            playerQty = (Integer) ticketMap.get(sender.getName());
        }

        if (playerQty == 1)
        {
            sender.sendMessage(ChatColor.GREEN + "Tickets cost " + ChatColor.GOLD + "$" +
                    twoDecimal.format(ticketPrice) + " each.");
            sender.sendMessage(ChatColor.GREEN + "You have " + ChatColor.GOLD + "1" + ChatColor.GREEN +
                    " ticket and the pot is currently worth " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.GREEN + ".");
        }
        else if (playerQty > 1)
        {
            sender.sendMessage(ChatColor.GREEN + "Tickets cost " + ChatColor.GOLD + "$" +
                    twoDecimal.format(ticketPrice) + " each.");
            sender.sendMessage(ChatColor.GREEN + "You have " + ChatColor.GOLD + playerQty + ChatColor.GREEN +
                    " tickets and the pot is currently worth " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.GREEN + ".");
        }
        else
        {
            sender.sendMessage(ChatColor.GREEN + "Tickets cost " + ChatColor.GOLD + "$" +
                    twoDecimal.format(ticketPrice) + " each.");
            sender.sendMessage(ChatColor.GREEN + "You don't have any tickets. The pot is currently worth " +
                    ChatColor.GOLD + "$" + twoDecimal.format(totalPrizePool) + ChatColor.GREEN + ".");
        }

        return true;
    }

    public static boolean stats(CommandSender sender, Command command, String label, String[] args)
    {
        //args length of 1
        if (args.length != 1)
        {
            return false;
        }

        Database.getPlayerStats(sender);

        return true;
    }

    public static void statsDBReturn(CommandSender sender, ResultSet rs)
    {
        //player stats
        int drawsEntered = 0;
        int drawsWon = 0;
        double playerTotalMoneySpent = 0;
        int totalTicketsBought = 0;
        double totalWinnings = 0;
        double totalDonated = 0;
        int mostTicketsEntered = 0;
        double mostMoneyEntered = 0;

        try
        {
            while (rs.next())
            {
                drawsEntered = rs.getInt("DrawsEntered");
                drawsWon = rs.getInt("DrawsWon");
                playerTotalMoneySpent = rs.getDouble("TotalMoneySpent");
                totalTicketsBought = rs.getInt("TotalTicketsBought");
                totalWinnings = rs.getDouble("TotalWinnings");
                totalDonated = rs.getDouble("TotalDonated");
                mostTicketsEntered = rs.getInt("MostTicketsEntered");
                mostMoneyEntered = rs.getDouble("MostMoneyEntered");
            }
        }
        catch (SQLException ex)
        {
            plugin.log.info("ShinyRaffle Error: " + ex.getMessage());
        }
        finally
        {
            Database.disconnect();
        }


        sender.sendMessage(ChatColor.DARK_AQUA + "Raffle Stats");
        sender.sendMessage(ChatColor.AQUA + "Raffles Drawn: " + ChatColor.WHITE + rafflesQty);
        sender.sendMessage(ChatColor.AQUA + "Total Tickets Sold: " + ChatColor.WHITE + ticketsSold);
        sender.sendMessage(ChatColor.AQUA + "Total Money Spent: " + ChatColor.WHITE + "$" + twoDecimal.format(totalMoneySpent));
        sender.sendMessage(ChatColor.AQUA + "Biggest Pot: " + ChatColor.WHITE + "$" + twoDecimal.format(biggestPot));

        sender.sendMessage(ChatColor.DARK_AQUA + "Your Stats");
        sender.sendMessage(ChatColor.AQUA + "Draws Entered: " + ChatColor.WHITE + drawsEntered);
        sender.sendMessage(ChatColor.AQUA + "Draws Won: " + ChatColor.WHITE + drawsWon);
        sender.sendMessage(ChatColor.AQUA + "Total Money Spent: " + ChatColor.WHITE + "$" + twoDecimal.format(playerTotalMoneySpent));
        sender.sendMessage(ChatColor.AQUA + "Total Tickets Bought: " + ChatColor.WHITE + totalTicketsBought);
        sender.sendMessage(ChatColor.AQUA + "Total Winnings: " + ChatColor.WHITE + "$" + twoDecimal.format(totalWinnings));
        sender.sendMessage(ChatColor.AQUA + "Total Donated: " + ChatColor.WHITE + "$" + twoDecimal.format(totalDonated));
        sender.sendMessage(ChatColor.AQUA + "Most Tickets Entered: " + ChatColor.WHITE + mostTicketsEntered);
        sender.sendMessage(ChatColor.AQUA + "Most Money Entered: " + ChatColor.WHITE + "$" + twoDecimal.format(mostMoneyEntered));
    }

    /* adds to the raffle from the fund */
    public static boolean add(CommandSender sender, Command command, String label, String[] args)
    {
        //if player does not have permission
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        //check correct number of args
        if (args.length != 2)
            return false;

        //if arg[1] is not a number, notify of correct use
        double addQty = 0;
        try
        {
            addQty = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage(ChatColor.RED + "/raffle add [#]. eg: '/raffle add 2000' will add $2000 to the pot.");
            return true;
        }

        //if there is no active pot
        if (!active)
        {
            sender.sendMessage(ChatColor.RED + "You can only add funds to the pot during an active raffle.");
            return true;
        }

        //make sure fund can afford it
        if (addQty > fund)
        {
            sender.sendMessage(ChatColor.RED + "Sorry, the raffle account only has " +
                    ChatColor.GOLD + "$" + twoDecimal.format(fund) + ChatColor.RED +
                    ". Use '/raffle addextra <amount>' to add to the pot from 'thin air'.");
            return true;
        }

        //update counts
        accountFunded = accountFunded + addQty;
        recalculateTotalPrizePool();
        fund = fund - addQty;

        //save
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);
        plugin.getConfig().set("Raffle.Fund", fund);
        plugin.saveConfig();

        //notify player
        sender.sendMessage(ChatColor.YELLOW + "You have transferred " + ChatColor.GOLD + "$" + twoDecimal.format(addQty) +
                ChatColor.YELLOW + " to the pot bringing it up to " + ChatColor.GOLD + "$" +
                twoDecimal.format(totalPrizePool) + ChatColor.YELLOW + ".");

        return true;
    }

    /* adds to the raffle from thin air */
    public static boolean addExtra(CommandSender sender, Command command, String label, String[] args)
    {
        //if player does not have permission
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        //check correct number of args
        if (args.length != 2)
            return false;

        //if arg[1] is not a number, notify of correct use
        double extraQty = 0;
        try
        {
            extraQty = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage(ChatColor.RED + "/raffle add [#]. eg: '/raffle add 2000' will add $2000 to the pot.");
            return true;
        }

        //if there is no active pot
        if (!active)
        {
            sender.sendMessage(ChatColor.RED + "You can only add funds to the pot during an active raffle.");
            return true;
        }

        //update counts and save
        extraFunded = extraFunded + extraQty;
        recalculateTotalPrizePool();
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);

        //notify player
        sender.sendMessage(ChatColor.YELLOW + "You have added an extra " + ChatColor.GOLD + "$" + twoDecimal.format(extraQty) +
                ChatColor.YELLOW + " to the pot bringing it up to " + ChatColor.GOLD + "$" +
                twoDecimal.format(totalPrizePool) + ChatColor.YELLOW + ".");

        return true;
    }

    /* removes from the raffle back to the pot */
    public static boolean remove (CommandSender sender, Command command, String label, String[] args)
    {
        //if player does not have permission
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        //check correct number of args
        if (args.length != 2)
            return false;

        //if arg[1] is not a number, notify of correct use
        double subtractQty = 0;
        try
        {
            subtractQty = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage(ChatColor.RED + "/raffle remove [#]. eg: '/raffle remove 2000' will remove $2000 from the pot.");
            return true;
        }

        //if there is no active pot
        if (!active)
        {
            sender.sendMessage(ChatColor.RED + "You can only remove funds from the pot during an active raffle.");
            return true;
        }

        //if pot does not have that much money to subtract, notify and stop
        if (totalPrizePool < subtractQty)
        {
            sender.sendMessage(ChatColor.RED + "You cannot subtract " + ChatColor.GOLD + "$" + twoDecimal.format(subtractQty) +
                    ChatColor.RED + " from the pot because it only contains " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.RED + ".");
            return true;
        }

        //remove amount from the pot and put it back in the fund
        accountFunded = accountFunded - subtractQty;
        recalculateTotalPrizePool();
        fund = fund + subtractQty;
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);
        config.set("Raffle.Fund", fund);
        plugin.saveConfig();

        //notify player
        sender.sendMessage(ChatColor.YELLOW + "You have subtracted " + ChatColor.GOLD + "$" + twoDecimal.format(subtractQty)
                + ChatColor.YELLOW + " from the pot bringing it down to " + ChatColor.GOLD + "$" + twoDecimal.format(totalPrizePool) + ChatColor.YELLOW + ".");

        return true;
    }

    /* removes from the raffle into thin air */
    public static boolean removeExtra(CommandSender sender, Command command, String label, String[] args)
    {
        //if player does not have permission
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        //check correct number of args
        if (args.length != 2)
            return false;

        //if arg[1] is not a number, notify of correct use
        double takeQty = 0;
        try
        {
            takeQty = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage(ChatColor.RED + "/raffle take [#]. eg: '/raffle take 2000' "
                    + "will take $2000 from the pot (but will 'not' put it back in the raffle fund).");
            return true;
        }

        //if there is no active pot
        if (!active)
        {
            sender.sendMessage(ChatColor.RED + "You can only remove funds from the pot during an active raffle.");
            return true;
        }

        //if pot does not have that much money to remove, notify and stop
        if (totalPrizePool < takeQty)
        {
            sender.sendMessage(ChatColor.RED + "You cannot remove " + ChatColor.GOLD + "$" + twoDecimal.format(takeQty) +
                    ChatColor.RED + " from the pot because it only contains " + ChatColor.GOLD + "$" +
                    twoDecimal.format(totalPrizePool) + ChatColor.RED + ".");
            return true;
        }

        //remove amount from the pot
        extraFunded = extraFunded - takeQty;
        recalculateTotalPrizePool();
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);

        //notify player
        sender.sendMessage(ChatColor.YELLOW + "You have removed " + ChatColor.GOLD + "$" + twoDecimal.format(takeQty)
                + ChatColor.YELLOW + " from the pot bringing it down to " + ChatColor.GOLD + "$" + twoDecimal.format(totalPrizePool) + ChatColor.YELLOW + ".");

        return true;
    }

    public static boolean draw(CommandSender sender, Command command, String label, String[] args)
    {
        //args length of 1
        if (args.length != 1)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        //if no one has bought a ticket, notify and stop
        if (ticketMap.isEmpty())
        {
            sender.sendMessage(ChatColor.RED + "No one has bought any tickets.");
            return true;
        }

        //get a random number between 1 and tickets sold
        int winningNumber = new Random().nextInt(ticketsSold) + 1;

        //draw vars
        int currentLimit = 0;
        String winner = "";

        //for each item in ticketMap
        for (Map.Entry<String, Object> entry : ticketMap.entrySet())
        {
            //set the currentLimit to the sum of all players' tickets checked so far
            currentLimit = currentLimit + (Integer)entry.getValue();

            //if the winning number is less than or equal to that limit, that player has won
            if (winningNumber <= currentLimit)
            {
                //get key of winning value (get winner's name)
                winner = entry.getKey();
                break;
            }
        }

        //precaution against possible logical error in code causing no winner to be selected
        if (winner.equals(""))
        {
            sender.sendMessage(ChatColor.DARK_RED + "Error!" + ChatColor.RED +
                    " Something is wrong! No winner has been found so nothing will be done to players' tickets or the pot.");
            return true;
        }

        //make sure winner's account has not been removed
        if (!economy.hasAccount(winner))
        {
            sender.sendMessage(ChatColor.DARK_RED + "Error!" + ChatColor.RED + " The winning player " + ChatColor.WHITE + winner + ChatColor.RED +
                    " no long has an account to deposit the money into! Use " + ChatColor.WHITE + "/raffle draw" + ChatColor.RED + " to redraw.");
            return true;
        }

        //disable raffle
        enabled = false;

        //vars for scheduled announcements
        final String winner2 = winner;
        final double prizePool2 = totalPrizePool;
        final int ticketsSold2 = ticketsSold;

        //start announcement
        plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE +
                "Ladies and gentlemen! Hold onto your seats, cause it's Raffle time!");

        //in 5 secs
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                if (ticketsSold2 > 1)
                    plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "The barrel is now spinning with " + ChatColor.GOLD +
                            ticketsSold2 + ChatColor.LIGHT_PURPLE + " tickets in the running for " +  ChatColor.GOLD + "$" + twoDecimal.format(prizePool2) +
                            ChatColor.LIGHT_PURPLE + "!");
                else
                    plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "The barrel is now spinning with " + ChatColor.GOLD +
                            1 + ChatColor.LIGHT_PURPLE + " ticket in the running for " +  ChatColor.GOLD + "$" + twoDecimal.format(prizePool2) +
                            ChatColor.LIGHT_PURPLE + "!"); }}, 100L);
        //in 10 secs
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE +
                        "And now, the moment we've all been waiting for!");
                plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE +
                        "\"Who is the winner!?\", you ask? Well, THE WINNER IS... !"); }}, 200L);
        //in 20 secs
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getServer().broadcastMessage(ChatColor.GOLD + winner2 + ChatColor.LIGHT_PURPLE + "! Congratulations!");
                //credit winner with the pot
                economy.depositPlayer(winner2, prizePool2); }}, 400L);
        //in 25 secs
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE +
                        "Thanks for playing everyone! And good luck in the next raffle!");}},500L);

        //update stats
        rafflesQty++;
        if (totalPrizePool > biggestPot)
        {
            biggestPot = totalPrizePool;
        }

        //prepare next raffle's figures
        double winnings = totalPrizePool;
        ticketFunded = 0;
        accountFunded = 0;
        donationFunded = 0;
        extraFunded = 0;
        totalPrizePool = 0;
        ticketsSold = 0;
        entrants = 0;
        active = false;

        config.set("Raffle.DonationFunded", donationFunded);
        config.set("Active", false);

        //update database
        Database.draw(winner2, sender.getName(), ticketPrice, buyLimit, winnings, ticketMap, moneyMap);

        //continues after DB operations have completed in drawDBReturn()

        return true;
    }

    public static void drawDBReturn()
    {
        //clear maps, increment active drawID
        ticketMap.clear();
        moneyMap.clear();
        activeDraw++;

        //update config
        config.createSection("Raffle.PlayerTickets", ticketMap);
        config.createSection("Raffle.PlayerMoneyEntered", moneyMap);
        plugin.saveConfig();

        setDisabled();
    }

    public static boolean buyLimit(CommandSender sender, Command command, String label, String[] args)
    {
        //args length should be 2
        if (args.length != 2)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that");
            return true;
        }

        int newLimit = 0;
        //if arg[1] is not a number, return false
        try
        {
            newLimit = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            return false;
        }

        //make sure it's a positive number
        if (newLimit < 0)
        {
            sender.sendMessage(ChatColor.RED + "You cannot enter a negative number. Use 0 for unlimited.");
            return true;
        }

        //set new limit and save to config
        buyLimit = newLimit;
        plugin.getConfig().set("Raffle.BuyLimit", buyLimit);
        plugin.saveConfig();

        //update database
        Database.setBuyLimit(buyLimit);

        //send notification to sender
        sender.sendMessage(ChatColor.YELLOW + "You have set the buy limit to " + ChatColor.GOLD +
                buyLimit + ChatColor.YELLOW + ".");

        //notify the rest of the admin team
        Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers)
        {
            if (player.hasPermission("rolyd.mod") && !player.getName().equals(sender.getName()))
            {
                player.sendMessage(ChatColor.GOLD + sender.getName() + ChatColor.YELLOW +
                        "has set the buy limit to " + ChatColor.GOLD + buyLimit + ChatColor.YELLOW + ".");
            }
        }

        return true;
    }

    public static boolean ticketPrice(CommandSender sender, Command command, String label, String[] args)
    {
        //args length should be 2
        if (args.length != 2)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that");
            return true;
        }

        double newPrice = 0;
        //if arg[1] is not a number, return false
        try
        {
            newPrice = Double.parseDouble(args[1]);
        }
        catch (NumberFormatException e)
        {
            return false;
        }

        //make sure it's a positive number
        if (newPrice <= 0)
        {
            sender.sendMessage(ChatColor.RED + "Please enter a number greater than 0.");
            return true;
        }

        //set new price and save to config
        ticketPrice = newPrice;
        plugin.getConfig().set("Raffle.TicketPrice", ticketPrice);
        plugin.saveConfig();

        //update database
        Database.setTicketPrice(ticketPrice);

        //send confirmation to sender
        sender.sendMessage(ChatColor.YELLOW + "You have set the ticket price to " + ChatColor.GOLD +
                "$" + twoDecimal.format(ticketPrice) + ChatColor.YELLOW + ".");

        //notify the rest of the admin team
        Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers)
        {
            if (player.hasPermission("rolyd.mod") && !player.getName().equals(sender.getName()))
            {
                player.sendMessage(ChatColor.GOLD + sender.getName() + ChatColor.YELLOW +
                        " has set the ticket price to " + ChatColor.GOLD + "$" +
                        twoDecimal.format(ticketPrice) + ChatColor.YELLOW + ".");
            }
        }

        return true;
    }

    public static boolean newRaffle(CommandSender sender, Command command, String label, String[] args)
    {
        //args length 1/2
        if (args.length != 1 && args.length != 2)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        //check if there is already an active raffle
        if (active)
        {
            sender.sendMessage(ChatColor.RED + "There is already a new raffle in progress.");
            return true;
        }

        //set accountFunded
        if (args.length == 2)
        {
           if (!transferFromFund(sender, args[1]))
               return true;
        }

        //enable the raffle
        setEnabled();
        active = true;
        config.set("Active", true);
        plugin.saveConfig();

        //notify sender
        sender.sendMessage(ChatColor.YELLOW + "You have started a new raffle worth " + ChatColor.GOLD +
            "$" + twoDecimal.format(totalPrizePool) + ChatColor.YELLOW + ".");

        //notify rest of admin team
        Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers)
        {
            if (player.hasPermission("rolyd.mod") && player.getName() != sender.getName())
            {
                player.sendMessage(ChatColor.YELLOW + "A new raffle has been started by " +
                        ChatColor.GOLD + sender.getName() + ChatColor.YELLOW +
                        ". Players can now buy tickets.");
            }
        }

        return true;
    }

    public static boolean account(CommandSender sender, Command command, String label, String[] args)
    {
        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        //args length 1
        if (args.length != 1 && args.length != 3)
            return false;

        if (args.length == 1)
        {
            //display info
            sender.sendMessage(ChatColor.YELLOW + "Raffle Account: " + ChatColor.GOLD + "$" + twoDecimal.format(fund));
            return true;
        }

        //get valid amount or null
        Double amount = getPositiveDouble(args[2]);
        if (args[1].equalsIgnoreCase("add"))
        {
            //check validity
            if (amount == null)
                return false;

            //negativity
            if (amount < 0)
            {
                sender.sendMessage(ChatColor.RED + "Obviously, you must enter a positive number!");
                return true;
            }

            //add
            fund = fund + amount;

            //inform
            sender.sendMessage(ChatColor.YELLOW + "You have added " + ChatColor.GOLD + "$" + twoDecimal.format(amount) +
                    ChatColor.YELLOW + " to the raffle account.");
        }
        else if (args[1].equalsIgnoreCase("remove"))
        {
            //check validity
            if (amount == null)
                return false;

            //negativity
            if (amount < 0)
            {
                sender.sendMessage(ChatColor.RED + "Obviously, you must enter a positive number!");
                return true;
            }

            //make sure fund has that much
            if (amount > fund)
            {
                sender.sendMessage(ChatColor.RED + "There is only " + ChatColor.GOLD + "$" + twoDecimal.format(fund) +
                ChatColor.RED + " available in the fund.");
                return true;
            }

            //remove
            fund = fund - amount;

            //inform
            sender.sendMessage(ChatColor.YELLOW + "You have removed " + ChatColor.GOLD + "$" + twoDecimal.format(amount) +
                    ChatColor.YELLOW + " from the raffle account.");
        }

        //save
        config.set("Raffle.Fund", fund);
        plugin.saveConfig();

        return true;
    }

    public static boolean enable(CommandSender sender, Command command, String label, String[] args)
    {
        //args length 1
        if (args.length != 1)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        //notify sender
        sender.sendMessage(ChatColor.YELLOW + "Raffle has been " + ChatColor.GOLD + "enabled" +
                ChatColor.YELLOW +  ". Players can now buy tickets.");

        Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers)
        {
            if (player.hasPermission("rolyd.mod") && player.getName() != sender.getName())
            {
                player.sendMessage(ChatColor.YELLOW + "Raffle has been " + ChatColor.GOLD + "enabled" +
                        ChatColor.YELLOW + " by " + ChatColor.GOLD + sender.getName() + ChatColor.YELLOW +
                        ". Players can now buy tickets.");
            }
        }

        setEnabled();
        return true;
    }

    public static boolean disable(CommandSender sender, Command command, String label, String[] args)
    {
        //args length 1
        if (args.length != 1)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You do not have permission to do that.");
            return true;
        }

        //notify sender
        sender.sendMessage(ChatColor.YELLOW + "Raffle has been "+ ChatColor.GOLD + "disabled" +
                ChatColor.YELLOW + ". Players can no longer buy tickets.");

        //notify rest of admin team
        Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
        for (Player player : onlinePlayers)
        {
            if (player.hasPermission("rolyd.mod") && player.getName() != sender.getName())
            {
                player.sendMessage(ChatColor.YELLOW + "Raffle has been " + ChatColor.GOLD + "disabled" +
                        ChatColor.YELLOW + " by " + ChatColor.GOLD + sender.getName() + ChatColor.YELLOW +
                        ". Players can no longer buy tickets.");
            }
        }

        setDisabled();
        return true;
    }

    private static void setEnabled()
    {
        enabled = true;
        plugin.getConfig().set("Enabled", true);
        plugin.saveConfig();
    }

    private static void setDisabled()
    {
        enabled = false;
        plugin.getConfig().set("Enabled", false);
        plugin.saveConfig();
    }

    public static boolean donate(CommandSender sender, Command command, String label, String[] args)
    {
        //args length 2
        if (args.length != 2)
            return false;

        //valid positive number
        Double amount = getPositiveDouble(args[1]);
        if (amount == null)
            return false;

        //if less than $1, stop
        if (amount < 100)
        {
            sender.sendMessage(ChatColor.RED + "Thanks, but the minimum amount you can donate is $100.");
            return true;
        }

        //round it to 2 decimal places
        amount = round(amount);

        //if they can't afford it
        if (!economy.has(sender.getName(), amount))
        {
            sender.sendMessage(ChatColor.RED + "Unfortunately, you can't afford to donate that much!");
            return true;
        }

        //all good, donate
        economy.withdrawPlayer(sender.getName(), amount);
        donationFunded = donationFunded + amount;
        recalculateTotalPrizePool();

        //save
        Database.donate(sender.getName(), amount);
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);
        config.set("Raffle.DonationFunded", donationFunded);
        plugin.saveConfig();

        DecimalFormat df = new DecimalFormat("#0.00");
        plugin.getServer().broadcastMessage(ChatColor.AQUA + sender.getName() + ChatColor.YELLOW + " has donated " +
                ChatColor.AQUA + "$" + df.format(amount) + ChatColor.YELLOW + " to the raffle fund.");

        return true;
    }

    public static boolean refund(CommandSender sender, Command command, String label, String[] args)
    {
        //args length = 2
        if (args.length != 2)
            return false;

        //check perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args[1].equalsIgnoreCase("all"))
        {
            //for each item in ticketMap
            String playerName;
            int ticketQty;
            double refund;

            for (Map.Entry<String, Object> entry : ticketMap.entrySet())
            {
                playerName = entry.getKey();
                ticketQty = (Integer)entry.getValue();
                refund = (Double)moneyMap.get(playerName);

                economy.depositPlayer(entry.getKey(), refund);
                recalculateTotalPrizePool();

                entrants--;
                ticketFunded = ticketFunded - refund;
                totalMoneySpent = totalMoneySpent - refund;
                ticketsSold = ticketsSold - ticketQty;
                totalTicketsSold = totalTicketsSold - ticketQty;
            }

            //clear the maps and recalculate
            ticketMap.clear();
            moneyMap.clear();
            recalculateTotalPrizePool();

            //safe
            config.createSection("Raffle.PlayerTickets", ticketMap);
            config.createSection("Raffle.PlayerMoneyEntered", moneyMap);
            plugin.saveConfig();

            //update database
            Database.refund(entrants, ticketsSold, ticketFunded, totalPrizePool);

            //notify command sender
            sender.sendMessage(ChatColor.YELLOW + "All players have had their tickets refunded.");

            return true;
        }

        //refund for 1 player
        else
        {
            boolean found = false;

            //for each item in ticketMap
            for (Map.Entry<String, Object> entry : ticketMap.entrySet())
            {
                //if lower case Key == lower case args 1, refund that player and break
                if (entry.getKey().equalsIgnoreCase(args[1]))
                {
                    //calculate refund and give it to player
                    double refund = (Double)moneyMap.get(entry.getKey());
                    economy.depositPlayer(entry.getKey(), refund);

                    //subtract it from totalPrizePool
                    recalculateTotalPrizePool();

                    entrants--;
                    ticketFunded = ticketFunded - refund;
                    totalMoneySpent = totalMoneySpent - refund;
                    ticketsSold = ticketsSold - (Integer)entry.getValue();
                    totalTicketsSold = totalTicketsSold - (Integer)entry.getValue();

                    //remove player from maps and recalculate
                    ticketMap.remove(entry.getKey());
                    moneyMap.remove(entry.getKey());
                    recalculateTotalPrizePool();

                    //save
                    config.createSection("Raffle.PlayerTickets", ticketMap);
                    config.createSection("Raffle.PlayerMoneyEntered", moneyMap);
                    plugin.saveConfig();

                    //update database
                    Database.refund(entrants, ticketsSold, ticketFunded, totalPrizePool);

                    //notify command sender
                    sender.sendMessage(ChatColor.GOLD + args[1] + ChatColor.YELLOW + " has had their tickets refunded.");

                    //player has bought tickets = true
                    found = true;
                    break;
                }
            }

            //if player has not bought any tickets
            if (!found)
            {
                sender.sendMessage(ChatColor.RED + "That player has not bought any tickets.");
            }
            return true;
        }
    }

    public static boolean help(CommandSender sender, Command command, String label, String[] args)
    {
        //return information about commands
        sender.sendMessage(ChatColor.GREEN + "/raffle buy <#>" + ChatColor.WHITE + " - Buy <#> ticket/s.");
        sender.sendMessage(ChatColor.GREEN + "/raffle check" + ChatColor.WHITE + " - Check ticket price, how many tickets you have, and the current value of the pot.");
        sender.sendMessage(ChatColor.GREEN + "/raffle stats" + ChatColor.WHITE + " - Check some raffle statistics.");
        sender.sendMessage(ChatColor.GREEN + "/raffle donate <#>" + ChatColor.WHITE + " - Donate <#> dollars to the current raffle, increasing the prize pool.");

        //for admin team
        if (sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Admin Commands: /raffle ...");
            sender.sendMessage(ChatColor.GREEN + "add <#>" + ChatColor.WHITE + " - Transfer <#> dollars to the pot from the raffle account.");
            sender.sendMessage(ChatColor.GREEN + "remove <#>" + ChatColor.WHITE + " - Transfer <#> dollars from the pot back into the raffle account.");
            sender.sendMessage(ChatColor.GREEN + "addextra <#>" + ChatColor.WHITE + " - Add <#> dollars to the pot from 'thin air'.");
            sender.sendMessage(ChatColor.GREEN + "removeextra <#>" + ChatColor.WHITE + " - Remove <#> dollars from the pot into 'thin air'.");
            sender.sendMessage(ChatColor.GREEN + "new" + ChatColor.WHITE + " - Start a new raffle starting with an empty pot.");
            sender.sendMessage(ChatColor.GREEN + "new <#>" + ChatColor.WHITE + " - Start a new raffle with <#> dollars transferred from the raffle account.");
            sender.sendMessage(ChatColor.GREEN + "draw" + ChatColor.WHITE + " - Draw the raffle to determine and pay the winner.");
            sender.sendMessage(ChatColor.GREEN + "ticketprice <#>" + ChatColor.WHITE + " - Set the price of tickets to <#> dollars. Not recommended during an active raffle for fairness.");
            sender.sendMessage(ChatColor.GREEN + "buylimit <#>" + ChatColor.WHITE + " - Set how many tickets you can buy in total to <#>.");
            sender.sendMessage(ChatColor.GREEN + "refund <all/[playername]>" + ChatColor.WHITE + " - Refunds all tickets for all players (all), or all tickets for one player (playername).");
            sender.sendMessage(ChatColor.GREEN + "account" + ChatColor.WHITE + " - Show the balance of the raffle account.");
            sender.sendMessage(ChatColor.GREEN + "account add/remove <#>" + ChatColor.WHITE + " - Add/remove <#> dollars to/from the pot to/from 'thin air'.");
            sender.sendMessage(ChatColor.GREEN + "enable/disable" + ChatColor.WHITE + " - Enable/disable Raffle, allowing/disallowing players to buy tickets. Not needed for normal raffles.");
            sender.sendMessage(ChatColor.GREEN + "status" + ChatColor.WHITE + " - Checks values of important variables. Not usually needed but can be useful.");
            sender.sendMessage(ChatColor.GREEN + "debug" + ChatColor.WHITE + " - Checks debug variables. If something is not working, note any values that are not 'OK' and tell Shiny!");
        }

        return true;
    }

    public static boolean status(CommandSender sender, Command command, String label, String[] args)
    {
        //perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        sender.sendMessage(ChatColor.AQUA + "Active Draw: " + ChatColor.WHITE + active);
        sender.sendMessage(ChatColor.AQUA + "Buying Enabled: " + ChatColor.WHITE + enabled);
        sender.sendMessage(ChatColor.AQUA + "Players: " + ChatColor.WHITE + ticketMap.size());
        sender.sendMessage(ChatColor.AQUA + "Ticket Funded: " + ChatColor.WHITE + "$" + twoDecimal.format(ticketFunded));
        sender.sendMessage(ChatColor.AQUA + "Account Funded: " + ChatColor.WHITE + "$" + twoDecimal.format(accountFunded));
        sender.sendMessage(ChatColor.AQUA + "Donation Funded: " + ChatColor.WHITE + "$" + twoDecimal.format(donationFunded));
        sender.sendMessage(ChatColor.AQUA + "Extra Funded: " + ChatColor.WHITE + "$" + twoDecimal.format(extraFunded));
        sender.sendMessage(ChatColor.AQUA + "Total Prize Pool: " + ChatColor.WHITE + "$" + twoDecimal.format(totalPrizePool));

        sender.sendMessage(ChatColor.AQUA + "Ticket Price: " + ChatColor.WHITE + "$" + twoDecimal.format(ticketPrice));
        sender.sendMessage(ChatColor.AQUA + "Buy Limit: " + ChatColor.WHITE + buyLimit);
        sender.sendMessage(ChatColor.AQUA + "Tickets Sold: " + ChatColor.WHITE + ticketsSold);
        sender.sendMessage(ChatColor.AQUA + "Total Tickets Sold: " + ChatColor.WHITE + totalTicketsSold);
        sender.sendMessage(ChatColor.AQUA + "Total Money Spent: " + ChatColor.WHITE + "$" + twoDecimal.format(totalMoneySpent));
        sender.sendMessage(ChatColor.AQUA + "Raffle Account: " + ChatColor.WHITE + "$" + twoDecimal.format(fund));
        return true;
    }

    private static boolean transferFromFund(CommandSender sender, String input)
    {
        Double amount = getPositiveDouble(input);
        if (amount == null)
            return false;

        if (amount > fund)
        {
            sender.sendMessage(ChatColor.RED + "You cannot transfer that much. The raffle fund only has " +
                    ChatColor.YELLOW + "$" + twoDecimal.format(fund) + ChatColor.RED + " available.");
            return false;
        }

        if (amount < 0)
        {
            sender.sendMessage(ChatColor.RED + "You cannot enter a negative value.");
            return false;
        }

        //the number is good, now round it
        amount = round(amount);

        //transfer
        accountFunded = accountFunded + amount;
        fund = fund - amount;
        recalculateTotalPrizePool();

        //save
        config.set("Raffle.Fund", fund);
        plugin.saveConfig();
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);

        return true;
    }

    private static boolean transferToFund(CommandSender sender, String input)
    {
        Double amount = getPositiveDouble(input);
        if (amount == null)
            return false;

        if (amount > totalPrizePool)
        {
            sender.sendMessage(ChatColor.RED + "You cannot transfer that much. The pot only has " +
                    ChatColor.YELLOW + "$" + twoDecimal.format(fund) + ChatColor.RED + "available.");
            return false;
        }

        if (amount < 0)
        {
            sender.sendMessage(ChatColor.RED + "You cannot enter a negative value.");
            return false;
        }

        //transfer
        accountFunded = accountFunded - amount;
        fund = fund + amount;
        recalculateTotalPrizePool();

        //save
        config.set("Raffle.Fund", fund);
        plugin.saveConfig();
        Database.setFunds(accountFunded, extraFunded, totalPrizePool);

        return true;
    }

    private static void recalculateTotalPrizePool()
    {
        totalPrizePool = ticketFunded + accountFunded + donationFunded + extraFunded;
        if (totalPrizePool < 0)
        {
            totalPrizePool = 0;
        }
    }

    //returns -1 if it's negative, null if it's invalid, or else the number
    private static Double getPositiveDouble(String input)
    {
        double amount;

        try
        {
            amount = Double.parseDouble(input);
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        if (amount < 0)
        {
            return -1d;
        }

        return amount;
    }

    public static boolean debug(CommandSender sender, Command command, String label, String[] args)
    {
        //perms
        if (!sender.hasPermission("rolyd.mod"))
        {
            sender.sendMessage(ChatColor.RED + "Sorry, you don't have permission to do that.");
            return true;
        }

        int mapSize;
        if (ticketMap == null) mapSize = 0;
        else mapSize = ticketMap.size();

        sender.sendMessage(ChatColor.AQUA + "Enabled: " + ChatColor.WHITE + enabled);
        sender.sendMessage(ChatColor.AQUA + "TicketMap Size: " + ChatColor.WHITE + mapSize);
        sender.sendMessage(ChatColor.AQUA + "activeDraw: " + ChatColor.WHITE + activeDraw);
        sender.sendMessage(ChatColor.AQUA + "donationID: " + ChatColor.WHITE + donationID);
        sender.sendMessage(ChatColor.AQUA + "maxPlayerID: " + ChatColor.WHITE + maxPlayerID);

        sender.sendMessage(ChatColor.AQUA + "TicketPrice: " + ChatColor.WHITE + ticketPrice);
        sender.sendMessage(ChatColor.AQUA + "TicketsSold: " + ChatColor.WHITE + ticketsSold);
        sender.sendMessage(ChatColor.AQUA + "BuyLimit: " + ChatColor.WHITE + buyLimit);
        sender.sendMessage(ChatColor.AQUA + "TicketFunded: " + ChatColor.WHITE + ticketFunded);
        sender.sendMessage(ChatColor.AQUA + "AccountFunded: " + ChatColor.WHITE + accountFunded);
        sender.sendMessage(ChatColor.AQUA + "ExtraFunded: " + ChatColor.WHITE + extraFunded);
        sender.sendMessage(ChatColor.AQUA + "TotalPrizePool: " + ChatColor.WHITE + totalPrizePool);

        sender.sendMessage(ChatColor.AQUA + "BiggestPot: " + ChatColor.WHITE + biggestPot);
        sender.sendMessage(ChatColor.AQUA + "RafflesQty: " + ChatColor.WHITE + rafflesQty);
        sender.sendMessage(ChatColor.AQUA + "Total Tickets Sold: " + ChatColor.WHITE + totalTicketsSold);
        sender.sendMessage(ChatColor.AQUA + "Total Money Spent: " + ChatColor.WHITE + totalMoneySpent);
        sender.sendMessage(ChatColor.AQUA + "Total Donated: " + ChatColor.WHITE + totalDonated);

        Database.showDebug(sender);

        return true;
    }

    public static int getActiveDraw()
    {
        return activeDraw;
    }

    public static Integer useNextDonationID()
    {
        donationID++;
        return donationID;
    }

    public static int useMaxPlayerID()
    {
        maxPlayerID++;
        return maxPlayerID;
    }

    private static double round(double input)
    {
        BigDecimal bd = new BigDecimal(input);
        BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return rounded.doubleValue();
    }

    public static Map<String, Object> getTicketMap()
    {
        return ticketMap;
    }
}