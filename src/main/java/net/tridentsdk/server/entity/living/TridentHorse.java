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

package net.tridentsdk.server.entity.living;

import net.tridentsdk.Position;
import net.tridentsdk.entity.*;
import net.tridentsdk.entity.living.Horse;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.event.entity.EntityDamageEvent;
import net.tridentsdk.server.entity.TridentLivingEntity;
import net.tridentsdk.window.inventory.Inventory;
import net.tridentsdk.window.inventory.Item;

import java.util.UUID;

public class TridentHorse extends TridentLivingEntity implements Horse {
    public TridentHorse(UUID id, Position spawnLocation) {
        super(id, spawnLocation);
    }

    @Override
    public HorseType breed() {
        return null;
    }

    @Override
    public boolean isGrazing() {
        return false;
    }

    @Override
    public int temper() {
        return 0;
    }

    @Override
    public boolean hasChest() {
        return false;
    }

    @Override
    public HorseVariant variant() {
        return null;
    }

    @Override
    public boolean isSitting() {
        return false;
    }

    @Override
    public int age() {
        return 0;
    }

    @Override
    public void setAge(int ticks) {

    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public boolean isInLove() {
        return false;
    }

    @Override
    public Inventory inventory() {
        return null;
    }

    @Override
    public Item heldItem() {
        return null;
    }

    @Override
    public void setHeldItem(Item item) {

    }

    @Override
    public boolean isSaddled() {
        return false;
    }

    @Override
    public void setSaddled(boolean saddled) {

    }

    @Override
    public boolean isTamed() {
        return false;
    }

    @Override
    public UUID owner() {
        return null;
    }

    @Override
    public void hide(Entity entity) {

    }

    @Override
    public void show(Entity entity) {

    }

    @Override
    public EntityDamageEvent lastDamageEvent() {
        return null;
    }

    @Override
    public Player lastPlayerDamager() {
        return null;
    }

    @Override
    public boolean isNameVisible() {
        return false;
    }

    @Override
    public void applyProperties(EntityProperties properties) {

    }

    @Override
    public <T extends Projectile> T launchProjectile(EntityProperties properties) {
        return null;
    }
}
