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

package processing.mode.java.preproc.issue;


/**
 * Data structure describing an issue simplification or explanation.
 *
 * <p>
 *   Data structure describing an edit that was made to an error message or warning to be shown to
 *   the user based on a series of rules that attempts to make error messages easier to understand
 *   for the user.
 * </p>
 */
public class IssueMessageSimplification {

  private final String message;
  private final boolean attributeToPriorToken;

  /**
   * Create a new issue message simplification.
   *
   * <p>
   *   Create a new issue message simplification that leaves the token attribution alone (the token
   *   on which the error was reported will be the same before error message simplification).
   * </p>
   *
   * @param newMessage The message to show to the user.
   */
  public IssueMessageSimplification(String newMessage) {
    message = newMessage;
    attributeToPriorToken = false;
  }

  /**
   * Create a new issue message simplification.
   *
   * <p>
   *   Create a new issue message simplification. Note that there is an option to have the error
   *   attributed to the "prior token". This is helpful, for example, when a semicolon is missing.
   *   The error is generated on the token after the line on which the semicolon was omitted so,
   *   while the error technically emerges on the next line, it is better for the user for it to
   *   appear earlier. Specifically, it is most sensible for it to appear on the "prior token".
   * </p>
   *
   * @param newMessage The message to show to the user.
   * @param newAttributeToPriorToken Boolean flag indicating if the error should be shown on the
   *    token prior to the one on which the error was originally generated. True if the error should
   *    be attributed to the prior token. False otherwise.
   */
  public IssueMessageSimplification(String newMessage, boolean newAttributeToPriorToken) {
    message = newMessage;
    attributeToPriorToken = newAttributeToPriorToken;
  }

  /**
   * Get the error message text that should be shown to the user.
   *
   * @return The error message text that should be shown to the user.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Flag indicating if the error should be attributed to the prior token.
   *
   * @return True if the error should be attributed to the prior non-skip token (not whitepsace or
   *    comment). This is useful when a mistake on a prior line like omitted semicolon causes an
   *    error on a later line but one wants error highlighting closer to the mistake itself. False
   *    if the error should be attributed to the original offending token.
   */
  public boolean getAttributeToPriorToken() {
    return attributeToPriorToken;
  }

}
