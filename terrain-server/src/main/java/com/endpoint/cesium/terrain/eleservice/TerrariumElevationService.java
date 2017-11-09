package com.endpoint.cesium.terrain.eleservice;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.endpoint.cesium.terrain.LonLat;

public class TerrariumElevationService implements ElevationService {
	
	private static final LRUMap<String, BufferedImage> imagesCache = new LRUMap<>(8);
	private static final Logger log = LoggerFactory.getLogger(TerrariumElevationService.class);

	private FilesAccessService filesDAO = new FilesAccessService();

	public Double getElevation(LonLat ll, int zoom) {

		BufferedImage img = getImage(ll, zoom);
		if (img != null) {
			return decodeElevation(ll, zoom, img);
		}

		return null;
	}

	private Double decodeElevation(LonLat ll, int zoom, BufferedImage img) {
		TileNumber tileNumber = getTileNumber(ll.getLat(), ll.getLon(), zoom);
		
		double tileWest = tile2lon(tileNumber.x, tileNumber.z);
		double tileEast = tile2lon(tileNumber.x + 1, tileNumber.z);
		double tileNorth = tile2lat(tileNumber.y, tileNumber.z);
		double tileSouth = tile2lat(tileNumber.y + 1, tileNumber.z);
		
		double pixelW = Math.abs(tileWest - tileEast) / 256;
		double pixelH = Math.abs(tileSouth - tileNorth) / 256;
		
		int x = (int) Math.floor(Math.abs(ll.getLon() - tileWest) / pixelW);
		int y = (int) Math.floor(Math.abs(ll.getLat() - tileNorth) / pixelH);
		
		if (x >= 256 || y >= 256) {
			log.error("({}, {}) is out of bitmap boundary. Lat = {}, Lon = {}, Zoom = {}", x, y, ll.getLat(), ll.getLon(), zoom); 
			return null;
		}
		int rgb = img.getRGB(x, y);
		
		int red = (rgb >> 16) & 0x000000FF;
		int green = (rgb >>8 ) & 0x000000FF;
		int blue = (rgb) & 0x000000FF;
		
		return (red * 256.0 + green + blue / 256.0) - 32768.0;
	}

	private BufferedImage getImage(LonLat ll, int zoom) {
		String imgKey = filesDAO.getKey(ll, zoom);
		BufferedImage img = null;
		
		synchronized (this) {
			img = imagesCache.get(imgKey);
		}
		
		if (img == null) {
			ByteBuffer bb = filesDAO.read(ll, zoom);
			try {
				img = ImageIO.read(new ByteArrayInputStream(bb.array()));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (img != null) {
			synchronized (this) {
				imagesCache.put(imgKey, img);
			}
		}
		
		return img;
	}

	static double tile2lon(int x, int z) {
		return x / Math.pow(2.0, z) * 360.0 - 180;
	}

	static double tile2lat(int y, int z) {
		double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
		return Math.toDegrees(Math.atan(Math.sinh(n)));
	}
	
	private static final class TileNumber {
		int x;
		int y;
		int z;
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
		tn.z = zoom;
		tn.x = xtile;
		tn.y = ytile;
		
		return tn;
	}
	
	@Override
	public ElevationServiceTrace trace(LonLat ll, int z) {
		TileNumber t = getTileNumber(ll.getLat(), ll.getLon(), z);
		Double ele = getElevation(ll, z);
		return new ElevationServiceTrace(z + "/" + t.x + "/" + t.y, ele);
	}

}
