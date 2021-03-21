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

package me.lucko.luckperms.krypton

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import me.lucko.luckperms.common.config.generic.adapter.ConfigurationAdapter
import java.io.File

class KryptonConfigAdapter(private val plugin: LPKryptonPlugin, private val file: File) : ConfigurationAdapter {

    private var config = ConfigFactory.parseFile(file)

    override fun reload() {
        config = ConfigFactory.parseFile(file)
    }

    override fun getString(path: String, def: String?): String? = try {
        config.getString(path)
    } catch (exception: ConfigException) {
        def
    }

    override fun getInteger(path: String, def: Int): Int = try {
        config.getInt(path)
    } catch (exception: ConfigException) {
        def
    }

    override fun getBoolean(path: String, def: Boolean): Boolean = try {
        config.getBoolean(path)
    } catch (exception: ConfigException) {
        def
    }

    override fun getStringList(path: String, def: List<String>): List<String> = try {
        config.getStringList(path)
    } catch (exception: ConfigException) {
        def
    }

    override fun getKeys(path: String, def: List<String>): List<String> {
        val section = try {
            config.getObject(path)
        } catch (exception: ConfigException) {
            return def
        }

        return section.keys.toMutableList()
    }

    override fun getStringMap(path: String, def: Map<String, String>): Map<String, String> {
        val section = try {
            config.getObject(path)
        } catch (exception: ConfigException) {
            return def
        }

        return section.keys.associateWith { section[it].toString() }
    }

    override fun getPlugin() = plugin
}