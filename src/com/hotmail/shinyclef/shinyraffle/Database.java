package com.hotmail.shinyclef.shinyraffle;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Author: ShinyClef
 * Date: 22/06/13
 * Time: 2:15 PM
 */

public class Database
{
    //debugging
    public static String setupDatabase = "OK";
    public static String initializeRaffleDraw = "OK";
    public static String buyUpdateDraw = "OK";
    public static String buyUpdatePlayer = "OK";
    public static String drawUpdateDraw = "OK";
    public static String newDrawUpdateDraw = "OK";
    public static String drawInsertDraw = "OK";
    public static String playerStatsSelectPlayer = "OK";
    public static String getOrCreatePlayerID = "OK";
    public static String drawEachPlayer = "OK";
    public static String setBuyLimitUpdateDraw = "OK";
    public static String setTicketPriceUpdateDraw = "OK";
    public static String refundUpdateDraw = "OK";
    public static String insertDonation = "OK";

    private static ShinyRaffle plugin;
    private static Logger log;
    private static Connection connection;
    private static Statement statement;

    private static String user;
    private static String password;
    private static String databaseName;
    private static String port;
    private static String hostname;

    public static void showDebug(CommandSender sender)
    {
        sender.sendMessage("setupDatabase: " + setupDatabase);
        sender.sendMessage("initializeRaffleDraw: " + initializeRaffleDraw);
        sender.sendMessage("buyUpdateDraw: " + buyUpdateDraw);
        sender.sendMessage("buyUpdatePlayer: " + buyUpdatePlayer);
        sender.sendMessage("drawUpdateDraw: " + drawUpdateDraw);
        sender.sendMessage("newDrawUpdateDraw: " + newDrawUpdateDraw);
        sender.sendMessage("newDrawInsertDraw: " + drawInsertDraw);
        sender.sendMessage("playerStatsSelectPlayer: " + playerStatsSelectPlayer);
        sender.sendMessage("insertNewPlayer: " + getOrCreatePlayerID);
        sender.sendMessage("drawEachPlayer: " + drawEachPlayer);
        sender.sendMessage("setBuyLimitUpdateDraw: " + setBuyLimitUpdateDraw);
        sender.sendMessage("setTicketPriceUpdateDraw: " + setTicketPriceUpdateDraw);
        sender.sendMessage("refundUpdateDraw: " + refundUpdateDraw);
        sender.sendMessage("insertDonation: " + insertDonation);
    }

    /* Prepares the database driver for use with connections. */
    public static void setupConnection(ShinyRaffle thePlugin)
    {
        plugin = thePlugin;
        log = plugin.getLogger();

        //create the connection to the database
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) //problem with driver
        {
            log.info("SEVERE!!! UNABLE TO PREPARE DATABASE DRIVER. DISABLING PLUGIN.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }

        //get database settings
        Configuration config = plugin.getConfig();
        user = config.getString("Database.User");
        password = config.getString("Database.Password");
        databaseName = config.getString("Database.DatabaseName");
        port = config.getString("Database.Port");
        hostname = config.getString("Database.Hostname");
    }

    /* Establishes new connection and statement objects with the database.
    * This method should be called before accessing the database. */
    public static void connect()
    {
        //create the connection to the database
        try
        {
            connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":"
                    + port + "/" + databaseName, user, password);
         }
        catch (SQLException ex) //problem with connection
        {
            System.out.println(ex.getMessage());
            log.info("An error has occurred while trying to establish a connection to the database: "
                    + ex.getMessage());
            return;
        }

        //create a statement object to use
        try
        {
            statement = connection.createStatement();
        }
        catch (SQLException ex)
        {
            log.info("An error has occurred while trying to establish a connection to the database: "
                + ex.getMessage());
        }
    }

    /* Closes the connection and statement objects.
    * This method should be called after the database has been accessed. */
    public static void disconnect()
    {
        try
        {
            connection.close();
        }
        catch (SQLException ex)
        {
            log.info("An error has occurred while trying to close the connection to the database: "
                + ex.getMessage());
        }
    }

