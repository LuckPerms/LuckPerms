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

package me.lucko.luckperms;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.sponge.LuckPermsService;
import me.lucko.luckperms.calculators.*;
import me.lucko.luckperms.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class SpongeCalculatorFactory implements CalculatorFactory {
    private final LPSpongePlugin plugin;

    @Override
    public PermissionCalculator build(Contexts contexts, User user, Map<String, Boolean> map) {
        List<PermissionProcessor> processors = new ArrayList<>(5);
        processors.add(new MapProcessor(map));
        if (plugin.getConfiguration().isApplyingWildcards()) {
            processors.add(new SpongeWildcardProcessor(map));
            processors.add(new WildcardProcessor(map));
        }
        if (plugin.getConfiguration().isApplyingRegex()) {
            processors.add(new RegexProcessor(map));
        }
        processors.add(new DefaultsProcessor(plugin.getService(), LuckPermsService.convertContexts(contexts.getContext())));

        return new PermissionCalculator(plugin, user.getName(), plugin.getConfiguration().isDebugPermissionChecks(), processors);
    }
}
