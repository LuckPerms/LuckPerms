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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.caching.type.PermissionCache;
import me.lucko.luckperms.common.utils.PasteUtils;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A readable view of a branch of {@link TreeNode}s.
 */
public class TreeView {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    // the root of the tree
    private final String rootPosition;

    // how many levels / branches to display
    private final int maxLevel;

    // the actual tree object
    private final ImmutableTreeNode view;

    public TreeView(PermissionVault source, String rootPosition, int maxLevel) {
        this.rootPosition = rootPosition;
        this.maxLevel = maxLevel;

        Optional<TreeNode> root = findRoot(rootPosition, source);
        this.view = root.map(TreeNode::makeImmutableCopy).orElse(null);
    }

    /**
     * Gets if this TreeView has any content.
     *
     * @return true if the treeview has data
     */
    public boolean hasData() {
        return view != null;
    }

    /**
     * Finds the root of the tree node at the given position
     *
     * @param source the node source
     * @return the root, if it exists
     */
    private static Optional<TreeNode> findRoot(String rootPosition, PermissionVault source) {
        // get the root of the permission vault
        TreeNode root = source.getRootNode();

        // just return the root
        if (rootPosition.equals(".")) {
            return Optional.of(root);
        }

        // get the parts of the node
        List<String> parts = Splitter.on('.').omitEmptyStrings().splitToList(rootPosition);

        // for each part
        for (String part : parts) {

            // check the current root has some children
            if (!root.getChildren().isPresent()) {
                return Optional.empty();
            }

            // get the current roots children
            Map<String, TreeNode> branch = root.getChildren().get();

            // get the new root
            root = branch.get(part);
            if (root == null) {
                return Optional.empty();
            }
        }

        return Optional.of(root);
    }

    /**
     * Converts the view to a readable list
     *
     * <p>The list contains KV pairs, where the key is the tree padding/structure,
     * and the value is the actual permission.</p>
     *
     * @return a list of the nodes in this view
     */
    private List<Map.Entry<String, String>> asTreeList() {
        // work out the prefix to apply
        // since the view is relative, we need to prepend this to all permissions
        String prefix = rootPosition.equals(".") ? "" : (rootPosition + ".");


        List<Map.Entry<String, String>> ret = new ArrayList<>();

        // iterate the node endings in the view
        for (Map.Entry<Integer, String> s : view.getNodeEndings()) {
            // don't include the node if it exceeds the max level
            if (s.getKey() >= maxLevel) {
                continue;
            }

            // generate the tree padding characters from the node level
            String treeStructure = Strings.repeat("│  ", s.getKey()) + "├── ";
            // generate the permission, using the prefix and the node
            String permission = prefix + s.getValue();

            ret.add(Maps.immutableEntry(treeStructure, permission));
        }

        return ret;
    }

    /**
     * Uploads the data contained in this TreeView to a paste, and returns the URL.
     *
     * @param version the plugin version string
     * @return the url, or null
     * @see PasteUtils#paste(String, List)
     */
    public String uploadPasteData(String version) {
        // only paste if there is actually data here
        if (!hasData()) {
            throw new IllegalStateException();
        }

        // get the data contained in the view in a list form
        // for each entry, the key is the padding tree characters
        // and the value is the actual permission string
        List<Map.Entry<String, String>> ret = asTreeList();

        // build the header of the paste
        ImmutableList.Builder<String> builder = getPasteHeader(version, "none", ret.size());

        // add the tree data
        builder.add("```");
        for (Map.Entry<String, String> e : ret) {
            builder.add(e.getKey() + e.getValue());
        }
        builder.add("```");

        // clear the initial data map
        ret.clear();

        // upload the return the data
        return PasteUtils.paste("LuckPerms Permission Tree", ImmutableList.of(Maps.immutableEntry("luckperms-tree.md", builder.build().stream().collect(Collectors.joining("\n")))));
    }

    /**
     * Uploads the data contained in this TreeView to a paste, and returns the URL.
     *
     * <p>Unlike {@link #uploadPasteData(String)}, this method will check each permission
     * against a corresponding user, and colorize the output depending on the check results.</p>
     *
     * @param version the plugin version string
     * @param username the username of the reference user
     * @param checker the permission data instance to check against
     * @return the url, or null
     * @see PasteUtils#paste(String, List)
     */
    public String uploadPasteData(String version, String username, PermissionCache checker) {
        // only paste if there is actually data here
        if (!hasData()) {
            throw new IllegalStateException();
        }

        // get the data contained in the view in a list form
        // for each entry, the key is the padding tree characters
        // and the value is the actual permission string
        List<Map.Entry<String, String>> ret = asTreeList();

        // build the header of the paste
        ImmutableList.Builder<String> builder = getPasteHeader(version, username, ret.size());

        // add the tree data
        builder.add("```diff");
        for (Map.Entry<String, String> e : ret) {

            // lookup a permission value for the node
            Tristate tristate = checker.getPermissionValue(e.getValue(), CheckOrigin.INTERNAL);

            // append the data to the paste
            builder.add(getTristateDiffPrefix(tristate) + e.getKey() + e.getValue());
        }
        builder.add("```");

        // clear the initial data map
        ret.clear();

        // upload the return the data
        return PasteUtils.paste("LuckPerms Permission Tree", ImmutableList.of(Maps.immutableEntry("luckperms-tree.md", builder.build().stream().collect(Collectors.joining("\n")))));
    }

    private static String getTristateDiffPrefix(Tristate t) {
        switch (t) {
            case TRUE:
                return "+ ";
            case FALSE:
                return "- ";
            default:
                return "# ";
        }
    }

    private ImmutableList.Builder<String> getPasteHeader(String version, String referenceUser, int size) {
        String date = DATE_FORMAT.format(new Date(System.currentTimeMillis()));
        String selection = rootPosition.equals(".") ? "any" : "`" + rootPosition + "`";

        return ImmutableList.<String>builder()
                .add("## Permission Tree")
                .add("#### This file was automatically generated by [LuckPerms](https://github.com/lucko/LuckPerms) v" + version)
                .add("")
                .add("### Metadata")
                .add("| Selection | Max Recursion | Reference User | Size | Produced at |")
                .add("|-----------|---------------|----------------|------|-------------|")
                .add("| " + selection + " | " + maxLevel + " | " + referenceUser + " | **" + size + "** | " + date + " |")
                .add("")
                .add("### Output");
    }

}
