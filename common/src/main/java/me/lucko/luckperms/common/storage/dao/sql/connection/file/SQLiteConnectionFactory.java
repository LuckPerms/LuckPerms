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

package me.lucko.luckperms.common.storage.dao.sql.connection.file;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class SQLiteConnectionFactory extends FlatfileConnectionFactory {
    public SQLiteConnectionFactory(File file) {
        super("SQLite", file);

        // backwards compat
        File data = new File(file.getParent(), "luckperms.sqlite");
        if (data.exists()) {
            data.renameTo(new File(file.getParent(), "luckperms-sqlite.db"));
        }
    }

    @Override
    public Map<String, String> getMeta() {
        Map<String, String> ret = new LinkedHashMap<>();

        File databaseFile = new File(super.file.getParent(), "luckperms-sqlite.db");
        if (databaseFile.exists()) {
            double size = databaseFile.length() / 1048576D;
            ret.put("File Size", DF.format(size) + "MB");
        } else {
            ret.put("File Size", "0MB");
        }

        return ret;
    }

    @Override
    protected String getDriverClass() {
        return "org.sqlite.JDBC";
    }

    @Override
    protected String getDriverId() {
        return "jdbc:sqlite";
    }
}
