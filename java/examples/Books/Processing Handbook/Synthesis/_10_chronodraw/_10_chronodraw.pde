/**
 * Synthesis 3: Motion and Arrays
 * Chronodraw by Andreas Gysin (www.ertdfgcvb.ch)
 * p. 373
 */


int hquads       = 23;
int vquads       = 1;
int quadwidth    = 38;
int quadheight   = 250;
int buflength    = 200;
int framestep    = 4;
int framerest    = 2;
boolean fat      = false;
boolean grid     = true;
int btnresomulti = 2;
int btnreso      = 23;
int btnheight;
int framecounter = 0;
PImage[] bitlist = new PImage[buflength];
PImage blank     = new PImage();
int currentpic   = buflength - 1;


void setup() {
  size(quadwidth * hquads, quadheight * vquads);
  blank.width  = quadwidth;
  blank.height = quadheight;
  blank.pixels = new color[quadwidth*quadheight];
  for (int j=0; j<blank.pixels.length; j++){
    blank.pixels[j] = color(0,0,0);
  }
  clearall();
}

void draw() {
  // drawpixels 
  if (mousePressed) {
    if ((mouseX >= 0) && (mouseX < width) && (mouseY >= 0) && (mouseY < height)){
      drawline(mouseX, mouseY, pmouseX, pmouseY);
      if (fat) {
        drawthickline(mouseX, mouseY, pmouseX, pmouseY, 2);
        drawdot(mouseX+1, mouseY);
        drawdot(mouseX,   mouseY+1);
        drawdot(mouseX,   mouseY-1);
        drawdot(mouseX-1, mouseY);
        drawdot(mouseX+1, mouseY-1);
        drawdot(mouseX+1, mouseY+1);
        drawdot(mouseX-1, mouseY-1);
        drawdot(mouseX-1, mouseY+1);
      }
    }
  }

  // draw frames
  int p = currentpic;
  for (int j=0; j<vquads; j++){
    for (int i=0; i<hquads; i++){
      set(i*quadwidth, j*quadheight, bitlist[p]);
      p+= framestep;
      p = p%buflength;
      if (p<0) { 
        p = buflength + p;
      }
    }
  }

  //  increment
  framecounter++;
  if (framecounter >= framerest) {
    framecounter = 0;
    currentpic--;
    if (currentpic < 0) { 
      currentpic = buflength - 1;
    }
  }

  // btncheck
  if (keyPressed && key==' ') {
    btnheight = height / 2;
    if (mouseY < btnheight && mouseY >=0){
      framestep = - int(btnreso / 2) + int(mouseX/(width/btnreso));
      //println(framestep);
    } else if (mouseY > btnheight && mouseY < height){
      framerest = int(mouseX/(width/btnreso))*btnresomulti;
    }
  } else {
    btnheight = 8;
  }

  // drawgrid
  if (grid) { 
    drawgrid();
  }

  // drawbtn1
  noStroke();
  fill(0,0,255,60);
  int btnxoffset = width/btnreso * (btnreso / 2 + framestep);
  rect(btnxoffset, 0, width/btnreso, btnheight);

  // drawbtn2
  fill(255,0,0,60);
  btnxoffset = width/btnreso * framerest / btnresomulti;
  rect(btnxoffset, height - btnheight, width/btnreso, btnheight);
}

void clearall(){
  for (int i=0; i<bitlist.length; i++){
    bitlist[i] = blank.get();
  }
}


void keyPressed(){
  if (key == 'x' || key == 'X') {
    clearall();
  }
  else if (key == 'f' || key == 'F') {
    fat = !fat;
  }

}

void drawgrid() {
  stroke(255, 255, 255, 40);

  for (int i=0; i<vquads; i++){
    int l = i*quadheight;
    line(0, l, width, l);
  }
  line(0, height-1, width, height-1); // last line

  for (int i=0; i<hquads; i++){
    int l = i*quadwidth;
    line(l, 0, l, height);
  }
  line(width-1, 0, width-1, height); // last line
}


void drawdot(int qx, int qy){
  int offsx = qx%quadwidth;
  int offsy = qy%quadheight;
  int mx = qx / quadwidth;
  int my = qy / quadheight;
  int offsp = mx + my * hquads;
  int cp = (currentpic + offsp * framestep) % buflength;
  if (cp < 0) cp = buflength + cp;
  bitlist[cp].set(offsx, offsy, color(255,255,255));
}


//Bresenham's algorithm
void drawline(int x1, int y1, int x2, int y2)
{
  int sizex, sizey, incx, incy;
  int countx, county, x, y;
  sizex=x2-x1;
  sizey=y2-y1;

  if(sizex<0) {
    sizex=-sizex;
    incx=-1;
  } else {
    incx=1;
  }

  if(sizey<0) {
    sizey=-sizey;
    incy=-1;
  }else {
    incy = 1;
  }
  countx=x1;
  county=y1;

  drawdot(x1, y1);
  if (sizex>=sizey) {
    y=sizex>>1;
    for(int i=0;i<sizex;i++) {
      y+=sizey;
      if (y>=sizex) {
        y-=sizex;
        county+=incy;
      }
      countx+=incx;
      drawdot(countx, county);
    }
  } else {
    x=sizey>>1;
    for(int i=0;i<sizey;i++) {
      x+=sizex;
      if (x>=sizey) {
        x-=sizey;
        countx+=incx;
      }
      county+=incy;
      drawdot(countx, county);
    }
  }
}


//idem
void drawthickline(int x1, int y1, int x2, int y2, int thickness) {

  int dX = x2 - x1;
  int dY = y2 - y1;

  double lineLength = Math.sqrt(dX * dX + dY * dY);
  double scale = (double)(thickness) / (2 * lineLength);
  double ddx = -scale * (double)dY;
  double ddy = scale * (double)dX;
  ddx += (ddx > 0) ? 0.5 : -0.5;
  ddy += (ddy > 0) ? 0.5 : -0.5;
  int dx = (int)ddx;
  int dy = (int)ddy;

  int xPoints[] = new int[4];
  int yPoints[] = new int[4];

  xPoints[0] = x1 + dx; yPoints[0] = y1 + dy;
  xPoints[1] = x1 - dx; yPoints[1] = y1 - dy;
  xPoints[2] = x2 - dx; yPoints[2] = y2 - dy;
  xPoints[3] = x2 + dx; yPoints[3] = y2 + dy;

  drawline(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
  drawline(xPoints[1], yPoints[1], xPoints[2], yPoints[2]);
  drawline(xPoints[2], yPoints[2], xPoints[3], yPoints[3]);
  drawline(xPoints[3], yPoints[3], xPoints[0], yPoints[0]);
}



