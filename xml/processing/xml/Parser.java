package processing.xml;

import processing.core.*;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;

/**
 * Part of the Mobile Processing project - http://mobile.processing.org
 *
 * Copyright (c) 2004-05 Francis Li
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 *
 * @author  Francis Li
 */
public class Parser implements Runnable {
    public static final int EVENT_TAG_START     = XmlPullParser.START_TAG;
    public static final int EVENT_TEXT          = XmlPullParser.TEXT;
    public static final int EVENT_TAG_END       = XmlPullParser.END_TAG;
    public static final int EVENT_DOCUMENT_END  = XmlPullParser.END_DOCUMENT;
    
    private PMIDlet     midlet;
    private KXmlParser  parser;
    private Thread      thread;
    
    public Parser(PMIDlet midlet) {
        this.midlet = midlet;
        parser = new KXmlParser();
    }
    
    public void start(InputStream is) {
        synchronized (this) {
            if (thread == null) {
                try {
                    parser.setInput(new InputStreamReader(is));
                    thread = new Thread(this);
                    thread.start();
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                throw new RuntimeException("Parser is already running");
            }
        }
    }
    
    public void start(String document) {
        start(new ByteArrayInputStream(document.getBytes()));
    }
    
    public void stop() {
        synchronized (this) {
            thread = null;
        }
    }
    
    public String attribute(String name) {
        if (parser != null) {
            return parser.getAttributeValue(null, name);
        } else {
            throw new RuntimeException("Parser not running");
        }
    }
    
    public void run() {
        Thread current = Thread.currentThread();
        while (thread == current) {
            try {
                int event = parser.next();
                String data = null;
                switch (event) {
                    case EVENT_TAG_START:
                    case EVENT_TAG_END:
                        data = parser.getName();
                        break;
                    case EVENT_TEXT:
                        data = parser.getText();
                        break;
                }
                synchronized (this) {
                    midlet.enqueueLibraryEvent(this, event, data);
                    wait();
                }
                if (event == EVENT_DOCUMENT_END) {
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
            Thread.yield();
        }
        synchronized (this) {
            if (thread == current) {
                thread = null;                
            }
        }
    }
}
