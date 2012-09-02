// Re-implementing java.awt.Rectangle
// so JS mode works

class Rectangle {
   int x;
   int y;
   int width;
   int height;
   
   Rectangle(int x_, int y_, int w, int h) {
     x = x_;
     y = y_;
     width = w;
     height = h;
   }
   
   boolean contains(int px, int py) {
     return (px > x && px < x + width  && py > y && py < y + height);
   }
   
}
