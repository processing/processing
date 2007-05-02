// Yahoo! Sonar
// by Francis Li
// http://www.francisli.com/
//
// Posted February 3, 2006
//
// Yahoo! Sonar uses the Yahoo! developer apis documented at
// http://developer.yahoo.com/ to query their local search engine.
// Enter a 5-digit United States zip code (i.e. 94114 for my
// neighborhood in San Francisco, CA), then enter some search
// query terms by pressing the "Search" softkey.  The results
// are plotted using an abstract representation- each ring of color
// represents a different distance from the center of the zip code.
// lighter/brighter rings are closer than darker rings.  Use
// UP and DOWN keys to zoom in and out of the rings.  Results are
// represented as dots positioned radially based on their distance
// from the center of the zip code.  Use LEFT and RIGHT to cycle between
// results, and FIRE to toggle showing more/less info.
//
// An obvious next feature would be to use the Phone library to allow
// direct dialing of the phone numbers retrieved.  However, in order to 
// make this example MIDP 1.0 compliant, I have not implemented this
// feature (since the Phone library will only run on MIDP 2.0 phones).
//
import processing.phone.*;
import processing.xml.*;

//// constant parameters for querying the Yahoo! search engine
final String SEARCH_APPID        = "YcqpwRfV34HsjH29JFxu4vwaV6R_vSU2AwUU1m7EKBALeMHusw7FYQMyDPytmc7pjXMGfA--";
final String SEARCH_SERVER       = "local.yahooapis.com";
final String SEARCH_FILE         = "/LocalSearchService/V3/localSearch";
final String SEARCH_PARAMS[]     = { 
  "appid", "query", "zip" };
final int    SEARCH_PARAM_APPID  = 0;
final int    SEARCH_PARAM_QUERY  = 1;
final int    SEARCH_PARAM_ZIP    = 2;
final int    SEARCH_PARAM_COUNT  = 3;
final String SEARCH_VALUES[]     = new String[SEARCH_PARAM_COUNT];

//// constant string tag names for parsing XML results
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

//// holds the digits of the zip code
final char   ZIP[]            = { 
  '0', '0', '0', '0', '0' };

//// softkey command labels
final String SOFTKEY_SEARCH   = "Search";
final String SOFTKEY_BACK     = "Back";

//// zoom/rotate timing values
final int    SELECTION_DELTA_MS  = 750;
final int    DISTANCE_DELTA_MS   = 1000;

//// size of result dot
final int    RESULT_DIAMETER     = 8;

//// speed of "sonar sweep" animation
final int    SWEEP_RATE_MS       = 2000;

//// attribute indexes into attrs array of Result class
final int    INDEX_TITLE       = 0;
final int    INDEX_PHONE       = 1;
final int    INDEX_PROMPT      = 2;
final int    INDEX_ADDRESS     = 3;
final int    INDEX_CITY        = 4;
final int    INDEX_DISTANCE    = 5;
final int    INDEX_COUNT       = 6;
final int    INDEX_COUNT_SHORT = 3;

//// data structure to hold result info
class Result {
  public final String[] attrs = new String[INDEX_COUNT];
  public String         lat;
  public String         lon;
  public int            distance;
}

//// client network library object
PClient net;
PRequest request;
//// xml parser library object
XMLParser parser;
//// phone object for calling numbers
Phone phone;
//// fonts used
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
  net = new PClient(this, SEARCH_SERVER);
  parser = new XMLParser(this);
  phone = new Phone(this);

  fontZip = loadFont(FACE_SYSTEM, STYLE_PLAIN, SIZE_LARGE);
  fontInfo = loadFont(FACE_PROPORTIONAL, STYLE_PLAIN, SIZE_SMALL);

  softkey(SOFTKEY_SEARCH);
  diameter = min(width, height) - 8;
  diameter2 = diameter - diameter / 6;
  distance = 1;
  pdistance = 1;

  framerate(20);

  noStroke();
}

void draw() {  
  //// draw the background and range rings based on current distance
  if (distance < 8) {
    background(255 - distance * 64 + 1);
  } 
  else {
    background(0);
  }
  int dt = millis() - start;
  int d;
  //// animate zooming in/out
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

  //// render current distance results
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

    //// if there are results in a closer ring, put a grey dot in the 
    //// center as an indication
    if (numCloser > 0) {
      stroke(96);
      fill(200);
      ellipse(width / 2, height / 2, RESULT_DIAMETER, RESULT_DIAMETER);
      noStroke();
    }

    //// if there are results in a further ring, render greyed dots on
    //// the circumference to indicate them
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

    //// render results within current distance range
    if (numSelections > 0) {
      stroke(0);
      fill(255);
      //// results are spaced at equal angles from each other on the circle
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

          //// draw info box text
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

      //// animate rotation
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

//// renders the zip code on the screen
void drawZip() {        
  textFont(fontZip);
  textAlign(CENTER);
  fill(0);
  text(new String(ZIP), width / 2, height / 2 + fontZip.baseline + RESULT_DIAMETER / 2);
}

//// renders the "sonar sweep" animation
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
  if (key == '*') {
    if (numSelections > 0) {
      Result r = selections[selection];
      phone.call(r.attrs[INDEX_PHONE]);
    }
  } 
  else {
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
}

void softkeyPressed(String label) {
  if (label.equals(SOFTKEY_SEARCH)) {
    if (!sweep) {
      //// open a text input screen for search terms
      String query = textInput("Search", null, 128);
      if (query != null) {
        //// set up url parameters for query
        SEARCH_VALUES[SEARCH_PARAM_APPID] = SEARCH_APPID;
        SEARCH_VALUES[SEARCH_PARAM_QUERY] = query;
        SEARCH_VALUES[SEARCH_PARAM_ZIP] = new String(ZIP);
        request = net.GET(SEARCH_FILE, SEARCH_PARAMS, SEARCH_VALUES);
        sweep = true;
        sweepstart = millis();
        loop();
      }
    }
  }
}

void libraryEvent(Object library, int event, Object data) {
  if (library == request) {
    if (event == PRequest.EVENT_CONNECTED) {
      parser.start(request);
    }
  } 
  else if (library == parser) {
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
        results[numResults].attrs[INDEX_PROMPT] = "Press * to dial";
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
      request.close();
      request = null;
      redraw();
    }
  }
}

//// determines number of results closer, in range, and further
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
