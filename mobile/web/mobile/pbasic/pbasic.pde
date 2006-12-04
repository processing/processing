PImageLabel header;
PLabel headerLabel;
PContainer container;
PScrollBar scrollbar;
PFont fontBold, fontSmall;

final int SCREEN_MAIN      = 0;
final int SCREEN_SUMMARY   = 1;
final int SCREEN_DETAILS   = 2;
final int SCREEN_FONTS     = 3;
final int SCREEN_SUBMIT    = 4;

final String LABEL_SUMMARY = "View Summary";
final String LABEL_DETAILS = "View Details";
final String LABEL_FONTS   = "View Fonts";
final String LABEL_SUBMIT  = "Submit Results";
final String LABEL_PREVIOUS= "Previous";
final String LABEL_NEXT    = "Next";

final String SOFTKEY_BACK  = "Back";

int screen;

String id;
String useragent;
String[] display;
Object[] displayValues;
String[] libraries;
boolean[] supported;
String[] categories;
String[][] properties, values;
String timezones;

int[] font;
String[] face, style, size;

void setup() {
  framerate(10);
  Class manager = null;
  try {
    manager = Class.forName("javax.microedition.media.Manager");
  } catch (ClassNotFoundException cnfe) {
  }
  Class videocontrol = null;
  try {
    videocontrol = Class.forName("javax.microedition.media.control.VideoControl");
  } catch (ClassNotFoundException cnfe) {
  }
  Class discoveryagent = null;
  try {
    discoveryagent = Class.forName("javax.bluetooth.DiscoveryAgent");
  } catch (ClassNotFoundException cnfe) {
  }
  Class messageconnection = null;
  try {
    messageconnection = Class.forName("javax.wireless.messaging.MessageConnection");
  } catch (ClassNotFoundException cnfe) {
  }
  
  id = getProperty("MP-Id");
  useragent = getProperty("MP-UserAgent");
  
  display = new String[] {
    "width", "height", "colors"
  };
  displayValues = new Object[length(display)];
  displayValues[0] = new Integer(width);
  displayValues[1] = new Integer(height);
  displayValues[2] = new Integer(numColors());
  
  categories = new String[] {
    "Base",
    "Bluetooth",
    "Messaging",
    "Multimedia"    
  };
  
  properties = new String[][] {
    new String[] {
      "microedition.configuration",
      "microedition.profiles",
      "microedition.locale",
      "microedition.encoding",
      "microedition.platform",
      "microedition.hostname",
      "microedition.commports",
    },
    new String[] {
      "bluetooth.api.version",
      "obex.api.version",
      "bluetooth.l2cap.receiveMTU.max",
      "bluetooth.connected.devices.max",
      "bluetooth.connected.inquiry",
      "bluetooth.connected.page",
      "bluetooth.connected.inquiry.scan",
      "bluetooth.connected.page.scan",
      "bluetooth.master.switch",
      "bluetooth.sd.trans.max",
      "bluetooth.sd.attr.retrievable.max"
    },
    new String[] {
      "wireless.messaging.sms.smsc",
      "wireless.messaging.mms.mmsc"
    },
    new String[] {
      "microedition.media.version",
      "supports.mixing",
      "supports.audio.capture",
      "supports.video.capture",
      "supports.recording",
      "audio.encodings",
      "video.encodings",
      "video.snapshot.encodings",
      "streamable.contents"
    }
  };
  
  values = new String[length(categories)][];
  for (int i = 0, length = length(categories); i < length; i++) {
    values[i] = new String[length(properties[i])];
    for (int j = 0, length2 = length(properties[i]); j < length2; j++) {
      values[i][j] = getProperty(properties[i][j]);
    }
  }
    
  libraries = new String[] {
    "Bluetooth", "Image2", "Messaging", "Phone", "Sound", "Video (playback)", "Video (capture)", "XML"
  };
  supported = new boolean[length(libraries)];
  supported[0] = discoveryagent != null;
  supported[1] = values[0][1].equals("MIDP-2.0");
  supported[2] = messageconnection != null;
  supported[3] = supported[1];
  supported[4] = supported[0] || (manager != null);
  supported[5] = videocontrol != null;
  supported[6] = supported[5] && ((values[3][3] != null) && (values[3][3].equals("true")));
  supported[7] = true;
  
  String[] tzs = java.util.TimeZone.getAvailableIDs();
  timezones = "";
  for (int i = 0, length= tzs.length; i < length; i++) {
    timezones += tzs[i] + "\n";
  }

  font = new int[3];
  face = new String[] {
    "FACE_SYSTEM", "FACE_MONOSPACE", "FACE_PROPORTIONAL"
  };
  style = new String[] {
    "STYLE_PLAIN", "STYLE_BOLD", "STYLE_ITALIC", "STYLE_UNDERLINED"
  };
  size = new String[] {
    "SIZE_SMALL", "SIZE_MEDIUM", "SIZE_LARGE"
  };
  
  fontBold = loadFont(FACE_PROPORTIONAL, STYLE_BOLD, SIZE_SMALL);
  fontSmall = loadFont(FACE_PROPORTIONAL, STYLE_PLAIN, SIZE_SMALL);
  
  int y = 4;
  header = new PImageLabel(loadImage("mobile.png"));
  header.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  header.initialize();
  y = header.y + header.height;
  
  headerLabel = new PLabel("Profiler Basic v1.0");
  headerLabel.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  headerLabel.initialize();
  y = headerLabel.y + headerLabel.height + 8;
  
  scrollbar = new PScrollBar();
  scrollbar.setBounds(width - 4, y, 4, height - y - 4);
  
  showMain();
}

