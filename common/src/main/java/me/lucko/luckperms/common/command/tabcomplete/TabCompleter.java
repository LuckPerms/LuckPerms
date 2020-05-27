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

package me.lucko.luckperms.common.command.tabcomplete;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Utility for computing tab completion results
 */
public class TabCompleter {

    public static TabCompleter create() {
        return new TabCompleter();
    }

    private final Map<Integer, CompletionSupplier> suppliers = new HashMap<>();
    private int from = Integer.MAX_VALUE;

    private TabCompleter() {

    }

    /**
     * Marks that the given completion supplier should be used to compute tab
     * completions at the given index.
     *
     * @param position the position
     * @param supplier the supplier
     * @return this
     */
    public TabCompleter at(int position, CompletionSupplier supplier) {
        Preconditions.checkState(position < this.from);
        this.suppliers.put(position, supplier);
        return this;
    }

    /**
     * Marks that the given completion supplier should be used to compute tab
     * completions at the given index and at all subsequent indexes infinitely.
     *
     * @param position the position
     * @param supplier the supplier
     * @return this
     */
    public TabCompleter from(int position, CompletionSupplier supplier) {
        Preconditions.checkState(this.from == Integer.MAX_VALUE);
        this.suppliers.put(position, supplier);
        this.from = position;
        return this;
    }

    public List<String> complete(List<String> args) {
        int lastIndex = 0;
        String partial;

        // nothing entered yet
        if (args.isEmpty() || (partial = args.get((lastIndex = args.size() - 1))).trim().isEmpty()) {
            return getCompletions(lastIndex, "");
        }

        // started typing something
        return getCompletions(lastIndex, partial);
    }

    private List<String> getCompletions(int position, String partial) {
        if (position >= this.from) {
            return this.suppliers.get(this.from).supplyCompletions(partial);
        }

        return this.suppliers.getOrDefault(position, CompletionSupplier.EMPTY).supplyCompletions(partial);
    }

    static Predicate<String> startsWithIgnoreCase(String prefix) {
        return string -> {
            if (string.length() < prefix.length()) {
                return false;
            }
            return string.regionMatches(true, 0, prefix, 0, prefix.length());
        };
    }

    static Predicate<String> containsIgnoreCase(String substring) {
        return string -> {
            if (string.length() < substring.length()) {
                return false;
            }
            return string.toLowerCase().contains(substring.toLowerCase());
        };
    }

}
