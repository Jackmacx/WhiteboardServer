package whiteboard;

/*********************************************************************
* Author:     Jack Macumber (817548)

Implements code for Assignment 2 of Distributed Systems
*********************************************************************/

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;

import java.awt.Color;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.swing.JToggleButton;
import java.awt.Button;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.awt.event.ActionEvent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Window {

	protected JFrame frame;

	public ButtonGroup toolGroup;
	public JSpinner spinnerSize;
	private Board board;
	private DefaultListModel<String> mlist;
	private JList<String> listUsers;
	private File file=null;
	
	protected Client client;

	public Window(Client client) {
		this.client = client;
		initialize();
	}

	private void initialize() {	
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 643, 404);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		// Handle Window Closing
		frame.addWindowListener(new WindowAdapter() {
	        @SuppressWarnings("unchecked")
			public void windowClosing(WindowEvent e) {
	        	if (client.isHost) {
	        		JSONObject command = new JSONObject();
	        		command.put("command", "exit");
	        		client.send(command);
	        	}
	        };
		});
		
		// Panel for the Actual Whiteboard
		JPanel panel = new JPanel();
		panel.setBounds(126, 11, 501, 333);
		panel.setLayout(new BorderLayout());
		frame.getContentPane().add(panel);
		board = new Board(this);
		panel.add(board, BorderLayout.CENTER);
		
		JPanel tools = new JPanel();
		tools.setBounds(10, 11, 95, 333);
		frame.getContentPane().add(tools);
		tools.setLayout(null);
		
		// User List
		mlist = new DefaultListModel<String>();
		listUsers = new JList<String>(mlist);
		listUsers.setBounds(10, 124, 75, 171);
		tools.add(listUsers);
		
		// Size Selection
		spinnerSize = new JSpinner();
		spinnerSize.setModel(new SpinnerNumberModel(1, 1, 200, 1));
		spinnerSize.setBounds(10, 93, 75, 20);
		tools.add(spinnerSize);
		
		// Drawing Tools
		toolGroup = new ButtonGroup();
		
		JToggleButton tbLine = new JToggleButton("Line");
		tbLine.setSelected(true);
		tbLine.setBounds(1, 0, 93, 20);
		
		JToggleButton tbCircle = new JToggleButton("Circle");
		tbCircle.setBounds(1, 21, 93, 20);
		
		JToggleButton tbRectangle = new JToggleButton("Rectangle");
		tbRectangle.setBounds(1, 42, 93, 20);
		
		JToggleButton tbText = new JToggleButton("Text");
		tbText.setBounds(1, 62, 94, 20);
		
		tools.add(tbLine);
		tools.add(tbCircle);
		tools.add(tbRectangle);
		tools.add(tbText);
		
		tbLine.setActionCommand("Line");
		tbCircle.setActionCommand("Circle");
		tbRectangle.setActionCommand("Rectangle");
		tbText.setActionCommand("Text");
		
		toolGroup.add(tbLine);
		toolGroup.add(tbCircle);
		toolGroup.add(tbRectangle);
		toolGroup.add(tbText);
		
		// Kick Button
		Button button = new Button("Kick");
		button.setEnabled(client.isHost);
		button.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent e) {
				String username = listUsers.getSelectedValue();
				if (username != null) {
					JSONObject command = new JSONObject();
					command.put("command", "kick");
					command.put("username", username);
					command.put("text", "You have been kicked");
					client.send(command);
				}
			}
		});
		button.setBounds(10, 301, 75, 22);
		tools.add(button);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		// New Image
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!client.isHost) return;
				board.clear();
				file = null;
				// Tell everyone
				sendImage();
			}
		});
		mntmNew.setEnabled(client.isHost);
		menuBar.add(mntmNew);
		
		// Open Image
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!client.isHost) return;
				final JFileChooser fc = new JFileChooser();
				int status = fc.showOpenDialog(frame);
		        if (status == JFileChooser.APPROVE_OPTION) {
		            file = fc.getSelectedFile();
		            board.drawFile(file);
		        }
		        // Tell everyone
		        sendImage();
			}
		});
		mntmOpen.setEnabled(client.isHost);
		menuBar.add(mntmOpen);
		
		// Save Image
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!client.isHost) return;
				if (file != null) {
				    try {
				    	ImageIO.write((RenderedImage) board.image, "png", file);
				    } catch (IOException ex) {
				    	ex.printStackTrace();
		    		}
				} else {
					// No file, so ask for one
					save();
				}
			}
		});
		mntmSave.setEnabled(client.isHost);
		menuBar.add(mntmSave);
		
		// Save Image As
		JMenuItem mntmSaveAs = new JMenuItem("Save As");
		mntmSaveAs.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!client.isHost) return;
				save();
			}
		});
		mntmSaveAs.setEnabled(client.isHost);
		menuBar.add(mntmSaveAs);
		
		// Close
		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent e) {
				// Tell everyone
	        	if (client.isHost) {
	        		JSONObject command = new JSONObject();
	        		command.put("command", "exit");
	        		client.send(command);
	        	}
				System.exit(0);
			}
		});
		menuBar.add(mntmClose);
	}
	
	// Select file to save to
	private void save() {
		JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter ext = new FileNameExtensionFilter("PNG", "png");
		fc.setFileFilter(ext);
	    int status = fc.showSaveDialog(frame);
	    if (status == JFileChooser.APPROVE_OPTION) {
		    file = fc.getSelectedFile();
		    try {
		    	ImageIO.write((RenderedImage) board.image, "png", file);
		    } catch (IOException ex) {
		    	ex.printStackTrace();
    		}
	    }
	}
	
	// Pass Draw Commands to Board
	public void draw(JSONObject action) {		
		switch ((String) action.get("style")) {
		case "line": 
			board.drawLine(
				((Number) action.get("x1")).intValue(), 
				((Number) action.get("y1")).intValue(), 
				((Number) action.get("x2")).intValue(), 
				((Number) action.get("y2")).intValue(), 
				((Number) action.get("size")).intValue(), 
				new Color(((Number) action.get("color")).intValue()));
		break;
		case "circle":
			board.drawCircle(
				((Number) action.get("x1")).intValue(), 
				((Number) action.get("y1")).intValue(), 
				((Number) action.get("x2")).intValue(), 
				((Number) action.get("y2")).intValue(), 
				((Number) action.get("size")).intValue(), 
				new Color(((Number) action.get("color")).intValue()));
		break;
		case "rectangle":
			board.drawRectangle(
				((Number) action.get("x1")).intValue(), 
				((Number) action.get("y1")).intValue(), 
				((Number) action.get("x2")).intValue(), 
				((Number) action.get("y2")).intValue(), 
				((Number) action.get("size")).intValue(), 
				new Color(((Number) action.get("color")).intValue()));
		break;
		case "text":
			board.drawText(
				(String) action.get("text"),
				((Number) action.get("x1")).intValue(), 
				((Number) action.get("y1")).intValue(), 
				((Number) action.get("size")).intValue(), 
				new Color(((Number) action.get("color")).intValue()));
		break;
		}
	}

	// Connection to Server Lost
	public void connectionLost() {
		JOptionPane.showMessageDialog(frame, "Connection to Server Lost");
		System.exit(0);
	}
	
	// Display Kicked Message
	public void kicked(JSONObject message) {
		JOptionPane.showMessageDialog(frame, (String) message.get("text"));
		System.exit(0);
	}

	// Handle Join Requests
	@SuppressWarnings("unchecked")
	public void joinRequest(JSONObject message) {
		Object[] options = {"Allow", "Deny"};
		
		String username = (String) message.get("username");
		
		int n = JOptionPane.showOptionDialog(
					frame, 
					username + " would like to join",
					"Join Request",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					options,
					options[0]);

		JSONObject command = new JSONObject();
		if (n==0) {
			command.put("command","accept");
			command.put("username", username);
		} else {
			command.put("command","kick");
			command.put("username", username);
			command.put("text", "Denied Access");
		}
		
		client.send(command);
	}

	// Update Displayed User List
	public void updateUsers(JSONObject message) {
		listUsers.clearSelection();
		mlist.clear();
		
		JSONArray list = (JSONArray) message.get("users");
		for (Object username: list) {
			mlist.addElement((String) username);
		}
	}

	// Send Image to Peers
	@SuppressWarnings("unchecked")
	public void sendImage() {
		try {
			ByteArrayOutputStream image = new ByteArrayOutputStream();
			JSONObject command = new JSONObject();
			command.put("command","full");
			ImageIO.write((RenderedImage) board.image, "png", Base64.getEncoder().wrap(image));
			command.put("b64", image.toString());
			client.send(command);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Draw Received Image
	public void drawImage(JSONObject message) {
		board.drawImage((String) message.get("b64"));
	}

}
