public class PdeRunner implements Runnable {
  //DbnGraphics graphics;
  //PdeEnvironment env;
  PdeEditor editor;
  String program;

  PdeEngine engine;
  // dbn definitely needs an engine, 
  // for the others it's just an interface

  static final int RUNNER_STARTED = 0;
  static final int RUNNER_FINISHED = 1;
  static final int RUNNER_ERROR = -1;
  static final int RUNNER_STOPPED = 2;
  int state = RUNNER_FINISHED;

  Thread thread;
  boolean forceStop;


  public PdeRunner(PdeEditor editor) {
    this(editor, "");
  }

  public PdeRunner(PdeEditor editor, String program) { 
    this.program = program;
    this.editor = editor;
  }


  public void setProgram(String program) {
    this.program = program;
  }


  public void start() {
    run(); 
    /*
    if (thread != null) {
      try { 
	thread.stop(); 
      } catch (Exception e) { }
      thread = null;
    }
    thread = new Thread(this, "PdeRunner");
    thread.start();
    */
  }


  public void run() {
    state = RUNNER_STARTED;
    //graphics.reset();  // remove for pde

    try {
      if (program.length() == 0) {

	/*
      } else if (program.indexOf('#') < 2) { //charAt(0) == '#') {
#ifdef PYTHON

#ifdef OPENGL
	program = "#\r\n" + 
	  "import DbnEditorGraphics3D\r\n" +
	  "import ExperimentalCanvas\r\n" +
	  "g = DbnEditorGraphics3D.getCurrentGraphics()\r\n" +
	  "glc = g.canvas\r\n" +
	  "gl = glc.getGL()\r\n" +
	  "glj = glc.getGLJ()\r\n" + program;
#endif

	forceStop = true;
	engine = new PythonEngine(program);
	engine.start();
	forceStop = false;
#else
	throw new Exception("python support not included");
#endif
	*/

	//      } else if (program.indexOf("extends ProcessingApplet") != -1) {
	//#ifdef JAVAC
	//	engine = new JavacEngine(program, graphics);
	//	engine.start();
	//#else
	//	throw new Exception("javac support not included");
	//#endif

      } else if (program.indexOf("// dbn") == 0) {
#ifdef DBN
	String pre = "set red 0; set green 1; set blue 2; " + 
	  "set quicktime 0; set tiff 1; set illustrator 2; ";
	DbnParser parser = 
	  new DbnParser(DbnPreprocessor.process(pre + program));

	DbnToken root = parser.getRoot();
	//root.print();
	if (!root.findToken(DbnToken.SIZE)) {
	  graphics.size(101, 101, 1);
	}
	if (root.findToken(DbnToken.REFRESH)) {
	  graphics.aiRefresh = false;
	}
	engine = new DbnEngine(root, graphics);
	engine.start();
#else
	throw new Exception("dbn support not included");
#endif

      } else {
	/*
	forceStop = true;
	engine = new PythonEngine(program);
	engine.start();
	forceStop = false;
	*/

	engine = new KjcEngine(program, editor);
	engine.start();

	/*
	while (!((KjcEngine)engine).applet.finished) { 
	  System.out.println("waiting");
	  try {
	    Thread.sleep(500);
	  } catch (InterruptedException e) { }
	}
	*/
      }

      // maybe this code shouldn't be called automatically, 
      // and instead ProcessingApplet and the others 
      // must call it explicitly
      //System.out.println("finished");
      /*
      state = RUNNER_FINISHED;
      System.out.println("finishing");
      env.finished();
      */
      //graphics.update();  // removed for pde

    } catch (PdeException e) { 
      state = RUNNER_ERROR;
      forceStop = false;
      this.stop();
      editor.error(e);

    } catch (Exception e) {
#ifndef KVM
      e.printStackTrace();
#endif
      this.stop();
    }	
  }


  public void finished() {  // called by KjcProcessingApplet or something
    state = RUNNER_FINISHED;
    editor.finished();
  }


  public void stop() {
    if (engine != null) {
      engine.stop();
      /*
      if (forceStop) {
	thread.stop();
	thread = null;
      }
      */
      // is this necessary [fry]
      //engine = null;
    }
  }
}
