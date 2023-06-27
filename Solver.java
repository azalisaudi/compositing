//
// Author: Azali Saudi
// Date Created : 18 Dec 2016
// Last Modified: 29 Dec 2016
// Task: The Solver implementation i.e. Jacobi, GS, RGS, SOR, AOR, etc.
//

import java.util.ArrayList;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;

public class Solver {
	//Variables passed along from the visual interface
	//used to display the image as it's iteratively updated
	public ArrayList<Coord> selectionArea;
	public int xMin, yMin;

	//Matrix variables
	int N;
	double[][] U;//Guess
	double[][] V;//Guess
	double[][] b;//Target of Ax = b
	double[][] c;//Target of Ax = c (Rotated Grid)

    public Solver(int[][] mask, ArrayList<Coord> selectionArea,
    					BufferedImage image, BufferedImage selectedImage,
    					int xMin, int yMin, int Width, int Height, boolean flatten) {
		this.selectionArea = selectionArea;
    	this.xMin = xMin;
    	this.yMin = yMin;

    	N = selectionArea.size();
    	U = new double[N][3]; // For the 3 color channels
    	V = new double[N][3];
    	b = new double[N][3];

		//
    	// Initialize the matrix U and V and make the initial guess the value
    	// of the pixels in "selectedImage"
    	//
    	int[][] dP = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
		for(int y = 1; y < Height-1; y++) {
		  for(int x = 1; x < Width-1; x++) {
			if (mask[x][y] < 0) continue;

            int i = mask[x][y];
    		int selX = x - xMin;
    		int selY = y - yMin;
    		int RGB = selectedImage.getRGB(selX, selY);
    		int pValueR = (RGB & 0xFF0000) >> 16;
    		int pValueG = (RGB & 0xFF00) >> 8;
    		int pValueB = RGB & 0xFF;

    		U[i][0] = V[i][0] = pValueR;
    		U[i][1] = V[i][1] = pValueG;
    		U[i][2] = V[i][2] = pValueB;

    		//
    		// Compute the solution for the b[N][3] array
    		//
    		b[i][0] = 0.0; b[i][1] = 0.0; b[i][2] = 0.0;
    		for (int k = 0; k < dP.length; k++) {
    			int x2 = x + dP[k][0];
    			int y2 = y + dP[k][1];

    			if (mask[x2][y2] == -1) { //It's a border pixel
    				RGB = image.getRGB(x2, y2);
    				b[i][0] += (RGB & 0xFF0000) >> 16;
    				b[i][1] += (RGB & 0xFF00) >> 8;
    				b[i][2] += RGB & 0xFF;
    			}
    			else if (mask[x2][y2] == -2) {
					// Do nothing
				}
    			else {
    				selX = x2 - xMin;
    				selY = y2 - yMin;
    				RGB = selectedImage.getRGB(selX, selY);
		    		int qValueR = (RGB & 0xFF0000) >> 16;
		    		int qValueG = (RGB & 0xFF00) >> 8;
		    		int qValueB = RGB & 0xFF;
    				//vPQ = P - Q
    				b[i][0] += (pValueR - qValueR);
    				b[i][1] += (pValueG - qValueG);
    				b[i][2] += (pValueB - qValueB);
    			}
    		}
		  }
    	}

    	//
    	// The c[N][3] array stores the gradient for rotated grid.
    	//
    	c = new double[N][3];
    	int[][] cP = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
    	for (int i = 0; i < N; i++) {
    		int x = selectionArea.get(i).x;
    		int y = selectionArea.get(i).y;
    		int selX = x - xMin;
    		int selY = y - yMin;
    		int RGB = selectedImage.getRGB(selX, selY);
    		int pValueR = (RGB & 0xFF0000) >> 16;
    		int pValueG = (RGB & 0xFF00) >> 8;
    		int pValueB = RGB & 0xFF;
    		c[i][0] = 0.0; c[i][1] = 0.0; c[i][2] = 0.0;
    		for (int k = 0; k < dP.length; k++) {
    			int x2 = x + cP[k][0];
    			int y2 = y + cP[k][1];

    			if (mask[x2][y2] == -1) { // It's a border or outside pixel
    				RGB = image.getRGB(x2, y2);
    				c[i][0] += (RGB & 0xFF0000) >> 16;
    				c[i][1] += (RGB & 0xFF00) >> 8;
    				c[i][2] += RGB & 0xFF;
    			}
    			else if (mask[x2][y2] == -2) { // It's an outside pixel
    			    // We use that outside pixel
    				RGB = image.getRGB(x2, y2);
    				c[i][0] += (RGB & 0xFF0000) >> 16;
    				c[i][1] += (RGB & 0xFF00) >> 8;
    				c[i][2] += RGB & 0xFF;
				}
    			else {
    				selX = x2 - xMin;
    				selY = y2 - yMin;
    				RGB = selectedImage.getRGB(selX, selY);
		    		int qValueR = (RGB & 0xFF0000) >> 16;
		    		int qValueG = (RGB & 0xFF00) >> 8;
		    		int qValueB = RGB & 0xFF;
    				//vPQ = P - Q
    				c[i][0] += (pValueR - qValueR);
    				c[i][1] += (pValueG - qValueG);
    				c[i][2] += (pValueB - qValueB);
    			}
    		}
    	}
    }

