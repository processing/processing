import processing.core.*; public class mandelbrotMobile extends PMIDlet{long scle, xoff, yoff,maxIter,w,h,fixpoint, pointerX,pointerY;
int scanline, maxIterBits, fixbits;
int[] cross;
int[] spectrum;
PFont fontg;
PFont fontw;
boolean rendering;
//------------------------------------------------
public void setup() {
  framerate(99);
  rendering = false;
  fontg = loadFont("verdana.mvlw",color(100,255,100));
  fontw = loadFont("verdana.mvlw",color(200,200,200));
  cross = new int[] {
    -1,-1,0,0,-1,1,1,-1,1,1  };
  fixbits = 8+8+8;
  fixpoint = pow(2,fixbits);
  maxIterBits = 5;
  maxIter = pow(2,maxIterBits);
  scle = fixpoint*3;
  xoff = -fixpoint/2;
  yoff = 0;
  w = width;
  h = height;
  pointerX = w/2;
  pointerY = h/2;
  spectrum = loadSpectrum(0,255,0);
  softkey("");
  background(0);
  scanline = 0;
}
//------------------------------------------------
public void draw(){
  if(rendering) scan();
  else menu();
}
//------------------------------------------------
public void menu(){
  background(0);
  stroke(0,80,0);
  fill(0,40,0);
  rect(-5,14,width+10,30);

  textAlign(LEFT);
  textFont(fontg);
  text("MandelbrotMOBILE",20,30);
  text("by skanaar     v.2.1",30,40);
  textFont(fontw);
  text("[1-3]   set color",20,60);
  text("[4-9]   set bailout level",20,72);
  text("[arrows]   move cursor",20,84);
  text("[click]   magnify at cursor",20,96);
  text("[*]   zoom out",20,108);
  text("[#]   save coordinates",20,120);
  text("[0]   load coordinates",20,132);
  text("press any key to begin",20,156);
}
//------------------------------------------------
public void scan(){
  long re, im, iterations, a, b, c;
  a = scle/w;
  b = w/2;
  stroke(255);
  line(0,scanline+1,(int)w,scanline+1);
  if(scanline<h){ 
    im = yoff + (scanline-h/2)*scle/w;
    for(long x=0 ; x<w ; x++ ){
      re = xoff + (x-b)*a;
      iterations = evalPoint(re,im);
      stroke(spectrum[ (int)( (63*iterations)>>maxIterBits ) ]);
      point((int)x,scanline);
    }
    scanline++;
  }
}
//------------------------------------------------
public int[] loadSpectrum(int r, int g, int b) {
  int[] s = new int[64];
  for( int i=0 ; i<64 ; i++) s[i] = color(i*r/64,i*g/64,i*b/64);
  return s;
}
//------------------------------------------------
public int[] loadSpectrumLockout(int r, int g, int b) {
  int[] s = new int[64];
  for( int i=0 ; i<64 ; i++) s[i] = color(i*r/32,i*g/32,i*b/32);
  for( int i=32 ; i<64 ; i++) s[i] = color((64-i)*r/32,(64-i)*g/32,(64-i)*b/32);
  return s;
}
//------------------------------------------------
public void hideCursor() {
  //removes the previous zoom pointer
  for( int i=0 ; i<5 ; i++){
    long iterations = evalPoint(xoff + (pointerX+cross[i*2]-w/2)*scle/ w,yoff + (pointerY+cross[i*2+1]-h/2)*scle/ w);
    stroke(spectrum[ (int)( (63*iterations)/maxIter ) ]);
    point((int)pointerX+cross[i*2],(int)pointerY+cross[i*2+1]);
  }
}
//------------------------------------------------
public void drawCursor() {
  //draws the zoom pointer
  for( int i=0 ; i<5 ; i++){
    stroke(255,80,80);
    point((int)pointerX+cross[i*2],(int)pointerY+cross[i*2+1]);
  }
}
//---------------------------------------------
public void keyReleased(){

  if(!rendering){
    rendering = true;
  } 
  else {

    hideCursor();

    if(keyCode == LEFT) pointerX  -= w/8;
    if(keyCode == RIGHT) pointerX += w/8;
    if(keyCode == UP) pointerY    -= w/8;
    if(keyCode == DOWN) pointerY  += w/8;
    if(keyCode == FIRE){
      xoff += scle*(pointerX-w/2)/w;
      yoff += scle*(pointerY-h/2)/w;
      scle = scle/2;
      pointerX = w/2;
      pointerY = h/2;
      scanline = 0;
    }
    if(key == '*'){
      scanline = 0;
      scle = scle*2;
      pointerX = w/2;
      pointerY = h/2;
    }
    if(key == '#') saveCoords();
    if(key == '0') readCoords();

    if(key == '1') spectrum = loadSpectrum(0,255,0);
    if(key == '2') spectrum = loadSpectrumLockout(0,160,255);
    if(key == '3') spectrum = loadSpectrumLockout(255,0,255);
    if(key == '4') maxIterBits = 5;
    if(key == '5') maxIterBits = 6;
    if(key == '6') maxIterBits = 7;
    if(key == '7') maxIterBits = 8;
    if(key == '8') maxIterBits = 9;
    if(key == '9') maxIterBits = 10;
    if(key >= '1' && key <= '9') scanline = 0;
    maxIter = pow(2,maxIterBits);

    drawCursor();
  }
}
//---------------------------------------------
public void saveCoords(){
  // saves the current position
  byte[] b = new byte[24];
  for(int i=0;i<8;i++){
    b[i]    = (byte) (xoff >> 8*i);
    b[i+8]  = (byte) (yoff >> 8*i);
    b[i+16] = (byte) (scle >> 8*i);
  }

  saveBytes("mbrotcoords.dat",b);
}
//---------------------------------------------
public void readCoords(){

  scanline = 0;
  byte[] b = loadBytes("mbrotcoords.dat");

  xoff = 0;
  yoff = 0;
  scle = 0;

  long[] l = new long[24];
  for(int i=0;i<24;i++){
    l[i] = (long) b[i];
    if(l[i]<0) l[i] += 256;
  }

  for(int i=0;i<8;i++){
    xoff = xoff | (l[i]   << 8*i);
    yoff = yoff | (l[i+8] << 8*i);
    scle = scle | (l[i+16]<< 8*i);
  }

}
//------------------------------------------------
public long evalPoint(long a, long b) {

  long x=0, y=0, re, iteration = 0, infinity=4*fixpoint*fixpoint;
  while ( x*x + y*y < infinity  &&  iteration < maxIter ) {
    re = ((x*x - y*y) >> fixbits) + a; // re
    y = ((2*x*y) >> fixbits) + b; // im
    x = re;
    iteration++;
  }
  return iteration;
}
}