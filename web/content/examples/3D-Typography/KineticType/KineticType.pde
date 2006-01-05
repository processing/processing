// Kinetic Type
// by Zach Lieberman <http://www.thesystemis.com>

// Using the push() pop() defines the curves of the lines of type.

// Created Spring 2002
// Updated 18 January 2003

Line ln;
Line lns[];
PFont f;
String words[] = {
  "sometimes it's like", "the lines of text", "are so happy", "that they want to dance",
  "or leave the page or jump", "can you blame them?", "living on the page like that",
  "waiting to be read..."
};

void setup()
{
  size(200, 200, P3D);
  
  framerate(30);
  
  // Array of line objects
  lns = new Line[8];

  // Load the font from the sketch's data directory
  f = loadFont("Univers66.vlw.gz");
  textFont(f, 1f);

  // White type, black background
  fill(255);

  // Creating the line objects
  for(int i = 0; i < 8; i++)
  {
    // For every line in the array, create a Line object to animate
    // i * 70 is the spacing
    ln = new Line(words[i], 0, i * 70, f);
    lns[i] = ln;
  }
}

void draw()
{
  background(0);
  
  translate((float)(width / 2.0) - 350, (height / 2.0) - 240, -450);
  rotateY(0.3);

  // Now animate every line object & draw it...
  for(int i = 0; i < 8; i++)
  {
    float f1 = sin((i + 1.0) * (millis() / 10000.0) * TWO_PI);
    float f2 = sin((8.0 - i) * (millis() / 10000.0) * TWO_PI);
    Line line = lns[i];
    pushMatrix();
    translate(0.0, line.yPosition, 0.0);
    for(int j = 0; j < line.myLetters.length; j++)
    {
      if(j != 0) {
        translate(textWidth(line.myLetters[j - 1].myChar)*75, 0.0, 0.0);
      }
      rotateY(f1 * 0.035 * f2);
      pushMatrix();
      scale(75.0, 75.0, 75.0);
      text(line.myLetters[j].myChar, 0.0, 0.0);
      popMatrix();
    }
    popMatrix();
  }
}

class Letter
{
  char myChar;
  float x;
  float y;
  
  Letter(char c, float f, float f1)
  {
    myChar = c;
    x = f;
    y = f1;
  }
}

class Word
{
  String myName;
  int x;
  
  Word(String s)
  {
    myName = s;
  }
}

class Line
{
  String myString;
  int xPosition;
  int yPosition;
  int highlightNum;
  PFont f;
  float speed;
  float curlInX;
  Letter myLetters[];
  
  Line(String s, int i, int j, PFont bagelfont) 
  {
    myString = s;
    xPosition = i;
    yPosition = j;
    f = bagelfont;
    myLetters = new Letter[s.length()];
    float f1 = 0.0;
    for(int k = 0; k < s.length(); k++)
    {
      char c = s.charAt(k);
      f1 += textWidth(c);
      Letter letter = new Letter(c, f1, 0.0);
      myLetters[k] = letter;
    }

    curlInX = 0.1;
  }
}
