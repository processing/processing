header {
package processing.app.antlr;
import processing.app.preproc.PdePreprocessor;
}
class PdeRecognizer extends Parser;

options {
	importVocab= Java;
	exportVocab= PdePartial;
	codeGenMakeSwitchThreshold=10;
	codeGenBitsetTestThreshold=10;
	defaultErrorHandler= false;
	k= 2;
	buildAST= true;
}

tokens {
    CONSTRUCTOR_CAST; EMPTY_FIELD;
}
{
	// this clause copied from java15.g! ANTLR does not copy this
	// section from the super grammar.
	/**
	 * Counts the number of LT seen in the typeArguments production.
	 * It is used in semantic predicates to ensure we have seen
	 * enough closing '>' characters; which actually may have been
	 * either GT, SR or BSR tokens.
	 */
	private int ltCounter = 0;

	private PdePreprocessor pp;
	public PdeRecognizer(final PdePreprocessor pp, final TokenStream ts) {
	    this(ts);
		this.pp = pp;
	}
	
	private void mixed() throws RecognitionException, TokenStreamException {
		throw new RecognitionException("It looks like you're mixing \"active\" and \"static\" modes.",
	    	                                     getFilename(), LT(1).getLine(), LT(1).getColumn());
	}
}
pdeProgram :// Some programs can be equally well interpreted as STATIC or ACTIVE;
		// this forces the parser to prefer the STATIC interpretation.
        (staticProgram) => staticProgram
        { pp.setMode(PdePreprocessor.Mode.STATIC); }
        
    |   (activeProgram) => activeProgram
        { pp.setMode(PdePreprocessor.Mode.ACTIVE); }
        
    |   staticProgram
        { pp.setMode(PdePreprocessor.Mode.STATIC); }
    ;

javaProgram :compilationUnit
    ;

activeProgram :(
    		(IDENT LPAREN) => IDENT LPAREN { mixed(); }
    	|	possiblyEmptyField
       )+ EOF!
    ;

staticProgram :(
    		(builtInType IDENT LPAREN) =>  builtInType IDENT LPAREN { mixed(); }
		| 	(modifiers) => modifiers builtInType IDENT LPAREN { mixed(); }    	
 		| 	statement
 	   )* EOF!
    ;

constant :NUM_INT
    |   CHAR_LITERAL
    |   STRING_LITERAL
    |   NUM_FLOAT
    |   NUM_LONG
    |   NUM_DOUBLE
    |   webcolor_literal
    ;

