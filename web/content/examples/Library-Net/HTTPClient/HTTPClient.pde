// http client
// by Tom Igoe

// Starts a network client that connects to a server on port 80,
// sends an HTTP 1.1 GET request, and prints the results.

// created March 18, 2005

import processing.net.*;

Client client;

void setup()
{
  size(200, 200);
  noStroke();
  // Open a TCP socket to the host:
  client = new Client(this, "processing.org", 80);

  // Print the IP address of the host:
  println(client.ip());

  // Send the HTTP GET request:
  client.write("GET /index.html HTTP/1.1\n");
  client.write("HOST: processing.org\n\n");
}

void draw()
{
  background(0);
  // Print the results of the GET:
  if (client.available() > 0) {
    int inByte = client.read();
    print((char)inByte);
  } else {
    println("\n\nThat's all, folks!\n");
  }
}

