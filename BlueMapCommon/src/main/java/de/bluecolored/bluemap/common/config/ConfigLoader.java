package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;

import java.util.function.Supplier;

public interface ConfigLoader extends Keyed {

    ConfigLoader HOCON = new Impl(Key.bluemap("hocon"), ".conf", HoconConfigurationLoader::builder);
    ConfigLoader JSON = new Impl(Key.bluemap("json"), ".json", GsonConfigurationLoader::builder);

    ConfigLoader DEFAULT = HOCON;

    Registry<ConfigLoader> REGISTRY = new Registry<>(
            HOCON,
            JSON
    );

    String getFileSuffix();

    AbstractConfigurationLoader.Builder<?, ?> createLoaderBuilder();

    @RequiredArgsConstructor
    @Getter
    class Impl implements ConfigLoader {

        private final Key key;
        private final String fileSuffix;
        private final Supplier<AbstractConfigurationLoader.Builder<?, ?>> builderSupplier;

        @Override
        public AbstractConfigurationLoader.Builder<?, ?> createLoaderBuilder() {
            return builderSupplier.get();
        }

    }

}