typeArguments {int currentLtLevel = 0;}
:{currentLtLevel = ltCounter;}
		LT! {ltCounter++;}
		typeArgument
		(options{greedy=true;}: // match as many as possible
			{if (! (inputState.guessing !=0 || ltCounter == currentLtLevel + 1)) {
				throw new RecognitionException("Maybe too many > characters?",
	    	                                     getFilename(), LT(1).getLine(), LT(1).getColumn());
			}}
			COMMA! typeArgument
		)*

		(	// turn warning off since Antlr generates the right code,
			// plus we have our semantic predicate below
			options{generateAmbigWarnings=false;}:
			typeArgumentsOrParametersEnd
		)?

		// make sure we have gobbled up enough '>' characters
		// if we are at the "top level" of nested typeArgument productions
		{if (! ((currentLtLevel != 0) || ltCounter == currentLtLevel)) {
			throw new RecognitionException("Maybe too many > characters?",
    	                                     getFilename(), LT(1).getLine(), LT(1).getColumn());
		}}

		{#typeArguments = #(#[TYPE_ARGUMENTS, "TYPE_ARGUMENTS"], #typeArguments);}
	;

typeParameters {int currentLtLevel = 0;}
:{currentLtLevel = ltCounter;}
		LT! {ltCounter++;}
		typeParameter (COMMA! typeParameter)*
		(typeArgumentsOrParametersEnd)?

		// make sure we have gobbled up enough '>' characters
		// if we are at the "top level" of nested typeArgument productions
		{if (! ((currentLtLevel != 0) || ltCounter == currentLtLevel)) {
			throw new RecognitionException("Maybe too many > characters?",
    	                                     getFilename(), LT(1).getLine(), LT(1).getColumn());
		}}

		{#typeParameters = #(#[TYPE_PARAMETERS, "TYPE_PARAMETERS"], #typeParameters);}
	;

protected typeArgumentsOrParametersEnd :GT! {ltCounter-=1;}
	|	SR! {ltCounter-=2;}
	|	BSR! {ltCounter-=3;}
	;

webcolor_literal :w:WEBCOLOR_LITERAL 
    { if (! (processing.app.Preferences.getBoolean("preproc.web_colors") 
    		 && 
        	 w.getText().length() == 6)) {
		throw new RecognitionException("Web colors must be exactly 6 hex digits. This looks like " + w.getText().length() + ".",
	                                     getFilename(), LT(1).getLine(), LT(1).getColumn());
     }}  // must be exactly 6 hex digits
    ;

builtInConsCastType :"void"
    |   "boolean"
    |   "byte"
    |   "char"
    |   "short"
    |   "int"
    |   "float"
    |   "long"
    |   "double"
    ;

builtInType :builtInConsCastType
    |   "color"              // aliased to an int in PDE
        { processing.app.Preferences.getBoolean("preproc.color_datatype") }? 
    ;

constructorCast! :t:consCastTypeSpec[true]
        LPAREN!
        e:expression
        RPAREN!
        // if this is a string literal, make sure the type we're trying to cast
        // to is one of the supported ones
        //
        { #e.getType() != STRING_LITERAL ||
            ( #t.getType() == LITERAL_byte ||
              #t.getType() == LITERAL_double ||
              #t.getType() == LITERAL_float ||
              #t.getType() == LITERAL_int ||
              #t.getType() == LITERAL_long ||
              #t.getType() == LITERAL_short ) }?
        // create the node
        //
        {#constructorCast = #(#[CONSTRUCTOR_CAST,"CONSTRUCTOR_CAST"], t, e);}
    ;

consCastTypeSpec[boolean addImagNode] :builtInConsCastTypeSpec[addImagNode]
// trying to remove String() cast [fry]
	;

builtInConsCastTypeSpec[boolean addImagNode] :builtInConsCastType
             {
                 if ( addImagNode ) {
                     #builtInConsCastTypeSpec = #(#[TYPE,"TYPE"],
                                                  #builtInConsCastTypeSpec);
                 }
             }
    ;

colorMethodCall :c:"color" {#c.setType(IDENT);} // this would default to LITERAL_color
      lp:LPAREN^ {#lp.setType(METHOD_CALL);}
      argList
      RPAREN!
    ;

primaryExpression :(consCastTypeSpec[false] LPAREN) => constructorCast   
            { processing.app.Preferences.getBoolean("preproc.enhanced_casting") }?
    |   identPrimary ( options {greedy=true;} : DOT^ "class" )?
    |   constant
    |   "true"
    |   "false"
    |   "null"
    |   newExpression
    |   "this"
    |   "super"
    |   LPAREN! assignmentExpression RPAREN!
    |   colorMethodCall
        // look for int.class and int[].class
    |   builtInType
        ( lbt:LBRACK^ {#lbt.setType(ARRAY_DECLARATOR);} RBRACK! )*
        DOT^ "class"
    ;

variableDefinitions![AST mods, AST t] :vd:variableDeclarator[getASTFactory().dupTree(mods),
                              getASTFactory().dupTree(t)]
        {#variableDefinitions = #(#[VARIABLE_DEF,"VARIABLE_DEF"], mods, 
                                  t, vd);}
    ;

variableDeclarator[AST mods, AST t] :( id:IDENT 	(lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!)*
        v:varInitializer (COMMA!)? )+
    ;

explicitConstructorInvocation! :(typeArguments)?
        t:"this" LPAREN a1:argList RPAREN SEMI
        {#explicitConstructorInvocation = #(#[CTOR_CALL, "CTOR_CALL"], 
                                            #t, #a1);}
    |   s:"super" LPAREN a2:argList RPAREN SEMI
        {#explicitConstructorInvocation = #(#[SUPER_CTOR_CALL, 
                                              "SUPER_CTOR_CALL"], 
                                            #s, #a2);}
    ;

classDefinition![AST modifiers] :"class" i:IDENT
        // it _might_ have type paramaters
		(tp:typeParameters)?
		// it _might_ have a superclass...
        sc:superClassClause
        // it might implement some interfaces...
        ic:implementsClause
        // now parse the body of the class
        cb:classBlock
        {#classDefinition = #(#[CLASS_DEF,"CLASS_DEF"],
                              modifiers,i,tp,sc,ic,cb);
         pp.setAdvClassName(i.getText());}
    ;

possiblyEmptyField :classField
    | s:SEMI {#s.setType(EMPTY_FIELD);}
    ;

// inherited from grammar JavaRecognizer
compilationUnit :// A compilation unit starts with an optional package definition
		(	(annotations "package")=> packageDefinition
		|	/* nothing */
		)

		// Next we have a series of zero or more import statements
		( importDefinition )*

		// Wrapping things up with any number of class or interface
		// definitions
		( typeDefinition )*

		EOF!
	;

// inherited from grammar JavaRecognizer
packageDefinition 
options {
	defaultErrorHandler= true;
}
:annotations p:"package"^ {#p.setType(PACKAGE_DEF);} identifier SEMI!
	;

// inherited from grammar JavaRecognizer
importDefinition 
options {
	defaultErrorHandler= true;
}
{ boolean isStatic = false; }
:i:"import"^ {#i.setType(IMPORT);} ( "static"! {#i.setType(STATIC_IMPORT);} )? identifierStar SEMI!
	;

// inherited from grammar JavaRecognizer
typeDefinition 
options {
	defaultErrorHandler= true;
}
:m:modifiers!
		typeDefinitionInternal[#m]
	|	SEMI!
	;

// inherited from grammar JavaRecognizer
protected typeDefinitionInternal[AST mods] :classDefinition[#mods]		// inner class
	|	interfaceDefinition[#mods]	// inner interface
	|	enumDefinition[#mods]		// inner enum
	|	annotationDefinition[#mods]	// inner annotation
	;

// inherited from grammar JavaRecognizer
declaration! :m:modifiers t:typeSpec[false] v:variableDefinitions[#m,#t]
		{#declaration = #v;}
	;

// inherited from grammar JavaRecognizer
typeSpec[boolean addImagNode] :classTypeSpec[addImagNode]
	|	builtInTypeSpec[addImagNode]
	;

// inherited from grammar JavaRecognizer
classTypeSpec[boolean addImagNode] :classOrInterfaceType[false]
		(options{greedy=true;}: // match as many as possible
			lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!
		)*
		{
			if ( addImagNode ) {
				#classTypeSpec = #(#[TYPE,"TYPE"], #classTypeSpec);
			}
		}
	;

// inherited from grammar JavaRecognizer
classOrInterfaceType[boolean addImagNode] :IDENT (typeArguments)?
		(options{greedy=true;}: // match as many as possible
			DOT^
			IDENT (typeArguments)?
		)*
		{
			if ( addImagNode ) {
				#classOrInterfaceType = #(#[TYPE,"TYPE"], #classOrInterfaceType);
			}
		}
	;

// inherited from grammar JavaRecognizer
typeArgumentSpec :classTypeSpec[true]
	|	builtInTypeArraySpec[true]
	;

// inherited from grammar JavaRecognizer
typeArgument :(	typeArgumentSpec
		|	wildcardType
		)
		{#typeArgument = #(#[TYPE_ARGUMENT,"TYPE_ARGUMENT"], #typeArgument);}
	;

// inherited from grammar JavaRecognizer
wildcardType :q:QUESTION^ {#q.setType(WILDCARD_TYPE);}
		(("extends" | "super")=> typeArgumentBounds)?
	;

// inherited from grammar JavaRecognizer
typeArgumentBounds {boolean isUpperBounds = false;}
:( "extends"! {isUpperBounds=true;} | "super"! ) classOrInterfaceType[false]
		{
			if (isUpperBounds)
			{
				#typeArgumentBounds = #(#[TYPE_UPPER_BOUNDS,"TYPE_UPPER_BOUNDS"], #typeArgumentBounds);
			}
			else
			{
				#typeArgumentBounds = #(#[TYPE_LOWER_BOUNDS,"TYPE_LOWER_BOUNDS"], #typeArgumentBounds);
			}
		}
	;

// inherited from grammar JavaRecognizer
builtInTypeArraySpec[boolean addImagNode] :builtInType
		(options{greedy=true;}: // match as many as possible
			lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!
		)+

		{
			if ( addImagNode ) {
				#builtInTypeArraySpec = #(#[TYPE,"TYPE"], #builtInTypeArraySpec);
			}
		}
	;

// inherited from grammar JavaRecognizer
builtInTypeSpec[boolean addImagNode] :builtInType
		(options{greedy=true;}: // match as many as possible
			lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!
		)*
		{
			if ( addImagNode ) {
				#builtInTypeSpec = #(#[TYPE,"TYPE"], #builtInTypeSpec);
			}
		}
	;

// inherited from grammar JavaRecognizer
type :classOrInterfaceType[false]
	|	builtInType
	;

// inherited from grammar JavaRecognizer
identifier :IDENT ( DOT^ IDENT )*
	;

// inherited from grammar JavaRecognizer
identifierStar :IDENT
		( DOT^ IDENT )*
		( DOT^ STAR )?
	;

// inherited from grammar JavaRecognizer
modifiers :(
			//hush warnings since the semantic check for "@interface" solves the non-determinism
			options{generateAmbigWarnings=false;}:

			modifier
			|
			//Semantic check that we aren't matching @interface as this is not an annotation
			//A nicer way to do this would be nice
			{LA(1)==AT && !LT(2).getText().equals("interface")}? annotation
		)*

		{#modifiers = #([MODIFIERS, "MODIFIERS"], #modifiers);}
	;

// inherited from grammar JavaRecognizer
modifier :"private"
	|	"public"
	|	"protected"
	|	"static"
	|	"transient"
	|	"final"
	|	"abstract"
	|	"native"
	|	"threadsafe"
	|	"synchronized"
	|	"volatile"
	|	"strictfp"
	;

// inherited from grammar JavaRecognizer
annotation! :AT! i:identifier ( LPAREN! ( args:annotationArguments )? RPAREN! )?
		{#annotation = #(#[ANNOTATION,"ANNOTATION"], i, args);}
	;

// inherited from grammar JavaRecognizer
annotations :(annotation)*
		{#annotations = #([ANNOTATIONS, "ANNOTATIONS"], #annotations);}
    ;

// inherited from grammar JavaRecognizer
annotationArguments :annotationMemberValueInitializer | anntotationMemberValuePairs
	;

// inherited from grammar JavaRecognizer
anntotationMemberValuePairs :annotationMemberValuePair ( COMMA! annotationMemberValuePair )*
	;

// inherited from grammar JavaRecognizer
annotationMemberValuePair! :i:IDENT ASSIGN! v:annotationMemberValueInitializer
		{#annotationMemberValuePair = #(#[ANNOTATION_MEMBER_VALUE_PAIR,"ANNOTATION_MEMBER_VALUE_PAIR"], i, v);}
	;

// inherited from grammar JavaRecognizer
annotationMemberValueInitializer :conditionalExpression | annotation | annotationMemberArrayInitializer
	;

// inherited from grammar JavaRecognizer
annotationMemberArrayInitializer :lc:LCURLY^ {#lc.setType(ANNOTATION_ARRAY_INIT);}
			(	annotationMemberArrayValueInitializer
				(
					// CONFLICT: does a COMMA after an initializer start a new
					// initializer or start the option ',' at end?
					// ANTLR generates proper code by matching
					// the comma as soon as possible.
					options {
						warnWhenFollowAmbig = false;
					}
				:
					COMMA! annotationMemberArrayValueInitializer
				)*
				(COMMA!)?
			)?
		RCURLY!
	;

// inherited from grammar JavaRecognizer
annotationMemberArrayValueInitializer :conditionalExpression
	|	annotation
	;

// inherited from grammar JavaRecognizer
superClassClause! :( "extends" c:classOrInterfaceType[false] )?
		{#superClassClause = #(#[EXTENDS_CLAUSE,"EXTENDS_CLAUSE"],c);}
	;

// inherited from grammar JavaRecognizer
interfaceDefinition![AST modifiers] :"interface" IDENT
		// it _might_ have type paramaters
		(tp:typeParameters)?
		// it might extend some other interfaces
		ie:interfaceExtends
		// now parse the body of the interface (looks like a class...)
		ib:interfaceBlock
		{#interfaceDefinition = #(#[INTERFACE_DEF,"INTERFACE_DEF"],
									modifiers,IDENT,tp,ie,ib);}
	;

// inherited from grammar JavaRecognizer
enumDefinition![AST modifiers] :"enum" IDENT
		// it might implement some interfaces...
		ic:implementsClause
		// now parse the body of the enum
		eb:enumBlock
		{#enumDefinition = #(#[ENUM_DEF,"ENUM_DEF"],
								modifiers,IDENT,ic,eb);}
	;

// inherited from grammar JavaRecognizer
annotationDefinition![AST modifiers] :AT "interface" IDENT
		// now parse the body of the annotation
		ab:annotationBlock
		{#annotationDefinition = #(#[ANNOTATION_DEF,"ANNOTATION_DEF"],
									modifiers,IDENT,ab);}
	;

// inherited from grammar JavaRecognizer
typeParameter :// I'm pretty sure Antlr generates the right thing here:
		(id:IDENT) ( options{generateAmbigWarnings=false;}: typeParameterBounds )?
		{#typeParameter = #(#[TYPE_PARAMETER,"TYPE_PARAMETER"], #typeParameter);}
	;

// inherited from grammar JavaRecognizer
typeParameterBounds :"extends"! classOrInterfaceType[false]
		(BAND! classOrInterfaceType[false])*
		{#typeParameterBounds = #(#[TYPE_UPPER_BOUNDS,"TYPE_UPPER_BOUNDS"], #typeParameterBounds);}
	;

// inherited from grammar JavaRecognizer
classBlock :LCURLY!
			( classField | SEMI! )*
		RCURLY!
		{#classBlock = #([OBJBLOCK, "OBJBLOCK"], #classBlock);}
	;

// inherited from grammar JavaRecognizer
interfaceBlock :LCURLY!
			( interfaceField | SEMI! )*
		RCURLY!
		{#interfaceBlock = #([OBJBLOCK, "OBJBLOCK"], #interfaceBlock);}
	;

// inherited from grammar JavaRecognizer
annotationBlock :LCURLY!
		( annotationField | SEMI! )*
		RCURLY!
		{#annotationBlock = #([OBJBLOCK, "OBJBLOCK"], #annotationBlock);}
	;

// inherited from grammar JavaRecognizer
enumBlock :LCURLY!
			( enumConstant ( options{greedy=true;}: COMMA! enumConstant )* ( COMMA! )? )?
			( SEMI! ( classField | SEMI! )* )?
		RCURLY!
		{#enumBlock = #([OBJBLOCK, "OBJBLOCK"], #enumBlock);}
	;

// inherited from grammar JavaRecognizer
annotationField! :mods:modifiers
		(	td:typeDefinitionInternal[#mods]
			{#annotationField = #td;}
		|	t:typeSpec[false]		// annotation field
			(	i:IDENT				// the name of the field

				LPAREN! RPAREN!

				rt:declaratorBrackets[#t]

				( "default" amvi:annotationMemberValueInitializer )?

				SEMI

				{#annotationField =
					#(#[ANNOTATION_FIELD_DEF,"ANNOTATION_FIELD_DEF"],
						 mods,
						 #(#[TYPE,"TYPE"],rt),
						 i,amvi
						 );}
			|	v:variableDefinitions[#mods,#t] SEMI	// variable
				{#annotationField = #v;}
			)
		)
	;

// inherited from grammar JavaRecognizer
enumConstant! :an:annotations
		i:IDENT
		(	LPAREN!
			a:argList
			RPAREN!
		)?
		( b:enumConstantBlock )?
		{#enumConstant = #([ENUM_CONSTANT_DEF, "ENUM_CONSTANT_DEF"], an, i, a, b);}
	;

// inherited from grammar JavaRecognizer
enumConstantBlock :LCURLY!
		( enumConstantField | SEMI! )*
		RCURLY!
		{#enumConstantBlock = #([OBJBLOCK, "OBJBLOCK"], #enumConstantBlock);}
	;

// inherited from grammar JavaRecognizer
enumConstantField! :mods:modifiers
		(	td:typeDefinitionInternal[#mods]
			{#enumConstantField = #td;}

		|	// A generic method has the typeParameters before the return type.
			// This is not allowed for variable definitions, but this production
			// allows it, a semantic check could be used if you wanted.
			(tp:typeParameters)? t:typeSpec[false]		// method or variable declaration(s)
			(	IDENT									// the name of the method

				// parse the formal parameter declarations.
				LPAREN! param:parameterDeclarationList RPAREN!

				rt:declaratorBrackets[#t]

				// get the list of exceptions that this method is
				// declared to throw
				(tc:throwsClause)?

				( s2:compoundStatement | SEMI )
				{#enumConstantField = #(#[METHOD_DEF,"METHOD_DEF"],
							 mods,
							 tp,
							 #(#[TYPE,"TYPE"],rt),
							 IDENT,
							 param,
							 tc,
							 s2);}
			|	v:variableDefinitions[#mods,#t] SEMI
				{#enumConstantField = #v;}
			)
		)

	// "{ ... }" instance initializer
	|	s4:compoundStatement
		{#enumConstantField = #(#[INSTANCE_INIT,"INSTANCE_INIT"], s4);}
	;

// inherited from grammar JavaRecognizer
interfaceExtends :(
		e:"extends"!
		classOrInterfaceType[false] ( COMMA! classOrInterfaceType[false] )*
		)?
		{#interfaceExtends = #(#[EXTENDS_CLAUSE,"EXTENDS_CLAUSE"],
								#interfaceExtends);}
	;

// inherited from grammar JavaRecognizer
implementsClause :(
			i:"implements"! classOrInterfaceType[false] ( COMMA! classOrInterfaceType[false] )*
		)?
		{#implementsClause = #(#[IMPLEMENTS_CLAUSE,"IMPLEMENTS_CLAUSE"],
								 #implementsClause);}
	;

// inherited from grammar JavaRecognizer
classField! :// method, constructor, or variable declaration
		mods:modifiers
		(	td:typeDefinitionInternal[#mods]
			{#classField = #td;}

		|	(tp:typeParameters)?
			(
				h:ctorHead s:constructorBody // constructor
				{#classField = #(#[CTOR_DEF,"CTOR_DEF"], mods, tp, h, s);}

				|	// A generic method/ctor has the typeParameters before the return type.
					// This is not allowed for variable definitions, but this production
					// allows it, a semantic check could be used if you wanted.
					t:typeSpec[false]		// method or variable declaration(s)
					(	IDENT				// the name of the method

						// parse the formal parameter declarations.
						LPAREN! param:parameterDeclarationList RPAREN!

						rt:declaratorBrackets[#t]

						// get the list of exceptions that this method is
						// declared to throw
						(tc:throwsClause)?

						( s2:compoundStatement | SEMI )
						{#classField = #(#[METHOD_DEF,"METHOD_DEF"],
									 mods,
									 tp,
									 #(#[TYPE,"TYPE"],rt),
									 IDENT,
									 param,
									 tc,
									 s2);}
					|	v:variableDefinitions[#mods,#t] SEMI
						{#classField = #v;}
					)
			)
		)

	// "static { ... }" class initializer
	|	"static" s3:compoundStatement
		{#classField = #(#[STATIC_INIT,"STATIC_INIT"], s3);}

	// "{ ... }" instance initializer
	|	s4:compoundStatement
		{#classField = #(#[INSTANCE_INIT,"INSTANCE_INIT"], s4);}
	;

// inherited from grammar JavaRecognizer
interfaceField! :// method, constructor, or variable declaration
		mods:modifiers
		(	td:typeDefinitionInternal[#mods]
			{#interfaceField = #td;}

		|	(tp:typeParameters)?
			// A generic method has the typeParameters before the return type.
			// This is not allowed for variable definitions, but this production
			// allows it, a semantic check could be used if you want a more strict
			// grammar.
			t:typeSpec[false]		// method or variable declaration(s)
			(	IDENT				// the name of the method

				// parse the formal parameter declarations.
				LPAREN! param:parameterDeclarationList RPAREN!

				rt:declaratorBrackets[#t]

				// get the list of exceptions that this method is
				// declared to throw
				(tc:throwsClause)?

				SEMI
				
				{#interfaceField = #(#[METHOD_DEF,"METHOD_DEF"],
							 mods,
							 tp,
							 #(#[TYPE,"TYPE"],rt),
							 IDENT,
							 param,
							 tc);}
			|	v:variableDefinitions[#mods,#t] SEMI
				{#interfaceField = #v;}
			)
		)
	;

// inherited from grammar JavaRecognizer
constructorBody :lc:LCURLY^ {#lc.setType(SLIST);}
			( options { greedy=true; } : explicitConstructorInvocation)?
			(statement)*
		RCURLY!
	;

// inherited from grammar JavaRecognizer
declaratorBrackets[AST typ] :{#declaratorBrackets=typ;}
		(lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);} RBRACK!)*
	;

// inherited from grammar JavaRecognizer
varInitializer :( ASSIGN^ initializer )?
	;

// inherited from grammar JavaRecognizer
arrayInitializer :lc:LCURLY^ {#lc.setType(ARRAY_INIT);}
			(	initializer
				(
					// CONFLICT: does a COMMA after an initializer start a new
					// initializer or start the option ',' at end?
					// ANTLR generates proper code by matching
					// the comma as soon as possible.
					options {
						warnWhenFollowAmbig = false;
					}
				:
					COMMA! initializer
				)*
				(COMMA!)?
			)?
		RCURLY!
	;

// inherited from grammar JavaRecognizer
initializer :expression
	|	arrayInitializer
	;

// inherited from grammar JavaRecognizer
ctorHead :IDENT // the name of the method

		// parse the formal parameter declarations.
		LPAREN! parameterDeclarationList RPAREN!

		// get the list of exceptions that this method is declared to throw
		(throwsClause)?
	;

// inherited from grammar JavaRecognizer
throwsClause :"throws"^ identifier ( COMMA! identifier )*
	;

// inherited from grammar JavaRecognizer
parameterDeclarationList :(	( parameterDeclaration )=> parameterDeclaration
			( options {warnWhenFollowAmbig=false;} : ( COMMA! parameterDeclaration ) => COMMA! parameterDeclaration )*
			( COMMA! variableLengthParameterDeclaration )?
		|
			variableLengthParameterDeclaration
		)?
		{#parameterDeclarationList = #(#[PARAMETERS,"PARAMETERS"],
									#parameterDeclarationList);}
	;

// inherited from grammar JavaRecognizer
parameterDeclaration! :pm:parameterModifier t:typeSpec[false] id:IDENT
		pd:declaratorBrackets[#t]
		{#parameterDeclaration = #(#[PARAMETER_DEF,"PARAMETER_DEF"],
									pm, #([TYPE,"TYPE"],pd), id);}
	;

// inherited from grammar JavaRecognizer
variableLengthParameterDeclaration! :pm:parameterModifier t:typeSpec[false] TRIPLE_DOT! id:IDENT
		pd:declaratorBrackets[#t]
		{#variableLengthParameterDeclaration = #(#[VARIABLE_PARAMETER_DEF,"VARIABLE_PARAMETER_DEF"],
												pm, #([TYPE,"TYPE"],pd), id);}
	;

// inherited from grammar JavaRecognizer
parameterModifier :(options{greedy=true;} : annotation)* (f:"final")? (annotation)*
		{#parameterModifier = #(#[MODIFIERS,"MODIFIERS"], #parameterModifier);}
	;

// inherited from grammar JavaRecognizer
compoundStatement :lc:LCURLY^ {#lc.setType(SLIST);}
			// include the (possibly-empty) list of statements
			(statement)*
		RCURLY!
	;

// inherited from grammar JavaRecognizer
statement :compoundStatement

	// declarations are ambiguous with "ID DOT" relative to expression
	// statements. Must backtrack to be sure. Could use a semantic
	// predicate to test symbol table to see what the type was coming
	// up, but that's pretty hard without a symbol table ;)
	|	(declaration)=> declaration SEMI!

	// An expression statement. This could be a method call,
	// assignment statement, or any other expression evaluated for
	// side-effects.
	|	expression SEMI!

	//TODO: what abour interfaces, enums and annotations
	// class definition
	|	m:modifiers! classDefinition[#m]

	// Attach a label to the front of a statement
	|	IDENT c:COLON^ {#c.setType(LABELED_STAT);} statement

	// If-else statement
	|	"if"^ LPAREN! expression RPAREN! statement
		(
			// CONFLICT: the old "dangling-else" problem...
			// ANTLR generates proper code matching
			// as soon as possible. Hush warning.
			options {
				warnWhenFollowAmbig = false;
			}
		:
			"else"! statement
		)?

	// For statement
	|	forStatement

	// While statement
	|	"while"^ LPAREN! expression RPAREN! statement

	// do-while statement
	|	"do"^ statement "while"! LPAREN! expression RPAREN! SEMI!

	// get out of a loop (or switch)
	|	"break"^ (IDENT)? SEMI!

	// do next iteration of a loop
	|	"continue"^ (IDENT)? SEMI!

	// Return an expression
	|	"return"^ (expression)? SEMI!

	// switch/case statement
	|	"switch"^ LPAREN! expression RPAREN! LCURLY!
			( casesGroup )*
		RCURLY!

	// exception try-catch block
	|	tryBlock

	// throw an exception
	|	"throw"^ expression SEMI!

	// synchronize a statement
	|	"synchronized"^ LPAREN! expression RPAREN! compoundStatement

	// asserts (uncomment if you want 1.4 compatibility)
	|	"assert"^ expression ( COLON! expression )? SEMI!

	// empty statement
	|	s:SEMI {#s.setType(EMPTY_STAT);}
	;

// inherited from grammar JavaRecognizer
forStatement :f:"for"^
		LPAREN!
			(	(forInit SEMI)=>traditionalForClause
			|	forEachClause
			)
		RPAREN!
		statement					 // statement to loop over
	;

// inherited from grammar JavaRecognizer
traditionalForClause :forInit SEMI!	// initializer
		forCond SEMI!	// condition test
		forIter			// updater
	;

// inherited from grammar JavaRecognizer
forEachClause :p:parameterDeclaration COLON! expression
		{#forEachClause = #(#[FOR_EACH_CLAUSE,"FOR_EACH_CLAUSE"], #forEachClause);}
	;

// inherited from grammar JavaRecognizer
casesGroup :(	// CONFLICT: to which case group do the statements bind?
			// ANTLR generates proper code: it groups the
			// many "case"/"default" labels together then
			// follows them with the statements
			options {
				greedy = true;
			}
			:
			aCase
		)+
		caseSList
		{#casesGroup = #([CASE_GROUP, "CASE_GROUP"], #casesGroup);}
	;

// inherited from grammar JavaRecognizer
aCase :("case"^ expression | "default") COLON!
	;

// inherited from grammar JavaRecognizer
caseSList :(statement)*
		{#caseSList = #(#[SLIST,"SLIST"],#caseSList);}
	;

// inherited from grammar JavaRecognizer
forInit :((declaration)=> declaration
		// otherwise it could be an expression list...
		|	expressionList
		)?
		{#forInit = #(#[FOR_INIT,"FOR_INIT"],#forInit);}
	;

// inherited from grammar JavaRecognizer
forCond :(expression)?
		{#forCond = #(#[FOR_CONDITION,"FOR_CONDITION"],#forCond);}
	;

// inherited from grammar JavaRecognizer
forIter :(expressionList)?
		{#forIter = #(#[FOR_ITERATOR,"FOR_ITERATOR"],#forIter);}
	;

// inherited from grammar JavaRecognizer
tryBlock :"try"^ compoundStatement
		(handler)*
		( finallyClause )?
	;

// inherited from grammar JavaRecognizer
finallyClause :"finally"^ compoundStatement
	;

// inherited from grammar JavaRecognizer
handler :"catch"^ LPAREN! parameterDeclaration RPAREN! compoundStatement
	;

// inherited from grammar JavaRecognizer
expression :assignmentExpression
		{#expression = #(#[EXPR,"EXPR"],#expression);}
	;

// inherited from grammar JavaRecognizer
expressionList :expression (COMMA! expression)*
		{#expressionList = #(#[ELIST,"ELIST"], expressionList);}
	;

// inherited from grammar JavaRecognizer
assignmentExpression :conditionalExpression
		(	(	ASSIGN^
			|	PLUS_ASSIGN^
			|	MINUS_ASSIGN^
			|	STAR_ASSIGN^
			|	DIV_ASSIGN^
			|	MOD_ASSIGN^
			|	SR_ASSIGN^
			|	BSR_ASSIGN^
			|	SL_ASSIGN^
			|	BAND_ASSIGN^
			|	BXOR_ASSIGN^
			|	BOR_ASSIGN^
			)
			assignmentExpression
		)?
	;

// inherited from grammar JavaRecognizer
conditionalExpression :logicalOrExpression
		( QUESTION^ assignmentExpression COLON! conditionalExpression )?
	;

// inherited from grammar JavaRecognizer
logicalOrExpression :logicalAndExpression (LOR^ logicalAndExpression)*
	;

// inherited from grammar JavaRecognizer
logicalAndExpression :inclusiveOrExpression (LAND^ inclusiveOrExpression)*
	;

// inherited from grammar JavaRecognizer
inclusiveOrExpression :exclusiveOrExpression (BOR^ exclusiveOrExpression)*
	;

// inherited from grammar JavaRecognizer
exclusiveOrExpression :andExpression (BXOR^ andExpression)*
	;

// inherited from grammar JavaRecognizer
andExpression :equalityExpression (BAND^ equalityExpression)*
	;

// inherited from grammar JavaRecognizer
equalityExpression :relationalExpression ((NOT_EQUAL^ | EQUAL^) relationalExpression)*
	;

// inherited from grammar JavaRecognizer
relationalExpression :shiftExpression
		(	(	(	LT^
				|	GT^
				|	LE^
				|	GE^
				)
				shiftExpression
			)*
		|	"instanceof"^ typeSpec[true]
		)
	;

// inherited from grammar JavaRecognizer
shiftExpression :additiveExpression ((SL^ | SR^ | BSR^) additiveExpression)*
	;

// inherited from grammar JavaRecognizer
additiveExpression :multiplicativeExpression ((PLUS^ | MINUS^) multiplicativeExpression)*
	;

// inherited from grammar JavaRecognizer
multiplicativeExpression :unaryExpression ((STAR^ | DIV^ | MOD^ ) unaryExpression)*
	;

// inherited from grammar JavaRecognizer
unaryExpression :INC^ unaryExpression
	|	DEC^ unaryExpression
	|	MINUS^ {#MINUS.setType(UNARY_MINUS);} unaryExpression
	|	PLUS^ {#PLUS.setType(UNARY_PLUS);} unaryExpression
	|	unaryExpressionNotPlusMinus
	;

// inherited from grammar JavaRecognizer
unaryExpressionNotPlusMinus :BNOT^ unaryExpression
	|	LNOT^ unaryExpression
	|	(	// subrule allows option to shut off warnings
			options {
				// "(int" ambig with postfixExpr due to lack of sequence
				// info in linear approximate LL(k). It's ok. Shut up.
				generateAmbigWarnings=false;
			}
		:	// If typecast is built in type, must be numeric operand
			// Have to backtrack to see if operator follows
		(LPAREN builtInTypeSpec[true] RPAREN unaryExpression)=>
		lpb:LPAREN^ {#lpb.setType(TYPECAST);} builtInTypeSpec[true] RPAREN!
		unaryExpression

		// Have to backtrack to see if operator follows. If no operator
		// follows, it's a typecast. No semantic checking needed to parse.
		// if it _looks_ like a cast, it _is_ a cast; else it's a "(expr)"
	|	(LPAREN classTypeSpec[true] RPAREN unaryExpressionNotPlusMinus)=>
		lp:LPAREN^ {#lp.setType(TYPECAST);} classTypeSpec[true] RPAREN!
		unaryExpressionNotPlusMinus

	|	postfixExpression
	)
	;

// inherited from grammar JavaRecognizer
postfixExpression :primaryExpression

		(
			/*
			options {
				// the use of postfixExpression in SUPER_CTOR_CALL adds DOT
				// to the lookahead set, and gives loads of false non-det
				// warnings.
				// shut them off.
				generateAmbigWarnings=false;
			}
		:	*/
			//type arguments are only appropriate for a parameterized method/ctor invocations
			//semantic check may be needed here to ensure that this is the case
			DOT^ (typeArguments)?
				(	IDENT
					(	lp:LPAREN^ {#lp.setType(METHOD_CALL);}
						argList
						RPAREN!
					)?
				|	"super"
					(	// (new Outer()).super() (create enclosing instance)
						lp3:LPAREN^ argList RPAREN!
						{#lp3.setType(SUPER_CTOR_CALL);}
					|	DOT^ (typeArguments)? IDENT
						(	lps:LPAREN^ {#lps.setType(METHOD_CALL);}
							argList
							RPAREN!
						)?
					)
				)
		|	DOT^ "this"
		|	DOT^ newExpression
		|	lb:LBRACK^ {#lb.setType(INDEX_OP);} expression RBRACK!
		)*

		(	// possibly add on a post-increment or post-decrement.
			// allows INC/DEC on too much, but semantics can check
			in:INC^ {#in.setType(POST_INC);}
	 	|	de:DEC^ {#de.setType(POST_DEC);}
		)?
 	;

// inherited from grammar JavaRecognizer
identPrimary :(ta1:typeArguments!)?
		IDENT
		// Syntax for method invocation with type arguments is
		// <String>foo("blah")
		(
			options {
				// .ident could match here or in postfixExpression.
				// We do want to match here. Turn off warning.
				greedy=true;
				// This turns the ambiguity warning of the second alternative
				// off. See below. (The "false" predicate makes it non-issue)
				warnWhenFollowAmbig=false;
			}
			// we have a new nondeterminism because of
			// typeArguments... only a syntactic predicate will help...
			// The problem is that this loop here conflicts with
			// DOT typeArguments "super" in postfixExpression (k=2)
			// A proper solution would require a lot of refactoring...
		:	(DOT (typeArguments)? IDENT) =>
				DOT^ (ta2:typeArguments!)? IDENT
		|	{false}?	// FIXME: this is very ugly but it seems to work...
						// this will also produce an ANTLR warning!
				// Unfortunately a syntactic predicate can only select one of
				// multiple alternatives on the same level, not break out of
				// an enclosing loop, which is why this ugly hack (a fake
				// empty alternative with always-false semantic predicate)
				// is necessary.
		)*
		(
			options {
				// ARRAY_DECLARATOR here conflicts with INDEX_OP in
				// postfixExpression on LBRACK RBRACK.
				// We want to match [] here, so greedy. This overcomes
				// limitation of linear approximate lookahead.
				greedy=true;
			}
		:	(	lp:LPAREN^ {#lp.setType(METHOD_CALL);}
				// if the input is valid, only the last IDENT may
				// have preceding typeArguments... rather hacky, this is...
				{if (#ta2 != null) astFactory.addASTChild(currentAST, #ta2);}
				{if (#ta2 == null) astFactory.addASTChild(currentAST, #ta1);}
				argList RPAREN!
			)
		|	( options {greedy=true;} :
				lbc:LBRACK^ {#lbc.setType(ARRAY_DECLARATOR);} RBRACK!
			)+
		)?
	;

// inherited from grammar JavaRecognizer
newExpression :"new"^ (typeArguments)? type
		(	LPAREN! argList RPAREN! (classBlock)?

			//java 1.1
			// Note: This will allow bad constructs like
			//	new int[4][][3] {exp,exp}.
			//	There needs to be a semantic check here...
			// to make sure:
			//   a) [ expr ] and [ ] are not mixed
			//   b) [ expr ] and an init are not used together

		|	newArrayDeclarator (arrayInitializer)?
		)
	;

// inherited from grammar JavaRecognizer
argList :(	expressionList
		|	/*nothing*/
			{#argList = #[ELIST,"ELIST"];}
		)
	;

// inherited from grammar JavaRecognizer
newArrayDeclarator :(
			// CONFLICT:
			// newExpression is a primaryExpression which can be
			// followed by an array index reference. This is ok,
			// as the generated code will stay in this loop as
			// long as it sees an LBRACK (proper behavior)
			options {
				warnWhenFollowAmbig = false;
			}
		:
			lb:LBRACK^ {#lb.setType(ARRAY_DECLARATOR);}
				(expression)?
			RBRACK!
		)+
	;

class PdeLexer extends Lexer;

options {
	importVocab=PdePartial;
	exportVocab=Pde;
	testLiterals=false;
	k=4;
	charVocabulary='\u0003'..'\uFFFF';
	codeGenBitsetTestThreshold=20;
}

{
	/** flag for enabling the "assert" keyword */
	private boolean assertEnabled = true;
	/** flag for enabling the "enum" keyword */
	private boolean enumEnabled = true;

	/** Enable the "assert" keyword */
	public void enableAssert(boolean shouldEnable) { assertEnabled = shouldEnable; }
	/** Query the "assert" keyword state */
	public boolean isAssertEnabled() { return assertEnabled; }
	/** Enable the "enum" keyword */
	public void enableEnum(boolean shouldEnable) { enumEnabled = shouldEnable; }
	/** Query the "enum" keyword state */
	public boolean isEnumEnabled() { return enumEnabled; }
}
WS :(   ' '
        |   '\t'
        |   '\f'
            // handle newlines
        |   (   options {generateAmbigWarnings=false;}
            :   "\r\n"  // Evil DOS
            |   '\r'    // Macintosh
            |   '\n'    // Unix (the right way)
            )
            { newline(); }
        )+
    ;

SL_COMMENT :"//"
        (~('\n'|'\r'))* ('\n'|'\r'('\n')?)
        {newline();}
    ;

ML_COMMENT :"/*"
        (   /*  '\r' '\n' can be matched in one alternative or by matching
                '\r' in one iteration and '\n' in another.  I am trying to
                handle any flavor of newline that comes in, but the language
                that allows both "\r\n" and "\r" and "\n" to all be valid
                newline is ambiguous.  Consequently, the resulting grammar
                must be ambiguous.  I'm shutting this warning off.
             */
            options {
                generateAmbigWarnings=false;
            }
        :
            { LA(2)!='/' }? '*'
        |   '\r' '\n'       {newline();}
        |   '\r'            {newline();}
        |   '\n'            {newline();}
        |   ~('*'|'\n'|'\r')
        )*
        "*/"
    ;

WEBCOLOR_LITERAL :'#'! (HEX_DIGIT)+
    ;

// inherited from grammar JavaLexer
QUESTION :'?'		;

// inherited from grammar JavaLexer
LPAREN :'('		;

// inherited from grammar JavaLexer
RPAREN :')'		;

// inherited from grammar JavaLexer
LBRACK :'['		;

// inherited from grammar JavaLexer
RBRACK :']'		;

// inherited from grammar JavaLexer
LCURLY :'{'		;

// inherited from grammar JavaLexer
RCURLY :'}'		;

// inherited from grammar JavaLexer
COLON :':'		;

// inherited from grammar JavaLexer
COMMA :','		;

// inherited from grammar JavaLexer
ASSIGN :'='		;

// inherited from grammar JavaLexer
EQUAL :"=="	;

// inherited from grammar JavaLexer
LNOT :'!'		;

// inherited from grammar JavaLexer
BNOT :'~'		;

// inherited from grammar JavaLexer
NOT_EQUAL :"!="	;

// inherited from grammar JavaLexer
DIV :'/'		;

// inherited from grammar JavaLexer
DIV_ASSIGN :"/="	;

// inherited from grammar JavaLexer
PLUS :'+'		;

// inherited from grammar JavaLexer
PLUS_ASSIGN :"+="	;

// inherited from grammar JavaLexer
INC :"++"	;

// inherited from grammar JavaLexer
MINUS :'-'		;

// inherited from grammar JavaLexer
MINUS_ASSIGN :"-="	;

// inherited from grammar JavaLexer
DEC :"--"	;

// inherited from grammar JavaLexer
STAR :'*'		;

// inherited from grammar JavaLexer
STAR_ASSIGN :"*="	;

// inherited from grammar JavaLexer
MOD :'%'		;

// inherited from grammar JavaLexer
MOD_ASSIGN :"%="	;

// inherited from grammar JavaLexer
SR :">>"	;

// inherited from grammar JavaLexer
SR_ASSIGN :">>="	;

// inherited from grammar JavaLexer
BSR :">>>"	;

// inherited from grammar JavaLexer
BSR_ASSIGN :">>>="	;

// inherited from grammar JavaLexer
GE :">="	;

// inherited from grammar JavaLexer
GT :">"		;

// inherited from grammar JavaLexer
SL :"<<"	;

// inherited from grammar JavaLexer
SL_ASSIGN :"<<="	;

// inherited from grammar JavaLexer
LE :"<="	;

// inherited from grammar JavaLexer
LT :'<'		;

// inherited from grammar JavaLexer
BXOR :'^'		;

// inherited from grammar JavaLexer
BXOR_ASSIGN :"^="	;

// inherited from grammar JavaLexer
BOR :'|'		;

// inherited from grammar JavaLexer
BOR_ASSIGN :"|="	;

// inherited from grammar JavaLexer
LOR :"||"	;

// inherited from grammar JavaLexer
BAND :'&'		;

// inherited from grammar JavaLexer
BAND_ASSIGN :"&="	;

// inherited from grammar JavaLexer
LAND :"&&"	;

// inherited from grammar JavaLexer
SEMI :';'		;

// inherited from grammar JavaLexer
CHAR_LITERAL :'\'' ( ESC | ~('\''|'\n'|'\r'|'\\') ) '\''
	;

// inherited from grammar JavaLexer
STRING_LITERAL :'"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
	;

// inherited from grammar JavaLexer
protected ESC :'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	'0'..'7'
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	'0'..'7'
			)?
		)
	;

// inherited from grammar JavaLexer
protected HEX_DIGIT :('0'..'9'|'A'..'F'|'a'..'f')
	;

// inherited from grammar JavaLexer
protected VOCAB :'\3'..'\377'
	;

// inherited from grammar JavaLexer
IDENT 
options {
	testLiterals=true;
}
:('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|'$')*
		{
			// check if "assert" keyword is enabled
			if (assertEnabled && "assert".equals($getText)) {
				$setType(LITERAL_assert); // set token type for the rule in the parser
			}
			// check if "enum" keyword is enabled
			if (enumEnabled && "enum".equals($getText)) {
				$setType(LITERAL_enum); // set token type for the rule in the parser
			}
		}
	;

// inherited from grammar JavaLexer
NUM_INT {boolean isDecimal=false; Token t=null;}
:'.' {_ttype = DOT;}
			(
				(('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
				{
				if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
					_ttype = NUM_FLOAT;
				}
				else {
					_ttype = NUM_DOUBLE; // assume double
				}
				})
				|
				// JDK 1.5 token for variable length arguments
				(".." {_ttype = TRIPLE_DOT;})
			)?

	|	(	'0' {isDecimal = true;} // special case for just '0'
			(	('x'|'X')
				(											// hex
					// the 'e'|'E' and float suffix stuff look
					// like hex digits, hence the (...)+ doesn't
					// know when to stop: ambig. ANTLR resolves
					// it correctly by matching immediately. It
					// is therefor ok to hush warning.
					options {
						warnWhenFollowAmbig=false;
					}
				:	HEX_DIGIT
				)+

			|	//float or double with leading zero
				(('0'..'9')+ ('.'|EXPONENT|FLOAT_SUFFIX)) => ('0'..'9')+

			|	('0'..'7')+									// octal
			)?
		|	('1'..'9') ('0'..'9')*  {isDecimal=true;}		// non-zero decimal
		)
		(	('l'|'L') { _ttype = NUM_LONG; }

		// only check to see if it's a float if looks like decimal so far
		|	{isDecimal}?
			(	'.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
			|	EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
			|	f4:FLOAT_SUFFIX {t=f4;}
			)
			{
			if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0) {
				_ttype = NUM_FLOAT;
			}
			else {
				_ttype = NUM_DOUBLE; // assume double
			}
			}
		)?
	;

// inherited from grammar JavaLexer
AT :'@'
	;

// inherited from grammar JavaLexer
protected EXPONENT :('e'|'E') ('+'|'-')? ('0'..'9')+
	;

// inherited from grammar JavaLexer
protected FLOAT_SUFFIX :'f'|'F'|'d'|'D'
	;


