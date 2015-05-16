import processing.core.PApplet;


// TODO This is code for testing; remove before final release

public class WriteSVG extends PApplet {

  public String sketchRenderer() {
    return "processing.svg.PGraphicsSVG";
  }


  public String sketchOutputPath() {
    return "/Users/fry/Desktop/frame-####.svg";
  }


  public void setup() {
    size(600, 400, "processing.svg.PGraphicsSVG", "/Users/fry/Desktop/frame-####.svg");
//    frameRate(1);
  }

  public void draw() {
    background(random(255));
    stroke(random(255));
    strokeWeight(20);
    line(random(width), random(height), random(width), random(height));
    if (frameCount == 5) exit();
//    exit();
  }

  static public void main(String[] args) {
    PApplet.main(new String[] { "WriteSVG" });
  }
}