package processing.mode.javascript;

import processing.mode.javascript.ServingEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import processing.app.*;
import processing.mode.java.AutoFormat;

import javax.swing.*;

public class JavaScriptEditor extends ServingEditor
{
	final static String PROP_KEY_MODE = "mode";
	final static String PROP_VAL_MODE = "JavaScript";

  private JavaScriptMode jsMode;

  private DirectivesEditor directivesEditor;

  // tapping into Java mode might not be wanted?
  processing.mode.java.PdeKeyListener listener;

	/**
	 *	Constructor, overrides ServingEditor( .. )
	 *
	 *	@see processing.mode.javascript.ServingEditor
	 */
  protected JavaScriptEditor ( Base base, String path, EditorState state, Mode mode )
  {
    super(base, path, state, mode);

	listener = new processing.mode.java.PdeKeyListener(this,textarea);

    jsMode = (JavaScriptMode) mode;
  }

	// ----------------------------------------
	//  abstract Editor implementations
	//  and standard overrides
	// ----------------------------------------

	/**
	 *	Create and return the toolbar (tools above text area),
	 *	implements abstract Editor.createToolbar(),
	 *	called in Editor constructor to add the toolbar to the window.
	 *
	 *	@return an EditorToolbar, in our case a JavaScriptToolbar
	 *	@see processing.mode.javascript.JavaScriptToolbar
	 */
  public EditorToolbar createToolbar ()
  {
    return new JavaScriptToolbar(this, base);
  }

	/**
	 *	Create a formatter to prettify code,
	 *	implements abstract Editor.createFormatter(),
	 *	called by Editor.handleAutoFormat() to handle menu item or shortcut
	 *
	 *	@return the formatter to handle formatting of code.
	 */
  public Formatter createFormatter ()
  {
    return new AutoFormat();
  }

