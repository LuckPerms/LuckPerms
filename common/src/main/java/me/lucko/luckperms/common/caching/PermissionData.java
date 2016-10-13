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
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.users.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionData {
    private final Contexts contexts;
    private final Map<String, Boolean> permissions;

    private final PermissionCalculator calculator;

    public PermissionData(Contexts contexts, User user, CalculatorFactory calculatorFactory) {
        this.contexts = contexts;
        permissions = new ConcurrentHashMap<>();
        calculator = calculatorFactory.build(contexts, user, permissions);
    }

    public void invalidateCache() {
        calculator.invalidateCache();
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions.clear();
        this.permissions.putAll(permissions);
        invalidateCache();
    }

    public void comparePermissions(Map<String, Boolean> toApply) {
        boolean different = false;
        if (toApply.size() != permissions.size()) {
            different = true;
        } else {
            for (Map.Entry<String, Boolean> e : permissions.entrySet()) {
                if (toApply.containsKey(e.getKey()) && toApply.get(e.getKey()) == e.getValue()) {
                    continue;
                }
                different = true;
                break;
            }
        }

        if (different) {
            setPermissions(toApply);
        }
    }

    public Map<String, Boolean> getImmutableBacking() {
        return ImmutableMap.copyOf(permissions);
    }

    public Tristate getPermissionValue(@NonNull String permission) {
        Tristate t = calculator.getPermissionValue(permission);
        if (t != Tristate.UNDEFINED) {
            return Tristate.fromBoolean(t.asBoolean());
        } else {
            return Tristate.UNDEFINED;
        }
    }
}
