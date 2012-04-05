/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2011-12 Ben Fry and Casey Reas

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

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
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.SwingUtilities;

import processing.core.PApplet;


/**
 * Class that handles a small server that prevents multiple instances of
 * Processing from running simultaneously. If there's already an instance
 * running, it'll handle opening a new empty sketch, or any files that had
 * been passed in on the command line.
 *
 * @author Peter Kalauskas, Ben Fry
 */
public class SingleInstance {
  static final String SERVER_PORT = "instance_server.port";
  static final String SERVER_KEY = "instance_server.key";


  /**
   * Returns true if there's an instance of Processing already running.
   * Will not return true unless this code was able to successfully
   * contact the already running instance to have it launch sketches.
   * @param filename Path to the PDE file that was opened, null if double-clicked
   * @return true if successfully launched on the other instance
   */
  static boolean alreadyRunning(String[] args) {
    return (Preferences.get(SERVER_PORT) != null &&
            sendArguments(args, 5000));
  }


//  static void startServer(final Platform platform) {
  static void startServer(final Base base) {
    try {
      final ServerSocket ss = new ServerSocket(0, 0, InetAddress.getByName(null));
      Preferences.set(SERVER_PORT, "" + ss.getLocalPort());
      final String key = "" + Math.random();
      Preferences.set(SERVER_KEY, key);
      Preferences.save();

      new Thread(new Runnable() {
        public void run() {
          while (true) {
            try {
              Socket s = ss.accept();  // blocks (sleeps) until connection
              final BufferedReader reader = PApplet.createReader(s.getInputStream());
              String receivedKey = reader.readLine();
              Base.log(this, "key is " + key + ", received is " + receivedKey);
//              Base.log(this, "platform base is " + platform.base);

//              if (platform.base != null) {
              if (key.equals(receivedKey)) {
                SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    try {
                      Base.log(this, "about to read line");
                      String path = reader.readLine();
                      if (path == null) {
                        // Because an attempt was made to launch the PDE again,
                        // throw the user a bone by at least opening a new
                        // Untitled window for them.
                        Base.log(this, "opening new empty sketch");
//                        platform.base.handleNew();
                        base.handleNew();

                      } else {
                        // loop through the sketches that were passed in
                        do {
                          Base.log(this, "calling open with " + path);
//                        platform.base.handleOpen(filename);
                          base.handleOpen(path);
                          path = reader.readLine();
                        } while (path != null);
                      }
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }
                });
              } else {
                Base.log(this, "keys do not match");
              }
//              }
            } catch (IOException e) {
              Base.log("SingleInstance error while listening", e);
            }
          }
        }
      }).start();

    } catch (IOException e) {
      Base.log("Could not create single instance server.", e);
    }
  }


  static boolean sendArguments(String[] args, long timeout) {
    try {
      //int port = Integer.parseInt(Preferences.get("server.port"));
      //String key = Preferences.get("server.key");
      int port = Preferences.getInteger(SERVER_PORT);
      String key = Preferences.get(SERVER_KEY);

      long endTime = System.currentTimeMillis() + timeout;

      Socket socket = null;
      while (socket == null && System.currentTimeMillis() < endTime) {
        try {
          socket = new Socket(InetAddress.getByName(null), port);
        } catch (Exception ioe) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException ie) {
            Thread.yield();
          }
        }
      }

      if (socket != null) {
        PrintWriter writer = PApplet.createWriter(socket.getOutputStream());
//        bw.write(key + "\n");
        writer.println(key);
        for (String arg : args) {
//        if (filename != null) {
////          bw.write(filename + "\n");
//          writer.println(filename);
          writer.println(arg);
        }
//        bw.close();
        writer.flush();
        writer.close();
        return true;
      }
    } catch (IOException e) {
      System.err.println("Error sending commands to other instance.");
      e.printStackTrace();
    }
    return false;
  }
}