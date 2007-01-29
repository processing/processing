import processing.phone.*;

final int SCREEN_MAIN      = 0;
final int SCREEN_SUMMARY   = 1;
final int SCREEN_DETAILS   = 2;
final int SCREEN_FONTS     = 3;
final int SCREEN_SHARE     = 4;

final String LABEL_SUMMARY = "View Summary";
final String LABEL_DETAILS = "View Details";
final String LABEL_FONTS   = "View Fonts";
final String LABEL_SHARE   = "Share Results";
final String LABEL_PREVIOUS= "Previous";
final String LABEL_NEXT    = "Next";
final String LABEL_SUBMIT  = "Submit";
final String LABEL_CANCEL  = "Cancel";
final String LABEL_EXIT    = "Exit";
final String LABEL_BACK    = "Back";

final String SERVER_NAME   = "wapmp.at";
final String SERVER_FILE   = "/post.php";

PImageLabel header;
PLabel headerLabel, status;
PButton submit, back;
PContainer container;
PScrollBar scrollbar;
PFont fontBold, fontSmall;

PClient client;
PRequest request;

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
  
  client = new PClient(this, SERVER_NAME);
  
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
    "width", "height", "fullWidth", "fullHeight", "colors", "alpha"
  };
  displayValues = new Object[length(display)];
  displayValues[0] = new Integer(width);
  displayValues[1] = new Integer(height);
  Phone p = new Phone(this);
  p.fullscreen();
  displayValues[2] = new Integer(width);
  displayValues[3] = new Integer(height);
  displayValues[4] = new Integer(numColors());
  displayValues[5] = new Integer(p.numAlphaLevels());
  
  categories = new String[] {
    "Base",
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
    "Bluetooth", "Image2", "Messaging", "Phone", "Sound", "Video (playback)", "Video (snapshot)", "XML"
  };
  supported = new boolean[length(libraries)];
  supported[0] = discoveryagent != null;
  supported[1] = values[0][1].indexOf("MIDP-2.0") >= 0;
  supported[2] = messageconnection != null;
  supported[3] = supported[1];
  supported[4] = supported[0] || (manager != null);
  supported[5] = videocontrol != null;
  supported[6] = supported[5] && (values[2][7] != null);
  supported[7] = true;
  
  String[] tzs = java.util.TimeZone.getAvailableIDs();
  StringBuffer tzb = new StringBuffer();
  for (int i = 0, length = length(tzs); i < length; i++) {
    tzb.append(tzs[i]);
    tzb.append('\n');
  }
  timezones = tzb.toString();
  
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
  //// hack for phones that don't switch to fullscreen immediately-
  //// update the fullWidth/Height values so that they are correct
  displayValues[2] = new Integer(width);
  displayValues[3] = new Integer(height);
  
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

