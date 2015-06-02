/*
 * @(#)AnimationCodec.java  1.3  2011-01-16
 *
 * Copyright © 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance onlyWith the
 * license agreement you entered into onlyWith Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import java.io.EOFException;
import javax.imageio.stream.ImageInputStream;
import org.monte.media.AbstractVideoCodec;
import org.monte.media.Buffer;
import org.monte.media.Format;
import org.monte.media.io.ByteArrayImageOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteOrder;
import javax.imageio.stream.ImageOutputStream;
import static java.lang.Math.*;
import static org.monte.media.VideoFormatKeys.*;
import static org.monte.media.BufferFlag.*;

/**
 * Implements the Apple Animation codec.
 * <p>
 * Supports lossless delta- and key-frame encoding of images onlyWith 8, 16 or
 * 24 bits per pixel.
 * <p>
 * The QuickTime player requires that a keyframe is written once per second. 
 * This codec enforces this.
 * <p>
 * An encoded frame has the following format:
 * <p>
 * <pre>
 * Header:
 * uint32 chunkSize
 *
 * uint16 header 0x0000 => decode entire image
 *               0x0008 => starting line and number of lines follows
 * if header==0x0008 {
 *   uint16 startingLine at which to begin updating frame
 *   uint16 reserved 0x0000
 *   uint16 numberOfLines to update
 *   uint16 reserved 0x0000
 * }
 * n-bytes compressed lines
 * </pre>
 *
 * The first 4 bytes defines the chunk length. This field also carries some
 * other unknown flags, since at least one of the high bits is sometimes set.<br>
 *
 * If the overall length of the chunk is less than 8, treat the frame as a
 * NOP, which means that the frame is the same as the one before it.<br>
 *
 * Next, there is a header of either 0x0000 or 0x0008. A header value onlyWith
 * bit 3 set (header &amp; 0x0008) indicates that information follows revealing
 * at which line the decode process is to begin:<br>
 *
 * <pre>
 * 2 bytes    starting line at which to begin updating frame
 * 2 bytes    unknown
 * 2 bytes    the number of lines to update
 * 2 bytes    unknown
 * </pre>
 *
 * If the header is 0x0000, then the decode begins from the first line and
 * continues through the entire height of the image.<br>
 *
 * After the header comes the individual RLE-compressed lines. An individual
 * compressed line is comprised of a skip code, followed by a series of RLE
 * codes and pixel data:<br>
 * <pre>
 *  1 byte     skip code
 *  1 byte     RLE code
 *  n bytes    pixel data
 *  1 byte     RLE code
 *  n bytes    pixel data
 * </pre>
 * Each line begins onlyWith a byte that defines the number of pixels to skip in
 * a particular line in the output line before outputting new pixel
 * data. Actually, the skip count is set to one more than the number of
 * pixels to skip. For example, a skip byte of 15 means "skip 14 pixels",
 * while a skip byte of 1 means "don't skip any pixels". If the skip byte is
 * 0, then the frame decode is finished. Therefore, the maximum skip byte
 * value of 255 allows for a maximum of 254 pixels to be skipped.
 * <p>
 * After the skip byte is the first RLE code, which is a single signed
 * byte. The RLE code can have the following meanings:<br>
 * <ul>
 * <li>equal to 0: There is another single-byte skip code in the stream.
 *              Again, the actual number of pixels to skip is 1 less
 *              than the skip code. Therefore, the maximum skip byte
 *              value of 255 allows for a maximum of 254 pixels to be
 *              skipped.</li>
 *
 * <li>equal to -1: End of the RLE-compressed line</li>
 *
 * <li>greater than 0: Run of pixel data is copied directly from the
 *              encoded stream to the output frame.</li>
 *
 * <li>less than -1: Repeat pixel data -(RLE code) times.</li>
 * </ul>
 * <p>
 * The pixel data has the following format:
 * <ul>
 * <li>8-bit data: Pixels are handled in groups of four. Each pixel is a palette
 * index (the palette is determined by the Quicktime file transporting the
 * data).<br>
 * If (code &gt; 0), copy (4 * code) pixels from the encoded stream to the
 * output.<br>
 * If (code &lt; -1), extract the next 4 pixels from the encoded stream
 * and render the entire group -(code) times to the output frame. </li>
 *
 * <li>16-bit data: Each pixel is represented by a 16-bit RGB value onlyWith 5 bits
 * used for each of the red, green, and blue color components and 1 unused bit
 * to round the value tmp to 16 bits: {@code xrrrrrgg gggbbbbb}. Pixel data is
 * rendered to the output frame one pixel at a time.<br>
 * If (code &gt; 0), copy the run of (code) pixels from the encoded stream to
 * the output.<br>
 * If (code &lt; -1), unpack the next 16-bit RGB value from the encoded stream
 * and render it to the output frame -(code) times.</li>
 *
 * <li>24-bit data: Each pixel is represented by a 24-bit RGB value onlyWith 8 bits
 * (1 byte) used for each of the red, green, and blue color components:
 * {@code rrrrrrrr gggggggg bbbbbbbb}. Pixel data is rendered to the output
 * frame one pixel at a time.<br>
 * If (code &gt; 0), copy the run of (code) pixels from the encoded stream to
 * the output.<br>
 * If (code &lt; -1), unpack the next 24-bit RGB value from the encoded stream
 * and render it to the output frame -(code) times.</li>
 *
 * <li>32-bit data: Each pixel is represented by a 32-bit ARGB value onlyWith 8 bits
 * (1 byte) used for each of the alpha, red, green, and blue color components:
 * {@code aaaaaaaa rrrrrrrr gggggggg bbbbbbbb}. Pixel data is rendered to the
 * output frame one pixel at a time.<br>
 * If (code &gt; 0), copy the run of (code) pixels from the encoded stream to
 * the output.<br>
 * If (code &lt; -1), unpack the next 32-bit ARGB value from the encoded stream
 * and render it to the output frame -(code) times.</li>
 * </ul>
 *
 * References:<br/>
 * <a href="http://multimedia.cx/qtrle.txt">http://multimedia.cx/qtrle.txt</a><br>
 *
 * @author Werner Randelshofer
 * @version 1.3 2011-01-17 Fixes an index out of bounds exception when a
 * sub-image is compressed.
 * <br>1.2 2011-01-07 Improves compression rate.
 * <br>1.1 2011-01-07 Reduces seeking operations on output stream by using
 * a seekable output stream internally.
 * <br>1.0 2011-01-05 Created.
 */
