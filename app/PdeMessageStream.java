// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

import java.io.*;

class PdeMessageStream extends OutputStream {

  PdeEditor editor;
  PdeMessageConsumer messageConsumer;

  public PdeMessageStream(PdeEditor editor,
                          PdeMessageConsumer messageConsumer) {
    this.editor = editor;
    this.messageConsumer = messageConsumer;
  }

  public void setMessageConsumer(PdeMessageConsumer messageConsumer) {
    this.messageConsumer = messageConsumer;
  }

  public void close() { }

  public void flush() { }

  public void write(byte b[]) { 
    System.out.println("leech1: " + new String(b));
  }

  public void write(byte b[], int offset, int length) {
    //System.out.println("leech2: " + new String(b));
    this.messageConsumer.message(new String(b, offset, length));
  }

  public void write(int b) {
    System.out.println("leech3: '" + ((char)b) + "'");
  }
}
