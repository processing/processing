/*
 * @(#)QuickTimeInputStream.java  
 * 
 * Copyright (c) 2012 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media.quicktime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.InflaterInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import org.monte.media.io.ImageInputStreamAdapter;

/**
 * Provides low-level support for reading encoded audio and video samples from a
 * QuickTime file.
 *
 * @author Werner Randelshofer
 * @version $Id: QuickTimeInputStream.java 307 2013-01-06 11:06:05Z werner $
 */
public class QuickTimeInputStream extends AbstractQuickTimeStream {

    /**
     * Same as DataAtom in super class, but used for reading the meta data.
     */
    private class InputAtom extends Atom {

        private byte[] data;

        public InputAtom(String type, byte[] data) throws IOException {
            super(type, -1);
            this.data = data;
        }

        @Override
        public void finish() throws IOException {
            //empty
        }

        @Override
        public long size() {
            return data.length;
        }
    }
    /**
     * The image input stream.
     */
    protected final ImageInputStream in;
    /**
     * This variable is set to true when all meta-data has been read from the
     * file.
     */
    private boolean isRealized = false;
    static final HashSet<String> compositeAtoms;

    static {
        compositeAtoms = new HashSet<String>();
        compositeAtoms.add("moov");
        compositeAtoms.add("cmov");
        compositeAtoms.add("gmhd");
        compositeAtoms.add("trak");
        compositeAtoms.add("tref");
        compositeAtoms.add("meta"); // sometimes has a special 4 byte header before its contents
        compositeAtoms.add("ilst");
        compositeAtoms.add("mdia");
        compositeAtoms.add("minf");
        compositeAtoms.add("udta");
        compositeAtoms.add("stbl");
        compositeAtoms.add("dinf");
        compositeAtoms.add("edts");
        compositeAtoms.add("clip");
        compositeAtoms.add("matt");
        compositeAtoms.add("rmra");
        compositeAtoms.add("rmda");
        compositeAtoms.add("tapt");
        compositeAtoms.add("mvex");
    }

    /**
     * Creates a new instance.
     *
     * @param file the input file
     */
    public QuickTimeInputStream(File file) throws IOException {

        this.in = new FileImageInputStream(file);
        in.setByteOrder(ByteOrder.BIG_ENDIAN);
        this.streamOffset = 0;
    }

    /**
     * Creates a new instance.
     *
     * @param in the input stream.
     */
    public QuickTimeInputStream(ImageInputStream in) throws IOException {
        this.in = in;
        this.streamOffset = in.getStreamPosition();
        in.setByteOrder(ByteOrder.BIG_ENDIAN);
    }