    public static void onPluginLoad()
    {
        new DBOnPluginLoad().runTaskAsynchronously(plugin);
    }

    private static class DBOnPluginLoad extends BukkitRunnable
    {
        @Override
        public void run()
        {
            connect();
            DBSetupDatabase();
            DBInitializeDBVariables();
        }
    }

    private static void DBSetupDatabase()
    {
        String createRafflePlayer =
                "Create Table if not exists RafflePlayer (" +
                        "PlayerID int not null," +
                        "PlayerName varchar(16) not null," +
                        "DrawsEntered int not null," +
                        "DrawsWon int not null," +
                        "TotalTicketsBought int not null," +
                        "TotalMoneySpent double not null," +
                        "TotalWinnings double not null," +
                        "TotalDonated double not null," +
                        "MostTicketsEntered int not null," +
                        "MostMoneyEntered double not null," +
                        "Primary Key (PlayerID));";

        String createRaffleDraw =
                "Create Table if not exists RaffleDraw (" +
                        "DrawID int not null," +
                        "Entrants int not null," +
                        "TicketPrice double not null," +
                        "TicketsSold int not null," +
                        "BuyLimit int not null," +
                        "TicketFunded double not null," +
                        "AccountFunded double not null," +
                        "ExtraFunded double not null," +
                        "TotalPrizePool double not null," +
                        "WinnerID int," +
                        "HostID int," +
                        "DateAndTime DateTime," +
                        "Primary Key (DrawID)," +
                        "Foreign Key (WinnerID) References RafflePlayer(PlayerID));";

        String createRaffleDonation =
                "Create Table if not exists RaffleDonation (" +
                        "DonationID int not null," +
                        "DrawID int not null," +
                        "PlayerID int not null," +
                        "DonationAmount double not null," +
                        "Primary Key (DonationID)," +
                        "Foreign Key (DrawID) References RaffleDraw(DrawID)," +
                        "Foreign Key (PlayerID) References RafflePlayer(PlayerID));";


        //creating the tables if they don't exist
        try
        {
            statement.executeUpdate(createRafflePlayer);
            statement.executeUpdate(createRaffleDraw);
            statement.executeUpdate(createRaffleDonation);
        }
        catch (SQLException ex)
        {
            setupDatabase = ex.getMessage();
            return;
        }


        String selectCount = "Select Count(DrawID) From RaffleDraw;";
        String insertDraw = "Insert Into RaffleDraw Values (1, 0, 1, 0, 1, 0, 0, 0, 0, null, null, null);";
        ResultSet rs;

        try
        {
            //if there are no draws, insert the first one
            PreparedStatement prep = connection.prepareStatement(selectCount);
            rs = prep.executeQuery();

            while (rs.next())
            {
                if (rs.getInt(1) == 0)
                {
                    prep = connection.prepareStatement(insertDraw);
                    prep.executeUpdate();
                }
            }
        }
        catch (SQLException ex)
        {
            setupDatabase = ex.getMessage();
            return;
        }
    }

    private static void DBInitializeDBVariables()
    {
        String selectAll = "Select Entrants, TicketPrice, TicketsSold, BuyLimit, TicketFunded, AccountFunded, "
                + "ExtraFunded, TotalPrizePool, (select Max(TotalPrizePool) from RaffleDraw) as BiggestPot, "
                + "(select Count(*) from RaffleDraw) as RafflesQty, "
                + "(select Sum(TotalTicketsBought) from RafflePlayer) as TotalTicketsSold, "
                + "(select Sum(TotalMoneySpent) from RafflePlayer) as TotalMoneySpent, "
                + "(select sum(DonationAmount) from RaffleDonation) as TotalDonated, "
                + "(select max(DrawID) from RaffleDraw) as ActiveDraw, "
                + "(select max(DonationID) from RaffleDonation) as MaxDonationID, "
                + "(select max(PlayerID) from RafflePlayer) as MaxPlayerID "
                + "From RaffleDraw Where DrawID = (Select Max(DrawID) From RaffleDraw);";
        ResultSet rs;

        try
        {
            rs = statement.executeQuery(selectAll);
            new GetAllVarsSyncReturn(rs).runTask(plugin);
        }
        catch (SQLException ex)
        {
            initializeRaffleDraw = ex.getMessage();
            return;
        }
    }

