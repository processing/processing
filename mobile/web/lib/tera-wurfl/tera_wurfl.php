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
 * $Id: tera_wurfl.php,v 1.1.4.6.2.13 2007/01/04 04:26:14 kamermans Exp $
 * $RCSfile: tera_wurfl.php,v $
 * 
 * Based On: WURFL PHP Tools by Andrea Trasatti ( atrasatti AT users DOT sourceforge DOT net )
 *
 */

if ( !defined('WURFL_CONFIG') )
	require_once(dirname(__FILE__).'/tera_wurfl_config.php');

if ( !defined('WURFL_CONFIG') )
	die("NO CONFIGURATION");

if ( defined('WURFL_PARSER_FILE') )
	require_once(WURFL_PARSER_FILE);
else
	require_once(dirname(__FILE__)."/tera_wurfl_parser.php");

/**
 * Tera-WURFL was written by Steve Kamerman, Tera Technologies and is based on the
 * WURFL PHP Tools from http://wurfl.sourceforge.net/.  This version uses a MySQL database
 * to store the entire WURFL file to provide extreme performance increases.
 * 
 * See the documentation for specific usage information.
 * Quick Example:
 * <code>
 * $myDevice = new tera_wurfl();
 * $myDevice->getDeviceCapabilitiesFromAgent($_SERVER['HTTP_USER_AGENT');
 * // see if this device is really a mobile browser
 * if($myDevice->browser_is_wap){
 * 	// check capabilities
 *	if($myDevice->capabilities['downloadfun']['downloadfun_support']){
 *		echo "downloadfun supported<br />";
 *	}else{
 *		echo "WAP is supported, downloadfun is not<br />";
 * 	}
 *	if($myDevice->device_image != ''){
 * 		// display device image
 * 		echo '<img src="'.$myDevice->device_image.'" /><br />';
 * 	}
 * }
 * </code>
 * 
 * @package tera_wurfl
 */
class tera_wurfl {
	/**
	 * Internal tracking of the WURFL ID
	 * @var string
	 * @access private
	 */
	var $id="";

	/**
	 * If true, Openwave's GUI (mostly wml 1.3) is supported
	 * @var bool
	 * @access public
	 */
	var $GUI=false;

	/**
	 * Device brand (manufacturer)
	 * @var string
	 * @access public
	 */
	var $brand='';

	/**
	 * Device model name
	 * @var string
	 * @access public
	 */
	var $model='';

	/**
	 * If this is a WAP device, this is set to true
	 * @var boolean
	 * @access public
	 */
	var $browser_is_wap=false;

	/**
	 * associative array with all the device's capabilities.
	 * 
	 * Example:  $this->capabilities['downloadfun']['downloadfun_support'] 
	 *	true if downloadfun is supported, otherwise false
	 *
	 * @var associative array
	 * @access public
	 */
	var $capabilities=array();

	/**
	 * HTTP_ACCEPT request headers
	 * Use this to manually set the http-accept string:
	 * 
	 * Example:  $this->http_accept = "text/vnd.wap.wml";
	 * @var string
	 * @access public
	 */
	var $http_accept="";

	/**
	 * Ignore desktop browser
	 * Set this to false if you want to search the WURFL/patch
	 * for desktop web browsers as well.
	 * 
	 * @var string
	 * @access public
	 */
	var $ignoreBrowser=false;

	/**
	 * Track errors
	 * Anytime a LOG_ERR level event occurs, a description is
	 * added to the end of the array.
	 * Example:  echo count($this->$errors); // echos number of errors
	 * @var array
	 * @access public
	 */
	var $errors = array();

	/**
	 * Internal database link resource
	 * @var resource
	 * @access private
	 */
	var $dbcon = '';
	
	/**
	 * Internal device table tracking.  Used to provide some members with the name
	 * of the current device table.  It will be either DB_DEVICE_TABLE or DB_HYBRID_TABLE
	 * @var string
	 * @access private
	 */
	var $devtable = '';
	
