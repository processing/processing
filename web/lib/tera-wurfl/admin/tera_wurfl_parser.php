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
 * $Id: tera_wurfl_parser.php,v 1.1.2.3.2.10 2007/01/04 04:26:14 kamermans Exp $
 * $RCSfile: tera_wurfl_parser.php,v $
 * 
 * Based On: WURFL PHP Tools by Andrea Trasatti ( atrasatti AT users DOT sourceforge DOT net )
 *
 */

if ( !defined('WURFL_CONFIG') )
	@require_once('../tera_wurfl_config.php');

if ( !defined('WURFL_CONFIG') )
	die("NO CONFIGURATION");

// temp storage for the parsed WURFL
$wurfl = array();
// temp storage for the parsed PATCH
$wurfl_patch = array();
$patch_params = array();

// this function check WURFL patch integrity/validity
function checkpatch($name, $attr) {
	global $wurfl, $wurfl_patch, $patch_params, $checkpatch_result, $wurfl_type;
	if($wurfl_type == "main"){
		$thiswurfl = &$wurfl;
	}elseif($wurfl_type == "patch"){
		$thiswurfl = &$wurfl_patch;
	}else{
		die("Invalid wurfl_type.");
	}
	if ( $name == 'wurfl_patch' ) {
		$checkpatch_result['wurfl_patch'] = true;
		return true;
	} else if ( !$checkpatch_result['wurfl_patch'] ) {
		$checkpatch_result['wurfl_patch'] = false;
		toLog('checkpatch', "no wurfl_patch tag! Patch file ignored.");
		return false;
	}
	if ( $name == 'devices' ) {
		$checkpatch_result['devices'] = true;
		return true;
	} else if ( !$checkpatch_result['devices'] ) {
		$checkpatch_result['devices'] = false;
		toLog('checkpatch', "no devices tag! Patch file ignored.");
		return false;
	}
	if ( $name == 'device' ) {
		if ( isset($thiswurfl['devices'][$attr['id']]) ) {
			if ( $thiswurfl['devices'][$attr['id']]['user_agent'] != $attr['user_agent'] ) {
				$checkpatch_result['device']['id'][$attr["id"]]['patch'] = false;
				$checkpatch_result['device']['id'][$attr["id"]]['reason'] = 'user agent mismatch, orig='.$thiswurfl['devices'][$attr['id']]['user_agent'].', new='.$attr['user_agent'].', id='.$attr['id'].', fall_back='.$attr['fall_back'];
			}
		}
		/*
		 * checking of the fall_back is disabled. I might define a device's fall_back which will be defined later in the patch file.
		 * fall_backs checking could be done after merging.
		if ( $attr['id'] == 'generic' && $attr['user_agent'] == '' && $attr['fall_back'] == 'root' ) {
			// generic device, everything's ok.
		} else if ( !isset($thiswurfl['devices'][$attr['fall_back']]) ) {
			$checkpatch_result['device']['id'][$attr["id"]]['patch'] = false;
			$checkpatch_result['device']['id'][$attr["id"]]['reason'] .= 'wrong fall_back, id='.$attr['id'].', fall_back='.$attr['fall_back'];
		}
		 */
		if ( isset($checkpatch_result['device']['id'][$attr["id"]]['patch'])
			&& !$checkpatch_result['device']['id'][$attr["id"]]['patch'] ) {
			toLog('checkpatch', $checkpatch_result['device']['id'][$attr["id"]]['reason'],LOG_ERR);
			return false;
		}
	}
	return true;
}

