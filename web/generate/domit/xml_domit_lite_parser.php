<?php
/**
* DOMIT! Lite is a non-validating, but lightweight and fast DOM parser for PHP
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @version 0.18
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

if (!defined('DOMIT_INCLUDE_PATH')) {
	define('DOMIT_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

/** current version of DOMIT! Lite */
define ('DOMIT_LITE_VERSION', '0.18');

/**
*@global array Flipped version of $definedEntities array, to allow two-way conversion of entities
* 
* Made global so that Attr nodes, which have no ownerDocument property, can access the array
*/
$GLOBALS['DOMIT_defined_entities_flip'] = array();

require_once(DOMIT_INCLUDE_PATH . 'xml_domit_shared.php');

/**
* The base class of all DOMIT node types
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Node {
	/** @var string The name of the node, varies according to node type */
	var $nodeName = null;
	/** @var string The value of the node, varies according to node type */
	var $nodeValue = null;
	/** @var int The type of node, e.g. CDataSection */
	var $nodeType = null;
	/** @var Object A reference to the parent of the current node */
	var $parentNode = null;
	/** @var Array An array of child node references */
	var $childNodes = null;
	/** @var Object A reference to the first node in the childNodes list */
	var $firstChild = null;
	/** @var Object A reference to the last node in the childNodes list */
	var $lastChild = null;
	/** @var Object A reference to the node prior to the current node in its parents childNodes list */
	var $previousSibling = null;
	/** @var Object A reference to the node after the current node in its parents childNodes list */
	var $nextSibling = null;
	/** @var Array An array of attribute key / value pairs */
	var $attributes = null;
	/** @var Object A reference to the Document node */
	var $ownerDocument = null;
	/** @var string The unique node id */
	var $uid;
	/** @var int The number of children of the current node */
	var $childCount = 0;
	
	/**
	* Raises error if abstract class is directly instantiated
	*/
	function DOMIT_Node() {		
		DOMIT_DOMException::raiseException(DOMIT_ABSTRACT_CLASS_INSTANTIATION_ERR, 
			 'Cannot instantiate abstract class DOMIT_Node'); 
	} //DOMIT_Node
	
	/**
	* DOMIT_Node constructor, assigns a uid
	*/
	function _constructor() {
		global $uidFactory;
		$this->uid = $uidFactory->generateUID();
	} //_constructor	
	
	/**
	* Appends a node to the childNodes list of the current node
	* @abstract 
	* @param Object The node to be appended 
	* @return Object The appended node
	*/
	function &appendChild(&$child) {
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method appendChild cannot be called by class ' . get_class($this)));
	} //appendChild
	
	/**
	* Inserts a node to the childNodes list of the current node
	* @abstract
	* @param Object The node to be inserted
	* @param Object The node before which the insertion is to occur 
	* @return Object The inserted node
	*/
	function &insertBefore(&$newChild, &$refChild) {
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method insertBefore cannot be called by class ' . get_class($this)));
	} //insertBefore
	
	/**
	* Replaces a node with another
	* @abstract
	* @param Object The new node
	* @param Object The old node
	* @return Object The new node
	*/
	function &replaceChild(&$newChild, &$oldChild) {
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method replaceChild cannot be called by class ' . get_class($this)));
	} //replaceChild
	
	/**
	* Removes a node from the childNodes list of the current node
	* @abstract
	* @param Object The node to be removed
	* @return Object The removed node
	*/
	function &removeChild(&$oldChild) {
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method removeChild cannot be called by class ' . get_class($this)));
	} //removeChild
	
	/**
	* Returns the index of the specified node in a childNodes list
	* @param Array The childNodes array to be searched
	* @param Object The node targeted by the search
	* @return int The index of the target node, or -1 if not found
	*/
	function getChildNodeIndex(&$arr, &$child) {
		$index = -1;
		$total = count($arr);
		
		for ($i = 0; $i < $total; $i++) {
			if ($child->uid == $arr[$i]->uid) {
				$index = $i;
				break;
			}
		}
		
		return $index;
	} //getChildNodeIndex
	
	/**
	* Determines whether a node has any children
	* @return boolean True if any child nodes are present
	*/
	function hasChildNodes() {
		return ($this->childCount > 0);
	} //hasChildNodes
	
	/**
	* Copies a node and/or its children 
	* @abstract
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &cloneNode($deep = false) {
		DOMIT_DOMException::raiseException(DOMIT_ABSTRACT_METHOD_INVOCATION_ERR, 
			 'Cannot invoke abstract method DOMIT_Node->cloneNode($deep). Must provide an overridden method in your subclass.'); 
	} //cloneNode
	
	/**
	* Adds elements with the specified tag name to a NodeList collection 
	* @param Object The NodeList collection
	* @param string The tag name of matching elements 
	*/
	function getNamedElements(&$nodeList, $tagName) {
		//Implemented in DOMIT_Element. 
		//Needs to be here though! This is called against all nodes in the document.
	} //getNamedElements	
	
	/**
	* Sets the ownerDocument property of a node to the containing DOMIT_Document
	* @param Object A reference to the document element of the DOMIT_Document
	*/
	function setOwnerDocument(&$rootNode) {
		if ($rootNode->ownerDocument == null) {
			unset($this->ownerDocument);
			$this->ownerDocument = null;
		}
		else {
			$this->ownerDocument =& $rootNode->ownerDocument;
		}
		
		$total = $this->childCount;
			
		for ($i = 0; $i < $total; $i++) {
			$this->childNodes[$i]->setOwnerDocument($rootNode);
		}
	} //setOwnerDocument
	
	/**
	* Tests whether a value is null, and if so, returns a default value
	* @param mixed The value to be tested
	* @param mixed The default value
	* @return mixed The specified value, or the default value if null
	*/
	function &nvl(&$value,$default) {
		  if (is_null($value)) return $default;
		  return $value;
	} //nvl	
	
	/**
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like expression.
	* @abstract
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByPath($pattern, $nodeIndex = 0) {
		 DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method getElementsByPath cannot be called by class ' . get_class($this)));
	} //getElementsByPath	
	
	/**
	* Returns the concatented text of the current node and its children
	* @return string The concatented text of the current node and its children
	*/
	function getText() {
		return $this->nodeValue;
	} //getText
	
	/**
	* Formats a string for presentation as HTML
	* @param string The string to be formatted
	* @param boolean True if the string is to be sent directly to output
	* @return string The HTML formatted string  
	*/
	function forHTML($str, $doPrint = false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
		return DOMIT_Utilities::forHTML($str, $doPrint);
	} //forHTML
	
	/**
	* Generates an array representation of the node and its children
	* @abstract
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method toArray cannot be called by class ' . get_class($this)));
	} //toArray
	
	/**
	* A node event that can be set to fire upon document loading, used for node initialization
	* @abstract
	*/
	function onLoad() {
		//you can override this method if you subclass any of the 
		//DOMIT_Nodes. It's a way of performing  
		//initialization of your subclass as soon as the document
		//has been loaded (as opposed to as soon as the current node
		//has been instantiated).
	} //onLoad
	
	/**
	* Clears previousSibling, nextSibling, and parentNode references from a node that has been removed
	*/
	function clearReferences() {
	    if ($this->previousSibling != null) {
	        unset($this->previousSibling);
	        $this->previousSibling = null;
	    }
	    if ($this->nextSibling != null) {
            unset($this->nextSibling);
	        $this->nextSibling = null;
	    }
	    if ($this->parentNode != null) {
            unset($this->parentNode);
	        $this->parentNode = null;
	    }
	} //clearReferences
	
	/**
	* Generates a normalized (formatted for readability) representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The formatted string representation 
	*/
	function toNormalizedString($htmlSafe = false, $subEntities=false) {
		//require this file for generating a normalized (readable) xml string representation
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
		global $DOMIT_defined_entities_flip;
		
		$result = DOMIT_Utilities::toNormalizedString($this, $subEntities, $DOMIT_defined_entities_flip);
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toNormalizedString
} //DOMIT_Node


