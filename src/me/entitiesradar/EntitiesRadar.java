package me.entitiesradar;

import org.bukkit.plugin.java.JavaPlugin;

import me.entitiesradar.commands.NearbyEntitiesCommand;
import me.entitiesradar.config.GeneralConfig;

public class EntitiesRadar extends JavaPlugin
{	
	private static EntitiesRadar INSTANCE;
	
	@Override
	public void onEnable() 
	{
		INSTANCE = this;
		
		GeneralConfig config = new GeneralConfig();
		
		getCommand("nearbyentities").setExecutor(new NearbyEntitiesCommand(config.radiusWarningFrom(), config.notifyOnlineWaiters()));
	}
	public static EntitiesRadar getInstance() 
	{
		return INSTANCE;
	}
}