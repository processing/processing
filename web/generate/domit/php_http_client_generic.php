<?php
/**
* PHP HTTP Tools is a library for working with the http protocol
* php_http_client_generic represents a basic http client
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

/** end-of-line character sequence as defined in HTTP spec */
define ('CRLF', "\r\n");
/** carriage return character */
define ('CR', "\r");
/** line feed character */
define ('LF', "\n");

//http read states for client
/** beginning read state */
define('HTTP_READ_STATE_BEGIN', 1); 
/** state when reading headers */
define('HTTP_READ_STATE_HEADERS', 2); 
/** state when reading body of message */
define('HTTP_READ_STATE_BODY', 3); 

require_once(PHP_HTTP_TOOLS_INCLUDE_PATH . 'php_http_exceptions.php');

/**

* An HTTP Request class
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_request {
	/** @var object A reference to the headers object */
	var $headers = null;	
	/** @var string The requested method, e.g. GET, POST, HEAD */
	var $requestMethod = 'POST';
	/** @var string The requested path */
	var $requestPath = '';
	/** @var string The requested protocol */
	var $protocol = 'HTTP';
	/** @var string The version of the requested protocol */
	var $protocolVersion= '1.1';
	
	/**
	* Returns the headers object
	* @return object The headers object
	*/
	function &getHeaders() {
		return $this->headers;
	} //getHeaders
	
	/**
	* Sets the header to the specified value
	* @param string The header name
	* @param string The header value
	* @param boolean True if multiple headers with the same name are allowed
	*/
	function setHeader($name, $value, $allowMultipleHeaders = false) {
		$this->headers->setHeader($name, $value, $allowMultipleHeaders);
	} //setHeader
	
	/**
	* Default method for setting headers; meant to be overridden in subclasses
	*/
	function setHeaders() {
		//you will want to override this method
		$this->setHeader('User-Agent', 'PHP-HTTP-Client(Generic)/0.1');
		$this->setHeader('Connection', 'Close');
	} //setHeaders
	
	/**
	* Sets the request method, e.g., GET 
	* @param string The name of the request method
	* @return boolean True if the version number is valid
	*/
	function setRequestMethod($method) {
		$method = strtoupper($method);
		
		switch ($method) {
			case 'POST':	
			case 'GET':
			case 'HEAD':
			case 'PUT':
				$this->requestMethod = $method;
				return true;
				break;
		}
		
		return false;
	} //setRequestMethod
	
	/**
	* Sets the request path, e.g., http://www.engageinteractive.com/domit/test.xml 
	* @param string The request path
	*/
	function setRequestPath($path) {
		$this->requestPath = $path;
	} //setRequestPath
	
	/**
	* Sets the version number of the protocol
	* @param string The version number
	* @return boolean True if the version number is valid
	*/
	function setProtocolVersion($version) {
		if (($version == '1.0') || ($version == '1.1')) {
			$this->protocolVersion = $version;
			return true;
		}
		
		return false;
	} //setProtocolVersion
	
	/**
	* Specifies a user name and password for basic authentication
	* @param string The user name
	* @param string The password
	*/
	function setAuthorization($user, $password) {
		$encodedChallengeResponse = 'Basic ' . base64_encode($this->user . ':' . $this->password);
		$this->setHeader('Authorization', $encodedChallengeResponse);
	} //setAuthorization
} //php_http_request


class php_http_client_generic extends php_http_request {
	/** @var object A reference to the connection object */
	var $connection;
	/** @var string True if response headers are to be generated as an object */
	var $responseHeadersAsObject = false;
	/** @var object The http response */
	var $response = null;
	/** @var string A list of event names that can be fired by the client */
	var $events = array('onRequest' => null, 'onRead' => null, 
						'onResponse' => null, 'onResponseHeaders' => null, 
						'onResponseBody' => null);
	
