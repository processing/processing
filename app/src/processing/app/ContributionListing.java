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

import processing.app.contribution.*;
import processing.app.contribution.Contribution.Type;
import processing.core.PApplet;

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
    
    listingFile = Base.getSettingsFile("contributions.txt");
    listingFile.setWritable(true);
    if (listingFile.exists()) {
      setAdvertisedList(listingFile);
    }
  }


  void setAdvertisedList(File file) {
    
    listingFile = file;
    
    advertisedContributions.clear();
    advertisedContributions.addAll(getLibraries(listingFile));
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
    
    return contrib.getAuthorList() != null && contrib.getAuthorList().toLowerCase().matches(filter)
        || contrib.getSentence() != null && contrib.getSentence().toLowerCase().matches(filter)
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
          url = new URL("http://processing.googlecode.com/svn/trunk/web/contrib_generate/contributions.txt");
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
  
  public ArrayList<AdvertisedContribution> getLibraries(File f) {
    ArrayList<AdvertisedContribution> outgoing = new ArrayList<AdvertisedContribution>();
    
    if (f != null && f.exists()) {
      String lines[] = PApplet.loadStrings(f);
      
      int start = 0;
      while (start < lines.length) {
        // Only consider 'invalid' lines. These lines contain the type of
        // software: library, tool, mode
        if (!lines[start].contains("=")) {
          String type = lines[start];

          // Scan forward for the next blank line
          int end = ++start;
          while (end < lines.length && !lines[end].equals("")) {
            end++;
          }
          
          int length = end - start;
          String strings[] = new String[length];
          System.arraycopy(lines, start, strings, 0, length);
          
          HashMap<String,String> exports = new HashMap<String,String>();
          Base.readSettings(strings, exports);
          
          Type kind = Contribution.Type.toType(type);
          outgoing.add(new AdvertisedContribution(kind, exports));
          
          start = end + 1;
        } else {
          start++;
        }
      }
    }
    
    return outgoing;
  }
  
  static class AdvertisedContribution implements Contribution {
    
    protected final String name;             // "pdf" or "PDF Export"
    protected final Type type;               // Library, tool, etc.
    protected final String category;         // "Sound"
    protected final String authorList;       // [Ben Fry](http://benfry.com/)
    protected final String url;              // http://processing.org
    protected final String sentence;         // Write graphics to PDF files.
    protected final String paragraph;        // <paragraph length description for site>
    protected final int version;             // 102
    protected final String prettyVersion;    // "1.0.2"
    protected final String link;             // Direct link to download the file
    
    public AdvertisedContribution(Type type, HashMap<String, String> exports) {
      
      this.type = type;
      name = exports.get("name");
      category = exports.get("category");
      authorList = exports.get("authorList");

      url = exports.get("url");
      sentence = exports.get("sentence");
      paragraph = exports.get("paragraph");

      int v = 0;
      try {
        v = Integer.parseInt(exports.get("version"));
      } catch (NumberFormatException e) {
      }
      version = v;
      
      prettyVersion = exports.get("prettyVersion");
      
      String download = null;
      
      String hostPlatform = Base.getPlatformName();
      int nativeBits = Base.getNativeBits();
      String hostVersion = Base.getPlatformVersionName();
      
      // Try download.macosx64.lion
      if (!hostPlatform.isEmpty())
        download = exports.get("download." + hostPlatform + nativeBits + "." + hostVersion);
      // Try download.macosx.lion
      if (download == null)
        download = exports.get("download." + hostPlatform + "." + hostVersion);
      // Try download.macosx64
      if (download == null)
        download = exports.get("download." + hostPlatform + nativeBits);
      // Try download.macosx
      if (download == null)
        download = exports.get("download." + hostPlatform);
      // Try download
      if (download == null)
        download = exports.get("download");
      
      // If it's still null by this point, the library doesn't support this OS
      this.link = download;
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
    
    public String getPrettyVersion() {
      return prettyVersion;
    }
    
  }

  public boolean isDownloadingListing() {
    return downloadingListingLock.isLocked();
  }
  
}
