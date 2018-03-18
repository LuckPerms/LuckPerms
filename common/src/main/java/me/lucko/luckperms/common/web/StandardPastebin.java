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

package me.lucko.luckperms.common.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public enum StandardPastebin implements Pastebin {

    BYTEBIN {
        public static final String URL = "https://bytebin.lucko.me/";
        private static final String POST_URL = URL + "post";

        @Override
        public String getPostUrl() {
            return POST_URL;
        }

        @Override
        protected String parseIdFromResult(BufferedReader reader) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            return object.get("key").getAsString();
        }

        @Override
        public String getRawUrl(String id) {
            return URL + id;
        }
    },

    HASTEBIN {
        private static final String URL = "https://hastebin.com/";
        private static final String RAW_URL = URL + "raw/";
        private static final String POST_URL = URL + "documents";

        @Override
        public String getPostUrl() {
            return POST_URL;
        }

        @Override
        protected String parseIdFromResult(BufferedReader reader) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            return object.get("key").getAsString();
        }

        @Override
        public String getRawUrl(String id) {
            return RAW_URL + id;
        }
    };

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType PLAIN_TYPE = MediaType.parse("text/plain; charset=utf-8");

    protected abstract String getPostUrl();
    protected abstract String parseIdFromResult(BufferedReader reader);

    @Override
    public Pastebin.Paste postJson(JsonElement content, boolean compress) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        OutputStream outputStream;
        if (compress) {
            try {
                outputStream = new GZIPOutputStream(byteOut);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            outputStream = byteOut;
        }

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            GSON.toJson(content, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return post(RequestBody.create(JSON_TYPE, byteOut.toByteArray()), compress);
    }

    @Override
    public Pastebin.Paste postPlain(String content) {
        return post(RequestBody.create(PLAIN_TYPE, content), false);
    }

    private Pastebin.Paste post(RequestBody body, boolean compressed) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(getPostUrl())
                .post(body);

        if (compressed) {
            requestBuilder.header("Content-Encoding", "gzip");
        }

        Request request = requestBuilder.build();
        try (Response response = HttpClient.makeCall(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        String id = parseIdFromResult(reader);
                        String url = getRawUrl(id);
                        return new Paste(url, id);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class Paste implements Pastebin.Paste {
        private final String url;
        private final String id;

        private Paste(String url, String id) {
            this.url = url;
            this.id = id;
        }

        @Override
        public String url() {
            return this.url;
        }

        @Override
        public String id() {
            return this.id;
        }
    }
}
