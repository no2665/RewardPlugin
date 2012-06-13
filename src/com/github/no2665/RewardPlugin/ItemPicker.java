package com.github.no2665.RewardPlugin;

import java.util.Map;
import java.util.Scanner;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.gmail.nossr50.util.Users;

public class ItemPicker {
	public static void pickItem(final Player player, RewardPlugin plugin){
		final Configuration config = plugin.getConfig();
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
			public void run(){
				String selectedRewards = "";
				int powerLevel = (Users.getProfile(player).getPowerLevel());
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
					String itemType = scanItems.next();
					int quantity = 1;
					if(scanItems.hasNext()) quantity = scanItems.nextInt();
					if(itemType.contains(":")){
						int itemID = Integer.parseInt(itemType.substring(0, itemType.indexOf(":")));
						int itemData = Integer.parseInt(itemType.substring(itemType.indexOf(":") + 1, itemType.length()));
						player.getInventory().addItem(new MaterialData(itemID, (byte) itemData).toItemStack(quantity));
					}
					else player.getInventory().addItem(new ItemStack(Integer.parseInt(itemType), quantity));
				}
			}
		}, 20L);
	}
}
