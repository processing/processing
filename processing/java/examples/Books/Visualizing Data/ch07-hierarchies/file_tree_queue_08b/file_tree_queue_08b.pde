// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


Node[] folders = new Node[10];
int folderCount; 
int folderIndex;

Node rootNode;


void setup() {
  size(400, 130);
  // Replace this location with a folder on your machine
  File rootFile = new File("/Applications/Processing 0135");
  rootNode = new Node(rootFile);
  PFont font = createFont("SansSerif", 11);
  textFont(font);
}


void draw() {
  background(224);
  nextFolder();
  drawStatus();
}


void drawStatus() {
  float statusX = 30;
  float statusW = width - statusX*2;
  float statusY = 60;
  float statusH = 20;

  fill(0);
  if (folderIndex != folderCount) {
    text("Reading " + nfc(folderIndex+1) + 
         " out of " + nfc(folderCount) + " folders...", 
         statusX, statusY - 10);    
  } else {
    text("Done reading.", statusX, statusY - 10);
  }
  fill(128);
  rect(statusX, statusY, statusW, statusH);

  float completedW = map(folderIndex + 1, 0, folderCount, 0, statusW);
  fill(255);
  rect(statusX, statusY, completedW, statusH);  
}


void addFolder(Node folder) {
  if (folderCount == folders.length) {
    folders = (Node[]) expand(folders);
  }
  folders[folderCount++] = folder;
}


void nextFolder() {
  if (folderIndex != folderCount) {
    Node n = folders[folderIndex++];
    n.check();
  }
}

