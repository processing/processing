/*
 * @(#)PCMAudioCodec.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.audio;

import org.monte.media.AbstractAudioCodec;
import org.monte.media.Buffer;
import org.monte.media.Format;
import org.monte.media.io.ByteArrayImageInputStream;
import org.monte.media.io.ByteArrayImageOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import static org.monte.media.AudioFormatKeys.*;
import static org.monte.media.BufferFlag.*;

/**
 * {@code PCMAudioCodec} performs sign conversion, endian conversion and
 * quantization conversion of PCM audio data. <p> Does not perform sampling rate
 * conversion or channel conversion. <p> FIXME Maybe create separate subclasses
 * for AVI PCM and QuickTime PCM.
 *
 * @author Werner Randelshofer
 * @version $Id: PCMAudioCodec.java 299 2013-01-03 07:40:18Z werner $
 */
public class PCMAudioCodec extends AbstractAudioCodec {

    private final static int UNSIGNED_BASE = 128;

    public PCMAudioCodec() {
        super(new Format[]{
                    new Format(MediaTypeKey, MediaType.AUDIO,//
                    EncodingKey, ENCODING_PCM_SIGNED,//
                    MimeTypeKey, MIME_JAVA,//
                    SignedKey, true),
                    new Format(MediaTypeKey, MediaType.AUDIO,//
                    EncodingKey, ENCODING_PCM_UNSIGNED,//
                    MimeTypeKey, MIME_JAVA,//
                    SignedKey, false),//
                });
        name = "PCM Codec";
    }

    protected PCMAudioCodec(Format[] supportedInputFormats, Format[] supportedOutputFormats) {
        super(supportedInputFormats, supportedOutputFormats);
    }

    protected PCMAudioCodec(Format[] supportedInputAndOutputFormats) {
        super(supportedInputAndOutputFormats, supportedInputAndOutputFormats);
    }

    @Override
    public int process(Buffer in, Buffer out) {
        out.setMetaTo(in);
        if (in.isFlag(DISCARD)) {
            return CODEC_OK;
        }

        Format inFormat = (Format) in.format;
        Format outFormat = (Format) outputFormat;
        if (inFormat.get(SampleRateKey) == null || !inFormat.get(SampleRateKey).equals(outFormat.get(SampleRateKey))) {
            out.setFlag(DISCARD);
            return CODEC_FAILED;
            //throw new UnsupportedOperationException("Sample Rate conversion not supported. in:" + inFormat + ", out:" + outFormat);
        }
        if (inFormat.get(ChannelsKey) != outFormat.get(ChannelsKey)) {
            out.setFlag(DISCARD);
            return CODEC_FAILED;
            //throw new UnsupportedOperationException("Channel conversion not supported. in:" + inFormat + ", out:" + outFormat);
        }
        String inEnc = inFormat.get(EncodingKey);
        String outEnc = outFormat.get(EncodingKey);
        
        boolean fixSilenceBug=inFormat.get(SilenceBugKey,false);
        
        /*
         if (!supportedEncodings.contains(inEnc)
         || !supportedEncodings.contains(outEnc)) {
         throw new UnsupportedOperationException("Unsupported encoding. in:" + inFormat + ", out:" + outFormat);
         }*/


        byte[] inData = (byte[]) in.data;
        byte[] outData = (out.data instanceof byte[]) ? (byte[]) out.data : new byte[inData.length];
        if (outData.length < inData.length * outFormat.get(FrameSizeKey) / inFormat.get(FrameSizeKey)) {
            outData = new byte[inData.length * outFormat.get(FrameSizeKey) / inFormat.get(FrameSizeKey)];
        }

        // Fast array copy if formats are identical 
        // or if 8 bit data with endian differences
        if (toAudioFormat(inFormat).matches(toAudioFormat(outFormat))) {
            System.arraycopy(inData, in.offset, outData, 0, in.length);
        } else {

            // Byte order conversion is done by ImageInputStream/ImageOutputStream.
            ByteOrder inOrder = inFormat.get(ByteOrderKey);
            boolean inSigned = inFormat.get(SignedKey);
            ByteArrayImageInputStream inStream = new ByteArrayImageInputStream(inData, in.offset, in.length, inOrder);
            ByteOrder outOrder = outFormat.get(ByteOrderKey);
            boolean outSigned = outFormat.get(SignedKey);
            ByteArrayImageOutputStream outStream = new ByteArrayImageOutputStream(outData, outOrder);
            try {
                // Now, we only have to care about sign conversion and 
                // quantization conversion
                int inSS = inFormat.get(SampleSizeInBitsKey);
                int outSS = outFormat.get(SampleSizeInBitsKey);

                switch ((inSS << 16) | outSS) {
                    case (16 << 16) | 16:
                        if (inSigned == outSigned) {
                            write16To16(inStream, outStream);
                        } else if (inSigned) {
                            write16STo16U(inStream, outStream);
                        } else {
                            write16UTo16S(inStream, outStream);
                        }
                        break;
                    case (16 << 16) | 8:
                        if (inSigned == outSigned) {
                            throw new UnsupportedOperationException("Unsupported sample size. in:" + inFormat + ", out:" + outFormat);
                        } else if (inSigned) {
                            write16STo8U(inStream, outStream);
                        } else {
                            throw new UnsupportedOperationException("Unsupported sample size. in:" + inFormat + ", out:" + outFormat);
                        }
                        break;
                    case (8 << 16) | 8:
                        if (inSigned == outSigned) {
                            write8STo8S(inStream, outStream, fixSilenceBug);
                        } else if (inSigned) {
                            write8STo8U(inStream, outStream, fixSilenceBug);
                        } else if (outSigned) {
                            write8UTo8S(inStream, outStream, fixSilenceBug);
                        }
                        break;
                    default:
                        // FIXME - The PCM Audio Codec should handle unsupported
                        //         sample size when the output format is set.
                        throw new UnsupportedOperationException("Unsupported sample size. in:" + inFormat + ", out:" + outFormat);
                    //out.setFlag(Buffer.FLAG_DISCARD);
                    //return BUFFER_PROCESSED_FAILED;

                }
            } catch (IOException ex) {
                out.flags.add(DISCARD);
                return CODEC_FAILED;
                //throw new InternalError(ex.getMessage());
            }

        }

        // Configure the buffer
        out.flags.add(KEYFRAME);
        out.format = outFormat;
        out.data = outData;
        out.offset = 0;
        out.length = in.length;
        return CODEC_OK;
    }