/**
* A parent class for nodes which possess child nodes
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_ChildNodes_Interface extends DOMIT_Node {
	/**
	* Raises error if abstract class is directly instantiated
	*/
	function DOMIT_ChildNodes_Interface() {		
		DOMIT_DOMException::raiseException(DOMIT_ABSTRACT_CLASS_INSTANTIATION_ERR, 
			 'Cannot instantiate abstract class DOMIT_ChildNodes_Interface'); 
	} //DOMIT_ChildNodes_Interface
	
	/**
	* Appends a node to the childNodes list of the current node
	* @param Object The node to be appended 
	* @return Object The appended node
	*/
	function &appendChild(&$child) {
		if (!($this->hasChildNodes())) {
			$this->childNodes[0] =& $child;
			$this->firstChild =& $child;
		}
		else {
			//remove $child if it already exists
			$index = $this->getChildNodeIndex($this->childNodes, $child);
			
			if ($index != -1) {
				$this->removeChild($child);
			}
			
			//append child
			$numNodes = $this->childCount;
			$prevSibling =& $this->childNodes[($numNodes - 1)];
			
			$this->childNodes[$numNodes] =& $child; 
			
			//set next and previous relationships
			$child->previousSibling =& $prevSibling;
			$prevSibling->nextSibling =& $child;
		}

		$this->lastChild =& $child;
		$child->parentNode =& $this;
		
		unset($child->nextSibling);
		$child->nextSibling = null;
	
		$child->setOwnerDocument($this);
		$this->childCount++;
		
		return $child;
	} //appendChild
	
	/**
	* Inserts a node to the childNodes list of the current node
	* @param Object The node to be inserted
	* @param Object The node before which the insertion is to occur 
	* @return Object The inserted node
	*/
	function &insertBefore(&$newChild, &$refChild) {
		if (($refChild->nodeType == DOMIT_DOCUMENT_NODE)  || 
			($refChild->parentNode == null)) {
			
			DOMIT_DOMException::raiseException(DOMIT_NOT_FOUND_ERR, 
				 'Reference child not present in the child nodes list.'); 
		}
		
		//if reference child is also the node to be inserted
		//leave the document as is and don't raise an exception
		if ($refChild->uid == $newChild->uid) {
			return $newChild;
		}
		
		//remove $newChild if it already exists
		$index = $this->getChildNodeIndex($this->childNodes, $newChild);
		if ($index != -1) {
			$this->removeChild($newChild);
		}
	
		//find index of $refChild in childNodes
		$index = $this->getChildNodeIndex($this->childNodes, $refChild);
				
		if ($index != -1) {
			//reset sibling chain
			if ($refChild->previousSibling != null) {			
				$refChild->previousSibling->nextSibling =& $newChild;
				$newChild->previousSibling =& $refChild->previousSibling;
			}
			else {
				$this->firstChild =& $newChild;
				
				if ($newChild->previousSibling != null) {
					unset($newChild->previousSibling);
					$newChild->previousSibling = null;
				}
			}
			
			$newChild->parentNode =& $refChild->parentNode;
			$newChild->nextSibling =& $refChild;
			$refChild->previousSibling =& $newChild;
			
			//add node to childNodes
			$i = $this->childCount;
	
			while ($i >= 0) {		
				if ($i > $index) {
					$this->childNodes[$i] =& $this->childNodes[($i - 1)];
				}
				else if ($i == $index) {
					$this->childNodes[$i] =& $newChild;
				}
				$i--;
			}
			
			$this->childCount++;
		}
		else {
			$this->appendChild($newChild);
		}
		
		$newChild->setOwnerDocument($this);
		
		return $newChild;
	} //insertBefore
	
	/**
	* Replaces a node with another
	* @param Object The new node
	* @param Object The old node
	* @return Object The new node
	*/
	function &replaceChild(&$newChild, &$oldChild) {
		if ($this->hasChildNodes()) { 
			//remove $newChild if it already exists
			$index = $this->getChildNodeIndex($this->childNodes, $newChild);
			if ($index != -1) {
				$this->removeChild($newChild);
			}
		
			//find index of $oldChild in childNodes
			$index = $this->getChildNodeIndex($this->childNodes, $oldChild);
			
			if ($index != -1) {
				$newChild->ownerDocument =& $oldChild->ownerDocument;
				$newChild->parentNode =& $oldChild->parentNode;
				
				//reset sibling chain
				if ($oldChild->previousSibling == null) {
					unset($newChild->previousSibling);
					$newChild->previousSibling = null;
				}
				else {
					$oldChild->previousSibling->nextSibling =& $newChild;
					$newChild->previousSibling =& $oldChild->previousSibling;
				}
				
				if ($oldChild->nextSibling == null) {
					unset($newChild->nextSibling);
					$newChild->nextSibling = null;
				}
				else {
					$oldChild->nextSibling->previousSibling =& $newChild;
					$newChild->nextSibling =& $oldChild->nextSibling;
				}
	
				$this->childNodes[$index] =& $newChild;
				
				if ($index == 0) $this->firstChild =& $newChild;
				if ($index == ($this->childCount - 1)) $this->lastChild =& $newChild;
				
				$newChild->setOwnerDocument($this);
				
				return $newChild;
			}
		}
		
		DOMIT_DOMException::raiseException(DOMIT_NOT_FOUND_ERR, 
			('Reference node for replaceChild not found.'));
	} //replaceChild
	
	/**
	* Removes a node from the childNodes list of the current node
	* @param Object The node to be removed
	* @return Object The removed node
	*/
	function &removeChild(&$oldChild) {
		if ($this->hasChildNodes()) { 
			//find index of $oldChild in childNodes
			$index = $this->getChildNodeIndex($this->childNodes, $oldChild);
				
			if ($index != -1) {
				//reset sibling chain
				if (($oldChild->previousSibling != null) && ($oldChild->nextSibling != null)) {
					$oldChild->previousSibling->nextSibling =& $oldChild->nextSibling;
					$oldChild->nextSibling->previousSibling =& $oldChild->previousSibling;			
				}
				else if (($oldChild->previousSibling != null) && ($oldChild->nextSibling == null)) {
					$this->lastChild =& $oldChild->previousSibling;
					unset($oldChild->previousSibling->nextSibling);
					$oldChild->previousSibling->nextSibling = null;
				}
				else if (($oldChild->previousSibling == null) && ($oldChild->nextSibling != null)) {
					unset($oldChild->nextSibling->previousSibling);
					$oldChild->nextSibling->previousSibling = null;			
					$this->firstChild =& $oldChild->nextSibling;
				}
				else if (($oldChild->previousSibling == null) && ($oldChild->nextSibling == null)) {
					unset($this->firstChild);
					$this->firstChild = null;					
					unset($this->lastChild);
					$this->lastChild = null;
				}
				
				$total = $this->childCount;

				//remove node from childNodes
				for ($i = 0; $i < $total; $i++) {
					if ($i == ($total - 1)) {
						array_splice($this->childNodes, $i, 1);
					}
					else if ($i >= $index) {
						$this->childNodes[$i] =& $this->childNodes[($i + 1)];
					}
				}
				
				$this->childCount--;
				
				$oldChild->clearReferences();
				return $oldChild;
			}
		}

		DOMIT_DOMException::raiseException(DOMIT_NOT_FOUND_ERR, 
				('Target node for removeChild not found.'));
	} //removeChild
	
	/**
	* Searches the element tree for an element with the specified attribute name and value.
	* @param string The value of the attribute
	* @param string The name of the attribute
	* @param boolean True if the first found node is to be returned as a node instead of a nodelist
	* @return object A NodeList of found elements, or null
	*/
	function &getElementsByAttribute($attrName = 'id', $attrValue = '', 
											$returnFirstFoundNode = false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_nodemaps.php');
		
		$nodelist =& new DOMIT_NodeList();
		
		switch ($this->nodeType) {
			case DOMIT_ELEMENT_NODE:
				$this->_getElementsByAttribute($nodelist, $attrName = 'id', $attrValue, 
															$returnFirstFoundNode);
				break;
				
			case DOMIT_DOCUMENT_NODE:
				if ($this->documentElement != null) {
					$this->documentElement->_getElementsByAttribute($nodelist, 
										$attrName, $attrValue, $returnFirstFoundNode);
				}
				break;
		}

		if ($returnFirstFoundNode) {
			if ($nodelist->getLength() > 0) {
				return $nodelist->item(0);
			}
			else {
				return null;	
			}
		}
		else {
			return $nodelist;
		}
	} //getElementsByAttribute
	
	/**
	* Searches the element tree for an element with the specified attribute name and value.
	* @param object The node list of found elements
	* @param string The value of the attribute
	* @param string The name of the attribute
	* @param boolean True if the first found node is to be returned as a node instead of a nodelist
	* @param boolean True the node has been found
	*/
	function _getElementsByAttribute(&$nodelist, $attrName, $attrValue, 
										$returnFirstFoundNode, $foundNode = false) {
		if (!($foundNode && $returnFirstFoundNode)) {
			if ($this->getAttribute($attrName) == $attrValue) {
				$nodelist->appendNode($this);
				$foundNode = true;
				if ($returnFirstFoundNode) return;				
			} 

			$total = $this->childCount;

			for ($i = 0; $i < $total; $i++) {
				$currNode =& $this->childNodes[$i];
			
				if ($currNode->nodeType == DOMIT_ELEMENT_NODE) {
					$currNode->_getElementsByAttribute($nodelist, 
									$attrName, $attrValue, 
									$returnFirstFoundNode, $foundNode);
				}
			
			}
		}
	} //_getElementsByAttribute
} //DOMIT_ChildNodes_Interface

