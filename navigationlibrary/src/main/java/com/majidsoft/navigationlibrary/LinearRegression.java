package com.majidsoft.navigationlibrary;

/**
 * The LinearRegression class calculate point p that project in line with two points
 *
 * @author Majid Mohammadnejad
 * @version 1.0
 * @email : majidrasht@gmail.com
 * @since 10/8/16
 */

public class LinearRegression {

    public Point getProjectedPointOnLine(Point v1, Point v2, Point p) {

        // get dot product of e1, e2
        Point e1 = new Point(v2.x - v1.x, v2.y - v1.y);
        Point e2 = new Point(p.x - v1.x, p.y - v1.y);
        double valDp = dotProduct(e1, e2);

        // get length of vectors
        double lenLineE1 = Math.sqrt(e1.x * e1.x + e1.y * e1.y);
        double lenLineE2 = Math.sqrt(e2.x * e2.x + e2.y * e2.y);
        double cos = valDp / (lenLineE1 * lenLineE2);

        // length of v1P'
        double projLenOfLine = cos * lenLineE2;
        Point pp = new Point((v1.x + (projLenOfLine * e1.x) / lenLineE1),
                (v1.y + (projLenOfLine * e1.y) / lenLineE1));

        return pp;
    }

    private double dotProduct(Point p1, Point p2) {
        return p1.x * p2.x + p1.y * p2.y;
    }

    public boolean checkPointInsideLine(Point startPoint, Point endPoint, Point pointInsideLine) {
        double maxX;
        double minX;
        double maxY;
        double minY;

        if (startPoint.x >= endPoint.x) {
            maxX = startPoint.x;
            minX = endPoint.x;
        } else {
            maxX = endPoint.x;
            minX = startPoint.x;
        }

        if (startPoint.y >= endPoint.y) {
            maxY = startPoint.y;
            minY = endPoint.y;
        } else {
            maxY = endPoint.y;
            minY = startPoint.y;
        }

        if ((pointInsideLine.x > minX && pointInsideLine.x < maxX) && (pointInsideLine.y > minY && pointInsideLine.y < maxY)) {
            return true;
        }

        return false;
    }

}
