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

package net.tridentsdk.server.player;

import net.tridentsdk.GameMode;
import net.tridentsdk.Position;
import net.tridentsdk.entity.Entity;
import net.tridentsdk.entity.EntityProperties;
import net.tridentsdk.entity.PlayerSpeed;
import net.tridentsdk.entity.Projectile;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.event.entity.EntityDamageEvent;
import net.tridentsdk.factory.Factories;
import net.tridentsdk.meta.nbt.*;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.data.Slot;
import net.tridentsdk.server.entity.TridentInventoryHolder;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.util.TridentLogger;
import net.tridentsdk.window.inventory.Inventory;
import net.tridentsdk.world.Dimension;
import net.tridentsdk.world.World;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@ThreadSafe
public class OfflinePlayer extends TridentInventoryHolder implements Player {
    private static final Set<OfflinePlayer> players = Factories.collect().createSet();

    protected String name;
    protected Dimension dimension;
    protected GameMode gameMode;
    protected int score;
    protected short selectedSlot;
    protected Position spawnLocation;
    protected short hunger;
    protected float exhaustion;
    protected float saturation;
    protected int foodTickTimer;
    protected int xpLevel;
    protected float xpPercent;
    protected int xpTotal;
    protected int xpSeed;
    protected boolean invulnerable;
    protected int portalCooldown;
    protected boolean onGround;
    protected boolean customNameVisible;
    protected int fireTicks;

    protected Inventory enderChest;
    protected PlayerAbilities abilities = new PlayerAbilities();
    protected PlayerSpeed playerSpeed = new PlayerSpeedImpl();

    public OfflinePlayer(CompoundTag tag, TridentWorld world) {
        super(null, world.spawnLocation());

        load(tag);

        dimension = Dimension.dimension(((IntTag) tag.getTag("Dimension")).value());
        gameMode = GameMode.gamemodeOf(((IntTag) tag.getTag("playerGameType")).value());
        score = ((IntTag) tag.getTag("Score")).value();
        selectedSlot = (short) ((IntTag) tag.getTag("SelectedItemSlot")).value();

        if (tag.containsTag("SpawnX")) {
            spawnLocation = Position.create(world, ((IntTag) tag.getTag("SpawnX")).value(),
                    ((IntTag) tag.getTag("SpawnY")).value(), ((IntTag) tag.getTag("SpawnZ")).value());
        } else {
            spawnLocation = world.spawnLocation();
        }

        hunger = (short) ((IntTag) tag.getTag("foodLevel")).value();
        exhaustion = ((FloatTag) tag.getTag("foodExhaustionLevel")).value();
        saturation = ((FloatTag) tag.getTag("foodSaturationLevel")).value();
        foodTickTimer = ((IntTag) tag.getTag("foodTickTimer")).value();
        xpLevel = ((IntTag) tag.getTag("XpLevel")).value();
        xpPercent = ((FloatTag) tag.getTag("XpP")).value();
        xpTotal = ((IntTag) tag.getTag("XpLevel")).value();
        xpSeed = tag.containsTag("XpSeed") ? ((IntTag) tag.getTag("XpSeed")).value() :
                new IntTag("XpSeed").setValue(0).value();

        for (NBTTag t : ((ListTag) tag.getTag("Inventory")).listTags()) {
            Slot slot = NBTSerializer.deserialize(Slot.class, (CompoundTag) t);

            //inventory.setSlot(slot.getSlot(), slot.toItemStack());
        }

        for (NBTTag t : ((ListTag) tag.getTag("EnderItems")).listTags()) {
            Slot slot = NBTSerializer.deserialize(Slot.class, (CompoundTag) t);

            //enderChest.setSlot(slot.getSlot(), slot.toItemStack());
        }

        NBTSerializer.deserialize(abilities, (CompoundTag) tag.getTag("abilities"));
        players.add(this);
    }

    public static OfflinePlayer getOfflinePlayer(UUID id) {
        for (OfflinePlayer player : players) {
            if (player.uniqueId().equals(id)) {
                return player;
            }
        }

        return null;
    }

