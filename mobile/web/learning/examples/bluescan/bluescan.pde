import processing.bluetooth.*;

final String SOFTKEY_DISCOVER = "Discover";
final String SOFTKEY_CANCEL   = "Cancel";

PFont font;
Bluetooth bt;
CellphoneIcon[] icons;
int numIcons;
int selected;

Record[] records;
int numRecords;

int prev;

void setup() {
  font = loadFont();
  textFont(font);

  bt = new Bluetooth(this);
  
  records = new Record[8];
  numRecords = 0;
  
  icons = new CellphoneIcon[8];
  numIcons = 0;
  
  softkey(SOFTKEY_DISCOVER);
  
  framerate(20);
  
  textAlign(CENTER);
  
  String[] savedRecords = loadStrings("records.txt");
  int numSaved = savedRecords.length;
  Record r;
  for (int i = 0; i < numSaved; i++) {
    r = new Record(savedRecords[i]);
    addRecord(r);
  }
}

void destroy() {
  String[] savedRecords = new String[numRecords];
  for (int i = 0; i < numRecords; i++) {
    savedRecords[i] = records[i].toString();
  }
  saveStrings("records.txt", savedRecords);
}

void draw() {
  background(0xff0099ff);
  
  int current = millis();
  int elapsed = current - prev;
  prev = current;
  
  for (int i = 0; i < numIcons; i++) {
    icons[i].move(elapsed);
    if (icons[i].ty >= (height - icons[i].height() / 2)) {
      icons[i].tscale_fp = 0;
    }
  }
  
  for (int i = numIcons - 1; i >= 0; i--) {
    if (icons[i].scale_fp == 0) {
      icons[i] = null;
      arraycopy(icons, i + 1, icons, i, numIcons - i - 1);
      numIcons--;
      selected--;
    }
  }
  if (selected < 0) {
    selected = 0;
  }
  
  for (int i = 0; i < numIcons; i++) {
    icons[i].draw();    
  }
  
  if (selected < numIcons) {
    String name = icons[selected].r.name;
    int width = textWidth(name);
    stroke(0);
    fill(255);
    rect(icons[selected].x - width / 2 - 2, icons[selected].y + icons[selected].height() / 2, width + 4, font.height + 4);
    fill(0);
    text(name, icons[selected].x, icons[selected].y + icons[selected].height() / 2 + font.baseline + 2);
  }  
  
  text(str(numIcons), width / 2, height);
}

void keyPressed() {
  if (key == '5') {
    Record r = new Record();
    r.name = "Test";
    r.count = 1;
    addIcon(r);
  } else if (keyCode == LEFT) {
    if (numIcons > 0) {
      selected = (selected - 1 + numIcons) % numIcons;
    }
  } else if (keyCode == RIGHT) {
    if (numIcons > 0) {
      selected = (selected + 1) % numIcons;
    }
  }
}

void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_DISCOVER)) {
    bt.discover();
    softkey(SOFTKEY_CANCEL);
  } else if (label.equals(SOFTKEY_CANCEL)) {
    bt.cancel();
    softkey(SOFTKEY_DISCOVER);
  }
}

void libraryEvent(Object library, int event, Object data) {
  if (library == bt) {
    switch (event) {
      case Bluetooth.EVENT_DISCOVER_DEVICE:
        Device d = (Device) data;
        Record r = null;
        String address = d.address();
        boolean found = false;
        for (int i = 0; i < numRecords; i++) {
          if (records[i].address.equals(address)) {
            r = records[i];
            r.count++;
            r.last = (int) (System.currentTimeMillis() / 1000);
            found = true;
            break;
          }
        }
        if (found) {
          found = false;
          //// see if it is currently on screen
          for (int i = 0; i < numIcons; i++) {
            if (icons[i].r == r) {
              icons[i].tx = icons[i].height() / 2;
              found = true;
              break;
            }
          }
          if (!found) {
            addIcon(r);
          }
        } else {
          r = new Record();
          r.name = d.name();
          r.address = d.address();
          r.count = 1;
          r.last = (int) (System.currentTimeMillis() / 1000);
          addRecord(r);
          addIcon(r);
        }
      case Bluetooth.EVENT_DISCOVER_DEVICE_COMPLETED:
        softkey(SOFTKEY_DISCOVER);
        break;
    }
  }
}

void addRecord(Record r) {
  records[numRecords] = r;
  numRecords++;
  
  if (numRecords == records.length) {
    Record[] oldRecords = records;
    records = new Record[2 * numRecords];
    arraycopy(oldRecords, 0, records, 0, numRecords);
  }
}

