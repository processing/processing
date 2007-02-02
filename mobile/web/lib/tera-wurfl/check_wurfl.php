<form action="check_wurfl.php" method="GET">
Mobile device user agent:<br />
<input type="text" name="force_ua" size="100" value="<?=$_GET['force_ua']?>">
</form>
Try some of these:
<ul>
  <li><pre>SonyEricssonK700i/R2AC SEMC-Browser/4.0.2 Profile/MIDP-2.0 Configuration/CLDC-1.1</pre></li>
  <li><pre>MOT-T720/S_G_05.30.0CR MIB/2.0 Profile/MIDP-1.0 Configuration/CLDC-1.0</pre></li>
  <li><pre>SAGEM-myX5-2/1.0 Profile/MIDP-2.0 Configuration/CLDC-1.0 UP.Browser/6.2.2.6.d.2 (GUI) MMP/1.0</pre></li>
  <li><pre>NokiaN90-1/3.0541.5.2 Series60/2.8 Profile/MIDP-2.0 Configuration/CLDC-1.1</pre></li>
</ul>
<hr size="1" />
<?php
/*
 * $Id: check_wurfl.php,v 1.1.2.1.2.2 2006/10/28 21:45:20 kamermans Exp $
 * $RCSfile: check_wurfl.php,v $ v2.1 beta2 (Apr, 16 2005)
 *
 * Author: Andrea Trasatti ( atrasatti AT users DOT sourceforge DOT net )
 * Modified by Steve Kamerman for testing in Tera-WURFL
 *
 */

set_time_limit(600);

list($usec, $sec) = explode(" ", microtime());
$start = ((float)$usec + (float)$sec); 

require_once('./tera_wurfl_config.php');
require_once(WURFL_CLASS_FILE);

list($usec, $sec) = explode(" ", microtime());
$load_class = ((float)$usec + (float)$sec); 

$wurflObj = new tera_wurfl();

list($usec, $sec) = explode(" ", microtime());
$init_class = ((float)$usec + (float)$sec); 

if ( isset($_GET['force_ua']) && strlen($_GET['force_ua'])>0 ) {
	$wurflObj->GetDeviceCapabilitiesFromAgent($_GET['force_ua']);
} else {
	//Forcing a test agent
	$wurflObj->GetDeviceCapabilitiesFromAgent("MOT-c350");
}

list($usec, $sec) = explode(" ", microtime());
$end = ((float)$usec + (float)$sec); 

echo "Time to load tera_wurfl_class.php:".($load_class-$start)."<br>\n";
echo "Time to initialize class:".($init_class-$load_class)."<br>\n";
echo "Time to find the user agent:".($end-$init_class)."<br>\n";
echo "Total:".($end-$start)."<br>\n";

if($wurflObj->device_image != ""){
	echo '<img src="'.$wurflObj->device_image.'" /><br />';
}else{
	echo "<strong>No device image available.</strong><br />";
}

echo "<pre>";
var_export($wurflObj->capabilities);
echo "</pre>";

?>
<form action="check_wurfl.php" method="GET">
Mobile device user agent:<input type="text" name="force_ua" size="100">
</form>
