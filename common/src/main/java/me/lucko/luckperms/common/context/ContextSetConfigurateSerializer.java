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

package me.lucko.luckperms.common.context;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.context.contextset.MutableContextSetImpl;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.MutableContextSet;

import ninja.leaping.configurate.ConfigurationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ContextSetConfigurateSerializer {
    private ContextSetConfigurateSerializer() {}

    public static ConfigurationNode serializeContextSet(ContextSet contextSet) {
        ConfigurationNode data = ConfigurationNode.root();
        Map<String, Set<String>> map = contextSet.toMap();

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            List<String> values = new ArrayList<>(entry.getValue());
            int size = values.size();

            if (size == 1) {
                data.getNode(entry.getKey()).setValue(values.get(0));
            } else if (size > 1) {
                data.getNode(entry.getKey()).setValue(values);
            }
        }

        return data;
    }

    public static ContextSet deserializeContextSet(ConfigurationNode data) {
        Preconditions.checkArgument(data.isMap());
        Map<Object, ? extends ConfigurationNode> dataMap = data.getChildrenMap();

        if (dataMap.isEmpty()) {
            return ImmutableContextSetImpl.EMPTY;
        }

        MutableContextSet map = new MutableContextSetImpl();
        for (Map.Entry<Object, ? extends ConfigurationNode> e : dataMap.entrySet()) {
            String k = e.getKey().toString();
            ConfigurationNode v = e.getValue();

            if (v.isList()) {
                for (ConfigurationNode value : v.getChildrenList()) {
                    map.add(k, value.getString());
                }
            } else {
                map.add(k, v.getString());
            }
        }

        return map;
    }

}
