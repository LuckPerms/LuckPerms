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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Uploads content to GitHub's GIST service.
 */
public class PasteUtils {
    private static final String GIST_API = "https://api.github.com/gists";
    private static final String SHORTEN_API = "https://git.io";

    /**
     * Uploads content to GIST, and returns a shortened URL.
     *
     * @param desc the description of the gist
     * @param files the files to include in the gist (file name --> content)
     * @return the url, or null
     */
    public static String paste(String desc, List<Map.Entry<String, String>> files) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(GIST_API).openConnection();
            connection.setRequestProperty("User-Agent", "luckperms");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                StringWriter sw = new StringWriter();

                JsonWriter jw = new JsonWriter(sw)
                        .beginObject()
                        .name("description").value(desc)
                        .name("public").value(false)
                        .name("files")
                        .beginObject();

                for (Map.Entry<String, String> file : files) {
                    jw.name(file.getKey()).beginObject().name("content").value(file.getValue()).endObject();
                }

                jw.endObject().endObject();
                os.write(sw.toString().getBytes(StandardCharsets.UTF_8));
            }

            if (connection.getResponseCode() >= 400) {
                throw new RuntimeException("Connection returned response code: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            }

            String pasteUrl;
            try (InputStream inputStream = connection.getInputStream()) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        JsonObject response = new Gson().fromJson(reader, JsonObject.class);
                        pasteUrl = response.get("html_url").getAsString();
                    }
                }
            }

            connection.disconnect();

            try {
                connection = (HttpURLConnection) new URL(SHORTEN_API).openConnection();
                connection.setRequestProperty("User-Agent", "luckperms");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(("url=" + pasteUrl).getBytes(StandardCharsets.UTF_8));
                }

                if (connection.getResponseCode() >= 400) {
                    new RuntimeException("Connection returned response code: " + connection.getResponseCode() + " - " + connection.getResponseMessage()).printStackTrace();
                } else {
                    String shortUrl = connection.getHeaderField("Location");
                    if (shortUrl != null) {
                        pasteUrl = shortUrl;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return pasteUrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
