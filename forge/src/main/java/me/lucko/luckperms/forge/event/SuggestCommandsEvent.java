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

package me.lucko.luckperms.forge.event;

import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.eventbus.api.Event;

public class SuggestCommandsEvent extends Event {

    private final CommandSourceStack source;
    private RootCommandNode<CommandSourceStack> node;

    public SuggestCommandsEvent(CommandSourceStack source, RootCommandNode<CommandSourceStack> node) {
        this.source = source;
        this.node = node;
    }

    public CommandSourceStack getSource() {
        return source;
    }

    public RootCommandNode<CommandSourceStack> getNode() {
        return node;
    }

    public void setNode(RootCommandNode<CommandSourceStack> node) {
        this.node = node;
    }

}
