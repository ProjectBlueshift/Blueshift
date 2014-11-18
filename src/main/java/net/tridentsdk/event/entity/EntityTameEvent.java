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
package net.tridentsdk.event.entity;

import net.tridentsdk.entity.Entity;
import net.tridentsdk.entity.living.Player;

/**
 * Called when an entity is tamed, i.e. a horse or ocelot
 */
public class EntityTameEvent extends EntityEvent {
    private final Player tamer;
    private boolean cancelled;

    public EntityTameEvent(Entity entity, Player tamer) {
        super(entity);
        this.tamer = tamer;
    }

    public Player getTamer() {
        return this.tamer;
    }

    @Override
    public boolean isCancelled() {

        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
