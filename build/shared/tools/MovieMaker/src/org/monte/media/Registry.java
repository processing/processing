/*
 * @(#)Registry.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance with the
 * license agreement you entered into with Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import java.io.File;
import java.util.ArrayList;
import static org.monte.media.FormatKeys.*;

/**
 * The {@code Registry} for audio and video codecs.
 *
 * @author Werner Randelshofer
 * @version $Id: Registry.java 299 2013-01-03 07:40:18Z werner $
 */
public abstract class Registry {

    private static Registry instance;

    public static Registry getInstance() {
        if (instance == null) {
            instance = new DefaultRegistry();
            instance.init();
        }
        return instance;
    }

    /**
     * Initializes the registry.
     */
    protected abstract void init();

    /**
     * Puts a codec into the registry.
     *
     * @param inputFormat The input format. Must not be null.
     * @param outputFormat The output format. Must not be null.
     * @param codecClass The codec class name. Must not be null.
     */
    public abstract void putCodec(Format inputFormat, Format outputFormat, String codecClass);

    /**
     * Gets all codecs which can decode the specified format.
     *
     * @param format The format.
     * @return An array of codec class names. If no codec was found, an empty
     * array is returned.
     */
    public final String[] getDecoderClasses(Format format) {
        return getCodecClasses(format, null);
    }

    /**
     * Gets all codecs which can decode the specified format.
     *
     * @param format The format.
     * @return An array of codec class names. If no codec was found, an empty
     * array is returned.
     */
    public final String[] getEncoderClasses(Format format) {
        return getCodecClasses(null, format);
    }

    /**
     * Gets all codecs which can transcode from the specified input format to
     * the specified output format.
     *
     * @param inputFormat The input format.
     * @param outputFormat The output format.
     * @return An array of codec class names. If no codec was found, an empty
     * array is returned.
     */
    public abstract String[] getCodecClasses(//
            Format inputFormat,
            Format outputFormat);

    /**
     * Gets all codecs which can decode the specified format.
     *
     * @param inputFormat The input format.
     * @return An array of codec class names. If no codec was found, an empty
     * array is returned.
     */
    public final Codec[] getDecoders(Format inputFormat) {
        return getCodecs(inputFormat, null);
    }

    /**
     * Gets the first codec which can decode the specified format.
     *
     * @param inputFormat The output format.
     * @return A codec. Returns null if no codec was found.
     */
    public Codec getDecoder(Format inputFormat) {
        return getCodec(inputFormat, null);
    }

    /**
     * Gets all codecs which can encode the specified format.
     *
     * @param outputFormat The output format.
     * @return An array of codecs. If no codec was found, an empty array is
     * returned.
     */
    public final Codec[] getEncoders(Format outputFormat) {
        return getCodecs(null, outputFormat);
    }

    /**
     * Gets the first codec which can encode the specified foramt.
     *
     * @param outputFormat The output format.
     * @return A codec. Returns null if no codec was found.
     */
    public Codec getEncoder(Format outputFormat) {
        return getCodec(null, outputFormat);
    }

    /**
     * Gets all codecs which can transcode from the specified input format to
     * the specified output format.
     *
     * @param inputFormat The input format.
     * @param outputFormat The output format.
     * @return An array of codec class names. If no codec was found, an empty
     * array is returned.
     */
    public Codec[] getCodecs(Format inputFormat, Format outputFormat) {
        String[] clazz = getCodecClasses(inputFormat, outputFormat);
        ArrayList<Codec> codecs = new ArrayList<Codec>(clazz.length);
        for (int i = 0; i < clazz.length; i++) {
            try {
                codecs.add((Codec) Class.forName(clazz[i]).newInstance());
            } catch (Exception ex) {
                //ex.printStackTrace();
                System.err.println("Monte Registry. Codec class not found: " + clazz[i]);
                unregisterCodec(clazz[i]);
            }
        }
        return codecs.toArray(new Codec[codecs.size()]);
    }

    /**
     * Gets a codec which can transcode from the specified input format to the
     * specified output format.
     *
     * @param inputFormat The input format.
     * @param outputFormat The output format.
     * @return A codec or null.
     */
    public Codec getCodec(Format inputFormat, Format outputFormat) {
        String[] clazz = getCodecClasses(inputFormat, outputFormat);
        for (int i = 0; i < clazz.length; i++) {
            try {
                Codec codec = ((Codec) Class.forName(clazz[i]).newInstance());
                codec.setInputFormat(inputFormat);
                if (outputFormat != null) {
                    codec.setOutputFormat(outputFormat);
                }
                return codec;
            } catch (Exception ex) {
                //ex.printStackTrace();
                System.err.println("Monte Registry. Codec class not found: " + clazz[i]);
                unregisterCodec(clazz[i]);
            }
        }
        return null;
    }

