/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2006 Daniel Shiffman

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License.

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

import quicktime.Errors;
import quicktime.QTException;
import quicktime.QTSession;
import quicktime.io.OpenMovieFile;
import quicktime.io.QTFile;
import quicktime.qd.QDConstants;
import quicktime.qd.QDGraphics;
import quicktime.qd.QDRect;
import quicktime.std.StdQTConstants;
import quicktime.std.StdQTException;
import quicktime.std.image.CSequence;
import quicktime.std.image.CodecComponent;
import quicktime.std.image.CompressedFrameInfo;
import quicktime.std.image.ImageDescription;
import quicktime.std.image.QTImage;
import quicktime.std.movies.Movie;
import quicktime.std.movies.Track;
import quicktime.std.movies.media.VideoMedia;
import quicktime.util.QTHandle;
import quicktime.util.RawEncodedImage;

import processing.core.*;


/**
 * Library to create a QuickTime movie from a Processing pixel array.
 * Written by <A HREF="http://www.shiffman.net">Daniel Shiffman</A>.
 * Thanks to Dan O'Sullivan and Shawn Van Every.
 */
public class MovieMaker {

  public static final int RAW = StdQTConstants.kRawCodecType;
  public static final int JPEG = StdQTConstants.kJPEGCodecType;
  public static final int CINEPAK = StdQTConstants.kCinepakCodecType;
  public static final int SORENSON = StdQTConstants.kSorensonCodecType;
  public static final int VIDEO = StdQTConstants.kVideoCodecType;
  public static final int H261 = StdQTConstants.kH261CodecType;
  public static final int H263 = StdQTConstants.kH263CodecType;
  public static final int BASE = StdQTConstants.kBaseCodecType;
  public static final int BMP = StdQTConstants.kBMPCodecType;
  public static final int CMYK = StdQTConstants.kCMYKCodecType;
  public static final int COMPONENT = StdQTConstants.kComponentVideoCodecType;
  public static final int GIF = StdQTConstants.kGIFCodecType;
  public static final int GRAPHICS = StdQTConstants.kGraphicsCodecType;
  public static final int MSVideo = StdQTConstants.kMicrosoftVideo1CodecType;
  public static final int MOTIONJPEGA = StdQTConstants.kMotionJPEGACodecType;
  public static final int MOTIONJPEGB = StdQTConstants.kMotionJPEGBCodecType;

  public static final int WORST = StdQTConstants.codecMinQuality;
  public static final int LOW = StdQTConstants.codecLowQuality;
  public static final int MEDIUM = StdQTConstants.codecNormalQuality;
  public static final int HIGH = StdQTConstants.codecHighQuality;
  public static final int BEST = StdQTConstants.codecMaxQuality;
  public static final int LOSSLESS = StdQTConstants.codecLosslessQuality;

  private int width;
  private int height;

  private boolean readyForFrames;

  private int TIME_SCALE = 1000;

  // QT Stuff
  private VideoMedia videoMedia;
  private Track videoTrack;
  private Movie movie;
  private QTFile movFile;
  private CSequence seq;
  private QTHandle imageHandle;
  private QDGraphics gw;
  private QDRect bounds;
  private ImageDescription imgDesc;
  private RawEncodedImage compressedImage;

  private int rate;
  private int keyFrameRate = 15;
  private int codecType, codecQuality;

  // my hack to make sure we don't get error -8691
  private boolean temporalSupported = true;

  private PApplet parent;


  public MovieMaker(PApplet p, int _w, int _h, String _filename) {
    this(p, _w, _h, _filename, 0, 0, 30, 15);
  }


  public MovieMaker(PApplet p, int _w, int _h, String _filename,
                    int _codecType, int _codecQuality) {
    this(p, _w, _h, _filename, _codecType, _codecQuality, 30, 15);
  }


  public MovieMaker(PApplet p, int _w, int _h, String _filename,
                    int _codecType, int _codecQuality, int _rate) {
    this(p, _w, _h, _filename, _codecType, _codecQuality, _rate, 15);
  }

  public MovieMaker(PApplet p, int _w, int _h, String _filename,
                    int _codecType, int _codecQuality, int _rate,
                    int _keyFrameRate) {
    parent = p;

    width = _w;
    height = _h;
    rate = _rate;

    // Start QT
    //if (!QTSession.isInitialized()) {  // [fry]
    try {
      QTSession.open();
    } catch (QTException e1) {
      e1.printStackTrace();
    }
    //}

    // Create GWorld
    try {
      // Broken on intel?
      // ImageDescription imgD =
      //   new ImageDescription(QDConstants.k32ARGBPixelFormat);
      ImageDescription imgD = null;
      // Intel fix, could it be??  I think this takes care of Windows too?!?!?
      if (quicktime.util.EndianOrder.isNativeLittleEndian()) {
        imgD = new ImageDescription(QDConstants.k32BGRAPixelFormat);
      } else {
        imgD = new ImageDescription(QDGraphics.kDefaultPixelFormat);
      }
      imgD.setWidth(width);
      imgD.setHeight(height);
      gw = new QDGraphics(imgD, 0);

    } catch (QTException e) {
      e.printStackTrace();
    }
    codecType = _codecType;
    codecQuality = _codecQuality;
    keyFrameRate = _keyFrameRate;
    initMovie(_filename);

    parent.registerDispose(this);
  }


