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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.IOException;
import java.util.Objects;

public class BytesocksClient extends AbstractHttpClient {

    /** The bytesocks URL */
    private final String url;
    /** The client user agent */
    private final String userAgent;

    /**
     * Creates a new bytebin instance
     *
     * @param url the bytebin url
     * @param userAgent the client user agent string
     */
    public BytesocksClient(OkHttpClient okHttpClient, String url, String userAgent) {
        super(okHttpClient);
        if (url.endsWith("/")) {
            this.url = url;
        } else {
            this.url = url + "/";
        }
        this.userAgent = userAgent;
    }

    public WebSocket createSocket(WebSocketListener listener) throws IOException, UnsuccessfulRequestException {
        Request createRequest = new Request.Builder()
                .url(this.url + "create")
                .header("User-Agent", this.userAgent)
                .build();

        String webSocketUrl;
        try (Response response = makeHttpRequest(createRequest)) {
            if (response.code() != 201) {
                throw new UnsuccessfulRequestException(response);
            }

            webSocketUrl = Objects.requireNonNull(response.header("Location"));
        }

        Request socketRequest = new Request.Builder()
                .url(webSocketUrl)
                .header("User-Agent", this.userAgent)
                .build();

        return this.okHttp.newWebSocket(socketRequest, listener);
    }

}
