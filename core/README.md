## Major destruction of 'core' has started for Processing 3.

#### What?
We're removing Applet as the base class for PApplet and redoing the entire rendering and threading model for Processing sketches.

#### Why?
1. The changes will improve performance--greatly, in some cases--and reduce flicker and quirkiness in others. Using AWT objects like (Applet subclasses Component) cause (sometimes major) performance restrictions or other visual glitches like flicker.
2. The design of 'core' is 13 years old, and the graphics models available (OpenGL, VolatileImage, BufferStrategy, etc) have changed drastically since then. I've papered over these changes and done my best to keep performance on-pace so that we don't break a lot of old code (or libraries), but now is the time for a clean break.
3. With the death of applets, keeping it is anachronistic.

#### What else?
1. A new "Surface" object has been added that acts as the layer between PApplet and PGraphics. It will handle interaction with the OS (creation of a window, placement on screen, getting events) as well as the animation thread (because OpenGL's animation thread is very different from an AWT animation thread).
2. Many deprecated functions (notably, the pre-2.0 only method registration mechanism used by libraries) are being removed. (Not a final decision.) 
3. Undocumented features (such as the 'image' object in PGraphics) may disappear and break code from advanced users.

#### But what about...? 
1. One downside is that you'll no longer be able to just drop a Processing sketch into other Java code, because PApplet will no longer subclass Applet (and therefore, Component). This is a huge downside for a tiny number of users. For the majority of users, re-read the "why" section.
2. We're still determining how much code we're willing to break due to API changes. Stay tuned.

## The Mess

The rest of this document are my notes while I'm making changes.

#### Modes on startup
1. nothing displayed (isDisplayable returns false, i.e. PDF)
2. sketch in a window (most usage, from the PDE)
3. present (sketch is smaller, color the rest of the screen)
4. full screen
5. all screens

#### To Document
sketchRenderer() is required
the renderer class/package is used to call a static method on the PGraphics
  that returns the class type for the rendering surface

inside main, will know the screen that's being used for the app

#### Removed functions (not final, just notes)
param() 
the old awt event handlers (they were a warning in 2.x)
PGraphics.requestDraw(), because renderers now have their own threads
PGraphics.setFrameRate removed, added to PSurface
requires 1.7.. uses revalidate() method
destroy() (only called by applet? calls dispose())

