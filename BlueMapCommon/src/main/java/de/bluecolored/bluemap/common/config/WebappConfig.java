package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.api.debug.DebugDump;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;
import java.util.Optional;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@DebugDump
@ConfigSerializable
public class WebappConfig {

    private boolean enabled = true;
    private boolean updateSettingsFile = true;

    private Path webroot = Path.of("bluemap", "web");

    private boolean useCookies = true;

    private boolean enableFreeFlight = true;

    private String startLocation = null;

    private float resolutionDefault = 1;

    private int minZoomDistance = 5;
    private int maxZoomDistance = 100000;

    private int hiresSliderMax = 500;
    private int hiresSliderDefault = 200;
    private int hiresSliderMin = 50;

    private int lowresSliderMax = 10000;
    private int lowresSliderDefault = 2000;
    private int lowresSliderMin = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public Path getWebroot() {
        return webroot;
    }

    public boolean isUpdateSettingsFile() {
        return updateSettingsFile;
    }

    public boolean isUseCookies() {
        return useCookies;
    }

    public boolean isEnableFreeFlight() {
        return enableFreeFlight;
    }

    public Optional<String> getStartLocation() {
        return Optional.ofNullable(startLocation);
    }

    public float getResolutionDefault() {
        return resolutionDefault;
    }

    public int getMinZoomDistance() {
        return minZoomDistance;
    }

    public int getMaxZoomDistance() {
        return maxZoomDistance;
    }

    public int getHiresSliderMax() {
        return hiresSliderMax;
    }

    public int getHiresSliderDefault() {
        return hiresSliderDefault;
    }

    public int getHiresSliderMin() {
        return hiresSliderMin;
    }

    public int getLowresSliderMax() {
        return lowresSliderMax;
    }

    public int getLowresSliderDefault() {
        return lowresSliderDefault;
    }

    public int getLowresSliderMin() {
        return lowresSliderMin;
    }

}