    /**
     * Puts a reader into the registry.
     *
     * @param fileFormat The file format, e.g."video/avi", "video/quicktime".
     * Use "Java" for formats which are not tied to a file format. Must not be
     * null.
     * @param readerClass The reader class name. Must not be null.
     */
    public abstract void putReader(Format fileFormat, String readerClass);

    /**
     * Puts a writer into the registry.
     *
     * @param fileFormat The file format, e.g."video/avi", "video/quicktime".
     * Use "Java" for formats which are not tied to a file format. Must not be
     * null.
     * @param writerClass The writer class name. Must not be null.
     */
    public abstract void putWriter(Format fileFormat, String writerClass);

    /**
     * Gets all reader class names from the registry for the specified file
     * format.
     *
     * @param fileFormat The file format, e.g."AVI", "QuickTime".
     * @return The reader class names.
     */
    public abstract String[] getReaderClasses(Format fileFormat);

    /**
     * Gets all writer class names from the registry for the specified file
     * format.
     *
     * @param fileFormat The file format, e.g."AVI", "QuickTime".
     * @return The writer class names.
     */
    public abstract String[] getWriterClasses(Format fileFormat);

    public MovieReader getReader(Format fileFormat, File file) {
        String[] clazz = getReaderClasses(fileFormat);
        for (int i = 0; i < clazz.length; i++) {
            try {
                return ((MovieReader) Class.forName(clazz[i]).getConstructor(File.class).newInstance(file));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public MovieWriter getWriter(File file) {
        Format format = getFileFormat(file);
        return format == null ? null : getWriter(format, file);
    }

    public MovieWriter getWriter(Format fileFormat, File file) {
        String[] clazz = getWriterClasses(fileFormat);
        for (int i = 0; i < clazz.length; i++) {
            try {
                return ((MovieWriter) Class.forName(clazz[i]).getConstructor(File.class).newInstance(file));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public MovieReader getReader(File file) {
        Format format = getFileFormat(file);
        return format == null ? null : getReader(format, file);
    }

    public abstract void putFileFormat(String extension, Format format);

    public abstract Format getFileFormat(File file);

    public abstract Format[] getReaderFormats();

    public abstract Format[] getWriterFormats();

    public abstract Format[] getFileFormats();

    public abstract String getExtension(Format ff);

    /**
     * Suggests output formats for the given input media format and specified
     * file format.
     *
     * @param inputMediaFormat
     * @param outputFileFormat
     * @return List of output media formats.
     */
    public ArrayList<Format> suggestOutputFormats(Format inputMediaFormat, Format outputFileFormat) {
        ArrayList<Format> formats = new ArrayList<Format>();
        Format matchFormat = new Format(//
                MimeTypeKey, outputFileFormat.get(MimeTypeKey),//
                MediaTypeKey, inputMediaFormat.get(MediaTypeKey));
        Codec[] codecs = getEncoders(matchFormat);
        int matchingCount = 0;
        for (Codec c : codecs) {
            for (Format mf : c.getOutputFormats(null)) {
                if (mf.matches(matchFormat)) {
                    if (inputMediaFormat.matchesWithout(mf, MimeTypeKey)) {
                        // add matching formats first
                        formats.add(0, mf.append(inputMediaFormat));
                        matchingCount++;
                    } else if (inputMediaFormat.matchesWithout(mf, MimeTypeKey, EncodingKey)) {
                        // add formats which match everything but the encoding second
                        formats.add(matchingCount, mf.append(inputMediaFormat));
                    } else {
                        // add remaining formats last
                        formats.add(mf.append(inputMediaFormat));
                    }
                }
            }
        }

        // remove duplicates
        for (int i = formats.size() - 1; i >= 0; i--) {
            Format fi = formats.get(i);
            for (int j = i - 1; j >= 0; j--) {
                Format fj = formats.get(j);
                if (fi.matches(fj)) {
                    formats.remove(i);
                    break;
                }
            }
        }

        return formats;
    }

    public abstract void unregisterCodec(String codecClass);
}
