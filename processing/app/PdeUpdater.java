/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeUpdater - self-updater code.. when was the last this worked?
  Part of the Processing project - http://processing.org

  Copyright (c) 2001-03 
  Ben Fry, Massachusetts Institute of Technology and 
  Casey Reas, Interaction Design Institute Ivrea

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

import java.io.*;
import java.net.*;
import java.util.*;


public class PdeUpdater {
  PdeUpdater() {
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream("lib/pde.properties"));
    } catch (Exception e) {
      System.err.println("Error reading pde.properties");
      e.printStackTrace();
      System.exit(1);
    }

    // 'enabled' no longer valid because 
    // this is a separate updater application
    boolean updateEnabled = true;
    //(new Boolean(properties.getProperty("update.enabled", 
    //                                  "false"))).booleanValue();

    // check for updates from the server, if enabled
    //if (getBoolean("update.enabled", false)) {
    if (updateEnabled) {
      // open the update file to get the latest version
      long lastUpdate = 0;
      try {
        DataInputStream dis = 
          new DataInputStream(new FileInputStream("lib/version"));
        lastUpdate = dis.readLong();
        //System.out.println("pde.jar timestamp is " + lastUpdate);
      } catch (IOException e) { }

      //String baseUrl = get("update.url");
      String baseUrl = properties.getProperty("update.url");

      try {
        URL url = new URL(baseUrl + "version");
        URLConnection conn = url.openConnection();
        //conn.connect();

        //System.out.println("date of last update" + conn.getDate());
        long newDate = conn.getLastModified();
        //System.out.println("server timestamp is " + newDate);
        //System.out.println((newDate - lastUpdate) + "ms newer");
        if (newDate > lastUpdate) {
          System.out.println("new update available");

          DataOutputStream vos = 
            new DataOutputStream(new FileOutputStream("lib/version"));
            //new DataOutputStream(new FileOutputStream("lib/version.update"));
          vos.writeLong(newDate);
          vos.flush();
          vos.close();

          url = new URL(baseUrl + "pde.jar");
          conn = url.openConnection();

          // move the old pde.jar file out of the way
          //File pdeJar = new File("lib/pde.jar");
          //pdeJar.renameTo("lib/pde.old.jar");

          // download the new pde.jar file
          //FileOutputStream os = new FileOutputStream("lib/pde.jar.update");
          FileOutputStream os = new FileOutputStream("lib/pde.jar");
          //Object object = conn.getContent();
          //System.out.println(object);
          InputStream is = conn.getInputStream();
          copyStream(is, os);
          os.close();

          // if everything copied ok, rename new/old files
          // this probably needs to be way more bulletproof
          /*
          File file = new File("lib/version");
          if (file.exists()) 
            System.out.println(file.renameTo(new File("lib/version.old")));
          file = new File("lib/version.update");
          System.out.println(file.renameTo(new File("lib/version")));

          file = new File("lib/pde.jar");
          file.delete();
          //System.out.println(file.renameTo(new File("lib/pde.jar.old")));
          file = new File("lib/pde.jar.update");
          System.out.println(file.renameTo(new File("lib/pde.jar")));
          */

          // restart or relaunch
          //System.out.println("done copying new version, restart");
          //System.exit(0);

        }

        /*
          try {
            Class c = Class.forName("PdeApplication");
            Object o = c.newInstance();
          //PdeApplication.main(null);
          } catch (Exception e) {
            System.err.println("update failed");
            e.printStackTrace();
          }
        */
        // mac mrj is not smart enough to exit the applicaiton here
        System.exit(0);

      } catch (IOException e1) {
        e1.printStackTrace();

        //} catch (MalformedURLException e2) {
        //e2.printStackTrace();
      }
    }
  }


  static public void copyStream(InputStream input, OutputStream output
                                /*int padding, long length*/) 
    throws IOException {
    byte[] buffer = new byte[4096];
    int count;
    int amount;

    int length = Integer.MAX_VALUE;
    // if length is not actually known, the function will still break
    // in the correct spot, so just set to some enormous value
    //if (length == -1) {
    //length = Integer.MAX_VALUE;
    //}
    //if (padding != 0) {
    //input.skip((int) padding);
    //}

    while (true) {
      amount = (length < 4096) ? (int) length : 4096;
      //System.err.print(amount + " ");
      count = input.read(buffer, 0, amount);
      //System.out.println("got " + count);
      if (count == -1)
        break;

      output.write(buffer, 0, count);
      length -= count;        // used to be amount... bug?
      if (length == 0)
        break;
    }
    output.flush();
  }


  static public void main(String args[]) {
    new PdeUpdater();
  }
}