    private static class GetAllVarsSyncReturn extends BukkitRunnable
    {
        ResultSet rs;

        public GetAllVarsSyncReturn(ResultSet rs)
        {
            this.rs = rs;
        }

        @Override
        public void run()
        {
            RaffleLogic.initialiseDBVariables(rs);
        }
    }

    /* Entry point for DBUpdateRaffleDrawOnBuy */
    public static void buy(int isNewPlayer, int ticketsBought, int totalTicketsEntered,
                           double money, double totalMoneyEntered, String playerName)
    {
        new DBUpdateTablesOnBuy(plugin, isNewPlayer, ticketsBought, totalTicketsEntered, money,
                totalMoneyEntered, playerName).runTaskAsynchronously(plugin);
    }

    //Updates the RaffleDraw table. To be used when a player buys a ticket.
    private static class DBUpdateTablesOnBuy extends BukkitRunnable
    {
        ShinyRaffle thePlugin;
        int entrants;
        int ticketsBought;
        int totalTicketsEntered;
        double moneySpent;
        double totalMoneyEntered;
        int drawID;
        String playerName;

        DBUpdateTablesOnBuy(ShinyRaffle thePlugin, int entrants, int ticketsBought,
                            int totalTicketsEntered, double moneySpent, double totalMoneyEntered, String playerName)
        {
            this.thePlugin = thePlugin;
            this.entrants = entrants;
            this.ticketsBought = ticketsBought;
            this.totalTicketsEntered = totalTicketsEntered;
            this.moneySpent = moneySpent;
            this.totalMoneyEntered = totalMoneyEntered;
            this.drawID = RaffleLogic.getActiveDraw();
            this.playerName = playerName;
        }

        @Override
        public void run()
        {
            connect();

            //update RaffleDraw
            String updateStatement =
                "Update RaffleDraw " +
                    "Set Entrants = ?, TicketsSold = TicketsSold + ?, "
                    + "TicketFunded = TicketFunded + ?, TotalPrizePool = TotalPrizePool + ? " +
                    "Where DrawID = ?;";

            try
            {
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setInt(1, entrants);
                prep.setInt(2, ticketsBought);
                prep.setDouble(3, moneySpent);
                prep.setDouble(4, moneySpent);
                prep.setInt(5, drawID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                buyUpdateDraw = ex.getMessage();
                return;
            }
        }
    }

    /* Entry point for DBUpdateRaffleDrawFunds. */
    public static void setFunds(double accountFunded, double extraFunded, double totalPrizePool)
    {
        new DBUpdateRaffleDrawFunds(plugin, accountFunded, extraFunded, totalPrizePool).runTaskAsynchronously(plugin);
    }

    //Updates the RaffleDraw table. To be used when a player buys a ticket.
    private static class DBUpdateRaffleDrawFunds extends BukkitRunnable
    {
        ShinyRaffle thePlugin;
        double accountFunded;
        double extraFunded;
        double totalPrizePool;
        int drawID;

        DBUpdateRaffleDrawFunds(ShinyRaffle thePlugin, double accountFunded,
                                double extraFunded, double totalPrizePool)
        {
            this.thePlugin = thePlugin;
            this.accountFunded = accountFunded;
            this.extraFunded = extraFunded;
            this.totalPrizePool = totalPrizePool;
            this.drawID = RaffleLogic.getActiveDraw();
        }

        @Override
        public void run()
        {
            //update RaffleDraw
            String updateStatement =
                "Update RaffleDraw " +
                "Set AccountFunded = ?, ExtraFunded = ?, TotalPrizePool = ? " +
                "Where DrawID = ?;";

            try
            {
                connect();
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setDouble(1, accountFunded);
                prep.setDouble(2, extraFunded);
                prep.setDouble(3, totalPrizePool);
                prep.setInt(4, drawID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                drawUpdateDraw = ex.getMessage();
                return;
            }
        }
    }