	/**
	* HTTP Client constructor
	* @param string The client connection host name, with or without its protocol prefix
	* @param string The client connection path, not including the host name
	* @param int The port to establish the client connection on
	* @param int The timeout value for the client connection
	*/
	function php_http_client_generic($host = '', $path = '/', $port = 80, $timeout = 0) {
		$this->connection =& new php_http_connection($host, $path, $port, $timeout);
		$this->headers =& new php_http_headers();
		$this->requestPath = $path;
		$this->response =& new php_http_response();
		$this->setHeaders();
	} //php_http_client_generic
	
	/**
	* Specifies that the response headers array should be generated
	* @param boolean True if the response headers array should be built
	*/
	function generateResponseHeadersAsObject($responseHeadersAsObject) {
		$this->responseHeadersAsObject = $responseHeadersAsObject;
		
		if ($responseHeadersAsObject) {
			$this->response->headers =& new php_http_headers();
		}
	} //generateResponseHeadersAsObject
	
	/**
	* Fires an http event that has been registered
	* @param string The name of the event, e.g., onRead
	* @param string The data to be passed to the event
	*/
	function fireEvent($target, $data) {
		if ($this->events[$target] != null) {
			call_user_func($this->events[$target], $data);
		}
	} //fireEvent
	
	/**
	* Sets which http events are to be fired
	* @param string The http event option to be set
	* @param string True if the event is to be fired
	* @param object A reference to a custom handler for the http event data
	*/
	function setHTTPEvent($option, $truthVal, $customHandler = null) {
		if ($customHandler != null) {
			$handler =& $customHandler;
		}
		else {
			$handler = array(&$this, 'defaultHTTPEventHandler');
		}

		switch($option) {
			case 'onRequest':				
			case 'onRead':
			case 'onResponse':
			case 'onResponseHeaders':
			case 'onResponseBody':
				$truthVal ? ($this->events[$option] =& $handler) : ($this->events[$option] = null);
				break;
		}
	} //setHTTPEvent
	
	/**
	* Evaluates whether the specified http event option is active
	* @param string The http event option to evaluate
	* @return boolean True if the specified option is active
	*/
	function getHTTPEvent($option) {
		switch($option) {
			case 'onRequest':
			case 'onRead':
			case 'onResponse':
			case 'onResponseHeaders':
			case 'onResponseBody':
				return ($this->events[$option] != null);
				break;
		}
	} //getHTTPEvent
	
	/**
	* The default http event handler; fired if no custom handler has been registered 
	* @param string The event data
	*/
	function defaultHTTPEventHandler($data) {
		$this->printHTML($data);
	} //defaultHTTPEventHandler
	
	/**
	* Prints the data to the browser as preformatted, htmlentified output
	* @param string The data to be printed
	*/
	function printHTML($html) {
		print('<pre>' . htmlentities($html)  . '</pre>');
	} //printHTML
	
	/**
	* Establishes a client connection
	*/
	function connect() {
		if (!$this->headers->headerExists('Host')) {
			$this->setHeader('Host', $this->connection->host);	
		}
		
		return $this->connection->connect();
	} //connect
	
	/**
	* Disconnects the current client connection if one exists
	*/
	function disconnect() {
		return $this->connection->disconnect();
	} //disconnect
	
	/**
	* Evaluated whether the current client is connected
	* @return boolean True if a connection exists
	*/
	function isConnected() {
		return $this->connection->isOpen();
	} //isConnected
	
	/**
	* Performs an HTTP GET
	* @param string The target url
	*/
	function &get($url) {
		$this->setRequestMethod('GET');
		$this->setRequestPath($url);
				
		$this->get_custom($url);
		
		$this->connect();
		
		$result = $this->send('');
		
		return $result;
	} //get
	
	/**
	* Handler for customizing the HTTP GET call
	* @param string The target url
	*/
	function get_custom($url) {
		//do nothing; meant to be overridden
	} //get_custom	
	
	/**
	* Performs an HTTP POST
	* @param string The posted data
	*/
	function &post($data) {
		$this->setRequestMethod('POST');
		$this->setHeader('Content-Type', 'text/html');
		$this->post_custom($data);
		
		$this->connect();
		
		return $this->send($data);
	} //post
	
