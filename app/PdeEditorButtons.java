#ifdef EDITOR


import java.awt.*;


public class PdeEditorButtons extends Panel {
  static final String EMPTY_STATUS = "                                                                 ";

  // play, stop, save, export, open

  static final String title[] = {
    "", "play", "stop", "save", "open", "export"
    //"", "Play", "Stop", "Save", "Open", "Export"
    //"Play", "Stop", "Close",
    //"Open", "Save", "Export Applet", "Print", "Beautify",
    //"Disable Full Screen", "Full Screen"
  };

  static final int BUTTON_COUNT  = title.length;
  static final int BUTTON_WIDTH  = PdeEditor.GRID_SIZE; //33;
  static final int BUTTON_HEIGHT = PdeEditor.GRID_SIZE; //33;

  static final int NOTHING    = 0;
  static final int PLAY       = 1;
  static final int STOP       = 2;

  static final int SAVE     = 3;
  static final int OPEN     = 4;
  static final int EXPORT   = 5;

  //static final int PRINT               = 6;
  //static final int BEAUTIFY            = 7;
  //static final int DISABLE_FULL_SCREEN = 8;
  //static final int FULL_SCREEN         = 9;

  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;

  PdeEditor editor;
  Label status;

  Image offscreen;
  int width, height;

  Image buttons;
  Image inactive[];
  Image rollover[];
  Image active[];
  int currentRollover;
  int currentSelection;

  int buttonCount;
  int state[];
  Image stateImage[];
  int which[]; // mapping indices to implementation

  //int x1[], x2[];
  //int y1, y2;
  int x1, x2;
  int y1[], y2[];


  public PdeEditorButtons(PdeEditor editor /*, boolean useOpenSave,
			  boolean useCourseware, boolean usePrint, 
			  boolean useBeautify*/) {
    this.editor = editor;
    // this could be causing trouble
    buttons = PdeApplet.readImage("buttons.gif");

    buttonCount = 0;
    which = new int[BUTTON_COUNT];

    which[buttonCount++] = NOTHING;
    which[buttonCount++] = PLAY;
    which[buttonCount++] = STOP;
    which[buttonCount++] = SAVE;
    which[buttonCount++] = OPEN;
    which[buttonCount++] = EXPORT;

    currentRollover = -1;

    setLayout(null);
    status = new Label();
    status.setFont(PdeApplet.getFont("editor.buttons.status.font",
				     new Font("SansSerif", Font.PLAIN, 10)));
    status.setForeground(PdeApplet.getColor("editor.buttons.status.color",
					    Color.black));
    //status.setForeground(Color.black);
    //status.setBackground(Color.yellow);
    add(status);
    status.setBounds(-5, BUTTON_COUNT*BUTTON_HEIGHT, 
		     BUTTON_WIDTH + 15, BUTTON_HEIGHT);
    status.setAlignment(Label.CENTER);
  }


