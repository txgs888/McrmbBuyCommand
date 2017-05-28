package com.mcrmb.mcrmbbuycommand;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public final class McrmbBuyCommand extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Mcrmb");
        int version = Integer.parseInt(plugin.getDescription().getVersion().replace(".", ""));
        if (version < 109) {//如果为1.0.8或更老版本的Mcrmb,就以非静态方法调用
            payStatic = false;
        }
    }

    private boolean payStatic = true;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("notPermission")));
            return true;
        }
        if (args.length < 4) {
            return false;
        }
        String player = args[0];
        int price = Integer.parseInt(args[1]);
        String reason = args[2];

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    synchronized (args[0]) {
                        StringBuilder commandBuilder = new StringBuilder();
                        for (int i = 3; i < args.length; i++) {
                            commandBuilder.append(args[i]).append(" ");
                        }
                        if (buy(player, price, reason)) {
                            for (String commandString : commandBuilder.toString().split(";")) {
                                String cmd = commandString.replace("{player}", player);
                                Player targetPlayer = Bukkit.getPlayer(player);
                                String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString("success"));
                                if (commandString.startsWith("op:") && targetPlayer != null && targetPlayer.isOnline()) {//如果指定以OP身份执行
                                    cmd = cmd.substring(3, cmd.length());
                                    getLogger().info("玩家 " + player + " 购买 " + reason + " 以OP身份执行命令: /" + cmd);
                                    boolean hasOp = targetPlayer.isOp(); //玩家执行命令前是否是OP
                                    try {
                                        targetPlayer.setOp(true);
                                        targetPlayer.chat("/" + cmd);
                                        if (!hasOp) {
                                            targetPlayer.setOp(false);//如果玩家执行命令前不是OP,就取消掉OP
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        if (!hasOp) {
                                            targetPlayer.setOp(false); //防止执行时出错导致OP没有正常被取消
                                        }
                                    }
                                    targetPlayer.sendMessage(message);
                                } else {//否则以后台执行
                                    getLogger().info("玩家 " + player + " 购买 " + reason + " 后台执行命令: /" + cmd + "  执行结果: " + Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                                    sender.sendMessage(message);
                                }
                                return;

                            }
                        } else {
                            Player targetPlayer = Bukkit.getPlayer(player);
                            String message = ChatColor.translateAlternateColorCodes('&', getConfig().getString("failed"));
                            if (targetPlayer != null && targetPlayer.isOnline()) {
                                targetPlayer.sendMessage(message);
                            } else {
                                sender.sendMessage(message);
                            }
                            return;
                        }

                    }


                }
        );
        return true;
    }

    public boolean buy(String player, int money, String reason) {
        try {
            Class payApi = Class.forName("com.mcrmb.PayApi");
            Method method = payApi.getMethod("Pay", String.class, String.class, String.class, boolean.class);//反射获取方法
            if (payStatic) {//使用静态方法调用Mcrmb
                return (Boolean) method.invoke(null, player, String.valueOf(money), reason, false);
            } else {//使用非静态调用(老版本Mcrmb)
                return (Boolean) method.invoke(new com.mcrmb.PayApi(), player, String.valueOf(money), reason, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
