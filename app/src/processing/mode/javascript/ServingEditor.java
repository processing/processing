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

public abstract class ServingEditor extends Editor
{
	public final static String PROP_KEY_SERVER_PORT = "basicserver.port";
	
	BasicServer server;
	
	public boolean showSizeWarning = true;
	
	protected ServingEditor ( Base base, String path, EditorState state, Mode mode )
	{
		super( base, path, state, mode );
	}
	
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

		if ( port < 0 || port > 65535 )
		{
			statusError("That port number is out of range");
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
	
	protected int getServerPort ()
	{
		if ( server != null ) return server.getPort();
		return -1;
	}
	
	protected String getServerAddress ()
	{
		if ( server != null && server.isRunning() ) return server.getAddress();
		return null;
	}
	
	protected void startStopServer ( File root )
	{
		if ( server != null && server.isRunning() )
	    {
			stopServer();
		}
		else
		{
			startServer( root );
		}
	}
	
	protected BasicServer createServer ( File root )
	{
		if ( server != null ) return server;

		server = new BasicServer( root );

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
	
	protected void startServer ( File root )
	{
		if ( server != null && 
			 (!server.isRunning() || !server.getRoot().equals(root)) )
	    {
			// if server hung or something else went wrong .. stop it.
			server.shutDown();
			server = null;
		}

	    if ( server == null )
		{
			server = createServer(root);
			server.start();

			// a little delay to give the server time to kick in ..
			long ts = System.currentTimeMillis();
			while ( System.currentTimeMillis() - ts < 200 ) {}

			while ( !server.isRunning() ) {}

			String location = server.getAddress();

			statusNotice( "Server started: " + location );

			openBrowserForServer();
		}
		else if ( server.isRunning() )
		{
			statusNotice( "Server running (" + 
						  server.getAddress() +
						  "), reload your browser window." );
		}
	}
	
	protected boolean serverRunning ()
	{
		return server != null && server.isRunning();
	}
	
	protected void stopServer ()
	{
		if ( server != null && server.isRunning() )
			server.shutDown();

		statusNotice("Server stopped.");
	}
	
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
	
	protected void openBrowserForServer ()
	{
		if ( server != null && server.isRunning() )
		{
			Base.openURL( server.getAddress() );
		}
	}
}