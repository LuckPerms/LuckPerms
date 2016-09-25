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

package me.lucko.luckperms.api.sponge;

import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.calculators.PermissionCalculator;
import me.lucko.luckperms.calculators.PermissionProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextData {
    private final LuckPermsUserSubject parent;

    @Getter
    private final Map<String, String> context;

    @Getter
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    private final PermissionCalculator calculator;

    public ContextData(LuckPermsUserSubject parent, Map<String, String> context, LuckPermsService service) {
        this.parent = parent;
        this.context = context;

        List<PermissionProcessor> processors = new ArrayList<>(5);
        processors.add(new PermissionCalculator.MapProcessor(permissionCache));
        if (service.getPlugin().getConfiguration().isApplyingWildcards()) {
            processors.add(new LuckPermsUserSubject.SpongeWildcardProcessor(permissionCache));
            processors.add(new PermissionCalculator.WildcardProcessor(permissionCache));
        }
        if (service.getPlugin().getConfiguration().isApplyingRegex()) {
            processors.add(new PermissionCalculator.RegexProcessor(permissionCache));
        }
        processors.add(new LuckPermsUserSubject.SpongeDefaultsProcessor(service));

        calculator = new PermissionCalculator(service.getPlugin(), parent.getUser().getName(), service.getPlugin().getConfiguration().isDebugPermissionChecks(), processors);
    }

    public void invalidateCache() {
        calculator.invalidateCache();
    }

    public Tristate getPermissionValue(@NonNull String permission) {
        me.lucko.luckperms.api.Tristate t =  calculator.getPermissionValue(permission);
        if (t != me.lucko.luckperms.api.Tristate.UNDEFINED) {
            return Tristate.fromBoolean(t.asBoolean());
        } else {
            return Tristate.UNDEFINED;
        }
    }

}
