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

package processing.app;

import java.awt.Frame;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import processing.app.ui.Editor;

public class Messages {
  /**
   * "No cookie for you" type messages. Nothing fatal or all that
   * much of a bummer, but something to notify the user about.
   */
  static public void showMessage(String title, String message) {
    if (title == null) title = "Message";

    if (Base.isCommandLine()) {
      System.out.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }


  /**
   * Non-fatal error message.
   */
  static public void showWarning(String title, String message) {
    showWarning(title, message, null);
  }

  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarning(String title, String message, Throwable e) {
    if (title == null) title = "Warning";

    if (Base.isCommandLine()) {
      System.out.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.WARNING_MESSAGE);
    }
    if (e != null) e.printStackTrace();
  }


  /**
   * Non-fatal error message with optional stack trace side dish.
   */
  static public void showWarningTiered(String title,
                                       String primary, String secondary,
                                       Throwable e) {
    if (title == null) title = "Warning";

    final String message = primary + "\n" + secondary;
    if (Base.isCommandLine()) {
      System.out.println(title + ": " + message);

    } else {
//      JOptionPane.showMessageDialog(new Frame(), message,
//                                    title, JOptionPane.WARNING_MESSAGE);
      if (!Platform.isMacOS()) {
        JOptionPane.showMessageDialog(new JFrame(),
                                      "<html><body>" +
                                      "<b>" + primary + "</b>" +
                                      "<br>" + secondary, title,
                                      JOptionPane.WARNING_MESSAGE);
      } else {
        // Pane formatting adapted from the Quaqua guide
        // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
        JOptionPane pane =
          new JOptionPane("<html> " +
                          "<head> <style type=\"text/css\">"+
                          "b { font: 13pt \"Lucida Grande\" }"+
                          "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
                          "</style> </head>" +
                          "<b>" + primary + "</b>" +
                          "<p>" + secondary + "</p>",
                          JOptionPane.WARNING_MESSAGE);

//        String[] options = new String[] {
//            "Yes", "No"
//        };
//        pane.setOptions(options);

        // highlight the safest option ala apple hig
//        pane.setInitialValue(options[0]);

        JDialog dialog = pane.createDialog(new JFrame(), null);
        dialog.setVisible(true);

//        Object result = pane.getValue();
//        if (result == options[0]) {
//          return JOptionPane.YES_OPTION;
//        } else if (result == options[1]) {
//          return JOptionPane.NO_OPTION;
//        } else {
//          return JOptionPane.CLOSED_OPTION;
//        }
      }
    }
    if (e != null) e.printStackTrace();
  }


  /**
   * Show an error message that's actually fatal to the program.
   * This is an error that can't be recovered. Use showWarning()
   * for errors that allow P5 to continue running.
   */
  static public void showError(String title, String message, Throwable e) {
    if (title == null) title = "Error";

    if (Base.isCommandLine()) {
      System.err.println(title + ": " + message);

    } else {
      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    JOptionPane.ERROR_MESSAGE);
    }
    if (e != null) e.printStackTrace();
    System.exit(1);
  }


  /**
   * Testing a new warning window that includes the stack trace.
   */
  static public void showTrace(String title, String message,
                                       Throwable t, boolean fatal) {
    if (title == null) title = fatal ? "Error" : "Warning";

    if (Base.isCommandLine()) {
      System.err.println(title + ": " + message);
      if (t != null) {
        t.printStackTrace();
      }

    } else {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      // Necessary to replace \n with <br/> (even if pre) otherwise Java
      // treats it as a closed tag and reverts to plain formatting.
      message = ("<html>" + message +
                 "<br/><font size=2><br/>" +
                 sw + "</html>").replaceAll("\n", "<br/>");

      JOptionPane.showMessageDialog(new Frame(), message, title,
                                    fatal ?
                                    JOptionPane.ERROR_MESSAGE :
                                    JOptionPane.WARNING_MESSAGE);

      if (fatal) {
        System.exit(1);
      }
    }
  }


  // ...................................................................



  // incomplete
  static public int showYesNoCancelQuestion(Editor editor, String title,
                                            String primary, String secondary) {
    if (!Platform.isMacOS()) {
      int result =
        JOptionPane.showConfirmDialog(null, primary + "\n" + secondary, title,
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);
      return result;
//    if (result == JOptionPane.YES_OPTION) {
//
//    } else if (result == JOptionPane.NO_OPTION) {
//      return true;  // ok to continue
//
//    } else if (result == JOptionPane.CANCEL_OPTION) {
//      return false;
//
//    } else {
//      throw new IllegalStateException();
//    }

    } else {
      // Pane formatting adapted from the Quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
                        "</style> </head>" +
                        "<b>" + Language.text("save.title") + "</b>" +
                        "<p>" + Language.text("save.hint") + "</p>",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
        Language.text("save.btn.save"),
        Language.text("prompt.cancel"),
        Language.text("save.btn.dont_save")
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             Integer.valueOf(2));

      JDialog dialog = pane.createDialog(editor, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {
        return JOptionPane.YES_OPTION;
      } else if (result == options[1]) {
        return JOptionPane.CANCEL_OPTION;
      } else if (result == options[2]) {
        return JOptionPane.NO_OPTION;
      } else {
        return JOptionPane.CLOSED_OPTION;
      }
    }
  }


  static public int showYesNoQuestion(Frame editor, String title,
                                      String primary, String secondary) {
    if (!Platform.isMacOS()) {
      return JOptionPane.showConfirmDialog(editor,
                                           "<html><body>" +
                                           "<b>" + primary + "</b>" +
                                           "<br>" + secondary, title,
                                           JOptionPane.YES_NO_OPTION,
                                           JOptionPane.QUESTION_MESSAGE);
    } else {
      // Pane formatting adapted from the Quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px; width: 300px }"+
                        "</style> </head>" +
                        "<b>" + primary + "</b>" +
                        "<p>" + secondary + "</p>",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
        "Yes", "No"
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      JDialog dialog = pane.createDialog(editor, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {
        return JOptionPane.YES_OPTION;
      } else if (result == options[1]) {
        return JOptionPane.NO_OPTION;
      } else {
        return JOptionPane.CLOSED_OPTION;
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  static public void log(Object from, String message) {
    if (Base.DEBUG) {
      System.out.println(from.getClass().getName() + ": " + message);
    }
  }


  static public void log(String message) {
    if (Base.DEBUG) {
      System.out.println(message);
    }
  }


  static public void logf(String message, Object... args) {
    if (Base.DEBUG) {
      System.out.println(String.format(message, args));
    }
  }


  static public void loge(String message, Throwable e) {
    if (Base.DEBUG) {
      System.err.println(message);
      e.printStackTrace();
    }
  }


  static public void loge(String message) {
    if (Base.DEBUG) {
      System.out.println(message);
    }
  }
}