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
package net.tridentsdk.server.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.Location;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutPlayerCompleteMove extends OutPacket {

    protected Location location;
    protected byte flags;

    @Override
    public int getId() {
        return 0x08;
    }

    public Location getLocation() {
        return this.location;
    }

    public byte getFlags() {
        return this.flags;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeDouble(this.location.getX());
        buf.writeDouble(this.location.getY());
        buf.writeDouble(this.location.getZ());

        buf.writeFloat(this.location.getYaw());
        buf.writeFloat(this.location.getPitch());

        buf.writeByte((int) this.flags);
    }
}
