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

package me.lucko.luckperms.common.webeditor;

import com.github.benmanes.caffeine.cache.Cache;

import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeType;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Contains a store of known web editor sessions and provides a lookup function for
 * trusted editor public keys.
 */
public class WebEditorStore {

    private final Sessions sessions;
    private final Sockets sockets;
    private final Keystore keystore;

    public WebEditorStore(LuckPermsPlugin plugin) {
        this.sessions = new Sessions();
        this.sockets = new Sockets();
        this.keystore = new Keystore(plugin.getBootstrap().getConfigDirectory().resolve("editor-keystore.json"));
    }

    public Sessions sessions() {
        return this.sessions;
    }

    public Sockets sockets() {
        return this.sockets;
    }

    public Keystore keystore() {
        return this.keystore;
    }

    public static final class Sessions {
        private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

        /**
         * Adds a newly created session to the store.
         *
         * @param id the id of the session
         */
        public void addNewSession(String id) {
            this.sessions.put(id, SessionState.IN_PROGRESS);
        }

        /**
         * Gets the session state for the given session id.
         *
         * @param id the id of the session
         * @return the session state
         */
        public @NonNull SessionState getSessionState(String id) {
            return this.sessions.getOrDefault(id, SessionState.NOT_KNOWN);
        }

        /**
         * Marks a given session as complete.
         *
         * @param id the id of the session
         */
        public void markSessionCompleted(String id) {
            this.sessions.put(id, SessionState.COMPLETED);
        }
    }

    public static final class Sockets {
        private final Cache<UUID, WebEditorSocket> sockets = CaffeineFactory.newBuilder()
                .weakValues()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        public WebEditorSocket getSocket(Sender sender) {
            return this.sockets.getIfPresent(sender.getUniqueId());
        }

        public void putSocket(Sender sender, WebEditorSocket socket) {
            this.sockets.put(sender.getUniqueId(), socket);
        }
    }

    public static final class Keystore {
        private static final String META_KEY = "lp-editor-key";

        private final Path consoleKeysPath;
        private final Set<String> trustedConsoleKeys;

        public Keystore(Path consoleKeysPath) {
            this.consoleKeysPath = consoleKeysPath;
            this.trustedConsoleKeys = new CopyOnWriteArraySet<>();

            try {
                load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Checks if the given public key has been trusted by the sender.
         *
         * @param sender the sender
         * @param publicKey the public key
         * @return true if trusted
         */
        public boolean isTrusted(Sender sender, byte[] publicKey) {
            return isTrusted(sender, hashPublicKey(publicKey));
        }

        /**
         * Checks if the given public key hash has been trusted by the sender.
         *
         * @param sender the sender
         * @param hash the public key hash
         * @return true if trusted
         */
        public boolean isTrusted(Sender sender, String hash) {
            if (sender.isConsole()) {
                return isTrustedConsole(hash);
            } else {
                User user = sender.getPlugin().getUserManager().getIfLoaded(sender.getUniqueId());
                return user != null && isTrusted(user, hash);
            }
        }

        /**
         * Trusts the given public key for the sender.
         *
         * @param sender the sender
         * @param publicKey the public key
         */
        public void trust(Sender sender, byte[] publicKey) {
            trust(sender, hashPublicKey(publicKey));
        }

        /**
         * Trusts the given public key hash for the sender.
         *
         * @param sender the sender
         * @param hash the public key hash
         */
        public void trust(Sender sender, String hash) {
            if (sender.isConsole()) {
                trustConsole(hash);
            } else {
                User user = sender.getPlugin().getUserManager().getIfLoaded(sender.getUniqueId());
                if (user != null) {
                    trust(user, hash);
                }
            }
        }

        // console

        private boolean isTrustedConsole(String hash) {
            return this.trustedConsoleKeys.contains(hash);
        }

        private void trustConsole(String hash) {
            this.trustedConsoleKeys.add(hash);

            try {
                save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void load() throws Exception {
            if (Files.exists(this.consoleKeysPath)) {
                try (BufferedReader reader = Files.newBufferedReader(this.consoleKeysPath, StandardCharsets.UTF_8)) {
                    KeystoreFile file = GsonProvider.normal().fromJson(reader, KeystoreFile.class);
                    if (file != null && file.consoleKeys != null) {
                        this.trustedConsoleKeys.addAll(file.consoleKeys);
                    }
                }
            }
        }

        private void save() throws Exception {
            try (BufferedWriter writer = Files.newBufferedWriter(this.consoleKeysPath, StandardCharsets.UTF_8)) {
                KeystoreFile file = new KeystoreFile();
                file.consoleKeys = new ArrayList<>(this.trustedConsoleKeys);
                GsonProvider.prettyPrinting().toJson(file, writer);
            }
        }

        // users

        private boolean isTrusted(User user, String hash) {
            String key = user.getCachedData().getMetaData(QueryOptionsImpl.DEFAULT_CONTEXTUAL)
                    .getMetaValue(META_KEY, CheckOrigin.INTERNAL).result();

            if (key == null || key.isEmpty()) {
                return false;
            }

            return hash.equals(key);
        }

        private void trust(User user, String hash) {
            user.removeIf(DataType.NORMAL, ImmutableContextSetImpl.EMPTY, NodeType.META.predicate(mn -> mn.getMetaKey().equals(META_KEY)), false);
            user.setNode(DataType.NORMAL, Meta.builder(META_KEY, hash).build(), false);

            user.getPlugin().getStorage().saveUser(user).join();
        }

        private static String hashPublicKey(byte[] publicKey) {
            byte[] digest = createDigest().digest(publicKey);
            return Base64.getEncoder().encodeToString(digest);
        }

        private static MessageDigest createDigest() {
            try {
                return MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings({"FieldMayBeFinal", "unused"})
        private static class KeystoreFile {
            private String _comment = "This file stores a list of trusted editor public keys";
            private List<String> consoleKeys = null;
        }
    }

}
