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

package me.lucko.luckperms.standalone;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.webeditor.WebEditorRequest;
import me.lucko.luckperms.common.webeditor.WebEditorSession;
import me.lucko.luckperms.standalone.app.integration.SingletonPlayer;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import net.luckperms.api.model.data.DataType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(parallel = true)
@Tag("docker")
public class WebEditorIntegrationTest {

    @Container
    private final GenericContainer<?> bytebin = new GenericContainer<>(DockerImageName.parse("ghcr.io/lucko/bytebin"))
            .withExposedPorts(8080);

    @Container
    private final GenericContainer<?> bytesocks = new GenericContainer<>(DockerImageName.parse("ghcr.io/lucko/bytesocks"))
            .withExposedPorts(8080);

    @Test
    public void testWebEditor(@TempDir Path tempDir) throws Exception {
        assertTrue(this.bytebin.isRunning());
        assertTrue(this.bytesocks.isRunning());

        String bytebinUrl = "http://" + this.bytebin.getHost() + ":" + this.bytebin.getFirstMappedPort() + "/";
        String bytesocksHost = this.bytesocks.getHost() + ":" + this.bytesocks.getFirstMappedPort();

        Map<String, String> config = ImmutableMap.<String, String>builder()
                .put("bytebin-url", bytebinUrl)
                .put("bytesocks-host", bytesocksHost)
                .put("bytesocks-use-tls", "false")
                .build();

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {

            // setup some example data
            UUID exampleUniqueId = UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d");
            plugin.getStorage().savePlayerData(exampleUniqueId, "Luck").join();

            User user = plugin.getStorage().loadUser(exampleUniqueId, null).join();
            user.setNode(DataType.NORMAL, Permission.builder().permission("test.node").build(), false);
            plugin.getStorage().saveUser(user).join();

            Group group = plugin.getGroupManager().getOrMake("default");
            group.setNode(DataType.NORMAL, Permission.builder().permission("other.test.node").build(), false);
            plugin.getStorage().saveGroup(group).join();

            // collect holders
            List<PermissionHolder> holders = new ArrayList<>();
            WebEditorRequest.includeMatchingGroups(holders, Predicates.alwaysTrue(), plugin);
            WebEditorRequest.includeMatchingUsers(holders, Collections.emptyList(), true, plugin);
            assertFalse(holders.isEmpty());

            // create a new editor session
            Sender sender = plugin.getSenderFactory().wrap(SingletonPlayer.INSTANCE);
            WebEditorSession session = WebEditorSession.create(holders, Collections.emptyList(), sender, "lp", plugin);
            String bytebinKey = session.open();
            String bytesocksKey = session.getSocket().getSocket().channelId();

            assertNotNull(bytebinKey);
            assertNotNull(bytesocksKey);

            // check bytebin payload
            OkHttpClient httpClient = plugin.getHttpClient();

            Response resp = httpClient.newCall(new Request.Builder()
                    .url(bytebinUrl + bytebinKey)
                    .build()).execute();
            assertTrue(resp.isSuccessful());
            assertEquals(200, resp.code());

            JsonObject respObject = GsonProvider.normal().fromJson(resp.body().string(), JsonObject.class);
            assertEquals(2, respObject.getAsJsonArray("permissionHolders").size());

            // check bytesocks channel is open
            CountDownLatch socketRespLatch = new CountDownLatch(1);
            AtomicReference<Response> socketResp = new AtomicReference<>();

            httpClient.newWebSocket(
                    new Request.Builder()
                            .url("ws://" + bytesocksHost + "/" +  bytesocksKey)
                            .build(),
                    new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket ws, Response resp) {
                            socketResp.set(resp);
                            socketRespLatch.countDown();
                        }

                        @Override
                        public void onFailure(WebSocket ws, Throwable err, @Nullable Response resp) {
                            socketResp.set(resp);
                            socketRespLatch.countDown();
                        }
                    });

            assertTrue(socketRespLatch.await(5, TimeUnit.SECONDS));
            assertEquals(101, socketResp.get().code());
        });
    }
}
