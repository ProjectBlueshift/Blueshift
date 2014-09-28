/*
 * Copyright (c) 2014, The TridentSDK Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     1. Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the The TridentSDK Team nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL The TridentSDK Team BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.tridentsdk.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.netty.protocol.Protocol;
import net.tridentsdk.server.threads.PlayerThreads;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles the connection of a client upon joining
 *
 * @author The TridentSDK Team
 */
public class ClientConnection {
    protected static final Map<InetSocketAddress, AtomicReference<ClientConnection>> clientData =
            new ConcurrentHashMap<>();

    protected static final SecureRandom SR = new SecureRandom();
    protected static final Cipher cipher = ClientConnection.getCipher();

    /* Network fields */
    protected InetSocketAddress address;
    protected Channel channel;

    /* Encryption and client data fields */
    protected volatile KeyPair loginKeyPair;
    protected volatile Protocol.ClientStage stage;
    protected volatile boolean encryptionEnabled;
    protected volatile SecretKey sharedSecret;
    protected volatile byte[] verificationToken; // DO NOT WRITE INDIVIDUAL ELEMENTS TO IT. Consult AgentTroll
    private IvParameterSpec ivSpec;

    /**
     * Creates a new connection handler for the joining channel stream
     */
    protected ClientConnection(Channel channel) {
        this.address = (InetSocketAddress) channel.remoteAddress();
        this.channel = channel;
        this.encryptionEnabled = false;
        this.stage = Protocol.ClientStage.HANDSHAKE;
    }

    protected ClientConnection() {
    }

    private static Cipher getCipher() {
        try {
            return Cipher.getInstance("AES/CFB8/NoPadding");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Checks if an IP address is logged into the server
     *
     * @param address the address to check if online
     * @return {@code true} if the IP is on the server, {@code false} if not
     */
    public static boolean isLoggedIn(InetSocketAddress address) {
        return ClientConnection.clientData.containsKey(address);
    }

    /**
     * Gets the connection by the IP address
     *
     * @param address the IP to lookup the connection handler
     * @return the instance of the client handler associated with the IP
     */
    public static ClientConnection getConnection(InetSocketAddress address) {
        // Get the connection reference
        AtomicReference<ClientConnection> reference = ClientConnection.clientData.get(address);

        // return null if connection is not found
        if (reference == null) {
            return null;
        }

        // return found connection
        return reference.get();
    }

    public static ClientConnection getConnection(ChannelHandlerContext chx) {
        return ClientConnection.getConnection((InetSocketAddress) chx.channel().remoteAddress());
    }

    public static ClientConnection registerConnection(Channel channel) {
        // Make a new instance of ClientConnection
        ClientConnection newConnection = new ClientConnection(channel);

        // Register data and return the new instance
        ClientConnection.clientData.put(newConnection.getAddress(), new AtomicReference<>(newConnection));
        return newConnection;
    }

    /**
     * Sends protocol data through the client stream
     *
     * @param packet the packet to send, encoded and written to the stream
     */
    public void sendPacket(Packet packet) {
        System.out.println("Sending Packet: " + packet.getClass().getSimpleName());

        // Create new ByteBuf
        ByteBuf buffer = this.channel.alloc().buffer();

        Codec.writeVarInt32(buffer, packet.getId());
        packet.encode(buffer);

        // Write the packet and flush it
        this.channel.write(buffer);
        this.channel.flush();
    }

    public byte[] encrypt(byte... data) throws Exception {
        ClientConnection.cipher.init(Cipher.ENCRYPT_MODE, this.sharedSecret, this.ivSpec);

        return ClientConnection.cipher.doFinal(data);
    }

    public byte[] decrypt(byte... data) throws Exception {
        ClientConnection.cipher.init(Cipher.DECRYPT_MODE, this.sharedSecret, this.ivSpec);

        return ClientConnection.cipher.doFinal(data);
    }

    public void generateToken() {
        this.verificationToken = new byte[4];
        ClientConnection.SR.nextBytes(this.verificationToken);
    }

    public void enableEncryption(byte... secret) {
        //Makes sure the secret is only set once
        if (!this.encryptionEnabled) {
            this.sharedSecret = new SecretKeySpec(secret, "AES");
            this.ivSpec = new IvParameterSpec(this.sharedSecret.getEncoded());
            this.encryptionEnabled = true;
        }
    }

    /**
     * Gets the channel context for the connection stream
     *
     * @return the netty channel wrapped by the handler
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * The IP address of the client handled by this connection wrapper
     *
     * @return the handled IP address
     */
    public InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * Gets the current state of the connection
     *
     * @return the current state of the protocol for the client
     */
    public Protocol.ClientStage getStage() {
        return this.stage;
    }

    /**
     * Sets the client state, should only be used by the ClientConnectionHandler
     *
     * @param stage the state to set the client to
     */
    public void setStage(Protocol.ClientStage stage) {
        this.stage = stage;
    }

    public byte[] getVerificationToken() {
        return this.verificationToken;
    }

    public boolean isEncryptionEnabled() {
        return this.encryptionEnabled;
    }

    public KeyPair getLoginKeyPair() {
        return this.loginKeyPair;
    }

    public void setLoginKeyPair(KeyPair keyPair) {
        this.loginKeyPair = keyPair;
    }

    public SecretKey getSharedSecret() {
        return this.sharedSecret;
    }

    /**
     * Removes the client's server side client handler
     */
    public void logout() {
        ClientConnection.clientData.remove(this.address);
        PlayerThreads.remove(this);
        this.channel.close();
    }
}