    public static CompoundTag generatePlayer(UUID id) {
        // TODO this is temporary for testing
        World defaultWorld = TridentServer.WORLD;
        Position spawnLocation = defaultWorld.spawnLocation();
        CompoundTagBuilder<NBTBuilder> builder = NBTBuilder.newBase(id.toString());

        builder.stringTag("id", String.valueOf(counter.incrementAndGet()));
        builder.longTag("UUIDMost", id.getMostSignificantBits());
        builder.longTag("UUIDLeast", id.getLeastSignificantBits());

        ListTagBuilder<CompoundTagBuilder<NBTBuilder>> pos = builder.beginListTag("Pos", TagType.DOUBLE);

        pos.tag(spawnLocation.x());
        pos.tag(spawnLocation.y());
        pos.tag(spawnLocation.z());

        builder = pos.endListTag();

        ListTagBuilder<CompoundTagBuilder<NBTBuilder>> motion = builder.beginListTag("Motion", TagType.DOUBLE);

        motion.tag(0d);
        motion.tag(0d);
        motion.tag(0d);

        builder = motion.endListTag();

        ListTagBuilder<CompoundTagBuilder<NBTBuilder>> rotation = builder.beginListTag("Rotation", TagType.FLOAT);

        rotation.tag(0f);
        rotation.tag(0f);

        builder = rotation.endListTag();

        builder.floatTag("FallDistance", 0);
        builder.shortTag("Fire", (short) -20);
        builder.shortTag("Air", (short) 0);

        builder.byteTag("OnGround", (byte) 1);
        builder.byteTag("Invulnerable", (byte) 0);

        builder.intTag("Dimension", Dimension.OVERWORLD.asByte());
        builder.intTag("PortalCooldown", 900);

        builder.stringTag("CustomName", "");
        // does not apply to players
        //builder.byteTag("CustomNameVisible", (byte) 0);

        builder.byteTag("Silent", (byte) 0);

        builder.compoundTag(new CompoundTag("Riding"));

        builder.intTag("Dimension", Dimension.OVERWORLD.asByte());
        builder.intTag("playerGameType", GameMode.SURVIVAL.asByte());
        builder.intTag("Score", 0);
        builder.intTag("SelectedGameSlot", 0);

        builder.intTag("foodLevel", 20);
        builder.floatTag("foodExhaustionLevel", 0F);
        builder.floatTag("foodSaturationLevel", 0F);
        builder.intTag("foodTickTimer", 0);

        builder.intTag("XpLevel", 0);
        builder.floatTag("XpP", 0);
        builder.intTag("XpLevel", 0);
        builder.intTag("XpSeed", 0); // actually give a proper seed

        builder.listTag(new ListTag("Inventory", TagType.COMPOUND));
        builder.listTag(new ListTag("EnderItems", TagType.COMPOUND));

        builder.intTag("SelectedItemSlot", 0);

        builder.compoundTag(NBTSerializer.serialize(new PlayerAbilities(), "abilities"));

        return builder.endCompoundTag().build();
    }

    public Position spawnLocation() {
        return spawnLocation;
    }

    @Override
    public Locale locale() {
        return null;
    }

    @Override
    public GameMode gameMode() {
        return gameMode;
    }

    @Override
    public PlayerSpeed speedModifiers() {
        return playerSpeed;
    }

    @Override
    public void sendMessage(String message) {
        TridentLogger.error(new UnsupportedOperationException("You can't send messages to a non-existant player"));
    }

    @Override
    public void invokeCommand(String message) {
        TridentLogger.error(new UnsupportedOperationException("You cannot make an OfflinePlayer invoke a command!"));
    }

    @Override
    public String lastCommand() {
        return null;
    }

    @Override
    public void hide(Entity entity) {
        TridentLogger.error(new UnsupportedOperationException("You cannot hide an entity from an OfflinePlayer!"));
    }

