package com.endpoint.cesium.terrain;

import spark.Filter;
import spark.Request;
import spark.Response;

public final class CORSFilter implements Filter {
	
	@Override
	public void handle(Request request, Response response) throws Exception {
		response.header("Access-Control-Allow-Origin", "*");
	}
	
}