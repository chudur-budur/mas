package sim.app.horde;

import java.io.*;
import java.util.*;
import java.net.*;

public class Harness
{
	public final static int PORT = 6000;
	public static final String STOP = "Stop";
	public static final int NUM_FEATURES = 4;
	public static PrintStream toSocket = null;

	public static void main(String[] args)
	{
		try
		{
			ServerSocket server = new ServerSocket(PORT);
			Socket sock = server.accept();
			final InputStream i = sock.getInputStream();
			final OutputStream o = sock.getOutputStream();

			// build the input stream to read incoming features
			new Thread(new Runnable()
			{
				public void run()
				{
					Scanner scan = new Scanner(i);
					while(true)
					{
						System.err.println("-------");
						for(int i =0; i < NUM_FEATURES; i++)
							System.err.println(scan.nextDouble());
					}
				}
			}).start();

			// build the output stream
			toSocket = new PrintStream(o);
			Scanner scan2 = new Scanner(System.in);
			while(true)
			{
				toSocket.println(scan2.nextLine());
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("FAILED TO OPEN AND SET UP SOCKET", e);
		}
	}
}