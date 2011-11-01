// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.


class Place {
  int code;
  String name;
  float x;
  float y;
		    
  int partial[];
  int matchDepth;


  public Place(int code, String name, float lon, float lat) {
    this.code = code;
    this.name = name;
    this.x = lon;
    this.y = lat;

    partial = new int[6];
    partial[5] = code;
    partial[4] = partial[5] / 10;
    partial[3] = partial[4] / 10;
    partial[2] = partial[3] / 10;
    partial[1] = partial[2] / 10;
  }


  void check() {
    // default to zero levels of depth that match
    matchDepth = 0;

    if (typedCount != 0) {
      // Start from the greatest depth, and work backwards to see how many 
      // items match. Want to figure out the maximum match, so better to 
      // begin from the end. 
      // The multiple levels of matching are important because more than one
      // depth level might be fading at a time.
      for (int j = typedCount; j > 0; --j) {
	if (typedPartials[j] == partial[j]) {
	  matchDepth = j;
	  break;  // since starting at end, can stop now
	}
      }
    }

    //if (partial[typedCount] == partialCode) {
    if (matchDepth == typedCount) {
      foundCount++;
      if (typedCount == 5) {
	chosen = this;
      }

      if (x < boundsX1) boundsX1 = x;
      if (y < boundsY1) boundsY1 = y;
      if (x > boundsX2) boundsX2 = x;
      if (y > boundsY2) boundsY2 = y;        
    }
  }

  void draw() {
    float xx = TX(x);
    float yy = TY(y);

    if ((xx < 0) || (yy < 0) || (xx >= width) || (yy >= height)) return;

    if ((zoomDepth.value < 2.8f) || !zoomEnabled) {  // show simple dots
      //pixels[((int) yy) * width + ((int) xx)] = faders[matchDepth].cvalue;
      set((int)xx, (int)yy, faders[matchDepth].colorValue);

    } else {  // show slightly more complicated dots
      noStroke();

      fill(faders[matchDepth].colorValue);
      //rect(TX(nlon), TY(nlat), depther.value-1, depther.value-1);

      if (matchDepth == typedCount) {
	if (typedCount == 4) {  // on the fourth digit, show nums for the 5th
	  text(code % 10, TX(x), TY(y));
	} else {  // show a larger box for selections
	  rect(xx, yy, zoomDepth.value, zoomDepth.value);
	}
      } else {  // show a slightly smaller box for unselected
	rect(xx, yy, zoomDepth.value-1, zoomDepth.value-1);
      }
    }
  }


  void drawChosen() {
    noStroke();
    fill(faders[matchDepth].colorValue);
    // the chosen point has to be a little larger when zooming
    int size = zoomEnabled ? 6 : 4;
    rect(TX(x), TY(y), size, size);

    // calculate position to draw the text, slightly offset from the main point
    float textX = TX(x);
    float textY = TY(y) - size - 4;

    // don't go off the top.. (e.g. 59544)
    if (textY < 20) {
      textY = TY(y) + 20;
    }

    // don't run off the bottom.. (e.g. 33242)
    if (textY > height - 5) {
      textY = TY(y) - 20;
    }

    String location = name + "  " + nf(code, 5);

    if (zoomEnabled) {
      textAlign(CENTER);
      text(location, textX, textY);

    } else {
      float wide = textWidth(location);

      if (textX > width/3) {
	textX -= wide + 8;
      } else {
	textX += 8;
      }

      textAlign(LEFT);
      fill(highlightColor);
      text(location, textX, textY);
    }
  }
}
