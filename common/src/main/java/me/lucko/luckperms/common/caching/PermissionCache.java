/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.caching;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.core.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a user's cached permissions for a given context
 */
public class PermissionCache implements PermissionData {

    /**
     * The raw set of permission strings.
     */
    private final Map<String, Boolean> permissions;

    /**
     * The calculator instance responsible for resolving the raw permission strings in the permission map.
     * This calculator will attempt to resolve all regex/wildcard permissions, as well as account for
     * defaults & attachment permissions (if applicable.)
     */
    private final PermissionCalculator calculator;

    public PermissionCache(Contexts contexts, User user, CalculatorFactory calculatorFactory) {
        permissions = new ConcurrentHashMap<>();
        calculator = calculatorFactory.build(contexts, user);
        calculator.updateBacking(permissions); // Initial setup.
    }

    @Override
    public void invalidateCache() {
        calculator.invalidateCache();
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions.clear();
        this.permissions.putAll(permissions);
        calculator.updateBacking(this.permissions);
        invalidateCache();
    }

    public void comparePermissions(Map<String, Boolean> toApply) {
        if (!permissions.equals(toApply)) {
            setPermissions(toApply);
        }
    }

    @Override
    public Map<String, Boolean> getImmutableBacking() {
        return ImmutableMap.copyOf(permissions);
    }

    @Override
    public Tristate getPermissionValue(@NonNull String permission) {
        return calculator.getPermissionValue(permission);
    }
}
