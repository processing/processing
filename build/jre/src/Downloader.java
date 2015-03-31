import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Ant Task for downloading the latest JRE or JDK from Oracle.
 */
public class Downloader extends Task {
  static final String COOKIE =
    "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; " +
    "oraclelicense=accept-securebackup-cookie";

  private int version;  // Java 8
  private int update;   // Update 31
  private int build;    // Build 13

  private boolean jdk;  // false if JRE

//  private String platform;  // macosx, windows, linux
//  private String bits;  // i586 or x64
  private String flavor;
  
  private String path;  // target path
//  private File baseDir;
//  private boolean includeRecorder;


  public Downloader() { }


  public void setVersion(int version) {
    this.version = version;
  }


  public void setUpdate(int update) {
    this.update = update;
  }


  public void setBuild(int build) {
    this.build = build;
  }


  public void setJDK(boolean jdk) {
    this.jdk = jdk;
  }
  
  
//  public void setPlatform(String platform) {
//    this.platform = platform;
//  }
  
  
//  public void setBits(String bits) {
//    this.bits = bits;
//  }
  
  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }
  
  
  public void setPath(String path) {
    this.path = path;
  }


  public void execute() throws BuildException {
    //if (baseDir == null) {
    //  throw new BuildException("dir parameter must be set!");
    //}
    
    if (version == 0) {
      throw new BuildException("version (i.e. 7 or 8) must be set");
    }
    
    if (build == 0) {
      throw new BuildException("build number must be set");
    }
    
    if (flavor == null) {
      throw new BuildException("you've gotta choose a flavor (macosx-x64.dmg, windows-x64.exe...");
    }
//    if (bits == null) {
//      throw new BuildException("bits must be set (x64 or i586)");
//    }

    //download(path, jdk, platform, bits, version, update, build);
    try {
      download();
    } catch (IOException e) {
      throw new BuildException(e);
    }
    
    /*
    downloadJRE("linux-i586.tar.gz");
    downloadJRE("linux-x64.tar.gz");
    downloadJRE("windows-i586.tar.gz");
    downloadJRE("windows-x64.tar.gz");
    downloadJRE("windows-i586.exe");
    downloadJRE("windows-x64.exe");
    downloadJRE("macosx-x64.dmg");
    downloadJRE("macosx-x64.tar.gz");

    downloadJDK("linux-i586.tar.gz");
    downloadJDK("linux-x64.tar.gz");
    downloadJDK("windows-i586.exe");
    downloadJDK("windows-x64.exe");
    downloadJDK("macosx-x64.dmg");
    */
  }


//  static void download(String path, //File folder, String filename,
//                       boolean jdk, String platform, String bits,
//                       int version, int update, int build) {
  void download() throws IOException {
    //HttpURLConnection.setFollowRedirects(true);
    String filename = (jdk ? "jdk" : "jre") +
      (update == 0 ?
       String.format("-%d-%s", version, flavor) :
       String.format("-%du%d-%s", version, update, flavor));
    
    if (path == null) {
      path = filename;  //System.getProperty("user.dir");
    }

    //String url = "http://download.oracle.com/otn-pub/java/jdk/" +
    // https://edelivery.oracle.com/otn-pub/java/jdk/7u45-b18/jre-7u45-linux-i586.tar.gz
    String url = "https://edelivery.oracle.com/otn-pub/java/jdk/" +
    //String url = "https://download.oracle.com/otn-pub/java/jdk/" +
      (update == 0 ?
       String.format("%d-b%02d/", version, build) :
       String.format("%du%d-b%02d/", version, update, build)) + filename;
//    System.out.println(url);

    HttpURLConnection conn =
        (HttpURLConnection) new URL(url).openConnection();
    //conn.setRequestProperty("Cookie", "name1=value1; name2=value2");
    conn.setRequestProperty("Cookie", COOKIE);
    //conn.setRequestProperty("Cookie", "gpw_e24=http://www.oracle.com/");

    //printHeaders(conn);
    //conn.connect();
    if (conn.getResponseCode() == 302) {
      Map<String, List<String>> headers = conn.getHeaderFields();
      List<String> location = headers.get("Location");
      if (location.size() == 1) {
        url = location.get(0);
      } else {
        throw new RuntimeException("Got " + location.size() + " locations.");
      }
      List<String> cookies = headers.get("Set-Cookie");
      conn = (HttpURLConnection) new URL(url).openConnection();
      for (String cookie : cookies) {
        conn.setRequestProperty("Cookie", cookie);
      }
      conn.setRequestProperty("Cookie", COOKIE);
      conn.connect();
    }

    if (conn.getResponseCode() == 200) {
      InputStream input = conn.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(input);
      File outputFile = new File(path); //folder, filename);
      // Write to a temp file so that we don't have an incomplete download
      // masquerading as a good archive.
      File tempFile = File.createTempFile("download", "", outputFile.getParentFile());
      BufferedOutputStream output =
        new BufferedOutputStream(new FileOutputStream(tempFile));
      int c = bis.read();
      while (c != -1) {
        output.write(c);
        c = bis.read();
      }
      bis.close();
      output.flush();
      output.close();

      if (outputFile.exists()) {
        if (!outputFile.delete()) {
          throw new BuildException("Could not delete old download: " + outputFile.getAbsolutePath());
        }
      }
      if (!tempFile.renameTo(outputFile)) {
        throw new BuildException(String.format("Could not rename %s to %s", 
                                               tempFile.getAbsolutePath(), 
                                               outputFile.getAbsolutePath()));
      }
    } else {
      printHeaders(conn);
      System.exit(1);
    }
  }


  static void printHeaders(URLConnection conn) {
    Map<String, List<String>> headers = conn.getHeaderFields();
    Set<Map.Entry<String, List<String>>> entrySet = headers.entrySet();
    for (Map.Entry<String, List<String>> entry : entrySet) {
      String headerName = entry.getKey();
      System.out.println("Header Name:" + headerName);
      List<String> headerValues = entry.getValue();
      for (String value : headerValues) {
        System.out.print("Header value:" + value);
      }
      System.out.println();
      System.out.println();
    }
  }
}
