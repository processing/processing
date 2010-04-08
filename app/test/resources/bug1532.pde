import processing.video.*;
import processing.opengl.*;



final int l=50;
final int hx=15;
final int hy=18;
final int dx=int((l+l*cos(PI/3))*hx);
final int dy=int(l*sin(PI/3)*hy);

boolean cheatScreen=false;
String shoot="shoot/";
final int fr=24;
PImage tile1,tile2;
Capture cam;


flatCube[][] grid;

void setup() {
  try {
    quicktime.QTSession.open();
  } 
  catch (quicktime.QTException qte) { 
    qte.printStackTrace();
  }

  size (dx,dy,OPENGL);
  int d=day();
  int m=month();
  int y=year();
  int h=hour();
  int mp=minute();
  int ms=second();
  
  shoot=shoot+y+m+d+h+mp+ms;
 
  cam = new Capture(this, hx, hy, fr);
  frameRate(fr);
  
 tile1=loadImage("111.png");
 tile2=loadImage("122.png");
 tile3=loadImage("001.png");
  grid= new flatCube[hx][hy];
  for (int j=0; j<hy;j++) {
    for (int i=0; i<hx; i++) {
      
      switch ((i+j) %3 ){
       case 0: grid[i][j]= new flatCube(tile1,0) break;
       case 1: grid[i][j]= new flatCube(tile2,0); break;
       case 2:  grid[i][j]= new flatCube(tile3,0); break;
      }
    }
  }
  
  noStroke();
}

public void captureEvent(Capture c) {
  c.read();
}


void draw() {

  background (0,0,0);
  float x=mouseX;
  float y=mouseY;
  int signx=-1;
  int signy=-1;
  int cx=0;
  int cy=0;
  color c;
  float rot=0;

  for (int j=0; j<hy;j++) {
    pushMatrix();
    for (int i=0; i<hx; i++) {
      c=cam.pixels[i+j*hx];
      pushMatrix();
      if (frameCount % 3 == 0 ) {
       rot=PI/3*int(random(0,7));
       cx=int (random(0,hx));
       cy=int (random(0,hy));
       }
      if ((cx==i)&&(cy==j)) grid[i][j].show(rot,c); else grid[i][j].show(c);
      popMatrix();
      translate (l+l*cos(PI/3),signx*l*sin(PI/3));


      signx=-signx;
    }
    popMatrix();
    signy=-signy;
    translate (signy*(l+l*cos(PI/3)),l*sin(PI/3));
  }


  if (cheatScreen) {
    image(cam, 0, 0);
  }
}


public void keyPressed() {

  switch (key) {
  case 'c': 
    cheatScreen = !cheatScreen; 
    break;
  case 'g': 
    saveFrame(shoot+"######.png"); 
    println ("saved "+l); 
    break;
  case 'm': 
    cam.settings();
    break;

  case  'q':  
    exit();
    break;
  }
}

class flatCube {

  
  PImage tile;
  float rot;
  flatCube (PImage i, float r) {
    tile=i;
    rot=r;
  }

    void show (float r, color c){
      rot=r;
      show(c);
    }
    
void show (color c) {
    pushMatrix();
    translate(l,l*sin(PI/3));
    rotateZ(rot);
    translate(-l,-l*sin(PI/3));
    tint(c);
    image (tile,0,0);    
    popMatrix();
  }

  void show(int l,int a, color c) {
    noStroke();
    pushMatrix();
    fill(c,a);
    quad(0,0, l,0, l+l*cos(PI/3),l*sin(PI/3), l*cos(PI/3),l*sin(PI/3) );
    rotateZ(PI/3);
    fill(c,a);
    quad(0,0, l,0, l+l*cos(PI/3),l*sin(PI/3), l*cos(PI/3),l*sin(PI/3) );
    translate(l+l*cos(PI/3),l*sin(PI/3));
    rotateZ(-TWO_PI/3);
    fill (c,a);
    quad(0,0, l,0, l+l*cos(PI/3),l*sin(PI/3), l*cos(PI/3),l*sin(PI/3) );

    popMatrix();
  }
  
}