    public void doJacobi(int[][] mask, int W, int H) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];

		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) xL[0] = xL[1] = xL[2] = 0;
			else {
				xL[0] = U[mask[x-1][y]][0];
				xL[1] = U[mask[x-1][y]][1];
				xL[2] = U[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) xB[0] = xB[1] = xB[2] = 0;
			else {
				xB[0] = U[mask[x][y-1]][0];
				xB[1] = U[mask[x][y-1]][1];
				xB[2] = U[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = 0.25 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]);
		    V[i][1] = 0.25 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]);
		    V[i][2] = 0.25 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]);
    	  }
	    }

        // Assign vector U with the updated vector V
    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void doGS(int[][] mask, int W, int H) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];

		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) xL[0] = xL[1] = xL[2] = 0;
			else {
				xL[0] = V[mask[x-1][y]][0];
				xL[1] = V[mask[x-1][y]][1];
				xL[2] = V[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) xB[0] = xB[1] = xB[2] = 0;
			else {
				xB[0] = V[mask[x][y-1]][0];
				xB[1] = V[mask[x][y-1]][1];
				xB[2] = V[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = 0.25 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]);
		    V[i][1] = 0.25 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]);
		    V[i][2] = 0.25 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]);
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void doSOR(int[][] mask, int W, int H, double w) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];

		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) xL[0] = xL[1] = xL[2] = 0;
			else {
				xL[0] = V[mask[x-1][y]][0];
				xL[1] = V[mask[x-1][y]][1];
				xL[2] = V[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) xB[0] = xB[1] = xB[2] = 0;
			else {
				xB[0] = V[mask[x][y-1]][0];
				xB[1] = V[mask[x][y-1]][1];
				xB[2] = V[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = w*0.25 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]) + (1-w)*U[i][0];
		    V[i][1] = w*0.25 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]) + (1-w)*U[i][1];
		    V[i][2] = w*0.25 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]) + (1-w)*U[i][2];
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void doAOR(int[][] mask, int W, int H, double w, double r) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];
        double[] vL = new double[3];
        double[] vB = new double[3];

		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) {
                xL[0] = xL[1] = xL[2] = 0;
                vL[0] = vL[1] = vL[2] = 0;
            }
			else {
				xL[0] = U[mask[x-1][y]][0];
				xL[1] = U[mask[x-1][y]][1];
				xL[2] = U[mask[x-1][y]][2];
				vL[0] = V[mask[x-1][y]][0];
				vL[1] = V[mask[x-1][y]][1];
				vL[2] = V[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) {
                xB[0] = xB[1] = xB[2] = 0;
                vB[0] = vB[1] = vB[2] = 0;
            }
			else {
				xB[0] = U[mask[x][y-1]][0];
				xB[1] = U[mask[x][y-1]][1];
				xB[2] = U[mask[x][y-1]][2];
				vB[0] = V[mask[x][y-1]][0];
				vB[1] = V[mask[x][y-1]][1];
				vB[2] = V[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = w/4 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]) + (1-w)*U[i][0] +
		              r/4 * (vL[0] - xL[0] + vB[0] - xB[0]);
		    V[i][1] = w/4 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]) + (1-w)*U[i][1] +
		              r/4 * (vL[1] - xL[1] + vB[1] - xB[1]);
		    V[i][2] = w/4 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]) + (1-w)*U[i][2] +
		              r/4 * (vL[2] - xL[2] + vB[2] - xB[2]);
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void doTOR(int[][] mask, int W, int H, double w, double r, double s) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];
        double[] vL = new double[3];
        double[] vB = new double[3];

		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) {
                xL[0] = xL[1] = xL[2] = 0;
                vL[0] = vL[1] = vL[2] = 0;
            }
			else {
				xL[0] = U[mask[x-1][y]][0];
				xL[1] = U[mask[x-1][y]][1];
				xL[2] = U[mask[x-1][y]][2];
				vL[0] = V[mask[x-1][y]][0];
				vL[1] = V[mask[x-1][y]][1];
				vL[2] = V[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) {
                xB[0] = xB[1] = xB[2] = 0;
                vB[0] = vB[1] = vB[2] = 0;
            }
			else {
				xB[0] = U[mask[x][y-1]][0];
				xB[1] = U[mask[x][y-1]][1];
				xB[2] = U[mask[x][y-1]][2];
				vB[0] = V[mask[x][y-1]][0];
				vB[1] = V[mask[x][y-1]][1];
				vB[2] = V[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = w/4 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]) + (1-w)*U[i][0] +
		              r/4 * (vL[0] - xL[0])+
		              s/4 * (vB[0] - xB[0]);
		    V[i][1] = w/4 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]) + (1-w)*U[i][1] +
		              r/4 * (vL[1] - xL[1])+
		              s/4 * (vB[1] - xB[1]);
		    V[i][2] = w/4 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]) + (1-w)*U[i][2] +
		              r/4 * (vL[2] - xL[2])+
		              s/4 * (vB[2] - xB[2]);
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void doRGS(int[][] mask, int W, int H) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];

		for(int y = 1; y < H-1; y++) {
		  for(int x = 1; x < W-2; x++) {
			if ((x+y) % 2 == 1) continue;
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y-1] < 0) xL[0] = xL[1] = xL[2] = 0;
			else {
				xL[0] = V[mask[x-1][y-1]][0];
				xL[1] = V[mask[x-1][y-1]][1];
				xL[2] = V[mask[x-1][y-1]][2];
		    }
    		if (mask[x+1][y-1] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = V[mask[x+1][y-1]][0];
				xR[1] = V[mask[x+1][y-1]][1];
				xR[2] = V[mask[x+1][y-1]][2];
		    }
    		if (mask[x-1][y+1] < 0) xB[0] = xB[1] = xB[2] = 0;
			else {
				xB[0] = U[mask[x-1][y+1]][0];
				xB[1] = U[mask[x-1][y+1]][1];
				xB[2] = U[mask[x-1][y+1]][2];
		    }
    		if (mask[x+1][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x+1][y+1]][0];
				xU[1] = U[mask[x+1][y+1]][1];
				xU[2] = U[mask[x+1][y+1]][2];
		    }

			int i = mask[x][y];
		    V[i][0] = 0.25 * (xL[0] + xR[0] + xB[0] + xU[0] + c[i][0]);
		    V[i][1] = 0.25 * (xL[1] + xR[1] + xB[1] + xU[1] + c[i][1]);
		    V[i][2] = 0.25 * (xL[2] + xR[2] + xB[2] + xU[2] + c[i][2]);
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}
    }

    public void fillRGS(int[][] mask, int W, int H) {
    	double[] xL = new double[3];
    	double[] xR = new double[3];
    	double[] xB = new double[3];
    	double[] xU = new double[3];

		for(int y = 1; y < H-2; y++) {
		  for(int x = 1; x < W-2; x++) {
			if ((x+y) % 2 == 0) continue;
			if (mask[x][y] < 0) continue;

    		if (mask[x-1][y] < 0) xL[0] = xL[1] = xL[2] = 0;
			else {
				xL[0] = U[mask[x-1][y]][0];
				xL[1] = U[mask[x-1][y]][1];
				xL[2] = U[mask[x-1][y]][2];
		    }
    		if (mask[x+1][y] < 0) xR[0] = xR[1] = xR[2] = 0;
			else {
				xR[0] = U[mask[x+1][y]][0];
				xR[1] = U[mask[x+1][y]][1];
				xR[2] = U[mask[x+1][y]][2];
		    }
    		if (mask[x][y-1] < 0) xB[0] = xB[1] = xB[2] = 0;
			else {
				xB[0] = U[mask[x][y-1]][0];
				xB[1] = U[mask[x][y-1]][1];
				xB[2] = U[mask[x][y-1]][2];
		    }
    		if (mask[x][y+1] < 0) xU[0] = xU[1] = xU[2] = 0;
			else {
				xU[0] = U[mask[x][y+1]][0];
				xU[1] = U[mask[x][y+1]][1];
				xU[2] = U[mask[x][y+1]][2];
		    }

			int i = mask[x][y];
		    U[i][0] = V[i][0] = 0.25 * (xL[0] + xR[0] + xB[0] + xU[0] + b[i][0]);
		    U[i][1] = V[i][1] = 0.25 * (xL[1] + xR[1] + xB[1] + xU[1] + b[i][1]);
		    U[i][2] = V[i][2] = 0.25 * (xL[2] + xR[2] + xB[2] + xU[2] + b[i][2]);
    	  }
	    }

    	for (int i = 0; i < N; i++) {
			U[i][0] = V[i][0];
			U[i][1] = V[i][1];
			U[i][2] = V[i][2];
		}

	}

    public double getError(int[][] mask, int W, int H) {
    	double total = 0.0;
		for(int y = 0; y < H; y++) {
		  for(int x = 0; x < W; x++) {
			if (mask[x][y] < 0) continue;

			int i = mask[x][y];
    		double[] error = {b[i][0], b[i][1], b[i][2]};

    		if (mask[x-1][y] >= 0) {
 				error[0] += U[mask[x-1][y]][0];
   				error[1] += U[mask[x-1][y]][1];
   				error[2] += U[mask[x-1][y]][2];
			}
    		if (mask[x+1][y] >= 0) {
 				error[0] += U[mask[x+1][y]][0];
   				error[1] += U[mask[x+1][y]][1];
   				error[2] += U[mask[x+1][y]][2];
			}
    		if (mask[x][y-1] >= 0) {
 				error[0] += U[mask[x][y-1]][0];
   				error[1] += U[mask[x][y-1]][1];
   				error[2] += U[mask[x][y-1]][2];
			}
    		if (mask[x][y+1] >= 0) {
 				error[0] += U[mask[x][y+1]][0];
   				error[1] += U[mask[x][y+1]][1];
   				error[2] += U[mask[x][y+1]][2];
			}
    		error[0] -= 4*U[i][0];
    		error[1] -= 4*U[i][1];
    		error[2] -= 4*U[i][2];
    		total += (error[0]*error[0] + error[1]*error[1] + error[2]*error[2]);
	  	  }
    	}
    	return Math.sqrt(total);
    }

    public double getRError(int[][] mask, int W, int H) {
    	double total = 0.0;
		for(int y = 1; y < H-1; y++) {
		  for(int x = 1; x < W-2; x++) {
			if ((x+y) % 2 == 1) continue;
			if (mask[x][y] < 0) continue;

			int i = mask[x][y];

    		double[] error = {c[i][0], c[i][1], c[i][2]};
    		if (mask[x-1][y-1] >= 0) {
 				error[0] += U[mask[x-1][y-1]][0];
   				error[1] += U[mask[x-1][y-1]][1];
   				error[2] += U[mask[x-1][y-1]][2];
			}
    		if (mask[x+1][y-1] >= 0) {
 				error[0] += U[mask[x+1][y-1]][0];
   				error[1] += U[mask[x+1][y-1]][1];
   				error[2] += U[mask[x+1][y-1]][2];
			}
    		if (mask[x-1][y+1] >= 0) {
 				error[0] += U[mask[x-1][y+1]][0];
   				error[1] += U[mask[x-1][y+1]][1];
   				error[2] += U[mask[x-1][y+1]][2];
			}
    		if (mask[x+1][y+1] >= 0) {
 				error[0] += U[mask[x+1][y+1]][0];
   				error[1] += U[mask[x+1][y+1]][1];
   				error[2] += U[mask[x+1][y+1]][2];
			}
    		error[0] -= 4*U[i][0];
    		error[1] -= 4*U[i][1];
    		error[2] -= 4*U[i][2];
    		total += (error[0]*error[0] + error[1]*error[1] + error[2]*error[2]);
	  	  }
    	}
    	return Math.sqrt(total);
    }

    public void updateImage(BufferedImage selectedImage) {
    	for (int i = 0; i < N; i++) {
    		int x = selectionArea.get(i).x - xMin;
    		int y = selectionArea.get(i).y - yMin;
    		int R = (int)Math.round(U[i][0]);
    		int G = (int)Math.round(U[i][1]);
    		int B = (int)Math.round(U[i][2]);
    		if (R > 255) R = 255;
    		if (R < 0) R = 0;
    		if (G > 255) G = 255;
    		if (G < 0) G = 0;
    		if (B > 255) B = 255;
    		if (B < 0) B = 0;
    		int RGB = 0xFF000000 | (R<<16)&0xFF0000 | (G<<8)&0xFF00 | B&0xFF;
    		selectedImage.setRGB(x, y, RGB);
    	}
    }
}
