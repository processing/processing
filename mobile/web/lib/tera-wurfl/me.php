<html>
<head>
<title>Me</title>
</head>
<body>
<?php
require_once('./tera_wurfl_config.php');
require_once(WURFL_CLASS_FILE);

$wurflObj = new tera_wurfl();
$wurflObj->GetDeviceCapabilitiesFromAgent($_SERVER['HTTP_USER_AGENT']);
if($wurflObj->device_image != ""){
	echo '<img src="'.$wurflObj->device_image.'" />';
}else{
	echo "<strong>No device image available.</strong>";
}
?>
</body>
</html>
