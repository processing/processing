<?php
/**
* PHP HTTP Tools is a library for working with the http protocol
* php_http_connector establishes http connections
* @package php-http-tools
* @version 0.1
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/php_http_tools/ PHP HTTP Tools Home Page
* PHP HTTP Tools are Free Software
**/

if (!defined('PHP_HTTP_TOOLS_INCLUDE_PATH')) {
	define('PHP_HTTP_TOOLS_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

/**
* A helper class for establishing HTTP connections
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_connector {
	/** @var object A reference to a http connection or proxy, if one is required */
	var $httpConnection = null;
	
	/**
	* Specifies the parameters of the http conection used to obtain the xml data
	* @param string The ip address or domain name of the connection
	* @param string The path of the connection
	* @param int The port that the connection is listening on
	* @param int The timeout value for the connection
	* @param string The user name, if authentication is required
	* @param string The password, if authentication is required
	*/
	function setConnection($host, $path = '/', $port = 80, $timeout = 0, $user = null, $password = null) {
	    require_once(PHP_HTTP_TOOLS_INCLUDE_PATH . 'php_http_client_generic.php');
		
		$this->httpConnection =& new php_http_client_generic($host, $path, $port, $timeout, $user, $password);
	} //setConnection
	
	/**
	* Specifies basic authentication for an http connection
	* @param string The user name
	* @param string The password
	*/
	function setAuthorization($user, $password) {
		$this->httpConnection->setAuthorization($user, $password);
	} //setAuthorization
	
	/**
	* Specifies that a proxy is to be used to obtain the data
	* @param string The ip address or domain name of the proxy
	* @param string The path to the proxy
	* @param int The port that the proxy is listening on
	* @param int The timeout value for the connection
	* @param string The user name, if authentication is required
	* @param string The password, if authentication is required
	*/
	function setProxyConnection($host, $path = '/', $port = 80, $timeout = 0, $user = null, $password = null) {
	    require_once(PHP_HTTP_TOOLS_INCLUDE_PATH . 'php_http_proxy.php');
		
		$this->httpConnection =& new php_http_proxy($host, $path, $port, $timeout, $user, $password);
	} //setProxyConnection
	
	/**
	* Specifies basic authentication for the proxy
	* @param string The user name
	* @param string The password
	*/
	function setProxyAuthorization($user, $password) {
		$this->httpConnection->setProxyAuthorization($user, $password);
	} //setProxyAuthorization
} //php_http_connector

?>