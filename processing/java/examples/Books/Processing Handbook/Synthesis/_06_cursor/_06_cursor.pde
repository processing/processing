/**
 * Synthesis 2: Input and Response
 * Cursor by Peter Cho (www.typotopo.com)
 * p. 257
 *
 * There are four modes, each is a different
 * way to use the input from the mouse to control
 * the cursor(s). Click on a number to select that
 * mode.
 */
 
 
PFont fontA;
int gx, gy;
int mode, nextmode;
int nummodes;
boolean forapplet = false;

float mx, my, lastmx, lastmy;
float lastrot, lastsc;

float bgx, bgy;


float p_x, p_y;
float p_fx, p_fy;
float p_v2, p_vx, p_vy;
float p_a2, p_ax, p_ay;
float p_mass, p_drag;


void setup()
{
  size(600, 600);
  gx = width; 
  gy = height;
  size(gx, gy);
  colorMode(RGB, 1.0);
  strokeWeight(1.3);
  loop();
  smooth();
  strokeJoin(ROUND);
  
  init_particle(0.6, 0.9,  width/2, height/2);
  
  fontA = loadFont("NewsGothicBT-Bold-48.vlw");
  nummodes = 4;
  mode = 1;

  bgx = 0;
  bgy = 0;
  mx = gx/2;
  my = gy/2;

  noCursor();

  if (forapplet) {
    cursor(CROSS);
  }
}


void init_particle(float _mass, float _drag, float ox, float oy) 
{
  p_x = ox;
  p_y = oy;
  p_v2 = 0.0f;
  p_vx = 0.0f;
  p_vy = 0.0f;
  p_a2 = 0.0f;
  p_ax = 0.0f;
  p_ay = 0.0f;
  p_mass = _mass;
  p_drag = _drag;
}

void iterate_particle(float fkx, float fky) 
{
  // iterate for a single force acting on the particle
  p_fx = fkx;
  p_fy = fky;
  p_a2 = p_fx*p_fx + p_fy*p_fy;
  if (p_a2 < 0.0000001) return;
  p_ax = p_fx/p_mass;
  p_ay = p_fy/p_mass;
  p_vx += p_ax;
  p_vy += p_ay;
  p_v2 = p_vx*p_vx + p_vy*p_vy;
  if (p_v2 < 0.0000001) return;
  p_vx *= (1.0 - p_drag);
  p_vy *= (1.0 - p_drag);
  p_x += p_vx;
  p_y += p_vy;
}

void drawCursor(float x, float y, float myscale, float myrot) {
  // draw generic arrow cursor
  if (forapplet) y -= gy/2;
  pushMatrix();
  translate(x, y);
  rotate(myrot);
  scale(myscale, myscale);
  beginShape(POLYGON);
  vertex(7, 21);
  vertex(4, 13);
  vertex(1, 16);
  vertex(0, 16);
  vertex(0, 0); // tip of cursor shape
  vertex(1, 0);
  vertex(12, 11);
  vertex(12, 12);
  vertex(7, 12);
  vertex(10, 20);
  vertex(9, 21);
  vertex(7, 21);
  endShape();  
  popMatrix();
}

void blurdot(float x, float y, float sc) {
  ellipse(x, y, sc*5, sc*5);
  ellipse(x, y, sc*23, sc*23);
  ellipse(x, y, sc*57, sc*57);
  ellipse(x, y, sc*93, sc*93);
}

void drawBlurCursor(float x, float y, float myscale, float dotval) {
  // draw generic arrow cursor
  if (forapplet) y -= gy/2;
  pushMatrix();
  translate(x, y);
  scale(myscale, myscale);
  float dotval2 = .5 + (1-dotval)*5;
  dotval = .5 + (1-dotval)*9;
  blurdot(7, 21, dotval2);
  blurdot(1, 16, dotval);
  blurdot(0, 8, dotval2); // midpt
  blurdot(0, 0, dotval); // tip of cursor shape
  blurdot(1, 0, dotval2);
  blurdot(6, 6, dotval); // midpt
  blurdot(12, 12, dotval2);
  blurdot(10, 20, dotval);
  popMatrix();
}


int n = 1;  
void keyPressed() {
  if (key == '1') {
    mode = 1;
  } else if (key == '2') {
    mode = 2;
  } else if (key == '3') {
    mode = 3;
  } else if (key == '4') {
    mode = 4;
  } 
  
  // saveFrame("cursor--" + mode + "-" + n + ".tif"); 
  n++;
}