    private static Integer getOrCreatePlayerID(String playerName, boolean connectionRequired)
    {
        int playerID = 0;

        String selectCount = "Select Count(PlayerID) as Count From RafflePlayer Where PlayerName = ?";
        String insertPlayer = "Insert Into RafflePlayer Values (?, ?, 0, 0, 0, 0, 0, 0, 0, 0);";
        String selectID = "Select PlayerID From RafflePlayer Where PlayerName = ?";
        ResultSet rs;

        if (connectionRequired)
        {
            connect();
        }

        try
        {
            //if player doesn't exist, insert player
            PreparedStatement prep = connection.prepareStatement(selectCount);
            prep.setString(1, playerName);
            rs = prep.executeQuery();

            while (rs.next())
            {
                if (rs.getInt(1) == 0)
                {
                    prep = connection.prepareStatement(insertPlayer);
                    prep.setInt(1, RaffleLogic.useMaxPlayerID());
                    prep.setString(2, playerName);
                    prep.executeUpdate();
                }
            }

            //player exists, get ID
            prep = connection.prepareStatement(selectID);
            prep.setString(1, playerName);
            rs = prep.executeQuery();

            while (rs.next())
            {
                playerID = rs.getInt(1);
            }
        }
        catch (SQLException ex)
        {
            getOrCreatePlayerID = ex.getMessage();
            return null;
        }

        if (connectionRequired)
        {
            disconnect();
        }

        if (playerID == 0)
        {
            return null;
        }
        else return playerID;
    }

    public static void draw(String winner, String host, double ticketPrice, int buyLimit, double totalPrizePool,
                            Map<String, Object> ticketMap, Map<String, Object> moneyMap)
    {
        new DBDraw(winner, host, ticketPrice, buyLimit, totalPrizePool, ticketMap, moneyMap)
                .runTaskAsynchronously(plugin);
    }

    private static class DBDraw extends BukkitRunnable
    {
        String winner;
        Integer winnerID;
        String host;
        Integer hostID;
        int drawID;
        double ticketPrice;
        int buyLimit;
        double totalPrizePool;
        Map<String, Object> ticketMap;
        Map<String, Object> moneyMap;

        private DBDraw(String winner, String host, double ticketPrice, int buyLimit, double totalPrizePool,
                       Map<String, Object> ticketMap, Map<String, Object> moneyMap)
        {
            this.winner = winner;
            this.host = host;
            this.drawID = RaffleLogic.getActiveDraw();
            this.ticketPrice = ticketPrice;
            this.buyLimit = buyLimit;
            this.totalPrizePool = totalPrizePool;
            this.ticketMap = ticketMap;
            this.moneyMap = moneyMap;
            winnerID = getOrCreatePlayerID(winner, true);
            hostID = getOrCreatePlayerID(host, true);
        }

