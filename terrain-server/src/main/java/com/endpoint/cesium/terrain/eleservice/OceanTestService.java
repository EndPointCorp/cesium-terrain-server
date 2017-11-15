package com.endpoint.cesium.terrain.eleservice;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.endpoint.cesium.terrain.LonLat;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.index.strtree.STRtree;

public class OceanTestService {
	
	private static final Logger log = LoggerFactory.getLogger(OceanTestService.class);
	
	private STRtree index = new STRtree();
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	public OceanTestService(String shapePath) {
		
		log.info("Start building land index from {}", shapePath);
		
		try {
			File file = new File(shapePath);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("url", file.toURI().toURL());
			
			DataStore dataStore = DataStoreFinder.getDataStore(map);
			String typeName = dataStore.getTypeNames()[0];
			
			FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore
					.getFeatureSource(typeName);
			Filter filter = Filter.INCLUDE;
			
			int i = 0;
			FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
			try (FeatureIterator<SimpleFeature> features = collection.features()) {
				while (features.hasNext()) {
					SimpleFeature feature = features.next();
					Geometry geometry = (Geometry) feature.getDefaultGeometry();
					
					try {
						for (Geometry g : split(Collections.singletonList(geometry), 250)) {
							index.insert(g.getEnvelopeInternal(), g);
						}
					}
					catch (TopologyException e) {
						index.insert(geometry.getEnvelopeInternal(), geometry);
					}
					
					if(i % 25000 == 0) {
						log.info("{} features in index", i);
					}
					i++;
				}
				log.info("Building index");
				index.build();
			}
			
			log.info("Cache built");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isLand(LonLat ll) {
		@SuppressWarnings("unchecked")
		List<Geometry> query = index.query(new Envelope(new Coordinate(ll.getLon(), ll.getLat())));
		Point point = geometryFactory.createPoint(new Coordinate(ll.getLon(), ll.getLat()));

		for (Geometry g : query) {
			if(g.contains(point)) {
				return true;
			}
		}
		return false;
	}
	
	private Collection<Geometry> split(List<Geometry> geoms, int maxPoints) {
		Collection<Geometry> result = new ArrayList<>();
		
		for(Geometry g : geoms) {
			if (g.getNumPoints() < maxPoints) {
				result.add(g);
			}
			else {
				Collection<Geometry> split = split(g);
				for (Geometry s : split) {
					result.addAll(split(Collections.singletonList(s), maxPoints));
				}
			}
		}
		
		return result;
	}

	public static Collection<Geometry> split(Geometry g)
    {
		Envelope boundary = g.getEnvelopeInternal();
		
		double cx = boundary.centre().x;
		double cy = boundary.centre().y;
		
		Polygon p1 = getRectangle(boundary.getMinX(), boundary.getMinY(), cx, cy);
		Polygon p2 = getRectangle(boundary.getMaxX(), boundary.getMinY(), cx, cy);
		Polygon p3 = getRectangle(boundary.getMinX(), boundary.getMinY(), cx, cy);
		Polygon p4 = getRectangle(boundary.getMinX(), boundary.getMinY(), cx, cy);
		
		Collection<Geometry> res = new ArrayList<>();;
		
		res.add(g.intersection(p1));
		res.add(g.intersection(p2));
		res.add(g.intersection(p3));
		res.add(g.intersection(p4));
		
		return res; 
		
    }

	private static Polygon getRectangle(double x1, double y1, double x2, double y2) {
		
		double minx = Math.min(x1, x2);
		double maxx = Math.max(x1, x2);
		
		double miny = Math.min(y1, y2);
		double maxy = Math.max(y1, y2);
	
		
		return geometryFactory.createPolygon(new Coordinate[]{
			new Coordinate(minx, miny),
			new Coordinate(maxx, miny),
			new Coordinate(maxx, maxy),
			new Coordinate(minx, maxy),
			new Coordinate(minx, miny),
		});
	}
	
}
