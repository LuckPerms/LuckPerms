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

package net.luckperms.api.node.types;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.ScopedNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A sub-type of {@link Node} used to store regex permissions.
 */
public interface RegexPermissionNode extends ScopedNode<RegexPermissionNode, RegexPermissionNode.Builder> {

    @Override
    default @NonNull NodeType<RegexPermissionNode> getType() {
        return NodeType.REGEX_PERMISSION;
    }

    /**
     * Gets the non-compiled pattern string.
     *
     * @return the pattern string
     */
    @NonNull String getPatternString();

    /**
     * Gets the pattern for the regex node.
     *
     * <p>Will return an empty optional if the Pattern could not be parsed.</p>
     *
     * @return the pattern
     */
    @NonNull Optional<Pattern> getPattern();

    /**
     * Creates a {@link RegexPermissionNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forRegexPermission();
    }

    /**
     * Creates a {@link RegexPermissionNode} builder.
     *
     * @param pattern the pattern to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String pattern) {
        return builder().pattern(pattern);
    }

    /**
     * Creates a {@link RegexPermissionNode} builder.
     *
     * @param pattern the pattern to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull Pattern pattern) {
        return builder().pattern(pattern);
    }

    /**
     * A {@link RegexPermissionNode} builder.
     */
    interface Builder extends NodeBuilder<RegexPermissionNode, Builder> {

        /**
         * Sets the pattern.
         *
         * @param pattern the pattern
         * @return the builder
         * @throws IllegalArgumentException if {@code pattern} is empty
         */
        @NonNull Builder pattern(@NonNull String pattern);

        /**
         * Sets the pattern.
         *
         * @param pattern the pattern
         * @return the builder
         */
        @NonNull Builder pattern(@NonNull Pattern pattern);

    }

}
