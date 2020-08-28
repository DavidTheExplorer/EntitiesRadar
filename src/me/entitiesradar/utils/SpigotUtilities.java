package me.entitiesradar.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import me.entitiesradar.EntitiesRadar;

public class SpigotUtilities
{
	@SafeVarargs
	public static <T extends Collection<?>> void scheduleRepeatingClean(T collection, int secondsDelay, Consumer<T>... cleanListeners) 
	{
		Bukkit.getScheduler().scheduleSyncRepeatingTask(EntitiesRadar.getInstance(), () -> 
		{
			//notify the listeners before the clean is done
			Arrays.stream(cleanListeners).forEach(listener -> listener.accept(collection));
			
			collection.clear();
		}, 0, secondsDelay * 20);
	}
}