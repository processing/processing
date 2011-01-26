/*
This book is here to help you get your job done. In general, you may use the 
code in this book in your programs and documentation. You do not need to contact 
us for permission unless you’re reproducing a significant portion of the code. 
For example, writing a program that uses several chunks of code from this book 
does not require permission. Selling or distributing a CD-ROM of examples from 
O’Reilly books does require permission. Answering a question by citing this book 
and quoting example code does not require permission. Incorporating a significant
amount of example code from this book into your product’s documentation does 
require permission.

We appreciate, but do not require, attribution. An attribution usually includes
the title, author, publisher, and ISBN. For example: “Visualizing Data, First 
Edition by Ben Fry. Copyright 2008 Ben Fry, 9780596514556.”

If you feel your use of code examples falls outside fair use or the permission
given above, feel free to contact us at permissions@oreilly.com.
*/
import java.util.regex.*;

int teamCount = 30;
String[] teamNames; 
String[] teamCodes;
HashMap teamIndices;
  
static final int ROW_HEIGHT = 23;
static final float HALF_ROW_HEIGHT = ROW_HEIGHT / 2.0f;

static final int SIDE_PADDING = 30;
static final int TOP_PADDING = 40;

SalaryList salaries;
StandingsList standings;
  
StandingsList[] season;
Integrator[] standingsPosition;

PImage[] logos;
float logoWidth;
float logoHeight;

PFont font;

  
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  

String firstDateStamp = "20070401";
String lastDateStamp = "20070930";
String todayDateStamp;

static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

// The number of days in the entire season. 
int dateCount;
// The current date being shown.
int dateIndex;
// Don't show the first 10 days, they're too erratic. 
int minDateIndex = 10;  
// The last day of the season, or yesterday, if the season is ongoing.
// This is the maximum date that can be viewed.
int maxDateIndex;

// This format makes "20070704" from the date July 4, 2007.
DateFormat stampFormat = new SimpleDateFormat("yyyyMMdd");
// This format makes "4 July 2007" from the same.
DateFormat prettyFormat = new SimpleDateFormat("d MMMM yyyy");

// All dates for the season formatted with stampFormat.
String[] dateStamp;
// All dates in the season formatted with prettyFormat.
String[] datePretty;

void setupDates() {
  try {
    Date firstDate = stampFormat.parse(firstDateStamp);
    long firstDateMillis = firstDate.getTime();
    Date lastDate = stampFormat.parse(lastDateStamp);
    long lastDateMillis = lastDate.getTime();

    // Calculate number of days by dividing the total milliseconds 
    // between the first and last dates by the number of milliseconds per day
    dateCount = (int) 
      ((lastDateMillis - firstDateMillis) / MILLIS_PER_DAY) + 1;      
    maxDateIndex = dateCount;
    dateStamp = new String[dateCount];
    datePretty = new String[dateCount];

    todayDateStamp = year() + nf(month(), 2) + nf(day(), 2);
    // Another option to do this, but more code
    //Date today = new Date(); 
    //String todayDateStamp = stampFormat.format(today);
      
    for (int i = 0; i < dateCount; i++) {
      Date date = new Date(firstDateMillis + MILLIS_PER_DAY*i);
      datePretty[i] = prettyFormat.format(date);
      dateStamp[i] = stampFormat.format(date);
      // If this value for 'date' is equal to today, then set the previous 
      // day as the maximum viewable date, because it means the season is 
      // still ongoing. The previous day is used because unless it is late 
      // in the evening, the updated numbers for the day will be unavailable 
      // or incomplete.
      if (dateStamp[i].equals(todayDateStamp)) {
        maxDateIndex = i-1;
      }
    }
  } catch (ParseException e) {
    die("Problem while setting up dates", e);
  }
}  


// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


public void setup() {
  size(480, 750);
    
  setupTeams();
  setupDates();
  setupSalaries();
  // Load the standings after the salaries, because salary 
  // will be used as the tie-breaker when sorting.
  setupStandings();
  setupRanking();
  setupLogos();
    
  font = createFont("Georgia", 12);
  textFont(font);

  frameRate(15);
  // Use today as the current day
  setDate(maxDateIndex);
}
  

void setupTeams() {
  String[] lines = loadStrings("teams.tsv");
    
  teamCount = lines.length;
  teamCodes = new String[teamCount];
  teamNames = new String[teamCount];
  teamIndices = new HashMap();
    
  for (int i = 0; i < teamCount; i++) {
    String[] pieces = split(lines[i], TAB);
    teamCodes[i] = pieces[0];
    teamNames[i] = pieces[1];
    teamIndices.put(teamCodes[i], new Integer(i));
  }
}
  
  
int teamIndex(String teamCode) {
  Integer index = (Integer) teamIndices.get(teamCode);
  return index.intValue();
}


