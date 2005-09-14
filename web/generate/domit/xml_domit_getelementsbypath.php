<?php
/**
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

/** Separator for elements path */
define('GET_ELEMENTS_BY_PATH_SEPARATOR', '/');
/** Constant for an absolute path search (starting at the document root) */
define('GET_ELEMENTS_BY_PATH_SEARCH_ABSOLUTE', 0);
/** Constant for a relative path search (starting at the level of the calling node) */
define('GET_ELEMENTS_BY_PATH_SEARCH_RELATIVE', 1);
/** Constant for a variable path search (finds all matches, regardless of place in the hierarchy) */
define('GET_ELEMENTS_BY_PATH_SEARCH_VARIABLE', 2);

/**
* getElementsByPath is a simple utility for path-based access to nodes in a DOMIT! document.
*/
class DOMIT_GetElementsByPath {
    /** @var Object The node from which the search is called */
	var $callingNode;
	/** @var int The type of search to be performed, i.e., relative, absolute, or variable */
	var $searchType;
	/** @var Object The node that is the current parent of the search */
	var $contextNode;
	/** @var array An array containing a series of path segments for which to search */
	var $arPathSegments = array();
	/** @var Object A DOMIT_NodeList of matching nodes */
	var $nodeList;
	/** @var Object The index of the current node of the search */
	var $targetIndex;
	/** @var Object if true, the search will be aborted once the first match is found */
	var $abortSearch = false;
	
	/**
	* Constructor - creates an empty DOMIT_NodeList to store matching nodes
	*/
	function DOMIT_GetElementsByPath() {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_nodemaps.php');
		$this->nodeList =& new DOMIT_NodeList();
	} //DOMIT_GetElementsByPath
	
	/**
	* Parses the supplied "path"-based pattern
	* @param Object The node from which the search is called
	* @param string The pattern
	* @param int The node level of the current search
	* @return Object The NodeList containing matching nodes
	*/
	function &parsePattern(&$node, $pattern, $nodeIndex = 0) {
		$this->callingNode =& $node;		 
		$pattern = trim($pattern);	
		
		$this->determineSearchType($pattern);
		$this->setContextNode();
		$this->splitPattern($pattern);
	
		$this->targetIndex = $nodeIndex;
		$totalSegments = count($this->arPathSegments);		
		
		if ($totalSegments > 0) {
			if ($this->searchType == GET_ELEMENTS_BY_PATH_SEARCH_VARIABLE) {
				$arContextNodes =& $this->contextNode->ownerDocument->getElementsByTagName($this->arPathSegments[0]);
				$totalContextNodes = $arContextNodes->getLength();
				
				for ($i = 0; $i < $totalContextNodes; $i++) {
					$this->selectNamedChild($arContextNodes->item($i), 1);
				}
			}
			else {
				if ($this->searchType == GET_ELEMENTS_BY_PATH_SEARCH_ABSOLUTE) {
					if ($this->contextNode->nodeName == $this->arPathSegments[0]) {
						if (count($this->arPathSegments) == 1) {
							$this->nodeList->appendNode($this->contextNode);
						}
						else {
							$this->selectNamedChild($this->contextNode, 1);	
						}
					}
				}
				else if ($this->searchType == GET_ELEMENTS_BY_PATH_SEARCH_RELATIVE) {
					$this->selectNamedChild($this->contextNode, 0);	
				}
			}
		}		
	
		if ($nodeIndex > 0) {
			if ($nodeIndex <= $this->nodeList->getLength()) {
				return $this->nodeList->item(($nodeIndex - 1));
			}
			else {
				return null;
			}
		}
		
		return $this->nodeList;
	} //parsePattern
	
	/**
	* Determines the type of search to be performed: absolute, relative, or variable
	* @param string The pattern
	*/
	function determineSearchType($pattern) {
		$firstChar = $pattern{0};
		
		if ($firstChar != GET_ELEMENTS_BY_PATH_SEPARATOR) {
			//relative path
			$this->searchType = GET_ELEMENTS_BY_PATH_SEARCH_RELATIVE;
		}
		else {
			$secondChar = $pattern{1};
				
			if ($secondChar != GET_ELEMENTS_BY_PATH_SEPARATOR) {
				//absolute path
				$this->searchType = GET_ELEMENTS_BY_PATH_SEARCH_ABSOLUTE;
			}
			else {
				//variable path
				$this->searchType = GET_ELEMENTS_BY_PATH_SEARCH_VARIABLE;				
			}
		}
	} //determineSearchType
	
	
	/**
	* Sets the context node, i.e., the node from which the search begins
	*/
	function setContextNode() {
		switch($this->searchType) {
			case GET_ELEMENTS_BY_PATH_SEARCH_ABSOLUTE:
				$this->contextNode =& $this->callingNode->ownerDocument->documentElement;
				break;
				
			case GET_ELEMENTS_BY_PATH_SEARCH_RELATIVE:
				if ($this->callingNode->uid != $this->callingNode->ownerDocument->uid) {
					$this->contextNode =& $this->callingNode;
				}
				else {
					$this->contextNode =& $this->callingNode->ownerDocument->documentElement;
				}
				break;

			case GET_ELEMENTS_BY_PATH_SEARCH_VARIABLE:
				$this->contextNode =& $this->callingNode->ownerDocument->documentElement;
				break;
		}
	} //setContextNode
	
