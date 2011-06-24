/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 Part of the Processing project - http://processing.org

 Copyright (c) 2004-11 Ben Fry and Casey Reas
 Copyright (c) 2001-04 Massachusetts Institute of Technology

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package processing.app;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class LibraryListing {

  Map<String, List<LibraryInfo>> librariesByCategory;

  public LibraryListing(File xmlFile) {

    librariesByCategory = new HashMap<String, List<LibraryInfo>>();

    new LibraryXmlParser(xmlFile);
  }

  public Set<String> getCategories() {
    return librariesByCategory.keySet();
  }

  public List<LibraryInfo> getAllLibararies() {
    ArrayList<LibraryInfo> allLibs = new ArrayList<LibraryInfo>();
    for (Entry<String, List<LibraryInfo>> libListEntry : librariesByCategory
        .entrySet()) {
      allLibs.addAll(libListEntry.getValue());
    }
    return allLibs;
  }

  public List<LibraryInfo> getLibararies(String category) {
    return librariesByCategory.get(category);
  }

  public static class LibraryListFetcher {

    LibraryListing libListing;

    File dest;
    
    URL url;
    
    FileDownloader downloader;
    
    Thread downloaderThread;
    

    public LibraryListFetcher() {
      libListing = null;
      try {
        File tmpFolder = Base.createTempFolder("libarylist", "download");

        dest = new File(tmpFolder, "libraries.xml");
        dest.setWritable(true);

        url = new URL("http://dl.dropbox.com/u/700641/libraries.xml");

      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    public void fetchLibraryList(ProgressMonitor pm) {
      downloader = new FileDownloader(url, dest, pm);
      downloader.setPostOperation(new Runnable() {
        
        public void run() {
          File xmlFile = downloader.getFile();
          if (xmlFile != null) {
            libListing = new LibraryListing(xmlFile);
          }
        }
      });
      
      downloaderThread = new Thread(downloader);
      downloaderThread.start();
    }
    
    public boolean isDone() {
      return !downloaderThread.isAlive();
    }
    
    public LibraryListing getLibraryListing() {
      return libListing;
    }
  }

  public static class LibraryInfo {
    public final String categoryName;
    
    public final String name;
    public final String url;
    public final String description;

    public final ArrayList<Author> authors;

    public final String versionId;
    public final String link;

    final boolean isInstalled;

    public LibraryInfo(String categoryName, String name, String url,
                       String description, ArrayList<Author> authors,
                       String versionId, String link) {
      this.categoryName = categoryName;
      this.name = name;
      this.url = url;
      this.description = description;
      this.authors = authors;
      this.versionId = versionId;
      this.link = link;

      isInstalled = false;
    }

    public static class Author {
      public final String name;

      public final String url;

      public Author(String name, String url) {
        this.name = name;
        this.url = url;
      }

    }

  }

  /**
   * Class to parse the libraries xml file
   */
  class LibraryXmlParser extends DefaultHandler {
    String categoryName;

    String libraryName;

    String libraryUrl;

    String libraryVersionId;

    String libraryLink;

    String libraryDescription;

    boolean doingDescription;

    ArrayList<LibraryInfo.Author> authors;

    String authorUrl;

    boolean doingAuthor;

    LibraryXmlParser(File xmlFile) {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);

      try {
        SAXParser sp = spf.newSAXParser();

        InputSource input = new InputSource(new FileReader(xmlFile));

        sp.parse(input, this);

        // XXX: Do something meaningful when we get an error
      } catch (SAXException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      }
    }

    private void reset() {
      libraryName = null;
      libraryUrl = null;
      libraryVersionId = null;
      libraryLink = null;

      libraryDescription = null;
      doingDescription = false;

      authors = new ArrayList<LibraryInfo.Author>();
      authorUrl = null;
      doingAuthor = false;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

      if ("category".equals(qName)) {
        categoryName = attributes.getValue("name");

      } else if ("library".equals(qName)) {
        reset();
        libraryName = attributes.getValue("name");
        libraryUrl = attributes.getValue("url");

      } else if ("author".equals(qName)) {
        authorUrl = attributes.getValue("url");
        doingAuthor = true;

      } else if ("description".equals(qName)) {
        doingDescription = true;

      } else if ("version".equals(qName)) {
        libraryVersionId = attributes.getValue("id");

      } else if ("location".equals(qName)) {
        libraryLink = attributes.getValue("url");
      }
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {

      if (doingAuthor) {
        String authorName = new String(ch, start, length).trim();
        authors.add(new LibraryInfo.Author(authorName, authorUrl));
        authorUrl = null;
        doingAuthor = false;
      } else if (doingDescription) {
        libraryDescription = new String(ch, start, length).trim();
        doingDescription = false;
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {

      if ("library".equals(qName)) {
        // Dump the information we collected and reset the variables

        LibraryInfo libInfo = new LibraryInfo(categoryName, libraryName,
                                              libraryUrl, libraryDescription,
                                              authors, libraryVersionId,
                                              libraryLink);

        if (librariesByCategory.containsKey(categoryName)) {
          librariesByCategory.get(categoryName).add(libInfo);
        } else {
          ArrayList<LibraryInfo> libs = new ArrayList<LibraryInfo>();
          libs.add(libInfo);
          librariesByCategory.put(categoryName, libs);
        }

        reset();
      }

    }

    @Override
    public void warning(SAXParseException exception) {
      System.err.println("WARNING: line " + exception.getLineNumber() + ": "
          + exception.getMessage());
    }

    @Override
    public void error(SAXParseException exception) {
      System.err.println("ERROR: line " + exception.getLineNumber() + ": "
          + exception.getMessage());
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      System.err.println("FATAL: line " + exception.getLineNumber() + ": "
          + exception.getMessage());
      throw (exception);
    }
  }

}
