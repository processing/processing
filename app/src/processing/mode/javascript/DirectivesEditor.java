package processing.mode.javascript;

import processing.app.Base;
import processing.app.Sketch;
import processing.app.SketchCode;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/* http://processingjs.org/reference/pjs%20directive */

public class DirectivesEditor
{
	JavaScriptEditor editor;
  
    JFrame frame;
	JCheckBox 	crispBox;
	JTextField 	fontField;
    JCheckBox 	globalKeyEventsBox;
    JCheckBox 	pauseOnBlurBox;
    JTextField 	preloadField;
    //JCheckBox 	transparentBox;

	private final static ArrayList<String> validKeys = new ArrayList<String>();
	static {
		validKeys.add("crisp");
		validKeys.add("font");
		validKeys.add("globalKeyEvents");
		validKeys.add("pauseOnBlur");
		validKeys.add("preload");
		validKeys.add("transparent");
	}
	private final static int CRISP = 0;
	private final static int FONT = 1;
	private final static int GLOBAL_KEY_EVENTS = 2;
	private final static int PAUSE_ON_BLUR = 3;
	private final static int PRELOAD = 4;
	private final static int TRANSPARENT = 5;
	
	private Pattern pjsPattern; 
	
	public DirectivesEditor ( JavaScriptEditor e )
	{
		editor = e;
		
		if ( frame == null ) createFrame();
		
		// see processing-1.2.0.js
		pjsPattern = Pattern.compile( 
						"\\/\\*\\s*@pjs\\s+((?:[^\\*]|\\*+[^\\*\\/])*)\\*\\/\\s*", 
						Pattern.DOTALL );
	}
	
	public void show ()
	{
		if ( editor.getSketch().isModified())
		{
			Base.showWarning( "Directives Editor", 
							  "Please save your sketch before changing "+
							  "the directives.", null);
			return;
		}
		
		resetInterface();
		findRemoveDirectives(false);
		
		frame.setVisible(true);
	}
	
	private void resetInterface ()
	{
		for ( JCheckBox b : new JCheckBox[] {
			crispBox, globalKeyEventsBox, pauseOnBlurBox /*, transparentBox*/ } )
		{
			b.setSelected(false);
		}
		for ( JTextField f : new JTextField[]{ fontField, preloadField } )
		{
			f.setText("");
		}
	}

	public void hide ()
	{
		frame.setVisible(false);
	}
	
	void applyDirectives ()
	{
		findRemoveDirectives(true);
		
		StringBuffer buffer = new StringBuffer();
		String head = "", toe = "; \n";
		
		if ( crispBox.isSelected() )
			buffer.append( head + "crisp=true" + toe );
		if ( !fontField.getText().trim().equals("") )
			buffer.append( head + "font=\""+fontField.getText().trim()+"\"" + toe );
		if ( globalKeyEventsBox.isSelected() )
			buffer.append( head + "globalKeyEvents=true" + toe );
		if ( pauseOnBlurBox.isSelected() )
			buffer.append( head + "pauseOnBlur=true" + toe );
		if ( !preloadField.getText().trim().equals("") )
			buffer.append( head + "preload=\""+preloadField.getText().trim()+"\"" + toe );
		/*if ( transparentBox.isSelected() )
			buffer.append( head + "transparent=true" + toe );*/
		
		Sketch sketch = editor.getSketch();
		SketchCode code = sketch.getCode(0); // first tab
		if ( buffer.length() > 0 )
		{
			code.setProgram( "/* @pjs " + buffer.toString() + " */\n\n" + code.getProgram() );
			if ( sketch.getCurrentCode() == code ) // update textarea if on first tab
			{
	   			editor.setText(sketch.getCurrentCode().getProgram());
				editor.setSelection(0,0);
			}	
		
			sketch.setModified( false );
			sketch.setModified( true );
		}
	}
	
