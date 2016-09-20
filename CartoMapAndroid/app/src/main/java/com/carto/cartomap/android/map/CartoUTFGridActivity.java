package com.carto.cartomap.android.map;

import android.os.Bundle;
import android.util.Log;

import com.carto.cartomap.android.Description;
import com.carto.cartomap.android.mapbase.VectorMapSampleBaseActivity;
import com.carto.cartomap.android.maplistener.MyUTFGridEventListener;
import com.carto.core.MapPos;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.TileDataSource;
import com.carto.layers.Layer;
import com.carto.layers.LayerVector;
import com.carto.layers.TileLayer;
import com.carto.layers.VectorLayer;
import com.carto.services.CartoMapsService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * A sample demonstrating how to use Carto Maps API with Raster tiles and UTFGrid
 */
@Description(value = "CARTO Maps API with Raster tiles and UTFGrid interactions")
public class CartoUTFGridActivity extends VectorMapSampleBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // MapSampleBaseActivity creates and configures mapView  
        super.onCreate(savedInstanceState);

        // Define server config
        JSONObject configJson = new JSONObject();
        try {
            // Change these according to your DB
            String sql = "select * from stations_1";
            String statTag = "3c6f224a-c6ad-11e5-b17e-0e98b61680bf";
            String[] columns = new String[]{"name", "field_9", "slot", "cartodb_id"};
            String cartoCss =
                    "#stations_1{marker-fill-opacity:0.9;marker-line-color:#FFF;" +
                            "marker-line-width:2;marker-line-opacity:1;marker-placement:point;" +
                            "marker-type:ellipse;marker-width:10;marker-allow-overlap:true;" +
                    "}\n" +
                    "#stations_1[status = 'In Service']{marker-fill:#0F3B82;}\n" +
                    "#stations_1[status = 'Not In Service']{marker-fill:#aaaaaa;}\n" +
                    "#stations_1[field_9 = 200]{marker-width:80.0;}\n" +
                    "#stations_1[field_9 <= 49]{marker-width:25.0;}\n" +
                    "#stations_1[field_9 <= 38]{marker-width:22.8;}\n" +
                    "#stations_1[field_9 <= 34]{marker-width:20.6;}\n" +
                    "#stations_1[field_9 <= 29]{marker-width:18.3;}\n" +
                    "#stations_1[field_9 <= 25]{marker-width:16.1;}\n" +
                    "#stations_1[field_9 <= 20.5]{marker-width:13.9;}\n" +
                    "#stations_1[field_9 <= 16]{marker-width:11.7;}\n" +
                    "#stations_1[field_9 <= 12]{marker-width:9.4;}\n" +
                    "#stations_1[field_9 <= 8]{marker-width:7.2;}\n" +
                    "#stations_1[field_9 <= 4]{marker-width:5.0;}";

            // You probably do not need to change much of below
            configJson.put("version", "1.0.1");
            configJson.put("stat_tag", statTag);

            JSONArray layersArrayJson = new JSONArray();
            JSONObject layersJson = new JSONObject();
            layersJson.put("type", "cartodb");

            JSONObject optionsJson = new JSONObject();
            optionsJson.put("sql", sql);
            optionsJson.put("cartocss", cartoCss);
            optionsJson.put("cartocss_version", "2.1.1");
            JSONArray interactivityJson = new JSONArray();
            interactivityJson.put("cartodb_id");
            optionsJson.put("interactivity", interactivityJson);
            JSONObject attributesJson = new JSONObject();
            attributesJson.put("id", "cartodb_id");
            JSONArray columnsJson = new JSONArray();

            for (String col : columns) {
                columnsJson.put(col);
            }

            attributesJson.put("columns", columnsJson);
            optionsJson.put("attributes", attributesJson);
            layersJson.put("options", optionsJson);
            layersArrayJson.put(layersJson);
            configJson.put("layers", layersArrayJson);

        } catch (JSONException e) {

        }

        final String config = configJson.toString();

        // Use the Maps service to configure layers. Note that this must be done
        // in a separate thread on Android, as Maps API requires connecting to server
        // which is not allowed in main thread.
		Thread serviceThread = new Thread(new Runnable() {
			@Override
			public void run() {

                CartoMapsService mapsService = new CartoMapsService();
                mapsService.setUsername("nutiteq");
                mapsService.setDefaultVectorLayerMode(true); // use vector layers
                mapsService.setInteractive(true); // turn on utfgrid loading

                try {
                    LayerVector layers = mapsService.buildMap(Variant.fromString(config));

                    LocalVectorDataSource vectorDataSource = new LocalVectorDataSource(baseProjection);
                    VectorLayer vectorLayer = new VectorLayer(vectorDataSource);

                    for (int i = 0; i < layers.size(); i++) {

                        // Add layer from map configuration
                        TileLayer layer = (TileLayer) layers.get(i);
                        System.out.println("SOURCE: " + layer.getUTFGridDataSource());
                        mapView.getLayers().add(layer);

                        MyUTFGridEventListener mapListener = new MyUTFGridEventListener(vectorLayer);
                        layer.setUTFGridEventListener(mapListener);
                    }

                    // Add layer for balloons
                    mapView.getLayers().add(vectorLayer);

                } catch (IOException e) {
                    Log.e("EXCEPTION", "Exception adding layers: " + e);
                }
            }
        });
		serviceThread.start();


        // finally animate map to the content area
        mapView.setFocusPos(baseProjection.fromWgs84(new MapPos(-74.0059, 40.7127)), 1); // NYC
        mapView.setZoom(15, 1);
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mapView.getLayers().count(); i++) {
            Layer layer = mapView.getLayers().get(i);
            if (layer instanceof TileLayer) {
                ((TileLayer) layer).setUTFGridEventListener(null);
            }
        }

        super.onDestroy();
    }
}