/*
 * @(#)AbstractQuickTimeStream.java  1.0  2011-03-15
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import java.io.UnsupportedEncodingException;
import org.monte.media.Buffer;
import org.monte.media.Codec;
import org.monte.media.Format;
import org.monte.media.io.ImageOutputStreamAdapter;
import org.monte.media.math.Rational;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import javax.imageio.stream.ImageOutputStream;
import static org.monte.media.FormatKeys.*;

/**
 * This is the base class for low-level QuickTime stream IO.
 *
 * <p>FIXME - Separation between AbstractQuickTimeStream and
 * QuickTimeOutputStream is not clean. Move write methods in the track classes
 * down to QuickTimeOutputStream.</p>
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-15 Created.
 */
public class AbstractQuickTimeStream {

    /**
     * Underlying output stream.
     */
    protected ImageOutputStream out;
    /**
     * The offset in the underlying ImageOutputStream. Normally this is 0 unless
     * the underlying stream already contained data when it was passed to the
     * constructor.
     */
    protected long streamOffset;
    /**
     * This atom holds the media data.
     */
    protected WideDataAtom mdatAtom;
    /**
     * Offset for the mdat atom.
     */
    protected long mdatOffset;
    /**
     * This atom holds the moovie header.
     */
    protected CompositeAtom moovAtom;
    /**
     * Creation time of the movie.
     */
    protected Date creationTime;
    /**
     * Modification time of the movie.
     */
    protected Date modificationTime;
    /**
     * The timeScale of the movie. A time value that indicates the time scale
     * for this media—that is, the number of time units that pass per second in
     * its time coordinate system.
     */
    protected long movieTimeScale = 600;
    /**
     * The preferred rate at which to play this movie. A value of 1.0 indicates
     * normal rate.
     */
    protected double preferredRate = 1d;
    /**
     * The preferred volume of this movie’s sound. A value of 1.0 indicates full
     * volume.
     */
    protected double preferredVolume = 1d;
    /**
     * The time value in the movie at which the preview begins.
     */
    protected long previewTime = 0;
    /**
     * The duration of the movie preview in movie time scale units.
     */
    protected long previewDuration = 0;
    /**
     * The time value of the time of the movie poster.
     */
    protected long posterTime = 0;
    /**
     * The time value for the start time of the current selection.
     */
    protected long selectionTime = 0;
    /**
     * The duration of the current selection in movie time scale units.
     */
    protected long selectionDuration = 0;
    /**
     * The time value for current time position within the movie.
     */
    protected long currentTime = 0;
    /**
     * The list of tracks in the movie.
     */
    protected ArrayList<Track> tracks = new ArrayList<Track>();
    /**
     * The transformation matrix for the entire movie.
     */
    protected double[] movieMatrix = {1, 0, 0, 0, 1, 0, 0, 0, 1};

    /**
     * The states of the movie output stream.
     */
    protected static enum States {

        REALIZED, STARTED, FINISHED, CLOSED;
    }
    /**
     * The current state of the movie output stream.
     */
    protected States state = States.REALIZED;

    /**
     * Gets the position relative to the beginning of the QuickTime stream. <p>
     * Usually this value is equal to the stream position of the underlying
     * ImageOutputStream, but can be larger if the underlying stream already
     * contained data.
     *
     * @return The relative stream position.
     * @throws IOException
     */
    protected long getRelativeStreamPosition() throws IOException {
        return out.getStreamPosition() - streamOffset;
    }

    /**
     * Seeks relative to the beginning of the QuickTime stream. <p> Usually this
     * equal to seeking in the underlying ImageOutputStream, but can be
     * different if the underlying stream already contained data.
     *
     */
    protected void seekRelative(long newPosition) throws IOException {
        out.seek(newPosition + streamOffset);
    }

    protected static int typeToInt(String str) {
        int value = ((str.charAt(0) & 0xff) << 24) |//
                ((str.charAt(1) & 0xff) << 16) | //
                ((str.charAt(2) & 0xff) << 8) | //
                (str.charAt(3) & 0xff);
        return value;
    }

    protected static String intToType(int id) {
        char[] b = new char[4];

        b[0] = (char) ((id >>> 24) & 0xff);
        b[1] = (char) ((id >>> 16) & 0xff);
        b[2] = (char) ((id >>> 8) & 0xff);
        b[3] = (char) (id & 0xff);
        return String.valueOf(b);
    }

    /**
     * Atom base class.
     */
    protected abstract class Atom {

        /**
         * The type of the atom. A String with the length of 4 characters.
         */
        protected String type;
        /**
         * The offset of the atom relative to the start of the
         * ImageOutputStream.
         */
        protected long offset;

        /**
         * Creates a new Atom at the current position of the ImageOutputStream.
         *
         * @param type The type of the atom. A string with a length of 4
         * characters.
         */
        public Atom(String type, long offset) {
            this.type = type;
            this.offset = offset;
        }

        /**
         * Writes the atom to the ImageOutputStream and disposes it.
         */
        public abstract void finish() throws IOException;

        /**
         * Returns the size of the atom including the size of the atom header.
         *
         * @return The size of the atom.
         */
        public abstract long size();
    }

    /**
     * A CompositeAtom contains an ordered list of Atoms.
     */
    protected class CompositeAtom extends DataAtom {

        protected LinkedList<Atom> children;

        /**
         * Creates a new CompositeAtom at the current position of the
         * ImageOutputStream.
         *
         * @param type The type of the atom.
         */
        public CompositeAtom(String type) throws IOException {
            super(type);
            children = new LinkedList<Atom>();
        }

        public void add(Atom child) throws IOException {
            if (children.size() > 0) {
                children.getLast().finish();
            }
            children.add(child);
        }

        /**
         * Writes the atom and all its children to the ImageOutputStream and
         * disposes of all resources held by the atom.
         *
         * @throws java.io.IOException
         */
        @Override
        public void finish() throws IOException {
            if (!finished) {
                if (size() > 0xffffffffL) {
                    throw new IOException("CompositeAtom \"" + type + "\" is too large: " + size());
                }

                long pointer = getRelativeStreamPosition();
                seekRelative(offset);

                DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
                headerData.writeInt((int) size());
                headerData.writeType(type);
                for (Atom child : children) {
                    child.finish();
                }
                seekRelative(pointer);
                finished = true;
            }
        }

        @Override
        public long size() {
            long length = 8 + data.size();
            for (Atom child : children) {
                length += child.size();
            }
            return length;
        }
    }

    /**
     * Data Atom.
     */
    protected class DataAtom extends Atom {

        protected DataAtomOutputStream data;
        protected boolean finished;

        /**
         * Creates a new DataAtom at the current position of the
         * ImageOutputStream.
         *
         * @param type The type name of the atom.
         */
        public DataAtom(String type) throws IOException {
            super(type, getRelativeStreamPosition());
            out.writeLong(0); // make room for the atom header
            data = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
        }

        public DataAtomOutputStream getOutputStream() {
            if (finished) {
                throw new IllegalStateException("DataAtom is finished");
            }
            return data;
        }

        /**
         * Returns the offset of this atom to the beginning of the random access
         * file
         */
        public long getOffset() {
            return offset;
        }

        @Override
        public void finish() throws IOException {
            if (!finished) {
                long sizeBefore = size();

                if (size() > 0xffffffffL) {
                    throw new IOException("DataAtom \"" + type + "\" is too large: " + size());
                }

                long pointer = getRelativeStreamPosition();
                seekRelative(offset);

                DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
                headerData.writeUInt(size());
                headerData.writeType(type);
                seekRelative(pointer);
                finished = true;
                long sizeAfter = size();
                if (sizeBefore != sizeAfter) {
                    System.err.println("size mismatch " + sizeBefore + ".." + sizeAfter);
                }
            }
        }

        @Override
        public long size() {
            return 8 + data.size();
        }
    }

    /**
     * WideDataAtom can grow larger then 4 gigabytes.
     */
    protected class WideDataAtom extends Atom {

        protected DataAtomOutputStream data;
        protected boolean finished;

        /**
         * Creates a new DataAtom at the current position of the
         * ImageOutputStream.
         *
         * @param type The type of the atom.
         */
        public WideDataAtom(String type) throws IOException {
            super(type, getRelativeStreamPosition());
            out.writeLong(0); // make room for the atom header
            out.writeLong(0); // make room for the atom header
            data = new DataAtomOutputStream(new ImageOutputStreamAdapter(out)) {
                @Override
                public void flush() throws IOException {
                    // DO NOT FLUSH UNDERLYING STREAM!
                }
            };
        }

