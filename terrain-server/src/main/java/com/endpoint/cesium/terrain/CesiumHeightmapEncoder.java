package com.endpoint.cesium.terrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CesiumHeightmapEncoder {
	
	private static final Logger log = LoggerFactory.getLogger(CesiumHeightmapEncoder.class);

	// Each height is the number of 1/5 meter units above -1000 meters.
	
	public void encodeToByteBuffer(ByteBuffer bb, double[] eleSamples, boolean rezero) {

		// Base offset for elevation
		int base = -1000;
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		for (int y = 0; y < 65; y++) {
			for (int x = 0; x < 65; x++) {
				double ele = eleSamples[y * 65 + x];
				if(rezero) {
					ele = Math.max(ele, 0.0);
				}
				
				int value = new Double((ele - base) * 5.0).intValue();
				value = checkRange(value);

				// Java, where is my unsigned types!!!
				bb.putShort((short)value);
		    }
		}		
		
	}

	private int checkRange(int value) {
		if (value > 256 * 256 - 1 || value < 0) {
			log.info("Value {} is out of range", value);
		}
		
		return Math.max(0, Math.min(value, 256 * 256 -1));
	}

}
