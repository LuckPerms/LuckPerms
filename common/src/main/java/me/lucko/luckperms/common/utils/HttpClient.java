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

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * Utilities for the OkHttp client
 */
public class HttpClient {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .addInterceptor(new LuckPermsUserAgentInterceptor())
            .build();

    public static Response makeCall(Request request) throws IOException {
        Response response = CLIENT.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw exceptionForUnsuccessfulResponse(response);
        }
        return response;
    }

    public static RuntimeException exceptionForUnsuccessfulResponse(Response response) {
        String msg = "";
        try (ResponseBody responseBody = response.body()) {
            if (responseBody != null) {
                msg = responseBody.string();
            }
        } catch (IOException e) {
            // ignore
        }
        return new RuntimeException("Got response: " + response.code() + " - " + response.message() + " - " + msg);
    }

    private static final class LuckPermsUserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request orig = chain.request();
            Request modified = orig.newBuilder()
                    .header("User-Agent", "luckperms")
                    .build();

            return chain.proceed(modified);
        }
    }

    private HttpClient() {}

}
