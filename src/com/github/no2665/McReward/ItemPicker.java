package com.github.no2665.McReward;

import java.util.Map;
import java.util.Scanner;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.gmail.nossr50.util.Users;

public class ItemPicker {
	public static void pickItem(final Player player, final McRewardPlugin plugin){
		final Configuration config = plugin.getConfig();
		
		plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable(){
			public void run(){
				Map<?, ?> selectedMap = selectMap();
				if(selectedMap != null){
					if(selectedMap.containsKey("rewards")){
						addRewards(selectedMap.get("rewards").toString());
					}
					if(selectedMap.containsKey("money")){
						addMoney(selectedMap.get("money").toString());
					}
					if(selectedMap.containsKey("enchantments")){
						addEnchantment(selectedMap.get("enchantments").toString());
					}
				}
			}
			
			public Map<?, ?> selectMap(){
				int powerLevel = Users.getProfile(player).getPowerLevel();
				Map<?, ?> map = null;
				for(Map<?, ?> m : config.getMapList("Rewards")){
					//Check each level set in the config file to see if it is greater than
					//the players power level, and if it is, stop checking
					if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
						map = m;
					}
					else break;
				}
				return map;
			}
			
			public void addRewards(String selectedRewards){
				Scanner scanItems = new Scanner(selectedRewards);
				while(scanItems.hasNext()){
					String itemType = scanItems.next();
					int quantity = 1;
					if(scanItems.hasNext()) quantity = scanItems.nextInt();
					if(itemType.contains(":")){
						String[] split = itemType.split(":");
						int itemID = Integer.parseInt(split[0]);
						int itemData = Integer.parseInt(split[1]);
						player.getInventory().addItem(new MaterialData(itemID, (byte) itemData).toItemStack(quantity));
					}
					else player.getInventory().addItem(new ItemStack(Integer.parseInt(itemType), quantity));
				}
			}
		
			public void addMoney(String selectedMoney){
				double money = Double.parseDouble(selectedMoney);
				RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
				Economy economy = null;
				if (economyProvider != null) {
		            economy = economyProvider.getProvider();
		            economy.depositPlayer(player.getName(), money);
		        }
			}
			
			public void addEnchantment(String selectedEnchantment){
				Scanner scan = new Scanner(selectedEnchantment);
				ItemStack item = null;
				while(scan.hasNext()){
					try{
						item = new ItemStack(scan.nextInt());
						while(scan.hasNext()){
							String[] enchantments = scan.next().split(":");
							Enchantment e = new EnchantmentWrapper(Integer.parseInt(enchantments[0]));
							if(e.canEnchantItem(item)){
								int level = Integer.parseInt(enchantments[1]);
								if(level < e.getStartLevel()) item.addEnchantment(e, e.getStartLevel());
								else if(level > e.getMaxLevel()) item.addEnchantment(e, e.getMaxLevel());
								else item.addEnchantment(e, level);
							}
							if(scan.hasNextInt()) break;
						}
						player.getInventory().addItem(item);
					}
					catch(Exception e){
						plugin.getLogger().warning("Enchanting failed! Check the config file to see if you have set it up correctly.");
					}
				}
			}
			
		}, 25L);
	}
}
