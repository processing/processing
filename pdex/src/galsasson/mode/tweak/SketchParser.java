package galsasson.mode.tweak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import processing.app.Sketch;

public class SketchParser 
{
	int intVarCount;
	int floatVarCount;
	final String varPrefix = "tweakmode";
	
	String[] codeTabs;
	boolean requiresComment;
	ArrayList<Handle> allHandles[];
	ArrayList<ColorMode> colorModes;
	ArrayList<ColorControlBox> colorBoxes[];
	
	ArrayList<Range> scientificNotations[];
	
	public SketchParser(String[] codeTabs, boolean requiresComment)
	{
		this.codeTabs = codeTabs;
		this.requiresComment = requiresComment;
		intVarCount=0;
		floatVarCount=0;
		
		scientificNotations = getAllScientificNotations();
		
		// find, add, and sort all tweakable numbers in the sketch
		addAllNumbers();
		
		// handle colors
		colorModes = findAllColorModes();
		colorBoxes = new ArrayList[codeTabs.length];
		createColorBoxes();
		createColorBoxesForLights();
		
		/* If there is more than one color mode per context,
		 * allow only hex and webcolors in this context.
		 * Currently there is no notion of order of execution so we
		 * cannot know which color mode relate to a color.
		 */
		handleMultipleColorModes();
	}
	
	public void addAllNumbers()
	{
		allHandles = new ArrayList[codeTabs.length];
		addAllDecimalNumbers();
		addAllHexNumbers();
		addAllWebColorNumbers();
		for (int i=0; i<codeTabs.length; i++) {
			Collections.sort(allHandles[i], new HandleComparator());
		}		
	}
	
	/**
	 * Get a list of all the numbers in this sketch
	 * @return
	 * list of all numbers in the sketch (excluding hexadecimals)
	 */
	private void addAllDecimalNumbers()
	{
		/* for every number found:
		 * save its type (int/float), name, value and position in code.
		 */
		Pattern p = Pattern.compile("[\\[\\{<>(),\\t\\s\\+\\-\\/\\*^%!|&=?:~]\\d+\\.?\\d*");
		for (int i=0; i<codeTabs.length; i++)
		{
			allHandles[i] = new ArrayList<Handle>();
			String c = codeTabs[i];
			Matcher m = p.matcher(c);

			while (m.find())
			{
				boolean forceFloat = false;
				int start = m.start()+1;
				int end = m.end();
				
				if (isInComment(start, codeTabs[i])) {
					// ignore comments
					continue;
				}

				if (requiresComment) {
					// only add numbers that have the "// tweak" comment in their line
					if (!lineHasTweakComment(start, c)) {
						continue;
					}
				}

				// ignore scientific notation (e.g. 1e-6)
				boolean found = false;
				for (Range r : scientificNotations[i]) {
					if (r.contains(start)) {
						found=true;
						break;
					}
				}
				if (found) {
					continue;
				}

				// remove any 'f' after the number
				if (c.charAt(end) == 'f') {
					forceFloat = true;
					end++;
				}
				
				// if its a negative, include the '-' sign
				if (c.charAt(start-1) == '-') {
					if (isNegativeSign(start-2, c)) {
						start--;
					}
				}

				// special case for ignoring (0x...). will be handled later
				if (c.charAt(m.end()) == 'x' ||
						c.charAt(m.end()) == 'X') {
					continue;
				}

				// special case for ignoring number inside a string ("")
				if (isInsideString(start, c))
					continue;

				// beware of the global assignment (bug from 26.07.2013)
				if (isGlobal(m.start(), c))
					continue;

				int line = countLines(c.substring(0, start)) - 1;			// zero based
				String value = c.substring(start, end);
				//value
    			if (value.contains(".") || forceFloat) {
    				// consider this as a float
        			String name = varPrefix + "_float[" + floatVarCount +"]";
        			int decimalDigits = getNumDigitsAfterPoint(value);
        			allHandles[i].add(new Handle("float", name, floatVarCount, value, i, line, start, end, decimalDigits));
    				floatVarCount++;
    			} else {
    				// consider this as an int
        			String name = varPrefix + "_int[" + intVarCount +"]";
        			allHandles[i].add(new Handle("int", name, intVarCount, value, i, line, start, end, 0));
    				intVarCount++;
    			}    			
    		}
    	}
    }
	
