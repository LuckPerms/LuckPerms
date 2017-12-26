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

package me.lucko.luckperms.sponge.migration;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.sponge.service.CompatibilityUtil;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class SpongeMigrationUtils {

    public static void migrateSubject(Subject from, PermissionHolder to, int priority) {
        if (to.getType().isGroup()) {
            MigrationUtils.setGroupWeight((Group) to, priority);
        }

        // Migrate permissions
        Map<Set<Context>, Map<String, Boolean>> perms = from.getSubjectData().getAllPermissions();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : perms.entrySet()) {
            ContextSet context = CompatibilityUtil.convertContexts(e.getKey());

            for (Map.Entry<String, Boolean> perm : e.getValue().entrySet()) {
                if (perm.getKey().isEmpty()) {
                    continue;
                }

                to.setPermission(NodeFactory.builder(perm.getKey()).withExtraContext(context).setValue(perm.getValue()).build());
            }
        }

        // Migrate options
        Map<Set<Context>, Map<String, String>> opts = from.getSubjectData().getAllOptions();
        for (Map.Entry<Set<Context>, Map<String, String>> e : opts.entrySet()) {
            ContextSet context = CompatibilityUtil.convertContexts(e.getKey());

            for (Map.Entry<String, String> opt : e.getValue().entrySet()) {
                if (opt.getKey().isEmpty() || opt.getValue().isEmpty()) {
                    continue;
                }

                if (opt.getKey().equalsIgnoreCase(NodeFactory.PREFIX_KEY)) {
                    to.setPermission(NodeFactory.buildPrefixNode(priority, opt.getValue()).withExtraContext(context).setValue(true).build());
                } else if (opt.getKey().equalsIgnoreCase(NodeFactory.SUFFIX_KEY)) {
                    to.setPermission(NodeFactory.buildSuffixNode(priority, opt.getValue()).withExtraContext(context).setValue(true).build());
                } else {
                    to.setPermission(NodeFactory.buildMetaNode(opt.getKey(), opt.getValue()).withExtraContext(context).setValue(true).build());
                }
            }
        }

        // Migrate parents
        Map<Set<Context>, List<Subject>> parents = from.getSubjectData().getAllParents();
        for (Map.Entry<Set<Context>, List<Subject>> e : parents.entrySet()) {
            ContextSet context = CompatibilityUtil.convertContexts(e.getKey());

            for (Subject s : e.getValue()) {
                if (!s.getContainingCollection().getIdentifier().equalsIgnoreCase(PermissionService.SUBJECTS_GROUP)) {
                    continue; // LuckPerms does not support persisting other subject types.
                }

                to.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(s.getIdentifier())).withExtraContext(context).setValue(true).build());
            }
        }
    }

    public static void migrateSubjectData(SubjectData from, SubjectData to) {
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : from.getAllPermissions().entrySet()) {
            for (Map.Entry<String, Boolean> e1 : e.getValue().entrySet()) {
                to.setPermission(e.getKey(), e1.getKey(), Tristate.fromBoolean(e1.getValue()));
            }
        }

        for (Map.Entry<Set<Context>, Map<String, String>> e : from.getAllOptions().entrySet()) {
            for (Map.Entry<String, String> e1 : e.getValue().entrySet()) {
                to.setOption(e.getKey(), e1.getKey(), e1.getValue());
            }
        }

        for (Map.Entry<Set<Context>, List<Subject>> e : from.getAllParents().entrySet()) {
            for (Subject s : e.getValue()) {
                to.addParent(e.getKey(), s);
            }
        }
    }

}
