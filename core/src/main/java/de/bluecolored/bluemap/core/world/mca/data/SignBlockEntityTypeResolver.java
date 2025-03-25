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
package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.world.mca.blockentity.SignBlockEntity;
import de.bluecolored.bluenbt.TypeResolver;
import de.bluecolored.bluenbt.TypeToken;

import java.util.Collection;
import java.util.List;

public class SignBlockEntityTypeResolver implements TypeResolver<SignBlockEntity, SignBlockEntity> {

    private static final TypeToken<SignBlockEntity> BASE_TYPE_TOKEN = TypeToken.of(SignBlockEntity.class);
    private static final TypeToken<SignBlockEntity.LegacySignBlockEntity> LEGACY_TYPE_TOKEN = TypeToken.of(SignBlockEntity.LegacySignBlockEntity.class);

    private static final Collection<TypeToken<? extends SignBlockEntity>> POSSIBLE_TYPES = List.of(
            BASE_TYPE_TOKEN,
            LEGACY_TYPE_TOKEN
    );

    @Override
    public TypeToken<SignBlockEntity> getBaseType() {
        return BASE_TYPE_TOKEN;
    }

    @Override
    public TypeToken<? extends SignBlockEntity> resolve(SignBlockEntity base) {
        if (base.getFrontText() == null) return LEGACY_TYPE_TOKEN;
        return BASE_TYPE_TOKEN;
    }

    @Override
    public Iterable<TypeToken<? extends SignBlockEntity>> getPossibleTypes() {
        return POSSIBLE_TYPES;
    }

}