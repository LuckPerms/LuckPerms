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

package net.luckperms.api.model.user;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataMutateResult;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

/**
 * A player which holds permission data.
 */
public interface User extends PermissionHolder {

    /**
     * Gets the users unique ID
     *
     * @return the users Mojang assigned unique id
     */
    @NonNull UUID getUniqueId();

    /**
     * Gets the users username
     *
     * <p>Returns null if no username is known for the user.</p>
     *
     * @return the users username
     */
    @Nullable String getUsername();

    /**
     * Gets the users current primary group.
     *
     * <p>The result of this method depends on which method is configured for primary group
     * calculation. It may not be the same as any value set through
     * {@link #setPrimaryGroup(String)}.</p>
     *
     * @return the users primary group
     */
    @NonNull String getPrimaryGroup();

    /**
     * Sets a users primary group.
     *
     * <p>This modifies the "stored value" for the users primary group, which may or may not
     * actually take effect, depending on how the platform is calculating primary groups.</p>
     *
     * @param group the new primary group
     * @return if the change was applied successfully
     * @throws IllegalStateException if the user is not a member of that group
     * @throws NullPointerException  if the group is null
     */
    @NonNull DataMutateResult setPrimaryGroup(@NonNull String group);

}
