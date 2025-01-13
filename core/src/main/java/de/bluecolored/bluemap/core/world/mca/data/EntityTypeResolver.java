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
