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

package me.lucko.luckperms.bukkit;

import lombok.AllArgsConstructor;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.bukkit.calculators.AttachmentProcessor;
import me.lucko.luckperms.bukkit.calculators.ChildProcessor;
import me.lucko.luckperms.bukkit.calculators.DefaultsProcessor;
import me.lucko.luckperms.bukkit.inject.Injector;
import me.lucko.luckperms.bukkit.model.LPPermissible;
import me.lucko.luckperms.common.calculators.AbstractCalculatorFactory;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionProcessor;
import me.lucko.luckperms.common.calculators.processors.MapProcessor;
import me.lucko.luckperms.common.calculators.processors.RegexProcessor;
import me.lucko.luckperms.common.calculators.processors.WildcardProcessor;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.core.model.User;

import java.util.UUID;

@AllArgsConstructor
public class BukkitCalculatorFactory extends AbstractCalculatorFactory {
    private final LPBukkitPlugin plugin;

    @Override
    public PermissionCalculator build(Contexts contexts, User user) {
        UUID uuid = plugin.getUuidCache().getExternalUUID(user.getUuid());

        ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
        processors.add(new MapProcessor());
        processors.add(new ChildProcessor(plugin.getChildPermissionProvider()));
        processors.add(new AttachmentProcessor(() -> {
            LPPermissible permissible = Injector.getPermissible(uuid);
            return permissible == null ? null : permissible.getAttachmentPermissions();
        }));
        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_WILDCARDS)) {
            processors.add(new WildcardProcessor());
        }
        if (plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) {
            processors.add(new RegexProcessor());
        }
        processors.add(new DefaultsProcessor(contexts.isOp(), plugin.getDefaultsProvider()));

        return registerCalculator(new PermissionCalculator(plugin, user.getName(), processors.build()));
    }
}
