package no.blackjack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import no.blackjack.commands.BlackJackCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import no.blackjack.listeners.BlackJackListener;

public class BlackJackPlugin extends JavaPlugin {
    public static BlackJackPlugin instance;

    private static Economy econ = null;


    public void onEnable() {

        instance = this;

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register command executors

        BlackJackPlugin.instance.registerCommand("blackjack", new BlackJackCommand(), "21", "pontoon");
        // Register event listeners
        getServer().getPluginManager().registerEvents(new BlackJackListener(), this);

        getLogger().info("BlackJack Enabled");
    }

    public void onDisable()
    {
        getLogger().info("BlackJack Disabled");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public void registerCommand(String name, CommandExecutor executor, String... aliases) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);

            PluginCommand command = constructor.newInstance(name, this);

            command.setExecutor(executor);
            command.setAliases(Lists.newArrayList(aliases));

            if (executor instanceof TabCompleter) {
                command.setTabCompleter((TabCompleter) executor);
            }
            this.getCommandMap().register("blackjack", command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CommandMap getCommandMap() {
        try {
            org.bukkit.Server server = Bukkit.getServer();
            Field commandMap = server.getClass().getDeclaredField("commandMap");
            commandMap.setAccessible(true);
            return (CommandMap) commandMap.get(server);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}
