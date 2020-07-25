package whiteboard;

/*********************************************************************
* Author:     Jack Macumber (817548)

Implements code for Assignment 2 of Distributed Systems
*********************************************************************/

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.json.simple.JSONObject;

public class Board extends JComponent {
	private static final long serialVersionUID = 1L;

	public Image image;
	private Graphics2D g2;
	private int sx, sy, ex, ey;
	private String str = null;
	
	private KeyRecorder kr = new KeyRecorder();

	private Window w;
	
	// Handle Typing for Text Drawing
	private class KeyRecorder extends KeyAdapter {
		@SuppressWarnings("unchecked")
		@Override
		public void keyTyped(KeyEvent e) {
			if ("Text".equals(w.toolGroup.getSelection().getActionCommand())) {
				if (e.getKeyChar() == (char)10) {
					int size = (int) w.spinnerSize.getValue();
					Color color = Color.BLACK;
					
					JSONObject request = new JSONObject();
					request.put("command", "draw");
					request.put("size", size);
					request.put("color", color.getRGB());
				    request.put("style", "text");
				    request.put("x1", ex);
					request.put("y1", ey);
					request.put("text", str);
					w.client.send(request);
					
					str = null;
				} else if (e.getKeyChar() == (char)8) {
					if (str.length() > 1) {
						str = str.substring(0, str.length()-1);
					}
				} else {
					str += e.getKeyChar();
				}
			}
		}
	}
	
	public Board(Window w) {
		this.w = w;
		Board b = this;
		b.setFocusable(true);
		b.addKeyListener(kr);
		
		// Begin Draw Request
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				sx = e.getX();
				sy = e.getY();
				
				if ("Text".equals(w.toolGroup.getSelection().getActionCommand())) {
					b.str = "";
					b.requestFocus();
				} else {
					b.str = null;
				}
			}
		});
		
//		addMouseMotionListener(new MouseMotionAdapter() {
//			public void mouseDragged(MouseEvent e) {
//				ex = e.getX();
//				ey = e.getY();
//			}			
//		});
		
		// Send Draw Requests to Server
		addMouseListener(new MouseAdapter() {
			@SuppressWarnings("unchecked")
			public void mouseReleased(MouseEvent e) {
				ex = e.getX();
				ey = e.getY();
				
				if (g2 != null) {		
					int size = (int) w.spinnerSize.getValue();
					Color color = Color.BLACK;
					
					JSONObject request = new JSONObject();
					request.put("command", "draw");
					request.put("size", size);
					request.put("color", color.getRGB());
					switch (w.toolGroup.getSelection().getActionCommand()) {
						case "Line":
							request.put("style", "line");
							request.put("x1", sx);
							request.put("y1", sy);
							request.put("x2", ex);
							request.put("y2", ey);
							w.client.send(request);
						break;
						case "Circle":
							request.put("style", "circle");
							request.put("x1", sx);
							request.put("y1", sy);
							request.put("x2", ex);
							request.put("y2", ey);
							w.client.send(request);
						break;
						case "Rectangle": 
							request.put("style", "rectangle");
							request.put("x1", sx);
							request.put("y1", sy);
							request.put("x2", ex);
							request.put("y2", ey);
							w.client.send(request);
						break;
						case "Text": break;
						default: System.out.println("Unexpected Default"); break;
					}
				}
			}
		});
	}
	
	// Draw Line
	public void drawLine(int x1, int y1, int x2, int y2, int thickness, Color c) {
		g2.setColor(c);
		g2.setStroke(new BasicStroke(thickness));
		g2.drawLine(x1, y1, x2, y2);
		repaint();
	}
	
	// Draw Circle
	public void drawCircle(int x1, int y1, int x2, int y2, int thickness, Color c) {
		int xmin = Math.min(x1, x2);		
		int ymin = Math.min(y1, y2);
		int xmax = Math.max(x1, x2);
		int ymax = Math.max(y1, y2); 
		
		g2.setColor(c);
		g2.setStroke(new BasicStroke(thickness));
		g2.drawOval(xmin, ymin, xmax-xmin, ymax-ymin);
		repaint();
	}
	
	// Draw Rectangle
	public void drawRectangle(int x1, int y1, int x2, int y2, int thickness, Color c) {
		int xmin = Math.min(x1, x2);		
		int ymin = Math.min(y1, y2);
		int xmax = Math.max(x1, x2);
		int ymax = Math.max(y1, y2); 
		
		g2.setColor(c);
		g2.setStroke(new BasicStroke(thickness));
		g2.drawRect(xmin, ymin, xmax-xmin, ymax-ymin);
		repaint();
	}
	
	// Draw Text
	public void drawText(String s, int x, int y, int size, Color c) {
		g2.setColor(c);
		Font current = g2.getFont();
		Font fontSized = current.deriveFont(size * 5.0f);
		g2.setFont(fontSized);
		g2.drawString(s, x, y);
		repaint();
	}
	
	// Draw Image from Network
	public void drawImage(String b64) {
		try {
			Image load = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(b64)));
			g2.drawImage(load, 0, 0, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		repaint();
	}
	
	// Draw Loaded Image from File
	public void drawFile(File file) {
		try {
			Image load = ImageIO.read(file);
			g2.drawImage(load, 0, 0, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		repaint();
	}
	
	// Handles JComponent Drawing
	@Override
	protected void paintComponent(Graphics g) {
		if (image == null) {
			image = createImage(getSize().width, getSize().height);
			g2 = (Graphics2D) image.getGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			clear();
		}
		g.drawImage(image, 0, 0, null);
	}
	
	// Clear board
	public void clear() {
		g2.setPaint(Color.white);
		g2.fillRect(0, 0, getSize().width, getSize().height);
		repaint();
	}	
}
