<?php

class Timer {
	var $startTime;
	var $stopTime;
	
	function start() {
		$this->startTime = microtime();
	} //start
	
	function stop() {
		$this->stopTime = microtime();
	} //stop
	
	function getTime() {
		return $this->elapsed($this->startTime, $this->stopTime);
	} //getTime
	
	function elapsed($a, $b) {
		list($a_micro, $a_int) = explode(' ',$a);
		list($b_micro, $b_int) = explode(' ',$b);
		
		if ($a_int > $b_int) {
			return ($a_int - $b_int) + ($a_micro - $b_micro);
		} 
		else if ($a_int == $b_int) {
			if ($a_micro > $b_micro) {
				return ($a_int - $b_int) + ($a_micro - $b_micro);
			} 
			else if ($a_micro<$b_micro) {
				return ($b_int - $a_int) + ($b_micro - $a_micro);
			} 
			else {
				return 0;
			 }
		} 
		else { // $a_int < $b_int
			return ($b_int - $a_int) + ($b_micro - $a_micro);
		}
	} //elapsed
} //Timer

?>