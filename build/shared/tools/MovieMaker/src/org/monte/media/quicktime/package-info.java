/* @(#)package-info.java
 *
 * Copyright (c) 2002-2012 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 *
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer. 
 * For details see accompanying license terms.
 */
/**
 * Provides media handlers for the QuickTime file format.
 *
 * <h2>Overview of the QuickTime movie file format</h2>
 * <p>
 * A QuickTime movie has a time dimension defined by a time scale and a duration.
 * A movie always starts at time 0. The time scale defines the unit of measure
 * for the movie's time value. The duration specifies how long the movie lasts.
 * </p>
 * <pre>
 * Movie time:      0   1   2   3   4   5   6   7    ...   20
 *                  +---+---+---+---+---+---+---+---+---+---+
 * Movie time unit:          --&gt;|   |&lt;--                      For example: 1/60 sec.
 * Movie duration:  &lt;---------------------------------------&gt; For example: 20 time units.
 * </pre>
 * <p>
 * A movie can contain one or more tracks. Each track refers to a media that
 * can be interpreted within the movie's time coordinate system. Each track
 * begins at the beginning of the movie. However, a track can end at any time.
 * In addition the media in the track may be offset from the beginning
 * of the movie. Tracks with media that does not commence at the beginning
 * of a movie start with an empty edit entry.
 * </p>
 * <pre>
 * Movie time:                0   1   2   3   4   5   6   7    ...   20
 *                            +---+---+---+---+---+---+---+---+---+---+
 * Track 1 (movie video):     XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 * Track 2 (movie audio):     XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 * Track 3 (preview audio):   ----XXXXXXXXXXXXX
 * Track 4 (poster graphics): ----X
 *                        --&gt;|   |&lt;-- track start time offset
 * </pre>
 * <p>
 * A track is always associated with one media. The media contains control
 * information that refers to the data that constitutes the track. Each media
 * has its own time coordinate system, which defines the media's time scale
 * and duration. A media's time coordinate system always starts at time 0, and
 * it is independend of the time coordinate system of the movie that uses its
 * data.
 * </p>
 * <pre>
 * Media 1 (movie video):     0 1 2 3 4 5 6 7                  ...   40
 *                            +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *                            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 *
 * Media 2 (movie audio):     0  1  2  3  4  5  6  7           ...   23
 *                            +--+--+--+--+--+--+--+--+--+--+--+--+--+-
 *                            XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
 *
 * Media 3 (preview audio):   0  1  2
 *                            +--+--+
 *                            XXXXXXX
 *
 * Media 4 (poster graphics): 0
 *                            +
 *                            X
 * </pre>
 * <p>
 * The track contains a list of references that identify portions of the media
 * that are used in the track. In essence, this is an edit list of the media.
 * Consequently a track can play the data in its media in any order and any
 * number of times.
 * </p>
 * <pre>
 * Edit List 3 (preview audio):   0  1  2  3  4
 *                                +--+--+--+--+
 *                                ---abcccafgab
 *
 * Media 3 (preview audio):       0  1  2
 *                                +--+--+
 *                                abcdefg
 * </pre>
 * <p>
 * A media describes the data for a track. The data is not actually stored in
 * the media. Rather, the media contains references to its data. The data
 * may reside in the movie file or in an external storage. The data referred
 * to by the media may be used by more than one movie, though the media itself
 * is not reused. If the data is stored in the movie, the samples of the
 * media data can be interleaved with each other. Interleaving can occur
 * after each sample, or after a chunk of samples.
 * </p>
 * <pre>
 * Media 1 to 4 interleaved data:  1112221112221112221113332224
 * </pre>
 * <p>
 * For more information about the QuickTime file format see the
 * "QuickTime File Format Specification", Apple Inc. 2010-08-03. (qtff)
 * <a href="http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf/">
 * http://developer.apple.com/library/mac/documentation/QuickTime/QTFF/qtff.pdf
 * </a>
 * </p>
 *
 * @author Werner Randelshofer
 * @version $Id: package-info.java 299 2013-01-03 07:40:18Z werner $
 */
package org.monte.media.quicktime;
