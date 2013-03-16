class Word {
  
  // Store a count for occurences in two different books
  int countDracula;
  int countFranken;
  // Also the total count
  int totalCount;

  // What is the String
  String word;
  
  // Where is it on the screen
  PVector position;
  
  Word(String s) {
    position = new PVector(random(width), random(-height, height*2));
    word = s;
  }
  
  // We will display a word if it appears at least 5 times
  // and only in one of the books
  boolean qualify() {
    if ((countDracula == totalCount || countFranken == totalCount) && totalCount > 5) {
      return true;
    } 
    else {
      return false;
    }
  }
  
  // Increment the count for Dracula
  void incrementDracula() {
    countDracula++;
    totalCount++;
  }
  
  
  // Increment the count for Frankenstein
  void incrementFranken() {
    countFranken++;
    totalCount++;
  }
  
  // The more often it appears, the faster it falls
  void move() {
    float speed = map(totalCount, 5, 25, 0.1, 0.4);
    speed = constrain(speed,0,10);
    position.y += speed;

    if (position.y > height*2) {
      position.y = -height;
    }
  }
  
  
  // Depending on which book it gets a color
  void display() {
    if (countDracula > 0) {
      fill(255);
    } 
    else if (countFranken > 0) {
      fill(0);
    }
    // Its size is also tied to number of occurences
    float fs = map(totalCount,5,25,2,24);
    fs = constrain(fs,2,48);
    textSize(fs);
    textAlign(CENTER);
    text(word, position.x, position.y);
  }
}  

