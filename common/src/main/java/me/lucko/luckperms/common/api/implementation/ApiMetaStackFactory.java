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

package me.lucko.luckperms.common.api.implementation;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.cacheddata.metastack.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.cacheddata.metastack.StandardStackElements;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.metastacking.MetaStackFactory;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ApiMetaStackFactory implements MetaStackFactory {
    public final LuckPermsPlugin plugin;

    public ApiMetaStackFactory(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NonNull Optional<MetaStackElement> fromString(@NonNull String definition) {
        Objects.requireNonNull(definition, "definition");
        return Optional.ofNullable(StandardStackElements.parseFromString(this.plugin, definition));
    }

    @Override
    public @NonNull List<MetaStackElement> fromStrings(@NonNull List<String> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        if (definitions.isEmpty()) {
            return ImmutableList.of();
        }
        return StandardStackElements.parseList(this.plugin, definitions);
    }

    @Override
    public @NonNull MetaStackDefinition createDefinition(@NonNull List<MetaStackElement> elements, @NonNull DuplicateRemovalFunction duplicateRemovalFunction, @NonNull String startSpacer, @NonNull String middleSpacer, @NonNull String endSpacer) {
        return new SimpleMetaStackDefinition(elements, duplicateRemovalFunction, startSpacer, middleSpacer, endSpacer);
    }
}
