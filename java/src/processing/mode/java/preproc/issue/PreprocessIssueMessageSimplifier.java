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


import processing.mode.java.preproc.issue.strategy.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Facade that tries to create a better error message for syntax issues in input source.
 *
 * <p>
 * Facade that interprets error messages from ANTLR in an attempt to generate an improved error
 * message when describing grammatically incorrect input. This is distinct from compiler errors
 * caused after generating an AST.
 * </p>
 *
 * <p>
 *   Note that this is distinct from the {CompileErrorMessageSimplifier}. This operates on issues
 *   caused in parsing and services all users whereas the {CompileErrorMessageSimplifier} only
 *   operates on issues generated after preprocessing has been successful.
 * </p>
 */
public class PreprocessIssueMessageSimplifier {

  private static AtomicReference<PreprocessIssueMessageSimplifier> instance = new AtomicReference<>();

  private List<PreprocIssueMessageSimplifierStrategy> strategies;

  /**
   * Get a shared instance of this singleton.
   *
   * @return Shared instance of this singleton, creating that shared instance if one did not exist
   *    previously.
   */
  public static PreprocessIssueMessageSimplifier get() {
    instance.compareAndSet(null, new PreprocessIssueMessageSimplifier());
    return instance.get();
  }

  /**
   * Create a new syntax issue message simplifier with the default simplifier strategies.
   */
  private PreprocessIssueMessageSimplifier() {
    strategies = new ArrayList<>();
    strategies.add(new MissingIdentifierMessageSimplifierStrategy());
    strategies.add(new KnownMissingMessageSimplifierStrategy());
    strategies.add(new ExtraneousInputMessageSimplifierStrategy());
    strategies.add(new MismatchedInputMessageSimplifierStrategy());
    strategies.add(new MissingVariableNameMessageSimplifierStrategy());
    strategies.add(new AssignmentMessageSimplifierStrategy());
    strategies.add(new BadIdentifierMessageSimplifierStrategy());
    strategies.add(new MissingClassNameMessageSimplifierStrategy());
    strategies.add(new MissingMethodNameMessageSimplifierStrategy());
    strategies.add(new BadParamMessageSimplifierStrategy());
    strategies.add(new MissingDoubleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingSingleQuoteMessageSimplifierStrategy());
    strategies.add(new MissingParenMessageSimplifierStrategy());
    strategies.add(new MissingChevMessageSimplifierStrategy());
    strategies.add(new MissingCurlyMessageSimplifierStrategy());
    strategies.add(new DefaultMessageSimplifier());
  }

  /**
   * Attempt to improve an error message.
   *
   * @param originalMessage Error message generated from ANTLR.
   * @return An improved error message or the originalMessage if no improvements could be made.
   */
  public IssueMessageSimplification simplify(String originalMessage) {
    //System.err.println(originalMessage);
    Optional<IssueMessageSimplification> matching = strategies.stream()
        .map((x) -> x.simplify(originalMessage))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();

    return matching.orElse(new IssueMessageSimplification(originalMessage));
  }

}
