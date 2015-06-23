/*
 * @(#)MP3ElementaryInputStream.java  1.1  2011-01-17
 *
 * Copyright © 2010 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.mp3;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import javax.sound.sampled.AudioFormat;

/**
 * Facilitates reading of an MP3 elementary stream frame by frame.
 * <p>
 * An MP3 frame has a 32-bit header with the following contents in big endian
 * order:
 * <ul>
 * <li>bit 31-21, MP3 Sync Word, all bits must be set</li>
 * <li>bit 20-19, Version, 00=MPEG 2.5,01=reserved,10=MPEG 2, 11=MPEG 1</li>
 * <li>bit 18-17, Layer, 00=reserved, 01=layer 3, 10=layer 2, 11=layer 1</li>
 * <li>bit 16, Error protection, 0=16 bit CRC follows header, 1=Not protected</li>
 * <li>bit 15-12, Bit Rate in kbps, interpretation depends on version and layer</li>
 * <li>bit 11-10, Frequency, interpretation depends on version</li>
 * <li>bit 9, Pad Bit, 0=frame is not padded, 1=frame is padded to exactly fit the bit rate</li>
 * <li>bit 8, Private bit, only informative</li>
 * <li>bit 7-6, Channel Mode, 00=stereo, 01=joint stereo, 10=dual channel (2 mono channels), 11=single channel (mono)</li>
 * <li>bit 5-4, Mode Extension (only used with Joint Stereo), interpretation depends on version and layer</li>
 * <li>bit 3, Copyright, 0=not copyrighted, 1=copyrighted</li>
 * <li>bit 2, Original, 0=Copy of original media,1=original media</li>
 * <li>bit 1-0, Emphasis, 00=none,01=50/15ms,10=reserved,11=CCIT J.17</li>
 * </ul>
 * <p>
 * Reference:<br>
 * <a href="http://en.wikipedia.org/wiki/MP3">http://en.wikipedia.org/wiki/MP3</a><br>
 * <a href="http://www.datavoyage.com/mpgscript/mpeghdr.htm">http://www.datavoyage.com/mpgscript/mpeghdr.htm</a><br>
 * <a href="http://www.mp3-tech.org/programmer/frame_header.html">http://www.mp3-tech.org/programmer/frame_header.html</a><br>
 * <a href="http://lame.sourceforge.net/tech-FAQ.txt">http://lame.sourceforge.net/tech-FAQ.txt</a><br>
 * <a href0"http://www.altera.com/literature/dc/1.4-2005_Taiwan_2nd_SouthernTaiwanU-web.pdf">http://www.altera.com/literature/dc/1.4-2005_Taiwan_2nd_SouthernTaiwanU-web.pdf</a><br>
 *
 * @author Werner Randelshofer
 * @version 1.1 2011-01-17 Renamed getHeader() to getHeaderCode().
 * <br>1.0 2011-01-03 Created.
 */
public class MP3ElementaryInputStream extends FilterInputStream {

    /** Defines the "MP3" encoding. */
    public final static AudioFormat.Encoding MP3 = new AudioFormat.Encoding("MP3");
    private Frame frame;
    private long pos;
    private final static int[][] BIT_RATES = { // All values are in kbps
        // V1 - MPEG Version 1
        // V2 - MPEG Version 2 and Version 2.5
        // L1 - Layer I
        // L2 - Layer II
        // L3 - Layer III
        //
        // V1L1, V1L2, V1L3, V2L1, V2L2&L3
        {-1, -1, -1, -1, -1}, // free
        {32, 32, 32, 32, 8},
        {64, 48, 40, 48, 16},
        {96, 56, 48, 56, 24},
        {128, 64, 56, 64, 32},
        {160, 80, 64, 80, 40},
        {192, 96, 80, 96, 48},
        {224, 112, 96, 112, 56},
        {256, 128, 112, 128, 64},
        {288, 160, 128, 144, 80},
        {320, 192, 160, 160, 96},
        {352, 224, 192, 176, 112},
        {384, 256, 224, 192, 128},
        {416, 320, 256, 224, 144},
        {448, 384, 320, 256, 160},
        {-2, -2, -2, -2, -2}, // bad
    };
    private final static int[][] SAMPLE_RATES = { // All values are in Hz
        // V1 - MPEG Version 1
        // V2 - MPEG Version 2
        // V25 - MPEG Version 2.5
        //
        // V1, V2, V25
        {44100, 22050, 11025},
        {48000, 24000, 12000},
        {32000, 16000, 8000},
        {-1, -1, -1}, // reserved
    };