void addIcon(Record r) {
  CellphoneIcon i = new CellphoneIcon();
  i.r = r;
  i.tscale_fp = (3 + r.count) * ONE / 4;
  i.x = i.tx = 10 + random(width - 20);
  i.y = i.ty = i.height() / 2;
  
  icons[numIcons] = i;
  numIcons++;
  
  if (numIcons == icons.length) {
    CellphoneIcon[] oldIcons = icons;
    icons = new CellphoneIcon[2 * numIcons];
    arraycopy(oldIcons, 0, icons, 0, numIcons);
  }
}

class Record {
  String name;
  String address;
  int count;
  int last;
  
  public Record() {
  }
  
  public Record(String savedRecord) {
    String[] parts = split(savedRecord, ",");
    name = parts[0];
    address = parts[1];
    count = int(parts[2]);
    last = int(parts[3]);
  }
  
  String toString() {
    return name + "," + address + "," + count + "," + last;
  }
}

class CellphoneIcon {
  /** Reference to device history record object */
  Record r;
  /** the actual x, y location on the screen */
  int x, y;
  /** the actual scale of the icon on the screen */
  int scale_fp;
  /** accumulated distance */
  int dx_fp, dy_fp;
  /** the target x, y, and scale */
  int tx, ty, tscale_fp;
  /** accumulated distance of target motion */  
  int dty_fp;
  /** x, y velocities */
  int vx_fp, vy_fp;
  
  int width() {
    return fptoi(tscale_fp * 12);
  }
  
  int height() {
    return fptoi(tscale_fp * 30);
  }
  
  void move(int elapsed) {
    //// quickly reach target scale
    int dscale_fp = (tscale_fp - scale_fp) / 2;
    if (dscale_fp == 0) {
      scale_fp = tscale_fp;
    } else {
      scale_fp += dscale_fp;
    }    
    
    //// target y moves downward at constant velocity
    dty_fp += elapsed * itofp(8) / 1000;
    if (dty_fp > ONE) {
      ty += fptoi(floor(dty_fp));
      dty_fp = dty_fp - floor(dty_fp);
    }    
    //// target x randomly shifts
    tx += random(-2, 2);
    if (tx < 10) {
      tx = 10;
    } else if (tx > (width - 10)) {
      tx = width - 10;
    }
    
    //// icon accelerates towards target
    vy_fp += (ty - y) * itofp(2) * elapsed / 1000;      
    if (vy_fp > itofp(16)) {
      vy_fp = itofp(16);
    } else if (vy_fp < -itofp(16)) {
      vy_fp = -itofp(16);
    }
    dy_fp += elapsed * vy_fp / 1000;
    if ((dy_fp > ONE) || (dy_fp < -ONE)) {
      y += fptoi(floor(dy_fp));
      dy_fp = dy_fp - floor(dy_fp);
    }
    
    vx_fp += (tx - x) * itofp(2) * elapsed / 1000;
    if (vx_fp > itofp(16)) {
      vx_fp = itofp(16);
    } else if (vx_fp < -itofp(16)) {
      vx_fp = -itofp(16);
    }
    dx_fp += elapsed * vx_fp / 1000;
    if ((dx_fp > ONE) || (dx_fp < -ONE)) {
      x += fptoi(floor(dx_fp));
      dx_fp = dx_fp - floor(dx_fp);
    }
  }
  
  void draw() {
    noStroke();
    if (r.count > 1) {
      fill(0xff00ff00);
    } else {
      fill(0xffff0000);
    }
    //// antenna
    rect(x - fptoi(scale_fp * 12) / 2 + fptoi(scale_fp * 12) - fptoi(scale_fp * 3), y - fptoi(scale_fp * 25) / 2 - fptoi(scale_fp * 5), fptoi(scale_fp * 3), fptoi(scale_fp * 5));    
    //// body
    rect(x - fptoi(scale_fp * 12) / 2, y - fptoi(scale_fp * 25) / 2, fptoi(scale_fp * 12), fptoi(scale_fp * 25));
    //// screen
    fill(230);
    rect(x - fptoi(scale_fp * 12) / 2 + fptoi(scale_fp * 2), y - fptoi(scale_fp * 25) / 2 + fptoi(scale_fp * 2), fptoi(scale_fp * 12) - 2 * fptoi(scale_fp * 2), fptoi(scale_fp * 8));
  }
}
