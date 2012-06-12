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
	
	/**
	 * Bugs I can think of:
	 * 	-Secondary data that is used for some blocks, i.e. red wool (35:14), not be an int, 
	 * 		so need a test to see if there is secondary data, in the ItemPicker class.
	 *
	 * Improvements:
	 *  -Config file can set time between rewards
	 *  -Config can set whether or not the plugin is enabled
	 *  -Config should set a message given to players when they receive rewards
	 *  -Add a way of adding money as a reward
	 *  -Edit config file through commands
	 */
	
	Map<String, Long> validJoins;
	
	//Everything starts here
	@Override
	public void onEnable(){ 
		File file = new File("plugins" + File.separator + "RewardPlugin" + File.separator + "config.yml");
		if(!file.exists()){
			saveDefaultConfig();
		}
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
		
		//Create new data for new players
		if(!validJoins.containsKey(player.getName()))
			validJoins.put(player.getName(), System.currentTimeMillis());
		
		long lastValidJoin = validJoins.get(player.getName());
		//86400000 for 24 hours
		if(System.currentTimeMillis() - lastValidJoin >= 10000){
			//Reset timer, and reward player
			validJoins.put(player.getName(), System.currentTimeMillis());
			ItemPicker.pickItem(player, this);
		}
		
		//Send the player a friendly message.
		getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run(){
				player.sendMessage("Here are your rewards for today");
			}
		}, 20L);
		
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
