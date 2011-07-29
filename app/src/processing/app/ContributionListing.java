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

import processing.app.contribution.*;
import processing.app.contribution.Contribution.Type;

public class ContributionListing {
  
  ArrayList<ContributionChangeListener> listeners;
  
  ArrayList<AdvertisedContribution> advertisedContributions;
  
  Map<String, List<Contribution>> librariesByCategory;
  
  ArrayList<Contribution> allContributions;

  private Comparator<Contribution> contribComparator;
  
  public ContributionListing() {
    listeners = new ArrayList<ContributionChangeListener>();
    librariesByCategory = new HashMap<String, List<Contribution>>();
    allContributions = new ArrayList<Contribution>();
    
    contribComparator = new Comparator<Contribution>() {
      public int compare(Contribution o1, Contribution o2) {
        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
      }
    };
  }


  public void setAdvertisedList(File xmlFile) {
    
    ContributionXmlParser xmlParser = new ContributionXmlParser(xmlFile);
    advertisedContributions = xmlParser.getLibraries();
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

  private boolean matches(Contribution info, String filter) {
    
    // Maybe this can be fancy some other time
    if (filter.equals("has:update") || filter.equals("has:updates")) {
      return hasUpdates(info);
    }
    if (filter.equals("is:installed")) {
      return info.isInstalled();
    }
    if (filter.equals("not:installed")) {
      return !info.isInstalled();
    }
    if (filter.contains(":")) {
      // Return true and ignore everything else if the property is invalid.
      return true;
    }
    // sdfikjfdslk Added filters properties for showing installed and upgrade 
    filter = ".*" + filter.toLowerCase() + ".*";
    
    if (filter.isEmpty()) {
      return true;
    }
    
    for (Author author : info.getAuthorList()) {
      if (author.name.toLowerCase().matches(filter)) {
        return true;
      }
    }
    
    return info.getSentence() != null && info.getSentence().toLowerCase().matches(filter)
        || info.getParagraph() != null && info.getParagraph().toLowerCase().matches(filter)
        || info.getCategory() != null && info.getCategory().toLowerCase().matches(filter)
        || info.getName() != null && info.getName().toLowerCase().matches(filter);
 
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
    listeners.add(listener);
  }
  
  public void removeContributionListener(ContributionChangeListener listener) {
    listeners.remove(listener);
  }
  
  public ArrayList<ContributionChangeListener> getContributionListeners() {
    return new ArrayList<ContributionChangeListener>(listeners);
  }
  
  public void getAdvertisedContributions(ProgressMonitor pm) {
  
    final ContributionListFetcher llf = new ContributionListFetcher();
    llf.setProgressMonitor(pm);
    new Thread(llf).start();
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

  public static interface ContributionChangeListener {
    
    public void contributionAdded(Contribution Contribution);
    
    public void contributionRemoved(Contribution Contribution);
    
    public void contributionChanged(Contribution oldLib, Contribution newLib);
    
  }
  
  public class ContributionListFetcher implements Runnable {

    File dest;
    
    URL url;
    
    FileDownloader downloader;
    
    Thread downloaderThread;
    
    ProgressMonitor progressMonitor;

    public ContributionListFetcher() {
      
      progressMonitor = new NullProgressMonitor();
      
      try {
        File tmpFolder = Base.createTempFolder("libarylist", "download");

        dest = new File(tmpFolder, "contributions.xml");
        dest.setWritable(true);

        url = new URL("http://dl.dropbox.com/u/700641/generated/contributions.xml");

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
            setAdvertisedList(xmlFile);
          }
        }
      });
      
      downloader.run();
    }
    
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

      } else if (LIBRARY_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.LIBRARY);
        setCommonAttributes(attributes);

      }  else if (LIBRARY_COMPILATION_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.LIBRARY_COMPILATION);
        setCommonAttributes(attributes);
        
      } else if (TOOL_TAG.equals(qName)) {
        currentInfo = new AdvertisedContribution(Type.TOOL);
        setCommonAttributes(attributes);

      } else if ("author".equals(qName)) {
        Author author = new Author();
        author.name = attributes.getValue("name");
        author.url = attributes.getValue("url");
        currentInfo.authorList.add(author);

      } else if ("description".equals(qName)) {
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
      currentInfo.authorList = new ArrayList<Author>();
      currentInfo.category = currentCategoryName;
      currentInfo.name = attributes.getValue("name");
      currentInfo.url = attributes.getValue("url");
    }
    
    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException {

      if (LIBRARY_TAG.equals(qName) || LIBRARY_COMPILATION_TAG.equals(qName)
          || TOOL_TAG.equals(qName)) {
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
    protected List<Author> authorList; // Ben Fry
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
    
    public List<Author> getAuthorList() {
      return new ArrayList<Author>(authorList);
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
  
}
