/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.monte.media;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.monte.media.math.Rational;
import org.monte.media.util.Methods;

/**
 * A {@code Buffer} carries media data from one media processing unit to another.
 *
 * @author Werner Randelshofer
 * @version 1.0 2011-03-12 Created.
 */
public class Buffer {

    /** A flag mask that describes the boolean attributes for this buffer.
     */
    public EnumSet<BufferFlag> flags = EnumSet.noneOf(BufferFlag.class);
    /** Values which are not specified must have this value. */
    public static final int NOT_SPECIFIED = -1;
    /** The track number. 
     * This can be set to NOT_SPECIFIED or to a number &gt;= 0.
     */
    public int track;
    /** Header information, such as RTP header for this chunk. */
    public Object header;
    /** The media data. */
    public Object data;
    /** The data offset. This field is only used if {@code data} is an array. */
    public int offset;
    /** The data length. This field is only used if {@code data} is an array. */
    public int length;
    /** Duration of a sample in seconds.
     * Multiply this with {@code sampleCount} to get the buffer duration. 
     */
    public Rational sampleDuration;
    /** The time stamp of this buffer in seconds. */
    public Rational timeStamp;
    /** The format of the data in this buffer. */
    public Format format;
    /** The number of samples in the data field. */
    public int sampleCount = 1;
    
    /** Sequence number of the buffer. This can be used for debugging. */
    public long sequenceNumber;

    /** Sets all variables of this buffer to that buffer except for {@code data},
     * {@code offset}, {@code length} and {@code header}.
     */
    public void setMetaTo(Buffer that) {
        this.flags = EnumSet.copyOf(that.flags);
        //this.data=that.data;
        //this.offset=that.offset;
        //this.length=that.length;
        //this.header=that.header;
        this.track = that.track;
        this.sampleDuration = that.sampleDuration;
        this.timeStamp = that.timeStamp;
        this.format = that.format;
        this.sampleCount = that.sampleCount;
        this.format = that.format;
        this.sequenceNumber=that.sequenceNumber;
    }

    /** Sets {@code data}, {@code offset}, {@code length} and {@code header}
     * of this buffer to that buffer.
     * Note that this method creates copies of the {@code data} and 
     * {@code header}, so that these fields in that buffer can be discarded
     * without affecting the contents of this buffer.
     * <p>
     * FIXME - This method does not always create a copy!!
     */
    public void setDataTo(Buffer that) {
        this.offset = that.offset;
        this.length = that.length;
        this.data = copy(that.data, this.data);
        this.header = copy(that.header, this.header);

    }

    private Object copy(Object from, Object into) {
        if (from instanceof byte[]) {
            byte[] b=(byte[])from;
            if (!(into instanceof byte[]) || ((byte[]) into).length < b.length) {
                into = new byte[b.length];
            }
            System.arraycopy(b, 0, (byte[])into, 0, b.length);
        } else if (from instanceof BufferedImage) {
            // FIXME - Try to reuse BufferedImage in output!
            BufferedImage img = (BufferedImage) from;
            ColorModel cm = img.getColorModel();
            boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
            WritableRaster raster = img.copyData(null);
            into = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        } else if (from instanceof Cloneable) {
            try {
                into=Methods.invoke(from, "clone");
            } catch (NoSuchMethodException ex) {
                into=from;
            }
        } else {
            // FIXME - This is very fragile, since we do not know, if the
            //         input data stays valid until the output data is processed!
            into = from;
        }
        
        return into;
    }
    
    /** Returns true if the specified flag is set. */
    public boolean isFlag(BufferFlag flag) {
        return flags.contains(flag);
    }

    /** Convenience method for setting a flag. */
    public void setFlag(BufferFlag flag) {
        setFlag(flag, true);
    }

    /** Convenience method for clearing a flag. */
    public void clearFlag(BufferFlag flag) {
        setFlag(flag, false);
    }

    /** Sets or clears the specified flag. */
    public void setFlag(BufferFlag flag, boolean value) {
        if (value) {
            flags.add(flag);
        } else {
            flags.remove(flag);
        }
    }

    /** Clears all flags, and then sets the specified flag. */
    public void setFlagsTo(BufferFlag... flags) {
        if (flags.length == 0) {
            this.flags = EnumSet.noneOf(BufferFlag.class);
        } else {
            this.flags = EnumSet.copyOf(Arrays.asList(flags));
        }
    }

    /** Clears all flags, and then sets the specified flag. */
    public void setFlagsTo(EnumSet<BufferFlag> flags) {
        if (flags == null) {
            this.flags = EnumSet.noneOf(BufferFlag.class);
        } else {
            this.flags = EnumSet.copyOf(flags);
        }
    }

    public void clearFlags() {
        flags.clear();
    }
}
