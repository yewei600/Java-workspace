import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;


public class CameraPanel extends JPanel implements Runnable,ActionListener {
	
	BufferedImage image;
	VideoCapture capture;
	JButton screenshot;
	
	public CameraPanel() {
		screenshot = new JButton("Screenshot");
		screenshot.addActionListener(this);
		add(screenshot);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		File output = new File("screenshot1.png");
		int i=0;
		while(output.exists()){
			i++;
			output = new File("screenshot"+i+".png");
		}
		try {
			ImageIO.write(image, "png", output);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
	}

	@Override
	public void run() {
		System.loadLibrary("opencv_java300");
		capture = new VideoCapture();
		Mat webcam_image = new Mat();
		if(capture.isOpened()){
			while(true){
				capture.read(webcam_image);
				if(!webcam_image.empty()){
					JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
					topFrame.setSize(webcam_image.width()+40,webcam_image.height()+110);
					MatToBufferedImage(webcam_image);
					repaint();
				}
			}
		}
	}
	
	public void paintComponent(Graphics g){
		if(image==null)return;
		g.drawImage(image, 10, 40, image.getWidth(),image.getHeight(),null);			
	}

	private void MatToBufferedImage(Mat matBGR) {
		int width = matBGR.width(), height=matBGR.height(), channels=matBGR.channels();
		byte[] source = new byte[width*height*channels];
		matBGR.get(0, 0, source);
		
		image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final byte[] target = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		System.arraycopy(source,0 , target, 0, source.length);
	}
	
}