    /** An elementary frame. */
    public static class Frame {

        /** 32-bit header. */
        private int header;
        /** 16-bit CRC. */
        private int crc;
        /** The size of the data in the frame. */
        private int bodySize;
        /** The offset of the data in the frame. */
        private long bodyOffset;

        /** Creates a new frame.
         *
         * @param header The 32-bit Frame header
         */
        public Frame(int header) {
            this.header = header;
        }

        /** Returns the raw 32-bit header code as it is stored in the file. */
        public int getHeaderCode() {
            return header;

        }

        /** Returns the version number: 1=MPEG 1, 2=MPEG 2, 25=MPEG 2.5; -1=unknown. */
        public int getVersion() {
            switch (getVersionCode()) {
                case 0:
                    return 25;
                case 2:
                    return 2;
                case 3:
                    return 1;
                default:
                    return -1;
            }
        }

        /** Returns the version code as it is stored in the
         * header. 3=MPEG 1, 2=MPEG 2, 1=reserved, 0=MPEG 2.5. */
        public int getVersionCode() {
            return (header >>> 19) & 3;
        }

        /** Returns the layer number.
         * 1=Layer I, 2=Layer II, 3=Layer III, -1=unknown. */
        public int getLayer() {
            switch (getLayerCode()) {
                case 1:
                    return 3;
                case 2:
                    return 2;
                case 3:
                    return 1;
                default:
                    return -1;
            }
        }

        /** Returns the raw layer code as it is stored in the header.
         * 3=Layer I, 2=Layer II, 1=Layer III, 0=reserved. */
        public int getLayerCode() {
            return (header >>> 17) & 3;
        }

        /** Returns the bitrate of the frame. Returns -1 if unknown.
         */
        public int getBitRate() {
            if (getVersion() < 0 || getLayer() < 0) {
                return -1;
            }
            int v = getVersion() == 1 ? 0 : 3;
            int l = getVersion() == 1 ? getLayer() - 1 : (getLayer() == 1 ? 0 : 1);
            return BIT_RATES[getBitRateCode()][v + l];
        }

        /** Returns the raw bitrate code as it is stored in the header.
         */
        public int getBitRateCode() {
            return (header >>> 12) & 15;
        }

        /** Returns true if this frame has a CRC. */
        public boolean hasCRC() {
            return ((header >>> 16) & 1) == 0;
        }

        /** Returns the CRC of this frame. The value is only valid if hasCRC() returns true. */
        public int getCRC() {
            return crc;
        }

        public boolean hasPadding() {
            return ((header >>> 9) & 1) == 1;
        }

        /** Returns the sample rate in Hz.
         * Returns -1 if unknown.
         */
        public int getSampleRate() {
            if (getVersion() < 0 || getLayer() < 0) {
                return -1;
            }
            int v = getVersion() == 25 ? 2 : getVersion() - 1;
            return SAMPLE_RATES[getSampleRateCode()][v];
        }

        /** Returns the raw sample rate code as it is stored in the header.
         */
        public int getSampleRateCode() {
            return (header >>> 10) & 3;
        }

        /** Returns the number of samples in the frame.
         * It is constant and always 384 samples for Layer I and 1152 samples
         * for Layer II and Layer III.
         * Returns -1 if unknown.
         */
        public int getSampleCount() {
            if (getLayer() < 0) {
                return -1;
            }
            return (getLayer() == 1 ? 192 : 576) * getChannelCount();
        }

        /** Returns the number of channels.
         * @return 1=mono, 2=stereo, joint stereo or dual channel.
         */
        public int getChannelCount() {
            return getChannelModeCode() == 3 ? 1 : 2;
        }

        /** Returns the sample size in bits. Always 16 bit per sample. */
        public int getSampleSize() {
            return 16;
        }

        /** Returns the raw channel mode code as stored in the header.
         *
         * @return 0=stereo, 1=joint stereo, 2=dual channel, 3=single channel (mono).
         */
        public int getChannelModeCode() {
            return (header >>> 6) & 3;
        }

        /** Returns the frame header as a byte array. */
        public byte[] headerToByteArray() {
            byte[] data = new byte[hasCRC() ? 6 : 4];
            headerToByteArray(data, 0);
            return data;
        }

