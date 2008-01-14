<?php

//// load offline functions
require 'offline.inc.php';

//// check if we're on the web, or being run on the command line to 
//// generate offline documentation
$offline = false;
$ext = "php";
if (is_null($argv)) {
    require 'settings.inc.php';
} else {
    //// define the site root, passed in as a relative path on the command line
    define("SITE_ROOT", $argv[1]);
    //// get working directory
    $dir = str_replace("\\", "/", getcwd());
    //// reconstruct PHP_SELF
    $count = substr_count($argv[1], "../");
    $root = $dir;
    for ($i = 0; $i < $count; $i++) {
        $root = substr($root, 0, strrpos($root, "/"));
    }
    $dir = substr($dir, strlen($root) + 1);
    $_SERVER['PHP_SELF'] = SITE_ROOT . $dir ."/". $argv[0];
    //// set name parameter, if exits
    if (isset($argv[3])) {      
        $_GET['name'] = $argv[3];
    }
    /*
    echo "count=". $count ."<br />";
    echo "root=". $root .", dir=". $dir ."<br />";
    echo "PHP_SELF=". $_SERVER['PHP_SELF'] ."<br />";
    echo var_dump($argv);
    */

    $offline = true;
    $ext = "html";
}

?>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
                      "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<?php if (isset($PAGE_HEAD)) { echo $PAGE_HEAD; } ?>
<link rel="shortcut icon" href="http://mobile.processing.org/favicon.ico" type="image/x-icon" />
<title><?php echo $PAGE_TITLE ?></title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<script language="javascript">
<!--

function MM_openBrWindow(theURL,winName,features) { //v2.0
  window.open(theURL,winName,features);
}
//-->
</script>
<link rel="stylesheet" href="<?php echo SITE_ROOT?>css/mobile.css" type="text/css">
</head>
<body>
<div id="head"> 
    <img src="<?php echo SITE_ROOT?>images/mobile.png">
</div>
<div id="navigation"> 
    <img src="<?php echo SITE_ROOT?>images/nav_bottomarrow.png" align="absmiddle">
<?php if (!$offline) { ?>
<?php if ($_SERVER['PHP_SELF'] == SITE_ROOT . 'index.php') { ?>
    Cover
<?php } else { ?>
    <a href="<?php echo SITE_ROOT ?>index.<?php echo $ext ?>">Cover</a>
<?php } ?>

    <span class="backslash">\</span>
<?php     if ($PAGE_LINKHEADER || (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'exhibition/') === false)) { ?>
    <a href="<?php echo SITE_ROOT ?>exhibition/index.php">Exhibition</a>
<?php     } else { ?>
    Exhibition
<?php     } ?>

    <span class="backslash">\</span>
<?php     if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'learning/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>learning/index.php">Learning</a>
<?php     } else { ?>
    Learning
<?php     } ?>
    <span class="backslash">\</span>
<?php } ?>
<?php if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'reference/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>reference/index.<?php echo $ext ?>">Reference</a> 
<?php } else { ?>
    Reference
<?php } ?>
<?php if (!$offline) { ?>
    <span class="backslash">\</span>
<?php     if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'download/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>download/index.php">Download</a>
<?php     } else { ?>
    Download
<?php     } ?>
    <span class="backslash">\</span>
<?php     if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'phones/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>phones/index.php">Phones</a>
<?php     } else { ?>
    Phones
<?php     } ?>
    <span class="backslash">\</span>
<?php     if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'faq/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>faq/index.<?php echo $ext ?>">FAQ</a>
<?php     } else { ?>
    FAQ
<?php     } ?>
    <span class="backslash">\</span>
<?php     if (strstr($_SERVER['PHP_SELF'], SITE_ROOT . 'discourse/') === false) { ?>
    <a href="<?php echo SITE_ROOT ?>discourse/index.php">Discourse</a>
<?php     } else { ?>
    Discourse
<?php     } ?>
    <span class="backslash">\</span>
    <a href="<?php echo SITE_ROOT ?>wiki/index.php">Wiki</a>
<?php } ?>
</div>
<?php 
if (!(@include 'subnavigation.inc.php')) {
    if (!(@include '../subnavigation.inc.php')) {
        @include '../../subnavigation.inc.php';
    }
}
?>
<?php if ($PAGE_SHOWBACKINDEX) { 
    if (strstr($_SERVER['PHP_SELF'], 'reference.php') !== false) {
        $pos = strpos($_GET['name'], "_");
        if ($pos !== false) {
            $parent = substr($_GET['name'], 0, $pos);
            if (file_exists('API/'. $parent .'.xml')) {
                if ($offline) {
                    $PAGE_BACK_LINK = $parent .".html";
                } else {
                    $PAGE_BACK_LINK = 'reference.php?name='. $parent;
                }
                $PAGE_BACK_NAME = $parent .' class';
            }
        }
    }
    if (is_null($PAGE_BACK_LINK)) {
        $PAGE_BACK_LINK = 'index.'. $ext;
    } else if ($offline) {
        $PAGE_BACK_LINK = str_replace(".php", ".html", $PAGE_BACK_LINK);
    }
    if (is_null($PAGE_BACK_NAME)) {
        $PAGE_BACK_NAME = 'Index';
    }
?>
<div class="backnavigation">
  <table border="0" cellspacing="0" cellpadding="0">
    <tr>
      <td align="right" width="50">
        <a href="<?php echo $PAGE_BACK_LINK ?>">
          <img src="<?php echo SITE_ROOT?>images/back_off.png" border="0" align="middle">
        </a>
      </td>
      <td valign="middle">
        <a href="<?php echo $PAGE_BACK_LINK ?>">
          <?php echo $PAGE_BACK_NAME ?>
        </a>
      </td>
    </tr>
  </table>
</div>
<?php } ?>
<div class="content">