	/**
	* Handler for customizing the HTTP POST call
	* @param string The post data
	*/
	function post_custom($data) {
		//do nothing; meant to be overridden
	} //post_custom	
	
	/**
	* Performs an HTTP HEAD
	* @param string The target url
	*/
	function &head($url) {
		$this->setRequestMethod('HEAD');
		$this->head_custom($url);
		
		$this->connect();
		
		return $this->send('');
	} //head
	
	/**
	* Handler for customizing the HTTP HEAD call
	* @param string The target url
	*/
	function head_custom($url) {
		//do nothing; meant to be overridden
	} //head_custom	
	
	/**
	* Sends data through the client connection
	* @return string The body of the http response
	*/
	function &send($message) {
		$conn =& $this->connection;

		if ($conn->isOpen()) {
			//build header info
			$request = $this->requestMethod  . ' '  . $this->requestPath . ' '  . $this->protocol . 
							'/' . $this->protocolVersion . CRLF;
			$request .= $this->headers->toString() . CRLF;
			$request .= $message;
			
			//init variables
			$response = $headers = $body = ''; 
			$readState = HTTP_READ_STATE_BEGIN;

			$this->fireEvent('onRequest', $request);
			
			//send request
			$connResource =& $conn->connection;			
			fputs ($connResource, $request);			

			//read response
			while (!feof($connResource)) {
				$data = fgets($connResource, 4096);
				$this->fireEvent('onRead', $data);
				
				switch ($readState) {
					case HTTP_READ_STATE_BEGIN:
						$this->response->statusLine = $data;
						$readState = HTTP_READ_STATE_HEADERS;
						break;
						
					case HTTP_READ_STATE_HEADERS:
						if (trim($data) == '') { //end of headers is signalled by a blank line
							$readState = HTTP_READ_STATE_BODY;
						}
						else {
							if ($this->responseHeadersAsObject) {
								$this->response->setUnformattedHeader($data);
							}
							else {
								$this->response->headers .= $data;
							}
						}
						break;
					
					case HTTP_READ_STATE_BODY:
						$this->response->message .= $data;
						break;
				}
			}
			
			$this->normalizeResponseIfChunked();
			
			$headerString = is_object($this->response->headers) ? 
						$this->response->headers->toString() : $this->response->headers;
			
			$this->fireEvent('onResponseHeaders', $headerString);
			$this->fireEvent('onResponseBody', $this->response->message);
			
			$this->fireEvent('onResponse', $this->response->headers . $this->response->message);
			
			return $this->response;
		}
		else {
			HTTPExceptions::raiseException(HTTP_SOCKET_CONNECTION_ERR, ('HTTP Transport Error - Unable to establish connection to host ' . 
						$conn->host));
		}
	} //send
	
	/**
	* Determines if response data is transfer encoding chunked, then decodes
	*/
	function normalizeResponseIfChunked() {
		if (($this->protocolVersion = '1.1') && (!$this->response->isResponseChunkDecoded)) {
			if ($this->responseHeadersAsObject) {
				if ($this->response->headers->headerExists('Transfer-Encoding') &&
					($this->response->headers->getHeader('Transfer-Encoding') == 'chunked')) {
					$this->response->message = $this->decodeChunkedData($this->response->getResponse());
					$this->response->isResponseChunkDecoded = true;
				}
			}
			else {
				if ((strpos($this->response->headers, 'Transfer-Encoding') !== false) && 
					(strpos($this->response->headers, 'chunked') !== false)){
					$this->response->message = $this->decodeChunkedData($this->response->getResponse());
					$this->response->isResponseChunkDecoded = true;
				}
			}
		}
	} //normalizeResponseIfChunked
	
