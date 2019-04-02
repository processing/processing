/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org
Copyright (c) 2012-15 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation, Inc.
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package processing.mode.java.tweak;

import java.net.*;
import java.nio.ByteBuffer;


public class TweakClient {
  private DatagramSocket socket;
  private InetAddress address;
  private boolean initialized;
  private int sketchPort;

  static final int VAR_INT = 0;
  static final int VAR_FLOAT = 1;
  static final int SHUTDOWN = 0xffffffff;


  public TweakClient(int sketchPort) {
    this.sketchPort = sketchPort;

    try {
      socket = new DatagramSocket();
      // only local sketch is allowed
      address = InetAddress.getByName("127.0.0.1");
      initialized = true;

    } catch (SocketException e) {
      initialized = false;

    } catch (UnknownHostException e) {
      socket.close();
      initialized = false;

    } catch (SecurityException e) {
      socket.close();
      initialized = false;
    }
  }


  public void shutdown() {
    if (initialized) {
      // send shutdown to the sketch
      sendShutdown();
      initialized = false;
    }
  }


  public boolean sendInt(int index, int val) {
    if (initialized) {
      try {
        byte[] buf = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.putInt(0, VAR_INT);
        bb.putInt(4, index);
        bb.putInt(8, val);
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, sketchPort);
        socket.send(packet);
        return true;

      } catch (Exception e) { }
    }
    return false;
  }


  public boolean sendFloat(int index, float val) {
    if (initialized) {
      try {
        byte[] buf = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.putInt(0, VAR_FLOAT);
        bb.putInt(4, index);
        bb.putFloat(8, val);
        socket.send(new DatagramPacket(buf, buf.length, address, sketchPort));
        return true;

      } catch (Exception e) { }
    }
    return false;
  }


  public boolean sendShutdown() {
    if (initialized) {
      try {
        byte[] buf = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(buf);
        bb.putInt(0, SHUTDOWN);
        socket.send(new DatagramPacket(buf, buf.length, address, sketchPort));
        return true;

      } catch (Exception e) { }
    }
    return false;
  }


  static public String getServerCode(int listenPort,
                                     boolean hasInts, boolean hasFloats) {
    String serverCode = ""+
    "class TweakModeServer extends Thread\n"+
    "{\n"+
    "  protected DatagramSocket socket = null;\n"+
    "  protected boolean running = true;\n"+
    "  final int INT_VAR = 0;\n"+
    "  final int FLOAT_VAR = 1;\n"+
    "  final int SHUTDOWN = 0xffffffff;\n"+
    "  public TweakModeServer() {\n"+
    "    this(\"TweakModeServer\");\n"+
    "  }\n"+
    "  public TweakModeServer(String name) {\n"+
    "    super(name);\n"+
    "  }\n"+
    "  public void setup()\n"+
    "  {\n"+
    "    try {\n"+
    "      socket = new DatagramSocket("+listenPort+");\n"+
    "      socket.setSoTimeout(250);\n"+
    "    } catch (IOException e) {\n"+
    "      addEmptyLine(\"error: could not create TweakMode server socket\");\n"+
    "    }\n"+
    "  }\n"+
    "  public void run()\n"+
    "  {\n"+
    "    byte[] buf = new byte[256];\n"+
    "    while(running)\n"+
    "    {\n"+
    "      try {\n"+
    "        DatagramPacket packet = new DatagramPacket(buf, buf.length);\n"+
    "        socket.receive(packet);\n"+
    "        ByteBuffer bb = ByteBuffer.wrap(buf);\n"+
    "        int type = bb.getInt(0);\n"+
    "        int index = bb.getInt(4);\n";

    if (hasInts) {
      serverCode +=
    "        if (type == INT_VAR) {\n"+
    "          int val = bb.getInt(8);\n"+
    "          tweakmode_int[index] = val;\n"+
    "        }\n"+
    "        else ";
    }
    if (hasFloats) {
      serverCode +=
    "         if (type == FLOAT_VAR) {\n"+
    "          float val = bb.getFloat(8);\n"+
    "          tweakmode_float[index] = val;\n"+
    "        }\n"+
    "        else";
    }
    serverCode +=
    "        if (type == SHUTDOWN) {\n"+
    "          running = false;\n"+
    "        }\n"+
    "      } catch (SocketTimeoutException e) {\n"+
    "        // nothing to do here just try receiving again\n"+
    "      } catch (Exception e) {\n"+
    "      }\n"+
    "    }\n"+
    "    socket.close();\n"+
    "  }\n"+
    "}\n\n\n";

    return serverCode;
  }
}
