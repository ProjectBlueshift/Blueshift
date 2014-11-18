/*
 *     TridentSDK - A Minecraft Server API
 *     Copyright (C) 2014, The TridentSDK Team
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.tridentsdk.event.player;

import net.tridentsdk.entity.living.Player;
import net.tridentsdk.world.World;

/**
 * Called when a player changes worlds
 */
public class PlayerChangeWorldEvent extends PlayerEvent {
    private final World to;
    private final World from;

    public PlayerChangeWorldEvent(Player player, World from, World to) {
        super(player);
        this.to = to;
        this.from = from;
    }

    public World getTo() {
        return this.to;
    }

    public World getFrom() {
        return this.from;
    }
}
