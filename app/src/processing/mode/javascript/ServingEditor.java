package processing.mode.javascript;

import processing.mode.javascript.BasicServer;
import processing.app.Base;
import processing.app.Mode;
import processing.app.Editor;
import processing.app.EditorState;
import processing.app.Settings;

import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

/**
 *	This is the basis for the JavaScript mode, an editor that serves files from a
 *  given root directory.
 */
public abstract class ServingEditor extends Editor implements BasicServerListener
{
	public final static String PROP_KEY_SERVER_PORT = "basicserver.port";
	
	BasicServer server;
	
	public boolean showSizeWarning = true;
	
	/**
	 *	Constructor
	 *
	 *	@param base the Processing Base this runs of
	 *	@param path the path to a sketch to open
	 *	@param editor state
	 *	@param the mode to open in
	 *	@see processing.app.Editor
	 *	@see processing.app.Base
	 */
	protected ServingEditor ( Base base, String path, EditorState state, Mode mode )
	{
		super( base, path, state, mode );
	}
	
	/**
	 *	Set the server port, shows an input dialog to enter a port number
	 */
	protected void setServerPort ()
	{
		String pString = null;
		String msg = "Set the server port (1024 < port < 65535)";
		int currentPort = -1;

		if ( server != null ) currentPort = server.getPort();

		if ( currentPort > 0 )
			pString = JOptionPane.showInputDialog( msg, (currentPort+"") );
		else
			pString = JOptionPane.showInputDialog( msg );

		if ( pString == null ) return;

		int port = -1;
		try {
			port = Integer.parseInt(pString);
		} catch ( Exception e ) {
			// sending foobar? you lil' hacker you ...
			statusError("That number was not okay ..");
			return;
		}

		if ( port < BasicServer.MIN_PORT || port > BasicServer.MAX_PORT )
		{
			statusError( "That port number is out of range" );
			return;
		}
		
		if ( server != null )
		{
			server.setPort(port);
		}
		
		File sketchProps = getSketchPropertiesFile();
	    if ( sketchProps.exists() ) {
			try {
	        	Settings settings = new Settings(sketchProps);
				settings.set( PROP_KEY_SERVER_PORT, (port + "") );
				settings.save();
			} catch ( IOException ioe ) {
				statusError(ioe);
			}
	    }
	}
	
	/**
	 *	Getter, returns the server port
	 *
	 *	@return the server port as int or -1
	 */
	public int getServerPort ()
	{
		if ( server != null ) return server.getPort();
		return -1;
	}
	
	/**
	 *	Getter, returns the current server address
	 *
	 *	@return the server address as URL string or null
	 */
	public String getServerAddress ()
	{
		if ( server != null && server.isRunning() ) return server.getAddress();
		return null;
	}
	
	/**
	 *	Getter, returns the server
	 *
	 *	@return the BasicServer of this editor
	 */
	public BasicServer getServer ()
	{
		return server;
	}
	
	/**
	 *	A toggle to start/stop the server
	 *
	 *	@param root the root folder to start from if it needs to be started
	 */
	protected void startStopServer ( File root )
	{
		if ( serverRunning() )
	    {
			stopServer();
		}
		else
		{
			startServer( root );
		}
	}
	
	/**
	 *	Create a server to server from given root dir
	 *
	 *	@param root the root folder to server from
	 *	@return the BasicServer instance running or created
	 */
	protected BasicServer createServer ( File root )
	{
		if ( server != null ) return server;
		
		if ( !root.exists() && !root.mkdir() )
		{
			// bad .. let server handle the complaining ..
		}

		server = new BasicServer( root );
		server.addListener( this );

	    File sketchProps = getSketchPropertiesFile();
	    if ( sketchProps.exists() ) {
			try {
	        	Settings props = new Settings(sketchProps);
				String portString = props.get( PROP_KEY_SERVER_PORT );
				if ( portString != null && !portString.trim().equals("") )
				{
	        		int port = Integer.parseInt(portString);
					server.setPort(port);
				}
			} catch ( IOException ioe ) {
				statusError(ioe);
			}
	    }

		return server;
	}
	
	/**
	 *	Start the internal server for this sketch.
	 *
	 *	@param root the root folder for the server to serve from
	 *	@return true if it was started anew, false if it was running
	 */
	protected boolean startServer ( File root )
	{
		if ( server != null && 
			 ( !server.isRunning() || !server.getRoot().equals(root) ) )
	    {
			// if server hung or something else went wrong .. stop it.
			server.shutDown();
			server = null;
		}

	    if ( server == null )
		{
			server = createServer( root );
		}
		
		if ( !server.isRunning() )
		{
			server.setRoot( root );
			server.start();
			statusNotice( "Waiting for server to start ..." );
		}
		else if ( server.isRunning() )
		{
			statusNotice( "Server running (" + 
						  server.getAddress() +
						  "), reload your browser window." );
						
			return false;
		}
		
		return true;
	}
	
	/**
	 *	Check if server is running
	 *
	 *	@return true if server is running
	 */
	protected boolean serverRunning ()
	{
		return server != null && server.isRunning();
	}
	
	/**
	 *	Stop server
	 */
	protected void stopServer ()
	{
		if ( serverRunning() ) server.shutDown();
	}
	
	/**
	 *	Create or get the sketch's properties file
	 *
	 *	@return the sketch properties file or null
	 */
	protected File getSketchPropertiesFile ()
	{
		File sketchPropsFile = new File( getSketch().getFolder(), "sketch.properties");
		if ( !sketchPropsFile.exists() )
		{
			try {
				sketchPropsFile.createNewFile();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				statusError( "Unable to create sketch properties file!" );
				return null;
			}
		}
		return sketchPropsFile;
	}
	
	/**
	 *	Open a new browser window or tab with the server address
	 */
	protected void openBrowserForServer ()
	{
		if ( serverRunning() )
		{
			Base.openURL( server.getAddress() );
		}
	}
	
	// ------------------------------------------
	//  interface BasicServerListener
	// ------------------------------------------
	
	/**
	 *	interface BasicServerListener
	 *	Called after the server was started from the server thread
	 */
	public void serverStarted ()
	{
		String location = server.getAddress();
		statusNotice( "Server started: " + location );
		openBrowserForServer();
	}
	
	/**
	 *	interface BasicServerListener
	 *	Called from server thread after the server stopped
	 */
	public void serverStopped ()
	{
		statusNotice( "Server stopped." );
	}
}