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

    /* The bytesocks urls */
    private final String httpUrl;
    private final String wsUrl;

    /** The client user agent */
    private final String userAgent;

    /**
     * Creates a new bytesocks instance
     *
     * @param host the bytesocks host
     * @param userAgent the client user agent string
     */
    public BytesocksClient(OkHttpClient okHttpClient, String host, boolean tls, String userAgent) {
        super(okHttpClient);

        this.httpUrl = (tls ? "https://" : "http://") + host + "/";
        this.wsUrl = (tls ? "wss://" : "ws://") + host + "/";
        this.userAgent = userAgent;
    }

    public Socket createSocket(WebSocketListener listener) throws IOException, UnsuccessfulRequestException {
        Request createRequest = new Request.Builder()
                .url(this.httpUrl + "create")
                .header("User-Agent", this.userAgent)
                .build();

        String id;
        try (Response response = makeHttpRequest(createRequest)) {
            if (response.code() != 201) {
                throw new UnsuccessfulRequestException(response);
            }

            id = Objects.requireNonNull(response.header("Location"));
        }

        Request socketRequest = new Request.Builder()
                .url(this.wsUrl + id)
                .header("User-Agent", this.userAgent)
                .build();

        return new Socket(id, this.okHttp.newWebSocket(socketRequest, listener));
    }

    public static final class Socket {
        private final String channelId;
        private final WebSocket socket;

        public Socket(String channelId, WebSocket socket) {
            this.channelId = channelId;
            this.socket = socket;
        }

        public String channelId() {
            return this.channelId;
        }

        public WebSocket socket() {
            return this.socket;
        }
    }

}
