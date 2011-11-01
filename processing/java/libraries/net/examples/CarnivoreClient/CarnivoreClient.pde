/**
 * Carnivore Client 
 * by Alexander R. Galloway. 
 
 * The Carnivore library for Processing allows the programmer to run a packet 
 * sniffer from within the Processing environment. A packet sniffer is any 
 * application that is able to indiscriminately eavesdrop on data traffic 
 * traveling through a local area network (LAN).
 * 
 * Note: requires Carnivore Library for Processing v2.2 (http://r-s-g.org/carnivore)
 * Windows, first install winpcap (http://winpcap.org)
 * Mac, first open a Terminal and execute this commmand: sudo chmod 777 /dev/bpf*
 * (must be done each time you reboot your mac)
 */


import java.util.Iterator;
import org.rsg.carnivore.*;
import org.rsg.carnivore.net.*;

HashMap nodes = new HashMap();
float startDiameter = 100.0;
float shrinkSpeed = 0.97;
int splitter, x, y;
PFont font;

void setup() 
{
  size(800, 600);
  background(255);
  frameRate(10);
  Log.setDebug(true); // Uncomment this for verbose mode
  CarnivoreP5 c = new CarnivoreP5(this);
  //c.setVolumeLimit(4);
  // Use the "Create Font" tool to add a 12 point font to your sketch,
  // then use its name as the parameter to loadFont().
  font = loadFont("CourierNew-12.vlw");
  textFont(font);
}

void draw() 
{
  background(255);
  drawNodes();
}

// Iterate through each node
synchronized void drawNodes() {
  Iterator it = nodes.keySet().iterator();
  while (it.hasNext()) {
    String ip = (String)it.next();
    float d = float(nodes.get(ip).toString());

    // Use last two IP address bytes for x/y coords
    splitter = ip.lastIndexOf(".");
    y = int(ip.substring(splitter + 1)) * height / 255; // Scale to applet size
    String tmp = ip.substring(0, splitter);
    splitter = tmp.lastIndexOf(".");
    x = int(tmp.substring(splitter + 1)) * width / 255; // Scale to applet size

    // Draw the node
    stroke(0);
    fill(color(100, 200)); // Rim
    ellipse(x, y, d, d); // Node circle
    noStroke();
    fill(color(100, 50)); // Halo
    ellipse(x, y, d + 20, d + 20);

    // Draw the text
    fill(0);
    text(ip, x, y);

    // Shrink the nodes a little
    nodes.put(ip, str(d * shrinkSpeed));
  }
}

// Called each time a new packet arrives
synchronized void packetEvent(CarnivorePacket packet) 
{
  println("[PDE] packetEvent: " + packet);
  // Remember these nodes in our hash map
  nodes.put(packet.receiverAddress.toString(), str(startDiameter));
  nodes.put(packet.senderAddress.toString(), str(startDiameter));
}
