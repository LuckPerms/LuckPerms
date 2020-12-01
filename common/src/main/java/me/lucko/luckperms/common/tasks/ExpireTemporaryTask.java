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

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.concurrent.locks.Lock;

public class ExpireTemporaryTask implements Runnable {
    private final LuckPermsPlugin plugin;

    public ExpireTemporaryTask(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean groupChanges = false;
        for (Group group : this.plugin.getGroupManager().getAll().values()) {
            if (shouldSkip(group)) {
                continue;
            }
            if (group.auditTemporaryNodes()) {
                this.plugin.getStorage().saveGroup(group);
                groupChanges = true;
            }
        }

        for (User user : this.plugin.getUserManager().getAll().values()) {
            if (shouldSkip(user)) {
                continue;
            }
            if (user.auditTemporaryNodes()) {
                this.plugin.getStorage().saveUser(user);
            }
        }

        if (groupChanges) {
            this.plugin.getGroupManager().invalidateAllGroupCaches();
            this.plugin.getUserManager().invalidateAllUserCaches();
        }
    }

    // return true if the holder's io lock is currently held, false otherwise
    private static boolean shouldSkip(PermissionHolder holder) {
        Lock lock = holder.getIoLock();

        // if the holder is currently being manipulated by the storage impl,
        // don't attempt to audit temporary permissions
        if (!lock.tryLock()) {
            // if #tryLock returns false, it means it's held by something else
            return true;
        }

        // immediately release the lock & return false
        lock.unlock();
        return false;
    }
}