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
package de.bluecolored.bluemap.sponge;

import de.bluecolored.bluecommands.*;
import de.bluecolored.bluemap.common.commands.CommandExecutor;
import de.bluecolored.bluemap.common.commands.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.*;

import static de.bluecolored.bluemap.common.commands.TextFormat.NEGATIVE_COLOR;
import static net.kyori.adventure.text.Component.text;

public class SpongeCommands {

    private final Command<CommandSource, Object> commands;
    private final CommandExecutor commandExecutor;

    public SpongeCommands(final Plugin plugin) {
        this.commands = Commands.create(plugin);
        this.commandExecutor = new CommandExecutor(plugin);
    }

    public Collection<SpongeCommandProxy> getRootCommands(){
        return List.of(new SpongeCommandProxy(((LiteralCommand<?, ?>) commands).getLiteral()));
    }

    public class SpongeCommandProxy implements org.spongepowered.api.command.Command.Raw {

        private final String label;

        protected SpongeCommandProxy(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public CommandResult process(CommandCause cause, ArgumentReader.Mutable arguments) {
            String input = label;
            if (!arguments.input().isEmpty()) {
                input += " " + arguments.input();
            }

            SpongeCommandSource context = new SpongeCommandSource(cause.audience(), cause.subject());
            ParseResult<CommandSource, Object> result = commands.parse(context, input);
            CommandExecutor.ExecutionResult executionResult = commandExecutor.execute(result);

            if (executionResult.parseFailure()) {
                Optional<ParseFailure<CommandSource, Object>> failure = result.getFailures().stream()
                        .max(Comparator.comparing(ParseFailure::getPosition));

                if (failure.isPresent()) {
                    context.sendMessage(text(failure.get().getReason()).color(NEGATIVE_COLOR));
                } else {
                    context.sendMessage(text("Unknown command!").color(NEGATIVE_COLOR));
                }

                return CommandResult.builder()
                        .result(0)
                        .build();
            }

            return CommandResult.builder()
                    .result(executionResult.resultCode())
                    .build();
        }

        @Override
        public List<CommandCompletion> complete(CommandCause cause, ArgumentReader.Mutable arguments) {
            String input = label + " " + arguments.input();
            int position = input.lastIndexOf(' ') + 1;

            SpongeCommandSource context = new SpongeCommandSource(cause.audience(), cause.subject());
            ParseResult<CommandSource, Object> result = commands.parse(context, input);

            List<String> completions = new ArrayList<>();
            for (ParseFailure<?, ?> failure : result.getFailures()) {
                if (failure.getPosition() != position) continue;
                for (var suggestion : failure.getSuggestions()) {
                    completions.add(suggestion.getString());
                }
            }

            return completions.stream()
                    .sorted(String::compareToIgnoreCase)
                    .map(CommandCompletion::of)
                    .toList();
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
            return Component.empty();
        }
    }

}
