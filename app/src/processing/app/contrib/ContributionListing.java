/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2013-16 The Processing Foundation
  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License along
  with this program; if not, write to the Free Software Foundation, Inc.
  59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package processing.app.contrib;

import java.awt.EventQueue;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import processing.app.Base;
import processing.app.Library;
import processing.app.Util;
import processing.core.PApplet;
import processing.data.StringDict;


public class ContributionListing {
  static volatile ContributionListing singleInstance;

  /** Stable URL that will redirect to wherever the file is hosted */
  static final String LISTING_URL = "http://download.processing.org/contribs";
  static final String LOCAL_FILENAME = "contribs.txt";

  /** Location of the listing file on disk, will be read and written. */
  File listingFile;

  List<ChangeListener> listeners;
  List<AvailableContribution> advertisedContributions;
  Map<String, List<Contribution>> librariesByCategory;
  Map<String, Contribution> librariesByImportHeader;
  // TODO: Every contribution is getting added twice
  // and nothing is replaced ever.
  Set<Contribution> allContributions;
  boolean listDownloaded;
  boolean listDownloadFailed;
  ReentrantLock downloadingListingLock;


  private ContributionListing() {
    listeners = new ArrayList<>();
    advertisedContributions = new ArrayList<>();
    librariesByCategory = new HashMap<>();
    librariesByImportHeader = new HashMap<>();
    allContributions = new LinkedHashSet<>();
    downloadingListingLock = new ReentrantLock();

    //listingFile = Base.getSettingsFile("contributions.txt");
    listingFile = Base.getSettingsFile(LOCAL_FILENAME);
    listingFile.setWritable(true, false);
    if (listingFile.exists()) {
      setAdvertisedList(listingFile);
    }
  }


  static public ContributionListing getInstance() {
    if (singleInstance == null) {
      synchronized (ContributionListing.class) {
        if (singleInstance == null) {
          singleInstance = new ContributionListing();
        }
      }
    }
    return singleInstance;
  }


  private void setAdvertisedList(File file) {
    listingFile = file;

    advertisedContributions.clear();
    advertisedContributions.addAll(parseContribList(listingFile));
    for (Contribution contribution : advertisedContributions) {
      addContribution(contribution);
    }
  }


  /**
   * Adds the installed libraries to the listing of libraries, replacing any
   * pre-existing libraries by the same name as one in the list.
   */
  protected void updateInstalledList(List<Contribution> installed) {
    for (Contribution contribution : installed) {
      Contribution existingContribution = getContribution(contribution);
      if (existingContribution != null) {
        replaceContribution(existingContribution, contribution);
      //} else if (contribution != null) {  // 130925 why would this be necessary?
      } else {
        addContribution(contribution);
      }
    }
  }


  protected void replaceContribution(Contribution oldLib, Contribution newLib) {
    if (oldLib != null && newLib != null) {
      for (String category : oldLib.getCategories()) {
        if (librariesByCategory.containsKey(category)) {
          List<Contribution> list = librariesByCategory.get(category);

          for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == oldLib) {
              list.set(i, newLib);
            }
          }
        }
      }

      if (oldLib.getImports() != null) {
        for (String importName : oldLib.getImports()) {
          if (getLibrariesByImportHeader().containsKey(importName)) {
            getLibrariesByImportHeader().put(importName, newLib);
          }
        }
      }

      allContributions.remove(oldLib);
      allContributions.add(newLib);

