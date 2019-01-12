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

import com.google.gson.JsonObject;

import me.lucko.luckperms.common.util.gson.GsonProvider;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;

public class Hastebin extends AbstractPastebin {
    public static final Hastebin INSTANCE = new Hastebin();

    private static final String URL = "https://hastebin.com/";
    private static final String RAW_URL = URL + "raw/";
    private static final String POST_URL = URL + "documents";

    private Hastebin() {

    }

    @Override
    protected String getPostUrl() {
        return POST_URL;
    }

    @Override
    protected String parseIdFromResult(Response response, ResponseBody responseBody, BufferedReader responseBodyReader) {
        JsonObject object = GsonProvider.prettyPrinting().fromJson(responseBodyReader, JsonObject.class);
        return object.get("key").getAsString();
    }

    @Override
    public String getPasteUrl(String id) {
        return RAW_URL + id;
    }
}