        @Override
        public void run()
        {
            connect();

            //Set the winner, host and timestamp
            String updateStatement =
                "Update RaffleDraw "
                    + "Set WinnerID = ?, HostID = ?, DateAndTime = ? "
                    + "Where DrawID = ?;";

            Date date = new Date();
            Object myDateTime = new java.sql.Timestamp(date.getTime());

            try
            {
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setInt(1, winnerID);
                prep.setInt(2, hostID);
                prep.setObject(3, myDateTime);
                prep.setInt(4, drawID);
                prep.executeUpdate();

            }
            catch (SQLException ex)
            {
                newDrawUpdateDraw = ex.getMessage();
                return;
            }

            //Update each entrant's applicable info
            String updatePlayerStatement =
                "Update RafflePlayer "
                    + "Set DrawsEntered = DrawsEntered + 1, "
                    + "TotalTicketsBought = TotalTicketsBought + ?, "
                    + "TotalMoneySpent = TotalMoneySpent + ?, "
                    + "MostTicketsEntered = (Case When ? > MostTicketsEntered Then ? Else MostTicketsEntered End), "
                    + "MostMoneyEntered = (Case When ? > MostMoneyEntered Then ? Else MostMoneyEntered End), "
                    + "DrawsWon = (Case When PlayerName = ? Then DrawsWon + 1 Else DrawsWon End), "
                    + "TotalWinnings = (Case When PlayerName = ? Then TotalWinnings + ? Else TotalWinnings End) "
                    + "Where PlayerID = ?;";


            for (Map.Entry<String, Object> entry : ticketMap.entrySet())
            {
                Integer playerID;
                String playerName = entry.getKey();
                int ticketsEntered = (Integer)entry.getValue();
                double moneyEntered = (Double)moneyMap.get(playerName);

                try
                {
                    playerID = getOrCreatePlayerID(playerName, false);

                    //update the player
                    PreparedStatement prep = connection.prepareStatement(updatePlayerStatement);
                    prep.setInt(1, ticketsEntered);
                    prep.setDouble(2, moneyEntered);
                    prep.setInt(3, ticketsEntered);
                    prep.setInt(4, ticketsEntered);
                    prep.setDouble(5, moneyEntered);
                    prep.setDouble(6, moneyEntered);
                    prep.setString(7, winner);
                    prep.setString(8, winner);
                    prep.setDouble(9, totalPrizePool);
                    prep.setInt(10, playerID);
                    prep.executeUpdate();
                }
                catch (SQLException ex)
                {
                    drawEachPlayer = ex.getMessage();
                    return;
                }
            }

            //Insert new raffle
            String insertStatement =
                    "Insert Into RaffleDraw "
                    + "Values (?, 0, ?, 0, ?, 0, 0, 0, 0, null, null, null);";
            try
            {
                PreparedStatement prep = connection.prepareStatement(insertStatement);
                prep.setInt(1, drawID + 1);
                prep.setDouble(2, ticketPrice);
                prep.setInt(3, buyLimit);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                drawInsertDraw = ex.getMessage();
                return;
            }

            disconnect();

            new DBDrawReturn().runTask(plugin);
        }

        public static class DBDrawReturn extends BukkitRunnable
        {
            @Override
            public void run()
            {
                RaffleLogic.drawDBReturn();
            }
        }
    }

    public static void setBuyLimit(int buyLimit)
    {
        new DBSetBuyLimit(buyLimit).runTaskAsynchronously(plugin);
    }

    private static class DBSetBuyLimit extends BukkitRunnable
    {
        int buyLimit;
        int drawID;

        private DBSetBuyLimit(int buyLimit)
        {
            this.buyLimit = buyLimit;
            this.drawID = RaffleLogic.getActiveDraw();
        }

        @Override
        public void run()
        {
            //update RaffleDraw
            String updateStatement = "Update RaffleDraw Set BuyLimit = ? Where DrawID = ?;";

            try
            {
                connect();
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setInt(1, buyLimit);
                prep.setInt(2, drawID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                setBuyLimitUpdateDraw = ex.getMessage();
                return;
            }
        }
    }

    public static void setTicketPrice(double ticketPrice)
    {
        new DBSetTicketPrice(ticketPrice).runTaskAsynchronously(plugin);
    }

    private static class DBSetTicketPrice extends BukkitRunnable
    {
        double ticketPrice;
        int drawID;

        private DBSetTicketPrice(double ticketPrice)
        {
            this.ticketPrice = ticketPrice;
            this.drawID = RaffleLogic.getActiveDraw();
        }

        @Override
        public void run()
        {
            //update RaffleDraw
            String updateStatement = "Update RaffleDraw Set TicketPrice = ? Where DrawID = ?;";

            try
            {
                connect();
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setDouble(1, ticketPrice);
                prep.setInt(2, drawID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                setTicketPriceUpdateDraw = ex.getMessage();
                return;
            }
        }
    }

    //Entry point for GetPlayerStatsAsync
    public static void getPlayerStats(CommandSender sender)
    {
        new GetPlayerStatsAsync(sender).runTaskAsynchronously(plugin);
    }

    private static class GetPlayerStatsAsync extends BukkitRunnable
    {
        CommandSender sender;

