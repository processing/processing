// Class for animating a sequence of GIFs

class AniSprite {
  PImage[] ani;
  int frame;
  int numFrames;
  
  AniSprite(String imageName, int frameCount) {
    numFrames = frameCount;
    ani = new PImage[numFrames];
    loadImages(imageName);
  }

  void loadImages(String name) {
    for(int i=0; i<numFrames; i++) {
      String imageName = name + ((i < 10) ? "0" : "") + i + ".gif";
      ani[i] = loadImage(imageName);
    }
  }

  void display(float xpos, float ypos) {
    frame = (frame+1)%numFrames;
    image(ani[frame], xpos, ypos);
  }
  
  int getWidth() {
    return ani[0].width;
  }
}
