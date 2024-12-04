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

package me.lucko.luckperms.common.storage.implementation.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class SchemaReader {
    private SchemaReader() {}

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("^CREATE TABLE [`\"']([^`\"']+)[`\"'].*");
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("^CREATE INDEX.* ON [`\"']([^`\"']+)[`\"'].*");

    /**
     * Parses a schema file to a list of SQL statements
     *
     * @param is the input stream to read from
     * @return a list of statements
     * @throws IOException if an error occurs whilst reading the file
     */
    public static List<String> getStatements(InputStream is) throws IOException {
        List<String> queries = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--") || line.startsWith("#")) {
                    continue;
                }

                sb.append(line);

                // check for end of declaration
                if (line.endsWith(";")) {
                    sb.deleteCharAt(sb.length() - 1);

                    String result = sb.toString().trim().replaceAll(" +", " ");
                    if (!result.isEmpty()) {
                        queries.add(result);
                    }

                    // reset
                    sb = new StringBuilder();
                }
            }
        }

        return queries;
    }

    public static String tableFromStatement(String statement) {
        Matcher table = CREATE_TABLE_PATTERN.matcher(statement);
        if (table.matches()) {
            return table.group(1).toLowerCase(Locale.ROOT);
        }
        Matcher index = CREATE_INDEX_PATTERN.matcher(statement);
        if (index.matches()) {
            return index.group(1).toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException("Unknown statement type: " + statement);
    }

    /**
     * Filters which statements should be executed based on the current list of tables in the database
     *
     * @param statements the statements to filter
     * @param currentTables the current tables in the database
     * @return the filtered list of statements
     */
    public static List<String> filterStatements(List<String> statements, List<String> currentTables) {
        return statements.stream()
                .filter(statement -> !currentTables.contains(tableFromStatement(statement)))
                .collect(Collectors.toList());
    }
}
