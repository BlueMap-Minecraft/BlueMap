/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.sponge8;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SpongeCommands {

    private final CommandDispatcher<CommandCause> dispatcher;

    public SpongeCommands(final Plugin plugin) {
        this.dispatcher = new CommandDispatcher<>();

        // register commands
        new Commands<>(plugin, dispatcher, cause -> new SpongeCommandSource(plugin, cause.audience(), cause.subject()));
    }

    public Collection<SpongeCommandProxy> getRootCommands(){
        Collection<SpongeCommandProxy> rootCommands = new ArrayList<>();

        for (CommandNode<CommandCause> node : this.dispatcher.getRoot().getChildren()) {
            rootCommands.add(new SpongeCommandProxy(node.getName()));
        }

        return rootCommands;
    }

    public class SpongeCommandProxy implements Command.Raw {

        private final String label;

        protected SpongeCommandProxy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
            String command = label;
            if (!arguments.input().isEmpty()) {
                command += " " + arguments.input();
            }

            try {
                return CommandResult.builder().result(dispatcher.execute(command, cause)).build();
            } catch (CommandSyntaxException ex) {
                Component errText = Component.text(ex.getRawMessage().getString(), NamedTextColor.RED);

                String context = ex.getContext();
                if (context != null)
                    errText = errText.append(Component.newline()).append(Component.text(context, NamedTextColor.GRAY));

                return CommandResult.error(errText);
            }
        }

        @Override
        public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
            String command = label;
            if (!arguments.input().isEmpty()) {
                command += " " + arguments.input();
            }

            List<CommandCompletion> completions = new ArrayList<>();

            try {
                Suggestions suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(command, cause)).get(100, TimeUnit.MILLISECONDS);
                for (Suggestion suggestion : suggestions.getList()) {
                    String text = suggestion.getText();

                    if (text.indexOf(' ') == -1) {
                        Message tooltip = suggestion.getTooltip();
                        if (tooltip == null) {
                            completions.add(CommandCompletion.of(text));
                        } else {
                            completions.add(CommandCompletion.of(text, Component.text(tooltip.getString())));
                        }
                    }
                }
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException ignore) {}

            completions.sort(Comparator.comparing(CommandCompletion::completion));

            return completions;
        }

        @Override
        public boolean canExecute(CommandCause cause) {
            return true;
        }

        @Override
        public Optional<Component> shortDescription(CommandCause cause) {
            return Optional.empty();
        }

        @Override
        public Optional<Component> extendedDescription(CommandCause cause) {
            return Optional.empty();
        }

        @Override
        public Component usage(CommandCause cause) {
            CommandNode<CommandCause> node = dispatcher.getRoot().getChild(label);
            if (node == null) return Component.text("/" + label);

            List<Component> lines = new ArrayList<>();
            for (String usageString : dispatcher.getSmartUsage(node, cause).values()) {
                lines.add(Component.text("/" + label + " ", NamedTextColor.WHITE).append(Component.text(usageString, NamedTextColor.GRAY)));
            }

            return Component.join(Component.newline(), lines);
        }
    }

}
