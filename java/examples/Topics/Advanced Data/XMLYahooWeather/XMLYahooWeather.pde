/**
 * Loading XML Data
 * by Daniel Shiffman.  
 * 
 * This example demonstrates how to use loadXML()
 * to retrieve data from an XML document via a URL
 */

// We're going to store the temperature
int temperature = 0;
// We're going to store text about the weather
String weather = "";

// The zip code we'll check for
String zip = "10003";

void setup() {
  size(200, 200);

  // The URL for the XML document
  String url = "http://xml.weather.yahoo.com/forecastrss?p=" + zip;
  
  // Load the XML document
  XML xml = loadXML(url);

  // Grab the element we want
  XML forecast = xml.getChild("channel").getChild("item").getChild("yweather:forecast");
  
  // Get the attributes we want
  temperature = forecast.getInt("high");
  weather = forecast.getString("text");
}

void draw() {
  background(255);
  fill(0);

  // Display all the stuff we want to display
  text("Zip code: " + zip, 10, 160);
  text("Today's high: " + temperature, 10, 40);
  text("Forecast: " + weather, 10, 90);

  // Draw a little thermometer based on the temperature
  stroke(0);
  fill(175);
  rect(10, 50, temperature*2, 20);
}
