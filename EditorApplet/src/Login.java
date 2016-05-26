import java.applet.Applet;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileLockInterruptionException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

import javax.swing.*;

public class Login extends Applet implements ActionListener {
	JLabel userL = new JLabel("Username: ");
	JTextField userTF = new JTextField();
	JLabel passL = new JLabel("Password:");
	JPasswordField passTF = new JPasswordField();
	JPanel loginP = new JPanel(new GridLayout(3, 2));
	JPanel panel = new JPanel();
	JButton login = new JButton("Login");
	JButton register = new JButton("Register");
	CardLayout cl;
	URL url;

	public void init() {
		setLayout(new CardLayout());
		loginP.add(userL);
		loginP.add(userTF);
		loginP.add(passL);
		loginP.add(passTF);
		login.addActionListener(this);
		register.addActionListener(this);
		loginP.add(login);
		loginP.add(register);
		panel.add(loginP);
		add(panel, "login");
		cl = (CardLayout) getLayout();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == login) {
			try {
				url=new URL("http://localhost:8080/EditorServlet/Verify");
				HttpURLConnection connection = (HttpURLConnection)url.openConnection();
				connection.setDoOutput(true);
				OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());				
				String pass = null;
				
				// encrypt the password again, and compare it to the already
				// encrypted password stored in "passwords.txt"
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(new String(passTF.getPassword()).getBytes());
				byte byteData[] = md.digest();
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < byteData.length; i++) {
					sb.append(Integer.toString((byteData[i] & 0xFF) + 0x100, 16).substring(1));
				}
				out.write(userTF.getText()+" "+sb.toString());
				out.close();
				int rc= connection.getResponseCode();  //response code
				if(rc==HttpURLConnection.HTTP_ACCEPTED){
					add(new FileBrowser(userTF.getText()),"fb");
					cl.show(this,"fb");
				}
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				System.out.println(in.readLine());
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if (arg0.getSource() == register) {
			add(new Register(), "register");
			cl.show(this, "register");
		}
	}
}