void draw() {
  background(255);
  header.draw();
  headerLabel.draw();
  container.draw();
}

void keyPressed() {
  container.keyPressed();
}

void keyReleased() {
  container.keyReleased();
}

void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_BACK)) {
    showMain();
  }
}

void libraryEvent(Object library, int event, Object data) {
  if (screen == SCREEN_MAIN) {
    if (data.equals(LABEL_SUMMARY)) {
      showSummary();
    } else if (data.equals(LABEL_DETAILS)) {
      showDetails();
    } else if (data.equals(LABEL_FONTS)) {
      font[0] = FACE_SYSTEM;
      font[1] = STYLE_PLAIN;
      font[2] = SIZE_SMALL;
      showFonts();
    } else if (data.equals(LABEL_SUBMIT)) {
      showSubmit();
    }
  } else if (screen == SCREEN_FONTS) {
    if (data.equals(LABEL_PREVIOUS)) {
      switch (font[1]) {
        case STYLE_PLAIN:
          font[1] = STYLE_UNDERLINED;
          switch (font[2]) {
            case SIZE_SMALL:
              font[2] = SIZE_LARGE;
              switch (font[0]) {
                case FACE_SYSTEM:
                  font[0] = FACE_PROPORTIONAL;
                  break;
                case FACE_MONOSPACE:
                  font[0] = FACE_SYSTEM;
                  break;
                case FACE_PROPORTIONAL:
                  font[0] = FACE_MONOSPACE;
                  break;
              }
              break;
            case SIZE_MEDIUM:
              font[2] = SIZE_SMALL;
              break;
            case SIZE_LARGE:
              font[2] = SIZE_MEDIUM;
              break;
          }
          break;
        case STYLE_BOLD:
          font[1] = STYLE_PLAIN;
          break;
        case STYLE_ITALIC:
          font[1] = STYLE_BOLD;
          break;
        case STYLE_UNDERLINED:
          font[1] = STYLE_ITALIC;
          break;
      }
      showFonts();
    } else if (data.equals(LABEL_NEXT)) {
      switch (font[1]) {
        case STYLE_PLAIN:
          font[1] = STYLE_BOLD;
          break;
        case STYLE_BOLD:
          font[1] = STYLE_ITALIC;
          break;
        case STYLE_ITALIC:
          font[1] = STYLE_UNDERLINED;
          break;
        case STYLE_UNDERLINED:
          font[1] = STYLE_PLAIN;
          switch (font[2]) {
            case SIZE_SMALL:
              font[2] = SIZE_MEDIUM;
              break;
            case SIZE_MEDIUM:
              font[2] = SIZE_LARGE;
              break;
            case SIZE_LARGE:
              font[2] = SIZE_SMALL;
              switch (font[0]) {
                case FACE_SYSTEM:
                  font[0] = FACE_MONOSPACE;
                  break;
                case FACE_MONOSPACE:
                  font[0] = FACE_PROPORTIONAL;
                  break;
                case FACE_PROPORTIONAL:
                  font[0] = FACE_SYSTEM;
                  break;
              }
              break;
          }
          break;
      }
      showFonts();
    }
  }
}

void showMain() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PButton summary, details, fonts, submit;
  summary = new PButton(LABEL_SUMMARY);
  summary.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(summary);
  y = summary.y + summary.height + 4;
  
  details = new PButton(LABEL_DETAILS);
  details.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(details);
  y = details.y + details.height + 4;
  
  fonts = new PButton(LABEL_FONTS);
  fonts.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(fonts);
  y = fonts.y + fonts.height + 4;
  
  submit = new PButton(LABEL_SUBMIT);
  submit.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(submit);
  
  int buttonWidth = max(max(max(summary.width, details.width), fonts.width), submit.width);
  summary.setBounds(summary.x, summary.y, buttonWidth, summary.height);
  details.setBounds(details.x, details.y, buttonWidth, details.height);
  fonts.setBounds(fonts.x, fonts.y, buttonWidth, fonts.height);
  submit.setBounds(submit.x, submit.y, buttonWidth, submit.height);
  
  container.initialize();
  container.acceptFocus();  
  
  softkey(null);
  
  screen = SCREEN_MAIN;
}

