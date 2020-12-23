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

package me.lucko.luckperms.common.locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import org.checkerframework.checker.nullness.qual.Nullable;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TranslationRepository {
    private static final String TRANSLATIONS_INFO_ENDPOINT = "https://metadata.luckperms.net/data/translations";
    private static final String TRANSLATIONS_DOWNLOAD_ENDPOINT = "https://metadata.luckperms.net/translation/";
    private static final long MAX_BUNDLE_SIZE = 1048576L; // 1mb
    private static final long CACHE_MAX_AGE = TimeUnit.HOURS.toMillis(23);

    private final LuckPermsPlugin plugin;

    public TranslationRepository(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets a list of available languages.
     *
     * @return a list of languages
     * @throws IOException if an i/o error occurs
     * @throws UnsuccessfulRequestException if the http request fails
     */
    public List<LanguageInfo> getAvailableLanguages() throws IOException, UnsuccessfulRequestException {
        return getTranslationsMetadata().languages;
    }

    /**
     * Schedules a refresh of the current translations if necessary.
     */
    public void scheduleRefresh() {
        if (!this.plugin.getConfiguration().get(ConfigKeys.AUTO_INSTALL_TRANSLATIONS)) {
            return; // skip
        }

        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            try {
                refresh();
            } catch (Exception e) {
                // ignore
            }
        });
    }

    private void refresh() throws Exception {
        Path translationsDirectory = this.plugin.getTranslationManager().getTranslationsDirectory();
        try {
            MoreFiles.createDirectoriesIfNotExists(translationsDirectory);
        } catch (IOException e) {
            // ignore
        }

        long lastRefresh = 0L;

        Path repoStatusFile = translationsDirectory.resolve("repository.json");
        if (Files.exists(repoStatusFile)) {
            try (BufferedReader reader = Files.newBufferedReader(repoStatusFile, StandardCharsets.UTF_8)) {
                JsonObject status = GsonProvider.normal().fromJson(reader, JsonObject.class);
                if (status.has("lastRefresh")) {
                    lastRefresh = status.get("lastRefresh").getAsLong();
                }
            } catch (Exception e) {
                // ignore
            }
        }

        long timeSinceLastRefresh = System.currentTimeMillis() - lastRefresh;
        if (timeSinceLastRefresh <= CACHE_MAX_AGE) {
            return;
        }

        MetadataResponse metadata = getTranslationsMetadata();

        if (timeSinceLastRefresh <= metadata.cacheMaxAge) {
            return;
        }

        // perform a refresh!
        downloadAndInstallTranslations(metadata.languages, null, true);
    }

    /**
     * Downloads and installs translations for the given languages.
     *
     * @param languages the languages to install translations for
     * @param sender the sender to report progress to
     * @param updateStatus if the status file should be updated
     */
    public void downloadAndInstallTranslations(List<LanguageInfo> languages, @Nullable Sender sender, boolean updateStatus) {
        TranslationManager manager = this.plugin.getTranslationManager();
        Path translationsDirectory = manager.getTranslationsDirectory();

        try {
            MoreFiles.createDirectoriesIfNotExists(translationsDirectory);
        } catch (IOException e) {
            // ignore
        }

        for (LanguageInfo language : languages) {
            if (sender != null) {
                Message.TRANSLATIONS_INSTALLING_SPECIFIC.send(sender, language.locale().toString());
            }

            Request request = new Request.Builder()
                    .header("User-Agent", this.plugin.getBytebin().getUserAgent())
                    .url(TRANSLATIONS_DOWNLOAD_ENDPOINT + language.id())
                    .build();

            Path file = translationsDirectory.resolve(language.locale().toString() + ".properties");

            try (Response response = this.plugin.getBytebin().makeHttpRequest(request)) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        throw new IOException("No response");
                    }

                    try (InputStream inputStream = new LimitedInputStream(responseBody.byteStream(), MAX_BUNDLE_SIZE)) {
                        Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (UnsuccessfulRequestException | IOException e) {
                if (sender != null) {
                    Message.TRANSLATIONS_DOWNLOAD_ERROR.send(sender, language.locale().toString());
                    this.plugin.getLogger().warn("Unable to download translations", e);
                }
            }
        }

        if (updateStatus) {
            // update status file
            Path repoStatusFile = translationsDirectory.resolve("repository.json");
            try (BufferedWriter writer = Files.newBufferedWriter(repoStatusFile, StandardCharsets.UTF_8)) {
                JsonObject status = new JsonObject();
                status.add("lastRefresh", new JsonPrimitive(System.currentTimeMillis()));
                GsonProvider.prettyPrinting().toJson(status, writer);
            } catch (IOException e) {
                // ignore
            }
        }

        this.plugin.getTranslationManager().reload();
    }

    private MetadataResponse getTranslationsMetadata() throws IOException, UnsuccessfulRequestException {
        Request request = new Request.Builder()
                .header("User-Agent", this.plugin.getBytebin().getUserAgent())
                .url(TRANSLATIONS_INFO_ENDPOINT)
                .build();

        JsonObject jsonResponse;
        try (Response response = this.plugin.getBytebin().makeHttpRequest(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = new LimitedInputStream(responseBody.byteStream(), MAX_BUNDLE_SIZE)) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        jsonResponse = GsonProvider.normal().fromJson(reader, JsonObject.class);
                    }
                }
            }
        }

        List<LanguageInfo> languages = new ArrayList<>();
        for (Map.Entry<String, JsonElement> language : jsonResponse.get("languages").getAsJsonObject().entrySet()) {
            languages.add(new LanguageInfo(language.getKey(), language.getValue().getAsJsonObject()));
        }
        languages.removeIf(language -> language.progress() <= 0);

        if (languages.size() >= 100) {
            // just a precaution: if more than 100 languages have been
            // returned then the metadata server is doing something silly
            throw new IOException("More than 100 languages - cancelling download");
        }

        long cacheMaxAge = jsonResponse.get("cacheMaxAge").getAsLong();

        return new MetadataResponse(cacheMaxAge, languages);
    }

    private static final class MetadataResponse {
        private final long cacheMaxAge;
        private final List<LanguageInfo> languages;

        MetadataResponse(long cacheMaxAge, List<LanguageInfo> languages) {
            this.cacheMaxAge = cacheMaxAge;
            this.languages = languages;
        }
    }

    public static final class LanguageInfo {
        private final String id;
        private final String name;
        private final Locale locale;
        private final int progress;
        private final List<String> contributors;

        LanguageInfo(String id, JsonObject data) {
            this.id = id;
            this.name = data.get("name").getAsString();
            this.locale = Objects.requireNonNull(TranslationManager.parseLocale(data.get("localeTag").getAsString()));
            this.progress = data.get("progress").getAsInt();
            this.contributors = new ArrayList<>();
            for (JsonElement contributor : data.get("contributors").getAsJsonArray()) {
                this.contributors.add(contributor.getAsJsonObject().get("name").getAsString());
            }
        }

        public String id() {
            return this.id;
        }

        public String name() {
            return this.name;
        }

        public Locale locale() {
            return this.locale;
        }

        public int progress() {
            return this.progress;
        }

        public List<String> contributors() {
            return this.contributors;
        }
    }

    private static final class LimitedInputStream extends FilterInputStream implements Closeable {
        private final long limit;
        private long count;

        public LimitedInputStream(InputStream inputStream, long limit) {
            super(inputStream);
            this.limit = limit;
        }

        private void checkLimit() throws IOException {
            if (this.count > this.limit) {
                throw new IOException("Limit exceeded");
            }
        }

        @Override
        public int read() throws IOException {
            int res = super.read();
            if (res != -1) {
                this.count++;
                checkLimit();
            }
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res = super.read(b, off, len);
            if (res > 0) {
                this.count += res;
                checkLimit();
            }
            return res;
        }
    }
}
