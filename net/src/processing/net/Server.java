/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PServer - basic network server implementation
  Part of the Processing project - http://processing.org

  Copyright (c) 2004 Ben Fry
  The previous version of this code was developed by Hernando Barragan

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.net;
import processing.core.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;


public class Server implements Runnable {

  PApplet parent;
  Method serverEventMethod;

  Thread thread;
  ServerSocket server;
  int port;
  Vector clients;  // people payin the bills


  public Server(PApplet parent, int port) {
    this.parent = parent;
    this.port = port;

    //parent.attach(this);

    try {
      server = new ServerSocket(this.port);
      clients = new Vector();

      thread = new Thread(this);
      thread.start();

      parent.registerDispose(this);

      // reflection to check whether host applet has a call for
      // public void serverEvent(Server s, Client c);
      // which is called when a new guy connects
      try {
        serverEventMethod =
          parent.getClass().getMethod("serverEvent",
                                      new Class[] { Server.class,
                                                    Client.class });
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (IOException e) {
      errorMessage("<init>", e);
    }
  }


  /**
   * Disconnect a particular client.
   */
  public void disconnect(Client client) {
    //client.stop();
    client.dispose();
    clients.removeElement(client);
  }


  // the last index used for available. can't just cycle through
  // the clients in order from 0 each time, because if client 0 won't
  // shut up, then the rest of the clients will never be heard from.
  int lastAvailable = -1;

  /**
   * Returns the next client in line that has something to say.
   */
  public Client available() {
    synchronized (clients) {
      int clientCount = clients.size();
      int index = lastAvailable + 1;
      if (index >= clientCount) index = 0;

      for (int i = 0; i < clientCount; i++) {
        int which = (index + i) % clientCount;
        Client client = (Client) clients.elementAt(which);
        if (client.available() > 0) {
          lastAvailable = which;
          return client;
        }
      }
    }
    return null;
  }


  /**
   * Disconnect all clients and stop the server.
   * <P>
   * Use this to shut down the server if you finish using it
   * while your applet is still running. Otherwise, it will be
   * automatically be shut down by the host PApplet
   * (using dispose, which is identical)
   */
  public void stop() {
    dispose();
  }


  /**
   * Disconnect all clients and stop the server: internal use only.
   */
  public void dispose() {
    try {
      thread = null;

      if (clients != null) {
        Enumeration en = clients.elements();
        while (en.hasMoreElements()) {
          disconnect((Client) en.nextElement());
        }
        clients = null;
      }

      if (server != null) {
        server.close();
        server = null;
      }

    } catch (IOException e) {
      errorMessage("stop", e);
    }
  }


  public void run() {
    while (Thread.currentThread() == thread) {
      try {
        Socket socket = server.accept();
        Client client = new Client(parent, socket);
        synchronized (clients) {
          clients.addElement(client);
          if (serverEventMethod != null) {
            try {
              serverEventMethod.invoke(parent, new Object[] { this, client });
            } catch (Exception e) {
              System.err.println("error, disabling serverEvent() " +
                                 " for port " + port);
              e.printStackTrace();
              serverEventMethod = null;
            }
          }
        }
      } catch (IOException e) {
        errorMessage("run", e);
      }
      try {
        Thread.sleep(8);
      } catch (InterruptedException ex) { }
    }
  }


  /**
   * Write a value to all the connected clients.
   * See Client.write() for operational details.
   */
  public void write(int what) {  // will also cover char
    Enumeration en = clients.elements();
    while (en.hasMoreElements()) {
      Client client = (Client) en.nextElement();
      client.write(what);
    }
  }


  /**
   * Write a byte array to all the connected clients.
   * See Client.write() for operational details.
   */
  public void write(byte bytes[]) {
    Enumeration en = clients.elements();
    while (en.hasMoreElements()) {
      Client client = (Client) en.nextElement();
      client.write(bytes);
    }
  }


  /**
   * Write a String to all the connected clients.
   * See Client.write() for operational details.
   */
  public void write(String what) {
    Enumeration en = clients.elements();
    while (en.hasMoreElements()) {
      Client client = (Client) en.nextElement();
      client.write(what);
    }
  }


  /**
   * General error reporting, all corraled here just in case
   * I think of something slightly more intelligent to do.
   */
  public void errorMessage(String where, Exception e) {
    parent.die("Error inside Server." + where + "()", e);
    //System.err.println("Error inside Server." + where + "()");
    //e.printStackTrace(System.err);
  }
}
