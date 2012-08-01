/**
 * Synthesis 2: Input and Response
 * Banded Clock by Golan Levin (www.flong.com)
 * p. 259
 *  
 *
 * 
 */
 
 
//================================================
int prevX = 0;
int prevY = 0;	
int clickX = 0;
int clickY = 0;

final int NCOLORS = 256;
color colorArray[];
float S, M, H;
int Scolors[];
int Mcolors[];
int Hcolors[];
int ys0, ys1;
int ym0, ym1;
int yh0, yh1;

float Soffset = 0;
float Hoffset = 0;
float Moffset = 0;
float Svel = 0;
float Hvel = 0;
float Mvel = 0;
float damp = 0.94f;

int mil, sec, minut, hou;
int milError = 0;
int canvasWidth;
int canvasHeight;

//================================================
void setup(){

  size(600, 600); 
  canvasWidth  = width;
  canvasHeight = height;

  Scolors = new int[canvasWidth];
  Mcolors = new int[canvasWidth];
  Hcolors = new int[canvasWidth];
  colorArray = new color[NCOLORS];
  for (int i=0; i<NCOLORS; i++){
    colorArray[i] = color(i,i,i);
  }

  ys0 = 0;     
  ys1 = canvasHeight/3;
  ym0 = ys1+1; 
  ym1 = canvasHeight*2/3;
  yh0 = ym1+1; 
  yh1 = canvasHeight;
}


void draw(){
  updateClock();
  drawClock();
}




//--------------------------------------------------------------------------
void updateClock(){
  
  findMillisError();
  
  mil   = (millis()-milError)%1000;
  sec   = second();
  minut = minute();
  hou   = hour()%12;

  S = (float)(sec *1000.0 + mil)/1000.0;
  M = (minut*60.0 + S)/60.0;
  H = (hou  *60.0 + M)/60.0;
  
  Soffset += Svel; 
  Svel *= damp;
  Moffset += Mvel; 
  Mvel *= damp;
  Hoffset += Hvel; 
  Hvel *= damp;

  float p;
  float ps, pm, ph;
  for (int i=0; i<canvasWidth; i++){
    p  = (float)i/(float)canvasWidth;
    ps = p*S + Soffset;
    pm = p*M + Moffset;
    ph = p*H + Hoffset;
    Scolors[i] = wave(GMOD(ps, 1.0f));
    Mcolors[i] = wave(GMOD(pm, 1.0f));
    Hcolors[i] = wave(GMOD(ph, 1.0f));
  }
}


//--------------------------------------------------------------------------
void drawClock(){
  for (int x=0; x<canvasWidth; x++){
    stroke(colorArray[Scolors[x]]);
    line (x, ys0, x, ys1);
    stroke(colorArray[Mcolors[x]]);
    line (x, ym0, x, ym1);
    stroke(colorArray[Hcolors[x]]);
    line (x, yh0, x, yh1);
  }
}

//--------------------------------------------------------------------------
// Utilities

void findMillisError(){
   int sec2 = second();
   if (sec2 > sec){
     milError = millis()%1000;
   }
}

//------------------
float GMOD (float A, float B){ 
  return (float)(A - (floor(A/B)*B)); 
}

//------------------
int wave(float a){
  // inexpensive ramp function, 
  // but not as nice as true sine wave (below)
  int val = 0;
  float cscale = 2.0f*255.0f;
  if (a < 0.5){ 
    val = (int) round (a *cscale); 
  }
  else {        
    val = (int) round ((1.0f-a)*cscale); 
  }
  return val;
}

//------------------
int sinWave (float a){
  // expensive trigonometric function, but nicer looking
  float sina = (1.0+sin(TWO_PI*a))*255.0/2.0;
  int val = (int)round(sina);
  return val;
}

//--------------------------------------------------------------------------
// interaction methods

void mousePressed (){ 
  prevX  = mouseX; 
  prevY  = mouseY;
  clickX = mouseX;
  clickY = mouseY;
}

void mouseDragged() {
  // allow bands to be shifted around, for "fun"
  float accel = (prevX - mouseX)*0.004f;
  if  ((clickY >= ys0) && (clickY < ys1)){ 
    Svel += accel; 
  }
  else if ((clickY >= ym0) && (clickY < ym1)){ 
    Mvel += accel; 
  }
  else if ((clickY >= yh0) && (clickY < yh1)){ 
    Hvel += accel; 
  }
  prevX  = mouseX; 
  prevY  = mouseY;
}




void keyPressed() {
  saveFrame("clock--" + hour() + "-" + minute() + "-" +  second() +  ".tif"); 
}


