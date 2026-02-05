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

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Locale;

/**
 * Calculates and caches permissions
 */
public class PermissionCalculatorBase implements PermissionCalculator {

    /** The processors which back this calculator */
    private final PermissionProcessor[] processors;

    /** Loading cache for permission checks */
    private final LoadingMap<String, TristateResult> lookupCache = LoadingMap.of(this::resolve);

    public PermissionCalculatorBase(Collection<PermissionProcessor> processors) {
        this.processors = processors.toArray(new PermissionProcessor[0]);
    }

    @Override
    public TristateResult checkPermission(String permission, CheckOrigin origin) {
        return this.lookupCache.get(permission);
    }

    private TristateResult resolve(@NonNull String permission) {
        // convert the permission to lowercase, as all values in the backing map are also lowercase.
        // this allows fast case insensitive lookups
        permission = permission.toLowerCase(Locale.ROOT);

        observePermission(permission);

        TristateResult result = TristateResult.UNDEFINED;
        for (PermissionProcessor processor : this.processors) {
            result = processor.hasPermission(result, permission);
        }
        return result;
    }

    protected void observePermission(String permission) {

    }

    @Override
    public void invalidateCache() {
        for (PermissionProcessor processor : this.processors) {
            processor.invalidate();
        }
        this.lookupCache.clear();
    }
}