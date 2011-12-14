package com.ktipr.kttools.blockrotate;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class BlockRotatePlayerListener extends PlayerListener {
	
	public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
                
        Block target = event.getClickedBlock();
        
        if (target.getType() == Material.RAILS &&
    		event.getPlayer().getItemInHand().getType() == Material.WOOD_PICKAXE) {
        	target.setData((byte) (target.getData() < 9 ? target.getData() + 1 : 0));
        	return;
        }
    }
	
}
