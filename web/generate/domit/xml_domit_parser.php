<?php
/**
* DOMIT! is a non-validating, but lightweight and fast DOM parser for PHP
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @version 0.99
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

if (!defined('DOMIT_INCLUDE_PATH')) {
	define('DOMIT_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

/** current version of DOMIT! */
define ('DOMIT_VERSION', '0.99');

/** current version of SAXY */
define ('DOMIT_XML_NAMESPACE', 'http://www.w3.org/xml/1998/namespace');

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
* @subpackage domit-xmlparser-main
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
	/** @var Object A NodeList of attribute nodes */
	var $attributes = null;
	/** @var Object A reference to the Document node */
	var $ownerDocument = null;
	/** @var String A URI that identifies the XML namespace to which the node belongs */
	var $namespaceURI = null;
	/** @var String The namespace prefix for the node */
	var $prefix = null;
	/** @var String The local name of the node */
	var $localname = null;
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
	* Determines whether a node has any attributes
	* @return boolean True if the node has attributes
	*/
	function hasAttributes() {
		//overridden in DOMIT_Element
		return false;
	} //hasChildNodes
	
	/**
	* Collapses adjacent text nodes in entire node subtree
	*/
	function normalize() {
		if (($this->nodeType == DOMIT_DOCUMENT_NODE) && ($this->documentElement != null)) {
			$this->documentElement->normalize();
		}
	} //normalize
	
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
	* Performs an XPath query (NOT YET IMPLEMENTED!)
	* @param string The query pattern
	* @return Object A NodeList containing the found nodes
	*/
	function &selectNodes($pattern) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_xpath.php');
		
		$xpParser =& new DOMIT_XPath();
		
		return $xpParser->parsePattern($this, $pattern);		
	} //selectNodes	
	
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
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like attribute expression (NOT YET IMPLEMENTED!)
	* @abstract
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByAttributePath($pattern, $nodeIndex = 0) {
		 DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method getElementsByAttributePath cannot be called by class ' . get_class($this)));
	} //getElementsByAttributePath
	
	/**
	* Adds all child nodes of the specified nodeType to the NodeList
	* @abstract
	* @param Object The NodeList collection
	* @param string The nodeType of matching nodes 
	*/
	function getTypedNodes(&$nodeList, $type) {		 
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method getTypedNodes cannot be called by class ' . get_class($this)));
	} //getTypedNodes
	
	/**
	* Adds all child nodes of the specified nodeValue to the NodeList
	* @abstract
	* @param Object The NodeList collection
	* @param string The nodeValue of matching nodes 
	*/
	function getValuedNodes(&$nodeList, $value) {		 
		DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
			('Method getValuedNodes cannot be called by class ' . get_class($this)));
	} //getValuedNodes
	
	/**
	* Returns the concatented text of the current node and its children
	* @return string The concatented text of the current node and its children
	*/
	function getText() {
		return $this->nodeValue;
	} //getText
	
	/**
	* Indicates whether the specified feature is supported by the DOM implementation and this node
	* @param string The feature
	* @param string The version of the DOM implementation
	* @return boolean True if the specified feature is supported
	*/
	function isSupported($feature, $version = null) {
		//don't worry about parsing based on version at this point in time; 
		//the only feature that is supported is 'XML'...
		if (($version == '1.0') || ($version == '2.0') || ($version == null)) {
			if (strtoupper($feature) == 'XML') {
				return true;
			}				
		}
		
		return false;
	} //isSupported

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
* @subpackage domit-xmlparser-main
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
		if ($child->nodeType == DOMIT_ATTRIBUTE_NODE) {
		    DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
				('Cannot add a node of type ' . get_class($child) . ' using appendChild'));
		}
		else if ($child->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) {
			$total = $child->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currChild =& $child->childNodes[$i];
				$this->appendChild($currChild);
			}
		}
		else {
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
		}
		
		return $child;
	} //appendChild
	
	/**
	* Inserts a node to the childNodes list of the current node
	* @param Object The node to be inserted
	* @param Object The node before which the insertion is to occur 
	* @return Object The inserted node
	*/
	function &insertBefore(&$newChild, &$refChild) {
	    if ($newChild->nodeType == DOMIT_ATTRIBUTE_NODE) {
	    	DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
				('Cannot add a node of type ' . get_class($newChild) . ' using insertBefore'));
	    }
	
		if (($refChild->nodeType == DOMIT_DOCUMENT_NODE) ||
			($refChild->parentNode->nodeType == DOMIT_DOCUMENT_NODE) || 
			($refChild->parentNode == null)) {
			
			DOMIT_DOMException::raiseException(DOMIT_NOT_FOUND_ERR, 
				 'Reference child not present in the child nodes list.'); 
		}
		
		//if reference child is also the node to be inserted
		//leave the document as is and don't raise an exception
		if ($refChild->uid == $newChild->uid) {
			return $newChild;
		}
		
		//if $newChild is a DocumentFragment, 
		//loop through and insert each node separately
		if ($newChild->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) {
			$total = $newChild->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currChild =& $newChild->childNodes[$i];
				$this->insertBefore($currChild, $refChild);
			}
			
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
	    if ($newChild->nodeType == DOMIT_ATTRIBUTE_NODE) {
	    	DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
				('Cannot add a node of type ' . get_class($newChild) . ' using replaceChild'));
	    }
		else if ($newChild->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) { //if $newChild is a DocumentFragment
			//replace the first node then loop through and insert each node separately
			$total = $newChild->childCount;
			
			if ($total > 0) {
				$newRef =& $newChild->lastChild;
				$this->replaceChild($newRef, $oldChild);
			
				for ($i = 0; $i < ($total - 1); $i++) {
					$currChild =& $newChild->childNodes[$i];
					$this->insertBefore($currChild, $newRef);
				}
			}
			
			return $newChild;
		}
		else {
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
		}
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
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Document extends DOMIT_ChildNodes_Interface {
	/** @var Object The xml declaration processing instruction */
	var $xmlDeclaration;
	/** @var Object A reference to a DOMIT_DocType object */
	var $doctype;
	/** @var Object A reference to the root node of the DOM document */
	var $documentElement;
	/** @var string The parser used to process the DOM document, either "EXPAT" or "SAXY" */
	var $parser;
	/** @var Object A reference to the DOMIT_DOMImplementation object */
	var $implementation;
	/** @var boolean True if the DOM document has been modifed since being parsed (NOT YET IMPLEMENTED!) */
	var $isModified;
	/** @var boolean True if whitespace is to be preserved during parsing (NOT YET IMPLEMENTED!) */
	var $preserveWhiteSpace = false; 
	/** @var Array User defined translation table for XML entities; passed to SAXY */
	var $definedEntities = array();
    /** @var boolean If true, loadXML or parseXML will attempt to detect and repair invalid xml */
    var $doResolveErrors = false;
    /** @var boolean If true, elements tags will be rendered to string as <element></element> rather than <element/> */
    var $doExpandEmptyElementTags = false;
	/** @var array A list of exceptions to the empty element expansion rule */
	var $expandEmptyElementExceptions = array();
    /** @var boolean If true, namespaces will be processed */
    var $isNamespaceAware = false;
	/** @var int The error code returned by the SAX parser */
    var $errorCode = 0;
	/** @var string The error string returned by the SAX parser */
    var $errorString = '';
	/** @var object A reference to a http connection or proxy server, if one is required */
    var $httpConnection = null;
	
	/**
	* DOM Document constructor
	*/
	function DOMIT_Document() {
		$this->_constructor();
		$this->xmlDeclaration = null;
		$this->doctype = null;
		$this->documentElement = null;
		$this->nodeType = DOMIT_DOCUMENT_NODE;
		$this->nodeName = '#document';
		$this->ownerDocument =& $this;
		$this->parser = '';
		$this->implementation =& new DOMIT_DOMImplementation();
	} //DOMIT_Document	
	
	/**
	* Specifies whether DOMIT! will try to fix invalid XML before parsing begins
	* @param boolean True if errors are to be resolved
	*/
	function resolveErrors($truthVal) {
	    $this->doResolveErrors = $truthVal;
	} //resolveErrors
	
	/**
	* Specifies whether DOMIT! processes namespace information
	* @param boolean True if namespaces are to be processed
	*/
	function setNamespaceAwareness($truthVal) {
	    $this->isNamespaceAware = $truthVal;
	} //setNamespaceAwareness
	
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
	    switch ($node->nodeType) {
	        case DOMIT_ELEMENT_NODE:
	            if ($this->documentElement == null) {
					parent::appendChild($node);
					$this->setDocumentElement($node);
				}
				else {
					//error thrown if documentElement already exists!
					DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
						('Cannot have more than one root node (documentElement) in a DOMIT_Document.'));
				}
	            break;
	            
			case DOMIT_PROCESSING_INSTRUCTION_NODE:
			case DOMIT_COMMENT_NODE:
			    parent::appendChild($node);
			    break;
			    
   			case DOMIT_DOCUMENT_TYPE_NODE:
   			    if ($this->doctype == null) {
					parent::appendChild($node);
				}
				else {
					DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
						('Cannot have more than one doctype node in a DOMIT_Document.'));
				}
   			    break;
   			    
			case DOMIT_DOCUMENT_FRAGMENT_NODE:
			    $total = $node->childCount;

				for ($i = 0; $i < $total; $i++) {
					$currChild =& $node->childNodes[$i];
					$this->appendChild($currChild);
				}
			    break;
			    
   			default:
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
		if ($this->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) {			
			$total = $newChild->childCount;
			
			if ($total > 0) {
				$newRef =& $newChild->lastChild;
				$this->replaceChild($newRef, $oldChild);
			
				for ($i = 0; $i < ($total - 1); $i++) {
					$currChild =& $newChild->childNodes[$i];
					parent::insertBefore($currChild, $newRef);
				}
			}
		}
		else {
			if (($this->documentElement != null) && ($oldChild->uid == $this->documentElement->uid)) {
				if ($newChild->nodeType == DOMIT_ELEMENT_NODE) {
					//replace documentElement with new node
					$this->setDocumentElement($newChild);
				}
				else {
					DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
						('Cannot replace Document Element with a node of class ' . get_class($newChild)));
				}
			}
			else {
				switch ($newChild->nodeType) {
				    case DOMIT_ELEMENT_NODE:
				        if ($this->documentElement != null) {
							DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
								('Cannot have more than one root node (documentElement) in a DOMIT_Document.'));
						}
						else {
							parent::replaceChild($newChild, $oldChild);
						}
				        break;

					case DOMIT_PROCESSING_INSTRUCTION_NODE:
					case DOMIT_COMMENT_NODE:
					    parent::replaceChild($newChild, $oldChild);
					    break;
					    
	 				case DOMIT_DOCUMENT_TYPE_NODE:
	 				    if ($this->doctype != null) {
							if ($this->doctype->uid == $oldchild->uid) {
								parent::replaceChild($newChild, $oldChild);
							}
							else {
								DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
									('Cannot have more than one doctype node in a DOMIT_Document.'));
							}
						}
						else {
							parent::replaceChild($newChild, $oldChild);
						}
	 				    break;
	 				    
	  				default:
	  				    DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR,
							('Nodes of class ' . get_class($newChild) . ' cannot be children of a DOMIT_Document.'));
				}
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
		
		if ($this->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) {			
			$total = $newChild->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currChild =& $newChild->childNodes[$i];
				$this->insertBefore($currChild, $refChild);
			}
		}
		else if ($type == DOMIT_ELEMENT_NODE) {
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
		else if ($type == DOMIT_PROCESSING_INSTRUCTION_NODE) {
			parent::insertBefore($newChild);
		}
		else if ($type == DOMIT_DOCUMENT_TYPE_NODE) {
			if ($this->doctype == null) {
				parent::insertBefore($newChild);
			}
			else {
				DOMIT_DOMException::raiseException(DOMIT_HIERARCHY_REQUEST_ERR, 
					('Cannot have more than one doctype node in a DOMIT_Document.'));
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
		if ($this->nodeType == DOMIT_DOCUMENT_FRAGMENT_NODE) {			
			$total = $oldChild->childCount;
			
			for ($i = 0; $i < $total; $i++) {
				$currChild =& $oldChild->childNodes[$i];
				$this->removeChild($currChild);
			}
		}
		else {
			if (($this->documentElement != null) && ($oldChild->uid == $this->documentElement->uid)) {
				parent::removeChild($oldChild);
				$this->documentElement = null;
			}
			else {
				parent::removeChild($oldChild);
			}
		}
		
		$oldChild->clearReferences();
		return $oldChild;
	} //removeChild
	
	/**
	* Imports a node from another document to this document (not yet implemented)
	* @param Object The node to import
	* @param boolean True if a child nodes of the imported node are to be included in the import
	* @return Object The imported node
	*/
	function &importNode(&$importedNode, $deep = true) {
		//not yet implemented
		DOMIT_DOMException::raiseException(DOMIT_NOT_SUPPORTED_ERROR, 
			('Method importNode is not yet implemented.'));
	} //importNode

	/**
	* Creates a new DOMIT_DocumentFragment node
	* @return Object The new document fragment node
	*/
	function &createDocumentFragment() {
		$node =& new DOMIT_DocumentFragment();
		$node->ownerDocument =& $this;
		
		return $node;
	} //createDocumentFragment
	
	/**
	* Creates a new DOMIT_Attr node
	* @param string The name of the attribute
	* @return Object The new attribute node
	*/
	function &createAttribute($name) {
		$node =& new DOMIT_Attr($name);
		
		return $node;
	} //createAttribute
	
	/**
	* Creates a new DOMIT_Attr node (namespace aware)
	* @param string The namespaceURI of the attribute
	* @param string The qualifiedName of the attribute
	* @return Object The new attribute node
	*/
	function &createAttributeNS($namespaceURI, $qualifiedName) {
		$node =& new DOMIT_Attr($qualifiedName);
		$node->namespaceURI = $namespaceURI;
		
		$colonIndex = strpos($qualifiedName, ":");
		
		if ($colonIndex !== false) {
	    	$node->prefix = substr($qualifiedName, 0, $colonIndex);
	    	$node->localName = substr($qualifiedName, ($colonIndex + 1));
		}
		else {
			$node->prefix = '';
			$node->localName = $qualifiedName;
		}

		return $node;
	} //createAttributeNS
	
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
	* Creates a new DOMIT_Element node (namespace aware)
	* @param string The namespaceURI of the element
	* @param string The qualifiedName of the element
	* @return Object The new element
	*/
	function &createElementNS($namespaceURI, $qualifiedName) {
	    $node =& new DOMIT_Element($qualifiedName);

	    $colonIndex = strpos($qualifiedName, ":");
		
		if ($colonIndex !== false) { 
	    	$node->prefix = substr($qualifiedName, 0, $colonIndex);
	    	$node->localName = substr($qualifiedName, ($colonIndex + 1));
		}
		else {
			$node->prefix = '';
	    	$node->localName = $qualifiedName;
		}
		
	    $node->namespaceURI = $namespaceURI;
		
		$node->ownerDocument =& $this;

		return $node;
	} //createElementNS
	
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
	* Creates a new DOMIT_Comment node
	* @param string The comment text
	* @return Object The new comment node
	*/
	function &createComment($text) {
		$node =& new DOMIT_Comment($text);
		$node->ownerDocument =& $this;
		
		return $node;
	} //createComment
	
	/**
	* Creates a new DOMIT_ProcessingInstruction node
	* @param string The target of the processing instruction
	* @param string The data of the processing instruction
	* @return Object The new processing instruction node
	*/
	function &createProcessingInstruction($target, $data) {
		$node =& new DOMIT_ProcessingInstruction($target, $data);
		$node->ownerDocument =& $this;
		
		return $node;
	} //createProcessingInstruction
	
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
	* Retrieves a NodeList of child elements with the specified namespaceURI and localName
	* @param string The matching namespaceURI
	* @param string The matching localName
	* @return Object A NodeList of found elements
	*/
	function &getElementsByTagNameNS($namespaceURI, $localName) {
		$nodeList =& new DOMIT_NodeList();

		if ($this->documentElement != null) {
			$this->documentElement->getNamedElementsNS($nodeList, $namespaceURI, $localName);
		}

		return $nodeList;
	} //getElementsByTagNameNS
	
	/**
	* Returns the element whose ID is given by elementId.
	* @param string The id of the matching element
	* @param boolean True if XML spec is to be strictly adhered to (only attributes xml:id are considered valid)
	* @return Object The found element or null
	*/
	function &getElementByID($elementID, $isStrict = true) {
		if ($this->isNamespaceAware) {
			if ($this->documentElement != null) {				
				$targetAttrNode =& $this->documentElement->_getElementByID($elementID, $isStrict);
				return $targetAttrNode->ownerElement;
			}
		
			return null;
		}
		else {
			DOMIT_DOMException::raiseException(DOMIT_INVALID_ACCESS_ERR, 
			 	'Namespace awareness must be enabled to use method getElementByID'); 
		}
	} //getElementByID
	
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
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like attribute expression (NOT YET IMPLEMENTED!)
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByAttributePath($pattern, $nodeIndex = 0) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_getelementsbypath.php');
	
		$gabp = new DOMIT_GetElementsByAttributePath();
		$myResponse =& $gabp->parsePattern($this, $pattern, $nodeIndex);

		return $myResponse;
	} //getElementsByAttributePath	
	
	/**
	* Retrieves all child nodes of the specified nodeType
	* @param string The nodeType of matching nodes
	* @param Object The root node of the search
	* @return Object A NodeList containing found nodes	 
	*/
	function &getNodesByNodeType($type, &$contextNode) {
		$nodeList =& new DOMIT_NodeList();
		
		if (($type == DOMIT_DOCUMENT_NODE) || ($contextNode->nodeType == DOMIT_DOCUMENT_NODE)){
			$nodeList->appendNode($this); 
		}
		else if ($contextNode->nodeType == DOMIT_ELEMENT_NODE) {
			$contextNode->getTypedNodes($nodeList, $type);
		}
		else if ($contextNode->uid == $this->uid) {
			if ($this->documentElement != null) {
				if ($type == DOMIT_ELEMENT_NODE) {
					$nodeList->appendNode($this->documentElement); 
				}
					
				$this->documentElement->getTypedNodes($nodeList, $type);
			}
		}
		
		return $nodeList;
	} //getNodesByNodeType	

	/**
	* Retrieves all child nodes of the specified nodeValue
	* @param string The nodeValue of matching nodes 
	* @param Object The root node of the search
	* @return Object A NodeList containing found nodes
	*/
	function &getNodesByNodeValue($value, &$contextNode) {
		$nodeList =& new DOMIT_NodeList();
		
		 if ($contextNode->uid == $this->uid) {
			 if ($this->nodeValue == $value) {
				 $nodeList->appendNode($this);
			 }
		 }
		
		if ($this->documentElement != null) {
			$this->documentElement->getValuedNodes($nodeList, $value);
		}
		
		return $nodeList;
	} //getNodesByNodeValue
	
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
				$this->parser = 'SAXY';
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
	* @return string Either "SAXY" or "EXPAT"
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

		return '';
	} //getText
	
	/**
	* Returns a doctype object
	* @return mixed The doctype object, or null if none exists
	*/
	function getDocType() {
		return $this->doctype;
	} //getDocType
	
	/**
	* Returns the xml declaration processing instruction
	* @return mixed The xml declaration processing instruction, or null if none exists
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
	* Returns the current version of DOMIT!
	* @return Object The current version of DOMIT!
	*/
	function getVersion() {
		return DOMIT_VERSION;
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
				
				if ($currentChild->nodeType == DOMIT_DOCUMENT_TYPE_NODE) {
					$clone->doctype =& $clone->childNodes[$i];
				}
				
				if (($currentChild->nodeType == DOMIT_PROCESSING_INSTRUCTION_NODE) && 
						($currentChild->getTarget() == 'xml')) {
					$clone->xmlDeclaration =& $clone->childNodes[$i];
				}
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
} //DOMIT_Document

