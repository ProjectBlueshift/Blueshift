/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tridentsdk.server.entity.block;

import net.tridentsdk.Position;
import net.tridentsdk.base.BlockSnapshot;
import net.tridentsdk.entity.traits.EntityProperties;
import net.tridentsdk.entity.types.EntityType;
import net.tridentsdk.entity.block.FallingBlock;
import net.tridentsdk.server.entity.TridentEntity;

import java.util.UUID;

public class TridentFallingBlock extends TridentEntity implements FallingBlock {
	
	private boolean drop = true;
	
    public TridentFallingBlock(UUID id, Position spawnLocation) {
        super(id, spawnLocation);
    }

    @Override
    public BlockSnapshot getSnapshot() {
        return null;
    }

    @Override
    public boolean shouldDrop() {
        return drop;
    }

	@Override
	public void setShouldDrop(boolean drop) {
		this.drop = drop;
	}

    @Override
    public boolean isNameVisible() {
        return false;
    }

    @Override
    public void applyProperties(EntityProperties properties) {

    }

    @Override
    public EntityType getType() {
        return EntityType.FALLING_BLOCK;
    }
    
}
