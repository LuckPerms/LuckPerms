package me.lucko.luckperms.velocity.service;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;

import me.lucko.luckperms.common.contexts.ContextsSupplier;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerPermissionProvider implements PermissionProvider, PermissionFunction {
    private final Player player;
    private final User user;
    private final ContextsSupplier contextsSupplier;

    public PlayerPermissionProvider(Player player, User user, ContextsSupplier contextsSupplier) {
        this.player = player;
        this.user = user;
        this.contextsSupplier = contextsSupplier;
    }

    @Override
    public @NonNull PermissionFunction createFunction(@NonNull PermissionSubject subject) {
        Preconditions.checkState(subject == this.player, "createFunction called with different argument");
        return this;
    }

    @Override
    public @NonNull Tristate getPermissionSetting(@NonNull String permission) {
        return CompatibilityUtil.convertTristate(this.user.getCachedData().getPermissionData(this.contextsSupplier.getContexts()).getPermissionValue(permission, CheckOrigin.PLATFORM_PERMISSION_CHECK));
    }
}
