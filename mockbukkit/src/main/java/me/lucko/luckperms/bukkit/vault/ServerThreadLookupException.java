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

package me.lucko.luckperms.bukkit.vault;

import java.util.UUID;

/**
 * Exception thrown when an unsafe Vault request is made on the server thread.
 */
class ServerThreadLookupException extends RuntimeException {
    private static String msg(String reason) {
        return "A Vault API request has been made on the main server thread that LuckPerms cannot safely respond to.\n" +
                "This is NOT a bug - please do not report it to LuckPerms.\n" +
                "Instead, please carefully read the information given below.\n" +
                "\n" +
                "LuckPerms cannot respond to the request because to do so:\n" +
                "- " + reason + "\n" +
                "\n" +
                "Performing this lookup on the main server thread would cause your server to lag.\n" +
                "There are two solutions to this problem:\n" +
                "  a) Ask the author of the plugin making the request to perform Vault calls for\n" +
                "     offline players \"asynchronously\" (using the scheduler). Additionally, prefer\n" +
                "     using the methods that accept 'Player' or 'OfflinePlayer' instead of usernames.\n" +
                "     You should be able to identify the plugin making the request in the trace below.\n" +
                "  b) As a server admin, you can disable this exception by setting 'vault-unsafe-lookups'\n" +
                "     to true in the LuckPerms configuration file. However, please use this only as\n" +
                "     a last resort.";
    }

    ServerThreadLookupException(String player) {
        super(msg("it needs to lookup a UUID for '" + player + "' (an offline player)"));
    }

    ServerThreadLookupException(UUID uniqueId) {
        super(msg("it needs to lookup user data for '" + uniqueId + "' (an offline player) from the database"));
    }

}
