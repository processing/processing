/*
 * @(#)AbstractAudioCodec.java  1.0  2011-07-10
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */

package org.monte.media;

/**
 * {@code AbstractAudioCodec}.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-07-10 Created.
 */
public abstract class AbstractAudioCodec extends AbstractCodec {

    public AbstractAudioCodec(Format[] supportedInputFormats, Format[] supportedOutputFormats) {
        super(supportedInputFormats, supportedOutputFormats);
    }
    public AbstractAudioCodec(Format[] supportedInputOutputFormats) {
        super(supportedInputOutputFormats);
    }

}
