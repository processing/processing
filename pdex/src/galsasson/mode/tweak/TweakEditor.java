/*
  Part of TweakMode project (https://github.com/galsasson/TweakMode)
  
  Under Google Summer of Code 2013 - 
  http://www.google-melange.com/gsoc/homepage/google/gsoc2013
  
  Copyright (C) 2013 Gal Sasson
	
  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package galsasson.mode.tweak;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;

import processing.app.Base;
import processing.app.EditorState;
import processing.app.EditorToolbar;
import processing.app.Mode;
import processing.app.Sketch;
import processing.app.SketchCode;
import processing.app.SketchException;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.syntax.SyntaxDocument;
import processing.mode.java.JavaBuild;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaToolbar;
import processing.mode.java.runner.Runner;

/**
 * Editor for STMode
 * 
 * @author Gal Sasson &lt;sasgal@gmail.com&gt;
 * 
 */
public class TweakEditor extends JavaEditor 
{
	TweakMode tweakMode;
	
	String[] baseCode;
	
	final static int SPACE_AMOUNT = 0;
	
	int oscPort;
	
	/**
	 * Custom TextArea
	 */
	protected TweakTextArea tweakTextArea;

	protected TweakEditor(Base base, String path, EditorState state,
							final Mode mode) {
		super(base, path, state, mode);

		tweakMode = (TweakMode)mode;
		
		// random port for OSC (0xff0 - 0xfff0)
		oscPort = (int)(Math.random()*0xf000) + 0xff0;
	}
	
	public EditorToolbar createToolbar() {
		return new TweakToolbar(this, base);
	}

	/**
	 * Override creation of the default textarea.
	 */
	protected JEditTextArea createTextArea() {
		tweakTextArea = new TweakTextArea(this, new PdeTextAreaDefaults(mode));
		return tweakTextArea;
	}
	
