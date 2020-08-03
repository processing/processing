/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Server - basic network server implementation
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-2007 Ben Fry and Casey Reas
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


/**
 * ( begin auto-generated from Server.xml )
 * 
 * A server sends and receives data to and from its associated clients 
 * (other programs connected to it). When a server is started, it begins 
 * listening for connections on the port specified by the <b>port</b> 
 * parameter. Computers have many ports for transferring data and some are 
 * commonly used so be sure to not select one of these. For example, web 
 * servers usually use port 80 and POP mail uses port 110.
 * 
 * ( end auto-generated )
 * @webref net
 * @usage application
 * @brief The server class is used to create server objects which send and receives data to and from its associated clients (other programs connected to it). 
 * @instanceName server  	any variable of type Server
 */
public class Server implements Runnable {
  PApplet parent;
  Method serverEventMethod;

  volatile Thread thread;
  ServerSocket server;
  int port;

  protected final Object clientsLock = new Object[0];
  /** Number of clients currently connected. */
  public int clientCount;
  /** Array of client objects, useful length is determined by clientCount. */
  public Client[] clients;

  
  /**
   * @param parent typically use "this"
   * @param port port used to transfer data
   */
  public Server(PApplet parent, int port) {
    this(parent, port, null);
  }
    
    
  /**
   * @param host when multiple NICs are in use, the ip (or name) to bind from 
   */
  public Server(PApplet parent, int port, String host) {
    this.parent = parent;
    this.port = port;

    try {
      if (host == null) {
        server = new ServerSocket(this.port);
      } else {
        server = new ServerSocket(this.port, 10, InetAddress.getByName(host));
      }
      //clients = new Vector();
      clients = new Client[10];

      thread = new Thread(this);
      thread.start();

      parent.registerMethod("dispose", this);

      // reflection to check whether host applet has a call for
      // public void serverEvent(Server s, Client c);
      // which is called when a new guy connects
      try {
        serverEventMethod =
          parent.getClass().getMethod("serverEvent", Server.class, Client.class);
      } catch (Exception e) {
        // no such method, or an error.. which is fine, just ignore
      }

    } catch (IOException e) {
      //e.printStackTrace();
      thread = null;
      throw new RuntimeException(e);
      //errorMessage("<init>", e);
    }
  }


