/*
 * @(#)ByteArrayImageOutputStream.java  1.0.1  2011-01-23
 * 
 * Copyright (c) 2011 Werner Randelshofer
 * Staldenmattweg 2, Goldau, CH-6405, Switzerland.
 * All rights reserved.
 * 
 * The copyright of this software is owned by Werner Randelshofer. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Werner Randelshofer. For details see accompanying license terms.
 */
package org.monte.media.io;

import java.io.OutputStream;
import javax.imageio.stream.ImageOutputStreamImpl;
import java.io.ByteArrayOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.nio.ByteOrder;
import static java.lang.Math.*;

/**
 * This class implements an image output stream in which the data is
 * written into a byte array. The buffer automatically grows as data
 * is written to it.
 * The data can be retrieved using {@code toByteArray()}, {@code toImageOutputStream()}
 * and {@code toOutputStream()}.
 * <p>
 * Closing a {@code ByteArrayImageOutputStream} has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an {@code IOException}.
 *
 * @author Werner Randelshofer
 * @version 1.0.1 2011-01-23 Implements length method.
 * <br>1.0 2011-01-18 Created.
 */
public class ByteArrayImageOutputStream extends ImageOutputStreamImpl {


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

    public ByteArrayImageOutputStream() {
        this(16);
    }

    public ByteArrayImageOutputStream(int initialCapacity) {
        this(new byte[initialCapacity]);
    }

    public ByteArrayImageOutputStream(byte[] buf) {
        this(buf, ByteOrder.BIG_ENDIAN);
    }

    public ByteArrayImageOutputStream(byte[] buf, ByteOrder byteOrder) {
        this(buf, 0, buf.length, byteOrder);
    }

    public ByteArrayImageOutputStream(byte[] buf, int offset, int length, ByteOrder byteOrder) {
        this.buf = buf;
        this.streamPos = offset;
        this.count = Math.min(offset + length, buf.length);
        this.arrayOffset = offset;
        this.byteOrder = byteOrder;
    }

    public ByteArrayImageOutputStream(ByteOrder byteOrder) {
        this(new byte[16],byteOrder);
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
    public synchronized int read() throws IOException {
        flushBits();
        return (streamPos < count) ? (buf[(int) (streamPos++)] & 0xff) : -1;
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
    public synchronized int read(byte b[], int off, int len) throws IOException {
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
            len = (int) (count - streamPos);
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, (int) streamPos, b, off, len);
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
        return (int) (count - streamPos);
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
        return streamPos - arrayOffset;
    }

    @Override
    public void seek(long pos) throws IOException {
        checkClosed();
        flushBits();

        // This test also covers pos < 0
        if (pos < flushedPos) {
            throw new IndexOutOfBoundsException("pos < flushedPos!");
        }

        this.streamPos = pos + arrayOffset;
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param   b   the byte to be written.
     */
    @Override
    public synchronized void write(int b) throws IOException {
        flushBits();
        long newcount = max(streamPos + 1, count);
        if (newcount> Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException(newcount+" > max array size");
        }
        if (newcount > buf.length) {
            buf = Arrays.copyOf(buf, max(buf.length << 1, (int) newcount));
        }
        buf[(int) streamPos++] = (byte) b;
        count = (int)newcount;
    }

    /**
     * Writes the specified byte array to this output stream.
     *
     * @param   b     the data.
     */
    @Override
    public synchronized void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        flushBits();
        if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("off="+off+", len="+len+", b.length="+b.length);
        } else if (len == 0) {
            return;
        }
        int newcount = max((int) streamPos + len, count);
        if (newcount > buf.length) {
            buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
        }
        System.arraycopy(b, off, buf, (int) streamPos, len);
        streamPos += len;
        count = newcount;
    }

    /** Writes the contents of the byte array into the specified output
     * stream.
     * @param out
     */
    public void toOutputStream(OutputStream out) throws IOException {
        out.write(buf, arrayOffset, count);
    }

    /** Writes the contents of the byte array into the specified image output
     * stream.
     * @param out
     */
    public void toImageOutputStream(ImageOutputStream out) throws IOException {
        out.write(buf, arrayOffset, count);
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    public synchronized byte[] toByteArray() {
        byte[] copy = new byte[count - arrayOffset];
        System.arraycopy(buf, arrayOffset, copy, 0, count);
        return copy;
    }

    /** Returns the internally used byte buffer. */
    public byte[] getBuffer() {
        return buf;
    }

        @Override
    public long length() {
        return count-arrayOffset;
    }

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     java.io.ByteArrayInputStream#count
     */
    public synchronized void clear() {
	count = arrayOffset;
        streamPos=arrayOffset;
    }
}
