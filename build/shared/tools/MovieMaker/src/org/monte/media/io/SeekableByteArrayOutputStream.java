/*
 * @(#)SeekableByteArrayOutputStream.java
 * 
 * Copyright © 2010-2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package org.monte.media.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import static java.lang.Math.*;
/**
 * {@code SeekableByteArrayOutputStream}.
 *
 * @author Werner Randelshofer
 * @version $Id: SeekableByteArrayOutputStream.java 299 2013-01-03 07:40:18Z werner $
 */
public class SeekableByteArrayOutputStream extends ByteArrayOutputStream {

    /**
     * The current stream position.
     */
    private int pos;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 32 bytes, though its size increases if necessary.
     */
    public SeekableByteArrayOutputStream() {
	this(32);
    }

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    public SeekableByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                                               + size);
        }
	buf = new byte[size];
    }
    /**
     * Creates a new byte array output stream, which reuses the supplied buffer.
     */
    public SeekableByteArrayOutputStream(byte[] buf) {
	this.buf = buf;
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param   b   the byte to be written.
     */
    @Override
    public synchronized void write(int b) {
	int newcount = max(pos + 1, count);
	if (newcount > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
	}
	buf[pos++] = (byte)b;
	count = newcount;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
    @Override
    public synchronized void write(byte b[], int off, int len) {
	if ((off < 0) || (off > b.length) || (len < 0) ||
            ((off + len) > b.length) || ((off + len) < 0)) {
	    throw new IndexOutOfBoundsException();
	} else if (len == 0) {
	    return;
	}
        int newcount = max(pos+len,count);
        if (newcount > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
        }
        System.arraycopy(b, off, buf, pos, len);
        pos+=len;
        count = newcount;
    }

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     java.io.ByteArrayInputStream#count
     */
    @Override
    public synchronized void reset() {
	count = 0;
        pos=0;
    }

    /**
     * Sets the current stream position to the desired location.  The
     * next read will occur at this location.  The bit offset is set
     * to 0.
     *
     * <p> An <code>IndexOutOfBoundsException</code> will be thrown if
     * <code>pos</code> is smaller than the flushed position (as
     * returned by <code>getflushedPosition</code>).
     *
     * <p> It is legal to seek past the end of the file; an
     * <code>EOFException</code> will be thrown only if a read is
     * performed.
     *
     * @param pos a <code>long</code> containing the desired file
     * pointer position.
     *
     * @exception IndexOutOfBoundsException if <code>pos</code> is smaller
     * than the flushed position.
     * @exception IOException if any other I/O error occurs.
     */
    public void seek(long pos) throws IOException {
        this.pos = (int)pos;
    }

        /**
     * Returns the current byte position of the stream.  The next write
     * will take place starting at this offset.
     *
     * @return a long containing the position of the stream.
     *
     * @exception IOException if an I/O error occurs.
     */
    public long getStreamPosition() throws IOException {
        return pos;
    }

    /** Writes the contents of the byte array into the specified output
     * stream.
     * @param out
     */
    public void toOutputStream(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

    /** Returns the underlying byte buffer. */
    public byte[] getBuffer() {
        return buf;
    }
}
