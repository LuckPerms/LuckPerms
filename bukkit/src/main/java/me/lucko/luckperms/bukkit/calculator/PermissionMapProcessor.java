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

package me.lucko.luckperms.bukkit.calculator;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.processor.AbstractOverrideWildcardProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import net.luckperms.api.util.Tristate;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.EnumSet;

/**
 * Permission Processor for Bukkits "default" permission system.
 */
public class PermissionMapProcessor extends AbstractOverrideWildcardProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(PermissionMapProcessor.class);

    private final LPBukkitPlugin plugin;
    private final boolean isOp;
    private final EnumSet<DefaultRule> defaultRules;

    public PermissionMapProcessor(LPBukkitPlugin plugin, boolean overrideWildcards, boolean isOp, EnumSet<DefaultRule> defaultRules) {
        super(overrideWildcards);
        this.plugin = plugin;
        this.isOp = isOp;
        this.defaultRules = defaultRules;
    }

    @Override
    public TristateResult hasPermission(String permission) {
        Permission defPerm = this.plugin.getPermissionMap().get(permission);
        if (defPerm == null) {
            return TristateResult.UNDEFINED;
        }
        return RESULT_FACTORY.result(applyDefaultRules(Tristate.of(defPerm.getDefault().getValue(this.isOp)), this.isOp));
    }

    private Tristate applyDefaultRules(Tristate prev, boolean isOp) {
        for (DefaultRule defaultRule : defaultRules) {
            prev = defaultRule.process(prev, isOp);
        }
        return prev;
    }
}