	/**
	 * Get a list of all the hexadecimal numbers in the code
	 * @return
	 * list of all hexadecimal numbers in the sketch
	 */
	private void addAllHexNumbers()
	{
		/* for every number found:
		 * save its type (int/float), name, value and position in code.
		 */
		Pattern p = Pattern.compile("[\\[\\{<>(),\\t\\s\\+\\-\\/\\*^%!|&=?:~]0x[A-Fa-f0-9]+");
		for (int i=0; i<codeTabs.length; i++)
		{
			String c = codeTabs[i];
			Matcher m = p.matcher(c);
        
			while (m.find())
			{
				int start = m.start()+1;
				int end = m.end();

				if (isInComment(start, codeTabs[i])) {
					// ignore comments
					continue;
				}

				if (requiresComment) {
					// only add numbers that have the "// tweak" comment in their line
					if (!lineHasTweakComment(start, c)) {
						continue;
					}
				}

				// special case for ignoring number inside a string ("")
				if (isInsideString(start, c)) {
					continue;
				}

				// beware of the global assignment (bug from 26.07.2013)
				if (isGlobal(m.start(), c)) {
					continue;
				}

				int line = countLines(c.substring(0, start)) - 1;			// zero based
				String value = c.substring(start, end);
				String name = varPrefix + "_int[" + intVarCount + "]";
				Handle handle;
				try {
					handle = new Handle("hex", name, intVarCount, value, i, line, start, end, 0);
				}
				catch (NumberFormatException e) {
					// don't add this number
					continue;
				}
				allHandles[i].add(handle);
				intVarCount++;
    		}
    	}
    }

	/**
	 * Get a list of all the webcolors (#) numbers in the code
	 * @return
	 * list of all hexadecimal numbers in the sketch
	 */
	private void addAllWebColorNumbers()
	{
		Pattern p = Pattern.compile("#[A-Fa-f0-9]{6}");
		for (int i=0; i<codeTabs.length; i++)
		{
			String c = codeTabs[i];
			Matcher m = p.matcher(c);
        
			while (m.find())
			{
				int start = m.start();
				int end = m.end();

				if (isInComment(start, codeTabs[i])) {
					// ignore comments
					continue;
				}

				if (requiresComment) {
					// only add numbers that have the "// tweak" comment in their line
					if (!lineHasTweakComment(start, c)) {
						continue;
					}
				}

				// special case for ignoring number inside a string ("")
				if (isInsideString(start, c)) {
					continue;
				}

				// beware of the global assignment (bug from 26.07.2013)
				if (isGlobal(m.start(), c)) {
					continue;
				}

				int line = countLines(c.substring(0, start)) - 1;			// zero based
				String value = c.substring(start, end);
				String name = varPrefix + "_int[" + intVarCount + "]";
				Handle handle;
				try {
					handle = new Handle("webcolor", name, intVarCount, value, i, line, start, end, 0);
				}
				catch (NumberFormatException e) {
					// don't add this number
					continue;
				}
				allHandles[i].add(handle);
				intVarCount++;
    		}
    	}
    }

	private ArrayList<ColorMode> findAllColorModes()
	{
		ArrayList<ColorMode> modes = new ArrayList<ColorMode>();
		
		for (String tab : codeTabs)
		{
			int index = -1;
			// search for a call to colorMode function
			while ((index = tab.indexOf("colorMode", index+1)) > -1) {
				// found colorMode at index
				
				if (isInComment(index, tab)) {
					// ignore comments
					continue;
				}

				index += 9;
				int parOpen = tab.indexOf('(', index);
				if (parOpen < 0) {
					continue;
				}
				
				int parClose = tab.indexOf(')', parOpen+1);
				if (parClose < 0) {
					continue;
				}
				
				// add this mode
				String modeDesc = tab.substring(parOpen+1, parClose);
				String context = getObject(index-9, tab);
				modes.add(ColorMode.fromString(context, modeDesc));
			}
		}
				
		return modes;
	}
	
