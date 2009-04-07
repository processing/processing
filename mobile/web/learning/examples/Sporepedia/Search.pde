class Search implements State {
  Asset[] assets;
  int index, count;
  int selection;
  int type;

  PRequest apirequest, imgrequest;
  Asset apifetching, imgfetching;
  boolean error;
  boolean show;
  String text;

  Search(int type) {
    this.type = type;
    fetch(0, 10);
  }

  void fetch(int index, int count) {
    if (apirequest == null) {
      this.count = 0;
      this.index = index;
      this.assets = null;
      String search = TYPES[type];
      apirequest = client.GET("/rest/assets/search/RANDOM/" + index +"/" + count + "/" + search);
      if (index < this.index) {
        this.selection = count - 2;
      } 
      else {
        this.selection = 0;
      }
    }
  }

  void fetchImage(Asset asset) {
    if (imgrequest == null) {
      String url = (String) asset.get("thumb");
      url = url.substring(20);
      imgfetching = asset;
      imgrequest = client.GET(url);
    }
  }

  void drawIcons(boolean blink) {
    int angle = 0;
    for (int i = 3; i >= 0; i--) {
      boolean draw = !blink;
      if (blink && (i == type) && (((millis() / 200) % 2) == 0)) {
        draw = true;
      }
      if (draw) {
        image(icons[i], (width >> 1) + fptoi(mul(itofp(100), cos(angle))) - 16, (height >> 1) + fptoi(mul(itofp(100), sin(angle))) - 16);
      }
      angle += PI / 2;
    }
  }

  State draw() {
    background(0);
    image(highlight, (width - highlight.width) >> 1, (height - highlight.height) >> 1);
    textFont(font);
    textAlign(CENTER);
    if (apirequest != null) {
      drawSpinner(width >> 1, height >> 1, width >> 2);
      if (apirequest.state == PRequest.STATE_OPENED) {
        sleep();
      }
      drawIcons(true);
    } 
    else if (assets == null) {
      text("An error occured connecting to Spore.com. Please try again later.", width >> 1, height >> 1);
    } 
    else {
      int y = font.height;
      if (selection < count) {
        if (assets[selection].image == null) {
          fetchImage(assets[selection]);
          drawSpinner(width >> 1, height >> 1, width >> 2);
          drawIcons(true);
          sleep();
        } 
        else {
          PImage img = assets[selection].image;
          image(img, (width - img.width) >> 1, (height - img.height) >> 1);
          drawIcons(false);
        }
        if (show) {
          noStroke();
          fill(0x7f0000ff);
          rect(0, (height >> 1) - (font.height << 1), width, font.height << 2);
          fill(0xffffffff);
          text((String) assets[selection].get("name"), width >> 1, height >> 1);
          text("by " + (String) assets[selection].get("author"), width >> 1, (height >> 1) + font.height);
        }
      }
    }
    fill(0xff0000ff);
    text("Press 0 to exit", width >> 1, height - 1);
    return this;
  }

  State keyPressed() {
    int newType = -1;
    switch (keyCode) {
    case UP:
      newType = 0;
      break;
    case LEFT:
      newType = 1;
      break;
    case DOWN:
      newType = 2;
      break;
    case RIGHT:
      newType = 3;
      break;
    case FIRE:
      show = !show;
      break;
    default:
      switch (key) {
      case '0':
        return null;
      }
    }
    if (newType >= 0) {
      if (newType == type) {
        selection += 1;
        if (selection >= count)  {
          fetch(0, 10);
        }
      }
      else {
        type = newType;
        fetch(0, 10);
      }
    }
    return this;
  }

  State libraryEvent(Object library, int event, Object data) {
    if (library == apirequest) {
      switch (event) {
      case PRequest.EVENT_CONNECTED:
        parser.start(apirequest);
        break;
      case PRequest.EVENT_ERROR:          
        this.selection = 0;
        this.index = 0;
        this.count = 0;
        this.assets = null;
      case PRequest.EVENT_DONE:
        apirequest = null;
        break;
      }
    } 
    else if (library == imgrequest) {
      switch (event) {
      case PRequest.EVENT_CONNECTED:
        imgrequest.readBytes();
        break;
      case PRequest.EVENT_DONE:
        imgfetching.image = new PImage((byte[]) data);
      case PRequest.EVENT_ERROR:
        imgfetching = null;
        imgrequest = null;
        break;
      }
    } 
    else if (library == parser) {
      switch (event) {
      case XMLParser.EVENT_TAG_START:
        if ("asset".equals(data)) {
          apifetching = new Asset();
        }
        break;
      case XMLParser.EVENT_TEXT:
        text = (String) data;
        break;
      case XMLParser.EVENT_TAG_END:
        if ("count".equals(data)) {
          int count = int(text);
          assets = new Asset[count];
        } 
        else if ("asset".equals(data)) {
          assets[count] = apifetching;
          apifetching = null;
          count++;
        } 
        else if (apifetching != null) {
          apifetching.put(data, text);
        }
        break;
      case XMLParser.EVENT_DOCUMENT_END:
        apirequest = null;
        break;
      }
    }
    return this;
  }
}
