package com.endpoint.cesium.terrain.eleservice;

import java.io.File;

import com.endpoint.cesium.terrain.LonLat;

public interface FilesLayout {

	File getFile(LonLat ll, int zoom);

	String getKey(LonLat ll, int zoom);

	String getURL(LonLat ll, int zoom);

	File[] listFiles();

	String getKeyByFile(File f);

}