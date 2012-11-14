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
import org.bukkit.inventory.ItemStack;
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
	private static final int numPaintings = 25;
    
    private KtChestCount chestCounter;
    
	private HashMap<Player, KtNote> tuneMap = new HashMap<Player, KtNote>();
	private Permissions permissions;
	private Zones zones;
	
	public void onEnable() { 		
		permissions = PermissionsResolver.resolve(this);
	    	    
	    PluginManager pm = getServer().getPluginManager();
	    
	    pm.registerEvents(this, this);
	    
		Plugin plugin = pm.getPlugin("Zones");
		zones = (Zones) plugin;
		
		if (zones != null)
			chestCounter = new KtChestCount(this);
		else
			log.severe("No zones plugin means no ktChestCount!");
		
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
            if (zones != null && !zones.getUtils().canBuild(player, target)) {
            	player.sendMessage(ChatColor.RED + "You do not have permissions to rotate this block");
            	return;
            }
            
            byte data = target.getData();
            switch (target.getType()) {
            	case RAILS:
                    target.setData((byte) (data == 9 ? 0 : data + 1));
                    break;
            	case POWERED_RAIL:
            	case DETECTOR_RAIL:
            		target.setData((byte) ((data & 0x7) == 5 ? data & 0x8 : data + 1));
                    break;
            	case WOOD_STAIRS:
            	case SMOOTH_STAIRS:
            	case BRICK_STAIRS:
            	case COBBLESTONE_STAIRS:
            	case NETHER_BRICK_STAIRS:
            	case SPRUCE_WOOD_STAIRS:
            	case BIRCH_WOOD_STAIRS:
            	case JUNGLE_WOOD_STAIRS:
            	case SANDSTONE_STAIRS:
            		target.setData((byte) (data == 7 ? 0 : data + 1));
            		break;
            	case STEP:
            	case WOOD_STEP:
                    int val = target.getType().getId();
                    target.setTypeIdAndData(val, (byte) (data ^ 0x8), true);
                    break;
            	case LOG:
            		target.setData((byte) (data >= 12 ? data - 12 : data + 4));
            		break;
            	case LEVER:
            		byte switched = (byte) (data & 0x8);
            		switch (data & 0x7) {
	            		case 0x5:
	            			target.setData((byte) (0x6 | switched));
	            			break;
	            		case 0x6:
	            			target.setData((byte) (0x5 | switched));
	            			break;
	            		case 0x7:
	            			target.setData((byte) (0x0 | switched));
	            			break;
	            		case 0x0:
	            			target.setData((byte) (0x7 | switched));
	            			break;
            		}
            		break;
            	case PUMPKIN:
            	case JACK_O_LANTERN:
                    target.setData((byte) (data == 4 ? 0 : data + 1));
                    break;
            	case FURNACE:
            	case DISPENSER:
                    target.setData((byte) (data == 5 ? 2 : data + 1));
                    break;
            	case ANVIL:
            		target.setData((byte) (data ^ 0x1));
                    break;
            	case PISTON_BASE:
            	case PISTON_STICKY_BASE:
                    target.setData((byte) (data == 5 ? 0 : data + 1));
                    break;
            	default:
            		break;
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
        if(getZonesPlugin() != null && cmd.getName().equalsIgnoreCase("chestcount")) {
            return chestCounter.chestcountCommand(sender, args);
        }
        return true;
    }
	
	@EventHandler
	public void onSignChangeEvent(SignChangeEvent event) {
		if (zones == null) return;
		
		Player player = event.getPlayer();
		String [] lines = event.getLines();
		
		int result = chestCounter.updateSign(event.getBlock(), player, lines);
		if (result > 0) {
			log.info("ChestCount sign created by " + player.getName() + " @ " + event.getBlock().getLocation());
			player.sendMessage(ChatColor.GREEN + "You created a chest count sign!");
			lines[3] = "" + result;
		} else if (result < KtChestCount.NO_CHESTCOUNT_SIGN ) {
			sendChestCountResultMessage(result, player);
			event.setCancelled(true);
			event.getBlock().breakNaturally();
		}
	}
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		if (zones == null) return;
		
		Block b = event.getBlock();
		if (event.getNewCurrent() == 0) return;
		
		int result = chestCounter.updateSign(b, null, null);
		
		if (result > 0)
			log.info("ChestCount sign update issued by redstone trigger @ " + b.getLocation());
	}
	
	@EventHandler
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
	    if (event.getRightClicked() instanceof CraftPainting) {
	        ItemStack item = event.getPlayer().getItemInHand();
	        if(item == null || item.getTypeId() != Material.DIAMOND_PICKAXE.getId()) return;
	        
	        CraftPainting pt = (CraftPainting) event.getRightClicked();
	        if(getZonesPlugin() != null) {
	            ZoneBase base = getZonesPlugin().getWorldManager(pt.getWorld()).getActiveZone(pt.getLocation());
	            if(base != null && !base.getAccess(event.getPlayer()).canModify()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You cannot do this here!");
	                return;
	            }
	        }
		    
			int t = pt.getArt().getId();
			boolean changed = false;
			
			for (int i = 1; i < numPaintings; ++i)
				if (pt.setArt(Art.getById((t + i) % numPaintings)))
				{
					changed = true;
					break;
				}
				
			if (!changed)
				event.getPlayer().sendMessage(ChatColor.RED + "Unable to change painting");
			else
				event.getPlayer().sendMessage(ChatColor.GREEN + "Changed painting to \"" + pt.getArt().name().toLowerCase() + "\"");
		}
	}
	
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (zones == null) return;
		
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
		if (zones == null) return;
		
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
			case KtChestCount.ERROR_ZONE_SIZE:
			    msg += "Zone too big";
			    break;
			case KtChestCount.ERROR_AMBIGUOUS_ZONE:
			    msg += "Too many zones matching description";
			    break;
			default:
				if (result >= 0)
					msg = ChatColor.GREEN + "Succesfully updated chestcount sign";
				else
					msg += "Unknown error";
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
		if (zones == null) return null;

		return zones.getZoneManager().getSelectedZone(player.getEntityId());
	}
	
	public Zones getZonesPlugin() {
		return zones;
	}
}


