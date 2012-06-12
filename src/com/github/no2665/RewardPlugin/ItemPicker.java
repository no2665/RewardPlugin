package com.github.no2665.RewardPlugin;

import java.util.Map;
import java.util.Scanner;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.nossr50.datatypes.PlayerProfile;

public class ItemPicker {
	public static ItemStack[] pickItem(final Player player, RewardPlugin plugin){
		final Configuration config = plugin.getConfig();
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			public void run(){
				
				String selectedRewards = "";
				int powerLevel = (new PlayerProfile(player.getName(), false)).getPowerLevel();
				
				for(Map<?, ?> m : config.getMapList("Rewards")){
					//Check each level set in the config file to see if it is greater than
					//the players power level, and if it is, stop checking
					if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
						selectedRewards = m.get("rewards").toString();
					}
					else break;
				}
				
				Scanner scanItems = new Scanner(selectedRewards);
				while(scanItems.hasNext()){
					int itemType = scanItems.nextInt();
					int quantity = scanItems.nextInt();
					//Probably shouldn't directly add items here,
					//Afterall this method does return and itemstack array
					player.getInventory().addItem(new ItemStack(itemType, quantity));
				}
			}
		}, 20L);
		return null;
	}
}
