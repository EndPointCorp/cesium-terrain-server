package com.endpoint.cesium.terrain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.endpoint.cesium.terrain.eleservice.ElevationService;
import com.endpoint.cesium.terrain.eleservice.ElevationServiceTrace;
import com.endpoint.cesium.terrain.eleservice.TerrariumElevationService;
import com.endpoint.cesium.terrain.eleservice.OceanTestService;
import com.endpoint.cesium.terrain.tiling.GeodeticTilingScema;

public class TilesService {
	
	private static final ElevationService eleService = new TerrariumElevationService();

	public static final double VOID_VALUE = -1000.0;
	
	private static final OceanTestService waterService;
	static {
		String landSourcePath = TerrainServer.getConfig().getProperty("land.source");
		
		if (landSourcePath != null) {
			waterService = new OceanTestService(landSourcePath);
		}
		else {
			waterService = null;
		}
	}
	
	public Tile get(String format, int z, int x, int y) {
		
		BBOX bbox = GeodeticTilingScema.getBBOX(z, x, y);
		List<LonLat> samples = getLonLatSamples(bbox, 65, 65);
		
		double[] eleSamples = new double[65 * 65];
		int i = 0;
		
		for (LonLat sample : samples) {
			Double ele = eleService.getElevation(sample, z);
			if (ele == null) {
				ele = VOID_VALUE;
			}
			
			if (waterService != null) {
				if (ele < 0.0 && !waterService.isLand(sample)) {
					ele = 0.0;
				}
			}

			eleSamples[i++] = ele; 
		}
		
		Tile tile = new Tile(eleSamples, bbox);
		
		return tile;
	}
	
	public TileMetadata getMetadata(int z, int x, int y) {
		TileMetadata metadata = new TileMetadata();
		BBOX bbox = GeodeticTilingScema.getBBOX(z, x, y);
		metadata.setBbox(bbox);
		metadata.setXYZ(x, y, z);
		
		double minele = Double.MAX_VALUE;
		double maxele = Double.MIN_VALUE;
		
		LinkedHashSet<String> keys = new LinkedHashSet<>();
		
		for(LonLat sample : getLonLatSamples(bbox, 65, 65)) {
			ElevationServiceTrace trace = eleService.trace(sample, z);
			
			if (trace.elevation != null) {
				minele = Math.min(minele, trace.elevation);
				maxele = Math.max(maxele, trace.elevation);
			}
			
			keys.add(trace.key);
			
		}
		
		metadata.setMinMaxEle(minele, maxele);
		metadata.setRequestedTileKeys(keys);

		return metadata;
	}
	
	private List<LonLat> getLonLatSamples(BBOX bbox, int cols, int rows) {
		double minx = bbox.getMinx();
		double maxx = bbox.getMaxx();
		double miny = bbox.getMiny();
		double maxy = bbox.getMaxy();
		
		double w = maxx - minx;
		double h = maxy - miny;

		List<LonLat> result = new ArrayList<LonLat>(cols * rows);
		
		double dx = w / (cols - 1);
		double dy = h / (rows - 1);
		
		for(int r = 0; r < rows; r++) {
			
			// Take the center of the pixel
			double lat = maxy - (r * dy + dy / 2.0);
			
			for (int c = 0; c < cols; c++) {
				double lon = minx + (c * dx + dx / 2.0);
				result.add(new LonLat(lon, lat));
			}
		}
		
		return result;
	}

}
