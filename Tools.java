import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import jssim.*;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Class to store constants and static methods.
 * 
 * @author Amanda Askew & Dane Barney
 * @version Feb 18, 2004 10:28:41 PM
 * 
 */
public class Tools {
	// number of bits used to store the mantissa in the 64-bit IEEE-754 standard
	public static final int DOUBLE_MANTISSA_BITS = 52;
	// number of bits used to store the mantissa in the 32-bit IEEE-754 standard
	public static final int FLOAT_MANTISSA_BITS = 23;

	public static final byte POSITIVE = 0;
	public static final byte NEGATIVE = 1;

	public static final int BLACK = 0;
	public static final int WHITE = 1;

	public static final int INSIGNIFICANT = 0;
	public static final int SIGNIFICANT = 1;

	public static long unsigned(long l) {
		long x = l; x <<= 32; x >>>= 32;
		return x;
	}

	/**
	 * Computes and prints out the PSNR of two images (which must have the same 
	 * dimensions and type).
	 */
	public static double printPSNR(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR PSNR: Could not read image files");
		}
		
		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double mse = 0;
		int width = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				if(((i+j)%2) == 1) continue;
				mse
					+= Math.pow(r1.getSample(i, j, 0) - r2.getSample(i, j, 0), 2);
			}
		mse /= (double) (width * height);
		System.err.println("MSE = " + mse);
		int maxVal = 255;
		double x = Math.pow(maxVal, 2) / mse;
		double psnr = 10.0 * logbase10(x);
		System.err.println("PSNR = " + psnr);
		return psnr;
	}
	
	/**
	 * Returns the base-10 logarithm of a number
	 */
	public static double logbase10(double x) {
		return Math.log(x) / Math.log(10);
	}

	/**
	 * The resultant SSIM index is a decimal value between -1 and 1, 
	 * and value 1 is only reachable in the case of two identical 
	 * sets of data.
	 */
	public static double printSSIM(File im1, File im2) {
		double ssim = 0.0;
		try {
			SsimCalculator ssimcalc = new SsimCalculator(im1);
			
			ssim = ssimcalc.compareTo(im2);
			System.out.println("SSIM = " + ssim);
		} catch (Exception e) {
			System.out.println("ERROR: Could not calculate SSIM");
		}
		return ssim;
	}

	/**
	 * Ideal value is 1
	 */
	public static double printSC(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR SC: Could not read image files");
		}

		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double stc = 0, A = .0, B = .0;
		int width = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				A += Math.pow(r1.getSample(i, j, 0), 2);
				B += Math.pow(r2.getSample(i, j, 0), 2);
			}
		stc = A / B;
		System.err.println("SC = " + stc);
		return stc;
	}
	
	/**
	 * Ideal value is 0
	 */
	public static double printNAE(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR NAE: Could not read image files");
		}

		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double nae = 0, A = .0, absDiff = .0;
		int width = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				absDiff += Math.abs(r1.getSample(i, j, 0) - r2.getSample(i, j, 0));
				A += r1.getSample(i, j, 0);
			}
		nae = absDiff / A;
		System.err.println("NAE = " + nae);
		return nae;
	}
	
	/**
	 * Ideal value is 0
	 */
	public static double printAVD(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR AVD: Could not read image files");
		}

		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double avd = .0;
		int width = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				avd += Math.abs(r1.getSample(i, j, 0) - r2.getSample(i, j, 0));
			}
		avd /= (double) (width * height);
		System.err.println("AVD = " + avd);
		return avd;
	}	
	
	/**
	 * Lower value is better
	 */
	public static double printMD(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR MD: Could not read image files");
		}

		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double mxd = .0;
		int width = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				double a = Math.abs(r1.getSample(i, j, 0) - r2.getSample(i, j, 0));
				if(a > mxd) mxd = a;
			}
		System.err.println("MD = " + mxd);
		return mxd;
	}
	
	
	/**
	 * 
	 * Added by Azali
	 * 
	 */
	public static double printNCC(File file1, File file2) {
		BufferedImage im1 = null;
		BufferedImage im2 = null;
		try {
			im1 = ImageIO.read(file1);
			im2 = ImageIO.read(file2);
		} catch (Exception e) {
			System.out.println("ERROR NCC: Could not read image files");
		}

		assert(
			im1.getType() == im2.getType()
				&& im1.getHeight() == im2.getHeight()
				&& im1.getWidth() == im2.getWidth());

		double ncc = 0, AB = .0, A2 = .0;
		int width  = im1.getWidth();
		int height = im1.getHeight();
		Raster r1 = im1.getRaster();
		Raster r2 = im2.getRaster();
		for (int j = 0; j < height; j++)
			for (int i = 0; i < width; i++) {
				AB += r1.getSample(i, j, 0) * r2.getSample(i, j, 0);
				A2 += Math.pow(r1.getSample(i, j, 0), 2);
			}
		ncc = AB / A2;
		System.out.println("NCC = " + ncc);
		return ncc;
	}		
}