function startElement($parser, $name, $attr) {
	global $wurfl, $wurfl_patch, $curr_event, $curr_device, $curr_group, $fp_cache, $check_patch_params, $checkpatch_result, $wurfl_type;
	if($wurfl_type == "main"){
		$thiswurfl = &$wurfl;
	}elseif($wurfl_type == "patch"){
		$thiswurfl = &$wurfl_patch;
	}else{
		die("Invalid wurfl_type.");
	}
	if ( $check_patch_params ) {
		// if the patch file checks fail I don't merge info retrived
		if ( !checkpatch($name, $attr) ) {
			toLog('startElement', "error on $name, ".$attr['id'],LOG_ERROR);
			$curr_device = 'dump_anything';
			return;
		} else if ( $curr_device == 'dump_anything' && $name != 'device' ) {
			// this capability is referred to a device that was erroneously defined for some reason, skip it
			toLog('startElement', $name." cannot be merged, the device was skipped because of an error",LOG_WARNING);
			return;
		}
	}

	switch($name) {
		case "ver":
		case "last_updated":
		case "official_url":
		case "statement":
			//cdata will take care of these, I'm just defining the array
			$thiswurfl[$name]="";
			//$curr_event=$thiswurfl[$name];
			break;
		case "maintainers":
		case "maintainer":
		case "authors":
		case "author":
		case "contributors":
		case "contributor":
			// for the MySQL version I will ignore these (for now)
			// TODO: Add support for non-device WURFL tags
			if ( sizeof($attr) > 0 ) {
				// dirty trick: author is child of authors, contributor is child of contributors
				while ($t = each($attr)) {
					// example: $thiswurfl["authors"]["author"]["name"]="Andrea Trasatti";
					$thiswurfl[$name."s"][$name][$attr["name"]][$t[0]]=$t[1];
				}
			}
			break;
		case "device":
			if ( ($attr["user_agent"] == "" || ! $attr["user_agent"]) && $attr["id"]!="generic" ) {
				die("No user agent and I am not generic!! id=".$attr["id"]." HELP");
			}
			if ( sizeof($attr) > 0 ) {
				while ($t = each($attr)) {
					// example: $thiswurfl["devices"]["ericsson_generic"]["fall_back"]="generic";
					$thiswurfl["devices"][$attr["id"]][$t[0]]=$t[1];
				}
			}
			$curr_device=$attr["id"];
			break;
		case "group":
			// this HAS NOT to be executed or we will define the id as string and then reuse it as array: ERROR
			//$thiswurfl["devices"][$curr_device][$attr["id"]]=$attr["id"];
			$curr_group=$attr["id"];
			break;
		case "capability":
			if ( $attr["value"] == 'true' ) {
				$value = true;
			} else if ( $attr["value"] == 'false' ) {
				$value =  false;
			} else {
				$value = $attr["value"];
				$intval = intval($value);
				if ( strcmp($value, $intval) == 0 ) {
					$value = $intval;
				}
			}
			$thiswurfl["devices"][$curr_device][$curr_group][$attr["name"]]=$value;
			break;
		case "devices":
			// This might look useless but it's good when you want to parse only the devices and skip the rest
			if ( !isset($thiswurfl["devices"]) )
				$thiswurfl["devices"]=array();
			break;
		case "wurfl_patch":
			// opening tag of the patch file
		case "wurfl":
			// opening tag of the WURFL, nothing to do
			break;
		case "default":
			// unknown events are not welcome
			die($name." is an unknown event<br>");
			break;
	}
}

function endElement($parser, $name) {
	global $wurfl, $wurfl_patch, $curr_event, $curr_device, $curr_group, $wurfl_type;
	if($wurfl_type == "main"){
		$thiswurfl = &$wurfl;
	}elseif($wurfl_type == "patch"){
		$thiswurfl = &$wurfl_patch;
	}else{
		die("Invalid wurfl_type.");
	}
	switch ($name) {
		case "group":
			break;
		case "device":
			break;
		case "ver":
		case "last_updated":
		case "official_url":
		case "statement":
			$thiswurfl[$name]=$curr_event;
			// referring to $GLOBALS to unset curr_event because unset will not destroy 
			// a global variable unless called in this way
			unset($GLOBALS['curr_event']);
			break;
		default:
			break;
	}

}

function characterData($parser, $data) {
	global $curr_event;
	if (trim($data) != "" ) {
		$curr_event.=$data;
		//echo "data=".$data."<br>\n";
	}
}

function emptyWurflDevTable($tablename){
	$droptable = "DROP TABLE IF EXISTS ".$tablename;
	$createtable = "CREATE TABLE `".$tablename."` (
  `deviceID` varchar(128) binary NOT NULL default '',
  `user_agent` varchar(255) default NULL,
  `fall_back` varchar(128) default NULL,
  `actual_device_root` tinyint(1) default '0',
  `capabilities` mediumtext,
  PRIMARY KEY  (`deviceID`),
  KEY `fallback` (`fall_back`),
  KEY `useragent` (`user_agent`)
) TYPE=".DB_TYPE;
	$emptytable = "DELETE FROM ".$tablename;
	if(DB_EMPTY_METHOD == "DROP_CREATE"){
		mysql_query($droptable) or die(mysql_error());
		mysql_query($createtable) or die(mysql_error());
	}else{
		mysql_query($emptytable) or die(mysql_error());
	}
	return(true);
}