void setupSalaries() {
  String[] lines = loadStrings("salaries.tsv");
  salaries = new SalaryList(lines);
}


/*
void setupStandings() {
  season = new StandingsList[maxDateIndex + 1];
  for (int i = minDateIndex; i <= maxDateIndex; i++) {
    String[] lines = acquireStandings(dateStamp[i]);
    season[i] = new StandingsList(lines);
  }
}
*/
  

void setupStandings() {
  String[] lines = loadStrings("http://benfry.com/writing/salaryper/mlb.cgi");
  int dataCount = lines.length / teamCount;
  int expectedCount = (maxDateIndex - minDateIndex) + 1;
  if (dataCount < expectedCount) {
    println("Found " + dataCount + " entries in the data file, " +
            "but was expecting " + expectedCount + " entries.");
    maxDateIndex = minDateIndex + dataCount - 1;
  }
  season = new StandingsList[maxDateIndex + 1];
  for (int i = 0; i < dataCount; i++) {
    String[] portion = subset(lines, i*teamCount, teamCount);
    season[i+minDateIndex] = new StandingsList(portion);
  }
}
  

void setupRanking() {
  standingsPosition = new Integrator[teamCount];
  for (int i = 0; i < teamCodes.length; i++) {
    standingsPosition[i] = new Integrator(i);
  }
}
  

void setupLogos() {
  logos = new PImage[teamCount];
  for (int i = 0; i < teamCount; i++) {
    logos[i] = loadImage("small/" + teamCodes[i] + ".gif");
  }
  logoWidth = logos[0].width / 2.0f;
  logoHeight = logos[0].height / 2.0f;
}
  
  
public void draw() {
  background(255);
  smooth();

  drawDateSelector();

  translate(SIDE_PADDING, TOP_PADDING);
  
  boolean updated = false;
  for (int i = 0; i < teamCount; i++) {
    if (standingsPosition[i].update()) {
      updated = true;
    }
  }
  if (!updated) {
    noLoop();
  }

  for (int i = 0; i < teamCount; i++) {
    //float standingsY = standings.getRank(i)*ROW_HEIGHT + HALF_ROW_HEIGHT;
    float standingsY = standingsPosition[i].value * ROW_HEIGHT + HALF_ROW_HEIGHT;

    image(logos[i], 0, standingsY - logoHeight/2, logoWidth, logoHeight);
      
    textAlign(LEFT, CENTER);
    text(teamNames[i], 28, standingsY);

    textAlign(RIGHT, CENTER);
    fill(128);
    text(standings.getTitle(i), 150, standingsY);

    float weight = map(salaries.getValue(i), 
                       salaries.getMinValue(), salaries.getMaxValue(), 
                       0.25f, 6);
    strokeWeight(weight);
      
    float salaryY = salaries.getRank(i)*ROW_HEIGHT + HALF_ROW_HEIGHT;
    if (salaryY >= standingsY) {
      stroke(33, 85, 156);  // Blue for positive (or equal) difference.
    } else {
      stroke(206, 0, 82);   // Red for wasting money.
    }
      
    line(160, standingsY, 325, salaryY);

    fill(128);
    textAlign(LEFT, CENTER);
    text(salaries.getTitle(i), 335, salaryY);
  }
}

  
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


int dateSelectorX;
int dateSelectorY = 30;

// Draw a series of lines for selecting the date
void drawDateSelector() {
  dateSelectorX = (width - dateCount*2) / 2;

  strokeWeight(1);
  for (int i = 0; i < dateCount; i++) {
    int x = dateSelectorX + i*2;

    // If this is the currently selected date, draw it differently
    if (i == dateIndex) {
      stroke(0);
      line(x, 0, x, 13);
      textAlign(CENTER, TOP);
      text(datePretty[dateIndex], x, 15);

    } else {
      // If this is a viewable date, make the line darker
      if ((i >= minDateIndex) && (i <= maxDateIndex)) {
        stroke(128);  // Viewable date
      } else {
        stroke(204);  // Not a viewable date
      }
      line(x, 0, x, 7);
    }
  }
}


