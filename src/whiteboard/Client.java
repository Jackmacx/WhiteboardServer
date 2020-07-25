package whiteboard;

/*********************************************************************
* Author:     Jack Macumber (817548)

Implements code for Assignment 2 of Distributed Systems
*********************************************************************/

import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Client {
	private Window window;
	public boolean isHost=false;
	
	public static void main(String[] args) {
		startGUI(Integer.parseInt(args[1]), args[0], args[2], false);
	}
	
	public static void startGUI(int port, String ip, String username, boolean host) {
		EventQueue.invokeLater(new Runnable() {
			Client client = new Client(port, ip, username, host);
			
			// Start UI
			public void run() {
				try {
					client.window = new Window(client);
					client.window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private String ip;
	private int port;
	private Socket socket=null;
	private DataInputStream input;
	private DataOutputStream output;
	
	public Client(int port, String ip, String username, boolean host){
		this.port = port;
		this.ip = ip;
		this.isHost = host;
		
		makeSocketConnection(username);
	}
		
	private void makeSocketConnection(String username) {
		try {
			socket = new Socket(ip, port);
			// Output and Input Stream
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
			
			output.writeUTF(username);
			output.flush();
			
			Thread t = new Thread(() -> listen());
			t.start();
		} catch (IOException e) {
			// Failed
		}
	}
	
	synchronized public void send(JSONObject action) {    
		//System.out.println("Sending Request: " + action.toString());
		try {
			// Send Request to Server
			output.writeUTF(action.toString());
		    output.flush();
		} catch (IOException e) {
			// Connection has been Lost
			window.connectionLost();
		}
	}
	
	public void listen() {
		try {
			// Stay open to handle communication
			while (true) {
				// Read input
				String msg;
				try {
			    	msg = input.readUTF();
				} catch (EOFException e) {
					// Nothing to read at the moment
					continue;
				}
				
				// Convert input to json
			    JSONObject message = new JSONObject();
		    	try {
		    		JSONParser parser = new JSONParser();
		    		message = (JSONObject) parser.parse(msg);
		    	}  catch (Exception e){
		    		e.printStackTrace();
				}
		    	//System.out.println("Recieved a Message: " + message.toString());
		    	
		    	//Handle messages
		    	switch ((String) message.get("command")) {
		    		case "join": window.joinRequest(message); break;
		    		case "list": 
		    			window.updateUsers(message);
		    			if (isHost) window.sendImage();
		    		break;
		    		case "full": window.drawImage(message); break;
		    		case "draw": window.draw(message); break;
		    		case "kick": 
		    			if (window == null) {
		    				System.out.println(message.get("text"));
		    				System.exit(0);
		    			} else {
		    				window.kicked(message);
		    			}
		    		break;
		    	}
			}
		} catch (IOException e) {
			// Connection Ended
		}  catch (Exception e) {
			// Unexpected error
			e.printStackTrace();
		}
	}
}
