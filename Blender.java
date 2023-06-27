//
// Author: Azali Saudi
// Date Created : 18 Dec 2016
// Last Modified: 01 Feb 2017
// Task: The GUI for Poisson Image Blending
//

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;
import java.util.concurrent.TimeUnit;

public class Blender extends JFrame implements ActionListener,
						MouseListener, MouseMotionListener {
	//Display parameters
	public static final int Width = 1000;
	public static final int Height = 500;

	//Program state
	public static final int NOTHING = 0;
	public static final int SELECTING = 1;
	public static final int DRAGGING = 2;
	public static final int BLENDING = 3;

	//Menu Options
	public static final String SELECT_IMAGE1 = "Target Image...";
	public static final String SELECT_IMAGE2 = "Source Image...";
	public static final String SELECT_REGION = "Select Region";
	public static final String SAVE_REGION = "Save Region...";
	public static final String LOAD_REGION = "Load Region...";
	public static final String BLEND_SELECTION = "Blend Selection";
	public static final String SAVE_IMAGE = "Save Image to File...";

	//GUI Widgets
	public JLabel label;
	public JTextField tfMethod;
	public JTextField tfW1;
	public JTextField tfW2;
	public JTextField tfR1;
	public JTextField tfR2;
	public JTextField tfR3;
	public JTextField tfR4;
	public JScrollPane spNote;
	public JTextArea taNote;
	public Display canvas;
	public JMenuBar menu;
	public JMenu fileMenu;
	public BufferedImage image;
	public BufferedImage targetImage = null;
	public BufferedImage sourceImage = null;

	//Variables for selected image
	public int[][] mask;//A 2D array that represents a selected region
	//It encodes the enclosed region and the border of that region
	public ArrayList<Coord> selectionBorder;
	public ArrayList<Coord> selectionArea;
	public BufferedImage selectedImage;
	int xMin, xMax, yMin, yMax;//Bounding box of selected area

	//GUI State Variables
	public int state;
	public boolean dragValid;
	public int lastX, lastY;
	public int dx, dy;
	public boolean selectingLeft;
	public String targetFilename;

	//The solver
	public Solver solver;
	public Thread iteratorThread;

	//-2 for uninvolved pixels
	//-1 for border pixels
	//Index number for area pixels
	//This function also moves everything over
	void updateMask() {
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++)
				mask[x][y] = -2;
		}
		for (int i = 0; i < selectionBorder.size(); i++) {
			int x = selectionBorder.get(i).x + dx;
			int y = selectionBorder.get(i).y + dy;
			selectionBorder.get(i).x = x;
			selectionBorder.get(i).y = y;
			if (x < 0 || x >= Width || y < 0 || y >= Height)
				continue;
			mask[x][y] = -1;
		}
		for (int i = 0; i < selectionArea.size(); i++) {
			int x = selectionArea.get(i).x + dx;
			int y = selectionArea.get(i).y + dy;
			selectionArea.get(i).x = x;
			selectionArea.get(i).y = y;
			if (x < 0 || x >= Width || y < 0 || y >= Height)
				continue;
			mask[x][y] = i;
		}
		xMin += dx; xMax += dx;
		yMin += dy; yMax += dy;
		dx = 0;
		dy = 0;
	}

	public Blender() {
		setTitle("Blender");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mask = new int[Width][Height];
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++)
				mask[x][y] = 0;
		}
		selectionArea = new ArrayList<Coord>();
		selectionBorder = new ArrayList<Coord>();

		Container content = getContentPane();
		content.setLayout(null);

		menu = new JMenuBar();
		fileMenu = new JMenu("File");
		fileMenu.addActionListener(this);
		fileMenu.add(SELECT_IMAGE1).addActionListener(this);
		fileMenu.add(SELECT_IMAGE2).addActionListener(this);
		fileMenu.addSeparator();
		fileMenu.add(SELECT_REGION).addActionListener(this);
		fileMenu.add(SAVE_REGION).addActionListener(this);
		fileMenu.add(LOAD_REGION).addActionListener(this);
		fileMenu.add(BLEND_SELECTION).addActionListener(this);
		fileMenu.addSeparator();
		fileMenu.add(SAVE_IMAGE).addActionListener(this);
		fileMenu.addSeparator();
		fileMenu.add("Exit").addActionListener((ActionEvent event) -> { System.exit(0); });
		menu.add(fileMenu);

		menu.setBounds(0, 0, Width, 20);
		content.add(menu);

		label = new JLabel("0");
		label.setBounds(10,20, 400,30);
		content.add(label);


		canvas = new Display();
		canvas.setSize(Width, Height);
		canvas.addMouseMotionListener(this);
		canvas.addMouseListener(this);
		canvas.setBounds(0, 50, Width, Height);
		content.add(canvas);

		tfMethod = new JTextField("GS");
		tfMethod.setBounds(200,Height+60, 80,25);
		content.add(tfMethod);

		tfW1 = new JTextField("1.60");
		tfW1.setBounds(10,Height+60, 50,25);
		content.add(tfW1);
		tfW2 = new JTextField("1.66");
		tfW2.setBounds(10,Height+85, 50,25);
		content.add(tfW2);

		tfR1 = new JTextField("1.70");
		tfR1.setBounds(80,Height+60, 50,25);
		content.add(tfR1);
		tfR2 = new JTextField("1.74");
		tfR2.setBounds(80,Height+85, 50,25);
		content.add(tfR2);
		tfR3 = new JTextField("1.78");
		tfR3.setBounds(80,Height+110,50,25);
		content.add(tfR3);
		tfR4 = new JTextField("1.82");
		tfR4.setBounds(80,Height+135,50,25);
		content.add(tfR4);

		taNote = new JTextArea("");
		JScrollPane spNote = new JScrollPane(taNote);
		spNote.setBounds(500,Height+60, 480,96);
		content.add(spNote);

		state = NOTHING;
		image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, Width, Height);
		selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		dx = 0;
		dy = 0;
		setSize(Width, Height+200);
		setVisible(true);
	}

	public class Display extends JPanel {
		public void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, this);
			g.setColor(Color.RED);

			if (state == SELECTING || state == DRAGGING) {
				for (int i = 0; i < selectionBorder.size(); i++) {
					int x = selectionBorder.get(i).x + dx;
					int y = selectionBorder.get(i).y + dy;
					g.drawLine(x, y, x, y);
				}
			}

			g.drawImage(selectedImage, xMin+dx, yMin+dy, this);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		String str = evt.getActionCommand();
		if (state == BLENDING)
			return;
		if (str.equals(SELECT_REGION)) {
			//Clear previous selection
			selectionBorder.clear();
			selectionArea.clear();
			state = SELECTING;
		}
		else if (str.equals(BLEND_SELECTION)) {
			state = BLENDING;

			updateMask();
			solver = new Solver(mask, selectionArea, image, selectedImage,
									  xMin, yMin, Width, Height, false);
			Iterator iterator = new Iterator();
			iteratorThread = new Thread(iterator);
			iteratorThread.start();
		}
		else if (str.equals(SELECT_IMAGE1)) {
			try {
            	JFileChooser chooser = new JFileChooser(new File(".").getCanonicalPath());
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
			   		targetImage = ImageIO.read(chooser.getSelectedFile());
			   		targetFilename = chooser.getSelectedFile().getName();
            	}
			}
			catch (Exception e) {
			    e.printStackTrace();
    		}
			selectingLeft = true;

	    	Graphics g = image.getGraphics();
			int startX = 0;
			g.drawImage(targetImage, startX, 0, this);
			canvas.repaint();
		}
		else if (str.equals(SELECT_IMAGE2)) {
			try {
            	JFileChooser chooser = new JFileChooser(new File(".").getCanonicalPath());
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
			   		sourceImage = ImageIO.read(chooser.getSelectedFile());
               	}
			}
            catch (Exception e) {
				e.printStackTrace();
    		}
			selectingLeft = false;

	    	Graphics g = image.getGraphics();
	    	int startX = Width/2;
			g.drawImage(sourceImage, startX, 0, this);
			canvas.repaint();
		}
		else if (str.equals(SAVE_IMAGE)) {
			try {
	    		JFileChooser chooser = new JFileChooser(new File(".").getCanonicalPath());
				int returnVal = chooser.showSaveDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					File fout = chooser.getSelectedFile();
					String saveFilename = fout.getName();
					BufferedImage bi = new BufferedImage(targetImage.getWidth(), targetImage.getHeight(), BufferedImage.TYPE_INT_RGB);
					canvas.paint(bi.getGraphics());
					ImageIO.write(bi, "png", new File(saveFilename));
/*
					String ext = "";
					int i = saveFilename.lastIndexOf('.');
					if (i > 0 &&  i < saveFilename.length() - 1) {
						ext = saveFilename.substring(i+1).toLowerCase();
					}
					BufferedImage bi = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
					canvas.paint(bi.getGraphics());
					ImageIO.write(bi, ext, fout);
*/
				}
    		}
    		catch (Exception e) {
    			e.printStackTrace();
    		}
		}
		else if (str.equals(SAVE_REGION)) {
			saveArea();
		}
		else if (str.equals(LOAD_REGION)) {
			selectionBorder.clear();
			selectionArea.clear();
			image.getGraphics().drawImage(targetImage, 0, 0, this);
			loadArea();

			state = DRAGGING;
		}

		canvas.repaint();
	}

	public void mouseMoved(MouseEvent evt) {
		lastX = evt.getX();
		lastY = evt.getY();
	}

	public void mouseDragged(MouseEvent evt) {
		int x = evt.getX();
		int y = evt.getY();
		if (state == SELECTING) {
			selectionBorder.add(new Coord(x, y));
		}
		else if (state == DRAGGING) {
			//Make sure the user is dragging within the bounds of the selection
			if (!dragValid) {
				if (mask[x][y] >= 0) {
					dragValid = true;
				}
			}
			if (dragValid) {
				dx += (x-lastX);
				dy += (y-lastY);
			}
		}
		lastX = x;
		lastY = y;
		canvas.repaint();
	}

	void fillOutside(int paramx, int paramy) {
		ArrayList<Coord> stack = new ArrayList<Coord>();
		stack.add(new Coord(paramx, paramy));
		while (stack.size() > 0) {
			Coord c = stack.remove(stack.size()-1);
			int x = c.x, y = c.y;
			if (x < 0 || x >= Width || y < 0 || y >= Height)
				continue;
			if (mask[x][y] == -1) //Stop at border pixels
				continue;
			if (mask[x][y] == 0) //Don't repeat nodes that have already been visited
				continue;
			mask[x][y] = 0;
			stack.add(new Coord(x-1, y));
			stack.add(new Coord(x+1, y));
			stack.add(new Coord(x, y-1));
			stack.add(new Coord(x, y+1));
		}
	}

	public void getSelectionArea() {
		selectionArea.clear();
		updateMask();
		//Find bounding box of selected region
		xMin = Width;
		xMax = 0;
		yMin = Height;
		yMax = 0;
		for (int i = 0; i < selectionBorder.size(); i++) {
			int x = selectionBorder.get(i).x;
			int y = selectionBorder.get(i).y;
			if (x < xMin)
				xMin = x;
			if (x > xMax)
				xMax = x;
			if (y < yMin)
				yMin = y;
			if (y > yMax)
				yMax = y;
		}
		int selWidth = xMax - xMin;
		int selHeight = yMax - yMin;
		selectedImage = new BufferedImage(selWidth, selHeight, BufferedImage.TYPE_INT_ARGB);
		//Find a pixel outside of the bounding box, which is guaranteed
		//to be outside of the selection
		boolean found = false;
		for (int x = 0; x < Width && !found; x++) {
			for (int y = 0; y < Height && !found; y++) {
				if ((x < xMin || x > xMax) && (y < yMin || y > yMax)) {
					found = true;
					fillOutside(x, y);
				}
			}
		}
		//Pixels in selection area have mask value of -2, outside have mask value of 0
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++) {
				if (x - xMin >= 0 && y - yMin >= 0 && x - xMin < selWidth && y - yMin < selHeight)
					selectedImage.setRGB(x-xMin, y-yMin, image.getRGB(x,y)&0x00FFFFFF);
				if (mask[x][y] == 0) {
					mask[x][y] = -2;
				}
				else if (mask[x][y] != -1) {
					mask[x][y] = selectionArea.size();//Make mask index of this coord
					selectionArea.add(new Coord(x, y));
					int color = (255 << 24) | image.getRGB(x, y);
					if (x - xMin >= 0 && y - yMin >= 0) ;
						selectedImage.setRGB(x-xMin, y-yMin, color);
				}
			}
		}
		updateMask();
	}

	public void mouseReleased(MouseEvent evt) {
		//Fill in pixels in between and connect the first to the last
		int N = selectionBorder.size();
		if (N == 0 || (state != SELECTING && state != DRAGGING))
			return;

		if (state == SELECTING) {
			for (int n = 0; n < N; n++) {
				int startx = selectionBorder.get(n).x;
				int starty = selectionBorder.get(n).y;
				int totalDX = selectionBorder.get((n+1)%N).x - startx;
				int totalDY = selectionBorder.get((n+1)%N).y - starty;
				int numAdded = Math.abs(totalDX) + Math.abs(totalDY);
				for (int t = 0; t < numAdded; t++) {
					double frac = (double)t / (double)numAdded;
					int x = (int)Math.round(frac*totalDX) + startx;
					int y = (int)Math.round(frac*totalDY) + starty;
					selectionBorder.add(new Coord(x, y));
				}
			}

			updateMask();
			getSelectionArea();
			state = DRAGGING;
			dragValid = false;
			dx = 0;
			dy = 0;
		}
		else if (state == DRAGGING) {
			dragValid = false;
			updateMask();
		}
		canvas.repaint();
	}

	public void finalizeBlending() {
		Graphics g = image.getGraphics();
		g.drawImage(selectedImage, xMin, yMin, null);
		selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		selectionBorder.clear();
		selectionArea.clear();
		state = NOTHING;
	}

	class Iterator implements Runnable {
		public void run() {
			double w = Float.parseFloat(tfW1.getText());
			double r = Float.parseFloat(tfR1.getText());
			double s = Float.parseFloat(tfR2.getText());
			double t = Float.parseFloat(tfR3.getText());
			double u = Float.parseFloat(tfR4.getText());
			int iteration = 0;
			long startTime = System.nanoTime();
			double error = 0.0;
			if (tfMethod.getText().toUpperCase().equals("JACOBI") || tfMethod.getText().equals("")) {
				do {
					solver.doJacobi(mask, Width, Height);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);
				taNote.append(">>> JACOBI\n");

			}
			if (tfMethod.getText().toUpperCase().equals("GS")) {
				do {
					solver.doGS(mask, Width, Height);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);
				taNote.append(">>> GS\n");
			}
			if (tfMethod.getText().toUpperCase().equals("SOR")) {
				do {
					solver.doSOR(mask, Width, Height, w);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);
				taNote.append(String.format(">>> SOR, w=%.2f\n", w));
			}
			if (tfMethod.getText().toUpperCase().equals("AOR")) {
				do {
					solver.doAOR(mask, Width, Height, w, r);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);
				taNote.append(String.format(">>> AOR, w=%.2f, r=%.2f\n", w, r));
			}
			if (tfMethod.getText().toUpperCase().equals("TOR")) {
				do {
					solver.doTOR(mask, Width, Height, w, r, s);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);
				taNote.append(String.format(">>> TOR, w=%.2f, r=%.2f, s=%.2f\n", w, r,s));
			}
			if (tfMethod.getText().toUpperCase().equals("RGS")) {
				do {
					solver.doRGS(mask, Width, Height);
//					solver.fillRGS(mask, Width, Height);
					synchronized(selectedImage) {
						solver.updateImage(selectedImage);
					}
		        	canvas.repaint();
					error = solver.getRError(mask, Width, Height);
					iteration++;
		        	label.setText(String.format("%d", iteration));
				} while (error > 1.0 && state == BLENDING);

				solver.fillRGS(mask, Width, Height);
				synchronized(selectedImage) {
					solver.updateImage(selectedImage);
				}
		        canvas.repaint();
				taNote.append(">>> RGS\n");
			}

			finalizeBlending();
			long stopTime = System.nanoTime();
			long elapsed = stopTime - startTime;
			elapsed = TimeUnit.NANOSECONDS.toMillis(elapsed); // Total elapsed in ms
		    label.setText(String.format("Iteration: %d| Elapsed: %d min, %d sec, %d ms| SRE: %.4f",
		                  iteration, (elapsed/1000) / 60, (elapsed/1000) % 60, (elapsed%60000) % 1000, error));

		    // Print out text area
		    taNote.append("Target: " + targetFilename + "\n");
			taNote.append("Iteration: " + Integer.toString(iteration) + "\n");
			taNote.append(String.format("Elapsed: %d min, %d sec, %d ms\n",
			                           (elapsed/1000) / 60,
			                           (elapsed/1000) % 60,
			                           (elapsed%60000) % 1000));
			taNote.append(String.format("Error: %.4f\n", error));
		}
	}

    public void saveArea() {
		String saveFilename = "";
		try {
			JFileChooser chooser = new JFileChooser(new File(".").getCanonicalPath());
			int returnVal = chooser.showSaveDialog(null);
			if(returnVal != JFileChooser.APPROVE_OPTION) return;

			File fout = chooser.getSelectedFile();
			saveFilename = fout.getName();
		}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter(saveFilename));
			fw.write(Integer.toString(selectionBorder.size()));
			fw.newLine();
			for (int i = 0; i < selectionBorder.size(); i++) {
				fw.write(String.format("%d %d", selectionBorder.get(i).x, selectionBorder.get(i).y));
			    fw.newLine();
			}
			fw.write(Integer.toString(selectionArea.size()));
			fw.newLine();
			for (int i = 0; i < selectionArea.size(); i++) {
				fw.write(String.format("%d %d", selectionArea.get(i).x, selectionArea.get(i).y));
			    fw.newLine();
			}
			fw.write(String.format("%d %d %d %d", xMin, yMin, xMax, yMax));
			fw.newLine();
			fw.flush();
			fw.close();
		} catch (IOException ex) {
		    System.out.println("File could not be created.");
		}

		try {
			ImageIO.write(selectedImage, "png", new File(saveFilename + ".png"));


            BufferedImage maskImage = new BufferedImage(selectedImage.getWidth(), selectedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < selectedImage.getHeight(); y++)
                for (int x = 0; x < selectedImage.getWidth(); x++) {
                    if (selectedImage.getRGB(x,y) > 0)
                        maskImage.setRGB(x,y, 0);
                    else
                        maskImage.setRGB(x,y, 255);
                }
			ImageIO.write(maskImage, "png", new File(saveFilename + "b.png"));

    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
	}

    public void loadArea() {
		String loadFilename = "";
		try {
			JFileChooser chooser = new JFileChooser(new File(".").getCanonicalPath());
			int returnVal = chooser.showOpenDialog(null);
			if(returnVal != JFileChooser.APPROVE_OPTION) return;

			File fout = chooser.getSelectedFile();
			loadFilename = fout.getName();
		}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

		try {
			Scanner sc = new Scanner(new File(loadFilename));
			int size = sc.nextInt();
			for (int i = 0; i < size; i++) {
				int x = sc.nextInt();
				int y = sc.nextInt();
				selectionBorder.add(new Coord(x, y));
			}
			size = sc.nextInt();
			for (int i = 0; i < size; i++) {
				int x = sc.nextInt();
				int y = sc.nextInt();
				selectionArea.add(new Coord(x, y));
			}
			xMin = sc.nextInt();
			yMin = sc.nextInt();
			xMax = sc.nextInt();
			yMax = sc.nextInt();
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			selectedImage = ImageIO.read(new File(loadFilename + ".png"));
   		}
   		catch (Exception e) {
   			e.printStackTrace();
   		}
	}

	public void mouseClicked(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	public void mousePressed(MouseEvent evt) {}

	public static void main(String[] args) {
		Blender a = new Blender();
	}

}
