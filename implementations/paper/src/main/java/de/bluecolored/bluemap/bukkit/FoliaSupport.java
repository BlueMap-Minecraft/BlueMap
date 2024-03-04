package de.bluecolored.bluemap.bukkit;

import de.bluecolored.bluemap.core.logger.Logger;

public class FoliaSupport {

    public static final boolean IS_FOLIA = isFolia();

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            Logger.global.logInfo("Folia detected, enabling folia-support mode.");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
