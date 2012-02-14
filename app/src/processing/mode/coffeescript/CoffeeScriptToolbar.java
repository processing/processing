package processing.mode.coffeescript;

import java.awt.Image;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import processing.app.Base;
import processing.app.Editor;
import processing.app.EditorToolbar;

public class CoffeeScriptToolbar extends EditorToolbar
{
	static protected final int RUN    = 0;
	static protected final int STOP   = 1;

	static protected final int NEW    = 2;
	static protected final int OPEN   = 3;
	static protected final int SAVE   = 4;
	static protected final int EXPORT = 5;
	
	static public String getTitle ( int index, boolean shift ) 
	{
	  switch (index) 
	  {
		case RUN:    return "Start server";
	    case STOP:   return "Stop server";
	    case NEW:    return !shift ? "New"  : "New Editor Window";
	    case OPEN:   return !shift ? "Open" : "Open in Another Window";
	    case SAVE:   return "Save";
	    case EXPORT: return "Export for Web";
	  }
	  return null;
	}

	public CoffeeScriptToolbar ( Editor editor, Base base ) 
	{
		super( editor, base );
	}

	public void init () 
	{
	  Image[][] images = loadImages();
	  for (int i = 0; i < 6; i++) 
	  {
	    addButton( getTitle(i, false), getTitle(i, true), images[i], i == NEW );
	  }
	}

	public void handlePressed ( MouseEvent e, int index ) 
	{
	  boolean shift = e.isShiftDown();
	  CoffeeScriptEditor csEditor = (CoffeeScriptEditor) editor;

	  switch (index) {

		case RUN:
			csEditor.handleStartServer();
			break;

		case STOP:
			csEditor.handleStopServer();
			break;

	  case OPEN:
	    JPopupMenu popup = editor.getMode().getToolbarMenu().getPopupMenu();
	    popup.show(this, e.getX(), e.getY());
	    break;

	  case NEW:
	    if (shift) {
	      base.handleNew();
	    } else {
	      base.handleNewReplace();
	    }
	    break;

	  case SAVE:
	    //jsEditor.handleSaveRequest(false);
	    break;

	  case EXPORT:
	    //jsEditor.handleExport( true );
	    break;
	  }
	}
}