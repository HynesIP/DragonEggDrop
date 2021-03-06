/*
    DragonEggDrop
    Copyright (C) 2016  NinjaStix
    ninjastix84@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.ninjaguild.dragoneggdrop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;

import com.ninjaguild.dragoneggdrop.commands.DragonEggDropCmd;
import com.ninjaguild.dragoneggdrop.events.DragonLifeListeners;
import com.ninjaguild.dragoneggdrop.events.LootListeners;
import com.ninjaguild.dragoneggdrop.events.PortalClickListener;
import com.ninjaguild.dragoneggdrop.events.RespawnListeners;
import com.ninjaguild.dragoneggdrop.loot.LootEntry;
import com.ninjaguild.dragoneggdrop.utils.ConfigUtil;
import com.ninjaguild.dragoneggdrop.utils.manager.DEDManager;
import com.ninjaguild.dragoneggdrop.utils.versions.NMSAbstract;
import com.ninjaguild.dragoneggdrop.utils.versions.v1_10.NMSAbstract1_10_R1;
import com.ninjaguild.dragoneggdrop.utils.versions.v1_11.NMSAbstract1_11_R1;
import com.ninjaguild.dragoneggdrop.utils.versions.v1_9.NMSAbstract1_9_R1;
import com.ninjaguild.dragoneggdrop.utils.versions.v1_9.NMSAbstract1_9_R2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * DragonEggDrop, reward your players with a dragon egg/loot chest 
 * after every ender dragon battle, in grand fashion!
 * 
 * @author NinjaStix
 * @author Parker Hawke - 2008Choco (Maintainer)
 */
public class DragonEggDrop extends JavaPlugin {
	
	private static final String CHAT_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "DED" + ChatColor.DARK_GRAY + "] ";
	
	static {
		ConfigurationSerialization.registerClass(LootEntry.class);
	}
	
	private static final int RESOURCE_ID = 35570;
	private static final String SPIGET_LINK = "https://api.spiget.org/v2/resources/" + RESOURCE_ID + "/versions/latest";
	
	private boolean newVersionAvailable = false;
	private String newVersion;

	private DEDManager dedManager;
	private NMSAbstract nmsAbstract;
	
	private BukkitTask updateTask;

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		// Update configuration version
		ConfigUtil cu = new ConfigUtil(this);
		cu.updateConfig(this.getConfig().getInt("version"));
		
		// Setup version abstraction
		if (!this.setupNMSAbstract()) {
			this.getLogger().severe("THE CURRENT SERVER VERSION IS NOT SUPPORTED. BOTHER THE MAINTAINER");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		try {
			Particle.valueOf(getConfig().getString("Particles.type", "FLAME").toUpperCase());
		} catch (IllegalArgumentException ex) {
			this.getLogger().log(Level.WARNING, "INVALID PARTICLE TYPE SPECIFIED! DISABLING...");
			this.getLogger().log(Level.INFO, "PLUGIN DISABLED");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		this.dedManager = new DEDManager(this);
		
		// Register events
		Bukkit.getPluginManager().registerEvents(new DragonLifeListeners(this), this);
		Bukkit.getPluginManager().registerEvents(new LootListeners(this), this);
		Bukkit.getPluginManager().registerEvents(new RespawnListeners(this), this);
		Bukkit.getPluginManager().registerEvents(new PortalClickListener(this), this);

		// Register commands
		this.getCommand("dragoneggdrop").setExecutor(new DragonEggDropCmd(this));
		
		// Update check
		if (this.getConfig().getBoolean("perform-update-checks", true)) {
			this.updateTask = new BukkitRunnable() {
				@Override
				public void run() {
					boolean previousState = newVersionAvailable;
					doVersionCheck();
					
					// New version found
					if (previousState != newVersionAvailable) {
						Bukkit.getOnlinePlayers().forEach(p -> {
							if (p.isOp()) sendMessage(p, ChatColor.GRAY + "A new version is available for download (Version " + newVersion + "). ");
						});
					}
				}
			}.runTaskTimerAsynchronously(this, 0, 36000);
		}
	}
	
	@Override
	public void onDisable() {
		if (this.updateTask != null)
			this.updateTask.cancel();
		
		this.dedManager.getDragonTemplates().clear();
		this.dedManager.getLootManager().getLoot().clear();
		
		// Clear the world wrappers
		this.dedManager.getWorldWrappers().forEach((u, w) -> {
			w.stopRespawn();
			w.cancelAnnounce();
		});
		this.dedManager.getWorldWrappers().clear();
	}
	
	/**
	 * Send a message to a command sender with the DragonEggDrop chat prefix
	 * 
	 * @param sender - The sender to send the message to
	 * @param message - The message to send
	 */
	public void sendMessage(CommandSender sender, String message) {
		sender.sendMessage(CHAT_PREFIX + message);
	}
	
	/**
	 * Get the main DEDManager instance
	 * 
	 * @return the DEDManager instance
	 */
	public DEDManager getDEDManager() {
		return dedManager;
	}
	
	/**
	 * Get the current implementation of the NMSAbstract interface
	 * 
	 * @return the NMSAbstract interface
	 */
	public NMSAbstract getNMSAbstract() {
		return nmsAbstract;
	}
	
	/**
	 * Get whether there is a new version available and ready for
	 * download or not
	 * 
	 * @return true if available
	 */
	public boolean isNewVersionAvailable() {
		return newVersionAvailable;
	}
	
	/**
	 * Get the version of the available update (if one exists). 
	 * 
	 * @see #isNewVersionAvailable()
	 * @return the new version
	 */
	public String getNewVersion() {
		return newVersion;
	}
	
	private void doVersionCheck() {
		Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(SPIGET_LINK).openStream()))){

				JSONParser parser = new JSONParser();
				JSONObject json = (JSONObject) parser.parse(reader);
				
				String currentVersion = getDescription().getVersion(), recentVersion = (String) json.get("name");
				
				if (!currentVersion.equals(recentVersion)) {
					getLogger().info("New version available. Your Version = " + currentVersion + ". New Version = " + recentVersion);
					newVersionAvailable = true;
					newVersion = recentVersion;
				}
			}catch(IOException e){
				getLogger().info("Could not check for a new version. Perhaps the website is down?");
			} catch (ParseException e) {
				getLogger().info("There was an issue parsing JSON formatted data. If issues continue, please put in a ticket on the "
						+ "DragonEggDrop Revival development page with the following stacktrace");
				e.printStackTrace();
			}
		});
	}
	
	private final boolean setupNMSAbstract(){
		String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        if (version.equalsIgnoreCase("v1_9_R1")){ // 1.9.0 - 1.9.3
        	this.nmsAbstract = new NMSAbstract1_9_R1();
        }else if (version.equalsIgnoreCase("v1_9_R2")){ // 1.9.4
        	this.nmsAbstract = new NMSAbstract1_9_R2();
        }else if (version.equalsIgnoreCase("v1_10_R1")){ // 1.10.0 - 1.10.2
        	this.nmsAbstract = new NMSAbstract1_10_R1();
        }else if (version.equalsIgnoreCase("v1_11_R1")){ // 1.11.0 - 1.11.2
        	this.nmsAbstract = new NMSAbstract1_11_R1();
        }
        
        return this.nmsAbstract != null;
	}
}