/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2005-12 Ben Fry and Casey Reas

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
import java.net.URL;
import java.util.Random;

import javax.swing.JOptionPane;

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
 * The ID number also helps provide us a general idea of how many
 * people are using Processing, which helps us when writing grant
 * proposals and that kind of thing so that we can keep Processing free.
 */
public class UpdateCheck {
  Base base;
  String downloadURL = "http://processing.org/download/latest.txt";

  static final long ONE_DAY = 24 * 60 * 60 * 1000;


  public UpdateCheck(Base base) {
    this.base = base;
    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(30 * 1000);  // give the PDE time to get rolling
          updateCheck();
        } catch (Exception e) {
          // this can safely be ignored, too many instances where no net
          // connection is available, so we'll leave it well alone.
//          String msg = e.getMessage();
//          if (msg.contains("UnknownHostException")) {
//            // nah, do nothing.. this happens when not connected to the net
//          } else {
//            e.printStackTrace();
//          }
        }
      }
    }).start();
  }


  public void updateCheck() throws Exception {
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
                                    PApplet.nf(Base.REVISION, 4) + "\t" +
                                    System.getProperty("java.version") + "\t" +
                                    System.getProperty("java.vendor") + "\t" +
                                    System.getProperty("os.name") + "\t" +
                                    System.getProperty("os.version") + "\t" +
                                    System.getProperty("os.arch"));

    int latest = readInt(downloadURL + "?" + info);

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
      boolean offerToUpdateContributions = true;

      if (latest > Base.REVISION) {
        System.out.println("You are running Processing revision " +
                           Base.REVISION + ", the latest is " + latest + ".");
        // Assume the person is busy downloading the latest version
        offerToUpdateContributions = !promptToVisitDownloadPage();
      }

      if (offerToUpdateContributions) {
        // Wait for xml file to be downloaded and updates to come in.
        // (this should really be handled better).
        Thread.sleep(5 * 1000);
        if ((!base.libraryManagerFrame.hasAlreadyBeenOpened() &&
             base.libraryManagerFrame.hasUpdates()) ||
            (!base.toolManagerFrame.hasAlreadyBeenOpened() &&
             base.toolManagerFrame.hasUpdates()) ||
            (!base.modeManagerFrame.hasAlreadyBeenOpened() &&
             base.modeManagerFrame.hasUpdates())) {
          promptToOpenContributionManager();
        }
      }
    }
  }


  protected boolean promptToVisitDownloadPage() {
    String prompt =
      "A new version of Processing is available,\n" +
      "would you like to visit the Processing download page?";

    Object[] options = { "Yes", "No" };
    int result = JOptionPane.showOptionDialog(base.activeEditor,
                                              prompt,
                                              "Update",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);
    if (result == JOptionPane.YES_OPTION) {
      Base.openURL("http://processing.org/download/");
      return true;
    }

    return false;
  }


  protected boolean promptToOpenContributionManager() {
    String contributionPrompt =
      "There are updates available for some of the installed contributions,\n" +
      "would you like to open the the Contribution Manager now?";

    Object[] options = { "Yes", "No" };
    int result = JOptionPane.showOptionDialog(base.activeEditor,
                                              contributionPrompt,
                                              "Update",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);
    if (result == JOptionPane.YES_OPTION) {
      base.handleShowUpdates();
      return true;
    }

    return false;
  }


  protected int readInt(String filename) throws Exception {
    URL url = new URL(filename);
    InputStream stream = url.openStream();
    InputStreamReader isr = new InputStreamReader(stream);
    BufferedReader reader = new BufferedReader(isr);
    return Integer.parseInt(reader.readLine());
  }
}
