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

package me.lucko.luckperms.common.api.delegates.misc;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.api.metastacking.MetaStackElement;
import me.lucko.luckperms.api.metastacking.MetaStackFactory;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
public class ApiMetaStackFactory implements MetaStackFactory {
    public final LuckPermsPlugin plugin;

    @Override
    public Optional<MetaStackElement> fromString(@NonNull String definition) {
        return StandardStackElements.parseFromString(plugin, definition);
    }

    @Override
    public List<MetaStackElement> fromStrings(@NonNull List<String> definitions) {
        if (definitions.isEmpty()) {
            return ImmutableList.of();
        }
        return StandardStackElements.parseList(plugin, definitions);
    }

    @Override
    public MetaStackDefinition createDefinition(List<MetaStackElement> elements, String startSpacer, String middleSpacer, String endSpacer) {
        return new SimpleMetaStackDefinition(elements, startSpacer, middleSpacer, endSpacer);
    }
}
