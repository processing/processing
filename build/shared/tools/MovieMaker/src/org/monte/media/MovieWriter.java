/*
 * @(#)MovieWriter.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import org.monte.media.math.Rational;
import java.io.IOException;

/**
 * A simple API for writing movie data (audio and video) into a file.
 *
 * @author Werner Randelshofer
 * @version $Id: MovieWriter.java 299 2013-01-03 07:40:18Z werner $
 */
public interface MovieWriter extends Multiplexer {
    /** Returns the file format. */
    public Format getFileFormat() throws IOException;

    /** Adds a track to the writer for a suggested input format.
     * <p>
     * The format should at least specify the desired {@link FormatKeys.MediaType}.
     * The actual input format is a refined version of the suggested format. For
     * example, if a MovieWriter only supports fixed frame rate video, then the
     * MovieWriter will extend the format with that information.
     * <p>
     * If the suggested input format is not compatible, then an IOException is
     * thrown. For example, if a MovieWriter only supports fixed frame rate video,
     * but a format with variable frame rate was requested.
     * 
     * @param format The desired input format of the track. The actual input
     * format may be a refined version of the specified format.
     * @return The track number.
     */
    public int addTrack(Format format) throws IOException;
    
    /** Returns the media format of the specified track.
     * This is a refined version of the format that was requested when the
     * track was added. See {@link #addTrack}. 
     * 
     * @param track Track number.
     * @return The media format of the track.
     */
    public Format getFormat(int track);
    
    /** Returns the number of tracks. */
    public int getTrackCount();

    /** Writes a sample into the specified track.
     * Does nothing if the discard-flag in the buffer is set to true.
     *
     * @param track The track number.
     * @param buf The buffer containing the sample data.
     */
    @Override
    public void write(int track, Buffer buf) throws IOException;

    /** Closes the writer. */
    @Override
    public void close() throws IOException;

    /** Returns true if the limit for media data has been reached.
     * If this limit is reached, no more samples should be added to the movie.
     * <p>
     * This limit is imposed by data structures of the movie file
     * which will overflow if more samples are added to the movie.
     * <p>
     * FIXME - Maybe replace by getCapacity():long. 
     */
    public boolean isDataLimitReached();

    /** Returns the duration of the track in seconds. */
    public Rational getDuration(int track);
    /** Returns true if the specified track has no samples. */
    public boolean isEmpty(int track);
}
