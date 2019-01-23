/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012-18 The Processing Foundation
  Copyright (c) 2004-12 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, version 2.1.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package processing.core.util.image.load;

import processing.core.PApplet;
import processing.core.PImage;


/**
 * Image load strategy which uses a secondary strategy if failed.
 *
 * <p>
 * Image loading strategy strategy which attempts to load an image first through a "primary
 * strategy" and, if the primary failed, it then attemps loading through an alternative "secondary
 * strategy".
 * </p>
 */
public class FallbackImageLoadStrategy implements ImageLoadStrategy {

  private ImageLoadStrategy primaryStrategy;
  private ImageLoadStrategy secondaryStrategy;

  /**
   * Create a new fallback image loading strategy.
   *
   * @param newPrimaryStrategy The strategy to try first.
   * @param newSecondaryStrategy The strategy to try if the first fails.
   */
  public FallbackImageLoadStrategy(ImageLoadStrategy newPrimaryStrategy,
      ImageLoadStrategy newSecondaryStrategy) {

    primaryStrategy = newPrimaryStrategy;
    secondaryStrategy = newSecondaryStrategy;
  }

  @Override
  public PImage load(PApplet pApplet, String path, String extension) {
    try {
      return primaryStrategy.load(pApplet, path, extension);
    } catch (Exception e) {
      // show error, but move on to the stuff below, see if it'll work
      e.printStackTrace();

      return secondaryStrategy.load(pApplet, path, extension);
    }
  }

}
