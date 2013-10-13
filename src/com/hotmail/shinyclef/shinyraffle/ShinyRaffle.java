package com.hotmail.shinyclef.shinyraffle;

import com.hotmail.shinyclef.shinybridge.ShinyBridge;
import com.hotmail.shinyclef.shinybridge.ShinyBridgeAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Author: Shinyclef
 * Date: 17/07/12
 * Time: 9:18 PM
 */

public class ShinyRaffle extends JavaPlugin
{
    Logger log;
    private Economy economy = null;
    private Server s;
    private ShinyBridgeAPI bridge = null;
    private boolean haveBridge = false;

    @Override
    public void onEnable()
    {
        log = this.getLogger();
        s = getServer();

        //if no economy is found, disable this plugin with a message
        if (!setupEconomy())
        {
            log.info("SEVERE!!! DISABLING PLUGIN DUE TO NO VAULT ECONOMY FOUND!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //get ShinyBridge
        Plugin bridgePlugin = Bukkit.getPluginManager().getPlugin("ShinyBridge");
        if (bridgePlugin != null)
        {
            ShinyBridge shinyBridge = (ShinyBridge) bridgePlugin;
            bridge = shinyBridge.getShinyBridgeAPI();
            haveBridge = true;
        }

        CmdExecutor commandExecutor = new CmdExecutor();
        getCommand("raffle").setExecutor(commandExecutor);
        getCommand("raf").setExecutor(commandExecutor);
        Database.setupConnection(this);
        Database.onPluginLoad();
        RaffleLogic.initializeVariables(this);
    }

    @Override
    public void onDisable(){}

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null)
        {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    public Economy getEconomy()
    {
        return economy;
    }


    /* Shiny bridge routing */

    public void broadcastMessage(String message)
    {
        //send to bridge clients if we have bridge
        if (haveBridge)
        {
            bridge.broadcastMessage(message, true);
        }
        else
        {
            //standard broadcast
            s.broadcastMessage(message);
        }
    }

    public void broadcastPermissionMessage(String message, String permission)
    {
        //send to bridge clients if we have bridge
        if (haveBridge)
        {
            bridge.broadcastMessage(message, permission, true);
        }
        else
        {
            //standard broadcast
            s.broadcast(message, permission);
        }
    }
}