/**
* A class representing the DOM Element
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
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
		$this->attributes = new DOMIT_NamedNodeMap_Attr();
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
	* Adds elements with the specified tag name to a NodeList collection
	* @param Object The NodeList collection
	* @param string The namespaceURI of matching elements
	* @param string The localName of matching elements
	*/
	function getNamedElementsNS(&$nodeList, $namespaceURI, $localName) {
	    if ((($namespaceURI == $this->namespaceURI) || ($namespaceURI == '*')) &&
                (($localName == $this->localName) || ($localName == '*')))	{
	        $nodeList->appendNode($this);
	    }

		$total = $this->childCount;

		for ($i = 0; $i < $total; $i++) {
			if ($this->childNodes[$i]->nodeType == DOMIT_ELEMENT_NODE) {
				$this->childNodes[$i]->getNamedElementsNS($nodeList, $namespaceURI, $localName);
			}
		}
	} //getNamedElementsNS
	
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
	* Retrieves a NodeList of child elements with the specified namespaceURI and localName
	* @param string The namespaceURI
	* @param string The localName
	* @return Object A NodeList of found elements
	*/
	function &getElementsByTagNameNS($namespaceURI, $localName) {
		$nodeList =& new DOMIT_NodeList();
		$this->getNamedElementsNS($nodeList, $namespaceURI, $localName);

		return $nodeList;
	} //getElementsByTagNameNS
	
	/**
	* Returns the attribute node whose ID is given by elementId.
	* @param string The id of the matching element
	* @param boolean True if XML spec is to be strictly adhered to (only attributes xml:id are considered valid)
	* @return Object The found attribute or null
	*/
	function &_getElementByID($elementID, $isStrict) {
		if ($isStrict) {
			$myAttrNode =& $this->getAttributeNodeNS(DOMIT_XML_NAMESPACE, 'id');
			if (($myAttrNode != null)&& ($myAttrNode->getValue() == $elementID)) return $myAttrNode;
		}
		else {
			$myAttrNode =& $this->getAttributeNodeNS('', 'ID');
			if (($myAttrNode != null)&& ($myAttrNode->getValue() == $elementID)) return $myAttrNode;
			
			$myAttrNode =& $this->getAttributeNodeNS('', 'id');
			if (($myAttrNode != null)&& ($myAttrNode->getValue() == $elementID)) return $myAttrNode;
		}
		
		$total = $this->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			if ($this->childNodes[$i]->nodeType == DOMIT_ELEMENT_NODE) {
				$foundNode =& $this->childNodes[$i]->_getElementByID($elementID, $isStrict);
			
				if ($foundNode != null) {
					return $foundNode;
				}	
			}
		}
		
		return null;
	} //_getElementByID
	
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
	* Retrieves an element or DOMIT_NodeList of elements corresponding to an Xpath-like attribute expression (NOT YET IMPLEMENTED!)
	* @param string The query pattern
	* @param int If a single node is to be returned (rather than the entire NodeList) the index of that node
	* @return mixed A NodeList or single node that matches the pattern
	*/
	function &getElementsByAttributePath($pattern, $nodeIndex = 0) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_getelementsbypath.php');
	
		$gabp = new DOMIT_GetElementsByAttributePath();
		$myResponse =& $gabp->parsePattern($this, $pattern, $nodeIndex);

		return $myResponse;
	} //getElementsByAttributePath
	
	/**
	* Adds all child nodes of the specified nodeType to the NodeList
	* @param Object The NodeList collection
	* @param string The nodeType of matching nodes 
	*/
	function getTypedNodes(&$nodeList, $type) {
		$numChildren = $this->childCount;
				
		for ($i = 0; $i < $numChildren; $i++) {
			$child =& $this->childNodes[$i];
			
			if ($child->nodeType == $type) {
				$nodeList->appendNode($child);
			}
			
			if ($child->hasChildNodes()) {
				$child->getTypedNodes($nodeList, $type);
			}
		}
	} //getTypedNodes
	
	/**
	* Adds all child nodes of the specified nodeValue to the NodeList
	* @param Object The NodeList collection
	* @param string The nodeValue of matching nodes 
	*/
	function getValuedNodes(&$nodeList, $value) {
		$numChildren = $this->childCount;
				
		for ($i = 0; $i < $numChildren; $i++) {
			$child =& $this->childNodes[$i];
			
			if ($child->nodeValue == $value) {
				$nodeList->appendNode($child);
			}
			
			if ($child->hasChildNodes()) {
				$child->getValuedNodes($nodeList, $value);
			}
		}
	} //getValuedNodes
	
	/**
	* Gets the value of the specified attribute, if it exists
	* @param string The attribute name
	* @return string The attribute value
	*/
	function getAttribute($name) {
		$returnNode =& $this->attributes->getNamedItem($name);
		
		if ($returnNode == null) {
			return '';
		}
		else {
			return $returnNode->getValue();
		}
	} //getAttribute

	/**
	* Gets the value of the attribute with the specified namespaceURI and localName, if it exists
	* @param string The namespaceURI
	* @param string The localName
	* @return string The attribute value
	*/
	function getAttributeNS($namespaceURI, $localName) {
		$returnNode =& $this->attributes->getNamedItemNS($namespaceURI, $localName);

		if ($returnNode == null) {
			return '';
		}
		else {
			return $returnNode->getValue();
		}
	} //getAttributeNS
	
	/**
	* Sets the value of the specified attribute; creates a new attribute if one doesn't exist
	* @param string The attribute name
	* @param string The desired attribute value
	*/
	function setAttribute($name, $value) {
		$returnNode =& $this->attributes->getNamedItem($name);
		
		if ($returnNode == null) {
			$newAttr =& new DOMIT_Attr($name);
			$newAttr->setValue($value);
			$this->attributes->setNamedItem($newAttr);
		}
		else {
			$returnNode->setValue($value);
		}
	} //setAttribute

	/**
	* Sets the value of the specified attribute; creates a new attribute if one doesn't exist
	* @param string The attribute namespaceURI
	* @param string The attribute qualifiedName
	* @param string The desired attribute value
	*/
	function setAttributeNS($namespaceURI, $qualifiedName, $value) {
	    //get localName
	    $colonIndex = strpos($qualifiedName, ":");
		
		if ($colonIndex !== false) {
	    	$localName = substr($qualifiedName, ($colonIndex + 1));
		}
		else {
			$localName = $qualifiedName;
		}
	    
		$returnNode =& $this->attributes->getNamedItemNS($namespaceURI, $localName);

		if ($returnNode == null) {
			//create this manually in case element has no ownerDocument to reference
			$newAttr =& new DOMIT_Attr($qualifiedName);
	    	$newAttr->prefix = substr($qualifiedName, 0, $colonIndex);
	    	$newAttr->localName = $localName;
	    	$newAttr->namespaceURI = $namespaceURI;
	    	
			$newAttr->setValue($value);
			$this->attributes->setNamedItemNS($newAttr);
			$newAttr->ownerElement =& $this;
		}
		else {
			$returnNode->setValue($value);
		}
	} //setAttributeNS
	
	/**
	* Removes the specified attribute
	* @param string The name of the attribute to be removed
	*/
	function removeAttribute($name) {
		$returnNode =& $this->attributes->removeNamedItem($name);
	} //removeAttribute
	
	/**
	* Removes the specified attribute
	* @param string The namespaceURI of the attribute to be removed
	* @param string The localName of the attribute to be removed
	*/
	function removeAttributeNS($namespaceURI, $localName) {
		$returnNode =& $this->attributes->removeNamedItemNS($namespaceURI, $localName);
		unset($returnNode->ownerElement);
		$returnNode->ownerElement = null;
	} //removeAttributeNS
	
	/**
	* Determines whether an attribute with the specified name exists
	* @param string The name of the attribute
	* @return boolean True if the attribute exists
	*/
	function hasAttribute($name) {
		$returnNode =& $this->attributes->getNamedItem($name);

		return ($returnNode != null);
	} //hasAttribute
	
	/**
	* Determines whether an attribute with the specified namespaceURI and localName exists
	* @param string The namespaceURI of the attribute
	* @param string The localName of the attribute
	* @return boolean True if the attribute exists
	*/
	function hasAttributeNS($namespaceURI, $localName) {
		$returnNode =& $this->attributes->getNamedItemNS($namespaceURI, $localName);

		return ($returnNode != null);
	} //hasAttributeNS
	
	/**
	* Determines whether the element has any atributes
	* @return boolean True if the element has any atributes
	*/
	function hasAttributes() {
		return ($this->attributes->getLength() > 0);
	} //hasChildNodes
	
	/**
	* Gets a reference to the specified attribute node
	* @param string The attribute name
	* @return Object A reference to the found node, or null 
	*/
	function &getAttributeNode($name) {
		$returnNode =& $this->attributes->getNamedItem($name);
		return $returnNode;
	} //getAttributeNode

	/**
	* Gets a reference to the specified attribute node
	* @param string The attribute namespaceURI
	* @param string The attribute localName
	* @return Object A reference to the found node, or null
	*/
	function &getAttributeNodeNS($namespaceURI, $localName) {
		$returnNode =& $this->attributes->getNamedItemNS($namespaceURI, $localName);
		return $returnNode;
	} //getAttributeNodeNS
	
	/**
	* Adds an attribute node to the current element
	* @param Object The attribute node to be added
	* @return Object A reference to the newly added node
	*/
	function &setAttributeNode(&$newAttr) {
		$returnNode =& $this->attributes->setNamedItem($newAttr);
		return $returnNode;
	} //setAttributeNode

	/**
	* Adds an attribute node to the current element (namespace aware)
	* @param Object The attribute node to be added
	* @return Object A reference to the newly added node
	*/
	function &setAttributeNodeNS(&$newAttr) {
		$returnNode =& $this->attributes->setNamedItemNS($newAttr);
		$newAttr->ownerElement =& $this;
		return $returnNode;
	} //setAttributeNodeNS
	
	/**
	* Removes an attribute node from the current element
	* @param Object The attribute node to be removed
	* @return Object A reference to the removed node
	*/
	function &removeAttributeNode(&$oldAttr) {
		$attrName = $oldAttr->getName();
		$returnNode =& $this->attributes->removeNamedItem($attrName);
		
		if ($returnNode == null) {
			DOMIT_DOMException::raiseException(DOMIT_NOT_FOUND_ERR, 
				'Target attribute not found.');
		}
		else {
			return $returnNode;
		}
	} //removeAttributeNode
	
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
		$arReturn = array($this->nodeName => array("attributes" => $this->attributes->toArray()));
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
		
		$clone->attributes =& $this->attributes->createClone($deep);
		
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
		$result = '<' . $this->nodeName;
		$result .= $this->attributes->toString(false, $subEntities);		
		
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
* A parent class for Text and CDATA Section nodes
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_CharacterData extends DOMIT_Node {
	/**
	* Prevents direct instantiation of DOMIT_CharacterData class
	* @abstract
	*/
	function DOMIT_CharacterData() {		
		DOMIT_DOMException::raiseException(DOMIT_ABSTRACT_CLASS_INSTANTIATION_ERR, 
			 'Cannot instantiate abstract class DOMIT_CharacterData'); 
	} //DOMIT_CharacterData
	
	/**
	* Gets the node value of the current text node 
	* @return string The node value of the current text node 
	*/
	function getData() {
		return $this->nodeValue;
	} //getData
	
	/**
	* Gets the length of the text in the current node
	* @return int The length of the text in the current node 
	*/
	function getLength() {
		return strlen($this->nodeValue);
	} //getLength
	
	/**
	* Gets a subset of the current node text
	* @param int The starting point of the substring
	* @param int The length of the substring
	* @return string The subset of the current node text 
	*/
	function substringData($offset, $count) {
		$totalChars = $this->getLength();
		
		if (($offset < 0) || (($offset + $count) > $totalChars)) {
			
			DOMIT_DOMException::raiseException(DOMIT_INDEX_SIZE_ERR, 
				'Character Data index out of bounds.');
				
		}
		else {
			$data = $this->getData();
			return substr($data, $offset, $count);
		}
	} //substringData
	
	/**
	* Appends the specified text to the current node text
	* @param string The text to be appended
	*/
	function appendData($arg) {
		$this->setText($this->nodeValue . $arg);
	} //appendData
	
	/**
	* Inserts text at the sepecified offset
	* @param int The insertion point
	* @param string The text to be inserted
	*/
	function insertData($offset, $arg) {
		$totalChars = $this->getLength();
		
		if (($offset < 0) || ($offset > $totalChars)) {

			DOMIT_DOMException::raiseException(DOMIT_INDEX_SIZE_ERR, 
				'Character Data index out of bounds.');
				
		}
		else {
			$data = $this->getData();
			$pre = substr($data, 0, $offset);
			$post = substr($data, $offset);
			
			$this->setText(($pre . $arg . $post));
		}
	} //insertData
	
	/**
	* Deletes a subset of the current node text
	* @param int The starting point of the deletion
	* @param int The length of the deletion
	*/
	function deleteData($offset, $count) {
		$totalChars = $this->getLength();
		
		if (($offset < 0) || (($offset + $count) > $totalChars)) {
		
			DOMIT_DOMException::raiseException(DOMIT_INDEX_SIZE_ERR, 
				'Character Data index out of bounds.');
				
		}
		else {
			$data = $this->getData();
			$pre = substr($data, 0, $offset);
			$post = substr($data, ($offset + $count));
			
			$this->setText(($pre . $post));
		}
	} //substringData
	
	/**
	* Replaces a subset of the current node text with the specified text
	* @param int The starting point of the replacement
	* @param int The length of the replacement
	* @param string The replacement text
	*/
	function replaceData($offset, $count, $arg) {
		$totalChars = $this->getLength();
		
		if (($offset < 0) || (($offset + $count) > $totalChars)) {
		
			DOMIT_DOMException::raiseException(DOMIT_INDEX_SIZE_ERR, 
				'Character Data index out of bounds.');
				
		}
		else {
			$data = $this->getData();
			$pre = substr($data, 0, $offset);
			$post = substr($data, ($offset + $count));
			
			$this->setText(($pre . $arg . $post));
		}
	} //replaceData
} //DOMIT_CharacterData