  /**
   * ( begin auto-generated from Server_disconnect.xml )
   * 
   * Disconnect a particular client.
   * 
   * ( end auto-generated )
   * @brief Disconnect a particular client.
   * @webref server:server
   * @param client the client to disconnect
   */
  public void disconnect(Client client) {
    client.stop();
    synchronized (clientsLock) {
      int index = clientIndex(client);
      if (index != -1) {
        removeIndex(index);
      }
    }
  }
  
  
  protected void removeIndex(int index) {
    synchronized (clientsLock) {
      clientCount--;
      // shift down the remaining clients
      for (int i = index; i < clientCount; i++) {
        clients[i] = clients[i + 1];
      }
      // mark last empty var for garbage collection
      clients[clientCount] = null;
    }
  }
  
  
  protected void disconnectAll() {
    synchronized (clientsLock) {
      for (int i = 0; i < clientCount; i++) {
        try {
          clients[i].stop();
        } catch (Exception e) {
          // ignore
        }
        clients[i] = null;
      }
      clientCount = 0;
    }
  }
  
  
  protected void addClient(Client client) {
    synchronized (clientsLock) {
      if (clientCount == clients.length) {
        clients = (Client[]) PApplet.expand(clients);
      }
      clients[clientCount++] = client;
    }
  }
  
  
  protected int clientIndex(Client client) {
    synchronized (clientsLock) {
      for (int i = 0; i < clientCount; i++) {
        if (clients[i] == client) {
          return i;
        }
      }
      return -1;
    }
  }

  
  /**
   * ( begin auto-generated from Server_active.xml )
   * 
   * Returns true if this server is still active and hasn't run
   * into any trouble.
   * 
   * ( end auto-generated )
   * @webref server:server
   * @brief Return true if this server is still active.
   */
  public boolean active() {
    return thread != null;
  }
  
  
  static public String ip() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return null;
    }
  }


  // the last index used for available. can't just cycle through
  // the clients in order from 0 each time, because if client 0 won't
  // shut up, then the rest of the clients will never be heard from.
  int lastAvailable = -1;

  /**
   * ( begin auto-generated from Server_available.xml )
   * 
   * Returns the next client in line with a new message.
   * 
   * ( end auto-generated )
   * @brief Returns the next client in line with a new message.
   * @webref server
   * @usage application
   */
  public Client available() {
    synchronized (clientsLock) {
      int index = lastAvailable + 1;
      if (index >= clientCount) index = 0;

      for (int i = 0; i < clientCount; i++) {
        int which = (index + i) % clientCount;
        Client client = clients[which];
        //Check for valid client
        if (!client.active()){
          removeIndex(which);  //Remove dead client
          i--;                 //Don't skip the next client
          //If the client has data make sure lastAvailable
          //doesn't end up skipping the next client
          which--;
          //fall through to allow data from dead clients
          //to be retreived.
        }
        if (client.available() > 0) {
          lastAvailable = which;
          return client;
        }
      }
    }
    return null;
  }


  /**
   * ( begin auto-generated from Server_stop.xml )
   * 
   * Disconnects all clients and stops the server.
   * 
   * ( end auto-generated )
   * <h3>Advanced</h3>
   * Use this to shut down the server if you finish using it while your applet 
   * is still running. Otherwise, it will be automatically be shut down by the 
   * host PApplet using dispose(), which is identical. 
   * @brief Disconnects all clients and stops the server.
   * @webref server
   * @usage application
   */
  public void stop() {
    dispose();
  }


  /**
   * Disconnect all clients and stop the server: internal use only.
   */
  public void dispose() {
    thread = null;

    if (clients != null) {
      disconnectAll();
      clientCount = 0;
      clients = null;
    }

    try {
      if (server != null) {
        server.close();
        server = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  @Override
  public void run() {
    while (Thread.currentThread() == thread) {
      try {
        Socket socket = server.accept();
        Client client = new Client(parent, socket);
        synchronized (clientsLock) {
          addClient(client);
          if (serverEventMethod != null) {
            try {
              serverEventMethod.invoke(parent, this, client);
            } catch (Exception e) {
              System.err.println("Disabling serverEvent() for port " + port);
              Throwable cause = e;
              // unwrap the exception if it came from the user code
              if (e instanceof InvocationTargetException && e.getCause() != null) {
                cause = e.getCause();
              }
              cause.printStackTrace();
              serverEventMethod = null;
            }
          }
        }
      } catch (SocketException e) {
        //thrown when server.close() is called and server is waiting on accept
        System.err.println("Server SocketException: " + e.getMessage());
        thread = null;
      } catch (IOException e) {
        //errorMessage("run", e);
        e.printStackTrace();
        thread = null;
      }
    }
  }


  /**
   * ( begin auto-generated from Server_write.xml )
   * 
   * Writes a value to all the connected clients. It sends bytes out from the 
   * Server object.
   * 
   * ( end auto-generated )
   * @webref server
   * @brief Writes data to all connected clients
   * @param data data to write
   */
  public void write(int data) {  // will also cover char
    synchronized (clientsLock) {
      int index = 0;
      while (index < clientCount) {
        if (clients[index].active()) {
          clients[index].write(data);
          index++;
        } else {
          removeIndex(index);
        }
      }
    }
  }
  

  public void write(byte data[]) {
    synchronized (clientsLock) {
      int index = 0;
      while (index < clientCount) {
        if (clients[index].active()) {
          clients[index].write(data);
          index++;
        } else {
          removeIndex(index);
        }
      }
    }
  }
  

  public void write(String data) {
    synchronized (clientsLock) {
      int index = 0;
      while (index < clientCount) {
        if (clients[index].active()) {
          clients[index].write(data);
          index++;
        } else {
          removeIndex(index);
        }
      }
    }
  }
}
