// Code from Visualizing Data, First Edition, Copyright 2008 Ben Fry.
// Based on the GraphLayout example by Sun Microsystems.


int nodeCount;
Node[] nodes = new Node[100];
HashMap nodeTable = new HashMap();

int edgeCount;
Edge[] edges = new Edge[500];

static final color nodeColor   = #F0C070;
static final color selectColor = #FF3030;
static final color fixedColor  = #FF8080;
static final color edgeColor   = #000000;

PFont font;


void setup() {
  size(600, 600);  
  loadData();
  println(edgeCount);
  font = createFont("SansSerif", 10);
}


void loadData() {
  String[] lines = loadStrings("huckfinn.txt");
  
  // Make the text into a single String object
  String line = join(lines, " ");
  
  // Replace -- with an actual em dash
  line = line.replaceAll("--", "\u2014");
  
  // Split into phrases using any of the provided tokens
  String[] phrases = splitTokens(line, ".,;:?!\u2014\"");
  //println(phrases);

  for (int i = 0; i < phrases.length; i++) {
    // Make this phrase lowercase
    String phrase = phrases[i].toLowerCase();
    // Split each phrase into individual words at one or more spaces
    String[] words = splitTokens(phrase, " ");
    for (int w = 0; w < words.length-1; w++) {
      addEdge(words[w], words[w+1]);
    }
  }
}


void addEdge(String fromLabel, String toLabel) {
  // Filter out unnecessary words
  if (ignoreWord(fromLabel) || ignoreWord(toLabel)) return;

  Node from = findNode(fromLabel);
  Node to = findNode(toLabel);
  from.increment();
  to.increment();
  
  for (int i = 0; i < edgeCount; i++) {
    if (edges[i].from == from && edges[i].to == to) {
      edges[i].increment();
      return;
    }
  } 
  
  Edge e = new Edge(from, to);
  e.increment();
  if (edgeCount == edges.length) {
    edges = (Edge[]) expand(edges);
  }
  edges[edgeCount++] = e;
}


String[] ignore = { "a", "of", "the", "i", "it", "you", "and", "to" };

boolean ignoreWord(String what) {
  for (int i = 0; i < ignore.length; i++) {
    if (what.equals(ignore[i])) {
      return true;
    }
  }
  return false;
}


Node findNode(String label) {
  label = label.toLowerCase();
  Node n = (Node) nodeTable.get(label);
  if (n == null) {
    return addNode(label);
  }
  return n;
}


Node addNode(String label) {
  Node n = new Node(label);  
  if (nodeCount == nodes.length) {
    nodes = (Node[]) expand(nodes);
  }
  nodeTable.put(label, n);
  nodes[nodeCount++] = n;  
  return n;
}


void draw() {
  if (record) {
    beginRecord(PDF, "output.pdf");
  }

  background(255);
  textFont(font);  
  smooth();  

  for (int i = 0 ; i < edgeCount ; i++) {
    edges[i].relax();
  }
  for (int i = 0; i < nodeCount; i++) {
    nodes[i].relax();
  }
  for (int i = 0; i < nodeCount; i++) {
    nodes[i].update();
  }
  for (int i = 0 ; i < edgeCount ; i++) {
    edges[i].draw();
  }
  for (int i = 0 ; i < nodeCount ; i++) {
    nodes[i].draw();
  }
  
  if (record) {
    endRecord();
    record = false;
  }
}


boolean record;

void keyPressed() {
  if (key == 'r') {
    record = true;
  }
}


Node selection; 


void mousePressed() {
  // Ignore anything greater than this distance
  float closest = 20;
  for (int i = 0; i < nodeCount; i++) {
    Node n = nodes[i];
    float d = dist(mouseX, mouseY, n.x, n.y);
    if (d < closest) {
      selection = n;
      closest = d;
    }
  }
  if (selection != null) {
    if (mouseButton == LEFT) {
      selection.fixed = true;
    } else if (mouseButton == RIGHT) {
      selection.fixed = false;
    }
  }
}


void mouseDragged() {
  if (selection != null) {
    selection.x = mouseX;
    selection.y = mouseY;
  }
}


void mouseReleased() {
  selection = null;
}
