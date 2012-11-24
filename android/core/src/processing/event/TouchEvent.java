/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2012 The Processing Foundation

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

package processing.event;


// PLACEHOLDER CLASS: DO NOT USE. IT HAS NOT EVEN DECIDED WHETHER
// THIS WILL BE CALLED TOUCHEVENT ONCE IT'S FINISHED.

/*
http://developer.android.com/guide/topics/ui/ui-events.html
http://developer.android.com/reference/android/view/MotionEvent.html
http://developer.apple.com/library/safari/#documentation/UserExperience/Reference/TouchEventClassReference/TouchEvent/TouchEvent.html
http://developer.apple.com/library/ios/#documentation/UIKit/Reference/UIGestureRecognizer_Class/Reference/Reference.html#//apple_ref/occ/cl/UIGestureRecognizer

Apple's high-level gesture names:
tap
pinch
rotate
swipe
pan
longpress
*/
public class TouchEvent extends Event {

  public TouchEvent(Object nativeObject, long millis, int action, int modifiers) {
    super(nativeObject, millis, action, modifiers);
    this.flavor = TOUCH;
  }
}
