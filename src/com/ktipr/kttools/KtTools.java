package com.ktipr.kttools;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Art;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.block.CraftNoteBlock;
import org.bukkit.craftbukkit.entity.CraftPainting;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        if(event.getName().equals("blockrotate")) {
        	event.setCancelled(true);
        	
        	Player player = event.getPlayer();
            if(!canUse(event.getPlayer(), "blockrotate")) {
            	player.sendMessage(ChatColor.RED + "You do not have permissions to rotate blocks");
            	return;
            }
            
            Block target = event.getTriggerEvent().getClickedBlock();
            //Joseph kijkt hier naar:
            if (zones != null && !zones.getUtils().canBuild(player, target)) {
            	player.sendMessage(ChatColor.RED + "You do not have permissions to rotate this block");
            	return;
            }
            
            if (target.getType() == Material.RAILS) {
                byte data = target.getData();
                target.setData((byte) (data == 9 ? 0 : data + 1));
                return;
            }
            
            if (target.getType() == Material.POWERED_RAIL ||
                target.getType() == Material.DETECTOR_RAIL) {
                byte data = target.getData();
                byte flag = (byte) (0x8 & data);
                byte rest = (byte) (0x7 & data);
                target.setData((byte) ((byte) (rest == 5 ? 0 : rest + 1) | flag));
                return;
            }
            
            if (target.getType() == Material.WOOD_STAIRS ||
                    target.getType() == Material.SMOOTH_STAIRS ||
                    target.getType() == Material.BRICK_STAIRS ||
                    target.getType() == Material.COBBLESTONE_STAIRS ||
                    target.getType() == Material.NETHER_BRICK_STAIRS) {
                byte data = target.getData();
                target.setData((byte) (data == 7 ? 0 : data + 1));
                return;
            }

            if (target.getType() == Material.STEP) {
                byte data = target.getData();
                int val = target.getType().getId();
                data = (byte) (data > 7 ? data - 8 : data + 8);
                target.setTypeIdAndData(val, data, true);
                return;
            }
        }
    }
	
	@EventHandler
	public void tune(EasyBindEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Player player = event.getPlayer();
                
        if(event.getName().equals("tune")) {
            if(!canUse(player, "tune")) return;
            
            Block target = event.getTriggerEvent().getClickedBlock();
            if (target.getType() != Material.NOTE_BLOCK) return;
            
            if (zones != null && !zones.getUtils().canModify(player, target)) {
            	player.sendMessage(ChatColor.RED + "You do not have permissions to tune this noteblock");
            	event.setCancelled(true);
            	return;
            }
            
            if (hasNote(player)) {
                KtNote note = getNote(player);
                ((CraftNoteBlock) target.getState()).setNote(note);
                player.sendMessage("Set note to " + note);
            } else {
                player.sendMessage("Please choose a note first with /tune");
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
	
	@EventHandler
	public void onSignChangeEvent(SignChangeEvent event) {	
		int result = chestCounter.updateSign(event.getBlock(), event.getPlayer(), event.getLines());
		if (result > 0) {
			log.info("ChestCount sign created by " + event.getPlayer().getName() + " @ " + event.getBlock().getLocation());
			event.getPlayer().sendMessage(ChatColor.GREEN + "You created a chest count sign!");
			event.setLine(3, "" + result);
		} else if (result < KtChestCount.NO_CHESTCOUNT_SIGN ) {
			sendChestCountResultMessage(result, event.getPlayer());
			event.setCancelled(true);
			event.getBlock().breakNaturally();
		}
	}
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		Block b = event.getBlock();
		if (event.getNewCurrent() == 0) return;
		
		int result = chestCounter.updateSign(b, null, null);
		
		if (result > 0)
			log.info("ChestCount sign update issued by redstone trigger @ " + b.getLocation());
	}
	
	@EventHandler
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof CraftPainting) 
		{
			CraftPainting pt = (CraftPainting) event.getRightClicked();
			int t = pt.getArt().getId();
			t = (t+1)%25;
			while(!pt.setArt(Art.getById(t)))
				t = (t+1)%25;
			
			event.getPlayer().sendMessage(ChatColor.GREEN + "Changed painting to \"" + pt.getArt().name().toLowerCase() + "\"");
		}
	}
	
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		
		Block b = event.getClickedBlock();
		if (b.getTypeId() != 63 && b.getTypeId() != 68) return;
		//event.setCancelled(true);

		int result = chestCounter.updateSign(b, event.getPlayer(), null);
		if (result > 0)
			log.info("ChestCount sign update issued by " + event.getPlayer().getName() + " @ " + b.getLocation());
		
		sendChestCountResultMessage(result, event.getPlayer());
	}
	
	private void sendChestCountResultMessage(int result, Player player) {
		String msg = ChatColor.RED + "Error updating chest count sign: ";
		switch (result) {
			case KtChestCount.NO_SIGN:
			case KtChestCount.NO_CHESTCOUNT_SIGN:
				return;
			case KtChestCount.ERROR_INVALID_SEARCH_ITEM:
				msg += "Invalid search item";
				break;
			case KtChestCount.ERROR_NO_ZONES_PLUGIN:
				msg += "No zones plugin found";
				break;
			case KtChestCount.ERROR_NO_ZONE:
				msg += "Sign must be placed in a zone";
				break;
			case KtChestCount.ERROR_NO_RIGHTS_IN_ZONE:
				msg += "You do not have access to chests in this zone";
				break;
			case KtChestCount.ERROR_UPDATE_TOO_SOON:
				msg += "You cannot update so often!";
				break;
			default:
				msg = ChatColor.GREEN + "Succesfully updated chestcount sign";
		}
		player.sendMessage(msg);
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
            player.sendMessage(ChatColor.GREEN + "Tunefork set at " + note);
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