        public DataAtomOutputStream getOutputStream() {
            if (finished) {
                throw new IllegalStateException("Atom is finished");
            }
            return data;
        }

        /**
         * Returns the offset of this atom to the beginning of the random access
         * file
         */
        public long getOffset() {
            return offset;
        }

        @Override
        public void finish() throws IOException {
            if (!finished) {
                long pointer = getRelativeStreamPosition();
                seekRelative(offset);

                DataAtomOutputStream headerData = new DataAtomOutputStream(new ImageOutputStreamAdapter(out));
                long finishedSize = size();
                if (finishedSize <= 0xffffffffL) {
                    headerData.writeUInt(8);
                    headerData.writeType("wide");
                    headerData.writeUInt(finishedSize - 8);
                    headerData.writeType(type);
                } else {
                    headerData.writeInt(1); // special value for extended size atoms
                    headerData.writeType(type);
                    headerData.writeLong(finishedSize - 8);
                }

                seekRelative(pointer);
                finished = true;
            }
        }

        @Override
        public long size() {
            return 16 + data.size();
        }
    }

    /**
     * Groups consecutive samples with same characteristics.
     */
    protected abstract static class Group {

        protected Sample firstSample;
        protected Sample lastSample;
        protected long sampleCount;
        protected final static long maxSampleCount = Integer.MAX_VALUE;

        protected Group(Sample firstSample) {
            this.firstSample = this.lastSample = firstSample;
            sampleCount = 1;
        }

        protected Group(Sample firstSample, Sample lastSample, long sampleCount) {
            this.firstSample = firstSample;
            this.lastSample = lastSample;
            this.sampleCount = sampleCount;
            if (sampleCount > maxSampleCount) {
                throw new IllegalArgumentException("Capacity exceeded");
            }
        }

        protected Group(Group group) {
            this.firstSample = group.firstSample;
            this.lastSample = group.lastSample;
            sampleCount = group.sampleCount;
        }

        /**
         * Returns true, if the samples was added to the group. If false is
         * returned, the sample must be added to a new group. <p> A sample can
         * only be added to a group, if the capacity of the group is not
         * exceeded.
         */
        protected boolean maybeAddSample(Sample sample) {
            if (sampleCount < maxSampleCount) {
                lastSample = sample;
                sampleCount++;
                return true;
            }
            return false;
        }

        /**
         * Returns true, if the chunk was added to the group. If false is
         * returned, the chunk must be added to a new group. <p> A chunk can
         * only be added to a group, if the capacity of the group is not
         * exceeded.
         */
        protected boolean maybeAddChunk(Chunk chunk) {
            if (sampleCount + chunk.sampleCount <= maxSampleCount) {
                lastSample = chunk.lastSample;
                sampleCount += chunk.sampleCount;
                return true;
            }
            return false;
        }

        public long getSampleCount() {
            return sampleCount;
        }
    }

    /**
     * QuickTime stores media data in samples. A sample is a single element in a
     * sequence of time-ordered data. Samples are stored in the mdat atom.
     */
    protected static class Sample {

        /**
         * Offset of the sample relative to the start of the QuickTime file.
         */
        long offset;
        /**
         * Data length of the sample.
         */
        long length;
        /**
         * The duration of the sample in media time scale units.
         */
        long duration;

        /**
         * Creates a new sample.
         *
         * @param duration
         * @param offset
         * @param length
         */
        public Sample(long duration, long offset, long length) {
            this.duration = duration;
            this.offset = offset;
            this.length = length;
        }
    }

    /**
     * Groups consecutive smples of the same duration.
     */
    protected static class TimeToSampleGroup extends Group {

        public TimeToSampleGroup(Sample firstSample) {
            super(firstSample);
        }

        public TimeToSampleGroup(Group group) {
            super(group);
        }

        /**
         * Returns true, if the sample was added to the group. If false is
         * returned, the sample must be added to a new group. <p> A sample can
         * only be added to a TimeToSampleGroup, if it has the same duration as
         * previously added samples, and if the capacity of the group is not
         * exceeded.
         */
        @Override
        public boolean maybeAddSample(Sample sample) {
            if (firstSample.duration == sample.duration) {
                return super.maybeAddSample(sample);
            }
            return false;
        }

        @Override
        public boolean maybeAddChunk(Chunk chunk) {
            if (firstSample.duration == chunk.firstSample.duration) {
                return super.maybeAddChunk(chunk);
            }
            return false;
        }

        /**
         * Returns the duration that all samples in this group share.
         */
        public long getSampleDuration() {
            return firstSample.duration;
        }
    }

    /**
     * Groups consecutive samples of the same size.
     */
    protected static class SampleSizeGroup extends Group {

        public SampleSizeGroup(Sample firstSample) {
            super(firstSample);
        }

        public SampleSizeGroup(Group group) {
            super(group);
        }

        /**
         * Returns true, if the sample was added to the group. If false is
         * returned, the sample must be added to a new group. <p> A sample can
         * only be added to a SampleSizeGroup, if it has the same size as
         * previously added samples, and if the capacity of the group is not
         * exceeded.
         */
        @Override
        public boolean maybeAddSample(Sample sample) {
            if (firstSample.length == sample.length) {
                return super.maybeAddSample(sample);
            }
            return false;
        }

        @Override
        public boolean maybeAddChunk(Chunk chunk) {
            if (firstSample.length == chunk.firstSample.length) {
                return super.maybeAddChunk(chunk);
            }
            return false;
        }

        /**
         * Returns the length that all samples in this group share.
         */
        public long getSampleLength() {
            return firstSample.length;
        }
    }

    /**
     * Groups consecutive samples with the same sample description Id and with
     * adjacent offsets in the movie file.
     */
    protected static class Chunk extends Group {

        protected int sampleDescriptionId;

        /**
         * Creates a new Chunk.
         *
         * @param firstSample The first sample contained in this chunk.
         * @param sampleDescriptionId The description Id of the sample.
         */
        public Chunk(Sample firstSample, int sampleDescriptionId) {
            super(firstSample);
            this.sampleDescriptionId = sampleDescriptionId;
        }

        /**
         * Creates a new Chunk.
         *
         * @param firstSample The first sample contained in this chunk.
         * @param sampleDescriptionId The description Id of the sample.
         */
        public Chunk(Sample firstSample, Sample lastSample, int sampleCount, int sampleDescriptionId) {
            super(firstSample, lastSample, sampleCount);
            this.sampleDescriptionId = sampleDescriptionId;
        }

        /**
         * Returns true, if the sample was added to the chunk. If false is
         * returned, the sample must be added to a new chunk. <p> A sample can
         * only be added to a chunk, if it has the same sample description Id as
         * previously added samples, if the capacity of the chunk is not
         * exceeded and if the sample offset is adjacent to the last sample in
         * this chunk.
         */
        public boolean maybeAddSample(Sample sample, int sampleDescriptionId) {
            if (sampleDescriptionId == this.sampleDescriptionId
                    && lastSample.offset + lastSample.length == sample.offset) {
                return super.maybeAddSample(sample);
            }
            return false;
        }

        @Override
        public boolean maybeAddChunk(Chunk chunk) {
            if (sampleDescriptionId == chunk.sampleDescriptionId //
                    && lastSample.offset + lastSample.length == chunk.firstSample.offset) {
                return super.maybeAddChunk(chunk);
            }
            return false;
        }

        /**
         * Returns the offset of the chunk in the movie file.
         */
        public long getChunkOffset() {
            return firstSample.offset;
        }
    }

    /**
     * Represents a track.
     */
    protected abstract class Track {

