/*
 * @(#)DefaultRegistry.java  
 * 
 * Copyright (c) 2011 Werner Randelshofer, Goldau, Switzerland.
 * All rights reserved.
 * 
 * You may not use, copy or modify this file, except in compliance onlyWith the
 * license agreement you entered into onlyWith Werner Randelshofer.
 * For details see accompanying license terms.
 */
package org.monte.media;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import static org.monte.media.VideoFormatKeys.*;
import static org.monte.media.AudioFormatKeys.*;

/**
 * {@code DefaultRegistry}. 
 * <p>
 * FIXME - The registry should be read from a file. 
 *
 * @author Werner Randelshofer
 * @version $Id: DefaultRegistry.java 299 2013-01-03 07:40:18Z werner $
 */
public class DefaultRegistry extends Registry {

    private HashMap<String, LinkedList<RegistryEntry>> codecMap;
    private HashMap<String, LinkedList<RegistryEntry>> readerMap;
    private HashMap<String, LinkedList<RegistryEntry>> writerMap;
    private HashMap<String, Format> fileFormatMap;

    @Override
    public Format[] getReaderFormats() {
        return getFileFormats();
    }

    @Override
    public Format[] getWriterFormats() {
        return getFileFormats();
    }

    @Override
    public Format[] getFileFormats() {
        return fileFormatMap.values().toArray(new Format[fileFormatMap.size()]);
    }

    private static class RegistryEntry {

        Format inputFormat;
        Format outputFormat;
        String className;

        public RegistryEntry(Format inputFormat, Format outputFormat, String className) {
            this.inputFormat = inputFormat;
            this.outputFormat = outputFormat;
            this.className = className;
        }
    }

    public DefaultRegistry() {
    }

