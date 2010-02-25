/**
 * 
 */
package processing.app.tools.android;

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
 * and informs its LineProcessors
 * until the InputStream is exhausted. <b>It is useful only for pumping lines of
 * text, and not for arbitrary binary cruft.</b> It's handy for reading
 * the output of processes that emit textual data, for example.
 * 
 * @author Jonathan Feinberg &lt;jdf@us.ibm.com&gt;
 *
 */
class StreamPump implements Runnable {
  private static final ExecutorService threads = Executors
      .newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(final Runnable r) {
          final Thread t = new Thread(r);
          t.setDaemon(true);
          t.setName("StreamPump " + t.getId());
          return t;
        }
      });

  private final BufferedReader reader;
  private final List<LineProcessor> outs = new CopyOnWriteArrayList<LineProcessor>();

  public StreamPump(final InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in));
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
    threads.execute(this);
  }

  @Override
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
      e.printStackTrace(System.err);
    }
  }

  static public class WriterLineProcessor implements LineProcessor {
    private final PrintWriter writer;

    public WriterLineProcessor(final OutputStream out) {
      this.writer = new PrintWriter(out, true);
    }

    public WriterLineProcessor(final Writer writer) {
      this.writer = new PrintWriter(writer, true);
    }

    @Override
    public void processLine(final String line) {
      writer.println(line);
    }
  }
}
