package com.example.sudokusolver;

import org.opencv.core.Point;


public class LineOperations {

    private LineOperations() {

    }

    /*public static Point findCorner(double[] l1, double[] l2) {
        double x1 = l1[0];
        double y1 = l1[1];
        double x2 = l1[2];
        double y2 = l1[3];
        double x3 = l2[0];
        double y3 = l2[1];
        double x4 = l2[2];
        double y4 = l2[3];
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        double x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2)
                * (x3 * y4 - y3 * x4))
                / d;
        double y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2)
                * (x3 * y4 - y3 * x4))
                / d;
        Point p = new Point(x, y);
        return p;
    }*/

    public static Point LineIntersection(Point A, Point B, Point C, Point D)
    {
        // Line AB represented as a1x + b1y = c1
        double a1 = B.y - A.y;
        double b1 = A.x - B.x;
        double c1 = a1*(A.x) + b1*(A.y);

        // Line CD represented as a2x + b2y = c2
        double a2 = D.y - C.y;
        double b2 = C.x - D.x;
        double c2 = a2*(C.x)+ b2*(C.y);

        double determinant = a1*b2 - a2*b1;

        if (determinant == 0)
        {
            // The lines are parallel. This is simplified
            // by returning a pair of FLT_MAX
            return new Point(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        else
        {
            double x = (b2*c1 - b1*c2)/determinant;
            double y = (a1*c2 - a2*c1)/determinant;
            return new Point(x, y);
        }
    }

}