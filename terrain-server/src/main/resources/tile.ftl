<html>
<head>
	<link rel="stylesheet" href="https://unpkg.com/leaflet@1.2.0/dist/leaflet.css"/>
	<script src="https://unpkg.com/leaflet@1.2.0/dist/leaflet.js"></script>
	
</head>
<body>
    <h1>Tile: ${tms}</h1>
    
    <div><span>Min elevation</span> <span>${minele?string["0.00"]?replace(",", ".")}</span></div>
    <div><span>Max elevation</span> <span>${maxele?string["0.00"]?replace(",", ".")}</span></div>

    <img src="/terrain/heightmap/${tms}.png" width="195" height="195">
    
    <#assign left_top>/terrain/heightmap/${z?string["0"]}/${(x-1)?string["0"]}/${(y+1)?string["0"]}</#assign>
    <#assign cntr_top>/terrain/heightmap/${z?string["0"]}/${(x)?string["0"]}/${(y+1)?string["0"]}</#assign>
    <#assign rght_top>/terrain/heightmap/${z?string["0"]}/${(x+1)?string["0"]}/${(y+1)?string["0"]}</#assign>
    
    <#assign left_cntr>/terrain/heightmap/${z?string["0"]}/${(x-1)?string["0"]}/${(y)?string["0"]}</#assign>
	<#assign cntr_cntr>/terrain/heightmap/${z?string["0"]}/${(x)?string["0"]}/${(y)?string["0"]}</#assign>
	<#assign rght_cntr>/terrain/heightmap/${z?string["0"]}/${(x+1)?string["0"]}/${(y)?string["0"]}</#assign>    

    <#assign left_btm>/terrain/heightmap/${z?string["0"]}/${(x-1)?string["0"]}/${(y - 1)?string["0"]}</#assign>
	<#assign cntr_btm>/terrain/heightmap/${z?string["0"]}/${(x)?string["0"]}/${(y - 1)?string["0"]}</#assign>
	<#assign rght_btm>/terrain/heightmap/${z?string["0"]}/${(x+1)?string["0"]}/${(y - 1)?string["0"]}</#assign>    
    
    
    <div style="margin-top: 10px;">
    	<div>
    		<a href="${left_top}.html"><img src="${left_top}.png"></a>
    		<a href="${cntr_top}.html"><img src="${cntr_top}.png"></a>
    		<a href="${rght_top}.html"><img src="${rght_top}.png"></a>
    	</div>
    	<div>
    		<a href="${left_cntr}.html"><img src="${left_cntr}.png"></a>
    		<a href="${cntr_cntr}.html"><img src="${cntr_cntr}.png"></a>
    		<a href="${rght_cntr}.html"><img src="${rght_cntr}.png"></a>
    	</div>
    	<div>
    		<a href="${left_btm}.html"><img src="${left_btm}.png"></a>
    		<a href="${cntr_btm}.html"><img src="${cntr_btm}.png"></a>
    		<a href="${rght_btm}.html"><img src="${rght_btm}.png"></a>
    	</div>
    </div>

	<h3 style="margin-bottom: 5px;">Parent and children</h3>

	<div>Parent: <a href="/terrain/heightmap/${parentTMS}.html">${parentTMS}</a></div>
    <div>Child mask: <span id="child-mask"></span><div>
    
    <table>
    	<tr>
    		<td>
    			<a href="/terrain/heightmap/${childTMS[0]}.html"><img src="/terrain/heightmap/${childTMS[0]}.png" alt="${childTMS[0]}"></a>
    		</td>
    		<td>
    			<a href="/terrain/heightmap/${childTMS[1]}.html"><img src="/terrain/heightmap/${childTMS[1]}.png" alt="${childTMS[1]}"></a>
    		</td>
    	</tr>
    	<tr>
    		<td>
    			<a href="/terrain/heightmap/${childTMS[2]}.html"><img src="/terrain/heightmap/${childTMS[2]}.png" alt="${childTMS[2]}"></a>
    		</td>
    		<td>
    			<a href="/terrain/heightmap/${childTMS[3]}.html"><img src="/terrain/heightmap/${childTMS[3]}.png" alt="${childTMS[3]}"></a>
    		</td>
    	</tr>
    </table>
    
            
    <h3 style="margin-bottom: 5px;">Bounding box</h3>
    
    <div class="bbox" style="margin-bottom: 10px;">
	    <div class="bbox-x">
		    <span>minx</span>
		    <span>${bbox.minx?string?replace(",", ".")}</span>
		    <span>maxx</span>
		    <span>${bbox.maxx?string?replace(",", ".")}</span>
	    </div>
	    <div class="bbox-y">
		    <span>miny</span>
		    <span>${bbox.miny?string?replace(",", ".")}</span>
		    <span>maxy</span>
		    <span>${bbox.maxy?string?replace(",", ".")}</span>
	    </div>
    </div>
    
    <div id="bboxmap" style="height: 260px; width: 400px;"></div>
    
    <script>
    	var bboxmap = L.map('bboxmap');
    	
    	var osmUrl = 'http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
		var osmAttrib = 'Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';
		var osm = new L.TileLayer(osmUrl, {minZoom: 0, maxZoom: 18, attribution: osmAttrib});
		bboxmap.addLayer(osm);
    	
    	var miny = ${bbox.miny?string?replace(",", ".")}
    	var maxy = ${bbox.maxy?string?replace(",", ".")}
    	var minx = ${bbox.minx?string?replace(",", ".")}
    	var maxx = ${bbox.maxx?string?replace(",", ".")}
    	
    	bboxmap.fitBounds([
		    [miny, minx],
		    [maxy, maxx]
		]);
    	
    	var polygon = L.polygon([
		    [miny, minx],
		    [maxy, minx],
		    [maxy, maxx],
		    [miny, maxx],
		    [miny, minx]
		]).addTo(bboxmap);
		
    </script>
    
    <h3>Content</h3>
    <table style="font-size: 8pt;" id="content">
    </table>
    
    <script>
    	var req = new XMLHttpRequest();
		req.open("GET", "${cntr_cntr}.terrain", true);
		req.responseType = "arraybuffer";
		
		req.onload = function (oEvent) {
		    var arrayBuffer = req.response;
		    if (arrayBuffer) {
		        var buffer = new Uint16Array(arrayBuffer, 0, 65 * 65);
		       	var target = document.getElementById('content');
		    	makeTable(target, buffer, 65, 65);
		    	
		    	var chldMask = new Uint8Array(arrayBuffer, 65 * 65 * 2, 1);
		    	var chldMaskArray = chldMask[0].toString(2).split('');
		    	chldMaskArray.reverse();
		    	
		    	for (var i = 0; i < 4; i++) {
		    		if(chldMaskArray[i] === undefined) {
		    			chldMaskArray[i] = 0;
		    		}
		    	}
		    	
		    	chldMaskArray.reverse();
		    	
		    	document.getElementById('child-mask').innerHTML = chldMaskArray.join(' ');
		  	}
		};
		
		function makeTable(target,buffer, w, h) {
		    var fragment = document.createDocumentFragment();
		
		    for (var r = 0; r < h; r++) {
		        var row = document.createElement('tr');
		        fragment.appendChild(row);
		
		        for (var c = 0; c < w; c++) {
		            var cell = document.createElement('td');
		            var ele = buffer[r * w + c] / 5 - 1000;
		            cell.appendChild(document.createTextNode(Math.round(ele)));
		            row.appendChild(cell);
		        }
		    }
		
		    target.appendChild(fragment);
		}
		
		req.send(null);
    </script>
    
    <h3>Requested tiles</h3>
    <#list requestedTileKeys as key>
    	<div>
    	<h3>${key}</h3>
    	<img src="http://elevation-tiles-prod.s3.amazonaws.com/terrarium/${key}.png">
    	<img src="http://tile.openstreetmap.org/${key}.png">
    	</div>
    </#list>
</body>
</html>