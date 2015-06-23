/*
 * @(#)DataAtomInputStream.java  
 * 
 * Copyright (c) 2012 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * {@code DataAtomInputStream}.
 *
 * @author Werner Randelshofer
 * @version $Id
 */
public class DataAtomInputStream extends FilterInputStream {

    protected static final long MAC_TIMESTAMP_EPOCH = new GregorianCalendar(1904, GregorianCalendar.JANUARY, 1).getTimeInMillis();
    private byte readBuffer[] = new byte[8];

    public DataAtomInputStream(InputStream in) {
        super(in);
    }

    public final byte readByte() throws IOException {
        int ch = in.read();
        if (ch < 0) {
            throw new EOFException();
        }
        return (byte) (ch);
    }

    public final short readShort() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0) {
            throw new EOFException();
        }
        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    public final int readInt() throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    public final long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[0] << 56)
                + ((long) (readBuffer[1] & 255) << 48)
                + ((long) (readBuffer[2] & 255) << 40)
                + ((long) (readBuffer[3] & 255) << 32)
                + ((long) (readBuffer[4] & 255) << 24)
                + ((readBuffer[5] & 255) << 16)
                + ((readBuffer[6] & 255) << 8)
                + ((readBuffer[7] & 255) << 0));
    }

    public final int readUByte() throws IOException {
        return readByte() & 0xFF;
    }
    public final int readUShort() throws IOException {
        return readShort() & 0xFFFF;
    }

    public final long readUInt() throws IOException {
        return readInt() & 0xFFFFFFFFL;
    }

    public final long skipBytes(long n) throws IOException {
        long total = 0;
        long cur = 0;

        while ((total < n) && ((cur = (int) in.skip(n - total)) > 0)) {
            total += cur;
        }

        return total;
    }

    public final void readFully(byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

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
        }
    }

    /**
     * Reads a 32-bit Mac timestamp (seconds since 1902).
     * @return date
     * @throws java.io.IOException
     */
    public Date readMacTimestamp() throws IOException {
        long timestamp = ((long) readInt()) & 0xffffffffL;
        return new Date(MAC_TIMESTAMP_EPOCH + timestamp * 1000);
    }

    /**
     * Reads 32-bit fixed-point number divided as 16.16.
     */
    public double readFixed16D16() throws IOException {
        int wholePart = readUShort();
        int fractionPart = readUShort();

        return new Double(wholePart + fractionPart / 65536.0);
    }

    /**
     * Reads 32-bit fixed-point number divided as 2.30.
     */
    public double readFixed2D30() throws IOException {
        int fixed = readInt();
        int wholePart = fixed >>> 30;
        int fractionPart = fixed & 0x3fffffff;

        return new Double(wholePart + fractionPart / (double) 0x3fffffff);
    }

    /**
     * Reads 16-bit fixed-point number divided as 8.8.
     */
    public double readFixed8D8() throws IOException {
        int fixed = readUShort();
        int wholePart = fixed >>> 8;
        int fractionPart = fixed & 0xff;

        return new Double(wholePart + fractionPart / 256f);
    }

    public String readType() throws IOException {
        int id = readInt();
        byte[] b = new byte[4];
        b[0] = (byte) ((id >>> 24) & 0xff);
        b[1] = (byte) ((id >>> 16) & 0xff);
        b[2] = (byte) ((id >>> 8) & 0xff);
        b[3] = (byte) (id & 0xff);
        try {
            return new String(b, "ASCII");
        } catch (UnsupportedEncodingException ex) {
            InternalError ie = new InternalError("ASCII not supported");
            ie.initCause(ex);
            throw ie;
        }
    }

    public String readPString() throws IOException {
        int size = in.read();
        if (size == 0) {
            size = in.read();
            in.read(); // why do we skip two bytes here?
            in.read();
        }
        if (size < 0) {
            return "";
        }
        byte[] b = new byte[size];
        //in.readFully(bytes);
        int n = 0;
        while (n < size) {
            int count = in.read(b, n, size - n);
            if (count < 0) {
                System.out.println("StructParser.PrimitiveSpecifier.read not enough bytes for pstring. Expected size:" + size + " actual size:" + n);
                break;
                //throw new EOFException();
            }
            n += count;
        }

        try {
            return new String(b, "ASCII");
        } catch (UnsupportedEncodingException ex) {
            InternalError ie = new InternalError("ASCII not supported");
            ie.initCause(ex);
            throw ie;
        }
    }
}