/**
* A class representing the DOM Document
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Lite_Document extends DOMIT_ChildNodes_Interface {
	/** @var string The xml declaration text */
	var $xmlDeclaration;
	/** @var string The doctype text */
	var $doctype;
	/** @var Object A reference to the root node of the DOM document */
	var $documentElement;
	/** @var string The parser used to process the DOM document, either "EXPAT" or "SAXY_LITE" */
	var $parser;
	/** @var Object A reference to the DOMIT_DOMImplementation object */
	var $implementation;
	/** @var Array User defined translation table for XML entities */
	var $definedEntities = array();
    /** @var boolean If true, loadXML or parseXML will attempt to detect and repair invalid xml */
    var $doResolveErrors = false;
    /** @var boolean If true, elements tags will be rendered to string as <element></element> rather than <element/> */
    var $doExpandEmptyElementTags = false;
	/** @var array A list of exceptions to the empty element expansion rule */
	var $expandEmptyElementExceptions = array();
	/** @var int The error code returned by the SAX parser */
    var $errorCode = 0;
	/** @var string The error string returned by the SAX parser */
    var $errorString = '';
	/** @var object A reference to a http connection or proxy server, if one is required */
    var $httpConnection = null;
	
	/**
	* DOM Document constructor
	*/
	function DOMIT_Lite_Document() {
		$this->_constructor();
		$this->xmlDeclaration = '';
		$this->doctype = '';
		$this->documentElement = null;
		$this->nodeType = DOMIT_DOCUMENT_NODE;
		$this->nodeName = '#document';
		$this->ownerDocument =& $this;
		$this->parser = '';
		$this->implementation =& new DOMIT_DOMImplementation();
	} //DOMIT_Lite_Document	
	
	/**
	* Specifies whether DOMIT! Lite will try to fix invalid XML before parsing begins
	* @param boolean True if errors are to be resolved
	*/
	function resolveErrors($truthVal) {
	    $this->doResolveErrors = $truthVal;
	} //resolveErrors
	
	/**
	* Specifies the parameters of the http conection used to obtain the xml data
	* @param string The ip address or domain name of the connection
	* @param string The path of the connection
	* @param int The port that the connection is listening on
	* @param int The timeout value for the connection
	* @param string The user name, if authentication is required
	* @param string The password, if authentication is required
	*/
	function setConnection($host, $path = '/', $port = 80, $timeout = 0, $user = null, $password = null) {
	    require_once(DOMIT_INCLUDE_PATH . 'php_http_client_generic.php');
		
		$this->httpConnection =& new php_http_client_generic($host, $path, $port, $timeout, $user, $password);
	} //setConnection
	
	/**
	* Specifies basic authentication for an http connection
	* @param string The user name
	* @param string The password
	*/
	function setAuthorization($user, $password) {
		$this->httpConnection->setAuthorization($user, $password);
	} //setAuthorization
	
	/**
	* Specifies that a proxy is to be used to obtain the xml data
	* @param string The ip address or domain name of the proxy
	* @param string The path to the proxy
	* @param int The port that the proxy is listening on
	* @param int The timeout value for the connection
	* @param string The user name, if authentication is required
	* @param string The password, if authentication is required
	*/
	function setProxyConnection($host, $path = '/', $port = 80, $timeout = 0, $user = null, $password = null) {
	    require_once(DOMIT_INCLUDE_PATH . 'php_http_proxy.php');
		
		$this->httpConnection =& new php_http_proxy($host, $path, $port, $timeout, $user, $password);
	} //setProxyConnection
	
	/**
	* Specifies basic authentication for the proxy
	* @param string The user name
	* @param string The password
	*/
	function setProxyAuthorization($user, $password) {
		$this->httpConnection->setProxyAuthorization($user, $password);
	} //setProxyAuthorization
	
	/**
	* Returns the error code from the underlying SAX parser
	* @return int The error code
	*/
	function getErrorCode() {
	    return $this->errorCode;
	} //getErrorCode
	
	/**
	* Returns the error string from the underlying SAX parser
	* @return string The error string
	*/
	function getErrorString() {
	    return $this->errorString;
	} //getErrorString
	
	/**
	* Specifies whether elements tags will be rendered to string as <element></element> rather than <element/>
	* @param boolean True if the expanded form is to be used
	* @param mixed An array of tag names that should be excepted from expandEmptyElements rule (optional)
	*/
	function expandEmptyElementTags($truthVal, $expandEmptyElementExceptions = false) {
		$this->doExpandEmptyElementTags = $truthVal;
		
		if (is_array($expandEmptyElementExceptions)) {
			$this->expandEmptyElementExceptions = $expandEmptyElementExceptions;
		}
	} //expandEmptyElementTags
	
	/**
	* Set the specified node as document element
	* @param Object The node that is to become document element
	* @return Object The new document element
	*/
	function &setDocumentElement(&$node) {
		if ($node->nodeType == DOMIT_ELEMENT_NODE) {
			if ($this->documentElement == null) {
				parent::appendChild($node);
			}
			else {
				parent::replaceChild($node, $this->documentElement);
			}
			
			$this->documentElement =& $node;
		}
		else {
			DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
				('Cannot add a node of type ' . get_class($node) . ' as a Document Element.'));
		}
		
		return $node;
	} //setDocumentElement
	
	/**
	* Appends a node to the childNodes list of the current node
	* @param Object The node to be appended 
	* @return Object The appended node
	*/
	function &appendChild(&$node) {
		if ($node->nodeType == DOMIT_ELEMENT_NODE) {
			if ($this->documentElement == null) { 
				parent::appendChild($node);
				$this->setDocumentElement($node);
			}
			else {
				//error thrown if documentElement already exists!
				DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
					('Cannot have more than one root node (documentElement) in a DOMIT_Document.'));
			}				
		}
		else {
			DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
				('Cannot add a node of type ' . get_class($node) . ' to a DOMIT_Document.'));
		}
		
		return $node;
	} //appendChild
	
	/**
	* Replaces a node with another
	* @param Object The new node
	* @param Object The old node
	* @return Object The new node
	*/
	function &replaceChild(&$newChild, &$oldChild) {
			if (($this->documentElement != null) && ($oldChild->uid == $this->documentElement->uid)) {
				if ($node->nodeType == DOMIT_ELEMENT_NODE) {
					//replace documentElement with new node
					$this->setDocumentElement($newChild);
				}
				else {
					DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
						('Cannot replace Document Element with a node of class ' . get_class($newChild)));
				}
			}
			else {
				if ($node->nodeType == DOMIT_ELEMENT_NODE) {	
					if ($this->documentElement != null) {
						DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
							('Cannot have more than one root node (documentElement) in a DOMIT_Document.'));
					}
					else {
						parent::replaceChild($newChild, $oldChild);
					}
				}
				else {
					DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
						('Nodes of class ' . get_class($newChild) . ' cannot be children of a DOMIT_Document.'));
				}
			}				

		return $newChild;
	} //replaceChild
	
	/**
	* Inserts a node to the childNodes list of the current node
	* @param Object The node to be inserted
	* @param Object The node before which the insertion is to occur 
	* @return Object The inserted node
	*/
	function &insertBefore(&$newChild, &$refChild) {
		$type = $newChild->nodeType;
		
		if ($type == DOMIT_ELEMENT_NODE) {
			if ($this->documentElement == null) { 
				parent::insertBefore($newChild);
				$this->setDocumentElement($newChild);
			}
			else {
				//error thrown if documentElement already exists!
				DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
					('Cannot have more than one root node (documentElement) in a DOMIT_Document.'));
			}				
		}	
		else {
			DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
				('Cannot insert a node of type ' . get_class($newChild) . ' to a DOMIT_Document.'));
		}
		
		return $newChild;
	} //insertBefore
	
	/**
	* Removes a node from the childNodes list of the current node
	* @param Object The node to be removed
	* @return Object The removed node
	*/
	function &removeChild(&$oldChild) {
		if (($this->documentElement != null) && ($oldChild->uid == $this->documentElement->uid)) {
			parent::removeChild($oldChild);
			$this->documentElement = null;
		}
		else {
			parent::removeChild($oldChild);
		}
		
		$oldChild->clearReferences();
		return $oldChild;
	} //removeChild
	
	/**
	* Creates a new DOMIT_Element node
	* @param string The tag name of the element
	* @return Object The new element
	*/
	function &createElement($tagName) {
		$node =& new DOMIT_Element($tagName);
		$node->ownerDocument =& $this;
		
		return $node;
	} //createElement
	
	/**
	* Creates a new DOMIT_Text node
	* @param string The text of the node
	* @return Object The new text node
	*/
	function &createTextNode($data) {
		$node =& new DOMIT_TextNode($data);
		$node->ownerDocument =& $this;
	
		return $node;
	} //createTextNode
	
	/**
	* Creates a new DOMIT_CDataSection node
	* @param string The text of the CDATASection
	* @return Object The new CDATASection node
	*/
	function &createCDATASection($data) {
		$node =& new DOMIT_CDATASection($data);
		$node->ownerDocument =& $this;
		
		return $node;
	} //createCDATASection
	
	/**
	* Retrieves a NodeList of child elements with the specified tag name
	* @param string The matching element tag name
	* @return Object A NodeList of found elements
	*/
	function &getElementsByTagName($tagName) {
		$nodeList =& new DOMIT_NodeList();
		
		if ($this->documentElement != null) {
			$this->documentElement->getNamedElements($nodeList, $tagName);
		}
		
		return $nodeList;
	} //getElementsByTagName
	
	/**
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like expression.
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByPath($pattern, $nodeIndex = 0) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_getelementsbypath.php');
	
		$gebp = new DOMIT_GetElementsByPath();
		$myResponse =& $gebp->parsePattern($this, $pattern, $nodeIndex);

		return $myResponse;
	} //getElementsByPath	
	
	/**
	* Parses an xml string; first encodes string as UTF-8
	* @param string The xml text to be parsed
	* @param boolean True if SAXY is to be used instead of Expat
	* @param boolean False if CDATA Section are to be generated as Text nodes
	* @param boolean True if onLoad is to be called on each node after parsing
	* @return boolean True if parsing is successful
	*/
	function parseXML_utf8($xmlText, $useSAXY = true, $preserveCDATA = true, $fireLoadEvent = false) {
		return $this->parseXML(utf8_encode($xmlText), $useSAXY, $preserveCDATA, $fireLoadEvent);
	} //parseXML_utf8
	
	/**
	* Parses an xml string
	* @param string The xml text to be parsed
	* @param boolean True if SAXY is to be used instead of Expat
	* @param boolean False if CDATA Section are to be generated as Text nodes
	* @param boolean True if onLoad is to be called on each node after parsing
	* @return boolean True if parsing is successful
	*/
	function parseXML($xmlText, $useSAXY = true, $preserveCDATA = true, $fireLoadEvent = false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
  
        if ($this->doResolveErrors) {
            require_once(DOMIT_INCLUDE_PATH . 'xml_domit_doctor.php');
            $xmlText = DOMIT_Doctor::fixAmpersands($xmlText);
        }
		
		if (DOMIT_Utilities::validateXML($xmlText)) {
			$domParser =& new DOMIT_Parser();
			
			if ($useSAXY || (!function_exists('xml_parser_create'))) {
				//use SAXY parser to populate xml tree
				$this->parser = 'SAXY_LITE';
				$success = $domParser->parseSAXY($this, $xmlText, $preserveCDATA, $this->definedEntities);
			}
			else {
				//use Expat parser to populate xml tree
				$this->parser = 'EXPAT';
				$success = $domParser->parse($this, $xmlText, $preserveCDATA);
			}
			
			if ($fireLoadEvent && ($this->documentElement != null)) $this->load($this->documentElement);
				
			return $success;
		}
		else {
			return false;
		}
	} //parseXML
	
	/**
	* Parses an xml file; first encodes text as UTF-8
	* @param string The xml file to be parsed
	* @param boolean True if SAXY is to be used instead of Expat
	* @param boolean False if CDATA Section are to be generated as Text nodes
	* @param boolean True if onLoad is to be called on each node after parsing
	* @return boolean True if parsing is successful
	*/
	function loadXML_utf8($filename, $useSAXY = true, $preserveCDATA = true, $fireLoadEvent = false) {
		$xmlText = $this->getTextFromFile($filename);
		return $this->parseXML_utf8($xmlText, $useSAXY, $preserveCDATA, $fireLoadEvent);
	} //loadXML_utf8	
	
	/**
	* Parses an xml file
	* @param string The xml file to be parsed
	* @param boolean True if SAXY is to be used instead of Expat
	* @param boolean False if CDATA Section are to be generated as Text nodes
	* @param boolean True if onLoad is to be called on each node after parsing
	* @return boolean True if parsing is successful
	*/
	function loadXML($filename, $useSAXY = true, $preserveCDATA = true, $fireLoadEvent = false) {
		$xmlText = $this->getTextFromFile($filename);
		return $this->parseXML($xmlText, $useSAXY, $preserveCDATA, $fireLoadEvent);
	} //loadXML
	
	/**
	* Retrieves text from a file
	* @param string The file path
	* @return string The text contained in the file
	*/
	function getTextFromFile($filename) {
		if ($this->httpConnection != null) {
			$response =& $this->httpConnection->get($filename);
			$this->httpConnection->disconnect();
			return $response->getResponse();
		}
		else if (function_exists('file_get_contents')) {
		    //if (file_exists($filename)) {
				return file_get_contents($filename);
		    //}
		}
		else {
			require_once(DOMIT_INCLUDE_PATH . 'php_file_utilities.php');

			$fileContents =& php_file_utilities::getDataFromFile($filename, 'r');
			return $fileContents;
		}
		
		return '';
	} //getTextFromFile	
	
	/**
	* Saves the current DOM document as an xml file; first encodes text as UTF-8
	* @param string The path of the xml file
	* @param boolean True if xml text is to be normalized before saving
	* @return boolean True if save is successful
	*/
	function saveXML_utf8($filename, $normalized=false) {
		if ($normalized) {
			$stringRep = $this->toNormalizedString(false, true); //param 2 is $subEntities
		}
		else {
			$stringRep = $this->toString(false, true);
		}
		
		return $this->saveTextToFile($filename, utf8_encode($stringRep));
	} //saveXML_utf8
	
	/**
	* Saves the current DOM document as an xml file
	* @param string The path of the xml file
	* @param boolean True if xml text is to be normalized before saving
	* @return boolean True if save is successful
	*/
	function saveXML($filename, $normalized=false) {
		if ($normalized) {
			$stringRep = $this->toNormalizedString(false, true);
		}
		else {
			$stringRep = $this->toString(false, true);
		}
		
		return $this->saveTextToFile($filename, $stringRep);
	} //saveXML	
	
	/**
	* Saves text to a file
	* @param string The file path
	* @param string The text to be saved
	* @return boolean True if the save is successful
	*/
	function saveTextToFile($filename, $text) {
		if (function_exists('file_put_contents')) {
			file_put_contents($filename, $text);
		}
		else {
			require_once(DOMIT_INCLUDE_PATH . 'php_file_utilities.php');
			php_file_utilities::putDataToFile($filename, $text, 'w');
		}
		
		return (file_exists($filename) && is_writable($filename));
	} //saveTextToFile		
	
	/**
	* Indicates the SAX parser used to parse the current document
	* @return string Either "SAXY_LITE" or "EXPAT"
	*/
	function parsedBy() {
		return $this->parser;
	} //parsedBy
	
	/**
	* Returns the concatented text of the current node and its children
	* @return string The concatented text of the current node and its children
	*/
	function getText() {
		if ($this->documentElement != null) {
			$root =& $this->documentElement; 
			return $root->getText();
		}
		else {
			return '';
		}
	} //getText
	
	/**
	* Returns the doctype text
	* @return string The doctype text, or an emty string
	*/
	function getDocType() {
		return $this->doctype;
	} //getDocType
	
	/**
	* Returns the xml declaration text
	* @return mixed The xml declaration text, or an empty string
	*/
	function getXMLDeclaration() {
		return $this->xmlDeclaration;
	} //getXMLDeclaration
	
	/**
	* Returns a reference to the DOMIT_DOMImplementation object
	* @return Object A reference to the DOMIT_DOMImplementation object
	*/
	function &getDOMImplementation() {
		return $this->implementation;
	} //getDOMImplementation
	
	/**
	* Manages the firing of the onLoad() event
	* @param Object The parent node of the current recursion
	*/
	function load(&$contextNode) {		
		$total = $contextNode->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			$currNode =& $contextNode->childNodes[$i];
			$currNode->ownerDocument->load($currNode);
		}

		$contextNode->onLoad();
	} //load
	
	/**
	* Returns the current version of DOMIT! Lite
	* @return Object The current version of DOMIT! Lite
	*/
	function getVersion() {
		return DOMIT_LITE_VERSION;
	} //getVersion
	
	/**
	* Appends an array of entity mappings to the existing translation table
	* 
	* Intended mainly to facilitate the conversion of non-ASCII entities into equivalent characters 
	* 
	* @param array A list of entity mappings in the format: array('&amp;' => '&');
	*/
	function appendEntityTranslationTable($table) {
		$this->definedEntities = $table;
		
		global $DOMIT_defined_entities_flip;
		$DOMIT_defined_entities_flip = array_flip($table);
	} //appendEntityTranslationTable
	
	/**
	* Generates an array representation of the node and its children
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		$arReturn = array($this->nodeName => array());
		$total = $this->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			$arReturn[$this->nodeName][$i] = $this->childNodes[$i]->toArray();
		} 
		
		return $arReturn;
	} //toArray
	
	/**
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &cloneNode($deep = false) {
		$className = get_class($this);
		$clone =& new $className($this->nodeName);

		if ($deep) {
			$total = $this->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currentChild =& $this->childNodes[$i];
				$clone->appendChild($currentChild->cloneNode($deep));
			}
		}

		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		$result = '';				
		$total = $this->childCount;

		for ($i = 0; $i < $total; $i++) {
			$result .= $this->childNodes[$i]->toString(false, $subEntities);
		}
		
		if ($htmlSafe) $result = $this->forHTML($result);

		return $result;
	} //toString
} //DOMIT_Lite_Document

/**
* A class representing the DOM Element
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Element extends DOMIT_ChildNodes_Interface {
	/**
	* DOM Element constructor
	* @param string The tag name of the element
	*/
	function DOMIT_Element($tagName) {
		$this->_constructor();		
		$this->nodeType = DOMIT_ELEMENT_NODE;
		$this->nodeName = $tagName;
		$this->attributes = array();
		$this->childNodes = array();
	} //DOMIT_Element
	
	/**
	* Returns the tag name of the element
	* @return string The tag name of the element
	*/
	function getTagName() {
		return $this->nodeName;
	} //getTagName
	
	/**
	* Adds elements with the specified tag name to a NodeList collection 
	* @param Object The NodeList collection
	* @param string The tag name of matching elements 
	*/
	function getNamedElements(&$nodeList, $tagName) {
		if (($this->nodeName == $tagName) || ($tagName == '*')) {
			$nodeList->appendNode($this); 
		}
		
		$total = $this->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			$this->childNodes[$i]->getNamedElements($nodeList, $tagName);
		}
	} //getNamedElements
	
	/**
	* Returns the concatented text of the current node and its children
	* @return string The concatented text of the current node and its children
	*/
	function getText() {
		$text = '';
		$numChildren = $this->childCount;
		
		for ($i = 0; $i < $numChildren; $i++) {
			$child =& $this->childNodes[$i];
			$text .= $child->getText();
		}
		
		return $text;
	} //getText
	
	/**
	* If a child text node exists, sets the nodeValue to $data. A child text node is created if none exists
	* @param string The text data of the node
	*/
	function setText($data) {
	    switch ($this->childCount) {
	        case 1:
	            if ($this->firstChild->nodeType == DOMIT_TEXT_NODE) {
	                $this->firstChild->setText($data);
	            }
	            break;

	        case 0:
	            $childTextNode =& $this->ownerDocument->createTextNode($data);
	            $this->appendChild($childTextNode);
	            break;

	        default:
	            //do nothing. Maybe throw error???
	    }
	} //setText
	
	/**
	* Retrieves a NodeList of child elements with the specified tag name
	* @param string The matching element tag name
	* @return Object A NodeList of found elements
	*/
	function &getElementsByTagName($tagName) {
		$nodeList =& new DOMIT_NodeList();		
		$this->getNamedElements($nodeList, $tagName);
		
		return $nodeList;
	} //getElementsByTagName
	
	/**
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like expression.
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByPath($pattern, $nodeIndex = 0) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_getelementsbypath.php');
	
		$gebp = new DOMIT_GetElementsByPath();
		$myResponse =& $gebp->parsePattern($this, $pattern, $nodeIndex);

		return $myResponse;
	} //getElementsByPath	
	
	/**
	* Gets the value of the specified attribute, if it exists
	* @param string The attribute name
	* @return string The attribute value
	*/
	function getAttribute($name) {
		if ($this->hasAttribute($name)) {
			return $this->attributes[$name];
		}
	} //getAttribute	
	
	/**
	* Sets the value of the specified attribute; creates a new attribute if one doesn't exist
	* @param string The attribute name
	* @param string The desired attribute value
	*/
	function setAttribute($name, $value) {
		$this->attributes[$name] = $value;
	} //setAttribute	
	
	/**
	* Removes the specified attribute
	* @param string The name of the attribute to be removed
	*/
	function removeAttribute($name) {
		if ($this->hasAttribute($name)) {
			unset($this->attributes[$name]);
		}
	} //removeAttribute
	
	/**
	* Determines whether an attribute with the specified name exists
	* @param string The name of the attribute
	* @return boolean True if the attribute exists
	*/
	function hasAttribute($name) {
		return isset($this->attributes[$name]);
	} //hasAttribute
	
	/**
	* Collapses adjacent text nodes in entire element subtree
	*/
	function normalize() {
		if ($this->hasChildNodes()) {
			$currNode =& $this->childNodes[0];
			
			while ($currNode->nextSibling != null) {
				$nextNode =& $currNode->nextSibling;
				
				if (($currNode->nodeType == DOMIT_TEXT_NODE) && 
					($nextNode->nodeType == DOMIT_TEXT_NODE)) {						
					$currNode->nodeValue .= $nextNode->nodeValue;
					$this->removeChild($nextNode);
				}
				else {
					$currNode->normalize();
				}
				
				if ($currNode->nextSibling != null) {
					$currNode =& $currNode->nextSibling;
				}
			}
		}
	} //normalize
	
	/**
	* Generates an array representation of the node and its children
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		$arReturn = array($this->nodeName => array("attributes" => $this->attributes));
		$total = $this->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			$arReturn[$this->nodeName][$i] = $this->childNodes[$i]->toArray();
		} 
		
		return $arReturn;
	} //toArray
	
	/**
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &cloneNode($deep = false) {
		$className = get_class($this);
		$clone =& new $className($this->nodeName);
		
		$clone->attributes = $this->attributes;
		
		if ($deep) {
			$total = $this->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currentChild =& $this->childNodes[$i];
				$clone->appendChild($currentChild->cloneNode($deep));				
			}
		}
		
		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
		global $DOMIT_defined_entities_flip;
		
		$result = '<' . $this->nodeName;
		
		//get attributes
		foreach ($this->attributes as $key => $value) {
			$result .= ' ' . $key . '="';
			$result .= ($subEntities ? DOMIT_Utilities::convertEntities($value, 
							$DOMIT_defined_entities_flip) : $value);
			$result .= '"';
		}
		
		//get children
		$myNodes =& $this->childNodes;
		$total = count($myNodes);
		
		if ($total != 0) {
			$result .= '>';
			
			for ($i = 0; $i < $total; $i++) {
				$child =& $myNodes[$i];
				$result .= $child->toString(false, $subEntities);
			}
			
			$result .= '</' . $this->nodeName . '>';
		}
		else {
			if ($this->ownerDocument->doExpandEmptyElementTags) {
				if (in_array($this->nodeName, $this->ownerDocument->expandEmptyElementExceptions)) {
					$result .= ' />';
				}
				else {
                	$result .= '></' . $this->nodeName . '>';
				}
		    }
		    else {
				if (in_array($this->nodeName, $this->ownerDocument->expandEmptyElementExceptions)) {
					$result .= '></' . $this->nodeName . '>';
				}
				else {
					$result .= ' />';
				}
		    }
		}	
		
		if ($htmlSafe) $result = $this->forHTML($result);	
		
		return $result;
	} //toString
} //DOMIT_Element

/**
* A class representing the DOM Text Node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_TextNode extends DOMIT_Node {
	/**
	* DOM Text Node constructor
	* @param string The text of the node
	*/
	function DOMIT_TextNode($data) {
		$this->_constructor();	
		$this->nodeType = DOMIT_TEXT_NODE;
		$this->nodeName = '#text';
		$this->setText($data);
	} //DOMIT_TextNode
	
	/**
	* Returns the text contained in the current node
	* @return string The text of the current node
	*/
	function getText() {
		return $this->nodeValue;
	} //getText
	
 	/**
	* Sets the text contained in the current node to $data.
	* @param string The text data of the node
	*/
	function setText($data) {
        $this->nodeValue = $data;
	} //setText
	
	/**
	* Generates an array representation of the node and its children
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		return $this->toString();
	} //toArray
	
	/**
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &cloneNode($deep = false) {
		$className = get_class($this);
		$clone =& new $className($this->nodeValue);
		
		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
		global $DOMIT_defined_entities_flip;
		
		$result = ($subEntities ? DOMIT_Utilities::convertEntities($this->nodeValue, 
						$DOMIT_defined_entities_flip) : $this->nodeValue);
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toString
} //DOMIT_TextNode

/**
* A class representing the DOM CDATA Section
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_CDATASection extends DOMIT_TextNode {
	/**
	* DOM CDATA Section node constructor
	* @param string The text of the node
	*/
	function DOMIT_CDATASection($data) {
		$this->_constructor();		
		$this->nodeType = DOMIT_CDATA_SECTION_NODE;
		$this->nodeName = '#cdata-section';
		$this->setText($data);
	} //DOMIT_CDATASection
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		$result = '<![CDATA[';  
		$result .= $subEntities ? str_replace("]]>", "]]&gt;", $this->nodeValue) :
								 $this->nodeValue; 
		$result .= ']]>';
		
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_CDATASection

