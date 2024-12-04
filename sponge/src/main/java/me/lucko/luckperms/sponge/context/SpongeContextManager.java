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

package me.lucko.luckperms.sponge.context;

import me.lucko.luckperms.common.context.manager.InlineContextManager;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.model.ContextCalculatorProxy;
import me.lucko.luckperms.sponge.service.model.TemporaryCauseHolderSubject;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.StaticContextCalculator;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.service.permission.Subject;

import java.util.UUID;

public class SpongeContextManager extends InlineContextManager<Subject, ServerPlayer> {

    public SpongeContextManager(LPSpongePlugin plugin) {
        super(plugin, Subject.class, ServerPlayer.class);
    }

    @Override
    protected void callContextCalculator(ContextCalculator<? super Subject> calculator, Subject subject, ContextConsumer consumer) {
        if (subject instanceof TemporaryCauseHolderSubject) {
            Cause cause = ((TemporaryCauseHolderSubject) subject).getCause();
            Subject actualSubject = ((TemporaryCauseHolderSubject) subject).getSubject();

            if (calculator instanceof ContextCalculatorProxy) {
                ((ContextCalculatorProxy) calculator).calculate(cause, consumer);
            } else if (actualSubject != null) {
                calculator.calculate(actualSubject, consumer);
            } else if (calculator instanceof StaticContextCalculator) {
                ((StaticContextCalculator) calculator).calculate(consumer);
            } /* else {
                // we just have to fail...
                // there's no way to call a LuckPerms ContextCalculator if a Subject instance
                // doesn't exist for the cause.
            } */
        } else {
            Object associatedObject = subject.associatedObject().orElse(null);
            if (associatedObject instanceof Subject) {
                calculator.calculate((Subject) associatedObject, consumer);
            } else {
                calculator.calculate(subject, consumer);
            }
        }
    }

    @Override
    public UUID getUniqueId(ServerPlayer player) {
        return player.uniqueId();
    }
}
