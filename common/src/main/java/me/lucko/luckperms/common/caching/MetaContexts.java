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

package me.lucko.luckperms.common.caching;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.metastacking.MetaType;
import me.lucko.luckperms.common.metastacking.definition.MetaStackDefinition;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

@Getter
@ToString
@EqualsAndHashCode
public final class MetaContexts {
    public static MetaContexts makeFromConfig(Contexts contexts, LuckPermsPlugin plugin) {
        return new MetaContexts(
                contexts,
                plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS),
                plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS)
        );
    }

    private final Contexts contexts;
    private final MetaStackDefinition prefixStackDefinition;
    private final MetaStackDefinition suffixStackDefinition;

    public MetaContexts(@NonNull Contexts contexts, @NonNull MetaStackDefinition prefixStackDefinition, @NonNull MetaStackDefinition suffixStackDefinition) {
        this.contexts = contexts;
        this.prefixStackDefinition = prefixStackDefinition;
        this.suffixStackDefinition = suffixStackDefinition;
    }

    public MetaAccumulator newAccumulator() {
        return new MetaAccumulator(
                prefixStackDefinition.newStack(MetaType.PREFIX),
                suffixStackDefinition.newStack(MetaType.SUFFIX)
        );
    }
}