        // Common metadata
        /**
         * The media type of the track.
         */
        protected final MediaType mediaType;
        /**
         * The format of the media in the track.
         */
        protected Format format;
        /**
         * The timeScale of the media in the track. A time value that indicates
         * the time scale for this media. That is, the number of time units that
         * pass per second in its time coordinate system.
         */
        protected long mediaTimeScale = 600;
        /**
         * The compression type of the media.
         */
        protected String mediaCompressionType;
        /**
         * The compressor name.
         */
        protected String mediaCompressorName;
        /**
         * List of chunks.
         */
        protected ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        /**
         * List of TimeToSample entries.
         */
        protected ArrayList<TimeToSampleGroup> timeToSamples = new ArrayList<TimeToSampleGroup>();
        /**
         * List of SampleSize entries.
         */
        protected ArrayList<SampleSizeGroup> sampleSizes = new ArrayList<SampleSizeGroup>();
        /**
         * List of sync samples. This list is null as long as all samples in
         * this track are sync samples.
         */
        protected ArrayList<Long> syncSamples = null;
        /**
         * The number of samples in this track.
         */
        protected long sampleCount = 0;
        /**
         * The duration of the media in this track in media time units.
         */
        protected long mediaDuration = 0;
        /**
         * The edit list of the track.
         */
        protected Edit[] editList;
        /**
         * Interval between sync samples (keyframes). 0 = automatic. 1 = write
         * all samples as sync samples. n = sync every n-th sample.
         */
        protected int syncInterval;
        /**
         * The codec.
         */
        protected Codec codec;
        protected Buffer outputBuffer;
        protected Buffer inputBuffer;
        /**
         * Start time of the first buffer that was added to the track.
         */
        protected Rational inputTime;
        /**
         * Current write time.
         */
        protected Rational writeTime;
        /**
         * The transformation matrix of the track.
         */
        protected double[] matrix = {//
            1, 0, 0,//
            0, 1, 0,//
            0, 0, 1
        };
        protected double width, height;

        public Track(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public void addSample(Sample sample, int sampleDescriptionId, boolean isSyncSample) {
            mediaDuration += sample.duration;
            sampleCount++;

            // Keep track of sync samples. If all samples in a track are sync
            // samples, we do not need to create a syncSample list.
            if (isSyncSample) {
                if (syncSamples != null) {
                    syncSamples.add(sampleCount);
                }
            } else {
                if (syncSamples == null) {
                    syncSamples = new ArrayList<Long>();
                    for (long i = 1; i < sampleCount; i++) {
                        syncSamples.add(i);
                    }
                }
            }

            //
            if (timeToSamples.isEmpty()//
                    || !timeToSamples.get(timeToSamples.size() - 1).maybeAddSample(sample)) {
                timeToSamples.add(new TimeToSampleGroup(sample));
            }
            if (sampleSizes.isEmpty()//
                    || !sampleSizes.get(sampleSizes.size() - 1).maybeAddSample(sample)) {
                sampleSizes.add(new SampleSizeGroup(sample));
            }
            if (chunks.isEmpty()//
                    || !chunks.get(chunks.size() - 1).maybeAddSample(sample, sampleDescriptionId)) {
                chunks.add(new Chunk(sample, sampleDescriptionId));
            }
        }

        public void addChunk(Chunk chunk, boolean isSyncSample) {
            mediaDuration += chunk.firstSample.duration * chunk.sampleCount;
            sampleCount += chunk.sampleCount;

            // Keep track of sync samples. If all samples in a track are sync
            // samples, we do not need to create a syncSample list.
            if (isSyncSample) {
                if (syncSamples != null) {
                    for (long i = sampleCount - chunk.sampleCount; i < sampleCount; i++) {
                        syncSamples.add(i);
                    }
                }
            } else {
                if (syncSamples == null) {
                    syncSamples = new ArrayList<Long>();
                    for (long i = 1; i < sampleCount; i++) {
                        syncSamples.add(i);
                    }
                }
            }

            //
            if (timeToSamples.isEmpty()//
                    || !timeToSamples.get(timeToSamples.size() - 1).maybeAddChunk(chunk)) {
                timeToSamples.add(new TimeToSampleGroup(chunk));
            }
            if (sampleSizes.isEmpty()//
                    || !sampleSizes.get(sampleSizes.size() - 1).maybeAddChunk(chunk)) {
                sampleSizes.add(new SampleSizeGroup(chunk));
            }
            if (chunks.isEmpty()//
                    || !chunks.get(chunks.size() - 1).maybeAddChunk(chunk)) {
                chunks.add(chunk);
            }
        }

        public boolean isEmpty() {
            return sampleCount == 0;
        }

        public long getSampleCount() {
            return sampleCount;
        }

        /**
         * Gets the track duration in the movie time scale.
         *
         * @param movieTimeScale The time scale of the movie.
         */
        public long getTrackDuration(long movieTimeScale) {
            if (editList == null || editList.length == 0) {
                return mediaDuration * movieTimeScale / mediaTimeScale;
            } else {
                long duration = 0;
                for (int i = 0; i < editList.length; ++i) {
                    duration += editList[i].trackDuration;
                }
                return duration;
            }
        }

        /** FIXME - Move this method into QuickTimeOutputStream. */ 
        protected void writeTrackAtoms(int trackIndex, CompositeAtom moovAtom, Date modificationTime) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;

            /* Track Atom ======== */
            CompositeAtom trakAtom = new CompositeAtom("trak");
            moovAtom.add(trakAtom);

            /* Track Header Atom -----------
             * The track header atom specifies the characteristics of a single track
             * within a movie. A track header atom contains a size field that
             * specifies the number of bytes and a type field that indicates the
             * format of the data (defined by the atom type 'tkhd').
             *
             typedef struct {
             byte version;
             byte flag0;
             byte flag1;
             byte set TrackHeaderFlags flag2;
             mactimestamp creationTime;
             mactimestamp modificationTime;
             int trackId;
             byte[4] reserved;
             int duration;
             byte[8] reserved;
             short layer;
             short alternateGroup;
             short volume;
             byte[2] reserved;
             int[9] matrix;
             int trackWidth;
             int trackHeight;
             } trackHeaderAtom;     */
            leaf = new DataAtom("tkhd");
            trakAtom.add(leaf);
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this track header.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            // FIXME - trackHeaderFlag should be a variable
            d.write(0xf); // flag[2]
            // Three bytes that are reserved for the track header flags. These flags
            // indicate how the track is used in the movie. The following flags are
            // valid (all flags are enabled when set to 1):
            //
            // Track enabled
            //      Indicates that the track is enabled. Flag value is 0x0001.
            // Track in movie
            //      Indicates that the track is used in the movie. Flag value is
            //      0x0002.
            // Track in preview
            //      Indicates that the track is used in the movie’s preview. Flag
            //      value is 0x0004.
            // Track in poster
            //      Indicates that the track is used in the movie’s poster. Flag
            //      value is 0x0008.

            d.writeMacTimestamp(creationTime); // creationTime
            // A 32-bit integer that indicates the calendar date and time (expressed
            // in seconds since midnight, January 1, 1904) when the track header was
            // created. It is strongly recommended that this value should be
            // specified using coordinated universal time (UTC).

            d.writeMacTimestamp(modificationTime); // modificationTime
            // A 32-bit integer that indicates the calendar date and time (expressed
            // in seconds since midnight, January 1, 1904) when the track header was
            // changed. It is strongly recommended that this value should be
            // specified using coordinated universal time (UTC).

            d.writeInt(trackIndex + 1); // trackId
            // A 32-bit integer that uniquely identifies the track. The value 0
            // cannot be used.

            d.writeInt(0); // reserved;
            // A 32-bit integer that is reserved for use by Apple. Set this field to 0.

            d.writeUInt(getTrackDuration(movieTimeScale)); // duration
            // A time value that indicates the duration of this track (in the
            // movie’s time coordinate system). Note that this property is derived
            // from the track’s edits. The value of this field is equal to the sum
            // of the durations of all of the track’s edits. If there is no edit
            // list, then the duration is the sum of the sample durations, converted
            // into the movie timescale.

            d.writeLong(0); // reserved
            // An 8-byte value that is reserved for use by Apple. Set this field to 0.

            d.writeShort(0); // layer;
            // A 16-bit integer that indicates this track’s spatial priority in its
            // movie. The QuickTime Movie Toolbox uses this value to determine how
            // tracks overlay one another. Tracks with lower layer values are
            // displayed in front of tracks with higher layer values.

            d.writeShort(0); // alternate group
            // A 16-bit integer that specifies a collection of movie tracks that
            // contain alternate data for one another. QuickTime chooses one track
            // from the group to be used when the movie is played. The choice may be
            // based on such considerations as playback quality, language, or the
            // capabilities of the computer.

            d.writeFixed8D8(mediaType == MediaType.AUDIO ? 1 : 0); // volume
            // A 16-bit fixed-point value that indicates how loudly this track’s
            // sound is to be played. A value of 1.0 indicates normal volume.

            d.writeShort(0); // reserved
            // A 16-bit integer that is reserved for use by Apple. Set this field to 0.

            d.writeFixed16D16(matrix[0]); // matrix[0]
            d.writeFixed16D16(matrix[1]); // matrix[1]
            d.writeFixed2D30(matrix[2]); // matrix[2]
            d.writeFixed16D16(matrix[3]); // matrix[3]
            d.writeFixed16D16(matrix[4]); // matrix[4]
            d.writeFixed2D30(matrix[5]); // matrix[5]
            d.writeFixed16D16(matrix[6]); // matrix[6]
            d.writeFixed16D16(matrix[7]); // matrix[7]
            d.writeFixed2D30(matrix[8]); // matrix[8]
            // The matrix structure associated with this track.
            // See Figure 2-8 for an illustration of a matrix structure:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/chapter_3_section_3.html#//apple_ref/doc/uid/TP40000939-CH204-32967

            d.writeFixed16D16(mediaType == MediaType.VIDEO ? ((VideoTrack) this).width : 0); // width
            // A 32-bit fixed-point number that specifies the width of this track in pixels.

            d.writeFixed16D16(mediaType == MediaType.VIDEO ? ((VideoTrack) this).height : 0); // height
            // A 32-bit fixed-point number that indicates the height of this track in pixels.

            /* Edit Atom ========= */
            CompositeAtom edtsAtom = new CompositeAtom("edts");
            trakAtom.add(edtsAtom);

            /* Edit List atom ------- */
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int numberOfEntries;
             editListTable editListTable[numberOfEntries];
             } editListAtom;
            
