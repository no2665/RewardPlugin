package com.github.no2665.RewardPlugin;

import java.util.Map;
import java.util.Scanner;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.nossr50.datatypes.PlayerProfile;

public class ItemPicker {
	public static ItemStack[] pickItem(final Player player, RewardPlugin plugin){
		final PlayerProfile profile = new PlayerProfile(player.getName(), false);
		//do some stuff with config here...
		final Configuration config = plugin.getConfig();
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			public void run(){
				String selectedRewards = "";
				for(Map<?, ?> m : config.getMapList("Rewards")){
					int powerLevel = profile.getPowerLevel();
					if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
						selectedRewards = m.get("rewards").toString();
					}
					else break;
				}
				Scanner scanItems = new Scanner(selectedRewards);
				while(scanItems.hasNext()){
					int itemType = scanItems.nextInt();
					int quantity = scanItems.nextInt();
					player.getInventory().addItem(new ItemStack(itemType, quantity));
				}
			}
		}, 20L);
		return null;
	}
}
