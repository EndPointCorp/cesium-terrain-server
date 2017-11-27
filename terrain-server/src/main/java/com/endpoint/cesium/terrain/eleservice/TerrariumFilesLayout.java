package com.endpoint.cesium.terrain.eleservice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.endpoint.cesium.terrain.LonLat;

public class TerrariumFilesLayout implements FilesLayout {

	private static final String EXTENSION = ".png";
	private static final String baseURL = "http://elevation-tiles-prod.s3.amazonaws.com/terrarium/";
	private File baseFolder;

	public TerrariumFilesLayout(File baseFolder) {
		this.baseFolder = baseFolder;
	}

	@Override
	public File getFile(LonLat ll, int zoom) {
		TileNumber tileNumber = getTileNumber(ll.getLat(), ll.getLon(), zoom);
		File f = new File(baseFolder, zoom + "/" + tileNumber.x + "/" + tileNumber.y + EXTENSION);
		f.getParentFile().mkdirs();
		return f;
	}

	@Override
	public String getKey(LonLat ll, int zoom) {
		TileNumber tileNumber = getTileNumber(ll.getLat(), ll.getLon(), zoom);
		
		return zoom + "/" + tileNumber.x + "/" + tileNumber.y;
	}

	@Override
	public String getURL(LonLat ll, int zoom) {
		TileNumber tileNumber = getTileNumber(ll.getLat(), ll.getLon(), zoom);
		return baseURL + zoom + "/" + tileNumber.x + "/" + tileNumber.y + EXTENSION;
	}

	@Override
	public File[] listFiles() {
		List<File> files = new ArrayList<>();
		for(File z : baseFolder.listFiles()) {
			if (z.isDirectory()) {
				for(File x : z.listFiles()) {
					if (x.isDirectory()) {
						for(File y : x.listFiles()) {
							if (y.isFile() && y.getName().endsWith(EXTENSION)) {
								files.add(y);
							}
						}
					}
				}
			}
		}
		return files.toArray(new File[files.size()]);
	}

	@Override
	public String getKeyByFile(File f) {
		String y = StringUtils.split(f.getName(), '.')[0];
		String x = f.getParentFile().getName();
		String z = f.getParentFile().getParentFile().getName();
		return z + "/" + x + "/" + y;
	}
	
	private static final class TileNumber {
		int x;
		int y;
	}

	public static TileNumber getTileNumber(final double lat, final double lon, final int zoom) {
		int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
		int ytile = (int) Math
				.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
						* (1 << zoom));
		if (xtile < 0)
			xtile = 0;
		if (xtile >= (1 << zoom))
			xtile = ((1 << zoom) - 1);
		if (ytile < 0)
			ytile = 0;
		if (ytile >= (1 << zoom))
			ytile = ((1 << zoom) - 1);
		
		TileNumber tn = new TileNumber();
		tn.x = xtile;
		tn.y = ytile;
		
		return tn;
	}

}
