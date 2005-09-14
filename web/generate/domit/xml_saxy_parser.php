<?php
/**
* SAXY is a non-validating, but lightweight and fast SAX parser for PHP, modelled on the Expat parser
* @package saxy-xmlparser
* @subpackage saxy-xmlparser-main
* @version 0.87
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/saxy/ SAXY Home Page
* SAXY is Free Software
**/

if (!defined('SAXY_INCLUDE_PATH')) {
	define('SAXY_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

/** current version of SAXY */
define ('SAXY_VERSION', '0.87');

/** default XML namespace */
define ('SAXY_XML_NAMESPACE', 'http://www.w3.org/xml/1998/namespace');

/** saxy parse state, before prolog is encountered */
define('SAXY_STATE_PROLOG_NONE', 0);
/** saxy parse state, in processing instruction */
define('SAXY_STATE_PROLOG_PROCESSINGINSTRUCTION', 1);
/** saxy parse state, an exclamation mark has been encountered */
define('SAXY_STATE_PROLOG_EXCLAMATION', 2);
/** saxy parse state, in DTD */
define('SAXY_STATE_PROLOG_DTD', 3);
/** saxy parse state, an inline DTD */
define('SAXY_STATE_PROLOG_INLINEDTD', 4);
/** saxy parse state, a comment */
define('SAXY_STATE_PROLOG_COMMENT', 5);
/** saxy parse state, processing main document */
define('SAXY_STATE_PARSING', 6);
/** saxy parse state, processing comment in main document */
define('SAXY_STATE_PARSING_COMMENT', 7);

//SAXY error codes; same as EXPAT error codes
/** no error */
define('SAXY_XML_ERROR_NONE', 0);
/** out of memory error */
define('SAXY_XML_ERROR_NO_MEMORY', 1);
/** syntax error */
define('SAXY_XML_ERROR_SYNTAX', 2);
/** no elements in document */
define('SAXY_XML_ERROR_NO_ELEMENTS', 3);
/** invalid token encountered error */
define('SAXY_XML_ERROR_INVALID_TOKEN', 4);
/** unclosed token error */
define('SAXY_XML_ERROR_UNCLOSED_TOKEN', 5);
/** partial character error */
define('SAXY_XML_ERROR_PARTIAL_CHAR', 6);
/** mismatched tag error */
define('SAXY_XML_ERROR_TAG_MISMATCH', 7);
/** duplicate attribute error */
define('SAXY_XML_ERROR_DUPLICATE_ATTRIBUTE', 8);
/** junk after document element error */
define('SAXY_XML_ERROR_JUNK_AFTER_DOC_ELEMENT', 9);
/** parameter enitity reference error */
define('SAXY_XML_ERROR_PARAM_ENTITY_REF', 10);
/** undefined entity error */
define('SAXY_XML_ERROR_UNDEFINED_ENTITY', 11);
/** recursive entity error */
define('SAXY_XML_ERROR_RECURSIVE_ENTITY_REF', 12);
/** asynchronous entity error */
define('SAXY_XML_ERROR_ASYNC_ENTITY', 13);
/** bad character reference error */
define('SAXY_XML_ERROR_BAD_CHAR_REF', 14);
/** binary entity reference error */
define('SAXY_XML_ERROR_BINARY_ENTITY_REF', 15);
/** attribute external entity error */
define('SAXY_XML_ERROR_ATTRIBUTE_EXTERNAL_ENTITY_REF', 16);
/** misplaced processing instruction error */
define('SAXY_XML_ERROR_MISPLACED_XML_PI', 17);
/** unknown encoding error */
define('SAXY_XML_ERROR_UNKNOWN_ENCODING', 18);
/** incorrect encoding error */
define('SAXY_XML_ERROR_INCORRECT_ENCODING', 19);
/** unclosed CDATA Section error */
define('SAXY_XML_ERROR_UNCLOSED_CDATA_SECTION', 20);
/** external entity handling error */
define('SAXY_XML_ERROR_EXTERNAL_ENTITY_HANDLING', 21);

require_once(SAXY_INCLUDE_PATH . 'xml_saxy_shared.php');

/**
* The SAX Parser class
*
* @package saxy-xmlparser
* @subpackage saxy-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class SAXY_Parser extends SAXY_Parser_Base {
    /** @var int The current error number */
	var $errorCode = SAXY_XML_ERROR_NONE;
	/** @var Object A reference to the DocType event handler */
	var $DTDHandler = null;
	/** @var Object A reference to the Comment event handler */
	var $commentHandler = null;
	/** @var Object A reference to the Processing Instruction event handler */
	var $processingInstructionHandler = null;
	/** @var Object A reference to the Start Namespace Declaration event handler */
	var $startNamespaceDeclarationHandler = null;
	/** @var Object A reference to the End Namespace Declaration event handler */
	var $endNamespaceDeclarationHandler = null;
	/** @var boolean True if SAXY takes namespaces into consideration when parsing element tags */
	var $isNamespaceAware = false;
	/** @var array An indexed array containing associative arrays of namespace prefixes mapped to their namespace URIs */
	var $namespaceMap = array();
	/** @var array A stack used to determine when an end namespace event should be fired */
	var $namespaceStack = array();
	/** @var array A track used to track the uri of the current default namespace */
	var $defaultNamespaceStack = array();
	/** @var array A stack containing tag names of unclosed elements */
	var $elementNameStack = array();
	

	/**
	* Constructor for SAX parser
	*/
	function SAXY_Parser() {
		$this->SAXY_Parser_Base();
		$this->state = SAXY_STATE_PROLOG_NONE;
	} //SAXY_Parser
	
	/**
	* Sets a reference to the handler for the DocType event 
	* @param mixed A reference to the DocType handler 
	*/
	function xml_set_doctype_handler($handler) {
		$this->DTDHandler =& $handler;
	} //xml_set_doctype_handler
	
	/**
	* Sets a reference to the handler for the Comment event 
	* @param mixed A reference to the Comment handler 
	*/
	function xml_set_comment_handler($handler) {
		$this->commentHandler =& $handler;
	} //xml_set_comment_handler
	
	/**
	* Sets a reference to the handler for the Processing Instruction event 
	* @param mixed A reference to the Processing Instruction handler 
	*/
	function xml_set_processing_instruction_handler($handler) {
		$this->processingInstructionHandler =& $handler;
	} //xml_set_processing_instruction_handler
	
	/**
	* Sets a reference to the handler for the Start Namespace Declaration event
	* @param mixed A reference to the Start Namespace Declaration handler
	*/
	function xml_set_start_namespace_decl_handler($handler) {
		$this->startNamespaceDeclarationHandler =& $handler;
	} //xml_set_start_namespace_decl_handler
	
	/**
	* Sets a reference to the handler for the End Namespace Declaration event
	* @param mixed A reference to the Start Namespace Declaration handler
	*/
	function xml_set_end_namespace_decl_handler($handler) {
		$this->endNamespaceDeclarationHandler =& $handler;
	} //xml_set_end_namespace_decl_handler
	
	/**
	* Specifies whether SAXY is namespace sensitive
	* @param boolean True if SAXY is namespace aware
	*/
	function setNamespaceAwareness($isNamespaceAware) {
		$this->isNamespaceAware =& $isNamespaceAware;
	} //setNamespaceAwareness
	
	/**
	* Returns the current version of SAXY
	* @return Object The current version of SAXY
	*/
	function getVersion() {
		return SAXY_VERSION;
	} //getVersion		
	
	/**
	* Processes the xml prolog, doctype, and any other nodes that exist outside of the main xml document
	* @param string The xml text to be processed
	* @return string The preprocessed xml text
	*/	
	function preprocessXML($xmlText) {
		//strip prolog
		$xmlText = trim($xmlText);
		$startChar = -1;
		$total = strlen($xmlText);
		
		for ($i = 0; $i < $total; $i++) {
			$currentChar = $xmlText{$i};

			switch ($this->state) {
				case SAXY_STATE_PROLOG_NONE:	
					if ($currentChar == '<') {
						$nextChar = $xmlText{($i + 1)};
						
						if ($nextChar == '?')  {
							$this->state = SAXY_STATE_PROLOG_PROCESSINGINSTRUCTION;
							$this->charContainer = '';
						}
						else if ($nextChar == '!') {								
							$this->state = SAXY_STATE_PROLOG_EXCLAMATION;								
							$this->charContainer .= $currentChar;
							break;
						}
						else {
							$this->charContainer = '';
							$startChar  = $i;
							$this->state = SAXY_STATE_PARSING;
							return (substr($xmlText, $startChar));
						}
					}
					
					break;
					
				case SAXY_STATE_PROLOG_EXCLAMATION:
					if ($currentChar == 'D') {
						$this->state = SAXY_STATE_PROLOG_DTD;	
						$this->charContainer .= $currentChar;							
					}
					else if ($currentChar == '-') {
						$this->state = SAXY_STATE_PROLOG_COMMENT;	
						$this->charContainer = '';
					}
					else {
						//will trap ! and add it
						$this->charContainer .= $currentChar;
					}						
					
					break;
					
				case SAXY_STATE_PROLOG_PROCESSINGINSTRUCTION:
					if ($currentChar == '>') {
						$this->state = SAXY_STATE_PROLOG_NONE;							
						$this->parseProcessingInstruction($this->charContainer);							
						$this->charContainer = '';
					}
					else {
						$this->charContainer .= $currentChar;
					}
					
					break;
					
				case SAXY_STATE_PROLOG_COMMENT:
					if ($currentChar == '>') {
						$this->state = SAXY_STATE_PROLOG_NONE;							
						$this->parseComment($this->charContainer);							
						$this->charContainer = '';
					}
					else if ($currentChar == '-') {
						if ((($xmlText{($i + 1)} == '-')  && ($xmlText{($i + 2)} == '>')) || 
							($xmlText{($i + 1)} == '>') ||
							(($xmlText{($i - 1)} == '-')  && ($xmlText{($i - 2)}== '!')) ){
							//do nothing
						}
						else {
							$this->charContainer .= $currentChar;
						}
					}
					else {
						$this->charContainer .= $currentChar;
					}
					
					break;
				
				case SAXY_STATE_PROLOG_DTD:
					if ($currentChar == '[') {
						$this->charContainer .= $currentChar;
						$this->state = SAXY_STATE_PROLOG_INLINEDTD;
					}					
					else if ($currentChar == '>') {
						$this->state = SAXY_STATE_PROLOG_NONE;
						
						if ($this->DTDHandler != null) {
							$this->fireDTDEvent($this->charContainer . $currentChar);
						}
						
						$this->charContainer = '';
					}
					else {
						$this->charContainer .= $currentChar;
					}	
					
					break;
					
				case SAXY_STATE_PROLOG_INLINEDTD:
					$previousChar = $xmlText{($i - 1)};

					if (($currentChar == '>') && ($previousChar == ']')){
						$this->state = SAXY_STATE_PROLOG_NONE;
						
						if ($this->DTDHandler != null) {
							$this->fireDTDEvent($this->charContainer . $currentChar);
						}
						
						$this->charContainer = '';
					}
					else {
						$this->charContainer .= $currentChar;
					}	
					
					break;
				
			}
		}
	} //preprocessXML

	/**
	* The controlling method for the parsing process 
	* @param string The xml text to be processed
	* @return boolean True if parsing is successful
	*/
	function parse ($xmlText) {
		$xmlText = $this->preprocessXML($xmlText);			
		$total = strlen($xmlText);

		for ($i = 0; $i < $total; $i++) {
			$currentChar = $xmlText{$i};

			switch ($this->state) {
				case SAXY_STATE_PARSING:
					switch ($currentChar) {
						case '<':
							if (substr($this->charContainer, 0, SAXY_CDATA_LEN) == SAXY_SEARCH_CDATA) {
								$this->charContainer .= $currentChar;
							}
							else {
								$this->parseBetweenTags($this->charContainer);
								$this->charContainer = '';
							}						
							break;
							
						case '-':
							if (($xmlText{($i - 1)} == '-') && ($xmlText{($i - 2)} == '!')) {
								$this->state = SAXY_STATE_PARSING_COMMENT;
								$this->charContainer = '';
							}
							else {
								$this->charContainer .= $currentChar;
							}
							break;

						case '>':
							if ((substr($this->charContainer, 0, SAXY_CDATA_LEN) == SAXY_SEARCH_CDATA) &&
								!(($this->getCharFromEnd($this->charContainer, 0) == ']') &&
								($this->getCharFromEnd($this->charContainer, 1) == ']'))) {
								$this->charContainer .= $currentChar;
							}
							else {
								$this->parseTag($this->charContainer);
								$this->charContainer = '';
							}
							break;
							
						default:
							$this->charContainer .= $currentChar;
					}
					
					break;
					
				case SAXY_STATE_PARSING_COMMENT:
					switch ($currentChar) {
						case '>':
							if (($xmlText{($i - 1)} == '-') && ($xmlText{($i - 2)} == '-')) {
								$this->fireCommentEvent(substr($this->charContainer, 0, 
													(strlen($this->charContainer) - 2)));
								$this->charContainer = '';
								$this->state = SAXY_STATE_PARSING;
							}
							else {
								$this->charContainer .= $currentChar;
							}
							break;
						
						default:
							$this->charContainer .= $currentChar;
					}
					
					break;
			}
		}	

		return ($this->errorCode == 0);
	} //parse

	/**
	* Parses an element tag
	* @param string The interior text of the element tag
	*/
	function parseTag($tagText) {
		$tagText = trim($tagText);
		$firstChar = $tagText{0};
		$myAttributes = array();

		switch ($firstChar) {
			case '/':
				$tagName = substr($tagText, 1);				
				$this->_fireEndElementEvent($tagName);
				break;
			
			case '!':
				$upperCaseTagText = strtoupper($tagText);
			
				if (strpos($upperCaseTagText, SAXY_SEARCH_CDATA) !== false) { //CDATA Section
					$total = strlen($tagText);
					$openBraceCount = 0;
					$textNodeText = '';
					
					for ($i = 0; $i < $total; $i++) {
						$currentChar = $tagText{$i};
						
						if (($currentChar == ']') && ($tagText{($i + 1)} == ']')) {
							break;
						}
						else if ($openBraceCount > 1) {
							$textNodeText .= $currentChar;
						}
						else if ($currentChar == '[') { //this won't be reached after the first open brace is found
							$openBraceCount ++;
						}
					}
					
					if ($this->cDataSectionHandler == null) {
						$this->fireCharacterDataEvent($textNodeText);
					}
					else {
						$this->fireCDataSectionEvent($textNodeText);
					}
				}
				else if (strpos($upperCaseTagText, SAXY_SEARCH_NOTATION) !== false) { //NOTATION node, discard
					return;
				}
				/*
				else if (substr($tagText, 0, 2) == '!-') { //comment node
					if ($this->commentHandler != null) {
						$this->fireCommentEvent(substr($tagText, 3, (strlen($tagText) - 5)));
					}
				}
				*/
				break;
				
			case '?': 
				//Processing Instruction node
				$this->parseProcessingInstruction($tagText);
				break;
				
			default:				
				if ((strpos($tagText, '"') !== false) || (strpos($tagText, "'") !== false)) {
					$total = strlen($tagText);
					$tagName = '';

					for ($i = 0; $i < $total; $i++) {
						$currentChar = $tagText{$i};
						
						if (($currentChar == ' ') || ($currentChar == "\t") ||
							($currentChar == "\n") || ($currentChar == "\r") ||
							($currentChar == "\x0B")) {
							$myAttributes = $this->parseAttributes(substr($tagText, $i));
							break;
						}
						else {
							$tagName .= $currentChar;
						}
					}

					if (strrpos($tagText, '/') == (strlen($tagText) - 1)) { //check $tagText, but send $tagName
						$this->_fireStartElementEvent($tagName, $myAttributes);
						$this->_fireEndElementEvent($tagName);
					}
					else {
						$this->_fireStartElementEvent($tagName, $myAttributes);
					}
				}
				else {
					if (strpos($tagText, '/') !== false) {
						$tagText = trim(substr($tagText, 0, (strrchr($tagText, '/') - 1)));
						$this->_fireStartElementEvent($tagText, $myAttributes);
						$this->_fireEndElementEvent($tagText);
					}
					else {
						$this->_fireStartElementEvent($tagText, $myAttributes);
					}
				}					
		}
	} //parseTag

 	/**
	* Fires a start element event and pushes the element name onto the elementName stack
	* @param string The start element tag name
	* @param Array The start element attributes
	*/
	function _fireStartElementEvent($tagName, &$myAttributes) {
	    $this->elementNameStack[] = $tagName;
	    
	    if ($this->isNamespaceAware) {
			$this->detectStartNamespaceDeclaration($myAttributes);
			$tagName = $this->expandNamespacePrefix($tagName);
			
			$this->expandAttributePrefixes($myAttributes);
	    }
	    
	    $this->fireStartElementEvent($tagName, $myAttributes);
	} //_fireStartElementEvent
	
	/**
	* Expands attribute prefixes to full namespace uri
	* @param Array The start element attributes
	*/
	function expandAttributePrefixes(&$myAttributes) {
	    $arTransform = array();
	    
	    foreach ($myAttributes as $key => $value) {
	        if (strpos($key, 'xmlns') === false) {
	            if (strpos($key, ':') !== false) {
	                $expandedTag = $this->expandNamespacePrefix($key);
	                $arTransform[$key] = $expandedTag;
	            }
	        }
	    }
	    
	    foreach ($arTransform as $key => $value) {
	        $myAttributes[$value] = $myAttributes[$key];
	        unset($myAttributes[$key]);
	    }
	} //expandAttributePrefixes
	
	/**
	* Expands the namespace prefix (if one exists) to the full namespace uri
	* @param string The tagName with the namespace prefix
	* @return string The tagName, with the prefix expanded to the namespace uri
	*/
	function expandNamespacePrefix($tagName) {
	    $stackLen = count($this->defaultNamespaceStack);
	    $defaultNamespace = $this->defaultNamespaceStack[($stackLen - 1)];

	    $colonIndex = strpos($tagName, ':');

	    if ($colonIndex !== false) {
			$prefix = substr($tagName, 0, $colonIndex);
			
			if ($prefix != 'xml') {
	        	$tagName = $this->getNamespaceURI($prefix) . substr($tagName, $colonIndex);
			}
			else {
				$tagName = SAXY_XML_NAMESPACE . substr($tagName, $colonIndex);
			}
	    }
	    else if ($defaultNamespace != '') {
	        $tagName = $defaultNamespace . ':' . $tagName;
	    }

	    return $tagName;
	} //expandNamespacePrefix
	
	/**
	* Searches the namespaceMap for the specified prefix, and returns the full namespace URI
	* @param string The namespace prefix
	* @return string The namespace uri
	*/
	function getNamespaceURI($prefix) {
	    $total = count($this->namespaceMap);
	    $uri = $prefix; //in case uri can't be found, just send back prefix
	                    //should really generate an error, but worry about this later
		//reset($this->namespaceMap);

	    for ($i = ($total - 1); $i >= 0; $i--) {
	        $currMap =& $this->namespaceMap[$i];

	        if (isset($currMap[$prefix])) {
	            $uri = $currMap[$prefix];
	            break;
	        }
	    }

	    return $uri;
	} //getNamespaceURI
	
	/**
	* Searches the attributes array for an xmlns declaration and fires an event if found
	* @param Array The start element attributes
	*/
	function detectStartNamespaceDeclaration($myAttributes) {
	    $namespaceExists = false;
	    $namespaceMapUpper = 0;
	    $userDefinedDefaultNamespace = false;
	    $total = count($myAttributes);
	    
	    foreach ($myAttributes as $key => $value) {
	        if (strpos($key, 'xmlns') !== false) {
	            //add an array to store all namespaces for the current element
	            if (!$namespaceExists) {
					$this->namespaceMap[] = array();
					$namespaceMapUpper = count($this->namespaceMap) - 1;
	            }

				//check for default namespace override, i.e. xmlns='...'
				if (strpos($key, ':') !== false) {
				    $prefix = $namespaceMapKey = substr($key, 6);
				    $this->namespaceMap[$namespaceMapUpper][$namespaceMapKey] = $value;
				}
				else {
				    $prefix = '';
					$userDefinedDefaultNamespace = true;
					
					//if default namespace '', store in map using key ':'
					$this->namespaceMap[$namespaceMapUpper][':'] = $value;
					$this->defaultNamespaceStack[] = $value;
				}
				
	            $this->fireStartNamespaceDeclarationEvent($prefix, $value);
	            $namespaceExists = true;
	        }
	    }
	    
	    //store the default namespace (inherited from the parent elements so grab last one)
		if (!$userDefinedDefaultNamespace) {
		    $stackLen = count($this->defaultNamespaceStack);
		    if ($stackLen == 0) {
		        $this->defaultNamespaceStack[] = '';
		    }
		    else {
				$this->defaultNamespaceStack[] =
					$this->defaultNamespaceStack[($stackLen - 1)];
		    }
		}
		
	    $this->namespaceStack[] = $namespaceExists;
	} //detectStartNamespaceDeclaration
	
	/**
	* Fires an end element event and pops the element name from the elementName stack
	* @param string The end element tag name
	*/
	function _fireEndElementEvent($tagName) {
	    $lastTagName = array_pop($this->elementNameStack);

		//check for mismatched tag error
		if ($lastTagName != $tagName) {
			$this->errorCode = SAXY_XML_ERROR_TAG_MISMATCH;
		}

		if ($this->isNamespaceAware) {
		    $tagName = $this->expandNamespacePrefix($tagName);
		    $this->fireEndElementEvent($tagName);
			$this->detectEndNamespaceDeclaration();
			$defaultNamespace = array_pop($this->defaultNamespaceStack);
		}
		else {
		    $this->fireEndElementEvent($tagName);
		}
	} //_fireEndElementEvent

	/**
	* Determines whether an end namespace declaration event should be fired
	*/
	function detectEndNamespaceDeclaration() {
	    $isNamespaceEnded = array_pop($this->namespaceStack);
	    
	    if ($isNamespaceEnded) {
			$map = array_pop($this->namespaceMap);
			
	        foreach ($map as $key => $value) {
	            if ($key == ':') {
					$key = '';
	            }
				$this->fireEndNamespaceDeclarationEvent($key);
	        }
		}
	} //detectEndNamespaceDeclaration

	/**
	* Parses a processing instruction
	* @param string The interior text of the processing instruction
	*/
	function parseProcessingInstruction($data) {
		$endTarget = 0;
		$total = strlen($data);
		
		for ($x = 2; $x < $total; $x++) {
			if (trim($data{$x}) == '') {
				$endTarget = $x;
				break;
			}
		}
		
		$target = substr($data, 1, ($endTarget - 1));
		$data = substr($data, ($endTarget + 1), ($total - $endTarget - 2));
	
		if ($this->processingInstructionHandler != null) {
			$this->fireProcessingInstructionEvent($target, $data);
		}
	} //parseProcessingInstruction
	
	/**
	* Parses a comment
	* @param string The interior text of the comment
	*/
	function parseComment($data) {
		if ($this->commentHandler != null) {
			$this->fireCommentEvent($data);
		}
	} //parseComment
	
	/**
	* Fires a doctype event
	* @param string The doctype data
	*/
	function fireDTDEvent($data) {
		call_user_func($this->DTDHandler, $this, $data);
	} //fireDTDEvent
	
	/**
	* Fires a comment event
	* @param string The text of the comment
	*/
	function fireCommentEvent($data) {
		call_user_func($this->commentHandler, $this, $data);
	} //fireCommentEvent
	
	/**
	* Fires a processing instruction event
	* @param string The processing instruction data
	*/
	function fireProcessingInstructionEvent($target, $data) {
		call_user_func($this->processingInstructionHandler, $this, $target, $data);
	} //fireProcessingInstructionEvent
	
	/**
	* Fires a start namespace declaration event
	* @param string The namespace prefix
	* @param string The namespace uri
	*/
	function fireStartNamespaceDeclarationEvent($prefix, $uri) {
		call_user_func($this->startNamespaceDeclarationHandler, $this, $prefix, $uri);
	} //fireStartNamespaceDeclarationEvent
	
	/**
	* Fires an end namespace declaration event
	* @param string The namespace prefix
	*/
	function fireEndNamespaceDeclarationEvent($prefix) {
		call_user_func($this->endNamespaceDeclarationHandler, $this, $prefix);
	} //fireEndNamespaceDeclarationEvent
	
	/**
	* Returns the current error code
	* @return int The current error code
	*/
	function xml_get_error_code() {
		return $this->errorCode;
	} //xml_get_error_code
	
	/**
	* Returns a textual description of the error code
	* @param int The error code
	* @return string The error message
	*/
	function xml_error_string($code) {
		switch ($code) {
		    case SAXY_XML_ERROR_NONE:
		        return "No error";
		        break;
			case SAXY_XML_ERROR_NO_MEMORY:
			    return "Out of memory";
		        break;
			case SAXY_XML_ERROR_SYNTAX:
			    return "Syntax error";
		        break;
			case SAXY_XML_ERROR_NO_ELEMENTS:
			    return "No elements in document";
		        break;
			case SAXY_XML_ERROR_INVALID_TOKEN:
			    return "Invalid token";
		        break;
			case SAXY_XML_ERROR_UNCLOSED_TOKEN:
			    return "Unclosed token";
		        break;
			case SAXY_XML_ERROR_PARTIAL_CHAR:
			    return "Partial character";
		        break;
			case SAXY_XML_ERROR_TAG_MISMATCH:
			    return "Tag mismatch";
		        break;
			case SAXY_XML_ERROR_DUPLICATE_ATTRIBUTE:
			    return "Duplicate attribute";
		        break;
			case SAXY_XML_ERROR_JUNK_AFTER_DOC_ELEMENT:
			    return "Junk encountered after document element";
		        break;
			case SAXY_XML_ERROR_PARAM_ENTITY_REF:
			    return "Parameter entity reference error";
		        break;
			case SAXY_XML_ERROR_UNDEFINED_ENTITY:
			    return "Undefined entity";
		        break;
			case SAXY_XML_ERROR_RECURSIVE_ENTITY_REF:
			    return "Recursive entity reference";
		        break;
			case SAXY_XML_ERROR_ASYNC_ENTITY:
			    return "Asynchronous internal entity found in external entity";
		        break;
			case SAXY_XML_ERROR_BAD_CHAR_REF:
			    return "Bad character reference";
		        break;
			case SAXY_XML_ERROR_BINARY_ENTITY_REF:
				return "Binary entity reference";
		        break;
			case SAXY_XML_ERROR_ATTRIBUTE_EXTERNAL_ENTITY_REF:
			    return "Attribute external entity reference";
		        break;
			case SAXY_XML_ERROR_MISPLACED_XML_PI:
			    return "Misplaced processing instruction";
		        break;
			case SAXY_XML_ERROR_UNKNOWN_ENCODING:
			    return "Unknown encoding";
		        break;
			case SAXY_XML_ERROR_INCORRECT_ENCODING:
				return "Incorrect encoding";
		        break;
			case SAXY_XML_ERROR_UNCLOSED_CDATA_SECTION:
			    return "Unclosed CDATA Section";
		        break;
			case SAXY_XML_ERROR_EXTERNAL_ENTITY_HANDLING:
			    return "Problem in external entity handling";
		        break;
			default:
			    return "No definition for error code " . $code;
		        break;
		}
	} //xml_error_string

} //SAXY_Parser
?>
