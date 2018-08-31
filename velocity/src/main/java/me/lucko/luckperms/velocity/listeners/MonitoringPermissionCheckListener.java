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

package me.lucko.luckperms.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.proxy.Player;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.velocity.LPVelocityPlugin;
import me.lucko.luckperms.velocity.service.CompatibilityUtil;

import org.checkerframework.checker.nullness.qual.NonNull;

public class MonitoringPermissionCheckListener {
    private final LPVelocityPlugin plugin;

    public MonitoringPermissionCheckListener(LPVelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onOtherPermissionSetup(PermissionsSetupEvent e) {
        // players are handled separately
        if (e.getSubject() instanceof Player) {
            return;
        }

        e.setProvider(new MonitoredPermissionProvider(e.getProvider()));
    }

    private final class MonitoredPermissionProvider implements PermissionProvider {
        private final PermissionProvider delegate;

        MonitoredPermissionProvider(PermissionProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public @NonNull PermissionFunction createFunction(@NonNull PermissionSubject subject) {
            PermissionFunction function = this.delegate.createFunction(subject);
            return new MonitoredPermissionFunction(subject, function);
        }
    }

    private final class MonitoredPermissionFunction implements PermissionFunction {
        private final String name;
        private final PermissionFunction delegate;

        MonitoredPermissionFunction(PermissionSubject subject, PermissionFunction delegate) {
            this.delegate = delegate;
            this.name = determineName(subject);
        }

        @Override
        public com.velocitypowered.api.permission.@NonNull Tristate getPermissionSetting(@NonNull String permission) {
            com.velocitypowered.api.permission.Tristate setting = this.delegate.getPermissionSetting(permission);

            // report result
            Tristate result = CompatibilityUtil.convertTristate(setting);
            String name = "internal/" + this.name;

            MonitoringPermissionCheckListener.this.plugin.getVerboseHandler().offerCheckData(CheckOrigin.PLATFORM_LOOKUP_CHECK, name, ContextSet.empty(), permission, result);
            MonitoringPermissionCheckListener.this.plugin.getPermissionRegistry().offer(permission);

            return setting;
        }
    }

    private String determineName(PermissionSubject subject) {
        if (subject == this.plugin.getBootstrap().getProxy().getConsoleCommandSource()) {
            return "console";
        }
        return subject.getClass().getSimpleName();
    }
}
