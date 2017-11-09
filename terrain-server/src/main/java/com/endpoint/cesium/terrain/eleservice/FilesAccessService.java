package com.endpoint.cesium.terrain.eleservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.endpoint.cesium.terrain.LonLat;
import com.endpoint.cesium.terrain.eleservice.util.SizedLinkedHashMap;

public class FilesAccessService {
	
	private static final Logger log = LoggerFactory.getLogger(FilesAccessService.class);
	
	/* 1. look for mem cache
	 * 2. if not in cache - look for local file
	 * 3. if it's not in local files cache, download and store 
	 */
	public static int CACHE_SIZE = 32;
	
	// Internal in memory cache
	private static final LRUMap<String, ByteBuffer> cache = new LRUMap<String, ByteBuffer>(CACHE_SIZE);
	private static final Map<String, Future<ByteBuffer>> futures = new HashMap<>();
	
	private static final int CACHED_FILES = 65 * 65 * 4;
	
	//private static final HGTFilesLayout FILES_LAYOUT = new HGTFilesLayout(new File("/opt/ep/mapzen/hgt"));
	private static final FilesLayout FILES_LAYOUT = new TerrariumFilesLayout(new File("/opt/ep/mapzen/terrarium"));
	
	private static final Set<String> USED_FILES_STATS = Collections.newSetFromMap(new SizedLinkedHashMap(CACHED_FILES));
	
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	static {
		scheduler.scheduleAtFixedRate(new DeleteOldFilesTask(), 5, 5, TimeUnit.SECONDS);
	}
	
	private static final ExecutorService fileReadES = Executors.newFixedThreadPool(4);
	
	private static final class ReadHgtFileTask implements Callable<ByteBuffer> {

		private LonLat ll;
		private int zoom;

		public ReadHgtFileTask(LonLat ll, int zoom) {
			this.ll = ll;
			this.zoom = zoom;
		}

		public ByteBuffer call() throws Exception {
			File f = FILES_LAYOUT.getFile(this.ll, this.zoom);
			USED_FILES_STATS.add(FILES_LAYOUT.getKeyByFile(f));
			if (!f.exists()) {
				downloadTile();
			}
			return readHgt(f);  
		}

		private ByteBuffer readHgt(File f) {
			
			InputStream is = null;
	        try {
	        	is = new FileInputStream(f);
	        	if (f.getName().endsWith(".gz")) {
	        		is = new GZIPInputStream(is);
	        	}

	        	byte[] bytes = IOUtils.toByteArray(is);
	            
	            return ByteBuffer.wrap(bytes);
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        }
			finally {
				try {
					if (is != null) {
						is.close();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
	        
	        return null;
		}

		private void downloadTile() {
			try {
				String url = FILES_LAYOUT.getURL(this.ll, this.zoom);
				URL website = new URL(url);
				
				log.info("Download {}", url);
				
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				FileOutputStream fos = new FileOutputStream(FILES_LAYOUT.getFile(this.ll, this.zoom));
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				fos.close();
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	private static class DeleteOldFilesTask implements Runnable {
		
		public void run() {
			File[] files = FILES_LAYOUT.listFiles();
			
			if (files.length > CACHED_FILES) {
				List<File> filesList = new LinkedList<File>(Arrays.asList(files));
				
				while(filesList.size() > CACHED_FILES) {
					File file = filesList.get(0);
					String fileKey = FILES_LAYOUT.getKeyByFile(file);
					if (!USED_FILES_STATS.contains(fileKey)) {
						file.delete();
						log.info("Delete {}", file);
					}
					filesList.remove(0);
				}
			}
		}
	}
	
	public ByteBuffer read(LonLat ll, int zoom) {
		
		String hgtKey = FILES_LAYOUT.getKey(ll, zoom);
		
		ByteBuffer cacheLine = cache.get(hgtKey);
		if (cacheLine != null) {
			return cacheLine;
		}
		
		Future<ByteBuffer> future;
		
		synchronized (cache) {
			cacheLine = cache.get(hgtKey);
			if (cacheLine != null) {
				return cacheLine;
			}
			
			future = futures.get(hgtKey);
			if (future == null) {
				future = fileReadES.submit(new ReadHgtFileTask(ll, zoom));
				futures.put(hgtKey, future);
			}
		}		

		try {
			ByteBuffer shortBuffer = future.get();
			synchronized (cache) {
				futures.remove(hgtKey);
				cache.put(hgtKey, shortBuffer);
			}
			
			return shortBuffer;
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public String getKey(LonLat ll, int zoom) {
		return FILES_LAYOUT.getKey(ll, zoom);
	}

}
