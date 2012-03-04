package com.ktipr.kttools;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftNoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.easybind.listeners.EasyBindEvent;
import com.easybind.permissions.Permissions;
import com.easybind.permissions.PermissionsResolver;
import com.zones.Zones;
import com.zones.model.ZoneBase;

public class KtTools extends JavaPlugin implements Listener, CommandExecutor {

	private final Logger log = Logger.getLogger("Minecraft");
	private static final String prefix = "kttools.";
    
    private KtChestCount chestCounter;
	
	private HashMap<Player, KtNote> tuneMap = new HashMap<Player, KtNote>();
	private Permissions permissions;
	private Zones zones;
	
	public void onEnable() { 
		chestCounter = new KtChestCount(this);
		
		permissions = PermissionsResolver.resolve(this);
	    	    
	    PluginManager pm = getServer().getPluginManager();
	    
	    pm.registerEvents(this, this);
	    
		Plugin plugin = pm.getPlugin("Zones");
		zones = (Zones) plugin;
		
		log.info("Ktipr's tools have been enabled! Rock on Mister!");
	}
	 
	public void onDisable(){ 
		log.info("Ktipr's tools have been disabled! :( Sad ktipr");
	}

	@EventHandler
	public void blockRotate(EasyBindEvent event) {
        if(event.isCancelled()) return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        if(event.getName().equals("blockrotate")) {
            if(!canUse(event.getPlayer(), "rotate")) return;
            
            Block target = event.getTriggerEvent().getClickedBlock();
            
            if (target.getType() == Material.RAILS) {
                byte data = target.getData();
                target.setData((byte) (data == 9 ? 0 : data + 1));
                event.setCancelled(true);
                return;
            }
            
            if (target.getType() == Material.POWERED_RAIL ||
                target.getType() == Material.DETECTOR_RAIL) {
                byte data = target.getData();
                byte flag = (byte) (0x8 & data);
                byte rest = (byte) (0x7 & data);
                target.setData((byte) ((byte) (rest == 5 ? 0 : rest + 1) | flag));
                event.setCancelled(true);
                return;
            }
            
            if (target.getType() == Material.WOOD_STAIRS ||
                    target.getType() == Material.SMOOTH_STAIRS ||
                    target.getType() == Material.BRICK_STAIRS ||
                    target.getType() == Material.COBBLESTONE_STAIRS ||
                    target.getType() == Material.NETHER_BRICK_STAIRS) {
                byte data = target.getData();
                target.setData((byte) (data == 7 ? 0 : data + 1));
                event.setCancelled(true);
                return;
            }
            
            if (target.getType() == Material.STEP) {
                byte data = target.getData();
                target.setData((byte) (data > 7 ? data - 8 : data + 8));
                event.setCancelled(true);
                return;
            }
        }
    }
	
	@EventHandler
	public void tune(EasyBindEvent event) {
        if(event.isCancelled()) return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
        
        if(event.getName().equals("tune")) {
            if(!canUse(player, "tune")) return;
            
            Block target = event.getTriggerEvent().getClickedBlock();
            if (target.getType() != Material.NOTE_BLOCK) return;
            
            if (hasNote(player)) {
                KtNote note = getNote(player);
                ((CraftNoteBlock) target.getState()).setNote(note);
                player.sendMessage("Set note to " + note);
                event.setCancelled(true);
            } else {
                player.sendMessage("Please configure you're note first with /note");
            }
        }
    }
	
	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
        if(cmd.getName().equalsIgnoreCase("tune")) {
            return tuneCommand(sender, cmd, commandLabel, args);
        }
        if(cmd.getName().equalsIgnoreCase("chestcount")) {
            return chestCounter.chestcountCommand(sender, args);
        }
        return true;
    }
	
	public boolean tuneCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command should be used as a player");
            return true;
        }
        Player player = (Player) sender;
        if (!canUse(player, "tune")) return false;
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /tune [pitch]");
            return true;
        }
        
        byte pitch;
        try {
            pitch = Byte.parseByte(args[0]);
        } catch (NumberFormatException err) {
            player.sendMessage(ChatColor.RED + "Invalid argument, pitch must be a number.");
            return true;
        }
        
        if (pitch < 0 || pitch > 24) {
            player.sendMessage(ChatColor.RED + "Invalid pitch, must be in interval [0;24]");
        } else {
            KtNote note = new KtNote(pitch);
            setNote(player, note);
            player.sendMessage("Tunefork set at " + note);
        }
        return true;
	}

	public boolean canUse(Player player, String node) {
	    return permissions.canUse(player, prefix + node);
	}
	
	public KtNote getNote(Player player) {
		if (tuneMap.containsKey(player))
			return tuneMap.get(player);
		else
			return null;
	}
	
	public KtNote setNote(Player player, KtNote note) {
		return tuneMap.put(player, note);
	}
	
	public boolean hasNote(Player player) {
		return tuneMap.containsKey(player);
	}
	
	public ZoneBase getZoneBaseByPlayer(Player player) {
		return zones.getZoneManager().getSelectedZone(player.getEntityId());
	}
	
	public Zones getZonesPlugin() {
		return zones;
	}
}


