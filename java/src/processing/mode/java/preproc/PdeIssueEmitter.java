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
import processing.mode.java.preproc.util.SyntaxIssueMessageSimplifier;

import java.util.BitSet;


public class PdeIssueEmitter extends BaseErrorListener {

  private final PdePreprocessIssueListener listener;

  public PdeIssueEmitter(PdePreprocessIssueListener newListener) {
    listener = newListener;
  }

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                          int charPositionInLine, String msg, RecognitionException e) {

    listener.onIssue(new PdePreprocessIssue(
        line,
        charPositionInLine,
        SyntaxIssueMessageSimplifier.get().simplify(msg)
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