/**
* A class representing the DOM Text Node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_TextNode extends DOMIT_CharacterData {
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
	* Splits a single node into multiple nodes, based on the specified offset
	* @param int The offset point for the split
	* @return Object The newly created text node
	*/
	function splitText($offset) {
		$totalChars = $this->getLength();
		
		if (($offset < 0) || ($offset > $totalChars)) {
		
			DOMIT_DOMException::raiseException(DOMIT_INDEX_SIZE_ERR, 
				'Character Data index out of bounds.');
				
		}
		else {
			$data = $this->getData();
			$pre = substr($data, 0, $offset);
			$post = substr($data, $offset);
			
			$this->setText($pre);
			
			//create new text node
			$className = get_class($this);
			$newTextNode =& new $className($post);
			$newTextNode->ownerDocument =& $this->ownerDocument;
			
			if ($this->parentNode->lastChild->uid == $this->uid) {
				$this->parentNode->appendChild($newTextNode);
			}
			else {
				$this->parentNode->insertBefore($newTextNode, $this);
			}
			
			return $newTextNode;
		}
	} //splitText
	
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
		
		$result = $subEntities ? DOMIT_Utilities::convertEntities($this->nodeValue, 
							$DOMIT_defined_entities_flip) : $this->nodeValue;
	
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_TextNode

