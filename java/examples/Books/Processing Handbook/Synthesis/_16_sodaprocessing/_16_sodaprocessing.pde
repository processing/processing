/**
 * Synthesis 4: Structure and Interface
 * SodaProcessing by Ed Burton (www.soda.co.k)
 * p. 499
 * 
 * A simplified version of the Soda Constructor to demonstrate how it works.
 * The complete Soda Constructor may be visited at http://www.sodaplay.com.
 */

Mass masses[];
int massCount = 0;

float mouseTolerance = 15;

Mass dragMass = null;
float dragDx,dragDy;
float tempDistance;

Mass drawMass = null;
Mass overMass;

Spring springs[];
int springCount = 0;
Spring overSpring;

Control controls[];
Control activeControl = null;

Button make,move,delete;
Slider g,f,k;
int sliderHeight = 13;

float speedFrictionThreshold = 20;

PFont font;

int mode;
static final int MAKE = 0;
static final int MOVE = 1;
static final int DELETE = 2;

String toolTips[];

void setup()
{
  size(600, 600);
  background(0xFF);

  masses = new Mass[8];
  springs = new Spring[8];

  font  =  loadFont("RotisSanSer-Bold.vlw.gz");
  textFont(font, 15);
  //hint(SMOOTH_IMAGES);
  smooth();
  controls = new Control[6];
  int x = 0;
  int i = 0;
  int controlWidth = width/(controls.length)+1;
  controls[i++] = make = new Button(0,0,controlWidth-1,sliderHeight,"make");
  controls[i++] = move = new Button(controls[i-2].x+controls[i-2].w,0,controlWidth-1,sliderHeight,"move");
  controls[i++] = delete = new Button(controls[i-2].x+controls[i-2].w,0,controlWidth-1,sliderHeight,"delete");
  controls[i++] = g = new Slider(controls[i-2].x+controls[i-2].w,0,controlWidth,sliderHeight,0,4,0.0,"g");
  controls[i++] = f = new Slider(controls[i-2].x+controls[i-2].w,0,controlWidth,sliderHeight,0,1,0.1,"f");
  controls[i++] = k = new Slider(controls[i-2].x+controls[i-2].w,0,controlWidth,sliderHeight,0,0.75,0.5,"k");
  make.selected = true;
  checkMode();

  toolTips = new String [] {
    ": click to make masses and springs"
    ,": click, drag and throw masses"
    ,": click to delete masses and springs"
    ,"  =  gravity (hint, set to zero to before choosing to make)"
    ,"  =  friction"
  ,"  =  spring stiffness"};
}

void draw()
{
  doUpdate();
  display();
}

void checkMode() {
  for (int i = 0;i<controls.length;i++)
  if (controls[i] instanceof Button && ((Button)controls[i]).selected)
  mode = i;
  if (mode != MAKE)
  drawMass = null;
}

void keyPressed() {
  // saveFrame(); 
}

void mouseMoved() {
  for (int i = 0;i<controls.length;i++)
  controls[i].mouseIn();

  tempDistance = -1;
  Mass m = null;
  if (mode == MOVE || mode == MAKE || mode == DELETE)
  m = mouseMass();
  float md = tempDistance;
  tempDistance = -1;
  Spring s = null;
  if (mode == DELETE)
  s = mouseSpring();
  float sd = tempDistance;
  if (m != null && md != -1 && (md <= sd || sd == -1) && md < mouseTolerance) {
    overMass = m;
    overSpring = null;
  } else if (s != null && sd != -1 && (sd<md || md == -1) && sd<mouseTolerance) {
    overSpring = s;
    overMass = null;
  } else {
    overMass = null;
    overSpring = null;
  }
}

void mouseDragged() {
  if (activeControl != null)
  activeControl.mouseDragged();
  else
  if (dragMass != null) {
    dragMass.x = mouseX+dragDx;
    dragMass.y = mouseY+dragDy;
    dragMass.xv = mouseX-pmouseX;
    dragMass.yv = mouseY-pmouseY;
    dragMass.clamp();
  }
}

void mouseReleased() {
  if (activeControl != null) {
    if (activeControl.mouseReleased() && activeControl instanceof Button) {
      for (int i = 0;i<controls.length;i++)
      if (controls[i] != activeControl && controls[i] instanceof Button)
      ((Button)controls[i]).selected = false;
      checkMode();
    }
    activeControl = null;
  }
  if (dragMass != null) {
    if (overMass == dragMass)
    overMass = null;
    dragMass = null;
  }
}