	/**
	 * WURFL ID of the ancestoral device root
	 * This is the ID of the actual device, not a firmware revision
	 *
	 * @var string
	 * @access public
	 */
	var $device_root = '';
	
	/**
	 * Device image path and filename, relative to the class file
	 *
	 * @var string
	 * @access public
	 */
	var $device_image = '';
	
	/**
	 * Constructor, sets the http_accept property and connects to the WURFL database. 
	 * You don't need to call the constructor - it is called when you instantiate the class
	 *
	 * @access public
	 * @return boolean	success
	 *
	 */
	function tera_wurfl() {
		// connect to database
		$this->dbcon = mysql_connect(DB_HOST,DB_USER,DB_PASS) or die("ERROR: Could not connect to MySQL Server (".DB_HOST."): ".mysql_error());
		// select schema
		mysql_select_db(DB_SCHEMA,$this->dbcon) or die("ERROR: Connected to MySQL Server but could not select database (".DB_SCHEMA."): ".mysql_error());
		if(WURFL_PATCH_ENABLE){
			$this->devtable = DB_HYBRID_TABLE;
		}else{
			$this->devtable = DB_DEVICE_TABLE;
		}
		// make sure the device table exists
		$test = @mysql_query("SELECT COUNT(deviceID) AS num FROM ".$this->devtable) or die("ERROR: Device table not found (".$this->devtable."): ".mysql_error()."<br/><br/><strong>If this is a new installation, please <a href=\"admin/\">update the database</a>.");
		// make sure the device table is not empty
		if(mysql_result($test,0,"num") == 0)die("ERROR: The device table (".$this->devtable.") is empty. Please update the WURFL database.");
		// set the default http-accept
		$this->http_accept = $_SERVER['HTTP_ACCEPT'];
		$this->_toLog('constructor', '-----Class Initiated-----', LOG_NOTICE);
		return(true);
	}

	/**
	 * Given the device's id reads all its capabilities
	 *
	 * @param string the device's id from the WURFL database
	 * @access private
	 * @return boolean success
	 */
	function _GetFullCapabilities($_id) {
		if(count($this->errors) != 0) return(false);
		$this->_toLog('_GetFullCapabilities', "searching for $_id", LOG_INFO);
		$_curr_device = $this->_getDeviceCapabilitiesFromId($_id);
		// array of full records
		$_capabilities[] = $_curr_device;
		// keep the while loop from running away on an error
		$iteration_limit = 20;
		$i = 0;
		while ( $_curr_device['fall_back'] != 'generic' && $_curr_device['fall_back'] != 'root' && $i <= $iteration_limit) {
			$this->_toLog('_GetFullCapabilities', 'parent device:'.$_curr_device['fall_back'].' now going to read its capabilities', LOG_INFO);
			$_curr_device = $this->_getDeviceCapabilitiesFromId($_curr_device['fall_back']);
			array_unshift($_capabilities,$_curr_device);
			$i++;
			//die("stopped");
		}
		if($i >= $iteration_limit){
			// the while loop from ran away
			$this->_toLog('_GetFullCapabilities', 'Killing runaway while loop - $_id='.$_id, LOG_ERR);
			return(false);
		}
		$this->_toLog('_GetFullCapabilities', 'reading capabilities of \'generic\' device', LOG_INFO);
		$generic = $this->_getDeviceCapabilitiesFromId('generic');
		$_final = $generic;
		// the generic devices are already at the top of the array because I used array_unshift()
		foreach($_capabilities as $curr_device){
			//TODO: Why don't I just array_merge the whole record???? Good question!
			foreach($curr_device as $key => $val) {
				if ( is_array($val) ) {
					$_final[$key] = array_merge($_final[$key], $val);
				} else {
					$_final[$key] = $val;
				}
			}
		}
		$this->capabilities = $_final;
		$this->brand = $this->capabilities['product_info']['brand_name'];
		$this->model = $this->capabilities['product_info']['model_name'];
		$this->id = $this->capabilities['id'];
		return(true);
	}

