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

package me.lucko.luckperms.sponge.calculator;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.cacheddata.CacheMetadata;
import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.processor.MapProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.RegexProcessor;
import me.lucko.luckperms.common.calculator.processor.SpongeWildcardProcessor;
import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.sponge.LPSpongePlugin;

import net.luckperms.api.query.QueryOptions;

public class SpongeCalculatorFactory implements CalculatorFactory {
    private final LPSpongePlugin plugin;

    public SpongeCalculatorFactory(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PermissionCalculator build(QueryOptions queryOptions, CacheMetadata metadata) {
        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();

        processors.add(new MapProcessor());

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS_SPONGE)) {
            processors.add(new SpongeWildcardProcessor());
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.APPLY_SPONGE_DEFAULT_SUBJECTS)) {
            boolean overrideWildcards = this.plugin.getConfiguration().get(ConfigKeys.APPLY_DEFAULT_NEGATIONS_BEFORE_WILDCARDS);
            if (metadata.getHolderType() == HolderType.USER) {
                processors.add(new UserDefaultsProcessor(this.plugin.getService(), queryOptions, overrideWildcards));
            } else if (metadata.getHolderType() == HolderType.GROUP) {
                processors.add(new GroupDefaultsProcessor(this.plugin.getService(), queryOptions, overrideWildcards));
            }
        }

        return new PermissionCalculator(this.plugin, metadata, processors.build());
    }
}
