// special subclass only used inside the pde environment
// while the kjc engine is in use. takes care of error handling.

public class KjcApplet extends BApplet {
    PdeRuntime pdeRuntime;

  public void setRuntime(PdeRuntime pdeRuntime) {
    this.pdeRuntime = pdeRuntime;
  }

  public void run() {
    try {
      super.run();

    } catch (Exception e) {
      //System.out.println("ex found in run");
      e.printStackTrace();
      //engine.error(e);
      pdeRuntime.newMessage = true;
      e.printStackTrace(pdeRuntime.leechErr);
    }
  }  
}


/*

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

*/
