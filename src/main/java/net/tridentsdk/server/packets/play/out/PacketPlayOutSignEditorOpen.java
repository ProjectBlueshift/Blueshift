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
package net.tridentsdk.server.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.Location;
import net.tridentsdk.server.data.Position;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutSignEditorOpen extends OutPacket {
    protected Location loc;

    @Override
    public int getId() {
        return 0x36;
    }

    @Override
    public void encode(ByteBuf buf) {
        new Position(this.loc).write(buf);
    }
}