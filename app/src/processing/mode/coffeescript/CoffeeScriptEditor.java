package processing.mode.coffeescript;

import processing.mode.coffeescript.CoffeeScriptMode;
import processing.mode.coffeescript.CoffeeScriptFormatter;  
import processing.mode.coffeescript.CoffeeScriptToolbar;
import processing.mode.coffeescript.CoffeeScriptBuild;   

import processing.app.Base;
import processing.app.EditorState;
import processing.app.Editor;
import processing.app.Mode;
import processing.app.EditorToolbar;
import processing.app.Formatter;

import java.io.File;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CoffeeScriptEditor extends Editor
{	
	CoffeeScriptMode csMode;
	
	protected CoffeeScriptEditor ( Base base, String path, EditorState state, Mode mode ) 
	{	
		super( base, path, state, mode );

		csMode = (CoffeeScriptMode) mode;
	}
	
	// -------- extending Editor ----------
	
	public EditorToolbar createToolbar () 
	{
		return new CoffeeScriptToolbar( this, base );
	}

	public Formatter createFormatter () 
	{ 
		return new CoffeeScriptFormatter();
	}
	
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
		JMenuItem startServerItem = Base.newJMenuItem("Start Server", 'R');
		startServerItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		      handleStartServer();
		    }
		  });

		JMenuItem openInBrowserItem = Base.newJMenuItem("Reopen in Browser", 'B');
		openInBrowserItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		      handleOpenInBrowser();
		    }
		  });

		JMenuItem stopServerItem = new JMenuItem("Stop Server");
		stopServerItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		      handleStopServer();
		    }
		  });

		JMenuItem copyServerAddressItem = new JMenuItem("Copy Server Address");
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

		JMenuItem setServerPortItem = new JMenuItem("Set Server Port");
		setServerPortItem.addActionListener(new ActionListener(){
		public void actionPerformed (ActionEvent e) {
			handleSetServerPort();
		}
		});

		return buildSketchMenu(new JMenuItem[] {
		startServerItem, openInBrowserItem, stopServerItem, 
		copyServerAddressItem, setServerPortItem
		});
	}
	
	public JMenu buildHelpMenu () 
	{
		JMenu menu = new JMenu("Help ");
		JMenuItem item;

		// TODO switch to "http://js.processing.org/"?

		item = new JMenuItem("CoffeeScript Language Overview");
		item.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		      Base.openURL("http://coffeescript.org/");
		    }
		});
		menu.add(item);

		// item = new JMenuItem("Reference");
		// item.addActionListener(new ActionListener() {
		//   public void actionPerformed(ActionEvent e) {
		//     //TODO get offline reference archive corresponding to the release 
		//     // packaged with this mode see: P.js ticket 1146 "Offline Reference"
		//     Base.openURL("http://processingjs.org/reference");
		//   }
		// });
		// menu.add(item);
		// 
		// item = Base.newJMenuItemShift("Find in Reference", 'F');
		// item.addActionListener(new ActionListener() {
		//   public void actionPerformed(ActionEvent e) {
		//     handleFindReferenceHACK();
		//   }
		// });
		// menu.add(item);

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
	
	public String getCommentPrefix () 
	{
		return "#";
	}
	
	public void internalCloseRunner ()
	{
	}
	
	public void deactivateRun ()
	{
	}
	
	// -------- handlers ----------
	
	/**
	 * Call the export method of the sketch and handle the gui stuff
	 */
	private boolean handleExport ( boolean openFolder ) 
	{		
		if ( !handleExportCheckModified() )
		{
			return false;
		}
		else
		{
			toolbar.activate(CoffeeScriptToolbar.EXPORT);
			try 
			{
				boolean success = csMode.handleExport(sketch);
				if ( success && openFolder ) 
				{
					File exportFolder = getExportFolder();
					Base.openFolder( exportFolder );

					statusNotice("Finished exporting.");
				} else if ( !success ) { 
					// error message already displayed by handleExport
					return false;
				}
			} catch (Exception e) {
				statusError(e);
				toolbar.deactivate(CoffeeScriptToolbar.EXPORT);
				return false;
			}
			toolbar.deactivate(CoffeeScriptToolbar.EXPORT);
		}
		return true;
	}
	
	public void handleStartServer () {
		
	}
	
	public void handleStopServer () {
		
	}
	
	private void handleSetServerPort () {
		
	}
	
	private void handleCopyServerAddress () {
		
	}
	
	private void handleOpenInBrowser () {
		
	}
	
	/**
	 *	Comes from Editor.
	 */
	public void handleImportLibrary (String item) 
	{
		Base.showWarning("CoffeeScript doesn't support libraries",
		               "Libraries are not supported. Import statements are " +
		               "ignored, and code relying on them will break.",
		               null);
	}
	
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
	      handleSaveRequest(true);

	    } else {
	      statusNotice("Export canceled, changes must first be saved.");
	      return false;
	    }
	  }
	  return true;
	}

	// -------- other stuff ----------
	
	private File getExportFolder ()
	{
	  	return new File( getSketch().getFolder(),
		 				 CoffeeScriptBuild.EXPORTED_FOLDER_NAME );
	}
}