import processing.xml.*;

final String SEARCH_APPID        = "mobileprocessing";
final String SEARCH_SERVER       = "api.local.yahoo.com";
final String SEARCH_FILE         = "/LocalSearchService/V2/localSearch";
final String SEARCH_PARAMS[]     = { 
  "appid", "query", "zip" };
final int    SEARCH_PARAM_APPID  = 0;
final int    SEARCH_PARAM_QUERY  = 1;
final int    SEARCH_PARAM_ZIP    = 2;
final int    SEARCH_PARAM_COUNT  = 3;
final String SEARCH_VALUES[]     = new String[SEARCH_PARAM_COUNT];

final String TAG_RESULTSET    = "ResultSet";
final String ATTR_RESULTS     = "totalResultsReturned";
final String TAG_RESULT       = "Result";
final String TAG_TITLE        = "Title";
final String TAG_ADDRESS      = "Address";
final String TAG_CITY         = "City";
final String TAG_PHONE        = "Phone";
final String TAG_LAT          = "Latitude";
final String TAG_LON          = "Longitude";
final String TAG_DISTANCE     = "Distance";

final char   ZIP[]            = { 
  '0', '0', '0', '0', '0' };

final String SOFTKEY_SEARCH   = "Search";
final String SOFTKEY_BACK     = "Back";

final int    SELECTION_DELTA_MS  = 750;
final int    DISTANCE_DELTA_MS   = 1000;
final int    RESULT_DIAMETER     = 8;

final int    SWEEP_RATE_MS       = 2000;

final int    INDEX_TITLE       = 0;
final int    INDEX_PHONE       = 1;
final int    INDEX_ADDRESS     = 2;
final int    INDEX_CITY        = 3;
final int    INDEX_DISTANCE    = 4;
final int    INDEX_COUNT       = 5;
final int    INDEX_COUNT_SHORT = 2;

class Result {
  public final String[] attrs = new String[INDEX_COUNT];
  public String         lat;
  public String         lon;
  public int            distance;
}

PClient net;
XMLParser parser;
PFont fontZip;
PFont fontInfo;

int diameter;
int diameter2;

int zipIndex;

int pdistance;
int distance;

int start;

Result selections[];
int numSelections;
int numFurther;
int numCloser;
int rotate_fp;
int selection;
int pselection;

boolean fulldetail;

String lastTag;
Result results[];
int numResults;

boolean sweep;
int sweepa_fp;
int sweepstart;

void setup() {
  net = new PClient(SEARCH_SERVER);
  parser = new XMLParser(this);

  fontZip = loadFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_LARGE);
  fontInfo = loadFont(FACE_PROPORTIONAL, STYLE_PLAIN, SIZE_SMALL);

  softkey(SOFTKEY_SEARCH);
  diameter = min(width, height) - 8;
  diameter2 = diameter - diameter / 6;
  distance = 1;
  pdistance = 1;

  framerate(30);

  noStroke();
}

