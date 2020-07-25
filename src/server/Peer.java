package server;

/*********************************************************************
* Author:     Jack Macumber (817548)

Implements code for Assignment 2 of Distributed Systems
*********************************************************************/

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Peer {
	private static ArrayList<Peer> peers = new ArrayList<Peer>();
	private static ArrayList<Peer> dcs = new ArrayList<Peer>();
	public static Peer host;
	public static String hostname;
	
	private String username;
	private DataOutputStream output;
	protected boolean accepted = false;
	
	public Peer(String username, DataOutputStream output) throws Exception {
		//Ensure Usernames are Unique
		if (contains(username)) {
			throw new Exception("Username Taken");
		}
		this.username = username;
		this.output = output;
		newpeer(this);
	}
	
	// Handle new Peer
	@SuppressWarnings("unchecked")
	private synchronized static void newpeer(Peer peer) {
		peers.add(peer);
		// Host Connected
		if (peer.username.equals(hostname)) {
			peer.accepted = true;
			host = peer;
			//System.out.println("Host connected");
		} else {
		// Peer Connected
			JSONObject command = new JSONObject();
			command.put("command", "join");
			command.put("username", peer.username);
			toHost(command.toString());
		}
	}
	
	// Check if Username exists in Peers
	public synchronized static boolean contains(String username) {
		for (Peer peer: peers) {
			if (peer.username.equals(username)) {
				return true;
			}
		}
		return false;
	}
	
	// Remove a Peer
	public synchronized static void remove(String username) {
		for (Peer peer: peers) {
			if (peer.username.equals(username)) {
				peers.remove(peer);
				break;
			}
		}
		
		broadcastUsers();
	}

	
	// Send a Message to all Accepted Peer
	public synchronized static void broadcast(String message) {	
		for (Peer peer: peers) {
			if (!peer.accepted) continue;
			
			try {
				peer.output.writeUTF(message);
				peer.output.flush();
			} catch (SocketException e) {
				dcs.add(peer);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		updateDcs();
	}
	
	// Kick a User
	public synchronized static void kick(JSONObject message) {
		String username = (String) message.get("username");
		
		for (Peer peer: peers) {
			if (peer.username.equals(username)) {
				try {
					peer.output.writeUTF(message.toString());
					peer.output.flush();
				} catch (SocketException e) {
					dcs.add(peer);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		
		remove(username);
	}
	
	// Broadcast Current Accepted Users
	@SuppressWarnings("unchecked")
	public synchronized static void broadcastUsers() {

		JSONObject command = new JSONObject();
		command.put("command", "list");
		
		JSONArray userlist = new JSONArray();
		for (Peer peer: peers) {
			if (!peer.accepted) continue;
		
			userlist.add(peer.username);
		}
		command.put("users", userlist);
		
		broadcast(command.toString());
	}

	// Removed Disconnected Users from Peers
	private synchronized static void updateDcs() {
		if (dcs.size() > 0) {
			for (Peer peer: dcs) {
				peers.remove(peer);
			}
			dcs.clear();
			broadcastUsers();
		}
	}

	// Accept User
	public static void accept(String username) {
		for (Peer peer: peers) {
			if (peer.username.equals(username)) {
				peer.accepted = true;
				break;
			}
		}
		broadcastUsers();
	}
	
	// Message Host
	public static void toHost(String message) {
		try {
			host.output.writeUTF(message);
			host.output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Tell Everyone Server is Closing
	@SuppressWarnings("unchecked")
	public static void close() {
		JSONObject command = new JSONObject();
		command.put("command", "kick");
		command.put("text", "Server Closed");
		broadcast(command.toString());
	}

}
