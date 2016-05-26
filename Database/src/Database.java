import java.awt.Dimension;

import javax.swing.*;


public class Database extends JPanel{
	static JTextArea sql = new JTextArea();
	JLabel prompt = new JLabel("Please enter your SQL statement below:");
	JButton exe = new JButton("Execute");
	JButton reset = new JButton("Reset");
	static JTable table = new JTable();
	
	public Database(){
		add(prompt);
		JScrollPane spane= new JScrollPane(sql);
		spane.setPreferredSize(new Dimension(750,100));
		add(spane);
		add(exe);
		add(reset);
		JScrollPane rpane = new JScrollPane(table);
		rpane.setPreferredSize(new Dimension(750, 350));
		add(rpane);
	}
}
