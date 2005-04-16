/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  MessageStream - outputstream to handle stdout/stderr messages
  Part of the Processing project - http://Proce55ing.net

  Except where noted, code is written by Ben Fry and
  Copyright (c) 2001-03 Massachusetts Institute of Technology

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


/**
 * this is used by Editor, System.err is set to
 * new PrintStream(new MessageStream())
 *
 * it's also used by Compiler
 */
class MessageStream extends OutputStream {

  //Editor editor;
  MessageConsumer messageConsumer;

  public MessageStream(/*Editor editor,*/
                          MessageConsumer messageConsumer) {
    //this.editor = editor;
    this.messageConsumer = messageConsumer;
  }

  //public void setMessageConsumer(MessageConsumer messageConsumer) {
  //this.messageConsumer = messageConsumer;
  //}

  public void close() { }

  public void flush() { }

  public void write(byte b[]) {
    // this never seems to get called
    System.out.println("leech1: " + new String(b));
  }

  public void write(byte b[], int offset, int length) {
    //System.out.println("leech2: " + new String(b));
    this.messageConsumer.message(new String(b, offset, length));
  }

  public void write(int b) {
    // this never seems to get called
    System.out.println("leech3: '" + ((char)b) + "'");
  }
}
