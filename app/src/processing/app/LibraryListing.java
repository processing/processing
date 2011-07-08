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

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import processing.app.Library.LibraryInfo;
import processing.app.Library.LibraryInfo.Author;

public class LibraryListing {
  
  Map<String, List<LibraryInfo>> librariesByCategory;
  
  ArrayList<LibraryInfo> allLibraries;
  
  boolean hasDownloadedList;
  
  
  public LibraryListing() {
    
    librariesByCategory = new HashMap<String, List<LibraryInfo>>();
    allLibraries = new ArrayList<LibraryInfo>();
    hasDownloadedList = false;
  }


  public void rebuildList(File xmlFile) {
    
    librariesByCategory = new HashMap<String, List<LibraryInfo>>();
    allLibraries = new ArrayList<LibraryInfo>();
    hasDownloadedList = true;
    
    new LibraryXmlParser(xmlFile);
    
    Collections.sort(allLibraries);
    
  }
  
  
  public void updateInstalled(List<Library> installed) {
    
    // Since there is no 'category' attribute in export.txt, we need some way to
    // determine the category names of libraries that are already installed. For
    // this, we use a mapping of library names to category names.
    HashMap<String, String> categoriesByName = new HashMap<String, String>();
    
    // Check if any of the advertised libraries are already installed, and
    // remove them if they are
    HashSet<String> installedLibraries = new HashSet<String>();
    for (Library library : installed) {
      installedLibraries.add(library.getName());
    }
    
    Iterator<LibraryInfo> it = allLibraries.iterator();
    while (it.hasNext()) {
      LibraryInfo libInfo = it.next();
      if (installedLibraries.contains(libInfo.name)) {
        it.remove();
        categoriesByName.put(libInfo.name, libInfo.category);
      }
    }
    
    // Create LibraryInfo objects for each of the installed libraries. Place
    // them into the unknown category if they weren't advertised at all.
    for (Library library : installed) {
      String category = categoriesByName.get(library.getName());
      if (category != null) {
        library.info.category = category;
      }
      addLibrary(library.info);
    }
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
      
      if (category != null && !category.equals(libInfo.category)) {
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
    
    for (Author author : libInfo.authorList) {
      if (author.name.toLowerCase().matches(filter)) {
        return true;
      }
    }
    
    return libInfo.sentence != null && libInfo.sentence.toLowerCase().matches(filter)
        || libInfo.paragraph != null && libInfo.paragraph.toLowerCase().matches(filter)
        || libInfo.category != null && libInfo.category.toLowerCase().matches(filter)
        || libInfo.name != null && libInfo.name.toLowerCase().matches(filter);
 
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

        url = new URL("http://dl.dropbox.com/u/700641/generated/software.xml");

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
          libListing = new LibraryListing();
          
          File xmlFile = downloader.getFile();
          if (xmlFile != null) {
            libListing.rebuildList(xmlFile);
          }
        }
      });
      
      downloader.run();
    }
    
    public LibraryListing getLibraryListing() {
      return libListing;
    }
  }

  private void addLibrary(LibraryInfo libInfo) {
    if (librariesByCategory.containsKey(libInfo.category)) {
      librariesByCategory.get(libInfo.category).add(libInfo);
    } else {
      ArrayList<LibraryInfo> libs = new ArrayList<LibraryInfo>();
      libs.add(libInfo);
      librariesByCategory.put(libInfo.category, libs);
    }
    allLibraries.add(libInfo);
  }
  
  /**
   * Class to parse the libraries xml file
   */
  class LibraryXmlParser extends DefaultHandler {
    
    String currentCategoryName;

    LibraryInfo currentLibInfo;

    LibraryXmlParser(File xmlFile) {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);
      
      try {
        SAXParser sp = spf.newSAXParser();

        InputSource input = new InputSource(new FileReader(xmlFile));

        sp.parse(input, this);

        // XXX: Do something meaningful when we get an error
      } catch (IOException e) {
        Base.showWarning("Error reading library list",
                         "A error occured while reading the list of available libraries.\n" +
                         "Try restarting the Library Manager.\n", e);
      } catch (Exception e) {
        Base.showWarning("Error reading library list",
                         "The list of libraries downloaded from Processing.org\n" +
                         "appears to be malformed. You can still install libraries\n" + 
                         "manually while we work on fixing this.", e);
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

      if ("category".equals(qName)) {
        currentCategoryName = attributes.getValue("name");

      } else if ("library".equals(qName)) {
        currentLibInfo = new LibraryInfo();
        currentLibInfo.authorList = new ArrayList<Author>();
        currentLibInfo.category = currentCategoryName;
        currentLibInfo.name = attributes.getValue("name");
        currentLibInfo.url = attributes.getValue("url");
        
      } else if ("author".equals(qName)) {
        Author author = new Author();
        author.name = attributes.getValue("name");
        author.url = attributes.getValue("url");
        currentLibInfo.authorList.add(author);

      } else if ("description".equals(qName)) {
        currentLibInfo.sentence = attributes.getValue("sentence");
        currentLibInfo.paragraph = attributes.getValue("paragraph");
        
      } else if ("version".equals(qName)) {
        currentLibInfo.version = Integer.parseInt(attributes.getValue("id"));
        currentLibInfo.prettyVersion = attributes.getValue("pretty");

      } else if ("location".equals(qName)) {
        currentLibInfo.link = attributes.getValue("url");

      }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {

      if ("library".equals(qName)) {
        addLibrary(currentLibInfo);
        currentLibInfo = null;
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
