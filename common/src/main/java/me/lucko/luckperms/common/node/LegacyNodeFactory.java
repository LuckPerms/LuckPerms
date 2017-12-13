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

package me.lucko.luckperms.common.node;

import lombok.experimental.UtilityClass;

import com.google.common.base.Splitter;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@UtilityClass
public class LegacyNodeFactory {

    /**
     * The characters which are delimited when serializing a permission string
     */
    static final String[] PERMISSION_DELIMITERS = new String[]{"/", "-", "$", "(", ")", "=", ","};

    /**
     * The characters which are delimited when serializing a server or world string
     */
    static final String[] SERVER_WORLD_DELIMITERS = new String[]{"/", "-"};

    /**
     * The characters which are delimited when serializing a context set
     */
    static final String[] CONTEXT_DELIMITERS = new String[]{"=", "(", ")", ","};

    /**
     * The characters which are delimited when serializing meta/prefix/suffix strings
     */
    private static final String[] GENERIC_DELIMITERS = new String[]{".", "/", "-", "$"};

    // legacy node format delimiters
    private static final Pattern LEGACY_SERVER_DELIM = PatternCache.compileDelimitedMatcher("/", "\\");
    private static final Splitter LEGACY_SERVER_SPLITTER = Splitter.on(LEGACY_SERVER_DELIM).limit(2);
    private static final Pattern LEGACY_WORLD_DELIM = PatternCache.compileDelimitedMatcher("-", "\\");
    private static final Splitter LEGACY_WORLD_SPLITTER = Splitter.on(LEGACY_WORLD_DELIM).limit(2);
    private static final Pattern LEGACY_EXPIRY_DELIM = PatternCache.compileDelimitedMatcher("$", "\\");
    private static final Splitter LEGACY_EXPIRY_SPLITTER = Splitter.on(LEGACY_EXPIRY_DELIM).limit(2);
    private static final Pattern LEGACY_CONTEXT_DELIM = PatternCache.compileDelimitedMatcher(")", "\\");
    private static final Splitter CONTEXT_SPLITTER = Splitter.on(LEGACY_CONTEXT_DELIM).limit(2);
    private static final Pattern LEGACY_CONTEXT_PAIR_DELIM = PatternCache.compileDelimitedMatcher(",", "\\");
    private static final Pattern LEGACY_CONTEXT_PAIR_PART_DELIM = PatternCache.compileDelimitedMatcher("=", "\\");
    private static final Splitter.MapSplitter LEGACY_CONTEXT_PART_SPLITTER = Splitter.on(LEGACY_CONTEXT_PAIR_DELIM)
            .withKeyValueSeparator(Splitter.on(LEGACY_CONTEXT_PAIR_PART_DELIM));

    public static Node fromLegacyString(String s, boolean b) {
        if (b) {
            return builderFromLegacyString(s, true).build();
        } else {
            return builderFromLegacyString(s, false).build();
        }
    }

    private static Node.Builder builderFromLegacyString(String s, boolean b) {
        // if contains /
        if (LEGACY_SERVER_DELIM.matcher(s).find()) {
            // 0=server(+world)   1=node
            Iterator<String> parts = LEGACY_SERVER_SPLITTER.split(s).iterator();
            String parts0 = parts.next();
            String parts1 = parts.next();

            // WORLD SPECIFIC
            // if parts[0] contains -
            if (LEGACY_WORLD_DELIM.matcher(parts0).find()) {
                // 0=server   1=world
                Iterator<String> serverParts = LEGACY_WORLD_SPLITTER.split(parts0).iterator();
                String serverParts0 = serverParts.next();
                String serverParts1 = serverParts.next();

                // if parts[1] contains $
                if (LEGACY_EXPIRY_DELIM.matcher(parts1).find()) {
                    // 0=node   1=expiry
                    Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(parts1).iterator();
                    String tempParts0 = tempParts.next();
                    String tempParts1 = tempParts.next();

                    return new LegacyNodeBuilder(tempParts0).setServer(serverParts0).setWorld(serverParts1).setExpiry(Long.parseLong(tempParts1)).setValue(b);
                } else {
                    return new LegacyNodeBuilder(parts1).setServer(serverParts0).setWorld(serverParts1).setValue(b);
                }
            } else {
                // SERVER BUT NOT WORLD SPECIFIC

                // if parts[1] contains $
                if (LEGACY_EXPIRY_DELIM.matcher(parts1).find()) {
                    // 0=node   1=expiry
                    Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(parts1).iterator();
                    String tempParts0 = tempParts.next();
                    String tempParts1 = tempParts.next();

                    return new LegacyNodeBuilder(tempParts0).setServer(parts0).setExpiry(Long.parseLong(tempParts1)).setValue(b);
                } else {
                    return new LegacyNodeBuilder(parts1).setServer(parts0).setValue(b);
                }
            }
        } else {
            // NOT SERVER SPECIFIC

            // if s contains $
            if (LEGACY_EXPIRY_DELIM.matcher(s).find()) {
                // 0=node   1=expiry
                Iterator<String> tempParts = LEGACY_EXPIRY_SPLITTER.split(s).iterator();
                String tempParts0 = tempParts.next();
                String tempParts1 = tempParts.next();

                return new LegacyNodeBuilder(tempParts0).setExpiry(Long.parseLong(tempParts1)).setValue(b);
            } else {
                return new LegacyNodeBuilder(s).setValue(b);
            }
        }
    }

    static String escapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        return escapeDelimiters(s, GENERIC_DELIMITERS);
    }

    static String unescapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        // super old hack - this format is no longer used for escaping,
        // but we'll keep supporting it when unescaping
        s = s.replace("{SEP}", ".");
        s = s.replace("{FSEP}", "/");
        s = s.replace("{DSEP}", "$");
        s = unescapeDelimiters(s, GENERIC_DELIMITERS);

        return s;
    }

    private static String escapeDelimiters(String s, String... delimiters) {
        if (s == null) {
            return null;
        }

        for (String d : delimiters) {
            s = s.replace(d, "\\" + d);
        }
        return s;
    }

    static String unescapeDelimiters(String s, String... delimiters) {
        if (s == null) {
            return null;
        }

        for (String d : delimiters) {
            s = s.replace("\\" + d, d);
        }
        return s;
    }

    private static final class LegacyNodeBuilder extends NodeBuilder {
        private static final Pattern NODE_CONTEXTS_PATTERN = Pattern.compile("\\(.+\\).*");

        LegacyNodeBuilder(String permission) {
            if (!NODE_CONTEXTS_PATTERN.matcher(permission).matches()) {
                this.permission = permission;
            } else {
                List<String> contextParts = CONTEXT_SPLITTER.splitToList(permission.substring(1));
                // 0 = context, 1 = node

                this.permission = contextParts.get(1);
                try {
                    Map<String, String> map = LEGACY_CONTEXT_PART_SPLITTER.split(contextParts.get(0));
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        this.withExtraContext(
                                unescapeDelimiters(e.getKey(), CONTEXT_DELIMITERS),
                                unescapeDelimiters(e.getValue(), CONTEXT_DELIMITERS)
                        );
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
