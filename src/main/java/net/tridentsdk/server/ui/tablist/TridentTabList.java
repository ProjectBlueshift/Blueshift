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
package net.tridentsdk.server.ui.tablist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import net.tridentsdk.chat.ChatComponent;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.server.packet.play.PlayOutPlayerListHeaderAndFooter;
import net.tridentsdk.server.packet.play.PlayOutTabListItem;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.ui.tablist.TabList;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The tab list implementation.
 */
@ThreadSafe
public abstract class TridentTabList implements TabList {
    /**
     * The players which are displayed this tab list
     */
    protected final Collection<Player> users;
    /**
     * Elements of this tab list
     */
    protected final List<TabListElement> elements;

    /**
     * The tab list header
     */
    @Getter
    private volatile ChatComponent header;
    /**
     * Tab list footer
     */
    @Getter
    private volatile ChatComponent footer;

    private final Set<TabListElement> lastSeen = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Creates and initailizes a new tab list/
     * superconstructor
     */
    public TridentTabList() {
        this.users = Collections.newSetFromMap(new ConcurrentHashMap<>());
        // TODO find a better implementation
        this.elements = new CopyOnWriteArrayList<>();
    }

    @Override
    public void setHeader(ChatComponent value) {
        this.header = value;
        this.updateHeaderFooter();
    }

    @Override
    public void setFooter(ChatComponent value) {
        this.footer = value;
        this.updateHeaderFooter();
    }

    @Override
    public Collection<Player> getUserList() {
        return Collections.unmodifiableCollection(this.users);
    }

    @Override
    public void subscribe(Player player) {
        this.users.add(player);
    }

    @Override
    public void unsubscribe(Player player) {
        this.users.remove(player);
    }

    /**
     * Sends the tab list to all subscribed players.
     */
    public void update() {
        if (this.users.isEmpty())
            return;

        PlayOutPlayerListHeaderAndFooter headerAndFooterPacket = new PlayOutPlayerListHeaderAndFooter(this.header, this.footer);
        PlayOutTabListItem.PlayOutTabListItemAddPlayer addPacket = PlayOutTabListItem.addPlayerPacket();
        PlayOutTabListItem.PlayOutTabListItemRemovePlayer removePacket = PlayOutTabListItem.removePlayerPacket();
        PlayOutTabListItem.PlayOutTabListItemUpdateDisplayName updatePacket = PlayOutTabListItem.updatePlayerPacket();

        Map<UUID, TabListElement> lastSeen = new LinkedHashMap<>();
        Map<UUID, TabListElement> current = new LinkedHashMap<>();

        this.lastSeen.forEach(e -> lastSeen.put(e.getUuid(), e));
        this.elements.forEach(e -> current.put(e.getUuid(), e));

        if (current.containsKey(null)) {
            throw new IllegalStateException("tablist currently has a null uuid (= " + current.get(null) + ")");
        }

        lastSeen.entrySet()
                .stream()
                .filter(e -> !current.containsKey(e.getKey()))
                .forEach(e -> removePacket.removePlayer(e.getKey()));

        current.entrySet()
                .stream()
                .filter(e -> !lastSeen.containsKey(e.getKey()))
                .forEach(e -> addPacket.addPlayer(e.getValue()));

        current.entrySet()
                .stream()
                .filter(e -> lastSeen.containsKey(e.getKey()))
                .filter(e -> !Objects.equals(e.getValue().getDisplayName(), lastSeen.get(e.getKey()).getDisplayName()))
                .forEach(e -> updatePacket.update(e.getKey(), e.getValue().getDisplayName()));

        synchronized (this) {
            this.lastSeen.clear();
            this.lastSeen.addAll(this.elements);
        }

        this.users.forEach(p -> {
            TridentPlayer player = (TridentPlayer) p;
            if (addPacket.getActionCount() > 0)
                player.net().sendPacket(addPacket);
            if (removePacket.getActionCount() > 0)
                player.net().sendPacket(removePacket);
            if (updatePacket.getActionCount() > 0)
                player.net().sendPacket(updatePacket);
            player.net().sendPacket(headerAndFooterPacket);
        });
    }

    public void forceSend(TridentPlayer player) {
        PlayOutTabListItem.PlayOutTabListItemAddPlayer addPacket = PlayOutTabListItem.addPlayerPacket();
        PlayOutPlayerListHeaderAndFooter headerAndFooterPacket = new PlayOutPlayerListHeaderAndFooter(this.header, this.footer);

        elements.forEach(addPacket::addPlayer);

        player.net().sendPacket(addPacket);
        player.net().sendPacket(headerAndFooterPacket);
    }

    /**
     * Update operation if the header or footer fields of
     * the tab list are updated.
     */
    private void updateHeaderFooter() {
        PlayOutPlayerListHeaderAndFooter packet = new PlayOutPlayerListHeaderAndFooter(this.header, this.footer);
        this.users.forEach(player -> ((TridentPlayer) player).net().sendPacket(packet));
    }
}
