/*
 * TextUtilities.java - Utility functions used by the text area classes
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;


/**
 * Class with several utility functions used by the text area component.
 * @author Slava Pestov
 * @version $Id$
 */
public class TextUtilities
{
        /**
         * Locates the start of the word at the specified position.
         * @param line The text
         * @param pos The position
         */
        public static int findWordStart(String line, int pos, String noWordSep)
        {
                char ch = line.charAt(pos - 1);

                if(noWordSep == null)
                        noWordSep = "";
                boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                        && noWordSep.indexOf(ch) == -1);

                int wordStart = 0;
                for(int i = pos - 1; i >= 0; i--)
                {
                        ch = line.charAt(i);
                        if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                                noWordSep.indexOf(ch) == -1))
                        {
                                wordStart = i + 1;
                                break;
                        }
                }

                return wordStart;
        }

        /**
         * Locates the end of the word at the specified position.
         * @param line The text
         * @param pos The position
         */
        public static int findWordEnd(String line, int pos, String noWordSep)
        {
                char ch = line.charAt(pos);

                if(noWordSep == null)
                        noWordSep = "";
                boolean selectNoLetter = (!Character.isLetterOrDigit(ch)
                        && noWordSep.indexOf(ch) == -1);

                int wordEnd = line.length();
                for(int i = pos; i < line.length(); i++)
                {
                        ch = line.charAt(i);
                        if(selectNoLetter ^ (!Character.isLetterOrDigit(ch) &&
                                noWordSep.indexOf(ch) == -1))
                        {
                                wordEnd = i;
                                break;
                        }
                }
                return wordEnd;
        }
}
