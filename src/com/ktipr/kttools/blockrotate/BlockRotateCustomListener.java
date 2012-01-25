package com.ktipr.kttools.blockrotate;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
//import org.bukkit.event.block.Action;

import com.easybind.listeners.EasyBindEvent;
import com.ktipr.kttools.KtTools;

public class BlockRotateCustomListener extends CustomEventListener {
    
    private final KtTools plugin;
    
    public BlockRotateCustomListener(KtTools plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onCustomEvent(Event event) {
        if(event instanceof EasyBindEvent) {
            EasyBindEvent e = (EasyBindEvent) event;
            if(e.isCancelled()) return;
            //if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            
            if(e.getName().equals("blockrotate")) {
                if(!plugin.canUse(e.getPlayer(), e.getName())) return;
                
                Block target = e.getTriggerEvent().getClickedBlock();
                byte data = target.getData();
                
                if (target.getType() == Material.POWERED_RAIL ||
                    target.getType() == Material.DETECTOR_RAIL)
                {
                    target.setData((byte) (data == 5 ? 0 : data + 1));
                    return;
                }
                
                if (target.getType() == Material.RAILS) {
                    target.setData((byte) (data == 9 ? 0 : data + 1));
                    return;
                }
                
                if (target.getType() == Material.WOOD_STAIRS ||
                		target.getType() == Material.SMOOTH_STAIRS ||
                		target.getType() == Material.BRICK_STAIRS ||
                		target.getType() == Material.COBBLESTONE_STAIRS ||
                		target.getType() == Material.NETHER_BRICK_STAIRS) {
                    target.setData((byte) (data == 3 ? 0 : data + 1));
                    return;
                }
            }
        }
    }
}
