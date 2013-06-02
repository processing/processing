
IntList lottery;
IntList results;
IntList ticket;

void setup() {
  size(640, 360);
  frameRate(30);
  lottery = new IntList();
  results = new IntList();
  ticket  = new IntList();
  // Let's add some numbers to our list
  for (int i = 0; i < 20; i++) {
    lottery.append(i);
  }

  for (int i = 0; i < 5; i++) {
    int index = int(random(lottery.size()));
    ticket.append(lottery.get(index));
  }
}

void draw() {
  background(51);

  lottery.shuffle();
  
  showList(lottery,16,48);
  showList(results,16,100);
  showList(ticket,16,140);
  
  for (int i = 0; i < results.size(); i++) {
    if (results.get(i) == ticket.get(i)) {
      fill(0,255,0,100);
    } else {
      fill(255,0,0,100);
    }
    ellipse(16+i*32, 140, 24, 24);
  }

  if (frameCount % 30 == 0) {
    if (results.size() < 5) {
    int val = lottery.get(0);
    lottery.remove(0);
    results.append(val);
    } else {
     for (int i = 0; i < results.size(); i++) {
       lottery.append(results.get(i)); 
     }
     results.clear();
    }
  }
}

void showList(IntList list, float x, float y) {
  for (int i = 0; i < list.size(); i++) {
    int val = list.get(i);
    stroke(255);
    noFill();
    ellipse(x+i*32, y, 24, 24);
    textAlign(CENTER);
    fill(255);
    text(val, x+i*32, y+6);
  }
}

