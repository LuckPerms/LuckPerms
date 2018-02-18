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

package me.lucko.luckperms.nukkit.calculators;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.calculators.AbstractCalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.processors.MapProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.processors.RegexProcessor;
import me.lucko.luckperms.common.processors.WildcardProcessor;
import me.lucko.luckperms.common.references.HolderType;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import me.lucko.luckperms.nukkit.processors.ChildProcessor;
import me.lucko.luckperms.nukkit.processors.DefaultsProcessor;

public class NukkitCalculatorFactory extends AbstractCalculatorFactory {
    private final LPNukkitPlugin plugin;

    public NukkitCalculatorFactory(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PermissionCalculator build(Contexts contexts, PermissionCalculatorMetadata metadata) {
        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();

        processors.add(new MapProcessor());

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLY_NUKKIT_CHILD_PERMISSIONS)) {
            processors.add(new ChildProcessor(this.plugin));
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLY_NUKKIT_DEFAULT_PERMISSIONS) && metadata.getHolderType() == HolderType.USER) {
            processors.add(new DefaultsProcessor(this.plugin, contexts.isOp()));
        }

        return registerCalculator(new PermissionCalculator(this.plugin, metadata, processors.build()));
    }
}
