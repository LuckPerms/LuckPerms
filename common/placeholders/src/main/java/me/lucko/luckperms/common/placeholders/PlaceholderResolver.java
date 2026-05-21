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

package me.lucko.luckperms.common.placeholders;

import java.util.Collection;
import java.util.Locale;

/**
 * Resolves placeholders using a simple string format:
 *
 * <p>
 * <ul>
 *     <li>placeholder</li>
 *     <li>placeholder_argument</li>
 * </ul>
 * </p>
 *
 * <p>Note: this resolver does not parse placeholders mid-string, it expects to receive the parsed
 * placeholder string as input.</p>
 */
public class PlaceholderResolver {

    /** The placeholders used by this resolver */
    private final Collection<Placeholder> placeholders;

    /**
     * Create a resolver using the built-in placeholders registered in {@link PlaceholderRegistry}.
     */
    public PlaceholderResolver() {
        this(PlaceholderRegistry.getAll());
    }

    /**
     * Create a resolver using a custom list of placeholders.
     *
     * @param placeholders the placeholders
     */
    public PlaceholderResolver(Collection<Placeholder> placeholders) {
        this.placeholders = placeholders;
    }

    /**
     * Resolve the placeholder value of a given input string
     *
     * @param input the input string
     * @return the resolved value, or null if no placeholder matched
     */
    public String resolve(PlaceholderContext ctx, String input) {
        input = input.toLowerCase(Locale.ROOT);
        for (Placeholder placeholder : this.placeholders) {
            String result = attemptResolve(ctx, input, placeholder);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Attempt to resolve the placeholder value of a given input string for a specific placeholder.
     *
     * @param ctx the placeholder context
     * @param input the input string
     * @param placeholder the placeholder to attempt to resolve with
     * @return the resolved value if the placeholder matches the input, or null if it does not match
     */
    protected String attemptResolve(PlaceholderContext ctx, String input, Placeholder placeholder) {
        String id = placeholder.id();
        if (placeholder instanceof Placeholder.Basic) {
            if (input.equals(id)) {
                return ((Placeholder.Basic) placeholder).resolve(ctx);
            }
        } else if (placeholder instanceof Placeholder.UsingArgument) {
            if (input.startsWith(id + "_") && input.length() > (id.length() + 1)) {
                String argument = input.substring(id.length() + 1);
                return ((Placeholder.UsingArgument) placeholder).resolve(ctx.withArgument(argument));
            }
        } else {
            throw new IllegalArgumentException("Unknown placeholder type: " + placeholder.getClass());
        }
        return null;
    }
}
