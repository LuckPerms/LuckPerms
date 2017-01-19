/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.locale;

import lombok.Cleanup;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.constants.Message;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

public class SimpleLocaleManager implements LocaleManager {

    private Map<String, String> translations = null;

    @SuppressWarnings("unchecked")
    public void loadFromFile(File file) throws Exception {
        @Cleanup FileReader fileReader = new FileReader(file);
        translations = ImmutableMap.copyOf((Map<String, String>) new Yaml().load(fileReader));
    }

    @Override
    public int getSize() {
        return translations == null ? 0 : translations.size();
    }

    @Override
    public String getTranslation(Message key) {
        if (translations == null) {
            return null;
        }

        String k = key.name().toLowerCase().replace('_', '-');
        return translations.get(k);
    }

}
