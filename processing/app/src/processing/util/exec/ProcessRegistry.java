package processing.util.exec;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProcessRegistry {
  private static final Set<Process> REGISTRY = Collections
      .synchronizedSet(new HashSet<Process>());

  static {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        synchronized (REGISTRY) {
          for (final Process p : REGISTRY) {
            try {
              //              System.err.println("Cleaning up rogue process " + p);
              p.destroy();
            } catch (final Exception drop) {
            }
          }
        }
      }
    });
  }

  /**
  * When starting up a process
  * @param p
  */
  public static void watch(final Process p) {
    REGISTRY.add(p);
  }

  public static void unwatch(final Process p) {
    REGISTRY.remove(p);
  }

}