	public JMenu buildModeMenu() {
		JMenu menu = new JMenu("Tweak");
		JCheckBoxMenuItem item;

		item = new JCheckBoxMenuItem("Dump modified code");
		item.setSelected(false);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tweakMode.dumpModifiedCode = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			}
		});
		menu.add(item);

		return menu;
	}

	public void startInteractiveMode()
	{
		tweakTextArea.startInteractiveMode();
	}

	public void stopInteractiveMode(ArrayList<Handle> handles[])
	{				
		tweakTextArea.stopInteractiveMode();

		// remove space from the code (before and after)
		removeSpacesFromCode();

		// check which tabs were modified
		boolean modified = false;
		boolean[] modifiedTabs = getModifiedTabs(handles);
		for (boolean mod : modifiedTabs) {
			if (mod) {
				modified = true;
				break;
			}
		}

		if (modified) {
			// ask to keep the values
			int ret = Base.showYesNoQuestion(this, "Tweak Mode",
									"Keep the changes?",
									"You changed some values in your sketch. Would you like to keep the changes?");
			if (ret == 1) {
				// NO! don't keep changes
				loadSavedCode();
				// update the painter to draw the saved (old) code
				tweakTextArea.invalidate();
			}
			else {
				// YES! keep changes
				// the new values are already present, just make sure the user can save the modified tabs
				for (int i=0; i<sketch.getCodeCount(); i++) {
					if (modifiedTabs[i]) {
						sketch.getCode(i).setModified(true);
					}
					else {
						// load the saved code of tabs that didn't change
						// (there might be formatting changes that should not be saved)
						sketch.getCode(i).setProgram(sketch.getCode(i).getSavedProgram());		
						/* Wild Hack: set document to null so the text editor will refresh 
						   the program contents when the document tab is being clicked */
						sketch.getCode(i).setDocument(null);

						if (i == sketch.getCurrentCodeIndex()) {
							// this will update the current code		
							setCode(sketch.getCurrentCode());
						}
					}
				}
				
				// save the sketch
				try {
					sketch.save();
				}
				catch (IOException e) {
					Base.showWarning("Tweak Mode", "Could not save the modified sketch!", e);
				}
				
				// repaint the editor header (show the modified tabs)
				header.repaint();
				tweakTextArea.invalidate();
			}
		}
		else {
			// number values were not modified but we need to load the saved code
			// because of some formatting changes
			loadSavedCode();
			tweakTextArea.invalidate();
		}
	}

	public void updateInterface(ArrayList<Handle> handles[], ArrayList<ColorControlBox> colorBoxes[])
	{
		// set OSC port of handles
		for (int i=0; i<handles.length; i++) {
			for (Handle h : handles[i]) {
				h.setOscPort(oscPort);
			}
		}

		tweakTextArea.updateInterface(handles, colorBoxes);
	}

	/**
	* Deactivate run button
	* Do this because when Mode.handleRun returns null the play button stays on.
	*/
	public void deactivateRun()
	{
		toolbar.deactivate(TweakToolbar.RUN);
	}
	
	private boolean[] getModifiedTabs(ArrayList<Handle> handles[])
	{
		boolean[] modifiedTabs = new boolean[handles.length];
		
		for (int i=0; i<handles.length; i++) {
			for (Handle h : handles[i]) {
				if (h.valueChanged()) {
					modifiedTabs[i] = true;
				}
			}
		}
		
		return modifiedTabs;
	}
	
	public void initBaseCode()
	{
    	SketchCode[] code = sketch.getCode();
    	
    	String space = new String();
    	
    	for (int i=0; i<SPACE_AMOUNT; i++) {
    		space += "\n";
    	}
    	
    	baseCode = new String[code.length];
		for (int i=0; i<code.length; i++)
		{
			baseCode[i] = new String(code[i].getSavedProgram());
			baseCode[i] = space + baseCode[i] + space;
		} 
	}
	
	public void initEditorCode(ArrayList<Handle> handles[], boolean withSpaces)
	{
		SketchCode[] sketchCode = sketch.getCode();
		for (int tab=0; tab<baseCode.length; tab++) {
				// beautify the numbers
				int charInc = 0;
				String code = baseCode[tab];
		
				for (Handle n : handles[tab])
				{
					int s = n.startChar + charInc;
					int e = n.endChar + charInc;
					String newStr = n.strNewValue;
					if (withSpaces) {
						newStr = "  " + newStr + "  ";
					}
					code = replaceString(code, s, e, newStr);
					n.newStartChar = n.startChar + charInc;
					charInc += n.strNewValue.length() - n.strValue.length();
					if (withSpaces) {
						charInc += 4;
					}
					n.newEndChar = n.endChar + charInc;
				}
				
				sketchCode[tab].setProgram(code);		
				/* Wild Hack: set document to null so the text editor will refresh 
				   the program contents when the document tab is being clicked */
				sketchCode[tab].setDocument(null);
			}
		
		// this will update the current code		
		setCode(sketch.getCurrentCode());
	}
	
	private void loadSavedCode()
	{
		SketchCode[] code = sketch.getCode();
		for (int i=0; i<code.length; i++) {
			if (!code[i].getProgram().equals(code[i].getSavedProgram())) {
				code[i].setProgram(code[i].getSavedProgram());		
				/* Wild Hack: set document to null so the text editor will refresh 
				   the program contents when the document tab is being clicked */
				code[i].setDocument(null);
			}
		}
		
		// this will update the current code
		setCode(sketch.getCurrentCode());
	}
	
	private void removeSpacesFromCode()
	{
		SketchCode[] code = sketch.getCode();
		for (int i=0; i<code.length; i++) {
			String c = code[i].getProgram();
			c = c.substring(SPACE_AMOUNT, c.length() - SPACE_AMOUNT);
			code[i].setProgram(c);
			/* Wild Hack: set document to null so the text editor will refresh
			   the program contents when the document tab is being clicked */
			code[i].setDocument(null);
		}
		// this will update the current code
		setCode(sketch.getCurrentCode());
	}
	
    /**
     * Replace all numbers with variables and add code to initialize these variables and handle OSC messages.
     * @param sketch
     * 	the sketch to work on
     * @param handles
     * 	list of numbers to replace in this sketch
     * @return
     *  true on success
     */
    public boolean automateSketch(Sketch sketch, ArrayList<Handle> handles[])
    {
    	SketchCode[] code = sketch.getCode();

    	if (code.length<1)
    		return false;

    	if (handles.length == 0)
    		return false;

    	int setupStartPos = SketchParser.getSetupStart(baseCode[0]);
    	if (setupStartPos < 0) {
    		return false;
    	}

		// Copy current program to interactive program

    	/* modify the code below, replace all numbers with their variable names */
    	// loop through all tabs in the current sketch
    	for (int tab=0; tab<code.length; tab++)
    	{
    		int charInc = 0;
			String c = baseCode[tab];
			for (Handle n : handles[tab])
    		{
    			// replace number value with a variable
    			c = replaceString(c, n.startChar + charInc, n.endChar + charInc, n.name);
    			charInc += n.name.length() - n.strValue.length();
    		}
			code[tab].setProgram(c);
    	}

    	/* add the main header to the code in the first tab */
    	String c = code[0].getProgram();

    	// header contains variable declaration, initialization, and OSC listener function
    	String header;
    	header = "\n\n" +
    		 "/*************************/\n" +
    		 "/* MODIFIED BY TWEAKMODE */\n" +
		 	 "/*************************/\n" +
    		 "\n\n";

    	// add needed OSC imports and the global OSC object
    	header += "import oscP5.*;\n";
    	header += "import netP5.*;\n\n";
    	header += "OscP5 tweakmode_oscP5;\n\n";

    	// write a declaration for int and float arrays
    	int numOfInts = howManyInts(handles);
    	int numOfFloats = howManyFloats(handles);
    	if (numOfInts > 0) {
    		header += "int[] tweakmode_int = new int["+numOfInts+"];\n";
    	}
    	if (numOfFloats > 0) {
    		header += "float[] tweakmode_float = new float["+numOfFloats+"];\n\n";
    	}

    	/* add the class for the OSC event handler that will respond to our messages */
    	header += "public class TweakMode_OscHandler {\n" +
    			  "  public void oscEvent(OscMessage msg) {\n" +
                  "    String type = msg.addrPattern();\n";
        if (numOfInts > 0) {
        	header += "    if (type.contains(\"/tm_change_int\")) {\n" +
                      "      int index = msg.get(0).intValue();\n" +
                      "      int value = msg.get(1).intValue();\n" +    
                      "      tweakmode_int[index] = value;\n" +
                      "    }\n";
        	if (numOfFloats > 0) {
        		header += "    else ";
        	}
        }
        if (numOfFloats > 0) {
            header += "if (type.contains(\"/tm_change_float\")) {\n" +
                      "      int index = msg.get(0).intValue();\n" +
                      "      float value = msg.get(1).floatValue();\n" +   
                      "      tweakmode_float[index] = value;\n" +
                      "    }\n";
        }
        header += "  }\n" +
                  "}\n";
    	header += "TweakMode_OscHandler tweakmode_oscHandler = new TweakMode_OscHandler();\n";

    	header += "void tweakmode_initAllVars() {\n";
    	for (int i=0; i<handles.length; i++) {
    		for (Handle n : handles[i])
    		{
    			header += "  " + n.name + " = " + n.strValue + ";\n";
    		}
    	}
    	header += "}\n\n";
    	header += "void tweakmode_initOSC() {\n";
    	header += "  tweakmode_oscP5 = new OscP5(tweakmode_oscHandler,"+oscPort+");\n";
    	header += "}\n";
    	
    	header += "\n\n\n\n\n";
    	
    	// add call to our initAllVars and initOSC functions from the setup() function.
    	String addToSetup = "\n  tweakmode_initAllVars();\n  tweakmode_initOSC();\n\n";
    	setupStartPos = SketchParser.getSetupStart(c);
    	c = replaceString(c, setupStartPos, setupStartPos, addToSetup);

    	code[0].setProgram(header + c);
    	
    	/* print out modified code */
    	if (tweakMode.dumpModifiedCode) {
    		System.out.println("\nModified code:\n");
    		for (int i=0; i<code.length; i++)
    		{
    			System.out.println("file " + i + "\n=========");
    			System.out.println(code[i].getProgram());
    		}
    	}

    	return true;
    }	
	
	private String replaceString(String str, int start, int end, String put)
	{
		return str.substring(0, start) + put + str.substring(end, str.length());
	}
	
	private int howManyInts(ArrayList<Handle> handles[])
	{
		int count = 0;
		for (int i=0; i<handles.length; i++) {
			for (Handle n : handles[i]) {
				if (n.type == "int" || n.type == "hex" || n.type == "webcolor")
					count++;
			}
		}
		return count;
	}

	private int howManyFloats(ArrayList<Handle> handles[])
	{
		int count = 0;
		for (int i=0; i<handles.length; i++) {
			for (Handle n : handles[i]) {
				if (n.type == "float")
					count++;
			}
		}
		return count;
	}
}
