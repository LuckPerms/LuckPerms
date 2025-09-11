package me.lucko.luckperms.minestom.app.integration;

import net.kyori.adventure.audience.Audience;

public interface MinestomPermissible extends Audience {

    boolean hasPermission(String node);
}
