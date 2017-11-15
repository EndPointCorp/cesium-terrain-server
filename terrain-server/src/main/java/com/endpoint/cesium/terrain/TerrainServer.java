package com.endpoint.cesium.terrain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.endpoint.cesium.terrain.tiling.GeodeticTilingScema;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.template.freemarker.FreeMarkerEngine;

public class TerrainServer {
	
	private static final Logger log = LoggerFactory.getLogger(TerrainServer.class);
	
	private static final String TYPE_JSON = "application/json";
	private static final String TYPE_PNG = "image/png";
	// private static final String TYPE_QUANTIZED_MESH = "application/vnd.quantized-mesh";
	private static final String TYPE_HEIGHTMAP = "application/octet-stream";
	
	private boolean GZIP = false;
	private boolean REZERO = false;
	
	private static final CesiumHeightmapEncoder hgtmapEncoder = new CesiumHeightmapEncoder();
	
	private static final Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_23);
	static {
		freeMarkerConfiguration.setDefaultEncoding("UTF-8");
		freeMarkerConfiguration.setTemplateLoader(new ClassTemplateLoader(TerrainServer.class, "/"));
	}
	private static final FreeMarkerEngine freeMarkerEngine = new FreeMarkerEngine(freeMarkerConfiguration);
	
	private static final Properties config = new Properties();
	
	public static Properties getConfig() {
		return TerrainServer.config;
	}
	
	public static void main(String[] args) {
		Options options = new Options();
		
		options.addOption("c", "cfg", true, "Properties file path.");
		
		Option fetchPath = Option.builder("f").longOpt("fetch")
			.hasArg().desc("Fetch tiles and save them by this path.").build();
		options.addOption(fetchPath);
		
		Option fetchTo = Option.builder("l").longOpt("fetch-to")
				.hasArg().desc("Fetch tiles up to this zoom level.").build();
		options.addOption(fetchTo);
		
		Option fetchFrom = Option.builder("L").longOpt("fetch-from")
				.hasArg().desc("Fetch tiles from this zoom level.").build();
		options.addOption(fetchFrom);
		
		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
	    try {
	        line = parser.parse( options, args );
	    }
	    catch( ParseException exp ) {
	        System.err.println( "Can't parse command line options: " + exp.getMessage() );
	    }

	    if (line != null) {
	    	String propPath = line.getOptionValue("cfg", "terrain-server.properties");
	    	try {
		    	File propFile = new File(propPath);
		    	if (propFile.exists()) {
		    		config.load(new FileInputStream(propFile));
		    	}
		    	else {
		    		config.load(TerrainServer.class.getResourceAsStream("/default.properties"));
		    	}
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
	    	
	    	new TerrainServer(new TilesService());
	    	
	    	if (line.hasOption("fetch")) {
	    		String fetchPathValue = line.getOptionValue("fetch");
	    		int fetchToValue = Integer.valueOf(line.getOptionValue("fetch-to", "16"));
	    		int fetchFromValue = Integer.valueOf(line.getOptionValue("fetch-from", "4"));
	    		
	    		fetchTiles(fetchPathValue, fetchToValue, fetchFromValue);
	    	}
	    }
	}

	public TerrainServer(final TilesService tiles) {
		
		boolean corsEnabled = Boolean.valueOf(getConfig().getProperty("cors.enabled", "true"));

		int port = Integer.parseInt(getConfig().getProperty("server.port", "4567"));
		Spark.setPort(port);
		
		this.GZIP = Boolean.parseBoolean(getConfig().getProperty("server.gzip", "true"));
		
		if (corsEnabled) {
			Spark.before(new CORSFilter());
		}
		
		Spark.get("/terrain/heightmap/layer.json", new Route() {
			public Object handle(Request req, Response res) {
				try {
					res.type(TYPE_JSON);
					ServletOutputStream os = res.raw().getOutputStream();

					InputStream is = TerrainServer.class.getResourceAsStream("/layer.json");
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
					if (z > 15) {
						res.status(404);
						return null;
					}
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

			InputStream is = TerrainServer.class.getResourceAsStream("/zero-tile.terrain");
			
			if (GZIP) {
				res.header("Content-Encoding", "gzip");
				
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
		byte childMask = getChildMask(eleSamples, REZERO);

		if(childMask == 0) {
			res.status(404);
			return;
		}
		
		hgtmapEncoder.encodeToByteBuffer(bb, eleSamples, REZERO);
		bb.put(childMask);
		
		// Water mask, all land for now
		bb.put((byte)1);
		
		try {
			res.type(TYPE_HEIGHTMAP);
			ServletOutputStream os = res.raw().getOutputStream();
			
			if(GZIP) {
				res.header("Content-Encoding", "gzip");
				
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

	private static void fetchTiles(String fetchPathValue, int fetchToValue, int fetchFromValue) {
		File tilesPath = new File(fetchPathValue);
		
		for (int z = fetchFromValue; z <= fetchToValue; z++) {
			for (int y = 0; y < GeodeticTilingScema.yTiles(z); y++) {
				for (int x = 0; x < GeodeticTilingScema.xTiles(z); x++) {
					String tmsFileName = z + "/" + x + "/" + y + ".terrain";
					File file = new File(tilesPath, tmsFileName);
					file.getParentFile().mkdirs();
					
					if (!file.exists()) {
						try {
							URL url = new URL("http://localhost:4567/terrain/heightmap/" + tmsFileName);
							ReadableByteChannel rbc = Channels.newChannel(url.openStream());
							FileOutputStream fos = new FileOutputStream(file);
							fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
							fos.close();
						}
						catch (FileNotFoundException fnf) {
							// Do nothing
						}
						catch (Exception e) {
							log.error("Failed to load {}", tmsFileName);
						}
					}
				}
			}
		}
		
		log.info("All done");
		System.exit(0);
	}
}
