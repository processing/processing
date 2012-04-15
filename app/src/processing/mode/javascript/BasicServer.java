package processing.mode.javascript;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *	BasicServer, the server underneath JavaScript mode.
 *	Based on a Sun tutorial at: http://bit.ly/fpoHAF
 *	Changed to accept a document root.
 */
class BasicServer implements HttpConstants, Runnable
{
	ArrayList<BasicServerListener> listeners;
	
	// TODO how to handle too many servers?
	// TODO read settings from sketch.properties
	// NOTE 0.0.0.0 does not work on XP
	public static final String localDomain = "http://127.0.0.1";
	
	final static int MIN_PORT = 0;
	final static int MAX_PORT = 49151;

	Thread thread = null;
	ServerSocket server = null;
	
	//private ArrayList<Worker> threads = new ArrayList<Worker>();
	//private int workers = 5;

	private File virtualRoot;
	private int timeout = 5000;
	private int port = -1;
	
	private boolean running = false, inited = false;
//	private boolean stopping = false;
	
	private ArrayList<String> addresses;
	
	/**
	 *	Constructor
	 *
	 *	@param root the root folder to serve from
	 */
	BasicServer ( File root ) 
	{
		setRoot( root );
		
		findPort();
	}
	
	/**
	 *	Getter, return the server root folder
	 *
	 *	@return the root file
	 */
	public File getRoot ()
	{
		return virtualRoot;
	}
	
	/**
	 *	Set the root folder to a new dir
	 *
	 *	@param root the new folder to server form 
	 */
	public void setRoot ( File root )
	{
		if ( root.exists() && root.isDirectory() && root.canRead() ) 
		{
			virtualRoot = root;
		}
		else
		{
			System.err.println( "BasicServer: error setting <root>" );
		}
	}
	
	/**
	 *	Add a listener to this server for start/stop callbacks
	 *
	 *	@param listener the listener to add, needs to implement interface BasicServerListener
	 */
	public void addListener ( BasicServerListener listener )
	{
		if ( listeners == null ) listeners = new ArrayList<BasicServerListener>();
		if ( listener != null ) {
			listeners.add( listener );
		}
	}
	
	/**
	 *	Get the timeout, the time span after which requests are discarded
	 *
	 *	@return the timeout as int in milliseconds
	 */
	public int getTimeout ()
	{
		return timeout;
	}
	
	/**
	 *	Getter, returns the server address with port as URL string
	 *
	 *	@return the server address as URL string
	 */
	public String getAddress ()
	{
		return localDomain + ":" + getPort() + "/";
	}
	
	/**
	 *	Build a list of addresses that this machine is available under
	 *
	 *	@return an ArrayList with 0 or more addresses as URL strings
	 */
	public ArrayList<String> getInetAddresses ()
	{
		addresses = new ArrayList<String>();
		
		try {
		    NetworkInterface ni = NetworkInterface.getByInetAddress( InetAddress.getLocalHost() );
		    Enumeration<InetAddress> ia = ni.getInetAddresses();
		    while ( ia.hasMoreElements () ) 
			{
		        InetAddress elem = ia.nextElement();
		        if ( elem instanceof Inet6Address ) 
				{
		            // ?
					continue;
		        } 
		        else 
				{
					addresses.add( elem.getHostAddress() );
		        }
		    }
		}  catch ( Exception e ) {
			// ignore?
		}
		
		return addresses;
	}
	
	/**
	 *	Look for and set to an available port
	 */
	private void findPort ()
	{
		ServerSocket ss = null;
		try {
			
			ss = new ServerSocket( 0 );
			ss.setReuseAddress(true);
			port = ss.getLocalPort();
			ss.close();
			
		} catch ( IOException ioe ) {
			System.err.println(ioe);
		} catch ( SecurityException se ) {
			System.err.println(se);
		}
	}
	
	/**
	 *	Getter, return the current port the server is available on
	 *
	 *	@return the port number as int
	 */
	public int getPort ()
	{
		return port;
	}
	
