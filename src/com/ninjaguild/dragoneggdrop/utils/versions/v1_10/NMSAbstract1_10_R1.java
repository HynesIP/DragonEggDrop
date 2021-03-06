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

package com.ninjaguild.dragoneggdrop.utils.versions.v1_10;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

import com.ninjaguild.dragoneggdrop.utils.versions.NMSAbstract;

import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftChest;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftEnderDragon;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_10_R1.BossBattleServer;
import net.minecraft.server.v1_10_R1.ChatMessage;
import net.minecraft.server.v1_10_R1.EnderDragonBattle;
import net.minecraft.server.v1_10_R1.Entity;
import net.minecraft.server.v1_10_R1.EntityEnderDragon;
import net.minecraft.server.v1_10_R1.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.BossBattle;
import net.minecraft.server.v1_10_R1.PacketPlayOutBoss;
import net.minecraft.server.v1_10_R1.PacketPlayOutChat;
import net.minecraft.server.v1_10_R1.WorldProvider;
import net.minecraft.server.v1_10_R1.WorldProviderTheEnd;
import net.minecraft.server.v1_10_R1.WorldServer;

/**
 * An abstract implementation of necessary net.minecraft.server and
 * org.bukkit.craftbukkit methods that vary between versions causing
 * version dependencies. Allows for version independency through
 * abstraction per Bukkit/Spigot release
 * <p>
 * <b><i>Supported Minecraft Versions:</i></b> 1.10.0, 1.10.1 and 1.10.2
 * 
 * @author Parker Hawke - 2008Choco
 */
public class NMSAbstract1_10_R1 implements NMSAbstract {
	
