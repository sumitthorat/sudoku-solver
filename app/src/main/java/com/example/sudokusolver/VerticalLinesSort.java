package com.example.sudokusolver;

import java.util.Comparator;



public class VerticalLinesSort implements Comparator<double[]> {
    @Override
    public int compare(double[] o1, double[] o2) {
        if (o1[0] > o2[0] && o1[2] > o2[2])
            return 1;
        else if (o1[0] == o2[0] && o1[2] == o2[2])
            return 0;
        else
            return -1;
    }
}