import java.util.Properties;

import javax.swing.JFrame;

public class Main {
	public static void main(String[] args){
		JFrame frame = new JFrame("Database");
		Properties props = new Properties();
		ConnectDialog dialog = new ConnectDialog(frame, "Database Connector", props);
		dialog.setVisible(true);
		if(dialog.isCancelled)
			System.exit(0);
		Connector conn = new Connector(dialog.getProps(), new String(dialog.pass.getPassword()));
		if(!conn.open())
			System.exit(0);		
		Database dpanel = new Database();
		frame.add(dpanel);
		frame.setSize(800,600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
}
