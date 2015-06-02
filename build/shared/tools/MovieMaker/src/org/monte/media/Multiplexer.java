/*
 * @(#)Multiplexer.java  1.0  2011-02-19
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package org.monte.media;

import java.io.IOException;

/**
 * A {@code Multiplexer} can write multiple media tracks into a
 * single output stream.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-02-19 Created.
 */
public interface Multiplexer {
    /** Writes a sample.
     * Does nothing if the discard-flag in the buffer is set to true.
     *
     * @param track The track number.
     * @param buf The buffer containing the sample data.
     */
    public void write(int track, Buffer buf) throws IOException;

    /** Closes the Multiplexer. */
    public void close() throws IOException;
}
