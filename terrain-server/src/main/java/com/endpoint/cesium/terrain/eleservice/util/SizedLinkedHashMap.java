package com.endpoint.cesium.terrain.eleservice.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SizedLinkedHashMap extends LinkedHashMap<String, Boolean> {
	private static final long serialVersionUID = -1015841342092571477L;
	private int maxSize = 0;
	
	public SizedLinkedHashMap(int size) {
		this.maxSize = size;
	}
	
	protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
        return size() > this.maxSize;
    }
}