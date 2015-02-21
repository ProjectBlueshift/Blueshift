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

package net.tridentsdk.server.world;

import com.google.common.collect.Lists;
import net.tridentsdk.Position;
import net.tridentsdk.base.Block;
import net.tridentsdk.base.Substance;
import net.tridentsdk.concurrent.TaskExecutor;
import net.tridentsdk.meta.nbt.*;
import net.tridentsdk.server.packets.play.out.PacketPlayOutChunkData;
import net.tridentsdk.server.threads.ThreadsHandler;
import net.tridentsdk.util.TridentLogger;
import net.tridentsdk.world.Chunk;
import net.tridentsdk.world.ChunkLocation;
import net.tridentsdk.world.ChunkSnapshot;
import net.tridentsdk.world.Dimension;
import net.tridentsdk.world.gen.AbstractGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class TridentChunk implements Chunk {
    private final TridentWorld world;
    private final ChunkLocation location;
    private final ByteArrayOutputStream data = new ByteArrayOutputStream();
    private volatile int lastFileAccess;
    private volatile long lastModified;
    private volatile long inhabitedTime;
    private volatile byte lightPopulated;
    private volatile byte terrainPopulated;
    public volatile ChunkSection[] sections;

    private final TaskExecutor executor = ThreadsHandler.chunkExecutor().assign(this);

    protected TridentChunk(TridentWorld world, int x, int z) {
        this(world, ChunkLocation.create(x, z));
    }

    protected TridentChunk(TridentWorld world, ChunkLocation coord) {
        this.world = world;
        this.location = coord;
        this.lastFileAccess = 0;
        sections = new ChunkSection[16];
        /*for (int i = 0; i < 16; i ++) {
            sections[i] = new ChunkSection();
        }*/
    }

    protected int lastFileAccess() {
        return this.lastFileAccess;
    }

    protected void setLastFileAccess(int last) {
        this.lastFileAccess = last;
    }

    @Override
    public void generate() {
        executor.addTask(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 16; i ++) {
                    if (sections[i] == null) {
                        sections[i] = new ChunkSection();
                    }
                }

                // TODO add flag to prevent double generation
                AbstractGenerator generator = world.loader().generator();
                int i = 0;

                for (char[] blockData: generator.generateChunkBlocks(location)) {
                    sections[i].setBlocks(blockData);
                    i++;
                }

                i = 0;

                for (byte[] dataValues: generator.generateBlockData(location)) {
                    sections[i].setData(dataValues);
                    i++;
                }

                for (ChunkSection section: sections) {
                    if (section.blockLight == null) {
                        section.blockLight = new byte[ChunkSection.LENGTH/2];
                    }

                    if (section.skyLight == null) {
                        section.skyLight = new byte[ChunkSection.LENGTH/2];
                    }

                    if (section.types == null) {
                        section.types = new char[ChunkSection.LENGTH];
                    }
                }

                // DEBUG ===== Makes the entire chunk full brightness, not exactly ideal
                for (i = 0; i < 16; i++) {
                    Arrays.fill(sections[i].skyLight, (byte) 255);
                }
                // =====

                //TODO lighting
            }
        });
    }

    @Override
    public int x() {
        return location.x();
    }

    @Override
    public int z() {
        return location.z();
    }

    @Override
    public ChunkLocation location() {
        return this.location;
    }

    @Override
    public TridentWorld world() {
        return this.world;
    }

    @Override
    public Block blockAt(final int relX, final int y, final int relZ) {
        final int index = WorldUtils.blockArrayIndex(relX, y % 16, relZ);

        try {
            return executor.submitTask(new Callable<TridentBlock>() {
                @Override
                public TridentBlock call() throws Exception {
                    ChunkSection section = sections[WorldUtils.section(y)];

                    /* Get block data; use extras accordingly */
                    byte b = (byte) (section.types[index] >> 4);
                    byte meta = (byte) (section.types[index] & 0xF);

                    Substance material = Substance.fromId(b);

                    if (material == null) {
                        material = Substance.AIR; // check if valid
                    }

                    return new TridentBlock(Position.create(world, relX + x() * 16, y, relZ + z() * 16),
                            material, meta);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            TridentLogger.error(e);
            return null;
        }
    }

    @Override
    public ChunkSnapshot snapshot() {
        final List<CompoundTag> sections = Lists.newArrayList();

        executor.addTask(new Runnable() {
            @Override
            public void run() {
                for (ChunkSection section : TridentChunk.this.sections) {
                    sections.add(NBTSerializer.serialize(section));
                }
            }
        });

        return new TridentChunkSnapshot(world, location, sections, lastFileAccess, lastModified, inhabitedTime,
                lightPopulated, terrainPopulated);
    }

    public PacketPlayOutChunkData asPacket() {
        try {
            return executor.submitTask(new Callable<PacketPlayOutChunkData>() {
                @Override
                public PacketPlayOutChunkData call() throws Exception {
                    PacketPlayOutChunkData packet = new PacketPlayOutChunkData();

                    int bitmask = (1 << sections.length) - 1;
                    int count = sections.length;
                    int size = 0;
                    int sectionSize = ChunkSection.LENGTH * 5 / 2;

                    if (world.dimension() == Dimension.OVERWORLD)
                        sectionSize += ChunkSection.LENGTH / 2;

                    size += count * sectionSize + 256;

                    //byte[] data = new byte[size];
                    //int pos = 0;

                    for (ChunkSection section : sections) {
                        if (section == null)
                            continue;

                        for (char c : section.types()) {
                            data.write(c & 0xff);
                            data.write(c >> 8);
                        }
                    }

                    for (ChunkSection section : sections) {
                        try {
                            data.write(section.blockLight);
                        } catch (IOException e) {
                            TridentLogger.error(e);
                        }
                    }

                    for (ChunkSection section : sections) {
                        try {
                            data.write(section.skyLight);
                        } catch (IOException e) {
                            TridentLogger.error(e);
                        }
                    }

                    for (int i = 0; i < 256; i += 1) {
                        data.write(0);
                    }

        /*if (pos != size) {
            TridentLogger.error(new IllegalArgumentException("Pos: " + pos + " does not equal size: " + size));
            return null;
        } */

                    packet.set("chunkLocation", location);
                    packet.set("bitmask", (short) bitmask);
                    packet.set("data", data.toByteArray());

                    data.reset();

                    return packet;
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            TridentLogger.error(e);
            return null;
        }
    }

    public void load(CompoundTag root) {
        CompoundTag tag = root.getTagAs("Level");
        LongTag lastModifed = tag.getTagAs("LastUpdate");
        ByteTag lightPopulated = (tag.containsTag("LightPopulated")) ? (ByteTag) tag.getTagAs(
                "LightPopulated") : new ByteTag("LightPopulated").setValue((byte) 0);
        ByteTag terrainPopulated = tag.getTagAs("TerrainPopulated");

        LongTag inhabitedTime = tag.getTagAs("InhabitedTime");
        IntArrayTag biomes = tag.getTagAs("HeightMap");

        final ListTag sectionTags = tag.getTagAs("Sections");
        ListTag entities = tag.getTagAs("Entities");
        ListTag tileEntities = (tag.containsTag("TileEntities")) ? (ListTag) tag.getTag("TileEntities") :
                new ListTag("TileEntities", TagType.COMPOUND);
        ListTag tileTicks = (tag.containsTag("TileTicks")) ? (ListTag) tag.getTag("TileTicks") : new ListTag(
                "TileTicks", TagType.COMPOUND);
        final List<NBTTag> sectionsList = sectionTags.listTags();

        final ChunkSection[] sections = new ChunkSection[sectionsList.size()];

                /* Load sections */
        for (int i = 0; i < sectionsList.size(); i += 1) {
            NBTTag t = sectionTags.getTag(i);

            if (t instanceof CompoundTag) {
                CompoundTag ct = (CompoundTag) t;

                ChunkSection section = NBTSerializer.deserialize(ChunkSection.class, ct);

                section.loadBlocks();
                sections[section.y()] = section;
            }
        }

        executor.addTask(new Runnable() {
            @Override
            public void run() {
                TridentChunk.this.sections = sections;
            }
        });

        for (NBTTag t : entities.listTags()) {
            //TridentEntity entity = EntityBuilder.create().build(TridentEntity.class);

            //entity.load((CompoundTag) t);
            //world.entities().add(entity);
        }

        /* Load extras */
        this.lightPopulated = lightPopulated.value(); // Unknown use
        this.terrainPopulated = terrainPopulated.value(); // if chunk was populated with special things (ores,
        // trees, etc.), if 1 regenerate
        this.lastModified = lastModifed.value(); // Tick when the chunk was last saved
        this.inhabitedTime = inhabitedTime.value(); // Cumulative number of ticks player have been in the chunk
    }

    public CompoundTag asNbt() {
        CompoundTag root = new CompoundTag("root");
        CompoundTag level = new CompoundTag("Level");

        level.addTag(new LongTag("LastUpdate").setValue(world.time()));
        level.addTag(new ByteTag("LightPopulated").setValue(lightPopulated));
        level.addTag(new ByteTag("TerrainPopulated").setValue(terrainPopulated));

        level.addTag(new LongTag("InhabitedTime").setValue(inhabitedTime));
        level.addTag(new IntArrayTag("HeightMap").setValue(new int[1024])); // placeholder TODO

        final ListTag sectionTags = new ListTag("Sections", TagType.COMPOUND);

        ChunkSection[] sectionCopy = null;
        try {
            sectionCopy = executor.submitTask(new Callable<ChunkSection[]>() {
                @Override
                public ChunkSection[] call() throws Exception {
                    return sections;
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            sectionCopy = sections;
            e.printStackTrace();
        }

        for (ChunkSection section : sectionCopy) {
            section.updateRaw();
            sectionTags.addTag(NBTSerializer.serialize(section));
        }

        level.addTag(sectionTags);
        level.addTag(new ListTag("Entities", TagType.COMPOUND)); // another placeholder TODO

        root.addTag(level);

        return root;
    }

    public void setAt(Position p, Substance type, byte metaData, byte skyLight, byte blockLight) {
        setAt((int) p.x(), (int) p.y(), (int) p.z(), type, metaData, skyLight, blockLight);
    }

    public void setAt(int x, final int y, int z, final Substance type, final byte metaData, final byte skyLight,
                      final byte blockLight) {
        final int index = WorldUtils.blockArrayIndex(x % 16, y % 16, z % 16);
        executor.addTask(new Runnable() {
            @Override
            public void run() {
                ChunkSection section = sections[WorldUtils.section(y)];

                section.types[index] = (char) ((type.asExtended() & 0xfff0) | metaData);
                section.skyLight[index] = skyLight;
                section.blockLight[index] = blockLight;
            }
        });
    }
}
