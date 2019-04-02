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
import java.util.regex.Pattern;


/**
 * Strategy that cleans up errors based on a regex matching the error message.
 */
public abstract class RegexTemplateMessageSimplifierStrategy
    implements PreprocIssueMessageSimplifierStrategy {

  private Pattern pattern;

  /**
   * Create a new instance of this strategy.
   */
  public RegexTemplateMessageSimplifierStrategy() {
    pattern = Pattern.compile(getRegexPattern());
  }

  @Override
  public Optional<IssueMessageSimplification> simplify(String message) {
    if (pattern.matcher(message).find()) {
      String newMessage = String.format(
          getHintTemplate(),
          MessageSimplifierUtil.getOffendingArea(message)
      );

      return Optional.of(
          new IssueMessageSimplification(newMessage)
      );
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get the regex that should be matched against the error message for this strategy to apply.
   *
   * @return The regex that should be matched in order to activate this strategy.
   */
  public abstract String getRegexPattern();

  /**
   * Get the hint template for this strategy.
   *
   * <p>
   * Get a template string with a "%s" where the "offending snippet of code" can be inserted where
   * the resulting rendered template can be used as an error hint for the user. For example,
   * "Invalid identifier near %s" may be rendered to the user like "Syntax error. Hint: Invalid
   * identifier near ,1a);" for example.
   * </p>
   *
   * @return The rendered hint template.
   */
  public abstract String getHintTemplate();

}
