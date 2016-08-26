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
package net.tridentsdk.server.net;

import com.google.common.collect.Maps;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import net.tridentsdk.chat.Chat;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.packet.PacketOut;
import net.tridentsdk.server.packet.login.LoginOutCompression;
import net.tridentsdk.server.packet.login.LoginOutDisconnect;
import net.tridentsdk.server.packet.play.PlayOutDisconnect;
import net.tridentsdk.server.packet.play.PlayOutKeepAlive;
import net.tridentsdk.server.player.TridentPlayer;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.net.SocketAddress;
import java.util.Map;

/**
 * This class represents the connection that a Minecraft
 * client player has to the server.
 */
@ThreadSafe
public class NetClient {
    /**
     * Represents the current connection state that the
     * client is in whilst connecting to the server.
     */
    public enum NetState {
        /**
         * Handshake, attempting to connect to server.
         */
        HANDSHAKE,
        /**
         * Obtain server status via ping.
         */
        STATUS,
        /**
         * Login, authenticate and complete connection
         * formalities before joining.
         */
        LOGIN,
        /**
         * Normal gameplay.
         */
        PLAY
    }

    /**
     * The buffer size used for compressed and
     * decompression
     * buffering
     */
    public static final int BUFFER_SIZE = 8192;
    /**
     * Time before the server kicks an inactive client, in
     * millis.
     */
    public static final int SERVER_TICK_KICK = 30_000;
    /**
     * The interval between each client tick to ensure that
     * the client is not disconnected
     */
    public static final int CLIENT_TICK_INTV = 10_000;
    /**
     * The mapping of currently connected clients
     */
    private static final Map<SocketAddress, NetClient> CLIENTS =
            Maps.newConcurrentMap();

    /**
     * The channel which is the connection that this client
     * has to the server.
     */
    private final Channel channel;
    /**
     * The current state of the client connection to the
     * server.
     */
    private volatile NetState currentState;
    /**
     * The name of the player that represents this client
     */
    private volatile String name;
    /**
     * The crypto module used for encrpyting and decrypting
     * server messages.
     */
    private volatile NetCrypto cryptoModule;
    /**
     * Whether or not the client performs compression
     */
    private volatile boolean doCompression;
    /**
     * The player object
     */
    private volatile TridentPlayer player;
    /**
     * The last time which this player was pinged for keep
     * alive
     */
    @GuardedBy("tickLock")
    private long lastKeepAlive = System.currentTimeMillis();
    /**
     * Locked used to protect ticks from overlapping
     */
    private final Object tickLock = new Object();

    /**
     * Creates a new netclient that represents a client's
     * connection to the server.
     */
    public NetClient(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
        this.currentState = NetState.HANDSHAKE;
        this.channel.closeFuture().addListener(future -> this.disconnect(Chat.empty()));
    }

    /**
     * Obtains an instance of a net client from the cache
     * of
     * currently connected clients, or creates a new one if
     * it doesn't exist.
     *
     * @param ctx the connection
     * @return the net client wrapping the given connection
     */
    public static NetClient get(ChannelHandlerContext ctx) {
        return CLIENTS.computeIfAbsent(ctx.channel().remoteAddress(), (k) -> new NetClient(ctx));
    }

    /**
     * Obtains the last moment (in the time value given by
     * {@link System#currentTimeMillis()}) since the client
     * was sent a keep alive packet.
     *
     * @return the last keep alive
     */
    public long lastKeepAlive() {
        return this.lastKeepAlive;
    }

    /**
     * Ticks the client
     */
    public void tick() {
        synchronized (this.tickLock) {
            long lastKeepAlive = this.lastKeepAlive;
            long now = System.currentTimeMillis();
            if ((now - lastKeepAlive) > CLIENT_TICK_INTV) {
                this.lastKeepAlive = now;
                this.sendPacket(new PlayOutKeepAlive(this));
            }
        }
    }

    /**
     * Obtains the current state which the client's
     * connection to the server is in.
     *
     * @return the network state
     */
    public NetState state() {
        return this.currentState;
    }

    /**
     * Sets the current state of the client to the given
     * next.
     *
     * @param next the next state
     */
    public void setState(NetState next) {
        this.currentState = next;
    }

    /**
     * Obtains the name of the player, if it exists.
     *
     * @return the name presented upon login
     */
    public String name() {
        return this.name;
    }

    /**
     * Sets the name of the client upon login.
     *
     * @param name the player name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Obtains the crypto module that is used for
     * encryption
     * and decryption of packets.
     *
     * @return the crypto module
     */
    public NetCrypto cryptoModule() {
        return this.cryptoModule;
    }

    /**
     * Initializes the crypto module and returns the result
     * of doing so.
     */
    public NetCrypto initCrypto() {
        return this.cryptoModule = new NetCrypto();
    }

    /**
     * Determines whether or not this Minecraft client will
     * perform compression on messages.
     *
     * @return {@code true} if so
     */
    public boolean doCompression() {
        return this.doCompression;
    }

    /**
     * Enables compression when the client is ready.
     */
    public void enableCompression() {
        if (TridentServer.cfg().compressionThresh() != -1) {
            this.sendPacket(new LoginOutCompression())
                    .addListener(future -> this.doCompression = true);
        }
    }

    /**
     * Sends the given packet to the client's channel.
     *
     * @param packet the packet to send
     */
    public ChannelFuture sendPacket(PacketOut packet) {
        return this.channel.writeAndFlush(packet);
    }

    /**
     * Obtains the player that this net client represents
     * the connection.
     *
     * @return the player
     */
    public TridentPlayer player() {
        return this.player;
    }

    /**
     * Sets the player that is represented by this instance
     * of the network client.
     *
     * @param player the player to set
     */
    public void setPlayer(TridentPlayer player) {
        this.player = player;
    }

    /**
     * Overload method of {@link #disconnect(Chat)} but
     * uses
     * shortcut String.
     *
     * @param reason the string reason
     */
    public void disconnect(String reason) {
        this.disconnect(Chat.plain(reason));
    }

    /**
     * Disconnects this client from the server.
     *
     * @param reason the reason for disconnecting
     */
    public void disconnect(Chat reason) {
        NetState state = this.currentState;
        if (state == NetState.LOGIN) {
            this.sendPacket(new LoginOutDisconnect(reason))
                    .addListener(future -> this.channel.close());
        } else if (state == NetState.PLAY) {
            this.sendPacket(new PlayOutDisconnect(reason))
                    .addListener(future -> this.channel.close());
            this.player.remove();
        } else {
            this.channel.close();
        }
    }
}