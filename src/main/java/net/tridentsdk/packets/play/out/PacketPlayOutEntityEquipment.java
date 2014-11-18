/*
 *     Trident - A Multithreaded Server Alternative
 *     Copyright (C) 2014, The TridentSDK Team
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.tridentsdk.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutEntityEquipment extends OutPacket {

    protected int entityId;
    protected short slot;
    // TODO: mojang slot shit

    @Override
    public int getId() {
        return 0x04;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public short getSlot() {
        return this.slot;
    }

    @Override
    public void encode(ByteBuf buf) {
        Codec.writeVarInt32(buf, this.entityId);
        buf.writeShort((int) this.slot);
    }
}
