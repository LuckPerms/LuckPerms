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

package me.lucko.luckperms.common.treeview;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Builds a {@link TreeView}.
 */
@Accessors(fluent = true)
public class TreeViewBuilder {
    public static TreeViewBuilder newBuilder() {
        return new TreeViewBuilder();
    }

    @Setter
    private String rootPosition;
    @Setter
    private int maxLevels;

    private TreeViewBuilder() {
        this.rootPosition = ".";
        this.maxLevels = 5;
    }

    public TreeView build(PermissionVault source) {
        if (maxLevels < 1) {
            maxLevels = 1;
        }
        if (rootPosition.equals("") || rootPosition.equals("*")) {
            rootPosition = ".";
        } else if (!rootPosition.equals(".") && rootPosition.endsWith(".")) {
            rootPosition = rootPosition.substring(0, rootPosition.length() - 1);
        }

        return new TreeView(source, rootPosition, maxLevels);
    }

}
