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

package me.lucko.luckperms.hytale.calculator;

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.processor.AbstractPermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import me.lucko.luckperms.hytale.calculator.virtualgroups.VirtualGroupsMap;
import net.luckperms.api.node.Node;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Permission Processor for Hytale "virtual groups".
 */
public class HytaleVirtualGroupProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    public static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(HytaleVirtualGroupProcessor.class);

    private final PermissionCalculator calculator;

    public HytaleVirtualGroupProcessor(LPHytalePlugin plugin, ImmutableSet<String> virtualGroups, Map<String, Node> sourceMap) {
        VirtualGroupsMap virtualGroupsMap = plugin.getVirtualGroupsMap();

        Set<String> groups = virtualGroups;
        for (String group : virtualGroupsMap.getAllVirtualGroups()) {
            if (groups.contains(group)) {
                continue;
            }

            Node node = sourceMap.get(Inheritance.key(group));
            if (node != null && node.getValue()) {
                if (groups instanceof ImmutableSet) {
                    groups = new HashSet<>(groups);
                }
                groups.add(group);
            }
        }

        this.calculator = virtualGroupsMap.getCalculator(ImmutableSet.copyOf(groups));
    }

    @Override
    public TristateResult hasPermission(String permission) {
        return RESULT_FACTORY.result(this.calculator.checkPermission(permission, CheckOrigin.INTERNAL).result());
    }

}