             typedef struct {
             int trackDuration;
             int mediaTime;
             fixed16d16 mediaRate;
             } editListTable;
             */
            leaf = new DataAtom("elst");
            edtsAtom.add(leaf);
            d = leaf.getOutputStream();

            d.write(0); // version
            // One byte that specifies the version of this header atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]

            Edit[] elist = editList;
            if (elist == null || elist.length == 0) {
                d.writeUInt(1); // numberOfEntries
                d.writeUInt(getTrackDuration(movieTimeScale)); // trackDuration
                d.writeUInt(0); // mediaTime
                d.writeFixed16D16(1); // mediaRate
            } else {
                d.writeUInt(elist.length); // numberOfEntries
                for (int i = 0; i < elist.length; ++i) {
                    d.writeUInt(elist[i].trackDuration); // trackDuration
                    d.writeUInt(elist[i].mediaTime); // mediaTime
                    d.writeUInt(elist[i].mediaRate); // mediaRate
                }
            }


            /* Media Atom ========= */
            CompositeAtom mdiaAtom = new CompositeAtom("mdia");
            trakAtom.add(mdiaAtom);

            /* Media Header atom -------
             typedef struct {
             byte version;
             byte[3] flags;
             mactimestamp creationTime;
             mactimestamp modificationTime;
             int timeScale;
             int duration;
             short language;
             short quality;
             } mediaHeaderAtom;*/
            leaf = new DataAtom("mdhd");
            mdiaAtom.add(leaf);
            d = leaf.getOutputStream();
            d.write(0); // version
            // One byte that specifies the version of this header atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // Three bytes of space for media header flags. Set this field to 0.

            d.writeMacTimestamp(creationTime); // creationTime
            // A 32-bit integer that specifies (in seconds since midnight, January
            // 1, 1904) when the media atom was created. It is strongly recommended
            // that this value should be specified using coordinated universal time
            // (UTC).

            d.writeMacTimestamp(modificationTime); // modificationTime
            // A 32-bit integer that specifies (in seconds since midnight, January
            // 1, 1904) when the media atom was changed. It is strongly recommended
            // that this value should be specified using coordinated universal time
            // (UTC).

            d.writeUInt(mediaTimeScale); // timeScale
            // A time value that indicates the time scale for this media—that is,
            // the number of time units that pass per second in its time coordinate
            // system.

            d.writeUInt(mediaDuration); // duration
            // The duration of this media in units of its time scale.

            d.writeShort(0); // language;
            // A 16-bit integer that specifies the language code for this media.
            // See “Language Code Values” for valid language codes:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap4/chapter_5_section_2.html#//apple_ref/doc/uid/TP40000939-CH206-27005

            d.writeShort(0); // quality
            // A 16-bit integer that specifies the media’s playback quality—that is,
            // its suitability for playback in a given environment.

            /**
             * Media Handler Reference Atom -------
             */
            leaf = new DataAtom("hdlr");
            mdiaAtom.add(leaf);
            /*typedef struct {
             byte version;
             byte[3] flags;
             magic componentType;
             magic componentSubtype;
             magic componentManufacturer;
             int componentFlags;
             int componentFlagsMask;
             pstring componentName;
             } handlerReferenceAtom;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this handler information.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for handler information flags. Set this field to 0.

            d.writeType("mhlr"); // componentType
            // A four-character code that identifies the type of the handler. Only
            // two values are valid for this field: 'mhlr' for media handlers and
            // 'dhlr' for data handlers.

            d.writeType(mediaType == MediaType.VIDEO ? "vide" : "soun"); // componentSubtype
            // A four-character code that identifies the type of the media handler
            // or data handler. For media handlers, this field defines the type of
            // data—for example, 'vide' for video data or 'soun' for sound data.
            //
            // For data handlers, this field defines the data reference type—for
            // example, a component subtype value of 'alis' identifies a file alias.

            if (mediaType == MediaType.AUDIO) {
                d.writeType("appl");
            } else {
                d.writeUInt(0);
            }
            // componentManufacturer
            // Reserved. Set to 0.

            d.writeUInt(mediaType == MediaType.AUDIO ? 268435456L : 0); // componentFlags
            // Reserved. Set to 0.

            d.writeUInt(mediaType == MediaType.AUDIO ? 65941 : 0); // componentFlagsMask
            // Reserved. Set to 0.

            d.writePString(mediaType == MediaType.AUDIO ? "Apple Sound Media Handler" : ""); // componentName (empty string)
            // A (counted) string that specifies the name of the component—that is,
            // the media handler used when this media was created. This field may
            // contain a zero-length (empty) string.

            /* Media Information atom ========= */
            writeMediaInformationAtoms(mdiaAtom);
        }

        protected void writeMediaInformationAtoms(CompositeAtom mdiaAtom) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;
            /* Media Information atom ========= */
            CompositeAtom minfAtom = new CompositeAtom("minf");
            mdiaAtom.add(minfAtom);

            /* Video or Audio media information atom -------- */
            writeMediaInformationHeaderAtom(minfAtom);


            /* Data Handler Reference Atom -------- */
            // The handler reference atom specifies the media handler component that
            // is to be used to interpret the media’s data. The handler reference
            // atom has an atom type value of 'hdlr'.
            leaf = new DataAtom("hdlr");
            minfAtom.add(leaf);
            /*typedef struct {
             byte version;
             byte[3] flags;
             magic componentType;
             magic componentSubtype;
             magic componentManufacturer;
             int componentFlags;
             int componentFlagsMask;
             pstring componentName;
             } handlerReferenceAtom;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this handler information.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for handler information flags. Set this field to 0.

            d.writeType("dhlr"); // componentType
            // A four-character code that identifies the type of the handler. Only
            // two values are valid for this field: 'mhlr' for media handlers and
            // 'dhlr' for data handlers.

            d.writeType("alis"); // componentSubtype
            // A four-character code that identifies the type of the media handler
            // or data handler. For media handlers, this field defines the type of
            // data—for example, 'vide' for video data or 'soun' for sound data.
            // For data handlers, this field defines the data reference type—for
            // example, a component subtype value of 'alis' identifies a file alias.

            if (mediaType == MediaType.AUDIO) {
                d.writeType("appl");
            } else {
                d.writeUInt(0);
            }
            // componentManufacturer
            // Reserved. Set to 0.

            d.writeUInt(mediaType == MediaType.AUDIO ? 268435457L : 0); // componentFlags
            // Reserved. Set to 0.

            d.writeInt(mediaType == MediaType.AUDIO ? 65967 : 0); // componentFlagsMask
            // Reserved. Set to 0.

            d.writePString("Apple Alias Data Handler"); // componentName (empty string)
            // A (counted) string that specifies the name of the component—that is,
            // the media handler used when this media was created. This field may
            // contain a zero-length (empty) string.

            /* Data information atom ===== */
            CompositeAtom dinfAtom = new CompositeAtom("dinf");
            minfAtom.add(dinfAtom);

