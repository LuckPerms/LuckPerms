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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

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
    public Response makeHttpRequest(Request request) throws IOException {
        return super.makeHttpRequest(request);
    }

    /**
     * POSTs GZIP compressed content to bytebin.
     *
     * @param buf the compressed content
     * @param contentType the type of the content
     * @param allowModification if the paste should be modifiable
     * @return the key of the resultant content
     * @throws IOException if an error occurs
     */
    public Content postContent(byte[] buf, MediaType contentType, boolean allowModification) throws IOException {
        RequestBody body = RequestBody.create(contentType, buf);

        Request.Builder requestBuilder = new Request.Builder()
                .url(this.url + "post")
                .header("User-Agent", this.userAgent)
                .header("Content-Encoding", "gzip");

        if (allowModification) {
            requestBuilder.header("Allow-Modification", "true");
        }

        Request request = requestBuilder.post(body).build();
        try (Response response = makeHttpRequest(request)) {
            String key = response.header("Location");
            if (key == null) {
                throw new IllegalStateException("Key not returned");
            }

            if (allowModification) {
                String modificationKey = response.header("Modification-Key");
                if (modificationKey == null) {
                    throw new IllegalStateException("Modification key not returned");
                }
                return new Content(key, modificationKey);
            } else {
                return new Content(key);
            }
        }
    }

    /**
     * PUTs modified GZIP compressed content to bytebin in place of existing content.
     *
     * @param existingContent the existing content
     * @param buf the compressed content to put
     * @param contentType the type of the content
     * @throws IOException if an error occurs
     */
    public void modifyContent(Content existingContent, byte[] buf, MediaType contentType) throws IOException {
        if (!existingContent.modifiable) {
            throw new IllegalArgumentException("Existing content is not modifiable");
        }

        RequestBody body = RequestBody.create(contentType, buf);

        Request.Builder requestBuilder = new Request.Builder()
                .url(this.url + existingContent.key())
                .header("User-Agent", this.userAgent)
                .header("Content-Encoding", "gzip")
                .header("Modification-Key", existingContent.modificationKey);

        Request request = requestBuilder.put(body).build();
        makeHttpRequest(request).close();
    }

    public static final class Content {
        private final String key;
        private final boolean modifiable;
        private final String modificationKey;

        Content(String key) {
            this.key = key;
            this.modifiable = false;
            this.modificationKey = null;
        }

        Content(String key, String modificationKey) {
            this.key = key;
            this.modifiable = true;
            this.modificationKey = modificationKey;
        }

        public String key() {
            return this.key;
        }
    }

}