    @Override
    public void show(Entity entity) {
        TridentLogger.error(new UnsupportedOperationException("You cannot show an entity to an OfflinePlayer!"));
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
    public void sendRaw(String... messages) {
        TridentLogger.error(new UnsupportedOperationException("You cannot send a message to an OfflinePlayer!"));
    }

    @Override
    public String lastMessage() {
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public <T extends Projectile> T launchProjectile(EntityProperties properties) {
        TridentLogger.error(new UnsupportedOperationException("You cannot make an OfflinePlayer launch a projectile!"));
        return null;
    }

    public CompoundTag asNbt() {
        CompoundTag tag = new CompoundTag(uniqueId().toString());

        tag.addTag(new LongTag("UUIDMost").setValue(uniqueId.getMostSignificantBits()));
        tag.addTag(new LongTag("UUIDLeast").setValue(uniqueId.getLeastSignificantBits()));

        tag.addTag(new IntTag("Dimension").setValue(dimension.asByte()));
        tag.addTag(new IntTag("playerGameType").setValue(gameMode.asByte()));
        tag.addTag(new IntTag("Score").setValue(score));
        tag.addTag(new IntTag("SelectedItemSlot").setValue(selectedSlot));

        //tag.addTag(NBTSerializer.serialize(new Slot(itemInHand())));
        tag.addTag(new IntTag("SpawnX").setValue((int) spawnLocation.x()));
        tag.addTag(new IntTag("SpawnY").setValue((int) spawnLocation.y()));
        tag.addTag(new IntTag("SpawnZ").setValue((int) spawnLocation.z()));

        tag.addTag(new IntTag("foodLevel").setValue(hunger));
        tag.addTag(new FloatTag("foodExhaustionLevel").setValue(exhaustion));
        tag.addTag(new FloatTag("foodSaturationLevel").setValue(saturation));
        tag.addTag(new IntTag("foodTickTimer").setValue(foodTickTimer));

        tag.addTag(new IntTag("XpLevel").setValue(xpLevel));
        tag.addTag(new FloatTag("XpP").setValue(xpPercent));
        tag.addTag(new IntTag("XpTotal").setValue(xpTotal));
        tag.addTag(new IntTag("XpSeed").setValue(xpSeed));

        tag.addTag(new ByteTag("Invulnerable").setValue(invulnerable));
        tag.addTag(new IntTag("PortalCooldown").setValue(portalCooldown));
        tag.addTag(new FloatTag("FallDistance").setValue(fallDistance.floatValue()));
        tag.addTag(new ByteTag("OnGround").setValue(onGround));
        tag.addTag(new ShortTag("Fire").setValue((short) fireTicks));
        tag.addTag(new ShortTag("Air").setValue((short) airTicks.get()));
        tag.addTag(new ByteTag("Silent").setValue(silent));
        tag.addTag(new IntTag("SelectedItemSlot").setValue(selectedSlot));

        ListTag position = new ListTag("Pos",TagType.DOUBLE);
        position.addTag(new DoubleTag("").setValue(loc.x()));
        position.addTag(new DoubleTag("").setValue(loc.y()));
        position.addTag(new DoubleTag("").setValue(loc.z()));

        tag.addTag(position);

        ListTag motion = new ListTag("Motion",TagType.DOUBLE) ;
        motion.addTag(new DoubleTag("").setValue(velocity.x()));
        motion.addTag(new DoubleTag("").setValue(velocity.y()));
        motion.addTag(new DoubleTag("").setValue(velocity.z()));

        tag.addTag(motion);

        ListTag rotation = new ListTag("Rotation", TagType.FLOAT);
        rotation.addTag(new FloatTag("").setValue(loc.yaw()));
        rotation.addTag(new FloatTag("").setValue(loc.pitch()));

        tag.addTag(rotation);

        ListTag inventoryTag = new ListTag("Inventory", TagType.COMPOUND);

        /*for (ItemStack is : inventory.items()) {
            inventoryTag.addTag(NBTSerializer.serialize(new Slot(is)));
        }*/

        tag.addTag(inventoryTag);

        ListTag enderTag = new ListTag("EnderItems", TagType.COMPOUND);

        /*for (ItemStack is : enderChest.items()) {
            enderTag.addTag(NBTSerializer.serialize(new Slot(is)));
        }*/

        tag.addTag(enderTag);
        tag.addTag(NBTSerializer.serialize(abilities, "abilities"));

        return tag;
    }

    class PlayerSpeedImpl implements PlayerSpeed {
        @Override
        public float flyingSpeed() {
            return abilities.flySpeed;
        }

        @Override
        public void setFlyingSpeed(float flyingSpeed) {
            TridentLogger.error(new UnsupportedOperationException("You may not set the flying speed of an OfflinePlayer!"));
        }

        @Override
        public float sneakSpeed() {
            return 0;
        }

        @Override
        public void setSneakSpeed(float speed) {

        }

        @Override
        public float walkSpeed() {
            return abilities.walkingSpeed;
        }

        @Override
        public void setWalkSpeed(float speed) {
            TridentLogger.error(
                    new UnsupportedOperationException("You may not set the walking speed of an OfflinePlayer!"));
        }
    }
}
