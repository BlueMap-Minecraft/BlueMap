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
package de.bluecolored.bluemap.common.commands.arguments;

import de.bluecolored.bluecommands.CommandParseException;
import de.bluecolored.bluecommands.InputReader;
import de.bluecolored.bluecommands.SimpleSuggestion;
import de.bluecolored.bluecommands.Suggestion;
import de.bluecolored.bluecommands.parsers.SimpleArgumentParser;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MapBackedArgumentParser<T> extends SimpleArgumentParser<CommandSource, T> {

    private final String typeName;
    private final Supplier<Map<String, T>> mapSupplier;

    public MapBackedArgumentParser(String typeName, Map<String, T> map) {
        this(typeName, () -> map);
    }

    public MapBackedArgumentParser(String typeName, Supplier<Map<String, T>> mapSupplier) {
        super(true, false);
        this.typeName = typeName;
        this.mapSupplier = mapSupplier;
    }

    @Override
    public T parse(CommandSource context, String string) throws CommandParseException {
        T value = mapSupplier.get().get(string);
        if (value == null) throw new CommandParseException("There is no %s for '%s'".formatted(typeName, string));
        return value;
    }

    @Override
    public List<Suggestion> suggest(CommandSource context, InputReader input) {
        return mapSupplier.get().keySet().stream()
                .sorted()
                .<Suggestion> map(SimpleSuggestion::new)
                .toList();
    }

}
