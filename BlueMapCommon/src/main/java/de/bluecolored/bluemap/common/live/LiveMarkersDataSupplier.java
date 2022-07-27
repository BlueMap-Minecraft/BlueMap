package de.bluecolored.bluemap.common.live;

import de.bluecolored.bluemap.api.markers.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;

import java.util.Map;
import java.util.function.Supplier;

public class LiveMarkersDataSupplier implements Supplier<String> {

    private final Map<String, MarkerSet> markerSets;

    public LiveMarkersDataSupplier(Map<String, MarkerSet> markerSets) {
        this.markerSets = markerSets;
    }

    @Override
    public String get() {
        return MarkerGson.INSTANCE.toJson(markerSets);
    }

}
