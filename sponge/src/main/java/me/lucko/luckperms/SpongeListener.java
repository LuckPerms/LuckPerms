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

package me.lucko.luckperms;

import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.AbstractListener;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;

@SuppressWarnings("WeakerAccess")
public class SpongeListener extends AbstractListener {
    private final LPSpongePlugin plugin;

    SpongeListener(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Listener
    public void onClientAuth(ClientConnectionEvent.Auth e) {
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // Datastore is disabled, prevent players from joining the server
            // Just don't load their data, they will be kicked at login
            return;
        }

        final GameProfile p = e.getProfile();
        onAsyncLogin(p.getUniqueId(), p.getName().get());
    }

    @SuppressWarnings("deprecation")
    @Listener
    public void onClientLogin(ClientConnectionEvent.Login e) {
        final GameProfile player = e.getProfile();
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(player.getUniqueId()));

        if (user == null) {
            e.setCancelled(true);
            e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.toString()));
            return;
        }

        user.refreshPermissions();
    }

    @Listener
    public void onClientJoin(ClientConnectionEvent.Join e) {
        // Refresh permissions again
        refreshPlayer(e.getTargetEntity().getUniqueId());
    }

    @Listener
    public void onPlayerTeleport(DisplaceEntityEvent.Teleport e) {
        final Entity entity = e.getTargetEntity();
        if (!(entity instanceof Player)){
            return;
        }

        refreshPlayer(entity.getUniqueId());
    }

    @Listener
    public void onClientLeave(ClientConnectionEvent.Disconnect e) {
        onLeave(e.getTargetEntity().getUniqueId());
    }
}
