package com.ktipr.kttools.perfecttune;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.ktipr.kttools.KtTools;

public class PerfectTuneCommandExecutor implements CommandExecutor {

    private final KtTools plugin;
    
    public PerfectTuneCommandExecutor(KtTools plugin) {
        this.plugin = plugin;
    }
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		//On command /tune do:
		if(cmd.getName().equalsIgnoreCase("tune")) {
			if (sender instanceof Player) {
				Player player = (Player) sender;
				
				//See if the user has rights
				if (!plugin.canUse(player, "tune")) return false;
				if (args.length != 1) {
					player.sendMessage(ChatColor.RED + "Usage: /tune [pitch]");
					return true;
				}
				
				//Parse the argument
				byte pitch;
				try {
					pitch = Byte.parseByte(args[0]);
				} catch (NumberFormatException err) {
					player.sendMessage(ChatColor.RED + "Invalid argument, pitch must be a number.");
					return true;
				}
				
				//If pitch is in range, remember
				if (pitch < 0 || pitch > 24) {
					player.sendMessage(ChatColor.RED + "Invalid pitch, must be in interval [0;24]");
				} else {
					KtNote note = new KtNote(pitch);
					plugin.setNote(player, note);
					player.sendMessage("Tunefork set at " + note);
				}
				
			} else {
				sender.sendMessage(ChatColor.RED + "This command should be used as a player");
			}
			return true;
		}
		
		return false; //command name was not tune
	}
	
}
