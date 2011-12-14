/*package com.ktipr.kttools.fluidpush;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class FluidPusherListener extends BlockListener {

	Logger log = Logger.getLogger("Minecraft");
	
	@Override
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		if (event.isCancelled()) return;

		List<Block> blocks = event.getBlocks();
		if (!blocks.isEmpty()) return; //If it can push blocks, do not interfere	
		
		Block source = event.getBlock().getRelative(event.getDirection());
		if (isLiquid(source)) {
			log.info("Pushable block is a liquid!");
			Block target = event.getBlock().getRelative(event.getDirection(), 2);
			if (target.isEmpty() || target.isLiquid()) {
				log.info("And it has room, so let's go!");
				target.setTypeId(source.getTypeId());
			}
		}
	}

	@Override
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		if (event.isCancelled()) return;
		if (!event.isSticky()) return;
		
		Block piston = event.getBlock();
		Block source = piston.getRelative(event.getDirection(), 2); //This is the block we are moving
		if (isLiquid(source)) {
			log.info("Pullable block is a liquid!");
			Block target = piston.getRelative(event.getDirection()); //This is the piston extension
			if (target.getType() == Material.PISTON_EXTENSION) {
				log.info("Target block is now still piston, so let's go!");
				target.setTypeId(source.getTypeId());
				source.setType(Material.AIR);
				event.setCancelled(true);
				piston.setData(piston.getData());
			}
		}
	}
	
	private boolean isLiquid(Block block) {
		return (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA);
	}
	
}*/
