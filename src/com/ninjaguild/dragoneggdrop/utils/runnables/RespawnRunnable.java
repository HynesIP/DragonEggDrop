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

package com.ninjaguild.dragoneggdrop.utils.runnables;

import java.util.stream.Collectors;

import com.ninjaguild.dragoneggdrop.DragonEggDrop;
import com.ninjaguild.dragoneggdrop.api.BattleState;
import com.ninjaguild.dragoneggdrop.api.BattleStateChangeEvent;
import com.ninjaguild.dragoneggdrop.utils.manager.EndWorldWrapper;
import com.ninjaguild.dragoneggdrop.utils.versions.NMSAbstract;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a BukkitRunnable that handles the respawning of the 
 * Ender Dragon after it has been slain
 */
public class RespawnRunnable extends BukkitRunnable {

	private final DragonEggDrop plugin;
	private final EndWorldWrapper worldWrapper;
	private final Location portalLocation;
	private final NMSAbstract nmsAbstract;
	
	private final Object dragonBattle;
	private final EnderDragon dragon;
	
	private final Location[] crystalLocations;
	private int currentCrystal = 0;
	
	private int secondsUntilRespawn;
	
	/**
	 * Construct a new RespawnRunnable object
	 * 
	 * @param plugin - An instance of the DragonEggDrop plugin
	 * @param portalLocation - The location in which the egg is located
	 * @param respawnTime - The time in seconds until the respawn is executed
	 */
	public RespawnRunnable(final DragonEggDrop plugin, final Location portalLocation, final int respawnTime) {
		this.plugin = plugin;
		this.worldWrapper = plugin.getDEDManager().getWorldWrapper(portalLocation.getWorld());
		this.portalLocation = portalLocation;
		this.secondsUntilRespawn = respawnTime;
		this.nmsAbstract = plugin.getNMSAbstract();
		
		this.dragonBattle = nmsAbstract.getEnderDragonBattleFromWorld(portalLocation.getWorld());
		this.dragon = nmsAbstract.getEnderDragonFromBattle(dragonBattle);
		
		this.crystalLocations = new Location[] {
			portalLocation.clone().add(3, -3, 0),
			portalLocation.clone().add(0, -3, 3),
			portalLocation.clone().add(-3, -3, 0),
			portalLocation.clone().add(0, -3, -3)
		};
		
		// Event call
		BattleStateChangeEvent bscEventCrystals = new BattleStateChangeEvent(dragonBattle, dragon, BattleState.DRAGON_DEAD, BattleState.CRYSTALS_SPAWNING);
		Bukkit.getPluginManager().callEvent(bscEventCrystals);
	}

	@Override
	public void run() {
		if (this.secondsUntilRespawn > 0) {
			this.secondsUntilRespawn--;
			return;
		}
		
		// Only respawn if a Player is in the World
		World world = this.worldWrapper.getWorld();
		if (world.getPlayers().size() <= 0) return;
		
		// Start respawn process
		Location crystalLocation = this.crystalLocations[currentCrystal++];
		World crystalWorld = crystalLocation.getWorld();
		Chunk crystalChunk = crystalWorld.getChunkAt(crystalLocation);
		
		if (!crystalChunk.isLoaded()) 
			crystalChunk.load();
		
		// Kill any existing entities at this location
		for (Entity entity : crystalWorld.getNearbyEntities(crystalLocation, 1, 1, 1))
			entity.remove();
		
		EnderCrystal crystal = (EnderCrystal) crystalWorld.spawnEntity(crystalLocation, EntityType.ENDER_CRYSTAL);
		crystal.setShowingBottom(false);
		crystal.setInvulnerable(true);

		crystalWorld.createExplosion(crystalLocation.getX(), crystalLocation.getY(), crystalLocation.getZ(), 0F, false, false);
		crystalWorld.spawnParticle(Particle.EXPLOSION_HUGE, crystalLocation, 0);

//		dragonBattle.e(); ** REPLACED WITH NMSAbstract#respawnEnderDragon() **
		
		// All crystals respawned
		if (currentCrystal >= 4) {
			
			// If dragon already exists, cancel the respawn process
			if (crystalWorld.getEntitiesByClass(EnderDragon.class).size() >= 1) {
				plugin.getLogger().warning("An EnderDragon is already present in world " + crystalWorld.getName() + ". Dragon respawn cancelled");
				this.nmsAbstract.broadcastActionBar(ChatColor.RED + "Dragon respawn abandonned! Dragon already exists! Slay it!", crystalWorld);
				
				// Destroy all crystals
				for (Location location : this.crystalLocations) {
					Entity crystalToRemove = crystalWorld.getNearbyEntities(location, 1, 1, 1).stream()
							.filter(e -> e instanceof EnderCrystal)
							.collect(Collectors.toList()).get(0);
					
					crystalWorld.getPlayers().forEach(p -> p.playSound(portalLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1000, 1));
					crystalWorld.createExplosion(location.getX(), location.getY(), location.getZ(), 0F, false, false);
					crystalToRemove.remove();
				}
				
				this.cancel();
				return;
			}
			
			this.nmsAbstract.respawnEnderDragon(dragonBattle);
			this.worldWrapper.setRespawnInProgress(true);
			
			BattleStateChangeEvent bscEventRespawning = new BattleStateChangeEvent(dragonBattle, dragon, BattleState.CRYSTALS_SPAWNING, BattleState.DRAGON_RESPAWNING);
			Bukkit.getPluginManager().callEvent(bscEventRespawning);
			
			this.cancel();
		}
	}

	public int getSecondsUntilRespawn() {
		return secondsUntilRespawn;
	}
}
