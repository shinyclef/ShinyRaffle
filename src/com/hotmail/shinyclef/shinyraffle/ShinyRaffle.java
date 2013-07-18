package com.hotmail.shinyclef.shinyraffle;

import net.milkbowl.vault.economy.Economy;
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

    @Override
    public void onEnable()
    {
        log = this.getLogger();

        //if no economy is found, disable this plugin with a message
        if (!setupEconomy())
        {
            log.info("SEVERE!!! DISABLING PLUGIN DUE TO NO VAULT ECONOMY FOUND!");
            getServer().getPluginManager().disablePlugin(this);
            return;
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
}
