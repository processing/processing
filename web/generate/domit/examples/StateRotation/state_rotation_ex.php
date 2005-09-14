<?php
	require_once("state_rotation.php");
	$sr =& new StateRotation();
	
	if ($sr->fromFile("statecount.xml")) {
		echo("The current banner value for 'ca' is: " . $sr->getBanner("ca"));
		
		$sr->incrementBanner("ca");
		
		echo("<br /><br />The new banner value for 'ca' is: "  . $sr->getBanner("ca"));
		
		$sr->toFile("statecount.xml");
	}	
	else {
		echo ("Invalid xml file");
	}
?>