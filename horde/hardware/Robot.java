package sim.app.horde.hardware;
import java.io.*;
import java.text.*;
import java.net.*;


/**
    General class for controlling Activmedia (MobileRobots) Pioneer and Amigobot robots.
<br>Copyright 2007 by Sean Luke, Liviu Panait, and George Mason University.
<br>Released under the Academic Free License, version 3.0, which is at the end of this file.

<p> This implements much of the basic API used in Pioneer robots.  Consult the Pioneer manual
    for more details.  You connect to the robot via this class in one of three ways:

    <p><ul>
    <li>Over a network socket.
    <li>Over some Input/Output stream pair
    <li>Directly to a serial port (using RobotSerial.java as a subclass)
    </ul>

<p>Once you have instantiated this class, you connect it with the connect() method.  Then you
start sending commands to the robot, and are (soon) able to receive sensor data from the robot.
The class is generally silent unless you turn on verbose mode, at which point you get a bunch
of sensor packet data back, as well as connection and error messages.  You disconnect the robot
with disconnect() -- we'd suggest throwing away the instance at that point and creating a new Robot
if you wish to reconnect, as it closes the underlying streams.

<-- This was due to mistaken 0x10->0x13 translation in Linux's ioctl
<p>We're finding a bug in Linux boxes using the Prolific USB->Serial device; often packets are
off by 3 in their sensor data.  As a (horrific) workaround, you an make the checksum lenient by that
much.
-->
 */

public class Robot
{
	public static final double YPOS_CONVERSION = 0.5083;
	public static final double XPOS_CONVERSION = 0.5083;
	public static final double THPOS_CONVERSION = 0.001534;
	public static final double LVEL_CONVERSION = 0.6154;
	public static final double RVEL_CONVERSION = 0.6154;
	public static final double CONTROL_CONVERSION = 0.001534;
	public static final double SONAR_CONVERSION = 0.555;

	public static final int POSITIVE_INTEGER = 0x3B;
	public static final int NEGATIVE_INTEGER = 0x1B;
	public static final int STRING = 0x2B;


	/// VERBOSITY.  Is the driver chatty about what's going on?
	private boolean verbose = false;
	private Object verboseLock = new Object();
	/** Set the driver to verbosely print out state sensor results and motor requests. */
	public void setVerbose(boolean val)
	{
		synchronized(verboseLock)
		{
			verbose = val;
		}
	}
	/** Get whether or not the driver is verbosely printing out state sensor results and motor requests. */
	public boolean isVerbose()
	{
		synchronized(verboseLock)
		{
			return verbose;
		}
	}

	// checksum lenience -- helps deal with problems in linux serial usb drivers
//    private boolean checksumLenient = false;
//
//    private Object checksumLock = new Object();
	/* Set the driver to be lenient in permitting checksums off by 3 bytes -- which permits *some* usage of broken Linux serial drivers.
	True by default. */
//    public void setChecksumLenient(boolean val) { synchronized(checksumLock) { checksumLenient = val; } }
	/* Returns whether or not the code permits checksums off by 3 bytes -- permits *some* usage of broken Linux serial drivers.  True by default. */
//    public boolean isChecksumLenient() { synchronized(checksumLock) { return checksumLenient; } }


	private boolean mobileSim = false;
	private Object mobileSimLock = new Object();
	/** Set the driver to connect with a MobileSim instance instead of an actual robot. */
	public void setMobileSim(boolean val)
	{
		synchronized(mobileSimLock)
		{
			mobileSim = val;
		}
	}
	/** Get whether or not the driver is configured to connect to a MobileSim instance instead of an actual robot. */
	public boolean isMobileSim()
	{
		synchronized(mobileSimLock)
		{
			return mobileSim;
		}
	}



	/** The communication packet header.  All Pioneer robot packets begin with this header. */
	protected final static byte[] HEADER = { (byte)(0xFA), (byte)(0xFB) };

	private OutputStream out;
	private InputStream in;
	private Socket sock;

	private boolean working = true;
	private Thread beatThread;
	private Thread readInfoThread;

	private NumberFormat format = NumberFormat.getInstance();

	// data from robot
	private Object readLock = new Object();
	private boolean validData = false;
	private boolean motorEngaged;
	private double xpos, ypos; // in millimeters
	private double orientation; // in radians
	private double leftWheelVelocity; // in mm/sec
	private double rightWheelVelocity; // in mm/sec
	private int battery;
	private boolean leftWheelStallIndicator;
	private boolean rightWheelStallIndicator;
	private double control; // server's angular position servo
	private double sonars[] = new double[32]; // in mm
	private double _sonars[] = new double[32];  // temporary usage
	private int lastSonarTime[] = new int[32];
	private int _lastSonarTime[] = new int[32]; // temporary usage
	private int currentSonarTime = 0;

	private String robotName;
	private String robotClass;
	private String robotSubclass;

	/** Returns true if we've received at least one valid sensor packet from the robot so far.  Otherwise, all the sensor functions return garbage. */
	public boolean isValidData()
	{
		synchronized(readLock)
		{
			return validData;
		}
	}

	/** Returns true if the motor is engaged.  This information will not be valid until validData is true.  */
	public boolean isMotorEngaged()
	{
		synchronized(readLock)
		{
			return motorEngaged;
		}
	}

	/** Returns the X coordinate of the center of the robot, as far as the robot believes.
	This information will not be valid until validData is true.
	The ARCOS documentation claims the raw value provided by the robot is in millimeters.
	However, this appears to be incorrect.  We are multiplying by XPOS_CONVERSION (0.5083)
	to get to millimeters -- it may have to do with the wheel diameter.  Help here would be welcome.*/
	public double getXPos()
	{
		synchronized(readLock)
		{
			return xpos;
		}
	}

	/** Returns the Y coordinate of the center of the robot, as far as the robot believes.
	This information will not be valid until validData is true.
	The ARCOS documentation claims the raw value provided by the robot is in millimeters.
	However, this appears to be incorrect.  We are multiplying by YPOS_CONVERSION (0.5083)
	to get to millimeters -- it may have to do with the wheel diameter.  Help here would be welcome.*/
	public double getYPos()
	{
		synchronized(readLock)
		{
			return ypos;
		}
	}

