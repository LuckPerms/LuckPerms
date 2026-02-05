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

package me.lucko.luckperms.hytale.calculator;

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.PermissionCalculatorMonitored;
import me.lucko.luckperms.common.calculator.processor.DirectProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.RegexProcessor;
import me.lucko.luckperms.common.calculator.processor.SpongeWildcardProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import me.lucko.luckperms.hytale.context.HytaleContextManager;
import me.lucko.luckperms.hytale.service.VirtualGroups;
import net.luckperms.api.node.Node;
import net.luckperms.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HytaleCalculatorFactory implements CalculatorFactory {
    private final LPHytalePlugin plugin;

    public HytaleCalculatorFactory(LPHytalePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, Map<String, Node> sourceMap, CacheMetadata metadata) {
        List<PermissionProcessor> processors = new ArrayList<>(6);

        processors.add(new DirectProcessor(sourceMap));

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor(sourceMap));
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor(sourceMap));
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS_SPONGE)) {
            processors.add(new SpongeWildcardProcessor(sourceMap));
        }

        boolean integratedOwner = queryOptions.option(HytaleContextManager.INTEGRATED_SERVER_OWNER).orElse(false);
        if (integratedOwner && this.plugin.getConfiguration().get(ConfigKeys.INTEGRATED_SERVER_OWNER_BYPASSES_CHECKS)) {
            processors.add(ServerOwnerProcessor.INSTANCE);
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLY_HYTALE_VIRTUAL_GROUPS)) {
            ImmutableSet<String> virtualGroups = queryOptions.option(VirtualGroups.KEY).orElse(VirtualGroups.EMPTY).groups();
            processors.add(new HytaleVirtualGroupProcessor(this.plugin, virtualGroups, sourceMap));
        }

        return new PermissionCalculatorMonitored(this.plugin, metadata, processors);
    }
}
