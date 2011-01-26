// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


import treemap.*;

Treemap map;

void setup() {
  size(1024, 768);

  smooth();
  strokeWeight(0.25f);
  PFont font = createFont("Serif", 13);
  textFont(font);

  WordMap mapData = new WordMap();
    
  String[] lines = loadStrings("equator.txt");
  for (int i = 0; i < lines.length; i++) {
    mapData.addWord(lines[i]);
  }
  mapData.finishAdd();

    // different choices for the layout method
    //MapLayout algorithm = new SliceLayout();
    //MapLayout algorithm = new StripTreemap();
    //MapLayout algorithm = new PivotBySplitSize();
    //MapLayout algorithm = new SquarifiedLayout();

  map = new Treemap(mapData, 0, 0, width, height);

  // only run draw() once
  noLoop();
}


void draw() {
  background(255);
  map.draw();
}
