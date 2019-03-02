/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015-19 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

import processing.app.Platform;
import processing.core.PApplet;
import processing.data.StringDict;


public class WebFrame extends JFrame {
  JEditorPane editorPane;
  HTMLEditorKit editorKit;
//  int contentHeight;
  boolean ready;


  public WebFrame(File file, int width, Container panel) throws IOException {
    // Need to use the URL version so that relative paths work for images
    // https://github.com/processing/processing/issues/3494
    URL fileUrl = file.toURI().toURL();
    requestContentHeight(width, fileUrl);

    editorPane = new JEditorPane();

    // Title cannot be set until the page has loaded
    editorPane.addPropertyChangeListener("page", new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        Object title = editorPane.getDocument().getProperty("title");
        if (title instanceof String) {
          setTitle((String) title);
        }
      }
    });

    editorPane.setPage(fileUrl);
    editorPane.setEditable(false);
    // set height to something generic
    editorPane.setPreferredSize(new Dimension(width, width));

    //getContentPane().add(editorPane);
    Container pain = getContentPane();
    pain.setLayout(new BoxLayout(pain, BoxLayout.Y_AXIS));
    pain.add(editorPane);
    if (panel != null) {
      pain.add(panel);
    }

    Toolkit.registerWindowCloseKeys(getRootPane(), new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleClose();
      }
    });
    Toolkit.setIcon(this);

    editorKit = (HTMLEditorKit) editorPane.getEditorKit();
    editorKit.setAutoFormSubmission(false);

    editorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        //System.out.println(e);
        if (e instanceof FormSubmitEvent) {
          String result = ((FormSubmitEvent) e).getData();
          StringDict dict = new StringDict();
          if (result.trim().length() != 0) {
            String[] pairs = result.split("&");
            for (String pair : pairs) {
              //System.out.println("pair is " + pair);
              String[] pieces = pair.split("=");
              String attr = PApplet.urlDecode(pieces[0]);
              String valu = PApplet.urlDecode(pieces[1]);
              dict.set(attr, valu);
            }
          }
          //dict.print();
          handleSubmit(dict);

        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          //System.out.println("clicked " + e.getURL());
          handleLink(e.getURL().toExternalForm());
        }
      }
    });
  }


  /* not yet working
  public void addStyle(String rule) {
    StyleSheet sheet = kit.getStyleSheet();
    System.out.println(sheet);
    sheet.addRule(rule);
    kit.setStyleSheet(sheet);
    //textPane.setEditorKit(kit);  // nukes everything
  }
  */


  public void setVisible(final boolean visible) {
    new Thread(new Runnable() {
      public void run() {
        while (!ready) {
          try {
            Thread.sleep(5);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        WebFrame.super.setVisible(visible);
      }
    }).start();
  }


  public void handleClose() {
    dispose();
  }


  /**
   * Override this to do something interesting when a form is submitted.
   * To keep things simple, this doesn't allow for multiple params with the
   * same name. If no params submitted, the Dict will be empty (not null).
   */
  public void handleSubmit(StringDict dict) {
  }


  public void handleLink(String link) {
    Platform.openURL(link);
  }


  /*
  // Why this doesn't work inline above is beyond me
  static int getContentHeight(int width, String content) {
    JEditorPane dummy = new JEditorPane("text/html", content);
    dummy.setSize(width, Short.MAX_VALUE);
    return dummy.getPreferredSize().height;
  }
  */


  // Unlike the static version above that uses an (already loaded) String for
  // the content, using setPage() makes things run asynchronously, causing
  // getContentHeight() to fail because it returns zero. Instead we make
  // things 10x more complicated so that images will work.
  void requestContentHeight(final int width, final URL url) {
    new Thread(new Runnable() {
      public void run() {
        final JEditorPane dummy = new JEditorPane();
        dummy.addPropertyChangeListener("page", new PropertyChangeListener() {
          @Override
          public void propertyChange(PropertyChangeEvent evt) {
            int high = dummy.getPreferredSize().height;
            editorPane.setPreferredSize(new Dimension(width, high));
            pack();
            setLocationRelativeTo(null);
            ready = true;
          }
        });
        try {
          dummy.setPage(url);
        } catch (IOException e) {
          e.printStackTrace();
        }
        dummy.setSize(width, Short.MAX_VALUE);
      }
    }).start();
  }
}