/**
* A class representing the DOM CDATA Section
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
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
* A class representing the Attr node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Attr extends DOMIT_Node {
	/** @var boolean True if the attribute has been modified since parsing (NOT YET IMPLEMENTED!) */
	var $specified = false;
	/** @var Object A reference to the element to which the attribute is assigned */
	var $ownerElement = null;
	
	/**
	* DOM Attr node constructor
	* @param string The name of the attribute
	*/
	function DOMIT_Attr($name) {
		$this->_constructor();	
		$this->nodeType = DOMIT_ATTRIBUTE_NODE;
		$this->nodeName =$name;
	} //DOMIT_Attr	
	
	/**
	* Returns the name of the attribute
	* @return string The name of the attribute
	*/
	function getName() {
		return $this->nodeName;
	} //getName
	
	/**
	* Indicates whether an attribute has been modified since parsing
	* @return boolean True if the node has been modified
	*/
	function getSpecified() {
		return $this->specified;
	} //getSpecified
	
	/**
	* Returns the value of the attribute
	* @return string The value of the attribute
	*/
	function getValue() {
		return $this->nodeValue;
	} //getValue
	
	/**
	* Sets the value of the attribute
	* @param string The value of the attribute
	*/
	function setValue($value) {
		$this->nodeValue = $value;
	} //setValue
	
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
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &cloneNode($deep = false) {
		$className = get_class($this);
		$clone =& new $className($this->nodeName);
		$clone->nodeValue = $this->nodeValue;
		
		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if HTML entities should be substituted
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_utilities.php');
		global $DOMIT_defined_entities_flip;
		
		$result = ' ' . $this->nodeName . '="';
		$result .= $subEntities ? DOMIT_Utilities::convertEntities($this->nodeValue, 
							$DOMIT_defined_entities_flip) : $this->nodeValue;
		$result .= '"';
	
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_Attr

/**
* A class representing the DOM Document Fragment node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_DocumentFragment extends DOMIT_ChildNodes_Interface {
	/**
	* DOM Document Fragment node constructor
	*/
	function DOMIT_DocumentFragment() {
		$this->_constructor();	
		$this->nodeType = DOMIT_DOCUMENT_FRAGMENT_NODE;
		$this->nodeName ='#document-fragment';
		$this->nodeValue = null;
		$this->childNodes = array();
	} //DOMIT_DocumentFragment	
	
	/**
	* Generates an array representation of the node and its children
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		$arReturn = array();
		$total = $this->childCount;
		
		for ($i = 0; $i < $total; $i++) {
			$arReturn[$i] = $this->childNodes[$i]->toArray();
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
		$clone =& new $className();
		
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
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		//get children
		$result = '';
		$myNodes =& $this->childNodes;
		$total = count($myNodes);
		
		if ($total != 0) {
			for ($i = 0; $i < $total; $i++) {
				$child =& $myNodes[$i];
				$result .= $child->toString(false, $subEntities);
			}
		}
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toString
} //DOMIT_DocumentFragment

/**
* A class representing the DOM Comment node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Comment extends DOMIT_CharacterData {
	/**
	* DOM Comment node constructor
	* @param string The 
	*/
	function DOMIT_Comment($nodeValue) {
		$this->_constructor();	
		$this->nodeType = DOMIT_COMMENT_NODE;
		$this->nodeName = '#comment';
		$this->nodeValue = $nodeValue;
	} //DOMIT_Comment	
	
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
	* @return string The string representation  
	*/
	function toString($htmlSafe = false) {
		$result = '<!--' . $this->nodeValue . '-->';
	
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_Comment

/**
* A class representing the DOM Processing Instruction node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_ProcessingInstruction extends DOMIT_Node {
	/**
	* DOM Processing Instruction node constructor
	* @param string The 
	*/
	function DOMIT_ProcessingInstruction($target, $data) {
		$this->_constructor();	
		$this->nodeType = DOMIT_PROCESSING_INSTRUCTION_NODE;
		$this->nodeName = $target;
		$this->nodeValue = $data;
	} //DOMIT_ProcessingInstruction	
	
	/**
	* Returns the processing instruction target
	* @return string The processing instruction target
	*/
	function getTarget() {
		return $this->nodeName;
	} //getTarget
	
	/**
	* Returns the processing instruction data
	* @return string The processing instruction data
	*/
	function getData() {
		return $this->nodeValue;
	} //getData
	
	/**
	* Returns the text contained in the current node
	* @return string The text of the current node
	*/
	function getText() {
		return ($this->nodeName . ' ' . $this->nodeValue);
	} //getText
	
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
		$clone =& new $className($this->nodeName, $this->nodeValue);
		
		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @return string The string representation  
	*/
	function toString($htmlSafe = false) {
		$result = '<' . '?' . $this->nodeName . ' ' . $this->nodeValue . '?' . '>';
	
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_ProcessingInstruction

/**
* A class representing the DOM Document Type node
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_DocumentType extends DOMIT_Node {
	/** @var string The doctype name */
	var $name;
	/** @var Object True if the entity has been modified since parsing (NOT YET IMPLEMENTED!) */
	var $entities;
	/** @var Object A NodeList of notation nodes (NOT YET IMPLEMENTED!) */
	var $notations;
	/** @var Object A NodeList of elements (NOT YET IMPLEMENTED!) */
	var $elements;
	/** @var string The full text of the doctype */
	var $text;
	/** @var string The public identifier of the external subset */
	var $publicID;
	/** @var string The system identifier of the external subset */
	var $systemID;
	/** @var string The full text of internal subset */
	var $internalSubset;
	
	/**
	* DOM Document Type node constructor
	* @param string The 
	*/
	function DOMIT_DocumentType($name, $text) {
		$this->_constructor();	
		$this->nodeType = DOMIT_DOCUMENT_TYPE_NODE;
		$this->nodeName = $name;
		$this->name = $name;
		$this->entities = null; //implement later
		$this->notations = null; //implement later
		$this->elements = null; //implement later
		$this->text = $text;
	} //DOMIT_DocumentType	
	
	/**
	* Returns the text contained in the current node
	* @return string The text of the current node
	*/
	function getText() {
		return $this->text;
	} //getText
	
	/**
	* Returns the name of the doctype node
	* @return string The name of the doctype node 
	*/
	function getName() {
		return $this->name;
	} //getName
	
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
		$clone =& new $className($this->nodeName, $this->text);
		
		return $clone;
	} //cloneNode
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @return string The string representation  
	*/
	function toString($htmlSafe = false) {
		$result = $this->text;
	
		if ($htmlSafe) $result = $this->forHTML($result);
	
		return $result;
	} //toString
} //DOMIT_DocumentType


/**
* A class representing the DOM Notation node (NOT YET IMPLEMENTED!)
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_Notation extends DOMIT_Node {
	/**
	* DOM Notation node constructor (NOT YET IMPLEMENTED!)
	*/
	function DOMIT_Notation() {		
		DOMIT_DOMException::raiseException(DOMIT_NOT_SUPPORTED_ERR, 
			 'Cannot instantiate DOMIT_Notation class. Notation nodes not yet supported.'); 
	} //DOMIT_Notation
} //DOMIT_Notation

