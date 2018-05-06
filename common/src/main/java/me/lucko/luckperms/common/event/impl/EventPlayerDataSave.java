/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.event.impl;

import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.api.event.player.PlayerDataSaveEvent;
import me.lucko.luckperms.common.event.AbstractEvent;

import java.util.UUID;

import javax.annotation.Nonnull;

public class EventPlayerDataSave extends AbstractEvent implements PlayerDataSaveEvent {
    private final UUID uuid;
    private final String username;
    private final PlayerSaveResult result;

    public EventPlayerDataSave(UUID uuid, String username, PlayerSaveResult result) {
        this.uuid = uuid;
        this.username = username;
        this.result = result;
    }

    @Nonnull
    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Nonnull
    @Override
    public String getUsername() {
        return this.username;
    }

    @Nonnull
    @Override
    public PlayerSaveResult getResult() {
        return this.result;
    }

    @Override
    public String toString() {
        return "PlayerDataSaveEvent(" +
                "uuid=" + this.uuid + ", " +
                "username=" + this.username + ", " +
                "result=" + this.result + ")";
    }
}