/**
* Manages the generation of a DOMIT! document from SAX events
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-lite
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Parser {
	/** @var Object A reference to the resulting xmldoc */
	var $xmlDoc = null;
	/** @var Object A reference to the current node in the parsing process */
	var $currentNode = null;
	/** @var Object A reference to the last child in the parsing process */
	var $lastChild = null;
	/** @var boolean True if currently parsing a CDATA Section */
	var $inCDATASection = false; //flag for Expat
	/** @var boolean True if currently parsing a Text node */
	var $inTextNode = false;
	/** @var boolean True is CDATA Section nodes are not to be converted into Text nodes */
	var $preserveCDATA;
	/** @var string A container for holding the currently parsed text data */
	var $parseContainer = '';
	/** @var string The current docutype text */
	var $parseItem = '';
	
	/**
	* Parses xml text using Expat
	* @param Object A reference to the DOM document that the xml is to be parsed into
	* @param string The text to be parsed
	* @param boolean True if CDATA Section nodes are not to be converted into Text nodes
	* @return boolean True if the parsing is successful
	*/
	function parse (&$myXMLDoc, $xmlText, $preserveCDATA = true) {
		$this->xmlDoc =& $myXMLDoc;
		$this->lastChild =& $this->xmlDoc;
		
		$this->preserveCDATA = $preserveCDATA;
		
		//create instance of expat parser (should be included in php distro)
		if (version_compare(phpversion(), '5.0', '<=')) {
			$parser =& xml_parser_create('');
		}
		else {
			$parser =& xml_parser_create();
		}
		
		//set handlers for SAX events
		xml_set_object($parser, $this);
		xml_set_element_handler($parser, 'startElement', 'endElement'); 		
		xml_set_character_data_handler($parser, 'dataElement');
		xml_set_default_handler($parser, 'defaultDataElement');  
		xml_parser_set_option($parser, XML_OPTION_CASE_FOLDING, 0); 	
		xml_parser_set_option($parser, XML_OPTION_SKIP_WHITE, 1);
		
		//parse out whitespace -  (XML_OPTION_SKIP_WHITE = 1 does not 
		//seem to work consistently across versions of PHP and Expat
		$xmlText = eregi_replace('>' . "[[:space:]]+" . '<' , '><', $xmlText);
		
		$success = xml_parse($parser, $xmlText);
		
		$this->xmlDoc->errorCode = xml_get_error_code($parser);
		$this->xmlDoc->errorString = xml_error_string($this->xmlDoc->errorCode);
		
		xml_parser_free($parser); 
		
		return $success;
	} //parse
	
	/**
	* Parses xml text using SAXY
	* @param Object A reference to the DOM document that the xml is to be parsed into
	* @param string The text to be parsed
	* @param boolean True if CDATA Section nodes are not to be converted into Text nodes
	* @return boolean True if the parsing is successful
	*/
	function parseSAXY(&$myXMLDoc, $xmlText, $preserveCDATA, $definedEntities) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_saxy_lite_parser.php');
		
		$this->xmlDoc =& $myXMLDoc;
		$this->lastChild =& $this->xmlDoc;
		
		//create instance of SAXY parser 
		$parser =& new SAXY_Lite_Parser();
		$parser->appendEntityTranslationTable($definedEntities);
		
		$parser->xml_set_element_handler(array(&$this, 'startElement'), array(&$this, 'endElement'));
		$parser->xml_set_character_data_handler(array(&$this, 'dataElement'));
		
		if ($preserveCDATA) {
			$parser->xml_set_cdata_section_handler(array(&$this, 'cdataElement'));
		}
		
		$success = $parser->parse($xmlText);
		
		$this->xmlDoc->errorCode = $parser->xml_get_error_code();
		$this->xmlDoc->errorString = $parser->xml_error_string($this->xmlDoc->errorCode);
		
		return $success;
	} //parseSAXY
	
	/**
	* Generates and appends a new text node from the parseContainer text
	*/
	function dumpTextNode() {
		//traps for mixed content
	    $currentNode =& $this->xmlDoc->createTextNode($this->parseContainer);
		$this->lastChild->appendChild($currentNode);
		$this->inTextNode = false;
		$this->parseContainer = '';
	} //dumpTextNode

	/**
	* Catches a start element event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The tag name of the current element
	* @param Array An array of the element attributes
	*/
	function startElement(&$parser, $name, $attrs) {
		if ($this->inTextNode) {
			$this->dumpTextNode();
		}
		
		$currentNode =& $this->xmlDoc->createElement($name);
		$currentNode->attributes = $attrs;
		$this->lastChild->appendChild($currentNode);
		$this->lastChild =& $currentNode;
	} //startElement	
	
	/**
	* Catches an end element event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The tag name of the current element
	*/
	function endElement(&$parser, $name) {
		if ($this->inTextNode) {
			$this->dumpTextNode();
		}
		
		$this->lastChild =& $this->lastChild->parentNode;
	} //endElement	 
	
	/**
	* Catches a data event and processes the text
	* @param Object A reference to the current SAX parser
	* @param string The current text data
	*/
	function dataElement(&$parser, $data) {
		if (!$this->inCDATASection) $this->inTextNode = true;
		
		$this->parseContainer .= $data;	
	} //dataElement	
	
	/**
	* Catches a CDATA Section event and processes the text
	* @param Object A reference to the current SAX parser
	* @param string The current text data
	*/
	function cdataElement(&$parser, $data) {
		$currentNode =& $this->xmlDoc->createCDATASection($data);

		$this->lastChild->appendChild($currentNode);
	} //cdataElement	
	
	/**
	* Catches a default data event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The current data
	*/
	function defaultDataElement(&$parser, $data) {
		if (strlen($data) > 2){
			$pre = strtoupper(substr($data, 0, 3));
			
			switch ($pre) {
				case '<![': //cdata section coming
					if ($this->preserveCDATA) {
						$this->inCDATASection = true;
					}
					break;	
				case ']]>': //cdata remnant - ignore
					$currentNode =& $this->xmlDoc->createCDATASection($this->parseContainer);
					$this->lastChild->appendChild($currentNode);
					$this->inCDATASection = false;
					$this->parseContainer = '';
					break;
			}
		}
	} //defaultDataElement
} //DOMIT_Parser

?>