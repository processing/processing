/**
 * @(#)QuickTimeWriter.java  1.3.5  2011-03-12
 *
 * Copyright (c) 2010-2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import org.monte.media.Registry;
import org.monte.media.Format;
import org.monte.media.Codec;
import org.monte.media.Buffer;
import org.monte.media.MovieWriter;
import org.monte.media.math.Rational;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteOrder;
import javax.imageio.stream.*;
import static org.monte.media.VideoFormatKeys.*;
import static org.monte.media.AudioFormatKeys.*;
import static org.monte.media.BufferFlag.*;

/**
 * Supports writing of time-based video and audio data into a QuickTime movie
 * file (.MOV) without the need of native code.
 * <p>
 * {@code QuickTimeWriter} works with tracks and samples. After creating a
 * {@code QuickTimeWriter} one or more video and audio tracks can be added to
 * it. Then samples can be written into the track(s). A sample is a single
 * element in a sequence of time-ordered data. For video data a sample typically
 * consists of a single video frame, for uncompressed stereo audio data a sample
 * contains one PCM impulse per channel. Samples of compressed media data may encompass larger time units.
 * <p>
 * Tracks support edit lists. An edit list specifies when to play which portion
 * of the media data at what speed. An empty edit can be used to insert an empty
 * time span, for example to offset a track from the start of the movie. Edits
 * can also be used to play the same portion of media data multiple times
 * without having it to store it more than once in the track.<br>
 * Moreover edit lists are useful for lossless cutting of media data at non-sync
 * frames. For example, MP3 layer III audio data can not be cut at arbitrary
 * frames, because audio data can be 'borrowed' from previous frames. An edit
 * list can be used to select the desired portion of the audio data, while the
 * track stores the media starting from the nearest sync frame.
 * <p>
 * Samples are stored in a QuickTime file in the same sequence as they are written.
 * In order to getCodec optimal movie playback, the samples from different tracks
 * should be interleaved from time to time. An interleave should occur about twice
 * per second. Furthermore, to overcome any latencies in sound playback, at
 * least one second of sound data needs to be placed at the beginning of the
 * movie. So that the sound and video data is offset from each other in the file
 * by one second.
 * <p>
 * For convenience, this class has built-in encoders for video frames in the following
 * formats: RAW, ANIMATION, JPEG and PNG. Media data in other formats, including all audio
 * data, must be encoded before it can be written with {@code QuickTimeWriter}.
 * Alternatively, you can plug in your own codec.
 * <p>
 * <b>Example:</b> Writing 10 seconds of a movie with 640x480 pixel, 30 fps,
 * PNG-encoded video and 16-bit stereo, 44100 Hz, PCM-encoded audio.
 * <p>
 * <pre>
 * QuickTimeWriter w = new QuickTimeWriter(new File("mymovie.mov"));
 * w.addAudioTrack(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED), 44100, 2, 16, 2, 44100, true)); // audio in track 0
 * w.addVideoTrack(QuickTimeWriter.VIDEO_PNG, 30, 640, 480);  // video in track 1
 *
 * // calculate total movie sampleDuration in media time units for each track
 * long atmax = w.getMediaTimeScale(0) * 10;
 * long vtmax = w.getMediaTimeScale(1) * 10;
 *
 * // sampleDuration of a single sample
 * long asduration = 1;
 * long vsduration = 1;
 *
 * // half a second in media time units (we interleave twice per second)
 * long atstep = w.getMediaTimeScale(0) / 2;
 * long vtstep = w.getMediaTimeScale(1) / 2;
 *
 * // the time when the next interleave occurs in media time units
 * long atnext = w.getMediaTimeScale(0); // offset audio by 1 second
 * long vtnext = 0;
 *
 * // the current time in media time units
 * long atime = 0;
 * long vtime = 0;
 *
 * // create buffers
 * int asamplesize = 2 * 2; // 16-bit stereo * 2 channels
 * byte[] audio=new byte[atstep * asamplesize];
 * BufferedImage img=new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
 *
 * // main loop
 * while (atime &lt; atmax || vtime &lt; vtmax) {
 *      atnext = Math.min(atmax, atnext + atstep); // advance audio to next interleave time
 *      while (atime &lt; atnext) { // catch up with audio time
 *          int sampleDuration = (int) Math.min(audio.length / asamplesize, atmax - atime);
 *          ...fill in audio data for time "atime" and sampleDuration "sampleDuration" here...
 *          w.writeSamples(0, sampleDuration, audio, 0, sampleDuration * asamplesize, asduration);
 *          atime += sampleDuration;
 *      }
 *      vtnext = Math.min(vtmax, vtnext + vtstep); // advance video to next interleave time
 *      while (vtime &lt; vtnext) { // catch up with video time
 *          int sampleDuration = (int) Math.min(1, vtmax - vtime);
 *          ...fill in image data for time "vtime" and sampleDuration "sampleDuration" here...
 *          w.write(1, img, vsduration);
 *          vtime += sampleDuration;
 *      }
 * }
 * w.close();
 * </pre>
 * <p>
 * For information about the QuickTime file format see the
 * "QuickTime File Format Specification", Apple Inc. 2010-08-03. (qtff)
 * <a href="http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf/">
 * http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf
 * </a>
 *
 * @author Werner Randelshofer
 * @version 1.3.4 2011-03-12 Streamlines the code with {@code AVIWriter}.
 * <br>1.3.3 2011-01-17 Improves writing of compressed movie headers.
 * <br>1.3.2 2011-01-17 Fixes out of bounds exception when writing
 * sub-images with ANIMATION codec. Fixes writing of compressed movie headers.
 * <br>1.3.1 2011-01-09 Fixes broken RAW codec.
 * <br>1.3 2011-01-07 Improves robustness of API.
 *                    Adds method toWebOptimizedMovie().
 * <br>1.2.2 2011-01-07 Reduces file seeking with "ANIMATION" codec.
 * <br>1.2.1 2011-01-07 Fixed default syncInterval for "ANIMATION" video.
 * <br>1.2 2011-01-05 Adds support for "ANIMATION" encoded video.
 * <br>1.1 2011-01-04 Adds "depth" parameter to addVideoTrack method.
 * <br>1.0 2011-01-02 Adds support for edit lists. Adds support for MP3
 * audio format.
 * <br>0.1.1 2010-12-05 Updates the link to the QuickTime file format
 * specification.
 * <br>0.1 2010-09-30 Created.
 */
