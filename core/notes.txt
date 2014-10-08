
1. nothing displayed (isDisplayable returns false, i.e. PDF)
2. sketch in a window (most usage, from the PDE)
3. present (sketch is smaller, color the rest of the screen)
4. full screen
5. all screens

sketchRenderer() is required
the renderer class/package is used to call a static method on the PGraphics
  that returns the class type for the rendering surface

inside main, will know the screen that's being used for the app

removed functions
param() 
the old awt event handlers (they were a warning in 2.x)
PGraphics.requestDraw(), because renderers now have their own threads
PGraphics.setFrameRate removed, added to PSurface
requires 1.7.. uses revalidate() method
destroy() (only called by applet? calls dispose())

