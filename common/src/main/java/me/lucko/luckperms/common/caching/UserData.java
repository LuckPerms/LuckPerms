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

import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.calculators.CalculatorFactory;
import me.lucko.luckperms.common.users.User;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class UserData {
    private final User user;
    private final CalculatorFactory calculatorFactory;

    private final Map<Contexts, PermissionData> permission = new ConcurrentHashMap<>();
    private final Map<Contexts, MetaData> meta = new ConcurrentHashMap<>();

    public PermissionData getPermissionData(Contexts contexts) {
        return permission.computeIfAbsent(contexts, this::calculatePermissions);
    }

    public MetaData getMetaData(Contexts contexts) {
        return meta.computeIfAbsent(contexts, this::calculateMeta);
    }

    public PermissionData calculatePermissions(Contexts contexts) {
        PermissionData data = new PermissionData(contexts, user, calculatorFactory);
        data.setPermissions(user.exportNodes(contexts, Collections.emptyList(), true));
        return data;
    }

    public void recalculatePermissions(Contexts contexts) {
        permission.compute(contexts, (c, data) -> {
            if (data == null) {
                data = new PermissionData(c, user, calculatorFactory);
            }

            data.comparePermissions(user.exportNodes(c, Collections.emptyList(), true));
            return data;
        });
    }

    public void recalculatePermissions() {
        permission.keySet().forEach(this::recalculatePermissions);
    }

    public MetaData calculateMeta(Contexts contexts) {
        MetaData data = new MetaData(contexts);
        data.loadMeta(user.getAllNodes(null, contexts));
        return data;
    }

    public void recalculateMeta(Contexts contexts) {
        meta.compute(contexts, (c, data) -> {
            if (data == null) {
                data = new MetaData(c);
            }

            data.loadMeta(user.getAllNodes(null, c));
            return data;
        });
    }

    public void recalculateMeta() {
        meta.keySet().forEach(this::recalculateMeta);
    }

    public void preCalculate(Set<Contexts> contexts) {
        contexts.forEach(this::preCalculate);
    }

    public void preCalculate(Contexts contexts) {
        getPermissionData(contexts);
        getMetaData(contexts);
    }

    public void invalidateCache() {
        permission.clear();
        meta.clear();
    }

}
