package com.ktipr.kttools;

//import java.util.HashMap;
import gnu.trove.THashMap;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.easybind.permissions.Permissions;
import com.easybind.permissions.PermissionsResolver;

//import com.ktipr.kttools.fluidpush.FluidPusherListener;
import com.ktipr.kttools.blockrotate.*;
import com.ktipr.kttools.perfecttune.*;

public class KtTools extends JavaPlugin {

	Logger log = Logger.getLogger("Minecraft");
	
	//Using hardcoded bind:
	//private final RailRotatePlayerListener railRotationListener = new RailRotatePlayerListener();
	//private final PerfectTunePlayerListener perfectTuneListener = new PerfectTunePlayerListener(this);
	
	//Using EasyBind:
	private final BlockRotateCustomListener blockRotationListener = new BlockRotateCustomListener(this);
	private final PerfectTuneCustomListener perfectTuneListener = new PerfectTuneCustomListener(this);
	//private final FluidPusherListener fluidPusherListener = new FluidPusherListener();
	
	private static final String prefix = "kttools.";
	
	private THashMap<Player, KtNote> tuneMap = new THashMap<Player, KtNote>();
	private Permissions permissions;
	
	public void onEnable() { 
		log.info("Ktipr's tools have been enabled! Rock on Mister!");
		
		permissions = PermissionsResolver.resolve(this);
		
		// Plugin
	    PluginManager pm = this.getServer().getPluginManager();
	    
	    //Using hardcoded bind:
	    //pm.registerEvent(Event.Type.PLAYER_INTERACT, perfectTuneListener, Event.Priority.Normal, this);
	    //pm.registerEvent(Event.Type.PLAYER_INTERACT, railRotationListener, Event.Priority.Normal, this);
	    
	    //Using EasyBind:
	    pm.registerEvent(Event.Type.CUSTOM_EVENT, blockRotationListener, Event.Priority.Normal, this);
	    pm.registerEvent(Event.Type.CUSTOM_EVENT, perfectTuneListener, Event.Priority.Normal, this);
	    //pm.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, fluidPusherListener, Event.Priority.Normal, this);
	    //pm.registerEvent(Event.Type.BLOCK_PISTON_RETRACT, fluidPusherListener, Event.Priority.Normal, this);
	    
	    PerfectTuneCommandExecutor myExecutor = new PerfectTuneCommandExecutor(this);
		getCommand("tune").setExecutor(myExecutor);
	}
	 
	public void onDisable(){ 
		log.info("Ktipr's tools have been disabled! :( Sad ktipr");
	}

	public boolean canUse(Player player, String node) {
	    return permissions.canUse(player, prefix + node);
		//return true;
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
	
}
