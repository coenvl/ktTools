package com.ktipr.kttools;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.block.CraftSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.zones.Zones;
import com.zones.model.ZoneBase;
import com.zones.model.ZoneForm;
import com.zones.util.FileUtil;

public class KtChestCount {
	static final int NO_SIGN = -1;
	static final int NO_CHESTCOUNT_SIGN = -2;
	static final int ERROR_INVALID_SEARCH_ITEM = -3;
	static final int ERROR_NO_ZONES_PLUGIN = -4;
	static final int ERROR_NO_ZONE = -5;
	static final int ERROR_NO_RIGHTS_IN_ZONE = -6;
	static final int ERROR_UPDATE_TOO_SOON = -7;
	static final int ERROR_ZONE_SIZE = -8;
	static final int ERROR_AMBIGUOUS_ZONE = -9;
	
	static final int MAX_ZONE_SIZE = 50000;
	
	private final Logger log = Logger.getLogger("Minecraft");

	private HashMap<String, Integer> itemDb = new HashMap<String, Integer>();
	private HashMap<Integer, Long> signUpdateTime = new HashMap<Integer, Long>();
	private KtTools ktTools;
	
	public KtChestCount(KtTools ktTools) {
		this.ktTools = ktTools;
		if (ktTools.getZonesPlugin() == null)
		{
			log.info("No zones plugin -> no ktChestcount");
			return;
		}
		
		loadItemDb();
		
		FileConfiguration config = ktTools.getConfig();
		config.options().copyDefaults(true);
		ktTools.saveConfig();
	}

	//This is actually the wrapper
	public boolean chestcountCommand(CommandSender sender, String[] args) {
		//Who runs the command
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command should be used as a player");
			return false;
		}
		
		Player player = (Player) sender;
		
		//Can the player run the chestcount command
		if (!ktTools.canUse(player, "chestcount"))
		{
			player.sendMessage(ChatColor.RED + "You do not have the permissions to use this command");
			return true;
		}

		if (args.length < 1 || args.length > 2) {
			player.sendMessage(ChatColor.RED + "Usage: /chestcount blockid [zone]");
			return true;
		}

		//Get zone
		ZoneBase b = null;
		if (args.length == 2)
		{
			List<ZoneBase> zonelist = ktTools.getZonesPlugin().getZoneManager().matchZone(player, args[1]);
					
			if (zonelist.isEmpty())
			{
				player.sendMessage(ChatColor.RED + "No zone found matching description");
				return true;			
			}
				
			if (zonelist.size() == 1)
				b = zonelist.get(0);
			
			if (zonelist.size() > 1)
			{
				player.sendMessage(ChatColor.RED + "Too many zones found matching description");
				return true;			
			}
		}
		else //If not successful use old style
			b = ktTools.getZoneBaseByPlayer(player);
		
		//Get the selected zone, if necessary select one now
		if (b == null) {
		    b = ktTools.getZonesPlugin().getWorldManager(player.getWorld()).getActiveZone(player);
		    if (b == null)
		    {
		    	player.sendMessage(ChatColor.RED + "No zones found for chestcount");
		    	return true;
		    }
		    ktTools.getZonesPlugin().getZoneManager().setSelected(player.getEntityId(), b.getId());
		}

		if(b.getForm().getSize() >= (MAX_ZONE_SIZE * 2)) {
		    player.sendMessage(ChatColor.RED + "Zone too big");
		    return true;
		}
		
		//Get requested block type
		ItemStack searchBlock = getBlockType(args[0]);
		if (searchBlock == null) {
			player.sendMessage(ChatColor.RED + "Unable to search for item " + args[0]);
			return true;
		}
		
		log.info("ChestCount command called by " + player.getName() + " for zone " + b.getName());
		
		//Count
		int [] chestCount = countItemsInZone(searchBlock, b);
				
