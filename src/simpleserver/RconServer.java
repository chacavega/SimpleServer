package simpleserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

public class RconServer implements Runnable {
	private Server parent;
	private DatagramSocket rconSocket;
	byte[] receive = new byte[4096 + 4 * 3 + 2];
	boolean udp;
	public RconServer(Server parent, boolean useUDP) {
		this.parent=parent;
		udp=useUDP;
	}
	
	public void distribute(DatagramPacket p) throws IOException {
		synchronized(parent.rcons) {
			for(Iterator<Rcon> itr = parent.rcons.iterator();itr.hasNext();) {
				Rcon i = itr.next();
				if (i.getName().equals(p.getAddress().getHostAddress())) {
					i.handle(p);
					return;
				}
			}
			Rcon r = new RconUDP(p,parent);
			parent.rcons.add(r);
		}
	}
	
	public void distribute(Socket p) throws IOException {
		synchronized(parent.rcons) {
			for(Iterator<Rcon> itr = parent.rcons.iterator();itr.hasNext();) {
				Rcon i = itr.next();
				if (i.getName().equals(p.getInetAddress().getHostAddress())) {
					i.handle(p);
					return;
				}
			}
			Rcon r = new RconTCP(p,parent);
			parent.rcons.add(r);
		}
	}
	
	public void run() {
		// TODO Auto-generated method stub
		if (!udp)
			try {
	            parent.rconSocket = new ServerSocket(parent.options.rconPort);
	            System.out.println("Opened RCON on port: " + parent.options.rconPort +"!");
	        } 
	        catch (IOException e) {
	            System.out.println("Could not listen on port " + parent.options.rconPort +"!\r\nIs it already in use? RCON is not available!");
	            return;
	        }
        else
		    try {
				rconSocket = new DatagramSocket(parent.options.rconPort);
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		
        while (!Thread.interrupted()) {
        	
        	if (!udp)
	        	try {
					Socket s = parent.rconSocket.accept();
					distribute(s);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			else {
        	
	        	DatagramPacket receivePacket=null;
	        	try {
	        		receivePacket = new DatagramPacket(receive, receive.length);
	        		rconSocket.receive(receivePacket);
	        		distribute(receivePacket);
	        	}
	            catch (Exception e) {
	        		e.printStackTrace();
	        		if (receivePacket!=null)
						System.out.println("[SimpleServer] RCON failed for " + receivePacket.getAddress().getHostAddress() + "! " + e.getCause());
	            }
			}
        }
        if (!udp)
	        try {
	        	parent.rconSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				if (!parent.isRestarting)
					e.printStackTrace();
			}
		else
			rconSocket.close();
	}
	
}