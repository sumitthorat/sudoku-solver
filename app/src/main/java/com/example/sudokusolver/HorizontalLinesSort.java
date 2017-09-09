package com.example.sudokusolver;


import java.util.Comparator;


public class HorizontalLinesSort implements Comparator<double[]> {
    @Override
    public int compare(double[] o1, double[] o2) {
        if (o1[1] > o2[1] && o1[3] > o2[3])
            return 1;
        else if (o1[1] == o2[1] && o1[3] == o1[3])
            return 0;
        else
            return -1;
    }
}