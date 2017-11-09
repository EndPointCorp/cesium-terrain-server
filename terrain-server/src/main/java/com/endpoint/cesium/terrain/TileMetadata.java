package com.endpoint.cesium.terrain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.endpoint.cesium.terrain.tiling.GeodeticTilingScema;

public class TileMetadata {
	private BBOX bbox;
	private List<String> requestedTileKeys;
	private int x;
	private int y;
	private int z;
	private double minele;
	private double maxele;

	public BBOX getBbox() {
		return bbox;
	}

	public void setBbox(BBOX bbox) {
		this.bbox = bbox;
	}

	public void setRequestedTileKeys(LinkedHashSet<String> keys) {
		this.requestedTileKeys = new ArrayList<>(keys);
	}

	public List<String> getRequestedTileKeys() {
		return requestedTileKeys;
	}

	public void setXYZ(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public String getTms() {
		return z + "/" + x + "/" + y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public void setMinMaxEle(double minele, double maxele) {
		this.minele = minele;
		this.maxele = maxele;
	}

	public double getMinele() {
		return minele;
	}

	public double getMaxele() {
		return maxele;
	}

	public String getParentTMS() {
		
		if(z == 0) {
			return getTms();
		}
		
		double cx = bbox.getMinx() + (bbox.getMaxx() - bbox.getMinx()) / 2;
		double cy = bbox.getMiny() + (bbox.getMaxy() - bbox.getMiny()) / 2;
		
		return GeodeticTilingScema.getTMS(new LonLat(cx, cy), z - 1);
	}
	
	public String[] getChildTMS() {
		double dx = bbox.getMaxx() - bbox.getMinx();
		double dy = bbox.getMaxy() - bbox.getMiny();

		double cx = bbox.getMinx() + dx / 2;
		double cy = bbox.getMiny() + dy / 2;
		
		// quarter x, y
		double qx = dx / 4;
		double qy = dy / 4;
		
		return new String[]{
				GeodeticTilingScema.getTMS(new LonLat(cx - qx, cy + qy), z + 1),
				GeodeticTilingScema.getTMS(new LonLat(cx + qx, cy + qy), z + 1),
				GeodeticTilingScema.getTMS(new LonLat(cx - qx, cy - qy), z + 1),
				GeodeticTilingScema.getTMS(new LonLat(cx + qx, cy - qy), z + 1),
		};
	}
	
}