	/** Returns the THETA (rotation) of the center of the robot, as far as the robot believes.
	This information will not be valid until validData is true.
	The ARCOS documentation claims the raw value provided by the robot is 'angular units',
	with 0.001534 radians per 'angular unit'.  So we are multiplying by THPOS_CONVERSION (0.001534)
	to get to radians.  Help here would be welcome if this appears to be incorrect. */
	public double getOrientation()
	{
		synchronized(readLock)
		{
			return orientation;
		}
	}

	/** Returns the left wheel velocity in millimeters per second.
		This information will not be valid until validData is true.
	The ARCOS documentation claims the raw value provided by the robot is in mm/sec.
	However, this appears to be incorrect.  We are multiplying by LVEL_CONVERSION (0.6154)
	to get to mm/sec -- it may have to do with the wheel diameter.  Help here would be welcome.*/
	public double getLeftWheelVelocity()
	{
		synchronized(readLock)
		{
			return leftWheelVelocity;
		}
	}

	/** Returns the right wheel velocity in millimeters per second.
		This information will not be valid until validData is true.
	The ARCOS documentation claims the raw value provided by the robot is in mm/sec.
	However, this appears to be incorrect.  We are multiplying by RVEL_CONVERSION (0.6154)
	to get to mm/sec -- it may have to do with the wheel diameter.  Help here would be welcome.*/
	public double getRightWheelVelocity()
	{
		synchronized(readLock)
		{
			return rightWheelVelocity;
		}
	}

	/** Returns the battery charge in tenths of volts.  */
	public int getBatteryStatus()
	{
		synchronized(readLock)
		{
			return battery;
		}
	}

	/** Returns true if the left wheel is being blocked and thus unable to move.  */
	public boolean isLeftWheelStalled()
	{
		synchronized(readLock)
		{
			return leftWheelStallIndicator;
		}
	}

	/** Returns true if the right wheel is being blocked and thus unable to move.  */
	public boolean isRightWheelStalled()
	{
		synchronized(readLock)
		{
			return rightWheelStallIndicator;
		}
	}

	/** Returns the current value of the server's angular position servo, in degrees (unused on our robots).  */
	public double getControlServo()
	{
		synchronized(readLock)
		{
			return control;
		}
	}

	/** Returns the last value of sonar #index.  The ARCOS documentation claims that the value is in millimeters
	away from the sonar.  However, this appears to be incorrect.  We are multiplying by SONAR_CONVERSION (0.555)
	to get to millimeters.
	Note that sonars come in different batches, so you may not receive all of them
	in one time tick.  Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo. */
	public double getSonar(int index)
	{
		synchronized(readLock)
		{
			return sonars[index];
		}
	}

	/** Returns the last time tick that information for sonar #index arrived.  Note that sonars come in different batches, so you may not receive all of them
	in one time tick.  Each time any sonar batch arrives, the time tick is increased; this can thus give you an idea
	as whether your sonar data is really old (if it's older than 8, you've got old data -- older than 32 say, you've got REALLY old data).
	Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo. */
	public int getLastSonarTime(int index)
	{
		synchronized(readLock)
		{
			return lastSonarTime[index];
		}
	}

	/** Returns the current time tick to compare with past time ticks for sonar. */
	public int getCurrentSonarTime()
	{
		synchronized(readLock)
		{
			return currentSonarTime;
		}
	}

	/** Returns all the sonars values.  The ARCOS documentation claims that the values are in millimeters
	away from the sonar.  However, this appears to be incorrect.  We are multiplying by SONAR_CONVERSION (0.555)
	to get to millimeters.  If you pass in NULL or an array differently sized, you'll get back a new array;
	else the array passed in will be filled in and returned, allowing you to save allocations.
	Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo.
	*/
	public double[] getSonars(double[] maybeReuse)
	{
		if (maybeReuse == null || maybeReuse.length != sonars.length)
			maybeReuse = new double[sonars.length];
		synchronized(readLock)
		{
			System.arraycopy(sonars,0,maybeReuse,0,sonars.length);
		}
		return maybeReuse;
	}

	/** Returns all the sonars values.  The ARCOS documentation claims that the values are in millimeters
	away from the sonar.  However, this appears to be incorrect.  We are multiplying by SONAR_CONVERSION (0.555)
	to get to millimeters.
	Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo.
	*/
	public double[] getSonars()
	{
		return getSonars(null);
	}

	/** Returns all the time ticks, for each sonar, when the sonar data last arrived.
	Note that sonars come in different batches, so you may not receive all of them
	in one time tick.  Each time any sonar batch arrives, the time tick is increased; this can thus give you an idea
	as whether your sonar data is really old (if it's older than 8, you've got old data -- older than 32 say, you've got REALLY old data).
	Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo. If you pass in NULL or an array differently sized, you'll get back a new array;
	else the array passed in will be filled in and returned, allowing you to save allocations.*/
	public int[] getLastSonarTimes(int[] maybeReuse)
	{
		if (maybeReuse == null || maybeReuse.length != lastSonarTime.length)
			maybeReuse = new int[lastSonarTime.length];
		synchronized(readLock)
		{
			System.arraycopy(lastSonarTime,0,maybeReuse,0,lastSonarTime.length);
		}
		return maybeReuse;
	}

	/** Returns all the time ticks, for each sonar, when the sonar data last arrived.
	Note that sonars come in different batches, so you may not receive all of them
	in one time tick.  Each time any sonar batch arrives, the time tick is increased; this can thus give you an idea
	as whether your sonar data is really old (if it's older than 8, you've got old data -- older than 32 say, you've got REALLY old data).
	Looking at the front of the robot, sonar 0 is the far-right front servo
	and sonar 7 is the far-left front servo.  Looking at the back of the robot, sonar 8 is the far-right rear servo and
	sonar 15 is the far-left rear servo. */
	public int[] getLastSonarTimes()
	{
		return getLastSonarTimes(null);
	}

	/** Returns the robot name string the robot had indicated upon connection. */
	public String getRobotName()
	{
		synchronized(readLock)
		{
			return robotName;
		}
	}

	/** Returns the robot class string the robot had indicated upon connection. */
	public String getRobotClass()
	{
		synchronized(readLock)
		{
			return robotClass;
		}
	}

