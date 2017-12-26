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

package me.lucko.luckperms.bukkit.calculators;

import lombok.AllArgsConstructor;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.processors.ChildProcessor;
import me.lucko.luckperms.bukkit.processors.DefaultsProcessor;
import me.lucko.luckperms.common.calculators.AbstractCalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.processors.MapProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.processors.RegexProcessor;
import me.lucko.luckperms.common.processors.WildcardProcessor;
import me.lucko.luckperms.common.references.HolderType;

import java.util.List;

@AllArgsConstructor
public class BukkitCalculatorFactory extends AbstractCalculatorFactory {
    private final LPBukkitPlugin plugin;

    @Override
    public PermissionCalculator build(Contexts contexts, PermissionCalculatorMetadata metadata) {
        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();

        processors.add(new MapProcessor());

        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_CHILD_PERMISSIONS)) {
            processors.add(new ChildProcessor(plugin.getChildPermissionProvider()));
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor());
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor());
        }

        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS) && metadata.getHolderType() == HolderType.USER) {
            processors.add(new DefaultsProcessor(contexts.isOp(), plugin.getDefaultsProvider()));
        }

        return registerCalculator(new PermissionCalculator(plugin, metadata, processors.build()));
    }

    @Override
    public List<String> getActiveProcessors() {
        ImmutableList.Builder<String> ret = ImmutableList.builder();
        ret.add("Map");
        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_CHILD_PERMISSIONS)) ret.add("Child");
        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_ATTACHMENT_PERMISSIONS)) ret.add("Attachment");
        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) ret.add("Regex");
        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) ret.add("Wildcard");
        if (plugin.getConfiguration().get(ConfigKeys.APPLY_BUKKIT_DEFAULT_PERMISSIONS)) ret.add("Defaults");
        return ret.build();
    }
}
