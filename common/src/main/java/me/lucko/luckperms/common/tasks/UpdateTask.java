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

package me.lucko.luckperms.common.tasks;

import lombok.AllArgsConstructor;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * System wide update task for LuckPerms.
 *
 * <p>Ensures that all local data is consistent with the storage.</p>
 */
@AllArgsConstructor
public class UpdateTask implements Runnable {
    private final LuckPermsPlugin plugin;

    /**
     * If this task is being called before the server has fully started
     */
    private final boolean initialUpdate;

    /**
     * Runs the update task
     *
     * <p>Called <b>async</b>.</p>
     */
    @Override
    public void run() {
        if (plugin.getApiProvider().getEventFactory().handlePreSync(false)) {
            return;
        }

        // Reload all groups
        plugin.getStorage().loadAllGroups().join();
        String defaultGroup = plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME);
        if (!plugin.getGroupManager().isLoaded(defaultGroup)) {
            plugin.getStorage().createAndLoadGroup(defaultGroup, CreationCause.INTERNAL).join();
        }

        // Reload all tracks
        plugin.getStorage().loadAllTracks().join();

        // Refresh all online users.
        CompletableFuture<Void> userUpdateFut = plugin.getUserManager().updateAllUsers();
        if (!initialUpdate) {
            userUpdateFut.join();
        }

        plugin.onPostUpdate();

        plugin.getApiProvider().getEventFactory().handlePostSync();
    }
}
