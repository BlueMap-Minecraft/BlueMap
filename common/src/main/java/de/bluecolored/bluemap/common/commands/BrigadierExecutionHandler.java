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

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import de.bluecolored.bluecommands.ParseFailure;
import de.bluecolored.bluecommands.ParseResult;
import de.bluecolored.bluecommands.brigadier.CommandExecutionHandler;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;

import java.util.Comparator;

public class BrigadierExecutionHandler extends CommandExecutor implements CommandExecutionHandler<CommandSource, Object> {

    public BrigadierExecutionHandler(Plugin plugin) {
        super(plugin);
    }

    @Override
    public int handle(ParseResult<CommandSource, Object> parseResult) throws CommandSyntaxException {
        ExecutionResult executionResult = this.execute(parseResult);
        if (executionResult.parseFailure())
            return parseFailure(parseResult);
        return executionResult.resultCode();
    }

    private int parseFailure(ParseResult<CommandSource, Object> result) throws CommandSyntaxException {
        ParseFailure<CommandSource, Object> failure = result.getFailures().stream()
                .max(Comparator.comparing(ParseFailure::getPosition))
                .orElseThrow(IllegalAccessError::new);
        throw new CommandSyntaxException(
                new SimpleCommandExceptionType(failure::getReason),
                failure::getReason,
                result.getInput(),
                failure.getPosition()
        );
    }

}
