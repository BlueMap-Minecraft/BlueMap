package de.bluecolored.bluemap.core.world.mca.entity;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.world.Entity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface EntityType extends Keyed {

    Registry<EntityType> REGISTRY = new Registry<>();

    Class<? extends Entity> getEntityClass();

    @RequiredArgsConstructor
    @Getter
    class Impl implements EntityType {

        private final Key key;
        private final Class<? extends Entity> entityClass;

    }

}