	/**
	* Splits the supplied pattern into searchable segments
	* @param string The pattern
	*/
	function splitPattern($pattern) {
		switch($this->searchType) {
			case GET_ELEMENTS_BY_PATH_SEARCH_ABSOLUTE:
				$this->arPathSegments = explode(GET_ELEMENTS_BY_PATH_SEPARATOR, substr($pattern, 1));
				break;
				
			case GET_ELEMENTS_BY_PATH_SEARCH_RELATIVE:
				$this->arPathSegments = explode(GET_ELEMENTS_BY_PATH_SEPARATOR, substr($pattern, 0));
				break;

			case GET_ELEMENTS_BY_PATH_SEARCH_VARIABLE:
				$this->arPathSegments = explode(GET_ELEMENTS_BY_PATH_SEPARATOR, substr($pattern, 2));
				break;
		}
	} //splitPattern
	
	/**
	* Matches the current path segment against the child nodes of the current context node
	* @param Object The context node
	* @param int The index in the arPathSegments array of the current path segment
	*/
	function selectNamedChild(&$node, $pIndex) {	
		if (!$this->abortSearch) {
			if ($pIndex < count($this->arPathSegments)) { //not at last path segment
				$name = $this->arPathSegments[$pIndex];
				$numChildren = $node->childCount;
			
				for ($i = 0; $i < $numChildren; $i++) {
					$currentChild =& $node->childNodes[$i];
		
					if ($currentChild->nodeName == $name) {
						$this->selectNamedChild($currentChild, ($pIndex + 1));
					}
				}
			}
			else {
				$this->nodeList->appendNode($node);
				
				if ($this->targetIndex == $this->nodeList->getLength()) {
					$this->abortSearch = true;
				}
			}
		}
	} //selectNamedChild
} //DOMIT_GetElementsByPath



/**
* getElementsByAttributePath is a temporary utility requested by a DOMIT! user for path-based access to attributes in a DOMIT! document.
* This class may be absent from future versions of DOMIT!
*/
class DOMIT_GetElementsByAttributePath {
    /** @var Object A DOMIT_NodeList of matching attribute nodes */
	var $nodeList;

    /**
	* Constructor - creates an empty DOMIT_NodeList to store matching nodes
	*/
	function DOMIT_GetElementsByAttributePath() {
		require_once(DOMIT_INCLUDE_PATH . 'xml_domit_nodemaps.php');
		$this->nodeList =& new DOMIT_NodeList();
	} //DOMIT_GetElementsByAttributePath
	
	/**
	* Matches the current path segment against the child nodes of the current context node
	* @param Object The context node
	* @param string The pattern
	* @param int The index of the current path segment
	*/
	function &parsePattern(&$node, $pattern, $nodeIndex = 0) {
		$beginSquareBrackets = strpos($pattern, '[');
		
		if ($beginSquareBrackets != 0) {
			$path = substr($pattern, 0, $beginSquareBrackets);
			
			$attrPattern =  substr($pattern, (strpos($pattern, '@') + 1));
			$attrPattern = substr($attrPattern, 0, strpos($attrPattern, ')'));

			$commaIndex = strpos($attrPattern, ',');
			$key = trim(substr($attrPattern, 0, $commaIndex));
			$value = trim(substr($attrPattern, ($commaIndex + 1)));
			$value = substr($value, 1, (strlen($value) - 2));

			$gebp = new DOMIT_GetElementsByPath();
			$myResponse =& $gebp->parsePattern($node, $path);


			$total = $myResponse->getLength();
			for ($i = 0; $i < $total; $i++) {
				$currNode =& $myResponse->item($i);
				
				if ($currNode->hasAttribute($key)) {
					if ($currNode->getAttribute($key) == $value) {
						$this->nodeList->appendNode($currNode);
					}
				}
			}	
		}

		if ($nodeIndex == 0) {
			return $this->nodeList;
		}
		else {
			if ($nodeIndex <= $this->nodeList->getLength()) {
				return $this->nodeList->item(($nodeIndex - 1));
			}
			else {
				$this->nodeList = new DOMIT_NodeList();
				return $this->nodeList;
			}
		}
	} //parsePattern
} //DOMIT_GetElementsByAttributePath
?>
