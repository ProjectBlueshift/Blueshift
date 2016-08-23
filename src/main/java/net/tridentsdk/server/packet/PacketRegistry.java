/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2016 The TridentSDK Team
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
package net.tridentsdk.server.packet;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.tridentsdk.server.packet.handshake.HandshakeIn;
import net.tridentsdk.server.packet.login.*;
import net.tridentsdk.server.packet.play.PlayOutJoinGame;
import net.tridentsdk.server.packet.status.StatusInPing;
import net.tridentsdk.server.packet.status.StatusInRequest;
import net.tridentsdk.server.packet.status.StatusOutPong;
import net.tridentsdk.server.packet.status.StatusOutResponse;

import javax.annotation.concurrent.Immutable;
import java.util.Map;

import static net.tridentsdk.server.net.NetClient.NetState;
import static net.tridentsdk.server.packet.Packet.Bound;

/**
 * This class holds packets registered by their identifying
 * packet ID as specified in the Minecraft protocol.
 */
// TODO consider using Trove
@Immutable
public final class PacketRegistry {
    /**
     * The constructors used to instantiate the packets
     */
    private static final Map<Class<? extends Packet>, ConstructorAccess<? extends Packet>> CTORS =
            Maps.newHashMap();
    /**
     * Packet registry
     */
    private static final BiMap<Class<? extends Packet>, Integer> PACKETS =
            HashBiMap.create();

    // Initialization done in static initializer performed
    // whilst under lock during class initialization, thus
    // is threadsafe to design the registry this way

    static {
        put(HandshakeIn.class, NetState.HANDSHAKE, Bound.SERVER, 0x00);

        put(StatusInRequest.class, NetState.STATUS, Bound.SERVER, 0x00);
        put(StatusOutResponse.class, NetState.STATUS, Bound.CLIENT, 0x00);
        put(StatusInPing.class, NetState.STATUS, Bound.SERVER, 0x01);
        put(StatusOutPong.class, NetState.STATUS, Bound.CLIENT, 0x01);

        put(LoginInStart.class, NetState.LOGIN, Bound.SERVER, 0x00);
        put(LoginOutDisconnect.class, NetState.LOGIN, Bound.CLIENT, 0x00);
        put(LoginOutEncryptionRequest.class, NetState.LOGIN, Bound.CLIENT, 0x01);
        put(LoginInEncryptionResponse.class, NetState.LOGIN, Bound.SERVER, 0x01);
        put(LoginOutSuccess.class, NetState.LOGIN, Bound.CLIENT, 0x02);
        put(LoginOutCompression.class, NetState.LOGIN, Bound.CLIENT, 0x03);

        put(PlayOutJoinGame.class, NetState.PLAY, Bound.CLIENT, 0x23);
    }

    /**
     * Combines the data into a single value which is used
     * to locate the a packet inside of the register.
     *
     * @param bound the bound of the packet
     * @param state the packet state
     * @param id the packet ID
     * @return the compressed packet represented as an
     * integer
     */
    // Bit shifting magic 101
    // Using compressed ID value helps flatten the map
    // hierarchy with minimal impact to performance
    // this helps save memory and autoboxing tremendously
    // top bit is bound switch
    // after that 4 bits for net state ordinal
    // then comes the rest of the ID
    private static int shift(NetState state, Bound bound, int id) {
        int identifier = id;
        identifier |= state.ordinal() << 27;
        identifier |= bound.ordinal() << 31;
        return identifier;
    }

    /**
     * Puts the given packet class into the map with the
     * given ID, and also inserts the constructor into the
     * CTOR cache.
     *
     * @param cls the class
     * @param id the ID
     */
    private static void put(Class<? extends Packet> cls,
                            NetState state, Bound bound, int id) {
        int identifier = shift(state, bound, id);
        PACKETS.put(cls, identifier);

        // Only in packets will need reflection inst
        if (bound == Bound.SERVER) {
            CTORS.put(cls, ConstructorAccess.get(cls));
        }
    }

    // Prevent instantiation
    private PacketRegistry() {
    }

    /**
     * Creates a new instance of the given packet class.
     *
     * @param cls the packet class to instantiate
     * @return the instantiated packet
     */
    public static <T extends Packet> T make(Class<? extends Packet> cls) {
        return (T) CTORS.get(cls).newInstance();
    }

    // When switching over enums always put PLAY in front
    // because the majority of packets can return quickly
    // as there are only a few packets that are not PLAY net
    // status and thus the majority of lookups can return
    // much quicker

    /**
     * Obtains the class of the packet containing the given
     * ID, bound, and the given state.
     *
     * @param state the packet's network state
     * @param bound the packet bound
     * @param id the packet ID
     * @return the packet class
     */
    public static Class<? extends Packet> byId(NetState state, Bound bound, int id) {
        int identifier = shift(state, bound, id);
        Class<? extends Packet> packet = PACKETS.inverse().get(identifier);
        if (packet != null) {
            return packet;
        }

        throw new IllegalArgumentException(state + "," + bound + "," + id + "," + " is not registered");
    }

    /**
     * Obtains the net state which the packet is registered
     * to be present in.
     *
     * @param cls the packet class
     * @return the state of the packet
     */
    public static int packetInfo(Class<? extends Packet> cls) {
        Integer identifier = PACKETS.get(cls);
        if (identifier != null) {
            return identifier;
        }

        throw new IllegalArgumentException(cls.getSimpleName() + " is not registered");
    }

    /**
     * Obtains the ID of the packet with the given info.
     *
     * @param info the info
     * @return the packet ID
     */
    public static int idOf(int info) {
        return info & 0x7ffffff;
    }

    /**
     * Obtains the state of the packet with the given info.
     *
     * @param info the info
     * @return the packet state
     */
    public static NetState stateOf(int info) {
        int ordinal = info >> 27 & 0xf;
        return NetState.values()[ordinal];
    }

    /**
     * Obtains the bound of the packet with the given info.
     *
     * @param info the info
     * @return the packet bound
     */
    public static Bound boundOf(int info) {
        int ordinal = info >> 31 & 0x1;
        return Bound.values()[ordinal];
    }
}