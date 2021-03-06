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

package com.ninjaguild.dragoneggdrop.utils;

import java.util.HashMap;
import java.util.Map;

import com.ninjaguild.dragoneggdrop.utils.MathUtils.MathExpression;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

/**
 * Represents a defined particle shape. Allows for easy shape-creation with two
 * expressions along the x and z axis, as well as an initial location to start
 * the shape
 * 
 * @author Parker Hawke - 2008Choco
 */
public class ParticleShapeDefinition {
	
	private final Map<String, Double> variables = new HashMap<>();
	
	private final Location initialLocation;
	private final World world;
	private final MathExpression xExpression, zExpression;
	
	public ParticleShapeDefinition(Location initialLocation, String xExpression, String zExpression) {
		this.variables.put("x", 0.0);
		this.variables.put("z", 0.0);
		this.variables.put("t", 0.0);
		this.variables.put("theta", 0.0);
		
		this.initialLocation = initialLocation;
		this.world = initialLocation.getWorld();
		this.xExpression = MathUtils.parseExpression(xExpression, variables);
		this.zExpression = MathUtils.parseExpression(zExpression, variables);
	}
	
	public void updateVariables(double x, double z, double t, double theta) {
		this.variables.put("x", x);
		this.variables.put("z", z);
		this.variables.put("t", t);
		this.variables.put("theta", theta);
	}
	
	public void executeExpression(Particle particleType, int particleAmount, double xOffset, double yOffset, double zOffset, double particleExtra) {
		double x = this.xExpression.evaluate(), z = this.zExpression.evaluate();
		
		this.initialLocation.add(x, 0, z);
		this.world.spawnParticle(particleType, this.initialLocation, particleAmount, xOffset, yOffset, zOffset, particleExtra, null);
		this.initialLocation.subtract(x, 0, z);
	}
}