/**
* Manages the generation of a DOMIT! document from SAX events
*
* @package domit-xmlparser
* @subpackage domit-xmlparser-main
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
	/** @var array An array of namespacesURIs mapped to prefixes */
	var $namespaceURIMap = array();	
	
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
			if ($this->xmlDoc->isNamespaceAware) {
				$parser = xml_parser_create_ns('');
			}
			else {
		    	$parser = xml_parser_create('');
			}
		}
		else {
			if ($this->xmlDoc->isNamespaceAware) {
				$parser = xml_parser_create_ns();
			}
			else {
		    	$parser = xml_parser_create();
			}
		}	
		
		//set handlers for SAX events
		xml_set_object($parser, $this); 
		xml_set_character_data_handler($parser, 'dataElement'); 
		xml_set_default_handler($parser, 'defaultDataElement'); 
		xml_set_notation_decl_handler($parser, 'notationElement'); 
		xml_set_processing_instruction_handler($parser, 'processingInstructionElement'); 
		xml_parser_set_option($parser, XML_OPTION_CASE_FOLDING, 0); 	
		xml_parser_set_option($parser, XML_OPTION_SKIP_WHITE, 1);
		
		if ($this->xmlDoc->isNamespaceAware) {
			xml_set_start_namespace_decl_handler($parser, 'startNamespaceDeclaration');
		    xml_set_end_namespace_decl_handler($parser, 'endNamespaceDeclaration');
			xml_set_element_handler($parser, 'startElementNS', 'endElement');
			$this->namespaceURIMap[DOMIT_XML_NAMESPACE] = 'xml';
		}
		else {
			xml_set_element_handler($parser, 'startElement', 'endElement');
		}
			
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
	function parseSAXY(&$myXMLDoc, $xmlText, $preserveCDATA = true, $definedEntities) {
		require_once(DOMIT_INCLUDE_PATH . 'xml_saxy_parser.php');
		
		$this->xmlDoc =& $myXMLDoc;
		$this->lastChild =& $this->xmlDoc;
		
		//create instance of SAXY parser 
		$parser =& new SAXY_Parser();
		$parser->appendEntityTranslationTable($definedEntities);
		
		if ($this->xmlDoc->isNamespaceAware) {
		    $parser->setNamespaceAwareness(true);
		    $parser->xml_set_start_namespace_decl_handler(array(&$this, 'startNamespaceDeclaration'));
		    $parser->xml_set_end_namespace_decl_handler(array(&$this, 'endNamespaceDeclaration'));
			$parser->xml_set_element_handler(array(&$this, 'startElementNS'), array(&$this, 'endElement'));
			$this->namespaceURIMap[DOMIT_XML_NAMESPACE] = 'xml';
		}
		else {
			$parser->xml_set_element_handler(array(&$this, 'startElement'), array(&$this, 'endElement'));	
		}
		
		$parser->xml_set_character_data_handler(array(&$this, 'dataElement'));
		$parser->xml_set_doctype_handler(array(&$this, 'doctypeElement'));
		$parser->xml_set_comment_handler(array(&$this, 'commentElement'));
		$parser->xml_set_processing_instruction_handler(array(&$this, 'processingInstructionElement')); 
		
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
		$this->lastChild->appendChild($currentNode);

		reset ($attrs);
			
		while (list($key, $value) = each ($attrs)) {
			$currentNode->setAttribute($key, $value);
		}
		
		$this->lastChild =& $currentNode;
	} //startElement	
	
	/**
	* Catches a start element event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The tag name of the current element
	* @param Array An array of the element attributes
	*/
	function startElementNS(&$parser, $name, $attrs) {
		$colonIndex = strrpos($name, ":");

		if ($colonIndex !== false) {
			//force to lower case because Expat for some reason forces to upper case
			$namespaceURI = strtolower(substr($name, 0, $colonIndex));
			$prefix = $this->namespaceURIMap[$namespaceURI];
				
			if ($prefix != '') {
				$qualifiedName = $prefix . ":" . substr($name, ($colonIndex + 1));
			}
			else {
				$qualifiedName = substr($name, ($colonIndex + 1));
			}					
		}
		else {
			$namespaceURI = '';
			$qualifiedName = $name;
		}
			
		$currentNode =& $this->xmlDoc->createElementNS($namespaceURI, $qualifiedName);	
		$this->lastChild->appendChild($currentNode);

		reset ($attrs);
			
		while (list($key, $value) = each ($attrs)) {
			$colonIndex = strrpos($key, ":");
			
			if ($colonIndex !== false) {
				//force to lower case because Expat for some reason forces to upper case
				$namespaceURI = strtolower(substr($key, 0, $colonIndex));
					
				if ($namespaceURI == 'xmlns') {
					//$qualifiedName = substr($key, ($colonIndex + 1));
					$qualifiedName = $key;
				}
				else {
					$qualifiedName = $this->namespaceURIMap[$namespaceURI] . 
						":" . substr($key, ($colonIndex + 1));;
				}				
			}
			else {
				$namespaceURI = '';
				$qualifiedName = $key;
			}

			$currentNode->setAttributeNS($namespaceURI, $qualifiedName, $value);
		}
		
		$this->lastChild =& $currentNode;	
	} //startElementNS

	
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
		if (!$this->inCDATASection) {
			$this->inTextNode = true;		
		}
		
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
		if ((strlen($data) > 2)  && ($this->parseItem == '')){
			$pre = strtoupper(substr($data, 0, 3));
			
			switch ($pre) {
				case '<?X': //xml declaration
					$this->processingInstructionElement($parser, 'xml', substr($data, 6, (strlen($data) - 6 - 2)));
					break;
				case '<!E': //dtd entity
					$this->xmlDoc->doctype .= "\n   " . $data;
					break;
				case '<![': //cdata section coming
					if ($this->preserveCDATA) {
						$this->inCDATASection = true;
					}
					break;	
				case '<!-': //comment
					$currentNode =& $this->commentElement($this, substr($data, 4, (strlen($data) - 7)));	
					break;
				case '<!D': //doctype
					$this->parseItem = 'doctype';
					$this->parseContainer = $data;
					break;
				case ']]>': //cdata end tag
					$this->inCDATASection = false;
					$currentNode =& $this->xmlDoc->createCDataSection($this->parseContainer);
					$this->lastChild->appendChild($currentNode);
					$this->parseContainer = '';
					break;
			}
		}
		else {
			switch ($this->parseItem) {
				case 'doctype':
					$this->parseContainer .= $data;
					
					if ($data == '>') {
						$this->doctypeElement($parser, $this->parseContainer);
						$this->parseContainer = '';
						$this->parseItem = '';
					}
					else if ($data == '[') {
						$this->parseItem = 'doctype_inline';
					}
					break;
					
				case 'doctype_inline':
					$this->parseContainer .= $data;
					
					if ($data == ']') {
						$this->parseItem = 'doctype';
					}	
					else if ($data{(strlen($data) - 1)} == '>') {
						$this->parseContainer .= "\n   ";
					}
					break;
			}
		}
	} //defaultDataElement
	
	/**
	* Catches a doctype event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The current data
	*/
	function doctypeElement(&$parser, $data) {
		$start = strpos($data, '<!DOCTYPE');
		$name = trim(substr($data, $start));
		$end = strpos($name, ' ');
		$name = substr($name, 0, $end);		
		
		$currentNode =& new DOMIT_DocumentType($name, $data);
		$currentNode->ownerDocument =& $this->xmlDoc;
		
		$this->lastChild->appendChild($currentNode);
		$this->xmlDoc->doctype =& $currentNode;
	} //doctypeElement
	
	/**
	* Catches a notation node event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The current notation data
	*/
	function notationElement(&$parser, $data) {
		//add to doctype string
		if (($this->parseItem == 'doctype_inline')  || ($this->parseItem == 'doctype')) {
			$this->parseContainer .= $data;
		}
	} //notationElement
	
	/**
	* Catches a comment node event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The comment data
	*/
	function commentElement(&$parser, $data) {
		if ($this->inTextNode) {
			$this->dumpTextNode();
		}
		
		$currentNode =& $this->xmlDoc->createComment($data);
		$this->lastChild->appendChild($currentNode);
	} //commentElement	
	
	/**
	* Catches a processing instruction node event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The target of the processing instruction data
	* @param string The processing instruction data
	*/
	function processingInstructionElement(&$parser, $target, $data) {	
		if ($this->inTextNode) {
			$this->dumpTextNode();
		}
		
		$currentNode =& $this->xmlDoc->createProcessingInstruction($target, $data);
		$this->lastChild->appendChild($currentNode);
		
		if (strtolower($target) == 'xml') {
			$this->xmlDoc->xmldeclaration =& $currentNode;
		}
	} //processingInstructionElement
	
	/**
	* Catches a start namespace declaration event and processes the data
	* @param Object A reference to the current SAX parser
	* @param string The namespace prefix
	* @param string The namespace uri
	*/
	function startNamespaceDeclaration(&$parser, $prefix, $uri) {
		//make uri lower case because Expat forces it to upper case for some reason
		$this->namespaceURIMap[strtolower($uri)] = $prefix;
	} //startNamespaceDeclaration
	
	/**
	* Catches an end namespace declaration event
	* @param Object A reference to the current SAX parser
	* @param string The namespace prefix
	*/
	function endNamespaceDeclaration(&$parser, $prefix) {
		//do nothing; could remove from map, but would hardly be optimal
	} //endNamespaceDeclaration
	
} //DOMIT_Parser

?>