		//Report
		player.sendMessage(ChatColor.GREEN + "Found a total of " + chestCount[0] + " " + 
					searchBlock.getType().name().toLowerCase() + " stored in " +
					chestCount[1] + (chestCount[1] == 1 ? " chest " : " chests ") + 
					"in zone '" + b.getName() + "'");

		return true;        
	}

	public int [] countItemsInZone(ItemStack searchBlock, ZoneBase b)
	{	
		ZoneForm f = b.getForm();
		int count = 0;
		int chestCount = 0;
		boolean chestContainsItem;
		
		for (int x = f.getLowX(); x <= f.getHighX(); x++) {
			for (int y = f.getLowY(); y <= f.getHighY(); y++) {
				for (int z = f.getLowZ(); z <= f.getHighZ(); z++) {
					
					//If the block is a chest in the zone, check it's content
					if (b.isInsideZone(x, y, z) && b.getWorld().getBlockTypeIdAt(x, z, y) == 54) {
        				Block current = b.getWorld().getBlockAt(x, z, y);

						chestContainsItem = false;
						for (ItemStack item : getSingleBlockInventory(current).getContents())
							if (isEqualItem(item, searchBlock))
							{
								count += item.getAmount();
								chestContainsItem = true;
							}
						
						chestCount += (chestContainsItem ? 1 : 0);
					}
				
				} //z
			} //y
		} //x
		
		int [] ret = {count, chestCount};
		
		return ret;
	}
	
	//Get the contents of a single chest block
	private Inventory getSingleBlockInventory(Block block) {
		if (block.getTypeId() != 54)
			return null;

		Chest chest = (Chest)block.getState();
		return chest.getBlockInventory();
	}

	//Get the block type of a search string (function from Meaglin's CraftTrade)
	public ItemStack getBlockType(String type) {        
		type = type.replace("_", "");
		type = type.replace(";", ":");
		type = type.replace("|", ":");

		String[] args = type.split(":", 2);
		if(args.length == 0) return null;

		int itemId = -1;
		byte data = 0;

		try {
			itemId = Integer.parseInt(args[0]);
		} catch(NumberFormatException e) {
			itemId = -1;
		}

		if(itemId < 0) {
			String name = args[0].toLowerCase();
			if(itemDb.containsKey(name)) {
				itemId = itemDb.get(name);
				data = (byte) (itemId >>> 16);
				itemId &= 0xFFFF;
			} else {
				for(Entry<String, Integer> entry : itemDb.entrySet()) {
					int distance = StringUtils.getLevenshteinDistance(name, entry.getKey());
					if(distance == 1) {
						itemId = entry.getValue() & 0xFFFF;
						data = (byte) (entry.getValue() >>> 16);
						break;
					}
				}
			}
		}

		if(itemId < 1 || itemId > Short.MAX_VALUE) return null; // We dind't get anything proper.

		if(args.length == 2) {
			try {
				data = Byte.parseByte(args[1]);
			} catch (NumberFormatException e) {
				String name = args[1].toLowerCase();
				if(name.equals("*")) data = -1;
			}
		}

		if(data < -1 || data > 15) data = 0; // Avoid bogus data.

		return new ItemStack(itemId, 0 , data);
	}

	//Load a database of item names (function from Meaglin's CraftTrade)
	public void loadItemDb() {
		itemDb.clear();

		File itemfile = new File(ktTools.getDataFolder(), "items.csv");
		String[] lines = FileUtil.readLines(itemfile);
		String[] parts = new String[3];
		for(int i = 0; i < lines.length;i++) {
			if(lines[i].charAt(0) == '#') continue;
			parts = lines[i].split(",", 3);
			if(parts.length != 3) continue;
			int id = -1;
			byte data = -1;
			try {
				id = Integer.parseInt(parts[1]);
				data = Byte.parseByte(parts[2]);
			} catch (NumberFormatException e) {
				continue;
			}
			if(id < 1 || id > Short.MAX_VALUE || data < 0 || data > 15) continue;
			int item = (id & 0xFFFF ) | (data << 16);
			String itemName = parts[0].toLowerCase();

			if(itemDb.containsKey(itemName)) {
				log.warning("[ktTools] Duplicate itemDB Entry: (" + i + ")" + lines[i] + "!");
				continue;
			}

			itemDb.put(itemName, item);
		}
	}

	//Check if an itemstack is of same type
	private boolean isEqualItem(ItemStack item1, ItemStack item2) {
		if (item1 == null || item2 == null)
			return false;
		
		if (item1.getDurability() == -1 || item2.getDurability() == -1)
			return item1.getTypeId() == item2.getTypeId();
		else
			return item1.getTypeId() == item2.getTypeId() && item1.getDurability() == item2.getDurability();
	}

	public int updateSign(Block b, Player player, String[] lines) {
		//Only update a real sign	
		if (b.getTypeId() != 63 && b.getTypeId() != 68) return NO_SIGN;
		
		CraftSign sign = (CraftSign) b.getState();
		
		if (lines == null)
			lines = sign.getLines();		
		
		//Only check chestcount signs
		if (lines[0] == null || !lines[0].equalsIgnoreCase("[chestcount]")) return NO_CHESTCOUNT_SIGN;
		
		Long lastUpdateTime = signUpdateTime.get(b.hashCode());
		Long currentTime = (new Date()).getTime();
		int minSignUpdateInterval = ktTools.getConfig().getInt("chestCount.updateTimeout");
		if (lastUpdateTime != null && lastUpdateTime + minSignUpdateInterval > currentTime) {
			return ERROR_UPDATE_TOO_SOON;
		}
		signUpdateTime.put(b.hashCode(), currentTime);
		
		//Check if search block is valid
		ItemStack searchBlock = getBlockType(lines[1]);
		if (searchBlock == null) {
			sign.setLine(3, ChatColor.DARK_RED + "UNKNOWN ITEM!");
			sign.update();
			return ERROR_INVALID_SEARCH_ITEM;
		}
		
		//Check if valid zone
		Zones zoneplugin = ktTools.getZonesPlugin();
		if (zoneplugin == null)
		{
			sign.setLine(3, ChatColor.DARK_RED + "NO ZONE PLUGIN!");
			sign.update();
			return ERROR_NO_ZONES_PLUGIN;
		}
				
		//Get zone
		ZoneBase zone = null;
		if (lines[2] != null && !lines[2].equals(""))
		{
			List<ZoneBase> zonelist;
			
			if (player != null)
				zonelist = zoneplugin.getZoneManager().matchZone(player, lines[2]);
			else
				zonelist = zoneplugin.getZoneManager().matchZone(lines[2]);
					
			if (zonelist.isEmpty())
				return ERROR_NO_ZONE;
			
			if (zonelist.size() == 1)
				zone = zonelist.get(0);
			
			if (zonelist.size() > 1)
				return ERROR_AMBIGUOUS_ZONE;
		}
		
		//If not successful use old style
		if (zone == null)
			zone = zoneplugin.getUtils().getActiveZone(b.getLocation());
			
		if (zone == null) {
			sign.setLine(3, ChatColor.DARK_RED + "NO ZONE FOUND!");
			sign.update();
			return ERROR_NO_ZONE;
		}
		
		//sign.setLine(2, zone.getName());
		//sign.update();
		
		if (zone.getForm().getSize() >= (player != null ? (MAX_ZONE_SIZE*2) : MAX_ZONE_SIZE )) {
		    sign.setLine(3, ChatColor.DARK_RED + "TOO BIG ZONE!");
		    sign.update();
		    return ERROR_ZONE_SIZE;
		}
		
		if (player != null && !zone.getAccess(player).canModify())
			return ERROR_NO_RIGHTS_IN_ZONE;
		
		//And finally do the real thing
		
		int [] results = countItemsInZone(searchBlock, zone);
		sign.setLine(3, "" + results[0]);		
		sign.update();
		
		return results[0];
		
	}
}
