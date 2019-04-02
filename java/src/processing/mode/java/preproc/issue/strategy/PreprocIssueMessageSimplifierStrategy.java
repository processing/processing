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
 * Interface for strategies that improve preprocess error messages before showing them to the user.
 */
public interface PreprocIssueMessageSimplifierStrategy {

  /**
   * Attempt to simplify an error message.
   *
   * @param message The message to be simplified.
   * @return An optional with an improved message or an empty optional if no improvements could be
   *    made by this strategy.
   */
  Optional<IssueMessageSimplification> simplify(String message);

}
