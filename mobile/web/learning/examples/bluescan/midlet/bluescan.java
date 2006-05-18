import processing.core.*; import processing.bluetooth.*; public class bluescan extends PMIDlet{// Bluescan
// by Francis Li
// http://www.francisli.com/
//
// inspired by
// 
// Jabberwocky
// by Intel Research Berkeley
// http://www.urban-atmospheres.net/Jabberwocky/index.htm
//
// Posted January 2, 2006
//
// Bluescan discovers nearby bluetooth devices and represents them on the screen
// as a cellphone icon (even though they may be PCs or other devices).  As they are discovered,
// they appear at the top of the screen, then slowly drift down until they disappear at the bottom.
// Newly discovered devices are red, devices you've encountered before are green.  The more times
// you encounter a device, the bigger the icon will be.
//


final String SOFTKEY_DISCOVER = "Discover";
final String SOFTKEY_CANCEL   = "Cancel";

PFont font;
Bluetooth bt;
boolean discovering;
CellphoneIcon[] icons;
int numIcons;
int selected;

Record[] records;
int numRecords;

int prev;

public void setup() {
  //// get and set the default system font
  font = loadFont();
  textFont(font);

  //// instantiate a new bluetooth object
  bt = new Bluetooth(this);
  
  //// initialize the records array to a default size
  records = new Record[8];
  numRecords = 0;
  
  //// initialize the icons array to a default size
  icons = new CellphoneIcon[8];
  numIcons = 0;
  
  //// set up a command softkey to start discovery process
  softkey(SOFTKEY_DISCOVER);
  
  //// cap framerate
  framerate(20);
  
  //// align everything center
  textAlign(CENTER);
  
  //// load previously saved records
  String[] savedRecords = loadStrings("records.txt");
  int numSaved = savedRecords.length;
  Record r;
  for (int i = 0; i < numSaved; i++) {
    r = new Record(savedRecords[i]);
    addRecord(r);
  }
}

public void destroy() {
  //// before the midlet is destroyed, save records
  String[] savedRecords = new String[numRecords];
  for (int i = 0; i < numRecords; i++) {
    savedRecords[i] = records[i].toString();
  }
  saveStrings("records.txt", savedRecords);
}

public void draw() {
  //// set a nice blue sea background
  background(0xff0099ff);
  
  //// update frame timing
  int current = millis();
  int elapsed = current - prev;
  prev = current;
  
  //// move icons based on elapsed time
  for (int i = 0; i < numIcons; i++) {
    icons[i].move(elapsed);
    if (icons[i].ty >= (height - icons[i].height() / 2)) {
      icons[i].tscale_fp = 0;
    }
  }
  
  //// if icons have reached the bottom, remove them from the array
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
  
  //// draw the icons
  for (int i = 0; i < numIcons; i++) {
    icons[i].draw();    
  }
  
  //// draw name of current selection
  if (selected < numIcons) {
    Record r = icons[selected].r;
    //// draw the name under the icon
    if (r.name == Device.UNKNOWN) {
      //// if the record name is unknown, check the device to see if it has
      //// been updated in the background discovery process
      r.name = r.device.name;
    }
    int width = textWidth(r.name);
    stroke(0);
    fill(255);
    rect(icons[selected].x - width / 2 - 2, icons[selected].y + icons[selected].height() / 2, width + 4, font.height + 4);
    fill(0);
    text(r.name, icons[selected].x, icons[selected].y + icons[selected].height() / 2 + font.baseline + 2);
  }  
  
  //// draw the current number of visible devices
  text(str(numIcons), width / 2, height);
}

public void keyPressed() {
  if (key == '5') {
    //// testing animation on the emulator, just puts a dummy device/icon on the screen
    Record r = new Record();
    r.name = "Test";
    r.count = random(5);
    addIcon(r);
  } else if (keyCode == LEFT) {
    //// update selection
    if (numIcons > 0) {
      selected = (selected - 1 + numIcons) % numIcons;
    }
  } else if (keyCode == RIGHT) {
    //// update selection
    if (numIcons > 0) {
      selected = (selected + 1) % numIcons;
    }
  }
}

public void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_DISCOVER)) {
    //// start discovery process
    discovering = true;  
    bt.discover();
    softkey(SOFTKEY_CANCEL);
  } else if (label.equals(SOFTKEY_CANCEL)) {
    //// cancel discovery process
    discovering = false;
    bt.cancel();
    softkey(SOFTKEY_DISCOVER);
  }
}

public void libraryEvent(Object library, int event, Object data) {
  if (library == bt) {
    switch (event) {
      case Bluetooth.EVENT_DISCOVER_DEVICE:
        //// new device discovered!
        Device d = (Device) data;
        Record r = null;
        String address = d.address;
        boolean found = false;
        //// first, look for it in existing records list
        for (int i = 0; i < numRecords; i++) {
          if (records[i].address.equals(address)) {
            /// found! update count and last encounter time
            r = records[i];
            r.count++;
            //// a bit of a hack to get an absolute system time (unlike millis() which is relative to app start)
            r.last = (int) (System.currentTimeMillis() / 1000);
            r.device = d;
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
            //// put it on screen!
            addIcon(r);
          }
        } else {
          //// not found, so create a new record
          r = new Record();
          r.name = d.name;
          r.address = d.address;
          r.count = 1;
          r.last = (int) (System.currentTimeMillis() / 1000);
          r.device = d;
          addRecord(r);
          addIcon(r);
        }
        break;
      case Bluetooth.EVENT_DISCOVER_DEVICE_COMPLETED:
        //// done, reset command key and state
        discovering = false;
        softkey(SOFTKEY_DISCOVER);
        break;
    }
  }
}

//// adds a record to the array, expanding it if necessary
public void addRecord(Record r) {
  records[numRecords] = r;
  numRecords++;
  
  if (numRecords == records.length) {
    Record[] oldRecords = records;
    records = new Record[2 * numRecords];
    arraycopy(oldRecords, 0, records, 0, numRecords);
  }
}

//// puts an icon on the screen
public void addIcon(Record r) {
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

//// a data structure to store device info, and to read/write it from strings
class Record {
  String name;
  String address;
  int count;
  int last;
  Device device;
  
  public Record() {
  }
  
  public Record(String savedRecord) {
    String[] parts = split(savedRecord, ",");
    name = parts[0];
    address = parts[1];
    count = PMIDlet.toInt(parts[2]);
    last = PMIDlet.toInt(parts[3]);
  }
  
  public String toString() {
    return name + "," + address + "," + count + "," + last;
  }
}

//// a data structure to store icon info, as well as render it to screen
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
  
  public int width() {
    return fptoi(tscale_fp * 12);
  }
  
  public int height() {
    return fptoi(tscale_fp * 30);
  }
  
  public void move(int elapsed) {
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
  
  public void draw() {
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
}