	@Override
	public void setDragonBossBarTitle(String title, Object battle) {
		if (battle == null || title == null || !(battle instanceof EnderDragonBattle)) return;
		
		EnderDragonBattle dragonBattle = (EnderDragonBattle) battle;
		try {
			Field field = EnderDragonBattle.class.getDeclaredField("c");
			field.setAccessible(true);
			BossBattleServer battleServer = (BossBattleServer) field.get(dragonBattle);
			battleServer.title = new ChatMessage(title);
			battleServer.sendUpdate(PacketPlayOutBoss.Action.UPDATE_NAME);
			field.setAccessible(false);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object getEnderDragonBattleFromWorld(World world) {
		if (world == null) return null;
		
		CraftWorld craftWorld = (CraftWorld) world;
		WorldProvider worldProvider = craftWorld.getHandle().worldProvider;
		
		if (!(worldProvider instanceof WorldProviderTheEnd)) return null;
		return ((WorldProviderTheEnd) worldProvider).s();
	}

	@Override
	public Object getEnderDragonBattleFromDragon(EnderDragon dragon) {
		if (dragon == null) return null;
		
		EntityEnderDragon nmsDragon = ((CraftEnderDragon) dragon).getHandle();
		return nmsDragon.cZ();
	}
	
	@Override
	public boolean setBattleBossBarStyle(Object battle, BarStyle style, BarColor colour) {
		if ((battle == null || !(battle instanceof EnderDragonBattle))) return false;
		
		EnderDragonBattle dragonBattle = (EnderDragonBattle) battle;
		try {
			Field field = EnderDragonBattle.class.getDeclaredField("c");
			field.setAccessible(true);
			
			BossBattleServer battleServer = (BossBattleServer) field.get(dragonBattle);
			if (battleServer == null) return false;
			
			if (style != null) {
				String nmsStyle = style.name().contains("SEGMENTED") ? style.name().replace("SEGMENTED", "NOTCHED") : "SOLID";
				if (!EnumUtils.isValidEnum(BossBattle.BarStyle.class, nmsStyle)) {
					return false;
				}
				
				battleServer.style = BossBattle.BarStyle.valueOf(nmsStyle);
			}
			if (colour != null) battleServer.color = BossBattle.BarColor.valueOf(colour.name());
			battleServer.sendUpdate(PacketPlayOutBoss.Action.UPDATE_STYLE);
			
			field.setAccessible(false);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	@Override
	public EnderDragon getEnderDragonFromBattle(Object battle) {
		if (battle == null || !(battle instanceof EnderDragonBattle)) return null;
		
		EnderDragon dragon = null;
		EnderDragonBattle dragonBattle = (EnderDragonBattle) battle;
		try {
			Field fieldWorldServer = EnderDragonBattle.class.getDeclaredField("d");
			Field fieldDragonUUID = EnderDragonBattle.class.getDeclaredField("m");
			fieldWorldServer.setAccessible(true);
			fieldDragonUUID.setAccessible(true);
			
			WorldServer world = (WorldServer) fieldWorldServer.get(dragonBattle);
			UUID dragonUUID = (UUID) fieldDragonUUID.get(dragonBattle);
			
			if (world == null || dragonUUID == null) 
				return null;
			
			Entity dragonEntity = world.getEntity(dragonUUID);
			if (dragonEntity == null) return null;
			dragon = (EnderDragon) dragonEntity.getBukkitEntity();
			
			fieldWorldServer.setAccessible(false);
			fieldDragonUUID.setAccessible(false);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return dragon;
	}

	@Override
	public void respawnEnderDragon(Object battle) {
		if (!(battle instanceof EnderDragonBattle)) return;
		
		EnderDragonBattle dragonBattle = (EnderDragonBattle) battle;
		dragonBattle.e();
	}

	@Override
	public boolean hasBeenPreviouslyKilled(EnderDragon dragon) {
		if (dragon == null) return false;
		
		EnderDragonBattle battle = (EnderDragonBattle) this.getEnderDragonBattleFromDragon(dragon);
		return battle.d();
	}
	
	@Override
	public int getEnderDragonDeathAnimationTime(EnderDragon dragon) {
		if (dragon == null) return -1;
		
		EntityEnderDragon nmsDragon = ((CraftEnderDragon) dragon).getHandle();
		return nmsDragon.bH;
	}
	
	@Override
	public Location getEndPortalLocation(Object battle) {
		if (battle == null || !(battle instanceof EnderDragonBattle)) return null;
		
		Location portalLocation = null;
		EnderDragonBattle dragonBattle = (EnderDragonBattle) battle;
		try {
			Field fieldExitPortalLocation = EnderDragonBattle.class.getDeclaredField("o");
			Field fieldWorldServer = EnderDragonBattle.class.getDeclaredField("d");
			fieldExitPortalLocation.setAccessible(true);
			fieldWorldServer.setAccessible(true);
			
			WorldServer worldServer = (WorldServer) fieldWorldServer.get(dragonBattle);
			BlockPosition position = (BlockPosition) fieldExitPortalLocation.get(dragonBattle);
			if (worldServer != null && position != null) {
				World world = worldServer.getWorld();
				portalLocation = new Location(world, Math.floor(position.getX()) + 0.5, position.getY() + 4, Math.floor(position.getZ()) + 0.5);
			}
			
			fieldWorldServer.setAccessible(false);
			fieldExitPortalLocation.setAccessible(false);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return portalLocation;
	}

	@Override
	public void setChestName(Chest chest, String name) {
		if (chest == null || name == null) return;
		
		CraftChest craftChest = (CraftChest) chest;
		craftChest.getTileEntity().a(name);
	}

	@Override
	public void sendActionBar(String message, Player... players) {
		PacketPlayOutChat packet = new PacketPlayOutChat(ChatSerializer.a("{\"text\":\"" + message + "\"}"), (byte) 2);
		Arrays.stream(players).forEach(p -> ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet));
	}

	@Override
	public void broadcastActionBar(String message, World world) {
		PacketPlayOutChat packet = new PacketPlayOutChat(ChatSerializer.a("{\"text\":\"" + message + "\"}"), (byte) 2);
		world.getPlayers().forEach(p -> ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet));
	}
}