        GetPlayerStatsAsync(CommandSender sender)
        {
            this.sender = sender;
        }

        @Override
        public void run()
        {
            connect();
            String selectAll = "Select * From RafflePlayer Where PlayerName = '" + sender.getName() + "'";
            ResultSet rs;

            try
            {
                rs = statement.executeQuery(selectAll);
                new GetPlayerStatsSyncReturn(rs, sender).runTask(plugin);
            }
            catch (SQLException ex)
            {
                playerStatsSelectPlayer = ex.getMessage();
                return;
            }
        }

        private static class GetPlayerStatsSyncReturn extends BukkitRunnable
        {
            ResultSet rs;
            CommandSender sender;

            GetPlayerStatsSyncReturn(ResultSet rs, CommandSender sender)
            {
                this.rs = rs;
                this.sender = sender;
            }

            @Override
            public void run()
            {
                RaffleLogic.statsDBReturn(sender, rs);
            }
        }
    }

    //Entry point for DBUpdateDrawOnRefund
    public static void refund(int entrants, int ticketsSold, double ticketFunded, double totalPrizePool)
    {
        new DBUpdateDrawOnRefund(entrants, ticketsSold, ticketFunded, totalPrizePool).runTaskAsynchronously(plugin);
    }

    private static class DBUpdateDrawOnRefund extends BukkitRunnable
    {
        int drawID;
        int entrants;
        int ticketsSold;
        double ticketFunded;
        double totalPrizePool;

        private DBUpdateDrawOnRefund(int entrants, int ticketsSold, double ticketFunded, double totalPrizePool)
        {
            this.drawID = RaffleLogic.getActiveDraw();
            this.entrants = entrants;
            this.ticketsSold = ticketsSold;
            this.ticketFunded = ticketFunded;
            this.totalPrizePool = totalPrizePool;
        }

        @Override
        public void run()
        {
            String updateStatement = "Update RaffleDraw "
                + "Set Entrants = ?, TicketsSold = ?, TicketFunded = ?, TotalPrizePool = ? Where DrawID = ?;";

            try
            {
                connect();
                PreparedStatement prep = connection.prepareStatement(updateStatement);
                prep.setInt(1, entrants);
                prep.setInt(2, ticketsSold);
                prep.setDouble(3, ticketFunded);
                prep.setDouble(4, totalPrizePool);
                prep.setInt(5, drawID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                refundUpdateDraw = ex.getMessage();
                return;
            }
        }
    }

    //Entry point for DBInsertDonation
    public static void donate(String playerName, double donationAmount)
    {
        new DBInsertDonation(playerName, donationAmount).runTaskAsynchronously(plugin);
    }

    private static class DBInsertDonation extends BukkitRunnable
    {
        Integer playerID;
        int donationID;
        int drawID;
        String playerName;
        double donationAmount;

        private DBInsertDonation(String playerName, double donationAmount)
        {
            this.donationID = RaffleLogic.useNextDonationID();
            this.drawID = RaffleLogic.getActiveDraw();
            this.playerName = playerName;
            this.donationAmount = donationAmount;
            playerID = getOrCreatePlayerID(playerName, true);
        }

        @Override
        public void run()
        {
            //Insert new donation
            String insertDonation =
                    "Insert Into RaffleDonation "
                            + "Select ?, ?, ?, ?;";

            String updatePlayer =
                    "Update RafflePlayer Set TotalDonated = TotalDonated + ? "
                    + "Where PlayerID = ?;";

            connect();
            try
            {
                PreparedStatement prep = connection.prepareStatement(insertDonation);
                prep.setInt(1, donationID);
                prep.setInt(2, drawID);
                prep.setInt(3, playerID);
                prep.setDouble(4, donationAmount);
                prep.executeUpdate();

                prep = connection.prepareStatement(updatePlayer);
                prep.setDouble(1, donationAmount);
                prep.setInt(2, playerID);
                prep.executeUpdate();
            }
            catch (SQLException ex)
            {
                Database.insertDonation = ex.getMessage();
                return;
            }
            disconnect();
        }
    }
}