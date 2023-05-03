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

package me.lucko.luckperms.common.node.factory;

import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public final class NodeBuilders {
    private NodeBuilders() {}

    private static final Parser<InheritanceNode.Builder> INHERITANCE = Inheritance::parse;
    private static final Parser<PrefixNode.Builder> PREFIX = Prefix::parse;
    private static final Parser<SuffixNode.Builder> SUFFIX = Suffix::parse;
    private static final Parser<MetaNode.Builder> META = Meta::parse;
    private static final Parser<WeightNode.Builder> WEIGHT = Weight::parse;
    private static final Parser<DisplayNameNode.Builder> DISPLAY_NAME = DisplayName::parse;
    private static final Parser<RegexPermissionNode.Builder> REGEX_PERMISSION = RegexPermission::parse;

    private static final Parser<?>[] PARSERS = new Parser[]{INHERITANCE, PREFIX, SUFFIX, META, WEIGHT, DISPLAY_NAME, REGEX_PERMISSION};

    public static @NonNull NodeBuilder<?, ?> determineMostApplicable(String key) {
        Objects.requireNonNull(key, "key");
        for (Parser<?> parser : PARSERS) {
            NodeBuilder<?, ?> builder = parser.parse(key);
            if (builder != null) {
                return builder;
            }
        }
        return Permission.builder().permission(key);
    }

    private interface Parser<B extends NodeBuilder<?, B>> {
        @Nullable B parse(String s);
    }

}
