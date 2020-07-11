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

package me.lucko.luckperms.common.floodgate;

import me.lucko.luckperms.common.storage.misc.DataConstraints;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Manages LuckPerms integration with the Minecraft Bedrock authentication plugin Floodgate (https://github.com/GeyserMC/Floodgate).
 * Unless lenient username checking is on, LuckPerms will fail to parse the Floodgate username
 * without this code as the prefix will interfere.
 * This code is also used with the /lp user [username] info command to show if the user is a Floodgate player.
 */
public abstract class FloodgateManager {

    /**
     * Test a username to ensure it complies with the Floodgate configuration.
     */
    public Predicate<String> playerFloodgateUsernameTest = s -> !s.isEmpty() && s.length() <= DataConstraints.MAX_PLAYER_USERNAME_LENGTH && getPattern().matcher(s).matches();

    /**
     * The username prefix as defined in the Floodgate config. Used in Floodgate to prevent username conflicts.
     */
    public final String prefix;

    public FloodgateManager(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Determine if the user is a Floodgate player.
     * @param uuid the UUID of the target player.
     * @return true if the player has a Floodgate UUID or is using Floodgate as an authentication method
     * (for linked accounts).
     */
    public abstract boolean isFloodgatePlayer(UUID uuid);

    /**
     * Gets the dynamic regex pattern used for Floodgate players - it changes depending on the prefix value in the Floodgate config.
     * @return the pattern to use for username validation.
     */
    private Pattern getPattern() {
        if (this.prefix == null || this.prefix.equals("")) {
            return Pattern.compile("[a-zA-Z0-9_]*"); // No need to worry about the prefix; just check for valid characters
        }
        String pattern = String.format("\\%s?[a-zA-Z0-9_]*", this.prefix);
        return Pattern.compile(pattern);
    }

}
