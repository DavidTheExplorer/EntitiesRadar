package me.entitiesradar.commands;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.groupingBy;
import static me.entitiesradar.utils.ChatColorUtilities.getBold;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.SpawnEgg;

import me.entitiesradar.EntitiesRadar;
import me.entitiesradar.api.fancymessage.FancyMessage;
import me.entitiesradar.utils.JavaUtilities;

public class NearbyEntitiesCommand implements CommandExecutor
{
	/** The number from which a radius is considered exceptional and requires a confirmation */
	private final int bigRadiusFrom;

	private final Set<UUID> waitingForConfirmation = new HashSet<>();

	/** Every key represents a max bound, and its min bound is the previous key - Which creates a range that the color value represents.*/
	private final Map<Integer, ChatColor> colorByAmountBound = new LinkedHashMap<>();

	public NearbyEntitiesCommand(int bigRadiusFrom, boolean notifyOnlineWaiting) 
	{
		this.bigRadiusFrom = bigRadiusFrom;

		setupStatusColors();

		//clean the waiting list every 30 seconds
		Bukkit.getScheduler().scheduleSyncRepeatingTask(EntitiesRadar.getInstance(), () -> cleanWaitingList(notifyOnlineWaiting), 0, 30 * 20);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{
		if(!(sender instanceof Player)) 
		{
			sender.sendMessage(ChatColor.RED + "Only players may execute this command.");
			return false;
		}
		Player player = (Player) sender;

		if(args.length != 1)
		{
			player.sendMessage(ChatColor.RED + "Please specify the radius.");
			return false;
		}
		Optional<Integer> radiusHolder = JavaUtilities.parseInt(args[0]);

		if(!radiusHolder.isPresent())
		{
			player.sendMessage(ChatColor.RED + "FAILED: " + args[0] + " is a ridiculously high radius which would " + getBold(ChatColor.RED) + "crash" + ChatColor.RED + " the server.");
			return false;
		}
		int radius = radiusHolder.get();

		//check if this time the player is accepting the previous execution of this command
		UUID playerUUID = player.getUniqueId();
		boolean playerConfirmingTheirWill = this.waitingForConfirmation.remove(playerUUID);

		if(isBigRadius(radius) && !playerConfirmingTheirWill)
		{
			this.waitingForConfirmation.add(playerUUID);
			sendConfirmationMessage(player, radius);
			return false;
		}
		List<Entity> nearbyEntities = player.getNearbyEntities(radius, radius, radius);

		if(nearbyEntities.isEmpty())
		{
			player.sendMessage(ChatColor.RED + "No nearby entities in radius of " + radius + " blocks!");
			return true;
		}
		sendNearbyEntitiesData(player, nearbyEntities, radius);
		return true;

		/*player.getNearbyEntities(10, 10, 10).stream()
		.filter(entity -> entity.getType() == EntityType.DROPPED_ITEM)
		.map(Item.class::cast)
		.map(Item::getItemStack)
		.map(NearbyEntitiesCommand::getDisplayName)
		.forEach(player::sendMessage);
		return true;*/
	}
	private boolean isBigRadius(int radius) 
	{
		return radius >= this.bigRadiusFrom;
	}
	private static void sendConfirmationMessage(Player player, int radius) 
	{
		player.sendMessage(getBold(ChatColor.RED) + "> " + ChatColor.GRAY + "The specified radius(" + ChatColor.RED + radius + ChatColor.GRAY + ") is exceptionally big,");

		new FancyMessage()
		.text("> ")
		.color(ChatColor.RED)
		.style(ChatColor.BOLD)
		.then("Click Here")
		.color(ChatColor.GREEN)
		.style(ChatColor.BOLD, ChatColor.UNDERLINE)
		.command("/nearbyentities " + radius)
		.formattedTooltip(new FancyMessage()
				.color(ChatColor.GREEN)
				.text("Click to Confirm The Scan"))
		.then(" if you still want to ")
		.color(ChatColor.GRAY)
		.then("run")
		.color(ChatColor.GREEN)
		.then(" this.")
		.color(ChatColor.GRAY)
		.send(player);
	}
	private void sendNearbyEntitiesData(Player player, List<Entity> nearbyEntities, int radius) 
	{
		Map<EntityType, Integer> typesAmounts = new HashMap<>(); //counts mobs types(amount of creepers, skeletons, etc)
		Map<Item, Integer> itemsAmounts = new HashMap<>(); //counts actual dropped items

		//Scan the nearby entities
		for(Entity entity : nearbyEntities)
		{
			if(entity.getType() != EntityType.DROPPED_ITEM) 
			{
				increaseAmount(typesAmounts, entity.getType(), 1);
			}
			else 
			{
				Item droppedItem = (Item) entity;
				int itemAmount = droppedItem.getItemStack().getAmount();
				
				increaseAmount(itemsAmounts, droppedItem, itemAmount);
			}
		}
		
		//Send the scan's data to the player
		player.sendMessage(createReportTitle("Mobs", radius));

		//display the mobs amounts
		typesAmounts.forEach((entityType, amount) -> player.sendMessage(" " + ChatColor.GREEN + getDisplayName(entityType) + ChatColor.GRAY + ": " + getAmountColor(amount) + amount));

		Map<Material, List<ItemStack>> groupedByMaterial = itemsAmounts.entrySet().stream()
				.map(Entry::getKey)
				.map(Item::getItemStack)
				.collect(groupingBy(ItemStack::getType));

		//display the total mobs amount
		final int totalMobs = typesAmounts.values().stream()
				.mapToInt(Integer::valueOf)
				.sum();

		player.sendMessage(ChatColor.GREEN + "-> " + ChatColor.GRAY + "In Summary: " + ChatColor.GOLD + totalMobs + ChatColor.GRAY + " mobs are within " + ChatColor.AQUA + radius + ChatColor.GRAY + " radius from you.");

		//display separately the dropped items, if there are any
		groupedByMaterial.forEach((material, items) -> player.sendMessage(String.format("%s: %d items.", material.name().toLowerCase(), items.size())));

		if(!itemsAmounts.isEmpty()) 
		{
			player.sendMessage(ChatColor.YELLOW + "Dropped Items: ");
			itemsAmounts.forEach((item, amount) -> player.sendMessage(getDisplayName(item.getItemStack()) + " Amount: " + amount));
		}
	}

	/**
	 * Increases by 1 the integer value of the specified {@code key} in the specified {@code counterMap}.
	 * @param <K> Generic Key parameter.
	 * @param counterMap The counters map.
	 * @param key The key whose value would be increased.
	 * @param amountToAdd in how much the {@code key}'s counter will increase
	 */
	private static <K> void increaseAmount(Map<K, Integer> counterMap, K key, int amountToAdd) 
	{
		Integer currentAmount = counterMap.computeIfAbsent(key, k -> 0);

		counterMap.put(key, currentAmount + amountToAdd);
	}

	/**
	 * Returns a suitable color that represents the stable status of the {@code entitiesAmount}.
	 * @param entitiesAmount The amount of entities detected.
	 * @return The amount's status's color.
	 */
	private ChatColor getAmountColor(int entitiesAmount) 
	{
		return this.colorByAmountBound.entrySet().stream()
				.filter(entry -> entitiesAmount <= entry.getKey())
				.min(comparingInt(Entry::getKey))
				.map(Entry::getValue)
				.orElse(ChatColor.RED);

		/* Faster(loop) Implementation:
		for(Map.Entry<Integer, ChatColor> entry : this.colorByAmountBound.entrySet()) 
		{
			if(entitiesAmount < entry.getKey()) 
			{
				return entry.getValue();
			}
		}
		return ChatColor.RED;*/
	}
	private static String getDisplayName(EntityType type)
	{
		return WordUtils.capitalizeFully(type.name().replace('_', ' '));
	}

	@SuppressWarnings("deprecation")
	private static String getDisplayName(ItemStack item)
	{
		Material material = item.getType();

		//start with the prefix of dropped item
		StringBuilder displayName = new StringBuilder(getDisplayName(EntityType.DROPPED_ITEM));
		displayName.append('(');

		//add the material's name
		displayName.append(WordUtils.capitalizeFully(material.name().replace('_', ' ')));

		//add the item's numeric data if it's not 0(default)
		if(item.getDurability() != 0) 
		{
			displayName.append(", Data: ").append(item.getDurability());
		}

		//if the item is a spawn egg, include the name of the mob it spawns
		if(material == Material.MONSTER_EGG) 
		{
			SpawnEgg eggData = (SpawnEgg) item.getData();
			displayName.append(": ").append(eggData.getSpawnedType().getName());
		}
		return displayName.append(')').toString();
	}
	private static String createReportTitle(String title, int radius) 
	{
		return ChatColor.GREEN + "*-" + getBold(ChatColor.GOLD) + "⇩" + ChatColor.GREEN + "-* Nearby " + title + " Report " + ChatColor.YELLOW + "(" + ChatColor.GOLD + "Radius: " + radius + ChatColor.YELLOW + ")" + ChatColor.GREEN + " *-" + getBold(ChatColor.GOLD) + "⇩" + ChatColor.GREEN + "-*";
	}
	private void cleanWaitingList(boolean notifyOnlineWaiting)
	{
		if(notifyOnlineWaiting)
		{
			this.waitingForConfirmation.stream()
			.map(Bukkit::getPlayer)
			.filter(Objects::nonNull)
			.forEach(waiting -> waiting.sendMessage(ChatColor.DARK_GREEN + "The confirmation time passed, But you can redo /nearbyentities"));
		}
		this.waitingForConfirmation.clear();
	}
	private void setupStatusColors() 
	{
		this.colorByAmountBound.put(20, ChatColor.GREEN);      //1-20     | OK
		this.colorByAmountBound.put(40, ChatColor.DARK_GREEN); //21-40    | Stable, But OK
		this.colorByAmountBound.put(70, ChatColor.YELLOW);     //41 - 70  | Concerning
		this.colorByAmountBound.put(71, ChatColor.RED);        //71+      | Bad
	}
	
	
	
	//SpigotUtilities.scheduleRepeatingClean(this.waitingForConfirmation, 10, new NotifyWaitingPlayersListener());

	/*private class NotifyWaitingPlayersListener implements Consumer<Set<UUID>>
	{
		@Override
		public void accept(Set<UUID> waitingPlayers)
		{
			waitingPlayers.stream()
			.map(Bukkit::getPlayer)
			.filter(Objects::nonNull)
			.forEach(waiting -> waiting.sendMessage(ChatColor.DARK_GREEN + "The confirmation time passed, But you can redo /nearbyentities"));
		}
	}*/
}