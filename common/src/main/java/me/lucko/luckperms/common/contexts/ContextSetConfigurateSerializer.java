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

package me.lucko.luckperms.common.contexts;

import lombok.experimental.UtilityClass;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ContextSetConfigurateSerializer {

    public static ConfigurationNode serializeContextSet(ContextSet contextSet) {
        ConfigurationNode data = SimpleConfigurationNode.root();
        Map<String, Collection<String>> map = contextSet.toMultimap().asMap();

        map.forEach((k, v) -> {
            List<String> values = new ArrayList<>(v);
            int size = values.size();

            if (size == 1) {
                data.getNode(k).setValue(values.get(0));
            } else if (size > 1) {
                data.getNode(k).setValue(values);
            }
        });

        return data;
    }

    public static ContextSet deserializeContextSet(ConfigurationNode data) {
        Preconditions.checkArgument(data.hasMapChildren());
        Map<Object, ? extends ConfigurationNode> dataMap = data.getChildrenMap();

        if (dataMap.isEmpty()) {
            return ContextSet.empty();
        }

        MutableContextSet map = MutableContextSet.create();
        for (Map.Entry<Object, ? extends ConfigurationNode> e : dataMap.entrySet()) {
            String k = e.getKey().toString();
            ConfigurationNode v = e.getValue();

            if (v.hasListChildren()) {
                List<? extends ConfigurationNode> values = v.getChildrenList();
                for (ConfigurationNode value : values) {
                    map.add(k, value.getString());
                }
            } else {
                map.add(k, v.getString());
            }
        }

        return map;
    }

}
