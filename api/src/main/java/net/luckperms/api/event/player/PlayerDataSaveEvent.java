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

package net.luckperms.api.event.player;

import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.user.UserManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

/**
 * Called when player data is saved to the storage.
 *
 * <p>Data can be saved using {@link UserManager#savePlayerData(UUID, String)}.</p>
 */
public interface PlayerDataSaveEvent extends LuckPermsEvent {

    /**
     * Gets the unique ID that was saved.
     *
     * @return the uuid
     */
    @Param(0)
    @NonNull UUID getUniqueId();

    /**
     * Gets the username that was saved.
     *
     * @return the username
     */
    @Param(1)
    @NonNull String getUsername();

    /**
     * Gets the result of the operation.
     *
     * @return the result
     */
    @Param(2)
    @NonNull PlayerSaveResult getResult();

}
