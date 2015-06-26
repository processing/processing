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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import processing.app.Base;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
//import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static javafx.concurrent.Worker.State.FAILED;

public class Welcome extends JFrame {

  private final JFXPanel jfxPanel = new JFXPanel();
  private WebEngine engine;

  private final JPanel panel = new JPanel(new BorderLayout());
  private final JLabel lblStatus = new JLabel();

  private final JButton btnGo = new JButton("Go");
  private final JTextField txtURL = new JTextField();
  private final JProgressBar progressBar = new JProgressBar();


  public Welcome() {
    super("Welcome to Processing 3");
    initComponents();
  }


  private void initComponents() {
    createScene();

    ActionListener al = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadURL(txtURL.getText());
      }
    };

    btnGo.addActionListener(al);
    txtURL.addActionListener(al);

    progressBar.setPreferredSize(new Dimension(150, 18));
    progressBar.setStringPainted(true);

    JPanel topBar = new JPanel(new BorderLayout(5, 0));
    topBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    topBar.add(txtURL, BorderLayout.CENTER);
    topBar.add(btnGo, BorderLayout.EAST);

    JPanel statusBar = new JPanel(new BorderLayout(5, 0));
    statusBar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
    statusBar.add(lblStatus, BorderLayout.CENTER);
    statusBar.add(progressBar, BorderLayout.EAST);

    panel.add(topBar, BorderLayout.NORTH);
    panel.add(jfxPanel, BorderLayout.CENTER);
    panel.add(statusBar, BorderLayout.SOUTH);

    getContentPane().add(panel);

    setPreferredSize(new Dimension(500, 500));
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    pack();
    setLocationRelativeTo(null);
  }


  private void createScene() {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {

        WebView view = new WebView();
        engine = view.getEngine();

        engine.titleProperty().addListener(new ChangeListener<String>() {
          @Override
          public void changed(ObservableValue<? extends String> observable, String oldValue, final String newValue) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                Welcome.this.setTitle(newValue);
              }
            });
          }
        });

        engine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
          @Override
          public void handle(final WebEvent<String> event) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                lblStatus.setText(event.getData());
              }
            });
          }
        });

        engine.locationProperty().addListener(new ChangeListener<String>() {
          @Override
          public void changed(ObservableValue<? extends String> ov, String oldValue, final String newValue) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                txtURL.setText(newValue);
              }
            });
          }
        });

        engine.getLoadWorker().workDoneProperty().addListener(new ChangeListener<Number>() {
          @Override
          public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, final Number newValue) {
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                progressBar.setValue(newValue.intValue());
              }
            });
          }
        });

        engine.getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
          @Override
          public void changed(ObservableValue<? extends Throwable> o, Throwable old, final Throwable value) {
            if (engine.getLoadWorker().getState() == FAILED) {
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  JOptionPane.showMessageDialog(panel,
                                                (value != null) ?
                                                engine.getLocation() + "\n" + value.getMessage() :
                                                engine.getLocation() + "\nUnexpected error.",
                                                "Loading error...",
                                                JOptionPane.ERROR_MESSAGE);
                }
              });
            }
          }
        });

        final String EVENT_TYPE_CLICK = "click";
        engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
          @Override
          public void changed(ObservableValue ov, State oldState, State newState) {
            if (newState == Worker.State.SUCCEEDED) {
              EventListener listener = new EventListener() {
                @Override
                public void handleEvent(final Event ev) {
                  String domEventType = ev.getType();
                  //System.err.println("EventType: " + domEventType);
                  if (domEventType.equals(EVENT_TYPE_CLICK)) {
                    //System.out.println("href is " + href);
                    EventQueue.invokeLater(new Runnable() {
                      public void run() {
                        String href = ((Element)ev.getTarget()).getAttribute("href");
                        Base.openURL(href);
                      }
                    });
                  }
                }
              };

              Document doc = engine.getDocument();
              NodeList nodeList = doc.getElementsByTagName("a");
              for (int i = 0; i < nodeList.getLength(); i++) {
                ((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
                //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
                //((EventTarget) nodeList.item(i)).addEventListener(EVENT_TYPE_MOUSEOVER, listener, false);
              }
            }
          }
        });

        jfxPanel.setScene(new Scene(view));
      }
    });
  }


  public void loadFile(final File htmlFile) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          engine.load(htmlFile.toURI().toURL().toExternalForm());

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }


  public void loadURL(final String url) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          engine.load(new URL(url).toExternalForm());
        } catch (MalformedURLException e) {
          e.printStackTrace();
        }
      }
    });
  }


  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {

      @Override
      public void run() {
        Welcome browser = new Welcome();
        browser.setVisible(true);

        Base.initPlatform();

        try {
          //System.out.println(System.getProperty("user.dir"));
          //File indexFile = Base.getLibFile("welcome/index.html");
          File indexFile = new File("../build/shared/lib/welcome/index.html");
          browser.loadFile(indexFile);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
