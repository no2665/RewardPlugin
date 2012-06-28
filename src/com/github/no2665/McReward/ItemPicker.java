package com.github.no2665.McReward;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Material;
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
						addRewards(splitString(selectedMap.get("rewards").toString()));
					}
					if(selectedMap.containsKey("money")){
						addMoney(selectedMap.get("money").toString());
					}
					if(selectedMap.containsKey("enchantments")){
						addEnchantment(selectedMap.get("enchantments").toString());
					}
				}
				if(config.contains("automaticCollection") && config.getBoolean("automaticCollection")){
					plugin.giveRewards(player);
				}
			}
			
			public String splitString(String selectedString){
				if(selectedString.contains("or")){
					String[] split = selectedString.split("or");
					return split[(new Random()).nextInt(split.length)];
				}
				return selectedString;
			}
			
			public Map<?, ?> selectMap(){
				int powerLevel = Users.getProfile(player).getPowerLevel();
				Map<?, ?> map = null;
				for(Map<?, ?> m : config.getMapList("Rewards")){
					if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
						map = m;
					}
					else break;
				}
				return map;
			}
			
			public void addRewards(String selectedRewards){
				if(selectedRewards.trim().equalsIgnoreCase("random")){
					ItemStack i = null;
					Random rnd = new Random();
					if(rnd.nextDouble() > 0.5d){
						i = new ItemStack(rnd.nextInt(123), 1 + rnd.nextInt(64));
					}
					else{
						i = new ItemStack(256  + rnd.nextInt(128), 1 + rnd.nextInt(64));
					}
					addRewardsToList(i);
				}
				else{
					Scanner scanItems = new Scanner(selectedRewards);
					while(scanItems.hasNext()){
						String itemType = scanItems.next();
						int quantity = 1;
						if(scanItems.hasNext()) quantity = scanItems.nextInt();
						if(itemType.contains(":")){
							String[] split = itemType.split(":");
							byte itemData = Byte.parseByte(split[1]);
							if(plugin.isInt(split[0])) addRewardsToList(new MaterialData(Integer.parseInt(split[0]), itemData).toItemStack(quantity));
							else{
								Material m = Material.matchMaterial(split[0]);
								if(m != null) addRewardsToList(new MaterialData(m, itemData).toItemStack(quantity));
								else plugin.getLogger().warning("Reward " + split[0] + " is not a valid item! Maybe you misspelled the name?");
							}
						}
						else{
							if(plugin.isInt(itemType)){
								addRewardsToList(new ItemStack(Integer.parseInt(itemType), quantity));
							}
							else{
								Material m = Material.matchMaterial(itemType);
								if(m != null) addRewardsToList(new ItemStack(m, quantity));
								else plugin.getLogger().warning("Reward " + itemType + " is not a valid item! Maybe you misspelled the name?");
							}
						}
					}
				}
			}
			
			private void addRewardsToList(ItemStack i){
				if(plugin.playersRewards.containsKey(player.getName())){
					ArrayList<ItemStack> l = (ArrayList<ItemStack>) plugin.playersRewards.get(player.getName());
					l.add(i);
					plugin.playersRewards.put(player.getName(), l);
				}
				else{
					ArrayList<ItemStack> l = new ArrayList<ItemStack>();
					l.add(i);
					plugin.playersRewards.put(player.getName(), l);
				}
			}
		
			public void addMoney(String selectedMoney){
				double money = 0;
				if(selectedMoney.contains("between")){
					Scanner scan = new Scanner(selectedMoney);
					scan.next();
					double min = scan.nextDouble();
					double max = scan.nextDouble();
					money = min + (new Random()).nextDouble() * (max - min);
				}
				else {
					money = Double.parseDouble(selectedMoney);
				}
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
				String current = ":";
				while(scan.hasNext()){
					try{
						String itemType;
						if(current.equals(":")) itemType = scan.next();
						else itemType = current;
						if(plugin.isInt(itemType)){
							item = new ItemStack(Integer.parseInt(itemType));
						}
						else{
							Material m = Material.matchMaterial(itemType);
							if(m != null) item = new ItemStack(m);
							else{
								plugin.getLogger().warning("Reward " + itemType + " is not a valid item! Maybe you misspelled the name?");
								throw new Exception();
							}
						}
						while(scan.hasNext()){
							current = scan.next();
							if(!current.contains(":")) break;
							String[] enchantments = current.split(":");
							Enchantment e;
							if(plugin.isInt(enchantments[0])) e = new EnchantmentWrapper(Integer.parseInt(enchantments[0]));
							else{
								e = Enchantment.getByName(enchantments[0]);
								if(e == null){
									plugin.getLogger().warning("Enchantment " + enchantments[0] + " is not a valid enchantment! Maybe you misspelled the name?");
									throw new Exception();
								}
							}
							if(e.canEnchantItem(item)){
								int level = Integer.parseInt(enchantments[1]);
								if(level < e.getStartLevel()) item.addEnchantment(e, e.getStartLevel());
								else if(level > e.getMaxLevel()) item.addEnchantment(e, e.getMaxLevel());
								else item.addEnchantment(e, level);
							}
							else{
								plugin.getLogger().warning("Enchantment " + e.getName() + " cannot be used on the item " + item.getType().toString() + "!");
							}
						}
						addRewardsToList(item);
					}
					catch(Exception e){
						plugin.getLogger().warning("Enchanting failed! Check the config file to see if you have set it up correctly.");
					}
				}
			}
			
		}, 25L);
	}
}
