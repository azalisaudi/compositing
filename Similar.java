import jssim.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Similar {
	public static void main(String[] args) {
		File f1 = new File(args[0]);
		File f2 = new File(args[1]);
		
		double psnr = Tools.printPSNR(f1, f2);
		
		double ssim = Tools.printSSIM(f1, f2);

		double stc = Tools.printSC(f1, f2);
		double nae = Tools.printNAE(f1, f2);
		double avd = Tools.printAVD(f1, f2);
		double mxd = Tools.printMD(f1, f2);
		double ncc = Tools.printNCC(f1, f2);
	}
}
