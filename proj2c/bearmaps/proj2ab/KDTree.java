package bearmaps.proj2ab;

import bearmaps.proj2ab.Point;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class KDTree implements Comparator {

    PointNode root;

    // used only for nearest
    double bestDistance;
    Point closetPoint;

    public KDTree(List<Point> points) {

        List<Point> pointsList = new ArrayList<>(points);   // because immutable
        root = new PointNode(pointsList.get(0), "xRef");
        pointsList.remove(0);

        // making the tree:
        for(Point p : pointsList)
            addPoint(p);
    }

    public Point nearest(double x, double y) {

        Point destination = new Point(x, y);
        closetPoint = root.point;
        bestDistance = Math.sqrt(Point.distance(root.point, destination));
        nearestHelper(root, destination);

        return closetPoint;
    }

    private void nearestHelper(PointNode current, Point destination) {

        // 0. if current == 0 -> means we reach null branch, return
        if (current == null)
            return;

        Point nearest;

        // 1. compute distance:
        double distance = Math.sqrt(Point.distance(current.point, destination));
        if (distance < bestDistance) {
            closetPoint = current.point;
            bestDistance = distance;
        }

        // 2. check best side:
        String bestSide;
        int value = compare(current, new PointNode(destination));
        if (value >= 0) {
            bestSide = "left";
            nearestHelper(current.left, destination);
        }
        else {
            bestSide = "right";
            nearestHelper(current.right, destination);
        }

        // 3a. see if we need to check bad side
        double checker;
        if(current.reference.equals("xRef"))
            checker = Math.abs(current.getX() - destination.getX());
        else
            checker = Math.abs(current.getY() - destination.getY());

        // 3b. check bad side?
        if (checker < bestDistance )
            computeBadSide(bestSide, current, destination);

    }

    private void computeBadSide(String bestSide, PointNode current, Point destination) {
        if (bestSide.equals("left")) {
            nearestHelper(current.right, destination);
            return;
        }
        nearestHelper(current.left, destination);
    }

    public void addPoint(Point p) {
        PointNode newPoint = new PointNode(p);
        addPointHelper(root, newPoint);
    }

    private void addPointHelper(PointNode current, PointNode descendant) {

        // result == 1 -> parent bigger/equal than descendant
        // result == -1 -> descendant bigger than parent
        // result == 0 -> parent point equal to descendant point
        int result = compare(current, descendant);


        if (result == 0) {
            swapNodes(current, descendant);
            return;
        }

        if (result == 1) {
            if (current.getLeft() == null) {
                current.setLeft(descendant);
                setRefOppositeFromParent(current, descendant);
                descendant.setParent(current);
            } else {
                addPointHelper(current.left, descendant);
            }
        } else {
            if (current.getRight() == null) {
                current.setRight(descendant);
                setRefOppositeFromParent(current, descendant);
                descendant.setParent(current);
            } else {
                addPointHelper(current.right, descendant);
            }
        }

    }

    private void swapNodes(PointNode current, PointNode descendant) {
        PointNode parent = current.getParent();

        if (parent.getLeft() != null) {
            if (compare(parent.getLeft(), descendant) == 0)
                parent.setLeft(descendant);
        } else {
            parent.setRight(descendant);
        }

        descendant.setRight(current.getRight());
        descendant.setLeft(current.getLeft());
        descendant.setParent(parent);
        setRefOppositeFromParent(parent, descendant);

        if (current.getRight() != null) {
            current.getRight().setParent(descendant);
            current.setRight(null);
        }
        if (current.getLeft() != null){
            current.getLeft().setParent(descendant);
            current.setLeft(null);
        }
        current.setParent(null);
    }

    private void setRefOppositeFromParent(PointNode current, PointNode descendant) {
        if (current.getReference().equals("xRef"))
            descendant.setReference("yRef");
        else
            descendant.setReference("xRef");
    }

    @Override
    public int compare(Object p1, Object p2) {
        PointNode node1 = (PointNode) p1;
        PointNode node2 = (PointNode) p2;

        if (node1.point.equals(node2.point))
            return 0;

        return node1.greater(node2) ? 1 : -1;
    }

    public class PointNode {

        Point point;
        String reference;   // xRef or yRef
        PointNode left, right, parent;

        public PointNode(Point p) {
            this(p, null);
        }

        public PointNode(Point p, String reference) {
            this.point = p;
            this.reference = reference;
        }

        public boolean greater(PointNode p2) {
            if(this.reference == "xRef") {
                return this.getX() > p2.getX();
            }

            return this.getY() > p2.getY();
        }

        // setters:
        public void setReference(String ref) {this.reference = ref;}
        public void setLeft(PointNode p) {this.left = p;}
        public void setRight(PointNode p) {this.right = p;}
        public void setParent(PointNode p) {this.parent = p;}

        // getters:
        public String getReference() {return this.reference;}
        public double getX() {return point.getX();}
        public double getY() {return point.getY();}
        public PointNode getLeft() {return this.left;}
        public PointNode getRight() {return this.right;}
        public PointNode getParent() {return this.parent;}
    }

    public static void main(String[] args) {
        List<Point> pointList = new ArrayList<>();
        Point a = new Point(2, 3);
        Point b = new Point(4, 2);
        Point c = new Point(4, 5);
        Point d = new Point(3,3);
        Point e = new Point(1, 5);
        Point f = new Point(4,4);
        Point g = new Point(4, 5);

        pointList.add(a);
        pointList.add(b);
        pointList.add(c);
        pointList.add(d);
        pointList.add(e);
        pointList.add(f);
        pointList.add(g);

        KDTree t = new KDTree(pointList);
        Point n = t.nearest(-4, 7);
        Point n2 = t.nearest(4, 1);
        Point n3 = t.nearest(5, 6);
    }

}
