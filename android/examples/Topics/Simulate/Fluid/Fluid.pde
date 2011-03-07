/**
 * Fluid 
 * by Glen Murphy. 
 * 
 * Click and drag the mouse to move the simulated fluid.
 * Adjust the "res" variable below to change resolution.
 * Code has not been optimised, and will run fairly slowly.
 */
 
int res = 2;
int penSize = 30;
int lwidth;
int lheight;
int pnum = 30000;
vsquare[][] v;
vbuffer[][] vbuf;
particle[] p = new particle[pnum];
int pcount = 0;
int mouseXvel = 0;
int mouseYvel = 0;

void setup() 
{
  size(200, 200);
  noStroke();
  frameRate(30);
  lwidth = width/res;
  lheight = height/res;
  v = new vsquare[lwidth+1][lheight+1];
  vbuf = new vbuffer[lwidth+1][lheight+1];
  for (int i = 0; i < pnum; i++) {
    p[i] = new particle(random(res,width-res),random(res,height-res));
  }
  for (int i = 0; i <= lwidth; i++) {
    for (int u = 0; u <= lheight; u++) {
      v[i][u] = new vsquare(i*res,u*res);
      vbuf[i][u] = new vbuffer(i*res,u*res);
    }
  }
}

void draw() 
{
  background(#666666);
  
  int axvel = mouseX-pmouseX;
  int ayvel = mouseY-pmouseY;

  mouseXvel = (axvel != mouseXvel) ? axvel : 0;
  mouseYvel = (ayvel != mouseYvel) ? ayvel : 0;

  for (int i = 0; i < lwidth; i++) {
    for (int u = 0; u < lheight; u++) {
      vbuf[i][u].updatebuf(i,u);
      v[i][u].col = 32;
    }
  }
  for (int i = 0; i < pnum-1; i++) {
    p[i].updatepos();
  }
  for (int i = 0; i < lwidth; i++) {
    for (int u = 0; u < lheight; u++) {
      v[i][u].addbuffer(i, u);
      v[i][u].updatevels(mouseXvel, mouseYvel);
      v[i][u].display(i, u);
    }
  }
}

class particle {
  float x;
  float y;
  float xvel;
  float yvel;
  int pos;
  particle(float xIn, float yIn) {
    x = xIn;
    y = yIn;
  }

  void updatepos() {
    float col1;
    if (x > 0 && x < width && y > 0 && y < height) {
      int vi = (int)(x/res);
      int vu = (int)(y/res);
      vsquare o = v[vi][vu];    
      
      float ax = (x%res)/res;
      float ay = (y%res)/res;
      
      xvel += (1-ax)*v[vi][vu].xvel*0.05;
      yvel += (1-ay)*v[vi][vu].yvel*0.05;
      
      xvel += ax*v[vi+1][vu].xvel*0.05;
      yvel += ax*v[vi+1][vu].yvel*0.05;
      
      xvel += ay*v[vi][vu+1].xvel*0.05;
      yvel += ay*v[vi][vu+1].yvel*0.05;

      o.col += 4;
      
      x += xvel;
      y += yvel;
    }
    else {
      x = random(0,width);
      y = random(0,height);
      xvel = 0;
      yvel = 0;
    }

    xvel *= 0.5;
    yvel *= 0.5;
  }
}

class vbuffer {
  int x;
  int y;
  float xvel;
  float yvel;
  float pressurex = 0;
  float pressurey = 0;
  float pressure = 0;

  vbuffer(int xIn,int yIn) {
    x = xIn;
    y = yIn;
    pressurex = 0;
    pressurey = 0;
    }

  void updatebuf(int i, int u) {
    if (i>0 && i<lwidth && u>0 && u<lheight) {
      pressurex = (v[i-1][u-1].xvel*0.5 + v[i-1][u].xvel + v[i-1][u+1].xvel*0.5 - v[i+1][u-1].xvel*0.5 - v[i+1][u].xvel - v[i+1][u+1].xvel*0.5);
      pressurey = (v[i-1][u-1].yvel*0.5 + v[i][u-1].yvel + v[i+1][u-1].yvel*0.5 - v[i-1][u+1].yvel*0.5 - v[i][u+1].yvel - v[i+1][u+1].yvel*0.5);
      pressure = (pressurex + pressurey)*0.25;
      }
    }
  }

class vsquare {
  int x;
  int y;
  float xvel;
  float yvel;
  float col;

  vsquare(int xIn,int yIn) {
    x = xIn;
    y = yIn;
    }

  void addbuffer(int i, int u) {
    if (i>0 && i<lwidth && u>0 && u<lheight) {
      xvel += (vbuf[i-1][u-1].pressure*0.5
              +vbuf[i-1][u].pressure
              +vbuf[i-1][u+1].pressure*0.5
              -vbuf[i+1][u-1].pressure*0.5
              -vbuf[i+1][u].pressure
              -vbuf[i+1][u+1].pressure*0.5
              )*0.25;
      yvel += (vbuf[i-1][u-1].pressure*0.5
              +vbuf[i][u-1].pressure
              +vbuf[i+1][u-1].pressure*0.5
              -vbuf[i-1][u+1].pressure*0.5
              -vbuf[i][u+1].pressure
              -vbuf[i+1][u+1].pressure*0.5
              )*0.25;
      }
    }

  void updatevels(int mvelX, int mvelY) {
    if (mousePressed) {
      float adj = x - mouseX;
      float opp = y - mouseY;
      float dist = sqrt(opp*opp + adj*adj);
      if (dist < penSize) {
        if (dist < 4) dist = penSize;
        float mod = penSize/dist;
        xvel += mvelX*mod;
        yvel += mvelY*mod;
        }
      }

    xvel *= 0.99;
    yvel *= 0.99;
  }
  
  void display(int i, int u) {
    float tcol = 0;
    if (col > 255) col = 255;
    if (i>0 && i<lwidth-1 && u>0 && u<lheight-1) {
      tcol = (+ v[i][u+1].col 
              + v[i+1][u].col 
              + v[i+1][u+1].col*0.5
              )*0.4;
      tcol = (int)(tcol+col*0.5);
      }
    else {
      tcol = (int)col;
      }
    fill(tcol, tcol, tcol);
    rect(x,y,res,res);
  }
}


