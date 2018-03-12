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

/**
 * Represents a pastebin service
 */
public interface Pastebin {

    /**
     * Posts the given json to the pastebin
     *
     * @param element the json element to post
     * @param compress whether to compress and post the data using gzip
     * @return a paste
     */
    Paste postJson(JsonElement element, boolean compress);

    /**
     * Posts "plain" content to the pastebin
     *
     * @param content the content
     * @return a paste
     */
    Paste postPlain(String content);

    /**
     * Gets the raw url of a paste's data from an id
     *
     * @param id the id
     * @return a url
     */
    String getRawUrl(String id);

    /**
     * Encapsulates the properties of a specific "paste" entry
     */
    interface Paste {

        /**
         * Gets the url of the paste
         *
         * @return the url
         */
        String url();

        /**
         * Gets the unique id of the paste
         *
         * @return the id
         */
        String id();
    }

}
