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

import processing.app.LibraryListing.LibraryInfo.Author;

public class LibraryListing {
  
  Map<String, List<LibraryInfo>> librariesByCategory;
  ArrayList<LibraryInfo> allLibraries;
  
  public LibraryListing(File xmlFile) {

    librariesByCategory = new HashMap<String, List<LibraryInfo>>();

    new LibraryXmlParser(xmlFile);
    
    allLibraries = new ArrayList<LibraryInfo>();
    for (Entry<String, List<LibraryInfo>> libListEntry : librariesByCategory
        .entrySet()) {
      allLibraries.addAll(libListEntry.getValue());
    }
    
    Collections.sort(allLibraries);
  }

  public Set<String> getCategories() {
    return librariesByCategory.keySet();
  }

  public List<LibraryInfo> getAllLibararies() {
    return new ArrayList<LibraryInfo>(allLibraries);
  }

  public List<LibraryInfo> getLibararies(String category) {
    ArrayList<LibraryInfo> libinfos = new ArrayList<LibraryInfo>(librariesByCategory.get(category));
    Collections.sort(libinfos);
    return libinfos;
  }
  
  public List<LibraryInfo> getFilteredLibraryList(String category, List<String> filters) {
    ArrayList<LibraryInfo> filteredList = new ArrayList<LibraryInfo>(allLibraries);
    
    Iterator<LibraryInfo> it = filteredList.iterator();
    while (it.hasNext()) {
      LibraryInfo libInfo = it.next();
      
      if (category != null && !category.equals(libInfo.categoryName)) {
        it.remove();
      } else {
        for (String filter : filters) {
          if (!matches(libInfo, filter)) {
            it.remove();
            break;
          }
        }
      }
      
    }
    
    return filteredList;
  }

  private boolean matches(LibraryInfo libInfo, String filter) {
    filter = ".*" + filter.toLowerCase() + ".*";
    
    if (filter.isEmpty()) {
      return true;
    }
    
    for (Author author : libInfo.authors) {
      if (author.name.toLowerCase().matches(filter)) {
        return true;
      }
    }
    
    return libInfo.description.toLowerCase().matches(filter)
        || libInfo.categoryName.toLowerCase().matches(filter)
        || libInfo.name.toLowerCase().matches(filter);
 
  }

  public static class LibraryListFetcher implements Runnable {

    LibraryListing libListing;

    File dest;
    
    URL url;
    
    FileDownloader downloader;
    
    Thread downloaderThread;
    
    ProgressMonitor progressMonitor;

    public LibraryListFetcher() {
      
      progressMonitor = new NullProgressMonitor();
      
      libListing = null;
      try {
        File tmpFolder = Base.createTempFolder("libarylist", "download");

        dest = new File(tmpFolder, "libraries.xml");
        dest.setWritable(true);

        url = new URL("http://dl.dropbox.com/u/700641/libraries.xml");

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    public void setProgressMonitor(ProgressMonitor pm) {
      progressMonitor = pm;
    }

    public void run() {
      downloader = new FileDownloader(url, dest, progressMonitor);
      downloader.setPostOperation(new Runnable() {
        
        public void run() {
          File xmlFile = downloader.getFile();
          if (xmlFile != null) {
            libListing = new LibraryListing(xmlFile);
          }
        }
      });
      
      downloader.run();
    }
    
    public LibraryListing getLibraryListing() {
      return libListing;
    }
  }

  public static class LibraryInfo implements Comparable<LibraryInfo> {
    public String categoryName;
    
    public String name;
    public String url;
    public String description;

    public ArrayList<Author> authors;

    public String versionId;
    public String link;

    boolean isInstalled = false;

    public String brief;

    public LibraryInfo() {
      authors = new ArrayList<Author>();
    }

    public static class Author {
      public String name;

      public String url;

    }

    public int compareTo(LibraryInfo o) {
      return name.toLowerCase().compareTo(o.name.toLowerCase());
    }

  }

  /**
   * Class to parse the libraries xml file
   */
  class LibraryXmlParser extends DefaultHandler {
    String currentCategoryName;

    LibraryInfo currentLibInfo;

    boolean doingDescription = false;
    
    Author currentAuthor;

    boolean doingAuthor = false;

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

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

      if ("category".equals(qName)) {
        currentCategoryName = attributes.getValue("name");

      } else if ("library".equals(qName)) {
        currentLibInfo = new LibraryInfo();
        currentLibInfo.categoryName = currentCategoryName;
        currentLibInfo.name = attributes.getValue("name");
        currentLibInfo.url = attributes.getValue("url");

      } else if ("author".equals(qName)) {
        currentAuthor = new Author();
        currentAuthor.url = attributes.getValue("url");
        doingAuthor = true;

      } else if ("description".equals(qName)) {
        currentLibInfo.brief = attributes.getValue("brief");
        doingDescription = true;

      } else if ("version".equals(qName)) {
        currentLibInfo.versionId = attributes.getValue("id"); 

      } else if ("location".equals(qName)) {
        currentLibInfo.link = attributes.getValue("url");

      }
    }

    @Override
    public void characters(char[] ch, int start, int length)
        throws SAXException {

      if (doingAuthor) {
        currentAuthor.name = new String(ch, start, length).trim();
        currentLibInfo.authors.add(currentAuthor);
        currentAuthor = null;
        doingAuthor = false;

      } else if (doingDescription) {
        String str = new String(ch, start, length);
        if (currentLibInfo.description == null) {
          currentLibInfo.description = str;
        } else {
          currentLibInfo.description += str;
        }
        
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {

      if ("library".equals(qName)) {
        if (librariesByCategory.containsKey(currentCategoryName)) {
          librariesByCategory.get(currentCategoryName).add(currentLibInfo);
        } else {
          ArrayList<LibraryInfo> libs = new ArrayList<LibraryInfo>();
          libs.add(currentLibInfo);
          librariesByCategory.put(currentCategoryName, libs);
        }

        currentLibInfo = null;
      } else if ("description".equals(qName)) {
        currentLibInfo.description = currentLibInfo.description.trim();
        doingDescription = false;
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
