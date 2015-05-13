## Major destruction of `core` has started for Processing 3.

#### What?
We're removing `Applet` as the base class for `PApplet` and redoing the entire rendering and threading model for Processing sketches.

#### Why?
1. The changes will improve performance--greatly, in some cases--and reduce flicker and quirkiness in others. Using AWT objects like `Applet` (which subclasses `Component`) cause (sometimes major) performance restrictions or other visual glitches like flicker. 
2. The code to mitigate the issues from #1 is very difficult to debug and make work properly across the many platforms we support (Macs, Macs with retina displays, Windows 7, Windows 8, 32- and 64-bit machines, Linux who-knows-what, and so on)
3. The design of `core` is 13 years old, and the graphics features available (OpenGL, VolatileImage, BufferStrategy, etc) have changed drastically since then. I've papered over these changes and done my best to keep performance on-pace so that we don't break a lot of old code (or libraries), but now is the time for a clean break.
4. With the death of applets, keeping the Applet base class is anachronistic. However, we're keeping the name `PApplet` because with any luck, these changes will only require a recompile of any sketch (or library) code. 

#### What else?
1. A new `PSurface` object has been added that acts as the layer between `PApplet` and `PGraphics`. It will handle interaction with the OS (creation of a window, placement on screen, getting events) as well as the animation thread (because OpenGL's animation thread is very different from an AWT animation thread).
2. Many deprecated functions (notably, the pre-2.0 only method registration mechanism used by libraries) are being removed. (Not a final decision.) 
3. Undocumented features (such as the `image` object in `PGraphics`) may disappear and break code from advanced users.
4. We're working to add the ability to span multiple screens in "full screen" mode.
5. In 3.0a2 we introduced a change on OS X to use Apple's "official" full screen mode. With this comes a dorky animation and the inability to span multiple screens. We've rolled that back.

#### But what about...? 
1. One downside is that you'll no longer be able to just drop a Processing sketch into other Java code, because `PApplet` will no longer subclass `Applet` (and therefore, `Component`). This is a huge downside for a tiny number of users. For the majority of users, re-read the "why" section. We'll try to figure out ways to continue embedding in other Java code, however, since we use this in our own work, and even within Processing itself (the Color Selector). 
2. We're still determining how much code we're willing to break due to API changes. Stay tuned.

#### Integration with Java applications
Because `PApplet` was a Java `Component`, it was possible to embed Processing into other Java code. Making it a generic component, however, places limitations on how much performance can be improved, due to the cross-platform mess of Java's outdated (and somewhat unsuspported) AWT. 
In earlier alpha releases, a `getCanvas()` or `getComponent()` method provided a way to get an object to be embedded, but at the current time, it looks like we'll have to move in another direction. At the present time, it looks like it'll be necessary to create a separate `PComponent` or `PCanvas` class that can be used, but it's not clear how that will work. 
This is one of many unfortunate tradeoffs I'm trying to sort through as we try to remove significant barriers to performance caused by the design of Java's AWT, while also supporting features (like embedding) that we've spent so much time supporting. 

#### Offscreen rendering
* createGraphics() will create a buffer that's not resizable. `PGraphics.setSize()` is called in `PApplet.makeGraphics()`, and that's the end of the story. No `Surface.setSize()` calls are involved as in a normal rendering situation.

#### Retina/HiDPI/2x drawing and displays
* Documentation changes [here](https://github.com/processing/processing-docs/issues/170)

#### The Event Dispatch Thread
The current source starts putting AWT (and Swing, if any) calls on the [EDT](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html), per Oracle's statements in Java's documentation. Actual rendering in the default renderer happens off the EDT, but the EDT is used to blit the image to the screen (or resize windows, etc). Looking for more consistent cross-platform results by doing this.

## The Mess

The rest of this document are my notes while I'm making changes.

#### Modes on startup
1. nothing displayed (isDisplayable returns false, i.e. PDF)
2. sketch in a window (most usage, from the PDE)
3. present (sketch is smaller, color the rest of the screen; does not work with multiple screens at once)
4. full screen (sketch same as screen size)
5. all screens (sketch spans all screens)

#### alternate version
1. pde sketch
2. standalone sketch (exported)
3. running from Eclipse
4. size command?
5. renderer change?
6. renderer that draws to screen (Java2D) or not (PDF)
7. OpenGL or not

resize events: 
Frame > Canvas > PGraphics > PApplet
user-driven Frame resize events follow that order
all resize events happen on the surface
applet sends message to the surface, which notifies the renderer itself
resize of component is handled in thread-safe way from PSurface

PApplet.size() calls setSize() in PSurface, and in surface:
  - Mode 2: resize the frame, which will resize the canvas, etc
  - Mode 3: resizes and places of Canvas
  - Mode 4 and 5: no resize, always full display


PSurfaceAWT is a simple Canvas-based surface that blits a BufferedImage
provides most compatibility with previous renderers
another PSurfaceAWT variant could allow direct rendering to the canvas (no loadPixels) through the whole strategy setup


#### To Document
- sketchRenderer() is required
- the renderer class/package is used to call a static method on the PGraphics that returns the class type for the rendering surface

inside main, will know the screen that's being used for the app

#### Questions/To Do
- change size() command to check through renderer constants and give better error message when using one of the built-in renderers
- bad idea, or worst idea, to have 'surface' var in PGraphics?
- move getFontRenderContext(font) to PApplet? surface? elsewhere? 
_ do we need canDraw() anymore?
- Can we remove while() loop that waits until defaultSize is set false?
- Does init() need to go away, because it's not going to work in any other setting? Because a surface must first be created, the init() method on its own will be a mess.
- If Frame/Canvas are moved to another display, will the display object update?
- Does sun.awt.noerasebackground (in PApplet) help flicker?
- The size() JavaDoc in PApplet is comically old
- Does createFont() need to run through PGraphics?
- Need to fix sketch placement issues (default size with long setup(), etc) Actually, the default size with long setup() is probably that defaultSize is set false, but the initial render doesn't finish before width/height are set to something useful.
- selectInput(), selectOutput(), selectFolder() now passing 'null' as parent Window. Should just leave them un-anchored, but need to test to make this doesn't break anything else.
- do we need sketchOutputPath() and sketchOutputStream()?

#### Removed functions (not final, just notes)
param() 
the old awt event handlers (they were a warning in 2.x)
PGraphics.requestDraw(), because renderers now have their own threads
PGraphics.setFrameRate removed, added to PSurface
requires 1.7.. uses revalidate() method
destroy() (only called by applet? calls dispose())