	/** Returns the robot subclass string the robot had indicated upon connection. */
	public String getRobotSubclass()
	{
		synchronized(readLock)
		{
			return robotSubclass;
		}
	}


	/** Constructs a robot but does not connect it.  Use connect() to connect the robot. */
	public Robot()
	{
		for(int x = 0; x < sonars.length; x++)
			sonars[x] = Double.NaN;
		format.setMinimumFractionDigits(4);
		format.setMaximumFractionDigits(4);
		format.setGroupingUsed(false);
	}


	/** Calculates the checksum of the data. */
	protected static short checksum(byte[] data)
	{
		int c=0, data1, data2;
		for(int x=0; x<data.length-1; x+=2)
		{
			data1 = data[x];
			if (data1 < 0) data1 += 256;
			data2 = data[x+1];
			if (data2 < 0) data2 += 256;
			c += ( (data1 << 8) | data2);
			c = c & 0xffff;
		}
		if ((data.length & 0x1) == 0x1)  // odd
		{
			data1 = data[data.length-1];
			if (data1 < 0) data1 += 256;
			c = c ^ data1;
		}
		return (short)c;
	}



	private long lastToRobotTime = 0;
	private final int THREAD_SLEEP = 300;

	/**
	   send the data to the robot.  The data contains only the instruction number and parameters.
	   the header if automatically added, the length of the packet is calculated and the checksum is
	   appended at the end of the packet, so that the user's job is easier.
	*/
	protected boolean submit(byte[] data, OutputStream out)
	{
		try
		{
			byte[] temp = new byte[ data.length + 5 ];

			short checksum = checksum(data);
			System.arraycopy(data,0,temp,3,data.length);
			temp[0] = HEADER[0];
			temp[1] = HEADER[1];
			temp[2] = (byte)(data.length+2); // remember this cannot exceed 200!
			temp[temp.length-2] = (byte)(checksum >> 8);
			temp[temp.length-1] = (byte)(checksum & 0x00ff);

			// write out the data
			if (out!=null)
			{
				out.write(temp,0,temp.length);
				out.flush();
			}

			long current = System.currentTimeMillis();
			/*
				    if ( isVerbose() )
					{
					System.err.print("Packet Sent (" +
					    ((lastToRobotTime == 0) ? "--" : ("" + (current - lastToRobotTime))) + "):");
					printPacket(temp,null,System.err);
					}
			*/
			lastToRobotTime = current;
		}
		catch (IOException e)
		{
			return false;
		}
		return true;
	}


	// prints out the two arrays to err in order.  Bytes are printed 0...256, not -128...127
	// either array or array2 can be null
	private void printPacket(byte[] header, byte[] data, PrintStream err)
	{
		boolean first = false;
		if (header!=null)
			for( byte i = 0 ; i < header.length ; i++ )
			{
				if (!first) first = true;
				else err.print(" ");
				err.print((((int)header[i])+256)%256);
			}
		if (data!=null)
			for( byte i = 0 ; i < data.length ; i++ )
			{
				if (!first) first = true;
				else err.print(" ");
				err.print((((int)data[i])+256)%256);
			}
		err.println();
	}


