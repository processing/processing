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

import java.awt.EventQueue;
import java.io.File;

import processing.app.Base;
import processing.data.StringDict;


public class Welcome {

  public Welcome() {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        // eventually this will be correct
        //File indexFile = Base.getLibFile("welcome/index.html");
        // version when running from command line for editing
        File indexFile = new File("../build/shared/lib/welcome/index.html");
        if (!indexFile.exists()) {
//          System.out.println("user dir is " + System.getProperty("user.dir"));
//          System.out.println(new File("").getAbsolutePath());
          // processing/build/macosx/work/Processing.app/Contents/Java
          // version for Scott to use for OS X debugging
          indexFile = Base.getContentFile("../../../../../shared/lib/welcome/index.html");
//          try {
//            System.out.println(indexFile.getCanonicalPath());
//          } catch (IOException e) {
//            e.printStackTrace();
//          }
        }

        WebFrame frame = new WebFrame(indexFile, 400) {
          public void handleSubmit(StringDict dict) {
            dict.print();
            handleClose();
          }

          public void handleClose() {
            //System.exit(0);
            dispose();
          }
        };
        frame.setVisible(true);
      }
    });
  }


  static public void main(String[] args) {
    Base.initPlatform();
    new Welcome();
  }
}
