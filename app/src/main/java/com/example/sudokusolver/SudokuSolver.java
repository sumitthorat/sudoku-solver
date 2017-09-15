package com.example.sudokusolver;

/**
 * Created by Dell on 14-09-2017.
 */

public class SudokuSolver {
    public SudokuSolver(){}

    //this method is the main backtracking algorithm ->returns true when the  grid is full and returns false when grid is still empty and backtracks
    public boolean solveSUDOKU(int[][] grid) {
        int row;
        int col;
        int[] blankCell = findBlankLocation(grid);
        row = blankCell[0];
        col = blankCell[1];
        if (row == -1) {
            return true;
        }
        for (int i = 1; i <= 9; i++) {
            if (isSafe(grid,row, col, i)) {
                grid[row][col] = i;
                if (solveSUDOKU(grid)) {
                    return true;
                }
                grid[row][col] = 0;
            }
        }
        return false; // This will cause the backtracking
    }
    //this method finds the unassigned location
    public int[] findBlankLocation(int[][] grid) {
        int[] cell = new int[2];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (grid[i][j] == 0) {
                    cell[0] = i;
                    cell[1] = j;
                    return cell;
                }
            }
        }
        cell[0] = -1;
        cell[1] = -1;
        return cell; // means grid is full
    }
    //this method checks whether the value assigned is safe in the position
    public boolean isSafe(int[][]grid,int row, int col, int n) {
        if (!UsedInRow(grid,row, n) && !UsedInColumn(grid,col, n)
                && !UsedInBox(grid,row - row % 3, col - col % 3, n)) {
            return true;
        }
        return false;
    }
    // check if a value is not present in particular row
    public boolean UsedInRow(int[][] grid,int row, int n) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == n) {
                return true;
            }
        }

        return false;
    }

    // check ifa value is not present in particular column
    public boolean UsedInColumn(int[][] grid,int col, int n) {
        for (int i = 0; i < 9; i++) {
            if (grid[i][col] == n) {
                return true;
            }
        }
        return false;
    }

    // check if value is  not present in  a particular(3x3) grid
    public boolean UsedInBox(int[][] grid,int boxStartRow, int boxStartCol, int n) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[i + boxStartRow][j + boxStartCol] == n) {
                    return true;
                }
            }
        }
        return false;
    }
    //this method copies the unsolved array into a temporary 2D array
    private static int[][] deepCopy(int[][] grid) {
        int[][] copy = new int[grid.length][grid[0].length];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                copy[i][j] = grid[i][j];
            }
        }
        return copy;
    }
    //this method returns the solved sudoku array
    public int[][] printFinal(int[][] grid){
        int[][] solve = deepCopy(grid);
       if(solveSUDOKU(solve))
            return solve;
        else
            return null;
    }
}
