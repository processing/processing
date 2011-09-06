/*
 * @(#)MP3AudioInputStream.java  1.0  2011-01-01
 *
 * Copyright © 2010 Werner Randelshofer, Immensee, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package ch.randelshofer.media.mp3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * {@code AudioInputStream} adapter for {@link MP3ElementaryInputStream}.
 * <p>
 * Unlike a regular audio input stream, an MP3 audio input stream can have a
 * variable frame size and can change its encoding method in mid-stream.
 * Therefore method getFormat can return different values for each frame,
 * and mark/reset is not supported, and method getFrameLength can not return
 * the total number of frames in the stream.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-01-01 Created.
 */
public class MP3AudioInputStream extends AudioInputStream {

    private MP3ElementaryInputStream in;

    /** Creates an MP3AudioInputStream and reads the stream until the first
     * frame is reached.
     *
     * @param file A File.
     * @throws IOException if the file does not contain an MP3 elementary stream.
     */
    public MP3AudioInputStream(File file) throws IOException {
        this(new BufferedInputStream(new FileInputStream(file)));
    }

    /** Creates an MP3AudioInputStream and reads the stream until the first
     * frame is reached.
     *
     * @param in An InputStream.
     * @throws IOException if the stream does not contain an MP3 elementary stream.
     */
    public MP3AudioInputStream(InputStream in) throws IOException {
        // Feed superclass with nonsense - we override all methods anyway.
        super(null, new AudioFormat(MP3ElementaryInputStream.MP3, 44100, 16, 2, 626, 44100f / 1152f, true), -1);
        this.in = new MP3ElementaryInputStream(in);
        if (this.in.getNextFrame() == null) {
            throw new IOException("Stream is not an MP3 elementary stream");
        }
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    /** Returns the format of the <i>next</i> frame. Returns null if the stream
     * is not positioned inside a frame.
     */
    @Override
    public AudioFormat getFormat() {
        return in.getFormat();
    }

    /** Returns -1 because we don't know how many frames the stream has. */
    @Override
    public long getFrameLength() {
        return -1;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /** Throws an IOException, because the frame size is greater than 1. */
    @Override
    public int read() throws IOException {
        throw new IOException("cannot read a single byte if frame size > 1");
    }

    /** Reads some number of bytes from the audio input stream and stores them
     * into the buffer array b. The number of bytes actually read is returned as
     * an integer. This method blocks until input data is available, the end of
     * the stream is detected, or an exception is thrown.
     * This method will always read an integral number of frames. If the length
     * of the array is not an integral number of frames, a maximum of
     * {@code b.length - (b.length % frameSize)} bytes will be read.
     *
     * @return Returns the total number of bytes read into the buffer, or -1 if there is
     * no more data because the end of the stream has been reached.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (in.getFrame() == null && in.getNextFrame() == null) {
            return -1;
        }
        if (in.getStreamPosition() != in.getFrame().getFrameOffset()) {
            if (in.getNextFrame() == null) {
                return -1;
            }
        }

        int bytesRead = 0;
        int frameSize = in.getFrame().getFrameSize();
        while (len >= frameSize) {
            in.readFully(b, off, frameSize);
            len -= frameSize;
            bytesRead += frameSize;
            off += frameSize;
            if (in.getNextFrame() == null) {
                break;
            }
            frameSize = in.getFrame().getFrameSize();
        }

        return bytesRead;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public void mark(int readlimit) {
        // can't do anything
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
}
