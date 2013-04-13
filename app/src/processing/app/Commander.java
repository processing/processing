package processing.app;

import java.io.PrintStream;
import java.lang.reflect.Method;

public class Commander {

  public static void main(String[] args) throws Exception {
    if (args.length <= 1) {
      printHelp(System.out);
    }

    Class<?> commander = Class.forName("processing.mode." + args[0] + ".Commander");
    Method entryPoint = commander.getMethod("main", String[].class);

    String[] newArgs = new String[args.length - 1];
    System.arraycopy(args, 1, newArgs, 0, newArgs.length);

    entryPoint.invoke(null, (Object) newArgs);
  }

  private static void printHelp(PrintStream out) {
    out.println("Command line front-end for Processing.");
    out.println("First argument is the mode, which should be launched.");
    out.println();
    out.println("ONLY 'java' and 'android' modes are supported.");
    out.println();
    out.println("Usage: ");
    out.println("./processing-cli java --sketch=...");
    out.println("./processing-cli android --sketch=...");
    out.println();
  }
}