void setDate(int index) {
  dateIndex = index;
  standings = season[dateIndex];

  for (int i = 0; i < teamCount; i++) {
    standingsPosition[i].target(standings.getRank(i));
  }
  // Re-enable the animation loop
  loop();
}


void mousePressed() {
  handleMouse();
}
  
void mouseDragged() {
  handleMouse();
}
  
void handleMouse() {
  if (mouseY < dateSelectorY) {
    int date = (mouseX - dateSelectorX) / 2;
    setDate(constrain(date, minDateIndex, maxDateIndex));
  }
}


void keyPressed() {
  if (key == CODED) {
    if (keyCode == LEFT) {
      int newDate = max(dateIndex - 1, minDateIndex);
      setDate(newDate);

    } else if (keyCode == RIGHT) {
      int newDate = min(dateIndex + 1, maxDateIndex);
      setDate(newDate);
    }
  }
}


// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


/*
String[] acquireStandings(String stamp) {
  int year = int(stamp.substring(0, 4));
  int month = int(stamp.substring(4, 6));
  int day = int(stamp.substring(6, 8));
  return acquireStandings(year, month, day);
}
  

String[] acquireStandings(int year, int month, int day) {
  String filename = year + nf(month, 2) + nf(day, 2) + ".tsv";
  String path = dataPath(filename);
  File file = new File(path);
  if (!file.exists()) {
    println("Downloading standings file " + filename);
    PrintWriter writer = createWriter(path);

    String base = "http://mlb.mlb.com/components/game" + 
      "/year_" + year + "/month_" + nf(month, 2) + "/day_" + nf(day, 2) + "/";

    // American League (AL)
    parseWinLoss(base + "standings_rs_ale.js", writer);
    parseWinLoss(base + "standings_rs_alc.js", writer);
    parseWinLoss(base + "standings_rs_alw.js", writer);

    // National League (NL)
    parseWinLoss(base + "standings_rs_nle.js", writer);
    parseWinLoss(base + "standings_rs_nlc.js", writer);
    parseWinLoss(base + "standings_rs_nlw.js", writer);

    writer.flush();
    writer.close();
  }
  return loadStrings(filename);
}

  
void parseWinLoss(String filename, PrintWriter writer) {
  String[] lines = loadStrings(filename);
  Pattern p = Pattern.compile("\\s+([\\w\\d]+):\\s'(.*)',?");

  String teamCode = "";
  int wins = 0;
  int losses = 0;

  for (int i = 0; i < lines.length; i++) {
    Matcher m = p.matcher(lines[i]);

    if (m.matches()) {
      String attr = m.group(1);
      String value = m.group(2);

      if (attr.equals("code")) {
        teamCode = value;
      } else if (attr.equals("w")) {
        wins = parseInt(value);
      } else if (attr.equals("l")) {
        losses = parseInt(value);
      }

    } else {
      if (lines[i].startsWith("}")) {
        // this is the end of a group, write these values
        //println(team + " " + wins + "-" + losses);
        //set(teamIndex(teamCode), wins, losses);
        writer.println(teamCode + TAB + wins + TAB + losses);
      }
    }
  }
}
*/
  
  
//. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
class SalaryList extends RankedList {
    
  SalaryList(String[] lines) {
    super(teamCount, false);
    
    for (int i = 0; i < teamCount; i++) {
      String pieces[] = split(lines[i], TAB);
        
      // First column is the team 2-3 digit team code.
      int index = teamIndex(pieces[0]);
        
      // Second column is the salary as a number. 
      value[index] = parseInt(pieces[1]);
        
      // Make the title in the format $NN,NNN,NNN
      int salary = (int) value[index];
      title[index] = "$" + nfc(salary);
    }
    update();
  }
}

  
// . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


class StandingsList extends RankedList {
    
  StandingsList(String[] lines) {
    super(teamCount, false);
      
    for (int i = 0; i < teamCount; i++) {
      String[] pieces = split(lines[i], TAB);
      int index = teamIndex(pieces[0]);
      int wins = parseInt(pieces[1]);
      int losses = parseInt(pieces[2]);
        
      value[index] = (float) wins / (float) (wins+losses);
      title[index] = wins + "\u2013" + losses;
    }
    update();
  }

  float compare(int a, int b) {
    // First compare based on the record of both teams
    float amt = super.compare(a, b);
    // If the record is not identical, return the difference
    if (amt != 0) return amt;

    // If records are equal, use salary as tie-breaker. 
    // In this case, a and b are switched, because a higher
    // salary is a negative thing, unlike the values above.
    return salaries.compare(a, b);
  }
}
