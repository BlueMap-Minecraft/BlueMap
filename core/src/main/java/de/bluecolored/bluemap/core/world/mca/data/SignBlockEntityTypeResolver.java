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