/****************************************************
Travus Helmly
wthelmly
The following is a client side chat room application.
It includes a GUI, and allows users to select a picture
to represent them.


*****************************************************/

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class PictureChatClient implements ActionListener,ListSelectionListener, Runnable
{
	private static Boolean DEBUG = false;
	private static String clientName="";
	private static String pictureFileName="";
	private  ImageIcon pictureIcon; 


	
	Vector<String> pictureFileNames=new Vector<String>();
	
	/* GUI objects */
	private JFrame chatWindow = new JFrame();
	private JTextArea inTextArea  = new JTextArea();
	private JTextArea outTextArea = new JTextArea();
	private JButton sendButton = new JButton("Send To All");
	private JScrollPane inScrollPane  = new JScrollPane(inTextArea);
	private JScrollPane outScrollPane = new JScrollPane(outTextArea);
	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			inScrollPane, outScrollPane);
	private JMenuBar  menuBar            = new JMenuBar();
	private JMenu     privateMessageMenu = new JMenu("PrivateMessages");
	private JMenuItem sendToMenuItem     = new JMenuItem("Send To");
	private JMenuItem saveForMenuItem    = new JMenuItem("Save For");
	
	private JFrame        whosInWindow      = new JFrame("WHO's IN");
	private JFrame        whosNotInWindow   = new JFrame("WHO's NOT IN");
	
	private JList whosInList        = new JList();
	private JList whosNotInList     = new JList();
	
	
	private JButton       clearWhosInButton = new JButton("Clear Selections");
	private JButton       clearWhosNotButton= new JButton("Clear Selections");
	private JButton       sendToButton      = new JButton("Send To");
	private JButton       saveForButton     = new JButton("Save For");
	private Socket           socket;
	private ObjectOutputStream objOS;
	private ObjectInputStream  objIS;
	
	/*pictureWindow GUIs */
	private JFrame pictureWindow			=new JFrame("Picture");
	private JPanel topPicturePanel 			=new JPanel(new GridLayout(3,1));
	private JPanel middlePicturePanel		=new JPanel(new GridLayout(1,2));
	private JPanel bottomPicturePanel		=new JPanel(new GridLayout(3,1));
	private JPanel previewPicturePanel      =new JPanel();
	private JButton clearPictureSelectionButton	=new JButton("Clear Picture Selection");
	private JButton sendPictureToAllButton  =new JButton("Send Picture to All");
	private JButton sendPictureOnlyToButton   =new JButton ("Send Picture Only To");
	private JButton savePictureForButton	=new JButton("Save Picture For");
	private JLabel noSelectionLabel			=new JLabel("");
	private JLabel selectPictureLabel 		=new JLabel("Select a Picture");
	private JList pictureList		=new JList();
	private JScrollPane pictureScrollPane 	=new JScrollPane(pictureList);
	
	/*Allows user to place other chat room members in an ignored list*/
	private JFrame		whosIgnoredWindow = new JFrame("WHO's IGNORED");
	private JMenu		ignoredMenu = new JMenu("IgnoreClients");
	private JList		whosIgnored       = new JList();
	private JTextField	userToBeIgnoredText = new JTextField();
	private JButton		clearWhosIgnoredButton = new JButton("Remove Selected");
	private JMenuItem 	whosIgonoredMenuItem    = new JMenuItem("List");
	Vector<String> lstWhosIgnored = new Vector<String>();
	
	/*Picture menu */
	private JMenu picturesMenu=new JMenu("SendPictures");
	private JMenuItem picturesMenuItem= new JMenuItem("List of Pictures");
	
	/* New Line */
	private String newLine = System.getProperty("line.separator");

	public static void main(String[] args) 
	{
		System.out.println("---------------------------------");
		System.out.println("Olivier Karangwa (okarang@ncsu)");
		System.out.println("---------------------------------");
		System.out.println("*********Client Console**********");
		System.out.println("Format for the parameters: serverAddress chatName password");

		if (args.length < 3) {
			System.out.println("Please restart following this format of 3 command line" ); 	
			System.out.println("parameters: serverAddress chatName password");
			return;
		} 

		try {
			new PictureChatClient(args[1], args[0], args[2]);
		}
		catch (Exception ex) {
			System.out.println("ERROR: " + ex.toString());
		}
	}

	public PictureChatClient(String chatName, String serverAddress, String password) throws Exception
	{
		clientName=chatName; //will be used later
		
		/* Verify invalid characters */
		if (chatName.contains(" "))
			throw new IllegalArgumentException("Blank Field on chatName parameter");
		else if (chatName.contains("/"))
			throw new IllegalArgumentException("Forward Slash on chatName parameter");

		if (serverAddress.contains(" "))
			throw new IllegalArgumentException("Blank Field on serverAddress parameter");
		else if (serverAddress.contains("/"))
			throw new IllegalArgumentException("Forward Slash on serverAddress parameter");

		if (password.contains(" "))
			throw new IllegalArgumentException("Blank Field on Password parameter");
		else if (password.contains("/"))
			throw new IllegalArgumentException("Forward Slash on Password parameter");

		/* Build GUI */
		chatWindow.getContentPane().add(sendButton);
		chatWindow.getContentPane().add(splitPane);

		sendButton.setMnemonic(KeyEvent.VK_ENTER); 
		sendButton.addActionListener(this);

		sendToMenuItem.addActionListener(this);
		saveForMenuItem.addActionListener(this);

		clearWhosInButton.addActionListener(this);
		clearWhosNotButton.addActionListener(this);

		sendToButton.addActionListener(this);
		saveForButton.addActionListener(this);
		
		clearPictureSelectionButton.addActionListener(this);
		sendPictureToAllButton.addActionListener(this);
		sendPictureOnlyToButton.addActionListener(this);
		savePictureForButton.addActionListener(this);
		
		picturesMenuItem.addActionListener(this);
		pictureList.addListSelectionListener(this);

		whosInWindow.getContentPane().add(clearWhosInButton,"North");
		whosInWindow.getContentPane().add(whosInList,       "Center");
		whosInWindow.getContentPane().add(sendToButton,     "South");

		whosNotInWindow.getContentPane().add(clearWhosNotButton,"North");
		whosNotInWindow.getContentPane().add(whosNotInList,     "Center");
		whosNotInWindow.getContentPane().add(saveForButton,     "South");
		
		topPicturePanel.add(clearPictureSelectionButton);
		topPicturePanel.add(noSelectionLabel);
		topPicturePanel.add(selectPictureLabel);		
		pictureWindow.getContentPane().add(topPicturePanel, "North");
		
		middlePicturePanel.add(pictureScrollPane);
		middlePicturePanel.add(previewPicturePanel);
		pictureWindow.getContentPane().add(middlePicturePanel, "Center");
		
		bottomPicturePanel.add(sendPictureToAllButton);
		bottomPicturePanel.add(sendPictureOnlyToButton);
		bottomPicturePanel.add(savePictureForButton);
		pictureWindow.getContentPane().add(bottomPicturePanel, "South");

		menuBar.add(privateMessageMenu);
		
		menuBar.add(picturesMenu);
		
		
		/* Extra-point GUI build */
		whosIgonoredMenuItem.addActionListener(this);
		userToBeIgnoredText.addActionListener(this);
		clearWhosIgnoredButton.addActionListener(this);
		
		whosIgnoredWindow.getContentPane().add(whosIgnored,"Center");
		whosIgnoredWindow.getContentPane().add(userToBeIgnoredText,"North");
		whosIgnoredWindow.getContentPane().add(clearWhosIgnoredButton,"South");
		
		menuBar.add(ignoredMenu);
		
		chatWindow.setJMenuBar(menuBar);
		
		/* Setting GUI objects attributes */
		splitPane.setDividerLocation(200); // 200 pixels from left (or top) 

		chatWindow.setTitle(chatName + "'s Chat Room!  "
				+ "Press alt-Enter to send.   "
				+ "Close window to leave the chat room.");
		chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		outTextArea.setEditable(false); // keep cursor out
		outTextArea.setFont(new Font("TimesRoman", Font.BOLD, 15));
		outTextArea.setText("Connecting to the chat server at " + serverAddress);

		inTextArea.setEditable(false);//initially keep cursor out
		inTextArea.setFont(new Font("TimesRoman", Font.BOLD, 15));

		privateMessageMenu.add(sendToMenuItem);
		privateMessageMenu.add(saveForMenuItem);
		
		ignoredMenu.add(whosIgonoredMenuItem);
		
		picturesMenu.add(picturesMenuItem);
		
		pictureList.setSelectionMode(0); // 0=single-select
		
		pictureWindow.setSize(400,400);
		pictureWindow.setLocation(210,50);

		whosInWindow.setSize(250,200);
		whosInWindow.setLocation(800,0);

		whosNotInWindow.setSize(250,200);
		whosNotInWindow.setLocation(800,200);
		
		whosIgnoredWindow.setSize(250,200);
		whosIgnoredWindow.setLocation(800,400);

		chatWindow.setSize(800,500);
		chatWindow.setVisible(true); 


		/* Start the connection */
		try {
			socket = new Socket(serverAddress, 6789);
			outTextArea.append("Connected to remote computer on port 2345!");
		}
		catch (UnknownHostException uhe) {
			outTextArea.append(newLine + "Invalid network address.");
			outTextArea.append(newLine + "Restart ChatRoomClient with correct network address.");
			outTextArea.append(newLine + uhe.toString());
			throw uhe; 
		}
		catch (IOException ioe) {
			outTextArea.append(newLine + "The ChatRoomServer is not responding.");
			outTextArea.append(newLine + "Either the network address is incorrect,");
			outTextArea.append(newLine + "or the port number 2345 is not correct,");
			outTextArea.append(newLine + "or the server program is simply not up!");
			outTextArea.append(newLine + ioe.toString());
			throw ioe; 
		}
		catch (Exception e) {
			outTextArea.append(newLine + "Problem encountered connecting to the ChatRoomServer");
			outTextArea.append(newLine + e.toString());
			throw e; 
		}

		/* Connected, print a message */
		outTextArea.append(newLine + "Joining " + chatName + ". (If no reply, server address is probably wrong.)");

		/* Connected, try to join in a chat room */
		try {
			objOS = new ObjectOutputStream(socket.getOutputStream());
			objOS.writeObject(chatName + "/" + password);   
			objIS = new ObjectInputStream(socket.getInputStream());
			String serverReply = (String) objIS.readObject();
			outTextArea.append(newLine + serverReply);

			if (serverReply.startsWith("Welcome")) {
				inTextArea.setEditable(true);
				inTextArea.requestFocus();
			} 
			else {
				throw new IllegalArgumentException(serverReply);
			} 

		}
		catch(Exception e) {
			outTextArea.append(newLine + "Connection terminated by the remote application."
					+ newLine + "(So it is probably not the ChatRoomServer!"
					+ newLine + "Verify remote computer address and port #"
					+ newLine + "Then restart to attempt connection again.");
			throw e; 
		}

		/* Connected and joined in a chat room, start the thread */
		new Thread(this).start();
	}

	public void actionPerformed(ActionEvent ae) 
	{ 
		if (DEBUG) {
			if (ae.getSource() == sendToMenuItem) 
				System.out.println("show who's-in list"); 

			if (ae.getSource() == saveForMenuItem) 
				System.out.println("show who's-not-in list"); 
		}
		
		if (ae.getSource() == sendToMenuItem)
			whosInWindow.setVisible(true);

		if (ae.getSource() == saveForMenuItem)
			whosNotInWindow.setVisible(true);

		if (ae.getSource() == whosIgonoredMenuItem)
			whosIgnoredWindow.setVisible(true);
		
		if (ae.getSource() == clearWhosInButton)
			whosInList.clearSelection();

		if (ae.getSource() == clearWhosNotButton)
			whosNotInList.clearSelection();
		if(ae.getSource()==picturesMenuItem)
		{
			pictureWindow.setVisible(true);
			
			int picCounter=0;
			System.out.println("Picture Menu item selected");
			//PPPPPPPPPPPPICTURES
			String localDirectoryPath= System.getProperty("user.dir"); //retrieve the path to the local directory
			File localDirectory=new File (localDirectoryPath);//File object representing the directory
			String[] fileNames= localDirectory.list(); //The list() method returns an array of file names
			
			for(String name:fileNames)
			{
				
				if(name.endsWith(".gif")||name.endsWith(".jpg")||name.endsWith(".png"))
				{
					picCounter++;
					pictureFileNames.add(name); //Adding a string to collection vector
					
				}
			}
			System.out.println("There are "+picCounter+" picture files found on your local disk: "+ pictureFileNames);
			pictureList.setListData(pictureFileNames);//put Vector elements in JList
		}
		
		if(ae.getSource()==clearPictureSelectionButton)
		{
			System.out.println("clearPictureSelectionButton is pushed.");
			pictureList.clearSelection();
			
			Graphics g = previewPicturePanel.getGraphics();
			g.clearRect(0,0,previewPicturePanel.getWidth(),previewPicturePanel.getHeight());
			
		}
		if(ae.getSource()==sendPictureToAllButton)
		{
			
			System.out.println("sendPictureToAllButton is pushed.");
			String pictureFileName = (String) pictureList.getSelectedValue();//note this is a singular version of getSelectedValues()
						
			if(pictureFileName==null)	//don't use ".equal(null)"; it won't work
			{
				noSelectionLabel.setText("PICTURE NOT SELECTED");
				noSelectionLabel.setForeground(Color.red);
				//Message box
				JOptionPane.showMessageDialog(pictureWindow,
					    "You have to select a picture!");
			}
			else
			{
				noSelectionLabel.setText("");
				System.out.println(pictureFileName+" is selected. ");
			}
			try {
				//This is where a picture is sent from client to server
				objOS.writeObject(pictureIcon);//This is to be displayed in a separate window in run()
				}
			catch(IOException ioe){}
			
		}
		if(ae.getSource()==sendPictureOnlyToButton)
		{
			System.out.println("sendPictureOnlyToButton is pushed.");
			String pictureFileName = (String) pictureList.getSelectedValue();
						
			String[] lstRecipientsList = Arrays.copyOf(whosInList.getSelectedValues(), whosInList.getSelectedValues().length, String[].class);

			List<String> recipientsList = Arrays.asList(lstRecipientsList);
			if (recipientsList.isEmpty())
			{
				noSelectionLabel.setText("RECIPIENTS NOT SELECTED");
				noSelectionLabel.setForeground(Color.red);
				//Message box
				JOptionPane.showMessageDialog(pictureWindow,
					    "You have to choose at least one present recipient!");
			}
			else
			{
				noSelectionLabel.setText("");
				System.out.println(pictureFileName+" is selected. ");
			}
			
			/*send type List<Object> to the server if it contains an ImageIcon*/
			List<Object> recipientList = new ArrayList<Object>();
			recipientList.add(pictureIcon); // add picture at front
			// copy recipients to the List of type Object
			for (String recipient : recipientsList)
			     recipientList.add(recipient);
			recipientsList = null; // for safety! (we're done with this list!)
			try {
				objOS.writeObject(recipientList);
				}
			catch(IOException ioe){}
		}
		if(ae.getSource()==savePictureForButton)
		{
			System.out.println("savePictureForButton is pushed.");
			String pictureFileName = (String) pictureList.getSelectedValue();
						
			String[] lstRecipientsList = Arrays.copyOf(whosNotInList.getSelectedValues(), whosNotInList.getSelectedValues().length, String[].class);

			List<String> recipientsList = Arrays.asList(lstRecipientsList);
			if (recipientsList.isEmpty())
			{
				
				noSelectionLabel.setText("RECIPIENTS NOT SELECTED");
				noSelectionLabel.setForeground(Color.red);
				//Message box
				JOptionPane.showMessageDialog(pictureWindow,
					    "You have to choose at least one absent recipient!");
			}
			else
			{
				
				noSelectionLabel.setText("");
				System.out.println(pictureFileName+" is selected. ");	
				
			}
			
			/*send type List<Object> to the server if it contains an ImageIcon*/
			List<Object> recipientList = new ArrayList<Object>();//NOTE the recipientList (with no s!) 
			recipientList.add(pictureIcon); // add picture at front
			// copy recipients to the List of type Object
			for (String recipient : recipientsList)
			     recipientList.add(recipient);
			recipientsList = null; // for safety! (we're done with this list!)
			try {
				objOS.writeObject(recipientList);
				}
			catch(IOException ioe){}
		}

		/* Extra-point GUI events */
		if (ae.getSource() == userToBeIgnoredText) {
			if (!lstWhosIgnored.contains(userToBeIgnoredText.getText().trim().toUpperCase())) {
				lstWhosIgnored.add(userToBeIgnoredText.getText().trim().toUpperCase());
				whosIgnored.setListData(lstWhosIgnored);
			}
		}
		
		if (ae.getSource() == clearWhosIgnoredButton) {
			for (Object obj : whosIgnored.getSelectedValues()) {
				if (lstWhosIgnored.contains(obj.toString()))
					lstWhosIgnored.remove(obj.toString());
			}
			whosIgnored.setListData(lstWhosIgnored);
		}
			
		/* Try to send a private message */
		if (ae.getSource() == sendToButton) 
		{
			String message = inTextArea.getText().trim();
			if (message.length() == 0) 
				return; 
			if (DEBUG)
				System.out.println("Sending private message: " + message); 
			String[] lstRecipientsList = Arrays.copyOf(whosInList.getSelectedValues(), whosInList.getSelectedValues().length, String[].class);

			List<String> recipientsList = Arrays.asList(lstRecipientsList);
			if (recipientsList.isEmpty())
				return;
			inTextArea.setText(""); 
			whosInList.clearSelection();

			ArrayList<String> lstAux = new ArrayList<String>(recipientsList);
			lstAux.add(0, message); 
			recipientsList = lstAux;

			try {objOS.writeObject(recipientsList);}
			catch(IOException ioe){}
		}

		/* Try to send a saved message */
		if (ae.getSource() == saveForButton) 
		{
			String message = inTextArea.getText().trim();
			if (message.length() == 0) 
				return;
			if (DEBUG)
				System.out.println("Sending private message: " + message); 
			String[] lstRecipientsList = Arrays.copyOf(whosNotInList.getSelectedValues(), whosNotInList.getSelectedValues().length, String[].class);

			List<String> recipientsList = Arrays.asList(lstRecipientsList);
			if (recipientsList.isEmpty())
				return;
			inTextArea.setText(""); 
			whosNotInList.clearSelection();

			ArrayList<String> lstAux = new ArrayList<String>(recipientsList);
			lstAux.add(0, message); 
			recipientsList = lstAux;

			try {objOS.writeObject(recipientsList);}
			catch(IOException ioe){}
		}

		/* Try to send a normal message */
		if (ae.getSource() == sendButton) {

			String chat = inTextArea.getText().trim();
			if (chat.length() == 0) return;
			if (DEBUG)
				System.out.println("Sending: " + chat); 
			try {
				objOS.writeObject(chat); 
				inTextArea.setText(""); 
				inTextArea.requestFocus(); 
			}
			catch(IOException ioe) 
			{
				inTextArea.setEditable(false);
			} 
		}
	} 
	
	public void valueChanged(ListSelectionEvent lse)
	{
		
		if (pictureList.getValueIsAdjusting()) //To ensure the user isn't in middle of selection an additional item
			return;
		if(lse.getSource()==pictureList)
		{
			String FileName = (String) pictureList.getSelectedValue();
			pictureFileName=FileName;
			String chatName=clientName; //Do you remember this "clientName" variable?
			
			if(pictureFileName==null)
				return;
			System.out.println("The picture " + pictureFileName 
	                 + " will be shown in the previewPanel.");
			
			pictureIcon = new ImageIcon(pictureFileName,
		              pictureFileName + " from " + chatName);
			Image picture=pictureIcon.getImage();			
			Graphics g=previewPicturePanel.getGraphics();			
			g.drawImage(picture,0,0,previewPicturePanel);			
			
		}
	}

	@SuppressWarnings("unchecked")
	public void run()
	{
		try {
			while(true)
			{
				Object messageFromServer = (Object)objIS.readObject();
				
				if (messageFromServer instanceof String)
				{
					synchronized (lstWhosIgnored) { /* Avoiding problems with ignored messages coming while the user change the ignored list */
						if (!lstWhosIgnored.isEmpty()) {
							String[] lstString = ((String) messageFromServer).split(" ");
							if (lstWhosIgnored.contains(lstString[0].trim()))
								continue;
						}
					}
					
					outTextArea.append(newLine + messageFromServer);
					outTextArea.setCaretPosition(outTextArea.getDocument().getLength()); 
					inTextArea.requestFocus();
					continue;
				}

				if (messageFromServer instanceof TreeSet)
				{        
					@SuppressWarnings("unchecked")
					TreeSet<String> sortedList = (TreeSet<String>) messageFromServer;
					whosNotInList.setListData(new Vector(sortedList));
					if (DEBUG)
						System.out.println("Received who's-NOT-in list from server: "
								+ messageFromServer);
					continue;
				}

				if (messageFromServer instanceof String[] )
				{
					if (DEBUG)
						System.out.print("Received who's-in list from server: "); 
					String[] whosInNames = (String[]) messageFromServer;
					whosInList.setListData(whosInNames);
					if (DEBUG) {
						for (String chatName : whosInNames)
							System.out.print( chatName + ", ");
						System.out.println("");
					}
					continue;
				}
				if(messageFromServer instanceof ImageIcon)
				{
					ImageIcon imageIcon=(ImageIcon)messageFromServer;
					
					//sending an image 
					String description = imageIcon.getDescription();
					JFrame receivedPictureWindow = new JFrame(description);
					Image image = imageIcon.getImage();
					receivedPictureWindow.setIconImage(image);//show image as icon on toolbar
					receivedPictureWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);//garbage collection on window close
					
					//To deal with resizing, which makes the JVM redraw the window
					//and make the picture disappear
					RefreshingPicturePanel rpp = new RefreshingPicturePanel(image);
					receivedPictureWindow.getContentPane().add(rpp,"Center");
					
					//The picture pop up window dimension
					int imageHeight = image.getHeight(rpp);
					int imageWidth  = image.getWidth(rpp);
					receivedPictureWindow.setSize(imageWidth,imageHeight+30);
					receivedPictureWindow.setLocation(100,100);
					receivedPictureWindow.setVisible(true);
					
					continue;
				}
				//if (DEBUG)
				System.out.println("Unrecognized object received: " + messageFromServer);
				outTextArea.append(newLine+ " Received unrecognized object: "+ messageFromServer.getClass().getName());
				//throw new IllegalArgumentException("Unexpected object type received from server"); // go to catch
			}
		}
		catch(IOException ioe)
		{
			outTextArea.append(newLine+"Connection to the server has been lost.");
			outTextArea.append(newLine+"Must restart ChatRoomClient to reconnect.");
		}
		catch (Exception e) {
			outTextArea.append(newLine+"Connection to the server has been lost.");
			outTextArea.append(newLine+"Must restart ChatRoomClient to reconnect.");
		}
	}
}