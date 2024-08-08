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

package me.lucko.luckperms.neoforge.capabilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class UserCapabilityListener {

    @SubscribeEvent
    public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                UserCapability.CAPABILITY,
                EntityType.PLAYER,
                (player, ctx) -> {
                    if (!(player instanceof ServerPlayer)) {
                        // Don't attach to LocalPlayer
                        return null;
                    }
                    return new UserCapabilityImpl();
                }
        );
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        Player previousPlayer = event.getOriginal();
        Player currentPlayer = event.getEntity();

        try {
            UserCapabilityImpl previous = UserCapabilityImpl.get(previousPlayer);
            UserCapabilityImpl current = UserCapabilityImpl.get(currentPlayer);

            current.initialise(previous);
            previous.invalidate();
            current.getQueryOptionsCache().invalidate();
        } catch (IllegalStateException e) {
            // continue on if we cannot copy original data
        }
    }

}