	void findRemoveDirectives ( boolean clean )
	{		
		//if ( clean ) editor.startCompoundEdit();
		
		Sketch sketch = editor.getSketch();
		for (int i = 0; i < sketch.getCodeCount(); i++)
		{
			SketchCode code = sketch.getCode(i);
			String program = code.getProgram();
			StringBuffer buffer = new StringBuffer();
			
			Matcher m = pjsPattern.matcher( program );
			while (m.find())
			{
				String mm = m.group();
				
				// TODO this urgently needs tests ..
				
				/* remove framing */
				mm = mm.replaceAll("^\\/\\*\\s*@pjs","").replaceAll("\\s*\\*\\/\\s*$","");
				/* fix multiline nice formatting */
				mm = mm.replaceAll("[\\s]*([^;\\s\\n\\r]+)[\\s]*,[\\s]*[\\n\\r]+","$1,");
				/* fix multiline version without semicolons */
				mm = mm.replaceAll("[\\s]*([^;\\s\\n\\r]+)[\\s]*[\\n\\r]+","$1;");
				mm = mm.replaceAll("\n"," ").replaceAll("\r"," ");
				
				//System.out.println(mm);
					
				if ( clean )
				{
					m.appendReplacement(buffer, "");
				}
				else
				{
					String[] directives = mm.split(";");
					for ( String d : directives )
					{
						//System.out.println(d);
						parseDirective(d);
					}
				}
			}
			
			if ( clean )
			{
				m.appendTail(buffer);
			
				// TODO: not working!
     			code.setProgram( buffer.toString() );
     			code.setModified( true );
			}
   		}
		
		if ( clean )
		{
			//editor.stopCompoundEdit();
   			editor.setText(sketch.getCurrentCode().getProgram());
			sketch.setModified( false );
			sketch.setModified( true );
		}
	}
	
	private void parseDirective ( String directive )
	{
		if ( directive == null )
		{
			System.err.println( "Directive is null." );
			return;
		}
		
		String[] pair = directive.split("=");
		if ( pair == null || pair.length != 2 )
		{
			System.err.println("Unable to parse directive: \"" + directive + "\" Ignored.");
			return;
		}
		
		String key   = pair[0].trim(), 
			   value = pair[1].trim();
		
		// clean these, might have too much whitespace around commas
		if ( validKeys.indexOf(key) == FONT || validKeys.indexOf(key) == PRELOAD )
		{
			value = value.replaceAll("[\\s]*,[\\s]*", ",");
		}
		
		if ( validKeys.indexOf(key) == -1 )
		{
			System.err.println("Directive key not recognized: \"" + key + "\" Ignored." );
			return;
		}
		if ( value.equals("") )
		{
			System.err.println("Directive value empty. Ignored.");
			return;
		}
		
		value = value.replaceAll("^\"|\"$","").replaceAll("^'|'$","");
		
		//System.out.println( key + " = " + value );
		
		boolean v;
		switch ( validKeys.indexOf(key) )
		{
			case CRISP:
				v = value.toLowerCase().equals("true");
				crispBox.setSelected(v);
				break;
			case FONT:
				fontField.setText(value);
				break;
			case GLOBAL_KEY_EVENTS:
				v = value.toLowerCase().equals("true");
				globalKeyEventsBox.setSelected(v);
				break;
			case PAUSE_ON_BLUR:
				v = value.toLowerCase().equals("true");
				pauseOnBlurBox.setSelected(v);
				break;
			case PRELOAD:
			 	preloadField.setText(value);
				break;
			case TRANSPARENT:
				v = value.toLowerCase().equals("true");
				//transparentBox.setSelected(v);
				break;
		}
	} 
	
	void createFrame ()
	{
		/* see Preferences.java */
	    int GUI_BIG     = 13;
	    int GUI_BETWEEN = 10;
	    int GUI_SMALL   = 6;
		int FIELD_SIZE  = 30;
	
		int left = GUI_BIG;
		int top = GUI_BIG;
		int right = 0;
		
		Dimension d;
		
	    frame = new JFrame("Directives Editor");
		Container pane = frame.getContentPane();
	    pane.setLayout(null);
	
	    JLabel label = new JLabel("Click here to read about directives.");
		label.addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e) {
				Base.openURL("http://processingjs.org/reference/pjs%20directive");
			}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
	    pane.add(label);
		d = label.getPreferredSize();
		label.setBounds(left, top, d.width, d.height);
	    top += d.height + GUI_BETWEEN + GUI_BETWEEN;
	
