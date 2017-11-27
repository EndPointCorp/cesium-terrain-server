package com.endpoint.cesium.terrain.eleservice;

import com.endpoint.cesium.terrain.LonLat;

public interface ElevationService {

	public Double getElevation(LonLat coor, int zoom);

	public ElevationServiceTrace trace(LonLat sample, int z);

}