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
  
  ArrayList<LibraryChangeListener> listeners;
  
  ArrayList<LibraryInfo> advertisedLibraries;
  
  Map<String, List<LibraryInfo>> librariesByCategory;
  
  ArrayList<LibraryInfo> allLibraries;
  
  boolean hasDownloadedList;
  
  
  public LibraryListing() {
    listeners = new ArrayList<LibraryChangeListener>();
    librariesByCategory = new HashMap<String, List<LibraryInfo>>();
    allLibraries = new ArrayList<LibraryInfo>();
    hasDownloadedList = false;
  }


  public void updateList(File xmlFile) {
    
    hasDownloadedList = true;
    
    LibraryXmlParser xmlParser = new LibraryXmlParser(xmlFile);
    advertisedLibraries = xmlParser.getLibraries();
    updateList(advertisedLibraries);
    
    Collections.sort(allLibraries);
    
  }
  
  /**
   * Adds the installed libraries to the listing of libraries, replacing any
   * pre-existing libraries by the same name as one in the list.
   */
  public void updateList(List<LibraryInfo> libraries) {
    
    // First, record the names of all the libraries in installedLibraries
    HashSet<String> installedLibraryNames = new HashSet<String>();
    for (LibraryInfo libInfo : libraries) {
      installedLibraryNames.add(libInfo.name);
    }

    // We also want to remember categories of libraries that happen to already
    // exist since there is no 'category' attribute in export.txt. For this, we
    // use a mapping of library names to category names.
    HashMap<String, String> categoriesByName = new HashMap<String, String>();
    
    Iterator<LibraryInfo> it = allLibraries.iterator();
    while (it.hasNext()) {
      LibraryInfo libInfo = it.next();
      if (installedLibraryNames.contains(libInfo.name)) {
        if (librariesByCategory.containsKey(libInfo.category)) {
          librariesByCategory.get(libInfo.category).remove(libInfo);
        }
        it.remove();
        notifyRemove(libInfo);
        categoriesByName.put(libInfo.name, libInfo.category);
      }
    }
    
    for (LibraryInfo libInfo : libraries) {
      String category = categoriesByName.get(libInfo.name);
      if (category != null) {
        libInfo.category = category;
      }
      addLibrary(libInfo);
    }
    
    Collections.sort(allLibraries);
    
  }
  
  
  public void replaceLibrary(LibraryInfo oldLib, LibraryInfo newLib) {
    
    if (oldLib == null || newLib == null) {
      return;
    }
    
    if (librariesByCategory.containsKey(oldLib.category)) {
      List<LibraryInfo> list = librariesByCategory.get(oldLib.category);
      
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) == oldLib) {
          list.set(i, newLib);
        }
      }
    }
    
    for (int i = 0; i < allLibraries.size(); i++) {
      if (allLibraries.get(i) == oldLib) {
        allLibraries.set(i, newLib);
      }
    }
    
    notifyChange(oldLib, newLib);
  }
  
  public void addLibrary(LibraryInfo libInfo) {
    
    if (librariesByCategory.containsKey(libInfo.category)) {
      List<LibraryInfo> list = librariesByCategory.get(libInfo.category);
      list.add(libInfo);
      
      Collections.sort(list);
    } else {
      ArrayList<LibraryInfo> libs = new ArrayList<LibraryInfo>();
      libs.add(libInfo);
      librariesByCategory.put(libInfo.category, libs);
    }
    allLibraries.add(libInfo);
    
    notifyAdd(libInfo);
    
    Collections.sort(allLibraries);
  }
  
  public void removeLibrary(LibraryInfo info) {
    if (librariesByCategory.containsKey(info.category)) {
      librariesByCategory.get(info.category).remove(info);
    }
    allLibraries.remove(info);
    
    notifyRemove(info);
  }
  
  public LibraryInfo getAdvertisedLibrary(String libName) {
    for (LibraryInfo advertisedLib : advertisedLibraries) {
      if (advertisedLib.name.equals(libName)) {
        return advertisedLib;
      }
    }
    
    return null;
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

  private void notifyRemove(LibraryInfo libraryInfo) {
    for (LibraryChangeListener listener : listeners) {
      listener.libraryRemoved(libraryInfo);
    }
  }
  
  private void notifyAdd(LibraryInfo libraryInfo) {
    for (LibraryChangeListener listener : listeners) {
      listener.libraryAdded(libraryInfo);
    }
  }
  
  private void notifyChange(LibraryInfo oldLib, LibraryInfo newLib) {
    for (LibraryChangeListener listener : listeners) {
      listener.libraryChanged(oldLib, newLib);
    }
  }
  
  public void addLibraryListener(LibraryChangeListener listener) {
    listeners.add(listener);
  }
  
  public void removeLibraryListener(LibraryChangeListener listener) {
    listeners.remove(listener);
  }
  
  public ArrayList<LibraryChangeListener> getLibraryListeners() {
    return new ArrayList<LibraryChangeListener>(listeners);
  }
  
  public static interface LibraryChangeListener {
    
    public void libraryAdded(LibraryInfo libraryInfo);
    
    public void libraryRemoved(LibraryInfo libraryInfo);
    
    public void libraryChanged(LibraryInfo oldLib, LibraryInfo newLib);
    
  }
  
  public static class LibraryListFetcher implements Runnable {

    LibraryListing libListing;

    File dest;
    
    URL url;
    
    FileDownloader downloader;
    
    Thread downloaderThread;
    
    ProgressMonitor progressMonitor;

    public LibraryListFetcher(LibraryListing libListing) {
      
      this.libListing = libListing;
      
      progressMonitor = new NullProgressMonitor();
      
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
          
          File xmlFile = downloader.getFile();
          if (xmlFile != null) {
            libListing.updateList(xmlFile);
          }
        }
      });
      
      downloader.run();
    }
    
    public LibraryListing getLibraryListing() {
      return libListing;
    }
  }

  /**
   * Class to parse the libraries xml file
   */
  private static class LibraryXmlParser extends DefaultHandler {
    
    ArrayList<LibraryInfo> libraries;
    
    String currentCategoryName;

    LibraryInfo currentLibInfo;

    LibraryXmlParser(File xmlFile) {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);
      
      try {
        SAXParser sp = spf.newSAXParser(); // throws ParserConfigurationException

        InputSource input = new InputSource(new FileReader(xmlFile));

        libraries = new ArrayList<LibraryInfo>();
        sp.parse(input, this); // throws SAXException

      } catch (ParserConfigurationException e) {
        Base.showWarning("Error reading library list",
                         "An internal error occured when preparing to read the list\n" +
                             "of libraries. You can still install libraries manually while\n" +
                             "we work on fixing this.", e);
      } catch (IOException e) {
        Base.showWarning("Error reading library list",
                         "A error occured while reading the list of available libraries.\n" +
                         "Try restarting the Library Manager.\n", e);
      } catch (SAXException e) {
        Base.showWarning("Error reading library list",
                         "The list of libraries downloaded from Processing.org\n" +
                         "appears to be malformed. You can still install libraries\n" + 
                         "manually while we work on fixing this.", e);
        libraries = null;
      }
    }

    public ArrayList<LibraryInfo> getLibraries() {
      return libraries;
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
        libraries.add(currentLibInfo);
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