        /** Writes the frame header into the specified byte array.
         * Returns the number of bytes written.
         */
        public int headerToByteArray(byte[] data, int offset) {
            if (data.length - offset < getHeaderSize()) {
                throw new IllegalArgumentException("data array is too small");
            }
            data[offset + 0] = (byte) (header >>> 24);
            data[offset + 1] = (byte) (header >>> 16);
            data[offset + 2] = (byte) (header >>> 8);
            data[offset + 3] = (byte) (header >>> 0);
            if (hasCRC()) {
                data[offset + 4] = (byte) (crc >>> 8);
                data[offset + 5] = (byte) (crc >>> 0);
            }
            return getHeaderSize();
        }

        /** Writes the frame header into the specified output stream. */
        public void writeHeader(OutputStream out) throws IOException {
            out.write((header >>> 24));
            out.write((header >>> 16));
            out.write((header >>> 8));
            out.write((header >>> 0));
            if (hasCRC()) {
                out.write((crc >>> 8));
                out.write((crc >>> 0));
            }
        }

        /** Returns the offset of the frame in the input stream. */
        public long getFrameOffset() {
            return getBodyOffset() - getHeaderSize();
        }

        /** Returns the size of the frame in bytes.
         * This size includes the header, the data and the padding.
         */
        public int getFrameSize() {
            return getHeaderSize() + getBodySize();
        }

        /** Returns the offset of the header in the input stream. */
        public long getHeaderOffset() {
            return getFrameOffset();
        }

        /** Returns the size of the header in bytes. */
        public int getHeaderSize() {
            return hasCRC() ? 6 : 4;
        }

        /** Returns the offset of the side info in the input stream. */
        public long getSideInfoOffset() {
            return bodyOffset;
        }

        /** Returns the size of the side info in bytes.
         * It is 17 bytes long in a single channel frame and 32 bytes in dual
         * channel or stereo channel.
         */
        public int getSideInfoSize() {
            return getChannelCount() == 1 ? 17 : 32;
        }

        /** Returns the offset of the frame body in the input stream. */
        public long getBodyOffset() {
            return bodyOffset;
        }

        /** Returns the size of the frame body in bytes.
         * The body includes the side info, the audio data, and the padding.
         */
        public int getBodySize() {
            return bodySize;
        }

        /** Padding is used to fit the bit rates exactly.
         * For an example: 128k 44.1kHz layer II uses a lot of 418 bytes and
         * some of 417 bytes long frames to get the exact 128k bitrate. For
         * Layer I slot is 32 bits long, for Layer II and Layer III slot is 8
         * bits long. */
        public int getPaddingSize() {
            if (hasPadding()) {
                return getLayer() == 1 ? 4 : 1;
            }
            return 0;
        }

        private float getFrameRate() {
            return (float) getSampleRate() / getSampleCount();
        }
    }

    public MP3ElementaryInputStream(File file) throws IOException {
        super(new PushbackInputStream(new BufferedInputStream(new FileInputStream(file)), 6));
    }

    public MP3ElementaryInputStream(InputStream in) {
        super(new PushbackInputStream(in, 6));
    }