public class QuickTimeWriter extends QuickTimeOutputStream implements MovieWriter {

    public final static Format QUICKTIME = new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_QUICKTIME);
    public final static Format VIDEO_RAW = new Format(
            MediaTypeKey, MediaType.VIDEO,//
            MimeTypeKey, MIME_QUICKTIME,
            EncodingKey, ENCODING_QUICKTIME_RAW,//
            CompressorNameKey, COMPRESSOR_NAME_QUICKTIME_RAW);
    public final static Format VIDEO_ANIMATION = new Format(
            MediaTypeKey, MediaType.VIDEO, //
            MimeTypeKey, MIME_QUICKTIME,
            EncodingKey, ENCODING_QUICKTIME_ANIMATION, //
            CompressorNameKey, COMPRESSOR_NAME_QUICKTIME_ANIMATION);
    public final static Format VIDEO_JPEG = new Format(
            MediaTypeKey, MediaType.VIDEO,//
            MimeTypeKey, MIME_QUICKTIME,
            EncodingKey, ENCODING_QUICKTIME_JPEG, //
            CompressorNameKey, COMPRESSOR_NAME_QUICKTIME_JPEG);
    public final static Format VIDEO_PNG = new Format(
            MediaTypeKey, MediaType.VIDEO,//
            MimeTypeKey, MIME_QUICKTIME,
            EncodingKey, ENCODING_QUICKTIME_PNG, //
            CompressorNameKey, COMPRESSOR_NAME_QUICKTIME_PNG);

    /**
     * Creates a new QuickTime writer.
     *
     * @param file the output file
     */
    public QuickTimeWriter(File file) throws IOException {
        super(file);
    }

    /**
     * Creates a new QuickTime writer.
     *
     * @param out the output stream.
     */
    public QuickTimeWriter(ImageOutputStream out) throws IOException {
        super(out);
    }

    @Override
    public Format getFileFormat() throws IOException {
        return QUICKTIME;
    }

    @Override
    public Format getFormat(int track) {
        return tracks.get(track).format;
    }

    /** Adds a track.
     *
     * @param fmt The format of the track.
     * @return The track number.
     */
    @Override
    public int addTrack(Format fmt) throws IOException {
        if (fmt.get(MediaTypeKey) == MediaType.VIDEO) {
            int t= addVideoTrack(fmt.get(EncodingKey),
                    fmt.get(CompressorNameKey,fmt.get(EncodingKey)),
                    Math.min(6000,fmt.get(FrameRateKey).getNumerator() * fmt.get(FrameRateKey).getDenominator()),
                    fmt.get(WidthKey), fmt.get(HeightKey), fmt.get(DepthKey),
                    (int) fmt.get(FrameRateKey).getDenominator());
            setCompressionQuality(t,fmt.get(QualityKey,1.0f));
            return t;
        } else if (fmt.get(MediaTypeKey) == MediaType.AUDIO) {
            // fill in unspecified values
            int sampleSizeInBits = fmt.get(SampleSizeInBitsKey, 16);
            ByteOrder bo = fmt.get(ByteOrderKey, ByteOrder.BIG_ENDIAN);
            boolean signed = fmt.get(SignedKey, true);
            String encoding = fmt.get(EncodingKey, null);
            Rational frameRate = fmt.get(FrameRateKey, fmt.get(SampleRateKey));
            int channels = fmt.get(ChannelsKey, 1);
            int frameSize = fmt.get(FrameSizeKey, (sampleSizeInBits + 7) / 8 * sampleSizeInBits);
            if (encoding == null||encoding.length()!=4) {
                if (signed) {
                    encoding = bo == ByteOrder.BIG_ENDIAN ? "twos" : "sowt";
                } else {
                    encoding = "raw ";
                }
            } 

            return addAudioTrack(encoding,
                    fmt.get(SampleRateKey).longValue(),
                    fmt.get(SampleRateKey).doubleValue(),
                    channels,
                    sampleSizeInBits,
                    false, // FIXME - We should support compressed formats
                    fmt.get(SampleRateKey).divide(frameRate).intValue(),
                    frameSize,
                    signed,
                    bo);
            //return addAudioTrack(AudioFormatKeys.toAudioFormat(fmt)); // FIXME Add direct support for AudioFormat
        } else {
            throw new IOException("Unsupported media type:" + fmt.get(MediaTypeKey));
        }
    }

    /** Adds a video track.
     *
     * @param format The QuickTime video format.
     * @param timeScale The media time scale. This is typically the frame rate.
     * If the frame rate is not an integer fraction of a second, specify a
     * multiple of the frame rate and specify a correspondingly multiplied
     * sampleDuration when writing frames. For example, for a rate of 23.976 fps
     * specify a time scale of 23976 and multiply the sampleDuration of a video frame
     * by 1000.
     * @param width The width of a video image. Must be larger than 0.
     * @param height The height of a video image. Must be larger than 0.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    @Deprecated
    public int addVideoTrack(Format format, long timeScale, int width, int height) throws IOException {
        return addVideoTrack(format.get(EncodingKey), format.get(CompressorNameKey), timeScale, width, height, 24, 30);
    }

    /** Adds a video track.
     *
     * @param format The QuickTime video format.
     * @param width The width of a video image. Must be larger than 0.
     * @param height The height of a video image. Must be larger than 0.
     *
     * @return Returns the track index.
     *
     * @throws IllegalArgumentException if the width or the height is smaller
     * than 1.
     */
    @Deprecated
    public int addVideoTrack(Format format, int width, int height, int depth, int syncInterval) throws IOException {
        return addVideoTrack(format.get(EncodingKey), format.get(CompressorNameKey), format.get(FrameRateKey).getDenominator() * format.get(FrameRateKey).getNumerator(), width, height, depth, syncInterval);
    }

    /** Adds an audio track, and configures it using an
     * {@code AudioFormat} object from the javax.sound API.
     * <p>
     * Use this method for writing audio data from an {@code AudioInputStream}
     * into a QuickTime Movie file.
     *
     * @param format The javax.sound audio format.
     * @return Returns the track index.
     */
    @Deprecated
    public int addAudioTrack(javax.sound.sampled.AudioFormat format) throws IOException {
        ensureStarted();
        String qtAudioFormat;
        double sampleRate = format.getSampleRate();
        long timeScale = (int) Math.floor(sampleRate);
        int sampleSizeInBits = format.getSampleSizeInBits();
        int numberOfChannels = format.getChannels();
        ByteOrder byteOrder = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        int frameDuration = (int) (format.getSampleRate() / format.getFrameRate());
        int frameSize = format.getFrameSize();
        boolean isCompressed = format.getProperty("vbr") != null && ((Boolean) format.getProperty("vbr")).booleanValue();
        boolean signed = false;
        javax.sound.sampled.AudioFormat.Encoding enc = format.getEncoding();
        if (enc.equals(javax.sound.sampled.AudioFormat.Encoding.ALAW)) {
            qtAudioFormat = "alaw";
            if (sampleSizeInBits != 8) {
                throw new IllegalArgumentException("Sample size of 8 for ALAW required:" + sampleSizeInBits);
            }
        } else if (javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED.equals(enc)) {
            switch (sampleSizeInBits) {
                case 8:// Requires conversion to PCM_UNSIGNED!
                    qtAudioFormat = "raw ";
                    break;
                case 16:
                    qtAudioFormat = (byteOrder == ByteOrder.BIG_ENDIAN) ? "twos" : "sowt";
                    break;
                case 24:
                    qtAudioFormat = "in24";
                    break;
                case 32:
                    qtAudioFormat = "in32";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sample size for PCM_SIGNED:" + sampleSizeInBits);
            }
        } else if (javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED.equals(enc)) {
            switch (sampleSizeInBits) {
                case 8:
                    qtAudioFormat = "raw ";
                    break;
                case 16:// Requires conversion to PCM_SIGNED!
                    qtAudioFormat = (byteOrder == ByteOrder.BIG_ENDIAN) ? "twos" : "sowt";
                    break;
                case 24:// Requires conversion to PCM_SIGNED!
                    qtAudioFormat = "in24";
                    break;
                case 32:// Requires conversion to PCM_SIGNED!
                    qtAudioFormat = "in32";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported sample size for PCM_UNSIGNED:" + sampleSizeInBits);
            }
        } else if (javax.sound.sampled.AudioFormat.Encoding.ULAW.equals(enc)) {
            if (sampleSizeInBits != 8) {
                throw new IllegalArgumentException("Sample size of 8 for ULAW required:" + sampleSizeInBits);
            }
            qtAudioFormat = "ulaw";
        } else if ("MP3".equals(enc == null ? null : enc.toString())) {
            qtAudioFormat = ".mp3";
        } else {
            qtAudioFormat = format.getEncoding().toString();
            if (qtAudioFormat == null || qtAudioFormat.length() != 4) {
                throw new IllegalArgumentException("Unsupported encoding:" + format.getEncoding());
            }
        }

        return addAudioTrack(qtAudioFormat, timeScale, sampleRate,
                numberOfChannels, sampleSizeInBits,
                isCompressed, frameDuration, frameSize, signed, byteOrder);
    }

    @Override
    public int getTrackCount() {
        return tracks.size();
    }

    /** Returns the sampleDuration of the track in seconds. */
    @Override
    public Rational getDuration(int track) {
        Track tr = tracks.get(track);
        return new Rational(tr.mediaDuration, tr.mediaTimeScale);
    }

    private Codec createCodec(Format fmt) {
        Codec[] codecs = Registry.getInstance().getEncoders(fmt.prepend(MimeTypeKey, MIME_QUICKTIME));
        Codec c= codecs.length == 0 ? null : codecs[0];
        return c;
    }

    private void createCodec(int track) {
        Track tr=tracks.get(track);
        Format fmt = tr.format;
        tr.codec = createCodec(fmt);
        String enc = fmt.get(EncodingKey);
        if (tr.codec != null) {
            if (fmt.get(MediaTypeKey) == MediaType.VIDEO) {
                Format vf = (Format) fmt;
                tr.codec.setInputFormat(fmt.prepend(
                        MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE,
                        DataClassKey, BufferedImage.class));

                if (null == tr.codec.setOutputFormat(
                        fmt.prepend(
                        QualityKey, getCompressionQuality(track),
                        MimeTypeKey, MIME_QUICKTIME,
                        DataClassKey, byte[].class))) {
                    throw new UnsupportedOperationException("Input format not supported:" + fmt);
                }
                //tr.codec.setQuality(tr.videoQuality);
            } else {
                Format vf = (Format) fmt;
                tr.codec.setInputFormat(fmt.prepend(
                        MimeTypeKey, MIME_JAVA, EncodingKey, fmt.containsKey(SignedKey) && fmt.get(SignedKey) ? ENCODING_PCM_SIGNED : ENCODING_PCM_UNSIGNED,
                        DataClassKey, byte[].class));
                if (tr.codec.setOutputFormat(fmt) == null) {
                    throw new UnsupportedOperationException("Codec output format not supported:" + fmt + " codec:" + tr.codec);
                } else {
                    tr.format = tr.codec.getOutputFormat();
                }
                //tr.codec.setQuality(tr.dwQuality);
            }
        }
    }

    /** Returns the codec of the specified track. */
    public Codec getCodec(int track) {
        return tracks.get(track).codec;
    }

    /** Sets the codec for the specified track. */
    public void setCodec(int track, Codec codec) {
        tracks.get(track).codec = codec;
    }

    /** Writes a sample.
     * Does nothing if the discard-flag in the buffer is set to true.
     *
     * @param track The track number.
     * @param buf The buffer containing the sample data.
     */
    @Override
    public void write(int track, Buffer buf) throws IOException {
        ensureStarted();
        Track tr = tracks.get(track);

        // Encode sample data
        {
            if (tr.outputBuffer == null) {
                tr.outputBuffer = new Buffer();
                tr.outputBuffer.format = tr.format;
            }
            Buffer outBuf;
            if (tr.format.matchesWithout(buf.format,FrameRateKey)) {
                outBuf = buf;
            } else {
                outBuf = tr.outputBuffer;
                boolean isSync = tr.syncInterval == 0 ? false : tr.sampleCount % tr.syncInterval == 0;
                buf.setFlag(KEYFRAME, isSync);
                if (tr.codec == null) {
                    createCodec(track);
                    if (tr.codec == null) {
                        throw new UnsupportedOperationException("No codec for this format " + tr.format);
                    }
                }

                tr.codec.process(buf, outBuf);
            }
            if (outBuf.isFlag(DISCARD)||outBuf.sampleCount==0) {
                return;
            }

            // Compute sample sampleDuration in media time scale
            Rational sampleDuration;
            if (tr.inputTime == null) {
                tr.inputTime = new Rational(0, 1);
                tr.writeTime = new Rational(0, 1);
            }
            tr.inputTime = tr.inputTime.add(outBuf.sampleDuration.multiply(outBuf.sampleCount));
            Rational exactSampleDuration = tr.inputTime.subtract(tr.writeTime);
            sampleDuration = exactSampleDuration.floor(tr.mediaTimeScale);
            if (sampleDuration.compareTo(new Rational(0, 1)) <= 0) {
                sampleDuration = new Rational(1, tr.mediaTimeScale);
            }
            tr.writeTime = tr.writeTime.add(sampleDuration);
            long sampleDurationInMediaTS = sampleDuration.getNumerator() * (tr.mediaTimeScale / sampleDuration.getDenominator());

            writeSamples(track, buf.sampleCount, (byte[]) outBuf.data, outBuf.offset, outBuf.length,
                    sampleDurationInMediaTS / buf.sampleCount, outBuf.isFlag(KEYFRAME));
        }
    }

    /**
     * Encodes an image as a video frame and writes it into a video track.
     *
     * @param track The track index.
     * @param image The image of the video frame.
     * @param duration The sampleDuration of the video frame in media time scale units.
     *
     * @throws IndexOutofBoundsException if the track index is out of bounds.
     * @throws if the duration is less than 1, or if the dimension of the frame
     * does not match the dimension of the video.
     * @throws UnsupportedOperationException if the QuickTimeWriter does not have
     * a built-in codec for this video format.
     * @throws IOException if writing the sample data failed.
     */
    public void write(int track, BufferedImage image, long duration) throws IOException {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be greater 0.");
        }
        VideoTrack vt = (VideoTrack) tracks.get(track); // throws index out of bounds exception if illegal track index
        if (vt.mediaType != MediaType.VIDEO) {
            throw new IllegalArgumentException("Track " + track + " is not a video track");
        }
        if (vt.codec == null) {
            createCodec(track);
        }
        if (vt.codec == null) {
            throw new UnsupportedOperationException("No codec for this format: " + vt.format);
        }
        ensureStarted();

        // Get the dimensions of the first image
        if (vt.width == -1) {
            vt.width = image.getWidth();
            vt.height = image.getHeight();
        } else {
            // The dimension of the image must match the dimension of the video track
            if (vt.width != image.getWidth() || vt.height != image.getHeight()) {
                throw new IllegalArgumentException("Dimensions of frame[" + tracks.get(track).getSampleCount()
                        + "] (width=" + image.getWidth() + ", height=" + image.getHeight()
                        + ") differs from video dimension (width="
                        + vt.width + ", height=" + vt.height + ") in track " + track + ".");
            }
        }

        // Encode pixel data
        {

            if (vt.outputBuffer == null) {
                vt.outputBuffer = new Buffer();
            }

            boolean isSync = vt.syncInterval == 0 ? false : vt.sampleCount % vt.syncInterval == 0;

            Buffer inputBuffer = new Buffer();
            inputBuffer.setFlag(KEYFRAME, isSync);
            inputBuffer.data = image;
            vt.codec.process(inputBuffer, vt.outputBuffer);
            if (vt.outputBuffer.isFlag(DISCARD)) {
                return;
            }

            isSync = vt.outputBuffer.isFlag(KEYFRAME);

            long offset = getRelativeStreamPosition();
            OutputStream mdatOut = mdatAtom.getOutputStream();
            mdatOut.write((byte[]) vt.outputBuffer.data, vt.outputBuffer.offset, vt.outputBuffer.length);

            long length = getRelativeStreamPosition() - offset;
            vt.addSample(new Sample(duration, offset, length), 1, isSync);
        }
    }

    /**
     * Writes a sample from a byte array into a track.
     * <p>
     * This method encodes the sample if the format of the track does not match
     * the format of the media in this track.
     *
     * @param track The track index.
     * @param data The sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write.
     * @param duration The duration of the sample in media time scale units.
     * @param isSync Whether the sample is a sync sample (keyframe).
     *
     * @throws IllegalArgumentException if the sampleDuration is less than 1.
     * @throws IOException if writing the sample data failed.
     */
    @Deprecated
    public void write(int track, byte[] data, int off, int len, long duration, boolean isSync) throws IOException {
        writeSamples(track, 1, data, off, len, duration, isSync);
    }

    /**
     * Writes multiple already encoded samples from a byte array into a track.
     * <p>
     * This method does not inspect the contents of the data. The
     * contents has to match the format and dimensions of the media in this
     * track.
     *
     * @param track The track index.
     * @param sampleCount The number of samples.
     * @param data The encoded sample data.
     * @param off The start offset in the data.
     * @param len The number of bytes to write. Must be dividable by sampleCount.
     * @param sampleDuration The sampleDuration of a sample. All samples must
     * have the same sampleDuration.
     * @param isSync Whether the samples are sync samples. All samples must
     * either be sync samples or non-sync samples.
     *
     * @throws IllegalArgumentException if the sampleDuration is less than 1.
     * @throws IOException if writing the sample data failed.
     */
    @Deprecated
    public void write(int track, int sampleCount, byte[] data, int off, int len, long sampleDuration, boolean isSync) throws IOException {
        Track tr = tracks.get(track);
        if (tr.codec == null) {
            writeSamples(track, sampleCount, data, off, len, sampleDuration, isSync);
        } else {
            if (tr.outputBuffer == null) {
                tr.outputBuffer = new Buffer();
            }
            if (tr.inputBuffer == null) {
                tr.inputBuffer = new Buffer();
            }
            Buffer outb = tr.outputBuffer;
            Buffer inb = tr.inputBuffer;
            inb.data = data;
            inb.offset = off;
            inb.length = len;
            inb.sampleDuration = new Rational(sampleDuration, tr.mediaTimeScale);
            inb.sampleCount = sampleCount;
            inb.setFlag(KEYFRAME, isSync);
            tr.codec.process(inb, outb);
            if (!outb.isFlag(DISCARD)) {
                writeSample(track, (byte[]) outb.data, outb.offset, outb.length, outb.sampleCount, outb.isFlag(KEYFRAME));
            }
        }
    }

    /** Returns true because QuickTime supports variable frame rates. */
    public boolean isVFRSupported() {
        return true;
    }

    /** Returns true if the limit for media samples has been reached.
     * If this limit is reached, no more samples should be added to the movie.
     * <p>
     * QuickTime files can be up to 64 TB long, but there are other values that
     * may overflow before this size is reached. This method returns true
     * when the files size exceeds 2^60 or when the media sampleDuration value of a
     * track exceeds 2^61.
     */
    @Override
    public boolean isDataLimitReached() {
        return super.isDataLimitReached();
    }
    @Override
    public boolean isEmpty(int track) {
       return tracks.get(track).isEmpty();
    }
}
