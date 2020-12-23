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

package me.lucko.luckperms.common.calculator;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.result.TristateResult;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Calculates and caches permissions
 */
public class PermissionCalculator implements Function<String, TristateResult> {

    /** The plugin instance */
    private final LuckPermsPlugin plugin;

    /** Info about the nature of this calculator. */
    private final CacheMetadata metadata;

    /** The processors which back this calculator */
    private final ImmutableList<PermissionProcessor> processors;

    /** Loading cache for permission checks */
    private final LoadingMap<String, TristateResult> lookupCache = LoadingMap.of(this);

    /** The object name passed to the verbose handler when checks are made */
    private final String verboseCheckTarget;

    public PermissionCalculator(LuckPermsPlugin plugin, CacheMetadata metadata, ImmutableList<PermissionProcessor> processors) {
        this.plugin = plugin;
        this.metadata = metadata;
        this.processors = processors;

        if (this.metadata.getHolderType() == HolderType.GROUP) {
            this.verboseCheckTarget = "group/" + this.metadata.getObjectName();
        } else {
            this.verboseCheckTarget = this.metadata.getObjectName();
        }
    }

    /**
     * Performs a permission check against this calculator.
     *
     * <p>The result is calculated using the calculators backing 'processors'.</p>
     *
     * @param permission the permission to check
     * @param origin marks where this check originated from
     * @return the result
     */
    public TristateResult checkPermission(String permission, PermissionCheckEvent.Origin origin) {
        // get the result
        TristateResult result = this.lookupCache.get(permission);

        // log this permission lookup to the verbose handler
        this.plugin.getVerboseHandler().offerPermissionCheckEvent(origin, this.verboseCheckTarget, this.metadata.getQueryOptions(), permission, result);

        // return the result
        return result;
    }

    @Override
    public TristateResult apply(@NonNull String permission) {
        // convert the permission to lowercase, as all values in the backing map are also lowercase.
        // this allows fast case insensitive lookups
        permission = permission.toLowerCase();

        // offer the permission to the permission vault
        // we only need to do this once per permission, so it doesn't matter
        // that this call is behind the cache.
        this.plugin.getPermissionRegistry().offer(permission);

        TristateResult result = TristateResult.UNDEFINED;
        for (PermissionProcessor processor : this.processors) {
            result = processor.hasPermission(result, permission);
        }
        return result;
    }

    /**
     * Defines the source permissions map which should be used when calculating
     * a result.
     *
     * @param sourceMap the source map
     */
    public synchronized void setSourcePermissions(Map<String, Boolean> sourceMap) {
        for (PermissionProcessor processor : this.processors) {
            processor.setSource(sourceMap);
            processor.refresh();
        }
    }

    public List<PermissionProcessor> getProcessors() {
        return this.processors;
    }

    public void invalidateCache() {
        for (PermissionProcessor processor : this.processors) {
            processor.invalidate();
        }
        this.lookupCache.clear();
    }
}