  public void update() {
    //System.out.println(currentRollover);
    //System.out.println("PdeEditorButtons.update()");
    paint(this.getGraphics());
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics screen) {
    //System.out.println("PdeEditorButtons.paint() " + screen);

    if (inactive == null) {
      inactive = new Image[BUTTON_COUNT];
      rollover = new Image[BUTTON_COUNT];
      active = new Image[BUTTON_COUNT];
      state = new int[BUTTON_COUNT];

      for (int i = 0; i < BUTTON_COUNT; i++) {
	inactive[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
	Graphics g = inactive[i].getGraphics();
	g.drawImage(buttons, -(i*BUTTON_WIDTH), -2*BUTTON_HEIGHT, null);

	rollover[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
	g = rollover[i].getGraphics();
	g.drawImage(buttons, -(i*BUTTON_WIDTH), -1*BUTTON_HEIGHT, null);

	active[i] = createImage(BUTTON_WIDTH, BUTTON_HEIGHT);
	g = active[i].getGraphics();
	g.drawImage(buttons, -(i*BUTTON_WIDTH), -0*BUTTON_HEIGHT, null);
      }

      state = new int[buttonCount];
      stateImage = new Image[buttonCount];
      for (int i = 0; i < buttonCount; i++) {
	setState(i, INACTIVE, false);
      }
    }
    Dimension size = size();
    if ((offscreen == null) || 
	(size.width != width) || (size.height != height)) {
      offscreen = createImage(size.width, size.height);
      width = size.width;
      height = size.height;

      x1 = 0; 
      x2 = BUTTON_WIDTH;

      y1 = new int[buttonCount];
      y2 = new int[buttonCount];

      int offsetY = 0;
      for (int i = 0; i < buttonCount; i++) {
	y1[i] = offsetY;
	y2[i] = offsetY + BUTTON_HEIGHT;
	offsetY = y2[i];
      }

      /*
	// horizontal alignment
      x1 = new int[buttonCount];
      x2 = new int[buttonCount];

      y1 = (height - BUTTON_HEIGHT) / 2;
      y2 = y1 + BUTTON_HEIGHT;

      int offsetX = 8;
      //for (int i = 0; i < 2; i++) {
      for (int i = 0; i < buttonCount; i++) {
	//g.drawImage(stateImage[i], offsetX, offsetY, null);
	x1[i] = offsetX;
	x2[i] = offsetX + BUTTON_WIDTH;
	offsetX += BUTTON_WIDTH + 4;
	// extra space after play/stop/close
	if (i == GAP_POSITION) offsetX += 8;  
      }
      */

      /*
      // start from righthand side and move left
      offsetX = width - 8 - BUTTON_WIDTH;
      for (int i = buttonCount-1; i >= 2; --i) {
	//g.drawImage(stateImage[i], offsetX, offsetY, null);
	x1[i] = offsetX;
	x2[i] = offsetX + BUTTON_WIDTH;
	offsetX -= BUTTON_WIDTH + 4;
      }
      */
    }
    Graphics g = offscreen.getGraphics();
    g.setColor(getBackground());
    g.fillRect(0, 0, width, height);

    for (int i = 0; i < buttonCount; i++) {
      //g.drawImage(stateImage[i], x1[i], y1, null);
      g.drawImage(stateImage[i], x1, y1[i], null);
    }
    //g.drawImage(stateImage[i], offsetX, offsetY, null);
    /*
    //Dimension dim = size();
    int offsetY = (height - BUTTON_HEIGHT) / 2;

    int offsetX = 8;
    for (int i = 0; i < 2; i++) {
      g.drawImage(stateImage[i], offsetX, offsetY, null);
      offsetX += BUTTON_WIDTH + 4;
    }

    // start from righthand side and move left
    offsetX = width - 8 - BUTTON_WIDTH;
    for (int i = buttonCount-1; i >= 2; --i) {
      g.drawImage(stateImage[i], offsetX, offsetY, null);
      offsetX -= BUTTON_WIDTH + 4;
    }
    */
    screen.drawImage(offscreen, 0, 0, null);
    //screen.fillRect(0, 0, 10, 10);
  }


  public boolean mouseMove(Event e, int x, int y) {
    //System.out.println(x + ", " + y);
    if (currentRollover != -1) {
      if ((y > y1[currentRollover]) && (x > x1) &&
	  (y < y2[currentRollover]) && (x < x2)) {
      //if ((x > x1[currentRollover]) && (y > y1) &&
      //  (x < x2[currentRollover]) && (y < y2)) {
	//System.out.println("same");
	return true; // no change

      } else {
	//state[currentRollover] = INACTIVE_STATE;
	//stateImage[currentRollover] = inactive[currentRollover];
	setState(currentRollover, INACTIVE, true);
	messageClear(title[currentRollover]);
	currentRollover = -1;
	//update();
      }
    }
    int sel = findSelection(x, y);
    if (sel == -1) return true;
    
    if (state[sel] != ACTIVE) {
      //state[sel] = ROLLOVER_STATE;
      //stateImage[sel] = rollover[sel];
      setState(sel, ROLLOVER, true);
      currentRollover = sel;
    }
    /*
    for (int i = 0; i < buttonCount; i++) {
      if ((x > x1[i]) && (y > y1) &&
	  (x < x2[i]) && (y < y2)) {
	//System.out.println(i);
	if (state[i] != ACTIVE_STATE) {
	  state[i] = ROLLOVER_STATE;
	  stateImage[i] = rollover[i];
	  currentRollover = i;
	}
	update();
	return true;
      }
    }
    */
    //update();
    return true;
  }

  private int findSelection(int x, int y) {
    for (int i = 0; i < buttonCount; i++) {
      if ((x > x1) && (y > y1[i]) &&
	  (x < x2) && (y < y2[i])) {
	//if ((x > x1[i]) && (y > y1) &&
	//(x < x2[i]) && (y < y2)) {
	return i;
      }
    }
    return -1;
  }

  private void setState(int slot, int newState, boolean updateAfter) {
    //if (inactive == null) return;
    state[slot] = newState;
    switch (newState) {
    case INACTIVE:
      stateImage[slot] = inactive[which[slot]]; 
      break;
    case ACTIVE: 
      stateImage[slot] = active[which[slot]]; 
      break;
    case ROLLOVER: 
      stateImage[slot] = rollover[which[slot]]; 
      message(title[which[slot]]);
      break;
    }
    if (updateAfter) update();
  }

  public boolean mouseEnter(Event e, int x, int y) {
    return mouseMove(e, x, y);
  }

  public boolean mouseExit(Event e, int x, int y) {
    // kludge
    for (int i = 0; i < BUTTON_COUNT; i++) {
      messageClear(title[i]);
    }
    return mouseMove(e, x, y);
  }

  int wasDown = -1;

  public boolean mouseDown(Event e, int x, int y) {
    int sel = findSelection(x, y);
    if (sel == -1) return false;
    currentRollover = -1;
    currentSelection = sel;
    setState(sel, ACTIVE, true);

    return true;
  }


  public boolean mouseUp(Event e, int x, int y) {
    //switch (which[sel]) {
    switch (currentSelection) {

    case PLAY: editor.doPlay(); break;
    case STOP: setState(PLAY, INACTIVE, true); editor.doStop(); break;
      //case CLOSE: editor.doClose(); break;

    case OPEN:  editor.doOpen(); break;
      //editor.doOpen(this, BUTTON_WIDTH, OPEN * BUTTON_HEIGHT); 

    case SAVE: editor.doSaveAs(); break;
    case EXPORT: editor.doExport(); break;
      //case PRINT: editor.doPrint(); break;
      //case BEAUTIFY: editor.doBeautify(); break;

      /*
    case FULL_SCREEN: 
      editor.enableFullScreen(); 
      which[buttonCount-1] = DISABLE_FULL_SCREEN;
      message(title[which[buttonCount-1]]);
      break;
    case DISABLE_FULL_SCREEN: 
      editor.disableFullScreen(); 
      which[buttonCount-1] = FULL_SCREEN;
      message(title[which[buttonCount-1]]);
      break;
      */
    }

    currentSelection = -1;
    //update();
    return true;
  }

  public void clear() { // (int button) {
    if (inactive == null) return;

    //setState(button, INACTIVE);
    // skip the play button, do the others
    for (int i = 1; i < buttonCount; i++) {
      //state[i] = INACTIVE;
      //stateImage[i] = inactive[which[i]];
      setState(i, INACTIVE, false);
    }
    update();
  }

  public void play() {
    if (inactive == null) return;
    clear();
    setState(0, ACTIVE, true);
  }

  public void clearPlay() {
    if (inactive == null) return;
    setState(0, INACTIVE, true);
  }

  /*
  public boolean mouseUp(Event e, int x, int y) {
    if (wasDown == -1) return true;
    if (which[wasDown] == PLAY) return true;

    setState(wasDown, INACTIVE);
    wasDown = -1;
    //update();
    return true;
  }
  */

  public void message(String msg) {  // formerly part of PdeEnvironment
    status.setText(msg + "  ");  // don't mind the hack
  }

  public void messageClear(String msg) {
    if (status.getText().equals(msg + "  ")) status.setText(PdeEditor.EMPTY);
  }


  public Dimension preferredSize() {
    return new Dimension(BUTTON_WIDTH, (BUTTON_COUNT + 1)*BUTTON_HEIGHT);
  }
}

#endif