	/**
	 *	Build the "File" menu,
	 *	implements abstract Editor.buildFileMenu(),
	 *	called by Editor.buildMenuBar() to generate the app menu for the editor window
	 *
	 *	@return JMenu containing the menu items for "File" menu
	 */
  public JMenu buildFileMenu ()
  {
    JMenuItem exportItem = Toolkit.newJMenuItem("Export", 'E');
    exportItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleExport( true );
      }
    });
    return buildFileMenu(new JMenuItem[] { exportItem });
  }

	/**
	 *	Build the "Sketch" menu,
	 *	implements abstract Editor.buildSketchMenu(),
	 *	called by Editor.buildMenuBar() to generate the app menu for the editor window
	 *
	 *	@return JMenu containing the menu items for "Sketch" menu
	 */
  public JMenu buildSketchMenu ()
  {
	JMenuItem startServerItem = Toolkit.newJMenuItem("Run in Browser", 'R');
    startServerItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStartServer();
        }
      });

	JMenuItem openInBrowserItem = Toolkit.newJMenuItem("Reopen in Browser", 'B');
    openInBrowserItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleOpenInBrowser();
        }
      });

    JMenuItem stopServerItem = new JMenuItem("Stop");
    stopServerItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStopServer();
        }
      });

    return buildSketchMenu(new JMenuItem[] {
		startServerItem,
		openInBrowserItem,
		stopServerItem
	});
  }

	/**
	 *	Build the mode menu,
	 *	overrides Editor.buildModeMenu(),
	 *	called by Editor.buildMenuBar() to generate the app menu for the editor window
	 *
	 *	@return JMenu containing the menu items for "JavaScript" menu
	 */
  public JMenu buildModeMenu()
  {
    JMenu menu = new JMenu("JavaScript");
    JMenuItem item;

	item = new JMenuItem("Playback Settings (Directives)");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	      handleShowDirectivesEditor();
		}
	});
	menu.add(item);

	JMenuItem copyServerAddressItem = new JMenuItem("Copy Server Address");
	copyServerAddressItem.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			handleCopyServerAddress();
		}
	});
	menu.add( copyServerAddressItem );

	JMenuItem setServerPortItem = new JMenuItem("Set Server Port");
	setServerPortItem.addActionListener(new ActionListener(){
		public void actionPerformed (ActionEvent e) {
			handleSetServerPort();
		}
	});
	menu.add( setServerPortItem );

    menu.addSeparator();

	item = new JMenuItem("Start Custom Template");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		  handleCreateCustomTemplate();
		}
	});
	menu.add(item);

	item = new JMenuItem("Show Custom Template");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		  handleOpenCustomTemplateFolder();
		}
	});
	menu.add(item);

    return menu;
  }

	/**
	 *	Build the "Help" menu,
	 *	implements abstract Editor.buildHelpMenu(),
	 *	called by Editor.buildMenuBar() to generate the app menu for the editor window
	 *
	 *	@return JMenu containing the menu items for "Help" menu
	 */
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

    item = Toolkit.newJMenuItemShift("Find in Reference", 'F');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleFindReferenceImpl();
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

	/**
	 *	Returns the default commenting prefix for comment/uncomment command,
	 *	implements abstract Editor.getCommentPrefix(),
	 *	called from Editor.handleCommentUncomment()
	 *
	 *	@return the comment prefix as String
	 */
  public String getCommentPrefix ()
  {
    return "//";
  }

	/**
	 *	Stop the runner, in our case this is the server,
	 *	implements abstract Editor.internalCloseRunner(),
	 *	called from Editor.prepareRun()
	 *
	 *  Called when the window is going to be reused for another sketch.
	 */
  public void internalCloseRunner ()
  {
      handleStopServer();
	  if ( directivesEditor != null )
	  {
		directivesEditor.hide();
		directivesEditor = null;
	  }
  }

	/**
	 *	Implements abstract Editor.deactivateRun()
	 */
  public void deactivateRun ()
  {
      // not sure what to do here ..
  }

	// ----------------------------------------
	//  handlers ... mainly for menu items
	// ----------------------------------------

	/**
	 *	Menu item callback, let's users set the server port number
	 */
  private void handleSetServerPort ()
  {
	statusEmpty();

	boolean wasRunning = serverRunning();
	if ( wasRunning )
	{
		statusNotice("Server was running, changing the port requires a restart.");
		stopServer();
	}

	setServerPort();
	saveSketchSettings();

	if ( wasRunning ) {
		startServer( getExportFolder() );
	}
  }

	/**
	 *	Menu item callback, copy basic template to sketch folder
	 */
  private void handleCreateCustomTemplate ()
  {
	Sketch sketch = getSketch();

	File ajs = sketch.getMode().
				getContentFile( JavaScriptBuild.TEMPLATE_FOLDER_NAME );

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

	/**
	 *	Menu item callback, open custom template folder from inside sketch folder
	 */
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

	/**
	 *	Menu item callback, copy server address to clipboard
	 */
  private void handleCopyServerAddress ()
  {
	String address = getServerAddress();

	if ( address != null )
	{
		java.awt.datatransfer.StringSelection stringSelection =
			new java.awt.datatransfer.StringSelection( address );
	    java.awt.datatransfer.Clipboard clipboard =
			java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	    clipboard.setContents( stringSelection, null );
	}
  }

	/**
	 *	Menu item callback, open the playback settings frontend
	 */
  private void handleShowDirectivesEditor ()
  {
	if ( directivesEditor == null )
	{
	  directivesEditor = new DirectivesEditor(this);
	}

	directivesEditor.show();
  }

	/**
	 *	Catches textarea right-click events,
	 *	overrides Editor.showReference()
	 *
	 *	@param filename the reference filename to open, provided by keywords.txt
	 */
  public void showReference ( String filename )
  {
	// TODO: catch handleFindReference directly
	handleFindReferenceImpl();
  }

	/**
	 *	Menu item callback, handles showing a reference page.
	 */
  private void handleFindReferenceImpl ()
  {
	if (textarea.isSelectionActive()) {
        Base.openURL(
          "http://www.google.com/search?q=" +
          textarea.getSelectedText().trim() +
          "+site%3Ahttp%3A%2F%2Fprocessingjs.org%2Freference"
        );
     }
  }

	/**
	 *	Menu item callback, replacement for RUN:
	 *  export to folder, start server, open in default browser.
	 */
	public void handleStartServer ()
	{
		statusEmpty();

		if ( !startServer( getExportFolder() ) )
		{
			if ( !handleExport( false ) ) return;
			toolbar.activate(JavaScriptToolbar.RUN);
		}

		// waiting for server to call "serverStarted() below ..."
	}

	/**
	 *	Menu item callback, open running server address in a browser
	 */
	private void handleOpenInBrowser ()
	{
		openBrowserForServer();
	}

	/**
	 *  Menu item callback, replacement for STOP: stop server.
	 */
  public void handleStopServer ()
  {
	stopServer();

	toolbar.deactivate(JavaScriptToolbar.RUN);
  }

	/**
	 *	Menu item callback, call the export method of the sketch
	 *	and handle the gui stuff
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
          File exportFolder = new File( sketch.getFolder(),
 										  JavaScriptBuild.EXPORTED_FOLDER_NAME );
          Base.openFolder( exportFolder );

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
	 *  Menu item callback, changed from Editor.java to automaticaly
	 *	export and handle the server when it's running.
	 *	Normal save ops otherwise.
	 *
	 *	@param immediately set to false to allow it to be run in a Swing optimized manner
	 */
  public boolean handleSave ( boolean immediately )
  {
    if (sketch.isUntitled())
	{
      return handleSaveAs();
    }
	else if (immediately)
	{
      handleSave();
	  statusEmpty();
		if ( serverRunning() ) handleStartServer();
    }
	else
	{
      	SwingUtilities.invokeLater(new Runnable()
		{
          	public void run()
			{
            	handleSave();
				statusEmpty();
				if ( serverRunning() ) handleStartServer();
			}
        });
    }
    return true;
  }

	/**
	 *	Called from handleSave( true/false )
	 */
  public void handleSave ()
  {
    toolbar.activate(JavaScriptToolbar.SAVE);
    handleSaveImpl();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
  }

	/**
	 *	Called from handleExport()
	 */
  private boolean handleExportCheckModified ()
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
        handleSave(true);

      } else {
        statusNotice("Export canceled, changes must first be saved.");
        return false;
      }
    }
    return true;
  }

	/**
	 *	Menu item callback
	 */
  public boolean handleSaveAs ()
  {
    toolbar.activate(JavaScriptToolbar.SAVE);
    boolean result = super.handleSaveAs();
    toolbar.deactivate(JavaScriptToolbar.SAVE);
    return result;
  }

	/**
	 *	Menu item callback
	 */
  public void handleImportLibrary (String item)
  {
    Base.showWarning("Processing.js doesn't support libraries",
                     "Libraries are not supported. Import statements are " +
                     "ignored, and code relying on them will break.",
                     null);
  }

	// ----------------------------------------
	//  implementation BasicServerListener
	// ----------------------------------------

	/**
	 *	BasicServerListener implementation,
	 *	called by server once it starts serving
	 */
  public void serverStarted ()
  {
  		super.serverStarted();

		if ( !handleExport( false ) ) return;
		toolbar.activate(JavaScriptToolbar.RUN);
  }

	// ----------------------------------------
	//  other methods
	// ----------------------------------------

	/**
	 *	Return the current export folder in a sane way
	 *
	 *	@return the export folder as File
	 */
  private File getExportFolder ()
  {
  	return new File( getSketch().getFolder(),
	 				 JavaScriptBuild.EXPORTED_FOLDER_NAME );
  }

	/**
	 *	Return the custom template folder
	 *
	 *	@return the custom template folder as File
	 */
  private File getCustomTemplateFolder ()
  {
	return new File( getSketch().getFolder(),
					 JavaScriptBuild.TEMPLATE_FOLDER_NAME );
  }

	/**
	 *	Save current sketch settings, this adds the server port to them
	 */
  private void saveSketchSettings ()
  {
	statusEmpty();

	File sketchProps = getSketchPropertiesFile();
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

	int port = getServerPort();
	if ( port > 0 ) settings.set( PROP_KEY_SERVER_PORT, (port+"") );

	settings.save();
  }
}
