package com.github.no2665.McReward;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class McRewardPlugin extends JavaPlugin implements Listener{
	
	/**
	 * Improvements:
	 *  -Add a way of adding money as a reward
	 *  -Also enchantments
	 *  
	 *  -Make ItemPicker check things more
	 *  	For instance it should check if the config file contains the Rewards key
	 */
	
	private Map<String, Long> validJoins;
	
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
		validJoins = loadMap("plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		this.getServer().getPluginManager().registerEvents(this, this);
	}
	 
	//And ends here
	@Override
	public void onDisable(){ 
		saveMap(validJoins, "plugins" + File.separator + "mcReward" + File.separator + "mcRewardData.bin");
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event){
		//RELOAD CONFIG???
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
	
	private int getTimeBetweenRewards(){
		String[] split = getConfig().getString("TimeBetweenRewards").split(":");
		//             Days                            +                 minutes                 +              hours
		return (Integer.parseInt(split[0]) * 86400000) + (Integer.parseInt(split[1]) *  3600000) + (Integer.parseInt(split[2]) * 60000);
	}
	
	private void saveMap(Map<String, Long> validJoinsMap, String path)
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
	private HashMap<String, Long> loadMap(String path) {
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
