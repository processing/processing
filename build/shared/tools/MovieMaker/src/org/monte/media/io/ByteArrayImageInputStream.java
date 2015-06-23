/*
 * @(#)ByteArrayImageInputStream.java  
 *
 * Copyright (c) 2008-2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.io;

import java.io.IOException;
import java.nio.ByteOrder;

/**
 * A {@code ByteArrayImageInputStream} contains
 * an internal buffer that contains bytes that
 * may be read from the stream. An internal
 * counter keeps track of the next byte to
 * be supplied by the {@code read} method.
 * <p>
 * Closing a {@code ByteArrayImageInputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 *
 * @author Werner Randelshofer, Hausmatt 10, CH-6405 Goldau
 * @version $Id: ByteArrayImageInputStream.java 299 2013-01-03 07:40:18Z werner $
 */
public class ByteArrayImageInputStream extends ImageInputStreamImpl2 {
    /**
     * An array of bytes that was provided
     * by the creator of the stream. Elements <code>buf[0]</code>
     * through <code>buf[count-1]</code> are the
     * only bytes that can ever be read from the
     * stream;  element <code>buf[streamPos]</code> is
     * the next byte to be read.
     */
    protected byte buf[];

    /**
     * The index one greater than the last valid character in the input
     * stream buffer.
     * This value should always be nonnegative
     * and not larger than the length of <code>buf</code>.
     * It  is one greater than the position of
     * the last byte within <code>buf</code> that
     * can ever be read  from the input stream buffer.
     */
    protected int count;

    /** The offset to the start of the array. */
    private final int arrayOffset;

    public ByteArrayImageInputStream(byte[] buf) {
        this(buf, ByteOrder.BIG_ENDIAN);
    }

    public ByteArrayImageInputStream(byte[] buf, ByteOrder byteOrder) {
        this(buf, 0, buf.length, byteOrder);
    }

    public ByteArrayImageInputStream(byte[] buf, int offset, int length, ByteOrder byteOrder) {
	this.buf = buf;
        this.streamPos = offset;
	this.count = Math.min(offset + length, buf.length);
        this.arrayOffset = offset;
        this.byteOrder = byteOrder;
    }

    /**
     * Reads the next byte of data from this input stream. The value
     * byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>. If no byte is available
     * because the end of the stream has been reached, the value
     * <code>-1</code> is returned.
     * <p>
     * This <code>read</code> method
     * cannot block.
     *
     * @return  the next byte of data, or <code>-1</code> if the end of the
     *          stream has been reached.
     */
    @Override
    public synchronized int read() {
        flushBits();
	return (streamPos < count) ? (buf[(int)(streamPos++)] & 0xff) : -1;
    }

    /**
     * Reads up to <code>len</code> bytes of data into an array of bytes
     * from this input stream.
     * If <code>streamPos</code> equals <code>count</code>,
     * then <code>-1</code> is returned to indicate
     * end of file. Otherwise, the  number <code>k</code>
     * of bytes read is equal to the smaller of
     * <code>len</code> and <code>count-streamPos</code>.
     * If <code>k</code> is positive, then bytes
     * <code>buf[streamPos]</code> through <code>buf[streamPos+k-1]</code>
     * are copied into <code>b[off]</code>  through
     * <code>b[off+k-1]</code> in the manner performed
     * by <code>System.arraycopy</code>. The
     * value <code>k</code> is added into <code>streamPos</code>
     * and <code>k</code> is returned.
     * <p>
     * This <code>read</code> method cannot block.
     *
     * @param   b     the buffer into which the data is read.
     * @param   off   the start offset in the destination array <code>b</code>
     * @param   len   the maximum number of bytes read.
     * @return  the total number of bytes read into the buffer, or
     *          <code>-1</code> if there is no more data because the end of
     *          the stream has been reached.
     * @exception  NullPointerException If <code>b</code> is <code>null</code>.
     * @exception  IndexOutOfBoundsException If <code>off</code> is negative,
     * <code>len</code> is negative, or <code>len</code> is greater than
     * <code>b.length - off</code>
     */
    @Override
    public synchronized int read(byte b[], int off, int len) {
        flushBits();
	if (b == null) {
	    throw new NullPointerException();
	} else if (off < 0 || len < 0 || len > b.length - off) {
	    throw new IndexOutOfBoundsException();
	}
	if (streamPos >= count) {
	    return -1;
	}
	if (streamPos + len > count) {
	    len = (int)(count - streamPos);
	}
	if (len <= 0) {
	    return 0;
	}
	System.arraycopy(buf, (int)streamPos, b, off, len);
	streamPos += len;
	return len;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream. Fewer
     * bytes might be skipped if the end of the input stream is reached.
     * The actual number <code>k</code>
     * of bytes to be skipped is equal to the smaller
     * of <code>n</code> and  <code>count-streamPos</code>.
     * The value <code>k</code> is added into <code>streamPos</code>
     * and <code>k</code> is returned.
     *
     * @param   n   the number of bytes to be skipped.
     * @return  the actual number of bytes skipped.
     */
    public synchronized long skip(long n) {
	if (streamPos + n > count) {
	    n = count - streamPos;
	}
	if (n < 0) {
	    return 0;
	}
	streamPos += n;
	return n;
    }

    /**
     * Returns the number of remaining bytes that can be read (or skipped over)
     * from this input stream.
     * <p>
     * The value returned is <code>count&nbsp;- streamPos</code>,
     * which is the number of bytes remaining to be read from the input buffer.
     *
     * @return  the number of remaining bytes that can be read (or skipped
     *          over) from this input stream without blocking.
     */
    public synchronized int available() {
	return (int)(count - streamPos);
    }



    /**
     * Closing a <tt>ByteArrayInputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     * <p>
     */
    @Override
    public void close() {
        // does nothing!!
    }

    @Override
    public long getStreamPosition() throws IOException {
        checkClosed();
        return streamPos-arrayOffset;
    }
    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        flushBits();

        // This test also covers pos < 0
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }

        this.streamPos = pos+arrayOffset;
    }

    private void flushBits() {
        bitOffset=0;
    }
    @Override
    public long length() {
        return count-arrayOffset;
    }
}
