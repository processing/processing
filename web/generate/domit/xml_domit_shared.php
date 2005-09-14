<?php
/**
* @package domit-xmlparser
* @version 0.98
* @copyright (C) 2004 John Heinstein. All rights reserved
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

if (!defined('DOMIT_INCLUDE_PATH')) {
	/* Path to DOMIT! files */
	define('DOMIT_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

//Nodes
/** DOM Element nodeType */
define('DOMIT_ELEMENT_NODE', 1);
/** DOM Attr nodeType */
define('DOMIT_ATTRIBUTE_NODE', 2);
/** DOM Text nodeType */
define('DOMIT_TEXT_NODE', 3);
/** DOM CDATA Section nodeType */
define('DOMIT_CDATA_SECTION_NODE', 4);
/** DOM Entity Reference nodeType */
define('DOMIT_ENTITY_REFERENCE_NODE', 5);
/** DOM Entity nodeType */
define('DOMIT_ENTITY_NODE', 6);
/** DOM Processing Instruction nodeType */
define('DOMIT_PROCESSING_INSTRUCTION_NODE', 7);
/** DOM Comment nodeType */
define('DOMIT_COMMENT_NODE', 8);
/** DOM Document nodeType */
define('DOMIT_DOCUMENT_NODE', 9);
/** DOM DocType nodeType */
define('DOMIT_DOCUMENT_TYPE_NODE', 10);
/** DOM Document Fragment nodeType */
define('DOMIT_DOCUMENT_FRAGMENT_NODE', 11);
/** DOM Notation nodeType */
define('DOMIT_NOTATION_NODE', 12);

//DOM Level 1 Exceptions
/** DOM error: array index out of bounds  */
define('DOMIT_INDEX_SIZE_ERR', 1);
/** DOM error: text doesn't fit into a DOMString */
define('DOMIT_DOMSTRING_SIZE_ERR', 2); 
/** DOM error: node can't be inserted at this location */
define('DOMIT_HIERARCHY_REQUEST_ERR', 3);
/** DOM error: node not a child of target document */
define('DOMIT_WRONG_DOCUMENT_ERR', 4); 
/** DOM error: invalid character specified */
define('DOMIT_INVALID_CHARACTER_ERR', 5);
/** DOM error: data can't be added to current node */
define('DOMIT_NO_DATA_ALLOWED_ERR', 6);
/** DOM error: node is read-only */
define('DOMIT_NO_MODIFICATION_ALLOWED_ERR', 7);
/** DOM error: node can't be found in specified context */
define('DOMIT_NOT_FOUND_ERR', 8);
/** DOM error: operation not supported by current implementation */
define('DOMIT_NOT_SUPPORTED_ERR', 9);
/** DOM error: attribute currently in use elsewhere */
define('DOMIT_INUSE_ATTRIBUTE_ERR', 10);

//DOM Level 2 Exceptions
/** DOM error: attempt made to use an object that is no longer usable */
define('DOMIT_INVALID_STATE_ERR', 11);
/** DOM error: invalid or illegal string specified */
define('DOMIT_SYNTAX_ERR', 12);
/** DOM error: can't modify underlying type of node */
define('DOMIT_INVALID_MODIFICATION_ERR', 13);
/** DOM error: attempt to change node in a way incompatible with namespaces */
define('DOMIT_NAMESPACE_ERR', 14);
/** DOM error: operation unsupported by underlying object */
define('DOMIT_INVALID_ACCESS_ERR', 15);

//DOMIT! Exceptions
/** DOM error: attempt to instantiate abstract class */
define('DOMIT_ABSTRACT_CLASS_INSTANTIATION_ERR', 100);
/** DOM error: attempt to call abstract method */
define('DOMIT_ABSTRACT_METHOD_INVOCATION_ERR', 101);
/** DOM error: can't perform this action on or with Document Fragment */
define('DOMIT_DOCUMENT_FRAGMENT_ERR', 102);

/**
*@global Object Instance of the UIDGenerator class
*/
$GLOBALS['uidFactory'] = new UIDGenerator();

require_once(DOMIT_INCLUDE_PATH . 'xml_domit_nodemaps.php');

/**
* Generates unique ids for each node
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class UIDGenerator {
	/** @var int A seed value for generating uids */
	var $seed;
	/** @var int A tally of the number of uids generated */
	var $counter = 0;
	
	/**
	* UIDGenerator constructor
	*/
	function UIDGenerator() {
		$this->seed = 'node' . time();
	} //UIDGenerator
	
	/**
	* Generates a unique id
	* @return uid 
	*/
	function generateUID() {
		return ($this->seed . $this->counter++);
	} //generateUID
} //UIDGenerator


/**
* A DOMIT! exception handling class
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_DOMException {
	/**
	* Raises the specified exception
	* @param int The error number
	* @param string A string explanation of the error
	*/
	function raiseException($errorNum, $errorString) {
		$errorMessage = 'Error: ' . $errorNum  .  "\n " . $errorString;
		
		if ((!isset($GLOBALS['DOMIT_ERROR_FORMATTING_HTML'])) ||
			($GLOBALS['DOMIT_ERROR_FORMATTING_HTML'] == true)) {
		        $errorMessage = "<p><pre>" . $errorMessage . "</pre></p>";
		}

		die($errorMessage);
	} //raiseException
} //DOMIT_DOMException

/**
* A class representing the DOM Implementation node
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_DOMImplementation {
	function hasFeature($feature, $version = null) {
		if (strtoupper($feature) == 'XML') {
			if (($version == '1.0') || ($version == '2.0') || ($version == null)) {
				return true;
			}
		}
		
		return false;
	} //hasFeature
	
	/**
	* Creates a new DOMIT_Document node and appends a documentElement with the specified info
	* @param string The namespaceURI of the documentElement
	* @param string The $qualifiedName of the documentElement
	* @param Object A document type node
	* @return Object The new document fragment node
	*/
	function &createDocument($namespaceURI, $qualifiedName, &$docType) {
		$xmldoc =& new DOMIT_Document();
		$documentElement =& $xmldoc->createElementNS($namespaceURI, $qualifiedName);
		
		$xmldoc->setDocumentElement($documentElement);
		
		if ($docType != null) {
			$xmldoc->doctype =& $docType;
		}
		
		return $xmldoc;
	} //createDocument
	
	/**
	* Creates a new DOMIT_DocumentType node (not yet implemented!)
	* @param string The $qualifiedName
	* @param string The $publicID
	* @param string The $systemID
	* @return Object The new document type node
	*/
	function &createDocumentType($qualifiedName, $publicID, $systemID) {
		//not yet implemented
		DOMIT_DOMException::raiseException(DOMIT_NOT_SUPPORTED_ERROR, 
			('Method createDocumentType is not yet implemented.'));
	} //createDocumentType
} //DOMIT_DOMImplementation

?>
