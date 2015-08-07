## There are significant changes to `core` in Processing 3.


#### What?
We've removed `Applet` as the base class for `PApplet` and have redone the entire rendering and threading model for Processing sketches.


#### Why?
1. The changes improve performance--greatly, in some cases--and reduce flicker and quirkiness in others. Using AWT objects like `Applet` (which subclasses `Component`) cause (sometimes major) performance restrictions or other visual glitches like flicker. 
2. Without making these changes, the code to mitigate the issues from #1 is very difficult to debug and make work properly across the many platforms we support: Macs, Macs with retina displays, Windows 7, Windows 8, 32- and 64-bit machines, Linux who-knows-what, and so on.
3. The design of `core` is 13 years old, and the graphics features available (OpenGL, `VolatileImage`, `BufferStrategy`, etc) have changed drastically since then. I've papered over these changes and done my best to keep performance on-pace so that we don't break a lot of old code (or libraries), but now is the time for a clean break.
4. With the death of applets, keeping the `Applet` base class is anachronistic (in addition to hindering performance). However, we're keeping the name `PApplet` because with any luck, these changes will only require a recompile of any sketch (or library) code. 


#### What else?
1. A new `PSurface` object has been added that acts as the layer between `PApplet` and `PGraphics`. It handles interaction with the OS (creation of a window, placement on screen, getting mouse and key events) as well as the animation thread (because OpenGL's animation thread is very different from an AWT animation thread).
2. Many deprecated functions (notably, the pre-2.0 only method registration mechanism used by libraries) have been removed.
3. Undocumented features (such as the `image` object in `PGraphics`) may disappear and break code from advanced users.
4. We've added the ability to span multiple screens in "full screen" mode.
5. In 3.0a2 we changed the OS X version to use Apple's "official" full screen mode. With this came a dorky animation and the inability to span multiple screens. We've rolled that back because of the latter, though the former was also a consideration.


#### But what about...? 

We're still determining how much code we're willing to break due to API changes. Stay tuned.


#### Integration with Java applications
One downside of these changes is that you'll no longer be able to just drop a Processing sketch into other Java code, because `PApplet` will no longer subclass `Applet` (and therefore, `Component`). This is a huge downside for a tiny number of users. 

Making it a generic `Component`, however, means that we cannot improve performance, due to the cross-platform mess of Java's outdated (and somewhat unsuspported) AWT. 

In 3.0 alpha 6 and 7, a `getCanvas()` or `getComponent()` method provided a way to get an object to be embedded, but as we prepare for alpha 8, it looks like we'll have to move in another direction. At the present time, it looks like it'll be necessary to create a separate `PComponent` or `PCanvas` class that can be used, but it's not clear how that will work. 

This is one of many unfortunate tradeoffs I'm trying to sort through as we try to remove significant barriers to performance caused by the design of Java's AWT, while also supporting features (like embedding) that we've spent so much time supporting in the past.


#### Offscreen rendering
* createGraphics() will create a buffer that's not resizable. `PGraphics.setSize()` is called in `PApplet.makeGraphics()`, and that's the end of the story. No `Surface.setSize()` calls are involved as in a normal rendering situation.


#### Retina/HiDPI/2x drawing and displays
* Documentation changes [here](https://github.com/processing/processing-docs/issues/170)


#### The Event Dispatch Thread
The source has gone back and forth between putting all AWT (and Swing, if any) calls on the [EDT](https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html). Per Oracle's statements in Java's documentation, this is best practice (at least for Swing; for AWT it's not clear). However, we've gone back and forth several times as to whether it's necessary or worthwhile. 
Actual rendering in the default renderer happens off the EDT, but the EDT is used to blit the image to the screen (or resize windows, etc). By moving to the EDT, we're looking for more consistent cross-platform results. In practice, results are either mixed or not there.


## OpenGL 

And now, for something completely different.

### Changes from 2.x

Any code that uses `javax.media.opengl` in imports should replace that `com.jogamp.opengl`. I guess the JOGL folks are fully on their own w/o Oracle/Sun support. 

### NEWT 

NEWT was written by the JOGL guys so that they could avoid AWT altogether. The outdated/outmoded AWT makes a lot of assumptions that make implementation of GL a mess and causes performance trouble. This is a big part of the rendering changes that I’ve been making in 3—that we’re moving away from AWT as much as possible so that we don’t have performance problems. In the GL case, AWT causes some stuttering and lowered frame rates. We can get rid of those by dropping Applet, Component, and Canvas, and switching to NEWT’s windowing mechanism.


### JOGL vs LWJGL

During the alpha 6, 7, and 8 release process we did some juggling with what OpenGL library we should use. 

The short version of how it played out (written 15 May 2015)
* JOGL had some major bugs and development seemed to have stopped (summer/fall 2014)
* @codeanticode had been trying out LWJGL2 to see how it fared (last fall 2014)
* The LWJGL project has moved all their development effort to LWJGL3 (since then)
* Andrés spent the week rewriting OpenGL to use LWJGL3
* LWJGL3 is simply too unstable for us to use, would require major reworking of PApplet to remove *all* uses of AWT, and they seem to be still struggling with many fundamental issues (this week) 
* Andrés went back to JOGL (last 48 hours) to find that many bugs had been fixed and development was continuing. 
* For 3.0a8, we dropped LWJGL since JOGL is performing much better, and we're 99% sure that's the final decision for 3.0 (yesterday).

It looks like LWJGL3 will be a nice game-centric platform (full screen, affordances for game input/behavior) in the coming months, but the direction they're having to go with 3 means they're moving further away from what we need in Processing with something *slightly* more general.

LWJGL and JOGL are both great projects and we're thankful for all the work that they put in, and our own experience with Processing means that we couldn't be more sympathetic to the difficulty they face in maintaining their cross-platform, cross-chipset, cross-everything code. Like Processing, both projects are open source and created by volunteers who give their work away for free. We're enormously appreciative of their efforts.


## `settings()` is required
Prior to Processing 3, dark magic was used to make the `size()` command work. This was done to hide an enormous amount of complexity from users. Over time, the hacks involved became untenable or just unsustainable. The process was like this:
* The default renderer would be initialized offscreen and unused
* `setup()` would run, and if the renderer changed, the sketch would throw an exception causing things to restart (re-calling the `setup()` method)
* The previous step gave fits to any other variants of Processing (like Python or Ruby or Scala)
* We had a tricky, stuttery situation where some things would happen automatically, other things would be delayed slightly
In the Android version of Processing, these methods weren't possible, so we enhanced the preprocessor to parse the `size()` command used in the sketch and create methods called `sketchWidth()` and `sketchHeight()` and so on, that returned the values found in `setup()`. 
In Processing 3, we're moving in a different direction. A new method called `settings()` has been introduced. When running inside the PDE, commands like `size()`, `fullScreen()`, `pixelDensity()`, and `smooth()` are all moved to the `settings()` method, which is called once, before `setup()`. Those are the only methods that can be called inside `settings()`. When outside the PDE (i.e. using Eclipse), you'll need to move those methods to `settings()` yourself. 


## JavaFX 

Similarly to the NEWT situation in JOGL described above, we’ve hit the upper bound of what we can do on performance in Java2D as well. The graphics engineers from the Java team seem to have all moved to JavaFX for the last few years, perhaps because AWT is a dead end. So… I’ve started doing the JavaFX port so that we can drop even more of the AWT code.

JavaFX provides significantly better performance on recent (last couple years) hardware. Performance is drastically better than Java2D on retina displays. It makes heavy use of OpenGL, so on machines that have mediocre GL performance (integrated graphics, ultralight laptops, that sort of thing), it may even be slower than Java2D. But those situations are growing more rare, especially for our target audience.

We hope to make JavaFX the default renderer instead of Java2D. With any luck, we'd like to do this before 3.0 final is released.


## More AWT Removal

Run away from the AWT. All of our focus is on the OpenGL and JavaFX rendering engines, neither of which use AWT. 

***

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

