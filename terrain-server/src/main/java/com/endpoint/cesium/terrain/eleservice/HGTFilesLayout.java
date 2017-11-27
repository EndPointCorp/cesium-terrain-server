package com.endpoint.cesium.terrain.eleservice;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.commons.lang3.StringUtils;

import com.endpoint.cesium.terrain.LonLat;

public class HGTFilesLayout implements FilesLayout {

	private static final String hgtBaseURL = "http://elevation-tiles-prod.s3.amazonaws.com/skadi/";
	private static final String HGT_FILE_SUFFIX = ".hgt.gz";
	private File baseFolder;
	
	public HGTFilesLayout(File baseFolder) {
		this.baseFolder = baseFolder;
	}

	@Override
	public File getFile(LonLat ll, int zoom) {
		return new File(baseFolder, this.getKey(ll, zoom) + HGT_FILE_SUFFIX);
	}

	@Override
	public String getKey(LonLat ll, int zoom) {
		String lonKey = getLonKey(ll);
		String latKey = getLatKey(ll);
		
		return latKey + lonKey;
	}

	private static String getLonKey(LonLat ll) {
    	int lon = (int) ll.getLon();

        String lonPref = "E";
        if (lon < 0) {
            lonPref = "W";
        }

        return String.format("%s%03d", lonPref, lon);
    }
    
    private static String getLatKey(LonLat ll) {
        int lat = (int) ll.getLat();

        String latPref = "N";
        if (lat < 0) latPref = "S";

        return String.format("%s%02d", latPref, lat);
    }

	/* (non-Javadoc)
	 * @see com.endpoint.cesium.terrain.hgt.FilesLayout#getURL(com.endpoint.cesium.terrain.LonLat, int)
	 */
	@Override
	public String getURL(LonLat ll, int zoom) {
		return hgtBaseURL + getLatKey(ll) + "/" + this.getKey(ll, zoom) + HGT_FILE_SUFFIX;
	}

	/* (non-Javadoc)
	 * @see com.endpoint.cesium.terrain.hgt.FilesLayout#listFiles()
	 */
	@Override
	public File[] listFiles() {
		return baseFolder.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(HGT_FILE_SUFFIX);
			}
			
		});
	}

	/* (non-Javadoc)
	 * @see com.endpoint.cesium.terrain.hgt.FilesLayout#getKeyByFile(java.io.File)
	 */
	@Override
	public String getKeyByFile(File f) {
		return StringUtils.split(f.getName(), '.')[0];
	}
	
}
