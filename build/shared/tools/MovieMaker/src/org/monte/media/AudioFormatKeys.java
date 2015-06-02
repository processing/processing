/*
 * @(#)AudioFormatKeys.java 
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance onlyWith the
 * license agreement you entered into onlyWith Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import org.monte.media.math.Rational;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * Defines common format keys for audio media.
 *
 * @author Werner Randelshofer
 * @version $Id: AudioFormatKeys.java 299 2013-01-03 07:40:18Z werner $
 */
public class AudioFormatKeys extends FormatKeys {
    // Standard video EncodingKey strings for use onlyWith FormatKey.Encoding. 

    /**
     * Specifies SignedKey, linear PCM data.
     */
    public static final String ENCODING_PCM_SIGNED = javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED.toString();
    /**
     * Specifies unsigned, linear PCM data.
     */
    public static final String ENCODING_PCM_UNSIGNED = javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED.toString();
    /**
     * Specifies u-law encoded data.
     */
    public static final String ENCODING_ULAW = javax.sound.sampled.AudioFormat.Encoding.ULAW.toString();
    /**
     * Specifies a-law encoded data.
     */
    public static final String ENCODING_ALAW = javax.sound.sampled.AudioFormat.Encoding.ALAW.toString();
    /**
     * AVI PCM encoding.
     */
    public static final String ENCODING_AVI_PCM = "\u0000\u0000\u0000\u0001";
    /**
     * QuickTime 16-bit big endian signed PCM encoding.
     */
    public static final String ENCODING_QUICKTIME_TWOS_PCM = "twos";
    /**
     * QuickTime 16-bit little endian signed PCM encoding.
     */
    public static final String ENCODING_QUICKTIME_SOWT_PCM = "sowt";
    /**
     * QuickTime 24-bit big endian signed PCM encoding.
     */
    public static final String ENCODING_QUICKTIME_IN24_PCM = "in24";
    /**
     * QuickTime 32-bit big endian signed PCM encoding.
     */
    public static final String ENCODING_QUICKTIME_IN32_PCM = "in32";
    /**
     * QuickTime 8-bit unsigned PCM encoding.
     */
    public static final String ENCODING_QUICKTIME_RAW_PCM = "raw ";
    /**
     * Specifies MP3 encoded data.
     */
    public static final String ENCODING_MP3 = "MP3";
    /**
     * The sample size in bits.
     */
    public final static FormatKey<Integer> SampleSizeInBitsKey = new FormatKey<Integer>("sampleSizeInBits", Integer.class);
    /**
     * The numer of ChannelsKey.
     */
    public final static FormatKey<Integer> ChannelsKey = new FormatKey<Integer>("channels", Integer.class);
    /**
     * The size of a frame.
     */
    public final static FormatKey<Integer> FrameSizeKey = new FormatKey<Integer>("frameSize", Integer.class);
    /**
     * The compressor name.
     */
    public final static FormatKey<ByteOrder> ByteOrderKey = new FormatKey<ByteOrder>("byteOrder", ByteOrder.class);
    /**
     * The number of frames per second.
     */
    public final static FormatKey<Rational> SampleRateKey = new FormatKey<Rational>("sampleRate", Rational.class);
    /**
     * Whether values are signed.
     */
    public final static FormatKey<Boolean> SignedKey = new FormatKey<Boolean>("signed", Boolean.class);
    /**
     * Whether silence is encoded as -128 instead of 0.
     */
    public final static FormatKey<Boolean> SilenceBugKey = new FormatKey<Boolean>("silenceBug", Boolean.class);

    public static Format fromAudioFormat(javax.sound.sampled.AudioFormat fmt) {
        return new Format(
                MediaTypeKey, MediaType.AUDIO,
                EncodingKey, fmt.getEncoding().toString(),
                SampleRateKey, Rational.valueOf(fmt.getSampleRate()),
                SampleSizeInBitsKey, fmt.getSampleSizeInBits(),
                ChannelsKey, fmt.getChannels(),
                FrameSizeKey, fmt.getFrameSize(),
                FrameRateKey, Rational.valueOf(fmt.getFrameRate()),
                ByteOrderKey, fmt.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN,
                SignedKey, AudioFormat.Encoding.PCM_SIGNED.equals(fmt.getEncoding())//,
                //
                );
    }

    public static javax.sound.sampled.AudioFormat toAudioFormat(Format fmt) {
        // We always use PCM_SIGNED or PCM_UNSIGNED
        return new javax.sound.sampled.AudioFormat(
                !fmt.containsKey(SignedKey) || fmt.get(SignedKey) ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED,
                fmt.get(SampleRateKey).floatValue(),
                fmt.get(SampleSizeInBitsKey, 16),
                fmt.get(ChannelsKey, 1),
                fmt.containsKey(FrameSizeKey) ? fmt.get(FrameSizeKey) : (fmt.get(SampleSizeInBitsKey, 16) + 7) / 8 * fmt.get(ChannelsKey, 1),
                fmt.containsKey(FrameRateKey) ? fmt.get(FrameRateKey).floatValue() : fmt.get(SampleRateKey).floatValue(),
                fmt.containsKey(ByteOrderKey) ? fmt.get(ByteOrderKey) == ByteOrder.BIG_ENDIAN : true);
    }
}
