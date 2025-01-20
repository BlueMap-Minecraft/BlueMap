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

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.mca.entity.EntityType;
import de.bluecolored.bluemap.core.world.mca.entity.MCAEntity;
import de.bluecolored.bluenbt.TypeResolver;
import de.bluecolored.bluenbt.TypeToken;

import java.io.IOException;
import java.util.stream.Stream;

public class EntityTypeResolver implements TypeResolver<Entity, MCAEntity> {

    private static final TypeToken<MCAEntity> TYPE_TOKEN = TypeToken.of(MCAEntity.class);

    @Override
    public TypeToken<MCAEntity> getBaseType() {
        return TYPE_TOKEN;
    }

    @Override
    public TypeToken<? extends Entity> resolve(MCAEntity base) {
        EntityType type = EntityType.REGISTRY.get(base.getId());
        if (type == null) return TYPE_TOKEN;
        return TypeToken.of(type.getEntityClass());
    }

    @Override
    public Iterable<TypeToken<? extends Entity>> getPossibleTypes() {
        return Stream.concat(
                Stream.of(TYPE_TOKEN),
                EntityType.REGISTRY.values().stream()
                        .map(EntityType::getEntityClass)
                        .<TypeToken<? extends Entity>> map(TypeToken::of)
                )
                .toList();
    }

    @Override
    public Entity onException(IOException parseException, MCAEntity base) {
        Logger.global.logDebug("Failed to parse block-entity of type '%s': %s"
                .formatted(base.getId(), parseException));
        return base;
    }

}
