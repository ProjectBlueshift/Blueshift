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

package net.tridentsdk.server.packets.play.in;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.Handler;
import net.tridentsdk.event.player.PlayerClickItemEvent;
import net.tridentsdk.server.data.Slot;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.window.Window;

/**
 * Packet sent by the player when it clicks on a slot in a window.
 */
public class PacketPlayInPlayerClickWindow extends InPacket {

    /**
     * The id of the window which was clicked. 0 for player inventory.
     */
    protected int windowId;
    /**
     * The button used in the click, dependent on action number  TODO reference to wiki
     */
    protected int clickedButton;

    /**
     * The clicked slot, -999 if not applicable
     */
    protected short clickedSlot;
    /**
     * A unique number for the action, used for transaction handling
     */
    protected short actionNumber;
    /**
     * Inventory operation mode
     */
    protected short mode;
    /**
     * Item clicked
     */
    protected Slot clickedItem;

    @Override
    public int id() {
        return 0x0E;
    }

    public int windowId() {
        return this.windowId;
    }

    public int clickedButton() {
        return this.clickedButton;
    }

    public short clickedSlot() {
        return this.clickedSlot;
    }

    public short actionNumber() {
        return this.actionNumber;
    }

    public short mode() {
        return this.mode;
    }

    public Slot clickedItem() {
        return this.clickedItem;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        this.windowId = (int) buf.readByte();
        this.clickedSlot = buf.readShort();
        this.clickedButton = (int) buf.readByte();

        this.actionNumber = buf.readShort();
        this.mode = buf.readShort();
        this.clickedItem = new Slot(buf);

        return this;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        Window window = Handler.forWindows().getById(this.windowId);
        PlayerClickItemEvent clickEvent = new PlayerClickItemEvent(window, this.clickedSlot, (int) this.actionNumber);

        Handler.forEvents().fire(clickEvent);

        if (clickEvent.isCancelled()) {
        }
    }
}
