package com.endpoint.cesium.terrain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import spark.Filter;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

public class TileController {
	
	private static final Logger log = LoggerFactory.getLogger(TileController.class);
	
	private static final String TYPE_JSON = "application/json";
	private static final String TYPE_PNG = "image/png";
	// private static final String TYPE_QUANTIZED_MESH = "application/vnd.quantized-mesh";
	private static final String TYPE_HEIGHTMAP = "application/octet-stream";
	
	private static final boolean GZIP = false;
	private static final boolean REZERO = true;
	private static final CesiumHeightmapEncoder hgtmapEncoder = new CesiumHeightmapEncoder();
	
	private static final Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_23);
	static {
		freeMarkerConfiguration.setDefaultEncoding("UTF-8");
		freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(TileController.class, "/"));
	}
	private static final FreeMarkerEngine freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);
	

	public TileController(final TilesService tiles) {
		
		Spark.before(new Filter() {

			@Override
			public void handle(Request request, Response response) throws Exception {
				response.header("Access-Control-Allow-Origin", "*");
			}
			
		});
		
		Spark.get("/terrain/heightmap/layer.json", new Route() {
			public Object handle(Request req, Response res) {
				try {
					res.type(TYPE_JSON);
					ServletOutputStream os = res.raw().getOutputStream();

					InputStream is = TileController.class.getResourceAsStream("/layer.json");
					IOUtils.copy(is, os);
					
					res.raw().flushBuffer();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		});
		
		Spark.get("/terrain/:format/:z/:x/:file", new Route() {
			
			public Object handle(Request req, Response res) {
				
				log.info("{}", req.pathInfo());
				
				String format = req.params().get(":format");
				int z = Integer.valueOf(req.params().get(":z"));
				int x = Integer.valueOf(req.params().get(":x"));
				String file = req.params().get(":file");
				String[] fileParts = StringUtils.split(file, '.');
				int y = Integer.valueOf(fileParts[0]);
				String extension = fileParts[1];
				
				if("html".equals(extension)) {
					TileMetadata metadata = tiles.getMetadata(z, x, y);
					res.status(200);
			        res.type("text/html");
					
			        return freeMarkerEngine.render(new ModelAndView(metadata, "tile.ftl"));
				}
				
				// Return empty file for first three levels
				if("heightmap".equals(format) && z < 5) {
					writeEmptyHeightmap(res);
					return null;
				}
				
				Tile tile = tiles.get(format, z, x, y);
				
				if("png".equals(extension)) {
					writePngResponse(res, tile);
					return null;
				}
				
				if("heightmap".equals(format)) {
					writeHeightmap(res, tile);
					return null;
				}
				
				return tile;
			}

		});
	}

	private void writeEmptyHeightmap(Response res) {
		try {
			res.type(TYPE_HEIGHTMAP);
			ServletOutputStream os = res.raw().getOutputStream();

			InputStream is = TileController.class.getResourceAsStream("/zero-tile.terrain");
			
			if (GZIP) {
				IOUtils.copy(is, new GZIPOutputStream(os));
			}
			else {
				IOUtils.copy(is, os);
			}
			
			res.raw().flushBuffer();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void writeHeightmap(Response res, Tile tile) {
		ByteBuffer bb = ByteBuffer.allocate(65 * 65 * 2 + 1 + 1);
		
		double[] eleSamples = tile.getEleSamples();
		hgtmapEncoder.encodeToByteBuffer(bb, eleSamples, REZERO);
		
		// Child mask
		bb.put((byte)getChildMask(eleSamples, REZERO));
		
		// Water mask, all land for now
		bb.put((byte)1);
		
		try {
			res.type(TYPE_HEIGHTMAP);
			ServletOutputStream os = res.raw().getOutputStream();
			
			if(GZIP) {
				GZIPOutputStream gzos = new GZIPOutputStream(os);
				gzos.write(bb.array());
				gzos.flush();
			}
			else {
				os.write(bb.array());
				os.flush();
			}
			
			res.raw().flushBuffer();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private byte getChildMask(double[] eleSamples, boolean rezero) {
		int i = 0;
		
		boolean voidNW = true, voidNE = true, voidSW = true, voidSE = true;
		
		for (double ele : eleSamples) {
			int c = i % 65;
			int r = i / 65;
			
			boolean voidEle = (ele <= TilesService.VOID_VALUE) || (rezero && ele < 0);

			if(!voidEle) {
				if (r < 32 && c < 32) {
					voidNW = false;
				}
				if (r < 32 && c >= 32) {
					voidNE = false;
				}
				if (r >= 32 && c < 32) {
					voidSW = false;
				}
				if (r >= 32 && c >= 32) {
					voidSE = false;
				}
			}
			i++;
		}

		int bitmask = 0;
		
		if (!voidSW) {
			bitmask = bitmask | 1;
		}
		if (!voidSE) {
			bitmask = bitmask | 2;
		}
		if(!voidNW) {
			bitmask = bitmask | 4;
		}
		if (!voidNE) {
			bitmask = bitmask | 8;
		}
		
		return (byte)bitmask;
	}


	private void writePngResponse(Response res, Tile tile) {
		BufferedImage img = asImage(tile);					
		try {
			res.type(TYPE_PNG);
			ServletOutputStream os = res.raw().getOutputStream();
			ImageIO.write(img, "png", os);
			res.raw().flushBuffer();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private BufferedImage asImage(Tile tile) {
		
		double minele = Double.MAX_VALUE;
		double maxele = - Double.MAX_VALUE;
		
		double[] eleSamples = tile.getEleSamples();
		
		for(double ele : eleSamples) {
			minele = Math.min(minele, ele);
			maxele = Math.max(maxele, ele);
		}
		
		BufferedImage img = new BufferedImage(65, 65, BufferedImage.TYPE_INT_ARGB);
		double scale = maxele / 256.0;
		for (int y = 0; y < img.getHeight(); y++) {
			for (int x = 0; x < img.getWidth(); x++) {
				double ele = eleSamples[y * 65 + x];

				// Take only positive numbers
				ele = Math.max(ele, 0.0);
				
				int v = new Double(ele / scale).intValue();
				v = Math.min(v, 255);
		        img.setRGB(x, y, new Color(v, v, v).getRGB());
		    }
		}
		return img;
	}
	
	public static void main(String[] args) {
		new TileController(new TilesService());
	}
	
}
