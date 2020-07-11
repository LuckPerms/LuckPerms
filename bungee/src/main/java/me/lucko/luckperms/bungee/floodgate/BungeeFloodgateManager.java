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

package me.lucko.luckperms.bungee.floodgate;

import me.lucko.luckperms.common.floodgate.FloodgateManager;
import net.md_5.bungee.api.ProxyServer;
import org.geysermc.floodgate.BungeePlugin;
import org.geysermc.floodgate.FloodgateAPI;

import java.util.UUID;

public class BungeeFloodgateManager extends FloodgateManager {

    public BungeeFloodgateManager(String prefix) {
        super(prefix);
    }

    @Override
    public boolean isFloodgatePlayer(UUID uuid) {
        return FloodgateAPI.isBedrockPlayer(uuid);
    }

    public static FloodgateManager checkFloodgateIntegration() {
        if (ProxyServer.getInstance().getPluginManager().getPlugin("floodgate-bungee") != null) {
            return new BungeeFloodgateManager(BungeePlugin.getInstance().getConfig().getUsernamePrefix());
        }
        return null;
    }
}