    /** Gets the next frame from the input stream.
     * Positions the stream in front of the frame header.
     */
    public Frame getNextFrame() throws IOException {
        while (frame != null && pos < frame.getBodyOffset() + frame.getBodySize()) {
            long skipped = skip(frame.getBodyOffset() + frame.getBodySize() - pos);
            if (skipped < 0) {
                break;
            }
        }

        while (true) {
            int b = read0();
            if (b == -1) {
                frame = null;
                break;
            } else if (b == 255) {
                int h0 = b;
                int h1 = read0();
                if (h1 != -1 && (h1 & 0xe0) == 0xe0) {
                    int h2 = read0();
                    int h3 = read0();
                    if (h3 != -1) {
                        frame = new Frame((h0 << 24) | (h1 << 16) | (h2 << 8) | h3);
                        if (frame.getBitRate() == -1 || frame.getLayer() == -1 || frame.getSampleRate() == -1) {
                            // => the header is corrupt: push back 3 bytes
                            PushbackInputStream pin = (PushbackInputStream) in;
                            pin.unread(h3);
                            pin.unread(h2);
                            pin.unread(h1);
                            pos -= 3;
                            continue;
                        }

                        int crc0 = -1, crc1 = -1;
                        if (frame.hasCRC()) {
                            crc0 = read0();
                            crc1 = read0();
                            if (crc1 == -1) {
                                throw new EOFException();
                            }
                            frame.crc = (crc0 << 8) | crc1;
                        }
                        frame.bodyOffset = pos;
                        if (frame.getBitRate() <= 0 || frame.getSampleRate() <= 0) {
                            frame.bodySize = 0;
                        } else if (frame.getLayer() == 1) {
                            frame.bodySize = (int) ((12000L * frame.getBitRate() / frame.getSampleRate()) * 4) - frame.getHeaderSize() + frame.getPaddingSize();
                        } else if (frame.getLayer() == 2 || frame.getLayer() == 3) {
                            if (frame.getChannelCount() == 1) {
                                frame.bodySize = (int) (72000L * frame.getBitRate() / (frame.getSampleRate() + frame.getPaddingSize())) - frame.getHeaderSize() + frame.getPaddingSize();
                            } else {
                                frame.bodySize = (int) (144000L * frame.getBitRate() / (frame.getSampleRate() + frame.getPaddingSize())) - frame.getHeaderSize() + frame.getPaddingSize();
                            }
                        }
                        PushbackInputStream pin = (PushbackInputStream) in;
                        if (frame.hasCRC()) {
                            pin.unread(crc1);
                            pin.unread(crc0);
                            pos -= 2;
                        }
                        pin.unread(h3);
                        pin.unread(h2);
                        pin.unread(h1);
                        pin.unread(h0);
                        pos -= 4;
                        assert pos == frame.getFrameOffset() : pos + "!=" + frame.getFrameOffset();
                        break;
                    }
                }
            }
        }
        return frame;
    }

    /** Returns the current frame. */
    public Frame getFrame() {
        return frame;
    }

    /** Gets the format of the current frame.
     * Returns null if the input stream is not positioned on a frame, or the frame
     * is not valid.
     * @return AudioFormat of current frame or null.
     */
    public AudioFormat getFormat() {
        if (frame == null) {
            return null;
        } else {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put("vbr", true);
            return new AudioFormat(MP3, //
                    frame.getSampleRate(), frame.getSampleSize(), frame.getChannelCount(),//
                    frame.getFrameSize(), frame.getFrameRate(), true, properties);
        }
    }

    private int read0() throws IOException {
        int b = super.read();
        if (b != -1) {
            pos++;
        }
        return b;
    }

    /** Reads a byte from the current frame (its header and its data).
     * Returns -1 on an attempt to read past the end of the frame.
     */
    @Override
    public int read() throws IOException {
        if (frame == null || pos >= frame.getBodyOffset() + frame.getBodySize()) {
            return -1;
        }
        return read0();
    }

    /** Reads up to {@code len} bytes from the current frame (its header and its data).
     * May read less then {@code len} bytes. Returns the actual number of bytes read.
     * Returns -1 on an attempt to read past the end of the frame.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (frame == null) {
            return -1;
        }
        int maxlen = (int) (frame.getBodyOffset() + frame.getBodySize() - pos);
        if (maxlen < 1) {
            return -1;
        }
        len = Math.min(maxlen, len);
        int count = super.read(b, off, len);
        if (count != -1) {
            pos += count;
        }
        return count;
    }

    /**
     * Reads {@code b.length} bytes from the current frame (its header and its data).
     * @throws {@code IOException} on an attempt to read past the end of the frame.
     */
    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * Reads {@code len} bytes from the current frame (its header and its data).
     * @throws {@code IOException} on an attempt to read past the end of the frame.
     */
    public final void readFully(byte b[], int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < len) {
            int count = in.read(b, off + n, len - n);
            if (count < 0) {
                throw new EOFException();
            }
            n += count;
            pos += count;
        }
    }

    /** Skips up to {@code n} bytes from the current frame (its header and its data).
     * Returns the actual number of bytes that have been skipped.
     * Returns -1 on an attempt to skip past the end of the frame.
     */
    @Override
    public long skip(long n) throws IOException {
        if (frame == null) {
            return -1;
        }
        int maxlen = (int) (frame.getBodyOffset() + frame.getBodySize() - pos);
        if (maxlen < 1) {
            return -1;
        }
        n = Math.min(maxlen, n);
        long skipped = in.skip(n);
        if (skipped > 0) {
            pos += skipped;
        }
        return skipped;
    }

    /**
     * Returns the current position in the stream.
     * @return The stream position.
     */
    public long getStreamPosition() {
        return pos;
    }
}
