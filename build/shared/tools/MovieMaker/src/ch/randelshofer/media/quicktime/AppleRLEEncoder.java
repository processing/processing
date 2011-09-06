/*
 * @(#)AppleRLEEncoder.java  1.3  2011-01-16
 *
 * Copyright © 2011 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.quicktime;

import java.io.IOException;
import java.io.OutputStream;

import ch.randelshofer.io.SeekableByteArrayOutputStream;

/**
 * Implements the run length encoding of the Apple QuickTime Animation (RLE)
 * format.
 * <p>
 * An RLE-encoded frame has the following format:
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
 * Next, there is a header of either 0x0000 or 0x0008. A header value with
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
 * Each line begins with a byte that defines the number of pixels to skip in
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
 * <li>16-bit data: Each pixel is represented by a 16-bit RGB value with 5 bits
 * used for each of the red, green, and blue color components and 1 unused bit
 * to round the value tmp to 16 bits: {@code xrrrrrgg gggbbbbb}. Pixel data is
 * rendered to the output frame one pixel at a time.<br>
 * If (code &gt; 0), copy the run of (code) pixels from the encoded stream to
 * the output.<br>
 * If (code &lt; -1), unpack the next 16-bit RGB value from the encoded stream
 * and render it to the output frame -(code) times.</li>
 *
 * <li>24-bit data: Each pixel is represented by a 24-bit RGB value with 8 bits
 * (1 byte) used for each of the red, green, and blue color components:
 * {@code rrrrrrrr gggggggg bbbbbbbb}. Pixel data is rendered to the output
 * frame one pixel at a time.<br>
 * If (code &gt; 0), copy the run of (code) pixels from the encoded stream to
 * the output.<br>
 * If (code &lt; -1), unpack the next 24-bit RGB value from the encoded stream
 * and render it to the output frame -(code) times.</li>
 *
 * <li>32-bit data: Each pixel is represented by a 32-bit ARGB value with 8 bits
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
public class AppleRLEEncoder {

    private SeekableByteArrayOutputStream tmpSeek = new SeekableByteArrayOutputStream();
    private DataAtomOutputStream tmp = new DataAtomOutputStream(tmpSeek);

    /** Encodes a 16-bit key frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();
        long headerPos = tmpSeek.getStreamPosition();

        // Reserve space for the header:
        tmp.writeInt(0);
        tmp.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            tmp.write(1); // this is a key-frame, there is nothing to skip at the start of line

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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeShorts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeShorts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    tmp.write(-repeatCount); // Repeat OP-code
                    tmp.writeShort(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeShorts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }

    /** Encodes a 16-bit delta frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta16(OutputStream out, short[] data, short[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();

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
            tmp.writeInt(4);
            tmpSeek.toOutputStream(out);
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
        //System.out.println("AppleRLEEncoder ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = tmpSeek.getStreamPosition();
        tmp.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines:
            tmp.writeShort(0x0000);
        } else {
            // => we can skip lines:
            tmp.writeShort(0x0008);
            tmp.writeShort(ymin / scanlineStride);
            tmp.writeShort(0);
            tmp.writeShort((ymax - ymin + 1) / scanlineStride);
            tmp.writeShort(0);
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
                tmp.write(1); // don't skip any pixels
                tmp.write(-1); // end of line
                continue;
            }
            tmp.write(Math.min(255, skipCount + 1));
            if (skipCount > 254) {
                skipCount -= 254;
                while (skipCount > 254) {
                    tmp.write(0); // Skip OP-code
                    tmp.write(255);
                    skipCount -= 254;
                }
                tmp.write(0); // Skip OP-code
                tmp.write(skipCount + 1);
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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeShorts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeShorts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 254) {
                            tmp.write(0); // Skip OP-code
                            tmp.write(255);
                            xy += 254;
                            skipCount -= 254;
                        }
                        tmp.write(0); // Skip OP-code
                        tmp.write(skipCount + 1);
                        xy += skipCount - 1;
                    } else {
                        tmp.write(-repeatCount); // Repeat OP-code
                        tmp.writeShort(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeShorts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }

    /** Encodes a 24-bit key frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();
        long headerPos = tmpSeek.getStreamPosition();

        // Reserve space for the header:
        tmp.writeInt(0);
        tmp.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            tmp.write(1); // this is a key-frame, there is nothing to skip at the start of line

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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts24(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts24(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    tmp.write(-repeatCount); // Repeat OP-code
                    tmp.writeInt24(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeInts24(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }

    /** Encodes a 24-bit delta frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta24(OutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();

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
            tmp.writeInt(4);
            tmpSeek.toOutputStream(out);
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
        //System.out.println("AppleRLEEncoder ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = tmpSeek.getStreamPosition();
        tmp.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines:
            tmp.writeShort(0x0000);
        } else {
            // => we can skip lines:
            tmp.writeShort(0x0008);
            tmp.writeShort(ymin / scanlineStride);
            tmp.writeShort(0);
            tmp.writeShort((ymax - ymin + 1) / scanlineStride);
            tmp.writeShort(0);
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
                tmp.write(1); // don't skip any pixels
                tmp.write(-1); // end of line
                continue;
            }
            tmp.write(Math.min(255, skipCount + 1));
            if (skipCount > 254) {
                skipCount -= 254;
                while (skipCount > 254) {
                    tmp.write(0); // Skip OP-code
                    tmp.write(255);
                    skipCount -= 254;
                }
                tmp.write(0); // Skip OP-code
                tmp.write(skipCount + 1);
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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts24(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts24(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 254) {
                            tmp.write(0); // Skip OP-code
                            tmp.write(255);
                            xy += 254;
                            skipCount -= 254;
                        }
                        tmp.write(0); // Skip OP-code
                        tmp.write(skipCount + 1);
                        xy += skipCount - 1;
                    } else {
                        tmp.write(-repeatCount); // Repeat OP-code
                        tmp.writeInt24(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeInts24(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }

    /** Encodes a 32-bit key frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeKey32(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();
        long headerPos = tmpSeek.getStreamPosition();

        // Reserve space for the header:
        tmp.writeInt(0);
        tmp.writeShort(0x0000);

        // Encode each scanline
        int ymax = offset + height * scanlineStride;
        for (int y = offset; y < ymax; y += scanlineStride) {
            int xy = y;
            int xymax = y + width;

            tmp.write(1); // this is a key-frame, there is nothing to skip at the start of line

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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    tmp.write(-repeatCount); // Repeat OP-code
                    tmp.writeInt(v);
                    xy += repeatCount - 1;
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeInts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }

    /** Encodes a 32-bit delta frame.
     *
     * @param tmp The output stream. Must be set to Big-Endian.
     * @param data The image data.
     * @param prev The image data of the previous frame.
     * @param offset The offset to the first pixel in the data array.
     * @param width The width of the image in data elements.
     * @param scanlineStride The number to add to offset to get to the next scanline.
     */
    public void writeDelta32(OutputStream out, int[] data, int[] prev, int width, int height, int offset, int scanlineStride)
            throws IOException {
        tmpSeek.reset();

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
            tmp.writeInt(4);
            tmpSeek.toOutputStream(out);
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
        //System.out.println("AppleRLEEncoder ymin:" + ymin / step + " ymax" + ymax / step);

        // Reserve space for the header
        long headerPos = tmpSeek.getStreamPosition();
        tmp.writeInt(0);

        if (ymin == offset && ymax == offset + height * scanlineStride) {
            // => we can't skip any lines:
            tmp.writeShort(0x0000);
        } else {
            // => we can skip lines:
            tmp.writeShort(0x0008);
            tmp.writeShort(ymin / scanlineStride);
            tmp.writeShort(0);
            tmp.writeShort((ymax - ymin + 1) / scanlineStride);
            tmp.writeShort(0);
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
                tmp.write(1); // don't skip any pixels
                tmp.write(-1); // end of line
                continue;
            }
            tmp.write(Math.min(255, skipCount + 1));
            if (skipCount > 254) {
                skipCount -= 254;
                while (skipCount > 254) {
                    tmp.write(0); // Skip OP-code
                    tmp.write(255);
                    skipCount -= 254;
                }
                tmp.write(0); // Skip OP-code
                tmp.write(skipCount + 1);
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
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts(data, xy - literalCount + 1, literalCount);
                        literalCount = 0;
                    }
                } else {
                    if (literalCount > 0) {
                        tmp.write(literalCount); // Literal OP-code
                        tmp.writeInts(data, xy - literalCount, literalCount);
                        literalCount = 0;
                    }
                    if (xy + skipCount == xymax) {
                        // => we can skip until the end of the line without
                        //    having to write an op-code
                        xy += skipCount - 1;
                    } else if (skipCount >= repeatCount) {
                        while (skipCount > 254) {
                            tmp.write(0); // Skip OP-code
                            tmp.write(255);
                            xy += 254;
                            skipCount -= 254;
                        }
                        tmp.write(0); // Skip OP-code
                        tmp.write(skipCount + 1);
                        xy += skipCount - 1;
                    } else {
                        tmp.write(-repeatCount); // Repeat OP-code
                        tmp.writeInt(v);
                        xy += repeatCount - 1;
                    }
                }
            }

            // flush literal run
            if (literalCount > 0) {
                tmp.write(literalCount);
                tmp.writeInts(data, xy - literalCount, literalCount);
                literalCount = 0;
            }

            tmp.write(-1);// End of line OP-code
        }


        // Complete the header
        long pos = tmpSeek.getStreamPosition();
        tmpSeek.seek(headerPos);
        tmp.writeInt((int) (pos - headerPos));
        tmpSeek.seek(pos);
        tmpSeek.toOutputStream(out);
    }