		// CRISP
	
		crispBox =
	      new JCheckBox("\"crisp\": disable antialiasing for line(), triangle() and rect()");
	    pane.add(crispBox);
		d = crispBox.getPreferredSize();
	    crispBox.setBounds(left, top, d.width + 10, d.height);
	    right = Math.max(right, left + d.width);
	    top += d.height + GUI_BETWEEN;
	
		// FONTS
	
	    label = new JLabel("\"font\": to load (comma separated)");
	    pane.add(label);
		d = label.getPreferredSize();
		label.setBounds(left, top, d.width, d.height);
	    top += d.height + GUI_SMALL;
	
	    fontField = new JTextField(FIELD_SIZE);
	    pane.add(fontField);
	    d = fontField.getPreferredSize();
		fontField.setBounds(left, top, d.width, d.height);
	
		JButton button = new JButton("scan");
		button.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e) {
				handleScanFonts();
			}
		});
	    pane.add(button);
		Dimension d2 = button.getPreferredSize();
	    button.setBounds(left + d.width + GUI_SMALL, top, d2.width, d2.height);
	    right = Math.max(right, left + d.width + GUI_SMALL + d2.width);
	    top += d.height + GUI_BETWEEN;
	
		// GLOBAL_KEY_EVENTS
	
		globalKeyEventsBox =
	      new JCheckBox("\"globalKeyEvents\": receive global key events");
	    pane.add(globalKeyEventsBox);
		d = globalKeyEventsBox.getPreferredSize();
	    globalKeyEventsBox.setBounds(left, top, d.width + 10, d.height);
	    right = Math.max(right, left + d.width);
	    top += d.height + GUI_BETWEEN;
	
		// PAUSE_ON_BLUR
	
		pauseOnBlurBox =
	      new JCheckBox("\"pauseOnBlur\": pause if applet loses focus");
	    pane.add(pauseOnBlurBox);
		d = pauseOnBlurBox.getPreferredSize();
	    pauseOnBlurBox.setBounds(left, top, d.width + 10, d.height);
	    right = Math.max(right, left + d.width);
	    top += d.height + GUI_BETWEEN;
	
		// PRELOAD images
		
	    label = new JLabel("\"preload\": images (comma separated)");
	    pane.add(label);
		d = label.getPreferredSize();
		label.setBounds(left, top, d.width, d.height);
	    top += d.height + GUI_SMALL;

	    preloadField = new JTextField(FIELD_SIZE);
	    pane.add(preloadField);
	    d = preloadField.getPreferredSize();
		preloadField.setBounds(left, top, d.width, d.height);

		button = new JButton("scan");
		button.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e) {
				handleScanImages();
			}
		});
	    pane.add(button);
		d2 = button.getPreferredSize();
	    button.setBounds(left + d.width + GUI_SMALL, top, d2.width, d2.height);
	    right = Math.max(right, left + d.width + GUI_SMALL + d2.width);
	    top += d.height + GUI_BETWEEN;
		
		// TRANSPARENT
	
		/*transparentBox =
	      new JCheckBox("\"transparent\": set applet background to be transparent");
	    pane.add(transparentBox);
		d = transparentBox.getPreferredSize();
	    transparentBox.setBounds(left, top, d.width + 10, d.height);
	    right = Math.max(right, left + d.width);
	    top += d.height + GUI_BETWEEN;*/
	
		// APPLY / OK
		
		button = new JButton("OK");
	    button.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          applyDirectives();
			  hide();
	        }
	      });
	    pane.add(button);
	    d2 = button.getPreferredSize();
	    int BUTTON_HEIGHT = d2.height;
		int BUTTON_WIDTH  = 80;

	    int h = right - (BUTTON_WIDTH + GUI_SMALL + BUTTON_WIDTH);
	    button.setBounds(h, top, BUTTON_WIDTH, BUTTON_HEIGHT);
	    h += BUTTON_WIDTH + GUI_SMALL;

	    button = new JButton("Cancel");
	    button.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
			  hide();
	        }
	      });
	    pane.add(button);
	    button.setBounds(h, top, BUTTON_WIDTH, BUTTON_HEIGHT);

	    top += BUTTON_HEIGHT + GUI_BETWEEN;
	
		//frame.getContentPane().add(box);
	    frame.pack();
		Insets insets = frame.getInsets();
	    frame.setSize(right + GUI_BIG + insets.left + insets.right,
	                  top + GUI_SMALL + insets.top + insets.bottom);
		
	    //frame.setResizable(false);
		
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	    frame.addWindowListener(new WindowAdapter() {
	        public void windowClosing(WindowEvent e) {
	          frame.setVisible(false);
	        }
	      });
	    Base.registerWindowCloseKeys(frame.getRootPane(), new ActionListener() {
	        public void actionPerformed(ActionEvent actionEvent) {
	          frame.setVisible(false);
	        }
	    });
	    Base.setIcon(frame);
	}
	
	void handleScanFonts()
	{
		handleScanFiles( fontField, new String[]{
			"woff", "svg", "eot", "ttf", "otf"
		});
	}

	void handleScanImages()
	{
		handleScanFiles( preloadField, new String[]{
			"gif", "jpg", "jpeg", "png", "tga"
		});
	}
	
	void handleScanFiles ( JTextField field, String[] extensions )
	{
		String[] dataFiles = scanDataFolderForFilesByType(extensions);
		if ( dataFiles == null || dataFiles.length == 0 )
			return;

		String[] oldFileList = field.getText().trim().split(",");
		ArrayList<String> newFileList = new ArrayList<String>();
		for ( String c : oldFileList )
		{
			c = c.trim();
			if ( !c.equals("") && newFileList.indexOf(c) == -1 ) // TODO check exists() here?
			{
				newFileList.add( c );
			}
		}
		for ( String c : dataFiles )
		{
			c = c.trim();
			if ( !c.equals("") && newFileList.indexOf(c) == -1 )
			{
				newFileList.add( c );
			}
		}
		Collections.sort(newFileList);
		String finalFileList = ""; int i = 0;
		for ( String s : newFileList )
		{
			finalFileList += (i > 0 ? ", " : "") + s;
			i++;
		}
		field.setText(finalFileList);
	}
	
	String[] scanDataFolderForFilesByType ( String[] extensions )
 	{
		ArrayList files = new ArrayList();
		File dataFolder = editor.getSketch().getDataFolder();
		
		if ( !dataFolder.exists() ) return null; // TODO no folder present .. warn?
		
		for ( String ext : extensions )
		{
			String[] found = listFiles(dataFolder, true, ext);
			if ( found == null || found.length == 0 ) continue;
			
			for ( String f : found )
			{
				if ( files.indexOf(f) == -1 )
					files.add(f);
			}
		}
		
		return (String[])files.toArray(new String[0]);
	}
	
	// #718
	// http://code.google.com/p/processing/issues/detail?id=718
	private String[] listFiles(File folder, boolean relative, String extension) {
	    String path = folder.getAbsolutePath();
	    Vector<String> vector = new Vector<String>();
	    if (extension != null) {
	      if (!extension.startsWith(".")) {
	        extension = "." + extension;
	      }
	    }
	    listFiles(relative ? (path + File.separator) : "", path, extension, vector);
	    String outgoing[] = new String[vector.size()];
	    vector.copyInto(outgoing);
	    return outgoing;
	  }


	 private void listFiles(String basePath,
	                                  String path, String extension,
	                                  Vector<String> vector) {
	    File folder = new File(path);
	    String[] list = folder.list();
	    if (list != null) {
	      for (String item : list) {
	        if (item.charAt(0) == '.') continue;
			File file = new File(path, item);
	        String newPath = file.getAbsolutePath();
	        if (newPath.startsWith(basePath)) {
	          newPath = newPath.substring(basePath.length());
	        }
	        if (extension == null || item.toLowerCase().endsWith(extension)) {
	          vector.add(newPath);
	        }
	        if (file.isDirectory()) {
	          listFiles(basePath, file.getAbsolutePath(), extension, vector);
	        }
	      }
	    }
	  }
}