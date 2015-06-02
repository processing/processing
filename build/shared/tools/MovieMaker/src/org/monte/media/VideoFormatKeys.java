/*
 * @(#)VideoFormatKeys.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import org.monte.media.math.Rational;

/**
 * Defines common format keys for video media.
 *
 * @author Werner Randelshofer
 * @version $Id: VideoFormatKeys.java 299 2013-01-03 07:40:18Z werner $
 */
public class VideoFormatKeys extends FormatKeys {
        // Standard video ENCODING strings for use with FormatKey.Encoding. 
    public static final String ENCODING_BUFFERED_IMAGE = "image";
    /** Cinepak format. */
    public static final String ENCODING_QUICKTIME_CINEPAK = "cvid";
    public static final String COMPRESSOR_NAME_QUICKTIME_CINEPAK = "Cinepak";
    /** JPEG format. */
    public static final String ENCODING_QUICKTIME_JPEG = "jpeg";
    public static final String COMPRESSOR_NAME_QUICKTIME_JPEG = "Photo - JPEG";
    /** PNG format. */
    public static final String ENCODING_QUICKTIME_PNG = "png ";
    public static final String COMPRESSOR_NAME_QUICKTIME_PNG = "PNG";
    /** Animation format. */
    public static final String ENCODING_QUICKTIME_ANIMATION = "rle ";
    public static final String COMPRESSOR_NAME_QUICKTIME_ANIMATION = "Animation";
    /** Raw format. */
    public static final String ENCODING_QUICKTIME_RAW = "raw ";
    public static final String COMPRESSOR_NAME_QUICKTIME_RAW = "NONE";
    // AVI Formats
    /** Microsoft Device Independent Bitmap (DIB) format. */
    public static final String ENCODING_AVI_DIB = "DIB ";
    /** Microsoft Run Length format. */
    public static final String ENCODING_AVI_RLE = "RLE ";
    /** Techsmith Screen Capture format. */
    public static final String ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE = "tscc";
    public static final String COMPRESSOR_NAME_AVI_TECHSMITH_SCREEN_CAPTURE = "Techsmith Screen Capture";
    /** DosBox Screen Capture format. */
    public static final String ENCODING_AVI_DOSBOX_SCREEN_CAPTURE = "ZMBV";
    /** JPEG format. */
    public static final String ENCODING_AVI_MJPG = "MJPG";
    /** PNG format. */
    public static final String ENCODING_AVI_PNG = "png ";
    /** Interleaved planar bitmap format. */
    public static final String ENCODING_BITMAP_IMAGE = "ILBM";

    //

    /** The WidthKey of a video frame. */
    public final static FormatKey<Integer> WidthKey = new FormatKey<Integer>("dimX","width", Integer.class);
    /** The HeightKey of a video frame. */
    public final static FormatKey<Integer> HeightKey = new FormatKey<Integer>("dimY","height", Integer.class);
    /** The number of bits per pixel. */
    public final static FormatKey<Integer> DepthKey = new FormatKey<Integer>("dimZ","depth", Integer.class);
    /** The data class. */
    public final static FormatKey<Class> DataClassKey = new FormatKey<Class>("dataClass", Class.class);
    /** The compressor name. */
    public final static FormatKey<String> CompressorNameKey = new FormatKey<String>("compressorName", "compressorName",String.class, true);
    /** The pixel aspect ratio WidthKey : HeightKey;
     */
    public final static FormatKey<Rational> PixelAspectRatioKey = new FormatKey<Rational>("pixelAspectRatio", Rational.class);
    /** Whether the frame rate must be fixed. False means variable frame rate. */
    public final static FormatKey<Boolean> FixedFrameRateKey = new FormatKey<Boolean>("fixedFrameRate", Boolean.class);
    /** Whether the video is interlaced. */
    public final static FormatKey<Boolean> InterlaceKey = new FormatKey<Boolean>("interlace", Boolean.class);
    /** Encoding quality. Value between 0 and 1. */
    public final static FormatKey<Float> QualityKey = new FormatKey<Float>("quality", Float.class);
}
