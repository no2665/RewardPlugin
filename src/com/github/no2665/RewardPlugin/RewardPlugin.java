package com.github.no2665.RewardPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardPlugin extends JavaPlugin implements Listener{
	
	/**
	 * Improvements:
	 *  -Add a way of adding money as a reward
	 *  -Also enchantments
	 */
	
	private Map<String, Long> validJoins;
	
	@Override
	public void onLoad(){
		File file = new File("plugins" + File.separator + "RewardPlugin" + File.separator + "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
		checkConfigFile();
	}
	
	private void checkConfigFile(){
		Scanner scan;
		for(Map<?, ?> m : getConfig().getMapList("Rewards")){
			String r = m.get("rewards").toString();
			scan = new Scanner(r);
			while(scan.hasNext()){
				scan.next();
				if(!scan.hasNext()){
					getLogger().info("Your configuration file for the Rewards Plugin is missing a quantity for the level " + m.get("level").toString() + " rewards.");
					getLogger().info("This quantity will be assumed to be 1.");
				}
				else scan.nextInt();
			}
		}
	}
	
	//Everything starts here
	@Override
	public void onEnable(){ 
		validJoins = loadMap("plugins" + File.separator + "RewardPlugin" + File.separator + "RewardData.bin");
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	 
	//And ends here
	@Override
	public void onDisable(){ 
		saveMap(validJoins, "plugins" + File.separator + "RewardPlugin" + File.separator + "RewardData.bin");
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event){
		if(getConfig().getBoolean("Enabled")){
			final Player player = event.getPlayer();
			
			//Create new data for new players
			if(!validJoins.containsKey(player.getName())){
				validJoins.put(player.getName(), System.currentTimeMillis());
				ItemPicker.pickItem(player, this);
			}
			else{
				long lastValidJoin = validJoins.get(player.getName());
				//86400000 for 24 hours
				if(System.currentTimeMillis() - lastValidJoin >= getConfig().getLong("TimeBetweenRewards")){
					//Reset timer, and reward player
					validJoins.put(player.getName(), System.currentTimeMillis());
					ItemPicker.pickItem(player, this);
				}
			}
			
			//Send the player a friendly message.
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run(){
					player.sendMessage(getConfig().getString("RewardMessage"));
				}
			}, 20L);
		}
	}
	
	public void saveMap(Map<String, Long> validJoinsMap, String path)
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
	public HashMap<String, Long> loadMap(String path) {
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			return (HashMap<String, Long>) result;
		}catch(Exception e){
		}
		return new HashMap<String, Long>();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("deleteReward")){
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