	/**
	   Connects to the given IP Address and port number, and establishes a stream connection to the robot at the other end.
	*/
	public void connect(String ipAddress, int port)
	{
		try
		{
			sock = new Socket(ipAddress, port);
			connect(sock.getInputStream(),sock.getOutputStream());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}


	private void readBytes( byte[] buf, int offset, int size ) throws IOException
	{
		int ptr = 0;
		do
		{
			ptr += in.read(buf,ptr+offset,size-ptr);
			if (ptr < size) try
				{
					Thread.currentThread().sleep(20);
				}
				catch (InterruptedException e) { }
		}
		while (ptr<size);
	}

	private void sleep()
	{
		try
		{
			Thread.sleep( THREAD_SLEEP );
		}
		catch( InterruptedException e )
		{
		}
	}

	/** Waits for the byte string in 'expected' to show up in the incoming stream.
	This is a simpler algorithm than your typical string-matching algorithm, because
	it assumes that the FIRST byte in 'expected' is different from all the others.
	Times out after the given timeout in milliseconds. */
	private boolean lookFor(byte[] expected, long timeout)
	{
		long t = System.currentTimeMillis();
		int bytesSearched = 0;
		int count = 0;
		byte[] buf = new byte[1];
		while(count < expected.length)
		{
			try
			{
				readBytes(buf, 0, 1);
				if (buf[0] == expected[count]) count++;
				else
				{
					count = 0;
					bytesSearched++;
				}
			}
			catch (IOException e) { }
			if (System.currentTimeMillis() - timeout >= t)
			{
				if (isVerbose()) System.err.println("Timeout, byte count " + bytesSearched);
				return false;
			}
		}
		if (isVerbose() && bytesSearched > 0) System.err.println("Searched " + bytesSearched);
		return true;
	}

	private boolean readBytesWithTimeout(int numBytes, long timeout)
	{
		long t = System.currentTimeMillis();
		byte[] buf = new byte[1];
		for(int count = 0; count < numBytes; count++)
		{
			try
			{
				readBytes(buf, 0, 1);
			}
			catch (IOException e) { }
			if (System.currentTimeMillis() - timeout >= t) return false;
		}
		return true;
	}

	private static final long SYNC_STREAM_WAIT = 300;
	private static final byte[] SYNC0_RESPONSE = new byte[] { (byte)250, (byte)251, 3, 0, 0, 0 };
	private boolean doSync0(byte[] buf)
	{
		if (sync0() == false) return false;
		if (isVerbose()) System.err.println( "Waiting for SYNC0...." );
		return lookFor(SYNC0_RESPONSE, SYNC_STREAM_WAIT);
	}

	private static final byte[] SYNC1_RESPONSE = new byte[] { (byte)250, (byte)251, 3, 1, 0, 1 };
	private boolean doSync1(byte[] buf)
	{
		if (sync1() == false) return false;
		if (isVerbose()) System.err.println( "Waiting for SYNC1...." );
		return lookFor(SYNC1_RESPONSE, SYNC_STREAM_WAIT);
	}

	private static final byte[] SYNC2_RESPONSE_START = new byte[] { (byte)250, (byte)251 };
	private boolean doSync2(byte[] buf)
	{
		if (sync2() == false) return false;
		if (isVerbose()) System.err.println( "Waiting for SYNC2...." );
		try
		{
			Thread.sleep(SYNC_STREAM_WAIT);
		}
		catch (InterruptedException e)
		{
			return false;
		}
		if (!lookFor(SYNC2_RESPONSE_START, SYNC_STREAM_WAIT)) return false;  // 250, 251
		if (!readBytesWithTimeout(2,SYNC_STREAM_WAIT)) return false;  // num bytes for string and Sync2 response
		return true;
	}

	private String readString()
	{
		try
		{
			byte[] buf = new byte[1];
			StringBuffer sb = new StringBuffer();
			while(true)
			{
				readBytes(buf,0,1);
				if( buf[0] == 0 ) break;
				else sb.append((char)(buf[0]));
			}
			return sb.toString();
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private long DISPLAY_INTERVAL = 1000;
	private long lastDisplayTime = 0;
	private long displayCount = 0;

	/**
	   Connects to a robot with the given input and output streams.  You probably want to connect in a different way than this.
	*/
	public void connect(InputStream inputStream, OutputStream outputStream) throws IOException
	{
		// set up a nonblocking input stream so syncs work more reliably
		InputStream syncInputStream = new TimeoutInputStream(new NonClosingInputStream(inputStream), 256, 100, 0);

		byte[] buf = new byte[7];

		// if count is exceeded in SYNCing, it's assume that the robot's stream is already open
		final int MAX_COUNT = 5;
		int count = 0;

		in = syncInputStream;
		out = outputStream;

		// do doSync0, and if that succeeds, do doSync1, and if that succeeds, do doSync2, and
		// if that succeeds, we're done.  Else fall back and do them again.
		boolean a = true;
		do
		{
			if (!mobileSim)
			{
				// close the controller if it's already open
				close();
			}
			if (a) a = false;
			else sleep();
			boolean b = true;
			do
			{
				if (b) b = false;
				else sleep();
				boolean c = true;
				do
				{
					if (c) c = false;
					else sleep();
				}
				while (!doSync0(buf));
			}
			while (!doSync1(buf));
		}
		while (!doSync2(buf));

		// read autoconfiguration strings (3)
		String s = "Name: " + (robotName = readString()) + " / " +
		           (robotClass = readString()) + " / " +
		           (robotSubclass = readString());
		if (isVerbose()) System.err.println(s);

		readBytesWithTimeout(2, SYNC_STREAM_WAIT);  // checksum of string

		// start the controller
		open();

		// we've synced, so get rid of the troublesome TimeoutInputStream
		syncInputStream.close();
		syncInputStream = null;
		in = inputStream;

		// start the read thread
		readInfoThread = new Thread(new Runnable()
		{
			public void run()
			{
				while(working)
				{
					try
					{
						if (readPacket())
						{
							displayCount++;
						}
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}

					// update every so often
					if (isVerbose())
					{
						long time = System.currentTimeMillis();
						if (time - lastDisplayTime >= DISPLAY_INTERVAL)
						{
							displayInfo();
							lastDisplayTime = time;
						}
					}
				}
			}
		});
		readInfoThread.start();

		// start the beat thread
		beatThread = new Thread(new Runnable()
		{
			public void run()
			{
				while( working )
				{
					pulse();
					try
					{
						Thread.currentThread().sleep(1500);    // must be under 2 seconds
					}
					catch (InterruptedException e) { }
				}
			}
		});
		beatThread.start();
	}


	// buffer-formats to 9 long
	private static final String [] buffers = {"         ", "        ", "       ", "      ", "     ", "    ", "   ", "  ", " "};
	private String bFormat(String s)
	{
		if (s.length() >= 9) return s;
		return buffers[s.length()] + s;
	}


	/**
	   Prints some current sensor information to the screen.
	*/
	public void displayInfo()
	{
		synchronized(readLock)
		{
			System.err.println("In(" + displayCount + ") Loc(" + format.format(xpos) + ", " + format.format(ypos) +", " + format.format(orientation) +
			                   ") Vel(L: " + format.format(leftWheelVelocity) + (leftWheelStallIndicator ? "S" : "") +
			                   " R: " + format.format(rightWheelVelocity) + (rightWheelStallIndicator ? "S" : "") +
			                   " C: " + format.format(control) + (motorEngaged ? ") Go" : ") Stop"));
			for(int i=0; i < sonars.length; i++)
			{
				if (sonars[i] != sonars[i] )  // NaN
				{
					if (i % 8 != 0) System.err.println();
					break;
				}
				System.err.print(bFormat(format.format(sonars[i])) + " ");
				if (i % 8 == 7) System.err.println();
			}
		}
	}

	private byte lowByte( short arg )
	{
		return (byte)( arg & 0xff );
	}

	private byte highByte( short arg )
	{
		return (byte)( (arg >> 8) & 0xff );
	}

	/** send sync0 message (for connection to robot) */
	public boolean sync0()
	{
		return submit(new byte[ ] { 0 }, out);
	}

	/** send sync1 message (for connection to robot) */
	public boolean sync1()
	{
		return submit(new byte[ ] { 1 }, out);
	}

	/** send sync2 message (for connection to robot) */
	public boolean sync2()
	{
		return submit(new byte[ ] { 2 }, out);
	}

	/** client pulse resets watchdog and prevents the robot from disconnecting */
	public boolean pulse()
	{
		return submit(new byte[ ] { 0 }, out);
	}

	/** starts the controller */
	public boolean open()
	{
		return submit(new byte[ ] { 1 }, out);
	}

	/** close client-server connection */
	public boolean close()
	{
		return submit(new byte[ ] { 2 }, out);
	}

	/** set sonar polling sequence */
	public boolean polling( String arg )
	{
		byte[] dele = arg.getBytes();
		byte[] temp = new byte[ 3 + dele.length ];
		temp[0] = 3; // command number
		temp[1] = STRING; // the parameter is a string
		temp[2] = (byte)(dele.length); // length of string
		for( int i = 0 ; i < dele.length ; i++ )
			temp[3+i] = dele[i];
		return submit( temp, out );
	}

	/** enable/disable the motors */
	public boolean enable( boolean arg )
	{
		if( arg )
			return submit( new byte[] { 4, POSITIVE_INTEGER, 1, 0 }, out );
		else
			return submit( new byte[] { 4, POSITIVE_INTEGER, 0, 0 }, out );
	}

	/** sets translation acc/deceleration; in mm/sec^2 */
	public boolean seta( short arg )
	{
		if( arg >= 0 )
			return submit( new byte[] { 5, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
		else
			return submit( new byte[] { 5, NEGATIVE_INTEGER, lowByte((short)(-arg)), highByte((short)(-arg)) }, out );
	}

	/** set maximum possible translation velocity; in mm/sec */
	public boolean setv( short arg )
	{
		return submit( new byte[] { 6, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** Resets the robot's believed position to be x=0, y=0, theta=0 */
	public boolean seto()
	{
		return submit( new byte[] { 7 }, out );
	}

	/** Translates forward or backward a given distance in mm at SETV speed */
	public boolean move(short arg)
	{
		if( arg >= 0 )
			return submit( new byte[] { 8, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
		else
			return submit( new byte[] { 8, NEGATIVE_INTEGER, lowByte((short)(-arg)), highByte((short)(-arg)) }, out );
	}

	/** Rotates positive or negative degrees per second. */
	public boolean rotate(short arg)
	{
		if( arg >= 0 )
			return submit( new byte[] { 9, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
		else
			return submit( new byte[] { 9, NEGATIVE_INTEGER, lowByte((short)(-arg)), highByte((short)(-arg)) }, out );
	}

	/** sets maximum rotational velocity; in degrees/sec */
	public boolean setrv( short arg )
	{
		return submit( new byte[] { 10, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** move forward (+) or reverse (-); in mm/sec */
	public boolean vel( short arg )
	{
		return submit( new byte[] { 11, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** turn to absolute heading (in the robot's belief); 0-359 degress */
	public boolean head( short arg )
	{
		return submit( new byte[] { 12, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** turn relative to current heading, that is, turn BY some number of degrees. */
	public boolean dhead( short arg )
	{
		return submit( new byte[] { 13, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/*
	    public boolean hostbaud( short arg )
		{
		return submit( new byte[] { 50, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
		}
	*/

	/** Play some tones.  sound duration (20 ms increments)/tone (half-cycle) pairs. */
	public boolean say( byte[] tones )
	{
		byte[] temp = new byte[ 3 + tones.length ];
		temp[0] = 15; // command number
		temp[1] = STRING; // the parameter is a string
		temp[2] = (byte)(tones.length); // length of string
		for( int i = 0 ; i < tones.length ; i++ )
			temp[3+i] = tones[i];
		return submit( temp, out );
	}

	/*
	// equest configuration SIP
	public boolean config( short arg )
	{
	return submit( new byte[] { 18, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/*
	//request continuous (>0) or stop sending (=0) encoder SIPs
	public boolean encoder( short arg )
	{
	return submit( new byte[] { 19, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/** rotate at +/- degrees/sec */
	public boolean rvel( short arg )
	{
		if( arg >= 0 )
			return submit( new byte[] { 21, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
		else
			return submit( new byte[] { 21, NEGATIVE_INTEGER, lowByte((short)(-arg)), highByte((short)(-arg)) }, out );
	}

	/*
	// colbert relative heading setpoint; +/- degrees
	public boolean dchead( short arg )
	{
	return submit( new byte[] { 22, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/** sets rotational (+/-)de/acceleration; in mm/sec^2 */
	public boolean setra( short arg )
	{
		return submit( new byte[] { 23, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** enable/disable the sonars */
	public boolean sonar( boolean arg )
	{
		if( arg )
			return submit( new byte[] { 28, POSITIVE_INTEGER, 1, 0 }, out );
		else
			return submit( new byte[] { 28, POSITIVE_INTEGER, 0, 0 }, out );
	}

	/** enable/disable a given sonar. */
	public boolean sonar( short arg )
	{
		return submit( new byte[] { 28, POSITIVE_INTEGER, (byte)(arg % 256), (byte)(arg / 256) }, out );
	}

	/** stops the robot (motors remain enabled) */
	public boolean stop()
	{
		return submit( new byte[] { 29 }, out );
	}

	/** msbits is a byte mask that selects output port(s) for changes; lsbits set (1) or reset (0) the selected port */
	public boolean digout( short arg )
	{
		return submit( new byte[] { 30, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** independent wheel velocities, left and right. PSOS is in +/- 4mm/sec; POS/AmigOS is in 2 cm/sec increments */
	public boolean vel2(byte left, byte right)
	{
		int l = left;
		int r = right;
		if (l < 0) l = 256 + l;
		if (r < 0) r = 256 + r;
		return vel2((short)(l << 8 | r));
	}

	/** independent wheel velocities; lsb=right wheel; msb=left wheel; PSOS is in +/- 4mm/sec; POS/AmigOS is in 2 cm/sec increments */
	public boolean vel2( short arg )
	{
		return submit( new byte[] { 32, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/*
	// Pioneer Gripper server command.  see the Pioneer Gripper manuals for details
	public boolean gripper( short arg )
	{
	return submit( new byte[] { 33, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/*
	// select the A/D port number for analog value in SIP.  selected port reported in SIP timer value
	public boolean adsel( short arg )
	{
	return submit( new byte[] { 35, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/*
	// Pioneer Gripper server value.  see P2 Gripper manual for details
	public boolean gripperval( short arg )
	{
	return submit( new byte[] { 36, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/*
	// msb is the port number (1-4) and lsb is the pulse width in 100 microsec units PSOS or 10 microsec units P2OS
	public boolean ptupos( short arg )
	{
	return submit( new byte[] { 41, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/**
	// send string argument to serial device connected to AUX port on microcontroller
	public boolean tty2( String arg )
	{
	byte[] dele = arg.getBytes();
	byte[] temp = new byte[ 3 + dele.length ];
	temp[0] = 42; // command number
	temp[1] = STRING; // the parameter is a string
	temp[2] = (byte)(dele.length); // length of string
	for( int i = 0 ; i < dele.length ; i++ )
	    temp[3+i] = dele[i];
	return submit( temp, out );
	}
	*/

	/*
	// request to retrieve 1-200 bytes from the aux serial channel; 0 flusshes the AUX serial input buffer
	public boolean getaux( short arg )
	{
	return submit( new byte[] { 43, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/** stop and register a stall in front (1), rear (2) or either (3) bump-ring contacted. Off (default) is 0 */
	public boolean bumpstall( short arg )
	{
		return submit( new byte[] { 44, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/*
	// TCM2 module commands; see P2 TCM2 manual for details
	public boolean tcm2( short arg )
	{
	return submit( new byte[] { 45, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}
	*/

	/** emergency stop, overrides deceleration */
	public boolean e_stop()
	{
		return submit( new byte[] { 55 }, out );
	}

	/*
	// single-step mode (simulator only)
	public boolean step()
	{
	return submit( new byte[] { 64 }, out );
	}
	*/

	/** play stored sound -- Amigobot only.  Pioneer's don't have such a thing. */
	public boolean sound( short arg )
	{
		return submit( new byte[] { 90, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** request playlist packet for sound number or 0 for all user sounds -- Amigobot only.  */
	public boolean playlist( short arg )
	{
		return submit( new byte[] { 91, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}

	/** mute (0) or enable (1) sounds -- Amigobot only. */
	public boolean soundtog( short arg )
	{
		return submit( new byte[] { 92, POSITIVE_INTEGER, lowByte(arg), highByte(arg) }, out );
	}


	boolean readPacket() throws IOException
	{
		if (lookFor(SYNC2_RESPONSE_START, 1000))
		{
			byte[] temp = new byte[1];
			readBytes(temp, 0, 1);
			int length;
			if (temp[0] < 0)
				length = temp[0]+256;
			else
				length = temp[0];
			byte[] data = new byte[ length ];
			readBytes( data, 0, length ); // read rest of packet

			// check if checksum is correct
			byte[] forchecksum = new byte[ length-2 ];
			System.arraycopy( data, 0, forchecksum, 0, length-2 );
			int packetCheckSum;
			packetCheckSum = data[data.length-2];
			if (data[data.length-2] < 0) packetCheckSum += 256;
			packetCheckSum *=256;
			packetCheckSum += data[data.length-1];
			if (data[data.length-1] < 0) packetCheckSum += 256;

			short expectedCheckSum = checksum(forchecksum);
			short gotCheckSum = (short)(packetCheckSum & 0xFFFF);

			// if checksum is correct, interpret data

			// NOTE: On Linux boxen running with Prolific/Manhattan usb serial drivers, we're
			// seeing checksums that are ALWAYS off by 3! But the data seems to be correct, so
			// we're allowing a weird checksum of this form.
			if( expectedCheckSum == gotCheckSum ) // || (checksumLenient && (expectedCheckSum == gotCheckSum + 3 || expectedCheckSum == gotCheckSum - 3)))
			{

				// temp variables -- we'll move these into the real variables after the read process
				boolean _motorEngaged = false;
				double _xpos = 0;
				double _ypos = 0;
				double _orientation = 0;
				double _leftWheelVelocity = 0;
				double _rightWheelVelocity = 0;
				int _battery = 0;
				boolean _leftWheelStallIndicator = false;
				boolean _rightWheelStallIndicator = false;
				double _control = 0;

				String ss = "";
				for( short i = 0 ; i < data.length ; i++ )
					if( data[i] >= 0 )
						ss = ss + " " + data[i];
					else
						ss = ss + " " + (data[i]+256);
				// read motor status
				if( data[0] == 0x32 )
					_motorEngaged = false;
				else if( data[0] == 0x33 )
					_motorEngaged = true;
				else
					System.err.println( "Invalid motor status: " + data[0] );
				int alpha;


				// read xpos
				if( data[2] >= 0 )
					alpha = data[2];
				else
					alpha = data[2] + 256;
				alpha = alpha & 0x7F;
				alpha = alpha * 256;
				if( data[1] >= 0 )
					alpha += data[1];
				else
					alpha += data[1] + 256;
				// it appears that this is actually ranging from 0 to 32768, which
				// would jibe with the Amigobot documentation but not with the Pioneer documentation
				if (alpha >= 16384) alpha -= 32768;
				_xpos = XPOS_CONVERSION * alpha; //0.5083*alpha;


				// read ypos
				if( data[4] >= 0 )
					alpha = data[4];
				else
					alpha = data[4] + 256;
				alpha = alpha & 0x7F;
				alpha = alpha * 256;
				if( data[3] >= 0 )
					alpha += data[3];
				else
					alpha += data[3] + 256;
				// it appears that this is actually ranging from 0 to 32768, which
				// would jibe with the Amigobot documentation but not with the Pioneer documentation
				if (alpha >= 16384) alpha -= 32768;
				_ypos = YPOS_CONVERSION * alpha; // 0.5083*alpha;


				// read orientation
				if( data[6] >= 0 )
					alpha = data[6];
				else
					alpha = data[6] + 256;
				alpha = alpha * 256;
				if( data[5] >= 0 )
					alpha += data[5];
				else
					alpha += data[5] + 256;
				// it appears that this ranges from 0 to 4096, which doesn't jibe with anything in the docs
				if (alpha >= 2048) alpha -= 4096;
				_orientation = THPOS_CONVERSION * alpha; // 0.001534 * ((short)alpha);


				// read velocity of left wheel
				if( data[8] >= 0 )
					alpha = data[8];
				else
					alpha = data[8] + 256;
				alpha = alpha * 256;
				if( data[7] >= 0 )
					alpha += data[7];
				else
					alpha += data[7] + 256;
				// it appears that this ranges from 0 to 65K signed
				if (alpha >= 32768) alpha -= 65536;
				_leftWheelVelocity = LVEL_CONVERSION * alpha; // 0.6154 * ((short)alpha);


				// read velocity of right wheel
				if( data[10] >= 0 )
					alpha = data[10];
				else
					alpha = data[10] + 256;
				alpha = alpha * 256;
				if( data[9] >= 0 )
					alpha += data[9];
				else
					alpha += data[9] + 256;
				// it appears that this ranges from 0 to 65K signed
				if (alpha >= 32768) alpha -= 65536;
				_rightWheelVelocity = RVEL_CONVERSION * alpha; // 0.6154 * ((short)alpha);


				// read battery status
				if( data[11] >= 0 )
					_battery = data[11];
				else
					_battery = data[11] + 256;


				// read bumpers
				_leftWheelStallIndicator = (data[12]&0x80)!=0;
				_rightWheelStallIndicator = (data[13]&0x80)!=0;


				// read control
				if( data[15] >= 0 )
					alpha = data[15];
				else
					alpha = data[15] + 256;
				alpha = alpha * 256;
				if( data[14] >= 0 )
					alpha += data[14];
				else
					alpha += data[14] + 256;
				_control = CONTROL_CONVERSION * alpha; // 0.001534 * ((short)alpha);


				// read PTU
				// not implemented


				// read Compass
				// not implemented


				// read Sonar readings
				int nr = data[19];
				currentSonarTime++;
				for( int nn = 0 ; nn < nr ; nn++ )
				{
					int n_sonar = data[20+3*nn];
					if (n_sonar < 0 || n_sonar > 31)  // crazy sonar
					{
						if (isVerbose())
						{
							System.err.println("Bad Sonar number, got " + n_sonar + ", expected a value between 0 and 31.");
						}
					}
					else if (20+3*nn+2 >= data.length)  // crazy index
					{
						if (isVerbose())
						{
							System.err.println("Invalid sonar index, got " + nn);
						}
					}
					else
					{
						if( data[20+3*nn+2] >= 0 )
							alpha = data[20+3*nn+2];
						else
							alpha = data[20+3*nn+2] + 256;
						alpha = alpha * 256;
						if( data[20+3*nn+1] >= 0 )
							alpha += data[20+3*nn+1];
						else
							alpha += data[20+3*nn+1] + 256;
						_sonars[n_sonar] = SONAR_CONVERSION * alpha; // 0.555 * alpha;
						_lastSonarTime[n_sonar] = currentSonarTime;
					}
				}


				// now synchronize and dump
				synchronized(readLock)
				{
					validData = true;
					motorEngaged = _motorEngaged;
					xpos = _xpos;
					ypos = _ypos;
					orientation = _orientation;
					leftWheelVelocity = _leftWheelVelocity;
					rightWheelVelocity = _rightWheelVelocity;
					battery = _battery;
					leftWheelStallIndicator = _leftWheelStallIndicator;
					rightWheelStallIndicator = _rightWheelStallIndicator;
					control = _control;
					System.arraycopy(_sonars, 0, sonars, 0, sonars.length);
					System.arraycopy(_lastSonarTime, 0, lastSonarTime, 0, lastSonarTime.length);
				}

				return true;
			}
			else if (isVerbose())
			{
				System.err.println("Bad checksum, got " + (short)(packetCheckSum & 0xFFFF) + ", expected " + checksum(forchecksum) + ".");
				System.err.print("Bad packet was: ");
				printPacket(temp,data, System.err);
			}
		}
		return false;
	}

	// just in case
	protected synchronized void finalize()
	{
		disconnect();
	}

	/** shuts down the communication to the robot */
	public void disconnect()
	{
		working = false;
		try
		{
			if (beatThread != null) beatThread.join();
		}
		catch( InterruptedException e )
		{
		}
		try
		{
			if (readInfoThread != null) readInfoThread.join();
		}
		catch( InterruptedException e )
		{
		}
		working = true;  // for next time

		// stopping sonars
		sonar( false );
		// stopping motor
		enable( false );

		if (isVerbose())
			System.err.println( "Disconecting" );
		close();

		// it's important to disconnect the socket first if we're using IBM's timeout
		// input stream -- else it hangs forever on close.
		try
		{
			if (sock!=null) sock.close();
		}
		catch (IOException e) { }
		sock = null;
		try
		{
			if (in!=null) in.close();
		}
		catch (IOException e) { }
		in=null;
		try
		{
			if (out!=null) out.close();
		}
		catch (IOException e) { }
		out=null;

		validData = false; // outside synchronization, which is fine.
	}
}


/*
Academic Free License ("AFL") v. 3.0

This Academic Free License (the "License") applies to any original work
of authorship (the "Original Work") whose owner (the "Licensor") has
placed the following licensing notice adjacent to the copyright notice
for the Original Work:

Licensed under the Academic Free License version 3.0

1) Grant of Copyright License. Licensor grants You a worldwide,
royalty-free, non-exclusive, sublicensable license, for the duration of
the copyright, to do the following:

        a) to reproduce the Original Work in copies, either alone or as
           part of a collective work;

        b) to translate, adapt, alter, transform, modify, or arrange the
           Original Work, thereby creating derivative works ("Derivative
           Works") based upon the Original Work;

        c) to distribute or communicate copies of the Original Work and
           Derivative Works to the public, UNDER ANY LICENSE OF YOUR
           CHOICE THAT DOES NOT CONTRADICT THE TERMS AND CONDITIONS,
           INCLUDING LICENSOR'S RESERVED RIGHTS AND REMEDIES, IN THIS
           ACADEMIC FREE LICENSE;

        d) to perform the Original Work publicly; and

        e) to display the Original Work publicly.

2) Grant of Patent License. Licensor grants You a worldwide,
royalty-free, non-exclusive, sublicensable license, under patent claims
owned or controlled by the Licensor that are embodied in the Original
Work as furnished by the Licensor, for the duration of the patents, to
make, use, sell, offer for sale, have made, and import the Original Work
and Derivative Works.

3) Grant of Source Code License. The term "Source Code" means the
preferred form of the Original Work for making modifications to it and
all available documentation describing how to modify the Original Work.
Licensor agrees to provide a machine-readable copy of the Source Code of
the Original Work along with each copy of the Original Work that
Licensor distributes. Licensor reserves the right to satisfy this
obligation by placing a machine-readable copy of the Source Code in an
information repository reasonably calculated to permit inexpensive and
convenient access by You for as long as Licensor continues to distribute
the Original Work.

4) Exclusions From License Grant. Neither the names of Licensor, nor the
names of any contributors to the Original Work, nor any of their
trademarks or service marks, may be used to endorse or promote products
derived from this Original Work without express prior permission of the
Licensor. Except as expressly stated herein, nothing in this License
grants any license to Licensor's trademarks, copyrights, patents, trade
secrets or any other intellectual property. No patent license is granted
to make, use, sell, offer for sale, have made, or import embodiments of
any patent claims other than the licensed claims defined in Section 2.
No license is granted to the trademarks of Licensor even if such marks
are included in the Original Work. Nothing in this License shall be
interpreted to prohibit Licensor from licensing under terms different
from this License any Original Work that Licensor otherwise would have a
right to license.

5) External Deployment. The term "External Deployment" means the use,
distribution, or communication of the Original Work or Derivative Works
in any way such that the Original Work or Derivative Works may be used
by anyone other than You, whether those works are distributed or
communicated to those persons or made available as an application
intended for use over a network. As an express condition for the grants
of license hereunder, You must treat any External Deployment by You of
the Original Work or a Derivative Work as a distribution under section
1(c).

6) Attribution Rights. You must retain, in the Source Code of any
Derivative Works that You create, all copyright, patent, or trademark
notices from the Source Code of the Original Work, as well as any
notices of licensing and any descriptive text identified therein as an
"Attribution Notice." You must cause the Source Code for any Derivative
Works that You create to carry a prominent Attribution Notice reasonably
calculated to inform recipients that You have modified the Original
Work.

7) Warranty of Provenance and Disclaimer of Warranty. Licensor warrants
that the copyright in and to the Original Work and the patent rights
granted herein by Licensor are owned by the Licensor or are sublicensed
to You under the terms of this License with the permission of the
contributor(s) of those copyrights and patent rights. Except as
expressly stated in the immediately preceding sentence, the Original
Work is provided under this License on an "AS IS" BASIS and WITHOUT
WARRANTY, either express or implied, including, without limitation, the
warranties of non-infringement, merchantability or fitness for a
particular purpose. THE ENTIRE RISK AS TO THE QUALITY OF THE ORIGINAL
WORK IS WITH YOU. This DISCLAIMER OF WARRANTY constitutes an essential
part of this License. No license to the Original Work is granted by this
License except under this disclaimer.

8) Limitation of Liability. Under no circumstances and under no legal
theory, whether in tort (including negligence), contract, or otherwise,
shall the Licensor be liable to anyone for any indirect, special,
incidental, or consequential damages of any character arising as a
result of this License or the use of the Original Work including,
without limitation, damages for loss of goodwill, work stoppage,
computer failure or malfunction, or any and all other commercial damages
or losses. This limitation of liability shall not apply to the extent
applicable law prohibits such limitation.

9) Acceptance and Termination. If, at any time, You expressly assented
to this License, that assent indicates your clear and irrevocable
acceptance of this License and all of its terms and conditions. If You
distribute or communicate copies of the Original Work or a Derivative
Work, You must make a reasonable effort under the circumstances to
obtain the express assent of recipients to the terms of this License.
This License conditions your rights to undertake the activities listed
in Section 1, including your right to create Derivative Works based upon
the Original Work, and doing so without honoring these terms and
conditions is prohibited by copyright law and international treaty.
Nothing in this License is intended to affect copyright exceptions and
limitations (including "fair use" or "fair dealing"). This License shall
terminate immediately and You may no longer exercise any of the rights
granted to You by this License upon your failure to honor the conditions
in Section 1(c).

10) Termination for Patent Action. This License shall terminate
automatically and You may no longer exercise any of the rights granted
to You by this License as of the date You commence an action, including
a cross-claim or counterclaim, against Licensor or any licensee alleging
that the Original Work infringes a patent. This termination provision
shall not apply for an action alleging patent infringement by
combinations of the Original Work with other software or hardware.

11) Jurisdiction, Venue and Governing Law. Any action or suit relating
to this License may be brought only in the courts of a jurisdiction
wherein the Licensor resides or in which Licensor conducts its primary
business, and under the laws of that jurisdiction excluding its
conflict-of-law provisions. The application of the United Nations
Convention on Contracts for the International Sale of Goods is expressly
excluded. Any use of the Original Work outside the scope of this License
or after its termination shall be subject to the requirements and
penalties of copyright or patent law in the appropriate jurisdiction.
This section shall survive the termination of this License.

12) Attorneys' Fees. In any action to enforce the terms of this License
or seeking damages relating thereto, the prevailing party shall be
entitled to recover its costs and expenses, including, without
limitation, reasonable attorneys' fees and costs incurred in connection
with such action, including any appeal of such action. This section
shall survive the termination of this License.

13) Miscellaneous. If any provision of this License is held to be
unenforceable, such provision shall be reformed only to the extent
necessary to make it enforceable.

14) Definition of "You" in This License. "You" throughout this License,
whether in upper or lower case, means an individual or a legal entity
exercising rights under, and complying with all of the terms of, this
License. For legal entities, "You" includes any entity that controls, is
controlled by, or is under common control with you. For purposes of this
definition, "control" means (i) the power, direct or indirect, to cause
the direction or management of such entity, whether by contract or
otherwise, or (ii) ownership of fifty percent (50%) or more of the
outstanding shares, or (iii) beneficial ownership of such entity.

15) Right to Use. You may use the Original Work in all ways not
otherwise restricted or conditioned by this License or by law, and
Licensor promises not to interfere with or be responsible for such uses
by You.

16) Modification of This License. This License is Copyright (c) 2005
Lawrence Rosen. Permission is granted to copy, distribute, or
communicate this License without modification. Nothing in this License
permits You to modify this License as applied to the Original Work or to
Derivative Works. However, You may modify the text of this License and
copy, distribute or communicate your modified version (the "Modified
License") and apply it to other original works of authorship subject to
the following conditions: (i) You may not indicate in any way that your
Modified License is the "Academic Free License" or "AFL" and you may not
use those names in the name of your Modified License; (ii) You must
replace the notice specified in the first paragraph above with the
notice "Licensed under <insert your license name here>" or with a notice
of your own that is not confusingly similar to the notice in this
License; and (iii) You may not claim that your original works are open
source software unless your Modified License has been approved by Open
Source Initiative (OSI) and You comply with its license review and
certification process.

*/
