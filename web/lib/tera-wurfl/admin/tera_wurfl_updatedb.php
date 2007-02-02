<?php
/*
 * Tera_WURFL - PHP MySQL driven WURFL
 * 
 * Tera-WURFL was written by Steve Kamerman, Tera Technologies and is based on the
 * WURFL PHP Tools from http://wurfl.sourceforge.net/.  This version uses a MySQL database
 * to store the entire WURFL file to provide extreme performance increases.
 * 
 * @package tera_wurfl
 * @author Steve Kamerman, Tera Technologies (kamermans AT teratechnologies DOT net)
 * @version Beta 1.4.4 $Date: 2007/01/04 04:26:14 $
 * @license http://www.mozilla.org/MPL/ MPL Vesion 1.1
 * $Id: tera_wurfl_updatedb.php,v 1.1.2.4.2.5 2007/01/04 04:26:14 kamermans Exp $
 * $RCSfile: tera_wurfl_updatedb.php,v $
 * 
 * Based On: WURFL PHP Tools by Andrea Trasatti ( atrasatti AT users DOT sourceforge DOT net )
 *
 */
$source = (isset($_GET['source']))? $_GET['source']: "local";
$type = (isset($_GET['type']))? $_GET['type']: "main";
require_once('../tera_wurfl_config.php');
require_once(WURFL_PARSER_FILE);

// connect to DB
$dbcon = mysql_connect(DB_HOST,DB_USER,DB_PASS) or die("Could not connect to MySQL Server (".DB_HOST."): ".mysql_error());
// select DB
mysql_select_db(DB_SCHEMA,$dbcon) or die("Connected to MySQL Server but could not select database (".DB_SCHEMA."): ".mysql_error($dbcon));
// check tables
$tablesres = mysql_query("SHOW TABLES");
$required_tables = array(DB_DEVICE_TABLE,DB_PATCH_TABLE,DB_HYBRID_TABLE);
$tables = array();
while($table = mysql_fetch_row($tablesres))$tables[]=$table[0];
foreach($required_tables as $req_table){
	if(!in_array($req_table,$tables)){
		echo "Required table '$req_table' was missing in database (".print_r($tables,true)."), creating...<br />";
		emptyWurflDevTable($req_table);
	}
}

$start = utime();
$results = load_wurfl($type,$source);
$end = utime();
if($type == "main"){
	// if this is a patch update, these stats don't really make sense, although they ARE correct
	echo "<strong>Database Update</strong><hr />";
	echo "Total Update Time: ".($end-$start)."<br />";
	echo "Total Devices in WURFL: ".$results['total']."<br />";
	echo "Total Devices inserted in DB: ".$results['inserted']."<br />";
	echo "Total Queries: ".$results['queries']."<br />";
	echo "Largest Query: ".(ceil($results['maxquerysize']/1024))."KB<br />";
	echo "Total Errors: ".count($results['errors'])."<br /><br />";
}
if(count($results['errors']) == 0){
	if(WURFL_PATCH_ENABLE === true){
		echo "<strong>Applying Patch</strong><hr />";
		if(!is_readable(WURFL_PATCH_FILE)){
			echo "<strong>ERROR:</strong> You have the 'WURFL_PATCH_ENABLE' option turned on in the 
configuration file (".CLASS_DIRNAME."tera_wurfl_config.php) but either the patch file (".WURFL_PATCH_FILE.") 
is missing or I can't read it.  To fix this problem either turn the patch option off or make sure the file is
in the correct location.";
		}else{
			$start = utime();
			$results = apply_patch();
			$end = utime();
			echo "Total Update Time: ".($end-$start)."<br />";
			echo "Total Devices in Patch File: ".$results['total']."<br />";
			echo "New Devices Added: ".$results['new']."<br />";
			echo "Merged Devices: ".$results['merged']."<br />";
			echo "Total Queries: ".$results['queries']."<br />";
			echo "Total Errors: ".count($results['errors'])."<br /><br />";
		}
	}else{
		echo "<strong>Patch Disabled in Configuration</strong><br />";
	}
	echo "<br /><br />Update Complete!<br />";
}else{
	echo "Did not attept to apply the patch since there were errors while updating.";
}
echo "<a href=\"index.php\">Return to administration tool.</a>";

function utime($time = false){
	if(!$time)$time = time();
	list($usec, $sec) = explode(" ", microtime());
	$out = ((float)$usec + (float)$sec);
	return($out); 
}
function javascriptRedir($page, $seconds, $custommsg=false){
	global $redirectDelay;
	if(isset($redirectDelay))$seconds += $redirectDelay;
	$file = '';
	$line = '';
	$sent = headers_sent($file,$line);
	if($seconds == 0 && !$sent){
		//headers have not been sent and instant redir was requested - do it!
		session_write_close();
		header("Location: $page");
//		die("header");
		exit;
	}
	$time = $seconds * 1000;
//	die("JavaScript, ($sent)output started on line $line in $file");
	if($custommsg){
		echo $msg;
	}else{
		echo "<br><br>You will be redirected to <a href='$page'>$page</a> in $seconds seconds...";
	}
	echo '<script language="javascript">'."\n".'setTimeout('."'".'window.location.href = "'.$page.'"'."'".', '.$time.');'.'</script>';
	echo '<noscript><meta http-equiv="refresh" content="'.$time.';url='.$page.'" /></noscript>';
	exit;
}
?>