      notifyChange(oldLib, newLib);
    }
  }


  private void addContribution(Contribution contribution) {
    if (contribution.getImports() != null) {
      for (String importName : contribution.getImports()) {
        getLibrariesByImportHeader().put(importName, contribution);
      }
    }
    for (String category : contribution.getCategories()) {
      if (librariesByCategory.containsKey(category)) {
        List<Contribution> list = librariesByCategory.get(category);
        list.add(contribution);
        Collections.sort(list, COMPARATOR);

      } else {
        ArrayList<Contribution> list = new ArrayList<>();
        list.add(contribution);
        librariesByCategory.put(category, list);
      }
      allContributions.add(contribution);
      notifyAdd(contribution);
    }
  }


  protected void removeContribution(Contribution contribution) {
    for (String category : contribution.getCategories()) {
      if (librariesByCategory.containsKey(category)) {
        librariesByCategory.get(category).remove(contribution);
      }
    }
    if (contribution.getImports() != null) {
      for (String importName : contribution.getImports()) {
        getLibrariesByImportHeader().remove(importName);
      }
    }
    allContributions.remove(contribution);
    notifyRemove(contribution);
  }


  private Contribution getContribution(Contribution contribution) {
    for (Contribution c : allContributions) {
      if (c.getName().equals(contribution.getName()) &&
          c.getType() == contribution.getType()) {
        return c;
      }
    }
    return null;
  }


  protected AvailableContribution getAvailableContribution(Contribution info) {
    synchronized (advertisedContributions) {
      for (AvailableContribution advertised : advertisedContributions) {
        if (advertised.getType() == info.getType() &&
            advertised.getName().equals(info.getName())) {
          return advertised;
        }
      }
    }
    return null;
  }


  protected Set<String> getCategories(Contribution.Filter filter) {
    Set<String> outgoing = new HashSet<>();

    Set<String> categorySet = librariesByCategory.keySet();
    for (String categoryName : categorySet) {
      for (Contribution contrib : librariesByCategory.get(categoryName)) {
        if (filter.matches(contrib)) {
          // TODO still not sure why category would be coming back null [fry]
          // http://code.google.com/p/processing/issues/detail?id=1387
          if (categoryName != null && !categoryName.trim().isEmpty()) {
            outgoing.add(categoryName);
          }
          break;
        }
      }
    }
    return outgoing;
  }


  public boolean matches(Contribution contrib, String typed) {
    int colon = typed.indexOf(":");
    if (colon != -1) {
      String isText = typed.substring(0, colon);
      String property = typed.substring(colon + 1);

      // Chances are the person is still typing the property, so rather than
      // make the list flash empty (because nothing contains "is:" or "has:",
      // just return true.
      if (!isProperty(property)) {
        return true;
      }

      if ("is".equals(isText) || "has".equals(isText)) {
        return hasProperty(contrib, typed.substring(colon + 1));
      } else if ("not".equals(isText)) {
        return !hasProperty(contrib, typed.substring(colon + 1));
      }
    }

    typed = ".*" + typed.toLowerCase() + ".*";

    return (matchField(contrib.getName(), typed) ||
            matchField(contrib.getAuthorList(), typed) ||
            matchField(contrib.getSentence(), typed) ||
            matchField(contrib.getParagraph(), typed) ||
            contrib.hasCategory(typed));
  }


  static private boolean matchField(String field, String typed) {
    return (field != null) &&
      removeAccents(field.toLowerCase()).matches(typed);
  }


  // TODO is this removing characters with accents, not ascii normalizing them? [fry]
  static private String removeAccents(String str) {
    String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(nfdNormalizedString).replaceAll("");
  }


  static private boolean isProperty(String property) {
    return property.startsWith("updat") || property.startsWith("upgrad")
        || property.startsWith("instal") && !property.startsWith("installabl")
        || property.equals("tool") || property.startsWith("lib")
        || property.equals("mode") || property.equals("compilation");
  }


  /**
   * Returns true if the contribution fits the given property, false otherwise.
   * If the property is invalid, returns false.
   */
  private boolean hasProperty(Contribution contrib, String property) {
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
    }
    if (property.equals("mode")) {
      return contrib.getType() == ContributionType.MODE;
    }
    return false;
  }


  /*
  protected List<Contribution> listCompatible(List<Contribution> contribs, boolean filter) {
    List<Contribution> filteredList =
      new ArrayList<Contribution>(contribs);

    if (filter) {
      Iterator<Contribution> it = filteredList.iterator();
      while (it.hasNext()) {
        Contribution libInfo = it.next();
        if (!libInfo.isCompatible(Base.getRevision())) {
          it.remove();
        }
      }
    }
    return filteredList;
  }
  */


  private void notifyRemove(Contribution contribution) {
    for (ChangeListener listener : listeners) {
      listener.contributionRemoved(contribution);
    }
  }


  private void notifyAdd(Contribution contribution) {
    for (ChangeListener listener : listeners) {
      listener.contributionAdded(contribution);
    }
  }


  private void notifyChange(Contribution oldLib, Contribution newLib) {
    for (ChangeListener listener : listeners) {
      listener.contributionChanged(oldLib, newLib);
    }
  }


  protected void addListener(ChangeListener listener) {
    for (Contribution contrib : allContributions) {
      listener.contributionAdded(contrib);
    }
    listeners.add(listener);
  }


  /**
   * Starts a new thread to download the advertised list of contributions.
   * Only one instance will run at a time.
   */
  public void downloadAvailableList(final Base base,
                                    final ContribProgressMonitor progress) {

    // TODO: replace with SwingWorker [jv]
    new Thread(new Runnable() {
      public void run() {
        downloadingListingLock.lock();

        try {
          URL url = new URL(LISTING_URL);
          // testing port
//          url = new URL("http", "download.processing.org", 8989, "/contribs");

//          "http://download.processing.org/contribs";
//          System.out.println(url);
//          final String contribInfo =
//            base.getInstalledContribsInfo();
//            "?id=" + Preferences.get("update.id") +
//            "&" + base.getInstalledContribsInfo();
//          url = new URL(LISTING_URL + "?" + contribInfo);
//          System.out.println(contribInfo.length() + " " + contribInfo);

          File tempContribFile = Base.getSettingsFile("contribs.tmp");
          tempContribFile.setWritable(true, false);
          ContributionManager.download(url, base.getInstalledContribsInfo(),
                                       tempContribFile, progress);
          if (!progress.isCanceled() && !progress.isError()) {
            if (listingFile.exists()) {
              listingFile.delete();  // may silently fail, but below may still work
            }
            if (tempContribFile.renameTo(listingFile)) {
              listDownloaded = true;
              listDownloadFailed = false;
              try {
                // TODO: run this in SwingWorker done() [jv]
                EventQueue.invokeAndWait(new Runnable() {
                  @Override
                  public void run() {
                    setAdvertisedList(listingFile);
                    base.setUpdatesAvailable(countUpdates(base));
                  }
                });
              } catch (InterruptedException e) {
                e.printStackTrace();
              } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                  throw (RuntimeException) cause;
                } else {
                  cause.printStackTrace();
                }
              }
            } else {
              listDownloadFailed = true;
            }
          }

        } catch (MalformedURLException e) {
          progress.error(e);
          progress.finished();
        } finally {
          downloadingListingLock.unlock();
        }
      }
    }, "Contribution List Downloader").start();
  }


  protected boolean hasUpdates(Contribution contribution) {
    if (contribution.isInstalled()) {
      Contribution advertised = getAvailableContribution(contribution);
      if (advertised == null) {
        return false;
      }
      return advertised.getVersion() > contribution.getVersion()
        && advertised.isCompatible(Base.getRevision());
    }
    return false;
  }


  protected String getLatestPrettyVersion(Contribution contribution) {
    Contribution newestContrib = getAvailableContribution(contribution);
    if (newestContrib == null) {
      return null;
    }
    return newestContrib.getPrettyVersion();
  }


  protected boolean hasDownloadedLatestList() {
    return listDownloaded;
  }


  protected boolean hasListDownloadFailed() {
    return listDownloadFailed;
  }


  private List<AvailableContribution> parseContribList(File file) {
    List<AvailableContribution> outgoing =
      new ArrayList<>();

    if (file != null && file.exists()) {
      String[] lines = PApplet.loadStrings(file);

      int start = 0;
      while (start < lines.length) {
        String type = lines[start];
        ContributionType contribType = ContributionType.fromName(type);
        if (contribType == null) {
          System.err.println("Error in contribution listing file on line " + (start+1));
          // Scan forward for the next blank line
          int end = ++start;
          while (end < lines.length && !lines[end].trim().isEmpty()) {
            end++;
          }
          start = end + 1;

        } else {
          // Scan forward for the next blank line
          int end = ++start;
          while (end < lines.length && !lines[end].trim().isEmpty()) {
            end++;
          }

          String[] contribLines = PApplet.subset(lines, start, end-start);
          StringDict contribParams = Util.readSettings(file.getName(), contribLines);
          outgoing.add(new AvailableContribution(contribType, contribParams));
          start = end + 1;
        }
      }
    }
    return outgoing;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * TODO This needs to be called when the listing loads, and also whenever
   * the contribs list has been updated (for whatever reason). In addition,
   * the caller (presumably Base) should update all Editor windows with the
   * correct information on the number of items available.
   * @return The number of contributions that have available updates.
   */
  public int countUpdates(Base base) {
    int count = 0;
    for (ModeContribution mc : base.getModeContribs()) {
      if (hasUpdates(mc)) {
        count++;
      }
    }
    for (Library lib : base.getActiveEditor().getMode().contribLibraries) {
      if (hasUpdates(lib)) {
        count++;
      }
    }
    for (Library lib : base.getActiveEditor().getMode().coreLibraries) {
      if (hasUpdates(lib)) {
        count++;
      }
    }
    for (ToolContribution tc : base.getToolContribs()) {
      if (hasUpdates(tc)) {
        count++;
      }
    }
    for (ExamplesContribution ec : base.getExampleContribs()) {
      if (hasUpdates(ec)) {
        count++;
      }
    }
    return count;
  }


  /** Used by JavaEditor to auto-import */
  public Map<String, Contribution> getLibrariesByImportHeader() {
    return librariesByImportHeader;
  }


  static public Comparator<Contribution> COMPARATOR = Comparator.comparing(o -> o.getName().toLowerCase());


  public interface ChangeListener {
    public void contributionAdded(Contribution Contribution);
    public void contributionRemoved(Contribution Contribution);
    public void contributionChanged(Contribution oldLib, Contribution newLib);
  }
}
