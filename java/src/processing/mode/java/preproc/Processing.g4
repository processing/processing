/**
 *	Based on Java 1.7 grammar for ANTLR 4, see Java.g4
 *
 *	- changes main entry point to reflect sketch types 'static' | 'active'
 *	- adds support for type converter functions like "int()"
 *	- adds pseudo primitive type "color"
 *	- adds HTML hex notation with hash symbol: #ff5522 
 */

grammar Processing;

@lexer::members {
	public static final int WHITESPACE = 1;
	public static final int COMMENTS = 2;
}

// import Java grammar
import Java;

// main entry point, select sketch type
processingSketch
    :   javaProcessingSketch
    |   staticProcessingSketch
    |   activeProcessingSketch
    ;

// java mode, is a compilation unit
javaProcessingSketch
    :   packageDeclaration? importDeclaration* typeDeclaration+ EOF
    ;

// static mode, has statements
staticProcessingSketch
    :   (importDeclaration | blockStatement)* EOF
    ;

// active mode, has function definitions
activeProcessingSketch
	:	(importDeclaration | classBodyDeclaration)* EOF
	;

importDeclaration
    :   'import' importString ';'
    ;
    
// to easily intercept imports in usable format
importString
    :   'static'? packageOrTypeName ('.' '*')?
    ;

variableDeclaratorId
    :   warnTypeAsVariableName
    |   Identifier ('[' ']')*
    ;

// bug #93
// https://github.com/processing/processing/issues/93
// prevent from types being used as variable names
warnTypeAsVariableName
    :   primitiveType ('[' ']')* { 
            notifyErrorListeners("Type names are not allowed as variable names: "+$primitiveType.text); 
        }
    ;

// Catch setup, draw, and settings method declarations
methodDeclaration
    :   specialMethodDeclaration
	|	methodModifier* methodHeader methodBody
	;

specialMethodDeclaration
	:	'public'? 'void' ( 'setup'
		|	'draw'
		|	'settings'
		) '(' ')' methodBody
	;

// catch special API function calls that we are interested in
methodInvocation
    :   apiFunction
    |   functionWithPrimitiveTypeName
	|	methodName '(' argumentList? ')'
	|	typeName '.' typeArguments? Identifier '(' argumentList? ')'
	|	expressionName '.' typeArguments? Identifier '(' argumentList? ')'
	|	primary '.' typeArguments? Identifier '(' argumentList? ')'
	|	'super' '.' typeArguments? Identifier '(' argumentList? ')'
	|	typeName '.' 'super' '.' typeArguments? Identifier '(' argumentList? ')'
	;

methodInvocation_lfno_primary
    :   apiFunction
    |   functionWithPrimitiveTypeName
	|	methodName '(' argumentList? ')'
	|	typeName '.' typeArguments? Identifier '(' argumentList? ')'
	|	expressionName '.' typeArguments? Identifier '(' argumentList? ')'
	|	'super' '.' typeArguments? Identifier '(' argumentList? ')'
	|	typeName '.' 'super' '.' typeArguments? Identifier '(' argumentList? ')'
	;

apiFunction
    :   apiSizeFunction
    ;

apiSizeFunction
    : 'size' '(' expression ',' expression ( ',' expression )? ')'
    ;

// these are primitive type names plus "()"
// "color" is a special Processing primitive (== int)
functionWithPrimitiveTypeName
	:	(	'boolean'
		|	'byte'
		|	'char'
		|	'float'
		|	'int'
        |   'color'
		) '(' argumentList? ')'
	;

// adding support for "color" primitive
integralType
	:	'byte'
	|	'short'
	|	'int'
	|	'long'
	|	'char'
	|   colorPrimitiveType
	;

colorPrimitiveType
    :   'color'
    ;

// added HexColorLiteral
literal
    :   hexColorLiteral
    |	IntegerLiteral
    |   decimalfloatingPointLiteral
    |	FloatingPointLiteral
    |   CharacterLiteral
    |   StringLiteral
    |   BooleanLiteral
    |   'null'
    ;

// As parser rule so this produces a separate listener
// for us to alter its value.
hexColorLiteral
	:	HexColorLiteral
	;

// add color literal notations for
// #ff5522
HexColorLiteral
	:	'#' HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit
	;

// catch floating point numbers in a parser rule
decimalfloatingPointLiteral
	:	DecimalFloatingPointLiteral
	;

// copy from Java.g4 where is is just a fragment
DecimalFloatingPointLiteral
    :   Digits '.' Digits? ExponentPart? FloatTypeSuffix?
    |   '.' Digits ExponentPart? FloatTypeSuffix?
    |   Digits ExponentPart FloatTypeSuffix?
    |   Digits FloatTypeSuffix
    ;

// hide but do not remove whitespace and comments

WS  :  [ \t\r\n\u000C]+ -> channel(WHITESPACE)
    ;

COMMENT
    :   '/*' .*? '*/' -> channel(COMMENTS)
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> channel(COMMENTS)
    ;

