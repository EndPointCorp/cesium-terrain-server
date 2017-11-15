package com.endpoint.cesium.terrain.tiling;

import com.endpoint.cesium.terrain.BBOX;
import com.endpoint.cesium.terrain.LonLat;

public class GeodeticTilingScema {
	
	private static final int ZERO_LEVEL_X_TILES = 2;
	private static final int ZERO_LEVEL_Y_TILES = 1;
	
	private static final double WidthDeg = 360.0;
	private static final double HeightDeg = 180.0;
	
	public static BBOX getBBOX(int z, int x, int y) {
		int tilesXOnThisZ = xTiles(z);
		int tilesYOnThisZ = yTiles(z);
		
		double tileWidth = WidthDeg / tilesXOnThisZ;
		double tileHeight = HeightDeg / tilesYOnThisZ;
		
		double tileX = tileWidth * x - 180.0;
		double tileY = tileHeight * y  - 90.0;
		
		return new BBOX(tileX + tileWidth, tileX, tileY + tileHeight, tileY);
	}

	public static int yTiles(int z) {
		return ( 1 << z ) * ZERO_LEVEL_Y_TILES;
	}

	public static int xTiles(int z) {
		return ( 1 << z ) * ZERO_LEVEL_X_TILES;
	}
	
	public static String getTMS(LonLat ll, int z) {
		int tilesXOnThisZ = xTiles(z);
		int tilesYOnThisZ = yTiles(z);
		
		double tileWidth = WidthDeg / tilesXOnThisZ;
		double tileHeight = HeightDeg / tilesYOnThisZ;
		
		int x = new Double((ll.getLon() + 180.0) / tileWidth).intValue();
		int y = new Double((ll.getLat() + 90.0) / tileHeight).intValue();
		
		return z + "/" + x + "/" + y;
	}
}
