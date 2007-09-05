<?php

require_once 'settings.inc.php';
require_once $WEB_ROOT .'/lib/tera-wurfl/tera_wurfl.php';

$wurfl = new tera_wurfl();
if (!$wurfl->getDeviceCapabilitiesFromAgent($_SERVER['HTTP_USER_AGENT'])) {
    $wurfl->capabilities['display']['resolution_width'] = 176;
}

function get_sized_image($url) {
    global $wurfl;

    $width = $wurfl->capabilities['display']['resolution_width'];
    if ($width <= 128) {
        $width = 128;
    } else if ($width <= 176) {
        $width = 176;
    } else {
        $width = 240;
    }

    $ext = strrchr($url, '.');
    $base = substr($url, 0, strlen($url) - strlen($ext));
    
    return $base .'_'. $width . $ext;
}

header("Vary: Accept");
if (stristr($_SERVER[HTTP_ACCEPT], "application/vnd.wap.xhtml+xml")) {
    header("Content-Type: application/vnd.wap.xhtml+xml");
} else if (stristr($_SERVER[HTTP_ACCEPT], "application/xhtml+xml")) {
    header("Content-Type: application/xhtml+xml");
} else {
    header("Content-Type: text/html");
}
echo '<?xml version="1.0" encoding="UTF-8"?>';
echo "\n";
?>
<!DOCTYPE html PUBLIC "-//WAPFORUM//DTD XHTML Mobile 1.0//EN" "http://www.wapforum.org/DTD/xhtml-mobile10.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title><?php echo $PAGE_TITLE ?></title>
    <style type="text/css"> 
body {
    font-family: sans-serif;
	background: #fff; 
	color: #553; 
    margin-top: 2px;
    margin-left: 2px;
}

a {
    color: #d60;
}

#header {
    margin-bottom: 8px;
    font-size: smaller;
}

#footer {
    margin-top: 12px;
    font-size: smaller;
}

#attribution {
    margin-top: 8px;
    font-size: smaller;
}

.accesskey {
    background: #d60;
    color: #fff;
}

.smaller {
    font-size: smaller;
}
    </style>
  </head>
  <body>
    <div id="header">
      <img src="images/mobile.gif" width="57" height="20" alt="Mobile" /><br />
      <?php echo $PAGE_HEADER ?><br />
    </div>
    <div id="content">
