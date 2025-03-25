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
package de.bluecolored.bluemap.common.commands;

import de.bluecolored.bluecommands.ParseMatch;
import de.bluecolored.bluecommands.ParseResult;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.ComponentLike;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static de.bluecolored.bluemap.common.commands.TextFormat.NEGATIVE_COLOR;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class CommandExecutor {

    private final Plugin plugin;

    public ExecutionResult execute(ParseResult<CommandSource, Object> parseResult) {
        if (parseResult.getMatches().isEmpty()) {

            // check if the plugin is not loaded first
            if (!Commands.checkPluginLoaded(plugin, parseResult.getContext()))
                return new ExecutionResult(0, false);

            return new ExecutionResult(0, true);
        }

        ParseMatch<CommandSource, Object> match = parseResult.getMatches().stream()
                .max(Comparator.comparing(ParseMatch::getPriority))
                .orElseThrow(IllegalStateException::new);

        if (!Commands.checkExecutablePreconditions(plugin, match.getContext(), match.getExecutable()))
            return new ExecutionResult(0, false);

        return CompletableFuture.supplyAsync(match::execute, BlueMap.THREAD_POOL)
                .thenApply(result -> switch (result) {
                    case Number n -> n.intValue();
                    case ComponentLike c -> {
                        match.getContext().sendMessage(c.asComponent());
                        yield 1;
                    }
                    case Boolean b -> b ? 1 : 0;
                    case null, default -> 1;
                })
                .exceptionally(e -> {
                    Logger.global.logError("Command execution for '%s' failed".formatted(parseResult.getInput()), e);
                    parseResult.getContext().sendMessage(text("There was an error executing this command! See logs or console for details.")
                            .color(NEGATIVE_COLOR));
                    return 0;
                })
                .completeOnTimeout(1, 100, TimeUnit.MILLISECONDS)
                .thenApply(code -> new ExecutionResult(code, false))
                .join();
    }

    public record ExecutionResult (
        int resultCode,
        boolean parseFailure
    ) {}

}
