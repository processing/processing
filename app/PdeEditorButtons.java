import java.awt.*;
import java.awt.event.*;

//import javax.swing.*;


public class PdeEditorButtons extends Panel /*implements ActionListener*/ {
  static final String EMPTY_STATUS = "                                                                 ";

  // run, stop, save, export, open

  static final String title[] = {
    "", "run", "stop", "new", "open", "save", "export"
    //"", "Run", "Stop", "Save", "Open", "Export"
    //"Run", "Stop", "Close",
    //"Open", "Save", "Export Applet", "Print", "Beautify",
    //"Disable Full Screen", "Full Screen"
  };

  static final int BUTTON_COUNT  = title.length;
  static final int BUTTON_WIDTH  = PdeEditor.GRID_SIZE; //33;
  static final int BUTTON_HEIGHT = PdeEditor.GRID_SIZE; //33;

  static final int NOTHING  = 0;
  static final int RUN      = 1;
  static final int STOP     = 2;

  static final int NEW      = 3;
  static final int OPEN     = 4;
  static final int SAVE     = 5;
  static final int EXPORT   = 6;

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

  //JPopupMenu popup;
  PopupMenu popup;

  int buttonCount;
  int state[];
  Image stateImage[];
  int which[]; // mapping indices to implementation

  //int x1[], x2[];
  //int y1, y2;
  int x1, x2;
  int y1[], y2[];


  public PdeEditorButtons(PdeEditor editor) {
    this.editor = editor;
    buttons = PdeBase.getImage("buttons.gif", this);

    buttonCount = 0;
    which = new int[BUTTON_COUNT];

    which[buttonCount++] = NOTHING;
    which[buttonCount++] = RUN;
    which[buttonCount++] = STOP;
    which[buttonCount++] = NEW;
    which[buttonCount++] = OPEN;
    which[buttonCount++] = SAVE;
    which[buttonCount++] = EXPORT;

    currentRollover = -1;

    setLayout(null);
    status = new Label();
    status.setFont(PdeBase.getFont("editor.buttons.status.font",
				     new Font("SansSerif", Font.PLAIN, 10)));
    status.setForeground(PdeBase.getColor("editor.buttons.status.color",
					    Color.black));
    add(status);

    status.setBounds(-5, BUTTON_COUNT*BUTTON_HEIGHT, 
		     BUTTON_WIDTH + 15, BUTTON_HEIGHT);
    status.setAlignment(Label.CENTER);
  }


  public void update() {
    paint(this.getGraphics());
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics screen) {
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
    // if app loads slowly and cursor is near the buttons 
    // when it comes up, the app may not have time to load
    if ((y1 == null) || (y2 == null)) return -1;

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

    if (currentSelection == OPEN) {
      if (popup == null) {
	//popup = new JPopupMenu();
	popup = new PopupMenu();
	add(popup);
      }
      //popup.addActionListener(this);
      editor.base.rebuildSketchbookMenu(popup);
      popup.show(this, x, y);
    }
    return true;
  }


    /*
  public void actionPerformed(ActionEvent e) {
    System.err.println(e);
    if (e.getSource() == popup) {
      System.err.println("posting bogus mouseup");
      mouseUp(null, 0, 0);
    }
  }
    */


  public boolean mouseUp(Event e, int x, int y) {
    //switch (which[sel]) {
    switch (currentSelection) {

    case RUN: 
      editor.doRun(e.shiftDown());
      //if (e.shiftDown()) {
      //editor.doPresent();
      //} else {
      //editor.doRun(false); 
      //}
      break;

    case STOP: 
      setState(RUN, INACTIVE, true); 
      if (editor.presenting) {
	editor.doClose();
      } else {
	editor.doStop(); 
      }
      break;
      //case CLOSE: editor.doClose(); break;

      //case OPEN:  editor.doOpen(); break;
      /*
      case OPEN:  
	System.err.println("popup mouseup");
      //popup.setVisible(false);
      remove(popup);
      // kill the popup?
      //PopupMenu popup = new PopupMenu();
      //editor.base.rebuildSketchbookMenu(popup);
      //popup.show(this, x, y);
      break;
      */
      //editor.doOpen(this, BUTTON_WIDTH, OPEN * BUTTON_HEIGHT); 

    case NEW: editor.skNew(); break;

      //case SAVE: editor.doSaveAs(); break;
    case SAVE: editor.doSave(); break;
    case EXPORT: editor.skExport(); break;
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
    // skip the run button, do the others
    for (int i = 1; i < buttonCount; i++) {
      //state[i] = INACTIVE;
      //stateImage[i] = inactive[which[i]];
      setState(i, INACTIVE, false);
    }
    update();
  }

  public void run() {
    if (inactive == null) return;
    clear();
    //setState(0, ACTIVE, true);
    setState(RUN, ACTIVE, true);
  }

  public void running(boolean yesno) {
    setState(RUN, yesno ? ACTIVE : INACTIVE, true);
  }

  public void clearRun() {
    if (inactive == null) return;
    //setState(0, INACTIVE, true);
    setState(RUN, INACTIVE, true);
  }

  /*
  public boolean mouseUp(Event e, int x, int y) {
    if (wasDown == -1) return true;
    if (which[wasDown] == RUN) return true;

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


  public Dimension getPreferredSize() {
    return new Dimension(BUTTON_WIDTH, (BUTTON_COUNT + 1)*BUTTON_HEIGHT);
  }
}
