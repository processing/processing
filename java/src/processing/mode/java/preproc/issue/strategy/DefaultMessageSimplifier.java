/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
Part of the Processing project - http://processing.org

Copyright (c) 2012-19 The Processing Foundation

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License version 2
as published by the Free Software Foundation.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.mode.java.preproc.issue.strategy;

import processing.mode.java.preproc.issue.IssueMessageSimplification;

import java.util.Optional;


/**
 * Default strategy to use if other message simplification strategies have failed.
 */
public class DefaultMessageSimplifier implements PreprocIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (message.contains("viable alternative")) {
      String newMessage = String.format(
          MessageSimplifierUtil.getLocalizedGenericError("%s"),
          MessageSimplifierUtil.getOffendingArea(message)
      );
      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    } else {
      return Optional.of(
          new IssueMessageSimplification(message)
      );
    }
  }

}