void libraryEvent(Object library, int event, Object data) {
  if ((library == back) && data.equals(LABEL_BACK)) {
    showMain();
  } else if (screen == SCREEN_MAIN) {
    if (data.equals(LABEL_SUMMARY)) {
      showSummary();
    } else if (data.equals(LABEL_DETAILS)) {
      showDetails();
    } else if (data.equals(LABEL_FONTS)) {
      font[0] = FACE_SYSTEM;
      font[1] = STYLE_PLAIN;
      font[2] = SIZE_SMALL;
      showFonts();
    } else if (data.equals(LABEL_SHARE)) {
      showShare();
    } else if (data.equals(LABEL_EXIT)) {
      exit();
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
  } else if (screen == SCREEN_SHARE) {
    if (library == request) {
      //// handle networking
      if (event == PRequest.EVENT_CONNECTED) {
        //// update status message
        status.label = "Status: Reading response...\n\n";
        status.calculateBounds(4, status.y, width - 8, Integer.MAX_VALUE);
        submit.setBounds(submit.x, status.y + status.height, submit.width, submit.height);
        
        container.initialize();
        container.acceptFocus();        
        
        //// read response
        request.readBytes();
      } else if (event == PRequest.EVENT_DONE) {        
        status.label = "Status: Done!\n\n";
        status.calculateBounds(4, status.y, width - 8, Integer.MAX_VALUE);
        submit.label = LABEL_SUBMIT;
        submit.setBounds(submit.x, status.y + status.height, submit.width, submit.height);
        
        back.setBounds(back.x, submit.y + submit.height + 4, back.width, back.height);
        container.add(back);
        
        container.initialize();
        container.acceptFocus();        
        
        request.close();
      } else if (event == PRequest.EVENT_ERROR) {
        status.label = "Status: An error has occured- " + data + "\n\n";
        status.calculateBounds(4, status.y, width - 8, Integer.MAX_VALUE);
        submit.label = LABEL_SUBMIT;
        submit.setBounds(submit.x, status.y + status.height, submit.width, submit.height);
        
        back.setBounds(back.x, submit.y + submit.height + 4, back.width, back.height);
        container.add(back);
        
        container.initialize();
        container.acceptFocus();        
        
        request.close();
      }
    } else if (library == submit) {
      //// handle the button ui events
      if (data.equals(LABEL_SUBMIT)) {
        //// set up connection status display and cancel option
        status.label = ("Status: Connecting...\n\n");
        status.calculateBounds(4, status.y, width - 8, Integer.MAX_VALUE);
        
        submit.label = LABEL_CANCEL;
        submit.setBounds(submit.x, status.y + status.height, submit.width, submit.height);
        
        //// hide back button
        container.remove(back);
        
        container.initialize();
        container.acceptFocus();  
          
        //// initiate network request
        String names[], values[];
        int totalLength = 3 + length(display) + length(libraries);
        for (int i = 0, length = length(properties); i < length; i++) {
          totalLength += length(properties[i]);
        }
        int index;
        names = new String[totalLength];
        values = new String[totalLength];
        names[0] = "id";           values[0] = (id == null) ? "0" : id;
        names[1] = "useragent";    values[1] = (useragent == null) ? "None" : useragent;
        names[2] = "timezones";    values[2] = timezones;
        index = 3;
        for (int i = 0, length = length(display); i < length; i++) {
          names[index] = display[i];
          values[index] = displayValues[i].toString();
          index++;
        }
        for (int i = 0, length = length(libraries); i < length; i++) {
          names[index] = libraries[i].toLowerCase();
          values[index] = String.valueOf(supported[i]);
          index++;
        }
        for (int i = 0, length = length(properties); i < length; i++) {
          String[] props = properties[i];
          String[] vals = this.values[i];
          for (int j = 0, length2 = length(props); j < length2; j++) {
            names[index] = props[j];
            values[index] = (vals[j] == null) ? "NULL" : vals[j];
            index++;
          }
        }
        request = client.POST(SERVER_FILE, names, values);
      } else if (data.equals(LABEL_CANCEL)) {
        //// cancel network request
        request.close();
        
        //// reset status display
        status.label = "Status: Not connected.\n\n";
        status.calculateBounds(4, status.y, width - 8, Integer.MAX_VALUE);
  
        submit.label = LABEL_SUBMIT;
        submit.setBounds(submit.x, status.y + status.height, submit.width, submit.height);
        
        back.setBounds(back.x, submit.y + submit.height + 4, back.width, back.height);
        container.add(back);
                
        container.initialize();
        container.acceptFocus();  
      }
    }
  }
}

void showMain() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PButton summary, details, fonts, share, exit;
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
  
  share = new PButton(LABEL_SHARE);
  share.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(share);
  y = share.y + share.height + 4;
  
  exit = new PButton(LABEL_EXIT);
  exit.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(exit);
  
  int buttonWidth = max(max(max(max(summary.width, details.width), fonts.width), share.width), exit.width);
  summary.setBounds(summary.x, summary.y, buttonWidth, summary.height);
  details.setBounds(details.x, details.y, buttonWidth, details.height);
  fonts.setBounds(fonts.x, fonts.y, buttonWidth, fonts.height);
  share.setBounds(share.x, share.y, buttonWidth, share.height);
  exit.setBounds(exit.x, exit.y, buttonWidth, exit.height);
  
  container.initialize();
  container.acceptFocus();  
  
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
  } else if (text.indexOf("MIDP-2.0") >= 0) {
    text += "\nYour phone CAN run Profiler Advanced.";
  }
  text += "\n\n";
  label = new PLabel(text);
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
  
  back = new PButton(LABEL_BACK);
  back.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(back);
  
  container.initialize();
  container.acceptFocus();  
    
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
  
  back = new PButton(LABEL_BACK);
  back.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(back);

  container.initialize();
  container.acceptFocus();  
  
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
      
  PButton prev, next;
  next = new PButton(LABEL_NEXT);
  next.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(next);  
  y = next.y + next.height + 4;
  
  prev = new PButton(LABEL_PREVIOUS);
  prev.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(prev);
  y = prev.y + prev.height + 4;
  
  back = new PButton(LABEL_BACK);
  back.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(back);
  
  int buttonWidth = max(max(back.width, prev.width), next.width);
  prev.setBounds(prev.x, prev.y, buttonWidth, prev.height);
  next.setBounds(next.x, next.y, buttonWidth, next.height);
  back.setBounds(back.x, back.y, buttonWidth, back.height);  
  
  container.initialize();
  container.acceptFocus();  
  
  screen = SCREEN_FONTS;
}

void showShare() {
  int y = headerLabel.y + headerLabel.height + 8;

  container = new PContainer();
  container.scrolling = true;
  container.scrollbar = scrollbar;
  container.setBounds(0, y, width, height - y - 4);  
  
  PLabel label;
  label = new PLabel("Press Submit to connect to the Internet and share your results with the Mobile Processing website.\n\n");
  label.font = fontSmall;
  label.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(label);
  y = label.y + label.height;
  
  status = new PLabel("Status: Not connected.\n\n");
  status.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  container.add(status);
  y = status.y + status.height;

  submit = new PButton(LABEL_SUBMIT);
  submit.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  submit.setBounds(4 + ((width - 8 - submit.width) >> 1), y, submit.width, submit.height);
  container.add(submit);
  y = submit.y + submit.height + 4;
  
  back = new PButton(LABEL_BACK);
  back.calculateBounds(4, y, width - 8, Integer.MAX_VALUE);
  back.setBounds(4 + ((width - 8 - submit.width) >> 1), y, submit.width, submit.height);
  container.add(back);
  
  container.initialize();
  container.acceptFocus();  
  
  screen = SCREEN_SHARE;
}
