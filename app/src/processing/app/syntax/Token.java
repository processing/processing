/*
 * Token.java - Generic token
 * Copyright (C) 1998, 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package processing.app.syntax;

/**
 * A linked list of tokens. Each token has three fields - a token
 * identifier, which is a byte value that can be looked up in the
 * array returned by <code>SyntaxDocument.getColors()</code>
 * to get a color value, a length value which is the length of the
 * token in the text, and a pointer to the next token in the list.
 *
 * @author Slava Pestov
 * @version $Id$
 */
public class Token {

        /** Normal text token id. This should be used to mark normal text. */
        public static final byte NULL = 0;

        /** This can be used to mark a comment. */
        public static final byte COMMENT1 = 1;

        /** This can be used to mark a comment. */
        public static final byte COMMENT2 = 2;

        /** Strings in quotes */
        public static final byte LITERAL1 = 3;
        
        /** Constants (QUARTER_PI, CORNERS, etc.) */
        public static final byte LITERAL2 = 4;
        
        /**
         * Label token id. This can be used to mark labels
         * (eg, C mode uses this to mark ...: sequences)
         */
        public static final byte LABEL = 5;

        /** Keywords (void, int, boolean, etc.) */
        public static final byte KEYWORD1 = 6;

        /** Fields [variables within a class] */
        public static final byte KEYWORD2 = 7;

         /** Loop/function-like blocks (for, while, etc.) */
        public static final byte KEYWORD3 = 8;

        /** Processing variables (width, height, focused, etc.) */
        public static final byte KEYWORD4 = 9;
        
        /** Datatypes (int, boolean, etc.) */
        public static final byte KEYWORD5 = 10;

        /** Functions */
        public static final byte FUNCTION1 = 11;

        /** Methods (functions inside a class) */
        public static final byte FUNCTION2 = 12;

        /** Loop/function-like blocks (for, while, etc.) */
        public static final byte FUNCTION3 = 13;

        /**
         * Operator token id. This can be used to mark an
         * operator. (eg, SQL mode marks +, -, etc with this
         * token type)
         */
        public static final byte OPERATOR = 14;

        /**
         * Invalid token id. This can be used to mark invalid
         * or incomplete tokens, so the user can easily spot
         * syntax errors.
         */
        public static final byte INVALID = 15;

        /** The total number of defined token ids. */
        public static final byte ID_COUNT = INVALID + 1;

        /**
         * The first id that can be used for internal state
         * in a token marker.
         */
        public static final byte INTERNAL_FIRST = 100;

        /**
         * The last id that can be used for internal state
         * in a token marker.
         */
        public static final byte INTERNAL_LAST = 126;

        /**
         * The token type, that along with a length of 0
         * marks the end of the token list.
         */
        public static final byte END = 127;

        /**
         * The length of this token.
         */
        public int length;

        /**
         * The id of this token.
         */
        public byte id;

        /**
         * The next token in the linked list.
         */
        public Token next;

        /**
         * Creates a new token.
         * @param length The length of the token
         * @param id The id of the token
         */
        public Token(int length, byte id)
        {
                this.length = length;
                this.id = id;
        }

        /**
         * Returns a string representation of this token.
         */
        public String toString()
        {
                return "[id=" + id + ",length=" + length + "]";
        }
}
