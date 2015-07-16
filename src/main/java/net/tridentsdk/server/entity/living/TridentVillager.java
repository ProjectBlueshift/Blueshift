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

import java.util.List;
import java.util.UUID;

import net.tridentsdk.Position;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.entity.living.Villager;
import net.tridentsdk.entity.types.EntityType;
import net.tridentsdk.entity.types.VillagerCareer;
import net.tridentsdk.entity.types.VillagerProfession;
import net.tridentsdk.event.entity.EntityDamageEvent;
import net.tridentsdk.server.entity.TridentLivingEntity;
import net.tridentsdk.window.trade.Trade;

/**
 * Represents a Villager
 *
 * @author The TridentSDK Team
 */
public class TridentVillager extends TridentLivingEntity implements Villager {
    private volatile VillagerCareer career;
    private volatile VillagerProfession role;

    public TridentVillager(UUID uuid, Position spawnPosition, VillagerCareer career, VillagerProfession role) {
        super(uuid, spawnPosition);
        this.career = career;
        this.role = role;
    }

    @Override
    public VillagerProfession getProfession() {
        return role;
    }

    @Override
    public void setProfession(VillagerProfession profession) {
        this.role = profession;
    }

    @Override
    public VillagerCareer getCareer() {
        return career;
    }

    @Override
    public void setCareer(VillagerCareer career) {
        this.career = career;
    }

    @Override
    public int getCareerLevel() {
        return 0;
    }

    @Override
    public int getAge() {
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
    public void setInLove(boolean love) {
    	
    }

    @Override
    public EntityDamageEvent getLastDamageEvent() {
        return null;
    }

    @Override
    public Player getLastPlayerDamager() {
        return null;
    }

    @Override
    public List<Trade> getTrades() {
        return null;
    }

    @Override
    public EntityType getType() {
        return EntityType.VILLAGER;
    }
}