    public int getTrackCount() throws IOException {
        ensureRealized();
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getMovieDuration() throws IOException {
        ensureRealized();
        long duration = 0;
        for (Track t : tracks) {
            duration = Math.max(duration, t.getTrackDuration(movieTimeScale));
        }
        return duration;
    }

    /**
     * Gets the creation time of the movie.
     */
    public Date getCreationTime() throws IOException {
        ensureRealized();
        return creationTime;
    }

    /**
     * Gets the modification time of the movie.
     */
    public Date getModificationTime() throws IOException {
        ensureRealized();
        return modificationTime;
    }

    /**
     * Gets the preferred rate at which to play this movie. A value of 1.0
     * indicates normal rate.
     */
    public double getPreferredRate() throws IOException {
        ensureRealized();
        return preferredRate;
    }

    /**
     * Gets the preferred volume of this movie’s sound. A value of 1.0 indicates
     * full volume.
     */
    public double getPreferredVolume() throws IOException {
        ensureRealized();
        return preferredVolume;
    }

    /**
     * Gets the time value for current time position within the movie.
     */
    public long getCurrentTime() throws IOException {
        ensureRealized();
        return currentTime;
    }

    /**
     * Gets the time value of the time of the movie poster.
     */
    public long getPosterTime() throws IOException {
        ensureRealized();
        return posterTime;
    }

    /**
     * Gets the duration of the movie preview in movie time scale units.
     */
    public long getPreviewDuration() throws IOException {
        ensureRealized();
        return previewDuration;
    }

    /**
     * Gets the time value in the movie at which the preview begins.
     */
    public long getPreviewTime() throws IOException {
        ensureRealized();
        return previewTime;
    }

    /**
     * Gets the transformation matrix of the entire movie.
     *
     * @return The transformation matrix.
     */
    public double[] getMovieTransformationMatrix() throws IOException {
        ensureRealized();
        return movieMatrix.clone();
    }

    /**
     * Returns the time scale of the movie. <p> The movie time scale is used for
     * editing tracks. Such as for specifying the start time of a track.
     *
     * @return time scale
     */
    public long getMovieTimeScale() throws IOException {
        ensureRealized();
        return movieTimeScale;
    }

    /**
     * Returns the time scale of the media in a track. <p> The media time scale
     * is used for specifying the duration of samples in a track.
     *
     * @param track Track index.
     * @return time scale
     */
    public long getMediaTimeScale(int track) throws IOException {
        ensureRealized();
        return tracks.get(track).mediaTimeScale;
    }

    /**
     * Returns the media duration of a track in the media's time scale.
     *
     * @param track Track index.
     * @return media duration
     */
    public long getMediaDuration(int track) throws IOException {
        ensureRealized();
        return tracks.get(track).mediaDuration;
    }

    /**
     * Gets the transformation matrix of the specified track.
     *
     * @param track The track number.
     * @return The transformation matrix.
     */
    public double[] getTransformationMatrix(int track) throws IOException {
        ensureRealized();
        return tracks.get(track).matrix.clone();
    }

    /**
     * Ensures that all meta-data has been read from the file.
     */
    protected void ensureRealized() throws IOException {
        if (!isRealized) {
            isRealized = true;
            readAllMetadata();
        }
    }

    private void readAllMetadata() throws IOException {
        long remainingSize = in.length();
        if (remainingSize == -1) {
            remainingSize = Long.MAX_VALUE;
        }

        in.seek(0);
        readAllMetadata(new DataAtomInputStream(new ImageInputStreamAdapter(in)), remainingSize, new HashMap<String, InputAtom>(), null);
    }

    private void readAllMetadata(DataAtomInputStream in, long remainingSize, HashMap<String, InputAtom> atoms, String path) throws IOException {
        long pos = 0;

        InputAtom atom;

        while (remainingSize > 0) {
            long size = in.readInt() & 0xffffffffL;
            int headerSize = 8;
            if (size == 0) {
                // A zero len indicates that the size is the remainder of
                // the parent atom.
                size = remainingSize;

                // skip 4 bytes after zero size.
                if (headerSize + 4 <= remainingSize) {
                    in.skipBytes(4);
                    headerSize += 4;
                }

            } else if (size == 1) {
                // A size of 1 indicates a 64 bit size field
                headerSize = 16;
                size = in.readLong();
            }
            String type;
            long atomSize = size;
            if (size > remainingSize) {
                //System.out.println("QuickTimeStructView truncating size: " + size + " to:" + (remainingSize));
                size = remainingSize;
            }

            if (size - headerSize >= 0) {
                type = intToType(in.readInt());
                //System.out.println("QuickTimeStructView " + type + " size:" + size + " remaining:" + remainingSize);
            } else {
                type = "";
            }
            remainingSize -= size;

            // "stsd" atoms have a special structure if one of their parent
            // atoms contains a "vmhd" or a "smhd" child atom.
            if (type.equals("stsd")) {
                // handle stsd chunk
            }

            if (compositeAtoms.contains(type) && size - headerSize >= 8) {
                if (type.equals("trak")) {
                    atoms.clear();
                    readAllMetadata(in, size - headerSize, atoms, path == null ? type : path + "." + type);
                    parseTrack(atoms);
                } else {
                    readAllMetadata(in, size - headerSize, atoms, path == null ? type : path + "." + type);
                }
            } else {
                byte[] data;
                if (type.equals("mdat")) {
                    data = new byte[0];
                    long skipped = 0;
                    while (skipped < size - headerSize) {
                        long skipValue = in.skipBytes(size - headerSize - skipped);
                        if (skipValue > 0) {
                            skipped += skipValue;
                        } else {
                            throw new IOException("unable to skip");
                        }
                    }
                } else {
                    if (size < headerSize) {
                        // Pad atom?
                        data = new byte[0];
                    } else {
                        data = new byte[(int) (size - headerSize)];
                        in.readFully(data);
                    }
                }
                atom = new InputAtom(type, data);
                atoms.put(path == null ? type : path + "." + type, atom);

                if (type.equals("cmvd")) {
                    // => We have found a compressed movie header.
                    //    Decompress it and start over.
                    try {
                        InputStream in2 = new InflaterInputStream(new ByteArrayInputStream(data, 4, data.length - 4));
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int b;
                        while ((b = in2.read()) != -1) {
                            out.write(b);
                        }
                        in2.close();
                        out.close();

                        byte[] decompressed = out.toByteArray();
                        readAllMetadata(new DataAtomInputStream(
                                new ByteArrayInputStream(decompressed)),
                                decompressed.length,
                                atoms, path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (type.equals("mvhd")) {
                    // => We have found an uncompressed movie header.
                    //    Parse it.
                    parseMovieHeader(data);
                }
            }
            pos += size;
        }
    }

    public void close() throws IOException {
        in.close();
    }

    /**
     * Parses the mvhd atom.
     */
    private void parseMovieHeader(byte[] data) throws IOException {
        /* Movie Header Atom -------------
         * The data contained in this atom defines characteristics of the entire
         * QuickTime movie, such as time scale and duration. It has an atom type
         * value of 'mvhd'.
         *
         * typedef struct {
         byte version;
         byte[3] flags;
         mactimestamp creationTime;
         mactimestamp modificationTime;
         int timeScale;
         int duration;
         fixed16d16 preferredRate;
         fixed8d8 preferredVolume;
         byte[10] reserved;
         fixed16d16 matrixA;
         fixed16d16 matrixB;
         fixed2d30 matrixU;
         fixed16d16 matrixC;
         fixed16d16 matrixD;
         fixed2d30 matrixV;
         fixed16d16 matrixX;
         fixed16d16 matrixY;
         fixed2d30 matrixW;
         int previewTime;
         int previewDuration;
         int posterTime;
         int selectionTime;
         int selectionDuration;
         int currentTime;
         int nextTrackId;
         } movieHeaderAtom; 
         */
        DataAtomInputStream i = new DataAtomInputStream(new ByteArrayInputStream(data));
        int version = i.readByte();
        i.skipBytes(3);// flags
        creationTime = i.readMacTimestamp();
        modificationTime = i.readMacTimestamp();
        movieTimeScale = i.readUInt();
        long movieDuration = i.readUInt();
        preferredRate = i.readFixed16D16();
        preferredVolume = i.readFixed8D8();
        i.skipBytes(10);//reserved
        // {a, b, u,
        //  c, d, v,
        //  tx,ty,w} // X- and Y-Translation
        movieMatrix[0] = i.readFixed16D16();
        movieMatrix[1] = i.readFixed16D16();
        movieMatrix[2] = i.readFixed2D30();
        movieMatrix[3] = i.readFixed16D16();
        movieMatrix[4] = i.readFixed16D16();
        movieMatrix[5] = i.readFixed2D30();
        movieMatrix[6] = i.readFixed16D16();
        movieMatrix[7] = i.readFixed16D16();
        movieMatrix[8] = i.readFixed2D30();
        previewTime = i.readUInt();
        previewDuration = i.readUInt();
        posterTime = i.readUInt();
        selectionTime = i.readUInt();
        selectionDuration = i.readUInt();
        currentTime = i.readUInt();
        long nextTrackId = i.readUInt();
    }

    /**
     * Parses track atoms.
     */
    private void parseTrack(HashMap<String, InputAtom> atoms) throws IOException {
        for (String p : atoms.keySet()) {
            System.out.println("QuickTimeInputStream " + p);
        }
        HashMap<String, Object> hdlrMap = parseHdlr(atoms.get("moov.trak.mdia.hdlr").data);
        String trackType = (String) hdlrMap.get("componentSubtype");
        Track t;
        if ("vide".equals(trackType)) {
            t = new VideoTrack();
        } else if ("soun".equals(trackType)) {
            t = new AudioTrack();
        } else {
            throw new IOException("Unsupported track type: " + trackType);
        }

        parseTkhd(t, atoms.get("moov.trak.tkhd").data);
        if (atoms.get("moov.trak.edts") != null) {
            parseEdts(t, atoms.get("moov.trak.edts").data);
        }
        if (atoms.get("moov.trak.mdhd") != null) {
            parseMdhd(t, atoms.get("moov.trak.mdhd").data);
        }

        if ("vide".equals(trackType)) {
            parseVideoTrack((VideoTrack) t, atoms);
        } else if ("soun".equals(trackType)) {
            parseAudioTrack((AudioTrack) t, atoms);
        } else {
            throw new IOException("Unsupported track type: " + trackType);
        }
        tracks.add(t);
    }

    private void parseVideoTrack(VideoTrack t, HashMap<String, InputAtom> atoms) throws IOException {
    }

    private void parseAudioTrack(AudioTrack t, HashMap<String, InputAtom> atoms) throws IOException {
    }

    /**
     * Parses a tkhd atom.
     */
    private void parseTkhd(Track t, byte[] data) throws IOException {
        /*
         // Enumeration for track header flags
         set {
         TrackEnable = 0x1, // enabled track
         TrackInMovie = 0x2, // track in playback
         TrackInPreview = 0x4, // track in preview
         TrackInPoster = 0x8 // track in poster
         } TrackHeaderFlags;
        
        
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
         fixed8d8 volume;
         byte[2] reserved;
         fixed16d16 matrixA;
         fixed16d16 matrixB;
         fixed2d30 matrixU;
         fixed16d16 matrixC;
         fixed16d16 matrixD;
         fixed2d30 matrixV;
         fixed16d16 matrixX;
         fixed16d16 matrixY;
         fixed2d30 matrixW;
         fixed16d16 trackWidth;
         fixed16d16 trackHeight;
         } trackHeaderAtom;
         */
        DataAtomInputStream dain = new DataAtomInputStream(new ByteArrayInputStream(data));

        int version = dain.readByte();
        dain.skipBytes(2);
        // FIXME - trackHeaderFlag should be a parsed
        int trackHeaderFlags = dain.readUByte();
        Date creationTime = dain.readMacTimestamp();
        Date modificationTime = dain.readMacTimestamp();
        int trackId = dain.readInt();
        dain.skipBytes(4);
        long duration = dain.readUInt();
        dain.skipBytes(8);
        int layer = dain.readUShort();
        int alternateGroup = dain.readUShort();
        double volume = dain.readFixed8D8();
        dain.skipBytes(2);

        // {a, b, u,
        //  c, d, v,
        //  tx,ty,w} // X- and Y-Translation
        t.matrix[0] = dain.readFixed16D16();
        t.matrix[1] = dain.readFixed16D16();
        t.matrix[2] = dain.readFixed2D30();
        t.matrix[3] = dain.readFixed16D16();
        t.matrix[4] = dain.readFixed16D16();
        t.matrix[5] = dain.readFixed2D30();
        t.matrix[6] = dain.readFixed16D16();
        t.matrix[7] = dain.readFixed16D16();
        t.matrix[8] = dain.readFixed2D30();

        t.width = dain.readFixed16D16();
        t.height = dain.readFixed16D16();
    }

    /**
     * Parses a hdlr atom.
     */
    private HashMap<String, Object> parseHdlr(byte[] data) throws IOException {
        /*
         typedef struct {
         byte version;
         byte[3] flags;
         magic componentType;
         magic componentSubtype;
         magic componentManufacturer;
         int componentFlags;
         int componentFlagsMask;
         pstring componentName;
         ubyte[] extraData;
         } handlerReferenceAtom;
         */
        DataAtomInputStream i = new DataAtomInputStream(new ByteArrayInputStream(data));

        int version = i.readByte();
        int flags = (i.readUShort() << 8) | (i.readUByte());
        String componentType = i.readType();
        String componentSubtype = i.readType();
        String componentManufactureer = i.readType();
        int componentFlags = i.readInt();
        int componentFlagsMask = i.readInt();
        String componentName = i.readPString();

        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("componentSubtype", componentSubtype);
        return m;
    }

    /**
     * Parses an edts atom.
     */
    private void parseEdts(Track t, byte[] data) throws IOException {

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
        DataAtomInputStream dain = new DataAtomInputStream(new ByteArrayInputStream(data));

        int version = dain.readByte();
        int flags = (dain.readUShort() << 8) | (dain.readUByte());
        int numberOfEntries = dain.readInt();
        t.editList = new Edit[numberOfEntries];
        for (int i = 0; i < numberOfEntries; i++) {
            Edit edit = new Edit(dain.readInt(), dain.readInt(), dain.readFixed16D16());
            t.editList[i] = edit;
        }
    }

    /**
     * Parses a mdhd atom.
     */
    private void parseMdhd(Track t, byte[] data) throws IOException {
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

        DataAtomInputStream dain = new DataAtomInputStream(new ByteArrayInputStream(data));

        int version = dain.readByte();
        int flags = (dain.readUShort() << 8) | (dain.readUByte());
        Date creationTime = dain.readMacTimestamp();
        Date modificationTime = dain.readMacTimestamp();
        t.mediaTimeScale = dain.readUInt();
        t.mediaDuration = dain.readUInt();
        short language = dain.readShort();
        short quality = dain.readShort();

    }
}
