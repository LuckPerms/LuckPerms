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

package me.lucko.luckperms.common.config;

import me.lucko.luckperms.common.config.generic.adapter.FileSecretConfigAdapter;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class FileSecretConfigAdapterTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private PluginLogger logger;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.plugin.getLogger()).thenReturn(this.logger);
    }

    @Test
    public void testRead(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("luckperms_server");
        Files.write(path, "test".getBytes());

        FileSecretConfigAdapter adapter = new FileSecretConfigAdapter(this.plugin, directory.toString());

        String server = adapter.getString("server", null);
        assertEquals("test", server);
    }

    @Test
    public void testNoOp() throws IOException {
        FileSecretConfigAdapter adapter = new FileSecretConfigAdapter(this.plugin, null);

        String server = adapter.getString("server", null);
        assertNull(server);
    }

}
