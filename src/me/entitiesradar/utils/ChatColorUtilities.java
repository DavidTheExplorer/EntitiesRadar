package me.entitiesradar.utils;

import static org.apache.commons.lang3.StringUtils.repeat;

import org.bukkit.ChatColor;

public class ChatColorUtilities 
{
	//Container of static methods
	private ChatColorUtilities(){}

	public static String colorize(String text) 
	{
		return ChatColor.translateAlternateColorCodes('&', text);
	}
	public static String getBold(ChatColor color) 
	{
		return color + ChatColor.BOLD.toString();
	}
	public static String getUnderlined(ChatColor color) 
	{
		return color + ChatColor.UNDERLINE.toString();
	}
	public static String createSeparationLine(ChatColor color, int length) 
	{
		return color.toString() + ChatColor.STRIKETHROUGH + repeat('-', length);
	}
	public static ChatColor searchFirstColor(String text) 
	{
		int charColorIndex = text.indexOf(ChatColor.COLOR_CHAR);
		
		return charColorIndex == -1 ? null : ChatColor.getByChar(text.charAt(charColorIndex+1));
	}
}