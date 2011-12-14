package com.ktipr.kttools.perfecttune;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.block.CraftNoteBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;

import com.easybind.listeners.EasyBindEvent;
import com.ktipr.kttools.KtTools;

public class PerfectTuneCustomListener extends CustomEventListener {

	private final KtTools plugin;

	public PerfectTuneCustomListener(KtTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCustomEvent(Event event) {
        if(event instanceof EasyBindEvent) {
            EasyBindEvent e = (EasyBindEvent) event;
            if(e.isCancelled()) return;
            if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            
            Player player = e.getPlayer();
            
            if(e.getName().equals("tune")) {
                if(!plugin.canUse(player, "tune")) return;
                
                Block target = e.getTriggerEvent().getClickedBlock();
                if (target.getType() != Material.NOTE_BLOCK) return;
                
            	if (plugin.hasNote(player)) {
            		KtNote note = plugin.getNote(player);
            		((CraftNoteBlock) target.getState()).setNote(note);
            		player.sendMessage("Set note to " + note);
            	}
            }
        }
    }
	
}