	private void createColorBoxes()
	{
		// search tab for the functions: 'color', 'fill', 'stroke', 'background', 'tint'
		Pattern p = Pattern.compile("color\\(|color\\s\\(|fill[\\(\\s]|stroke[\\(\\s]|background[\\(\\s]|tint[\\(\\s]");
		for (int i=0; i<codeTabs.length; i++)
		{
			colorBoxes[i] = new ArrayList<ColorControlBox>();
			String tab = codeTabs[i];
			Matcher m = p.matcher(tab);
			
			while (m.find())
			{
				ArrayList<Handle> colorHandles = new ArrayList<Handle>();
				
				// look for the '(' and ')' positions
				int openPar = tab.indexOf("(", m.start());
				int closePar = tab.indexOf(")", m.end());
				if (openPar < 0 || closePar < 0) {
					// ignore this color
					continue;
				}
				
				if (isInComment(m.start(), tab)) {
					// ignore colors in a comment
					continue;
				}

				// look for handles inside the parenthesis
				for (Handle handle : allHandles[i])
				{
					if (handle.startChar > openPar &&
							handle.endChar <= closePar) {
						// we have a match
						colorHandles.add(handle);
					}
				}
				
				if (colorHandles.size() > 0) {
					/* make sure there is no other stuff between '()' like variables.
					 * substract all handle values from string inside parenthesis and
					 * check there is no garbage left
					 */
					String insidePar = tab.substring(openPar+1, closePar);
					for (Handle h : colorHandles) {
						insidePar = insidePar.replaceFirst(h.strValue, "");
					}

					// make sure there is only ' ' and ',' left in the string.
					boolean garbage = false;
					for (int j=0; j<insidePar.length(); j++) {
						if (insidePar.charAt(j) != ' ' && insidePar.charAt(j) != ',') {
							// don't add this color box because we can not know the
							// real value of this color
							garbage = true;
						}
					}
					
					// create a new color box
					if (!garbage) {
						// find the context of the color (e.g. this.fill() or <object>.fill())
						String context = getObject(m.start(), tab);
						ColorMode cmode = getColorModeForContext(context);
						
						// not adding color operations for modes we couldn't understand
						ColorControlBox newCCB = new ColorControlBox(context, cmode, colorHandles);
						
						if (cmode.unrecognizedMode) {
							// the color mode is unrecognizable add only if is a hex or webcolor
							if (newCCB.isHex) {
								colorBoxes[i].add(newCCB);
							}
						}
						else {
							colorBoxes[i].add(newCCB);
						}
					}
				}
			}
		}
	}
	
	private void createColorBoxesForLights()
	{
		// search code for light color and material color functions.
		Pattern p = Pattern.compile("ambientLight[\\(\\s]|directionalLight[\\(\\s]"+
					"|pointLight[\\(\\s]|spotLight[\\(\\s]|lightSpecular[\\(\\s]"+
					"|specular[\\(\\s]|ambient[\\(\\s]|emissive[\\(\\s]");
		for (int i=0; i<codeTabs.length; i++)
		{
			String tab = codeTabs[i];
			Matcher m = p.matcher(tab);
			
			while (m.find())
			{
				ArrayList<Handle> colorHandles = new ArrayList<Handle>();
				
				// look for the '(' and ')' positions
				int openPar = tab.indexOf("(", m.start());
				int closePar = tab.indexOf(")", m.end());
				if (openPar < 0 || closePar < 0) {
					// ignore this color
					continue;
				}
				
				if (isInComment(m.start(), tab)) {
					// ignore colors in a comment
					continue;
				}

				// put 'colorParamsEnd' after three parameters inside the parenthesis or at the close
				int colorParamsEnd = openPar;
				int commas=3;
				while (commas-- > 0) {
					colorParamsEnd=tab.indexOf(",", colorParamsEnd+1);
					if (colorParamsEnd < 0 ||
							colorParamsEnd > closePar) {
						colorParamsEnd = closePar;
						break;
					}
				}
				
				for (Handle handle : allHandles[i])
				{
					if (handle.startChar > openPar &&
							handle.endChar <= colorParamsEnd) {
						// we have a match
						colorHandles.add(handle);
					}
				}
				
				if (colorHandles.size() > 0) {
					/* make sure there is no other stuff between '()' like variables.
					 * substract all handle values from string inside parenthesis and
					 * check there is no garbage left
					 */
					String insidePar = tab.substring(openPar+1, colorParamsEnd);
					for (Handle h : colorHandles) {
						insidePar = insidePar.replaceFirst(h.strValue, "");
					}

					// make sure there is only ' ' and ',' left in the string.
					boolean garbage = false;
					for (int j=0; j<insidePar.length(); j++) {
						if (insidePar.charAt(j) != ' ' && insidePar.charAt(j) != ',') {
							// don't add this color box because we can not know the
							// real value of this color
							garbage = true;
						}
					}
					
					// create a new color box
					if (!garbage) {
						// find the context of the color (e.g. this.fill() or <object>.fill())
						String context = getObject(m.start(), tab);
						ColorMode cmode = getColorModeForContext(context);
						
						// not adding color operations for modes we couldn't understand
						ColorControlBox newCCB = new ColorControlBox(context, cmode, colorHandles);
						
						if (cmode.unrecognizedMode) {
							// the color mode is unrecognizable add only if is a hex or webcolor
							if (newCCB.isHex) {
								colorBoxes[i].add(newCCB);
							}
						}
						else {
							colorBoxes[i].add(newCCB);
						}
					}
				}
			}
		}
	}

