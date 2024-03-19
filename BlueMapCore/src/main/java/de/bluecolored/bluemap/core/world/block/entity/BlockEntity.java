package de.bluecolored.bluemap.core.world.block.entity;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTDeserializer;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

@NBTDeserializer(BlockEntity.BlockEntityDeserializer.class)
public class BlockEntity {
    private static final BlueNBT BLUENBT = new BlueNBT();

    @FunctionalInterface
    private interface BlockEntityInitializer {
        BlockEntity create(Map<String, Object> data);
    }

    @SuppressWarnings("StaticInitializerReferencesSubClass")
    private static final Map<String, BlockEntityInitializer> ID_MAPPING = Map.of(
            "minecraft:sign", SignBlockEntity::new,
            "minecraft:skull", SkullBlockEntity::new,
            "minecraft:banner", BannerBlockEntity::new
    );

    protected final String id;
    protected final int x, y, z;
    protected final boolean keepPacked;

    protected BlockEntity(Map<String, Object> data) {
        this.id = (String) data.get("id");
        this.x = (int) data.get("x");
        this.y = (int) data.get("y");
        this.z = (int) data.get("z");
        this.keepPacked = (byte) data.getOrDefault("keepPacked", (byte) 0) == 1;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isKeepPacked() {
        return keepPacked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockEntity that = (BlockEntity) o;
        return x == that.x && y == that.y && z == that.z && keepPacked == that.keepPacked && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, z, keepPacked);
    }

    @Override
    public String toString() {
        return "BlockEntity{" +
                "id='" + id + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", keepPacked=" + keepPacked +
                '}';
    }

    public static class BlockEntityDeserializer implements TypeDeserializer<BlockEntity> {
        @Override
        public BlockEntity read(NBTReader reader) throws IOException {
            @SuppressWarnings("unchecked") Map<String, Object> data =
                    (Map<String, Object>) BLUENBT.read(reader, TypeToken.getParameterized(Map.class, String.class, Object.class));

            String id = (String) data.get("id");
            if (id == null || id.isBlank()) {
                return null;
            }

            BlockEntityInitializer instance = ID_MAPPING.getOrDefault(id, BlockEntity::new);

            try {
                return instance.create(data);
            } catch (Exception e) {
                Logger.global.logError("Failed to instantiate BlockEntity instance!", e);
            }

            return null;
        }
    }
}
