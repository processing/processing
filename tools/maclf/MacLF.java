import java.io.*;


public class MacLF
{
  static final int MAC = 0;
  static final int UNIX = 1;
  static final int WINDOWS = 2;

  static public void main(String args[]) {
    if (args.length == 0) {
      System.err.println("maclf <filename>");
      System.exit(0);
    }
    int kind = MAC;

    for (int i = 0; i < args.length; i++) {
      File file = new File(args[i]);
      if (file.isDirectory()) {
	  dir(file, kind);
      } else {
	  load(file, kind);
      }
    }
  }

    static public void dir(File dir, int kind) {
	String files[] = dir.list();
	for (int i = 0; i < files.length; i++) {
	    File file = new File(dir, files[i]);
	    if (file.isDirectory()) {
		dir(file, kind);
	    } else {
		load(file, kind);
	    }
	}
    }

  static public void load(File file, int kind) {
    try {
      if (file.isDirectory()) {
	String list[] = file.list();
	for (int i = 0; i < list.length; i++) {
	  load(new File(file, list[i]), kind);
	}
      } else {
	new MacLF(file, kind);
      }
    } catch (Exception e) {
      System.err.println(file);
      e.printStackTrace();
    }
  }
  
  public MacLF(File file, int outputKind) 
       throws FileNotFoundException, IOException {
    String path = null;
    try {
      path = file.getCanonicalPath();
    } catch (IOException e) {
      path = file.getPath();
    }
    FileInputStream input = new FileInputStream(file);
    byte data[] = null;
    data = readBytes(input, 0, (int)file.length());
    input.close();

    File tempFile = new File(path + ".temp");
    FileOutputStream output = null;
    output = new FileOutputStream(tempFile);
    for (int i = 0; i < data.length; i++) {
      if (data[i] == 10) {
	// got a unix lf
	outputLF(output, outputKind);
      } else if (data[i] == 13) {
	// mac or windows
	outputLF(output, outputKind);
	if (((i + 1) != data.length) &&
	    (data[i+1] == 10)) {
	  // windows, skip LF after CR
	  i++;
	}
      } else {
	output.write(data[i]);
      }
    }
    output.flush();
    output.close();
    if (!file.delete()) {
      System.err.println("Could not delete original file.");
    } else {
      if (!tempFile.renameTo(file)) {
	System.err.println("Could not rename temp file.");
      }
    } 
  }
  
  protected void outputLF(OutputStream output, int outputKind)
       throws IOException {
    if (outputKind == UNIX) {
      output.write(10);
    } else if (outputKind == WINDOWS) {
      output.write(13);
      output.write(10);
    } else if (outputKind == MAC) {
      output.write(13);
    }
  }


  static public byte[] readBytes(InputStream input, int start, int length)
    throws IOException 
  {
    byte[] returning = new byte[length];

    while (true) {
      int byteCount = input.read(returning, start, length);
      if (byteCount <= 0)
	break;

      start += byteCount;
      length -= byteCount;
    }
    return returning;
  }
}
