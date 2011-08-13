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
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import processing.app.contribution.*;
import processing.app.contribution.Contribution.Type;

public class ContributionListing {
  
  ArrayList<ContributionChangeListener> listeners;
  
  ArrayList<AdvertisedContribution> advertisedContributions;
  
  Map<String, List<Contribution>> librariesByCategory;
  
  ArrayList<Contribution> allContributions;

  boolean hasDownloadedLatestList;
  
  ReentrantLock downloadingListingLock;
  
  static Comparator<Contribution> contribComparator = new Comparator<Contribution>() {
    public int compare(Contribution o1, Contribution o2) {
      return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
    }
  };
  
  File listingFile;
  
  public ContributionListing() {
    listeners = new ArrayList<ContributionChangeListener>();
    advertisedContributions = new ArrayList<AdvertisedContribution>();
    librariesByCategory = new HashMap<String, List<Contribution>>();
    allContributions = new ArrayList<Contribution>();
    downloadingListingLock = new ReentrantLock();
    
    listingFile = Base.getSettingsFile("contributions.xml");
    listingFile.setWritable(true);
    if (listingFile.exists()) {
      setAdvertisedList(listingFile);
    }
  }


  void setAdvertisedList(File xmlFile) {
    
    listingFile = xmlFile;
    
    ContributionXmlParser xmlParser = new ContributionXmlParser(listingFile);
    advertisedContributions.clear();
    advertisedContributions.addAll(xmlParser.getLibraries());
    for (Contribution contribution : advertisedContributions) {
      addContribution(contribution);
    }
    
    Collections.sort(allContributions, contribComparator);

  }
  
  public Comparator<? super Contribution> getComparator() {
    return contribComparator;
  }
  