	private ColorMode getColorModeForContext(String context)
	{
		for (ColorMode cm: colorModes) {
			if (cm.drawContext.equals(context)) {
				return cm;
			}
		}
		
		// if non found, create the default color mode for this context and return it
		ColorMode newCM = new ColorMode(context);
		colorModes.add(newCM);
		
		return newCM;
	}

	private void handleMultipleColorModes()
	{
		// count how many color modes per context
		Map<String, Integer> modeCount = new HashMap<String, Integer>();
		for (ColorMode cm : colorModes)
		{
			Integer prev = modeCount.get(cm.drawContext);
			if (prev == null) {
				prev=0;
			}
			modeCount.put(cm.drawContext, prev+1);
		}

		// find the contexts that have more than one color mode
		ArrayList<String> multipleContexts = new ArrayList<String>();
		Set<String> allContexts = modeCount.keySet();
		for (String context : allContexts) {
			if (modeCount.get(context) > 1) {
				multipleContexts.add(context);
			}
		}

		/* keep only hex and web color boxes in color calls
		 * that belong to 'multipleContexts' contexts
		 */
		for (int i=0; i<codeTabs.length; i++) {
			ArrayList<ColorControlBox> toDelete = new ArrayList<ColorControlBox>();
			for (String context : multipleContexts)
			{
				for (ColorControlBox ccb : colorBoxes[i])
				{
					if (ccb.drawContext.equals(context) && !ccb.isHex) {
						toDelete.add(ccb);
					}
				}
			}
			colorBoxes[i].removeAll(toDelete);
		}
	}
	
