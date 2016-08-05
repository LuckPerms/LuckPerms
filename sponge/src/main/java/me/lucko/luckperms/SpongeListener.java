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

class SpongeListener extends AbstractListener {
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