boolean isInside(float x, float y, float rx, float ry, float rw, float rh) {
  return (x >= rx && x <= rx+rw && y >= ry && y <= ry+rh);  
}

void scrollbg(float x, float y) {
  // scroll the bg
  float amt = 30;
  if (x < gx*.5) {
    bgx += amt*(gx*.5 - x)*(gx*.5 - x)/(gx*gx);
  } else if (x > gx - gx*.5) {
    bgx -= amt*(x - (gx-gx*.5))*(x - (gx-gx*.5))/(gx*gx);
  }
  bgx = bgx % gx;
  if (bgx < 0) bgx += gx;

  if (y < gy*.5) {
    bgy += amt*(gy*.5 - y)*(gy*.5 - y)/(gy*gy);
  } else if (y > gy - gy*.5) {
    bgy -= amt*(y - (gy-gy*.5))*(y - (gy-gy*.5))/(gy*gy);
  }
  bgy = bgy % gy;
  if (bgy < 0) bgy += gy;
}

void draw()
{
  lastmx = mx;
  lastmy = my;
  
  mx = mouseX;
  my = mouseY;
  
  if (mode == 3) {
    mx = mx*.01 + lastmx*.99;
    my = my*.01 + lastmy*.99;
    
  } else if (mode == 4) {
    mx = mx*.25 + lastmx*.75;
    my = my*.25 + lastmy*.75;

  } else {
    mx = mx*.5 + lastmx*.5;
    my = my*.5 + lastmy*.5;
  }
  
  iterate_particle(.15*(-p_x+mx), .15*(-p_y+my));
  scrollbg(p_x, p_y);

  background(.8,.8,.8);
  // Set the font and its size (in units of pixels)
  textFont(fontA, 24);

  float x, y;
  int w=95, h=75;
  for (int i=0; i<nummodes; i++) {
    x = bgx + 15+100*i;
    y = bgy + 55;
    noFill();
    stroke(1,1,1);//.6, .6, .6);
    if (i+1 != mode) {
      if (isInside(mx, my, x-4, y-32, w, h) ||
        isInside(mx, my, x-4 - gx, y-32, w, h) ||
        isInside(mx, my, x-4, y-32 - gy, w, h) ||
        isInside(mx, my, x-4 - gx, y-32 - gy, w, h)) {
        fill(.7, .1, 0);
        if (mousePressed) {
          mode = i+1;
          println("chose "+mode);
          mousePressed = false;
        }
      }
      rect(x-4, y-32, w, h);
      rect(x-4 - gx, y-32, w, h);
      rect(x-4, y-32 - gy, w, h);
      rect(x-4 - gx, y-32 - gy, w, h);
    }
    fill(.6, .6, .6);
    fill(1,1,1);
    y += 34;
    x += 2;
    text(""+(i+1), x, y);
    text(""+(i+1), x - gx, y);
    text(""+(i+1), x, y - gy);
    text(""+(i+1), x - gx, y - gy);
  }

  if (mode == 1) {
    fill(1,1,1);
    stroke(.2,.2,.2);

    drawCursor(mx, my, 1, 0);
    
  } else if (mode == 2) {
    // scaling/rotating cursor
    fill(1,1,1);
    stroke(.2,.2,.2);

    float rot = atan2(my-lastmy, mx-lastmx);
    float sc = 1 + .06*abs(lastmx-mx) + .06*abs(lastmy-my);
    drawCursor(mx, my, sc, rot*.5 + lastrot*.5);
    lastrot = rot*.5 + lastrot*.5;
    lastsc = sc;
    
  } else if (mode == 3) {
    // slow poke / blur
    fill(1,1,1, .07);
    
    float sc = 1 - .04*abs(lastmx-mx) - .03*abs(lastmy-my);
    if (sc < 0.01) sc = .01;
    
    noStroke();
    
    drawBlurCursor(mx, my, sc*.15 + lastsc*.85, sc);
    
    noFill();
    stroke(.2,.2,.2, .35);
    drawCursor(mx, my, sc*.15 + lastsc*.85, 0);
    
    lastsc = sc;
    
  } else if (mode == 4) {
    // grid
    fill(1,1,1);
    stroke(.2,.2,.2);

    float rot;
    for (int i=16; i<gx+2; i+=62) {
      for (int j=11; j<gy; j+=60) {
        rot = -PI/4. + atan2(j-my, i-mx);
        drawCursor(i, j, 1, rot);
      }
    }  
  }
  
  
  if (forapplet) {
    fill(0, 0, 0);
    rect(0, gy/2, gx, gy/2);
  }
}


