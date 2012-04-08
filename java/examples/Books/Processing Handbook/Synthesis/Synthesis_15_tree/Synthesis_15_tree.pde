/**
 * Synthesis 4: Structure and Interface
 * Swingtree by Andreas Schlegel (www.sojamo.de) at ART+COM (www.artcom.de)
 * p. 498
 *
 * Loads a data file to create the connections of the tree elements.
 * The size and motion is affected by the mouse.
 */
 

Branch[] b;
Segment[] s;

int rootX;
int rootY;
int rootId;
int redNum = 139;

float pX,pY;
int frameCounter = 0;

void setup() {
  size(400, 900);
  rootX = width/2;
  rootY = height;
  parseTree();
  for(int i=0; i<s.length; i++) {
   s[i].setFutureToOrigin();
   s[i].setLength(0);
   s[i].scaleFutureLength(1.4);
  }
  smooth();
}

void draw() {

  background(255);
  s[rootId].render();
  pX += (mouseX-pX)/10;
  pY += (mouseY-pY)/10;
  
  float tmpSpeed = 8*(pX-width/2)/width;
  s[rootId].setAngle(90 + sin(millis()*0.0006)*10*tmpSpeed);
  s[redNum].setAngle(sin(millis()*0.0001)*10*tmpSpeed);
  s[240].setAngle(sin(millis()*0.0001)*(30*tmpSpeed));
  s[148].setAngle(sin(millis()*0.0001)*(20*tmpSpeed));
  s[113].setAngle(sin(millis()*0.0004)*(20*tmpSpeed));
  
  float tmpHeight = (pY-height/2)/height;
  for(int i=0;i<s.length;i++) {
    s[i].scaleLengthFromOrigin(1-tmpHeight);
  }
}
  
void mousePressed() {
  redNum++;
  println(redNum%391);

  // saveFrame("tree-####.tif");
}

void parseTree() {
  String[] lines;
  String[] params;
  lines  = loadStrings("treeData.txt");
  println("There are " + lines.length + " lines.");
  b = new Branch[lines.length];
  s = new Segment[lines.length];
    
  for (int i=0; i < lines.length; i++) {
    params = split(lines[i],'|');
     b[i] = new Branch(params[0], params[1], params[2]);
  }
  
  for(int i=0; i<b.length; i++) {
    b[i].checkForParents();
  }
  for(int i=0; i<b.length; i++) {
    s[i] = new Segment(i);
  }
  for(int i=0; i<b.length; i++) {
    s[i].setBranch(b[i]);
  }
  
  // get the ID of the root Segment
  rootId = -1;
  for(int i=0; i<b.length; i++) {
    if(s[i].parent == null) {
      rootId = i;
    }
  }
  println("ROOT -> " + rootId);
  
  for(int i=0; i<b.length; i++) {
    s[i].setParamsFromBranch();
  }
  
  s[rootId].adjustAngle(0);
  s[rootId].adjustDepth(1);
  
}