            /* Data reference atom ----- */
            // Data reference atoms contain tabular data that instructs the data
            // handler component how to access the media’s data.
            leaf = new DataAtom("dref");
            dinfAtom.add(leaf);
            /*typedef struct {
             ubyte version;
             ubyte[3] flags;
             int numberOfEntries;
             dataReferenceEntry dataReference[numberOfEntries];
             } dataReferenceAtom;
            
             set {
             dataRefSelfReference=1 // I am not shure if this is the correct value for this flag
             } drefEntryFlags;
            
             typedef struct {
             int size;
             magic type;
             byte version;
             ubyte flag1;
             ubyte flag2;
             ubyte set drefEntryFlags flag3;
             byte[size - 12] data;
             } dataReferenceEntry;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this data reference atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for data reference flags. Set this field to 0.

            d.writeInt(1); // numberOfEntries
            // A 32-bit integer containing the count of data references that follow.

            d.writeInt(12); // dataReference.size
            // A 32-bit integer that specifies the number of bytes in the data
            // reference.

            d.writeType("alis"); // dataReference.type
            // A 32-bit integer that specifies the type of the data in the data
            // reference. Table 2-4 lists valid type values:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/chapter_3_section_4.html#//apple_ref/doc/uid/TP40000939-CH204-38840

            d.write(0); // dataReference.version
            // A 1-byte specification of the version of the data reference.

            d.write(0); // dataReference.flag1
            d.write(0); // dataReference.flag2
            d.write(0x1); // dataReference.flag3
            // A 3-byte space for data reference flags. There is one defined flag.
            //
            // Self reference
            //      This flag indicates that the media’s data is in the same file as
            //      the movie atom. On the Macintosh, and other file systems with
            //      multifork files, set this flag to 1 even if the data resides in
            //      a different fork from the movie atom. This flag’s value is
            //      0x0001.


            /* Sample Table atom ========= */
            writeSampleTableAtoms(minfAtom);
        }

        protected abstract void writeMediaInformationHeaderAtom(CompositeAtom minfAtom) throws IOException;

        protected abstract void writeSampleDescriptionAtom(CompositeAtom stblAtom) throws IOException;

        protected void writeSampleTableAtoms(CompositeAtom minfAtom) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;

            /* Sample Table atom ========= */
            CompositeAtom stblAtom = new CompositeAtom("stbl");
            minfAtom.add(stblAtom);

            /* Sample Description atom ------- */
            writeSampleDescriptionAtom(stblAtom);


            /* Time to Sample atom ---- */
            // Time-to-sample atoms store duration information for a media’s
            // samples, providing a mapping from a time in a media to the
            // corresponding data sample. The time-to-sample atom has an atom type
            // of 'stts'.
            leaf = new DataAtom("stts");
            stblAtom.add(leaf);
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int numberOfEntries;
             timeToSampleTable timeToSampleTable[numberOfEntries];
             } timeToSampleAtom;
            
             typedef struct {
             int sampleCount;
             int sampleDuration;
             } timeToSampleTable;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this time-to-sample atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for time-to-sample flags. Set this field to 0.

            d.writeUInt(timeToSamples.size()); // numberOfEntries
            // A 32-bit integer containing the count of entries in the
            // time-to-sample table.

            for (TimeToSampleGroup tts : timeToSamples) {
                d.writeUInt(tts.getSampleCount()); // timeToSampleTable[0].sampleCount
                // A 32-bit integer that specifies the number of consecutive
                // samples that have the same duration.

                d.writeUInt(tts.getSampleDuration()); // timeToSampleTable[0].sampleDuration
                // A 32-bit integer that specifies the duration of each
                // sample.
            }
            /* sample to chunk atom -------- */
            // The sample-to-chunk atom contains a table that maps samples to chunks
            // in the media data stream. By examining the sample-to-chunk atom, you
            // can determine the chunk that contains a specific sample.
            leaf = new DataAtom("stsc");
            stblAtom.add(leaf);
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int numberOfEntries;
             sampleToChunkTable sampleToChunkTable[numberOfEntries];
             } sampleToChunkAtom;
            
             typedef struct {
             int firstChunk;
             int samplesPerChunk;
             int sampleDescription;
             } sampleToChunkTable;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this time-to-sample atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for time-to-sample flags. Set this field to 0.

            int entryCount = 0;
            long previousSampleCount = -1;
            long previousSampleDescriptionId = -1;
            for (Chunk c : chunks) {
                if (c.sampleCount != previousSampleCount//
                        || c.sampleDescriptionId != previousSampleDescriptionId) {
                    previousSampleCount = c.sampleCount;
                    previousSampleDescriptionId = c.sampleDescriptionId;
                    entryCount++;
                }
            }

            d.writeInt(entryCount); // number of entries
            // A 32-bit integer containing the count of entries in the sample-to-chunk table.

            int firstChunk = 1;
            previousSampleCount = -1;
            previousSampleDescriptionId = -1;
            for (Chunk c : chunks) {
                if (c.sampleCount != previousSampleCount//
                        || c.sampleDescriptionId != previousSampleDescriptionId) {
                    previousSampleCount = c.sampleCount;
                    previousSampleDescriptionId = c.sampleDescriptionId;

                    d.writeUInt(firstChunk); // first chunk
                    // The first chunk number using this table entry.

                    d.writeUInt(c.sampleCount); // samples per chunk
                    // The number of samples in each chunk.

                    d.writeInt(c.sampleDescriptionId); // sample description

                    // The identification number associated with the sample description for
                    // the sample. For details on sample description atoms, see “Sample
                    // Description Atoms.”:
                    // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap2/chapter_3_section_5.html#//apple_ref/doc/uid/TP40000939-CH204-25691
                }
                firstChunk++;
            }
            //
        /* sync sample atom -------- */
            if (syncSamples != null) {
                leaf = new DataAtom("stss");
                stblAtom.add(leaf);
                /*
                 typedef struct {
                 byte version;
                 byte[3] flags;
                 int numberOfEntries;
                 syncSampleTable syncSampleTable[numberOfEntries];
                 } syncSampleAtom;
                
                 typedef struct {
                 int number;
                 } syncSampleTable;
                 */
                d = leaf.getOutputStream();
                d.write(0); // version
                // A 1-byte specification of the version of this time-to-sample atom.

                d.write(0); // flag[0]
                d.write(0); // flag[1]
                d.write(0); // flag[2]
                // A 3-byte space for time-to-sample flags. Set this field to 0.

                d.writeUInt(syncSamples.size());
                // Number of entries
                //A 32-bit integer containing the count of entries in the sync sample table.

                for (Long number : syncSamples) {
                    d.writeUInt(number);
                    // Sync sample table A table of sample numbers; each sample
                    // number corresponds to a key frame.
                }
            }


            /* sample size atom -------- */
            // The sample size atom contains the sample count and a table giving the
            // size of each sample. This allows the media data itself to be
            // unframed. The total number of samples in the media is always
            // indicated in the sample count. If the default size is indicated, then
            // no table follows.
            leaf = new DataAtom("stsz");
            stblAtom.add(leaf);
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int sampleSize;
             int numberOfEntries;
             sampleSizeTable sampleSizeTable[numberOfEntries];
             } sampleSizeAtom;
            
             typedef struct {
             int size;
             } sampleSizeTable;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this time-to-sample atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for time-to-sample flags. Set this field to 0.

            int sampleUnit = mediaType == MediaType.AUDIO//
                    && ((AudioTrack) this).soundCompressionId != -2 //
                    ? ((AudioTrack) this).soundSampleSize / 8 * ((AudioTrack) this).soundNumberOfChannels//
                    : 1;
            if (sampleSizes.size() == 1) {
                d.writeUInt(sampleSizes.get(0).getSampleLength() / sampleUnit); // sample size
                // A 32-bit integer specifying the sample size. If all the samples are
                // the same size, this field contains that size value. If this field is
                // set to 0, then the samples have different sizes, and those sizes are
                // stored in the sample size table.

                d.writeUInt(sampleSizes.get(0).getSampleCount()); // number of entries
                // A 32-bit integer containing the count of entries in the sample size
                // table.

            } else {
                d.writeUInt(0); // sample size
                // A 32-bit integer specifying the sample size. If all the samples are
                // the same size, this field contains that size value. If this field is
                // set to 0, then the samples have different sizes, and those sizes are
                // stored in the sample size table.


                long count = 0;
                for (SampleSizeGroup s : sampleSizes) {
                    count += s.sampleCount;
                }
                d.writeUInt(count); // number of entries
                // A 32-bit integer containing the count of entries in the sample size
                // table.

                for (SampleSizeGroup s : sampleSizes) {
                    long sampleSize = s.getSampleLength() / sampleUnit;
                    for (int i = 0; i < s.sampleCount; i++) {
                        d.writeUInt(sampleSize); // sample size
                        // The size field contains the size, in bytes, of the sample in
                        // question. The table is indexed by sample number—the first entry
                        // corresponds to the first sample, the second entry is for the
                        // second sample, and so on.
                    }
                }
            }
            //
        /* chunk offset atom -------- */
            // The chunk-offset table gives the index of each chunk into the
            // QuickTime Stream. There are two variants, permitting the use of
            // 32-bit or 64-bit offsets. The latter is useful when managing very
            // large movies. Only one of these variants occurs in any single
            // instance of a sample table atom.
            if (chunks.isEmpty() || chunks.get(chunks.size() - 1).getChunkOffset() <= 0xffffffffL) {
                /* 32-bit chunk offset atom -------- */
                leaf = new DataAtom("stco");
                stblAtom.add(leaf);
                /*
                 typedef struct {
                 byte version;
                 byte[3] flags;
                 int numberOfEntries;
                 chunkOffsetTable chunkOffsetTable[numberOfEntries];
                 } chunkOffsetAtom;
                
                 typedef struct {
                 int offset;
                 } chunkOffsetTable;
                 */
                d = leaf.getOutputStream();
                d.write(0); // version
                // A 1-byte specification of the version of this time-to-sample atom.

                d.write(0); // flag[0]
                d.write(0); // flag[1]
                d.write(0); // flag[2]
                // A 3-byte space for time-to-sample flags. Set this field to 0.

                d.writeUInt(chunks.size()); // number of entries
                // A 32-bit integer containing the count of entries in the chunk
                // offset table.
                for (Chunk c : chunks) {
                    d.writeUInt(c.getChunkOffset() + mdatOffset); // offset
                    // The offset contains the byte offset from the beginning of the
                    // data stream to the chunk. The table is indexed by chunk
                    // number—the first table entry corresponds to the first chunk,
                    // the second table entry is for the second chunk, and so on.
                }
            } else {
                /* 64-bit chunk offset atom -------- */
                leaf = new DataAtom("co64");
                stblAtom.add(leaf);
                /*
                 typedef struct {
                 byte version;
                 byte[3] flags;
                 int numberOfEntries;
                 chunkOffsetTable chunkOffset64Table[numberOfEntries];
                 } chunkOffset64Atom;
                
                 typedef struct {
                 long offset;
                 } chunkOffset64Table;
                 */
                d = leaf.getOutputStream();
                d.write(0); // version
                // A 1-byte specification of the version of this time-to-sample atom.

                d.write(0); // flag[0]
                d.write(0); // flag[1]
                d.write(0); // flag[2]
                // A 3-byte space for time-to-sample flags. Set this field to 0.

                d.writeUInt(chunks.size()); // number of entries
                // A 32-bit integer containing the count of entries in the chunk
                // offset table.

                for (Chunk c : chunks) {
                    d.writeLong(c.getChunkOffset()); // offset
                    // The offset contains the byte offset from the beginning of the
                    // data stream to the chunk. The table is indexed by chunk
                    // number—the first table entry corresponds to the first chunk,
                    // the second table entry is for the second chunk, and so on.
                }
            }
        }
    }

    protected class VideoTrack extends Track {
        // Video metadata

        /**
         * The video compression quality.
         */
        protected float videoQuality = 0.97f;
        /**
         * Number of bits per ixel. All frames must have the same depth. The
         * value -1 is used to mark unspecified depth.
         */
        protected int videoDepth = -1;
        /**
         * The color table used for rendering the video. This variable is only
         * used when the video uses an index color model.
         */
        protected IndexColorModel videoColorTable;

        public VideoTrack() {
            super(MediaType.VIDEO);
        }

        @Override
        protected void writeMediaInformationHeaderAtom(CompositeAtom minfAtom) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;

            /* Video media information atom -------- */
            leaf = new DataAtom("vmhd");
            minfAtom.add(leaf);
            /*typedef struct {
             byte version;
             byte flag1;
             byte flag2;
             byte set vmhdFlags flag3;
             short graphicsMode;
             ushort[3] opcolor;
             } videoMediaInformationHeaderAtom;*/
            d = leaf.getOutputStream();
            d.write(0); // version
            // One byte that specifies the version of this header atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0x1); // flag[2]
            // Three bytes of space for media header flags.
            // This is a compatibility flag that allows QuickTime to distinguish
            // between movies created with QuickTime 1.0 and newer movies. You
            // should always set this flag to 1, unless you are creating a movie
            // intended for playback using version 1.0 of QuickTime. This flag’s
            // value is 0x0001.

            d.writeShort(0x40); // graphicsMode (0x40 = DitherCopy)
            // A 16-bit integer that specifies the transfer mode. The transfer mode
            // specifies which Boolean operation QuickDraw should perform when
            // drawing or transferring an image from one location to another.
            // See “Graphics Modes” for a list of graphics modes supported by
            // QuickTime:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap4/chapter_5_section_5.html#//apple_ref/doc/uid/TP40000939-CH206-18741

            d.writeUShort(0); // opcolor[0]
            d.writeUShort(0); // opcolor[1]
            d.writeUShort(0); // opcolor[2]
            // Three 16-bit values that specify the red, green, and blue colors for
            // the transfer mode operation indicated in the graphics mode field.
        }

        @Override
        protected void writeSampleDescriptionAtom(CompositeAtom stblAtom) throws IOException {
            CompositeAtom leaf;
            DataAtomOutputStream d;

            /* Sample Description atom ------- */
            // The sample description atom stores information that allows you to
            // decode samples in the media. The data stored in the sample
            // description varies, depending on the media type. For example, in the
            // case of video media, the sample descriptions are image description
            // structures. The sample description information for each media type is
            // explained in “Media Data Atom Types”:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap3/chapter_4_section_1.html#//apple_ref/doc/uid/TP40000939-CH205-SW1
            leaf = new CompositeAtom("stsd");
            stblAtom.add(leaf);
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int numberOfEntries;
             sampleDescriptionEntry sampleDescriptionTable[numberOfEntries];
             } sampleDescriptionAtom;
            
             typedef struct {
             int size;
             magic type;
             byte[6] reserved; // six bytes that must be zero
             short dataReferenceIndex; // A 16-bit integer that contains the index of the data reference to use to retrieve data associated with samples that use this sample description. Data references are stored in data reference atoms.
             byte[size - 16] data;
             } sampleDescriptionEntry;
             */
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this sample description atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for sample description flags. Set this field to 0.

            d.writeInt(1); // number of Entries
            // A 32-bit integer containing the number of sample descriptions that follow.

            // A 32-bit integer indicating the number of bytes in the sample description.
            d.writeInt(86); // sampleDescriptionTable[0].size

            d.writeType(mediaCompressionType); // sampleDescriptionTable[0].type

            // A 32-bit integer indicating the format of the stored data.
            // This depends on the media type, but is usually either the
            // compression format or the media type.

            d.write(new byte[6]); // sampleDescriptionTable[0].reserved
            // Six bytes that must be set to 0.

            d.writeShort(1); // sampleDescriptionTable[0].dataReferenceIndex
            // A 16-bit integer that contains the index of the data
            // reference to use to retrieve data associated with samples
            // that use this sample description. Data references are stored
            // in data reference atoms.

            // Video Sample Description
            // ------------------------
            // The format of the following fields is described here:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap3/chapter_4_section_2.html#//apple_ref/doc/uid/TP40000939-CH205-BBCGICBJ

            d.writeShort(0); // sampleDescriptionTable.videoSampleDescription.version
            // A 16-bit integer indicating the version number of the
            // compressed data. This is set to 0, unless a compressor has
            // changed its data format.

            d.writeShort(0); // sampleDescriptionTable.videoSampleDescription.revisionLevel
            // A 16-bit integer that must be set to 0.

            d.writeType("java"); // sampleDescriptionTable.videoSampleDescription.manufacturer
            // A 32-bit integer that specifies the developer of the
            // compressor that generated the compressed data. Often this
            // field contains 'appl' to indicate Apple Computer, Inc.

            d.writeInt(0);  // sampleDescriptionTable.videoSampleDescription.temporalQuality
            // A 32-bit integer containing a value from 0 to 1023 indicating
            // the degree of temporal compression.

            d.writeInt(512); // sampleDescriptionTable.videoSampleDescription.spatialQuality
            // A 32-bit integer containing a value from 0 to 1024 indicating
            // the degree of spatial compression.

            d.writeUShort((int) width); // sampleDescriptionTable.videoSampleDescription.width
            // A 16-bit integer that specifies the width of the source image
            // in pixels.

            d.writeUShort((int) height); // sampleDescriptionTable.videoSampleDescription.height
            // A 16-bit integer that specifies the height of the source image in pixels.

            d.writeFixed16D16(72.0); // sampleDescriptionTable.videoSampleDescription.horizontalResolution
            // A 32-bit fixed-point number containing the horizontal
            // resolution of the image in pixels per inch.

            d.writeFixed16D16(72.0); // sampleDescriptionTable.videoSampleDescription.verticalResolution
            // A 32-bit fixed-point number containing the vertical
            // resolution of the image in pixels per inch.

            d.writeInt(0); // sampleDescriptionTable.videoSampleDescription.dataSize
            // A 32-bit integer that must be set to 0.

            d.writeShort(1); // sampleDescriptionTable.videoSampleDescription.sampleCount
            // A 16-bit integer that indicates how many bytes of compressed
            // data are stored in each sample. Usually set to 1.

            d.writePString(mediaCompressorName, 32); // sampleDescriptionTable.videoSampleDescription.compressorName
            // A 32-byte Pascal string containing the name of the compressor
            // that created the image, such as "jpeg".

            d.writeShort(videoDepth); // sampleDescriptionTable.videoSampleDescription.depth
            // A 16-bit integer that indicates the pixel depth of the
            // compressed image. Values of 1, 2, 4, 8 ,16, 24, and 32
            // indicate the depth of color images. The value 32 should be
            // used only if the image contains an alpha channel. Values of
            // 34, 36, and 40 indicate 2-, 4-, and 8-bit grayscale,
            // respectively, for grayscale images.

            d.writeShort(videoColorTable == null ? -1 : 0); // sampleDescriptionTable.videoSampleDescription.colorTableID
            // A 16-bit integer that identifies which color table to use.
            // If this field is set to –1, the default color table should be
            // used for the specified depth. For all depths below 16 bits
            // per pixel, this indicates a standard Macintosh color table
            // for the specified depth. Depths of 16, 24, and 32 have no
            // color table.



            if (videoColorTable != null) {
                writeColorTableAtom(leaf);
            }
        }

        /**
         * Color table atoms define a list of preferred colors for displaying
         * the movie on devices that support only 256 colors. The list may
         * contain up to 256 colors. These optional atoms have a type value of
         * 'ctab'. The color table atom contains a Macintosh color table data
         * structure.
         *
         * @param stblAtom
         * @throws IOException
         */
        protected void writeColorTableAtom(CompositeAtom stblAtom) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;
            leaf = new DataAtom("ctab");
            stblAtom.add(leaf);

            d = leaf.getOutputStream();

            d.writeUInt(0); // Color table seed. A 32-bit integer that must be set to 0.
            d.writeUShort(0x8000); // Color table flags. A 16-bit integer that must be set to 0x8000.
            d.writeUShort(videoColorTable.getMapSize() - 1);
            // Color table size. A 16-bit integer that indicates the number of
            // colors in the following color array. This is a zero-relative value;
            // setting this field to 0 means that there is one color in the array.

            for (int i = 0, n = videoColorTable.getMapSize(); i < n; ++i) {
                // An array of colors. Each color is made of four unsigned 16-bit integers.
                // The first integer must be set to 0, the second is the red value,
                // the third is the green value, and the fourth is the blue value.
                d.writeUShort(0);
                d.writeUShort((videoColorTable.getRed(i) << 8) | videoColorTable.getRed(i));
                d.writeUShort((videoColorTable.getGreen(i) << 8) | videoColorTable.getGreen(i));
                d.writeUShort((videoColorTable.getBlue(i) << 8) | videoColorTable.getBlue(i));
            }
        }
    }

    protected class AudioTrack extends Track {
        // Audio metadata

        /**
         * Number of sound channels used by the sound sample.
         */
        protected int soundNumberOfChannels;
        /**
         * Number of bits per audio sample before compression.
         */
        protected int soundSampleSize;
        /**
         * Sound compressionId. The value -1 means fixed bit rate, -2 means
         * varable bit rate.
         */
        protected int soundCompressionId;
        /**
         * Sound stsd samples per packet. The number of uncompressed samples
         * generated by a compressed sample (an uncompressed sample is one
         * sample from each channel). This is also the sample duration,
         * expressed in the media’s timescale, where the timescale is equal to
         * the sample rate. For uncompressed formats, this field is always 1.
         */
        protected long soundSamplesPerPacket;
        /**
         * For uncompressed audio, the number of bytes in a sample for a single
         * channel. This replaces the older sampleSize field, which is set to
         * 16. This value is calculated by dividing the frame size by the number
         * of channels. The same calculation is performed to calculate the value
         * of this field for compressed audio, but the result of the calculation
         * is not generally meaningful for compressed audio.
         */
        protected int soundBytesPerPacket;
        /**
         * The number of bytes in a frame: for uncompressed audio, an
         * uncompressed frame; for compressed audio, a compressed frame. This
         * can be calculated by multiplying the bytes per packet field by the
         * number of channels.
         */
        protected int soundBytesPerFrame;
        /**
         * The size of an uncompressed sample in bytes. This is set to 1 for
         * 8-bit audio, 2 for all other cases, even if the sample size is
         * greater than 2 bytes.
         */
        protected int soundBytesPerSample;
        /**
         * Sound sample rate. The integer portion must match the media's time
         * scale.
         */
        protected double soundSampleRate;
        /**
         * Extensions to the stsd chunk. Must contain atom-based fields: ([long
         * size, long type, some data], repeat)
         */
        protected byte[] stsdExtensions = new byte[0];

        public AudioTrack() {
            super(MediaType.AUDIO);
        }

        @Override
        protected void writeMediaInformationHeaderAtom(CompositeAtom minfAtom) throws IOException {
            DataAtom leaf;
            DataAtomOutputStream d;

            /* Sound media information header atom -------- */
            leaf = new DataAtom("smhd");
            minfAtom.add(leaf);
            /*typedef struct {
             ubyte version;
             ubyte[3] flags;
             short balance;
             short reserved;
             } soundMediaInformationHeaderAtom;*/
            d = leaf.getOutputStream();
            d.write(0); // version
            // A 1-byte specification of the version of this sound media information header atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for sound media information flags. Set this field to 0.

            d.writeFixed8D8(0); // balance
            // A 16-bit integer that specifies the sound balance of this
            // sound media. Sound balance is the setting that controls
            // the mix of sound between the two speakers of a computer.
            // This field is normally set to 0.
            // Balance values are represented as 16-bit, fixed-point
            // numbers that range from -1.0 to +1.0. The high-order 8
            // bits contain the integer portion of the value; the
            // low-order 8 bits contain the fractional part. Negative
            // values weight the balance toward the left speaker;
            // positive values emphasize the right channel. Setting the
            // balance to 0 corresponds to a neutral setting.

            d.writeUShort(0); // reserved
            // Reserved for use by Apple. Set this field to 0.

        }

        @Override
        protected void writeSampleDescriptionAtom(CompositeAtom stblAtom) throws IOException {
            // TO DO
            DataAtom leaf;
            DataAtomOutputStream d;

            /* Sample Description atom ------- */
            // The sample description atom stores information that allows you to
            // decode samples in the media. The data stored in the sample
            // description varies, depending on the media type. For example, in the
            // case of video media, the sample descriptions are image description
            // structures. The sample description information for each media type is
            // explained in “Media Data Atom Types”:
            // http://developer.apple.com/documentation/QuickTime/QTFF/QTFFChap3/chapter_4_section_1.html#//apple_ref/doc/uid/TP40000939-CH205-SW1
            leaf = new DataAtom("stsd");
            stblAtom.add(leaf);
            /*
             typedef struct {
             byte version;
             byte[3] flags;
             int numberOfEntries;
             soundSampleDescriptionEntry sampleDescriptionTable[numberOfEntries];
             } soundSampleDescriptionAtom;
            
             typedef struct {
             int size;
             magic type;
             byte[6] reserved;
             short dataReferenceIndex;
             soundSampleDescription data;
             } soundSampleDescriptionEntry;
            
             typedef struct {
             ushort version;
             ushort revisionLevel;
             uint vendor;
             ushort numberOfChannels;
             ushort sampleSize;
             short compressionId;
             ushort packetSize;
             fixed16d16 sampleRate;
             byte[] extendedData;
             } soundSampleDescription;
             */
            d = leaf.getOutputStream();

            // soundSampleDescriptionAtom:
            // ---------------------------
            d.write(0); // version
            // A 1-byte specification of the version of this sample description atom.

            d.write(0); // flag[0]
            d.write(0); // flag[1]
            d.write(0); // flag[2]
            // A 3-byte space for sample description flags. Set this field to 0.

            d.writeInt(1); // number of Entries
            // A 32-bit integer containing the number of sample descriptions that follow.

            // soundSampleDescriptionEntry:
            // ----------------------------
            // A 32-bit integer indicating the number of bytes in the sample description.
            d.writeUInt(4 + 12 + 20 + 16 + stsdExtensions.length); // sampleDescriptionTable[0].size

            // Common header: 12 bytes
            d.writeType(mediaCompressionType); // sampleDescriptionTable[0].type
            // A 32-bit integer indicating the format of the stored data.
            // This depends on the media type, but is usually either the
            // compression format or the media type.

            d.write(new byte[6]); // sampleDescriptionTable[0].reserved
            // Six bytes that must be set to 0.

            d.writeUShort(1); // sampleDescriptionTable[0].dataReferenceIndex
            // A 16-bit integer that contains the index of the data
            // reference to use to retrieve data associated with samples
            // that use this sample description. Data references are stored
            // in data reference atoms.

            // Sound Sample Description (Version 0) 20 bytes
            // ------------------------

            d.writeUShort(1); // version
            // A 16-bit integer that holds the sample description version (currently 0 or 1).

            d.writeUShort(0); // revisionLevel
            // A 16-bit integer that must be set to 0.

            d.writeUInt(0); // vendor
            // A 32-bit integer that must be set to 0.

            d.writeUShort(soundNumberOfChannels);  // numberOfChannels
            // A 16-bit integer that indicates the number of sound channels used by
            // the sound sample. Set to 1 for monaural sounds, 2 for stereo sounds.
            // Higher numbers of channels are not supported.

            d.writeUShort(soundSampleSize); // sampleSize (bits)
            // A 16-bit integer that specifies the number of bits in each
            // uncompressed sound sample. Allowable values are 8 or 16. Formats
            // using more than 16 bits per sample set this field to 16 and use sound
            // description version 1.

            d.writeUShort(soundCompressionId); // compressionId
            // XXX - This must be set to -1, or the QuickTime player won't accept this file.
            // A 16-bit integer that must be set to 0 for version 0 sound
            // descriptions. This may be set to –2 for some version 1 sound
            // descriptions; see “Redefined Sample Tables” (page 135).

            d.writeUShort(0); // packetSize
            // A 16-bit integer that must be set to 0.

            d.writeFixed16D16(soundSampleRate); // sampleRate
            // A 32-bit unsigned fixed-point number (16.16) that indicates the rate
            // at which the sound samples were obtained. The integer portion of this
            // number should match the media’s time scale. Many older version 0
            // files have values of 22254.5454 or 11127.2727, but most files have
            // integer values, such as 44100. Sample rates greater than 2^16 are not
            // supported.

            // Sound Sample Description Additional fields (only in Version 1) 16 bytes
            // ------------------------
            d.writeUInt(soundSamplesPerPacket); // samplesPerPacket
            // A 32-bit integer.
            // The number of uncompressed samples generated by a
            // compressed sample (an uncompressed sample is one sample
            // from each channel). This is also the sample duration,
            // expressed in the media’s timescale, where the
            // timescale is equal to the sample rate. For
            // uncompressed formats, this field is always 1.
            //
            d.writeUInt(soundBytesPerPacket); // bytesPerPacket
            // A 32-bit integer.
            // For uncompressed audio, the number of bytes in a
            // sample for a single channel. This replaces the older
            // sampleSize field, which is set to 16.
            // This value is calculated by dividing the frame size
            // by the number of channels. The same calculation is
            // performed to calculate the value of this field for
            // compressed audio, but the result of the calculation
            // is not generally meaningful for compressed audio.
            //
            d.writeUInt(soundBytesPerFrame); // bytesPerFrame
            // A 32-bit integer.
            // The number of bytes in a sample: for uncompressed
            // audio, an uncompressed frame; for compressed audio, a
            // compressed frame. This can be calculated by
            // multiplying the bytes per packet field by the number
            // of channels.
            //
            d.writeUInt(soundBytesPerSample); // bytesPerSample
            // A 32-bit integer.
            // The size of an uncompressed sample in bytes. This is
            // set to 1 for 8-bit audio, 2 for all other cases, even
            // if the sample size is greater than 2 bytes.

            // Write stsd Extensions
            // Extensions must be atom-based fields
            // ------------------------------------
            d.write(stsdExtensions);
        }
    }

    /**
     * An {@code Edit} define the portions of the media that are to be used to
     * build up a track for a movie. The edits themselves are stored in an edit
     * list table, which consists of time offset and duration values for each
     * segment. <p> In the absence of an edit list, the presentation of the
     * track starts immediately. An empty edit is used to offset the start time
     * of a track.
     */
    public static class Edit {

        /**
         * A 32-bit integer that specifies the duration of this edit segment in
         * units of the movie's time scale.
         */
        public int trackDuration;
        /**
         * A 32-bit integer containing the start time within the media of this
         * edit segment (in media time scale units). If this field is set to -1,
         * it is an empty edit. The last edit in a track should never be an
         * empty edit. Any differece between the movie's duration and the
         * track's duration is expressed as an implicit empty edit.
         */
        public int mediaTime;
        /**
         * A 32-bit fixed-point number (16.16) that specifies the relative rate
         * at which to play the media corresponding to this edit segment. This
         * rate value cannot be 0 or negative.
         */
        public int mediaRate;

        /**
         * Creates an edit.
         *
         * @param trackDuration Duration of this edit in the movie's time scale.
         * @param mediaTime Start time of this edit in the media's time scale.
         * Specify -1 for an empty edit. The last edit in a track should never
         * be an empty edit.
         * @param mediaRate The relative rate at which to play this edit.
         */
        public Edit(int trackDuration, int mediaTime, double mediaRate) {
            if (trackDuration < 0) {
                throw new IllegalArgumentException("trackDuration must not be < 0:" + trackDuration);
            }
            if (mediaTime < -1) {
                throw new IllegalArgumentException("mediaTime must not be < -1:" + mediaTime);
            }
            if (mediaRate <= 0) {
                throw new IllegalArgumentException("mediaRate must not be <= 0:" + mediaRate);
            }
            this.trackDuration = trackDuration;
            this.mediaTime = mediaTime;
            this.mediaRate = (int) (mediaRate * (1 << 16));
        }

        /**
         * Creates an edit. <p> Use this constructor only if you want to compute
         * the fixed point media rate by yourself.
         *
         * @param trackDuration Duration of this edit in the movie's time scale.
         * @param mediaTime Start time of this edit in the media's time scale.
         * Specify -1 for an empty edit. The last edit in a track should never
         * be an empty edit.
         * @param mediaRate The relative rate at which to play this edit given
         * as a 16.16 fixed point value.
         */
        public Edit(int trackDuration, int mediaTime, int mediaRate) {
            if (trackDuration < 0) {
                throw new IllegalArgumentException("trackDuration must not be < 0:" + trackDuration);
            }
            if (mediaTime < -1) {
                throw new IllegalArgumentException("mediaTime must not be < -1:" + mediaTime);
            }
            if (mediaRate <= 0) {
                throw new IllegalArgumentException("mediaRate must not be <= 0:" + mediaRate);
            }
            this.trackDuration = trackDuration;
            this.mediaTime = mediaTime;
            this.mediaRate = mediaRate;
        }
    }
}
