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

package net.luckperms.api.context;

/**
 * Some default context keys used by the plugin.
 */
public final class DefaultContextKeys {
    private DefaultContextKeys() {
        throw new AssertionError();
    }

    /**
     * The context key used to denote the name of the subjects server.
     */
    public static final String SERVER_KEY = "server";

    /**
     * The context key used to denote the name of the subjects world.
     */
    public static final String WORLD_KEY = "world";

    /**
     * The context key used to denote the dimension type of the subjects world.
     *
     * <p>Possible values: overworld, the_nether, the_end</p>
     *
     * @since 5.3
     */
    public static final String DIMENSION_TYPE_KEY = "dimension-type";

    /**
     * The context key used to denote the subjects gamemode.
     *
     * <p>Possible values: survival, creative, adventure, spectator</p>
     *
     * @since 5.3
     */
    public static final String GAMEMODE_KEY = "gamemode";

}