void mousePressed() {
  activeControl = null;
  for (int i = 0;i<controls.length && activeControl == null;i++)
  if ( controls[i].mousePressed() && !(controls[i] instanceof Button && ((Button)controls[i]).selected))
  activeControl = controls[i];
  if (activeControl == null) {
    switch(mode) {
      case MAKE:
      Mass m = mouseMass();
      if (m != null && tempDistance<mouseTolerance) {
        if (drawMass != null) {
          if (drawMass != m) {
            boolean springExists = false;
            for (int i = 0;i<springCount&&!springExists;i++)
            springExists  =  ((springs[i].a == drawMass && springs[i].b == m)||(springs[i].b == drawMass && springs[i].a == m));
            if (!springExists)
            addSpring(new Spring(drawMass,m));
          }
          drawMass = null;
        } else {
          drawMass = m;
        }
      } else {
        Mass newMass;
        addMass(newMass = new Mass(mouseX,mouseY));
        if (drawMass != null)
        addSpring(new Spring(drawMass,newMass));
        drawMass = newMass;
      }
      break;
      case MOVE:
      m = mouseMass();
      if (m != null && tempDistance<mouseTolerance) {
        overMass = dragMass = m;
        dragDx = m.x-mouseX;
        dragDy = m.y-mouseY;
      } else
      overMass = dragMass = null;
      break;
      case DELETE:
      if (overMass != null) {
        for (int i = 0;i<springCount;i++)
        if (springs[i].a == overMass || springs[i].b == overMass)
        deleteSpring(springs[i--]);
        deleteMass(overMass);
        if (overMass == dragMass)
        dragMass = null;
        overMass = null;
      } else if (overSpring != null) {
        deleteSpring(overSpring);
        overSpring = null;
      }
      break;
    }
  }
}

Mass mouseMass() {
  tempDistance = -1;
  Mass m = null;
  for (int i = 0;i<massCount;i++) {
    float d = masses[i].distanceTo(mouseX,mouseY);
    if (d != -1 && (d<tempDistance || tempDistance == -1)) {
      tempDistance = d;
      m = masses[i];
    }
  }
  return m;
}

Spring mouseSpring() {
  tempDistance = -1;
  Spring s = null;
  for (int i = 0;i<springCount;i++) {
    float d = springs[i].distanceTo(mouseX,mouseY);
    if (d != -1 && (d<tempDistance || tempDistance == -1)) {
      tempDistance = d;
      s = springs[i];
    }
  }
  return s;
}

void doUpdate() {
  for (int i = 0;i<springCount;i++)
  springs[i].applyForces();
  for (int i = 0;i<massCount;i++)
  if (masses[i] != dragMass)
  masses[i].update();
}

void display() {
  stroke(0x00,0x99,0xFF);
  fill(0xFF,0xFF,0xFF);
  rect(0,0,width-1,height-1);

  for (int i = 0;i<springCount;i++)
  springs[i].display();

  if (drawMass != null) {
    stroke(0x00,0x99,0xFF);
    line(drawMass.x,drawMass.y,mouseX,mouseY);
  }

  for (int i = 0;i<massCount;i++)
  masses[i].display();

  for (int i = 0;i<controls.length;i++) {
   controls[i].display();
  }
  fill(0x00,0x99,0xFF);
  for (int i = 0;i<controls.length;i++)
  if (controls[i].over)
  text(controls[i].label+toolTips[i], 2, sliderHeight*3-3);
}

// list handling for masses

void addMass(Mass mass) {
  if (massCount  ==  masses.length) {
    Mass temp[]  =  new Mass[massCount*2];
    System.arraycopy(masses, 0, temp, 0, massCount);
    masses  =  temp;
  }
  masses[massCount++]  =  mass;
}

void deleteMass(Mass mass) {
  int index = massIndex(mass);
  if (index >= 0)
  deleteMassIndex(index);
}

void deleteMassIndex(int index) {
  if (index<massCount)
  System.arraycopy(masses, index+1, masses, index, massCount-index);
  massCount--;
}

int massIndex(Mass mass) {
  for (int i = 0;i<massCount;i++)
  if (masses[i] == mass)
  return i;
  return -1;
}

// list handling for springs

void addSpring(Spring spring) {
  if (springCount  ==  springs.length) {
    Spring temp[]  =  new Spring[springCount*2];
    System.arraycopy(springs, 0, temp, 0, springCount);
    springs  =  temp;
  }
  springs[springCount++]  =  spring;
}

void deleteSpring(Spring spring) {
  int index = springIndex(spring);
  if (index >= 0)
  deleteSpringIndex(index);
}

void deleteSpringIndex(int index) {
  if (index<springCount)
  System.arraycopy(springs, index+1, springs, index, springCount-index);
  springCount--;
}

int springIndex(Spring spring) {
  for (int i = 0;i<springCount;i++)
  if (springs[i] == spring)
  return i;
  return -1;
}

// end of list handling

