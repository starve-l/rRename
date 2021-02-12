package io.reliances.rrename;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class rRename extends JavaPlugin {

    public static Economy econ = null;
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }
    }

    @Override
    public void onEnable() {
        System.out.println("[rRename] Loading config...");
        this.saveDefaultConfig();
        PluginManager pm = getServer().getPluginManager();
        getConfig().options().copyDefaults(getConfig().contains("version"));
        getConfig().options().copyDefaults(getConfig().contains("appearance.prefix"));
        getConfig().options().copyDefaults(getConfig().contains("appearance.prefix-color"));
        getConfig().options().copyDefaults(getConfig().contains("appearance.color-normal"));
        getConfig().options().copyDefaults(getConfig().contains("appearance.color-error"));
        getConfig().options().copyDefaults(getConfig().contains("economy.enabled"));
        getConfig().options().copyDefaults(getConfig().contains("economy.price"));
        saveConfig();
        rRenameListener listener = new rRenameListener();
        pm.registerEvents(listener, this);
        System.out.println("[rRename] Registering economy...");
        setupEconomy();
    }

    @Override
    public void onDisable() {  }

    FileConfiguration config = getConfig();

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {


        String prefix = ChatColor.GRAY + "[" + ChatColor.of("#" + config.getString("appearance.prefix-color")) + config.getString("appearance.prefix") + ChatColor.GRAY + "] ";
        ChatColor colorNormal = ChatColor.of("#" + config.getString("appearance.color-normal"));
        ChatColor colorError = ChatColor.of("#" + config.getString("appearance.color-error"));
        String version = config.getString("version");

        Player player = (Player) sender;
        if (player == null) return true;

        if (sender instanceof Player) {
            if (args.length == 0) {
                player.sendMessage(prefix + colorNormal + "Version " + version + " by reliances.");
                return true;
            }
        }

        switch (args[0]) {
            case "reload":
                if (player.hasPermission("rrename.reload") || player.isOp()) {
                    this.reloadConfig();
                    config = this.getConfig();
                    player.sendMessage(prefix + colorNormal + "Config successfully reloaded.");
                } else {
                    player.sendMessage(prefix + colorError + "You don't have the required permission node: " + ChatColor.GRAY + "rrename.reload");
                }
                break;
            case "info":
                player.sendMessage(prefix + colorNormal + "Version " + version + " by reliances.");
                break;
            default:
                if (player.getInventory().getItemInMainHand().getType().isAir()) {
                    player.sendMessage(prefix + colorError + "You need to hold an item/block to rename!");
                    break;
                }
                if (config.getBoolean("economy.enabled")) {
                    if (    player.hasPermission("rrename.bypass") || !player.isOp()) {
                        double bal = econ.getBalance(player);
                        if (bal < config.getInt("economy.price")) {
                            player.sendMessage(prefix + colorError + "You have insufficient funds! You need $" + config.getInt("economy.price") + " to rename items.");
                            break;
                        } else {
                            econ.withdrawPlayer(player, config.getInt("economy.price"));
                        }
                    }
                }
                String[] reSplitArgs;
                String finalArgs = "";
                String joinedArgs = "";
                for (int i = 0; i < args.length; i++) {
                    joinedArgs += args[i] + " ";
                }
                joinedArgs = joinedArgs.substring(0, joinedArgs.length() - 1);
                reSplitArgs = joinedArgs.split("&#");
                finalArgs += reSplitArgs[0];
                for (int i = 1; i < reSplitArgs.length; i++) {
                    String color = reSplitArgs[i].substring(0, 6);
                    if (color.matches("^[A-Fa-f0-9]{6}"))
                        finalArgs += ChatColor.of("#" + color) + reSplitArgs[i].substring(6);
                    else {
                        finalArgs += reSplitArgs[i].substring(6);
                    }
                }
                ItemStack is = player.getInventory().getItemInMainHand();
                ItemMeta im = is.getItemMeta();
                String formattedFinalArgs = ChatColor.translateAlternateColorCodes('&', finalArgs);
                assert im != null;
                im.setDisplayName(ChatColor.RESET + formattedFinalArgs);
                is.setItemMeta(im);
                player.updateInventory();
                player.sendMessage(prefix + colorNormal + "Item successfully renamed to " + ChatColor.RESET + formattedFinalArgs);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete (CommandSender sender, Command cmd, String label, String[]args){
        List<String> completions = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("rrename.reload")) commands.add("reload");
            if (sender.hasPermission("rrename.rename")) commands.add("[name]");
            commands.add("info");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        }
        Collections.sort(completions);
        return completions;
    }

}
