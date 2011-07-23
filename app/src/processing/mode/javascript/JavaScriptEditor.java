package processing.mode.javascript;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import processing.app.Base;
import processing.app.Settings;
import processing.app.Editor;
import processing.app.EditorToolbar;
import processing.app.Sketch;
import processing.app.Formatter;
import processing.app.Mode;
import processing.mode.java.AutoFormat;

import javax.swing.*;

public class JavaScriptEditor extends Editor 
{
	final static String PROP_KEY_MODE = "mode";
	final static String PROP_VAL_MODE = "JavaScript";
	final static String PROP_KEY_SERVER_PORT = "js.server.port";
	
  private JavaScriptMode jsMode;
  private JavaScriptServer jsServer;

  private DirectivesEditor directivesEditor;

  // tapping into Java mode might not be wanted?
  processing.mode.java.PdeKeyListener listener;

  protected JavaScriptEditor ( Base base, String path, int[] location, Mode mode ) 
  {
    super(base, path, location, mode);

	listener = new processing.mode.java.PdeKeyListener(this,textarea);
	
    jsMode = (JavaScriptMode) mode;
  }

  
  public EditorToolbar createToolbar () 
  {
    return new JavaScriptToolbar(this, base);
  }

  
  public Formatter createFormatter () 
  { 
    return new AutoFormat();    
  }

  
  // - - - - - - - - - - - - - - - - - -
  // Menu methods

  
  public JMenu buildFileMenu () 
  {
    JMenuItem exportItem = Base.newJMenuItem("Export", 'E');
    exportItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExport( true );
      }
    });
    return buildFileMenu(new JMenuItem[] { exportItem });
  }


  public JMenu buildSketchMenu () 
  {
	JMenuItem startServerItem = Base.newJMenuItem("Start server", 'R');
    startServerItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStartServer();
        }
      });

    JMenuItem stopServerItem = new JMenuItem("Stop server");
    stopServerItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStopServer();
        }
      });

	JMenuItem copyServerAddressItem = new JMenuItem("Copy server address");
	copyServerAddressItem.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			handleCopyServerAddress();
		}
	});
	// copyServerAddressItem.getInputMap().put(
	// 	javax.swing.KeyStroke.getKeyStroke('C', java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.META_MASK ),
	// 	new AbstractAction () {
	// 		public void actionPerformed ( ActionEvent e ) {
	// 			handleCopyServerAddress();
	// 		}
	// 	}
	// );
	
	JMenuItem setServerPortItem = new JMenuItem("Set server port");
	setServerPortItem.addActionListener(new ActionListener(){
		public void actionPerformed (ActionEvent e) {
			handleSetServerPort();
		}
	});

    return buildSketchMenu(new JMenuItem[] {
		startServerItem, stopServerItem, 
		copyServerAddressItem, setServerPortItem
	});
  }

  public JMenu buildModeMenu() {
    JMenu menu = new JMenu("JavaScript");    
    JMenuItem item;

	item = new JMenuItem("Playback settings (directives)");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      handleShowDirectivesEditor();
		}
	});
	menu.add(item);

    menu.addSeparator();

	item = new JMenuItem("Start custom template");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		  handleCreateCustomTemplate();
		}
	});
	menu.add(item);

	item = new JMenuItem("Show custom template");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		  handleOpenCustomTemplateFolder();
		}
	});
	menu.add(item);

    return menu;
  }
  
  public JMenu buildHelpMenu () 
  {
    JMenu menu = new JMenu("Help ");
    JMenuItem item;

	// TODO switch to "http://js.processing.org/"?

    item = new JMenuItem("QuickStart for JS Devs");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://processingjs.org/reference/articles/jsQuickStart");
      }
    });
    menu.add(item);

    item = new JMenuItem("QuickStart for Processing Devs");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Base.openURL("http://processingjs.org/reference/articles/p5QuickStart");
      }
    });
    menu.add(item);

    /* TODO Implement an environment page
    item = new JMenuItem("Environment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showReference("environment" + File.separator + "index.html");
        }
      });
    menu.add(item);
     */

    /* TODO Implement a troubleshooting page
    item = new JMenuItem("Troubleshooting");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/Troubleshooting");
        }
      });
    menu.add(item);
     */

    item = new JMenuItem("Reference");
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //TODO get offline reference archive corresponding to the release 
        // packaged with this mode see: P.js ticket 1146 "Offline Reference"
        Base.openURL("http://processingjs.org/reference");
      }
    });
    menu.add(item);

    item = Base.newJMenuItemShift("Find in Reference", 'F');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleFindReferenceHACK();
      }
    });
    menu.add(item);

    /* TODO FAQ
    item = new JMenuItem("Frequently Asked Questions");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://wiki.processing.org/w/FAQ");
        }
      });
    menu.add(item);
    */
    
    item = new JMenuItem("Visit Processingjs.org");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processingjs.org/");
        }
      });
    menu.add(item);
    
    // OSX has its own about menu
    if (!Base.isMacOS()) {
      menu.addSeparator();
      item = new JMenuItem("About Processing");
      item.addActionListener( new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleAbout();
        }
      });
      menu.add(item);
    }
    
    return menu;
  }

  
  // - - - - - - - - - - - - - - - - - -
  
  
  public String getCommentPrefix () 
  { 
    return "//";
  }
  
  // - - - - - - - - - - - - - - - - - -

  private void handleSetServerPort ()
  {
	statusEmpty();
	
	String pString = null;
	String msg = "Set the server port (1024 < port < 65535)";
	int currentPort = -1;
	
	if ( jsServer != null ) currentPort = jsServer.getPort();
	
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
	
	createJavaScriptServer();
	if ( jsServer != null )
	{
		jsServer.setPort(port);
	}
	
	saveSketchSettings();
  }

  private void saveSketchSettings ()
  {
	statusEmpty();

	File sketchProps = getSketchPropertiesFile();
	if ( !sketchProps.exists() )
	{
		try {
			sketchProps.createNewFile();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			statusError( "Unable to create sketch properties file!" );
			return;
		}
	}
	
	Settings settings;
	try {
		settings = new Settings(sketchProps);
	} catch ( IOException ioe ) {
		ioe.printStackTrace();
		return;
	}
	if ( settings == null )
	{
		statusError( "Unable to create sketch properties file!" );
		return;
	}
	settings.set( PROP_KEY_MODE, PROP_VAL_MODE );
	
	if ( jsServer != null )
	{
		int port = jsServer.getPort();
		if ( port > 0 ) settings.set( PROP_KEY_SERVER_PORT, (port+"") );
	}
	
	settings.save();
  }

  private void handleCreateCustomTemplate ()
  {
	Sketch sketch = getSketch();
	
	File ajs = sketch.getMode().
				getContentFile(JavaScriptBuild.EXPORTED_FOLDER_NAME);
				
	File tjs = getCustomTemplateFolder();
					
	if ( !tjs.exists() )
	{
		try {
			Base.copyDir( ajs, tjs );
			statusNotice( "Default template copied." );
			Base.openFolder( tjs );
		} catch ( java.io.IOException ioe ) {
			Base.showWarning("Copy default template folder", 
				"Something went wrong when copying the template folder.", ioe);
		}
	}
	else
		statusError( "You need to remove the current "+
				     "\""+JavaScriptBuild.TEMPLATE_FOLDER_NAME+"\" "+
					 "folder from the sketch." );
  }

  private void handleOpenCustomTemplateFolder ()
  {
  	File tjs = getCustomTemplateFolder();
	if ( tjs.exists() )
	{
		Base.openFolder( tjs );
	}
	else
	{
		// TODO: promt to create one?
		statusNotice( "You have no custom template with this sketch. Create one from the menu!" );
	}
  }

  private void handleCopyServerAddress ()
  {
	if ( jsServer != null && jsServer.isRunning() )
	{
		java.awt.datatransfer.StringSelection stringSelection = 
			new java.awt.datatransfer.StringSelection( jsServer.getAddress() );
	    java.awt.datatransfer.Clipboard clipboard = 
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents( stringSelection, null );
	}
  }

  private void handleShowDirectivesEditor ()
  {
	if ( directivesEditor == null )
	{
	  directivesEditor = new DirectivesEditor(this);
	}
	
	directivesEditor.show();
  }

  // this catches the textarea right-click events
  public void showReference( String filename )
  {
	// TODO: catch handleFindReference directly
	handleFindReferenceHACK();
  }

  private void handleFindReferenceHACK ()
  {
	if (textarea.isSelectionActive()) {
        Base.openURL(
          "http://www.google.com/search?q=" +
          textarea.getSelectedText().trim() + 
          "+site%3Ahttp%3A%2F%2Fprocessingjs.org%2Freference"
        );
     }
  }

  public void handleStartStopServer ()
  {
  	if ( jsServer != null && jsServer.isRunning())
    {
		handleStopServer();
	}
	else
	{
		handleStartServer();
	}
  }

  private File getExportFolder ()
  {
  	return new File( getSketch().getFolder(),
	 				 JavaScriptBuild.EXPORTED_FOLDER_NAME );
  }

  private File getSketchPropertiesFile ()
  {
  	return new File( getSketch().getFolder(), "sketch.properties");
  }

  private File getCustomTemplateFolder ()
  {
	return new File( getSketch().getFolder(), 
					 JavaScriptBuild.TEMPLATE_FOLDER_NAME );
  }
  
  /**
   *  Replacement for RUN: 
   *  export to folder, start server, open in default browser.
   */
  public void handleStartServer ()
  {
	statusEmpty();
	if ( !handleExport( false ) ) return;

	File serverRoot = getExportFolder();

	// if server hung or something else went wrong .. stop it.
	if ( jsServer != null && 
		 (!jsServer.isRunning() || !jsServer.getRoot().equals(serverRoot)) )
    {
		jsServer.shutDown();
		jsServer = null;
	}
	
    if ( jsServer == null )
	{
		jsServer = createJavaScriptServer();
		jsServer.start();
		
		while ( !jsServer.isRunning() ) {}
		
		String location = jsServer.getAddress();
		
		statusNotice( "Server started: " + location );
		
		Base.openURL( location );
	}
	else if ( jsServer.isRunning() )
	{
		statusNotice( "Server running (" + 
					  jsServer.getAddress() +
					  "), reload your browser window." );
	}
    toolbar.activate(JavaScriptToolbar.RUN);
  }

  private JavaScriptServer createJavaScriptServer ()
  {
	if ( jsServer != null ) return jsServer;
	
	jsServer = new JavaScriptServer( getExportFolder() );
	
    File sketchProps = getSketchPropertiesFile();
    if ( sketchProps.exists() ) {
		try {
        	Settings props = new Settings(sketchProps);
			String portString = props.get( PROP_KEY_SERVER_PORT );
			if ( portString != null && !portString.trim().equals("") )
			{
        		int port = Integer.parseInt(portString);
				jsServer.setPort(port);
			}
		} catch ( IOException ioe ) {
			statusError(ioe);
		}
    }

	return jsServer;
  }

  /**
   *  Replacement for STOP: stop server.
   */
  public void handleStopServer ()
  {
	if ( jsServer != null && jsServer.isRunning() )
		jsServer.shutDown();
	
	statusNotice("Server stopped.");
	toolbar.deactivate(JavaScriptToolbar.RUN);
  }
  
  /**
   * Call the export method of the sketch and handle the gui stuff
   */
  public boolean handleExport ( boolean openFolder ) 
  {
    if ( !handleExportCheckModified() )
    {
		return false;
	}
	else
	{
      toolbar.activate(JavaScriptToolbar.EXPORT);
      try 
	  {
        boolean success = jsMode.handleExport(sketch);
        if ( success && openFolder ) 
		{
          File appletJSFolder = new File( sketch.getFolder(),
 										  JavaScriptBuild.EXPORTED_FOLDER_NAME );
          Base.openFolder(appletJSFolder);

          statusNotice("Finished exporting.");
        } else if ( !success ) { 
          // error message already displayed by handleExport
	      return false;
        }
      } catch (Exception e) {
        statusError(e);
	    toolbar.deactivate(JavaScriptToolbar.EXPORT);
		return false;
      }
      toolbar.deactivate(JavaScriptToolbar.EXPORT);
    }
	return true;
  }
  
  /**
   *  Changed from Editor.java to automaticaly export and
   *  handle the server when it's running. Normal save ops otherwise.
   */
  public boolean handleSaveRequest(boolean immediately)
  {
    if (untitled) {
      return handleSaveAs();
      // need to get the name, user might also cancel here

    } else if (immediately) {
      handleSave();
	  if ( jsServer != null && jsServer.isRunning() )
		handleStartServer();
	  else
		statusEmpty();
    } else {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            handleSave();
			  if ( jsServer != null && jsServer.isRunning() )
				handleStartServer();
			  else
				statusEmpty();
          }
        });
    }
    return true;
  }
  
  public boolean handleExportCheckModified () 
  {
    if (sketch.isModified()) {
      Object[] options = { "OK", "Cancel" };
      int result = JOptionPane.showOptionDialog(this,
                                                "Save changes before export?",
                                                "Save",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);

      if (result == JOptionPane.OK_OPTION) {
        handleSaveRequest(true);

      } else {
        // why it's not CANCEL_OPTION is beyond me (at least on the mac)
        // but f-- it.. let's get this shite done..
        //} else if (result == JOptionPane.CANCEL_OPTION) {
        statusNotice("Export canceled, changes must first be saved.");
        //toolbar.clear();
        return false;
      }
    }
    return true;
  }
  
  
  public void handleSave () 
  { 
    toolbar.activate(JavaScriptToolbar.SAVE);
    super.handleSave();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
  }
  
  
  public boolean handleSaveAs () 
  { 
    toolbar.activate(JavaScriptToolbar.SAVE);
    boolean result = super.handleSaveAs();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
    return result;
  }


  public void handleImportLibrary (String item) 
  {
    Base.showWarning("Processing.js doesn't support libraries",
                     "Libraries are not supported. Import statements are " +
                     "ignored, and code relying on them will break.",
                     null);
  }

  /**
   *  Called when the window is going to be reused for another sketch.
   */
  public void internalCloseRunner()
  {
      handleStopServer();
	  if ( directivesEditor != null )
	  {
		directivesEditor.hide();
		directivesEditor = null;
	  }
  }

  public void deactivateRun ()
  {
      // not sure what to do here ..
  } 
}
