package com.endpoint.cesium.terrain;

public class BBOX {
	
	private final double minx;
	private final double maxx;
	private final double miny;
	private final double maxy;
	
	public BBOX(double maxx, double minx, double maxy, double miny) {
		this.minx = minx;
		this.maxx = maxx;
		
		this.miny = miny;
		this.maxy = maxy;
	}

	public double getMinx() {
		return minx;
	}

	public double getMaxx() {
		return maxx;
	}

	public double getMaxy() {
		return maxy;
	}
	
	public double getMiny() {
		return miny;
	}

}
