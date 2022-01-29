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

package me.lucko.luckperms.common.webeditor;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains a store of known web editor sessions.
 */
public class WebEditorSessionStore {
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    /**
     * Adds a newly created session to the store.
     *
     * @param id the id of the session
     */
    public void addNewSession(String id) {
        this.sessions.put(id, SessionState.IN_PROGRESS);
    }

    /**
     * Gets the session state for the given session id.
     *
     * @param id the id of the session
     * @return the session state
     */
    public @NonNull SessionState getSessionState(String id) {
        return this.sessions.getOrDefault(id, SessionState.NOT_KNOWN);
    }

    /**
     * Marks a given session as complete.
     *
     * @param id the id of the session
     */
    public void markSessionCompleted(String id) {
        this.sessions.put(id, SessionState.COMPLETED);
    }

}
