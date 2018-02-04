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

package me.lucko.luckperms.common.caching.type;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

/**
 * Holds cached permissions data for a given context
 */
public class PermissionCache implements PermissionData {

    /**
     * The contexts this container is holding data for
     */
    private final Contexts contexts;

    /**
     * The raw set of permission strings.
     */
    private final Map<String, Boolean> permissions;

    /**
     * An immutable copy of {@link #permissions}
     */
    private final Map<String, Boolean> permissionsUnmodifiable;

    /**
     * The calculator instance responsible for resolving the raw permission strings in the permission map.
     * This calculator will attempt to resolve all regex/wildcard permissions, as well as account for
     * defaults & attachment permissions (if applicable.)
     */
    private final PermissionCalculator calculator;

    public PermissionCache(Contexts contexts, PermissionCalculatorMetadata metadata, CalculatorFactory calculatorFactory) {
        this.contexts = contexts;
        this.permissions = new ConcurrentHashMap<>();
        this.permissionsUnmodifiable = Collections.unmodifiableMap(this.permissions);

        this.calculator = calculatorFactory.build(contexts, metadata);
        this.calculator.setSourcePermissions(this.permissions); // Initial setup.
    }

    @Override
    public void invalidateCache() {
        this.calculator.invalidateCache();
    }

    private void setPermissionsInternal(Map<String, Boolean> permissions) {
        this.permissions.clear();
        this.permissions.putAll(permissions);
        this.calculator.setSourcePermissions(this.permissions);
        invalidateCache();
    }

    public void setPermissions(Map<String, Boolean> toApply) {
        if (!this.permissions.equals(toApply)) {
            setPermissionsInternal(toApply);
        }
    }

    public PermissionCalculator getCalculator() {
        return this.calculator;
    }

    @Nonnull
    @Override
    public Map<String, Boolean> getImmutableBacking() {
        return this.permissionsUnmodifiable;
    }

    @Nonnull
    @Override
    public Tristate getPermissionValue(@Nonnull String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        return this.calculator.getPermissionValue(permission, CheckOrigin.API);
    }

    public Tristate getPermissionValue(String permission, CheckOrigin origin) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        return this.calculator.getPermissionValue(permission, origin);
    }

    @Nonnull
    @Override
    public Contexts getContexts() {
        return this.contexts;
    }
}
