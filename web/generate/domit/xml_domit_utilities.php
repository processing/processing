<?php
/**
* DOMIT Utilities are a set of utilities for the DOMIT! parser
* @package domit-xmlparser
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

/**
*@global Array Translation table for predefined XML entities
*/
$GLOBALS['DOMIT_PREDEFINED_ENTITIES'] = array('&' => '&amp;', '<' => '&lt;', '>' => '&gt;',
											'"' => '&quot;', "'" => '&apos;');
/**
* A set of utilities for the DOMIT! parser
* 
* These methods are intended to be called statically
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Utilities {
	/**
	* Raises an error if an attempt to instantiate the class is made
	*/
	function DOMIT_Utilities() {		
	    die("DOMIT_Utilities Error: this is a static class that should never be instantiated.\n" . 
		    "Please use the following syntax to access methods of this class:\n" .
		    'DOMIT_Utilities::methodName(parameters)');
	} //DOMIT_Utilities	

	/**
	* Generates a normalized (formatted for readability) representation of the node and its children
	* @param Object The root node of the narmalization
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The formatted string representation 
	*/
	function toNormalizedString (&$node, $subEntities=false, $definedEntities) {
		$node_level = 0;
		$response = '';
		
		//$node is a DOMIT_Document
		if ($node->nodeType == DOMIT_DOCUMENT_NODE) { 
			$total = $node->childCount;

			for ($i = 0; $i < $total; $i++) {
				$response .= DOMIT_Utilities::getNormalizedString($node->childNodes[$i], 
											$node_level, $subEntities, $definedEntities);
			}
			
			return $response;
		}
		else {
			return ($response . DOMIT_Utilities::getNormalizedString($node, 
								$node_level, $subEntities, $definedEntities));
		}
	} //toNormalizedString		
	
	/**
	* Converts illegal XML characters to their entity representations
	* @param string The text to be formatted
	* @param array User defined translation table for entities
	* @return string The formatted text 
	*/
	function convertEntities($text, $definedEntities) {
		global $DOMIT_PREDEFINED_ENTITIES;
		$result = strtr($text, $DOMIT_PREDEFINED_ENTITIES);
		$result = strtr($result, $definedEntities);
		return $result;
	} //convertEntities($text)
	
	/**
	* Gets a normalized (formatted for readability) representation of the current node
	* @param Object The node to be normalized
	* @param int The level in the DOM hierarchy where the node is located
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @param array User defined translation table for entities
	* @return string The normalized string representation 
	*/
	function getNormalizedString(&$node, $node_level, $subEntities=false, $definedEntities) {
		$response = '';

		switch ($node->nodeType)  {
			case DOMIT_ELEMENT_NODE: 
				$response .= DOMIT_Utilities::getNormalizedElementString($node, $response, 
									 		$node_level, $subEntities, $definedEntities);								
				break;
			
			case DOMIT_TEXT_NODE: 
				if ($node->nextSibling == null) {
					$node_level--;
				}
					
				$response .= ($subEntities ? 
								DOMIT_Utilities::convertEntities($node->nodeValue, $definedEntities) : 
								$node->nodeValue);
				break;
			
			case DOMIT_CDATA_SECTION_NODE:
				if ($node->nextSibling == null) {
					$node_level--;
				}
				
				$response .= '<![CDATA[' . $node->nodeValue . ']]>';
				break;
				
			case DOMIT_ATTRIBUTE_NODE:
				$response .= $node->toString(false, $subEntities);
				break;
				
			case DOMIT_DOCUMENT_FRAGMENT_NODE: 
				$total = $node->childCount;
				
				for ($i = 0; $i < $total; $i++) {
					$response .= DOMIT_Utilities::getNormalizedString($node->childNodes[$i], $node_level,
															$subEntities, $definedEntities);
				}
				
				break;
				
			case DOMIT_COMMENT_NODE: 
				$response .= '<!--' . $node->nodeValue . '-->';

				if ($node->nextSibling == null) {
					$node_level--;
				}
								
				$response .= DOMIT_Utilities::getIndentation($node_level) ;
				
				break;
				
			case DOMIT_PROCESSING_INSTRUCTION_NODE: 
				$response .= '<' . '?' . $node->nodeName . ' ' . $node->nodeValue . '?' . '>';

				if ($node->nextSibling == null) {
					$node_level--;
				}
								
				$response .= DOMIT_Utilities::getIndentation($node_level) ;
				
				break;
				
			case DOMIT_DOCUMENT_TYPE_NODE:
				$response .= $node->toString() . "\n";
				break;
		} 

		return $response;
	} //getNormalizedString
	
	/**
	* Gets a normalized (formatted for readability) representation of the current element
	* @param Object The node to be normalized
	* @param string The current normalized text
	* @param int The level in the DOM hierarchy where the node is located
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @param array User defined translation table for entities
	* @return string The normalized string representation 
	*/
	function getNormalizedElementString(&$node, $response, $node_level,  
											$subEntities, $definedEntities) {
		$response .= '<' . $node->nodeName;
				
		//get attributes text
		if (is_object($node->attributes)) { //DOMIT
			$response .= $node->attributes->toString(false, $subEntities);
		}
		else { //DOMIT_Lite
			foreach ($node->attributes as $key => $value) {
				$response .= ' ' . $key . '="';
				$response .= ($subEntities ? DOMIT_Utilities::convertEntities($value, 
														$definedEntities) : $value); 
				$response .= '"';
			}
		}
		
		$node_level++;
		
		//if node is childless
		if ($node->childCount == 0) {
            if ($node->ownerDocument->doExpandEmptyElementTags) { 
				if (in_array($node->nodeName, $node->ownerDocument->expandEmptyElementExceptions)) {
					$response .= ' />';
				}
				else {
                	$response .= '></' . $node->nodeName . '>';
				}
		    }
		    else {
				if (in_array($node->nodeName, $node->ownerDocument->expandEmptyElementExceptions)) {
					$response .= '></' . $node->nodeName . '>';
				}
				else {
					$response .= ' />';
				}
		    }
		}
		else {
			$response .= '>';						
			
			//get children
			$myNodes =& $node->childNodes;
			$total = $node->childCount;
			
			//no indentation if first child is a text node 
			if (!DOMIT_Utilities::isTextNode($node->firstChild)) {
				$response .= DOMIT_Utilities::getIndentation($node_level); 
			 } 

			for ($i = 0; $i < $total; $i++) {
				$child =& $myNodes[$i];
				$response .= DOMIT_Utilities::getNormalizedString($child, $node_level,
												$subEntities, $definedEntities);
			}
	
			$response .= '</' . $node->nodeName . '>';
		}

		$node_level--;

		if ($node->nextSibling == null) {
			$node_level--;
			$response .= DOMIT_Utilities::getIndentation($node_level);
		}
		else {				
			//no indentation if next sibling is a text node 
			if (!DOMIT_Utilities::isTextNode($node->nextSibling)) {
				$response .= DOMIT_Utilities::getIndentation($node_level); 
			}
		} 
		
		return $response;
	} //getNormalizedElementString
	
	/**
	* Determines whether the specified node is a Text node
	* @param Object The node to be tested
	* @return boolean True if the node is a Text node 
	*/
	function isTextNode(&$node) {
		$type = $node->nodeType;
		return (($type == DOMIT_TEXT_NODE) || ($type == DOMIT_CDATA_SECTION_NODE));
	} //isTextNode
	
	/**
	* Returns the indentation required for the specified node level
	* @param int The current node level
	* @return string The indentation required for the specified node level
	*/
	function getIndentation($node_level) {
		$INDENT_LEN = '    ';
		$indentation = "\n";

		for ($i = 0; $i < $node_level; $i++) {
			$indentation .= $INDENT_LEN;
		}
		
		return $indentation;
	} //getIndentation
	
	/**
	* Removes the extension from the specified file name
	* @param string The file name
	* @return string The file name, stripped of its extension
	*/
	function removeExtension($fileName) {
		$total = strlen($fileName);
		$index = -1;
		
		for ($i = ($total - 1); $i >= 0; $i--) {
			if ($fileName{$i} == '.') {
				$index = $i;
			}
		}
		
		if ($index == -1) {
			return $fileName;
		}
		
		return (substr($fileName, 0, $index));
	} //removeExtension	
	
	/**
	* Determines whether the XML string is valid (NOT FULLY IMPLEMENTED!)
	* @param string The XML text
	* @return boolean True if the XML text is valid
	*/
	function validateXML($xmlText) {
		//this does only rudimentary validation
		//at this point in time
		$isValid = true;
		
		if (is_string($xmlText)) {		
			$text = trim($xmlText);
			
			switch ($text) {
				case '':
					$isValid = false;
					break;
			}
		}
		else {
			$isValid = false;
		}
		
		return $isValid;
	} //validateXML
	
	/**
	* Set the browser header to interpret data as UTF-8 formatted
	* @param string The content type of the data
	*/
	function printUTF8Header($contentType = 'text/html') {
		echo header('Content-type: ' . $contentType . '; charset=utf-8');
	} //printUTF8Header
	
	/**
	* Formats a string for presentation as HTML
	* @param string The string to be formatted
	* @param boolean True if the string is to be sent directly to output
	* @return string The HTML formatted string  
	*/
	function forHTML($text, $doPrint = false) {
		if ($doPrint) {
			print ('<pre>' . htmlspecialchars($text) . '</pre>');
		}
		else {
			return ('<pre>' . htmlspecialchars($text) . '</pre>');
		}		
	} //forHTML
	
	/**
	* Generates a node tree from an array and appends it to the specified document or node
	* @param object The document or node to which the child nodes should be appended
	* @param array An associative multidimensional array of elements and values
	*/
	function fromArray (&$node, &$myArray) {
		if ($node->nodeType == DOMIT_DOCUMENT_NODE) {
			$docNode =& $node;
		}
		else {
			$docNode =& $node->ownerDocument;
		}

		foreach ($myArray as $key => $value) {			
			if (is_array($value)) {
				//check for numeric indices
				$total = count($value);
				
				if (($total > 0)  && isset($value[0])){
					for ($i = 0; $i < $total; $i++) {
						$node->appendChild($docNode->createElement($key));
						DOMIT_Utilities::fromArray($node->lastChild, $value[$i]);					
					}
				}
				else {
					//generate child elements
					$node->appendChild($docNode->createElement($key));
					DOMIT_Utilities::fromArray($node->lastChild, $value);
				}
			}
			else {
				$node->appendChild($docNode->createElement($key));
				$node->lastChild->appendChild($docNode->createTextNode($value));
			}			
		}		
	} //fromArray
} //DOMIT_Utilities
?>
