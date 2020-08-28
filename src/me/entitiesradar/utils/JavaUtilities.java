package me.entitiesradar.utils;

import java.util.Optional;

public class JavaUtilities 
{
	public static Optional<Integer> parseInt(String text) 
	{
		try 
		{
			int number = Integer.parseInt(text);
			return Optional.of(number);
		}
		catch(NumberFormatException exception) 
		{
			return Optional.empty();
		}
	}
}
