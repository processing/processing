import java.awt.*;
import java.io.*;
import java.net.*;


public class BAppletViewer implements Runnable {
  BApplet applet;
  Thread killer;
  //int portnum;
  //Socket umbilical;
  //OutputStream umbilicalOut;
  //InputStream umbilicalIn;


  static public void main(String args[]) {
    try {
      //portnum = Integer.parseInt(args[1]);
      //umbilical = new Socket("localhost", portnum);

      new BAppletViewer(args[0], 
			Integer.parseInt(args[1]),
			Integer.parseInt(args[2]));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public BAppletViewer(String name, int x1, int y1) throws Exception {
    Class c = Class.forName(name);
    applet = (BApplet) c.newInstance();
    applet.init();
    applet.start();

    Window window = new Window(new Frame());
    window.setBounds(x1 - applet.width, y1, applet.width, applet.height);
    window.add(applet);
    applet.setBounds(0, 0, applet.width, applet.height);
    window.show();
    applet.requestFocus();  // necessary for key events

    //umbilical = new Socket("localhost", portnum);
    //umbilicalOut = umbilical.getOutputStream();
    //umbilicalIn = umbilical.getInputStream();

    killer = new Thread(this);
    killer.start();
  }


  File deathNotice = new File("die");

  public void run() {
    //while (Thread.currentThread() == killer) {
    while (true) {
      if (deathNotice.exists()) {
	deathNotice.delete();
	System.exit(0);
      }
	//try {
	//System.out.println("testing");
	//umbilicalOut.write(100);
	//umbilicalIn.read();
	//} catch (Exception e) {
	//e.printStackTrace();
	//System.exit(0);
	//}
      try {
	Thread.sleep(100);
      } catch (InterruptedException e) { }
    }
  }
}
