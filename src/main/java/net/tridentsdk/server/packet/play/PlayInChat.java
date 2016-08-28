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
package net.tridentsdk.server.packet.play;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.chat.ChatType;
import net.tridentsdk.server.TridentServer;
import net.tridentsdk.server.net.NetClient;
import net.tridentsdk.server.packet.PacketIn;
import net.tridentsdk.server.player.TridentPlayer;

import static net.tridentsdk.server.net.NetData.rstr;

public class PlayInChat extends PacketIn {

    public PlayInChat() {
        super(PlayInChat.class);
    }

    @Override
    public void read(ByteBuf buf, NetClient client) {
        TridentPlayer player = client.player();
        String msg = rstr(buf);

        ChatComponent chat = ChatComponent.create()
                .setTranslate("chat.type.text")
                .addWith(player.name())
                .addWith(msg);
        TridentServer.instance().players().forEach(p -> p.sendMessage(chat, ChatType.CHAT));
    }

}
