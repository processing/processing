<?php
/**
* PHP HTTP Tools is a library for working with the http protocol
* HTTPExceptions is an HTTP Exceptions class
* @package php-http-tools
* @copyright (C) 2004 John Heinstein. All rights reserved
* @license http://www.gnu.org/copyleft/lesser.html LGPL License
* @author John Heinstein <johnkarl@nbnet.nb.ca>
* @link http://www.engageinteractive.com/php_http_tools/ PHP HTTP Tools Home Page
* PHP HTTP Tools are Free Software
**/

/** socket connection error */
define('HTTP_SOCKET_CONNECTION_ERR', 1); 
/** http transport error */
define('HTTP_TRANSPORT_ERR', 2); 

/**
* An HTTP Exceptions class (not yet implemented)
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class HTTPExceptions {
	function raiseException($errorNum, $errorString) {
		die('HTTP Exception: ' . $errorNum  .  "\n " . $errorString);
	} //raiseException
} //HTTPExceptions

?>