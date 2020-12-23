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

package me.lucko.luckperms.common.http;

import com.google.gson.JsonElement;

import me.lucko.luckperms.common.util.gson.GsonProvider;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BytebinClient extends AbstractHttpClient {

    /** The bytebin URL */
    private final String url;
    /** The client user agent */
    private final String userAgent;

    /**
     * Creates a new bytebin instance
     *
     * @param url the bytebin url
     * @param userAgent the client user agent string
     */
    public BytebinClient(OkHttpClient okHttpClient, String url, String userAgent) {
        super(okHttpClient);
        if (url.endsWith("/")) {
            this.url = url;
        } else {
            this.url = url + "/";
        }
        this.userAgent = userAgent;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    @Override
    public Response makeHttpRequest(Request request) throws IOException, UnsuccessfulRequestException {
        return super.makeHttpRequest(request);
    }

    /**
     * POSTs GZIP compressed content to bytebin.
     *
     * @param buf the compressed content
     * @param contentType the type of the content
     * @return the key of the resultant content
     * @throws IOException if an error occurs
     */
    public Content postContent(byte[] buf, MediaType contentType) throws IOException, UnsuccessfulRequestException {
        RequestBody body = RequestBody.create(contentType, buf);

        Request.Builder requestBuilder = new Request.Builder()
                .url(this.url + "post")
                .header("User-Agent", this.userAgent)
                .header("Content-Encoding", "gzip");

        Request request = requestBuilder.post(body).build();
        try (Response response = makeHttpRequest(request)) {
            String key = response.header("Location");
            if (key == null) {
                throw new IllegalStateException("Key not returned");
            }
            return new Content(key);
        }
    }

    /**
     * GETs json content from bytebin
     *
     * @param id the id of the content
     * @return the data
     * @throws IOException if an error occurs
     */
    public JsonElement getJsonContent(String id) throws IOException, UnsuccessfulRequestException {
        Request request = new Request.Builder()
                .header("User-Agent", this.userAgent)
                .url(this.url + id)
                .build();

        try (Response response = makeHttpRequest(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        return GsonProvider.normal().fromJson(reader, JsonElement.class);
                    }
                }
            }
        }
    }

    public static final class Content {
        private final String key;

        Content(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

}
