package processing.app.exec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * <p>A StreamPump reads lines of text from its given InputStream 
 * and informs its LineProcessors until the InputStream is exhausted. 
 * <b>It is useful only for pumping lines of text, and not for arbitrary 
 * binary cruft.</b> It's handy for reading the output of processes that 
 * emit textual data, for example.
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 *
 */
public class StreamPump implements Runnable {
  private static final ExecutorService threads = 
    Executors.newCachedThreadPool(new ThreadFactory() {
        public Thread newThread(final Runnable r) {
          final Thread t = new Thread(r);
          t.setDaemon(true);
          t.setName("StreamPump " + t.getId());
          return t;
        }
      });

  private final BufferedReader reader;
  private final List<LineProcessor> outs = new CopyOnWriteArrayList<LineProcessor>();
  private final String name;

  public StreamPump(final InputStream in, final String name) {
    this.reader = new BufferedReader(new InputStreamReader(in));
    this.name = name;
  }

  public StreamPump addTarget(final OutputStream out) {
    outs.add(new WriterLineProcessor(out));
    return this;
  }

  public StreamPump addTarget(final Writer out) {
    outs.add(new WriterLineProcessor(out));
    return this;
  }

  public StreamPump addTarget(final LineProcessor out) {
    outs.add(out);
    return this;
  }

  public void start() {
//    System.out.println("starting new StreamPump");
//    new Exception().printStackTrace(EditorConsole.systemOut);
    threads.execute(this);
  }

  public void run() {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        for (final LineProcessor out : outs) {
          try {
            out.processLine(line);
          } catch (final Exception e) {
          }
        }
      }
    } catch (final IOException e) {
//      System.err.println("StreamPump: " + name);
//      e.printStackTrace(System.err);
      throw new RuntimeException("Inside " + this + " for " + name, e);
    }
  }

  private static class WriterLineProcessor implements LineProcessor {
    private final PrintWriter writer;

    private WriterLineProcessor(final OutputStream out) {
      this.writer = new PrintWriter(out, true);
    }

    private WriterLineProcessor(final Writer writer) {
      this.writer = new PrintWriter(writer, true);
    }

    public void processLine(final String line) {
      writer.println(line);
    }
  }

//  public static final LineProcessor DEVNULL = new LineProcessor() {
//    public void processLine(final String line) {
//      // noop
//    }
//  };
}
