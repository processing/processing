<?php
/**
* DOMIT! Doctor is a set of utilities for repairing malformed XML
* @package domit-xmlparser
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/domit/ DOMIT! Home Page
* DOMIT! is Free Software
**/

/**
* A (static) class containing utilities for repairing malformed XML
*
* @package domit-xmlparser
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class domit_doctor {
	
	/**
	* Looks for illegal ampersands and converts them to entities
	* @param string The xml text to be repaired
	* @return string The repaired xml text
	*/
	function fixAmpersands($xmlText) {
		$xmlText = trim($xmlText);
		$startIndex = -1;
		$processing = true;
		$illegalChar = '&';
				
		while ($processing) {
			$startIndex = strpos($xmlText, $illegalChar, ($startIndex + 1));
			
			if ($startIndex !== false) {
				$xmlText = domit_doctor::evaluateCharacter($xmlText, 
									$illegalChar, ($startIndex + 1));
			}
			else {
				$processing = false;
			}
		}
		
		return $xmlText;
	} //fixAmpersands
	
	/**
	* Evaluates whether an ampersand should be converted to an entity, and performs the conversion
	* @param string The xml text
	* @param string The (ampersand) character
	* @param int The character index immediately following the ampersand in question
	* @return string The repaired xml text
	*/
	function evaluateCharacter($xmlText, $illegalChar, $startIndex) {
		$total = strlen($xmlText);
		$searchingForCDATASection = false; 
		
		for ($i = $startIndex; $i < $total; $i++) {
			$currChar = substr($xmlText, $i, 1);
			
			if (!$searchingForCDATASection) {
				switch ($currChar) {
					case ' ':
					case "'":
					case '"':
					case "\n":
					case "\r":
					case "\t":
					case '&':
					case "]":
						$searchingForCDATASection = true;
						break;
					case ";":
						return $xmlText;
						break;			
				}
			}
			else {
				switch ($currChar) {
					case '<':
					case '>':
						return (substr_replace($xmlText, '&amp;', 
										($startIndex - 1) , 1));
						break;
					case "]":
						return $xmlText;
						break;			
				}
			}
		}
		
		return $xmlText;
	} //evaluateCharacter
} //domit_doctor
?>
