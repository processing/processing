/*
 * @(#)RawCodec.java  1.0  2011-03-15
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance onlyWith the
 * license agreement you entered into onlyWith Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import org.monte.media.AbstractVideoCodec;
import org.monte.media.Buffer;
import org.monte.media.Format;
import org.monte.media.io.SeekableByteArrayOutputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import static org.monte.media.VideoFormatKeys.*;
import static org.monte.media.BufferFlag.*;

/**
 * {@code RawCodec} encodes a BufferedImage as a byte[] array.
 * <p>
 * This codec does not encode the color palette of an image. This must be done
 * separately.
 * <p>
 * The pixels of a frame are written row by row from top to bottom and from
 * the left to the right.
 * <p>
 * Supported input formats:
 * <ul>
 * {@code VideoFormat} onlyWith {@code BufferedImage.class}, any width, any height,
 * depth=4.
 * </ul>
 * Supported output formats:
 * <ul>
 * {@code VideoFormat} onlyWith {@code byte[].class}, same width and height as input
 * format, depth=4.
 * </ul>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-15 Created.
 */
public class RawCodec extends AbstractVideoCodec {

    public RawCodec() {
        super(new Format[]{
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, 
                            EncodingKey, ENCODING_BUFFERED_IMAGE), //
                },
                new Format[]{
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_RAW, DataClassKey, byte[].class, DepthKey, 8), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_RAW, DataClassKey, byte[].class, DepthKey, 16), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_RAW, DataClassKey, byte[].class, DepthKey, 24), //
                    new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME,
                    EncodingKey, ENCODING_QUICKTIME_RAW, DataClassKey, byte[].class, DepthKey, 32), //
                });
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
    public void writeKey8(OutputStream out, byte[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            out.write(data, xy, width);
        }
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
    public void writeKey16(OutputStream out, short[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 2]; // holds a scanline of raw image data onlyWith 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x = 0, i = 0; x < width; x++, i += 2) {
                int pixel = data[xy + x];
                bytes[i] = (byte) (pixel >> 8);
                bytes[i + 1] = (byte) (pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
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
    public void writeKey24(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 3]; // holds a scanline of raw image data onlyWith 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x = 0, i = 0; x < width; x++, i += 3) {
                int pixel = data[xy + x];
                bytes[i] = (byte) (pixel >> 16);
                bytes[i + 1] = (byte) (pixel >> 8);
                bytes[i + 2] = (byte) (pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
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
    public void writeKey32(OutputStream out, int[] data, int width, int height, int offset, int scanlineStride)
            throws IOException {

        // Write the samples
        byte[] bytes = new byte[width * 4]; // holds a scanline of raw image data onlyWith 3 channels of 32 bit data
        for (int xy = offset, ymax = offset + height * scanlineStride; xy < ymax; xy += scanlineStride) {
            for (int x = 0, i = 0; x < width; x++, i += 4) {
                int pixel = data[xy + x];
                bytes[i] = (byte) (pixel >> 24);
                bytes[i + 1] = (byte) (pixel >> 16);
                bytes[i + 2] = (byte) (pixel >> 8);
                bytes[i + 3] = (byte) (pixel);
            }
            out.write(bytes, 0, bytes.length);
        }
    }

    /** Encodes a 24-bit key frame.
     *
     * @param out The output stream.
     * @param image The image.
     */
    public void writeKey24(OutputStream out, BufferedImage image)
            throws IOException {

        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();
        int[] rgb = new int[width * 3]; // holds a scanline of raw image data onlyWith 3 channels of 32 bit data
        byte[] bytes = new byte[width * 3]; // holds a scanline of raw image data onlyWith 3 channels of 8 bit data
        for (int y = 0; y < height; y++) {
            // Note: Method getPixels is very slow as it does sample conversions for us
            rgb = raster.getPixels(0, y, width, 1, rgb);
            for (int k = 0, n = width * 3; k < n; k++) {
                bytes[k] = (byte) rgb[k];
            }
            out.write(bytes);
        }
    }

    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        if (in.isFlag(DISCARD)) {
            return CODEC_OK;
        }
        out.format = outputFormat;

        SeekableByteArrayOutputStream tmp;
        if (out.data instanceof byte[]) {
            tmp = new SeekableByteArrayOutputStream((byte[]) out.data);
        } else {
            tmp = new SeekableByteArrayOutputStream();
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

        try {
            switch (vf.get(DepthKey)) {
                case 8: {
                    writeKey8(tmp, getIndexed8(in), r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 16: {
                    writeKey16(tmp, getRGB15(in), r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 24: {
                    writeKey24(tmp, getRGB24(in), r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                case 32: {
                    writeKey24(tmp, getARGB32(in), r.width, r.height, r.x + r.y * scanlineStride, scanlineStride);
                    break;
                }
                default: {
                    out.setFlag(DISCARD);
                    return CODEC_FAILED;
                }
            }

            out.format = outputFormat;
            out.sampleCount = 1;
            out.setFlag(KEYFRAME);
            out.data = tmp.getBuffer();
            out.offset = 0;
            out.length = (int) tmp.getStreamPosition();
            return CODEC_OK;
        } catch (IOException ex) {
            ex.printStackTrace();
            out.setFlag(DISCARD);
            return CODEC_FAILED;
        }
    }
}
