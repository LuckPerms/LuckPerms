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

package me.lucko.luckperms.bukkit.inject.permissible;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.context.BukkitContextManager;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier;
import me.lucko.luckperms.common.model.User;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LuckPermsPermissibleTest {

    @Mock private Player player;
    @Mock private User user;
    @Mock private LPBukkitPlugin plugin;
    @Mock private BukkitContextManager contextManager;
    @Mock private QueryOptionsSupplier queryOptionsSupplier;
    @Mock private LuckPermsConfiguration configuration;

    @BeforeEach
    public void setupMocks() {
        when(this.plugin.getContextManager()).thenReturn(this.contextManager);
        when(this.contextManager.createQueryOptionsSupplier(this.player)).thenReturn(this.queryOptionsSupplier);
        when(this.plugin.getConfiguration()).thenReturn(this.configuration);
    }

    @Test
    public void testRecalculatePermissionsRevokesDisabledOperator() {
        LuckPermsPermissible permissible = createPermissible(false, false, true);

        permissible.recalculatePermissions();

        verify(this.queryOptionsSupplier).invalidateCache();
        verify(this.player).setOp(false);
    }

    @Test
    public void testRecalculatePermissionsKeepsEnabledOperator() {
        LuckPermsPermissible permissible = createPermissible(true, false, true);

        permissible.recalculatePermissions();

        verify(this.queryOptionsSupplier).invalidateCache();
        verify(this.player, never()).setOp(anyBoolean());
    }

    @Test
    public void testRecalculatePermissionsKeepsAutoOperator() {
        LuckPermsPermissible permissible = createPermissible(false, true, true);

        permissible.recalculatePermissions();

        verify(this.queryOptionsSupplier).invalidateCache();
        verify(this.player, never()).setOp(anyBoolean());
    }

    @Test
    public void testRecalculatePermissionsDoesNotUpdateNonOperator() {
        LuckPermsPermissible permissible = createPermissible(false, false, false);

        permissible.recalculatePermissions();

        verify(this.queryOptionsSupplier).invalidateCache();
        verify(this.player, never()).setOp(anyBoolean());
    }

    private LuckPermsPermissible createPermissible(boolean opsEnabled, boolean autoOp, boolean op) {
        lenient().when(this.configuration.get(ConfigKeys.OPS_ENABLED)).thenReturn(opsEnabled);
        lenient().when(this.configuration.get(ConfigKeys.AUTO_OP)).thenReturn(autoOp);
        lenient().when(this.player.isOp()).thenReturn(op);
        return new LuckPermsPermissible(this.player, this.user, this.plugin);
    }

}
