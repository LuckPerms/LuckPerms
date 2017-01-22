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

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.core.model.Track;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MetaStackElement {

    Optional<Map.Entry<Integer, String>> getEntry();

    boolean accumulateNode(LocalizedNode node);

    /**
     * Returns true if the types do not match
     * @param expectingPrefix if the method is expecting a prefix
     * @param node the node to check
     * @return true if the accumulation should return
     */
    static boolean checkMetaType(boolean expectingPrefix, LocalizedNode node) {
        if (expectingPrefix) {
            if (!node.isPrefix()) {
                return true;
            }
        } else {
            if (!node.isSuffix()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the node is not held by a user
     * @param node the node to check
     * @return true if the accumulation should return
     */
    static boolean checkOwnElement(LocalizedNode node) {
        if (node.getLocation() == null || node.getLocation().equals("")) {
            return true;
        }

        try {
            UUID.fromString(node.getLocation());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Returns true if the node is not held by a group on the track
     * @param node the node to check
     * @param track the track
     * @return true if the accumulation should return
     */
    static boolean checkTrackElement(LuckPermsPlugin plugin, LocalizedNode node, String track) {
        if (node.getLocation() == null || node.getLocation().equals("")) {
            return true;
        }

        Track t = plugin.getTrackManager().getIfLoaded(track);
        return t == null || !t.containsGroup(node.getLocation());
    }

}