    @Override
    protected void init() {
        codecMap = new HashMap<String, LinkedList<RegistryEntry>>();
        readerMap = new HashMap<String, LinkedList<RegistryEntry>>();
        writerMap = new HashMap<String, LinkedList<RegistryEntry>>();
        fileFormatMap = new HashMap<String, Format>();

        // IFF ANIM
        // --------
        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_ANIM, EncodingKey, ENCODING_BITMAP_IMAGE),
                "org.monte.media.anim.BitmapCodec");

        // AVI
        // --------
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_DIB),
                "org.monte.media.avi.DIBCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_MJPG),
                "org.monte.media.jpeg.JPEGCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_PNG),
                "org.monte.media.png.PNGCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_RLE),
                "org.monte.media.avi.RunLengthCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                "org.monte.media.avi.TechSmithCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_DOSBOX_SCREEN_CAPTURE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                "org.monte.media.avi.ZMBVCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_PCM),
                "org.monte.media.avi.AVIPCMAudioCodec");
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_PCM),
                "org.monte.media.avi.AVIPCMAudioCodec");

        // QuickTime
        // --------
        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_RAW),
                "org.monte.media.quicktime.RawCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_ANIMATION),
                "org.monte.media.quicktime.AnimationCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_JPEG),
                "org.monte.media.jpeg.JPEGCodec");
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_AVI, EncodingKey, ENCODING_AVI_MJPG),
                "org.monte.media.jpeg.JPEGCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_PNG),
                "org.monte.media.png.PNGCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_QUICKTIME, 
                EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE, CompressorNameKey, COMPRESSOR_NAME_AVI_TECHSMITH_SCREEN_CAPTURE),
                new Format(MediaTypeKey, MediaType.VIDEO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_BUFFERED_IMAGE),
                "org.monte.media.avi.TechSmithCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_TWOS_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");
        putCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_TWOS_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");

        putCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_SOWT_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");
        putCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_SOWT_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_IN24_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_IN24_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_IN32_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_IN32_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");

        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_SIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_RAW_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");
        putBidiCodec(
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_JAVA, EncodingKey, ENCODING_PCM_UNSIGNED),
                new Format(MediaTypeKey, MediaType.AUDIO, MimeTypeKey, MIME_QUICKTIME, EncodingKey, ENCODING_QUICKTIME_RAW_PCM),
                "org.monte.media.quicktime.QuickTimePCMAudioCodec");


        putReader(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI), "org.monte.media.avi.AVIReader");
        putReader(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_QUICKTIME), "org.monte.media.quicktime.QuickTimeReader");
        putReader(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_ANIM), "org.monte.media.anim.ANIMReader");
        putWriter(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI), "org.monte.media.avi.AVIWriter");
        putWriter(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_QUICKTIME), "org.monte.media.quicktime.QuickTimeWriter");
        putWriter(new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_ANIM), "org.monte.media.anim.ANIMWriter");

        putFileFormat("avi", new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI));
        putFileFormat("mov", new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_QUICKTIME));
        putFileFormat("anim", new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_ANIM));
    }

    /**
     * 
     * @param inputFormat Must have {@code MediaTypeKey}, {@code EncodingKey}, {@code MimeTypeKey}.
     * @param outputFormat Must have {@code MediaTypeKey}, {@code EncodingKey}, {@code MimeTypeKey}.
     * @param codecClass 
     */
    public void putBidiCodec(Format inputFormat, Format outputFormat, String codecClass) {
        putCodec(inputFormat, outputFormat, codecClass);
        putCodec(outputFormat, inputFormat, codecClass);
    }

    /**
     * 
     * @param inputFormat Must have {@code MediaTypeKey}, {@code EncodingKey}, {@code MimeTypeKey}.
     * @param outputFormat  Must have {@code MediaTypeKey}, {@code EncodingKey}, {@code MimeTypeKey}.
     * @param codecClass 
     */
    @Override
    public void putCodec(Format inputFormat, Format outputFormat, String codecClass) {
        RegistryEntry entry = new RegistryEntry(inputFormat, outputFormat, codecClass);
        addCodecEntry(inputFormat.get(EncodingKey), entry);
        addCodecEntry(outputFormat.get(EncodingKey), entry);
    }

    private void addCodecEntry(String key, RegistryEntry entry) {
        LinkedList<RegistryEntry> list = codecMap.get(key);
        if (list == null) {
            list = new LinkedList<RegistryEntry>();
            codecMap.put(key, list);
        }
        list.add(entry);
    }

    /**
     * 
     * @param fileFormat Must have {@code MediaTypeKey}, {@code MimeTypeKey}.
     * @param readerClass 
     */
    @Override
    public void putReader(Format fileFormat, String readerClass) {
        RegistryEntry entry = new RegistryEntry(null, fileFormat, readerClass);
        String key = fileFormat.get(MimeTypeKey);
        LinkedList<RegistryEntry> list = readerMap.get(key);
        if (list == null) {
            list = new LinkedList<RegistryEntry>();
            readerMap.put(key, list);
        }
        list.add(entry);
    }

    /**
     * 
     * @param fileFormat Must have {@code MediaTypeKey}, {@code MimeTypeKey}.
     * @param writerClass 
     */
    @Override
    public void putWriter(Format fileFormat, String writerClass) {
        RegistryEntry entry = new RegistryEntry(fileFormat, null, writerClass);
        String key = fileFormat.get(MimeTypeKey);
        LinkedList<RegistryEntry> list = writerMap.get(key);
        if (list == null) {
            list = new LinkedList<RegistryEntry>();
            writerMap.put(key, list);
        }
        list.add(entry);
    }

    @Override
    public String[] getCodecClasses(Format inputFormat, Format outputFormat) {
        HashSet<String> classNames = new HashSet<String>();
        HashSet<RegistryEntry> entries = new HashSet<RegistryEntry>();
        if (inputFormat != null) {
            LinkedList<RegistryEntry> re;
            if (inputFormat.get(EncodingKey) == null) {
                re = new LinkedList<RegistryEntry>();
                for (Map.Entry<String, LinkedList<RegistryEntry>> i : codecMap.entrySet()) {
                    for (RegistryEntry j : i.getValue()) {
                        if (inputFormat.matches(j.inputFormat)) {
                            re.add(j);
                        }
                    }
                }
            } else {
                re = codecMap.get(inputFormat.get(EncodingKey));
            }
            if (re != null) {
                entries.addAll(re);
            }
        }
        if (outputFormat != null) {
            LinkedList<RegistryEntry> re;
            if (outputFormat.get(EncodingKey) == null) {
                re = new LinkedList<RegistryEntry>();
                for (Map.Entry<String, LinkedList<RegistryEntry>> i : codecMap.entrySet()) {
                    for (RegistryEntry j : i.getValue()) {
                        if (outputFormat.matches(j.outputFormat)) {
                            re.add(j);
                        }
                    }
                }
            } else {
                re = codecMap.get(outputFormat.get(EncodingKey));
            }
            if (re != null) {
                entries.addAll(re);
            }
        }
        for (RegistryEntry e : entries) {
            if ((inputFormat == null || e.inputFormat == null || inputFormat.matches(e.inputFormat))
                    && (outputFormat == null || e.outputFormat == null || outputFormat.matches(e.outputFormat))) {
                classNames.add(e.className);
            }
        }
        return classNames.toArray(new String[classNames.size()]);
    }

    @Override
    public String[] getReaderClasses(Format fileFormat) {
        LinkedList<RegistryEntry> rr = readerMap.get(fileFormat.get(MimeTypeKey));
        String[] names = new String[rr == null ? 0 : rr.size()];
        if (rr != null) {
            int i = 0;
            for (RegistryEntry e : rr) {
                names[i++] = e.className;
            }
        }
        return names;
    }

    @Override
    public Format getFileFormat(File file) {
        String ext = file.getName();
        int p = ext.lastIndexOf('.');
        if (p != -1) {
            ext = ext.substring(p + 1);
        }
        ext = ext.toLowerCase();
        return fileFormatMap.get(ext);
    }

    @Override
    public String[] getWriterClasses(Format fileFormat) {
        LinkedList<RegistryEntry> rr = writerMap.get(fileFormat.get(MimeTypeKey));
        String[] names = new String[rr == null ? 0 : rr.size()];
        if (rr != null) {
            int i = 0;
            for (RegistryEntry e : rr) {
                names[i++] = e.className;
            }
        }
        return names;
    }

    @Override
    public void putFileFormat(String extension, Format format) {
        fileFormatMap.put(extension.toLowerCase(), format);
    }

    @Override
    public String getExtension(Format ff) {
        for (Map.Entry<String, Format> e : fileFormatMap.entrySet()) {
            if (e.getValue().get(MimeTypeKey).equals(ff.get(MimeTypeKey))) {
                return e.getKey();
            }
        }
        return "";
    }

    @Override
    public void unregisterCodec(String codecClass) {
        for (Map.Entry<String, LinkedList<RegistryEntry>> i:codecMap.entrySet()) {
            LinkedList<RegistryEntry> ll=i.getValue();
            for (Iterator<RegistryEntry> j=ll.iterator();j.hasNext();) {
                RegistryEntry e=j.next();
                if (e.className.equals(codecClass)) {
                    j.remove();
                }
            }
        }
    }
    
    
}
