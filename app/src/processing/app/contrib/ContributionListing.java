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

package processing.app.contrib;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import processing.app.Base;
import processing.core.PApplet;


public class ContributionListing {
  static final String LISTING_URL = 
    "https://raw.github.com/processing/processing-web/master/contrib_generate/contributions.txt";

  File listingFile;
  ArrayList<ContributionChangeListener> listeners;
  ArrayList<AdvertisedContribution> advertisedContributions;
  Map<String, List<Contribution>> librariesByCategory;
  ArrayList<Contribution> allContributions;
  boolean hasDownloadedLatestList;
  ReentrantLock downloadingListingLock;

  static protected final String validCategories[] = {
    "3D", "Animation", "Compilations", "Data", "Geometry", "GUI", "Hardware",
    "I/O", "Math", "Simulation", "Sound", "Utilities", "Typography",
    "Video & Vision" };

  static ContributionListing singleInstance;
  


  private ContributionListing() {
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


  static public ContributionListing getInstance() {
    if (singleInstance == null) {
      singleInstance = new ContributionListing();
    }
    return singleInstance;
  }


  void setAdvertisedList(File file) {
    listingFile = file;

    advertisedContributions.clear();
    advertisedContributions.addAll(parseContribList(listingFile));
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
      if (advertised.getType() == info.getType() && 
          advertised.getName().equals(info.getName())) {
        return advertised;
      }
    }
    return null;
  }

  
  public Set<String> getCategories(Filter filter) {
    Set<String> outgoing = new HashSet<String>();

    Set<String> categorySet = librariesByCategory.keySet();
    for (String categoryName : categorySet) {
      for (Contribution contrib : librariesByCategory.get(categoryName)) {
        if (filter.matches(contrib)) {
          // TODO still not sure why category would be coming back null [fry]
          // http://code.google.com/p/processing/issues/detail?id=1387
          if (categoryName != null && categoryName.trim().length() != 0) {
            outgoing.add(categoryName);
          }
          break;
        }
      }
    }
    return outgoing;
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
      if (!isProperty(property)) {
        return true;
      }

      if ("is".equals(isText) || "has".equals(isText)) {
        return hasProperty(contrib, filter.substring(colon + 1));
      } else if ("not".equals(isText)) {
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
      return contrib.getType() == ContributionType.TOOL;
    }
    if (property.startsWith("lib")) {
      return contrib.getType() == ContributionType.LIBRARY;
//      return contrib.getType() == Contribution.Type.LIBRARY
//          || contrib.getType() == Contribution.Type.LIBRARY_COMPILATION;
    }
    if (property.equals("mode")) {
      return contrib.getType() == ContributionType.MODE;
    }
//    if (property.equals("compilation")) {
//      return contrib.getType() == Contribution.Type.LIBRARY_COMPILATION;
//    }

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
   * Starts a new thread to download the advertised list of contributions. 
   * Only one instance will run at a time.
   */
  public void getAdvertisedContributions(ProgressMonitor pm) {
    final ProgressMonitor progressMonitor = 
      (pm != null) ? pm : new NullProgressMonitor();

    new Thread(new Runnable() {
      public void run() {
        downloadingListingLock.lock();

        URL url = null;
        try {
          url = new URL(LISTING_URL);
        } catch (MalformedURLException e) {
          progressMonitor.error(e);
          progressMonitor.finished();
        }

        if (!progressMonitor.isFinished()) {
          download(url, listingFile, progressMonitor);
          if (!progressMonitor.isCanceled() && !progressMonitor.isError()) {
            hasDownloadedLatestList = true;
            setAdvertisedList(listingFile);
          }
        }
        downloadingListingLock.unlock();
      }
    }).start();
  }
  
  
  /**
   * Blocks until the file is downloaded or an error occurs. 
   * Returns true if the file was successfully downloaded, false otherwise.
   * 
   * @param source
   *          the URL of the file to download
   * @param dest
   *          the file on the local system where the file will be written. This
   *          must be a file (not a directory), and must already exist.
   * @param progress
   * @throws FileNotFoundException
   *           if an error occurred downloading the file
   */
  static boolean download(URL source, File dest, ProgressMonitor progress) {
    boolean success = false;
    try {
//      System.out.println("downloading file " + source);
      URLConnection conn = source.openConnection();
      conn.setConnectTimeout(1000);
      conn.setReadTimeout(5000);
  
      // TODO this is often -1, may need to set progress to indeterminate
      int fileSize = conn.getContentLength();
//      System.out.println("file size is " + fileSize);
      progress.startTask("Downloading", fileSize);
  
      InputStream in = conn.getInputStream();
      FileOutputStream out = new FileOutputStream(dest);
  
      byte[] b = new byte[8192];
      int amount;
      int total = 0;
      while (!progress.isCanceled() && (amount = in.read(b)) != -1) {
        out.write(b, 0, amount);
        total += amount;  
        progress.setProgress(total);
      }
      out.flush();
      out.close();
      success = true;
      
    } catch (IOException ioe) {
      progress.error(ioe);
      ioe.printStackTrace();
    }
    progress.finished();
    return success;
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
      if (advertised == null) {
        return false;
      }
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
   * @return a lowercase string with all non-alphabetic characters removed
   */
  static protected String normalize(String s) {
    return s.toLowerCase().replaceAll("^\\p{Lower}", "");
  }

  
  /**
   * @return the proper, valid name of this category to be displayed in the UI
   *         (e.g. "Typography / Geometry"). "Unknown" if the category null.
   */
  static public String getCategory(String category) {
    if (category == null) {
      return "Unknown";
    }
    String normCatName = normalize(category);

    for (String validCatName : validCategories) {
      String normValidCatName = normalize(validCatName);
      if (normValidCatName.equals(normCatName)) {
        return validCatName;
      }
    }
    return category;
  }

  
  public ArrayList<AdvertisedContribution> parseContribList(File f) {
    ArrayList<AdvertisedContribution> outgoing = new ArrayList<AdvertisedContribution>();

    if (f != null && f.exists()) {
      String lines[] = PApplet.loadStrings(f);

      int start = 0;
      while (start < lines.length) {
//        // Only consider 'invalid' lines. These lines contain the type of
//        // software: library, tool, mode
//        if (!lines[start].contains("=")) {
        String type = lines[start];
        ContributionType contribType = ContributionType.fromName(type);
        if (contribType == null) {
          System.err.println("Error in contribution listing file on line " + (start+1));
          return outgoing;
        }

        // Scan forward for the next blank line
        int end = ++start;
        while (end < lines.length && !lines[end].equals("")) {
          end++;
        }

        int length = end - start;
        String[] contribLines = new String[length];
        System.arraycopy(lines, start, contribLines, 0, length);

        HashMap<String,String> contribParams = new HashMap<String,String>();
        Base.readSettings(f.getName(), contribLines, contribParams);
        
        outgoing.add(new AdvertisedContribution(contribType, contribParams));
        start = end + 1;
//        } else {
//          start++;
//        }
      }
    }
    return outgoing;
  }

  
  public boolean isDownloadingListing() {
    return downloadingListingLock.isLocked();
  }

  
  public static interface Filter {
    boolean matches(Contribution contrib);
  }
  

  static Comparator<Contribution> contribComparator = new Comparator<Contribution>() {
    public int compare(Contribution o1, Contribution o2) {
      return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
    }
  };
}
