package com.github.no2665.McReward;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.util.Users;

public class McRewardPlugin extends JavaPlugin implements Listener{
	
	private Map<String, Long> validJoins;
	public Map<String, List<ItemStack>> playersRewards;
	
	@Override
	public void onLoad(){
		File file = new File("plugins" + File.separator + "mcReward" + File.separator + "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
	}
	
	@Override
	public void onEnable(){ 
		validJoins = loadLoginMap("plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		playersRewards = loadRewardMap("plugins" + File.separator + "mcReward" + File.separator + "PlayerData.bin");
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable(){ 
		saveLoginMap(validJoins, "plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		saveRewardMap(playersRewards, "plugins" + File.separator + "mcReward" + File.separator + "PlayerData.bin");
		this.getServer().getScheduler().cancelTasks(this);
	}

	@EventHandler
	public void onPlayerLogin(PlayerJoinEvent event){
		reloadConfig();
		if(getConfig().getBoolean("Enabled")){
			if(getConfig().contains("Rewards")){
				Player player = event.getPlayer();
				if(!validJoins.containsKey(player.getName())){
					validJoins.put(player.getName(), System.currentTimeMillis());
					ItemPicker.pickItem(player, this);
					sendMessage(player);
				}
				else{
					long lastValidJoin = validJoins.get(player.getName());
					if(System.currentTimeMillis() - lastValidJoin >= getTimeBetweenRewards(player)){
						validJoins.put(player.getName(), System.currentTimeMillis());
						ItemPicker.pickItem(player, this);
						sendMessage(player);
					}
				}
			}
		}
	}
	
	private void sendMessage(final Player player){
		if(getConfig().contains("Messages.rewardsReady")){
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run(){
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.rewardsReady")));
				}
			}, 20L);
		}
	}
	
	private long getTimeBetweenRewards(Player player){
		int powerLevel = Users.getProfile(player).getPowerLevel();
		Map<?, ?> map = null;
		for(Map<?, ?> m : getConfig().getMapList("Rewards")){
			if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
				map = m;
			}
			else break;
		}
		if(map.containsKey("time")){
			String[] levelSplit = map.get("time").toString().split(":");
			long levelTime = 0;
			for(int i = 0; i < levelSplit.length; i++){
				if(i == 0) levelTime += Integer.parseInt(levelSplit[0]) * 86400000;
				else if(i == 1) levelTime += Integer.parseInt(levelSplit[1]) * 3600000;
				else if(i == 2) levelTime += Integer.parseInt(levelSplit[2]) * 60000;
			}
			return levelTime;
		}
		String[] defaultSplit = getConfig().getString("TimeBetweenRewards").split(":");
		long time = 0;
		for(int i = 0; i < defaultSplit.length; i++){
			if(i == 0) time += Integer.parseInt(defaultSplit[0]) * 86400000;
			else if(i == 1) time += Integer.parseInt(defaultSplit[1]) * 3600000;
			else if(i == 2) time += Integer.parseInt(defaultSplit[2]) * 60000;
		}
		return time;
	}
	
	private void saveRewardMap(Map<String, List<ItemStack>> validJoinsMap, String path)
	{
		try{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(validJoinsMap);
			oos.flush();
			oos.close();
		}catch(Exception e){
		}
	}
	
	private void saveLoginMap(Map<String, Long> validJoinsMap, String path)
	{
		try{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(validJoinsMap);
			oos.flush();
			oos.close();
		}catch(Exception e){
		}
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, List<ItemStack>> loadRewardMap(String path) {
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			return (HashMap<String, List<ItemStack>>) result;
		}catch(Exception e){
		}
		return new HashMap<String, List<ItemStack>>();
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, Long> loadLoginMap(String path) {
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			return (HashMap<String, Long>) result;
		}catch(Exception e){
		}
		return new HashMap<String, Long>();
	}
	
	public void giveRewards(Player player){
		ArrayList<ItemStack> l = (ArrayList<ItemStack>) playersRewards.get(player.getName());
		if(l.isEmpty() && getConfig().contains("Messages.noRewards")){
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.noRewards")));
		}
		else{
			boolean showRewards = getConfig().getBoolean("ShowRewards");
			String givenRewards = "";
			int highestIndex = -1;
			HashMap<Integer, ItemStack> unableToGive = null;
			for(int i = 0; i < l.size(); i++){
				unableToGive = player.getInventory().addItem(l.get(i));
				if(showRewards) givenRewards += l.get(i).getType().toString().toLowerCase() + " x " + l.get(i).getAmount() +  ", ";
				if(unableToGive.isEmpty()){
					highestIndex = i;
				}
				else{
					if(getConfig().contains("Messages.unableToGiveRewards")) player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.unableToGiveRewards")));
					break;
				}
			}
			if(highestIndex > -1){
				for(int i = 0; i <= highestIndex; i++){
					l.remove(0);
				}
			}
			playersRewards.put(player.getName(), l);
			if(showRewards) player.sendMessage("You have received: " + givenRewards.substring(0, givenRewards.length() - 2));
		}
	}
	
	public boolean isInt(String t){
		try{
			Integer.parseInt(t);
			return true;
		}
		catch(NumberFormatException e){
			return false;
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("receive") || command.getAliases().contains("receiveRewards")){
			if(sender instanceof Player && sender.hasPermission("receiveRewards")){
				if(playersRewards.containsKey(sender.getName())){
					giveRewards((Player) sender);
				}
			}
			else{
				if(getConfig().contains("Messages.noPermission"))
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.noPermission")));
			}
		}
		else if(command.getName().equals("listRewards")){
			if(sender instanceof Player && sender.hasPermission("receiveRewards")){
				int powerLevel = Users.getProfile((Player) sender).getPowerLevel();
				Map<?, ?> map = null;
				for(Map<?, ?> m : getConfig().getMapList("Rewards")){
					if(Integer.parseInt(m.get("level").toString()) <= powerLevel){
						map = m;
					}
					else break;
				}
				sender.sendMessage("You will get:");
				if(map.containsKey("rewards")){
					Scanner scan = new Scanner(map.get("rewards").toString());
					String rewardString = "";
					while(scan.hasNext()){
						String item = scan.next();
						if(item.equals("or")){
							rewardString += "OR ";
							continue;
						}
						else if(item.contains(":")){
							item = (item.split(":"))[0];
						}
						if(isInt(item)){
							item = Material.getMaterial(Integer.parseInt(item)).toString().toLowerCase();
						}
						else{
							Material m = Material.matchMaterial(item);
							if(m != null) item = m.toString().toLowerCase();
							else{
								getLogger().warning("Reward " + item + " is not a valid item! Maybe you misspelled the name?");
								item = "INVALID";
							}
						}
						int quantity = 1;
						if(scan.hasNext()) quantity = scan.nextInt();
						rewardString += item + " x" + quantity + ", "; 
					}
					sender.sendMessage("These rewards: " + rewardString.substring(0, rewardString.length() - 2));
				}
				if(map.containsKey("money")){
					String moneyString = map.get("money").toString();
					if(moneyString.contains("between")){
						Scanner scan = new Scanner(moneyString);
						moneyString = scan.next() + " " + scan.next() + " and " + scan.next();
					}
					sender.sendMessage("This much money: " + moneyString);
				}
				if(map.containsKey("enchantments")){
					Scanner scan = new Scanner(map.get("enchantments").toString());
					String enchantmentString = "";
					String current = ":";
					while(scan.hasNext()){
						String itemType;
						if(current.equals(":")) itemType = scan.next();
						else itemType = current;
						if(isInt(itemType)){
							itemType = Material.getMaterial(Integer.parseInt(itemType)).toString().toLowerCase();
						}
						else{
							Material m = Material.matchMaterial(itemType);
							if(m != null) itemType = m.toString().toLowerCase();
							else{
								getLogger().warning("Reward " + itemType + " is not a valid item! Maybe you misspelled the name?");
								itemType = "INVALID";
							}
						}
						enchantmentString += itemType + " with ";
						while(scan.hasNext()){
							current = scan.next();
							if(!current.contains(":")) break;
							String[] enchantments = current.split(":");
							if(isInt(enchantments[0])){
								enchantmentString += Enchantment.getById(Integer.parseInt(enchantments[0])).getName() + " at level " + enchantments[1] + ", ";
							}
							else{
								enchantmentString += enchantments[0] + " at level " + enchantments[1] + ", ";
							}
						}
					}
					sender.sendMessage("These enchanted items: " + enchantmentString.substring(0, enchantmentString.length() - 2));	
				}
				if(getConfig().contains("ShowTime") && getConfig().getBoolean("ShowTime")){
					long seconds = (validJoins.get(sender.getName()) - (System.currentTimeMillis() - getTimeBetweenRewards((Player) sender))) / 1000;
					if(seconds <= 0){
						sender.sendMessage("Your rewards are ready! Log back in to receive them.");
					}
					else {
						int minutes = 0, hours = 0;
						if(seconds >= 60){
							minutes = (int) (seconds / 60);
							seconds -= minutes * 60;
							if(minutes >= 60){
								hours = minutes / 60;
								minutes -= hours * 60;
							}
						}
						sender.sendMessage("You have to wait " + (hours == 0 ? "" : hours + " hours, ") + (minutes == 0 ? "" : minutes + " minutes, ") + (seconds == 0 ? "" : seconds + " seconds ") + "till your next reward.");
					}
				}
			}
			else{
				if(getConfig().contains("Messages.noPermission"))
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.noPermission")));
			}
		}
		else if(command.getName().equals("deleteReward")){
			if(sender.hasPermission("editConfigPerms")){
				if(args.length == 0) return false;
				boolean found = false;
				int level = Integer.parseInt(args[0]);
				int index = 0;
				List<Map<?, ?>> configList = getConfig().getMapList("Rewards");
				for(Map<?, ?> m : configList){
					if(Integer.parseInt(m.get("level").toString()) < level) index++;	
					else if(Integer.parseInt(m.get("level").toString()) == level){ found = true; break; }
					else break;
				}
				if(found){
					configList.remove(index);
					getConfig().set("Rewards", configList);
					saveConfig();
				}
				else sender.sendMessage("Level " + level + " is not included in the config file.");
			}
			else{
				if(getConfig().contains("Messages.noPermission"))
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.noPermission")));
			}
		}
		else if(command.getName().equals("addReward")){
			if(sender.hasPermission("editConfigPerms")){
				boolean edit = false;
				if(args.length == 0) return false;
				int level = Integer.parseInt(args[0]);
				int index = 0;
				String rewards = "";
				for(int i = 1; i < args.length; i++){
					rewards += args[i] + " ";
				}
				List<Map<?, ?>> configList = getConfig().getMapList("Rewards");
				for(Map<?, ?> m : configList){
					if(Integer.parseInt(m.get("level").toString()) < level) index++;	
					else if(Integer.parseInt(m.get("level").toString()) == level){ edit = true; break; }
					else break;
				}
				if(edit){
					@SuppressWarnings("unchecked")
					Map<String, Object> map = (Map<String, Object>) configList.get(index);
					map.put("rewards", map.get("rewards") + " " + rewards);
					configList.set(index, map);
				}
				else{
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("level", level);
					map.put("rewards", rewards);
					configList.add(index, map);
				}
				getConfig().set("Rewards", configList);
				saveConfig();
			}
			else {
				if(getConfig().contains("Messages.noPermission"))
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Messages.noPermission")));
			}
		}
		return true;
	}
}
