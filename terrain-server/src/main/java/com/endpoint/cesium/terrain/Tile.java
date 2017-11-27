package com.endpoint.cesium.terrain;

public class Tile {

	private double[] eleSamples;
	private BBOX bbox;

	public Tile(double[] eleSamples, BBOX bbox) {
		this.eleSamples = eleSamples;
		this.bbox = bbox;
	}

	public double[] getEleSamples() {
		return eleSamples;
	}

	public BBOX getBbox() {
		return bbox;
	}

	
	
}