	public ArrayList<Range>[] getAllScientificNotations()
	{
		ArrayList<Range> notations[] = new ArrayList[codeTabs.length];
		
		Pattern p = Pattern.compile("[+\\-]?(?:0|[1-9]\\d*)(?:\\.\\d*)?[eE][+\\-]?\\d+");
		for (int i=0; i<codeTabs.length; i++)
		{
			notations[i] = new ArrayList<Range>();
			Matcher m = p.matcher(codeTabs[i]);
			while (m.find()) {
				notations[i].add(new Range(m.start(), m.end()));
			}
		}
		
		return notations;
	}

	
	public static boolean containsTweakComment(String[] codeTabs)
	{
		for (String tab : codeTabs) {
			if (hasTweakComment(tab)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public static boolean lineHasTweakComment(int pos, String code)
	{
		int lineEnd = getEndOfLine(pos, code);
		if (lineEnd < 0) {
			return false;
		}
		
		String line = code.substring(pos, lineEnd);		
		return hasTweakComment(line);
	}
	
	private static boolean hasTweakComment(String code)
	{
		Pattern p = Pattern.compile("\\/\\/.*tweak", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(code);
		if (m.find()) {
			return true;
		}
		
		return false;		
	}
	
	
	private boolean isNegativeSign(int pos, String code)
	{
		// go back and look for ,{[(=?+-/*%<>:&|^!~
		for (int i=pos; i>=0; i--)
		{
			char c = code.charAt(i);
			if (c == ' ' || c == '\t') {
				continue;
			}
			if (c==',' || c=='{' || c=='[' || c=='(' ||
					c=='=' || c=='?' || c=='+' || c=='-' ||
					c=='/' || c=='*' || c=='%' || c=='<' ||
					c=='>' || c==':' || c=='&' || c=='|' ||
					c=='^' || c=='!' || c=='~') {
				return true;
			}
			else {
				return false;
			}
		}

		return false;
	}
	
	private int getNumDigitsAfterPoint(String number)
	{
		Pattern p = Pattern.compile("\\.[0-9]+");
		Matcher m = p.matcher(number);
		
		if (m.find()) {
			return m.end() - m.start() - 1;
		}
		else {
			return 0;
		}
	}

	private int countLines(String str)
	{
		String[] lines = str.split("\r\n|\n\r|\n|\r");
		return lines.length;
	}

	/**
	* Are we inside a string? (TODO: ignore comments in the code)
	* @param pos
	* position in the code
	* @param code
	* the code
	* @return
	*/
	private boolean isInsideString(int pos, String code)
	{
		int quoteNum = 0;	// count '"'

		for (int c = pos; c>=0 && code.charAt(c) != '\n'; c--)
		{
			if (code.charAt(c) == '"') {
				quoteNum++;
			}
		}

		if (quoteNum%2 == 1) {
			return true;
		}

		return false;
	}

	/**
	* Is this a global position?
	* @param pos position
	* @param code code
	* @return
	* true if the position 'pos' is in global scope in the code 'code'
 	*/
	private boolean isGlobal(int pos, String code)
	{
		int curlyScope = 0;	// count '{-}'

		for (int c=pos; c>=0; c--)
		{
			if (code.charAt(c) == '{') {
				// check if a function or an array assignment
				for (int cc=c; cc>=0; cc--) {
					if (code.charAt(cc)==')') {
						curlyScope++;
						break;
					}
					else if (code.charAt(cc)==']') {
						break;
					}
					else if (code.charAt(cc)==';') {
						break;
					}
				}
			}
			else if (code.charAt(c) == '}') {
				// check if a function or an array assignment
				for (int cc=c; cc>=0; cc--) {
					if (code.charAt(cc)==')') {
						curlyScope--;
						break;
					}
					else if (code.charAt(cc)==']') {
						break;
					}
					else if (code.charAt(cc)==';') {
						break;
					}
				}
			}
		}

		if (curlyScope == 0) {
			// it is a global position
			return true;
		}

		return false;
	};
	
	private boolean isInComment(int pos, String code)
	{
		// look for one line comment
		int lineStart = getStartOfLine(pos, code);
		if (lineStart < 0) {
			return false;
		}
		if (code.substring(lineStart, pos).indexOf("//") != -1) {
			return true;
		}
		
		// TODO: look for block comments
		
		return false;
	}
	
	public static int getEndOfLine(int pos, String code)
	{
		return code.indexOf("\n", pos);
	}
	
	public static int getStartOfLine(int pos, String code)
	{
		while (pos >= 0) {
			if (code.charAt(pos) == '\n') {
				return pos+1;
			}
			pos--;
		}
		
		return 0;
	}
	
	/** returns the object of the function starting at 'pos'
	 * 
	 * @param pos
	 * @param code
	 * @return
	 */
	private String getObject(int pos, String code)
	{
		boolean readObject = false;
		String obj = "this";
		
		while (pos-- >= 0) {
			if (code.charAt(pos) == '.') {
				if (!readObject) {
					obj = "";
					readObject = true;
				}
				else {
					break;
				}
			}
			else if (code.charAt(pos) == ' ' || code.charAt(pos) == '\t') {
				break;
			}
			else if (readObject) {
				obj = code.charAt(pos) + obj;
			}
		}
		
		return obj;
	}
	
	public static int getSetupStart(String code)
	{
		Pattern p = Pattern.compile("void[\\s\\t\\r\\n]*setup[\\s\\t]*\\(\\)[\\s\\t\\r\\n]*\\{");
		Matcher m = p.matcher(code);
		
		if (m.find()) {
			return m.end();
		}
		
		return -1;
	}

	
	private String replaceString(String str, int start, int end, String put)
	{
		return str.substring(0, start) + put + str.substring(end, str.length());
	}
	
	class Range
	{
		int start;
		int end;
		
		public Range(int s, int e) {
			start = s;
			end = e;
		}
		
		public boolean contains(int v)
		{
			if (v>=start && v<end) {
				return true;
			}
			
			return false;
		}
	}
}
