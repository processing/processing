/**
 * HTTP Client. 
 * 
 * Starts a network client that connects to a server on port 80,
 * sends an HTTP 1.1 GET request, and prints the results. 
 */
 

import processing.net.*;

Client c;
String data;

void setup() {
  size(200, 200);
  background(50);
  fill(200);
  c = new Client(this, "www.processing.org", 80); // Connect to server on port 80
  c.write("GET / HTTP/1.1\n"); // Use the HTTP "GET" command to ask for a Web page
  c.write("Host: my_domain_name.com\n\n"); // Be polite and say who we are
}

void draw() {
  if (c.available() > 0) { // If there's incoming data from the client...
    data = c.readString(); // ...then grab it and print it
    println(data);
  }
}