	/**
	 * Given a device id reads its capabilities
	 *
	 * @param string device's wurfl_id
	 * @access private
	 * @return mixed boolean false if not identified or array capabilities
	 *
	 */
	function _getDeviceCapabilitiesFromId($_id) {
		if(count($this->errors) != 0) return(false);
		$this->_toLog('_getDeviceCapabilitiesFromId', "reading id:$_id", LOG_INFO);
		if ( $_id == 'upgui_generic' ) {
			$this->GUI = true;
		}
		$res = mysql_query("SELECT * FROM ".$this->devtable." WHERE deviceID=".$this->_sqlPrep($_id),$this->dbcon) or die(mysql_error($this->dbcon));
		if(mysql_num_rows($res) > 0){
			$device = mysql_fetch_assoc($res);
			//print_r($device);
			if($this->device_root == '' && $device['actual_device_root'] == 1){
				$this->_toLog("_getDeviceCapabilitiesFromId","device root detected: ".$device['deviceID'], LOG_INFO);
				$this->device_root = $device['deviceID'];
				$image = IMAGE_DIR.$device['deviceID'].".gif";
				// PHP evaluates from left to right, so "file_exists" will not get
				// called if IMAGE_CHECKING is false
				if(IMAGE_CHECKING && file_exists(dirname(__FILE__).'/'.$image)){
					$this->device_image = $image;
					$this->_toLog("_getDeviceCapabilitiesFromId","device image found: $image",LOG_INFO);
				}
			}
			$cap = unserialize($device['capabilities']);
			//echo "<pre>".print_r($cap,true)."</pre>";
			return($cap);
		}else{
			// device is not in the WURFL
			// deal with it appropriately
			$this->_toLog('_getDeviceCapabilitiesFromId', "the id $_id is not present in wurfl", LOG_WARNING);
			//die("the id $_id is not present in wurfl_agents");
			echo("the id ($_id) is not present in wurfl DB<br />");
			// I should never get here!!
			return(false);
		}
	}