	/**
	 *	Set the port number to a new one
	 * 
	 *	@param newPort the new port number as int, should be > 1024 and < 49151
	 */
	public void setPort ( int newPort )
	{
		if ( !isRunning() ) 
		{	
			// port available? see:
			// http://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
			// http://stackoverflow.com/questions/2675362/how-to-find-an-available-port

			if ( newPort >= 0 )
			{
				if ( !available(newPort) )
				{
					System.err.println( "BasicServer: " + 
										"that port ("+newPort+") seems to be taken " + 
										"or is out of range (<1025 or >49151)");
					System.out.println( "... if it works anyway, ignore the warning." );
				}
				else
				{
					port = newPort;
				}
			}
		}
	}
	
	/**
	 *	Start this server, starts internal thread
	 */
	public void start ()
	{
		if ( virtualRoot == null )
		{
			System.err.println( "BasicServer: virtual root is null." );
			return;
		}
		
		thread = null;
		thread = new Thread( this, "Processing.BasicServer" );
		thread.start();
	}
	
	/**
	 * Checks to see if a specific port is available.
	 * http://mina.apache.org/
	 * http://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
	 *
	 *	@param port a port number to test
	 *	@return true if port number is available
	 */
	public static boolean available ( int port )
	{
		// http://en.wikipedia.org/wiki/Port_number
		// http://en.wikipedia.org/wiki/Ephemeral_port
		if ( port < 1025 || port > 49151 )
		{
			//throw new IllegalArgumentException( "Invalid start port: " + port );
			return false;
		}

		ServerSocket servSocket = null;
		DatagramSocket dataSock = null;
		try {
			servSocket = new ServerSocket(port);
			servSocket.setReuseAddress(true);
			servSocket.close();
			dataSock = new DatagramSocket(port);
			dataSock.setReuseAddress(true);
			dataSock.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (dataSock != null) {
				dataSock.close();
			}

			if (servSocket != null) {
				try {
					servSocket.close();
				} catch (IOException e) {
					/* should not be thrown */
				}
			}
		}

		return false;
	}
	
	/**
	 *	Restart the server
	 */
	public void restart ()
	{
		if ( running ) shutDown();
		
		start();
	}
	
