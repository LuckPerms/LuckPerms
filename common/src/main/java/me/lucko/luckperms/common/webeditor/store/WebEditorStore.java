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

package me.lucko.luckperms.common.webeditor.store;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.webeditor.socket.CryptographyUtils;

import java.security.KeyPair;
import java.util.concurrent.CompletableFuture;

/**
 * Contains a store of known web editor sessions and provides a lookup function for
 * trusted editor public keys.
 */
public class WebEditorStore {
    private final WebEditorSessionMap sessions;
    private final WebEditorSocketMap sockets;
    private final WebEditorKeystore keystore;
    private final CompletableFuture<KeyPair> keyPair;

    public WebEditorStore(LuckPermsPlugin plugin) {
        this.sessions = new WebEditorSessionMap();
        this.sockets = new WebEditorSocketMap();
        this.keystore = new WebEditorKeystore(plugin.getBootstrap().getConfigDirectory().resolve("editor-keystore.json"));
        this.keyPair = CompletableFuture.supplyAsync(CryptographyUtils::generateKeyPair, plugin.getBootstrap().getScheduler().async());
    }

    public WebEditorSessionMap sessions() {
        return this.sessions;
    }

    public WebEditorSocketMap sockets() {
        return this.sockets;
    }

    public WebEditorKeystore keystore() {
        return this.keystore;
    }

    public KeyPair keyPair() {
        if (!this.keyPair.isDone()) {
            throw new IllegalStateException("Web editor keypair has not been generated yet! Has the server just started?");
        }
        return this.keyPair.join();
    }

}