	/**
	 * Given the user_agent reads the device's capabilities.
	 * This method will return true or false based on its success.
	 * After calling this function, the following properties will be accessible (on success):
	 * <ul>
	 * <li>$this->brand</li>
	 * <li>$this->model</li>
	 * <li>$this->browser_is_wap</li>
	 * <li>$this->capabilities</li>
	 * <li>$this->device_root</li>
	 * <li>$this->device_image (if enabled)</li>
	 * <li>$this->errors</li>
	 * <li>$this->GUI</li>
	 * </ul>
	 * 
	 * Example:
	 * <code>
	 * $myDevice = new tera_wurfl();
	 * // get the capabilities of your device
	 * $myDevice->getDeviceCapabilitiesFromAgent($_SERVER['HTTP_USER_AGENT');
	 * echo "Device: ".$myDevice->brand." ".$myDevice->model."<br />";
	 * </code>
	 * 
	 * @param string	device's user_agent
	 * @param boolean	check the HTTP-ACCEPT headers if needed
	 * @access public
	 * @return boolean success
	 *
	 **/
	function getDeviceCapabilitiesFromAgent($_user_agent, $_check_accept=false) {
		if(count($this->errors) != 0) return(false);
		//TODO: Would be cool to log user agent and headers to future use to feed WURFL
		// clear any existing device root and image
		$this->device_root = '';
		$this->device_image = '';
		// Would be cool to log user agent and headers to future use to feed WURFL
		// Resetting properties
		$this->user_agent = '';
		$this->wurfl_agent = '';
		$this->id = '';
		$this->GUI = false;
		$this->brand = '';
		$this->model = '';
		$this->browser_is_wap = false;
		$this->capabilities = array();

		// removing the possible Openwave MAG tag
		// at this point a user agent like this: "UP.Link/6.3.0.0.0" will
		// result in an null $_user_agent
		$_user_agent = trim(ereg_replace("UP.Link.*", "", $_user_agent));
//		if(trim($_user_agent) == '' || !$_user_agent)
		//FIXME: PocketPCs (Windows Mobile) contain "MSIE 5"
		// like the Cingular 8125
		if (	( stristr($_user_agent, 'Opera') && stristr($_user_agent, 'Windows') )
			|| ( stristr($_user_agent, 'Opera') && stristr($_user_agent, 'Linux') )
			|| stristr($_user_agent, 'Gecko')
			|| ( (stristr($_user_agent, 'MSIE 6') || stristr($_user_agent, 'MSIE 5') ) && !stristr($_user_agent, 'MIDP') && !stristr($_user_agent, 'Windows CE') && !stristr($_user_agent, 'Symbian') )
			) {
			// This is a web browser. Setting the defaults
			$this->_toLog('constructor', 'Web browser', LOG_INFO);
			$this->browser_is_wap=false;
			$this->capabilities['product_info']['brand_name'] = 'Generic Web browser';
			$this->capabilities['product_info']['model_name'] = '1.0';
			$this->capabilities['product_info']['is_wireless_device'] = false;
			$this->capabilities['product_info']['device_claims_web_support'] = true;
			if($this->ignoreBrowser){
				// choosing not to waste time looking up desktop browsers
				return(true);
			}
		} else if ( $_check_accept == true ) {
			if (
			     !eregi('wml', $this->http_accept)
			     && !eregi('wap', $this->http_accept)
			     && !eregi('xhtml', $this->http_accept)
			     ) {
				$this->_toLog('constructor', 'This browser does not support wml, nor wap, nor xhtml', LOG_WARNING);
				$this->browser_is_wap=false;
			}
			// We know this is a mobile device
		}
		$this->_toLog('getDeviceCapabilitiesFromAgent', 'searching for '.$_user_agent, LOG_INFO);
		if ( trim($_user_agent) == '' || !$_user_agent ) {
			// NO USER AGENT??? This is not a WAP device
			$this->_toLog('getDeviceCapabilitiesFromAgent', 'No user agent', LOG_ERR);
			$this->browser_is_wap=false;
			return(false);
		}
		$curr_device = $this->_UserAgentInDB($_user_agent);
		if(is_array($curr_device)){
			// the user agent was in the WURFL - Great!
			//DEBUG: echo "Found exact UA in DB!<br />".print_r($curr_device,true)."<br />";
			$this->_GetFullCapabilities($curr_device['deviceID']);
			// in case you have web browsers in your patch file
			// It was suggested by MOLABIB on 22Dec2006 that the following line
			// is incorrect and should be replaced:
			// $this->browser_is_wap = $this->capabilities['browser_is_wap'];
			// the following is it's replacement:
			$this->browser_is_wap = $this->capabilities['product_info']['is_wireless_device'];
			$this->brand = $this->capabilities['product_info']['brand_name'];
			$this->model = $this->capabilities['product_info']['model_name'];
			return(true);
		}
		// the user agent was NOT in the WURFL - try to dissect it
		$_ua = $_user_agent;
		// Used to be $_ua_len = strlen($_ua) - 1 but that resulted in erroneous results since
		// the full string could still match UAs longer than itself in the DB
		// Thanks to Christian Aune Thomassen [christian.thomassen(at)waptheweb.no] for finding this bug!
		$_ua_len = strlen($_ua);
		// Searching in wurfl_agents
		// The user_agent should not become shorter than 4 characters
		$this->_toLog('getDeviceCapabilitiesFromAgent', 'Searching in the agent database', LOG_INFO);
		// I request to set a short list of UA's among which I should search an unknown user agent
		$_short_ua_len = 4;
		$_last_good_short_ua = array();
		$_min_len = 4;
		$niceua = rtrim($this->_sqlPrep(substr($_ua, 0, $_min_len)),"'")."%'";
		$minquery = "SELECT COUNT(deviceID) AS num FROM ".$this->devtable." WHERE user_agent LIKE $niceua";
		$res = mysql_query($minquery,$this->dbcon);
		if(mysql_result($res,0,'num') == 0){
			//DEBUG: echo "no devices match the UA down to the min chars in the DB<br />$minquery";
			// no devices match the UA down to the min chars in the DB
			// basically you're not going to get a match.
			// look for acceptable generic ID
			if(!$this->_getGenericID($_user_agent)){
				// no generic ID found - assuming this is not a WAP device
				return(true);
			}
		}else{
			while ( $_ua_len > $_min_len) {
				//DEBUG: echo "--trying user agent length $_ua_len<br />";
				$_short_wurfl_ua = array();
				$_short_ua = substr($_ua, 0, $_ua_len);
				// take the user agent and prep it (escapes chars and adds single quotes
				// then remove the right most quote and put %' in it's place which will
				// make it this MOT- look like this: 'MOT-%' - that will work for MySQL LIKE queries
				$niceua = rtrim($this->_sqlPrep($_short_ua),"'")."%'";
				$res = mysql_query("SELECT user_agent FROM ".$this->devtable." WHERE user_agent LIKE $niceua",$this->dbcon);
				while($row = mysql_fetch_assoc($res)){
					// load the $_short_wurfl_ua array with matching user agents
					$_short_wurfl_ua[] = $row['user_agent'];
				}
				if ( $_ua_len <= $_min_len && count($_short_wurfl_ua) == 0 ) {
					// Probably impossible to get here, but if it does there is no match
					// DEBUG fast search echo "no match even for the first 4 chars<br>\n";
					break;
				}
				if ( count($_short_wurfl_ua) > 0 ) {
					// This is the FIRST time that ANY matching user agents were found
					//DEBUG: echo "list has ".count($_short_wurfl_ua)." elements<br />";
					break;
				}
				// shortening the agent by one each time
				$_ua_len--;
			}
		}
		//DEBUG: die("Done.<br />Original UA: $_user_agent<br />Best match UA: ".$_short_wurfl_ua[0]."<br />");
		//FIXME: probably shouldn't blindly grab the first UA, so improve...
		if(count($_short_wurfl_ua) > 0){
			$bestmatch = array_shift($_short_wurfl_ua);
			$device = $this->_UserAgentInDB($bestmatch);
//			die(print_r($device,true));
			$this->_GetFullCapabilities($device['deviceID']);
		}else{
			return(false);
		}
		return(true);
	}
	/**
	 * Checks to see if a given user agent is in the WURFL database
	 * 
	 * @param string user agent
	 * @return mixed false if not in DB, else fully device record array
	 * @access private
	 */
	function _UserAgentInDB($ua){
		//TODO: using LIKE will do the search case-insensitive, but = is much faster
		$res = mysql_query("SELECT * FROM ".$this->devtable." WHERE user_agent LIKE ".$this->_sqlPrep($ua),$this->dbcon);
		$curr_device = mysql_fetch_assoc($res);
		$ret = (mysql_num_rows($res) > 0)? $curr_device: false;
		return($ret);
	}
	/**
	 * Given a capability name returns the value (true|false|<anythingelse>).  This is
	 * helpful if you don't know the group that the capability is in.
	 * 
	 * Example:
	 * <code>
	 * $myDevice = new tera_wurfl();
	 * // get the capabilities of your device
	 * $callstring $myDevice->getDeviceCapability('wml_make_phone_call_string');
	 * echo "<a href=\"$callstring6161234567\">Call me</a>";
	 * </code>
	 * 
	 * @param string capability name as a string
	 * @access public
	 * @return mixed boolean success or other string if specified in the WURFL
	 *
	 */
	function getDeviceCapability($capability) {
		$this->_toLog('_getDeviceCapability', 'Searching for '.$capability.' as a capability', LOG_INFO);
		$deviceCapabilities = $this->capabilities;
		foreach ( $deviceCapabilities as $group ) {
			if ( !is_array($group) ) {
				continue;
			}
			while ( list($key, $value)=each($group) ) {
				if ($key==$capability) {
					$this->_toLog('_getDeviceCapability', 'I found it, value is '.$value, LOG_INFO);
					return $value;
				}
			}
		}
		$this->_toLog('_getDeviceCapability', 'I could not find the requested capability, returning false', LOG_WARNING);
		return false;
	}
	/**
	 * Clears any LOG_ERR level errors that were detected.  This will
	 * allow you to continue testing other user agents after an error
	 * is detected.
	 *
	 * Example:
	 * <code>
	 * $myDevice = new tera_wurfl();
	 * // this will throw an error and prevent the methods from working
	 * $myDevice->getDeviceCapabilitiesFromAgent("");
	 * // clear any errors
	 * $myDevice->clearErrors();
	 * // now we can check another user agent
	 * $myDevice->getDeviceCapabilitiesFromAgent("MOT-V3");
	 * </code>
	 * 
	 * @access public
	 * @return boolean true
	 */
	function clearErrors(){
		$this->errors = array();
		return(true);
	}

