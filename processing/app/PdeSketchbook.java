#ifdef HELL_HAS_FROZEN_OVER


//import at.dms.kjc.*;


public class PdeSketchbook {
  int fileCount;
  String files[] = new String[100];
  long lastCompile[];
  boolean shouldCompile[];

  at.dms.kjc.Main compiler;


  public PdeSketchbook() {
    compiler = new at.dms.kjc.Main();
  }


  public void compile() {
    buildFileList();
    shouldCompile = new boolean[fileCount];

    // read cache that lists when all the last compiles were
    int cacheCount = 0;
    try {
      DataInputStream dis = 
	new DataInputStream(new FileInputStream("lib/sketchbook/cache"));
      lastUpdate = dis.readLong();
    } catch (IOException e) { }

    // search through sketchbook directory 
    // and compile everything that's new since last run
    
  }

  
  public void buildFileList() {
    buildFileList("sketchbook");
    fileCount = 0;
  }

  public void buildFileList(String base) {
    File dir = new File(base);
    String listing[] = dir.list();
    for (int i = 0; i < listing.length; i++) {
      String fullname = base + File.separator + listing[i];

      if (listing[i].endsWith(".java")) {
	if (files.length == fileCount) {
	  String temp[] = new String[fileCount<<1];
	  System.arraycopy(files, 0, temp, 0, fileCount);
	  files = temp;
	}
	files[fileCount++] = fullname;

      } else if (new File(fullname).isDirectory) {
	buildFileList(fullname);
      }
    }
  }
}


#endif
