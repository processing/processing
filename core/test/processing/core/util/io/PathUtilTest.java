/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-19 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

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

package processing.core.util.io;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class PathUtilTest {

  @Test
  public void testParseExtension() {
    String extension = PathUtil.parseExtension("directory/subdirectory/image.png");

    assertEquals(
      "png",
      extension
    );
  }

  @Test
  public void testCleanExtension() {
    String extension = PathUtil.parseExtension(".PNG");

    assertEquals(
      "png",
      extension
    );
  }

  @Test
  public void testCreatePath() {
    PathUtil.createPath("resource-test/scratch/path/recurse/image.png");
    File unit = new File("resource-test/scratch/path/recurse");
    assertTrue(unit.exists());
  }

}
