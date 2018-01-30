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

package me.lucko.luckperms.common.utils;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import okhttp3.FormBody;
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
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a posted GitHub Gist
 */
public class Gist {
    private static final Gson GSON = new Gson();

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String GIST_API_URL = "https://api.github.com/gists";
    private static final String GIT_IO_URL = "https://git.io";

    public static Builder builder() {
        return new Builder();
    }

    private final String url;
    private final String id;

    private Gist(String url, String id) {
        this.url = url;
        this.id = id;
    }

    public String getUrl() {
        return this.url;
    }

    public String getId() {
        return this.id;
    }

    private static final class GistFile {
        private final String name;
        private final String content;

        private GistFile(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    public static final class Builder {
        private final List<GistFile> files = new ArrayList<>();
        private boolean shorten = true;
        private String description = "LuckPerms Gist";

        private Builder() {

        }

        public Builder file(String name, String content) {
            this.files.add(new GistFile(name, content));
            return this;
        }

        public Builder shorten(boolean shorten) {
            this.shorten = shorten;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Gist upload() {
            return Gist.upload(ImmutableList.copyOf(this.files), this.shorten, this.description);
        }
    }

    private static Gist upload(List<GistFile> files, boolean shorten, String description) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonWriter jw = new JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            jw.beginObject();
            jw.name("description").value(description);
            jw.name("public").value(false);
            jw.name("files").beginObject();
            for (GistFile file : files) {
                jw.name(file.name).beginObject().name("content").value(file.content).endObject();
            }
            jw.endObject().endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(JSON_TYPE, out.toByteArray());
        Request request = new Request.Builder()
                .url(GIST_API_URL)
                .post(body)
                .build();

        try (Response response = HttpClient.makeCall(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                String id;
                String pasteUrl;
                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        JsonObject object = GSON.fromJson(reader, JsonObject.class);
                        id = object.get("id").getAsString();
                        pasteUrl = object.get("html_url").getAsString();
                    }
                }

                if (shorten) {
                    pasteUrl = shortenUrl(pasteUrl);
                }

                return new Gist(pasteUrl, id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String shortenUrl(String pasteUrl) {
        RequestBody requestBody = new FormBody.Builder()
                .add("url", pasteUrl)
                .build();

        Request request = new Request.Builder()
                .url(GIT_IO_URL)
                .post(requestBody)
                .build();

        try (Response response = HttpClient.makeCall(request)) {
            String location = response.header("Location");
            if (location == null) {
                throw new RuntimeException("No location header");
            }
            return location;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pasteUrl;
    }
}
