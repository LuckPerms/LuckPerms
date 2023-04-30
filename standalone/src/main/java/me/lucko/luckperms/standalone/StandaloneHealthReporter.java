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

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.standalone.app.integration.HealthReporter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class StandaloneHealthReporter implements HealthReporter {
    private final LPStandalonePlugin plugin;

    public StandaloneHealthReporter(LPStandalonePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Health poll() {
        if (!this.plugin.isRunning()) {
            return Health.down(Collections.emptyMap());
        }

        Map<String, String> meta = this.plugin.getStorage().getMeta().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> render(e.getKey()).toLowerCase(Locale.ROOT),
                        e -> render(e.getValue())
                ));

        if ("false".equals(meta.get("connected"))) {
            return Health.down(Collections.singletonMap("reason", "storage disconnected"));
        }

        return Health.up(meta);
    }

    private static String render(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(
                TranslationManager.render(component)
        );
    }
}
