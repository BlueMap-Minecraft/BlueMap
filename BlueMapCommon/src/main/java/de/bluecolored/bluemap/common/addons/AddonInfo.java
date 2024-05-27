package de.bluecolored.bluemap.common.addons;

import lombok.Getter;

@Getter
public class AddonInfo {
    public static final String ADDON_INFO_FILE = "bluemap.addon.json";

    private String id;
    private String entrypoint;

}
