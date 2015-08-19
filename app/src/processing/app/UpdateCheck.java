/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-15 Ben Fry and Casey Reas

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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.Random;

import javax.swing.JOptionPane;

import processing.app.contrib.ContributionManager;
import processing.core.PApplet;


/**
 * Threaded class to check for updates in the background.
 * <P>
 * This is the class that handles the mind control and stuff for
 * spying on our users and stealing their personal information.
 * A random ID number is generated for each user, and hits the server
 * to check for updates. Also included is the operating system and
 * its version and the version of Java being used to run Processing.
 * <P>
 * Aside from the privacy invasion of knowing that an anonymous Processing
 * user opened the software at one time during a 24-hour period somewhere
 * in the world, we use the ID number to give us a general idea of how many
 * people are using Processing, which helps us when writing grant proposals
 * and that kind of thing so that we can keep Processing free. The numbers
 * are also sometimes used in ugly charts when Ben and Casey present.
 */
public class UpdateCheck {
  private final Base base;

  static private final String DOWNLOAD_URL = "http://processing.org/download/";
  static private final String LATEST_URL = "http://processing.org/download/latest.txt";

  static private final long ONE_DAY = 24 * 60 * 60 * 1000;

  static boolean allowed;


  public UpdateCheck(Base base) {
    this.base = base;

    if (isAllowed()) {
      new Thread(new Runnable() {
        public void run() {
          try {
            Thread.sleep(5 * 1000);  // give the PDE time to get rolling
            updateCheck();

          } catch (Exception e) {
            // This can safely be ignored, too many situations where no net
            // connection is available that behave in strange ways.
            // Covers likely IOException, InterruptedException, and any others.
          }
        }
      }, "Update Checker").start();
    }
  }


  public void updateCheck() throws IOException, InterruptedException {
    // generate a random id in case none exists yet
    Random r = new Random();
    long id = r.nextLong();

    String idString = Preferences.get("update.id");
    if (idString != null) {
      id = Long.parseLong(idString);
    } else {
      Preferences.set("update.id", String.valueOf(id));
    }

    String info = PApplet.urlEncode(id + "\t" +
                                    PApplet.nf(Base.getRevision(), 4) + "\t" +
                                    System.getProperty("java.version") + "\t" +
                                    System.getProperty("java.vendor") + "\t" +
                                    System.getProperty("os.name") + "\t" +
                                    System.getProperty("os.version") + "\t" +
                                    System.getProperty("os.arch"));

    int latest = readInt(LATEST_URL + "?" + info);

    String lastString = Preferences.get("update.last");
    long now = System.currentTimeMillis();
    if (lastString != null) {
      long when = Long.parseLong(lastString);
      if (now - when < ONE_DAY) {
        // don't annoy the shit outta people
        return;
      }
    }
    Preferences.set("update.last", String.valueOf(now));

    if (base.activeEditor != null) {
//      boolean offerToUpdateContributions = true;

      if (latest > Base.getRevision()) {
        System.out.println("You are running Processing revision 0" +
                           Base.getRevision() + ", the latest build is 0" +
                           latest + ".");
        // Assume the person is busy downloading the latest version
//        offerToUpdateContributions = !promptToVisitDownloadPage();
        promptToVisitDownloadPage();
      }

      /*
      if (offerToUpdateContributions) {
        // Wait for xml file to be downloaded and updates to come in.
        // (this should really be handled better).
        Thread.sleep(5 * 1000);
        if ((!base.contributionManagerFrame.hasAlreadyBeenOpened()
          && (base.contributionManagerFrame.hasUpdates(base)))){
          promptToOpenContributionManager();
        }
      }
      */
    }
  }


  protected boolean promptToVisitDownloadPage() {
    String prompt = Language.text("update_check.updates_available.core");

    Object[] options = { Language.text("prompt.yes"), Language.text("prompt.no") };
    int result = JOptionPane.showOptionDialog(base.activeEditor,
                                              prompt,
                                              Language.text("update_check"),
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);
    if (result == JOptionPane.YES_OPTION) {
      Platform.openURL(DOWNLOAD_URL);
      return true;
    }

    return false;
  }


  protected boolean promptToOpenContributionManager() {
    String contributionPrompt =
      Language.text("update_check.updates_available.contributions");

    Object[] options = {
      Language.text("prompt.yes"), Language.text("prompt.no")
    };
    int result = JOptionPane.showOptionDialog(base.activeEditor,
                                              contributionPrompt,
                                              Language.text("update_check"),
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);
    if (result == JOptionPane.YES_OPTION) {
      ContributionManager.openUpdates(base.getActiveEditor());
      return true;
    }

    return false;
  }


  protected int readInt(String filename) throws IOException {
    URL url = new URL(filename);
    InputStream stream = url.openStream();
    InputStreamReader isr = new InputStreamReader(stream);
    BufferedReader reader = new BufferedReader(isr);
    return Integer.parseInt(reader.readLine());
  }


  static public boolean isAllowed() {
    // Disable update checks for the paranoid
    return Preferences.getBoolean("update.check");
  }
}
