import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;


/**
 * Ant Task for downloading the latest JRE or JDK from Oracle.
 * This was used to set a cookie properly to retrieve a JRE.
 * Nowadays the older versions have been removed from Oracle's site,
 * so this is hard wired it to use download.processing.org instead.
 */
public class Downloader extends Task {
  static final boolean ORACLE_SUCKS = true;  // that's final

  static final String COOKIE =
    "oraclelicense=accept-securebackup-cookie";

  private int version;  // Java 8
  private int update;   // Update 131
  private int build;    // Build 11
  // https://gist.github.com/P7h/9741922
  // http://stackoverflow.com/q/10268583
  private String hash;  // d54c1d3a095b4ff2b6607d096fa80163

  private boolean jdk;  // false if JRE

  private String flavor;

  private String path;  // target path


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


  public void setHash(String hash) {
    this.hash = hash;
  }


  public void setJDK(boolean jdk) {
    this.jdk = jdk;
  }


  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }


  public void setPath(String path) {
    this.path = path;
  }


  public void execute() throws BuildException {
    if (version == 0) {
      throw new BuildException("Version (i.e. 7 or 8) must be set");
    }

    if (build == 0) {
      throw new BuildException("Build number must be set");
    }

    if (flavor == null) {
      throw new BuildException("You've gotta choose a flavor (macosx-x64.dmg, windows-x64.exe...)");
    }

    if (update >= 121 && hash == null) {
      throw new BuildException("Starting with 8u121, a hash is required, see https://gist.github.com/P7h/9741922");
    }

    try {
      download();
    } catch (IOException e) {
      throw new BuildException(e);
    }
  }


  void download() throws IOException {
    String filename = (jdk ? "jdk" : "jre") +
      (update == 0 ?
       String.format("-%d-%s", version, flavor) :
       String.format("-%du%d-%s", version, update, flavor));

    if (path == null) {
      path = filename;
    }

    String url = "http://download.oracle.com/otn-pub/java/jdk/" +
      (update == 0 ?
       String.format("%d-b%02d/", version, build) :
       String.format("%du%d-b%02d/", version, update, build));

    // URL format changed starting with 8u121
    if (update >= 121) {
      url += hash + "/";
    }

    if (ORACLE_SUCKS) {
      url = "https://download.processing.org/java/";
    }

    // Finally, add the filename to the end
    url += filename;

    HttpURLConnection conn =
      (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestProperty("Cookie", COOKIE);

    //printHeaders(conn);
    //conn.connect();
    while (conn.getResponseCode() == 302 || conn.getResponseCode() == 301) {
      Map<String, List<String>> headers = conn.getHeaderFields();
      List<String> location = headers.get("Location");
      if (location.size() == 1) {
        url = location.get(0);
        System.out.println("Redirecting to " + url);
      } else {
        throw new BuildException("Got " + location.size() + " locations.");
      }
      List<String> cookies = headers.get("Set-Cookie");
      conn = (HttpURLConnection) new URL(url).openConnection();
      if (cookies != null) {
        for (String cookie : cookies) {
          conn.setRequestProperty("Cookie", cookie);
        }
      }
      conn.setRequestProperty("Cookie", COOKIE);
      conn.connect();
    }

    if (conn.getResponseCode() == 200) {
      InputStream input = conn.getInputStream();
      BufferedInputStream bis = new BufferedInputStream(input);
      File outputFile = new File(path); //folder, filename);
      System.out.format("Downloading %s from %s%n", outputFile.getAbsolutePath(), url);
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
