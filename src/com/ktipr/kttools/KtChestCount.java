package com.ktipr.kttools;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.zones.command.GeneralCommands;
import com.zones.model.ZoneBase;
import com.zones.model.ZoneForm;
import com.zones.util.FileUtil;

public class KtChestCount {
	private final Logger log = Logger.getLogger("Minecraft");

	private HashMap<String, Integer> itemDb = new HashMap<String, Integer>();
	private KtTools ktTools;

	public KtChestCount(KtTools ktTools) {
		this.ktTools = ktTools;
		loadItemDb();
	}

	public boolean chestcountCommand(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command should be used as a player");
			return true;
		}
		Player player = (Player) sender;
		if (!ktTools.canUse(player, "chestcount")) return false;

		if (args.length != 1) {
			player.sendMessage(ChatColor.RED + "Usage: /chestcount [blockid]");
			return true;
		}

		ItemStack searchBlock = getBlockType(args[0]);
		if (searchBlock == null) {
			player.sendMessage(ChatColor.RED + "Unable to search for item " + args[0]);
			return true;
		}

		ZoneBase b = ktTools.getZoneBaseByPlayer(player);
		if (b == null) {
			GeneralCommands zonecommands = new GeneralCommands(ktTools.getZonesPlugin());
			zonecommands.select(player, new String[0]);
			b = ktTools.getZoneBaseByPlayer(player);
			if (b == null)
				return true; //Error message is already shown by zones
		}

		ZoneForm f = b.getForm();

		int count = 0;
		int chestCount = 0;
		boolean chestContainsItem;
		
		for (int x = f.getLowX(); x < f.getHighX(); x++) {
			for (int y = f.getLowY(); y < f.getHighY(); y++) {
				for (int z = f.getLowZ(); z < f.getHighZ(); z++) {

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

		player.sendMessage(ChatColor.GREEN + "Found a total of " + count + " " + 
					searchBlock.getType().name().toLowerCase() + " stored in " +
					chestCount + (chestCount == 1 ? " chest " : " chests ") + 
					"in zone '" + b.getName() + "'");

		return true;        
	}

	private Inventory getSingleBlockInventory(Block block) {
		if (block.getTypeId() != 54)
			return null;

		Chest chest = (Chest)block.getState();
		Inventory inventory = chest.getInventory();
		if(inventory instanceof DoubleChestInventory) { 
			DoubleChestInventory dinv = ((DoubleChestInventory)inventory);
			if(dinv.getLeftSide().getHolder().equals(chest)) {
				return dinv.getLeftSide();
			} else {
				return dinv.getRightSide();
			}
		}
		return inventory;
	}

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

	private boolean isEqualItem(ItemStack item1, ItemStack item2) {
		if (item1 == null || item2 == null)
			return false;

		return item1.getTypeId() == item2.getTypeId() && item1.getDurability() == item2.getDurability();
	}
}
