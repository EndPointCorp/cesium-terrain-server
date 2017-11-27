package com.endpoint.cesium.terrain;

public class LonLat {

	private final double lon;
	private final double lat;
	
	public LonLat(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}
	
}