  private void initMovie(String filename) {
    // If nothing has been specified
    if (codecType == 0) {
      System.out.println("No code type specified, using default of RAW");
      codecType = RAW;
    }
    if (codecQuality == 0) {
      System.out.println("No code quality specified, using default of HIGH");
      codecQuality = HIGH;
    }

    try {
      String path = parent.savePath(filename);
      System.out.println("Creating movie file: " + path);
      movFile = new QTFile (new java.io.File(path));
      movie = Movie.createMovieFile(movFile, StdQTConstants.kMoviePlayer, StdQTConstants.createMovieFileDeleteCurFile);
      int timeScale = TIME_SCALE; // 100 units per second
      videoTrack = movie.addTrack(width, height, 0);
      videoMedia = new VideoMedia(videoTrack, timeScale);
      videoMedia.beginEdits();
      bounds = new QDRect(0, 0, width, height);
      int rawImageSize = QTImage.getMaxCompressionSize(gw, bounds, gw.getPixMap().getPixelSize(), codecQuality, codecType, CodecComponent.anyCodec);
      imageHandle = new QTHandle(rawImageSize, true);
      imageHandle.lock();
      compressedImage = RawEncodedImage.fromQTHandle(imageHandle);
      seq = new CSequence(gw, bounds, gw.getPixMap().getPixelSize(), codecType, CodecComponent.bestFidelityCodec, codecQuality, codecQuality, keyFrameRate, null, 0);
      imgDesc = seq.getDescription();
      readyForFrames = true;

    } catch (QTException e) {
      //  if it's a -8961 error, do it the other way
      if (e.errorCode() == Errors.noCodecErr) {
        //System.out.println("Temporal compression not supported, switching modes. ");
        temporalSupported = false;
        readyForFrames = true;

      } else if (e.errorCode() == Errors.fBsyErr) {
        System.err.println("Movie file already exists.  " +
                           "Please delete it first. " +
                           "(DS to solve this eventually.)");

      } else {
        e.printStackTrace();
      }
    }
  }


  public void addFrame(int[] _pixels, int w, int h) {
    // Now that I fixed the intel mac bug in the constructor, I think windows
    // is covered too so the code below is unnecessary.  I think.  I hope.
    //boolean windows = false;
    //String os = System.getProperty("os.name");
    //if (os.charAt(0) == 'w' || os.charAt(0) == 'W') windows = true;
    if (readyForFrames){
      RawEncodedImage pixelData = gw.getPixMap().getPixelData();
      int rowBytes = pixelData.getRowBytes() / 4;
      int[] newpixels = new int[rowBytes*h];
      for (int i = 0; i < rowBytes; i++) {
        for (int j = 0; j < h; j++) {
          if (i < w) {
            newpixels[i+j*rowBytes] = _pixels[i+j*w];
            // We can skip this now, right?!?!?!?
            // On windows, we have to flip the pixels (i think)
            //if (!windows) newpixels[i+j*rowBytes] = _pixels[i+j*w];
            //else newpixels[i+j*rowBytes] = EndianOrder.flipBigEndianToNative32(_pixels[i+j*w]);
          }
          else newpixels[i+j*rowBytes] = 0;
        }
      }
      pixelData.setInts(0,newpixels);
      compressAndAdd();
    }
  }


  private void compressAndAdd() {
    try {
      if (temporalSupported) {
        CompressedFrameInfo cfInfo = seq.compressFrame(gw, bounds, StdQTConstants.codecFlagUpdatePrevious, compressedImage);
        boolean syncSample = cfInfo.getSimilarity() == 0; // see developer.apple.com/qa/qtmcc/qtmcc20.html
        videoMedia.addSample(imageHandle, 0, cfInfo.getDataSize(), TIME_SCALE/rate, imgDesc, 1, syncSample ? 0 : StdQTConstants.mediaSampleNotSync);
      } else {
        imgDesc = QTImage.fCompress(gw,gw.getBounds(),32,codecQuality,codecType, CodecComponent.anyCodec, null, 0, RawEncodedImage.fromQTHandle(imageHandle));
        boolean syncSample = true;   // UM, what the hell should this be???
        videoMedia.addSample(imageHandle, 0, imgDesc.getDataSize(), TIME_SCALE/rate, imgDesc, 1, syncSample ? 0 : StdQTConstants.mediaSampleNotSync);
      }
    } catch (QTException e) {
      e.printStackTrace();
    }
  }
  
  
  public void finishMovie() {
    System.out.println("Finishing movie file.");
    try {
      readyForFrames = false;
      videoMedia.endEdits();
      videoTrack.insertMedia(0, 0, videoMedia.getDuration(), 1);
      OpenMovieFile omf = OpenMovieFile.asWrite(movFile);
      movie.addResource(omf, StdQTConstants.movieInDataForkResID,
                        movFile.getName());
    } catch (StdQTException e) {
      e.printStackTrace();
    } catch (QTException e) {
      e.printStackTrace();
    }
    // Causes windows to hang??
    //QTSession.close();
  }

  
  public void dispose() {
    if (readyForFrames) finishMovie();

    try {
      QTSession.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
