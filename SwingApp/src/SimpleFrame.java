import javax.swing.JFrame;
import javax.swing.UIManager;

public class SimpleFrame extends JFrame {
	public SimpleFrame(){
		super("Frame Title");
		setSize(500,500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLookAndFeel();
		setVisible(true);
	}
	
	private static void setLookAndFeel(){
		try{
			UIManager.setLookAndFeel(
					"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
		}catch(Exception exc){
			
		}
	}
	
	public static void main(String[] args){
		setLookAndFeel();
		SimpleFrame sf=new SimpleFrame();
	}
}
