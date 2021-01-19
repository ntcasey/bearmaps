package bearmaps.proj2c.server.handler.impl;

import bearmaps.proj2c.AugmentedStreetMapGraph;
import bearmaps.proj2c.server.handler.APIRouteHandler;
import spark.Request;
import spark.Response;
import bearmaps.proj2c.utils.Constants;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

import static bearmaps.proj2c.utils.Constants.*;

/**
 * Handles requests from the web browser for map images. These images
 * will be rastered into one large image to be displayed to the user.
 * @author rahul, Josh Hug, Casey Nguyen
 */
public class RasterAPIHandler extends APIRouteHandler<Map<String, Double>, Map<String, Object>> {

    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside RasterAPIHandler.processRequest(). <br>
     * ullat : upper left corner latitude, <br> ullon : upper left corner longitude, <br>
     * lrlat : lower right corner latitude,<br> lrlon : lower right corner longitude <br>
     * w : user viewport window width in pixels,<br> h : user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
            "lrlon", "w", "h"};

    /**
     * The result of rastering must be a map containing all of the
     * fields listed in the comments for RasterAPIHandler.processRequest.
     **/
    private static final String[] REQUIRED_RASTER_RESULT_PARAMS = {"render_grid", "raster_ul_lon",
            "raster_ul_lat", "raster_lr_lon", "raster_lr_lat", "depth", "query_success"};


    @Override
    protected Map<String, Double> parseRequestParams(Request request) {
        return getRequestParams(request, REQUIRED_RASTER_REQUEST_PARAMS);
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param requestParams Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @param response : Not used by this function. You may ignore.
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image;
     *                    can also be interpreted as the length of the numbers in the image
     *                    string. <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    @Override
    public Map<String, Object> processRequest(Map<String, Double> requestParams, Response response) {
        //System.out.println("yo, wanna know the parameters given by the web browser? They are:");

        // user request:
        Double ullat = requestParams.get("ullat");
        Double ullon = requestParams.get("ullon");
        Double lrlat = requestParams.get("lrlat");
        Double lrlon = requestParams.get("lrlon");
        Double w = requestParams.get("w");
        Double h = requestParams.get("h");      // is this useful?

        Map<String, Object> results = new HashMap<>();

        System.out.println(requestParams);

        Double userRequestDPP = queryLonDPP(lrlon, ullon, w);
        int depth = getDepth(userRequestDPP);


        // check if user lon/lat lies within map
        double[] coordinate = checkAndConfigCoordinates(ullon, ullat, lrlon, lrlat);
        if (coordinate == null) {
            queryFail();
        }
        else {
            ullon = coordinate[0];
            ullat = coordinate[1];
            lrlon = coordinate[2];
            lrlat = coordinate[3];
            results.put("query_success", true);
        }


        results.put("depth", depth);

        // upper coordinate
        double[] upperImageCoordinate = pinPointCoordinate(ullon, ullat, depth, Constants.ROOT_ULLON, Constants.ROOT_LRLON,
                Constants.ROOT_ULLAT, Constants.ROOT_LRLAT, "UpperPoint");

        // lower coordinate
        double[] lowerImageCoordinate = pinPointCoordinate(lrlon, lrlat, depth, Constants.ROOT_ULLON, Constants.ROOT_LRLON,
                Constants.ROOT_ULLAT, Constants.ROOT_LRLAT, "LowerPoint");

        results.put("raster_ul_lon", upperImageCoordinate[0]);
        results.put("raster_ul_lat", upperImageCoordinate[1]);
        results.put("raster_lr_lon", lowerImageCoordinate[0]);
        results.put("raster_lr_lat", lowerImageCoordinate[1]);



        int renderULLON = imageLocation(ullon, Constants.ROOT_ULLON, Constants.ROOT_LRLON, depth, "lon");
        int renderULLAT = imageLocation(ullat, Constants.ROOT_LRLAT, Constants.ROOT_ULLAT, depth, "lat");
        int renderLRLON  = imageLocation(lrlon, Constants.ROOT_ULLON, Constants.ROOT_LRLON, depth, "lon");
        int renderLRLAT = imageLocation(lrlat, Constants.ROOT_LRLAT, Constants.ROOT_ULLAT, depth, "lat");


        String[][] grid2DArray = retrieveImgs(renderULLON, renderULLAT, renderLRLON, renderLRLAT, depth);
        results.put("render_grid", grid2DArray);

        return results;
    }

    // user ullon, ullat, lron, lrlat
    private double[] checkAndConfigCoordinates(double ULLON, double ULLAT, double LRLON, double LRLAT){

        if (ULLON >= ROOT_LRLON || LRLAT >= ROOT_ULLAT || LRLON <= ROOT_ULLON || ULLAT <= ROOT_LRLAT
                || LRLON <= ULLON || LRLAT >= ULLAT)
            // cannot solve:
            return null; //new double[]{0, 0, 0, 0};

        if (ULLON < ROOT_ULLON)
            ULLON = ROOT_ULLON;

        if (ULLAT > ROOT_ULLAT)
            ULLAT = ROOT_ULLAT;

        if (LRLON > ROOT_LRLON)
            LRLON = ROOT_LRLON;

        if (LRLAT < ROOT_LRLAT)
            LRLAT = ROOT_LRLAT;

        return new double[]{ULLON, ULLAT, LRLON, LRLAT};
    }

    private String[][] retrieveImgs(int renderULLON, int renderULLAT, int renderLRLON, int renderLRLAT, int depth) {
        ArrayList<ArrayList<String>> grid = new ArrayList<>();
        for(int y = renderULLAT; y <= renderLRLAT; y++){

            ArrayList<String> imgGrid = new ArrayList<>();
            for(int x = renderULLON; x <= renderLRLON; x++){
                imgGrid.add("d" + Integer.toString(depth) + "_x" + Integer.toString(x)
                        + "_y" + Integer.toString(y) + ".png");
            }
            grid.add(imgGrid);
        }
        return convertTo2DArray(grid);
    }

    private String[][] convertTo2DArray(ArrayList l) {
        // https://stackoverflow.com/questions/10043209/convert-arraylist-into-2d-array-containing-varying-lengths-of-arrays

        String[][] array = new String[l.size()][];
        for (int i = 0; i < l.size(); i++) {
            ArrayList<String> row = (ArrayList<String>) l.get(i);
            array[i] = row.toArray(new String[row.size()]);
        }
        return array;
    }

    private int imageLocation(double USER, double smallValue, double largeValue, int depth, String persepctive){

        double blockSize = (Math.abs(largeValue - smallValue))/Math.pow(2, depth);
        //int location = (int) Math.floor(Math.abs(USER_LON)/blockSize);

        int longFlag = (USER == largeValue && persepctive == "lon") ? -1 : 0;
        int latFlag = (USER == smallValue && persepctive == "lon") ? -1 : 0;

        if (smallValue > 0)
            USER = USER - Math.abs(smallValue);

        if (smallValue < 0)
            USER = USER + Math.abs(smallValue);

        int location = (int) Math.floor(Math.abs(USER)/blockSize);
        if (persepctive == "lon")
            if (longFlag == -1) { return location-1;}
            else {
                return location;
            }

        // not sure if this if is nessary.
        if (latFlag == -1)
            return (int) Math.pow(2, depth) - location - 2;

        return (int) Math.pow(2, depth) - location - 1;
    }

    private double[] pinPointCoordinate(double USER_LON, double USER_LAT, int depth,double ULLON,
                                        double LRLON, double ULLAT, double LRLAT, String point) {
        if (depth == 0) {
            if (point.equals("UpperPoint"))
                return new double[]{ULLON, ULLAT};
            else
                return new double[]{LRLON, LRLAT};
        }

        double MIDLON = (LRLON - ULLON)/2 + ULLON;
        double MIDLAT = (ULLAT - LRLAT)/2 + LRLAT;

        // 1. Upper Left Box
        if(upperLeftBox(USER_LON, USER_LAT, ULLON, MIDLON, ULLAT, MIDLAT))
            return pinPointCoordinate(USER_LON, USER_LAT, depth-1, ULLON, MIDLON, ULLAT, MIDLAT, point);

        // 2. Upper Right Box
        if(upperRightBox(USER_LON, USER_LAT, MIDLON, LRLON, ULLAT, MIDLAT))
            return pinPointCoordinate(USER_LON, USER_LAT, depth-1, MIDLON, LRLON, ULLAT, MIDLAT, point);

        // 3. Lower Left Box
        if(lowerLeftBox(USER_LON, USER_LAT, ULLON, MIDLON, MIDLAT, LRLAT))
            return pinPointCoordinate(USER_LON, USER_LAT, depth-1, ULLON, MIDLON, MIDLAT, LRLAT, point);

        // 4. Lower Right Box
        return pinPointCoordinate(USER_LON, USER_LAT, depth-1, MIDLON, LRLON, MIDLAT, LRLAT, point);
    }


    private boolean upperLeftBox(double USER_LON, double USER_LAT, double ULLON, double MIDLON,
                                 double ULLAT, double MIDLAT) {

        return (ULLON <= USER_LON && USER_LON < MIDLON)
                && (ULLAT >= USER_LAT && USER_LAT > MIDLAT);
    }

    private boolean upperRightBox(double USER_LON, double USER_LAT, double MIDLON, double LRLON,
                                  double ULLAT, double MIDLAT) {

        return (MIDLON <= USER_LON && USER_LON < LRLON)
                && (ULLAT >= USER_LAT && USER_LAT > MIDLAT);
    }

    private boolean lowerLeftBox(double USER_LON, double USER_LAT, double ULLON, double MIDLON,
                                 double MIDLAT, double LRLAT) {

        return (ULLON <= USER_LON && USER_LON < MIDLON)
                && (MIDLAT >= USER_LAT && USER_LAT > LRLAT);
    }

    private boolean lowerRightBox(double USER_LON, double USER_LAT, double MIDLON, double LRLON,
                                  double MIDLAT, double LRLAT) {

        return (MIDLON <= USER_LON && USER_LON < LRLON)
                && (MIDLAT >= USER_LAT && USER_LAT > LRLAT);
    }

    private int getDepth(double userRequestDPP){

        int depth = 0;
        return getDepthHelper(userRequestDPP, Constants.ROOT_LRLON, Constants.ROOT_ULLON, depth);
    }

    private int getDepthHelper(double userRequestDPP, double depth_LRLON, double depth_ULLON, int depth) {
        if (depth == 7)
            return depth;

        double currentDepthDPP = (depth_LRLON - depth_ULLON)/Constants.TILE_SIZE;
        double depthPlus_1_ULLON = ((depth_LRLON - depth_ULLON)/2) + depth_ULLON;
        double next_DepthDPP = (depth_LRLON - depthPlus_1_ULLON)/Constants.TILE_SIZE;

        if (userRequestDPP >= currentDepthDPP)
            return 0;

        if (userRequestDPP < currentDepthDPP && userRequestDPP >= next_DepthDPP) // check logic later
            return depth + 1;

        return getDepthHelper(userRequestDPP, depth_LRLON, depthPlus_1_ULLON, depth + 1);
    }

    /**
     * User requested longitudinal distance per pixel.
     **/
    private Double queryLonDPP(Double lrlon, Double ullon, Double width){
        return (lrlon - ullon)/width;
    }

    @Override
    protected Object buildJsonResponse(Map<String, Object> result) {
        boolean rasterSuccess = validateRasteredImgParams(result);

        if (rasterSuccess) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeImagesToOutputStream(result, os);
            String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
            result.put("b64_encoded_image_data", encodedImage);
        }
        return super.buildJsonResponse(result);
    }

    private Map<String, Object> queryFail() {
        Map<String, Object> results = new HashMap<>();
        results.put("render_grid", null);
        results.put("raster_ul_lon", 0);
        results.put("raster_ul_lat", 0);
        results.put("raster_lr_lon", 0);
        results.put("raster_lr_lat", 0);
        results.put("depth", 0);
        results.put("query_success", true);
        return results;
    }

    /**
     * Validates that Rasterer has returned a result that can be rendered.
     * @param rip : Parameters provided by the rasterer
     */
    private boolean validateRasteredImgParams(Map<String, Object> rip) {
        for (String p : REQUIRED_RASTER_RESULT_PARAMS) {
            if (!rip.containsKey(p)) {
                System.out.println("Your rastering result is missing the " + p + " field.");
                return false;
            }
        }
        if (rip.containsKey("query_success")) {
            boolean success = (boolean) rip.get("query_success");
            if (!success) {
                System.out.println("query_success was reported as a failure");
                return false;
            }
        }
        return true;
    }

    /**
     * Writes the images corresponding to rasteredImgParams to the output stream.
     * In Spring 2016, students had to do this on their own, but in 2017,
     * we made this into provided code since it was just a bit too low level.
     */
    private  void writeImagesToOutputStream(Map<String, Object> rasteredImageParams,
                                            ByteArrayOutputStream os) {
        String[][] renderGrid = (String[][]) rasteredImageParams.get("render_grid");
        int numVertTiles = renderGrid.length;
        int numHorizTiles = renderGrid[0].length;

        BufferedImage img = new BufferedImage(numHorizTiles * Constants.TILE_SIZE,
                numVertTiles * Constants.TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics graphic = img.getGraphics();
        int x = 0, y = 0;

        for (int r = 0; r < numVertTiles; r += 1) {
            for (int c = 0; c < numHorizTiles; c += 1) {
                graphic.drawImage(getImage(Constants.IMG_ROOT + renderGrid[r][c]), x, y, null);
                x += Constants.TILE_SIZE;
                if (x >= img.getWidth()) {
                    x = 0;
                    y += Constants.TILE_SIZE;
                }
            }
        }

        /* If there is a route, draw it. */
        double ullon = (double) rasteredImageParams.get("raster_ul_lon"); //tiles.get(0).ulp;
        double ullat = (double) rasteredImageParams.get("raster_ul_lat"); //tiles.get(0).ulp;
        double lrlon = (double) rasteredImageParams.get("raster_lr_lon"); //tiles.get(0).ulp;
        double lrlat = (double) rasteredImageParams.get("raster_lr_lat"); //tiles.get(0).ulp;

        final double wdpp = (lrlon - ullon) / img.getWidth();
        final double hdpp = (ullat - lrlat) / img.getHeight();
        AugmentedStreetMapGraph graph = SEMANTIC_STREET_GRAPH;
        List<Long> route = ROUTE_LIST;

        if (route != null && !route.isEmpty()) {
            Graphics2D g2d = (Graphics2D) graphic;
            g2d.setColor(Constants.ROUTE_STROKE_COLOR);
            g2d.setStroke(new BasicStroke(Constants.ROUTE_STROKE_WIDTH_PX,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            route.stream().reduce((v, w) -> {
                g2d.drawLine((int) ((graph.lon(v) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(v)) * (1 / hdpp)),
                        (int) ((graph.lon(w) - ullon) * (1 / wdpp)),
                        (int) ((ullat - graph.lat(w)) * (1 / hdpp)));
                return w;
            });
        }

        rasteredImageParams.put("raster_width", img.getWidth());
        rasteredImageParams.put("raster_height", img.getHeight());

        try {
            ImageIO.write(img, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private BufferedImage getImage(String imgPath) {
        BufferedImage tileImg = null;
        if (tileImg == null) {
            try {
                File in = new File(imgPath);
                tileImg = ImageIO.read(in);
                //tileImg = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource(imgPath));

            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        return tileImg;
    }
}
