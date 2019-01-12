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

import com.google.gson.JsonElement;

import me.lucko.luckperms.common.util.gson.GsonProvider;

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

/**
 * Represents a pastebin service
 */
public abstract class AbstractPastebin {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType PLAIN_TYPE = MediaType.parse("text/plain; charset=utf-8");

    /**
     * Gets the URL that post requests should be made to.
     *
     * @return the post URL
     */
    protected abstract String getPostUrl();

    /**
     * Gets the id of the resultant post from the response of an upload request
     *
     * @param response the response
     * @param responseBody the response body
     * @param responseBodyReader the response body content
     * @return
     */
    protected abstract String parseIdFromResult(Response response, ResponseBody responseBody, BufferedReader responseBodyReader);

    /**
     * Gets the raw url of a paste's data from an id
     *
     * @param id the id
     * @return a url
     */
    public abstract String getPasteUrl(String id);

    /**
     * Posts the given json to the pastebin
     *
     * @param content the json element to post
     * @param compress whether to compress and post the data using gzip
     * @return a paste
     */
    public Paste postJson(JsonElement content, boolean compress) {
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
            GsonProvider.prettyPrinting().toJson(content, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return post(RequestBody.create(JSON_TYPE, byteOut.toByteArray()), compress);
    }

    /**
     * Posts "plain" content to the pastebin
     *
     * @param content the content
     * @return a paste
     */
    public Paste postPlain(String content) {
        return post(RequestBody.create(PLAIN_TYPE, content), false);
    }

    private Paste post(RequestBody body, boolean compressed) {
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
                        String id = parseIdFromResult(response, responseBody, reader);
                        String url = getPasteUrl(id);
                        return new Paste(url, id);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encapsulates the properties of a specific "paste" entry
     */
    public static final class Paste {
        private final String url;
        private final String id;

        Paste(String url, String id) {
            this.url = url;
            this.id = id;
        }

        /**
         * Gets the url of the paste
         *
         * @return the url
         */
        public String url() {
            return this.url;
        }

        /**
         * Gets the unique id of the paste
         *
         * @return the id
         */
        public String id() {
            return this.id;
        }
    }

}
