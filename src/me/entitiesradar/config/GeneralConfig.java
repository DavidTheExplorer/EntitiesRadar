package me.entitiesradar.config;

import org.bukkit.configuration.file.FileConfiguration;

import me.entitiesradar.EntitiesRadar;

public class GeneralConfig 
{
	/**From what number the radius is considered too big, and needs a confirmation. */
	private int radiusWarningMin;

	/**Determines if the plugin will update the players waiting for confirmation once the waiting list gets cleared.*/
	private boolean notifyOnlineWaiters;

	public GeneralConfig()
	{
		EntitiesRadar.getInstance().saveDefaultConfig();
		FileConfiguration config = EntitiesRadar.getInstance().getConfig();

		this.radiusWarningMin = config.getInt(getSettingPath("Confirmation Message From Radius"));
		this.notifyOnlineWaiters = config.getBoolean(getSettingPath("Notify Waiters Before They Need To Reconfirm"));
	}
	public int radiusWarningFrom() 
	{
		return this.radiusWarningMin;
	}
	public boolean notifyOnlineWaiters() 
	{
		return this.notifyOnlineWaiters;
	}
	
	private String getSettingPath(String innerPath) 
	{
		return String.format("Settings.%s", innerPath);
	}
}
