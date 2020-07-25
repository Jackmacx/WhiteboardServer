package server;

/*********************************************************************
* Author:     Jack Macumber (817548)

Implements code for Assignment 2 of Distributed Systems
*********************************************************************/

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import whiteboard.Client;

public class Server {
	private static int port;
	private static String ip;
	private static String username;
	
	// Request counter
	// private static int requests = 0;
	
	public static void main(String[] args) {
		// Handle Command Line Arguments
		ip = args[0];
		port = Integer.parseInt(args[1]);
		username = args[2];
		
		// Start Server Socket
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		try(ServerSocket server = factory.createServerSocket(port)) {
			Peer.hostname = username;
			Client.startGUI(port, ip, username, true);
			
			while(true) {
				Socket client = server.accept();
				// requests++;		
				//System.out.println("SERVER: New Connection");
				
				// Start a new thread for a connection
				Thread t = new Thread(() -> serveClient(client));
				t.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private static void serveClient(Socket client) {
		try(Socket clientSocket = client)  {
			// Output and Input Stream
			DataInputStream input = new DataInputStream(clientSocket.getInputStream());
			DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
			
			// Add new Peer
			String username = input.readUTF();
			Peer peer;
			try {
				peer = new Peer(username, output);
			} catch (Exception e) {
				JSONObject fail = new JSONObject();
				fail.put("command", "kick");
				fail.put("text", "Username Taken");
				output.writeUTF(fail.toString());
				output.flush();
				return;
			}
			
			// Stay open to handle requests
			while (true) {
				// Read input
				String msg;
				try {
			    	msg = input.readUTF();
				} catch (EOFException e) {
					// Nothing to read at the moment
					continue;
				}
			    //System.out.println("SERVER: Got a Request: " + msg);
			    
				// Convert input to json
			    JSONObject request = new JSONObject();
		    	try {
		    		JSONParser parser = new JSONParser();
		    		request = (JSONObject) parser.parse(msg);
		    	}  catch (Exception e){
		    		e.printStackTrace();
				}
		    	
		    	//Handle messages
		    	switch ((String) request.get("command")) {
		    		case "full": if (peer.accepted) Peer.broadcast(request.toString()); break;
		    		case "draw": if (peer.accepted) Peer.broadcast(request.toString()); break;
		    		case "accept": Peer.accept((String) request.get("username")); break;
		    		case "kick": Peer.kick(request); break;
		    		case "exit": Peer.close(); break;
		    	}
			}
		} 
		catch (IOException e) {
			// Connection Ended
		}  catch (Exception e) {
			// Unexpected error
			e.printStackTrace();
		}
	}

}