	/**
	* Decodes data if transfer encoding chunked
	* @param string The encoded data
	* @return string The decoded data
	*/
	function decodeChunkedData($data) {
		$chunkStart = $chunkEnd = strpos($data, CRLF) + 2;
		$chunkLengthInHex = substr($data, 0, $chunkEnd);
		$chunkLength = hexdec(trim($chunkLengthInHex));
		
		$decodedData = '';
		
		while ($chunkLength > 0) {
			$chunkEnd = strpos($data, CRLF, ($chunkStart + $chunkLength));
			
			if (!$chunkEnd) { 
				//if the trailing CRLF is missing, return all the remaining data 
				$decodedData .= substr($data, $chunkStart);
				break;
			}
			
			$decodedData .= substr($data, $chunkStart, ($chunkEnd - $chunkStart));
			$chunkStart = $chunkEnd + 2;
			$chunkEnd = strpos($data, CRLF, $chunkStart) + 2;
			
			if (!$chunkEnd) break;
			
			$chunkLengthInHex = substr($data, $chunkStart, ($chunkEnd - $chunkStart));
			$chunkLength = hexdec(trim($chunkLengthInHex));
			$chunkStart = $chunkEnd;
		}
		
		return $decodedData;
	} //decodeChunkedData
} //php_http_client_generic


/**
* An HTTP Connection class
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_connection {
	/** @var object A reference to the current connection */
	var $connection = null;
	/** @var string The host of the connection */
	var $host;
	/** @var string The path of the connection */
	var $path;
	/** @var int The port of the connection */
	var $port;
	/** @var int The timeout value for the connection */
	var $timeout;
	/** @var int The error number of the connection */
	var $errorNumber = 0;
	/** @var string The error string of the connection */
	var $errorString = '';
	
	/**
	* HTTP Connection constructor
	* @param string The connection host name, with or without its protocol prefix
	* @param string The connection path, not including the host name
	* @param int The port to establish the client connection on
	* @param int The timeout value for the client connection
	*/
	function php_http_connection($host = '', $path = '/', $port = 80, $timeout = 0) {
		$this->host = $this->formatHost($host);
		$this->path = $this->formatPath($path);
		$this->port = $port;
		$this->timeout = $timeout;
	} //php_http_connection
	
	/**
	* Formats a host string by stripping off the http:// prefix
	* @param string The host name
	* @return string The formatted host name
	*/
	function formatHost($hostString) {
		$hasProtocol = (substr(strtoupper($hostString), 0, 7) == 'HTTP://');
	
		if ($hasProtocol) {
			$hostString = substr($hostString, 7);
		}
		
		return $hostString;
	} //formatHost
	
	/**
	* Formats a path string
	* @param string The path
	* @return string The formatted path
	*/
	function formatPath($pathString) {
		if (($pathString == '') || ($pathString == null)) {
			$pathString = '/';
		}
		
		return $pathString;
	} //formatPath
	
	/**
	* Establishes a socket connection
	* @return boolean True if the connection was successful
	*/
	function connect() {
		if ($this->timeout == 0) {
			$this->connection = @fsockopen($this->host, $this->port, $errorNumber, $errorString);
		}
		else {
			$this->connection = @fsockopen($this->host, $this->port, $errorNumber, $errorString, $this->timeout);
		}
		
		$this->errorNumber = $errorNumber;
		$this->errorString = $errorString;
		
		return is_resource($this->connection);
	} //connect
	
	/**
	* Determines whether the connection is still open
	* @return boolean True if the connection is still open
	*/
	function isOpen() {
		return (is_resource($this->connection) && (!feof($this->connection)));
	} //isOpen
	
	/**
	* Disconnects the current connection
	* @return boolean True if the connection has been disconnected
	*/
	function disconnect() {
		fclose($this->connection);
		$this->connection = null;
		return true;
	} //disconnect
} //php_http_connection

