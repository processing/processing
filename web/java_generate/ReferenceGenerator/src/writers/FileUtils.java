package writers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;

//taken from processing source, yay

public class FileUtils {
	
	static public String nf(int num, int digits) {
	    NumberFormat int_nf;

	    int_nf = NumberFormat.getInstance();
	    int_nf.setGroupingUsed(false); // no commas
	    
	    int_nf.setMinimumIntegerDigits(digits);
	    
	    return int_nf.format(num);
	 }
	
	static public String[] loadStrings(String filename){
		InputStream is = createInput(filename);
	    if (is != null) return loadStrings(is);
	    return null;
	}
	
	static public InputStream createInput(String filename) {
		File file = new File(filename);
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	static public String[] loadStrings(InputStream input) {
	    try {
	      BufferedReader reader =
	        new BufferedReader(new InputStreamReader(input, "UTF-8"));

	      String lines[] = new String[100];
	      int lineCount = 0;
	      String line = null;
	      while ((line = reader.readLine()) != null) {
	        if (lineCount == lines.length) {
	          String temp[] = new String[lineCount << 1];
	          System.arraycopy(lines, 0, temp, 0, lineCount);
	          lines = temp;
	        }
	        lines[lineCount++] = line;
	      }
	      reader.close();

	      if (lineCount == lines.length) {
	        return lines;
	      }

	      // resize array to appropriate amount for these lines
	      String output[] = new String[lineCount];
	      System.arraycopy(lines, 0, output, 0, lineCount);
	      return output;

	    } catch (IOException e) {
	      e.printStackTrace();
	      //throw new RuntimeException("Error inside loadStrings()");
	    }
	    return null;
	  }
}
