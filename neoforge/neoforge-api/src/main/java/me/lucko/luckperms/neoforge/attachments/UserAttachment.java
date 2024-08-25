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

package me.lucko.luckperms.neoforge.attachments;

import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A NeoForge {@link AttachmentType Data Attachment} that attaches LuckPerms functionality onto {@link ServerPlayer}s.
 */
public interface UserAttachment {

    /**
     * The identifier used for the attachment
     */
    ResourceLocation IDENTIFIER = ResourceLocation.fromNamespaceAndPath("luckperms", "user");

    /**
     * The attachment type instance; used for querying the actual {@link UserAttachment} instance.
     */
    DeferredHolder<AttachmentType<?>, AttachmentType<UserAttachment>> TYPE = DeferredHolder.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, IDENTIFIER);

    /**
     * Checks for a permission.
     *
     * @param permission the permission
     * @return the result
     */
    default boolean hasPermission(String permission) {
        return checkPermission(permission).asBoolean();
    }

    /**
     * Runs a permission check.
     *
     * @param permission the permission
     * @return the result
     */
    Tristate checkPermission(String permission);

    /**
     * Runs a permission check.
     *
     * @param permission the permission
     * @param queryOptions the query options
     * @return the result
     */
    Tristate checkPermission(String permission, QueryOptions queryOptions);

    /**
     * Gets the user's currently query options.
     *
     * @return the current query options for the user
     */
    QueryOptions getQueryOptions();

}
