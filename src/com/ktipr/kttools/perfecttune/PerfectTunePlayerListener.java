package com.ktipr.kttools.perfecttune;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftNoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

import com.ktipr.kttools.KtTools;

public class PerfectTunePlayerListener extends PlayerListener {
	
	Logger log = Logger.getLogger("Minecraft");
	
	private final KtTools plugin;

	public PerfectTunePlayerListener(KtTools plugin) {
        this.plugin = plugin;
    }
	
	public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        
        Block target = (Block) event.getClickedBlock();        
        Player player = event.getPlayer();
        
        if (target.getType() == Material.NOTE_BLOCK &&
        		player.getItemInHand().getType() == Material.IRON_INGOT &&
				plugin.hasNote(player)) {
        	
    		KtNote note = plugin.getNote(player);
    		((CraftNoteBlock) target.getState()).setNote(note);
    		player.sendMessage("Set note to " + note);
        }
    }
}
