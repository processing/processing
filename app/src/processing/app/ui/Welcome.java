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
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

import processing.app.Base;
import processing.core.PApplet;
import processing.data.StringDict;


public class Welcome {

  static public void main(String[] args) {
    Base.initPlatform();

    //File indexFile = Base.getLibFile("welcome/index.html");
    final File indexFile = new File("../build/shared/lib/welcome/index.html");

    EventQueue.invokeLater(new Runnable() {
      public void run() {
        JFrame frame = new JFrame();
//        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        String[] lines = PApplet.loadStrings(indexFile);
        String content = PApplet.join(lines, "\n");

        int high = getContentHeight(400, content);
        //System.out.println(high);
        //JTextPane textPane = new JTextPane();
        //JEditorPane textPane = new JEditorPane();
        JEditorPane textPane = new JEditorPane("text/html", content);
        //textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setPreferredSize(new Dimension(400, high));
        //String[] lines = PApplet.loadStrings(indexFile);
        //textPane.setText(PApplet.join(lines, "\n"));
//        System.out.println(textPane.getPreferredSize().height);
//        textPane.setSize(400, textPane.getPreferredSize().height);

        //frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.getContentPane().add(textPane);

        Toolkit.registerWindowCloseKeys(frame.getRootPane(), new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            System.exit(0);
          }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();
        kit.setAutoFormSubmission(false);

        Object title = textPane.getDocument().getProperty("title");
        if (title instanceof String) {
          frame.setTitle((String) title);
        }

        textPane.addHyperlinkListener(new HyperlinkListener() {

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
              dict.print();
            } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
              //System.out.println("clicked " + e.getURL());
              Base.openURL(e.getURL().toExternalForm());
            }
          }
        });
      }
    });
  }


  // Why this doesn't work inline above is beyoned me
  static int getContentHeight(int width, String content) {
    JEditorPane dummy = new JEditorPane("text/html", content);
    dummy.setSize(width, Short.MAX_VALUE);
    return dummy.getPreferredSize().height;
  }
}