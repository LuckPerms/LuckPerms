/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.caching.stacking;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.common.caching.stacking.elements.HighestPriorityElement;
import me.lucko.luckperms.common.caching.stacking.elements.HighestPriorityOwnElement;
import me.lucko.luckperms.common.caching.stacking.elements.HighestPriorityTrackElement;
import me.lucko.luckperms.common.caching.stacking.elements.LowestPriorityElement;
import me.lucko.luckperms.common.caching.stacking.elements.LowestPriorityOwnElement;
import me.lucko.luckperms.common.caching.stacking.elements.LowestPriorityTrackElement;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class StackElementFactory {

    public static Optional<MetaStackElement> fromString(LuckPermsPlugin plugin, String s, boolean prefix) {
        s = s.toLowerCase();

        if (s.equals("highest")) {
            return Optional.of(new HighestPriorityElement(prefix));
        }

        if (s.equals("lowest")) {
            return Optional.of(new LowestPriorityElement(prefix));
        }

        if (s.equals("highest_own")) {
            return Optional.of(new HighestPriorityOwnElement(prefix));
        }

        if (s.equals("lowest_own")) {
            return Optional.of(new LowestPriorityOwnElement(prefix));
        }

        if (s.startsWith("highest_on_track_") && s.length() > "highest_on_track_".length()) {
            String track = s.substring("highest_on_track_".length());
            return Optional.of(new HighestPriorityTrackElement(prefix, plugin, track));
        }

        if (s.startsWith("lowest_on_track_") && s.length() > "lowest_on_track_".length()) {
            String track = s.substring("lowest_on_track_".length());
            return Optional.of(new LowestPriorityTrackElement(prefix, plugin, track));
        }

        new IllegalArgumentException("Cannot parse MetaStackElement: " + s).printStackTrace();
        return Optional.empty();
    }

    public static List<MetaStackElement> fromList(LuckPermsPlugin plugin, List<String> strings, boolean prefix) {
        return strings.stream().map(s -> fromString(plugin, s, prefix)).filter(Optional::isPresent).map(Optional::get).collect(ImmutableCollectors.toImmutableList());
    }

}
