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

import processing.app.Language;
import processing.mode.java.preproc.issue.IssueMessageSimplification;
import processing.mode.java.preproc.code.SyntaxUtil;

import java.util.Optional;


/**
 * Strategy to check to make sure that the number of occurrences of a token are even.
 *
 * <p>
 *   Strategy to ensure that there are an even number of tokens like even number of double quotes
 *   for example.
 * </p>
 */
public abstract class EvenCountTemplateMessageSimplifierStrategy
    implements PreprocIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    if (getFilter().isPresent()) {
      messageContent = messageContent.replace(getFilter().get(), "");
    }

    int count = SyntaxUtil.getCount(messageContent, getToken());

    if (count % 2 == 0) {
      return Optional.empty();
    } else {
      String newMessage = String.format(
          MessageSimplifierUtil.getLocalStr("editor.status.missing.default").replace("%c", "%s"),
          getToken()
      );
      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    }
  }

  /**
   * Get the token that should be counted.
   *
   * @return The token whose occurrences should be even.
   */
  public abstract String getToken();

  /**
   * Get the text that should be removed before counting.
   *
   * @return An optional string whose occurrences will be removed prior to counting.
   */
  public Optional<String> getFilter() {
    return Optional.empty();
  }

}
