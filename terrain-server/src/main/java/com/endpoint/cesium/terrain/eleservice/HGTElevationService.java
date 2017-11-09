package com.endpoint.cesium.terrain.eleservice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import com.endpoint.cesium.terrain.LonLat;

public class HGTElevationService implements ElevationService {

	private static final int SECONDS_PER_MINUTE = 60;

	// alter these values for different SRTM resolutions
	public static final int HGT_RES = 3; // resolution in arc seconds
	public static final int HGT_ROW_LENGTH = 1201; // number of elevation values
												   // per line
	public static final int HGT_VOID = -32768; // magic number which indicates
											   // 'void data' in HGT file
	private FilesAccessService tiles = new FilesAccessService();

	@Override
	public Double getElevation(LonLat coor, int zoom) {

		ByteBuffer bb = tiles.read(coor, zoom);
		ShortBuffer sb = bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
		if (sb == null) {
			return null;
		}

		// see
		// http://gis.stackexchange.com/questions/43743/how-to-extract-elevation-from-hgt-file
		double fLat = frac(coor.getLat()) * SECONDS_PER_MINUTE;
		double fLon = frac(coor.getLon()) * SECONDS_PER_MINUTE;

		int row = (int) Math.round(fLat * SECONDS_PER_MINUTE / HGT_RES);
		int col = (int) Math.round(fLon * SECONDS_PER_MINUTE / HGT_RES);

		row = HGT_ROW_LENGTH - row;
		int cell = (HGT_ROW_LENGTH * (row - 1)) + col;

		if (cell < sb.limit()) {
			short ele = sb.get(cell);
			if (ele == HGT_VOID) {
				return null;
			} else {
				
				return (double)ele;
			}
		} else {
			return null;
		}

	}

	public static double frac(double d) {
		long iPart;
		double fPart;

		// Get user input
		iPart = (long) d;
		fPart = d - iPart;
		return fPart;
	}

	@Override
	public ElevationServiceTrace trace(LonLat ll, int z) {
		Double ele = getElevation(ll, z);
		return new ElevationServiceTrace(tiles.getKey(ll, z), ele);
	}
}
