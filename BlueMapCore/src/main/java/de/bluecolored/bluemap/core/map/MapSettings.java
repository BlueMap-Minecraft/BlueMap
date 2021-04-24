package de.bluecolored.bluemap.core.map;

import de.bluecolored.bluemap.core.map.hires.RenderSettings;

public interface MapSettings extends RenderSettings {

	int getHiresTileSize();

	int getLowresPointsPerLowresTile();

	int getLowresPointsPerHiresTile();

}
