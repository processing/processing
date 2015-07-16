/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2015 The Processing Foundation

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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

import processing.app.Base;
import processing.core.PApplet;
import processing.data.StringDict;


public class WebFrame extends JFrame {
  JEditorPane editorPane;
  HTMLEditorKit editorKit;


  public WebFrame(File file, int width) {
    //setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

    String[] lines = PApplet.loadStrings(file);
    String content = PApplet.join(lines, "\n");

    int high = getContentHeight(width, content);
    editorPane = new JEditorPane("text/html", content);
    editorPane.setEditable(false);
    editorPane.setPreferredSize(new Dimension(width, high));
    getContentPane().add(editorPane);

    Toolkit.registerWindowCloseKeys(getRootPane(), new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        handleClose();
      }
    });

    editorKit = (HTMLEditorKit) editorPane.getEditorKit();
    editorKit.setAutoFormSubmission(false);

    Object title = editorPane.getDocument().getProperty("title");
    if (title instanceof String) {
      setTitle((String) title);
    }

    editorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {
        //System.out.println(e);
        if (e instanceof FormSubmitEvent) {
          //System.out.println("got submit event");
          String result = ((FormSubmitEvent) e).getData();
          StringDict dict = new StringDict();
          String[] pairs = result.split("&");
          for (String pair : pairs) {
            String[] pieces = pair.split("=");
            String attr = PApplet.urlDecode(pieces[0]);
            String valu = PApplet.urlDecode(pieces[1]);
            dict.set(attr, valu);
          }
          //dict.print();
          handleSubmit(dict);

        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          //System.out.println("clicked " + e.getURL());
          handleLink(e.getURL().toExternalForm());
        }
      }
    });
    pack();
    setLocationRelativeTo(null);
    //setVisible(true);
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


  public void handleClose() {
    dispose();
  }


  /**
   * Override this to do something interesting when a form is submitted.
   * To keep things simple, this doesn't allow for multiple params with the
   * same name.
   */
  public void handleSubmit(StringDict dict) {
  }


  public void handleLink(String link) {
    Base.openURL(link);
  }


  // Why this doesn't work inline above is beyoned me
  static int getContentHeight(int width, String content) {
    JEditorPane dummy = new JEditorPane("text/html", content);
    dummy.setSize(width, Short.MAX_VALUE);
    return dummy.getPreferredSize().height;
  }
}