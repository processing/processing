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
    "oraclelicense=accept-securebackup-cookie";

  private boolean openJdk; // If using openJDK.
  private String platform; // macos
  private int train;  // Java 11 (was 1 through Java 8)
  private int version;  // 0 (was 8 prior to Java 9)
  private int update;   // Update 131
  private int build;    // Build 11
  // https://gist.github.com/P7h/9741922
  // http://stackoverflow.com/q/10268583
  private String hash;  // d54c1d3a095b4ff2b6607d096fa80163

  private boolean jdk;  // false if JRE

  private String flavor;

  private String path;  // target path


  public Downloader() { }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public void setOpenJdk(boolean openJdk) {
    this.openJdk = openJdk;
  }

  public void setTrain(int train) {
    this.train = train;
  }

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
    if (train == 0) {
      throw new BuildException("Train (i.e. 1 or 11) must be set");
    }

    boolean isJava11 = train == 11;
    if (!isJava11 && version == 0) {
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

    //download(path, jdk, platform, bits, version, update, build);
    try {
      download();
    } catch (IOException e) {
      throw new BuildException(e);
    }
  }

  void download() throws IOException {
    DownloadUrlGenerator downloadUrlGenerator;

    if (openJdk) {
      downloadUrlGenerator = new AdoptOpenJdkDownloadUrlGenerator();
    } else {
      downloadUrlGenerator = new OracleDownloadUrlGenerator();
    }

    if (path == null) {
      path = downloadUrlGenerator.getLocalFilename(
              platform,
              jdk,
              train,
              version,
              update,
              build,
              flavor,
              hash
      );
    }

    String url = downloadUrlGenerator.buildUrl(
            platform,
            jdk,
            train,
            version,
            update,
            build,
            flavor,
            hash
    );

    System.out.println("Attempting download at " + url);

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
