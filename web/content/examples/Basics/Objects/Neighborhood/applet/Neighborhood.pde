/**
 * Neighborhood (OOP Example)
 * By Ira Greenberg 
 * 
 * Draw a neighborhood of houses using
 * Door, Window, Roof and House classes.
 * Good example of class composition, with component
 * Door, Window, Roof class references encapsulated
 * within House class. This arrangement allows 
 * House class to handle placement and sizing of 
 * its components, while still allowing user
 * customization of the individual components.
 */
 
void setup(){
  size(200, 200);
  background(190);
  smooth();
  // Ground plane
  int groundHeight = 10;
  fill(0);
  rect(0, height-groundHeight, width, groundHeight);
  fill(255);
  
  // Center the houses
  translate(12, 0);

  // Houses
  Door door1 = new Door(20, 40);
  Window window1 = new Window(50, 62, false, Window.DOUBLE);
  Roof roof1 = new Roof(Roof.DOME);
  House house1 = new House(75, 75, door1, window1, roof1, House.MIDDLE_DOOR);
  house1.drawHouse(0, height-groundHeight-house1.h, true);

  Door door2 = new Door(20, 40);
  Window window2 = new Window(50, 62, true, Window.QUAD);
  Roof roof2 = new Roof(Roof.GAMBREL);
  House house2 = new House(100, 60, door2, window2, roof2, House.LEFT_DOOR);
  house2.drawHouse(house1.x + house1.w, height-groundHeight-house2.h, true);
}

class Door{
  //door properties
  int x;
  int y;
  int w;
  int h;

  // for knob
  int knobLoc = 1;
  //constants
  final static int RT = 0;
  final static int LFT = 1;

  // constructor
  Door(int w, int h){
    this.w = w;
    this.h = h;
  }

  // draw the door
  void drawDoor(int x, int y) {
    rect(x, y, w, h);
    int knobsize = w/10;
    if (knobLoc == 0){
      //right side
      ellipse(x+w-knobsize, y+h/2, knobsize, knobsize);
    }
    else {
      //left side
      ellipse(x+knobsize, y+h/2, knobsize, knobsize);
    }
  }

  // set knob position
  void setKnob(int knobLoc){
    this. knobLoc = knobLoc;
  }
}

class Window{
  //window properties
  int x;
  int y;
  int w;
  int h;

  // customized features
  boolean hasSash = false;

  // single, double, quad pane
  int style = 0;
  //constants
  final static int SINGLE = 0;
  final static int DOUBLE = 1;
  final static int QUAD = 2;

  // constructor 1
  Window(int w, int h){
    this.w = w;
    this.h = h;
  }
  // constructor 2
  Window(int w, int h, int style){
    this.w = w;
    this.h = h;
    this.style = style;
  }
  // constructor 3
  Window(int w, int h, boolean hasSash, int style){
    this.w = w;
    this.h = h;
    this.hasSash = hasSash;
    this.style = style;
  }

  // draw the window
  void drawWindow(int x, int y) {
    //local variables
    int margin = 0;
    int winHt = 0;
    int winWdth = 0;

    if (hasSash){
      margin = w/15;
    }

    switch(style){
    case 0:
      //outer window (sash)
      rect(x, y, w, h);
      //inner window
      rect(x+margin, y+margin, w-margin*2, h-margin*2);
      break;
    case 1:
      winHt = (h-margin*3)/2;
      //outer window (sash)
      rect(x, y, w, h);
      //inner window (top)
      rect(x+margin, y+margin, w-margin*2, winHt);
      //inner windows (bottom)
      rect(x+margin, y+winHt+margin*2, w-margin*2, winHt);
      break;
    case 2:
      winWdth = (w-margin*3)/2;
      winHt = (h-margin*3)/2;
      //outer window (sash)
      rect(x, y, w, h);
      //inner window (top-left)
      rect(x+margin, y+margin, winWdth, winHt);
      //inner window (top-right)
      rect(x+winWdth+margin*2,  y+margin, winWdth, winHt);
      //inner windows (bottom-left)
      rect(x+margin, y+winHt+margin*2, winWdth, winHt);
      //inner windows (bottom-right)
      rect(x+winWdth+margin*2,  y+winHt+margin*2, winWdth, winHt);
      break;
    }
  }

  // set window style (number of panes)
  void setStyle(int style){
    this.style = style;
  }
}

class Roof{
  //roof properties
  int x;
  int y;
  int w;
  int h;

  // roof style
  int style = 0;
  //constants  
  final static int CATHEDRAL = 0;
  final static int GAMBREL = 1;
  final static int DOME = 2;

  // default constructor
  Roof(){
  }
   
   // constructor 2
   Roof(int style){
    this.style = style;
  }

  // draw the roof
  void drawRoof(int x, int y, int w, int h) {
    switch(style){
    case 0:
      beginShape();
      vertex(x, y);
      vertex(x+w/2, y-h/3);
      vertex(x+w, y);
      endShape(CLOSE);
      break;
    case 1:
     beginShape();
      vertex(x, y);
      vertex(x+w/7, y-h/4);
      vertex(x+w/2, y-h/2);
      vertex(x+(w-w/7), y-h/4);
      vertex(x+w, y);
      endShape(CLOSE);
      break;
    case 2:
      ellipseMode(CORNER);
      arc(x, y-h/2, w, h, PI, TWO_PI);
      line(x, y, x+w, y);
      break;
    }

  }

  // set roof style
  void setStyle(int style){
    this.style = style;
  }
}

class House{
  //house properties
  int x;
  int y;
  int w;
  int h;

  //component reference variables
  Door door;
  Window window;
  Roof roof;

  //optional autosize variable
  boolean AutoSizeComponents = false;

  //door placement
  int doorLoc = 0;
  //constants
  final static int MIDDLE_DOOR = 0;
  final static int LEFT_DOOR = 1;
  final static int RIGHT_DOOR = 2;

  //constructor
  House(int w, int h, Door door, Window window, Roof roof, int doorLoc) {
    this.w = w;
    this.h = h;
    this.door = door;
    this.window = window;
    this.roof = roof;
    this.doorLoc = doorLoc;
  }

  void drawHouse(int x, int y, boolean AutoSizeComponents) {
    this.x = x;
    this.y =y;
    this.AutoSizeComponents = AutoSizeComponents;

    //automatically sizes doors and windows
    if(AutoSizeComponents){
      //autosize door
      door.h = h/4;
      door.w = door.h/2;

      //autosize windows
      window.h = h/3;
      window.w = window.h/2;

    }
    // draw bldg block
    rect(x, y, w, h);

    // draw door
    switch(doorLoc){
    case 0:
      door.drawDoor(x+w/2-door.w/2, y+h-door.h);
      break;
    case 1:
      door.drawDoor(x+w/8, y+h-door.h);
      break;
    case 2:
      door.drawDoor(x+w-w/8-door.w,  y+h-door.h);
      break;
    }

    // draw windows
    int windowMargin = (w-window.w*2)/3;
    window.drawWindow(x+windowMargin, y+h/6);
    window.drawWindow(x+windowMargin*2+window.w, y+h/6);

    // draw roof
    roof.drawRoof(x, y, w, h);
  }

  // catch drawHouse method without boolean argument
  void drawHouse(int x, int y){
    // recall with required 3rd argument
    drawHouse(x, y, false);
  }
}

