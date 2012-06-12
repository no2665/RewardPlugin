package com.github.no2665.RewardPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class RewardPlugin extends JavaPlugin implements Listener{
	
	/*****************
	 * ok, so player joins for first time. so PlayerTime = 0
	 * Then they play, and times increases.
	 * Log off.
	 * Log back on in a day or so. 
	 * just keep a map of the last valid join and the players
	 * if currentTime - LastJoin > 24hours then
	 * reward them with tools
	 * set a record of the time (the last valid join) that you rewarded them, 
	 * 			to be used later to measure 24 hours from.
	 * else 
	 * do not reward them, do not track that time
	 * 
	 * ok so, theres the plan.
	 * so get player time, etc, when player joins.
	 * Can then reward them if a valid join.
	 * Can also send them some sort of message?
	 * 
	 * in onEnable() load up a map from player to lastValidJoin (long?)
	 * when a player joins check map.
	 * if in the map
	 * 		check if valid join
	 * else
	 * 		create new entry with valid time set to time of joining server
	 * 
	 * right seems simple enough, don't need any scheduling then...
	 * 
	 * 
	 * 0    10  16 19   24 0    10
	 * |----|   |--|     /|-----|
	 * j    l   j  l      j     l
	 * clearly third join is valid
	 *****************/
	
	Map<String, Long> validJoins;
	
	/*@Override
	public void onLoad(){
		//probably not needed, but is called before onEnable
	}*/
	
	//Everything starts here
	@Override
	public void onEnable(){ 
		File file = new File("plugins" + File.separator + "RewardPlugin" + File.separator + "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
		Configuration config = getConfig();
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
		final Player player = event.getPlayer();
		Inventory inventory = player.getInventory();
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run(){
				player.sendMessage("Here are your rewards for today");
			}
		}, 20L);
		if(validJoins.containsKey(player.getName())){
			long lastValidJoin = validJoins.get(player.getName());
			//86400000 for 24 hours
			if(System.currentTimeMillis() - lastValidJoin >= 10000){
				//Reset timer, and reward player
				validJoins.put(player.getName(), System.currentTimeMillis());
				inventory.addItem(new ItemStack(Material.DIAMOND, 1));
				ItemPicker.pickItem(player, this);
			}
			
		}
		else{
			validJoins.put(player.getName(), System.currentTimeMillis());
			inventory.addItem(new ItemStack(Material.DIAMOND_PICKAXE, 1));
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
	public HashMap<String, Long> loadMap(String path) {
		try{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			Object result = ois.readObject();
			return (HashMap<String, Long>) result;
		}catch(Exception e){
		}
		return new HashMap<String, Long>();
	}
	
	//Used to deal with commands
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equals("echo")){
			if(args.length != 0){
				if(sender instanceof Player){
					String echoMessage = "";
					for(String string: args){
						echoMessage += string + " ";
					}
					sender.sendMessage("Here is your echo: " + echoMessage);
				}
				else{
					sender.sendMessage("You do not need to use this command, you fool!");
				}
				return true;
			}
			return false;
		}
		return false;
	}
}
