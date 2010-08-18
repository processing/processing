package processing.plugin.ui.processingeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;

public class ProcessingPartitionScanner extends RuleBasedPartitionScanner {

	public static final String MULTILINE_COMMENT = "__multiline_comment";
	public static final String[] PARTITION_TYPES = new String[] { MULTILINE_COMMENT };

	/** Detector for empty comments. */
	static class EmptyCommentDetector implements IWordDetector {

		/* Method declared on IWordDetector */
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		/* Method declared on IWordDetector */
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}
	
	static class WordPredicateRule extends WordRule implements IPredicateRule {
		
		private IToken fSuccessToken;
		
		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken= successToken;
			addWord("/**/", fSuccessToken); //$NON-NLS-1$
		}
		
		/* @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(ICharacterScanner, boolean) */
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		/* @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()*/
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}

	/** Create the partitioner and sets up the appropriate rules. */
	public ProcessingPartitionScanner() {
		super();
		
		IToken comment= new Token(MULTILINE_COMMENT);

		List rules= new ArrayList();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", Token.UNDEFINED)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

		// Add special case word rule.
		rules.add(new WordPredicateRule(comment));

		// Add rules for multi-line comments and javadoc.
		rules.add(new MultiLineRule("/*", "*/", comment, (char) 0, true)); //$NON-NLS-1$ //$NON-NLS-2$

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
	
}
