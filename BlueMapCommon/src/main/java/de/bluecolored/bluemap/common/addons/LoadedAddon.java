package de.bluecolored.bluemap.common.addons;

public record LoadedAddon (
        AddonInfo addonInfo,
        ClassLoader classLoader,
        Object instance
) {}