void draw() {  
  if (distance < 8) {
    background(255 - distance * 64 + 1);
  } 
  else {
    background(0);
  }
  int dt = millis() - start;
  int d;
  if (distance != pdistance) {
    if (dt < DISTANCE_DELTA_MS) {
      if (distance > pdistance) {
        d = 2 * diameter - diameter * dt / DISTANCE_DELTA_MS;
        fill(255 - pdistance * 64 + 1);
        ellipse(width / 2, height / 2, d, d);

        d = diameter - (diameter - diameter2) * dt / DISTANCE_DELTA_MS;
        fill(255 - pdistance * 32 + 1);
        ellipse(width / 2, height / 2, d, d);

        d = diameter2 - diameter2 * dt / DISTANCE_DELTA_MS;
        fill(255 - pdistance * 16 + 1);
        ellipse(width / 2, height / 2, d, d);
      } 
      else {
        if (pdistance < 8) {
          background(255 - pdistance * 64 + 1);
        } 
        else {
          background(1);
        }

        d = diameter + diameter * dt / DISTANCE_DELTA_MS;
        fill(255 - pdistance * 32 + 1);
        ellipse(width / 2, height / 2, d, d);

        d = diameter2 + (diameter - diameter2) * dt / DISTANCE_DELTA_MS;
        fill(255 - pdistance * 16 + 1);
        ellipse(width / 2, height / 2, d, d);

        d = diameter2 * dt / DISTANCE_DELTA_MS;
        fill(255 - distance * 16 + 1);
        ellipse(width / 2, height / 2, d, d);
      }
      drawSweep();
      drawZip();
    } 
    else {
      pdistance = distance;
      checkSelections();
    }
  } 

  if (distance == pdistance) {
    if (distance < 8) {
      fill(255 - distance * 32 + 1);
      ellipse(width / 2, height / 2, diameter, diameter);
    }
    fill(255 - distance * 16 + 1);
    ellipse(width / 2, height / 2, diameter2, diameter2);

    stroke(0);
    point(width / 2, height / 2);
    noStroke();

    drawSweep();
    drawZip();

    if (numCloser > 0) {
      stroke(96);
      fill(200);
      ellipse(width / 2, height / 2, RESULT_DIAMETER, RESULT_DIAMETER);
      noStroke();
    }

    if (numFurther > 0) {
      stroke(96);
      fill(200);
      int a_fp = PI / 2;
      int da_fp = TWO_PI / numFurther;
      for (int i = 0; i < numFurther; i++) {
        ellipse(width / 2 + fptoi(diameter / 2 * cos(a_fp)),
        height / 2 - fptoi(diameter / 2 * sin(a_fp)),
        RESULT_DIAMETER,
        RESULT_DIAMETER);
        a_fp += da_fp;
      }      
      noStroke();
    }

    if (numSelections > 0) {
      stroke(0);
      fill(255);
      int da_fp = TWO_PI / numSelections;
      int a_fp = PI / 2 + rotate_fp + da_fp * (numSelections - 1);
      Result r;
      int diff, x, y;
      for (int i = numSelections - 1; i >= 0; i--) {
        r = selections[(selection + i) % numSelections];
        diff = r.distance - distance / 2 * 100;        
        x = width / 2 + fptoi(diff * diameter2 / 2 * cos(a_fp) / ((distance - distance / 2) * 100));
        y = height / 2 - fptoi(diff * diameter2 / 2 * sin(a_fp) / ((distance - distance / 2) * 100));
        if (i == 0) {
          noStroke();        
          fill(0);
          ellipse(x, y, RESULT_DIAMETER + 4, RESULT_DIAMETER + 4);
          fill(255);
          ellipse(x, y, RESULT_DIAMETER, RESULT_DIAMETER);

          stroke(0);
          textFont(fontInfo);
          int rectWidth = 0;
          int count = fulldetail ? INDEX_COUNT : INDEX_COUNT_SHORT;
          int lines = 0;
          for (int j = 0; j < count; j++) {
            if (r.attrs[j] != null) {
              rectWidth = max(rectWidth, textWidth(r.attrs[j]));
              lines++;
            }
          }
          rectWidth += 4;
          rect(x - rectWidth / 2, y + RESULT_DIAMETER, rectWidth, fontInfo.height * lines);          

          textAlign(LEFT);
          fill(0);
          int lineX = x - rectWidth / 2 + 2;
          int lineY = y + RESULT_DIAMETER + fontInfo.baseline + 1;
          for (int j = 0; j < count; j++) {
            if (r.attrs[j] != null) {
              text(r.attrs[j], lineX, lineY);
              lineY += fontInfo.height;
            }
          }
          fill(255);
        } 
        else {
          ellipse(x, y, RESULT_DIAMETER, RESULT_DIAMETER);
        }
        a_fp -= da_fp;
      }
      noStroke();

      if (rotate_fp != 0) {  
        int new_rotate_fp = dt * TWO_PI / SELECTION_DELTA_MS / numSelections;
        if (rotate_fp < 0) {
          new_rotate_fp = rotate_fp + new_rotate_fp;
          if (new_rotate_fp > 0) {
            new_rotate_fp = 0;
          }
        } 
        else {
          new_rotate_fp = rotate_fp - new_rotate_fp;
          if (new_rotate_fp < 0) {
            new_rotate_fp = 0;
          }
        }    
        rotate_fp = new_rotate_fp;
        start = millis();
      } 
      else if (!sweep) {
        noLoop();
      }
    } 
    else if (!sweep) {  
      noLoop();
    }
  }
}

void drawZip() {        
  textFont(fontZip);
  textAlign(CENTER);
  fill(0);
  text(new String(ZIP), width / 2, height / 2 + fontZip.baseline + RESULT_DIAMETER / 2);
}

void drawSweep() {
  if (sweep) {
    sweepa_fp = ((sweepa_fp - (millis() - sweepstart) * TWO_PI / SWEEP_RATE_MS) + TWO_PI) % TWO_PI;
    sweepstart = millis();
    for (int i = 9; i >= 0; i--) {
      stroke(255 - i * 5);
      line(width / 2, height / 2, width / 2 + fptoi(diameter / 2 * cos(sweepa_fp + i * TWO_PI / 360)),
      height / 2 - fptoi(diameter / 2 * sin(sweepa_fp + i * TWO_PI / 360)));
    }
    noStroke();
  }
}

