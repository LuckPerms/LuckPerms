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

package me.lucko.luckperms.sponge.service.references;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import com.google.common.base.Splitter;

import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;

import org.spongepowered.api.service.permission.Subject;

import java.lang.ref.WeakReference;
import java.util.List;

@ToString(of = {"collection", "identifier"})
@EqualsAndHashCode(of = {"collection", "identifier"})
@RequiredArgsConstructor(staticName = "of")
public class SubjectReference {
    public static SubjectReference deserialize(String s) {
        List<String> parts = Splitter.on('/').limit(2).splitToList(s);
        return of(parts.get(0), parts.get(1));
    }

    public static SubjectReference of(Subject subject) {
        return of(subject.getContainingCollection().getIdentifier(), subject.getIdentifier());
    }

    @Getter
    private final String collection;

    @Getter
    private final String identifier;

    private WeakReference<LPSubject> ref = null;

    public synchronized LPSubject resolve(LuckPermsService service) {
        if (ref != null) {
            LPSubject s = ref.get();
            if (s != null) {
                return s;
            }
        }

        LPSubject s = service.getSubjects(collection).get(identifier);
        ref = new WeakReference<>(s);
        return s;
    }

}
