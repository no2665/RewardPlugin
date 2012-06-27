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

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
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
	
	//Everything starts here
	@Override
	public void onEnable(){ 
		validJoins = loadLoginMap("plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		playersRewards = loadRewardMap("plugins" + File.separator + "mcReward" + File.separator + "PlayerData.bin");
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	 
	//And ends here
	@Override
	public void onDisable(){ 
		saveLoginMap(validJoins, "plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		saveRewardMap(playersRewards, "plugins" + File.separator + "mcReward" + File.separator + "PlayerData.bin");
		this.getServer().getScheduler().cancelTasks(this);
	}

	//Try PlayerJoinEvent instead. To see if the message is delayed or not?
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event){
		reloadConfig();
		if(getConfig().getBoolean("Enabled")){
			if(getConfig().contains("Rewards")){
				Player player = event.getPlayer();
				//Create new data for new players
				if(!validJoins.containsKey(player.getName())){
					validJoins.put(player.getName(), System.currentTimeMillis());
					ItemPicker.pickItem(player, this);
					sendMessage(player);
				}
				else{
					long lastValidJoin = validJoins.get(player.getName());
					if(System.currentTimeMillis() - lastValidJoin >= getTimeBetweenRewards()){
						//Reset timer, and reward player
						validJoins.put(player.getName(), System.currentTimeMillis());
						ItemPicker.pickItem(player, this);
						sendMessage(player);
					}
				}
			}
		}
	}
	
	private void sendMessage(final Player player){
		//Send the player a friendly message.
		if(getConfig().contains("RewardMessage")){
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run(){
					player.sendMessage(getConfig().getString("RewardMessage"));
				}
			}, 20L);
		}
	}
	
	private long getTimeBetweenRewards(){
		String[] split = getConfig().getString("TimeBetweenRewards").split(":");
		long time = 0;
		for(int i = 0; i < split.length; i++){
			if(i == 0){
				time += Integer.parseInt(split[0]) * 86400000;
			}
			else if(i == 1){
				time += Integer.parseInt(split[1]) * 3600000;
			}
			else if(i == 2){
				time += Integer.parseInt(split[2]) * 60000;
			}
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
		if(l.isEmpty()){
			player.sendMessage("You currently do not have any rewards to collect");
		}
		else{
			int highestIndex = -1;
			HashMap<Integer, ItemStack> unableToGive = null;
			for(int i = 0; i < l.size(); i++){
				unableToGive = player.getInventory().addItem(l.get(i));
				if(unableToGive.isEmpty()){
					highestIndex = i;
				}
				else{
					player.sendMessage("Unable to give you all of your rewards");
					break;
				}
			}
			if(highestIndex > -1){
				for(int i = 0; i <= highestIndex; i++){
					l.remove(0);
				}
			}
			playersRewards.put(player.getName(), l);
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
						item = Material.getMaterial(Integer.parseInt(item)).toString().toLowerCase();
						int quantity = 1;
						if(scan.hasNext()) quantity = scan.nextInt();
						rewardString += item + " x" + quantity + ", "; 
					}
					sender.sendMessage("These rewards: " + rewardString);
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
					sender.sendMessage("These enchanted items: " + map.get("enchantments"));
				}
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
			else sender.sendMessage("You do not have permission to use this command"); 
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
			else sender.sendMessage("You do not have permission to use this command"); 
		}
		return true;
	}
}