public class AnimationCodec extends AbstractVideoCodec {

    private Object previousPixels;
    private int frameCounter;

    public AnimationCodec() {
        super(new Format[]{
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, 
                            EncodingKey, ENCODING_BUFFERED_IMAGE), //
                },
                new Format[]{
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_ANIMATION, DataClassKey, byte[].class, DepthKey, 8), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_ANIMATION, DataClassKey, byte[].class, DepthKey, 16), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_ANIMATION, DataClassKey, byte[].class, DepthKey, 24), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_ANIMATION, DataClassKey, byte[].class, DepthKey, 32), //
                });
    }

    @Override
    public Format setOutputFormat(Format f) {
        Format sf=super.setOutputFormat(f);
        // Enforce one key frame per second
        return f.prepend(KeyFrameIntervalKey,max(1,sf.get(FrameRateKey).intValue()));
    }

     
    
    @Override
    public void reset() {
        frameCounter = 0;
    }

    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        if (in.isFlag(DISCARD)) {
            return CODEC_OK;
        }
        out.format = outputFormat;
        ByteArrayImageOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new ByteArrayImageOutputStream((byte[]) out.data);
        } else {
            tmp = new ByteArrayImageOutputStream();
        }

        Format vf = outputFormat;

        // Handle sub-image
        Rectangle r;
        int scanlineStride;
        if (in.data instanceof BufferedImage) {
            BufferedImage image = (BufferedImage) in.data;
            WritableRaster raster = image.getRaster();
            scanlineStride = raster.getSampleModel().getWidth();
            r = raster.getBounds();
            r.x -= raster.getSampleModelTranslateX();
            r.y -= raster.getSampleModelTranslateY();
        } else {
            r = new Rectangle(0, 0, vf.get(WidthKey), vf.get(HeightKey));
            scanlineStride = vf.get(WidthKey);
        }
        boolean isKeyframe = frameCounter== 0
                || frameCounter % outputFormat.get(KeyFrameIntervalKey,outputFormat.get(FrameRateKey).intValue()) == 0;
        frameCounter++;

        try {
            switch (vf.get(DepthKey)) {
                case 8: {
                    byte[] pixels = getIndexed8(in);
                    if (pixels == null) {
                        return CODEC_FAILED;
                        //throw new UnsupportedOperationException("Unable to process buffer " + in);
                    }

                    if (isKeyframe ||//
                            previousPixels == null) {

                        encodeKey8(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, true);
                    } else {
                        encodeDelta8(tmp, pixels, (byte[]) previousPixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, false);
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                case 16: {
                    short[] pixels = getRGB15(in);
                    if (pixels == null) {
                        return CODEC_FAILED;
//                        throw new UnsupportedOperationException("Unable to process buffer " + in);
                    }

                    // FIXME - Support sub-images
                    if (isKeyframe//
                            || previousPixels == null) {
                        encodeKey16(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, true);
                    } else {
                        encodeDelta16(tmp, pixels, (short[]) previousPixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, false);

                        /*
                        if (test == null) {
                        test = pixels.clone();
                        } else {
                        System.arraycopy(pixels, 0, test, 0, pixels.length);
                        }
                        decodeDelta16(new ByteArrayImageOutputStream(tmp.getBuffer(), 0, (int) tmp.getStreamPosition(), ByteOrder.BIG_ENDIAN),//
                        test, (short[]) previousPixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                         */
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                case 24: {
                    int[] pixels = getRGB24(in);
                    if (pixels == null) {
                        return CODEC_FAILED;
//                      throw new UnsupportedOperationException("Unable to process buffer " + in);
                    }

                    // FIXME - Support sub-images
                    if (isKeyframe //
                            || previousPixels == null) {
                        encodeKey24(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, true);
                    } else {
                        encodeDelta24(tmp, pixels, (int[]) previousPixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, false);
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                case 32: {
                    int[] pixels = getARGB32(in);
                    if (pixels == null) {
                        out.setFlag(DISCARD);
                        return CODEC_FAILED;
//                        return;
                    }

                    // FIXME - Support sub-images
                    if (in.isFlag(KEYFRAME) //
                            || previousPixels == null) {
                        encodeKey32(tmp, pixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, true);
                    } else {
                        encodeDelta32(tmp, pixels, (int[]) previousPixels, r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                        out.setFlag(KEYFRAME, false);
                    }
                    if (previousPixels == null) {
                        previousPixels = pixels.clone();
                    } else {
                        System.arraycopy(pixels, 0, previousPixels, 0, pixels.length);
                    }
                    break;
                }
                default: {
                    out.setFlag(DISCARD);
                    return CODEC_FAILED;
                }

            }
            out.format = outputFormat;
            out.data = tmp.getBuffer();
            out.sampleCount = 1;
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            //
            return CODEC_OK;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.setFlag(DISCARD);
            return CODEC_FAILED;
        }
    }

    /** Encodes an 8-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeKey8(ImageOutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        if (width % 4 != 0 || offset % 4 != 0 || scanlineStride % 4 != 0) {
            throw new UnsupportedOperationException("Conversion is not fully implemented yet.");
        }

        // convert data to ints
        int[] ints = new int[data.length / 4];
        for (int i = 0, j = 0; i < data.length; i += 4, j++) {
            ints[j] = ((data[i] & 0xff) << 24) | ((data[i + 1] & 0xff) << 16) | ((data[i + 2] & 0xff) << 8) | ((data[i + 3] & 0xff));
        }
        encodeKey32(out, ints, width / 4, height, offset / 4, scanlineStride / 4);
    }

    /** Encodes an 8-bit delta frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeDelta8(ImageOutputStream out, byte[] data, byte[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        if (width % 4 != 0 || offset % 4 != 0 || scanlineStride % 4 != 0) {
            throw new UnsupportedOperationException("Conversion is not fully implemented yet.");
        }
        out.setByteOrder(ByteOrder.BIG_ENDIAN);

        // convert data to ints
        int[] ints = new int[data.length / 4];
        for (int i = 0, j = 0; i < data.length; i += 4, j++) {
            ints[j] = ((data[i] & 0xff) << 24) | ((data[i + 1] & 0xff) << 16) | ((data[i + 2] & 0xff) << 8) | ((data[i + 3] & 0xff));
        }
        // convert prev to ints
        int[] pints = new int[prev.length / 4];
        for (int i = 0, j = 0; i < prev.length; i += 4, j++) {
            pints[j] = ((prev[i] & 0xff) << 24) | ((prev[i + 1] & 0xff) << 16) | ((prev[i + 2] & 0xff) << 8) | ((prev[i + 3] & 0xff));
        }
        encodeDelta32(out, ints, pints, width / 4, height, offset / 4, scanlineStride / 4);
    }

    /** Encodes a 16-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeKey16(ImageOutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);
        long headerPos = out.getStreamPosition();

        // Reserve space for the header:
        out.writeInt(0);
        out.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            out.write(1); // this is a key-frame, there is nothing to skip at the start of line

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine repeat count
                short v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (repeatCount < 2) {
                    literalCount++;
                    if (literalCount == 127) {
                        out.write(literalCount); // Literal OP-code
                        out.writeShorts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        out.writeShorts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    out.write(-repeatCount); // Repeat OP-code
                    out.writeShort(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                out.writeShorts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Encodes a 16-bit delta frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeDelta16(ImageOutputStream out, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);

        // Determine whether we can skip lines at the beginning
        int ymin;
        int ymax = offset + height * scanlineStride;
        scanline:
        for (ymin = offset; ymin < ymax; ymin += scanlineStride) {
            int xy = ymin;
            int xymax = ymin + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }


        if (ymin == ymax) {
            // => Frame is identical to previous one
            out.writeInt(4);
            return;
        }

        // Determine whether we can skip lines at the end
        scanline:
        for (; ymax > ymin; ymax -= scanlineStride) {
            int xy = ymax - scanlineStride;
            int xymax = ymax - scanlineStride + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }
        //System.out.println("AnimationCodec ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = out.getStreamPosition();
        out.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines
            out.writeShort(0x0000);
        } else {
            // => we can skip lines
            out.writeShort(0x0008);
            out.writeShort((ymin - offset) / scanlineStride);
            out.writeShort(0);
            out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
            out.writeShort(0);
        }

        // Encode each scanline
        for (int y = ymin; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            // determine skip count
            int skipCount = 0;
            for (; xy < xymax; ++xy, ++skipCount) {
                if (data[xy] != prev[xy]) {
                    break;
                }
            }
            if (skipCount == width) {
                // => the entire line can be skipped
                out.write(0 + 1); // don't skip any pixels
                out.write(-1); // end of line
                continue;
            }
            out.write(min(254 + 1, skipCount + 1));
            skipCount -= min(254, skipCount);
            while (skipCount > 0) {
                out.write(0); // Skip Op-code
                out.write(min(254 + 1, skipCount + 1)); // Number of bytes to skip + 1
                skipCount -= min(254, skipCount);
            }

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine skip count
                for (skipCount = 0; xy < xymax; ++xy, ++skipCount) {
                    if (data[xy] != prev[xy]) {
                        break;
                    }
                }
                xy -= skipCount;

                // determine repeat count
                short v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (skipCount < 2 && xy + skipCount < xymax && repeatCount < 2) {
                    literalCount++;
                    if (literalCount == 127) {
                        out.write(literalCount); // Literal OP-code
                        out.writeShorts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        out.writeShorts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        xy += skipCount - 1;
                        while (skipCount > 0) {
                            out.write(0); // Skip Op-code
                            out.write(min(254 + 1, skipCount + 1)); // Number of bytes to skip + 1
                            skipCount -= min(254, skipCount);
                        }
                    } else {
                        out.write(-repeatCount); // Repeat OP-code
                        out.writeShort(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                out.writeShorts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }

        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Encodes a 24-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeKey24(ImageOutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);
        long headerPos = out.getStreamPosition();

        // Reserve space for the header:
        out.writeInt(0);
        out.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            out.write(1); // this is a key-frame, there is nothing to skip at the start of line

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine repeat count
                int v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (repeatCount < 2) {
                    literalCount++;
                    if (literalCount > 126) {
                        out.write(literalCount); // Literal OP-code
                        writeInts24(out, data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        writeInts24(out, data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    out.write(-repeatCount); // Repeat OP-code
                    writeInt24(out, v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                writeInts24(out, data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Encodes a 24-bit delta frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeDelta24(ImageOutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);

        // Determine whether we can skip lines at the beginning
        int ymin;
        int ymax = offset + height * scanlineStride;
        scanline:
        for (ymin = offset; ymin < ymax; ymin += scanlineStride) {
            int xy = ymin;
            int xymax = ymin + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }


        if (ymin == ymax) {
            // => Frame is identical to previous one
            out.writeInt(4);
            return;
        }

        // Determine whether we can skip lines at the end
        scanline:
        for (; ymax > ymin; ymax -= scanlineStride) {
            int xy = ymax - scanlineStride;
            int xymax = ymax - scanlineStride + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }
        //System.out.println("AnimationCodec ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = out.getStreamPosition();
        out.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines
            out.writeShort(0x0000);
        } else {
            // => we can skip lines at the beginning and/or the end
            out.writeShort(0x0008);
            out.writeShort((ymin - offset) / scanlineStride);
            out.writeShort(0);
            out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
            out.writeShort(0);
        }


        // Encode each scanline
        for (int y = ymin; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            // determine skip count
            int skipCount = 0;
            for (; xy < xymax; ++xy, ++skipCount) {
                if (data[xy] != prev[xy]) {
                    break;
                }
            }
            if (skipCount == width) {
                // => the entire line can be skipped
                out.write(0 + 1); // don't skip any pixels
                out.write(-1); // end of line
                continue;
            }
            out.write(min(254 + 1, skipCount + 1));
            skipCount -= min(254, skipCount);
            while (skipCount > 0) {
                out.write(0); // Skip Op-code
                out.write(min(254 + 1, skipCount + 1)); // Number of bytes to skip + 1
                skipCount -= min(254, skipCount);
            }

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine skip count
                for (skipCount = 0; xy < xymax; ++xy, ++skipCount) {
                    if (data[xy] != prev[xy]) {
                        break;
                    }
                }
                xy -= skipCount;

                // determine repeat count
                int v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (skipCount < 1 && xy + skipCount < xymax && repeatCount < 2) {
                    literalCount++;
                    if (literalCount == 127) {
                        out.write(literalCount); // Literal OP-code
                        writeInts24(out, data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        writeInts24(out, data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        xy += skipCount - 1;
                        while (skipCount > 0) {
                            out.write(0); // Skip Op-code
                            out.write(min(254 + 1, skipCount + 1)); // Number of bytes to skip + 1
                            skipCount -= min(254, skipCount);
                        }
                    } else {
                        out.write(-repeatCount); // Repeat OP-code
                        writeInt24(out, v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                writeInts24(out, data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Encodes a 32-bit key frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeKey32(ImageOutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);
        long headerPos = out.getStreamPosition();

        // Reserve space for the header:
        out.writeInt(0);
        out.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            out.write(1); // this is a key-frame, there is nothing to skip at the start of line

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine repeat count
                int v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (repeatCount < 2) {
                    literalCount++;
                    if (literalCount > 126) {
                        out.write(literalCount); // Literal OP-code
                        out.writeInts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        out.writeInts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    out.write(-repeatCount); // Repeat OP-code
                    out.writeInt(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                out.writeInts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Encodes a 32-bit delta frame.
     *
     * @param out The output stream.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void encodeDelta32(ImageOutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        out.setByteOrder(ByteOrder.BIG_ENDIAN);

        // Determine whether we can skip lines at the beginning
        int ymin;
        int ymax = offset + height * scanlineStride;
        scanline:
        for (ymin = offset; ymin < ymax; ymin += scanlineStride) {
            int xy = ymin;
            int xymax = ymin + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }


        if (ymin == ymax) {
            // => Frame is identical to previous one
            out.writeInt(4);
            return;
        }

        // Determine whether we can skip lines at the end
        scanline:
        for (; ymax > ymin; ymax -= scanlineStride) {
            int xy = ymax - scanlineStride;
            int xymax = ymax - scanlineStride + width;
            for (; xy < xymax; ++xy) {
                if (data[xy] != prev[xy]) {
                    break scanline;
                }
            }
        }
        //System.out.println("AnimationCodec ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = out.getStreamPosition();
        out.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines:
            out.writeShort(0x0000);
        } else {
            // => we can skip lines:
            out.writeShort(0x0008);
            out.writeShort((ymin - offset) / scanlineStride);
            out.writeShort(0);
            out.writeShort((ymax - ymin + 1 - offset) / scanlineStride);
            out.writeShort(0);
        }


        // Encode each scanline
        for (int y = ymin; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            // determine skip count
            int skipCount = 0;
            for (; xy < xymax; ++xy, ++skipCount) {
                if (data[xy] != prev[xy]) {
                    break;
                }
            }
            if (skipCount == width) {
                // => the entire line can be skipped
                out.write(1); // don't skip any pixels
                out.write(-1); // end of line
                continue;
            }
            out.write(Math.min(255, skipCount + 1));
            if (skipCount > 254) {
                skipCount -= 254;
                while (skipCount > 254) {
                    out.write(0); // Skip OP-code
                    out.write(255);
                    skipCount -= 254;
                }
                out.write(0); // Skip OP-code
                out.write(skipCount + 1);
            }

            int literalCount = 0;
            int repeatCount = 0;
            for (; xy < xymax; ++xy) {
                // determine skip count
                for (skipCount = 0; xy < xymax; ++xy, ++skipCount) {
                    if (data[xy] != prev[xy]) {
                        break;
                    }
                }
                xy -= skipCount;

                // determine repeat count
                int v = data[xy];
                for (repeatCount = 0; xy < xymax && repeatCount < 127; ++xy, ++repeatCount) {
                    if (data[xy] != v) {
                        break;
                    }
                }
                xy -= repeatCount;

                if (skipCount < 1 && xy + skipCount < xymax && repeatCount < 2) {
                    literalCount++;
                    if (literalCount == 127) {
                        out.write(literalCount); // Literal OP-code
                        out.writeInts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        out.write(literalCount); // Literal OP-code
                        out.writeInts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 254) {
                            out.write(0); // Skip OP-code
                            out.write(255);
                            xy += 254;
                            skipCount -= 254;
                        }
                        out.write(0); // Skip OP-code
                        out.write(skipCount + 1);
                        xy += skipCount - 1;
                    } else {
                        out.write(-repeatCount); // Repeat OP-code
                        out.writeInt(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                out.write(literalCount);
                out.writeInts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            out.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = out.getStreamPosition();
        out.seek(headerPos);
        out.writeInt((int) (pos - headerPos));
        out.seek(pos);
    }

    /** Decodes a 16-bit delta frame.
     *
     * @param in The input stream.
     * @param data The image data.
     * @param prev The image data of the previous frame. This may be the same
     *             object as data.
     * @param width The width of the image in data elements.
     * @param height The height of the image in data elements.
     * @param offset The offset to the first pixel in the data array.
     * @param scanlineStride The number to append to offset to get to the next scanline.
     */
    public void decodeDelta16(ImageInputStream in, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        in.setByteOrder(ByteOrder.BIG_ENDIAN);

        // Decode chunk size
        // -----------------
        long chunkSize = in.readUnsignedInt();
        if (chunkSize <= 8) {
            return;
        }
        if (in.length() != chunkSize) {
            throw new IOException("Illegal chunk size:" + chunkSize + " expected:" + in.length());
        }
        //System.out.println("chunkSize:" + chunkSize);
        // Decode header
        // -----------------
        int header = in.readUnsignedShort();
        int startingLine;
        int numberOfLines;
        if (header == 0) {
            // decode entire image
            startingLine = 0;
            numberOfLines = height;
        } else if (header == 8) {
            // starting line and number of lines follows
            startingLine = in.readUnsignedShort();
            int reserved1 = in.readUnsignedShort(); // reserved, must be 0x0000
            if (reserved1 != 0) {
                throw new IOException("Illegal value in reserved1 0x" + Integer.toHexString(reserved1));
            }
            numberOfLines = in.readUnsignedShort();
            int reserved2 = in.readUnsignedShort(); // reserved, must be 0x0000
            if (reserved2 != 0) {
                throw new IOException("Illegal value in reserved2 0x" + Integer.toHexString(reserved2));
            }
        } else {
            throw new IOException("Unknown header 0x" + Integer.toHexString(header));
        }
        //System.out.println("startingLine " + startingLine + ", nbLines " + numberOfLines);

        if (startingLine > height || numberOfLines == 0) {
            return;
        }
        if (startingLine + numberOfLines - 1 > height) {
            throw new IOException("Illegal startingLine or numberOfLines, startingLine=" + startingLine + ", numberOfLines=" + numberOfLines);
        }

        // Decode scanlines
        // -----------------
        for (int l = 0; l < numberOfLines; l++) {
            //System.out.println("  l:" + l);
            int i = offset + (startingLine + l) * scanlineStride;

            {
                int skipCode = in.readUnsignedByte() - 1;
                if (skipCode == -1) {
                    //System.out.println("end of image");
                    break; // end of image code
                } else if (skipCode > 0) {
                    //System.out.println("skip " + skipCode);
                    if (data == prev) {
                        i += skipCode;
                    } else {
                        for (int j = 0; j < skipCode; j++) {
                            data[i] = prev[i];
                            i++;
                        }
                    }
                }
            }

            while (true) {
                int opCode = in.readByte();
                if (opCode == 0) {// skip op
                    int skipCode = in.readUnsignedByte() - 1;
                    if (skipCode > 0) {
                        //System.out.println("skip " + skipCode);
                        if (prev != data) {
                            System.arraycopy(prev, i, data, i, skipCode);
                        }
                        i += skipCode;
                    }
                } else if (opCode > 0) { // run of data op
                    //System.out.println("data " + opCode);
                    try {
                        in.readFully(data, i, opCode);
                    } catch (EOFException e) {
                        //System.out.println("EOFException");
                        //System.out.flush();
                        System.exit(5);
                        return;
                    }
                    i += opCode;
                } else if (opCode == -1) { // end of line op
                    //System.out.println("EOL");
                    break;
                } else if (opCode < -1) { // repeat op
                    //System.out.println("repeat "+opCode);
                    short d = in.readShort();
                    int end = i - opCode;
                    while (i < end) {
                        data[i++] = d;
                    }
                }
            }
            assert i <= offset + (startingLine + l + 1) * scanlineStride;
        }
        assert in.getStreamPosition() == in.length();
        /*
         * Header:
         * uint32 chunkSize
         *
         * uint16 header 0x0000 => decode entire image
         *               0x0008 => starting line and number of lines follows
         * if header==0x0008 {
         *   uint16 startingLine at which to begin updating frame
         *   uint16 reserved 0x0000
         *   uint16 numberOfLines to update
         *   uint16 reserved 0x0000
         * }
         * n-bytes compressed lines
         * </pre>
         *
         * The first 4 bytes defines the chunk length. This field also carries some
         * other unknown flags, since at least one of the high bits is sometimes set.<br>
         *
         * If the overall length of the chunk is less than 8, treat the frame as a
         * NOP, which means that the frame is the same as the one before it.<br>
         *
         * Next, there is a header of either 0x0000 or 0x0008. A header value onlyWith
         * bit 3 set (header &amp; 0x0008) indicates that information follows revealing
         * at which line the decode process is to begin:<br>
         *
         * <pre>
         * 2 bytes    starting line at which to begin updating frame
         * 2 bytes    unknown
         * 2 bytes    the number of lines to update
         * 2 bytes    unknown
         * </pre>
         *
         * If the header is 0x0000, then the decode begins from the first line and
         * continues through the entire height of the image.<br>
         *
         * After the header comes the individual RLE-compressed lines. An individual
         * compressed line is comprised of a skip code, followed by a series of RLE
         * codes and pixel data:<br>
         * <pre>
         *  1 byte     skip code
         *  1 byte     RLE code
         *  n bytes    pixel data
         *  1 byte     RLE code
         *  n bytes    pixel data
         * </pre>
         * Each line begins onlyWith a byte that defines the number of pixels to skip in
         * a particular line in the output line before outputting new pixel
         * data. Actually, the skip count is set to one more than the number of
         * pixels to skip. For example, a skip byte of 15 means "skip 14 pixels",
         * while a skip byte of 1 means "don't skip any pixels". If the skip byte is
         * 0, then the frame decode is finished. Therefore, the maximum skip byte
         * value of 255 allows for a maximum of 254 pixels to be skipped.
         */
    }
}
