<?php
/**
* PHP HTTP Tools is a library for working with the http protocol
* php_http_proxy represents a basic http proxy
* @package php-http-tools
* @version 0.2-pre
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/php_http_tools/ PHP HTTP Tools Home Page
* PHP HTTP Tools are Free Software
**/
if (!defined('PHP_HTTP_TOOLS_INCLUDE_PATH')) {
	define('PHP_HTTP_TOOLS_INCLUDE_PATH', (dirname(__FILE__) . "/"));
}

require_once(PHP_HTTP_TOOLS_INCLUDE_PATH . 'php_http_client_generic.php');

/**
* An HTTP Proxy class
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_proxy extends php_http_client_generic {
	
	/**
	* HTTP Proxy constructor
	* @param string The client connection host name, with or without its protocol prefix
	* @param string The client connection path, not including the host name
	* @param int The port to establish the client connection on
	* @param int The timeout value for the client connection
	*/
	function php_http_proxy($host, $path = '/', $port = 80, $timeout = 0) {
		$this->php_http_client_generic($host, $path, $port, $timeout);		
		$this->setHeaders();		
	} //php_http_proxy
	
	/**
	* Sets the proxy timeout to the specified value
	* @param int The timeout value for the client connection
	*/
	function setTimeout($timeout) {
		$this->timeout = $timeout;
	} //setTimeout
	
	/**
	* Sets the proxy headers
	*/
	function setHeaders() {
		$this->setHeader('User-Agent', 'PHP-HTTP-Proxy-Client/0.1');		
		$this->setHeader('Connection', 'Close');
	} //setHeaders
	
	/**
	* Specifies a user name and password for basic proxy authentication
	* @param string The user name for proxy authentication
	* @param string The password for proxy authentication
	*/
	function setProxyAuthorization($user, $password) {
		$encodedChallengeResponse = 'Basic ' . base64_encode($this->user . ':' . $this->password);
		$this->setHeader('Proxy-Authorization', $encodedChallengeResponse);		
	} //setProxyAuthorization	

	/**
	* Handler for customizing the HTTP GET call
	* @param string The target url
	*/
	function get_custom($filename) {
		$url = $this->connection->formatHost($filename);
		$sep = strpos($url, '/');
		$targetHost = substr($url, 0, $sep);
		
		$this->setHeader('Host', $targetHost);
	} //get_custom
} //php_http_proxy

?>