void keyPressed() {
  switch (keyCode) {
  case UP:
    pdistance = distance;
    distance = max(1, distance / 2);
    if (distance != pdistance) {
      start = millis();
      selection = 0;
      loop();
    }
    break;
  case DOWN:
    pdistance = distance;
    distance = min(8, distance * 2);
    if (distance != pdistance) {
      start = millis();
      selection = 0;
      loop();
    }
    break;
  case LEFT:
    if (numSelections > 1) {
      pselection = selection;
      selection = (selection - 1 + numSelections) % numSelections;
      if (selection != pselection) {
        start = millis();
        rotate_fp -= TWO_PI / numSelections;
        loop();
      }
    }
    break;
  case RIGHT:
    if (numSelections > 1) {
      pselection = selection;
      selection = (selection + 1) % numSelections;
      if (selection != pselection) {
        start = millis();
        rotate_fp += TWO_PI / numSelections;
        loop();
      }
    }
    break;
  case FIRE:
    fulldetail = !fulldetail;
    redraw();
    break;
  default:
    if ((key >= '0') && (key <= '9')) {
      ZIP[zipIndex] = key;
      zipIndex = (zipIndex + 1) % 5;
      redraw();
    }
    break;      
  }  
}

void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_SEARCH)) {
    if (!sweep) {
      String query = textInput("Search", null, 128);
      if (query != null) {
        SEARCH_VALUES[SEARCH_PARAM_APPID] = SEARCH_APPID;
        SEARCH_VALUES[SEARCH_PARAM_QUERY] = query;
        SEARCH_VALUES[SEARCH_PARAM_ZIP] = new String(ZIP);
        if (net.GET(SEARCH_FILE, SEARCH_PARAMS, SEARCH_VALUES)) {
          parser.start(net);
          sweep = true;
          sweepstart = millis();
          loop();
        }
      }
    }
  }
}

void libraryEvent(Object library, int event, Object data) {
  if (library == parser) {
    if (event == XMLParser.EVENT_TAG_START) {      
      lastTag = (String) data;
      if (data.equals(TAG_RESULTSET)) {
        numResults = int(parser.attribute(ATTR_RESULTS));
        println("num results: " + numResults);
        results = new Result[numResults];
        selections = new Result[numResults];
        numResults = -1;
        numFurther = 0;
        numSelections = 0;
        numCloser = 0;
      } 
      else if (data.equals(TAG_RESULT)) {
        numResults++;
        results[numResults] = new Result();
      }
    } 
    else if (event == XMLParser.EVENT_TAG_END) {        
      if (data.equals(TAG_RESULT)) {
        checkSelections();
        redraw();    
      }
    } 
    else if (event == XMLParser.EVENT_TEXT) {
      if (lastTag.equals(TAG_DISTANCE)) {
        String value = (String) data;
        int index = value.indexOf(".");
        if (index >= 0) {
          results[numResults].distance = int(value.substring(index + 1));
          value = value.substring(0, index);
        }
        results[numResults].distance += 100 * int(value);
        results[numResults].attrs[INDEX_DISTANCE] = data + " miles";
        println("Distance: " + results[numResults].distance);
      } 
      else if (lastTag.equals(TAG_TITLE)) {
        results[numResults].attrs[INDEX_TITLE] = (String) data;
        println("Title: " + data);
      } 
      else if (lastTag.equals(TAG_ADDRESS)) {
        results[numResults].attrs[INDEX_ADDRESS] = (String) data;
      } 
      else if (lastTag.equals(TAG_CITY)) {
        results[numResults].attrs[INDEX_CITY] = (String) data;
      } 
      else if (lastTag.equals(TAG_PHONE)) {
        results[numResults].attrs[INDEX_PHONE] = (String) data;
      } 
      else if (lastTag.equals(TAG_LAT)) {
        results[numResults].lat = (String) data;
      } 
      else if (lastTag.equals(TAG_LON)) {
        results[numResults].lon = (String) data;
      }
    } 
    else if (event == XMLParser.EVENT_DOCUMENT_END) {
      numResults++;
      checkSelections();
      sweep = false;
      redraw();
    }
  }
}

void checkSelections() {
  numSelections = 0;
  numFurther = 0;
  numCloser = 0;
  for (int i = 0; i < numResults; i++) {
    if ((results[i].distance < (distance * 100)) &&
      (results[i].distance >= (distance / 2 * 100))) {
      selections[numSelections] = results[i];
      numSelections++;
    } 
    else if (results[i].distance >= (distance * 100)) {
      numFurther++;
    }
    else if (results[i].distance < (distance / 2 * 100)) {
      numCloser++;
    }
  }
}
