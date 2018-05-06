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

package me.lucko.luckperms.api.event.player;

import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.api.manager.UserManager;

import java.util.UUID;

import javax.annotation.Nonnull;

/**
 * Called when player data is saved to the storage.
 *
 * <p>Data can be saved using {@link UserManager#savePlayerData(UUID, String)}.</p>
 *
 * @since 4.3
 */
public interface PlayerDataSaveEvent extends LuckPermsEvent {

    /**
     * Gets the unique ID that was saved.
     *
     * @return the uuid
     */
    @Nonnull
    UUID getUuid();

    /**
     * Gets the username that was saved.
     *
     * @return the username
     */
    @Nonnull
    String getUsername();

    /**
     * Gets the result of the operation.
     *
     * @return the result
     */
    @Nonnull
    PlayerSaveResult getResult();

}
