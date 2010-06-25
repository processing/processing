package processing.editor.netbeans.syntax.scheme;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.TokenID;

/**
 * SchemeSyntax is a Syntax responsible for parsing Scheme language source
 *  code and transforming it into appropriate TokenID tokens.
 * @author $Author: antonio $ Antonio Vieiro (antonio@antonioshome.net)
 * @version $Revision: 1.2 $
 */
public class SchemeSyntax
  extends Syntax
{
  public SchemeSyntax()
  {
    tokenContextPath = SchemeTokenContext.contextPath;
  }
  
  protected TokenID parseToken()
  {
    TokenID retValue;
    retValue = doParseToken();
    return retValue;
  }
  
  private static final int ISI_WHITESPACE = 2;
  private static final int ISI_LINE_COMMENT = 3;
  private static final int ISI_STRING = 4;
  private static final int ISI_STRING_AFTER_SLASH = 5;
  private static final int ISA_TEXT = 6;
  private static final int ISA_HASH = 7;
  
  private TokenID doParseToken()
  {
    while( offset < stopOffset )
    {
      char curChar = buffer[offset];
      switch( state )
      {
        case ISA_HASH:
          if ( isDelimiter( curChar ) )
          {
            state = INIT;
            if ( curChar == '(' )
              return SchemeTokenContext.SCHEME_OPEN_PAREN;
            return categorizeHash();
          }
          offset++;
          break;
        case ISA_TEXT:
          if ( isDelimiter( curChar ) )
          {
            state = INIT;
            return categorizeText();
          }
          offset++;
          break;
        case ISI_LINE_COMMENT:
          offset++;
          if ( curChar == '\n' )
          {
            state = INIT;
            return SchemeTokenContext.SCHEME_COMMENT;
          }
          break;
        case ISI_WHITESPACE:
          if ( ! isWhitespace( curChar ) )
          {
            state = INIT;
            return SchemeTokenContext.SCHEME_WHITESPACE;
          }
          offset ++;
          break;
        case ISI_STRING:
          if ( curChar == '\\' )
          {
            state = ISI_STRING_AFTER_SLASH;
          }
          else if ( curChar == '"' )
          {
            state = INIT;
            offset++;
            return SchemeTokenContext.SCHEME_CONSTANT;
          }
          offset++;
          break;
        case ISI_STRING_AFTER_SLASH:
          state = ISI_STRING;
          offset++;
          break;
        case INIT:
          switch( curChar )
          {
            case '#':
              state = ISA_HASH;
              offset ++;
              break;
            case ';':
              state = ISI_LINE_COMMENT;
              offset ++;
              break;
            case '(':
              offset ++;
              return SchemeTokenContext.SCHEME_OPEN_PAREN;
            case ')':
              offset ++;
              return SchemeTokenContext.SCHEME_CLOSE_PAREN;
            case '"':
              state = ISI_STRING;
              offset ++;
              break;
            default:
              if ( isWhitespace( curChar ) )
              {
                state = ISI_WHITESPACE;
                offset ++;
                break;
              }
              else
              {
                state = ISA_TEXT;
                offset ++;
                break;
              }
          }
      }
    }
    if ( lastBuffer )
    {
      if ( state == ISI_STRING || state == ISI_STRING_AFTER_SLASH || state == ISA_HASH )
        return SchemeTokenContext.SCHEME_CONSTANT;
      if ( state == ISA_TEXT )
      {
        return categorizeText();
      }
      if ( state == ISI_LINE_COMMENT )
        return SchemeTokenContext.SCHEME_COMMENT;
      if ( state == ISI_WHITESPACE )
        return SchemeTokenContext.SCHEME_WHITESPACE;
      Logger.getLogger( getClass().getName() ).log( Level.WARNING,
        "SchemeSyntax: Unknown token type for state: '" + state + "'");
    }
    return null;
  }
  
  
  private boolean isWhitespace( char aChar )
  {
    switch( aChar )
    {
      case ' ':
      case '\t':
      case '\r':
      case '\n':
      case '\f':
        return true;
      default:
        return false;
    }
  }
  
  private boolean isDelimiter( char aChar )
  {
    switch( aChar )
    {
      case '#':
      case ';':
      case '(':
      case ')':
        return true;
      default:
        return isWhitespace( aChar );
    }
  }
  
  private TokenID categorizeText()
  {
    if ( isStandardProcedure() )
      return SchemeTokenContext.SCHEME_STDPROC;
    else if ( isConstruct() )
      return SchemeTokenContext.SCHEME_CONSTRUCT;
    else
      return SchemeTokenContext.SCHEME_VARIABLE;
  }
  
  private static Pattern CONSTRUCT  = Pattern.compile("and|begin|case|cond" +
    "|define|define-syntax|delay|do|else|if|lambda|let|let\\*|letrec" +
    "|letrec-syntax|let-syntax|or|quasiquote|set!|syntax-rules" );
  
  public boolean isConstruct()
  {
    SegmentCharSequence chars = new SegmentCharSequence( buffer, tokenOffset, offset - tokenOffset );
    return CONSTRUCT.matcher( chars ).matches();
  }
  
  private static Pattern STDPROC    = Pattern.compile(
    "-|/|\\*|\\+|acos|angle|apply|asin|atan|" +
    "call-with-current-continuation|call-with-values|car|cdr|ceiling" +
    "|char<=\\?|char<\\?|char=\\?|char>=\\?|char>\\?|char\\?|char->integer" +
    "|char-ready\\?|close-input-port|close-output-port|complex\\?|cons" +
    "|cos|current-input-port|current-output-port|denominator" +
    "|dynamic-wind|eof-object\\?|eq\\?|eqv\\?|eval|exact\\?" +
    "|exact->inexact|exp|expt|floor|imag-part|inexact\\?" +
    "|inexact->exact|input-port\\?|integer\\?|integer->char" +
    "|interaction-environment|load|log|magnitude|make-polar|make-rectangular" +
    "|make-string|make-vector|modulo|null-environment|number\\?" +
    "|number->string|numerator|open-input-file|open-output-file" +
    "|output-port\\?|pair\\?|peek-char|procedure\\?|quotient|rational\\?" +
    "|read-char|real\\?|real-part|remainder|round|scheme-report-environment" +
    "|set-car!|set-cdr!|sin|sqrt|string\\?|string-length|string->number" +
    "|string-ref|string-set!|string->symbol|symbol\\?|symbol->string|tan" +
    "|transcript-off|transcript-on|truncate|values|vector\\?|vector-length" +
    "|vector-ref|vector-set!|with-input-from-file|with-output-to-file" +
    "|write-char|<=|>=" );
  
  private static Pattern STDPROC_SHORTER = Pattern.compile(
    "(<|>|=)", Pattern.DOTALL );
  
  private boolean isStandardProcedure()
  {
    SegmentCharSequence chars = new SegmentCharSequence( buffer, tokenOffset, offset - tokenOffset );
    return STDPROC.matcher( chars ).matches() || STDPROC_SHORTER.matcher( chars ).matches();
  }
  
  private static Pattern HASH = Pattern.compile(
    "#t|#f|#space|#newline|#x[0-9|a-f|A-F]+|#\\[a-z|A-Z]" );
  
  private TokenID categorizeHash()
  {
    SegmentCharSequence chars = new SegmentCharSequence( buffer, tokenOffset, offset - tokenOffset );
    return HASH.matcher( chars ).matches() ? SchemeTokenContext.SCHEME_CONSTANT : SchemeTokenContext.SCHEME_ERROR;
  }
  
  
}
