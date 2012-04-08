/**
 * Synthesis 2: Input and Response
 * Typing by Casey Reas (www.processing.org)
 * p. 258
 *
 * Click and drag the mouse to set the size and
 * and angle of the current line of text. Type to change the
 * text. Hit "delete" or "backspace" to remove letters from the 
 * end of the line. Hit "return" to switch to the next line of text.
 */
 
 
String text1 = "Flatland by Edwin A. Abbott";
String text2 = "1884";
String text3 = "I call our world Flatland, not because we call it so,";
String text4 = "but to make its nature clearer to you, my happy readers,";
String text5 = "who are privileged to live in Space.";
String s = "";
int x, y;
int mx, my;

int mode = 1;

float s1 = 1.0;
float r1 = 0;
float x1 = 0;
float y1 = 0;

float s2 = 1.0;
float r2 = 0;
float x2 = 0;
float y2 = 0;

float s3 = 1.0;
float r3 = 0;
float x3 = 0;
float y3 = 0;

float s4 = 1.0;
float r4 = 0;
float x4 = 0;
float y4 = 0;

float s5 = 1.0;
float r5 = 0;
float x5 = 0;
float y5 = 0;


PFont font;
int whichString = 0;

void setup() {
  size(600, 300);
  smooth();
  
  //String[] fontList = PFont.list();
  //println(fontList);
  
  font = createFont("Serif", 128);
  textFont(font, 18);
  stroke(0);
  fill(0, 200);
}

void draw() {
  background(255);
  
  stroke(204);
  line(mouseX, 0, mouseX, height);
  line(0, mouseY, width, mouseY);
  stroke(0);
  line(mx, 0, mx, height);
  line(0, my, width, my);
  
  pushMatrix();
  translate(x1, y1);
  scale(s1);
  rotate(r1);
  text(text1, 0, 0);
  popMatrix();

  pushMatrix();
  translate(x2, y2);
  scale(s2);
  rotate(r2);
  text(text2, 0, 0);
  popMatrix();

  pushMatrix();
  translate(x3, y3);
  scale(s3);
  rotate(r3);
  text(text3, 0, 0);
  popMatrix();

  pushMatrix();
  translate(x4, y4);
  scale(s4);
  rotate(r4);
  text(text4, 0, 0);
  popMatrix();

  pushMatrix();
  translate(x5, y5);
  scale(s5);
  rotate(r5);
  text(text5, 0, 0);
  popMatrix();

}

void keyPressed() {
  if(key == ' ') {
    //saveFrame("type-####.tif"); 
  }
  if(key == ENTER || key == RETURN) {
    mode++;
    if(mode > 5) {
      mode = 1; 
    } 
  } 
  else if (key == BACKSPACE) { 
    if(mode == 1) {
      if(text1.length() > 0) {
        text1 = text1.substring(0, text1.length() - 1);
      }
    } 
    else if (mode ==2 ) {
      if(text2.length() > 0) {
        text2 = text2.substring(0, text2.length() - 1);
      }   
    } 
    else if (mode == 3) {
      if(text3.length() > 0) {
        text3 = text3.substring(0, text3.length() - 1);
      }  
    } 
    else if (mode == 4) {
      if(text4.length() > 0) {
        text4 = text4.substring(0, text4.length() - 1);
      }    
    } 
    else if (mode == 5) {
      if(text5.length() > 0) {
        text5 = text5.substring(0, text5.length() - 1);
      }   
    }
  } 
  else {
    if(mode == 1) {
      text1 += key;
    } 
    else if (mode ==2 ) {
      text2 += key;    
    } 
    else if (mode == 3) {
      text3 += key;    
    } 
    else if (mode == 4) {
      text4 += key;    
    } 
    else if (mode == 5) {
      text5 += key;   
    }
  }
}

void mousePressed() {
  mx = mouseX;
  my = mouseY;  
}

void mouseMoved() {
  x = mouseX;
  y = mouseY; 
  mx = mouseX;
  my = mouseY;

  if(mode == 1) {
    x1 = x;
    y1 = y;
  } 
  else if (mode ==2 ) {
    x2 = x;
    y2 = y;    
  } 
  else if (mode == 3) {
    x3 = x;
    y3 = y;    
  } 
  else if (mode == 4) {
    x4 = x;
    y4 = y;    
  } 
  else if (mode == 5) {
    x5 = x;
    y5 = y;    
  }
}

void mouseDragged() {
  //float mr = float(mouseX - mx) / width * TWO_PI;
  
  
  float mr = atan2(mouseY-my, mouseX-mx);
  //float ms = abs(float(mouseY-my) / height * 5.0) + 0.25;
  float ms = dist(mouseX, mouseY, mx, my) / 100.0;
  if(mode == 1) {
    r1 = mr;
    s1 = ms;
  } 
  else if (mode ==2 ) {
    r2 = mr;
    s2 = ms;    
  } 
  else if (mode == 3) {
    r3 = mr;
    s3 = ms;    
  } 
  else if (mode == 4) {
    r4 = mr;
    s4 = ms;    
  } 
  else if (mode == 5) {
    r5 = mr;
    s5 = ms;    
  }
}
