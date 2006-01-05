// Typing (Excerpt from the piece Textension)
// by Josh Nimoy <http://www.jtnimoy.com>

// Click in the window to give it focus.
// Type to add letters and press backspace to delete.

// Created 27 January 2003

PFont f;
int leftmargin = 10;
int rightmargin = 20;
String buff = "";
boolean didntTypeYet = true;

void setup()
{
  size(200, 200, P3D);
  
  // Load the font. Fonts are located within the 
  // main Processing directory/folder and they
  // must be placed within the data directory
  // of your sketch for them to load
  f = loadFont("Univers45.vlw.gz");
  textFont(f, 25);
}

void draw()
{
  background(176);
  
  if(millis()%500<250){  // Only fill cursor half the time
    noFill();
  }else{
    fill(255);
    stroke(0);
  }
  float rPos;
  // Store the cursor rectangle's position
  rPos = textWidth(buff)+leftmargin;
  rect(rPos+1, 19, 10, 21);

  // Some instructions at first
  if(didntTypeYet){
    fill(0);
    //text("Use the keyboard.", 22, 40);
  }
  
  fill(0);
  pushMatrix();
  translate(rPos,10+25);
  char k;
  for(int i=0;i<buff.length();i++){
    k = buff.charAt(i);
    translate(-textWidth(k),0);
    rotateY(-textWidth(k)/70.0); 
    rotateX(textWidth(k)/70.0);
    scale(1.1);
    text(k,0,0);
  }
  popMatrix();
}

void keyPressed()
{
  char k;
  k = (char)key;
  switch(k){
    case 8:
    if(buff.length()>0){
      buff = buff.substring(1);
    }
    break;
    case 13:  // Avoid special keys
    case 10:
    case 65535:
    case 127:
    case 27:
    break;
    default:
    if(textWidth(buff+k)+leftmargin < width-rightmargin){
      didntTypeYet = false;
      buff=k+buff;
    }
    break;
  }
}