void showSummary() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PLabel label;
  String text;
  
  label = new PLabel("Id:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
       
  label = new PLabel(id + "\n\n");
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;

  label = new PLabel("User Agent:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
     
  label = new PLabel(useragent + "\n\n");
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;

  label = new PLabel("Configuration:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
   
  text = values[0][0];
  if (text.equals("CLDC-1.0")) {
    text += "\nYour phone does NOT support the float datatype.";
  } else if (text.equals("CLDC-1.1")) {
    text += "\nYour phone DOES support the float datatype.";
  }
  text += "\n\n";
  label = new PLabel(text);
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  label = new PLabel("Profile:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
   
  text = values[0][1];
  if (text.equals("MIDP-1.0")) {
    text += "\nYour phone CANNOT run Profiler Advanced.";
  } else if (text.equals("MIDP-2.0")) {
    text += "\nYour phone CAN run Profiler Advanced.";
  }
  text += "\n\n";
  label = new PLabel(text);
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  label = new PLabel("Time Zones:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
    
  label = new PLabel(timezones + "\n");
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;

  label = new PLabel("Display:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
   
  text = "";
  for (int i = 0, length = length(display); i < length; i++) {
    text += display[i] + ": " + displayValues[i] + "\n";
  }
  text += "\n";
  label = new PLabel(text);
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;

  label = new PLabel("Libraries:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  text = "";
  for (int i = 0, length = length(libraries); i < length; i++) {
    text += libraries[i] + ": " + supported[i] + "\n";
  }
  label = new PLabel(text);
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  container.initialize();
  container.acceptFocus();  
  
  softkey(SOFTKEY_BACK);
  
  screen = SCREEN_SUMMARY;
}

void showDetails() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PLabel label;
  for (int i = 0, length = length(categories); i < length; i++) {
    label = new PLabel(categories[i] + ":");
    label.font = fontBold;
    label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
    container.add(label);
    y = label.y + label.height;
    
    String text = "";
    for (int j = 0, length2 = length(properties[i]); j < length2; j++) {
      text += properties[i][j] + ": " + values[i][j] + "\n";
    }
    text += "\n";
    label = new PLabel(text);
    label.font = fontSmall;
    label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
    container.add(label);
    y = label.y + label.height;
  }

  container.initialize();
  container.acceptFocus();  
  
  softkey(SOFTKEY_BACK);
  
  screen = SCREEN_DETAILS;
}

void showFonts() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PLabel label;
  PFont fontShow = loadFont(font[0], font[1], font[2]);
  label = new PLabel("Font:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  String text = "Face: ";
  switch (font[0]) {
    case FACE_SYSTEM:
      text += face[0];
      break;
    case FACE_MONOSPACE:
      text += face[1];
      break;
    case FACE_PROPORTIONAL:
      text += face[2];
      break;
  }
  text += "\nStyle: ";
  switch (font[1]) {
    case STYLE_PLAIN:
      text += style[0];
      break;
    case STYLE_BOLD:
      text += style[1];
      break;
    case STYLE_ITALIC:
      text += style[2];
      break;
    case STYLE_UNDERLINED:
      text += style[3];
      break;
  }
  text += "\nSize: ";
  switch (font[2]) {
    case SIZE_SMALL:
      text += size[0];
      break;
    case SIZE_MEDIUM:
      text += size[1];
      break;
    case SIZE_LARGE:
      text += size[2];
      break;
  }
  text += "\nHeight: " + fontShow.height +"\nBaseline: " + fontShow.baseline + "\n\n";
  label = new PLabel(text);
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  label = new PLabel("Sample:");
  label.font = fontBold;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  label = new PLabel("0123456789\nABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz\nThe quick brown fox jumped over the lazy dogs.\n\n");
  label.font = fontShow;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  PButton prev, next;
  next = new PButton(LABEL_NEXT);
  next.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(next);
  y = next.y + next.height;
  
  prev = new PButton(LABEL_PREVIOUS);
  prev.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(prev);
  
  int buttonWidth = max(prev.width, next.width);
  prev.setBounds(prev.x, prev.y, buttonWidth, prev.height);
  next.setBounds(next.x, next.y, buttonWidth, next.height);
  
  container.initialize();
  container.acceptFocus();  
  
  softkey(SOFTKEY_BACK);
  
  screen = SCREEN_FONTS;
}

void showSubmit() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = false;
  container.setBounds(0, y, width, height - y - 4);  
  
  String[] items = new String[50];
  for (int i = 0; i < 50; i++) {
    items[i] = Integer.toString(i);
  }
  
  PList list = new PList();
  list.setBounds(0, y, width - 4, height - y - 4);
  list.scrollBar = scrollbar;
  list.add(items);
  list.initialize();
  container.add(list);
  container.add(scrollbar);
  
  softkey(SOFTKEY_BACK);
  
  screen = SCREEN_SUBMIT;
}