/**
* An HTTP Headers class
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_headers {
	/** @var object An array of headers */
	var $headers;
	
	/**
	* HTTP Headers constructor
	*/
	function php_http_headers() {
		$this->headers = array();
	} //php_http_headers
	
	/**
	* Returns the specified header value
	* @param string The header name
	* @return mixed The header value, or an array of header values
	*/
	function &getHeader($name) {
		if ($this->headerExists($name))	{	
			return $this->headers[$name];
		}
		
		return false;
	} //getHeader
	
	/**
	* Sets the named header to the specified value
	* @param string The header name
	* @param string The header value
	* @param boolean True if multiple headers with the same name are allowed
	*/
	function setHeader($name, $value, $allowMultipleHeaders = false) {
		if ($allowMultipleHeaders) {
			if (isset($this->headers[$name])) {
				if (is_array($this->headers[$name])) {
					$this->headers[$name][count($this->headers)] = $value;
				}
				else {
					$tempVal = $this->headers[$name];
					$this->headers[$name] = array($tempVal, $value);
				}
			}
			else {
				$this->headers[$name] = array();
				$this->headers[$name][0] = $value;
			}
		}
		else {
			$this->headers[$name] = $value;
		}
	} //setHeader
	
	/**
	* Determines whether the specified header exists
	* @param string The header name
	* @return boolean True if the specified header exists
	*/
	function headerExists($name) {
		return isset($this->headers[$name]);
	} //headerExists
	
	/**
	* Removes the specified header
	* @param string The header name
	* @return boolean True if the specified header has been removed
	*/
	function removeHeader($name) {
		if ($this->headerExists($name))	{	
			unset($this->headers[$name]);
			return true;
		}
		
		return false;
	} //removeHeader
	
	/**
	* Returns a reference to the headers array
	* @return array The headers array
	*/
	function getHeaders() {
		return $this->headers;
	} //getHeaders
	
	/**
	* Returns a list of existing headers names
	* @return array A list of existing header names
	*/
	function getHeaderList() {
		return array_keys($this->headers);
	} //getHeaderList
	
	/**
	* Returns a string representation of the headers array
	* @return string A string representation of the headers array
	*/
	function toString() {
		$retString = '';
		
		foreach ($this->headers as $key => $value) {
			if (is_array($value)) {
				foreach ($value as $key2 => $value2) {
					$retString .= $key . ': ' . $value2 . CRLF;
				}
			}
			else {
				$retString .= $key . ': ' . $value . CRLF;
			}
		}
		
		return $retString;
	} //toString
} //php_http_headers

/**
* An HTTP Response class
*
* @package php-http-tools
* @author John Heinstein <johnkarl@nbnet.nb.ca>
*/
class php_http_response {
	/** @var string Response number */
	var $statusLine = '';
	/** @var mixed Response headers, either as object or string */
	var $headers = '';	
	/** @var string Response message */
	var $message = '';
	/** @var boolean True if the chunked transfer-encoding of the response has been decoded */	
	var $isResponseChunkDecoded = false;

	/**
	* Returns a reference to the headers array
	* @return array The headers array
	*/
	function getResponse() {
		return $this->message;
	} //getResponse
	
	/**
	* Returns the response status line
	* @return string The response status line
	*/
	function getStatusLine() {
		return $this->statusLine;
	} //getStatusLine
	
	/**
	* Returns the response status code
	* @return int The response status code
	*/
	function getStatusCode() {
		$statusArray = split(' ', $this->statusLine);
		
		if (count($statusArray > 1)) {
			return intval($statusArray[1], 10);
		}
		
		return -1;
	} //getStatusCode
	
	/**
	* Returns a reference to the headers array
	* @return array The headers array
	*/
	function &getHeaders() {
		return $this->headers;
	} //getHeaders
	
	/**
	* Converts a header string into a key value pair and sets header
	*/
	function setUnformattedHeader($headerString) {
		$colonIndex = strpos($headerString, ':');

		if ($colonIndex !== false) {
			$key = trim(substr($headerString, 0, $colonIndex));
			$value = trim(substr($headerString, ($colonIndex + 1)));
			$this->headers->setHeader($key, $value, true);
		}
	} //setUnformattedHeader	
} //php_http_response

?>