function load_wurfl($filetype="main",$source="local") {
	global $wurfl, $wurfl_patch, $curr_event, $curr_device, $curr_group, $fp_cache, $check_patch_params, $checkpatch_result, $wurfl_type;
	$wurfl_type = $filetype;
	if($wurfl_type == "main"){
		$devtable = DB_DEVICE_TABLE.DB_TEMP_EXT;
		$prodtable = DB_DEVICE_TABLE;
		$wurflfile = WURFL_FILE;
		$thiswurfl = &$wurfl;
	}elseif($wurfl_type == "patch"){
		$devtable = DB_PATCH_TABLE.DB_TEMP_EXT;
		$prodtable = DB_PATCH_TABLE;
		$wurflfile = WURFL_PATCH_FILE;
		$thiswurfl = &$wurfl_patch;
	}
	if(($source == "remote" || $source == "remote_cvs") && $wurfl_type == "main"){
		if($source == "remote"){
			$dl_url = WURFL_DL_URL; 
		}elseif($source == "remote_cvs"){
			$dl_url = WURFL_CVS_URL;
		}
		$newfile = DATADIR."dl_wurfl.xml";
		echo "Downloading WURFL from $dl_url ...\n<br/>";
		flush();
		if(!is_writable(DATADIR)){
			toLog('update',"no write permissions for data directory",LOG_ERR);	
			die("Fatal Error: The data directory is not writable. (".DATADIR.")<br/><br/><strong>Please make the data directory writable by the user that runs the webserver process, in Linux this command would do the trick if you're not too concered about security: <pre>chmod -R 777 ".DATADIR."</pre></strong>");
		}
		$dl_wurfl = file_get_contents($dl_url);
		file_put_contents($newfile,$dl_wurfl);
		$size = filesize($newfile);
		echo "done ($size bytes)<br />";
		flush();
		// ignore this error - I know I'm redefining a constant :P
		@define("WURFL_FILE",$newfile);
		$wurflfile = $newfile;
	}
	$thiswurfl = array();
	$xml_parser = xml_parser_create();
	xml_parser_set_option($xml_parser, XML_OPTION_CASE_FOLDING, false);
	xml_set_element_handler($xml_parser, "startElement", "endElement");
	xml_set_character_data_handler($xml_parser, "characterData");
	if ( !file_exists($wurflfile) ) {
		toLog('parse', $wurflfile." does not exist",LOG_ERR);
		die($wurflfile." does not exist");
	}
	if (!($fp = fopen($wurflfile, "r"))) {
		toLog('parse', "$wurflfile could not be opened for XML input",LOG_ERR);
		die("$wurflfile could not opened XML input");
	}
	while ($data = fread($fp, 4096)) {
		if (!xml_parse($xml_parser, $data, feof($fp))) {
			$errmsg = sprintf("XML error: %s at line %d",xml_error_string(xml_get_error_code($xml_parser)),xml_get_current_line_number($xml_parser));
			toLog('parse',$wurflfile." ".$errmsg);
			die($wurflfile." ".$errmsg);
		}
	}
	fclose($fp);
	xml_parser_free($xml_parser);
	$devices = $thiswurfl["devices"];
	emptyWurflDevTable($devtable);
	$processedrows = count($devices);
	$queries = 0;
	$insertedrows = 0;
	$maxquerysize = 0;
	$insert_errors = array();
	$insertcache = array();
	$used_ids = array();
	foreach($devices as $dev_id => $dev_data) {
		/*
		 * This will detect duplicate device_ids in the WURFL
		 * if they are different cases.  This is important for
		 * databases like MySQL where keys are sorted without regard
		 * for case.
		 * 
		if(in_array(strtolower($dev_id),$used_ids)){
			$insert_errors[] = "Duplicate ID omitted: \"$dev_id\"";
			continue;
		}else{
			$used_ids[] = $dev_id;
		}
		*/
	//	$wurfl_agents[$one['user_agent']] = $one['id'];
		// convert device root to tinyint format (0|1) for db
		$devroot = (isset($dev_data['actual_device_root']) && $dev_data['actual_device_root'])? 1: 0;
		if(strlen($dev_data['user_agent']) > 255){
			$insert_errors[] = "Warning: user agent too long: \"".$dev['user_agent'].'"';
		}
		if(DB_MULTI_INSERT){
			$insertcache[] = sprintf("(%s,%s,%s,%s,%s)",
			sqlPrep($dev_id),
			sqlPrep($dev_data['user_agent']),
			sqlPrep($dev_data['fall_back']),
			sqlPrep($devroot),
			sqlPrep(serialize($dev_data))
			);
			if(count($insertcache) >= DB_MAX_INSERTS){
				$query = "INSERT INTO ".$devtable." (deviceID, user_agent, fall_back, actual_device_root, capabilities) VALUES ".implode(",",$insertcache);
				mysql_query($query) or $insert_errors[] = "DB server reported error on id \"$dev_id\": ".mysql_error();
				$insertedrows += mysql_affected_rows();
				$insertcache = array();
				$queries++;
				$maxquerysize = (strlen($query)>$maxquerysize)? strlen($query): $maxquerysize;
			}
		}else{
			$query = sprintf("INSERT INTO ".$devtable." (deviceID, user_agent, fall_back, actual_device_root, capabilities) VALUES (%s,%s,%s,%s,%s)",
			sqlPrep($dev_id),
			sqlPrep($dev_data['user_agent']),
			sqlPrep($dev_data['fall_back']),
			sqlPrep($devroot),
			sqlPrep(serialize($dev_data))
			);
			mysql_query($query) or $insert_errors[]=mysql_error();
			$insertedrows += mysql_affected_rows();
			$queries++;
			$maxquerysize = (strlen($query)>$maxquerysize)? strlen($query): $maxquerysize;
		}
	}
	// some records are probably left in the insertcache
	if(DB_MULTI_INSERT && count($insertcache) > 0){
		$query = "INSERT INTO ".$devtable." (deviceID, user_agent, fall_back, actual_device_root, capabilities) VALUES ".implode(",",$insertcache);
		mysql_query($query) or $insert_errors[]=mysql_error();
		$insertedrows += mysql_affected_rows();
		$queries++;
		$maxquerysize = (strlen($query)>$maxquerysize)? strlen($query): $maxquerysize;
	}
	// perform sanity checks
	if(count($insert_errors) > 0){
		// problem with update - changes will not be applied
		echo "There were errors while updating the WURFL.  No changes have been made to your database.<br /><br />";
		foreach($insert_errors as $error){
			toLog("load_wurfl","error inserting device: ".$error,LOG_ERR);
			echo "Error inserting device: ".$error."<br />";
		}
	}else{
		// everything seems to be fine
		replace_table($prodtable, $devtable);
		toLog("load_wurfl","the $wurfl_type database was successfully updated from: $source",LOG_WARNING);
	}
	return(array("total" => $processedrows, "inserted" => $insertedrows, "errors" => $insert_errors, "queries" => $queries, "maxquerysize" => $maxquerysize));
}
function replace_table($to_be_replaced, $replacement){
	@mysql_query("SELECT COUNT(deviceID) AS num FROM ".$replacement) or die("ERROR: table not found (".$replacement."): ".mysql_error());
	mysql_query("DROP TABLE IF EXISTS ".$to_be_replaced);
	mysql_query("RENAME TABLE `".$replacement."` TO `".$to_be_replaced."`") or die("ERROR: could not rename table - make sure the database users has ALTER permissions! Error message: ".mysql_error());
	return(true);
}
function apply_patch(){
	emptyWurflDevTable(DB_HYBRID_TABLE);
	$queries = 1;
	$merge_errors = array();
	// find total number of patch records
	$res = mysql_query("SELECT COUNT(deviceID) AS num FROM ".DB_PATCH_TABLE);
	$processedrows = mysql_result($res,0,'num');
	$queries++;
	// fill the hybrid table with the stock WURFL first
	$fillhybrid = "INSERT INTO ".DB_HYBRID_TABLE." SELECT * FROM ".DB_DEVICE_TABLE;
	mysql_query($fillhybrid);
	$queries++;
	// insert all the patch devices that DON'T already exist in the WURFL into the hybrid table
	mysql_query("INSERT INTO ".DB_HYBRID_TABLE." SELECT p.* FROM ".DB_PATCH_TABLE." AS p LEFT JOIN ".DB_HYBRID_TABLE." AS d ON p.deviceID = d.deviceID WHERE d.deviceID IS NULL");
	$queries++;
	$newdevs = mysql_affected_rows();
	// get all the devices that DO exist in the main WURFL so we can merge them in the hybrid table
	$patchres = mysql_query("SELECT p.* FROM ".DB_PATCH_TABLE." AS p LEFT JOIN ".DB_DEVICE_TABLE." AS d ON p.deviceID = d.deviceID WHERE d.deviceID IS NOT NULL");
	$queries++;
	$mergeddevs = mysql_num_rows($patchres);
	while($new = mysql_fetch_assoc($patchres)){
		// grab the original record to merge with the new one
		$origres = mysql_query("SELECT * FROM ".DB_HYBRID_TABLE." WHERE deviceID=".sqlPrep($new['deviceID']));
		$queries++;
		$orig = mysql_fetch_assoc($origres);
		$origcap = unserialize($orig['capabilities']);
		$newcap = unserialize($new['capabilities']);
		$merged = $new;
		$mergedcap = $origcap;
		foreach($newcap as $key => $val) {
			if ( is_array($val) ) {
				// TODO: Make sure this works correctly, I'm suspicious because of LOG_NOTICE errors
				$mergedcap[$key] = @array_merge($mergedcap[$key], $val);
			} else {
				$mergedcap[$key] = $val;
			}
		}
		$merged['capabilities'] = serialize($mergedcap);
		// now we should have a merged record - update it
		$setstringarr = array();
		foreach($merged as $key => $val){
			$setstringarr[] = $key."=".sqlPrep($val);
		}
		$setstring = implode(", ",$setstringarr);
		mysql_query("UPDATE ".DB_HYBRID_TABLE." SET ".$setstring." WHERE deviceID=".sqlPrep($merged['deviceID']));
		$queries++;
	}
	return(array("total" => $processedrows, "new" => $newdevs, "merged" => $mergeddevs, "errors" => $merge_errors, "queries" => $queries));
}
function sqlPrep($value){
	if (get_magic_quotes_gpc()) $value = stripslashes($value);
	if($value == '') $value = 'NULL';
	else if (!is_numeric($value) || $value[0] == '0') $value = "'" . mysql_real_escape_string($value) . "'"; //Quote if not integer
	return $value;
}

