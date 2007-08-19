<?php
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
    </style>
  </head>
  <body>
    <div id="header">
      <img src="images/mobile.gif" width="57" height="20" alt="Mobile" /><br />
      <?php echo $PAGE_HEADER ?><br />
    </div>
    <div id="content">
