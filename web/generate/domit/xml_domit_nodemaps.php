<?php
/**
* DOMIT node maps are structures for storing and accessing collections of DOMIT_Nodes.
* @package domit-xmlparser
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

if (!defined('DOMIT_INCLUDE_PATH')) {
	define('DOMIT_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

/**
* A DOM NodeList implementation
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_NodeList {
	/** @var Array A container for the nodes in the list */
	var $arNodeList = array();
	
	/**
	* Return the node at the specified index
	* @param int The index of the requested node
	* @return Object A reference to the requested node, or null
	*/
	function &item($index) {
		if ($index < $this->getLength()) {
			return $this->arNodeList[$index];
		}
		return null;
	} //item
	
	/**
	* Returns the number of nodes in the list
	* @return int The number of nodes in the list
	*/
	function getLength() {
		return count($this->arNodeList);
	} //getLength
	
	/**
	* Appends a node to the list
	* @return Object The appended node
	*/
	function &appendNode(&$node) {
		$this->arNodeList[] =& $node;
		return $node;
	} //appendNode
	
	/**
	* Removes the specified node from the list
	* @param Object A reference to the node to be removed
	* @return Object A reference to the removed node
	*/
	function &removeNode(&$node) {
		$total = $this->getLength();
		$returnNode = null;
		$found = false;
		
		for ($i = 0; $i < $total; $i++) {
			if (!$found) {
				if ($node->uid == $this->arNodeList[$i]->uid) {
					$found = true;
					$returnNode=& $node;
				}
			}
			
			if ($found) {
				if ($i == ($total - 1)) {
					unset($this->arNodeList[$i]);
				}
				else {
					$this->arNodeList[$i] =& $this->arNodeList[($i + 1)];
				}
			}			
		}
		
		return $returnNode;
	} //$removeNode
	
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
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		return $this->arNodeList;
	} //toArray
	
	/**
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &createClone($deep = false) {
		$className = get_class($this);
		$clone =& new $className();
		
		foreach ($this->arNodeList as $key => $value) {
			$currNode =& $this->arNodeList[$key];
			$clone->arNodeList[$key] =& $currNode->cloneNode($deep);
		}
		
		return $clone;
	} //createClone
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		$result = '';
		
		foreach ($this->arNodeList as $key => $value) {
			$currNode =& $this->arNodeList[$key];
			$result .= $currNode->toString(false, $subEntities);
		}
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toString
} //DOMIT_NodeList

/**
* A DOM NamedNodeMap implementation
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_NamedNodeMap {
	/** @var Array A container for the nodes in the map */
	var $arNodeMap = array();
	/** @var Array A numerical index to the keys of the mapped nodes */
	var $indexedNodeMap = array();
	/** @var boolean True if the list has been modified and $indexedNodeMap needs reindexing */
	var $isDirty = true;
	
	/**
	* Gets a node with the specifed name
	* @param string The name of the node
	* @return mixed A reference to the requested node, or null
	*/
	function &getNamedItem($name) {
		if (isset($this->arNodeMap[$name])) {
			return $this->arNodeMap[$name];
		}
		
		return null;
	} //getNamedItem
	
	/**
	* Reindexes the numerical index for the named node map
	*/
	function reindexNodeMap() {
	    $this->indexedNodeMap = array();

	    foreach ($this->arNodeMap as $key => $value) {
	        $this->indexedNodeMap[] = $key;
	    }
	    
	    $this->isDirty = false;
	} //reindexNodeMap
	
	/**
	* Assigns a node to the list 
	* @param Object A reference to the node to be assigned
	* @return Object A reference to the assigned node
	*/
	function &setNamedItem(&$arg) {
		$returnNode = null;
		
		if (isset($this->arNodeMap[$arg->nodeName])) {
			$returnNode =& $this->arNodeMap[$arg->nodeName];
		}
		else {
		    $this->isDirty = true;
		}
		
		$this->arNodeMap[$arg->nodeName] =& $arg;		
		return $returnNode;
	} //setNamedItem
	
	/**
	* Removes a node from the list, by name
	* @param string The name of the node to be removed
	* @return mixed A reference to the removed node, or null
	*/
	function &removeNamedItem($name) {
		$returnNode = null;
		
		if (isset($this->arNodeMap[$name])) {
			$returnNode =& $this->arNodeMap[$name];
			unset($this->arNodeMap[$name]);
			$this->isDirty = true;
		}
		
		return $returnNode;
	} //removeNamedItem
	
	/**
	* Gets a node with the specifed name, taking into account namespaces
	* @param string The namespaceURI of the node
	* @param string The localName of the node
	* @return mixed A reference to the requested node, or null
	*/
	function &getNamedItemNS($namespaceURI, $localName) {
	    $key = $this->getKeyNS($namespaceURI, $localName);

		if (isset($this->arNodeMap[$key])) {
			return $this->arNodeMap[$key];
		}

		return null;
	} //getNamedItemNS
	
	/**
	* Assigns a node to the list, using its namespaceURI and localName
	* @param Object A reference to the node to be assigned
	* @return Object A reference to the assigned node
	*/
	function &setNamedItemNS(&$arg) {
		$returnNode = null;
		$key = $this->getKeyNS($arg->namespaceURI, $arg->localName);

		if (isset($this->arNodeMap[$key])) {
			$returnNode =& $this->arNodeMap[$key];
		}
		else {
		    $this->isDirty = true;
		}

		$this->arNodeMap[$key] =& $arg;
		return $returnNode;
	} //setNamedItemNS
	
	/**
	* Removes a node from the list, by name, by local name and namespace URI
	* @param string The namespaceURI of the node to be removed
	* @param string The localName of the node to be removed
	* @return mixed A reference to the removed node, or null
	*/
	function &removeNamedItemNS($namespaceURI, $localName) {
		$returnNode = null;
		$key = $this->getKeyNS($namespaceURI, $localName);

		if (isset($this->arNodeMap[$key])) {
			$returnNode =& $this->arNodeMap[$key];
			unset($this->arNodeMap[$key]);
			$this->isDirty = true;
		}

		return $returnNode;
	} //removeNamedItemNS
	
	/**
	* Returns the key of the NamedNodeMap, given the namespaceURI and localName
	* @param string The namespaceURI of the node to be removed
	* @param string The localName of the node to be removed
	* @return string The key of the NamedNodeMap
	*/
	function getKeyNS($namespaceURI, $localName) {
	    if ($namespaceURI != '') {
	    	return $namespaceURI . ":" . $localName;
	    }

		return $localName;
	} //getKeyNS
	
	/**
	* Return the node at the specified index
	* @param int The index of the requested node
	* @return mixed A reference to the requested node, or null
	*/
	function &item($index) {
  		if ($this->isDirty) $this->reindexNodeMap();
  		return $this->arNodeMap[$this->indexedNodeMap[$index]];
	} //item
	
	/**
	* Returns the number of nodes in the map
	* @return int The number of nodes in the map
	*/
	function getLength() {
		return count($this->arNodeMap);
	} //getLength
	
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
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		return $this->arNodeMap;
	} //toArray
	
	/**
	* Copies a node and/or its children 
	* @param boolean True if all child nodes are also to be cloned
	* @return Object A copy of the node and/or its children
	*/
	function &createClone($deep = false) {
		$className = get_class($this);
		$clone =& new $className();
		
		foreach ($this->arNodeMap as $key => $value) {
			$currNode =& $this->arNodeMap[$key];
			$clone->arNodeMap[$key] =& $currNode->cloneNode($deep);
		}
		
		return $clone;
	} //createClone
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		$result = '';
		
		foreach ($this->arNodeMap as $key => $value) {
			$currNode =& $this->arNodeMap[$key];
			$result .= $currNode->toString(false, $subEntities);
		}
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toString
} //DOMIT_NamedNodeMap

/**
* A NamedNodeMap with specialized funtionality for Attribute nodes
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class DOMIT_NamedNodeMap_Attr extends DOMIT_NamedNodeMap {
	/**
	* Generates an array representation of the node and its children
	* @return Array A representation of the node and its children 
	*/
	function toArray() {
		$arReturn = array();
		
		foreach ($this->arNodeMap as $key => $value) {
			$arReturn[$key] = $this->arNodeMap[$key]->getValue(); 
		}
		
		return $arReturn;
	} //toArray
	
	/**
	* Generates a string representation of the node and its children
	* @param boolean True if HTML readable output is desired
	* @param boolean True if illegal xml characters in text nodes and attributes should be converted to entities
	* @return string The string representation  
	*/
	function toString($htmlSafe = false, $subEntities=false) {
		$result = '';

		foreach ($this->arNodeMap as $key => $value) {
			$currNode =& $this->arNodeMap[$key];
			$result .= $currNode->toString(false, $subEntities);
		}
		
		if ($htmlSafe) $result = $this->forHTML($result);
		
		return $result;
	} //toString
} //DOMIT_NamedNodeMap_Attr

?>
