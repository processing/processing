/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2019 The Processing Foundation

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

package processing.mode.java.preproc;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import processing.mode.java.preproc.util.*;
import processing.mode.java.preproc.util.strategy.MessageSimplifierUtil;

import java.util.BitSet;
import java.util.Optional;


public class PdeIssueEmitter extends BaseErrorListener {

  private final PdePreprocessIssueListener listener;
  private final Optional<String> sourceMaybe;

  public PdeIssueEmitter(PdePreprocessIssueListener newListener) {
    listener = newListener;
    sourceMaybe = Optional.empty();
  }

  public PdeIssueEmitter(PdePreprocessIssueListener newListener, String newSource) {
    listener = newListener;
    sourceMaybe = Optional.of(newSource);
  }

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                          int charPositionInLine, String msg, RecognitionException e) {

    if (msg.contains("\\n")) {
      String msgContent = MessageSimplifierUtil.getOffendingArea(msg, false);
      line -= SyntaxUtil.getCount(msgContent, "\\n");
      charPositionInLine = msgContent.length();
    }

    IssueMessageSimplification simplification = SyntaxIssueMessageSimplifier.get().simplify(msg);

    LineOffset lineOffset;

    if (sourceMaybe.isPresent()) {
      lineOffset = LineOffsetFactory.get().getLineWithOffset(
          simplification,
          line,
          charPositionInLine,
          sourceMaybe.get()
      );
    } else {
      lineOffset = new LineOffset(line, charPositionInLine);
    }

    listener.onIssue(new PdePreprocessIssue(
        lineOffset.getLine(),
        lineOffset.getCharPosition(),
        simplification.getMessage()
    ));
  }

  public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
      boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
  }

  public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
      BitSet conflictingAlts, ATNConfigSet configs) {
  }

  public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
      int prediction, ATNConfigSet configs) {
  }

}