    /**
     * 16-bit audio data in and out. No sign conversion. Endian conversion is
     * performed by the stream objects.
     */
    protected void write16To16(ImageInputStream in, ImageOutputStream out) throws IOException {
        try {
            while (true) {
                out.writeShort(in.readShort());
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * 16-bit audio data unsigned in and signed out. Endian conversion is
     * performed by the stream objects.
     */
    protected void write16UTo16S(ImageInputStream in, ImageOutputStream out) throws IOException {
        try {
            while (true) {
                out.writeShort((in.readShort() & 0xffff) - (1 << 15));
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * 16-bit audio data signed in and unsigned out. Endian conversion is
     * performed by the stream objects.
     */
    protected void write16STo16U(ImageInputStream in, ImageOutputStream out) throws IOException {
        try {
            while (true) {
                out.writeShort(in.readShort() + (1 << 15));
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * Signed 16-bit audio data in, unsigned 8-bit out. Endian conversion is
     * performed by the stream objects.
     */
    protected void write16STo8U(ImageInputStream in, ImageOutputStream out) throws IOException {
        try {
            while (true) {
                out.writeByte((in.readShort() >> 8) + UNSIGNED_BASE);
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * 8-bit audio data in and out. No sign conversion.
     */
    protected void write8To8(ImageInputStream in, ImageOutputStream out) throws IOException {
        try {
            byte[] buf = new byte[512];
            while (true) {
                int count = in.read(buf, 0, buf.length);
                if (count == -1) {
                    break;
                }
                out.write(buf, 0, count);
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * Unsigned 8-bit audio data in, signed out.
     */
    protected void write8UTo8S(ImageInputStream in, ImageOutputStream out, boolean fixSilenceBug) throws IOException {
        try {
            byte[] buf = new byte[512];
            while (true) {
                int count = in.read(buf, 0, buf.length);
                if (count == -1) {
                    break;
                }
                if (fixSilenceBug) {
                    for (int i = 0; i < count; i++) {
                        if (buf[i] == 0) {
                            buf[i] = (byte) UNSIGNED_BASE;
                        }
                        buf[i] = (byte) ((buf[i] & 0xff) - UNSIGNED_BASE);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        buf[i] = (byte) ((buf[i] & 0xff) - UNSIGNED_BASE);
                    }
                }
                out.write(buf, 0, count);
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * Signed 8-bit audio data in, unsigned out.
     */
    protected void write8STo8U(ImageInputStream in, ImageOutputStream out, boolean fixSilenceBug) throws IOException {
        try {
            byte[] buf = new byte[512];
            while (true) {
                int count = in.read(buf, 0, buf.length);
                if (count == -1) {
                    break;
                }
                if (fixSilenceBug) {
                    for (int i = 0; i < count; i++) {
                        // FIXME - For some reason, the Java sound system records
                        //         silence as -128 instead of 0.
                        buf[i] = (byte) (buf[i] == -128 ? -UNSIGNED_BASE :buf[i] + UNSIGNED_BASE);
                    }
                } else {
                    for (int i = 0; i < count; i++) {
                        buf[i] = (byte) (buf[i] + UNSIGNED_BASE);
                    }
                }

                out.write(buf, 0, count);
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }

    /**
     * Signed 8-bit audio data in, signed out.
     */
    protected void write8STo8S(ImageInputStream in, ImageOutputStream out, boolean fixSilenceBug) throws IOException {
        try {
            byte[] buf = new byte[512];
            while (true) {
                int count = in.read(buf, 0, buf.length);
                if (count == -1) {
                    break;
                }
                if (fixSilenceBug) {
                    for (int i = 0; i < count; i++) {
                        // FIXME - For some reason, the Java sound system records
                        //         silence as -128 instead of 0.
                        buf[i] = (byte) (buf[i] == -128 ? 0 : buf[i]);
                    }
                }
                out.write(buf, 0, count);
            }
        } catch (EOFException e) {
            // end of data reached
        }
    }
}
