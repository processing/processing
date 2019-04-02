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
 * Template class for checking that two tokens appear in pairs.
 *
 * <p>
 * Template class for message simplification strategies that check for an equal number of
 * occurrences for two characters like "(" and ")".
 * </p>
 */
public abstract class TokenPairTemplateMessageSimplifierStrategy
    implements PreprocIssueMessageSimplifierStrategy {

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    String messageContent = MessageSimplifierUtil.getOffendingArea(message);

    int count1 = SyntaxUtil.getCount(messageContent, getToken1());
    int count2 = SyntaxUtil.getCount(messageContent, getToken2());

    if (count1 == count2) {
      return Optional.empty();
    }

    String missingToken;
    if (count1 < count2) {
      missingToken = getToken1();
    } else {
      missingToken = getToken2();
    }

    String newMessage = String.format(
        MessageSimplifierUtil.getLocalStr("editor.status.missing.default")
            .replace("%c", "%s"), missingToken);

    return Optional.of(
        new IssueMessageSimplification(newMessage)
    );
  }

  /**
   * Get the first token in the pair.
   *
   * @return The first token whose occurrences should be counted.
   */
  public abstract String getToken1();


  /**
   * Get the second token in the pair.
   *
   * @return The second token whose occurrences should be counted.
   */
  public abstract String getToken2();

}