  /**
   * Adds the installed libraries to the listing of libraries, replacing any
   * pre-existing libraries by the same name as one in the list.
   */
  public void updateInstalledList(List<Contribution> installedContributions) {
    
    for (Contribution contribution : installedContributions) {
      
      Contribution preexistingContribution = getContribution(contribution);

      if (preexistingContribution != null) {
        replaceContribution(preexistingContribution, contribution);
      } else {
        addContribution(contribution);
      }
    }
    
  }
  
  
  public void replaceContribution(Contribution oldLib, Contribution newLib) {
    
    if (oldLib == null || newLib == null) {
      return;
    }
    
    if (librariesByCategory.containsKey(oldLib.getCategory())) {
      List<Contribution> list = librariesByCategory.get(oldLib.getCategory());
      
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i) == oldLib) {
          list.set(i, newLib);
        }
      }
    }
    
    for (int i = 0; i < allContributions.size(); i++) {
      if (allContributions.get(i) == oldLib) {
        allContributions.set(i, newLib);
      }
    }
    
    notifyChange(oldLib, newLib);
  }
  
  public void addContribution(Contribution contribution) {
    
    if (librariesByCategory.containsKey(contribution.getCategory())) {
      List<Contribution> list = librariesByCategory.get(contribution.getCategory());
      list.add(contribution);
      
      Collections.sort(list, contribComparator);
    } else {
      ArrayList<Contribution> list = new ArrayList<Contribution>();
      list.add(contribution);
      librariesByCategory.put(contribution.getCategory(), list);
    }
    allContributions.add(contribution);
    
    notifyAdd(contribution);
    
    Collections.sort(allContributions, contribComparator);
  }
  
  public void removeContribution(Contribution info) {
    if (librariesByCategory.containsKey(info.getCategory())) {
      librariesByCategory.get(info.getCategory()).remove(info);
    }
    allContributions.remove(info);
    
    notifyRemove(info);
  }
  
  public Contribution getContribution(Contribution contribution) {
    for (Contribution preexistingContribution : allContributions) {
      if (preexistingContribution.getName().equals(contribution.getName())
          && preexistingContribution.getType() == contribution.getType()) {
        return preexistingContribution;
      }
    }
    return null;
  }
  
  public AdvertisedContribution getAdvertisedContribution(Contribution info) {
    for (AdvertisedContribution advertised : advertisedContributions) {
      
      if (advertised.getType() == info.getType()
          && advertised.getName().equals(info.getName())) {
        
        return advertised;
      }
      
    }
    
    return null;
  }
  
  public Set<String> getCategories() {
    return librariesByCategory.keySet();
  }

  public List<Contribution> getAllContributions() {
    return new ArrayList<Contribution>(allContributions);
  }

  public List<Contribution> getLibararies(String category) {
    ArrayList<Contribution> libinfos =
        new ArrayList<Contribution>(librariesByCategory.get(category));
    Collections.sort(libinfos, contribComparator);
    return libinfos;
  }
  
  public List<Contribution> getFilteredLibraryList(String category, List<String> filters) {
    ArrayList<Contribution> filteredList = new ArrayList<Contribution>(allContributions);
    
    Iterator<Contribution> it = filteredList.iterator();
    while (it.hasNext()) {
      Contribution libInfo = it.next();
      
      if (category != null && !category.equals(libInfo.getCategory())) {
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

  public boolean matches(Contribution contrib, String filter) {
    
    int colon = filter.indexOf(":");
    if (colon != -1) {
      String isText = filter.substring(0, colon);
      String property = filter.substring(colon + 1);
      
      // Chances are the person is still typing the property, so rather than
      // make the list flash empty (because nothing contains "is:" or "has:",
      // just return true.
      if (!isProperty(property))
        return true;
      
      if ("is".equals(isText) || "has".equals(isText)) {
        return hasProperty(contrib, filter.substring(colon + 1));
      } else  if ("not".equals(isText)) {
        return !hasProperty(contrib, filter.substring(colon + 1));
      }
    }
    
    filter = ".*" + filter.toLowerCase() + ".*";
    
    if (filter.isEmpty()) {
      return true;
    }
    
    if (contrib.getAuthorList().toLowerCase().matches(filter)) {
      return true;
    }
    
    return contrib.getSentence() != null && contrib.getSentence().toLowerCase().matches(filter)
        || contrib.getParagraph() != null && contrib.getParagraph().toLowerCase().matches(filter)
        || contrib.getCategory() != null && contrib.getCategory().toLowerCase().matches(filter)
        || contrib.getName() != null && contrib.getName().toLowerCase().matches(filter);
 
  }
  
  public boolean isProperty(String property) {
    return property.startsWith("updat") || property.startsWith("upgrad")
        || property.startsWith("instal") && !property.startsWith("installabl")
        || property.equals("tool") || property.startsWith("lib")
        || property.equals("mode") || property.equals("compilation");
  }

  /** Returns true if the contribution fits the given property, false otherwise.
   *  If the property is invalid, returns false. */
  public boolean hasProperty(Contribution contrib, String property) {
    // update, updates, updatable, upgrade
    if (property.startsWith("updat") || property.startsWith("upgrad")) {
      return hasUpdates(contrib);
    }
    if (property.startsWith("instal") && !property.startsWith("installabl")) {
      return contrib.isInstalled();
    }
    if (property.equals("tool")) {
      return contrib.getType() == Contribution.Type.TOOL;
    }
    if (property.startsWith("lib")) {
      return contrib.getType() == Contribution.Type.LIBRARY
          || contrib.getType() == Contribution.Type.LIBRARY_COMPILATION;
    }
    if (property.equals("mode")) {
      return contrib.getType() == Contribution.Type.MODE;
    }
    if (property.equals("compilation")) {
      return contrib.getType() == Contribution.Type.LIBRARY_COMPILATION;
    }

    return false;
  }

  private void notifyRemove(Contribution contribution) {
    for (ContributionChangeListener listener : listeners) {
      listener.contributionRemoved(contribution);
    }
  }
  
  private void notifyAdd(Contribution contribution) {
    for (ContributionChangeListener listener : listeners) {
      listener.contributionAdded(contribution);
    }
  }
  
  private void notifyChange(Contribution oldLib, Contribution newLib) {
    for (ContributionChangeListener listener : listeners) {
      listener.contributionChanged(oldLib, newLib);
    }
  }
  
  public void addContributionListener(ContributionChangeListener listener) {
    for (Contribution contrib : allContributions) {
      listener.contributionAdded(contrib);
    }
    listeners.add(listener);
  }
  
  public void removeContributionListener(ContributionChangeListener listener) {
    listeners.remove(listener);
  }
  
  public ArrayList<ContributionChangeListener> getContributionListeners() {
    return new ArrayList<ContributionChangeListener>(listeners);
  }

  /**
   * Starts a new thread to download the advertised list of contributions. Only
   * one instance will run at a time.
   */
  public void getAdvertisedContributions(ProgressMonitor pm) {
    
    final ProgressMonitor progressMonitor = (pm != null) ? pm : new NullProgressMonitor();
    
    new Thread(new Runnable() {
      
      public void run() {
        downloadingListingLock.lock();
        
        URL url = null;
        try {
          url = new URL("http://processing.googlecode.com/svn/trunk/web/contrib_generate/contributions.xml");
        } catch (MalformedURLException e) {
          progressMonitor.error(e);
          progressMonitor.finished();
        }
        
        if (!progressMonitor.isFinished()) {
          FileDownloader.downloadFile(url, listingFile, progressMonitor);
          if (!progressMonitor.isCanceled() && !progressMonitor.isError()) {
            hasDownloadedLatestList = true;
            setAdvertisedList(listingFile);
          }
        }
        
        downloadingListingLock.unlock();
      }
    }).start();
  }
  
  public boolean hasUpdates() {
    for (Contribution info : allContributions) {
      if (hasUpdates(info)) {
        return true;
      }
    }
    return false;
  }


  public boolean hasUpdates(Contribution contribution) {
    if (contribution.isInstalled()) {
      Contribution advertised = getAdvertisedContribution(contribution);
      if (advertised == null)
        return false;
      
      return advertised.getVersion() > contribution.getVersion();
    }
    
    return false;
  }
  
  public boolean hasDownloadedLatestList() {
    return hasDownloadedLatestList;
  }

  public static interface ContributionChangeListener {
    
    public void contributionAdded(Contribution Contribution);
    
    public void contributionRemoved(Contribution Contribution);
    
    public void contributionChanged(Contribution oldLib, Contribution newLib);
    
  }
  
  /**
   * Class to parse the libraries xml file
   */
  private static class ContributionXmlParser extends DefaultHandler {
    
    final static String LIBRARY_TAG = "library";
    final static String LIBRARY_COMPILATION_TAG = "librarycompilation";
    final static String TOOL_TAG = "tool";
    //final static String MODE_TAG = "mode";
    
    ArrayList<AdvertisedContribution> contributions;
    
    String currentCategoryName;

    AdvertisedContribution currentInfo;

    ContributionXmlParser(File xmlFile) {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setValidating(false);
      
      try {
        SAXParser sp = spf.newSAXParser(); // throws ParserConfigurationException

        InputSource input = new InputSource(new FileReader(xmlFile));

        contributions = new ArrayList<AdvertisedContribution>();
        sp.parse(input, this); // throws SAXException

      } catch (ParserConfigurationException e) {
        Base.showWarning("Error reading contributions list",
                         "An internal error occured when preparing to read the list\n" +
                             "of libraries. You can still install libraries manually while\n" +
                             "we work on fixing this.", e);
      } catch (IOException e) {
        Base.showWarning("Error reading contributions list",
                         "A error occured while reading the list of available libraries.\n" +
                         "Try restarting the Contribution Manager.\n", e);
      } catch (SAXException e) {
        Base.showWarning("Error reading contributions list",
                         "The list of libraries downloaded from Processing.org\n" +
                         "appears to be malformed. You can still install libraries\n" + 
                         "manually while we work on fixing this.", e);
        contributions = null;
      }
    }

    public ArrayList<AdvertisedContribution> getLibraries() {
      return contributions;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

      if ("category".equals(qName)) {
        currentCategoryName = attributes.getValue("name");

      } else if (ContributionXmlParser.LIBRARY_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.LIBRARY);
        setCommonAttributes(attributes);

      }  else if (ContributionXmlParser.LIBRARY_COMPILATION_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.LIBRARY_COMPILATION);
        setCommonAttributes(attributes);
        
      } else if (ContributionXmlParser.TOOL_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.TOOL);
        setCommonAttributes(attributes);

      } else if ("description".equals(qName)) {
        currentInfo.authorList = attributes.getValue("authorList");
        currentInfo.sentence = attributes.getValue("sentence");
        currentInfo.paragraph = attributes.getValue("paragraph");
        
      } else if ("version".equals(qName)) {
        currentInfo.version = Integer.parseInt(attributes.getValue("id"));
        currentInfo.prettyVersion = attributes.getValue("pretty");

      } else if ("location".equals(qName)) {
        currentInfo.link = attributes.getValue("url");

      }
      
    }
    
    private void setCommonAttributes(Attributes attributes) {
      currentInfo.category = currentCategoryName;
      currentInfo.name = attributes.getValue("name");
      currentInfo.url = attributes.getValue("url");
    }
    
    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {

      if (ContributionXmlParser.LIBRARY_TAG.equals(qName)
          || ContributionXmlParser.LIBRARY_COMPILATION_TAG.equals(qName)
          || ContributionXmlParser.TOOL_TAG.equals(qName)) {
        contributions.add(currentInfo);
        currentInfo = null;
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
  
  static class AdvertisedContribution implements Contribution {
    
    protected String name;             // "pdf" or "PDF Export"
    protected Type type;               // Library, tool, etc.
    protected String category;         // "Sound"
    protected String authorList;       // [Ben Fry](http://benfry.com/)
    protected String url;              // http://processing.org
    protected String sentence;         // Write graphics to PDF files.
    protected String paragraph;        // <paragraph length description for site>
    protected int version;             // 102
    protected int latestVersion;       // 103
    protected String prettyVersion;    // "1.0.2"
    protected String link;             // Direct link to download the file
    
    public AdvertisedContribution(Type type) {
      this.type = type;
    }
    
    public boolean isInstalled() {
      return false;
    }
    
    public Type getType() {
      return type;
    }
    
    public String getCategory() {
      return category;
    }
    
    public String getName() {
      return name;
    }
    
    public String getAuthorList() {
      return authorList;
    }
    
    public String getUrl() {
      return url;
    }
    
    public String getSentence() {
      return sentence;
    }
    
    public String getParagraph() {
      return paragraph;
    }
    
    public int getVersion() {
      return version;
    }
    
    public int getLatestVersion() {
      return latestVersion;
    }
    
    public String getPrettyVersion() {
      return prettyVersion;
    }
    
  }

  public boolean isDownloadingListing() {
    return downloadingListingLock.isLocked();
  }
  
}
