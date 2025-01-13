package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.blockentity.BlockEntityType;
import de.bluecolored.bluemap.core.world.mca.blockentity.MCABlockEntity;
import de.bluecolored.bluenbt.TypeResolver;
import de.bluecolored.bluenbt.TypeToken;

import java.io.IOException;
import java.util.stream.Stream;

public class BlockEntityTypeResolver implements TypeResolver<BlockEntity, MCABlockEntity> {

    private static final TypeToken<MCABlockEntity> TYPE_TOKEN = TypeToken.of(MCABlockEntity.class);

    @Override
    public TypeToken<MCABlockEntity> getBaseType() {
        return TYPE_TOKEN;
    }

    @Override
    public TypeToken<? extends BlockEntity> resolve(MCABlockEntity base) {
        BlockEntityType type = BlockEntityType.REGISTRY.get(base.getId());
        if (type == null) return TYPE_TOKEN;
        return TypeToken.of(type.getBlockEntityClass());
    }

    @Override
    public Iterable<TypeToken<? extends BlockEntity>> getPossibleTypes() {
        return Stream.concat(
                Stream.of(TYPE_TOKEN),
                BlockEntityType.REGISTRY.values().stream()
                        .map(BlockEntityType::getBlockEntityClass)
                        .<TypeToken<? extends BlockEntity>> map(TypeToken::of)
                )
                .toList();
    }

    @Override
    public BlockEntity onException(IOException parseException, MCABlockEntity base) {
        Logger.global.logDebug("Failed to parse block-entity of type '%s': %s"
                .formatted(base.getId(), parseException));
        return base;
    }

}
