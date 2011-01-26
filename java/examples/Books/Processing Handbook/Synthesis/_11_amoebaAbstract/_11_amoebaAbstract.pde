/**
 * Synthesis 3: Motion and Arrays
 * AmoebaAbstract_03 by Marius Watz <http://www.unlekker.net>
 * p. 374
 * 
 * Move the mouse to control the speed of the elements. Click to 
 * restart with different colors.
 * 
 * Notes from Marius: 
 * Abstract computational animation for the exhibition "Abstraction Now", 
 * Kunstlerhaus Vienna, 29.08-28.09 2003.
 * You are allowed to play with this code as much as you like, but
 * you may not publish pieces based directly on it.
*/


int num,cnt,colNum,colScheme;
float h,maxSpeed;
float[] x,y,speed;
boolean initialised;
float[] colR,colG,colB;

void setup() {
  size(700, 400);
  background(255);
  smooth();
  frameRate(30);

  cnt=0;
  maxSpeed=8;
  num=20;
  x=new float[num];
  y=new float[num];
  speed=new float[num];
  h=height/num;
  for(int i=0; i<num; i++) {
    x[i]=0;
    y[i]=h*i+2;
    speed[i]=random(maxSpeed/2-1)+1;
    if(random(100)>50) speed[i]=-speed[i];
  }
  noStroke();

  colR=new float[1000];
  colG=new float[1000];
  colB=new float[1000];

  colScheme=-1;
  initColors();
}

void draw() {
  int c;

  cnt++;
  for(int i=0; i<num; i++) {
    x[i] += speed[i];
    if(x[i] < 0) { 
      x[i] += width;
    } else if(x[i] > width) { 
      x[i] -= width;
    }

    c = ((i*11+cnt*3+int(speed[i]*10))/20)%colNum;
    if(c<0) {
      c=0;
    }
    fill(colR[c],colG[c],colB[c],150);

    if(i%2==0) { 
      rect(x[i]%width, y[i], 6, h);
    }
    else { 
      ellipse(x[i]%width, y[i]+h/2, h-6, h-6);
    }
  }

  stroke(255,255,255,10);
  noFill();
  strokeWeight(2);
  for(int i=0; i < num-3; i++) {
    line(x[i], y[i]+h/2, x[(i+3)%num], y[(i+3)%num]+h/2);
    line(x[num-i-1], y[num-i-1]+h/2, x[(num-i-2)%num], y[(num-i-2)%num]+h/2);
  }
  noStroke();
  fill(255);
  rect(0, 0, 2, height);
  rect(width-2, 0, 2, height);
}

void initColors() {
  colNum=0;
  colScheme=(colScheme+1)%3;
  if(colScheme==0) {
    addColor(12, 100,200,255, 50,100,128);
    addColor(8, 26,41,58, 50,100,128);
    addColor(2, 255,255,255, 255,255,255);
    addColor(16, 0,0,50, 40,40,80);
    addColor(12, 100,200,255, 50,100,128);
    addColor(8, 26,41,58, 50,100,128);
    addColor(6, 0,200,20, 0,255,100);
    addColor(2, 255,255,255, 255,255,255);
    addColor(16, 0,0,50, 40,40,80);
    addColor(5, 255,200,0, 255,170,0);
  }
  else if(colScheme==1) {
    addColor(20, 255,0,100, 128,0,0);
    addColor(6, 255,100,0, 255,150,0);
    addColor(6, 128,0,0, 50,20,20);
    addColor(12, 255,255,255, 255,100,100);
    addColor(4, 255,100,0, 102,0,0);
  }
  else if(colScheme==2) {
    addColor(14, 128,163,120, 27,53,38);
    addColor(8, 64,95,0, 225,227,0);
    addColor(8, 0,150,150, 215,255,255);
    addColor(4, 168,106,170, 235,183,237);
    addColor(14, 128,163,120, 27,53,38);
    addColor(8, 64,95,0, 225,227,0);
    addColor(8, 0,150,150, 215,255,255);
    addColor(12, 92,18,96, 217,111,226);
  }
}

void addColor(float r,float g,float b) {
  colR[colNum]=r;
  colG[colNum]=g;
  colB[colNum]=b;
  colNum++;
}

void addColor(int num, float r1,float g1,float b1, float r2,float g2,float b2) {

  r2=(r2-r1)/float(num-1);
  g2=(g2-g1)/float(num-1);
  b2=(b2-b1)/float(num-1);

  for(int i=0; i<num; i++) {
    colR[colNum] = r1+r2*(float)i;
    colG[colNum] = g1+g2*(float)i;
    colB[colNum] = b1+b2*(float)i;
    colNum++;
  }
}

void updateSpeed() {
  int which = mouseY/(height/num);
  speed[which] = sq(((mouseX-width/2.0)/(width/2.0)))*maxSpeed;
  if(speed[which] < 0.2) { 
    speed[which] = 0.2;
  }
  if(mouseX < width/2) { 
    speed[which] *= -1;
  }
}

void mouseDragged() {
  updateSpeed();
}

void mousePressed() {
  background(255);
  initColors();
  updateSpeed();
}

void mouseMoved() {
  updateSpeed();
}


void keyPressed() {
  // saveFrame("amoebaAbstract-####.tif"); 
}