// PHP4 does not have file_put_contents() so I emulate it if it's not defined
if(!function_exists("file_put_contents")){
	function file_put_contents($n, $d, $flag = false) {
	   $mode = @($flag == FILE_APPEND || strtoupper($flag) == 'FILE_APPEND') ? 'a' : 'w';
	   $f = @fopen($n, $mode);
	   if ($f === false) {
	       return 0;
	   } else {
	       if (is_array($d)) $d = implode($d);
	       $bytes_written = fwrite($f, $d);
	       fclose($f);
	       return $bytes_written;
	   }
	}
}
function toLog($func, $text, $requestedLogLevel=LOG_NOTICE){
	if($requestedLogLevel == LOG_ERR) $this->errors[] = $text;
	if ( !defined('LOG_LEVEL') || LOG_LEVEL == 0 || ($requestedLogLevel-1) >= LOG_LEVEL ) {
		return;
	}
	if ( $requestedLogLevel == LOG_ERR ) {
		$warn_banner = 'ERROR: ';
	} else if ( $requestedLogLevel == LOG_WARNING ) {
		$warn_banner = 'WARNING: ';
	} else {
		$warn_banner = '';
	}
	// Thanks laacz
	$_textToLog = date('r')." [".php_uname('n')." ".getmypid()."]"."[$func] ".$warn_banner . $text;
	$_logFP = fopen(WURFL_LOG_FILE, "a+");
	fputs($_logFP, $_textToLog."\n");
	fclose($_logFP);
	return(true);
}
?>