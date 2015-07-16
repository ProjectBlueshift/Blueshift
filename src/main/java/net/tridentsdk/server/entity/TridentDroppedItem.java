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
package net.tridentsdk.server.entity;

import net.tridentsdk.Position;
import net.tridentsdk.entity.DroppedItem;
import net.tridentsdk.entity.types.EntityType;

import java.util.UUID;

/**
 * Represents an item that is dropped on the ground
 *
 * @author The TridentSDK Team
 */
public class TridentDroppedItem extends TridentEntity implements DroppedItem {
    public TridentDroppedItem(UUID uuid, Position spawnPosition) {
        super(uuid, spawnPosition);
    }

    @Override
    public int getAge() {
        return 0;
    }

    @Override
    public void setAge(int age) {

    }

//    Pretty sure Dropped Items don't have health, hm?
//
//    @Override
//    public short getHealth() {
//        return 0;
//    }
//
//    @Override
//    public void setHealth(short health) {
//
//    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public void setOwner(String owner) {

    }

    @Override
    public String dropper() {
        return null;
    }

    @Override
    public void setDropper(String dropper) {

    }

    @Override
    public EntityType getType() {
        return EntityType.ITEM;
    }
}