	/**
	 *	Stop the server
	 *	With a little help from: http://bit.ly/eA8iGj
	 */
	public void shutDown ()
	{
		//System.out.println("Shutting down");
		/*if ( threads != null )
		{
			for ( Worker w : threads )
			{
				w.stop();
			}
		}*/
		
		thread = null;
		try {
			if ( server != null )
			{
				server.close();
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		
		if ( listeners != null )
		{
			for ( BasicServerListener l : listeners ) 
			{
				l.serverStopped();
			}
		}
	}
	
	/**
	 *	Getter, return if server is running
	 *
	 *	@return true if server is running
	 */
	public boolean isRunning ()
	{
		return running && inited;
	}

	/**
	 *	interface Runnable
	 *	Called on a new thread to do handle the requests coming in
	 * 
	 *	@see <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Runnable.html">java.lang.Runnable</a>
	 */	
	public void run ()
	{	
		try
		{
			running = true;
	
			if ( port < 0 )
			{
				server = new ServerSocket( 0 );
				port = server.getLocalPort();
				/* self assigned free port */
			}
			else
			{
				server = new ServerSocket( port );
			}
			
		} catch ( IOException ioe ) {
			// problem starting server!
			System.err.println(ioe);
		} catch ( SecurityException se ) {
			System.err.println(se);
		}
		
		try 
		{	
			if ( server != null )
			{
				inited = true;
				
				if ( listeners != null )
				{
					for ( BasicServerListener l : listeners ) 
					{
						l.serverStarted();
					}
				}
				
				while ( thread != null )
				{
					Socket s = server.accept();

					Worker ws = new Worker( virtualRoot );
					ws.setSocket(s);
					//threads.add(ws);
					(new Thread(ws, "Processing.BasicServer.Worker")).start();
				}
			}
			else
			{
				System.err.println( "Server is null. Bad. Really." );
			}
		} catch ( IOException ioe ) {
			// happens on shutDown(), ignore ..
			//System.err.println( ioe );
		}
		
		running = false;
	}
}

/**
 *	Worker class, handles the actual serving of files
 */
class Worker extends BasicServer 
implements HttpConstants, Runnable
{
	final static int BUF_SIZE = 2048;

	static final byte[] EOL = {(byte)'\r', (byte)'\n' };

	/* buffer to use for requests */
	byte[] buf;
	/* Socket to client we're handling */
	private Socket socket;

	private boolean stopping = false;
	
	/* mapping of file extensions to content-types */
	static java.util.Hashtable map = new java.util.Hashtable();

	static {
		fillMap();
	}
	
	/**
	 *	Constructor
	 *
	 *	@param root the root folder to serve from
	 */
	Worker ( File root ) 
	{
		super( root );
		buf = new byte[BUF_SIZE];
		socket = null;
	}
	
	/**
	 *	Set socket, this is the actual request
	 */
	synchronized void setSocket( Socket s ) 
	{
		socket = s;
		notify();
	}
	
	/**
	 *	Try to stop it
	 */
	synchronized void stop ()
	{
		stopping = true;
		notify();
	}
	
	/**
	 *	interface Runnable
	 *	Called to do the heavy lifting
	 *
	 *	@see <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Runnable.html">java.lang.Runnable</a>
	 */
	public synchronized void run ()
	{
		while ( true && !stopping )
		{
			if ( socket == null ) 
			{
				/* nothing to do */
				try {
					wait();
				} catch (InterruptedException e) {
					/* should not happen */
					continue;
				}
			}
			if ( socket != null && !stopping )
			{
				try {
					handleClient();
				} catch (Exception e) {
					// TODO check why this is raised instead of 
					// sending 404 for favicon.ico, etc. ..
					//e.printStackTrace();
				}
			}
			socket = null;
			return;
		}
	}

	/**
	 *	Handle a request from a client
	 */
	void handleClient() throws IOException 
	{
		InputStream is = new BufferedInputStream( socket.getInputStream() );
		PrintStream ps = new PrintStream( socket.getOutputStream() );
		
		/* we will only block in read for this many milliseconds
		 * before we fail with java.io.InterruptedIOException,
		 * at which point we will abandon the connection.
		 */
		socket.setSoTimeout( getTimeout() );
		socket.setTcpNoDelay(true);
		
		/* zero out the buffer from last time */
		for (int i = 0; i < BUF_SIZE; i++) {
			buf[i] = 0;
		}
		try {
			/* We only support HTTP GET/HEAD, and don't
			 * support any fancy HTTP options,
			 * so we're only interested really in
			 * the first line.
			 */
			int nread = 0, r = 0;

outerloop:
			while (nread < BUF_SIZE) 
			{
				r = is.read(buf, nread, BUF_SIZE - nread);
				if (r == -1) {
					/* EOF */
					return;
				}
				int i = nread;
				nread += r;
				for (; i < nread; i++) {
					if (buf[i] == (byte)'\n' || buf[i] == (byte)'\r') {
						/* read one line */
						break outerloop;
					}
				}
			}

			/* are we doing a GET or just a HEAD */
			boolean doingGet;
			/* beginning of file name */
			int index;
			if (buf[0] == (byte)'G' &&
				buf[1] == (byte)'E' &&
				buf[2] == (byte)'T' &&
				buf[3] == (byte)' ') {
				doingGet = true;
				index = 4;
			} else if (buf[0] == (byte)'H' &&
					   buf[1] == (byte)'E' &&
					   buf[2] == (byte)'A' &&
					   buf[3] == (byte)'D' &&
					   buf[4] == (byte)' ') {
				doingGet = false;
				index = 5;
			} else {
				/* we don't support this method */
				ps.print("HTTP/1.0 " + HTTP_BAD_METHOD +
						   " unsupported method type: ");
				ps.write(buf, 0, 5);
				ps.write(EOL);
				ps.flush();
				socket.close();
				return;
			}

			int i = 0;
			/* find the file name, from:
			 * GET /foo/bar.html HTTP/1.0
			 * extract "/foo/bar.html"
			 */
			for (i = index; i < nread; i++) {
				if (buf[i] == (byte)' ') {
					break;
				}
			}

			String fname = (new String(buf, index, i-index)).
									replace('/', File.separatorChar);
			if (fname.startsWith(File.separator)) 
			{
				fname = fname.substring(1);
			}

			fname = java.net.URLDecoder.decode(fname, "UTF-8");
			
			// TODO
			//implement a logger service that will receive messages from p.js?
			//processing-1.2.1-examples/examples/seneca/log/customLogger.html
			if ( fname.startsWith("logger?") )
			{
				System.out.println(fname.substring(7));  
				// TODO somewhere on the way the encoding gets screw'd
			}
			else
			{		
				File targ = new File( getRoot(), fname );
				if (targ.isDirectory()) 
				{
					File ind = new File(targ, "index.html");
					if (ind.exists()) {
						targ = ind;
					}
				}

				boolean OK = printHeaders(targ, ps);
				if (doingGet) 
				{
					if (OK) {
						sendFile(targ, ps);
					} else {
						send404(targ, ps);
					}
				}
			}
		} finally {
			socket.close();
		}
	}

	/**
	 *	Send the default headers to client
	 */
	boolean printHeaders ( File targ, PrintStream ps ) throws IOException 
	{
		boolean ret = false;

		if (!targ.exists()) 
		{
			ps.print("HTTP/1.0 " + HTTP_NOT_FOUND + " not found");
			ps.write(EOL);
			ret = false;
		}  else {
			ps.print("HTTP/1.0 " + HTTP_OK + " OK");
			ps.write(EOL);
			ret = true;
		}
		// System.out.println(String.format(
		// 	"From %s: GET %s --> %s",
		// 	 socket.getInetAddress().getHostAddress(),
		// 	targ.getAbsolutePath(),
		// 	(ret ? "200" : "404")
		// ));
		
		ps.print("Server: Processing/2.0");
		ps.write(EOL);
		ps.print("Date: " + (new Date()));
		ps.write(EOL);
		
		// ps.print("Access-Control-Allow-Origin: *");
		// ps.write(EOL);
		// ps.print("Access-Control-Request-Method: *");
		// ps.write(EOL);
		
		if (ret) 
		{
			if (!targ.isDirectory()) 
			{
				ps.print("Content-length: "+targ.length());
				ps.write(EOL);
				ps.print("Last Modified: " + (new
							  Date(targ.lastModified())));
				ps.write(EOL);
				String name = targ.getName();
				int ind = name.lastIndexOf('.');
				String ct = null;
				if (ind > 0) 
				{
					ct = (String) map.get(name.substring(ind));
				}
				if (ct == null) 
				{
					ct = "unknown/unknown";
				}
				ps.print("Content-type: " + ct);
				ps.write(EOL);
			} else {
				ps.print("Content-type: text/html");
				ps.write(EOL);
			}
		}
		return ret;
	}
	
	/**
	 *	Send 404 content to client
	 */
	void send404(File targ, PrintStream ps) throws IOException 
	{
		ps.write(EOL);
		ps.write(EOL);
		ps.println("Not Found\n\n"+
				   "The requested resource was not found.\n");
	}
	
	
	/**
	 *	Build list of contents and send it to client
	 */
	void listDirectory ( File dir, PrintStream ps ) throws IOException
	{
		ps.println("<TITLE>Directory listing</TITLE><P>\n");
		ps.println("<A HREF=\"..\">Parent Directory</A><BR>\n");
		String[] list = dir.list();
		for (int i = 0; list != null && i < list.length; i++) {
			File f = new File(dir, list[i]);
			if (f.isDirectory()) {
				ps.println("<A HREF=\""+list[i]+"/\">"+list[i]+"/</A><BR>");
			} else {
				ps.println("<A HREF=\""+list[i]+"\">"+list[i]+"</A><BR");
			}
		}
		ps.println("<P><HR><BR><I>" + (new Date()) + "</I>");
	}
	
	/**
	 *	
	 */
	void logAndSendNop (PrintStream ps) throws IOException 
	{
		ps.write(EOL);
		ps.write(EOL);
		ps.println("\n");
	}
	
	/**
	 *	Send contents of a file to client
	 */
	void sendFile(File targ, PrintStream ps) throws IOException 
	{
		InputStream is = null;
		ps.write(EOL);
		if (targ.isDirectory()) {
			listDirectory(targ, ps);
			return;
		} else {
			is = new FileInputStream(targ.getAbsolutePath());
		}

		try {
			int n;
			while ((n = is.read(buf)) > 0) {
				ps.write(buf, 0, n);
			}
		} finally {
			is.close();
		}
	}
	
	/**
	 *	Add a mapping of file extension to mime-type / content-type
	 */
	static void setSuffix ( String k, String v ) 
	{
		map.put(k, v);
	}
	
	/**
	 *	Add some default mappings
	 */
	static void fillMap ()
	{
		// this probably can be shortened a lot since this is not a normal server ..
		setSuffix("",		   "content/unknown");
		setSuffix(".3dm",	  "x-world/x-3dmf"); 
		setSuffix(".3dmf",	  "x-world/x-3dmf"); 
		setSuffix(".ai",	  "application/pdf");
		setSuffix(".aif",	  "audio/x-aiff");
		setSuffix(".aifc",	  "audio/x-aiff");
		setSuffix(".aiff",	  "audio/x-aiff");
		setSuffix(".asc",	   "text/plain");
		setSuffix(".asd",	  "application/astound");
		setSuffix(".asn",	  "application/astound");
		setSuffix(".atom",	   "application/atom+xml");
		setSuffix(".au",	   "audio/basic");
		setSuffix(".avi",	  "video/x-msvideo");
		setSuffix(".avi",	   "video/x-msvideo");
		setSuffix(".bcpio",	  "application/x-bcpio");
		setSuffix(".bin",	  "application/octet-stream");
		setSuffix(".bmp",	   "image/bmp");
		setSuffix(".c",	   "text/plain");
		setSuffix(".c++",	  "text/plain");
		setSuffix(".cab",	  "application/x-shockwave-flash");
		setSuffix(".cc",	  "text/plain");
		setSuffix(".cdf",	  "application/x-netcdf");
		setSuffix(".cgm",	   "image/cgm");
		setSuffix(".chm",	  "application/mshelp");
		setSuffix(".cht",	  "audio/x-dspeeh");
		setSuffix(".class",	  "application/octet-stream");
		setSuffix(".cod",	  "image/cis-cod");
		setSuffix(".coffee",  "text/coffeescript");
		setSuffix(".com",	  "application/octet-stream"); 
		setSuffix(".cpio",	  "application/x-cpio");
		setSuffix(".cpt",	   "application/mac-compactpro");
		setSuffix(".csh",	  "application/x-csh");
		setSuffix(".css",	  "text/css");
		setSuffix(".csv",	  "text/comma-separated-values");
		setSuffix(".dcr",	   "application/x-director");
		setSuffix(".dif",	   "video/x-dv");
		setSuffix(".dir",	   "application/x-director");
		setSuffix(".djv",	   "image/vnd.djvu");
		setSuffix(".djvu",	   "image/vnd.djvu");
		setSuffix(".dll",	  "application/octet-stream"); 
		setSuffix(".dmg",	   "application/octet-stream");
		setSuffix(".dms",	   "application/octet-stream");
		setSuffix(".doc",	  "application/msword");
		setSuffix(".dot",	  "application/msword");
		setSuffix(".dtd",	   "application/xml-dtd");
		setSuffix(".dus",	  "audio/x-dspeeh");
		setSuffix(".dv",	   "video/x-dv");
		setSuffix(".dvi",	  "application/x-dvi");
		setSuffix(".dwf",	  "drawing/x-dwf");
		setSuffix(".dwg",	  "application/acad");
		setSuffix(".dxf",	  "application/dxf");
		setSuffix(".dxr",	  "application/x-director");
		setSuffix(".eps",	  "application/pdf");
		setSuffix(".es",	  "audio/echospeech");
		setSuffix(".etx",	  "text/x-setext");
		setSuffix(".etx",	   "text/x-setext");
		setSuffix(".evy",	  "application/x-envoy");
		setSuffix(".exe",	  "application/octet-stream");
		setSuffix(".ez",	   "application/andrew-inset");
		setSuffix(".fh4",	  "image/x-freehand"); 
		setSuffix(".fh5",	  "image/x-freehand"); 
		setSuffix(".fhc",	  "image/x-freehand");
		setSuffix(".fif",	  "image/fif");
		setSuffix(".gif",	  "image/gif");
		setSuffix(".gram",	   "application/srgs");
		setSuffix(".grxml",   "application/srgs+xml");
		setSuffix(".gtar",	  "application/x-gtar");
		setSuffix(".gtar",	   "application/x-gtar");
		setSuffix(".gz",	  "application/gzip");
		setSuffix(".h",	   "text/plain");
		setSuffix(".hdf",	  "application/x-hdf");
		setSuffix(".hlp",	  "application/mshelp"); 
		setSuffix(".hqx",	  "application/mac-binhex40");
		setSuffix(".htm",	  "text/html");
		setSuffix(".html",	  "text/html");
		setSuffix(".ice",	   "x-conference/x-cooltalk");
		setSuffix(".ico",	  "image/x-icon");
		setSuffix(".ics",	   "text/calendar");
		setSuffix(".ief",	  "image/ief");
		setSuffix(".ifb",	   "text/calendar");
		setSuffix(".iges",	   "model/iges");
		setSuffix(".igs",	   "model/iges");
		setSuffix(".java",	  "text/plain");
		setSuffix(".jnlp",	   "application/x-java-jnlp-file");
		setSuffix(".jp2",	   "image/jp2");
		setSuffix(".jpe",	  "image/jpeg");
		setSuffix(".jpeg",	  "image/jpeg");
		setSuffix(".jpg",	  "image/jpeg");
		setSuffix(".js",	  "text/javascript");
		setSuffix(".kar",	   "audio/midi");
		setSuffix(".latex",	  "application/x-latex");
		setSuffix(".latex",   "application/x-latex");
		setSuffix(".lha",	   "application/octet-stream");
		setSuffix(".lzh",	   "application/octet-stream");
		setSuffix(".m3u",	   "audio/x-mpegurl");
		setSuffix(".m4a",	   "audio/mp4a-latm");
		setSuffix(".m4b",	   "audio/mp4a-latm");
		setSuffix(".m4p",	   "audio/mp4a-latm");
		setSuffix(".m4u",	   "video/vnd.mpegurl");
		setSuffix(".m4v",	   "video/m4v");
		setSuffix(".mac",	   "image/x-macpaint");
		setSuffix(".man",	  "application/x-troff-man");
		setSuffix(".mathml",  "application/mathml+xml");
		setSuffix(".mbd",	  "application/mbedlet");
		setSuffix(".mcf",	  "image/vasa");
		setSuffix(".me",	  "application/x-troff-me"); 
		setSuffix(".mesh",	   "model/mesh");
		setSuffix(".mid",	   "audio/midi");
		setSuffix(".midi",	   "audio/midi");
		setSuffix(".mif",	  "application/mif");
		setSuffix(".mov",	  "video/quicktime");
		setSuffix(".movie",	  "video/x-sgi-movie");
		setSuffix(".mp2",	   "audio/mpeg");
		setSuffix(".mp3",	   "audio/mpeg");
		setSuffix(".mp4",	   "video/mp4");
		setSuffix(".mpe",	  "video/mpeg");
		setSuffix(".mpeg",	  "video/mpeg");
		setSuffix(".mpg",	  "video/mpeg");
		setSuffix(".mpga",	   "audio/mpeg");
		setSuffix(".ms",	   "application/x-troff-ms");
		setSuffix(".msh",	   "model/mesh");
		setSuffix(".mxu",	   "video/vnd.mpegurl");
		setSuffix(".nc",	  "application/x-netcdf");
		setSuffix(".nsc",	  "application/x-nschat");
		setSuffix(".oda",	  "application/oda");
		setSuffix(".oga",	   "audio/ogg");
		setSuffix(".ogg",	   "application/ogg");
		setSuffix(".ogv",	   "video/ogg");
		setSuffix(".pbm",	  "image/x-portable-bitmap");
		setSuffix(".pct",	   "image/pict");
		setSuffix(".pdb",	   "chemical/x-pdb");
		setSuffix(".pde",	  "text/plain");
		setSuffix(".pdf",	  "application/pdf");
		setSuffix(".pgm",	  "image/x-portable-graymap");
		setSuffix(".pgn",	   "application/x-chess-pgn");
		setSuffix(".php",	  "application/x-httpd-php");
		setSuffix(".phtml",	  "application/x-httpd-php");
		setSuffix(".pic",	   "image/pict");
		setSuffix(".pict",	   "image/pict");
		setSuffix(".pl",	  "text/plain");
		setSuffix(".png",	  "image/png");
		setSuffix(".pnm",	  "image/x-portable-anymap");
		setSuffix(".pnt",	   "image/x-macpaint");
		setSuffix(".pntg",	   "image/x-macpaint");
		setSuffix(".pot",	  "application/mspowerpoint");
		setSuffix(".ppm",	  "image/x-portable-pixmap");
		setSuffix(".pps",	  "application/mspowerpoint"); 
		setSuffix(".ppt",	  "application/mspowerpoint");
		setSuffix(".ppz",	  "application/mspowerpoint"); 
		setSuffix(".ps",	  "application/postscript");
		setSuffix(".ps",	   "application/postscript");
		setSuffix(".ptlk",	  "application/listenup");
		setSuffix(".qd3",	  "x-world/x-3dmf");
		setSuffix(".qd3d",	  "x-world/x-3dmf"); 
		setSuffix(".qt",	  "video/quicktime");
		setSuffix(".qti",	   "image/x-quicktime");
		setSuffix(".qtif",	   "image/x-quicktime");
		setSuffix(".ra",	  "audio/x-pn-realaudio");
		setSuffix(".ra",	   "audio/x-pn-realaudio");
		setSuffix(".ram",	  "audio/x-mpeg"); 
		setSuffix(".ras",	  "image/cmu-raster");
		setSuffix(".rdf",	   "application/rdf+xml");
		setSuffix(".rgb",	  "image/x-rgb");
		setSuffix(".rm",	   "application/vnd.rn-realmedia");
		setSuffix(".roff",	  "application/x-troff");
		setSuffix(".rpm",	  "audio/x-pn-realaudio-plugin");
		setSuffix(".rtc",	  "application/rtc");
		setSuffix(".rtf",	   "text/rtf");
		setSuffix(".rtx",	  "text/richtext");
		setSuffix(".sca",	  "application/x-supercard");
		setSuffix(".sgm",	   "text/sgml");
		setSuffix(".sgml",	   "text/sgml");
		setSuffix(".sh",	  "application/x-sh");
		setSuffix(".shar",	   "application/x-shar");
		setSuffix(".shtml",	  "text/html");
		setSuffix(".silo",	   "model/mesh");
		setSuffix(".sit",	  "application/x-stuffit");
		setSuffix(".skd",	   "application/x-koan");
		setSuffix(".skm",	   "application/x-koan");
		setSuffix(".skp",	   "application/x-koan");
		setSuffix(".skt",	   "application/x-koan");
		setSuffix(".smi",	   "application/smil");
		setSuffix(".smil",	   "application/smil");
		setSuffix(".smp",	  "application/studiom");
		setSuffix(".snd",	  "audio/basic");
		setSuffix(".so",	   "application/octet-stream");
		setSuffix(".spc",	  "text/x-speech");
		setSuffix(".spl",	  "application/futuresplash");
		setSuffix(".spr",	  "application/x-sprite"); 
		setSuffix(".sprite",  "application/x-sprite");
		setSuffix(".src",	  "application/x-wais-source");
		setSuffix(".stream",  "audio/x-qt-stream");
		setSuffix(".sv4cpio", "application/x-sv4cpio");
		setSuffix(".sv4crc",  "application/x-sv4crc");
		setSuffix(".svg",	   "image/svg+xml");
		setSuffix(".swf",	  "application/x-shockwave-flash"); 
		setSuffix(".t",	   "application/x-troff");
		setSuffix(".talk",	  "text/x-speech"); 
		setSuffix(".tar",	  "application/x-tar");
		setSuffix(".tbk",	  "application/toolbook");
		setSuffix(".tcl",	  "application/x-tcl");
		setSuffix(".tex",	  "application/x-tex");
		setSuffix(".texi",	  "application/x-texinfo");
		setSuffix(".texinfo", "text/plain"); 
		setSuffix(".text",	  "text/plain");
		setSuffix(".tif",	  "image/tiff");
		setSuffix(".tiff",	  "image/tiff");
		setSuffix(".tr",	  "application/x-troff"); 
		setSuffix(".troff",	  "application/x-troff-man");
		setSuffix(".tsi",	  "audio/tsplayer");
		setSuffix(".tsp",	  "application/dsptype");
		setSuffix(".tsv",	  "text/tab-separated-values");
		setSuffix(".tsv",	   "text/tab-separated-values");
		setSuffix(".txt",	  "text/plain");
		setSuffix(".ustar",	  "application/x-ustar");
		setSuffix(".uu",	  "application/octet-stream");
		setSuffix(".vcd",	   "application/x-cdlink");
		setSuffix(".viv",	  "video/vnd.vivo"); 
		setSuffix(".vivo",	  "video/vnd.vivo");
		setSuffix(".vmd",	  "application/vocaltec-media-desc");
		setSuffix(".vmf",	  "application/vocaltec-media-file");
		setSuffix(".vox",	  "audio/voxware");
		setSuffix(".vrml",	   "model/vrml");
		setSuffix(".vts",	  "workbook/formulaone"); 
		setSuffix(".vtts",	  "workbook/formulaone");
		setSuffix(".vxml",	   "application/voicexml+xml");
		setSuffix(".wav",	  "audio/x-wav");
		setSuffix(".wav",	   "audio/x-wav");
		setSuffix(".wbmp",	  "image/vnd.wap.wbmp");
		setSuffix(".wbmxl",   "application/vnd.wap.wbxml");
		setSuffix(".webm",	  "video/webm");
		setSuffix(".wml",	  "text/vnd.wap.wml");
		setSuffix(".wmlc",	  "application/vnd.wap.wmlc");
		setSuffix(".wmls",	  "text/vnd.wap.wmlscript");
		setSuffix(".wmlsc",	  "application/vnd.wap.wmlscriptc");
		setSuffix(".wrl",	  "model/vrml");
		setSuffix(".xbm",	  "image/x-xbitmap");
		setSuffix(".xht",	   "application/xhtml+xml");
		setSuffix(".xhtml",	  "application/xhtml+xml");
		setSuffix(".xla",	  "application/msexcel");
		setSuffix(".xls",	  "application/msexcel");
		setSuffix(".xml",	  "text/xml");
		setSuffix(".xpm",	  "image/x-xpixmap");
		setSuffix(".xsl",	   "application/xml");
		setSuffix(".xslt",	   "application/xslt+xml");
		setSuffix(".xul",	   "application/vnd.mozilla.xul+xml");
		setSuffix(".xwd",	  "image/x-windowdump");
		setSuffix(".xyz",	   "chemical/x-xyz");
		setSuffix(".z",	   "application/x-compress");
		setSuffix(".zip",	  "application/zip");
	}
}

/**
 *	Needed HTTP constants/codes
 *
 *	@see <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes">Wikipedia: List of HTTP codes</a>
 */
interface HttpConstants {
	/** 2XX: generally "OK" */
	public static final int HTTP_OK = 200;
	public static final int HTTP_CREATED = 201;
	public static final int HTTP_ACCEPTED = 202;
	public static final int HTTP_NOT_AUTHORITATIVE = 203;
	public static final int HTTP_NO_CONTENT = 204;
	public static final int HTTP_RESET = 205;
	public static final int HTTP_PARTIAL = 206;

	/** 3XX: relocation/redirect */
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;

	/** 4XX: client error */
	public static final int HTTP_BAD_REQUEST = 400;
	public static final int HTTP_UNAUTHORIZED = 401;
	public static final int HTTP_PAYMENT_REQUIRED = 402;
	public static final int HTTP_FORBIDDEN = 403;
	public static final int HTTP_NOT_FOUND = 404;
	public static final int HTTP_BAD_METHOD = 405;
	public static final int HTTP_NOT_ACCEPTABLE = 406;
	public static final int HTTP_PROXY_AUTH = 407;
	public static final int HTTP_CLIENT_TIMEOUT = 408;
	public static final int HTTP_CONFLICT = 409;
	public static final int HTTP_GONE = 410;
	public static final int HTTP_LENGTH_REQUIRED = 411;
	public static final int HTTP_PRECON_FAILED = 412;
	public static final int HTTP_ENTITY_TOO_LARGE = 413;
	public static final int HTTP_REQ_TOO_LONG = 414;
	public static final int HTTP_UNSUPPORTED_TYPE = 415;

	/** 5XX: server error */
	public static final int HTTP_SERVER_ERROR = 500;
	public static final int HTTP_INTERNAL_ERROR = 501;
	public static final int HTTP_BAD_GATEWAY = 502;
	public static final int HTTP_UNAVAILABLE = 503;
	public static final int HTTP_GATEWAY_TIMEOUT = 504;
	public static final int HTTP_VERSION = 505;
}

/**
 *	Interface BasicServerListener
 */
interface BasicServerListener
{
	public abstract void serverStarted();
	public abstract void serverStopped();
}