<?php
class StateRotation {
	var $xmlDoc;
	
	function StateRotation() {
		require_once("xml_domit_parser.php");
		$this->xmlDoc =& new DOMIT_Document();
	} //StateRotation
	
	function fromFile($filename) {
		return $this->xmlDoc->loadXML($filename);
	} //fromFile
	
	function fromString($string) {
		return $this->xmlDoc->parseXML($string);
	} //fromString
	
	function toFile($filename) {
		return $this->xmlDoc->saveXML($filename);
	} //toFIle
	
	function count() {
		return count($this->xmlDoc->documentElement->childNodes);
	} //count
	
	function &getStateList() {
		$total = $this->count();
		$states = array();
		
		for ($i = 0; $i < $total; $i++) {
			$currRotation =& $this->xmlDoc->documentElement->childNodes[$i];
			$states[] = $currRotation->childNodes[0]->firstChild->nodeValue;
		}
		
		return $states;
	} //getStateList
	
	function getBanner($state) {
		$total = $this->count();
		
		for ($i = 0; $i < $total; $i++) {
			$currRotation =& $this->xmlDoc->documentElement->childNodes[$i];
			
			if ($currRotation->childNodes[0]->firstChild->nodeValue == $state) {
				return $currRotation->childNodes[1]->firstChild->nodeValue;
			}
		}
		
		return "";
	} //getBanner
	
	function setBanner($state, $num) {
		$total = $this->count();
		
		for ($i = 0; $i < $total; $i++) {
			$currRotation =& $this->xmlDoc->documentElement->childNodes[$i];
			
			if ($currRotation->childNodes[0]->firstChild->nodeValue == $state) {
				$currRotation->childNodes[1]->firstChild->nodeValue = $num;
				break;
			}
		}
	} //setBanner
	
	function incrementBanner($state) {
		$total = $this->count();
		
		for ($i = 0; $i < $total; $i++) {
			$currRotation =& $this->xmlDoc->documentElement->childNodes[$i];
			
			if ($currRotation->childNodes[0]->firstChild->nodeValue == $state) {
				$currNum = intval($currRotation->childNodes[1]->firstChild->nodeValue);
				$currNum++;
				$currRotation->childNodes[1]->firstChild->nodeValue = ("" . $currNum);
				break;
			}
		}
	} //incrementBanner
	
} //StateRotation

?>