	/**
	 * This function checks and prepares the text to be logged
	 *
	 * @access private
	 * @param string		Function name
	 * @param string		Reason text/description
	 * @param int			Log level (LOG_ERR|LOG_WARN|LOG_NOTICE|LOG_INFO)
	 * 						Any of the PHP LOG contstants will work, but if you
	 * 						throw a LOG_ERR, the script will stop and return false
	 */
	function _toLog($func, $text, $requestedLogLevel=LOG_NOTICE){
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
	/**
	 * Kills the script on fatal database errors
	 * 
	 * @access private
	 */
	function _dberror(){
		die("Error in query: ".mysql_error($this->dbcon));
	}
	/**
	 * This function checks the user agent for signs that it's a WAP device
	 * Returns true if a generic ID is found, otherwise false.
	 * This is a last resort function that is only called if the device in question
	 * does not exist in the WURFL and the class is forced to find another way to
	 * identify the device.
	 *
	 * @param string User agent
	 * @access private
	 * @return boolean success
	 */
	function _getGenericID($_user_agent){
		$this->_toLog('getGenericID', "I couldn't find the device in my list, the headers are my last chance", LOG_WARNING);
		if ( strstr($_user_agent, 'UP.Browser/') && strstr($_user_agent, '(GUI)') ) {
			$this->browser_is_wap = true;
			$this->user_agent = $_user_agent;
			$this->wurfl_agent = 'upgui_generic';
			$this->id = 'upgui_generic';
		} else if ( strstr($_user_agent, 'UP.Browser/') ) {
			$this->browser_is_wap = true;
			$this->user_agent = $_user_agent;
			$this->wurfl_agent = 'uptext_generic';
			$this->id = 'uptext_generic';
		} else if ( eregi('wml', $this->http_accept) || eregi('wap', $this->http_accept) ) {
			$this->browser_is_wap = true;
			$this->user_agent = $_user_agent;
			$this->wurfl_agent = 'generic';
			$this->id = 'generic';
		} else {
			$this->_toLog('getGenericID', 'This should not be a WAP device, quitting', LOG_WARNING);
			$this->browser_is_wap=false;
			$this->user_agent = $_user_agent;
			$this->wurfl_agent = 'generic';
			$this->id = 'generic';
			return(false);
		}
		return(true);
	}
	/**
	 * Given a string, will escape any unfriendly charachters and return
	 * 	a single quoted string to be used directly in an SQL statement to a
	 * 	MySQL server.
	 * Given a real number it will return the number without quotes so MySQL
	 * 	sees it as a number instead of a string.
	 * Given a null string ('') it will return the MySQL keyword NULL.
	 * 
	 * Example:  it's a fine day -> 'it\'s a fine day'
	 * @param mixed Input variable to be prepared
	 * @access private
	 */
	function _sqlPrep($value){
		if (get_magic_quotes_gpc()) $value = stripslashes($value);
		if($value == '') $value = 'NULL';
		else if (!is_numeric($value) || $value[0] == '0') $value = "'" . mysql_real_escape_string($value) . "'"; //Quote if not integer
		return($value);
	}

} 
?>
