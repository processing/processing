/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PMovie - reading from video files
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry
  The previous version of this code was developed by Hernando Barragan

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.video;
import processing.core.*;

import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;

import quicktime.*;
import quicktime.io.QTFile;
import quicktime.qd.*;
import quicktime.std.*;
import quicktime.std.movies.*;
import quicktime.std.movies.media.DataRef;
import quicktime.util.RawEncodedImage;


public class Movie extends PImage
  implements /*StdQTConstants, StdQTConstants4,*/ PConstants, Runnable {
  PApplet parent;
  Method movieEventMethod;
  String filename;
  Thread runner;

  PImage borderImage;
  boolean removeBorders = true;

  boolean play;
  boolean repeat;
  boolean available;
  //float rate;
  int fps;

  /** The movie object, made public in case anyone wants to play with it. */
  public quicktime.std.movies.Movie movie;

  QDRect movieRect;
  QDGraphics movieGraphics;
  boolean firstFrame = true;
  RawEncodedImage raw;


  static {
    try {
      //System.out.println("jlp = " + System.getProperty("java.library.path"));
      QTSession.open();
    } catch (QTException e) {
      e.printStackTrace();
    }
    QTRuntimeException.registerHandler(new QTRuntimeHandler() {
        public void exceptionOccurred(QTRuntimeException e,
                                      Object obj, String s, boolean flag) {
          System.err.println("Problem inside Movie");
          e.printStackTrace();
        }
      });
  }


  public Movie(PApplet parent, String filename) {
    this(parent, filename, 30);
  }


  public Movie(PApplet parent, String filename, int ifps) {
    //this(parent, parent.getClass().getResource("data/" + filename), ifps);

    // this creates a fake image so that the first time this
    // attempts to draw, something happens that's not an exception
    super.init(1, 1, RGB);

    URL url = null;
    this.filename = filename; // for error messages

    if (filename.startsWith("http://")) {
      try {
        url = new URL(filename);
        DataRef urlRef = new DataRef(url.toExternalForm());
        movie = fromDataRef(urlRef);
        //movie = quicktime.std.movies.Movie.fromDataRef(urlRef,
        //StdQTConstants4.newMovieAsyncOK |
        //StdQTConstants.newMovieActive);
        init(parent, movie, ifps);
        return;

      } catch (QTException qte) {
        qte.printStackTrace();
        return;

      } catch (MalformedURLException e) {
        e.printStackTrace();
        return;
      }
    }

    url = getClass().getResource(filename);
    if (url != null) {
      init(parent, url, ifps);
      return;
    }

    url = getClass().getResource("data/" + filename);
    if (url != null) {
      init(parent, url, ifps);
      return;
    }

    try {
      try {
        // look inside the sketch folder (if set)
        String location = parent.folder + File.separator + "data";
        File file = new File(location, filename);
        if (file.exists()) {
          movie = fromDataRef(new DataRef(new QTFile(file)));
          //movie = quicktime.std.movies.Movie.fromDataRef(new DataRef(new QTFile(file)),
          //StdQTConstants4.newMovieAsyncOK |
          //StdQTConstants.newMovieActive);
          init(parent, movie, ifps);
          return;
        }
      } catch (QTException e) { }  // ignored

      try {
        File file = new File("data", filename);
        if (file.exists()) {
          movie = fromDataRef(new DataRef(new QTFile(file)));
          //movie = quicktime.std.movies.Movie.fromDataRef(new DataRef(new QTFile(file)),
          //StdQTConstants4.newMovieAsyncOK |
          //StdQTConstants.newMovieActive);
          init(parent, movie, ifps);
          return;
        }
      } catch (QTException e2) { }

      try {
        File file = new File(filename);
        if (file.exists()) {
          movie = fromDataRef(new DataRef(new QTFile(file)));
          //movie = quicktime.std.movies.Movie.fromDataRef(new DataRef(new QTFile(file)),
          //StdQTConstants4.newMovieAsyncOK |
          //StdQTConstants.newMovieActive);
          init(parent, movie, ifps);
          return;
        }
      } catch (QTException e1) { }

    } catch (SecurityException se) { }  // online, whups

    parent.die("Could not find movie file " + filename, null);
  }


  public Movie(PApplet parent, URL url) {
    init(parent, url, 30);
  }


  public Movie(PApplet parent, URL url, int ifps) {
    init(parent, url, ifps);
  }


  public void init(PApplet parent, URL url, int ifps) {

    // qtjava likes file: urls to read file:/// not file:/
    // so this changes them when appropriate
    String externalized = url.toExternalForm();
    if (externalized.startsWith("file:/") &&
        !externalized.startsWith("file:///")) {
      externalized = "file:///" + url.getPath();
    }

    // the url version is the only available that can take
    // an InputStream (indirectly) since it uses url syntax
    //DataRef urlRef = new DataRef(requestFile);
    try {
      DataRef urlRef = new DataRef(externalized);
      movie = fromDataRef(urlRef);
      //movie = quicktime.std.movies.Movie.fromDataRef(urlRef,
      //StdQTConstants4.newMovieAsyncOK |
      //StdQTConstants.newMovieActive);
      init(parent, movie, ifps);

    } catch (QTException e) {
      e.printStackTrace();
    }
    //System.out.println("done with init");
  }


  private quicktime.std.movies.Movie fromDataRef(DataRef ref)
    throws QTException {

    return
      quicktime.std.movies.Movie.fromDataRef(ref,
                                             StdQTConstants4.newMovieAsyncOK |
                                             StdQTConstants.newMovieActive);
  }


  public void init(PApplet parent,
                   quicktime.std.movies.Movie movie, int ifps) {
    this.parent = parent;

    try {
      movie.prePreroll(0, 1.0f);
      movie.preroll(0, 1.0f);
      while (movie.maxLoadedTimeInMovie() == 0) {
        movie.task(100);
      }
      movie.setRate(1);
      fps = ifps;

      runner = new Thread(this);
      runner.start();


      // register methods

      parent.registerDispose(this);

      try {
        movieEventMethod =
          parent.getClass().getMethod("movieEvent",
                                      new Class[] { Movie.class });
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (QTException qte) {
      qte.printStackTrace();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public boolean available() {
    return available;
  }


  public void read() {
    try {
      if (firstFrame) {
        movieRect = movie.getBox();
        movieGraphics = new QDGraphics(movieRect);
      }

      Pict pict = movie.getPict(movie.getTime());  // returns an int
      pict.draw(movieGraphics, movieRect);
      PixMap pixmap = movieGraphics.getPixMap();
      raw = pixmap.getPixelData();

      // It needs to get at least a small part
      // of the video to get the parameters
      if (firstFrame) {
        //int intsPerRow = pixmap.getRowBytes() / 4;
        int movieWidth = movieRect.getWidth();
        int movieHeight = movieRect.getHeight();
        int j = raw.getRowBytes() - movieWidth*4;
        // this doesn't round up.. does it need to?
        int k = j / 4;
        int dataWidth = movieWidth + k;

        if (dataWidth != movieWidth) {
          if (removeBorders) {
            int bpixels[] = new int[dataWidth * movieHeight];
            borderImage = new PImage(bpixels, dataWidth, movieHeight, RGB);
          } else {
            movieWidth = dataWidth;
          }
        }
        int vpixels[] = new int[movieWidth * movieHeight];
        //image = new PImage(vpixels, movieWidth, movieHeight, RGB);
        super.init(movieWidth, movieHeight, RGB);
        //parent.video = image;
        firstFrame = false;
      }
      // this happens later (found by hernando)
      //raw.copyToArray(0, image.pixels, 0, image.width * image.height);

      // this is identical to a chunk of code inside PCamera
      // this might be a candidate to move up to PVideo or something
      if (borderImage != null) {  // need to remove borders
        raw.copyToArray(0, borderImage.pixels,
                        0, borderImage.width * borderImage.height);
        int borderIndex = 0;
        int targetIndex = 0;
        for (int i = 0; i < height; i++) {
          System.arraycopy(borderImage.pixels, borderIndex,
                           pixels, targetIndex, width);
          borderIndex += borderImage.width;
          targetIndex += width;
        }
      } else {  // just copy directly
        raw.copyToArray(0, pixels, 0, width * height);
      }

      // ready to rock
      //System.out.println("updating pixels");
      updatePixels();  // mark as modified

    } catch (QTException qte) {
      qte.printStackTrace();
      QTSession.close();
    }
  }


  /**
   * Begin playing the movie, with no repeat.
   */
  public void play() {
    play = true;
  }


  /**
   * Begin playing the movie, with repeat.
   */
  public void loop() {
    play = true;
    repeat = true;
  }


  /**
   * Shut off the repeating loop.
   */
  public void noLoop() {
    repeat = false;
  }


  /**
   * Pause the movie at its current time.
   */
  public void pause() {
    play = false;
    System.out.println("pause");
  }


  /**
   * Stop the movie, and rewind.
   */
  public void stop() {
    play = false;
    System.out.println("stop");
    try {
      movie.setTimeValue(0);

    } catch (StdQTException e) {
      errorMessage("stop", e);
    }
  }


  /**
   * Set how often new frames are to be read from the movie.
   */
  public void framerate(int ifps) {
    if (ifps <= 0) {
      System.err.println("Movie: ignoring bad framerate of " +
                         ifps + " fps.");
      return;
    }
    fps = ifps;
  }


  /**
   * Set a multiplier for how fast/slow the movie should be run.
   * speed(2) will play the movie at double speed (2x).
   * speed(0.5) will play at half speed.
   * speed(-1) will play backwards at regular speed.
   */
  public void speed(float rate) {
    //rate = irate;
    try {
      movie.setRate(rate);

    } catch (StdQTException e) {
      errorMessage("speed", e);
    }
  }


  /**
   * Return the current time in seconds.
   * The number is a float so fractions of seconds can be used.
   */
  public float time() {
    try {
      return (float)movie.getTime() / (float)movie.getTimeScale();

    } catch (StdQTException e) {
      errorMessage("time", e);
    }
    return -1;
  }


  /**
   * Jump to a specific location (in seconds).
   * The number is a float so fractions of seconds can be used.
   */
  //public void jump(int where) {
  public void jump(float where) {
    try {
      //movie.setTime(new TimeRecord(rate, where));  // scale, value
      //movie.setTime(new TimeRecord(1, where));  // scale, value
      int scaledTime = (int) (where * movie.getTimeScale());
      movie.setTimeValue(scaledTime);

    } catch (StdQTException e) {
      errorMessage("jump", e);
    }
  }


  /**
   * Get the full length of this movie (in seconds).
   */
  public float duration() {
    try {
      return (float)movie.getDuration() / (float)movie.getTimeScale();

    } catch (StdQTException e) {
      errorMessage("length", e);
    }
    return -1;
  }


  /*
  public void play() {
    if(!play) {
      play = true;
    }
    start();
    while( image == null) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) { }
    }
    pixels = image.pixels;
    width = image.width;
    height = image.height;
  }


  public void repeat() {
    loop = true;
    if(!play) {
      play = true;
    }
    start();
    while( image == null) {
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) { }
    }
    pixels = image.pixels;
    width = image.width;
    height = image.height;
  }


  public void pause() {
    play = false;
  }
  */


  public void run() {
    //System.out.println("entering thread");
    while (Thread.currentThread() == runner) {
      //System.out.print("<");
      try {
        //Thread.sleep(5);
        Thread.sleep(1000 / fps);
      } catch (InterruptedException e) { }
      //System.out.print(">");

      // this could be a lie, but..
      if (play) {
        //read();
        //System.out.println("play");
        available = true;

        if (movieEventMethod != null) {
          try {
            movieEventMethod.invoke(parent, new Object[] { this });
          } catch (Exception e) {
            System.err.println("error, disabling movieEvent() for " +
                               filename);
            e.printStackTrace();
            movieEventMethod = null;
          }
        }

        try {
          if (movie.isDone() && repeat) {
            movie.goToBeginning();
          }
        } catch (StdQTException e) {
          play = false;
          errorMessage("rewinding", e);
        }
        //} else {
        //System.out.println("no play");
      }

      //try {
      //read();
      //if (movie.isDone() && loop) movie.goToBeginning();

      //} catch (QTException e) {
      //System.err.println("Movie exception");
      //e.printStackTrace();
      //QTSession.close(); ??
      //}
    }
  }


  public void dispose() {
    System.out.println("disposing");
    stop();
    runner = null;
    QTSession.close();
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  protected void errorMessage(String where, Exception e) {
    parent.die("Error inside Movie." + where + "()", e);
  }
}