/*
    public static void main(String[] args) {
        BufferedImage img=new BufferedImage(640,400,BufferedImage.TYPE_INT_RGB);
        BufferedImage subImg = img.getSubimage(10, 20, 320, 200);
        BufferedImage subsubImg = subImg.getSubimage(10, 10, 160, 100);
        Graphics2D g = img.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, 800, 600);
        g.setColor(Color.BLUE);
        g.drawRect(0, 0, 320-1, 400-1);
        g.setColor(Color.RED);
        g.drawRect(10, 20, 320-1, 200-1);
         QuickTimeWriter qtr=null;
        try {
             qtr = new QuickTimeWriter(new File("RLE SubImg.mov"));
            qtr.addVideoTrack("rle ","Animation", 600, 160, 100,24,30);
            qtr.writeFrame(0, subsubImg, 100);
            g.setColor(Color.GREEN);
            g.drawRect(20,30, 160-1, 100-1);
            qtr.writeFrame(0, subsubImg, 100);
            qtr.close();
            qtr=null;
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (qtr!=null) {
                try {
                    qtr.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            g.dispose();
        }

    }
    public static void main1(String[] args) {
        int w = 800, h = 600;
        int[] data = new int[w * h];
        int[] prev = new int[w * h];

        Random rnd = new Random();
        int repeat = 0;
        for (int i = 0; i < data.length; ++i) {
            if (--repeat > 0) {
                data[i] = data[i - 1];
            } else {
                repeat = rnd.nextInt(10);
                data[i] = rnd.nextInt(1 << 24);
            }
        }
        ArrayIndexOutOfBoundsException ai;

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        AppleRLEEncoder enc = new AppleRLEEncoder();
        try {
            enc.writeKey24(buf, data, w-2, h, 1, w);
            buf.close();
            byte[] result = buf.toByteArray();
            int fullSize=(w-2)*h*3;
            System.out.println("full size:" + fullSize);
            System.out.println("compressed size:" + result.length);
            System.out.println("compression percentage:" + (100 * (result.length / (float) fullSize)));
            /*            System.out.println(Arrays.toString(result));

            System.out.print("0x [");
            for (int i = 0; i < result.length; ++i) {
            if (i != 0) {
            System.out.print(',');
            }
            String hex = "00" + Integer.toHexString(result[i]);
            System.out.print(hex.substring(hex.length() - 2));
            }
            System.out.println(']');
             * /
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main2(String[] args) {
        short[] data = {//
            8, 1, 1, 1, 1, 2, 8,//
            8, 0, 2, 0, 0, 0, 8,//
            8, 2, 3, 4, 4, 3, 8,//
            8, 2, 2, 3, 4, 4, 8,//
            8, 1, 4, 4, 4, 5, 8};
        short[] prev = {//
            8, 1, 1, 1, 1, 1, 8, //
            8, 5, 5, 5, 5, 0, 8,//
            8, 3, 3, 3, 3, 3, 8,//
            8, 2, 2, 0, 0, 0, 8,//
            8, 2, 0, 0, 0, 5, 8};

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        AppleRLEEncoder enc = new AppleRLEEncoder();
        try {
//            enc.writeDelta16(buf, data, prev, 1, 5, 7);
            enc.writeKey16(buf, data, 5, 5, 1, 7);
            buf.close();
            byte[] result = buf.toByteArray();
            System.out.println("size:" + result.length);
            System.out.println(Arrays.toString(result));

            System.out.print("0x [");
            for (int i = 0; i < result.length; i++) {
                if (i != 0) {
                    System.out.print(',');
                }
                String hex = "00" + Integer.toHexString(result[i]);
                System.out.print(hex.substring(hex.length() - 2));
            }
            System.out.println(']');
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    */
}
