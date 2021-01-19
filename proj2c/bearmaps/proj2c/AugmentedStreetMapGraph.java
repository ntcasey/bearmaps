package bearmaps.proj2c;

import bearmaps.hw4.streetmap.Node;
import bearmaps.hw4.streetmap.StreetMapGraph;
import bearmaps.proj2ab.Point;
import bearmaps.proj2ab.KDTree;
import bearmaps.proj2ab.WeirdPointSet;

import java.util.*;

/**
 * An augmented graph that is more powerful that a standard StreetMapGraph.
 * Specifically, it supports the following additional operations:
 *
 *
 * @author Alan Yao, Josh Hug, Casey Nguyen
 */
public class AugmentedStreetMapGraph extends StreetMapGraph {

    HashMap<Point, Long> pointToID = new HashMap<>();
    HashMap<String, HashSet<String>> cleanToFull = new HashMap<>();
    HashMap<String, HashSet<Node>> cleanToInfo = new HashMap<>();
    KDTree t;

    MyTrieSet trie = new MyTrieSet();

    public AugmentedStreetMapGraph(String dbPath) {
        super(dbPath);
        // You might find it helpful to uncomment the line below:
        List<Node> nodes = this.getNodes();

        ArrayList<Point> pointList = new ArrayList<>();
        for(Node n : nodes) { 
            if(!this.neighbors(n.id()).isEmpty()){
                Point p = new Point(n.lon(), n.lat());
                pointToID.put(p, n.id());
                pointList.add(p);
            }
            
            String fullName = n.name();
            if(fullName != null) {
                String clean = cleanString(fullName);
                HashSet<String> full = cleanToFull.get(clean);
                if (full == null) full = new HashSet<>();
                full.add(fullName);
                cleanToFull.put(clean, full);
                trie.add(clean);

                HashSet<Node> info = cleanToInfo.get(clean);
                if(info == null) info = new HashSet<>();
                info.add(n);
                cleanToInfo.put(clean, info);
            }
        }

        this.t = new KDTree(pointList);

         System.out.println("Number of total nodes: " + nodes.size());
         System.out.println("Number of intersections: " + pointList.size());
         //System.out.println("idtoname size: " + idtoname.size());
         System.out.println("map size " + pointToID.size());
         System.out.println("cleantofull size " + cleanToFull.size());
         System.out.println("cleanloc size " + cleanToInfo.size());
    }


    /**
     * For Project Part II
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    public long closest(double lon, double lat) {
        Point p = t.nearest(lon, lat);
        long id = pointToID.get(p);
        return id;
    }

    /**
     * For Project Part III (gold points)
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {

        LinkedList<String> locations = new LinkedList<>();
        String clean = cleanString(prefix);
        List<String> l = trie.keysWithPrefix(clean);
        for(String cleanString : l) {
            HashSet<String> fullName = cleanToFull.get(cleanString);
            if(fullName != null) {
                for (String s : fullName) {
                    locations.add(s);
                }
            }
        }
        return locations;
    }

    /**
     * For Project Part III (gold points)
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public List<Map<String, Object>> getLocations(String locationName) {
        String clean = cleanString(locationName);
        HashSet<Node> l = cleanToInfo.get(clean);
        ArrayList<Map<String, Object>> locationInfo = new ArrayList<>(l.size());

        for(Node info : l) {
            HashMap<String, Object> temp = new HashMap<>();
            temp.put("name", info.name());
            temp.put("lat", info.lat());
            temp.put("lon", info.lon());
            temp.put("id", info.id());
            locationInfo.add(temp);
        }
        return locationInfo;
    }


    /**
     * Useful for Part III. Do not modify.
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

}
