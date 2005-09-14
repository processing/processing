<?php
/**
 * Php.XPath
 *
 * +======================================================================================================+
 * | A php class for searching an XML document using XPath, and making modifications using a DOM 
 * | style API. Does not require the DOM XML PHP library. 
 * |
 * +======================================================================================================+
 * | What Is XPath:
 * | --------------
 * | - "What SQL is for a relational database, XPath is for an XML document." -- Sam Blum
 * | - "The primary purpose of XPath is to address parts of an XML document. In support of this 
 * |    primary purpose, it also provides basic facilities for manipulting it." -- W3C
 * | 
 * | XPath in action and a very nice intro is under:
 * |    http://www.zvon.org/xxl/XPathTutorial/General/examples.html
 * | Specs Can be found under:
 * |    http://www.w3.org/TR/xpath     W3C XPath Recommendation 
 * |    http://www.w3.org/TR/xpath20   W3C XPath Recommendation 
 * |
 * | NOTE: Most of the XPath-spec has been realized, but not all. Usually this should not be
 * |       problem as the missing part is either rarely used or it's simpler to do with PHP itself.
 * +------------------------------------------------------------------------------------------------------+
 * | Requires PHP version  4.0.5 and up
 * +------------------------------------------------------------------------------------------------------+
 * | Main Active Authors:
 * | --------------------
 * | Nigel Swinson <nigelswinson@users.sourceforge.net>
 * |   Started around 2001-07, saved phpxml from near death and renamed to Php.XPath
 * |   Restructured XPath code to stay in line with XPath spec.
 * | Sam Blum <bs_php@infeer.com>
 * |   Started around 2001-09 1st major restruct (V2.0) and testbench initiator.   
 * |   2nd (V3.0) major rewrite in 2002-02
 * | Daniel Allen <bigredlinux@yahoo.com>
 * |   Started around 2001-10 working to make Php.XPath adhere to specs 
 * | Main Former Author: Michael P. Mehl <mpm@phpxml.org>
 * |   Inital creator of V 1.0. Stoped activities around 2001-03        
 * +------------------------------------------------------------------------------------------------------+
 * | Code Structure:
 * | --------------_
 * | The class is split into 3 main objects. To keep usability easy all 3 
 * | objects are in this file (but may be split in 3 file in future).
 * |   +-------------+ 
 * |   |  XPathBase  | XPathBase holds general and debugging functions. 
 * |   +------+------+
 * |          v      
 * |   +-------------+ XPathEngine is the implementation of the W3C XPath spec. It contains the 
 * |   | XPathEngine | XML-import (parser), -export  and can handle xPathQueries. It's a fully 
 * |   +------+------+ functional class but has no functions to modify the XML-document (see following).
 * |          v      
 * |   +-------------+ 
 * |   |    XPath    | XPath extends the functionality with actions to modify the XML-document.
 * |   +-------------+ We tryed to implement a DOM - like interface.
 * +------------------------------------------------------------------------------------------------------+
 * | Usage:
 * | ------
 * | Scroll to the end of this php file and you will find a short sample code to get you started
 * +------------------------------------------------------------------------------------------------------+
 * | Glossary:
 * | ---------
 * | To understand how to use the functions and to pass the right parameters, read following:
 * |     
 * | Document: (full node tree, XML-tree)
 * |     After a XML-source has been imported and parsed, it's stored as a tree of nodes sometimes 
 * |     refered to as 'document'.
 * |     
 * | AbsoluteXPath: (xPath, xPathSet)
 * |     A absolute XPath is a string. It 'points' to *one* node in the XML-document. We use the
 * |     term 'absolute' to emphasise that it is not an xPath-query (see xPathQuery). A valid xPath 
 * |     has the form like '/AAA[1]/BBB[2]/CCC[1]'. Usually functions that require a node (see Node) 
 * |     will also accept an abs. XPath.
 * |     
 * | Node: (node, nodeSet, node-tree)
 * |     Some funtions require or return a node (or a whole node-tree). Nodes are only used with the 
 * |     XPath-interface and have an internal structure. Every node in a XML document has a unique 
 * |     corresponding abs. xPath. That's why public functions that accept a node, will usually also 
 * |     accept a abs. xPath (a string) 'pointing' to an existing node (see absolutXPath).
 * |     
 * | XPathQuery: (xquery, query)
 * |     A xPath-query is a string that is matched against the XML-document. The result of the match 
 * |     is a xPathSet (vector of xPath's). It's always possible to pass a single absoluteXPath 
 * |     instead of a xPath-query. A valid xPathQuery could look like this:
 * |     '//XXX/*[contains(., "foo")]/..' (See the link in 'What Is XPath' to learn more).
 * |     
 * |     
 * +------------------------------------------------------------------------------------------------------+
 * | Internals:
 * | ----------
 * | - The Node Tree
 * |   -------------
 * | A central role of the package is how the XML-data is stored. The whole data is in a node-tree.
 * | A node can be seen as the equvalent to a tag in the XML soure with some extra info.
 * | For instance the following XML 
 * |                        <AAA foo="x">***<BBB/><CCC/>**<BBB/>*</AAA>
 * | Would produce folowing node-tree:
 * |                              'super-root'      <-- $nodeRoot (Very handy)  
 * |                                    |                                           
 * |             'depth' 0            AAA[1]        <-- top node. The 'textParts' of this node would be
 * |                                /   |   \                     'textParts' => array('***','','**','*')
 * |             'depth' 1     BBB[1] CCC[1] BBB[2]               (NOTE: Is always size of child nodes+1)
 * | - The Node
 * |   --------
 * | The node itself is an structure desiged mainly to be used in connection with the interface of PHP.XPath.
 * | That means it's possible for functions to return a sub-node-tree that can be used as input of an other 
 * | PHP.XPath function.
 * | 
 * | The main structure of a node is:
 * |   $node = array(
 * |     'name'        => '',      # The tag name. E.g. In <FOO bar="aaa"/> it would be 'FOO'
 * |     'attributes'  => array(), # The attributes of the tag E.g. In <FOO bar="aaa"/> it would be array('bar'=>'aaa')
 * |     'textParts'   => array(), # Array of text parts surrounding the children E.g. <FOO>aa<A>bb<B/>cc</A>dd</FOO> -> array('aa','bb','cc','dd')
 * |     'childNodes'  => array(), # Array of refences (pointers) to child nodes.
 * |     
 * | For optimisation reasions some additional data is stored in the node too:
 * |     'parentNode'  => NULL     # Reference (pointer) to the parent node (or NULL if it's 'super root')
 * |     'depth'       => 0,       # The tag depth (or tree level) starting with the root tag at 0.
 * |     'pos'         => 0,       # Is the zero-based position this node has in the parent's 'childNodes'-list.
 * |     'contextPos'  => 1,       # Is the one-based position this node has by counting the siblings tags (tags with same name)
 * |     'xpath'       => ''       # Is the abs. XPath to this node.
 * |     'generated_id'=> ''       # The id returned for this node by generate-id() (attribute and text nodes not supported)
 * | 
 * | - The NodeIndex
 * |   -------------
 * | Every node in the tree has an absolute XPath. E.g '/AAA[1]/BBB[2]' the $nodeIndex is a hash array
 * | to all the nodes in the node-tree. The key used is the absolute XPath (a string).
 * |    
 * +------------------------------------------------------------------------------------------------------+
 * | License:
 * | --------
 * | The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License"); 
 * | you may not use this file except in compliance with the License. You may obtain a copy of the 
 * | License at http://www.mozilla.org/MPL/ 
 * | 
 * | Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * | OF ANY KIND, either express or implied. See the License for the specific language governing 
 * | rights and limitations under the License. 
 * |
 * | The Original Code is <phpXML/>. 
 * | 
 * | The Initial Developer of the Original Code is Michael P. Mehl. Portions created by Michael 
 * | P. Mehl are Copyright (C) 2001 Michael P. Mehl. All Rights Reserved.
 * |
 * | Contributor(s): N.Swinson / S.Blum / D.Allen
 * | 
 * | Alternatively, the contents of this file may be used under the terms of either of the GNU 
 * | General Public License Version 2 or later (the "GPL"), or the GNU Lesser General Public 
 * | License Version 2.1 or later (the "LGPL"), in which case the provisions of the GPL or the 
 * | LGPL License are applicable instead of those above.  If you wish to allow use of your version 
 * | of this file only under the terms of the GPL or the LGPL License and not to allow others to 
 * | use your version of this file under the MPL, indicate your decision by deleting the 
 * | provisions above and replace them with the notice and other provisions required by the 
 * | GPL or the LGPL License.  If you do not delete the provisions above, a recipient may use 
 * | your version of this file under either the MPL, the GPL or the LGPL License. 
 * | 
 * +======================================================================================================+
 *
 * @author  S.Blum / N.Swinson / D.Allen / (P.Mehl)
 * @link    http://sourceforge.net/projects/phpxpath/
 * @version 3.5
 * @CVS $Id: XPath.class.php,v 1.148 2004/08/13 11:47:36 nigelswinson Exp $
 */

// Include guard, protects file being included twice
$ConstantName = 'INCLUDED_'.strtoupper(__FILE__);
if (defined($ConstantName)) return;
define($ConstantName,1, TRUE);

/************************************************************************************************
* ===============================================================================================
*                               X P a t h B a s e  -  Class                                      
* ===============================================================================================
************************************************************************************************/
class XPathBase {
  var $_lastError;
  
  // As debugging of the xml parse is spread across several functions, we need to make this a member.
  var $bDebugXmlParse = FALSE;

  // Used to help navigate through the begin/end debug calls
  var $iDebugNextLinkNumber = 1;
  var $aDebugOpenLinks = array();
  var $aDebugFunctions = array(
		//'_evaluateStep',
          //'_evaluatePrimaryExpr',
          //'_evaluateExpr',
          //'_evaluateStep',
          //'_checkPredicates',
          //'_evaluateFunction',
          //'_evaluateOperator',
          //'_evaluatePathExpr',
               );

  /**
   * Constructor
   */
  function XPathBase() {
    # $this->bDebugXmlParse = TRUE;
    $this->properties['verboseLevel'] = 1;  // 0=silent, 1 and above produce verbose output (an echo to screen). 
    
    if (!isSet($_ENV)) {  // Note: $_ENV introduced in 4.1.0. In earlier versions, use $HTTP_ENV_VARS.
      $_ENV = $GLOBALS['HTTP_ENV_VARS'];
    }
    
    // Windows 95/98 do not support file locking. Detecting OS (Operation System) and setting the 
    // properties['OS_supports_flock'] to FALSE if win 95/98 is detected. 
    // This will surpress the file locking error reported from win 98 users when exportToFile() is called.
    // May have to add more OS's to the list in future (Macs?).
    // ### Note that it's only the FAT and NFS file systems that are really a problem.  NTFS and
    // the latest php libs do support flock()
    $_ENV['OS'] = isSet($_ENV['OS']) ? $_ENV['OS'] : 'Unknown OS';
    switch ($_ENV['OS']) { 
      case 'Windows_95':
      case 'Windows_98':
      case 'Unknown OS':
        // should catch Mac OS X compatible environment 
        if (!empty($_SERVER['SERVER_SOFTWARE']) 
            && preg_match('/Darwin/',$_SERVER['SERVER_SOFTWARE'])) { 
           // fall-through 
        } else { 
           $this->properties['OS_supports_flock'] = FALSE; 
           break; 
        }
      default:
        $this->properties['OS_supports_flock'] = TRUE;
    }
  }
  
  
  /**
   * Resets the object so it's able to take a new xml sting/file
   *
   * Constructing objects is slow.  If you can, reuse ones that you have used already
   * by using this reset() function.
   */
  function reset() {
    $this->_lastError   = '';
  }
  
  //-----------------------------------------------------------------------------------------
  // XPathBase                    ------  Helpers  ------                                    
  //-----------------------------------------------------------------------------------------
  
  /**
   * This method checks the right amount and match of brackets
   *
   * @param     $term (string) String in which is checked.
   * @return          (bool)   TRUE: OK / FALSE: KO  
   */
  function _bracketsCheck($term) {
    $leng = strlen($term);
    $brackets = 0;
    $bracketMisscount = $bracketMissmatsh = FALSE;
    $stack = array();
    for ($i=0; $i<$leng; $i++) {
      switch ($term[$i]) {
        case '(' : 
        case '[' : 
          $stack[$brackets] = $term[$i]; 
          $brackets++; 
          break;
        case ')': 
          $brackets--;
          if ($brackets<0) {
            $bracketMisscount = TRUE;
            break 2;
          }
          if ($stack[$brackets] != '(') {
            $bracketMissmatsh = TRUE;
            break 2;
          }
          break;
        case ']' : 
          $brackets--;
          if ($brackets<0) {
            $bracketMisscount = TRUE;
            break 2;
          }
          if ($stack[$brackets] != '[') {
            $bracketMissmatsh = TRUE;
            break 2;
          }
          break;
      }
    }
    // Check whether we had a valid number of brackets.
    if ($brackets != 0) $bracketMisscount = TRUE;
    if ($bracketMisscount || $bracketMissmatsh) {
      return FALSE;
    }
    return TRUE;
  }
  
  /**
   * Looks for a string within another string -- BUT the search-string must be located *outside* of any brackets.
   *
   * This method looks for a string within another string. Brackets in the
   * string the method is looking through will be respected, which means that
   * only if the string the method is looking for is located outside of
   * brackets, the search will be successful.
   *
   * @param     $term       (string) String in which the search shall take place.
   * @param     $expression (string) String that should be searched.
   * @return                (int)    This method returns -1 if no string was found, 
   *                                 otherwise the offset at which the string was found.
   */
  function _searchString($term, $expression) {
    $bracketCounter = 0; // Record where we are in the brackets. 
    $leng = strlen($term);
    $exprLeng = strlen($expression);
    for ($i=0; $i<$leng; $i++) {
      $char = $term[$i];
      if ($char=='(' || $char=='[') {
        $bracketCounter++;
        continue;
      }
      elseif ($char==')' || $char==']') {
        $bracketCounter--;
      }
      if ($bracketCounter == 0) {
        // Check whether we can find the expression at this index.
        if (substr($term, $i, $exprLeng) == $expression) return $i;
      }
    }
    // Nothing was found.
    return (-1);
  }
  
  /**
   * Split a string by a searator-string -- BUT the separator-string must be located *outside* of any brackets.
   * 
   * Returns an array of strings, each of which is a substring of string formed 
   * by splitting it on boundaries formed by the string separator. 
   *
   * @param     $separator  (string) String that should be searched.
   * @param     $term       (string) String in which the search shall take place.
   * @return                (array)  see above
   */
  function _bracketExplode($separator, $term) {
    // Note that it doesn't make sense for $separator to itself contain (,),[ or ],
    // but as this is a private function we should be ok.
    $resultArr   = array();
    $bracketCounter = 0;  // Record where we are in the brackets. 
    do { // BEGIN try block
      // Check if any separator is in the term
      $sepLeng =  strlen($separator);
      if (strpos($term, $separator)===FALSE) { // no separator found so end now
        $resultArr[] = $term;
        break; // try-block
      }
      
      // Make a substitute separator out of 'unused chars'.
      $substituteSep = str_repeat(chr(2), $sepLeng);
      
      // Now determine the first bracket '(' or '['.
      $tmp1 = strpos($term, '(');
      $tmp2 = strpos($term, '[');
      if ($tmp1===FALSE) {
        $startAt = (int)$tmp2;
      } elseif ($tmp2===FALSE) {
        $startAt = (int)$tmp1;
      } else {
        $startAt = min($tmp1, $tmp2);
      }
      
      // Get prefix string part before the first bracket.
      $preStr = substr($term, 0, $startAt);
      // Substitute separator in prefix string.
      $preStr = str_replace($separator, $substituteSep, $preStr);
      
      // Now get the rest-string (postfix string)
      $postStr = substr($term, $startAt);
      // Go all the way through the rest-string.
      $strLeng = strlen($postStr);
      for ($i=0; $i < $strLeng; $i++) {
        $char = $postStr[$i];
        // Spot (,),[,] and modify our bracket counter.  Note there is an
        // assumption here that you don't have a string(with[mis)matched]brackets.
        // This should be ok as the dodgy string will be detected elsewhere.
        if ($char=='(' || $char=='[') {
          $bracketCounter++;
          continue;
        } 
        elseif ($char==')' || $char==']') {
          $bracketCounter--;
        }
        // If no brackets surround us check for separator
        if ($bracketCounter == 0) {
          // Check whether we can find the expression starting at this index.
          if ((substr($postStr, $i, $sepLeng) == $separator)) {
            // Substitute the found separator 
            for ($j=0; $j<$sepLeng; $j++) {
              $postStr[$i+$j] = $substituteSep[$j];
            }
          }
        }
      }
      // Now explod using the substitute separator as key.
      $resultArr = explode($substituteSep, $preStr . $postStr);
    } while (FALSE); // End try block
    // Return the results that we found. May be a array with 1 entry.
    return $resultArr;
  }

  /**
   * Split a string at it's groups, ie bracketed expressions
   * 
   * Returns an array of strings, when concatenated together would produce the original
   * string.  ie a(b)cde(f)(g) would map to:
   * array ('a', '(b)', cde', '(f)', '(g)')
   *
   * @param     $string  (string) The string to process
   * @param     $open    (string) The substring for the open of a group
   * @param     $close   (string) The substring for the close of a group
   * @return             (array)  The parsed string, see above
   */
  function _getEndGroups($string, $open='[', $close=']') {
    // Note that it doesn't make sense for $separator to itself contain (,),[ or ],
    // but as this is a private function we should be ok.
    $resultArr   = array();
    do { // BEGIN try block
      // Check if we have both an open and a close tag      
      if (empty($open) and empty($close)) { // no separator found so end now
        $resultArr[] = $string;
        break; // try-block
      }

      if (empty($string)) {
        $resultArr[] = $string;
        break; // try-block
      }

      
      while (!empty($string)) {
        // Now determine the first bracket '(' or '['.
        $openPos = strpos($string, $open);
        $closePos = strpos($string, $close);
        if ($openPos===FALSE || $closePos===FALSE) {
          // Oh, no more groups to be found then.  Quit
          $resultArr[] = $string;
          break;
        }

        // Sanity check
        if ($openPos > $closePos) {
          // Malformed string, dump the rest and quit.
          $resultArr[] = $string;
          break;
        }

        // Get prefix string part before the first bracket.
        $preStr = substr($string, 0, $openPos);
        // This is the first string that will go in our output
        if (!empty($preStr))
          $resultArr[] = $preStr;

        // Skip over what we've proceed, including the open char
        $string = substr($string, $openPos + 1 - strlen($string));

        // Find the next open char and adjust our close char
//echo "close: $closePos\nopen: $openPos\n\n";
        $closePos -= $openPos + 1;
        $openPos = strpos($string, $open);
//echo "close: $closePos\nopen: $openPos\n\n";

        // While we have found nesting...
        while ($openPos && $closePos && ($closePos > $openPos)) {
          // Find another close pos after the one we are looking at
          $closePos = strpos($string, $close, $closePos + 1);
          // And skip our open
          $openPos = strpos($string, $open, $openPos + 1);
        }
//echo "close: $closePos\nopen: $openPos\n\n";

        // If we now have a close pos, then it's the end of the group.
        if ($closePos === FALSE) {
          // We didn't... so bail dumping what was left
          $resultArr[] = $open.$string;
          break;
        }

        // We did, so we can extract the group
        $resultArr[] = $open.substr($string, 0, $closePos + 1);
        // Skip what we have processed
        $string = substr($string, $closePos + 1);
      }
    } while (FALSE); // End try block
    // Return the results that we found. May be a array with 1 entry.
    return $resultArr;
  }
  
  /**
   * Retrieves a substring before a delimiter.
   *
   * This method retrieves everything from a string before a given delimiter,
   * not including the delimiter.
   *
   * @param     $string     (string) String, from which the substring should be extracted.
   * @param     $delimiter  (string) String containing the delimiter to use.
   * @return                (string) Substring from the original string before the delimiter.
   * @see       _afterstr()
   */
  function _prestr(&$string, $delimiter, $offset=0) {
    // Return the substring.
    $offset = ($offset<0) ? 0 : $offset;
    $pos = strpos($string, $delimiter, $offset);
    if ($pos===FALSE) return $string; else return substr($string, 0, $pos);
  }
  
  /**
   * Retrieves a substring after a delimiter.
   *
   * This method retrieves everything from a string after a given delimiter,
   * not including the delimiter.
   *
   * @param     $string     (string) String, from which the substring should be extracted.
   * @param     $delimiter  (string) String containing the delimiter to use.
   * @return                (string) Substring from the original string after the delimiter.
   * @see       _prestr()
   */
  function _afterstr($string, $delimiter, $offset=0) {
    $offset = ($offset<0) ? 0 : $offset;
    // Return the substring.
    return substr($string, strpos($string, $delimiter, $offset) + strlen($delimiter));
  }
  
  //-----------------------------------------------------------------------------------------
  // XPathBase                ------  Debug Stuff  ------                                    
  //-----------------------------------------------------------------------------------------
  
  /**
   * Alter the verbose (error) level reporting.
   *
   * Pass an int. >0 to turn on, 0 to turn off.  The higher the number, the 
   * higher the level of verbosity. By default, the class has a verbose level 
   * of 1.
   *
   * @param $levelOfVerbosity (int) default is 1 = on
   */
  function setVerbose($levelOfVerbosity = 1) {
    $level = -1;
    if ($levelOfVerbosity === TRUE) {
      $level = 1;
    } elseif ($levelOfVerbosity === FALSE) {
      $level = 0;
    } elseif (is_numeric($levelOfVerbosity)) {
      $level = $levelOfVerbosity;
    }
    if ($level >= 0) $this->properties['verboseLevel'] = $levelOfVerbosity;
  }
   
  /**
   * Returns the last occured error message.
   *
   * @access public
   * @return string (may be empty if there was no error at all)
   * @see    _setLastError(), _lastError
   */
  function getLastError() {
    return $this->_lastError;
  }
  
  /**
   * Creates a textual error message and sets it. 
   * 
   * example: 'XPath error in THIS_FILE_NAME:LINE. Message: YOUR_MESSAGE';
   * 
   * I don't think the message should include any markup because not everyone wants to debug 
   * into the browser window.
   * 
   * You should call _displayError() rather than _setLastError() if you would like the message,
   * dependant on their verbose settings, echoed to the screen.
   * 
   * @param $message (string) a textual error message default is ''
   * @param $line    (int)    the line number where the error occured, use __LINE__
   * @see getLastError()
   */
  function _setLastError($message='', $line='-', $file='-') {
    $this->_lastError = 'XPath error in ' . basename($file) . ':' . $line . '. Message: ' . $message;
  }
  
  /**
   * Displays an error message.
   *
   * This method displays an error messages depending on the users verbose settings 
   * and sets the last error message.  
   *
   * If also possibly stops the execution of the script.
   * ### Terminate should not be allowed --fab.  Should it??  N.S.
   *
   * @param $message    (string)  Error message to be displayed.
   * @param $lineNumber (int)     line number given by __LINE__
   * @param $terminate  (bool)    (default TURE) End the execution of this script.
   */
  function _displayError($message, $lineNumber='-', $file='-', $terminate=TRUE) {
    // Display the error message.
    $err = '<b>XPath error in '.basename($file).':'.$lineNumber.'</b> '.$message."<br \>\n";
    $this->_setLastError($message, $lineNumber, $file);
    if (($this->properties['verboseLevel'] > 0) OR ($terminate)) echo $err;
    // End the execution of this script.
    if ($terminate) exit;
  }

  /**
   * Displays a diagnostic message
   *
   * This method displays an error messages
   *
   * @param $message    (string)  Error message to be displayed.
   * @param $lineNumber (int)     line number given by __LINE__
   */
  function _displayMessage($message, $lineNumber='-', $file='-') {
    // Display the error message.
    $err = '<b>XPath message from '.basename($file).':'.$lineNumber.'</b> '.$message."<br \>\n";
    if ($this->properties['verboseLevel'] > 0) echo $err;
  }
  
  /**
   * Called to begin the debug run of a function.
   *
   * This method starts a <DIV><PRE> tag so that the entry to this function
   * is clear to the debugging user.  Call _closeDebugFunction() at the
   * end of the function to create a clean box round the function call.
   *
   * @author    Nigel Swinson <nigelswinson@users.sourceforge.net>
   * @author    Sam   Blum    <bs_php@infeer.com>
   * @param     $functionName (string) the name of the function we are beginning to debug
   * @return                  (array)  the output from the microtime() function.
   * @see       _closeDebugFunction()
   */
  function _beginDebugFunction($functionName) {
    $fileName = basename(__FILE__);
    static $color = array('green','blue','red','lime','fuchsia', 'aqua');
    static $colIndex = -1;
    $colIndex++;
    echo '<div style="clear:both" align="left"> ';
    echo '<pre STYLE="border:solid thin '. $color[$colIndex % 6] . '; padding:5">';
    echo '<a style="float:right;margin:5px" name="'.$this->iDebugNextLinkNumber.'Open" href="#'.$this->iDebugNextLinkNumber.'Close">Function Close '.$this->iDebugNextLinkNumber.'</a>';
    echo "<STRONG>{$fileName} : {$functionName}</STRONG>";
    echo '<hr style="clear:both">';
    array_push($this->aDebugOpenLinks, $this->iDebugNextLinkNumber);
    $this->iDebugNextLinkNumber++;
    return microtime();
  }
  
  /**
   * Called to end the debug run of a function.
   *
   * This method ends a <DIV><PRE> block and reports the time since $aStartTime
   * is clear to the debugging user.
   *
   * @author    Nigel Swinson <nigelswinson@users.sourceforge.net>
   * @param     $aStartTime   (array) the time that the function call was started.
   * @param     $return_value (mixed) the return value from the function call that 
   *                                  we are debugging
   */
  function _closeDebugFunction($aStartTime, $returnValue = "") {
    echo "<hr>";
    $iOpenLinkNumber = array_pop($this->aDebugOpenLinks);
    echo '<a style="float:right" name="'.$iOpenLinkNumber.'Close" href="#'.$iOpenLinkNumber.'Open">Function Open '.$iOpenLinkNumber.'</a>';
    if (isSet($returnValue)) {
      if (is_array($returnValue))
        echo "Return Value: ".print_r($returnValue)."\n";
      else if (is_numeric($returnValue)) 
        echo "Return Value: ".(string)$returnValue."\n";
      else if (is_bool($returnValue)) 
        echo "Return Value: ".($returnValue ? "TRUE" : "FALSE")."\n";
      else 
        echo "Return Value: \"".htmlspecialchars($returnValue)."\"\n";
    }
    $this->_profileFunction($aStartTime, "Function took");
    echo '<br style="clear:both">';
    echo " \n</pre></div>";
  }
  
  /**
   * Call to return time since start of function for Profiling
   *
   * @param     $aStartTime  (array)  the time that the function call was started.
   * @param     $alertString (string) the string to describe what has just finished happening
   */
  function _profileFunction($aStartTime, $alertString) {
    // Print the time it took to call this function.
    $now   = explode(' ', microtime());
    $last  = explode(' ', $aStartTime);
    $delta = (round( (($now[1] - $last[1]) + ($now[0] - $last[0]))*1000 ));
    echo "\n{$alertString} <strong>{$delta} ms</strong>";
  }

  /**
   * Echo an XPath context for diagnostic purposes
   *
   * @param $context   (array)   An XPath context
   */
  function _printContext($context) {
    echo "{$context['nodePath']}({$context['pos']}/{$context['size']})";
  }
  
  /**
   * This is a debug helper function. It dumps the node-tree as HTML
   *
   * *QUICK AND DIRTY*. Needs some polishing.
   *
   * @param $node   (array)   A node 
   * @param $indent (string) (optional, default=''). For internal recursive calls.
   */
  function _treeDump($node, $indent = '') {
    $out = '';
    
    // Get rid of recursion
    $parentName = empty($node['parentNode']) ? "SUPER ROOT" :  $node['parentNode']['name'];
    unset($node['parentNode']);
    $node['parentNode'] = $parentName ;
    
    $out .= "NODE[{$node['name']}]\n";
    
    foreach($node as $key => $val) {
      if ($key === 'childNodes') continue;
      if (is_Array($val)) {
        $out .= $indent . "  [{$key}]\n" . arrayToStr($val, $indent . '    ');
      } else {
        $out .= $indent . "  [{$key}] => '{$val}' \n";
      }
    }
    
    if (!empty($node['childNodes'])) {
      $out .= $indent . "  ['childNodes'] (Size = ".sizeOf($node['childNodes']).")\n";
      foreach($node['childNodes'] as $key => $childNode) {
        $out .= $indent . "     [$key] => " . $this->_treeDump($childNode, $indent . '       ') . "\n";
      }
    }
    
    if (empty($indent)) {
      return "<pre>" . htmlspecialchars($out) . "</pre>";
    }
    return $out;
  }
} // END OF CLASS XPathBase


/************************************************************************************************
* ===============================================================================================
*                             X P a t h E n g i n e  -  Class                                    
* ===============================================================================================
************************************************************************************************/

class XPathEngine extends XPathBase {
  
  // List of supported XPath axes.
  // What a stupid idea from W3C to take axes name containing a '-' (dash)
  // NOTE: We replace the '-' with '_' to avoid the conflict with the minus operator.
  //       We will then do the same on the users Xpath querys
  //   -sibling => _sibling
  //   -or-     =>     _or_
  //  
  // This array contains a list of all valid axes that can be evaluated in an
  // XPath query.
  var $axes = array ( 'ancestor', 'ancestor_or_self', 'attribute', 'child', 'descendant', 
                        'descendant_or_self', 'following', 'following_sibling',  
                        'namespace', 'parent', 'preceding', 'preceding_sibling', 'self' 
     );
  
  // List of supported XPath functions.
  // What a stupid idea from W3C to take function name containing a '-' (dash)
  // NOTE: We replace the '-' with '_' to avoid the conflict with the minus operator.
  //       We will then do the same on the users Xpath querys 
  //   starts-with      => starts_with
  //   substring-before => substring_before
  //   substring-after  => substring_after
  //   string-length    => string_length
  //
  // This array contains a list of all valid functions that can be evaluated
  // in an XPath query.
  var $functions = array ( 'last', 'position', 'count', 'id', 'name',
    'string', 'concat', 'starts_with', 'contains', 'substring_before',
    'substring_after', 'substring', 'string_length', 'normalize_space', 'translate',
    'boolean', 'not', 'true', 'false', 'lang', 'number', 'sum', 'floor',
    'ceiling', 'round', 'x_lower', 'x_upper', 'generate_id' );
    
  // List of supported XPath operators.
  //
  // This array contains a list of all valid operators that can be evaluated
  // in a predicate of an XPath query. The list is ordered by the
  // precedence of the operators (lowest precedence first).
  var $operators = array( ' or ', ' and ', '=', '!=', '<=', '<', '>=', '>',
    '+', '-', '*', ' div ', ' mod ', ' | ');

  // List of literals from the xPath string.
  var $axPathLiterals = array();
  
  // The index and tree that is created during the analysis of an XML source.
  var $nodeIndex = array();
  var $nodeRoot  = array();
  var $emptyNode = array(
                     'name'        => '',       // The tag name. E.g. In <FOO bar="aaa"/> it would be 'FOO'
                     'attributes'  => array(),  // The attributes of the tag E.g. In <FOO bar="aaa"/> it would be array('bar'=>'aaa')
                     'childNodes'  => array(),  // Array of pointers to child nodes.
                     'textParts'   => array(),  // Array of text parts between the cilderen E.g. <FOO>aa<A>bb<B/>cc</A>dd</FOO> -> array('aa','bb','cc','dd')
                     'parentNode'   => NULL,     // Pointer to parent node or NULL if this node is the 'super root'
                     //-- *!* Following vars are set by the indexer and is for optimisation only *!*
                     'depth'       => 0,  // The tag depth (or tree level) starting with the root tag at 0.
                     'pos'         => 0,  // Is the zero-based position this node has in the parents 'childNodes'-list.
                     'contextPos'  => 1,  // Is the one-based position this node has by counting the siblings tags (tags with same name)
                     'xpath'       => ''  // Is the abs. XPath to this node.
                   );
  var $_indexIsDirty = FALSE;

  
  // These variable used during the parse XML source
  var $nodeStack       = array(); // The elements that we have still to close.
  var $parseStackIndex = 0;       // The current element of the nodeStack[] that we are adding to while 
                                  // parsing an XML source.  Corresponds to the depth of the xml node.
                                  // in our input data.
  var $parseOptions    = array(); // Used to set the PHP's XML parser options (see xml_parser_set_option)
  var $parsedTextLocation   = ''; // A reference to where we have to put char data collected during XML parsing
  var $parsInCData     = 0 ;      // Is >0 when we are inside a CDATA section.  
  var $parseSkipWhiteCache = 0;   // A cache of the skip whitespace parse option to speed up the parse.

  // This is the array of error strings, to keep consistency.
  var $errorStrings = array(
    'AbsoluteXPathRequired' => "The supplied xPath '%s' does not *uniquely* describe a node in the xml document.",
    'NoNodeMatch'           => "The supplied xPath-query '%s' does not match *any* node in the xml document.",
    'RootNodeAlreadyExists' => "An xml document may have only one root node."
    );
    
  /**
   * Constructor
   *
   * Optionally you may call this constructor with the XML-filename to parse and the 
   * XML option vector. Each of the entries in the option vector will be passed to
   * xml_parser_set_option().
   *
   * A option vector sample: 
   *   $xmlOpt = array(XML_OPTION_CASE_FOLDING => FALSE, 
   *                   XML_OPTION_SKIP_WHITE => TRUE);
   *
   * @param  $userXmlOptions (array) (optional) Vector of (<optionID>=><value>, 
   *                                 <optionID>=><value>, ...).  See PHP's
   *                                 xml_parser_set_option() docu for a list of possible
   *                                 options.
   * @see   importFromFile(), importFromString(), setXmlOptions()
   */
  function XPathEngine($userXmlOptions=array()) {
    parent::XPathBase();
    // Default to not folding case
    $this->parseOptions[XML_OPTION_CASE_FOLDING] = FALSE;
    // And not skipping whitespace
    $this->parseOptions[XML_OPTION_SKIP_WHITE] = FALSE;
    
    // Now merge in the overrides.
    // Don't use PHP's array_merge!
    if (is_array($userXmlOptions)) {
      foreach($userXmlOptions as $key => $val) $this->parseOptions[$key] = $val;
    }
  }
  
  /**
   * Resets the object so it's able to take a new xml sting/file
   *
   * Constructing objects is slow.  If you can, reuse ones that you have used already
   * by using this reset() function.
   */
  function reset() {
    parent::reset();
    $this->properties['xmlFile']  = ''; 
    $this->parseStackIndex = 0;
    $this->parsedTextLocation = '';
    $this->parsInCData   = 0;
    $this->nodeIndex     = array();
    $this->nodeRoot      = array();
    $this->nodeStack     = array();
    $this->aLiterals     = array();
    $this->_indexIsDirty = FALSE;
  }
  
  
  //-----------------------------------------------------------------------------------------
  // XPathEngine              ------  Get / Set Stuff  ------                                
  //-----------------------------------------------------------------------------------------
  
  /**
   * Returns the property/ies you want.
   * 
   * if $param is not given, all properties will be returned in a hash.
   *
   * @param  $param (string) the property you want the value of, or NULL for all the properties
   * @return        (mixed)  string OR hash of all params, or NULL on an unknown parameter.
   */
  function getProperties($param=NULL) {
    $this->properties['hasContent']      = !empty($this->nodeRoot);
    $this->properties['caseFolding']     = $this->parseOptions[XML_OPTION_CASE_FOLDING];
    $this->properties['skipWhiteSpaces'] = $this->parseOptions[XML_OPTION_SKIP_WHITE];
    
    if (empty($param)) return $this->properties;
    
    if (isSet($this->properties[$param])) {
      return $this->properties[$param];
    } else {
      return NULL;
    }
  }
  
  /**
   * Set an xml_parser_set_option()
   *
   * @param $optionID (int) The option ID (e.g. XML_OPTION_SKIP_WHITE)
   * @param $value    (int) The option value.
   * @see XML parser functions in PHP doc
   */
  function setXmlOption($optionID, $value) {
    if (!is_numeric($optionID)) return;
     $this->parseOptions[$optionID] = $value;
  }

  /**
   * Sets a number of xml_parser_set_option()s
   *
   * @param  $userXmlOptions (array) An array of parser options.
   * @see setXmlOption
   */
  function setXmlOptions($userXmlOptions=array()) {
    if (!is_array($userXmlOptions)) return;
    foreach($userXmlOptions as $key => $val) {
      $this->setXmlOption($key, $val);
    }
  }
  
  /**
   * Alternative way to control whether case-folding is enabled for this XML parser.
   *
   * Short cut to setXmlOptions(XML_OPTION_CASE_FOLDING, TRUE/FALSE)
   *
   * When it comes to XML, case-folding simply means uppercasing all tag- 
   * and attribute-names (NOT the content) if set to TRUE.  Note if you
   * have this option set, then your XPath queries will also be case folded 
   * for you.
   *
   * @param $onOff (bool) (default TRUE) 
   * @see XML parser functions in PHP doc
   */
  function setCaseFolding($onOff=TRUE) {
    $this->parseOptions[XML_OPTION_CASE_FOLDING] = $onOff;
  }
  
  /**
   * Alternative way to control whether skip-white-spaces is enabled for this XML parser.
   *
   * Short cut to setXmlOptions(XML_OPTION_SKIP_WHITE, TRUE/FALSE)
   *
   * When it comes to XML, skip-white-spaces will trim the tag content.
   * An XML file with no whitespace will be faster to process, but will make 
   * your data less human readable when you come to write it out.
   *
   * Running with this option on will slow the class down, so if you want to 
   * speed up your XML, then run it through once skipping white-spaces, then
   * write out the new version of your XML without whitespace, then use the
   * new XML file with skip whitespaces turned off.
   *
   * @param $onOff (bool) (default TRUE) 
   * @see XML parser functions in PHP doc
   */
  function setSkipWhiteSpaces($onOff=TRUE) {
    $this->parseOptions[XML_OPTION_SKIP_WHITE] = $onOff;
  }
   
  /**
   * Get the node defined by the $absoluteXPath.
   *
   * @param   $absoluteXPath (string) (optional, default is 'super-root') xpath to the node.
   * @return                 (array)  The node, or FALSE if the node wasn't found.
   */
  function &getNode($absoluteXPath='') {
    if ($absoluteXPath==='/') $absoluteXPath = '';
    if (!isSet($this->nodeIndex[$absoluteXPath])) return FALSE;
    if ($this->_indexIsDirty) $this->reindexNodeTree();
    return $this->nodeIndex[$absoluteXPath];
  }

  /**
   * Get a the content of a node text part or node attribute.
   * 
   * If the absolute Xpath references an attribute (Xpath ends with @ or attribute::), 
   * then the text value of that node-attribute is returned.
   * Otherwise the Xpath is referencing a text part of the node. This can be either a 
   * direct reference to a text part (Xpath ends with text()[<nr>]) or indirect reference 
   * (a simple abs. Xpath to a node).
   * 1) Direct Reference (xpath ends with text()[<part-number>]):
   *   If the 'part-number' is omitted, the first text-part is assumed; starting by 1.
   *   Negative numbers are allowed, where -1 is the last text-part a.s.o.
   * 2) Indirect Reference (a simple abs. Xpath to a node):
   *   Default is to return the *whole text*; that is the concated text-parts of the matching
   *   node. (NOTE that only in this case you'll only get a copy and changes to the returned  
   *   value wounld have no effect). Optionally you may pass a parameter 
   *   $textPartNr to define the text-part you want;  starting by 1.
   *   Negative numbers are allowed, where -1 is the last text-part a.s.o.
   *
   * NOTE I : The returned value can be fetched by reference
   *          E.g. $text =& wholeText(). If you wish to modify the text.
   * NOTE II: text-part numbers out of range will return FALSE
   * SIDENOTE:The function name is a suggestion from W3C in the XPath specification level 3.
   *
   * @param   $absoluteXPath  (string)  xpath to the node (See above).
   * @param   $textPartNr     (int)     If referring to a node, specifies which text part 
   *                                    to query.
   * @return                  (&string) A *reference* to the text if the node that the other 
   *                                    parameters describe or FALSE if the node is not found.
   */
  function &wholeText($absoluteXPath, $textPartNr=NULL) {
    $status = FALSE;
    $text   = NULL;
    if ($this->_indexIsDirty) $this->reindexNodeTree();
    
    do { // try-block
      if (preg_match(";(.*)/(attribute::|@)([^/]*)$;U", $absoluteXPath, $matches)) {
        $absoluteXPath = $matches[1];
        $attribute = $matches[3];
        if (!isSet($this->nodeIndex[$absoluteXPath]['attributes'][$attribute])) {
          $this->_displayError("The $absoluteXPath/attribute::$attribute value isn't a node in this document.", __LINE__, __FILE__, FALSE);
          break; // try-block
        }
        $text =& $this->nodeIndex[$absoluteXPath]['attributes'][$attribute];
        $status = TRUE;
        break; // try-block
      }
            
      // Xpath contains a 'text()'-function, thus goes right to a text node. If so interpret the Xpath.
      if (preg_match(":(.*)/text\(\)(\[(.*)\])?$:U", $absoluteXPath, $matches)) {
        $absoluteXPath = $matches[1];
 
        if (!isSet($this->nodeIndex[$absoluteXPath])) {
            $this->_displayError("The $absoluteXPath value isn't a node in this document.", __LINE__, __FILE__, FALSE);
            break; // try-block
        }

        // Get the amount of the text parts in the node.
        $textPartSize = sizeOf($this->nodeIndex[$absoluteXPath]['textParts']);

        // default to the first text node if a text node was not specified
        $textPartNr = isSet($matches[2]) ? substr($matches[2],1,-1) : 1;

        // Support negative indexes like -1 === last a.s.o.
        if ($textPartNr < 0) $textPartNr = $textPartSize + $textPartNr +1;
        if (($textPartNr <= 0) OR ($textPartNr > $textPartSize)) {
          $this->_displayError("The $absoluteXPath/text()[$textPartNr] value isn't a NODE in this document.", __LINE__, __FILE__, FALSE);
          break; // try-block
        }
        $text =& $this->nodeIndex[$absoluteXPath]['textParts'][$textPartNr - 1];
        $status = TRUE;
        break; // try-block
      }
      
      // At this point we have been given an xpath with neither a 'text()' nor 'attribute::' axis at the end
      // So we assume a get to text is wanted and use the optioanl fallback parameters $textPartNr
     
      if (!isSet($this->nodeIndex[$absoluteXPath])) {
          $this->_displayError("The $absoluteXPath value isn't a node in this document.", __LINE__, __FILE__, FALSE);
          break; // try-block
      }

      // Get the amount of the text parts in the node.
      $textPartSize = sizeOf($this->nodeIndex[$absoluteXPath]['textParts']);

      // If $textPartNr == NULL we return a *copy* of the whole concated text-parts
      if (is_null($textPartNr)) {
        unset($text);
        $text = implode('', $this->nodeIndex[$absoluteXPath]['textParts']);
        $status = TRUE;
        break; // try-block
      }
      
      // Support negative indexes like -1 === last a.s.o.
      if ($textPartNr < 0) $textPartNr = $textPartSize + $textPartNr +1;
      if (($textPartNr <= 0) OR ($textPartNr > $textPartSize)) {
        $this->_displayError("The $absoluteXPath has no text part at pos [$textPartNr] (Note: text parts start with 1).", __LINE__, __FILE__, FALSE);
        break; // try-block
      }
      $text =& $this->nodeIndex[$absoluteXPath]['textParts'][$textPartNr -1];
      $status = TRUE;
    } while (FALSE); // END try-block
    
    if (!$status) return FALSE;
    return $text;
  }

  /**
   * Obtain the string value of an object
   *
   * http://www.w3.org/TR/xpath#dt-string-value
   *
   * "For every type of node, there is a way of determining a string-value for a node of that type. 
   * For some types of node, the string-value is part of the node; for other types of node, the 
   * string-value is computed from the string-value of descendant nodes."
   *
   * @param $node   (node)   The node we have to convert
   * @return        (string) The string value of the node.  "" if the object has no evaluatable
   *                         string value
   */
  function _stringValue($node) {
    // Decode the entitites and then add the resulting literal string into our array.
    return $this->_addLiteral($this->decodeEntities($this->wholeText($node)));
  }
  
  //-----------------------------------------------------------------------------------------
  // XPathEngine           ------ Export the XML Document ------                             
  //-----------------------------------------------------------------------------------------
   
  /**
   * Returns the containing XML as marked up HTML with specified nodes hi-lighted
   *
   * @param $absoluteXPath    (string) The address of the node you would like to export.
   *                                   If empty the whole document will be exported.
   * @param $hilighXpathList  (array)  A list of nodes that you would like to highlight
   * @return                  (mixed)  The Xml document marked up as HTML so that it can
   *                                   be viewed in a browser, including any XML headers.
   *                                   FALSE on error.
   * @see _export()    
   */
  function exportAsHtml($absoluteXPath='', $hilightXpathList=array()) {
    $htmlString = $this->_export($absoluteXPath, $xmlHeader=NULL, $hilightXpathList);
    if (!$htmlString) return FALSE;
    return "<pre>\n" . $htmlString . "\n</pre>"; 
  }
  
  /**
   * Given a context this function returns the containing XML
   *
   * @param $absoluteXPath  (string) The address of the node you would like to export.
   *                                 If empty the whole document will be exported.
   * @param $xmlHeader      (array)  The string that you would like to appear before
   *                                 the XML content.  ie before the <root></root>.  If you
   *                                 do not specify this argument, the xmlHeader that was 
   *                                 found in the parsed xml file will be used instead.
   * @return                (mixed)  The Xml fragment/document, suitable for writing
   *                                 out to an .xml file or as part of a larger xml file, or
   *                                 FALSE on error.
   * @see _export()    
   */
  function exportAsXml($absoluteXPath='', $xmlHeader=NULL) {
    $this->hilightXpathList = NULL;
    return $this->_export($absoluteXPath, $xmlHeader); 
  }
    
  /**
   * Generates a XML string with the content of the current document and writes it to a file.
   *
   * Per default includes a <?xml ...> tag at the start of the data too. 
   *
   * @param     $fileName       (string) 
   * @param     $absoluteXPath  (string) The path to the parent node you want(see text above)
   * @param     $xmlHeader      (array)  The string that you would like to appear before
   *                                     the XML content.  ie before the <root></root>.  If you
   *                                     do not specify this argument, the xmlHeader that was 
   *                                     found in the parsed xml file will be used instead.
   * @return                    (string) The returned string contains well-formed XML data 
   *                                     or FALSE on error.
   * @see       exportAsXml(), exportAsHtml()
   */
  function exportToFile($fileName, $absoluteXPath='', $xmlHeader=NULL) {   
    $status = FALSE;
    do { // try-block
      if (!($hFile = fopen($fileName, "wb"))) {   // Did we open the file ok?
        $errStr = "Failed to open the $fileName xml file.";
        break; // try-block
      }
      
      if ($this->properties['OS_supports_flock']) {
        if (!flock($hFile, LOCK_EX + LOCK_NB)) {  // Lock the file
          $errStr = "Couldn't get an exclusive lock on the $fileName file.";
          break; // try-block
        }
      }
      if (!($xmlOut = $this->_export($absoluteXPath, $xmlHeader))) {
        $errStr = "Export failed";
        break; // try-block
      }
      
      $iBytesWritten = fwrite($hFile, $xmlOut);
      if ($iBytesWritten != strlen($xmlOut)) {
        $errStr = "Write error when writing back the $fileName file.";
        break; // try-block
      }
      
      // Flush and unlock the file
      @fflush($hFile);
      $status = TRUE;
    } while(FALSE);
    
    @flock($hFile, LOCK_UN);
    @fclose($hFile);
    // Sanity check the produced file.
    clearstatcache();
    if (filesize($fileName) < strlen($xmlOut)) {
      $errStr = "Write error when writing back the $fileName file.";
      $status = FALSE;
    }
    
    if (!$status)  $this->_displayError($errStr, __LINE__, __FILE__, FALSE);
    return $status;
  }

  /**
   * Generates a XML string with the content of the current document.
   *
   * This is the start for extracting the XML-data from the node-tree. We do some preperations
   * and then call _InternalExport() to fetch the main XML-data. You optionally may pass 
   * xpath to any node that will then be used as top node, to extract XML-parts of the 
   * document. Default is '', meaning to extract the whole document.
   *
   * You also may pass a 'xmlHeader' (usually something like <?xml version="1.0"? > that will
   * overwrite any other 'xmlHeader', if there was one in the original source.  If there
   * wasn't one in the original source, and you still don't specify one, then it will
   * use a default of <?xml version="1.0"? >
   * Finaly, when exporting to HTML, you may pass a vector xPaths you want to hi-light.
   * The hi-lighted tags and attributes will receive a nice color. 
   * 
   * NOTE I : The output can have 2 formats:
   *       a) If "skip white spaces" is/was set. (Not Recommended - slower)
   *          The output is formatted by adding indenting and carriage returns.
   *       b) If "skip white spaces" is/was *NOT* set.
   *          'as is'. No formatting is done. The output should the same as the 
   *          the original parsed XML source. 
   *
   * @param  $absoluteXPath (string) (optional, default is root) The node we choose as top-node
   * @param  $xmlHeader     (string) (optional) content before <root/> (see text above)
   * @param  $hilightXpath  (array)  (optional) a vector of xPaths to nodes we wat to 
   *                                 hi-light (see text above)
   * @return                (mixed)  The xml string, or FALSE on error.
   */
  function _export($absoluteXPath='', $xmlHeader=NULL, $hilightXpathList='') {
    // Check whether a root node is given.
    if (empty($absoluteXpath)) $absoluteXpath = '';
    if ($absoluteXpath == '/') $absoluteXpath = '';
    if ($this->_indexIsDirty) $this->reindexNodeTree();
    if (!isSet($this->nodeIndex[$absoluteXpath])) {
      // If the $absoluteXpath was '' and it didn't exist, then the document is empty
      // and we can safely return ''.
      if ($absoluteXpath == '') return '';
      $this->_displayError("The given xpath '{$absoluteXpath}' isn't a node in this document.", __LINE__, __FILE__, FALSE);
      return FALSE;
    }
    
    $this->hilightXpathList = $hilightXpathList;
    $this->indentStep = '  ';
    $hilightIsActive = is_array($hilightXpathList);
    if ($hilightIsActive) {
      $this->indentStep = '&nbsp;&nbsp;&nbsp;&nbsp;';
    }    
    
    // Cache this now
    $this->parseSkipWhiteCache = isSet($this->parseOptions[XML_OPTION_SKIP_WHITE]) ? $this->parseOptions[XML_OPTION_SKIP_WHITE] : FALSE;

    ///////////////////////////////////////
    // Get the starting node and begin with the header

    // Get the start node.  The super root is a special case.
    $startNode = NULL;
    if (empty($absoluteXPath)) {
      $superRoot = $this->nodeIndex[''];
      // If they didn't specify an xml header, use the one in the object
      if (is_null($xmlHeader)) {
        $xmlHeader = $this->parseSkipWhiteCache ? trim($superRoot['textParts'][0]) : $superRoot['textParts'][0];
        // If we still don't have an XML header, then use a suitable default
        if (empty($xmlHeader)) {
            $xmlHeader = '<?xml version="1.0"?>';
        }
      }

      if (isSet($superRoot['childNodes'][0])) $startNode = $superRoot['childNodes'][0];
    } else {
      $startNode = $this->nodeIndex[$absoluteXPath];
    }

    if (!empty($xmlHeader)) { 
      $xmlOut = $this->parseSkipWhiteCache ? $xmlHeader."\n" : $xmlHeader;
    } else {
      $xmlOut = '';
    }

    ///////////////////////////////////////
    // Output the document.

    if (($xmlOut .= $this->_InternalExport($startNode)) === FALSE) {
      return FALSE;
    }
    
    ///////////////////////////////////////

    // Convert our markers to hi-lights.
    if ($hilightIsActive) {
      $from = array('<', '>', chr(2), chr(3));
      $to = array('&lt;', '&gt;', '<font color="#FF0000"><b>', '</b></font>');
      $xmlOut = str_replace($from, $to, $xmlOut);
    }
    return $xmlOut; 
  }  

  /**
   * Export the xml document starting at the named node.
   *
   * @param $node (node)   The node we have to start exporting from
   * @return      (string) The string representation of the node.
   */
  function _InternalExport($node) {
    $bDebugThisFunction = FALSE;

    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_InternalExport");
      echo "Exporting node: ".$node['xpath']."<br>\n";
    }

    ////////////////////////////////

    // Quick out.
    if (empty($node)) return '';

    // The output starts as empty.
    $xmlOut = '';
    // This loop will output the text before the current child of a parent then the 
    // current child.  Where the child is a short tag we output the child, then move
    // onto the next child.  Where the child is not a short tag, we output the open tag, 
    // then queue up on currentParentStack[] the child.  
    //
    // When we run out of children, we then output the last text part, and close the 
    // 'parent' tag before popping the stack and carrying on.
    //
    // To illustrate, the numbers in this xml file indicate what is output on each
    // pass of the while loop:
    //
    // 1
    // <1>2
    //  <2>3
    //   <3/>4
    //  </4>5
    //  <5/>6
    // </6>

    // Although this is neater done using recursion, there's a 33% performance saving
    // to be gained by using this stack mechanism.

    // Only add CR's if "skip white spaces" was set. Otherwise leave as is.
    $CR = ($this->parseSkipWhiteCache) ? "\n" : '';
    $currentIndent = '';
    $hilightIsActive = is_array($this->hilightXpathList);

    // To keep track of where we are in the document we use a node stack.  The node 
    // stack has the following parallel entries:
    //   'Parent'     => (array) A copy of the parent node that who's children we are 
    //                           exporting
    //   'ChildIndex' => (array) The child index of the corresponding parent that we
    //                           are currently exporting.
    //   'Highlighted'=> (bool)  If we are highlighting this node.  Only relevant if
    //                           the hilight is active.

    // Setup our node stack.  The loop is designed to output children of a parent, 
    // not the parent itself, so we must put the parent on as the starting point.
    $nodeStack['Parent'] = array($node['parentNode']);
    // And add the childpos of our node in it's parent to our "child index stack".
    $nodeStack['ChildIndex'] = array($node['pos']);
    // We start at 0.
    $nodeStackIndex = 0;

    // We have not to output text before/after our node, so blank it.  We will recover it
    // later
    $OldPreceedingStringValue = $nodeStack['Parent'][0]['textParts'][$node['pos']];
    $OldPreceedingStringRef =& $nodeStack['Parent'][0]['textParts'][$node['pos']];
    $OldPreceedingStringRef = "";
    $currentXpath = "";

    // While we still have data on our stack
    while ($nodeStackIndex >= 0) {
      // Count the children and get a copy of the current child.
      $iChildCount = count($nodeStack['Parent'][$nodeStackIndex]['childNodes']);
      $currentChild = $nodeStack['ChildIndex'][$nodeStackIndex];
      // Only do the auto indenting if the $parseSkipWhiteCache flag was set.
      if ($this->parseSkipWhiteCache)
        $currentIndent = str_repeat($this->indentStep, $nodeStackIndex);

      if ($bDebugThisFunction)
        echo "Exporting child ".($currentChild+1)." of node {$nodeStack['Parent'][$nodeStackIndex]['xpath']}\n";

      ///////////////////////////////////////////
      // Add the text before our child.

      // Add the text part before the current child
      $tmpTxt =& $nodeStack['Parent'][$nodeStackIndex]['textParts'][$currentChild];
      if (isSet($tmpTxt) AND ($tmpTxt!="")) {
        // Only add CR indent if there were children
        if ($iChildCount)
          $xmlOut .= $CR.$currentIndent;
        // Hilight if necessary.
        $highlightStart = $highlightEnd = '';
        if ($hilightIsActive) {
          $currentXpath = $nodeStack['Parent'][$nodeStackIndex]['xpath'].'/text()['.($currentChild+1).']';
          if (in_array($currentXpath, $this->hilightXpathList)) {
           // Yes we hilight
            $highlightStart = chr(2);
            $highlightEnd   = chr(3);
          }
        }
        $xmlOut .= $highlightStart.$nodeStack['Parent'][$nodeStackIndex]['textParts'][$currentChild].$highlightEnd;
      }
      if ($iChildCount && $nodeStackIndex) $xmlOut .= $CR;

      ///////////////////////////////////////////

      // Are there any more children?
      if ($iChildCount <= $currentChild) {
        // Nope, so output the last text before the closing tag
        $tmpTxt =& $nodeStack['Parent'][$nodeStackIndex]['textParts'][$currentChild+1];
        if (isSet($tmpTxt) AND ($tmpTxt!="")) {
          // Hilight if necessary.
          $highlightStart = $highlightEnd = '';
          if ($hilightIsActive) {
            $currentXpath = $nodeStack['Parent'][$nodeStackIndex]['xpath'].'/text()['.($currentChild+2).']';
            if (in_array($currentXpath, $this->hilightXpathList)) {
             // Yes we hilight
              $highlightStart = chr(2);
              $highlightEnd   = chr(3);
            }
          }
          $xmlOut .= $highlightStart
                .$currentIndent.$nodeStack['Parent'][$nodeStackIndex]['textParts'][$currentChild+1].$CR
                .$highlightEnd;
        }

        // Now close this tag, as we are finished with this child.

        // Potentially output an (slightly smaller indent).
        if ($this->parseSkipWhiteCache
          && count($nodeStack['Parent'][$nodeStackIndex]['childNodes'])) {
          $xmlOut .= str_repeat($this->indentStep, $nodeStackIndex - 1);
        }

        // Check whether the xml-tag is to be hilighted.
        $highlightStart = $highlightEnd = '';
        if ($hilightIsActive) {
          $currentXpath = $nodeStack['Parent'][$nodeStackIndex]['xpath'];
          if (in_array($currentXpath, $this->hilightXpathList)) {
            // Yes we hilight
            $highlightStart = chr(2);
            $highlightEnd   = chr(3);
          }
        }
        $xmlOut .=  $highlightStart
                     .'</'.$nodeStack['Parent'][$nodeStackIndex]['name'].'>'
                     .$highlightEnd;
        // Decrement the $nodeStackIndex to go back to the next unfinished parent.
        $nodeStackIndex--;

        // If the index is 0 we are finished exporting the last node, as we may have been
        // exporting an internal node.
        if ($nodeStackIndex == 0) break;

        // Indicate to the parent that we are finished with this child.
        $nodeStack['ChildIndex'][$nodeStackIndex]++;

        continue;
      }

      ///////////////////////////////////////////
      // Ok, there are children still to process.

      // Queue up the next child (I can copy because I won't modify and copying is faster.)
      $nodeStack['Parent'][$nodeStackIndex + 1] = $nodeStack['Parent'][$nodeStackIndex]['childNodes'][$currentChild];

      // Work out if it is a short child tag.
      $iGrandChildCount = count($nodeStack['Parent'][$nodeStackIndex + 1]['childNodes']);
      $shortGrandChild = (($iGrandChildCount == 0) AND (implode('',$nodeStack['Parent'][$nodeStackIndex + 1]['textParts'])==''));

      ///////////////////////////////////////////
      // Assemble the attribute string first.
      $attrStr = '';
      foreach($nodeStack['Parent'][$nodeStackIndex + 1]['attributes'] as $key=>$val) {
        // Should we hilight the attribute?
        if ($hilightIsActive AND in_array($currentXpath.'/attribute::'.$key, $this->hilightXpathList)) {
          $hiAttrStart = chr(2);
          $hiAttrEnd   = chr(3);
        } else {
          $hiAttrStart = $hiAttrEnd = '';
        }
        $attrStr .= ' '.$hiAttrStart.$key.'="'.$val.'"'.$hiAttrEnd;
      }

      ///////////////////////////////////////////
      // Work out what goes before and after the tag content

      $beforeTagContent = $currentIndent;
      if ($shortGrandChild) $afterTagContent = '/>';
      else                  $afterTagContent = '>';

      // Check whether the xml-tag is to be hilighted.
      if ($hilightIsActive) {
        $currentXpath = $nodeStack['Parent'][$nodeStackIndex + 1]['xpath'];
        if (in_array($currentXpath, $this->hilightXpathList)) {
          // Yes we hilight
          $beforeTagContent .= chr(2);
          $afterTagContent  .= chr(3);
        }
      }
      $beforeTagContent .= '<';
//      if ($shortGrandChild) $afterTagContent .= $CR;
      
      ///////////////////////////////////////////
      // Output the tag

      $xmlOut .= $beforeTagContent
                  .$nodeStack['Parent'][$nodeStackIndex + 1]['name'].$attrStr
                  .$afterTagContent;

      ///////////////////////////////////////////
      // Carry on.            

      // If it is a short tag, then we've already done this child, we just move to the next
      if ($shortGrandChild) {
        // Move to the next child, we need not go deeper in the tree.
        $nodeStack['ChildIndex'][$nodeStackIndex]++;
        // But if we are just exporting the one node we'd go no further.
        if ($nodeStackIndex == 0) break;
      } else {
        // Else queue up the child going one deeper in the stack
        $nodeStackIndex++;
        // Start with it's first child
        $nodeStack['ChildIndex'][$nodeStackIndex] = 0;
      }
    }

    $result = $xmlOut;

    // Repair what we "undid"
    $OldPreceedingStringRef = $OldPreceedingStringValue;

    ////////////////////////////////////////////

    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }

    return $result;
  }
     
  //-----------------------------------------------------------------------------------------
  // XPathEngine           ------ Import the XML Source ------                               
  //-----------------------------------------------------------------------------------------
  
  /**
   * Reads a file or URL and parses the XML data.
   *
   * Parse the XML source and (upon success) store the information into an internal structure.
   *
   * @param     $fileName (string) Path and name (or URL) of the file to be read and parsed.
   * @return              (bool)   TRUE on success, FALSE on failure (check getLastError())
   * @see       importFromString(), getLastError(), 
   */
  function importFromFile($fileName) {
    $status = FALSE;
    $errStr = '';
    do { // try-block
      // Remember file name. Used in error output to know in which file it happend
      $this->properties['xmlFile'] = $fileName;
      // If we already have content, then complain.
      if (!empty($this->nodeRoot)) {
        $errStr = 'Called when this object already contains xml data. Use reset().';
        break; // try-block
      }
      // The the source is an url try to fetch it.
      if (preg_match(';^http(s)?://;', $fileName)) {
        // Read the content of the url...this is really prone to errors, and we don't really
        // check for too many here...for now, suppressing both possible warnings...we need
        // to check if we get a none xml page or something of that nature in the future
        $xmlString = @implode('', @file($fileName));
        if (!empty($xmlString)) {
          $status = TRUE;
        } else {
          $errStr = "The url '{$fileName}' could not be found or read.";
        }
        break; // try-block
      } 
      
      // Reaching this point we're dealing with a real file (not an url). Check if the file exists and is readable.
      if (!is_readable($fileName)) { // Read the content from the file
        $errStr = "File '{$fileName}' could not be found or read.";
        break; // try-block
      }
      if (is_dir($fileName)) {
        $errStr = "'{$fileName}' is a directory.";
        break; // try-block
      }
      // Read the file
      if (!($fp = @fopen($fileName, 'rb'))) {
        $errStr = "Failed to open '{$fileName}' for read.";
        break; // try-block
      }
      $xmlString = fread($fp, filesize($fileName));
      @fclose($fp);
      
      $status = TRUE;
    } while (FALSE);
    
    if (!$status) {
      $this->_displayError('In importFromFile(): '. $errStr, __LINE__, __FILE__, FALSE);
      return FALSE;
    }
    return $this->importFromString($xmlString);
  }
  
  /**
   * Reads a string and parses the XML data.
   *
   * Parse the XML source and (upon success) store the information into an internal structure.
   * If a parent xpath is given this means that XML data is to be *appended* to that parent.
   *
   * ### If a function uses setLastError(), then say in the function header that getLastError() is useful.
   *
   * @param  $xmlString           (string) Name of the string to be read and parsed.
   * @param  $absoluteParentPath  (string) Node to append data too (see above)
   * @return                      (bool)   TRUE on success, FALSE on failure 
   *                                       (check getLastError())
   */
  function importFromString($xmlString, $absoluteParentPath = '') {
    $bDebugThisFunction = FALSE;

    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("importFromString");
      echo "Importing from string of length ".strlen($xmlString)." to node '$absoluteParentPath'\n<br>";
      echo "Parser options:\n<br>";
      print_r($this->parseOptions);
    }

    $status = FALSE;
    $errStr = '';
    do { // try-block
      // If we already have content, then complain.
      if (!empty($this->nodeRoot) AND empty($absoluteParentPath)) {
        $errStr = 'Called when this object already contains xml data. Use reset() or pass the parent Xpath as 2ed param to where tie data will append.';
        break; // try-block
      }
      // Check whether content has been read.
      if (empty($xmlString)) {
        // Nothing to do!!
        $status = TRUE;
        // If we were importing to root, build a blank root.
        if (empty($absoluteParentPath)) {
          $this->_createSuperRoot();
        }
        $this->reindexNodeTree();
//        $errStr = 'This xml document (string) was empty';
        break; // try-block
      } else {
        $xmlString = $this->_translateAmpersand($xmlString);
      }
      
      // Restart our node index with a root entry.
      $nodeStack = array();
      $this->parseStackIndex = 0;

      // If a parent xpath is given this means that XML data is to be *appended* to that parent.
      if (!empty($absoluteParentPath)) {
        // Check if parent exists
        if (!isSet($nodeIndex[$absoluteParentPath])) {
          $errStr = "You tried to append XML data to a parent '$absoluteParentPath' that does not exist.";
          break; // try-block
        } 
        // Add it as the starting point in our array.
        $this->nodeStack[0] =& $nodeIndex[$absoluteParentPath];
      } else {
        // Build a 'super-root'
        $this->_createSuperRoot();
        // Put it in as the start of our node stack.
        $this->nodeStack[0] =& $this->nodeRoot;
      }

      // Point our text buffer reference at the next text part of the root
      $this->parsedTextLocation =& $this->nodeStack[0]['textParts'][];
      $this->parsInCData = 0;
      // We cache this now.
      $this->parseSkipWhiteCache = isSet($this->parseOptions[XML_OPTION_SKIP_WHITE]) ? $this->parseOptions[XML_OPTION_SKIP_WHITE] : FALSE;
      
      // Create an XML parser.
      $parser = xml_parser_create();
      // Set default XML parser options.
      if (is_array($this->parseOptions)) {
        foreach($this->parseOptions as $key => $val) {
          xml_parser_set_option($parser, $key, $val);
        }
      }
      
      // Set the object and the element handlers for the XML parser.
      xml_set_object($parser, $this);
      xml_set_element_handler($parser, '_handleStartElement', '_handleEndElement');
      xml_set_character_data_handler($parser, '_handleCharacterData');
      xml_set_default_handler($parser, '_handleDefaultData');
      xml_set_processing_instruction_handler($parser, '_handlePI');
     
      if ($bDebugThisFunction)
       $this->_profileFunction($aStartTime, "Setup for parse");

      // Parse the XML source and on error generate an error message.
      if (!xml_parse($parser, $xmlString, TRUE)) {
        $source = empty($this->properties['xmlFile']) ? 'string' : 'file ' . basename($this->properties['xmlFile']) . "'";
        $errStr = "XML error in given {$source} on line ".
               xml_get_current_line_number($parser). '  column '. xml_get_current_column_number($parser) .
               '. Reason:'. xml_error_string(xml_get_error_code($parser));
        break; // try-block
      }
      
      // Free the parser.
      @xml_parser_free($parser);
      // And we don't need this any more.
      $this->nodeStack = array();

      if ($bDebugThisFunction)
        $this->_profileFunction($aStartTime, "Parse Object");

      $this->reindexNodeTree();

      if ($bDebugThisFunction) {
        print_r(array_keys($this->nodeIndex));
      }

      if ($bDebugThisFunction)
       $this->_profileFunction($aStartTime, "Reindex Object");
      
      $status = TRUE;
    } while (FALSE);
    
    if (!$status) {
      $this->_displayError('In importFromString(): '. $errStr, __LINE__, __FILE__, FALSE);
      $bResult = FALSE;
    } else {
      $bResult = TRUE;
    }

    ////////////////////////////////////////////

    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $bResult);
    }

    return $bResult;
  }
  
  
  //-----------------------------------------------------------------------------------------
  // XPathEngine               ------  XML Handlers  ------                                  
  //-----------------------------------------------------------------------------------------
  
  /**
   * Handles opening XML tags while parsing.
   *
   * While parsing a XML document for each opening tag this method is
   * called. It'll add the tag found to the tree of document nodes.
   *
   * @param $parser     (int)    Handler for accessing the current XML parser.
   * @param $name       (string) Name of the opening tag found in the document.
   * @param $attributes (array)  Associative array containing a list of
   *                             all attributes of the tag found in the document.
   * @see _handleEndElement(), _handleCharacterData()
   */
  function _handleStartElement($parser, $nodeName, $attributes) {
    if (empty($nodeName)) {
      $this->_displayError('XML error in file at line'. xml_get_current_line_number($parser) .'. Empty name.', __LINE__, __FILE__);
      return;
    }

    // Trim accumulated text if necessary.
    if ($this->parseSkipWhiteCache) {
      $iCount = count($this->nodeStack[$this->parseStackIndex]['textParts']);
      $this->nodeStack[$this->parseStackIndex]['textParts'][$iCount-1] = rtrim($this->parsedTextLocation);
    } 

    if ($this->bDebugXmlParse) {
      echo "<blockquote>" . htmlspecialchars("Start node: <".$nodeName . ">")."<br>";
      echo "Appended to stack entry: $this->parseStackIndex<br>\n";
      echo "Text part before element is: ".htmlspecialchars($this->parsedTextLocation);
      /*
      echo "<pre>";
      $dataPartsCount = count($this->nodeStack[$this->parseStackIndex]['textParts']);
      for ($i = 0; $i < $dataPartsCount; $i++) {
        echo "$i:". htmlspecialchars($this->nodeStack[$this->parseStackIndex]['textParts'][$i])."\n";
      }
      echo "</pre>";
      */
    }

    // Add a node and set path to current.
    if (!$this->_internalAppendChild($this->parseStackIndex, $nodeName)) {
      $this->_displayError('Internal error during parse of XML file at line'. xml_get_current_line_number($parser) .'. Empty name.', __LINE__, __FILE__);
      return;
    }    

    // We will have gone one deeper then in the stack.
    $this->parseStackIndex++;

    // Point our parseTxtBuffer reference at the new node.
    $this->parsedTextLocation =& $this->nodeStack[$this->parseStackIndex]['textParts'][0];
    
    // Set the attributes.
    if (!empty($attributes)) {
      if ($this->bDebugXmlParse) {
        echo 'Attributes: <br>';
        print_r($attributes);
        echo '<br>';
      }
      $this->nodeStack[$this->parseStackIndex]['attributes'] = $attributes;
    }
  }
  
  /**
   * Handles closing XML tags while parsing.
   *
   * While parsing a XML document for each closing tag this method is called.
   *
   * @param $parser (int)    Handler for accessing the current XML parser.
   * @param $name   (string) Name of the closing tag found in the document.
   * @see       _handleStartElement(), _handleCharacterData()
   */
  function _handleEndElement($parser, $name) {
    if (($this->parsedTextLocation=='') 
        && empty($this->nodeStack[$this->parseStackIndex]['textParts'])) {
      // We reach this point when parsing a tag of format <foo/>. The 'textParts'-array 
      // should stay empty and not have an empty string in it.
    } else {
      // Trim accumulated text if necessary.
      if ($this->parseSkipWhiteCache) {
        $iCount = count($this->nodeStack[$this->parseStackIndex]['textParts']);
        $this->nodeStack[$this->parseStackIndex]['textParts'][$iCount-1] = rtrim($this->parsedTextLocation);
      }
    }

    if ($this->bDebugXmlParse) {
      echo "Text part after element is: ".htmlspecialchars($this->parsedTextLocation)."<br>\n";
      echo htmlspecialchars("Parent:<{$this->parseStackIndex}>, End-node:</$name> '".$this->parsedTextLocation) . "'<br>Text nodes:<pre>\n";
      $dataPartsCount = count($this->nodeStack[$this->parseStackIndex]['textParts']);
      for ($i = 0; $i < $dataPartsCount; $i++) {
        echo "$i:". htmlspecialchars($this->nodeStack[$this->parseStackIndex]['textParts'][$i])."\n";
      }
      var_dump($this->nodeStack[$this->parseStackIndex]['textParts']);
      echo "</pre></blockquote>\n";
    }

    // Jump back to the parent element.
    $this->parseStackIndex--;

    // Set our reference for where we put any more whitespace
    $this->parsedTextLocation =& $this->nodeStack[$this->parseStackIndex]['textParts'][];

    // Note we leave the entry in the stack, as it will get blanked over by the next element
    // at this level.  The safe thing to do would be to remove it too, but in the interests 
    // of performance, we will not bother, as were it to be a problem, then it would be an
    // internal bug anyway.
    if ($this->parseStackIndex < 0) {
      $this->_displayError('Internal error during parse of XML file at line'. xml_get_current_line_number($parser) .'. Empty name.', __LINE__, __FILE__);
      return;
    }    
  }
  
  /**
   * Handles character data while parsing.
   *
   * While parsing a XML document for each character data this method
   * is called. It'll add the character data to the document tree.
   *
   * @param $parser (int)    Handler for accessing the current XML parser.
   * @param $text   (string) Character data found in the document.
   * @see       _handleStartElement(), _handleEndElement()
   */
  function _handleCharacterData($parser, $text) {
  
    if ($this->parsInCData >0) $text = $this->_translateAmpersand($text, $reverse=TRUE);
    
    if ($this->bDebugXmlParse) echo "Handling character data: '".htmlspecialchars($text)."'<br>";
    if ($this->parseSkipWhiteCache AND !empty($text) AND !$this->parsInCData) {
      // Special case CR. CR always comes in a separate data. Trans. it to '' or ' '. 
      // If txtBuffer is already ending with a space use '' otherwise ' '.
      $bufferHasEndingSpace = (empty($this->parsedTextLocation) OR substr($this->parsedTextLocation, -1) === ' ') ? TRUE : FALSE;
      if ($text=="\n") {
        $text = $bufferHasEndingSpace ? '' : ' ';
      } else {
        if ($bufferHasEndingSpace) {
          $text = ltrim(preg_replace('/\s+/', ' ', $text));
        } else {
          $text = preg_replace('/\s+/', ' ', $text);
        }
      }
      if ($this->bDebugXmlParse) echo "'Skip white space' is ON. reduced to : '" .htmlspecialchars($text) . "'<br>";
    }
    $this->parsedTextLocation .= $text;
  }
  
  /**
   * Default handler for the XML parser.  
   *
   * While parsing a XML document for string not caught by one of the other
   * handler functions, we end up here.
   *
   * @param $parser (int)    Handler for accessing the current XML parser.
   * @param $text   (string) Character data found in the document.
   * @see       _handleStartElement(), _handleEndElement()
   */
  function _handleDefaultData($parser, $text) {
    do { // try-block
      if (!strcmp($text, '<![CDATA[')) {
        $this->parsInCData++;
      } elseif (!strcmp($text, ']]>')) {
        $this->parsInCData--;
        if ($this->parsInCData < 0) $this->parsInCData = 0;
      }
      $this->parsedTextLocation .= $this->_translateAmpersand($text, $reverse=TRUE);
      if ($this->bDebugXmlParse) echo "**Default handler data: ".htmlspecialchars($text)."<br>";    
      break; // try-block
    } while (FALSE); // END try-block
  }
  
  /**
   * Handles processing instruction (PI)
   *
   * A processing instruction has the following format: 
   * <?  target data  ? > e.g.  <? dtd version="1.0" ? >
   *
   * Currently I have no bether idea as to left it 'as is' and treat the PI data as normal 
   * text (and adding the surrounding PI-tags <? ? >). 
   *
   * @param     $parser (int)    Handler for accessing the current XML parser.
   * @param     $target (string) Name of the PI target. E.g. XML, PHP, DTD, ... 
   * @param     $data   (string) Associative array containing a list of
   * @see       PHP's manual "xml_set_processing_instruction_handler"
   */
  function _handlePI($parser, $target, $data) {
    //echo("pi data=".$data."end"); exit;
    $data = $this->_translateAmpersand($data, $reverse=TRUE);
    $this->parsedTextLocation .= "<?{$target} {$data}?>";
    return TRUE;
  }
  
  //-----------------------------------------------------------------------------------------
  // XPathEngine          ------  Node Tree Stuff  ------                                    
  //-----------------------------------------------------------------------------------------

  /**
   * Creates a super root node.
   */
  function _createSuperRoot() {
    // Build a 'super-root'
    $this->nodeRoot = $this->emptyNode;
    $this->nodeRoot['name']      = '';
    $this->nodeRoot['parentNode'] = NULL;
    $this->nodeIndex[''] =& $this->nodeRoot;
  }

  /**
   * Adds a new node to the XML document tree during xml parsing.
   *
   * This method adds a new node to the tree of nodes of the XML document
   * being handled by this class. The new node is created according to the
   * parameters passed to this method.  This method is a much watered down
   * version of appendChild(), used in parsing an xml file only.
   * 
   * It is assumed that adding starts with root and progresses through the
   * document in parse order.  New nodes must have a corresponding parent. And
   * once we have read the </> tag for the element we will never need to add
   * any more data to that node.  Otherwise the add will be ignored or fail.
   *
   * The function is faciliated by a nodeStack, which is an array of nodes that
   * we have yet to close.
   *
   * @param   $stackParentIndex (int)    The index into the nodeStack[] of the parent
   *                                     node to which the new node should be added as 
   *                                     a child. *READONLY*
   * @param   $nodeName         (string) Name of the new node. *READONLY*
   * @return                    (bool)   TRUE if we successfully added a new child to 
   *                                     the node stack at index $stackParentIndex + 1,
   *                                     FALSE on error.
   */
  function _internalAppendChild($stackParentIndex, $nodeName) {
    // This call is likely to be executed thousands of times, so every 0.01ms counts.
    // If you want to debug this function, you'll have to comment the stuff back in
    //$bDebugThisFunction = FALSE;
    
    /*
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_internalAppendChild");
      echo "Current Node (parent-index) and the child to append : '{$stackParentIndex}' +  '{$nodeName}' \n<br>";
    }
    */
     //////////////////////////////////////

    if (!isSet($this->nodeStack[$stackParentIndex])) {
      $errStr = "Invalid parent. You tried to append the tag '{$nodeName}' to an non-existing parent in our node stack '{$stackParentIndex}'.";
      $this->_displayError('In _internalAppendChild(): '. $errStr, __LINE__, __FILE__, FALSE); 

      /*
      if ($bDebugThisFunction)
        $this->_closeDebugFunction($aStartTime, FALSE);
      */

      return FALSE;
    }

    // Retrieve the parent node from the node stack.  This is the last node at that 
    // depth that we have yet to close.  This is where we should add the text/node.
    $parentNode =& $this->nodeStack[$stackParentIndex];
          
    // Brand new node please
    $newChildNode = $this->emptyNode;
    
    // Save the vital information about the node.
    $newChildNode['name'] = $nodeName;
    $parentNode['childNodes'][] =& $newChildNode;
    
    // Add to our node stack
    $this->nodeStack[$stackParentIndex + 1] =& $newChildNode;

    /*
    if ($bDebugThisFunction) {
      echo "The new node received index: '".($stackParentIndex + 1)."'\n";
      foreach($this->nodeStack as $key => $val) echo "$key => ".$val['name']."\n"; 
      $this->_closeDebugFunction($aStartTime, TRUE);
    }
    */

    return TRUE;
  }
  
  /**
   * Update nodeIndex and every node of the node-tree. 
   *
   * Call after you have finished any tree modifications other wise a match with 
   * an xPathQuery will produce wrong results.  The $this->nodeIndex[] is recreated 
   * and every nodes optimization data is updated.  The optimization data is all the
   * data that is duplicate information, would just take longer to find. Child nodes 
   * with value NULL are removed from the tree.
   *
   * By default the modification functions in this component will automatically re-index
   * the nodes in the tree.  Sometimes this is not the behaver you want. To surpress the 
   * reindex, set the functions $autoReindex to FALSE and call reindexNodeTree() at the 
   * end of your changes.  This sometimes leads to better code (and less CPU overhead).
   *
   * Sample:
   * =======
   * Given the xml is <AAA><B/>.<B/>.<B/></AAA> | Goal is <AAA>.<B/>.</AAA>  (Delete B[1] and B[3])
   *   $xPathSet = $xPath->match('//B'); # Will result in array('/AAA[1]/B[1]', '/AAA[1]/B[2]', '/AAA[1]/B[3]');
   * Three ways to do it.
   * 1) Top-Down  (with auto reindexing) - Safe, Slow and you get easily mix up with the the changing node index
   *    removeChild('/AAA[1]/B[1]'); // B[1] removed, thus all B[n] become B[n-1] !!
   *    removeChild('/AAA[1]/B[2]'); // Now remove B[2] (That originaly was B[3])
   * 2) Bottom-Up (with auto reindexing) -  Safe, Slow and the changing node index (caused by auto-reindex) can be ignored.
   *    for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
   *      if ($i==1) continue; 
   *      removeChild($xPathSet[$i]);
   *    }
   * 3) // Top-down (with *NO* auto reindexing) - Fast, Safe as long as you call reindexNodeTree()
   *    foreach($xPathSet as $xPath) {
   *      // Specify no reindexing
   *      if ($xPath == $xPathSet[1]) continue; 
   *      removeChild($xPath, $autoReindex=FALSE);
   *      // The object is now in a slightly inconsistent state.
   *    }
   *    // Finally do the reindex and the object is consistent again
   *    reindexNodeTree();
   *
   * @return (bool) TRUE on success, FALSE otherwise.
   * @see _recursiveReindexNodeTree()
   */
  function reindexNodeTree() {
    //return;
    $this->_indexIsDirty = FALSE;
    $this->nodeIndex = array();
    $this->nodeIndex[''] =& $this->nodeRoot;
    // Quick out for when the tree has no data.
    if (empty($this->nodeRoot)) return TRUE;
    return $this->_recursiveReindexNodeTree('');
  }
  

  /**
   * Create the ids that are accessable through the generate-id() function
   */
  function _generate_ids() {
    // If we have generated them already, then bail.
    if (isset($this->nodeIndex['']['generate_id'])) return;

    // keys generated are the string 'id0' . hexatridecimal-based (0..9,a-z) index
    $aNodeIndexes = array_keys($this->nodeIndex);
    $idNumber = 0;
    foreach($aNodeIndexes as $index => $key) {
//      $this->nodeIndex[$key]['generated_id'] = 'id' . base_convert($index,10,36);
      // Skip attribute and text nodes.
      // ### Currently don't support attribute and text nodes.
      if (strstr($key, 'text()') !== FALSE) continue;
      if (strstr($key, 'attribute::') !== FALSE) continue;
      $this->nodeIndex[$key]['generated_id'] = 'idPhpXPath' . $idNumber;

      // Make the id's sequential so that we can test predictively.
      $idNumber++;
    }
  }

  /**
   * Here's where the work is done for reindexing (see reindexNodeTree)
   *
   * @param  $absoluteParentPath (string) the xPath to the parent node
   * @return                     (bool)   TRUE on success, FALSE otherwise.
   * @see reindexNodeTree()
   */
  function _recursiveReindexNodeTree($absoluteParentPath) {
    $parentNode =& $this->nodeIndex[$absoluteParentPath];
    
    // Check for any 'dead' child nodes first and concate the text parts if found.
    for ($iChildIndex=sizeOf($parentNode['childNodes'])-1; $iChildIndex>=0; $iChildIndex--) {
      // Check if the child node still exits (it may have been removed).
      if (!empty($parentNode['childNodes'][$iChildIndex])) continue;
      // Child node was removed. We got to merge the text parts then.
      $parentNode['textParts'][$iChildIndex] .= $parentNode['textParts'][$iChildIndex+1];
      array_splice($parentNode['textParts'], $iChildIndex+1, 1); 
      array_splice($parentNode['childNodes'], $iChildIndex, 1);
    }

    // Now start a reindex.
    $contextHash = array();
    $childSize = sizeOf($parentNode['childNodes']);

    // If there are no children, we have to treat this specially:
    if ($childSize == 0) {
      // Add a dummy text node.
      $this->nodeIndex[$absoluteParentPath.'/text()[1]'] =& $parentNode;
    } else {
      for ($iChildIndex=0; $iChildIndex<$childSize; $iChildIndex++) {
        $childNode =& $parentNode['childNodes'][$iChildIndex];
        // Make sure that there is a text-part in front of every node. (May be empty)
        if (!isSet($parentNode['textParts'][$iChildIndex])) $parentNode['textParts'][$iChildIndex] = '';
        // Count the nodes with same name (to determine their context position)
        $childName = $childNode['name'];
        if (empty($contextHash[$childName])) { 
          $contextPos = $contextHash[$childName] = 1;
        } else {
          $contextPos = ++$contextHash[$childName];
        }
        // Make the node-index hash
        $newPath = $absoluteParentPath . '/' . $childName . '['.$contextPos.']';

        // ### Note ultimately we will end up supporting text nodes as actual nodes.

        // Preceed with a dummy entry for the text node.
        $this->nodeIndex[$absoluteParentPath.'/text()['.($childNode['pos']+1).']'] =& $childNode;
        // Then the node itself
        $this->nodeIndex[$newPath] =& $childNode;

        // Now some dummy nodes for each of the attribute nodes.
        $iAttributeCount = sizeOf($childNode['attributes']);
        if ($iAttributeCount > 0) {
          $aAttributesNames = array_keys($childNode['attributes']);
          for ($iAttributeIndex = 0; $iAttributeIndex < $iAttributeCount; $iAttributeIndex++) {
            $attribute = $aAttributesNames[$iAttributeIndex];
            $newAttributeNode = $this->emptyNode;
            $newAttributeNode['name'] = $attribute;
            $newAttributeNode['textParts'] = array($childNode['attributes'][$attribute]);
            $newAttributeNode['contextPos'] = $iAttributeIndex;
            $newAttributeNode['xpath'] = "$newPath/attribute::$attribute";
            $newAttributeNode['parentNode'] =& $childNode;
            $newAttributeNode['depth'] =& $parentNode['depth'] + 2;
            // Insert the node as a master node, not a reference, otherwise there will be 
            // variable "bleeding".
            $this->nodeIndex["$newPath/attribute::$attribute"] = $newAttributeNode;
          }
        }

        // Update the node info (optimisation)
        $childNode['parentNode'] =& $parentNode;
        $childNode['depth'] = $parentNode['depth'] + 1;
        $childNode['pos'] = $iChildIndex;
        $childNode['contextPos'] = $contextHash[$childName];
        $childNode['xpath'] = $newPath;
        $this->_recursiveReindexNodeTree($newPath);

        // Follow with a dummy entry for the text node.
        $this->nodeIndex[$absoluteParentPath.'/text()['.($childNode['pos']+2).']'] =& $childNode;
      }

      // Make sure that their is a text-part after the last node.
      if (!isSet($parentNode['textParts'][$iChildIndex])) $parentNode['textParts'][$iChildIndex] = '';
    }

    return TRUE;
  }
  
  /** 
   * Clone a node and it's child nodes.
   *
   * NOTE: If the node has children you *MUST* use the reference operator!
   *       E.g. $clonedNode =& cloneNode($node);
   *       Otherwise the children will not point back to the parent, they will point 
   *       back to your temporary variable instead.
   *
   * @param   $node (mixed)  Either a node (hash array) or an abs. Xpath to a node in 
   *                         the current doc
   * @return        (&array) A node and it's child nodes.
   */
  function &cloneNode($node, $recursive=FALSE) {
    if (is_string($node) AND isSet($this->nodeIndex[$node])) {
      $node = $this->nodeIndex[$node];
    }
    // Copy the text-parts ()
    $textParts = $node['textParts'];
    $node['textParts'] = array();
    foreach ($textParts as $key => $val) {
      $node['textParts'][] = $val;
    }
    
    $childSize = sizeOf($node['childNodes']);
    for ($i=0; $i<$childSize; $i++) {
      $childNode =& $this->cloneNode($node['childNodes'][$i], TRUE);  // copy child 
      $node['childNodes'][$i] =& $childNode; // reference the copy
      $childNode['parentNode'] =& $node;      // child references the parent.
    }
    
    if (!$recursive) {
      //$node['childNodes'][0]['parentNode'] = null;
      //print "<pre>";
      //var_dump($node);
    }
    return $node;
  }
  
  
/** Nice to have but __sleep() has a bug. 
    (2002-2 PHP V4.1. See bug #15350)
  
  /**
   * PHP cals this function when you call PHP's serialize. 
   *
   * It prevents cyclic referencing, which is why print_r() of an XPath object doesn't work.
   *
  function __sleep() {
    // Destroy recursive pointers
    $keys = array_keys($this->nodeIndex);
    $size = sizeOf($keys);
    for ($i=0; $i<$size; $i++) {
      unset($this->nodeIndex[$keys[$i]]['parentNode']);
    }
    unset($this->nodeIndex);
  }
  
  /**
   * PHP cals this function when you call PHP's unserialize. 
   *
   * It reindexes the node-tree
   *
  function __wakeup() {
    $this->reindexNodeTree();
  }
  
*/
  
  //-----------------------------------------------------------------------------------------
  // XPath            ------  XPath Query / Evaluation Handlers  ------                      
  //-----------------------------------------------------------------------------------------
  
  /**
   * Matches (evaluates) an XPath query
   *
   * This method tries to evaluate an XPath query by parsing it. A XML source must 
   * have been imported before this method is able to work.
   *
   * @param     $xPathQuery  (string) XPath query to be evaluated.
   * @param     $baseXPath   (string) (default is super-root) XPath query to a single document node, 
   *                                  from which the XPath query should  start evaluating.
   * @return                 (mixed)  The result of the XPath expression.  Either:
   *                                    node-set (an ordered collection of absolute references to nodes without duplicates) 
   *                                    boolean (true or false) 
   *                                    number (a floating-point number) 
   *                                    string (a sequence of UCS characters) 
   */
  function match($xPathQuery, $baseXPath='') {
    if ($this->_indexIsDirty) $this->reindexNodeTree();
    
    // Replace a double slashes, because they'll cause problems otherwise.
    static $slashes2descendant = array(
        '//@' => '/descendant_or_self::*/attribute::', 
        '//'  => '/descendant_or_self::node()/', 
        '/@'  => '/attribute::');
    // Stupid idea from W3C to take axes name containing a '-' (dash) !!!
    // We replace the '-' with '_' to avoid the conflict with the minus operator.
    static $dash2underscoreHash = array( 
        '-sibling'    => '_sibling', 
        '-or-'        => '_or_',
        'starts-with' => 'starts_with', 
        'substring-before' => 'substring_before',
        'substring-after'  => 'substring_after', 
        'string-length'    => 'string_length',
        'normalize-space'  => 'normalize_space',
        'x-lower'          => 'x_lower',
        'x-upper'          => 'x_upper',
        'generate-id'      => 'generate_id');
    
    if (empty($xPathQuery)) return array();

    // Special case for when document is empty.
    if (empty($this->nodeRoot)) return array();

    if (!isSet($this->nodeIndex[$baseXPath])) {
            $xPathSet = $this->_resolveXPathQuery($baseXPath,'match');
            if (sizeOf($xPathSet) !== 1) {
                $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
                return FALSE;
            }
            $baseXPath = $xPathSet[0];
    }

    // We should possibly do a proper syntactical parse, but instead we will cheat and just
    // remove any literals that could make things very difficult for us, and replace them with
    // special tags.  Then we can treat the xPathQuery much more easily as JUST "syntax".  Provided 
    // there are no literals in the string, then we can guarentee that most of the operators and 
    // syntactical elements are indeed elements and not just part of a literal string.
    $processedxPathQuery = $this->_removeLiterals($xPathQuery);
    
    // Replace a double slashes, and '-' (dash) in axes names.
    $processedxPathQuery = strtr($processedxPathQuery, $slashes2descendant);
    $processedxPathQuery = strtr($processedxPathQuery, $dash2underscoreHash);

    // Build the context
    $context = array('nodePath' => $baseXPath, 'pos' => 1, 'size' => 1);

    // The primary syntactic construct in XPath is the expression.
    $result = $this->_evaluateExpr($processedxPathQuery, $context);

    // We might have been returned a string.. If so convert back to a literal
    $literalString = $this->_asLiteral($result);
    if ($literalString != FALSE) return $literalString;
    else return $result;
  }

  /**
   * Alias for the match function
   *
   * @see match()
   */
  function evaluate($xPathQuery, $baseXPath='') {
    return $this->match($xPathQuery, $baseXPath);
  }

  /**
   * Parse out the literals of an XPath expression.
   *
   * Instead of doing a full lexical parse, we parse out the literal strings, and then
   * Treat the sections of the string either as parts of XPath or literal strings.  So
   * this function replaces each literal it finds with a literal reference, and then inserts
   * the reference into an array of strings that we can access.  The literals can be accessed
   * later from the literals associative array.
   *
   * Example:
   *  XPathExpr = /AAA[@CCC = "hello"]/BBB[DDD = 'world'] 
   *  =>  literals: array("hello", "world")
   *      return value: /AAA[@CCC = $1]/BBB[DDD = $2] 
   *
   * Note: This does not interfere with the VariableReference syntactical element, as these 
   * elements must not start with a number.
   *
   * @param  $xPathQuery  (string) XPath expression to be processed
   * @return              (string) The XPath expression without the literals.
   *                              
   */
  function _removeLiterals($xPathQuery) {
    // What comes first?  A " or a '?
    if (!preg_match(":^([^\"']*)([\"'].*)$:", $xPathQuery, $aMatches)) {
      // No " or ' means no more literals.
      return $xPathQuery;
    }
    
    $result = $aMatches[1];
    $remainder = $aMatches[2];
    // What kind of literal?
    if (preg_match(':^"([^"]*)"(.*)$:', $remainder, $aMatches)) {
      // A "" literal.
      $literal = $aMatches[1];
      $remainder = $aMatches[2];
    } else if (preg_match(":^'([^']*)'(.*)$:", $remainder, $aMatches)) {
      // A '' literal.
      $literal = $aMatches[1];
      $remainder = $aMatches[2];
    } else {
      $this->_displayError("The '$xPathQuery' argument began a literal, but did not close it.", __LINE__, __FILE__);
    }

    // Store the literal
    $literalNumber = count($this->axPathLiterals);
    $this->axPathLiterals[$literalNumber] = $literal;
    $result .= '$'.$literalNumber;
    return $result.$this->_removeLiterals($remainder);
  }

  /**
   * Returns the given string as a literal reference.
   *
   * @param $string (string) The string that we are processing
   * @return        (mixed)  The literal string.  FALSE if the string isn't a literal reference.
   */
  function _asLiteral($string) {
    if (empty($string)) return FALSE;
    if (empty($string[0])) return FALSE;
    if ($string[0] == '$') {
      $remainder = substr($string, 1);
      if (is_numeric($remainder)) {
        // We have a string reference then.
        $stringNumber = (int)$remainder;
        if ($stringNumber >= count($this->axPathLiterals)) {
            $this->_displayError("Internal error.  Found a string reference that we didn't set in xPathQuery: '$xPathQuery'.", __LINE__, __FILE__);
            return FALSE;
        }
        return $this->axPathLiterals[$stringNumber];
      }
    }

    // It's not a reference then.
    return FALSE;
  }
  
  /**
   * Adds a literal to our array of literals
   *
   * In order to make sure we don't interpret literal strings as XPath expressions, we have to
   * encode literal strings so that we know that they are not XPaths.
   *
   * @param $string (string) The literal string that we need to store for future access
   * @return        (mixed)  A reference string to this literal.
   */
  function _addLiteral($string) {
    // Store the literal
    $literalNumber = count($this->axPathLiterals);
    $this->axPathLiterals[$literalNumber] = $string;
    $result = '$'.$literalNumber;
    return $result;
  }

  /**
   * Look for operators in the expression
   *
   * Parses through the given expression looking for operators.  If found returns
   * the operands and the operator in the resulting array.
   *
   * @param  $xPathQuery  (string) XPath query to be evaluated.
   * @return              (array)  If an operator is found, it returns an array containing
   *                               information about the operator.  If no operator is found
   *                               then it returns an empty array.  If an operator is found,
   *                               but has invalid operands, it returns FALSE.
   *                               The resulting array has the following entries:
   *                                'operator' => The string version of operator that was found,
   *                                              trimmed for whitespace
   *                                'left operand' => The left operand, or empty if there was no
   *                                              left operand for this operator.
   *                                'right operand' => The right operand, or empty if there was no
   *                                              right operand for this operator.
   */
  function _GetOperator($xPathQuery) {
    $position = 0;
    $operator = '';

    // The results of this function can easily be cached.
    static $aResultsCache = array();
    if (isset($aResultsCache[$xPathQuery])) {
      return $aResultsCache[$xPathQuery];
    }

    // Run through all operators and try to find one.
    $opSize = sizeOf($this->operators);
    for ($i=0; $i<$opSize; $i++) {
      // Pick an operator to try.
      $operator = $this->operators[$i];
      // Quickcheck. If not present don't wast time searching 'the hard way'
      if (strpos($xPathQuery, $operator)===FALSE) continue;
      // Special check
      $position = $this->_searchString($xPathQuery, $operator);
      // Check whether a operator was found.
      if ($position <= 0 ) continue;

      // Check whether it's the equal operator.
      if ($operator == '=') {
        // Also look for other operators containing the equal sign.
        switch ($xPathQuery[$position-1]) {
          case '<' : 
            $position--;
            $operator = '<=';
            break;
          case '>' : 
            $position--;
            $operator = '>=';
            break;
          case '!' : 
            $position--;
            $operator = '!=';
            break;
          default:
            // It's a pure = operator then.
        }
        break;
      }

      if ($operator == '*') {
        // http://www.w3.org/TR/xpath#exprlex:
        // "If there is a preceding token and the preceding token is not one of @, ::, (, [, 
        // or an Operator, then a * must be recognized as a MultiplyOperator and an NCName must 
        // be recognized as an OperatorName."

        // Get some substrings.
        $character = substr($xPathQuery, $position - 1, 1);
      
        // Check whether it's a multiply operator or a name test.
        if (strchr('/@:([', $character) != FALSE) {
          // Don't use the operator.
            $position = -1;
          continue;
        } else {
          // The operator is good.  Lets use it.
          break;
        }
      }

      // Extremely annoyingly, we could have a node name like "for-each" and we should not
      // parse this as a "-" operator.  So if the first char of the right operator is alphabetic,
      // then this is NOT an interger operator.
      if (strchr('-+*', $operator) != FALSE) {
        $rightOperand = trim(substr($xPathQuery, $position + strlen($operator)));
        if (strlen($rightOperand) > 1) {
          if (preg_match(':^\D$:', $rightOperand[0])) {
            // Don't use the operator.
            $position = -1;
            continue;
          } else {
            // The operator is good.  Lets use it.
            break;
          }
        }
      }

      // The operator must be good then :o)
      break;

    } // end while each($this->operators)

    // Did we find an operator?
    if ($position == -1) {
      $aResultsCache[$xPathQuery] = array();
      return array();
    }

    /////////////////////////////////////////////
    // Get the operands

    // Get the left and the right part of the expression.
    $leftOperand  = trim(substr($xPathQuery, 0, $position));
    $rightOperand = trim(substr($xPathQuery, $position + strlen($operator)));
  
    // Remove whitespaces.
    $leftOperand  = trim($leftOperand);
    $rightOperand = trim($rightOperand);

    /////////////////////////////////////////////
    // Check the operands.

    if ($leftOperand == '') {
      $aResultsCache[$xPathQuery] = FALSE;
      return FALSE;
    }

    if ($rightOperand == '') {
      $aResultsCache[$xPathQuery] = FALSE;
      return FALSE;
    }

    // Package up and return what we found.
    $aResult = array('operator' => $operator,
                'left operand' => $leftOperand,
                'right operand' => $rightOperand);

    $aResultsCache[$xPathQuery] = $aResult;

    return $aResult;
  }

  /**
   * Evaluates an XPath PrimaryExpr
   *
   * http://www.w3.org/TR/xpath#section-Basics
   *
   *  [15]    PrimaryExpr    ::= VariableReference  
   *                             | '(' Expr ')'  
   *                             | Literal  
   *                             | Number  
   *                             | FunctionCall 
   *
   * @param  $xPathQuery  (string)   XPath query to be evaluated.
   * @param  $context     (array)    The context from which to evaluate
   * @param  $results     (mixed)    If the expression could be parsed and evaluated as one of these
   *                                 syntactical elements, then this will be either:
   *                                    - node-set (an ordered collection of nodes without duplicates) 
   *                                    - boolean (true or false) 
   *                                    - number (a floating-point number) 
   *                                    - string (a sequence of UCS characters) 
   * @return              (string)    An empty string if the query was successfully parsed and 
   *                                  evaluated, else a string containing the reason for failing.
   * @see    evaluate()
   */
  function _evaluatePrimaryExpr($xPathQuery, $context, &$result) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluatePrimaryExpr', $this->aDebugFunctions);
    
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_evaluatePrimaryExpr");
      echo "Path: $xPathQuery\n";
      echo "Context:";
      $this->_printContext($context);
      echo "\n";
    }

    // Certain expressions will never be PrimaryExpr, so to speed up processing, cache the
    // results we do find from this function.
    static $aResultsCache = array();
    
    // Do while false loop
    $error = "";
    // If the result is independant of context, then we can cache the result and speed this function
    // up on future calls.
    $bCacheableResult = FALSE;
    do {
      if (isset($aResultsCache[$xPathQuery])) {
        $error = $aResultsCache[$xPathQuery]['Error'];
        $result = $aResultsCache[$xPathQuery]['Result'];
        break;
      }

      // VariableReference 
      // ### Not supported.

      // Is it a number?
      // | Number  
      if (is_numeric($xPathQuery)) {
        $result = doubleval($xPathQuery);
        $bCacheableResult = TRUE;
        break;
      }

      // If it starts with $, and the remainder is a number, then it's a string.
      // | Literal  
      $literal = $this->_asLiteral($xPathQuery);
      if ($literal !== FALSE) {
        $result = $xPathQuery;
        $bCacheableResult = TRUE;
        break;
      }

      // Is it a function?
      // | FunctionCall 
      {
        // Check whether it's all wrapped in a function.  will be like count(.*) where .* is anything
        // text() will try to be matched here, so just explicitly ignore it
        $regex = ":^([^\(\)\[\]/]*)\s*\((.*)\)$:U";
        if (preg_match($regex, $xPathQuery, $aMatch) && $xPathQuery != "text()") {
          $function = $aMatch[1];
          $data     = $aMatch[2];
          // It is possible that we will get "a() or b()" which will match as function "a" with
          // arguments ") or b(" which is clearly wrong... _bracketsCheck() should catch this.
          if ($this->_bracketsCheck($data)) {
            if (in_array($function, $this->functions)) {
              if ($bDebugThisFunction) echo "XPathExpr: $xPathQuery is a $function() function call:\n";
              $result = $this->_evaluateFunction($function, $data, $context);
              break;
            } 
          }
        }
      }

      // Is it a bracketed expression?
      // | '(' Expr ')'  
      // If it is surrounded by () then trim the brackets
      $bBrackets = FALSE;
      if (preg_match(":^\((.*)\):", $xPathQuery, $aMatches)) {
        // Do not keep trimming off the () as we could have "(() and ())"
        $bBrackets = TRUE;
        $xPathQuery = $aMatches[1];
      }

      if ($bBrackets) {
        // Must be a Expr then.
        $result = $this->_evaluateExpr($xPathQuery, $context);
        break;
      }

      // Can't be a PrimaryExpr then.
      $error = "Expression is not a PrimaryExpr";
      $bCacheableResult = TRUE;
    } while (FALSE);
    //////////////////////////////////////////////    

    // If possible, cache the result.
    if ($bCacheableResult) {
        $aResultsCache[$xPathQuery]['Error'] = $error;
        $aResultsCache[$xPathQuery]['Result'] = $result;
    }

    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, array('result' => $result, 'error' => $error));
    }
    // Return the result.
    return $error;
  }

  /**
   * Evaluates an XPath Expr
   *
   * $this->evaluate() is the entry point and does some inits, while this 
   * function is called recursive internaly for every sub-xPath expresion we find.
   * It handles the following syntax, and calls evaluatePathExpr if it finds that none
   * of this grammer applies.
   *
   * http://www.w3.org/TR/xpath#section-Basics
   *
   * [14]    Expr               ::= OrExpr 
   * [21]    OrExpr             ::= AndExpr  
   *                                | OrExpr 'or' AndExpr  
   * [22]    AndExpr            ::= EqualityExpr  
   *                                | AndExpr 'and' EqualityExpr  
   * [23]    EqualityExpr       ::= RelationalExpr  
   *                                | EqualityExpr '=' RelationalExpr  
   *                                | EqualityExpr '!=' RelationalExpr  
   * [24]    RelationalExpr     ::= AdditiveExpr  
   *                                | RelationalExpr '<' AdditiveExpr  
   *                                | RelationalExpr '>' AdditiveExpr  
   *                                | RelationalExpr '<=' AdditiveExpr  
   *                                | RelationalExpr '>=' AdditiveExpr  
   * [25]    AdditiveExpr       ::= MultiplicativeExpr  
   *                                | AdditiveExpr '+' MultiplicativeExpr  
   *                                | AdditiveExpr '-' MultiplicativeExpr  
   * [26]    MultiplicativeExpr ::= UnaryExpr  
   *                                | MultiplicativeExpr MultiplyOperator UnaryExpr  
   *                                | MultiplicativeExpr 'div' UnaryExpr  
   *                                | MultiplicativeExpr 'mod' UnaryExpr  
   * [27]    UnaryExpr          ::= UnionExpr  
   *                                | '-' UnaryExpr 
   * [18]    UnionExpr          ::= PathExpr  
   *                                | UnionExpr '|' PathExpr 
   *
   * NOTE: The effect of the above grammar is that the order of precedence is 
   * (lowest precedence first): 
   * 1) or 
   * 2) and 
   * 3) =, != 
   * 4) <=, <, >=, > 
   * 5) +, -
   * 6) *, div, mod
   * 7) - (negate)
   * 8) |
   *
   * @param  $xPathQuery  (string)   XPath query to be evaluated.
   * @param  $context     (array)    An associative array the describes the context from which
   *                                 to evaluate the XPath Expr.  Contains three members:
   *                                  'nodePath' => The absolute XPath expression to the context node
   *                                  'size' => The context size
   *                                  'pos' => The context position
   * @return              (mixed)    The result of the XPath expression.  Either:
   *                                 node-set (an ordered collection of nodes without duplicates) 
   *                                 boolean (true or false) 
   *                                 number (a floating-point number) 
   *                                 string (a sequence of UCS characters) 
   * @see    evaluate()
   */
  function _evaluateExpr($xPathQuery, $context) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluateExpr', $this->aDebugFunctions);
    
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_evaluateExpr");
      echo "Path: $xPathQuery\n";
      echo "Context:";
      $this->_printContext($context);
      echo "\n";    
    }

    // Numpty check
    if (!isset($xPathQuery) || ($xPathQuery == '')) {
      $this->_displayError("The \$xPathQuery argument must have a value.", __LINE__, __FILE__);
      return FALSE;
    }

    // At the top level we deal with booleans.  Only if the Expr is just an AdditiveExpr will 
    // the result not be a boolean.
    //
    //
    // Between these syntactical elements we get PathExprs.

    // Do while false loop
    do {
      static $aKnownPathExprCache = array();

      if (isset($aKnownPathExprCache[$xPathQuery])) {
        if ($bDebugThisFunction) echo "XPathExpr is a PathExpr\n";
        $result = $this->_evaluatePathExpr($xPathQuery, $context);
        break;
      }

      // Check for operators first, as we could have "() op ()" and the PrimaryExpr will try to
      // say that that is an Expr called ") op ("
      // Set the default position and the type of the operator.
      $aOperatorInfo = $this->_GetOperator($xPathQuery);

      // An expression can be one of these, and we should catch these "first" as they are most common
      if (empty($aOperatorInfo)) {
        $error = $this->_evaluatePrimaryExpr($xPathQuery, $context, $result);
        if (empty($error)) {
          // It could be parsed as a PrimaryExpr, so look no further :o)
          break;
        }
      }

      // Check whether an operator was found.
      if (empty($aOperatorInfo)) {
        if ($bDebugThisFunction) echo "XPathExpr is a PathExpr\n";
        $aKnownPathExprCache[$xPathQuery] = TRUE;
        // No operator.  Means we have a PathExpr then.  Go to the next level.
        $result = $this->_evaluatePathExpr($xPathQuery, $context);
        break;
      } 

      if ($bDebugThisFunction) { echo "\nFound and operator:"; print_r($aOperatorInfo); }//LEFT:[$leftOperand]  oper:[$operator]  RIGHT:[$rightOperand]";

      $operator = $aOperatorInfo['operator'];

      /////////////////////////////////////////////
      // Recursively process the operator

      // Check the kind of operator.
      switch ($operator) {
        case ' or ': 
        case ' and ':
          $operatorType = 'Boolean';
          break;
        case '+': 
        case '-': 
        case '*':
        case ' div ':
        case ' mod ':
          $operatorType = 'Integer';
          break;
        case ' | ':
          $operatorType = 'NodeSet';
          break;
        case '<=':
        case '<': 
        case '>=':
        case '>':
        case '=': 
        case '!=':
          $operatorType = 'Multi';
          break;
        default:
            $this->_displayError("Internal error.  Default case of switch statement reached.", __LINE__, __FILE__);
      }

      if ($bDebugThisFunction) echo "\nOperator is a [$operator]($operatorType operator)";

      /////////////////////////////////////////////
      // Evaluate the operands

      // Evaluate the left part.
      if ($bDebugThisFunction) echo "\nEvaluating LEFT:[{$aOperatorInfo['left operand']}]\n";
      $left = $this->_evaluateExpr($aOperatorInfo['left operand'], $context);
      if ($bDebugThisFunction) {echo "{$aOperatorInfo['left operand']} evals as:\n"; print_r($left); }
      
      // If it is a boolean operator, it's possible we don't need to evaluate the right part.

      // Only evaluate the right part if we need to.
      $right = '';
      if ($operatorType == 'Boolean') {
        // Is the left part false?
        $left = $this->_handleFunction_boolean($left, $context);
        if (!$left and ($operator == ' and ')) {
          $result = FALSE;
          break;
        } else if ($left and ($operator == ' or ')) {
          $result = TRUE;
          break;
        }
      } 

      // Evaluate the right part
      if ($bDebugThisFunction) echo "\nEvaluating RIGHT:[{$aOperatorInfo['right operand']}]\n";
      $right = $this->_evaluateExpr($aOperatorInfo['right operand'], $context);
      if ($bDebugThisFunction) {echo "{$aOperatorInfo['right operand']} evals as:\n"; print_r($right); echo "\n";}

      /////////////////////////////////////////////
      // Combine the operands

      // If necessary, work out how to treat the multi operators
      if ($operatorType != 'Multi') {
        $result = $this->_evaluateOperator($left, $operator, $right, $operatorType, $context);
      } else {
        // http://www.w3.org/TR/xpath#booleans
        // If both objects to be compared are node-sets, then the comparison will be true if and 
        // only if there is a node in the first node-set and a node in the second node-set such 
        // that the result of performing the comparison on the string-values of the two nodes is 
        // true. 
        // 
        // If one object to be compared is a node-set and the other is a number, then the 
        // comparison will be true if and only if there is a node in the node-set such that the 
        // result of performing the comparison on the number to be compared and on the result of 
        // converting the string-value of that node to a number using the number function is true. 
        //
        // If one object to be compared is a node-set and the other is a string, then the comparison 
        // will be true if and only if there is a node in the node-set such that the result of performing 
        // the comparison on the string-value of the node and the other string is true. 
        // 
        // If one object to be compared is a node-set and the other is a boolean, then the comparison 
        // will be true if and only if the result of performing the comparison on the boolean and on 
        // the result of converting the node-set to a boolean using the boolean function is true.
        if (is_array($left) || is_array($right)) {
          if ($bDebugThisFunction) echo "As one of the operands is an array, we will need to loop\n";
          if (is_array($left) && is_array($right)) {
            $operatorType = 'String';
          } elseif (is_numeric($left) || is_numeric($right)) {
            $operatorType = 'Integer';
          } elseif (is_bool($left)) {
            $operatorType = 'Boolean';
            $right = $this->_handleFunction_boolean($right, $context);
          } elseif (is_bool($right)) {
            $operatorType = 'Boolean';
            $left = $this->_handleFunction_boolean($left, $context);
          } else {
            $operatorType = 'String';
          }
          if ($bDebugThisFunction) echo "Equals operator is a $operatorType operator\n";
          // Turn both operands into arrays to simplify logic
          $aLeft = $left;
          $aRight = $right;
          if (!is_array($aLeft)) $aLeft = array($aLeft);
          if (!is_array($aRight)) $aRight = array($aRight);
          $result = FALSE;
          if (!empty($aLeft)) {
            foreach ($aLeft as $leftItem) {
              if (empty($aRight)) break;
              // If the item is from a node set, we should evaluate it's string-value
              if (is_array($left)) {
                if ($bDebugThisFunction) echo "\tObtaining string-value of LHS:$leftItem as it's from a nodeset\n";
                $leftItem = $this->_stringValue($leftItem);
              }
              foreach ($aRight as $rightItem) {
                // If the item is from a node set, we should evaluate it's string-value
                if (is_array($right)) {
                  if ($bDebugThisFunction) echo "\tObtaining string-value of RHS:$rightItem as it's from a nodeset\n";
                  $rightItem = $this->_stringValue($rightItem);
                }

                if ($bDebugThisFunction) echo "\tEvaluating $leftItem $operator $rightItem\n";
                $result = $this->_evaluateOperator($leftItem, $operator, $rightItem, $operatorType, $context);
                if ($result === TRUE) break;
              }
              if ($result === TRUE) break;
            }
          }
        } 
        // When neither object to be compared is a node-set and the operator is = or !=, then the 
        // objects are compared by converting them to a common type as follows and then comparing 
        // them. 
        //
        // If at least one object to be compared is a boolean, then each object to be compared 
        // is converted to a boolean as if by applying the boolean function. 
        //
        // Otherwise, if at least one object to be compared is a number, then each object to be 
        // compared is converted to a number as if by applying the number function. 
        //
        // Otherwise, both objects to be compared are converted to strings as if by applying 
        // the string function. 
        //  
        // The = comparison will be true if and only if the objects are equal; the != comparison 
        // will be true if and only if the objects are not equal. Numbers are compared for equality 
        // according to IEEE 754 [IEEE 754]. Two booleans are equal if either both are true or 
        // both are false. Two strings are equal if and only if they consist of the same sequence 
        // of UCS characters.
        else {
          if (is_bool($left) || is_bool($right)) {
            $operatorType = 'Boolean';
          } elseif (is_numeric($left) || is_numeric($right)) {
            $operatorType = 'Integer';
          } else {
            $operatorType = 'String';
          }
          if ($bDebugThisFunction) echo "Equals operator is a $operatorType operator\n";
          $result = $this->_evaluateOperator($left, $operator, $right, $operatorType, $context);
        }
      }

    } while (FALSE);
    //////////////////////////////////////////////

    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the result.
    return $result;
  }

  /**
   * Evaluate the result of an operator whose operands have been evaluated
   *
   * If the operator type is not "NodeSet", then neither the left or right operators 
   * will be node sets, as the processing when one or other is an array is complex,
   * and should be handled by the caller.
   *
   * @param  $left          (mixed)   The left operand
   * @param  $right         (mixed)   The right operand
   * @param  $operator      (string)  The operator to use to combine the operands
   * @param  $operatorType  (string)  The type of the operator.  Either 'Boolean', 
   *                                  'Integer', 'String', or 'NodeSet'
   * @param  $context     (array)    The context from which to evaluate
   * @return              (mixed)    The result of the XPath expression.  Either:
   *                                 node-set (an ordered collection of nodes without duplicates) 
   *                                 boolean (true or false) 
   *                                 number (a floating-point number) 
   *                                 string (a sequence of UCS characters) 
   */
  function _evaluateOperator($left, $operator, $right, $operatorType, $context) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluateOperator', $this->aDebugFunctions);
    
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_evaluateOperator");
      echo "left: $left\n";
      echo "right: $right\n";
      echo "operator: $operator\n";
      echo "operator type: $operatorType\n";
    }

    // Do while false loop
    do {
      // Handle the operator depending on the operator type.
      switch ($operatorType) {
        case 'Boolean':
          {
            // Boolify the arguments.  (The left arg is already a bool)
            $right = $this->_handleFunction_boolean($right, $context);
            switch ($operator) {
              case '=': // Compare the two results.
                $result = (bool)($left == $right); 
                break;
              case ' or ': // Return the two results connected by an 'or'.
                $result = (bool)( $left or $right );
                break;
              case ' and ': // Return the two results connected by an 'and'.
                $result = (bool)( $left and $right );
                break;
              case '!=': // Check whether the two results are not equal.
                $result = (bool)( $left != $right );
                break;
              default:
                $this->_displayError("Internal error.  Default case of switch statement reached.", __LINE__, __FILE__);
            }
          }
          break;
        case 'Integer':
          {
            // Convert both left and right operands into numbers.
            if (empty($left) && ($operator == '-')) {
              // There may be no left operator if the op is '-'
              $left = 0;
            } else {
              $left = $this->_handleFunction_number($left, $context);
            }
            $right = $this->_handleFunction_number($right, $context);
            if ($bDebugThisFunction) echo "\nLeft is $left, Right is $right\n";
            switch ($operator) {
              case '=': // Compare the two results.
                $result = (bool)($left == $right); 
                break;
              case '!=': // Compare the two results.
                $result = (bool)($left != $right); 
                break;
              case '+': // Return the result by adding one result to the other.
                $result = $left + $right;
                break;
              case '-': // Return the result by decrease one result by the other.
                $result = $left - $right;
                break;
              case '*': // Return a multiplication of the two results.
                $result =  $left * $right;
                break;
              case ' div ': // Return a division of the two results.
                $result = $left / $right;
                break;
              case ' mod ': // Return a modulo division of the two results.
                $result = $left % $right;
                break;
              case '<=': // Compare the two results.
                $result = (bool)( $left <= $right );
                break;
              case '<': // Compare the two results.
                $result = (bool)( $left < $right );
                break;
              case '>=': // Compare the two results.
                $result = (bool)( $left >= $right );
                break;
              case '>': // Compare the two results.
                $result = (bool)( $left > $right );
                break;
              default:
                $this->_displayError("Internal error.  Default case of switch statement reached.", __LINE__, __FILE__);
            }
          }
          break;
        case 'NodeSet':
          // Add the nodes to the result set
          $result = array_merge($left, $right);
          // Remove duplicated nodes.
          $result = array_unique($result);

          // Preserve doc order if there was more than one query.
          if (count($result) > 1) {
            $result = $this->_sortByDocOrder($result);
          }
          break;
        case 'String':
            $left = $this->_handleFunction_string($left, $context);
            $right = $this->_handleFunction_string($right, $context);
            if ($bDebugThisFunction) echo "\nLeft is $left, Right is $right\n";
            switch ($operator) {
              case '=': // Compare the two results.
                $result = (bool)($left == $right); 
                break;
              case '!=': // Compare the two results.
                $result = (bool)($left != $right); 
                break;
              default:
                $this->_displayError("Internal error.  Default case of switch statement reached.", __LINE__, __FILE__);
            }
          break;
        default:
          $this->_displayError("Internal error.  Default case of switch statement reached.", __LINE__, __FILE__);
      }
    } while (FALSE);

    //////////////////////////////////////////////

    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the result.
    return $result;
  }
  
  /**
   * Evaluates an XPath PathExpr
   *
   * It handles the following syntax:
   *
   * http://www.w3.org/TR/xpath#node-sets
   * http://www.w3.org/TR/xpath#NT-LocationPath
   * http://www.w3.org/TR/xpath#path-abbrev
   * http://www.w3.org/TR/xpath#NT-Step
   *
   * [19]   PathExpr              ::= LocationPath  
   *                                  | FilterExpr  
   *                                  | FilterExpr '/' RelativeLocationPath  
   *                                  | FilterExpr '//' RelativeLocationPath
   * [20]   FilterExpr            ::= PrimaryExpr  
   *                                  | FilterExpr Predicate 
   * [1]    LocationPath          ::= RelativeLocationPath  
   *                                  | AbsoluteLocationPath  
   * [2]    AbsoluteLocationPath  ::= '/' RelativeLocationPath?  
   *                                  | AbbreviatedAbsoluteLocationPath
   * [3]    RelativeLocationPath  ::= Step  
   *                                  | RelativeLocationPath '/' Step  
   *                                  | AbbreviatedRelativeLocationPath
   * [4]    Step                  ::= AxisSpecifier NodeTest Predicate*  
   *                                  | AbbreviatedStep  
   * [5]    AxisSpecifier         ::= AxisName '::'  
   *                                  | AbbreviatedAxisSpecifier  
   * [10]   AbbreviatedAbsoluteLocationPath
   *                              ::= '//' RelativeLocationPath
   * [11]   AbbreviatedRelativeLocationPath
   *                              ::= RelativeLocationPath '//' Step
   * [12]   AbbreviatedStep       ::= '.'  
   *                                  | '..'  
   * [13]   AbbreviatedAxisSpecifier    
   *                              ::= '@'? 
   *
   * If you expand all the abbreviated versions, then the grammer simplifies to:
   *
   * [19]   PathExpr              ::= RelativeLocationPath  
   *                                  | '/' RelativeLocationPath?  
   *                                  | FilterExpr  
   *                                  | FilterExpr '/' RelativeLocationPath  
   * [20]   FilterExpr            ::= PrimaryExpr  
   *                                  | FilterExpr Predicate 
   * [3]    RelativeLocationPath  ::= Step  
   *                                  | RelativeLocationPath '/' Step  
   * [4]    Step                  ::= AxisName '::' NodeTest Predicate*  
   *
   * Conceptually you can say that we should split by '/' and try to treat the parts
   * as steps, and if that fails then try to treat it as a PrimaryExpr.  
   * 
   * @param  $PathExpr   (string) PathExpr syntactical element
   * @param  $context    (array)  The context from which to evaluate
   * @return             (mixed)  The result of the XPath expression.  Either:
   *                               node-set (an ordered collection of nodes without duplicates) 
   *                               boolean (true or false) 
   *                               number (a floating-point number) 
   *                               string (a sequence of UCS characters) 
   * @see    evaluate()
   */
  function _evaluatePathExpr($PathExpr, $context) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluatePathExpr', $this->aDebugFunctions);
    
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_evaluatePathExpr");
      echo "PathExpr: $PathExpr\n";
      echo "Context:";
      $this->_printContext($context);
      echo "\n";
    }
    
    // Numpty check
    if (empty($PathExpr)) {
      $this->_displayError("The \$PathExpr argument must have a value.", __LINE__, __FILE__);
      return FALSE;
    }
    //////////////////////////////////////////////

    // Parsing the expression into steps is a cachable operation as it doesn't depend on the context
    static $aResultsCache = array();

    if (isset($aResultsCache[$PathExpr])) {
      $steps = $aResultsCache[$PathExpr];
    } else {
      // Note that we have used $this->slashes2descendant to simplify this logic, so the 
      // "Abbreviated" paths basically never exist as '//' never exists.

      // mini syntax check
      if (!$this->_bracketsCheck($PathExpr)) {
        $this->_displayError('While parsing an XPath query, in the PathExpr "' .
        $PathExpr.
        '", there was an invalid number of brackets or a bracket mismatch.', __LINE__, __FILE__);
      }
      // Save the current path.
      $this->currentXpathQuery = $PathExpr;
      // Split the path at every slash *outside* a bracket.
      $steps = $this->_bracketExplode('/', $PathExpr);
      if ($bDebugThisFunction) { echo "<hr>Split the path '$PathExpr' at every slash *outside* a bracket.\n "; print_r($steps); }
      // Check whether the first element is empty.
      if (empty($steps[0])) {
        // Remove the first and empty element. It's a starting  '//'.
        array_shift($steps);
      }
      $aResultsCache[$PathExpr] = $steps;
    }

    // Start to evaluate the steps.
    // ### Consider implementing an evaluateSteps() function that removes recursion from
    // evaluateStep()
    $result = $this->_evaluateStep($steps, $context);

    // Preserve doc order if there was more than one result
    if (count($result) > 1) {
      $result = $this->_sortByDocOrder($result);
    }
    //////////////////////////////////////////////
    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the result.
    return $result;
  }

  /**
   * Sort an xPathSet by doc order.
   *
   * @param  $xPathSet (array) Array of full paths to nodes that need to be sorted
   * @return           (array) Array containing the same contents as $xPathSet, but
   *                           with the contents in doc order
   */
  function _sortByDocOrder($xPathSet) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_sortByDocOrder', $this->aDebugFunctions);

    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction(__LINE__.":_sortByDocOrder(xPathSet:[".count($xPathSet)."])");
      echo "xPathSet:\n";
      print_r($xPathSet);
      echo "<hr>\n";
    }
    //////////////////////////////////////////////

    $aResult = array();

    // Spot some common shortcuts.
    if (count($xPathSet) < 1) {
      $aResult = $xPathSet;
    } else {
      // Build an array of doc-pos indexes.
      $aDocPos = array();
      $nodeCount = count($this->nodeIndex);
      $aPaths = array_keys($this->nodeIndex);
      if ($bDebugThisFunction) {
        echo "searching for path indices in array_keys(this->nodeIndex)...\n";
        //print_r($aPaths);
      }

      // The last index we found.  In general the elements will be in groups
      // that are themselves in order.
      $iLastIndex = 0;
      foreach ($xPathSet as $path) {
        // Cycle round the nodes, starting at the last index, looking for the path.
        $foundNode = FALSE;
        for ($iIndex = $iLastIndex; $iIndex < $nodeCount + $iLastIndex; $iIndex++) {
          $iThisIndex = $iIndex % $nodeCount;
          if (!strcmp($aPaths[$iThisIndex],$path)) {
            // we have found the doc-position index of the path 
            $aDocPos[] = $iThisIndex;
            $iLastIndex = $iThisIndex;
            $foundNode = TRUE;
            break;
          }
        }
        if ($bDebugThisFunction) {
          if (!$foundNode)
            echo "Error: $path not found in \$this->nodeIndex\n";
          else 
            echo "Found node after ".($iIndex - $iLastIndex)." iterations\n";
        }
      }
      // Now count the number of doc pos we have and the number of results and
      // confirm that we have the same number of each.
      $iDocPosCount = count($aDocPos);
      $iResultCount = count($xPathSet);
      if ($iDocPosCount != $iResultCount) {
        if ($bDebugThisFunction) {
          echo "count(\$aDocPos)=$iDocPosCount; count(\$result)=$iResultCount\n";
          print_r(array_keys($this->nodeIndex));
        }
        $this->_displayError('Results from _InternalEvaluate() are corrupt.  '.
                                      'Do you need to call reindexNodeTree()?', __LINE__, __FILE__);
      }

      // Now sort the indexes.
      sort($aDocPos);

      // And now convert back to paths.
      $iPathCount = count($aDocPos);
      for ($iIndex = 0; $iIndex < $iPathCount; $iIndex++) {
        $aResult[] = $aPaths[$aDocPos[$iIndex]];
      }
    }

    // Our result from the function is this array.
    $result = $aResult;

    //////////////////////////////////////////////
    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the result.
    return $result;
  }

  /**
   * Evaluate a step from a XPathQuery expression at a specific contextPath.
   *
   * Steps are the arguments of a XPathQuery when divided by a '/'. A contextPath is a 
   * absolute XPath (or vector of XPaths) to a starting node(s) from which the step should 
   * be evaluated.
   *
   * @param  $steps        (array) Vector containing the remaining steps of the current 
   *                               XPathQuery expression.
   * @param  $context      (array) The context from which to evaluate
   * @return               (array) Vector of absolute XPath's as a result of the step 
   *                               evaluation.  The results will not necessarily be in doc order
   * @see    _evaluatePathExpr()
   */
  function _evaluateStep($steps, $context) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluateStep', $this->aDebugFunctions);

    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction(__LINE__.":_evaluateStep");
      echo "Context:";
      $this->_printContext($context);
      echo "\n";
      echo "Steps: ";
      print_r($steps);
      echo "<hr>\n";
    }
    //////////////////////////////////////////////

    $result = array(); // Create an empty array for saving the abs. XPath's found.

    $contextPaths = array();   // Create an array to save the new contexts.
    $step = trim(array_shift($steps)); // Get this step.
    if ($bDebugThisFunction) echo __LINE__.":Evaluating step $step\n";
    
    $axis = $this->_getAxis($step); // Get the axis of the current step.

    // If there was no axis, then it must be a PrimaryExpr
    if ($axis == FALSE) {
      if ($bDebugThisFunction) echo __LINE__.":Step is not an axis but a PrimaryExpr\n";
      // ### This isn't correct, as the result of this function might not be a node set.
      $error = $this->_evaluatePrimaryExpr($step, $context, $contextPaths);
      if (!empty($error)) {
        $this->_displayError("Expression failed to parse as PrimaryExpr because: $error"
                , __LINE__, __FILE__, FALSE);
      }
    } else {
      if ($bDebugThisFunction) { echo __LINE__.":Axis of step is:\n"; print_r($axis); echo "\n";}
      $method = '_handleAxis_' . $axis['axis']; // Create the name of the method.
    
      // Check whether the axis handler is defined. If not display an error message.
      if (!method_exists($this, $method)) {
        $this->_displayError('While parsing an XPath query, the axis ' .
        $axis['axis'] . ' could not be handled, because this version does not support this axis.', __LINE__, __FILE__);
      }
      if ($bDebugThisFunction) echo __LINE__.":Calling user method $method\n";        
      
      // Perform an axis action.
      $contextPaths = $this->$method($axis, $context['nodePath']);
      if ($bDebugThisFunction) { echo __LINE__.":We found these contexts from this step:\n"; print_r( $contextPaths ); echo "\n";}
    }

    // Check whether there are predicates.
    if (count($contextPaths) > 0 && count($axis['predicate']) > 0) {
      if ($bDebugThisFunction) echo __LINE__.":Filtering contexts by predicate...\n";
      
      // Check whether each node fits the predicates.
      $contextPaths = $this->_checkPredicates($contextPaths, $axis['predicate']);
    }

    // Check whether there are more steps left.
    if (count($steps) > 0) {
      if ($bDebugThisFunction) echo __LINE__.":Evaluating next step given the context of the first step...\n";        
      
      // Continue the evaluation of the next steps.

      // Run through the array.
      $size = sizeOf($contextPaths);
      for ($pos=0; $pos<$size; $pos++) {
        // Build new context
        $newContext = array('nodePath' => $contextPaths[$pos], 'size' => $size, 'pos' => $pos + 1);
        if ($bDebugThisFunction) echo __LINE__.":Evaluating step for the {$contextPaths[$pos]} context...\n";
        // Call this method for this single path.
        $xPathSetNew = $this->_evaluateStep($steps, $newContext);
        if ($bDebugThisFunction) {echo "New results for this context:\n"; print_r($xPathSetNew);}
        $result = array_merge($result, $xPathSetNew);
      }

      // Remove duplicated nodes.
      $result = array_unique($result);
    } else {
      $result = $contextPaths; // Save the found contexts.
    }
    
    //////////////////////////////////////////////
    if ($bDebugThisFunction) $this->_closeDebugFunction($aStartTime, $result);
    
    // Return the result.
    return $result;
  }
  
  /**
   * Checks whether a node matches predicates.
   *
   * This method checks whether a list of nodes passed to this method match
   * a given list of predicates. 
   *
   * @param  $xPathSet   (array)  Array of full paths of all nodes to be tested.
   * @param  $predicates (array)  Array of predicates to use.
   * @return             (array)  Vector of absolute XPath's that match the given predicates.
   * @see    _evaluateStep()
   */
  function _checkPredicates($xPathSet, $predicates) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_checkPredicates', $this->aDebugFunctions);

    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_checkPredicates(Nodes:[$xPathSet], Predicates:[$predicates])");
      echo "XPathSet:";
      print_r($xPathSet);
      echo "Predicates:";
      print_r($predicates);
      echo "<hr>";
    }
    //////////////////////////////////////////////
    // Create an empty set of nodes.
    $result = array();

    // Run through all predicates.
    $pSize = sizeOf($predicates);
    for ($j=0; $j<$pSize; $j++) {
      $predicate = $predicates[$j]; 
      if ($bDebugThisFunction) echo "Evaluating predicate \"$predicate\"\n";

      // This will contain all the nodes that match this predicate
      $aNewSet = array();
      
      // Run through all nodes.
      $contextSize = count($xPathSet);
      for ($contextPos=0; $contextPos<$contextSize; $contextPos++) {
        $xPath = $xPathSet[$contextPos];

        // Build the context for this predicate
        $context = array('nodePath' => $xPath, 'size' => $contextSize, 'pos' => $contextPos + 1);
      
        // Check whether the predicate is just an number.
        if (preg_match('/^\d+$/', $predicate)) {
          if ($bDebugThisFunction) echo "Taking short cut and calling _handleFunction_position() directly.\n";
          // Take a short cut.  If it is just a position, then call 
          // _handleFunction_position() directly.  70% of the
          // time this will be the case. ## N.S
//          $check = (bool) ($predicate == $context['pos']);
          $check = (bool) ($predicate == $this->_handleFunction_position('', $context));
        } else {                
          // Else do the predicate check the long and through way.
          $check = $this->_evaluateExpr($predicate, $context);
        }
        if ($bDebugThisFunction) {
          echo "Evaluating the predicate returned "; 
          var_dump($check); 
          echo "\n";
        }

        if (is_int($check)) { // Check whether it's an integer.
          // Check whether it's the current position.
          $check = (bool) ($check == $this->_handleFunction_position('', $context));
        } else {
          $check = (bool) ($this->_handleFunction_boolean($check, $context));
//          if ($bDebugThisFunction) {echo $this->_handleFunction_string($check, $context);}
        }

        if ($bDebugThisFunction) echo "Node $xPath matches predicate $predicate: " . (($check) ? "TRUE" : "FALSE") ."\n";

        // Do we add it?
        if ($check) $aNewSet[] = $xPath;
      }
       
      // Use the newly filtered list.
      $xPathSet = $aNewSet;

      if ($bDebugThisFunction) {echo "Node set now contains : "; print_r($xPathSet); }
    }

    $result = $xPathSet;

    //////////////////////////////////////////////
    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the array of nodes.
    return $result;
  }
  
  /**
   * Evaluates an XPath function
   *
   * This method evaluates a given XPath function with its arguments on a
   * specific node of the document.
   *
   * @param  $function      (string) Name of the function to be evaluated.
   * @param  $arguments     (string) String containing the arguments being
   *                                 passed to the function.
   * @param  $context       (array)  The context from which to evaluate
   * @return                (mixed)  This method returns the result of the evaluation of
   *                                 the function. Depending on the function the type of the 
   *                                 return value can be different.
   * @see    evaluate()
   */
  function _evaluateFunction($function, $arguments, $context) {
    // If you are having difficulty using this function.  Then set this to TRUE and 
    // you'll get diagnostic info displayed to the output.
    $bDebugThisFunction = in_array('_evaluateFunction', $this->aDebugFunctions);
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction("_evaluateFunction");
      if (is_array($arguments)) {
        echo "Arguments:\n";
        print_r($arguments);
      } else {
        echo "Arguments: $arguments\n";
      }
      echo "Context:";
      $this->_printContext($context);
      echo "\n";
      echo "<hr>\n";
    }
    /////////////////////////////////////
    // Remove whitespaces.
    $function  = trim($function);
    $arguments = trim($arguments);
    // Create the name of the function handling function.
    $method = '_handleFunction_'. $function;
    
    // Check whether the function handling function is available.
    if (!method_exists($this, $method)) {
      // Display an error message.
      $this->_displayError("While parsing an XPath query, ".
        "the function \"$function\" could not be handled, because this ".
        "version does not support this function.", __LINE__, __FILE__);
    }
    if ($bDebugThisFunction) echo "Calling function $method($arguments)\n"; 
    
    // Return the result of the function.
    $result = $this->$method($arguments, $context);
    
    //////////////////////////////////////////////
    // Return the nodes found.
    if ($bDebugThisFunction) {
      $this->_closeDebugFunction($aStartTime, $result);
    }
    // Return the result.
    return $result;
  }
    
  /**
   * Checks whether a node matches a node-test.
   *
   * This method checks whether a node in the document matches a given node-test.
   * A node test is something like text(), node(), or an element name.
   *
   * @param  $contextPath (string)  Full xpath of the node, which should be tested for 
   *                                matching the node-test.
   * @param  $nodeTest    (string)  String containing the node-test for the node.
   * @return              (boolean) This method returns TRUE if the node matches the 
   *                                node-test, otherwise FALSE.
   * @see    evaluate()
   */
  function _checkNodeTest($contextPath, $nodeTest) {
    // Empty node test means that it must match
//    if (empty($nodeTest)) return TRUE;

    if ($nodeTest == '*') {
      // * matches all element nodes.
      return (!preg_match(':/[^/]+\(\)\[\d+\]$:U', $contextPath));
    }
    elseif (preg_match('/^[\w-:\.]+$/', $nodeTest)) {
       // http://www.w3.org/TR/2000/REC-xml-20001006#NT-Name
       // The real spec for what constitutes whitespace is quite elaborate, and 
       // we currently just hope that "\w" catches them all.  In reality it should
       // start with a letter too, not a number, but we've just left it simple.
       // It's just a node name test.  It should end with "/$nodeTest[x]"
       return (preg_match('"/'.$nodeTest.'\[\d+\]$"', $contextPath));
    }
    elseif (preg_match('/\(/U', $nodeTest)) { // Check whether it's a function.
      // Get the type of function to use.
      $function = $this->_prestr($nodeTest, '(');
      // Check whether the node fits the method.
      switch ($function) {
        case 'node':   // Add this node to the list of nodes.
          return TRUE;
        case 'text':   // Check whether the node has some text.
          $tmp = implode('', $this->nodeIndex[$contextPath]['textParts']);
          if (!empty($tmp)) {
            return TRUE; // Add this node to the list of nodes.
          }
          break;
/******** NOT supported (yet?)          
        case 'comment':  // Check whether the node has some comment.
          if (!empty($this->nodeIndex[$contextPath]['comment'])) {
            return TRUE; // Add this node to the list of nodes.
          }
          break;
        case 'processing-instruction':
          $literal = $this->_afterstr($axis['node-test'], '('); // Get the literal argument.
          $literal = substr($literal, 0, strlen($literal) - 1); // Cut the literal.
          
          // Check whether a literal was given.
          if (!empty($literal)) {
            // Check whether the node's processing instructions are matching the literals given.
            if ($this->nodeIndex[$context]['processing-instructions'] == $literal) {
              return TRUE; // Add this node to the node-set.
            }
          } else {
            // Check whether the node has processing instructions.
            if (!empty($this->nodeIndex[$contextPath]['processing-instructions'])) {
              return TRUE; // Add this node to the node-set.
            }
          }
          break;
***********/            
        default:  // Display an error message.
          $this->_displayError('While parsing an XPath query there was an undefined function called "' .
             str_replace($function, '<b>'.$function.'</b>', $this->currentXpathQuery) .'"', __LINE__, __FILE__);
      }
    }
    else { // Display an error message.
      $this->_displayError("While parsing the XPath query \"{$this->currentXpathQuery}\" ".
        "an empty and therefore invalid node-test has been found.", __LINE__, __FILE__, FALSE);
    }
    
    return FALSE; // Don't add this context.
  }
  
  //-----------------------------------------------------------------------------------------
  // XPath                    ------  XPath AXIS Handlers  ------                            
  //-----------------------------------------------------------------------------------------
  
  /**
   * Retrieves axis information from an XPath query step.
   *
   * This method tries to extract the name of the axis and its node-test
   * from a given step of an XPath query at a given node.  If it can't parse
   * the step, then we treat it as a PrimaryExpr.
   *
   * [4]    Step            ::= AxisSpecifier NodeTest Predicate*  
   *                            | AbbreviatedStep  
   * [5]    AxisSpecifier   ::= AxisName '::'  
   *                            | AbbreviatedAxisSpecifier 
   * [12]   AbbreviatedStep ::= '.'  
   *                            | '..'  
   * [13]   AbbreviatedAxisSpecifier    
   *                        ::=    '@'? 
   * 
   * [7]    NodeTest        ::= NameTest  
   *                            | NodeType '(' ')'  
   *                            | 'processing-instruction' '(' Literal ')'  
   * [37]   NameTest        ::= '*'  
   *                            | NCName ':' '*'  
   *                            | QName  
   * [38]   NodeType        ::= 'comment'  
   *                            | 'text'  
   *                            | 'processing-instruction'  
   *                            | 'node' 
   *
   * @param  $step     (string) String containing a step of an XPath query.
   * @return           (array)  Contains information about the axis found in the step, or FALSE
   *                            if the string isn't a valid step.
   * @see    _evaluateStep()
   */
  function _getAxis($step) {
    // The results of this function are very cachable, as it is completely independant of context.
    static $aResultsCache = array();

    // Create an array to save the axis information.
    $axis = array(
      'axis'      => '',
      'node-test' => '',
      'predicate' => array()
    );

    $cacheKey = $step;
    do { // parse block
      $parseBlock = 1;

      if (isset($aResultsCache[$cacheKey])) {
        return $aResultsCache[$cacheKey];
      } else {
        // We have some danger of causing recursion here if we refuse to parse a step as having an
        // axis, and demand it be treated as a PrimaryExpr.  So if we are going to fail, make sure
        // we record what we tried, so that we can catch to see if it comes straight back.
        $guess = array(
          'axis' => 'child',
          'node-test' => $step,
          'predicate' => array());
        $aResultsCache[$cacheKey] = $guess;
      }

      ///////////////////////////////////////////////////
      // Spot the steps that won't come with an axis

      // Check whether the step is empty or only self. 
      if (empty($step) OR ($step == '.') OR ($step == 'current()')) {
        // Set it to the default value.
        $step = '.';
        $axis['axis']      = 'self';
        $axis['node-test'] = '*';
        break $parseBlock;
      }

      if ($step == '..') {
        // Select the parent axis.
        $axis['axis']      = 'parent';
        $axis['node-test'] = '*';
        break $parseBlock;
      }

      ///////////////////////////////////////////////////
      // Pull off the predicates

      // Check whether there are predicates and add the predicate to the list 
      // of predicates without []. Get contents of every [] found.
      $groups = $this->_getEndGroups($step);
//print_r($groups);
      $groupCount = count($groups);
      while (($groupCount > 0) && ($groups[$groupCount - 1][0] == '[')) {
        // Remove the [] and add the predicate to the top of the list
        $predicate = substr($groups[$groupCount - 1], 1, -1);
        array_unshift($axis['predicate'], $predicate);
        // Pop a group off the end of the list
        array_pop($groups);
        $groupCount--;
      }

      // Finally stick the rest back together and this is the rest of our step
      if ($groupCount > 0) {
        $step = implode('', $groups);
      }

      ///////////////////////////////////////////////////
      // Pull off the axis

      // Check for abbreviated syntax
      if ($step[0] == '@') {
        // Use the attribute axis and select the attribute.
        $axis['axis']      = 'attribute';
        $step = substr($step, 1);
      } else {
        // Check whether the axis is given in plain text.
        if (preg_match("/^([^:]*)::(.*)$/", $step, $match)) {
          // Split the step to extract axis and node-test.
          $axis['axis'] = $match[1];
          $step         = $match[2];
        } else {
          // The default axis is child
          $axis['axis'] = 'child';
        }
      }

      ///////////////////////////////////////////////////
      // Process the rest which will either a node test, or else this isn't a step.

      // Check whether is an abbreviated syntax.
      if ($step == '*') {
        // Use the child axis and select all children.
        $axis['node-test'] = '*';
        break $parseBlock;
      }

      // ### I'm pretty sure our current handling of cdata is a fudge, and we should
      // really do this better, but leave this as is for now.
      if ($step == "text()") {
        // Handle the text node
        $axis["node-test"] = "cdata";
        break $parseBlock;
      }

      // There are a few node tests that we match verbatim.
      if ($step == "node()"
          || $step == "comment()"
          || $step == "text()"
          || $step == "processing-instruction") {
        $axis["node-test"] = $step;
        break $parseBlock;
      }

      // processing-instruction() is allowed to take an argument, but if it does, the argument
      // is a literal, which we will have parsed out to $[number].
      if (preg_match(":processing-instruction\(\$\d*\):", $step)) {
        $axis["node-test"] = $step;
        break $parseBlock;
      }

      // The only remaining way this can be a step, is if the remaining string is a simple name
      // or else a :* name.
      // http://www.w3.org/TR/xpath#NT-NameTest
      // NameTest   ::= '*'  
      //                | NCName ':' '*'  
      //                | QName 
      // QName      ::=  (Prefix ':')? LocalPart 
      // Prefix     ::=  NCName 
      // LocalPart  ::=  NCName 
      //
      // ie
      // NameTest   ::= '*'  
      //                | NCName ':' '*'  
      //                | (NCName ':')? NCName
      $NCName = "[a-zA-Z][\w\.\-_]*";
      if (preg_match("/^$NCName:$NCName$/", $step)
        || preg_match("/^$NCName:*$/", $step)) {
        $axis['node-test'] = $step;
        if (!empty($this->parseOptions[XML_OPTION_CASE_FOLDING])) {
          // Case in-sensitive
          $axis['node-test'] = strtoupper($axis['node-test']);
        }
        // Not currently recursing
        $LastFailedStep = '';
        $LastFailedContext = '';
        break $parseBlock;
      } 

      // It's not a node then, we must treat it as a PrimaryExpr
      // Check for recursion
      if ($LastFailedStep == $step) {
        $this->_displayError('Recursion detected while parsing an XPath query, in the step ' .
              str_replace($step, '<b>'.$step.'</b>', $this->currentXpathQuery)
              , __LINE__, __FILE__, FALSE);
        $axis['node-test'] = $step;
      } else {
        $LastFailedStep = $step;
        $axis = FALSE;
      }
      
    } while(FALSE); // end parse block
    
    // Check whether it's a valid axis.
    if ($axis !== FALSE) {
      if (!in_array($axis['axis'], array_merge($this->axes, array('function')))) {
        // Display an error message.
        $this->_displayError('While parsing an XPath query, in the step ' .
          str_replace($step, '<b>'.$step.'</b>', $this->currentXpathQuery) .
          ' the invalid axis ' . $axis['axis'] . ' was found.', __LINE__, __FILE__, FALSE);
      }
    }

    // Cache the real axis information
    $aResultsCache[$cacheKey] = $axis;

    // Return the axis information.
    return $axis;
  }
   

  /**
   * Handles the XPath child axis.
   *
   * This method handles the XPath child axis.  It essentially filters out the
   * children to match the name specified after the '/'.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should 
   *                               be processed.
   * @return              (array)  A vector containing all nodes that were found, during 
   *                               the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_child($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set to hold the results of the child matches
    if ($axis["node-test"] == "cdata") {
      if (!isSet($this->nodeIndex[$contextPath]['textParts']) ) return '';
      $tSize = sizeOf($this->nodeIndex[$contextPath]['textParts']);
      for ($i=1; $i<=$tSize; $i++) { 
        $xPathSet[] = $contextPath . '/text()['.$i.']';
      }
    }
    else {
      // Get a list of all children.
      $allChildren = $this->nodeIndex[$contextPath]['childNodes'];
      
      // Run through all children in the order they where set.
      $cSize = sizeOf($allChildren);
      for ($i=0; $i<$cSize; $i++) {
        $childPath = $contextPath .'/'. $allChildren[$i]['name'] .'['. $allChildren[$i]['contextPos']  .']';
        $textChildPath = $contextPath.'/text()['.($i + 1).']';
        // Check the text node
        if ($this->_checkNodeTest($textChildPath, $axis['node-test'])) { // node test check
          $xPathSet[] = $textChildPath; // Add the child to the node-set.
        }
        // Check the actual node
        if ($this->_checkNodeTest($childPath, $axis['node-test'])) { // node test check
          $xPathSet[] = $childPath; // Add the child to the node-set.
        }
      }

      // Finally there will be one more text node to try
     $textChildPath = $contextPath.'/text()['.($cSize + 1).']';
     // Check the text node
     if ($this->_checkNodeTest($textChildPath, $axis['node-test'])) { // node test check
       $xPathSet[] = $textChildPath; // Add the child to the node-set.
     }
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath parent axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the 
   *                               evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_parent($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Check whether the parent matches the node-test.
    $parentPath = $this->getParentXPath($contextPath);
    if ($this->_checkNodeTest($parentPath, $axis['node-test'])) {
      $xPathSet[] = $parentPath; // Add this node to the list of nodes.
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath attribute axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_attribute($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Check whether all nodes should be selected.
    $nodeAttr = $this->nodeIndex[$contextPath]['attributes'];
    if ($axis['node-test'] == '*'  
        || $axis['node-test'] == 'node()') {
      foreach($nodeAttr as $key=>$dummy) { // Run through the attributes.
        $xPathSet[] = $contextPath.'/attribute::'.$key; // Add this node to the node-set.
      }
    }
    elseif (isset($nodeAttr[$axis['node-test']])) {
      $xPathSet[] = $contextPath . '/attribute::'. $axis['node-test']; // Add this node to the node-set.
    }
    return $xPathSet; // Return the nodeset.
  }
   
  /**
   * Handles the XPath self axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_self($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Check whether the context match the node-test.
    if ($this->_checkNodeTest($contextPath, $axis['node-test'])) {
      $xPathSet[] = $contextPath; // Add this node to the node-set.
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath descendant axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_descendant($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Get a list of all children.
    $allChildren = $this->nodeIndex[$contextPath]['childNodes'];
    
    // Run through all children in the order they where set.
    $cSize = sizeOf($allChildren);
    for ($i=0; $i<$cSize; $i++) {
      $childPath = $allChildren[$i]['xpath'];
      // Check whether the child matches the node-test.
      if ($this->_checkNodeTest($childPath, $axis['node-test'])) {
        $xPathSet[] = $childPath; // Add the child to the list of nodes.
      }
      // Recurse to the next level.
      $xPathSet = array_merge($xPathSet, $this->_handleAxis_descendant($axis, $childPath));
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath ancestor axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_ancestor($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
        
    $parentPath = $this->getParentXPath($contextPath); // Get the parent of the current node.
    
    // Check whether the parent isn't super-root.
    if (!empty($parentPath)) {
      // Check whether the parent matches the node-test.
      if ($this->_checkNodeTest($parentPath, $axis['node-test'])) {
        $xPathSet[] = $parentPath; // Add the parent to the list of nodes.
      }
      // Handle all other ancestors.
      $xPathSet = array_merge($this->_handleAxis_ancestor($axis, $parentPath), $xPathSet);
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath namespace axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_namespace($axis, $contextPath) {
    $this->_displayError("The axis 'namespace is not suported'", __LINE__, __FILE__, FALSE);
  }
  
  /**
   * Handles the XPath following axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_following($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    do { // try-block
      $node = $this->nodeIndex[$contextPath]; // Get the current node
      $position = $node['pos'];               // Get the current tree position.
      $parent = $node['parentNode'];
      // Check if there is a following sibling at all; if not end.
      if ($position >= sizeOf($parent['childNodes'])) break; // try-block
      // Build the starting abs. XPath
      $startXPath = $parent['childNodes'][$position+1]['xpath'];
      // Run through all nodes of the document.
      $nodeKeys = array_keys($this->nodeIndex);
      $nodeSize = sizeOf($nodeKeys);
      for ($k=0; $k<$nodeSize; $k++) {
        if ($nodeKeys[$k] == $startXPath) break; // Check whether this is the starting abs. XPath
      }
      for (; $k<$nodeSize; $k++) {
        // Check whether the node fits the node-test.
        if ($this->_checkNodeTest($nodeKeys[$k], $axis['node-test'])) {
          $xPathSet[] = $nodeKeys[$k]; // Add the node to the list of nodes.
        }
      }
    } while(FALSE);
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath preceding axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_preceding($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Run through all nodes of the document.
    foreach ($this->nodeIndex as $xPath=>$dummy) {
      if (empty($xPath)) continue; // skip super-Root
      
      // Check whether this is the context node.
      if ($xPath == $contextPath) {
        break; // After this we won't look for more nodes.
      }
      if (!strncmp($xPath, $contextPath, strLen($xPath))) {
        continue;
      }
      // Check whether the node fits the node-test.
      if ($this->_checkNodeTest($xPath, $axis['node-test'])) {
        $xPathSet[] = $xPath; // Add the node to the list of nodes.
      }
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath following-sibling axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_following_sibling($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Get all children from the parent.
    $siblings = $this->_handleAxis_child($axis, $this->getParentXPath($contextPath));
    // Create a flag whether the context node was already found.
    $found = FALSE;
    
    // Run through all siblings.
    $size = sizeOf($siblings);
    for ($i=0; $i<$size; $i++) {
      $sibling = $siblings[$i];
      
      // Check whether the context node was already found.
      if ($found) {
        // Check whether the sibling matches the node-test.
        if ($this->_checkNodeTest($sibling, $axis['node-test'])) {
          $xPathSet[] = $sibling; // Add the sibling to the list of nodes.
        }
      }
      // Check if we reached *this* context node.
      if ($sibling == $contextPath) {
        $found = TRUE; // Continue looking for other siblings.
      }
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath preceding-sibling axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_preceding_sibling($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Get all children from the parent.
    $siblings = $this->_handleAxis_child($axis, $this->getParentXPath($contextPath));
    
    // Run through all siblings.
    $size = sizeOf($siblings);
    for ($i=0; $i<$size; $i++) {
      $sibling = $siblings[$i];
      // Check whether this is the context node.
      if ($sibling == $contextPath) {
        break; // Don't continue looking for other siblings.
      }
      // Check whether the sibling matches the node-test.
      if ($this->_checkNodeTest($sibling, $axis['node-test'])) {
        $xPathSet[] = $sibling; // Add the sibling to the list of nodes.
      }
    }
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath descendant-or-self axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_descendant_or_self($axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Read the nodes.
    $xPathSet = array_merge(
                 $this->_handleAxis_self($axis, $contextPath),
                 $this->_handleAxis_descendant($axis, $contextPath)
               );
    return $xPathSet; // Return the nodeset.
  }
  
  /**
   * Handles the XPath ancestor-or-self axis.
   *
   * This method handles the XPath ancestor-or-self axis.
   *
   * @param  $axis        (array)  Array containing information about the axis.
   * @param  $contextPath (string) xpath to starting node from which the axis should be processed.
   * @return              (array)  A vector containing all nodes that were found, during the evaluation of the axis.
   * @see    evaluate()
   */
  function _handleAxis_ancestor_or_self ( $axis, $contextPath) {
    $xPathSet = array(); // Create an empty node-set.
    
    // Read the nodes.
    $xPathSet = array_merge(
                 $this->_handleAxis_ancestor($axis, $contextPath),
                 $this->_handleAxis_self($axis, $contextPath)
               );
    return $xPathSet; // Return the nodeset.
  }
  
  
  //-----------------------------------------------------------------------------------------
  // XPath                  ------  XPath FUNCTION Handlers  ------                          
  //-----------------------------------------------------------------------------------------
  
   /**
    * Handles the XPath function last.
    *    
    * @param  $arguments     (string) String containing the arguments that were passed to the function.
    * @param  $context       (array)  The context from which to evaluate the function
    * @return                (mixed)  Depending on the type of function being processed
    * @see    evaluate()
    */
  function _handleFunction_last($arguments, $context) {
    return $context['size'];
  }
  
  /**
   * Handles the XPath function position.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_position($arguments, $context) {
    return $context['pos'];
  }
  
  /**
   * Handles the XPath function count.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_count($arguments, $context) {
    // Evaluate the argument of the method as an XPath and return the number of results.
    return count($this->_evaluateExpr($arguments, $context));
  }
  
  /**
   * Handles the XPath function id.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_id($arguments, $context) {
    $arguments = trim($arguments);         // Trim the arguments.
    $arguments = explode(' ', $arguments); // Now split the arguments into an array.
    // Create a list of nodes.
    $resultXPaths = array();
    // Run through all nodes of the document.
    $keys = array_keys($this->nodeIndex);
    $kSize = $sizeOf($keys);
    for ($i=0; $i<$kSize; $i++) {
      if (empty($keys[$i])) continue; // skip super-Root
      if (in_array($this->nodeIndex[$keys[$i]]['attributes']['id'], $arguments)) {
        $resultXPaths[] = $context['nodePath']; // Add this node to the list of nodes.
      }
    }
    return $resultXPaths; // Return the list of nodes.
  }
  
  /**
   * Handles the XPath function name.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_name($arguments, $context) {
    // If the argument it omitted, it defaults to a node-set with the context node as its only member.
    if (empty($arguments)) {
      return $this->_addLiteral($this->nodeIndex[$context['nodePath']]['name']);
    }

    // Evaluate the argument to get a node set.
    $nodeSet = $this->_evaluateExpr($arguments, $context);
    if (!is_array($nodeSet)) return '';
    if (count($nodeSet) < 1) return '';
    if (!isset($this->nodeIndex[$nodeSet[0]])) return '';
     // Return a reference to the name of the node.
    return $this->_addLiteral($this->nodeIndex[$nodeSet[0]]['name']);
  }
  
  /**
   * Handles the XPath function string.
   *
   * http://www.w3.org/TR/xpath#section-String-Functions
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_string($arguments, $context) {
    // Check what type of parameter is given
    if (is_array($arguments)) {
      // Get the value of the first result (which means we want to concat all the text...unless
      // a specific text() node has been given, and it will switch off to substringData
      if (!count($arguments)) $result = '';
      else {
        $result = $this->_stringValue($arguments[0]);
        if (($literal = $this->_asLiteral($result)) !== FALSE) {
          $result = $literal;
        }
      }
    }
    // Is it a number string?
    elseif (preg_match('/^[0-9]+(\.[0-9]+)?$/', $arguments) OR preg_match('/^\.[0-9]+$/', $arguments)) {
      // ### Note no support for NaN and Infinity.
      $number = doubleval($arguments); // Convert the digits to a number.
      $result = strval($number); // Return the number.
    }
    elseif (is_bool($arguments)) { // Check whether it's TRUE or FALSE and return as string.
      // ### Note that we used to return TRUE and FALSE which was incorrect according to the standard.
      if ($arguments === TRUE) {        
        $result = 'true'; 
      } else {
        $result = 'false';
      }
    }
    elseif (($literal = $this->_asLiteral($arguments)) !== FALSE) {
      return $literal;
    }
    elseif (!empty($arguments)) {
      // Spec says:
      // "An object of a type other than the four basic types is converted to a string in a way that 
      // is dependent on that type."
      // Use the argument as an XPath.
      $result = $this->_evaluateExpr($arguments, $context);
      if (is_string($result) && is_string($arguments) && (!strcmp($result, $arguments))) {
        $this->_displayError("Loop detected in XPath expression.  Probably an internal error :o/.  _handleFunction_string($result)", __LINE__, __FILE__, FALSE);
        return '';
      } else {
        $result = $this->_handleFunction_string($result, $context);
      }
    }
    else {
      $result = '';  // Return an empty string.
    }
    return $result;
  }
  
  /**
   * Handles the XPath function concat.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_concat($arguments, $context) {
    // Split the arguments.
    $arguments = explode(',', $arguments);
    // Run through each argument and evaluate it.
    $size = sizeof($arguments);
    for ($i=0; $i<$size; $i++) {
      $arguments[$i] = trim($arguments[$i]);  // Trim each argument.
      // Evaluate it.
      $arguments[$i] = $this->_handleFunction_string($arguments[$i], $context);
    }
    $arguments = implode('', $arguments);  // Put the string together and return it.
    return $this->_addLiteral($arguments);
  }
  
  /**
   * Handles the XPath function starts-with.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_starts_with($arguments, $context) {
    // Get the arguments.
    $first  = trim($this->_prestr($arguments, ','));
    $second = trim($this->_afterstr($arguments, ','));
    // Evaluate each argument.
    $first  = $this->_handleFunction_string($first, $context);
    $second = $this->_handleFunction_string($second, $context);
    // Check whether the first string starts with the second one.
    return  (bool) ereg('^'.$second, $first);
  }
  
  /**
   * Handles the XPath function contains.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_contains($arguments, $context) {
    // Get the arguments.
    $first  = trim($this->_prestr($arguments, ','));
    $second = trim($this->_afterstr($arguments, ','));
    //echo "Predicate: $arguments First: ".$first." Second: ".$second."\n";
    // Evaluate each argument.
    $first = $this->_handleFunction_string($first, $context);
    $second = $this->_handleFunction_string($second, $context);
    //echo $second.": ".$first."\n";
    // If the search string is null, then the provided there is a value it will contain it as
    // it is considered that all strings contain the empty string. ## N.S.
    if ($second==='') return TRUE;
    // Check whether the first string starts with the second one.
    if (strpos($first, $second) === FALSE) {
      return FALSE;
    } else {
      return TRUE;
    }
  }
  
  /**
   * Handles the XPath function substring-before.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_substring_before($arguments, $context) {
    // Get the arguments.
    $first  = trim($this->_prestr($arguments, ','));
    $second = trim($this->_afterstr($arguments, ','));
    // Evaluate each argument.
    $first  = $this->_handleFunction_string($first, $context);
    $second = $this->_handleFunction_string($second, $context);
    // Return the substring.
    return $this->_addLiteral($this->_prestr(strval($first), strval($second)));
  }
  
  /**
   * Handles the XPath function substring-after.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_substring_after($arguments, $context) {
    // Get the arguments.
    $first  = trim($this->_prestr($arguments, ','));
    $second = trim($this->_afterstr($arguments, ','));
    // Evaluate each argument.
    $first  = $this->_handleFunction_string($first, $context);
    $second = $this->_handleFunction_string($second, $context);
    // Return the substring.
    return $this->_addLiteral($this->_afterstr(strval($first), strval($second)));
  }
  
  /**
   * Handles the XPath function substring.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_substring($arguments, $context) {
    // Split the arguments.
    $arguments = explode(",", $arguments);
    $size = sizeOf($arguments);
    for ($i=0; $i<$size; $i++) { // Run through all arguments.
      $arguments[$i] = trim($arguments[$i]); // Trim the string.
      // Evaluate each argument.
      $arguments[$i] = $this->_handleFunction_string($arguments[$i], $context);
    }
    // Check whether a third argument was given and return the substring..
    if (!empty($arguments[2])) {
      return $this->_addLiteral(substr(strval($arguments[0]), $arguments[1] - 1, $arguments[2]));
    } else {
      return $this->_addLiteral(substr(strval($arguments[0]), $arguments[1] - 1));
    }
  }
  
  /**
   * Handles the XPath function string-length.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_string_length($arguments, $context) {
    $arguments = trim($arguments); // Trim the argument.
    // Evaluate the argument.
    $arguments = $this->_handleFunction_string($arguments, $context);
    return strlen(strval($arguments)); // Return the length of the string.
  }

  /**
   * Handles the XPath function normalize-space.
   *
   * The normalize-space function returns the argument string with whitespace
   * normalized by stripping leading and trailing whitespace and replacing sequences
   * of whitespace characters by a single space.
   * If the argument is omitted, it defaults to the context node converted to a string,
   * in other words the string-value of the context node
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                 (stri)g trimed string
   * @see    evaluate()
   */
  function _handleFunction_normalize_space($arguments, $context) {
    if (empty($arguments)) {
      $arguments = $this->getParentXPath($context['nodePath']).'/'.$this->nodeIndex[$context['nodePath']]['name'].'['.$this->nodeIndex[$context['nodePath']]['contextPos'].']';
    } else {
       $arguments = $this->_handleFunction_string($arguments, $context);
    }
    $arguments = trim(preg_replace (";[[:space:]]+;s",' ',$arguments));
    return $this->_addLiteral($arguments);
  }

  /**
   * Handles the XPath function translate.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_translate($arguments, $context) {
    $arguments = explode(',', $arguments); // Split the arguments.
    $size = sizeOf($arguments);
    for ($i=0; $i<$size; $i++) { // Run through all arguments.
      $arguments[$i] = trim($arguments[$i]); // Trim the argument.
      // Evaluate the argument.
      $arguments[$i] = $this->_handleFunction_string($arguments[$i], $context);
    }
    // Return the translated string.
    return $this->_addLiteral(strtr($arguments[0], $arguments[1], $arguments[2]));
  }

  /**
   * Handles the XPath function boolean.
   *   
   * http://www.w3.org/TR/xpath#section-Boolean-Functions
   *
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_boolean($arguments, $context) {
    if (empty($arguments)) {
      return FALSE; // Sorry, there were no arguments.
    }
    // a bool is dead obvious
    elseif (is_bool($arguments)) {
      return $arguments;
    }
    // a node-set is true if and only if it is non-empty
    elseif (is_array($arguments)) {
      return (count($arguments) > 0);
    }
    // a number is true if and only if it is neither positive or negative zero nor NaN 
    // (Straight out of the XPath spec.. makes no sense?????)
    elseif (preg_match('/^[0-9]+(\.[0-9]+)?$/', $arguments) || preg_match('/^\.[0-9]+$/', $arguments)) {
      $number = doubleval($arguments);  // Convert the digits to a number.
      // If number zero return FALSE else TRUE.
      if ($number == 0) return FALSE; else return TRUE;
    }
    // a string is true if and only if its length is non-zero
    elseif (($literal = $this->_asLiteral($arguments)) !== FALSE) {
      return (strlen($literal) != 0);
    }
    // an object of a type other than the four basic types is converted to a boolean in a 
    // way that is dependent on that type
    else {
      // Spec says:
      // "An object of a type other than the four basic types is converted to a number in a way 
      // that is dependent on that type"
      // Try to evaluate the argument as an XPath.
      $result = $this->_evaluateExpr($arguments, $context);
      if (is_string($result) && is_string($arguments) && (!strcmp($result, $arguments))) {
        $this->_displayError("Loop detected in XPath expression.  Probably an internal error :o/.  _handleFunction_boolean($result)", __LINE__, __FILE__, FALSE);
        return FALSE;
      } else {
        return $this->_handleFunction_boolean($result, $context);
      }
    }
  }
  
  /**
   * Handles the XPath function not.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_not($arguments, $context) {
    // Return the negative value of the content of the brackets.
    $bArgResult = $this->_handleFunction_boolean($arguments, $context);
//echo "Before inversion: ".($bArgResult?"TRUE":"FALSE")."\n";
    return !$bArgResult;
  }
  
  /**
   * Handles the XPath function TRUE.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_true($arguments, $context) {
    return TRUE; // Return TRUE.
  }
  
  /**
   * Handles the XPath function FALSE.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_false($arguments, $context) {
    return FALSE; // Return FALSE.
  }
  
  /**
   * Handles the XPath function lang.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_lang($arguments, $context) {
    $arguments = trim($arguments); // Trim the arguments.
    $currentNode = $this->nodeIndex[$context['nodePath']];
    while (!empty($currentNode['name'])) { // Run through the ancestors.
      // Check whether the node has an language attribute.
      if (isSet($currentNode['attributes']['xml:lang'])) {
        // Check whether it's the language, the user asks for; if so return TRUE else FALSE
        return eregi('^'.$arguments, $currentNode['attributes']['xml:lang']);
      }
      $currentNode = $currentNode['parentNode']; // Move up to parent
    } // End while
    return FALSE;
  }
  
  /**
   * Handles the XPath function number.
   *   
   * http://www.w3.org/TR/xpath#section-Number-Functions
   *
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_number($arguments, $context) {
    // Check the type of argument.

    // A string that is a number
    if (is_numeric($arguments)) {
      return doubleval($arguments); // Return the argument as a number.
    }
    // A bool
    elseif (is_bool($arguments)) {  // Return TRUE/FALSE as a number.
      if ($arguments === TRUE) return 1; else return 0;  
    }
    // A node set
    elseif (is_array($arguments)) {
      // Is converted to a string then handled like a string
      $string = $this->_handleFunction_string($arguments, $context);
      if (is_numeric($string))
        return doubleval($string);
    }
    elseif (($literal = $this->_asLiteral($arguments)) !== FALSE) {
      if (is_numeric($literal)) {
        return doubleval($literal);
      } else {
        // If we are to stick strictly to the spec, we should return NaN, but lets just
        // leave PHP to see if can do some dynamic conversion.
        return $literal;
      }
    }
    else {
      // Spec says:
      // "An object of a type other than the four basic types is converted to a number in a way 
      // that is dependent on that type"
      // Try to evaluate the argument as an XPath.
      $result = $this->_evaluateExpr($arguments, $context);
      if (is_string($result) && is_string($arguments) && (!strcmp($result, $arguments))) {
        $this->_displayError("Loop detected in XPath expression.  Probably an internal error :o/.  _handleFunction_number($result)", __LINE__, __FILE__, FALSE);
        return FALSE;
      } else {
        return $this->_handleFunction_number($result, $context);
      }
    }
  }

  /**
   * Handles the XPath function sum.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_sum($arguments, $context) {
    $arguments = trim($arguments); // Trim the arguments.
    // Evaluate the arguments as an XPath query.
    $result = $this->_evaluateExpr($arguments, $context);
    $sum = 0; // Create a variable to save the sum.
    // The sum function expects a node set as an argument.
    if (is_array($result)) {
      // Run through all results.
      $size = sizeOf($result);
      for ($i=0; $i<$size; $i++) {
        $value = $this->_stringValue($result[$i], $context);
        if (($literal = $this->_asLiteral($value)) !== FALSE) {
          $value = $literal;
        }
        $sum += doubleval($value); // Add it to the sum.
      }
    }
    return $sum; // Return the sum.
  }

  /**
   * Handles the XPath function floor.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_floor($arguments, $context) {
    if (!is_numeric($arguments)) {
      $arguments = $this->_handleFunction_number($arguments, $context);
    }
    $arguments = doubleval($arguments); // Convert the arguments to a number.
    return floor($arguments);           // Return the result
  }
  
  /**
   * Handles the XPath function ceiling.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_ceiling($arguments, $context) {
    if (!is_numeric($arguments)) {
      $arguments = $this->_handleFunction_number($arguments, $context);
    }
    $arguments = doubleval($arguments); // Convert the arguments to a number.
    return ceil($arguments);            // Return the result
  }
  
  /**
   * Handles the XPath function round.
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_round($arguments, $context) {
    if (!is_numeric($arguments)) {
      $arguments = $this->_handleFunction_number($arguments, $context);
    }
    $arguments = doubleval($arguments); // Convert the arguments to a number.
    return round($arguments);           // Return the result
  }

  //-----------------------------------------------------------------------------------------
  // XPath                  ------  XPath Extension FUNCTION Handlers  ------                          
  //-----------------------------------------------------------------------------------------

  /**
   * Handles the XPath function x-lower.
   *
   * lower case a string.
   *    string x-lower(string) 
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_x_lower($arguments, $context) {
    // Evaluate the argument.
    $string = $this->_handleFunction_string($arguments, $context);
     // Return a reference to the lowercased string
    return $this->_addLiteral(strtolower(strval($string)));
  }

  /**
   * Handles the XPath function x-upper.
   *
   * upper case a string.
   *    string x-upper(string) 
   *   
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @see    evaluate()
   */
  function _handleFunction_x_upper($arguments, $context) {
    // Evaluate the argument.
    $string = $this->_handleFunction_string($arguments, $context);
     // Return a reference to the lowercased string
    return $this->_addLiteral(strtoupper(strval($string)));
  }

  /**
   * Handles the XPath function generate-id.
   *
   * Produce a unique id for the first node of the node set.
   * 
   * Example usage, produces an index of all the nodes in an .xml document, where the content of each
   * "section" is the exported node as XML.
   *
   *   $aFunctions = $xPath->match('//');
   *   
   *   foreach ($aFunctions as $Function) {
   *       $id = $xPath->match("generate-id($Function)");
   *       echo "<a href='#$id'>$Function</a><br>";
   *   }
   *   
   *   foreach ($aFunctions as $Function) {
   *       $id = $xPath->match("generate-id($Function)");
   *       echo "<h2 id='$id'>$Function</h2>";
   *       echo htmlspecialchars($xPath->exportAsXml($Function));
   *   }
   * 
   * @param  $arguments     (string) String containing the arguments that were passed to the function.
   * @param  $context       (array)  The context from which to evaluate the function
   * @return                (mixed)  Depending on the type of function being processed
   * @author Ricardo Garcia
   * @see    evaluate()
   */
  function _handleFunction_generate_id($arguments, $context) {
    // If the argument is omitted, it defaults to a node-set with the context node as its only member.
    if (is_string($arguments) && empty($arguments)) {
      // We need ids then
      $this->_generate_ids();
      return $this->_addLiteral($this->nodeIndex[$context['nodePath']]['generated_id']);
    }

    // Evaluate the argument to get a node set.
    $nodeSet = $this->_evaluateExpr($arguments, $context);

    if (!is_array($nodeSet)) return '';
    if (count($nodeSet) < 1) return '';
    if (!isset($this->nodeIndex[$nodeSet[0]])) return '';
     // Return a reference to the name of the node.
    // We need ids then
    $this->_generate_ids();
    return $this->_addLiteral($this->nodeIndex[$nodeSet[0]]['generated_id']);
  }

  //-----------------------------------------------------------------------------------------
  // XPathEngine                ------  Help Stuff  ------                                   
  //-----------------------------------------------------------------------------------------

  /**
   * Decodes the character set entities in the given string.
   *
   * This function is given for convenience, as all text strings or attributes
   * are going to come back to you with their entities still encoded.  You can
   * use this function to remove these entites.
   *
   * It makes use of the get_html_translation_table(HTML_ENTITIES) php library 
   * call, so is limited in the same ways.  At the time of writing this seemed
   * be restricted to iso-8859-1
   *
   * ### Provide an option that will do this by default.
   *
   * @param $encodedData (mixed) The string or array that has entities you would like to remove
   * @param $reverse     (bool)  If TRUE entities will be encoded rather than decoded, ie
   *                             < to &lt; rather than &lt; to <.
   * @return             (mixed) The string or array returned with entities decoded.
   */
  function decodeEntities($encodedData, $reverse=FALSE) {
    static $aEncodeTbl;
    static $aDecodeTbl;
    // Get the translation entities, but we'll cache the result to enhance performance.
    if (empty($aDecodeTbl)) {
      // Get the translation entities.
      $aEncodeTbl = get_html_translation_table(HTML_ENTITIES);
      $aDecodeTbl = array_flip($aEncodeTbl);
    }

    // If it's just a single string.
    if (!is_array($encodedData)) {
      if ($reverse) {
        return strtr($encodedData, $aEncodeTbl);
      } else {
        return strtr($encodedData, $aDecodeTbl);
      }
    }

    $result = array();
    foreach($encodedData as $string) {
      if ($reverse) {
        $result[] = strtr($string, $aEncodeTbl);
      } else {
        $result[] = strtr($string, $aDecodeTbl);
      }
    }

    return $result;
  }
  
  /**
   * Compare two nodes to see if they are equal (point to the same node in the doc)
   *
   * 2 nodes are considered equal if the absolute XPath is equal.
   * 
   * @param  $node1 (mixed) Either an absolute XPath to an node OR a real tree-node (hash-array)
   * @param  $node2 (mixed) Either an absolute XPath to an node OR a real tree-node (hash-array)
   * @return        (bool)  TRUE if equal (see text above), FALSE if not (and on error).
   */
  function equalNodes($node1, $node2) {
    $xPath_1 = is_string($node1) ? $node1 : $this->getNodePath($node1);
    $xPath_2 = is_string($node2) ? $node2 : $this->getNodePath($node2);
    return (strncasecmp ($xPath_1, $xPath_2, strLen($xPath_1)) == 0);
  }
  
  /**
   * Get the absolute XPath of a node that is in a document tree.
   *
   * @param $node (array)  A real tree-node (hash-array)   
   * @return      (string) The string path to the node or FALSE on error.
   */
  function getNodePath($node) {
    if (!empty($node['xpath'])) return $node['xpath'];
    $pathInfo = array();
    do {
      if (empty($node['name']) OR empty($node['parentNode'])) break; // End criteria
      $pathInfo[] = array('name' => $node['name'], 'contextPos' => $node['contextPos']);
      $node = $node['parentNode'];
    } while (TRUE);
    
    $xPath = '';
    for ($i=sizeOf($pathInfo)-1; $i>=0; $i--) {
      $xPath .= '/' . $pathInfo[$i]['name'] . '[' . $pathInfo[$i]['contextPos'] . ']';
    }
    if (empty($xPath)) return FALSE;
    return $xPath;
  }
  
  /**
   * Retrieves the absolute parent XPath query.
   *
   * The parents stored in the tree are only relative parents...but all the parent
   * information is stored in the XPath query itself...so instead we use a function
   * to extract the parent from the absolute Xpath query
   *
   * @param  $childPath (string) String containing an absolute XPath query
   * @return            (string) returns the absolute XPath of the parent
   */
   function getParentXPath($absoluteXPath) {
     $lastSlashPos = strrpos($absoluteXPath, '/'); 
     if ($lastSlashPos == 0) { // it's already the root path
       return ''; // 'super-root'
     } else {
       return (substr($absoluteXPath, 0, $lastSlashPos));
     }
   }
  
  /**
   * Returns TRUE if the given node has child nodes below it
   *
   * @param  $absoluteXPath (string) full path of the potential parent node
   * @return                (bool)   TRUE if this node exists and has a child, FALSE otherwise
   */
  function hasChildNodes($absoluteXPath) {
    if ($this->_indexIsDirty) $this->reindexNodeTree();
    return (bool) (isSet($this->nodeIndex[$absoluteXPath]) 
                   AND sizeOf($this->nodeIndex[$absoluteXPath]['childNodes']));
  }
  
  /**
   * Translate all ampersands to it's literal entities '&amp;' and back.
   *
   * I wasn't aware of this problem at first but it's important to understand why we do this.
   * At first you must know:
   * a) PHP's XML parser *translates* all entities to the equivalent char E.g. &lt; is returned as '<'
   * b) PHP's XML parser (in V 4.1.0) has problems with most *literal* entities! The only one's that are 
   *    recognized are &amp;, &lt; &gt; and &quot;. *ALL* others (like &nbsp; &copy; a.s.o.) cause an 
   *    XML_ERROR_UNDEFINED_ENTITY error. I reported this as bug at http://bugs.php.net/bug.php?id=15092
   *    (It turned out not to be a 'real' bug, but one of those nice W3C-spec things).
   * 
   * Forget position b) now. It's just for info. Because the way we will solve a) will also solve b) too. 
   *
   * THE PROBLEM
   * To understand the problem, here a sample:
   * Given is the following XML:    "<AAA> &lt; &nbsp; &gt; </AAA>"
   *   Try to parse it and PHP's XML parser will fail with a XML_ERROR_UNDEFINED_ENTITY becaus of 
   *   the unknown litteral-entity '&nbsp;'. (The numeric equivalent '&#160;' would work though). 
   * Next try is to use the numeric equivalent 160 for '&nbsp;', thus  "<AAA> &lt; &#160; &gt; </AAA>"
   *   The data we receive in the tag <AAA> is  " <   > ". So we get the *translated entities* and 
   *   NOT the 3 entities &lt; &#160; &gt. Thus, we will not even notice that there were entities at all!
   *   In *most* cases we're not able to tell if the data was given as entity or as 'normal' char.
   *   E.g. When receiving a quote or a single space were not able to tell if it was given as 'normal' char
   *   or as &nbsp; or &quot;. Thus we loose the entity-information of the XML-data!
   * 
   * THE SOLUTION
   * The better solution is to keep the data 'as is' by replacing the '&' before parsing begins.
   * E.g. Taking the original input from above, this would result in "<AAA> &amp;lt; &amp;nbsp; &amp;gt; </AAA>"
   * The data we receive now for the tag <AAA> is  " &lt; &nbsp; &gt; ". and that's what we want.
   * 
   * The bad thing is, that a global replace will also replace data in section that are NOT translated by the 
   * PHP XML-parser. That is comments (<!-- -->), IP-sections (stuff between <? ? >) and CDATA-block too.
   * So all data comming from those sections must be reversed. This is done during the XML parse phase.
   * So:
   * a) Replacement of all '&' in the XML-source.
   * b) All data that is not char-data or in CDATA-block have to be reversed during the XML-parse phase.
   *
   * @param  $xmlSource (string) The XML string
   * @return            (string) The XML string with translated ampersands.
   */
  function _translateAmpersand($xmlSource, $reverse=FALSE) {
    $PHP5 = (substr(phpversion(), 0, 1) == '5');
    if ($PHP5) {
      //otherwise we receive  &amp;nbsp;  instead of  &nbsp;
      return $xmlSource;
    } else {
      return ($reverse ? str_replace('&amp;', '&', $xmlSource) : str_replace('&', '&amp;', $xmlSource));
    }
  }

} // END OF CLASS XPathEngine


/************************************************************************************************
* ===============================================================================================
*                                     X P a t h  -  Class                                        
* ===============================================================================================
************************************************************************************************/

define('XPATH_QUERYHIT_ALL'   , 1);
define('XPATH_QUERYHIT_FIRST' , 2);
define('XPATH_QUERYHIT_UNIQUE', 3);

class XPath extends XPathEngine {
    
  /**
   * Constructor of the class
   *
   * Optionally you may call this constructor with the XML-filename to parse and the 
   * XML option vector. A option vector sample: 
   *   $xmlOpt = array(XML_OPTION_CASE_FOLDING => FALSE, XML_OPTION_SKIP_WHITE => TRUE);
   *
   * @param  $userXmlOptions (array)  (optional) Vector of (<optionID>=><value>, <optionID>=><value>, ...)
   * @param  $fileName       (string) (optional) Filename of XML file to load from.
   *                                  It is recommended that you call importFromFile()
   *                                  instead as you will get an error code.  If the
   *                                  import fails, the object will be set to FALSE.
   * @see    parent::XPathEngine()
   */
  function XPath($fileName='', $userXmlOptions=array()) {
    parent::XPathEngine($userXmlOptions);
    $this->properties['modMatch'] = XPATH_QUERYHIT_ALL;
    if ($fileName) {
      if (!$this->importFromFile($fileName)) {
        // Re-run the base constructor to "reset" the object.  If the user has any sense, then
        // they will have created the object, and then explicitly called importFromFile(), giving
        // them the chance to catch and handle the error properly.
        parent::XPathEngine($userXmlOptions);
      }
    }
  }
  
  /**
   * Resets the object so it's able to take a new xml sting/file
   *
   * Constructing objects is slow.  If you can, reuse ones that you have used already
   * by using this reset() function.
   */
  function reset() {
    parent::reset();
    $this->properties['modMatch'] = XPATH_QUERYHIT_ALL;
  }
  
  //-----------------------------------------------------------------------------------------
  // XPath                    ------  Get / Set Stuff  ------                                
  //-----------------------------------------------------------------------------------------
  
  /**
   * Resolves and xPathQuery array depending on the property['modMatch']
   *
   * Most of the modification functions of XPath will also accept a xPathQuery (instead 
   * of an absolute Xpath). The only problem is that the query could match more the one 
   * node. The question is, if the none, the fist or all nodes are to be modified.
   * The behaver can be set with setModMatch()  
   *
   * @param $modMatch (int) One of the following:
   *                        - XPATH_QUERYHIT_ALL (default) 
   *                        - XPATH_QUERYHIT_FIRST
   *                        - XPATH_QUERYHIT_UNIQUE // If the query matches more then one node. 
   * @see  _resolveXPathQuery()
   */
  function setModMatch($modMatch = XPATH_QUERYHIT_ALL) {
    switch($modMatch) {
      case XPATH_QUERYHIT_UNIQUE : $this->properties['modMatch'] =  XPATH_QUERYHIT_UNIQUE; break;
      case XPATH_QUERYHIT_FIRST: $this->properties['modMatch'] =  XPATH_QUERYHIT_FIRST; break;
      default: $this->properties['modMatch'] = XPATH_QUERYHIT_ALL;
    }
  }
  
  //-----------------------------------------------------------------------------------------
  // XPath                    ------  DOM Like Modification  ------                          
  //-----------------------------------------------------------------------------------------
  
  //-----------------------------------------------------------------------------------------
  // XPath                  ------  Child (Node)  Set/Get  ------                           
  //-----------------------------------------------------------------------------------------
  
  /**
   * Retrieves the name(s) of a node or a group of document nodes.
   *          
   * This method retrieves the names of a group of document nodes
   * specified in the argument.  So if the argument was '/A[1]/B[2]' then it
   * would return 'B' if the node did exist in the tree.
   *          
   * @param  $xPathQuery (mixed) Array or single full document path(s) of the node(s), 
   *                             from which the names should be retrieved.
   * @return             (mixed) Array or single string of the names of the specified 
   *                             nodes, or just the individual name.  If the node did 
   *                             not exist, then returns FALSE.
   */
  function nodeName($xPathQuery) {
    if (is_array($xPathQuery)) {
      $xPathSet = $xPathQuery;
    } else {
      // Check for a valid xPathQuery
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'nodeName');
    }
    if (count($xPathSet) == 0) return FALSE;
    // For each node, get it's name
    $result = array();
    foreach($xPathSet as $xPath) {
      $node = &$this->getNode($xPath);
      if (!$node) {
        // ### Fatal internal error?? 
        continue;
      }
      $result[] = $node['name'];
    }
    // If just a single string, return string
    if (count($xPathSet) == 1) $result = $result[0];
    // Return result.
    return $result;
  }
  
  /**
   * Removes a node from the XML document.
   *
   * This method removes a node from the tree of nodes of the XML document. If the node 
   * is a document node, all children of the node and its character data will be removed. 
   * If the node is an attribute node, only this attribute will be removed, the node to which 
   * the attribute belongs as well as its children will remain unmodified.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery  (string) xpath to the node (See note above).
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                               the changes.  A performance helper.  See reindexNodeTree()
   * @return              (bool)   TRUE on success, FALSE on error;
   * @see    setModMatch(), reindexNodeTree()
   */
  function removeChild($xPathQuery, $autoReindex=TRUE) {
    $NULL = NULL;
    $bDebugThisFunction = FALSE;  // Get diagnostic output for this function
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction('removeChild');
      echo "Node: $xPathQuery\n";
      echo '<hr>';
    }
    $status = FALSE;
    do { // try-block
      // Check for a valid xPathQuery
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'removeChild');
      if (sizeOf($xPathSet) === 0) {
        $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
        break; // try-block
      }
      $mustReindex = FALSE;
      // Make chages from 'bottom-up'. In this manner the modifications will not affect itself.
      for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
        $absoluteXPath = $xPathSet[$i];
        if (preg_match(';/attribute::;', $absoluteXPath)) { // Handle the case of an attribute node
          $xPath = $this->_prestr($absoluteXPath, '/attribute::');       // Get the path to the attribute node's parent.
          $attribute = $this->_afterstr($absoluteXPath, '/attribute::'); // Get the name of the attribute.
          unSet($this->nodeIndex[$xPath]['attributes'][$attribute]);     // Unset the attribute
          if ($bDebugThisFunction) echo "We removed the attribute '$attribute' of node '$xPath'.\n";
          continue;
        }
        // Otherwise remove the node by setting it to NULL. It will be removed on the next reindexNodeTree() call.
        $mustReindex = $autoReindex;
        // Flag the index as dirty; it's not uptodate. A reindex will be forced (if dirty) when exporting the XML doc
        $this->_indexIsDirty = TRUE;
        
        $theNode = $this->nodeIndex[$absoluteXPath];
        $theNode['parentNode']['childNodes'][$theNode['pos']] =& $NULL;
        if ($bDebugThisFunction) echo "We removed the node '$absoluteXPath'.\n";
      }
      // Reindex the node tree again
      if ($mustReindex) $this->reindexNodeTree();
      $status = TRUE;
    } while(FALSE);
    
    if ($bDebugThisFunction) $this->_closeDebugFunction($aStartTime, $status);
    return $status;
  }
  
  /**
   * Replace a node with any data string. The $data is taken 1:1.
   *
   * This function will delete the node you define by $absoluteXPath (plus it's sub-nodes) and 
   * substitute it by the string $text. Often used to push in not well formed HTML.
   * WARNING: 
   *   The $data is taken 1:1. 
   *   You are in charge that the data you enter is valid XML if you intend
   *   to export and import the content again.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery  (string) xpath to the node (See note above).
   * @param  $data        (string) String containing the content to be set. *READONLY*
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                               the changes.  A performance helper.  See reindexNodeTree()
   * @return              (bool)   TRUE on success, FALSE on error;
   * @see    setModMatch(), replaceChild(), reindexNodeTree()
   */
  function replaceChildByData($xPathQuery, $data, $autoReindex=TRUE) {
    $NULL = NULL;
    $bDebugThisFunction = FALSE;  // Get diagnostic output for this function
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction('replaceChildByData');
      echo "Node: $xPathQuery\n";
    }
    $status = FALSE;
    do { // try-block
      // Check for a valid xPathQuery
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'replaceChildByData');
      if (sizeOf($xPathSet) === 0) {
        $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
        break; // try-block
      }
      $mustReindex = FALSE;
      // Make chages from 'bottom-up'. In this manner the modifications will not affect itself.
      for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
        $mustReindex = $autoReindex;
        // Flag the index as dirty; it's not uptodate. A reindex will be forced (if dirty) when exporting the XML doc
        $this->_indexIsDirty = TRUE;
        
        $absoluteXPath = $xPathSet[$i];
        $theNode = $this->nodeIndex[$absoluteXPath];
        $pos = $theNode['pos'];
        $theNode['parentNode']['textParts'][$pos] .= $data;
        $theNode['parentNode']['childNodes'][$pos] =& $NULL;
        if ($bDebugThisFunction) echo "We replaced the node '$absoluteXPath' with data.\n";
      }
      // Reindex the node tree again
      if ($mustReindex) $this->reindexNodeTree();
      $status = TRUE;
    } while(FALSE);
    
    if ($bDebugThisFunction) $this->_closeDebugFunction($aStartTime, ($status) ? 'Success' : '!!! FAILD !!!');
    return $status;
  }
  
  /**
   * Replace the node(s) that matches the xQuery with the passed node (or passed node-tree)
   * 
   * If the passed node is a string it's assumed to be XML and replaceChildByXml() 
   * will be called.
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery  (string) Xpath to the node being replaced.
   * @param  $node        (mixed)  String or Array (Usually a String)
   *                               If string: Vaild XML. E.g. "<A/>" or "<A> foo <B/> bar <A/>"
   *                               If array:  A Node (can be a whole sub-tree) (See comment in header)
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                               the changes.  A performance helper.  See reindexNodeTree()
   * @return              (array)  The last replaced $node (can be a whole sub-tree)
   * @see    reindexNodeTree()
   */
  function &replaceChild($xPathQuery, $node, $autoReindex=TRUE) {
    $NULL = NULL;
    if (is_string($node)) {
      if (empty($node)) { //--sam. Not sure how to react on an empty string - think it's an error.
        return array();
      } else { 
        if (!($node = $this->_xml2Document($node))) return FALSE;
      }
    }
    
    // Special case if it's 'super root'. We then have to take the child node == top node
    if (empty($node['parentNode'])) $node = $node['childNodes'][0];
    
    $status = FALSE;
    do { // try-block
      // Check for a valid xPathQuery
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'replaceChild');
      if (sizeOf($xPathSet) === 0) {
        $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
        break; // try-block
      }
      $mustReindex = FALSE;
      
      // Make chages from 'bottom-up'. In this manner the modifications will not affect itself.
      for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
        $mustReindex = $autoReindex;
        // Flag the index as dirty; it's not uptodate. A reindex will be forced (if dirty) when exporting the XML doc
        $this->_indexIsDirty = TRUE;
        
        $absoluteXPath = $xPathSet[$i];
        $childNode =& $this->nodeIndex[$absoluteXPath];
        $parentNode =& $childNode['parentNode'];
        $childNode['parentNode'] =& $NULL;
        $childPos = $childNode['pos'];
        $parentNode['childNodes'][$childPos] =& $this->cloneNode($node);
      }
      if ($mustReindex) $this->reindexNodeTree();
      $status = TRUE;
    } while(FALSE);
    
    if (!$status) return FALSE;
    return $childNode;
  }
  
  /**
   * Insert passed node (or passed node-tree) at the node(s) that matches the xQuery.
   *
   * With parameters you can define if the 'hit'-node is shifted to the right or left 
   * and if it's placed before of after the text-part.
   * Per derfault the 'hit'-node is shifted to the right and the node takes the place 
   * the of the 'hit'-node. 
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   * 
   * E.g. Following is given:           AAA[1]           
   *                                  /       \          
   *                              ..BBB[1]..BBB[2] ..    
   *
   * a) insertChild('/AAA[1]/BBB[2]', <node CCC>)
   * b) insertChild('/AAA[1]/BBB[2]', <node CCC>, $shiftRight=FALSE)
   * c) insertChild('/AAA[1]/BBB[2]', <node CCC>, $shiftRight=FALSE, $afterText=FALSE)
   *
   * a)                          b)                           c)                        
   *          AAA[1]                       AAA[1]                       AAA[1]          
   *        /    |   \                   /    |   \                   /    |   \        
   *  ..BBB[1]..CCC[1]BBB[2]..     ..BBB[1]..BBB[2]..CCC[1]     ..BBB[1]..BBB[2]CCC[1]..
   *
   * #### Do a complete review of the "(optional)" tag after several arguments.
   *
   * @param  $xPathQuery  (string) Xpath to the node to append.
   * @param  $node        (mixed)  String or Array (Usually a String)
   *                               If string: Vaild XML. E.g. "<A/>" or "<A> foo <B/> bar <A/>"
   *                               If array:  A Node (can be a whole sub-tree) (See comment in header)
   * @param  $shiftRight  (bool)   (optional, default=TRUE) Shift the target node to the right.
   * @param  $afterText   (bool)   (optional, default=TRUE) Insert after the text.
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                                the changes.  A performance helper.  See reindexNodeTree()
   * @return              (mixed)  FALSE on error (or no match). On success we return the path(s) to the newly
   *                               appended nodes. That is: Array of paths if more then 1 node was added or
   *                               a single path string if only one node was added.
   *                               NOTE:  If autoReindex is FALSE, then we can't return the *complete* path
   *                               as the exact doc-pos isn't available without reindexing. In that case we leave
   *                               out the last [docpos] in the path(s). ie  we'd return /A[3]/B instead of /A[3]/B[2]
   * @see    appendChildByXml(), reindexNodeTree()
   */
  function insertChild($xPathQuery, $node, $shiftRight=TRUE, $afterText=TRUE, $autoReindex=TRUE) {
    if (is_string($node)) {
      if (empty($node)) { //--sam. Not sure how to react on an empty string - think it's an error.
        return FALSE;
      } else { 
        if (!($node = $this->_xml2Document($node))) return FALSE;
      }
    }

    // Special case if it's 'super root'. We then have to take the child node == top node
    if (empty($node['parentNode'])) $node = $node['childNodes'][0];
    
    // Check for a valid xPathQuery
    $xPathSet = $this->_resolveXPathQuery($xPathQuery,'insertChild');
    if (sizeOf($xPathSet) === 0) {
      $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
      return FALSE;
    }
    $mustReindex = FALSE;
    $newNodes = array();
    $result = array();
    // Make chages from 'bottom-up'. In this manner the modifications will not affect itself.
    for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
      $absoluteXPath = $xPathSet[$i];
      $childNode =& $this->nodeIndex[$absoluteXPath];
      $parentNode =& $childNode['parentNode'];

      // We can't insert at the super root or at the root.
      if (empty($absoluteXPath) || (!$parentNode['parentNode'])) {
        $this->_displayError(sprintf($this->errorStrings['RootNodeAlreadyExists']), __LINE__, __FILE__, FALSE);
        return FALSE;
      }

      $mustReindex = $autoReindex;
      // Flag the index as dirty; it's not uptodate. A reindex will be forced (if dirty) when exporting the XML doc
      $this->_indexIsDirty = TRUE;
      
      //Special case: It not possible to add siblings to the top node.
      if (empty($parentNode['name'])) continue;
      $newNode =& $this->cloneNode($node);
      $pos = $shiftRight ? $childNode['pos'] : $childNode['pos']+1;
      $parentNode['childNodes'] = array_merge(
                                    array_slice($parentNode['childNodes'], 0, $pos),
                                    array(&$newNode),
                                    array_slice($parentNode['childNodes'], $pos)
                                  );
      $pos += $afterText ? 1 : 0;
      $parentNode['textParts'] = array_merge(
                                   array_slice($parentNode['textParts'], 0, $pos),
                                   '',
                                   array_slice($parentNode['textParts'], $pos)
                                 );
      
      // We are going from bottom to top, but the user will want results from top to bottom.
      if ($mustReindex) {
        // We'll have to wait till after the reindex to get the full path to this new node.
        $newNodes[] = &$newNode;
      } else {
        // If we are reindexing the tree later, then we can't return the user any
        // useful results, so we just return them the count.
        $newNodePath = $parentNode['xpath'].'/'.$newNode['name'];
        array_unshift($result, $newNodePath);
      }
    }
    if ($mustReindex) {
      $this->reindexNodeTree();
      // Now we must fill in the result array.  Because until now we did not
      // know what contextpos our newly added entries had, just their pos within
      // the siblings.
      foreach ($newNodes as $newNode) {
        array_unshift($result, $newNode['xpath']);
      }
    }
    if (count($result) == 1) $result = $result[0];
    return $result;
  }
  
  /**
   * Appends a child to anothers children.
   *
   * If you intend to do a lot of appending, you should leave autoIndex as FALSE
   * and then call reindexNodeTree() when you are finished all the appending.
   *
   * @param  $xPathQuery  (string) Xpath to the node to append to.
   * @param  $node        (mixed)  String or Array (Usually a String)
   *                               If string: Vaild XML. E.g. "<A/>" or "<A> foo <B/> bar <A/>"
   *                               If array:  A Node (can be a whole sub-tree) (See comment in header)
   * @param  $afterText   (bool)   (optional, default=FALSE) Insert after the text.
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                               the changes.  A performance helper.  See reindexNodeTree()
   * @return              (mixed)  FALSE on error (or no match). On success we return the path(s) to the newly
   *                               appended nodes. That is: Array of paths if more then 1 node was added or
   *                               a single path string if only one node was added.
   *                               NOTE:  If autoReindex is FALSE, then we can't return the *complete* path
   *                               as the exact doc-pos isn't available without reindexing. In that case we leave
   *                               out the last [docpos] in the path(s). ie  we'd return /A[3]/B instead of /A[3]/B[2]
   * @see    insertChild(), reindexNodeTree()
   */
  function appendChild($xPathQuery, $node, $afterText=FALSE, $autoReindex=TRUE) {
    if (is_string($node)) {
      if (empty($node)) { //--sam. Not sure how to react on an empty string - think it's an error.
        return FALSE;
      } else { 
        if (!($node = $this->_xml2Document($node))) return FALSE;
      }
    }
    
    // Special case if it's 'super root'. We then have to take the child node == top node
    if (empty($node['parentNode'])) $node = $node['childNodes'][0];

    // Check for a valid xPathQuery
    $xPathSet = $this->_resolveXPathQueryForNodeMod($xPathQuery, 'appendChild');
    if (sizeOf($xPathSet) === 0) return FALSE;

    $mustReindex = FALSE;
    $newNodes = array();
    $result = array();
    // Make chages from 'bottom-up'. In this manner the modifications will not affect itself.
    for ($i=sizeOf($xPathSet)-1; $i>=0; $i--) {
      $mustReindex = $autoReindex;
      // Flag the index as dirty; it's not uptodate. A reindex will be forced (if dirty) when exporting the XML doc
      $this->_indexIsDirty = TRUE;
      
      $absoluteXPath = $xPathSet[$i];
      $parentNode =& $this->nodeIndex[$absoluteXPath];
      $newNode =& $this->cloneNode($node);
      $parentNode['childNodes'][] =& $newNode;
      $pos = count($parentNode['textParts']);
      $pos -= $afterText ? 0 : 1;
      $parentNode['textParts'] = array_merge(
                                   array_slice($parentNode['textParts'], 0, $pos),
                                   '',
                                   array_slice($parentNode['textParts'], $pos)
                                 );
      // We are going from bottom to top, but the user will want results from top to bottom.
      if ($mustReindex) {
        // We'll have to wait till after the reindex to get the full path to this new node.
        $newNodes[] = &$newNode;
      } else {
        // If we are reindexing the tree later, then we can't return the user any
        // useful results, so we just return them the count.
        array_unshift($result, "$absoluteXPath/{$newNode['name']}");
      }
    }
    if ($mustReindex) {
      $this->reindexNodeTree();
      // Now we must fill in the result array.  Because until now we did not
      // know what contextpos our newly added entries had, just their pos within
      // the siblings.
      foreach ($newNodes as $newNode) {
        array_unshift($result, $newNode['xpath']);
      }
    } 
    if (count($result) == 1) $result = $result[0];
    return $result;
  }
  
  /**
   * Inserts a node before the reference node with the same parent.
   *
   * If you intend to do a lot of appending, you should leave autoIndex as FALSE
   * and then call reindexNodeTree() when you are finished all the appending.
   *
   * @param  $xPathQuery  (string) Xpath to the node to insert new node before
   * @param  $node        (mixed)  String or Array (Usually a String)
   *                               If string: Vaild XML. E.g. "<A/>" or "<A> foo <B/> bar <A/>"
   *                               If array:  A Node (can be a whole sub-tree) (See comment in header)
   * @param  $afterText   (bool)   (optional, default=FLASE) Insert after the text.
   * @param  $autoReindex (bool)   (optional, default=TRUE) Reindex the document to reflect 
   *                               the changes.  A performance helper.  See reindexNodeTree()
   * @return              (mixed)  FALSE on error (or no match). On success we return the path(s) to the newly
   *                               appended nodes. That is: Array of paths if more then 1 node was added or
   *                               a single path string if only one node was added.
   *                               NOTE:  If autoReindex is FALSE, then we can't return the *complete* path
   *                               as the exact doc-pos isn't available without reindexing. In that case we leave
   *                               out the last [docpos] in the path(s). ie  we'd return /A[3]/B instead of /A[3]/B[2]
   * @see    reindexNodeTree()
   */
  function insertBefore($xPathQuery, $node, $afterText=TRUE, $autoReindex=TRUE) {
    return $this->insertChild($xPathQuery, $node, $shiftRight=TRUE, $afterText, $autoReindex);
  }
  

  //-----------------------------------------------------------------------------------------
  // XPath                     ------  Attribute  Set/Get  ------                            
  //-----------------------------------------------------------------------------------------
  
  /** 
   * Retrieves a dedecated attribute value or a hash-array of all attributes of a node.
   * 
   * The first param $absoluteXPath must be a valid xpath OR a xpath-query that results 
   * to *one* xpath. If the second param $attrName is not set, a hash-array of all attributes 
   * of that node is returned.
   *
   * Optionally you may pass an attrubute name in $attrName and the function will return the 
   * string value of that attribute.
   *
   * @param  $absoluteXPath (string) Full xpath OR a xpath-query that results to *one* xpath.
   * @param  $attrName      (string) (Optional) The name of the attribute. See above.
   * @return                (mixed)  hash-array or a string of attributes depending if the 
   *                                 parameter $attrName was set (see above).  FALSE if the 
   *                                 node or attribute couldn't be found.
   * @see    setAttribute(), removeAttribute()
   */
  function getAttributes($absoluteXPath, $attrName=NULL) {
    // Numpty check
    if (!isSet($this->nodeIndex[$absoluteXPath])) {
      $xPathSet = $this->_resolveXPathQuery($absoluteXPath,'getAttributes');
      if (empty($xPathSet)) return FALSE;
      // only use the first entry
      $absoluteXPath = $xPathSet[0];
    }
    
    // Return the complete list or just the desired element
    if (is_null($attrName)) {
      return $this->nodeIndex[$absoluteXPath]['attributes'];
    } elseif (isSet($this->nodeIndex[$absoluteXPath]['attributes'][$attrName])) {
      return $this->nodeIndex[$absoluteXPath]['attributes'][$attrName];
    }
    return FALSE;
  }
  
  /**
   * Set attributes of a node(s).
   *
   * This method sets a number single attributes. An existing attribute is overwritten (default)
   * with the new value, but setting the last param to FALSE will prevent overwritten.
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery (string) xpath to the node (See note above).
   * @param  $name       (string) Attribute name.
   * @param  $value      (string) Attribute value.   
   * @param  $overwrite  (bool)   If the attribute is already set we overwrite it (see text above)
   * @return             (bool)   TRUE on success, FALSE on failure.
   * @see    getAttribute(), removeAttribute()
   */
  function setAttribute($xPathQuery, $name, $value, $overwrite=TRUE) {
    return $this->setAttributes($xPathQuery, array($name => $value), $overwrite);
  }
  
  /**
   * Version of setAttribute() that sets multiple attributes to node(s).
   *
   * This method sets a number of attributes. Existing attributes are overwritten (default)
   * with the new values, but setting the last param to FALSE will prevent overwritten.
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery (string) xpath to the node (See note above).
   * @param  $attributes (array)  associative array of attributes to set.
   * @param  $overwrite  (bool)   If the attributes are already set we overwrite them (see text above)
   * @return             (bool)   TRUE on success, FALSE otherwise
   * @see    setAttribute(), getAttribute(), removeAttribute()
   */
  function setAttributes($xPathQuery, $attributes, $overwrite=TRUE) {
    $status = FALSE;
    do { // try-block
      // The attributes parameter should be an associative array.
      if (!is_array($attributes)) break;  // try-block
      
      // Check for a valid xPathQuery
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'setAttributes');
      foreach($xPathSet as $absoluteXPath) {
        // Add the attributes to the node.
        $theNode =& $this->nodeIndex[$absoluteXPath];
        if (empty($theNode['attributes'])) {
          $this->nodeIndex[$absoluteXPath]['attributes'] = $attributes;
        } else {
          $theNode['attributes'] = $overwrite ? array_merge($theNode['attributes'],$attributes) : array_merge($attributes, $theNode['attributes']);
        }
      }
      $status = TRUE;
    } while(FALSE); // END try-block
    
    return $status;
  }
  
  /**
   * Removes an attribute of a node(s).
   *
   * This method removes *ALL* attributres per default unless the second parameter $attrList is set.
   * $attrList can be either a single attr-name as string OR a vector of attr-names as array.
   * E.g. 
   *  removeAttribute(<xPath>);                     # will remove *ALL* attributes.
   *  removeAttribute(<xPath>, 'A');                # will only remove attributes called 'A'.
   *  removeAttribute(<xPath>, array('A_1','A_2')); # will remove attribute 'A_1' and 'A_2'.
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param   $xPathQuery (string) xpath to the node (See note above).
   * @param   $attrList   (mixed)  (optional) if not set will delete *all* (see text above)
   * @return              (bool)   TRUE on success, FALSE if the node couldn't be found
   * @see     getAttribute(), setAttribute()
   */
  function removeAttribute($xPathQuery, $attrList=NULL) {
    // Check for a valid xPathQuery
    $xPathSet = $this->_resolveXPathQuery($xPathQuery, 'removeAttribute');
    
    if (!empty($attrList) AND is_string($attrList)) $attrList = array($attrList);
    if (!is_array($attrList)) return FALSE;
    
    foreach($xPathSet as $absoluteXPath) {
      // If the attribute parameter wasn't set then remove all the attributes
      if ($attrList[0] === NULL) {
        $this->nodeIndex[$absoluteXPath]['attributes'] = array();
        continue; 
      }
      // Remove all the elements in the array then.
      foreach($attrList as $name) {
        unset($this->nodeIndex[$absoluteXPath]['attributes'][$name]);
      }
    }
    return TRUE;
  }
  
  //-----------------------------------------------------------------------------------------
  // XPath                        ------  Text  Set/Get  ------                              
  //-----------------------------------------------------------------------------------------
  
  /**
   * Retrieve all the text from a node as a single string.
   *
   * Sample  
   * Given is: <AA> This <BB\>is <BB\>  some<BB\>text </AA>
   * Return of getData('/AA[1]') would be:  " This is   sometext "
   * The first param $xPathQuery must be a valid xpath OR a xpath-query that 
   * results to *one* xpath. 
   *
   * @param  $xPathQuery (string) xpath to the node - resolves to *one* xpath.
   * @return             (mixed)  The returned string (see above), FALSE if the node 
   *                              couldn't be found or is not unique.
   * @see getDataParts()
   */
  function getData($xPathQuery) {
    $aDataParts = $this->getDataParts($xPathQuery);
    if ($aDataParts === FALSE) return FALSE;
    return implode('', $aDataParts);
  }
  
  /**
   * Retrieve all the text from a node as a vector of strings
   * 
   * Where each element of the array was interrupted by a non-text child element.
   *
   * Sample  
   * Given is: <AA> This <BB\>is <BB\>  some<BB\>text </AA>
   * Return of getDataParts('/AA[1]') would be:  array([0]=>' This ', [1]=>'is ', [2]=>'  some', [3]=>'text ');
   * The first param $absoluteXPath must be a valid xpath OR a xpath-query that results 
   * to *one* xpath. 
   *
   * @param  $xPathQuery (string) xpath to the node - resolves to *one* xpath.
   * @return             (mixed)  The returned array (see above), or FALSE if node is not 
   *                              found or is not unique.
   * @see getData()
   */
  function getDataParts($xPathQuery) {
    // Resolve xPath argument
    $xPathSet = $this->_resolveXPathQuery($xPathQuery, 'getDataParts');
    if (1 !== ($setSize=count($xPathSet))) {
      $this->_displayError(sprintf($this->errorStrings['AbsoluteXPathRequired'], $xPathQuery) . "Not unique xpath-query, matched {$setSize}-times.", __LINE__, __FILE__, FALSE);
      return FALSE;
    }
    $absoluteXPath = $xPathSet[0];
    // Is it an attribute node?
    if (preg_match(";(.*)/attribute::([^/]*)$;U", $xPathSet[0], $matches)) {
      $absoluteXPath = $matches[1];
      $attribute = $matches[2];
      if (!isSet($this->nodeIndex[$absoluteXPath]['attributes'][$attribute])) {
        $this->_displayError("The $absoluteXPath/attribute::$attribute value isn't a node in this document.", __LINE__, __FILE__, FALSE);
        continue;
      }
      return array($this->nodeIndex[$absoluteXPath]['attributes'][$attribute]);
    } else if (preg_match(":(.*)/text\(\)(\[(.*)\])?$:U", $xPathQuery, $matches)) {
      $absoluteXPath = $matches[1];
      $textPartNr = $matches[2];      
      return array($this->nodeIndex[$absoluteXPath]['textParts'][$textPartNr]);
    } else {
      return $this->nodeIndex[$absoluteXPath]['textParts'];
    }
  }
  
  /**
   * Retrieves a sub string of a text-part OR attribute-value.
   *
   * This method retrieves the sub string of a specific text-part OR (if the 
   * $absoluteXPath references an attribute) the the sub string  of the attribute value.
   * If no 'direct referencing' is used (Xpath ends with text()[<part-number>]), then 
   * the first text-part of the node ist returned (if exsiting).
   *
   * @param  $absoluteXPath (string) Xpath to the node (See note above).   
   * @param  $offset        (int)    (optional, default is 0) Starting offset. (Just like PHP's substr())
   * @param  $count         (number) (optional, default is ALL) Character count  (Just like PHP's substr())
   * @return                (mixed)  The sub string, FALSE if not found or on error
   * @see    XPathEngine::wholeText(), PHP's substr()
   */
  function substringData($absoluteXPath, $offset = 0, $count = NULL) {
    if (!($text = $this->wholeText($absoluteXPath))) return FALSE;
    if (is_null($count)) {
      return substr($text, $offset);
    } else {
      return substr($text, $offset, $count);
    } 
  }
  
  /**
   * Replace a sub string of a text-part OR attribute-value.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery    (string) xpath to the node (See note above).
   * @param  $replacement   (string) The string to replace with.
   * @param  $offset        (int)    (optional, default is 0) Starting offset. (Just like PHP's substr_replace ())
   * @param  $count         (number) (optional, default is 0=ALL) Character count  (Just like PHP's substr_replace())
   * @param  $textPartNr    (int)    (optional) (see _getTextSet() )
   * @return                (bool)   The new string value on success, FALSE if not found or on error
   * @see    substringData()
   */
  function replaceData($xPathQuery, $replacement, $offset = 0, $count = 0, $textPartNr=1) {
    if (!($textSet = $this->_getTextSet($xPathQuery, $textPartNr))) return FALSE;
    $tSize=sizeOf($textSet);
    for ($i=0; $i<$tSize; $i++) {
      if ($count) {
        $textSet[$i] = substr_replace($textSet[$i], $replacement, $offset, $count);
      } else {
        $textSet[$i] = substr_replace($textSet[$i], $replacement, $offset);
      } 
    }
    return TRUE;
  }
  
  /**
   * Insert a sub string in a text-part OR attribute-value.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery (string) xpath to the node (See note above).
   * @param  $data       (string) The string to replace with.
   * @param  $offset     (int)    (optional, default is 0) Offset at which to insert the data.
   * @return             (bool)   The new string on success, FALSE if not found or on error
   * @see    replaceData()
   */
  function insertData($xPathQuery, $data, $offset=0) {
    return $this->replaceData($xPathQuery, $data, $offset, 0);
  }
  
  /**
   * Append text data to the end of the text for an attribute OR node text-part.
   *
   * This method adds content to a node. If it's an attribute node, then
   * the value of the attribute will be set, otherwise the passed data will append to 
   * character data of the node text-part. Per default the first text-part is taken.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param   $xPathQuery (string) to the node(s) (See note above).
   * @param   $data       (string) String containing the content to be added.
   * @param   $textPartNr (int)    (optional, default is 1) (see _getTextSet())
   * @return              (bool)   TRUE on success, otherwise FALSE
   * @see     _getTextSet()
   */
  function appendData($xPathQuery, $data, $textPartNr=1) {
    if (!($textSet = $this->_getTextSet($xPathQuery, $textPartNr))) return FALSE;
    $tSize=sizeOf($textSet);
    for ($i=0; $i<$tSize; $i++) {
      $textSet[$i] .= $data;
    }
    return TRUE;
  }
  
  /**
   * Delete the data of a node.
   *
   * This method deletes content of a node. If it's an attribute node, then
   * the value of the attribute will be removed, otherwise the node text-part. 
   * will be deleted.  Per default the first text-part is deleted.
   *
   * NOTE: When passing a xpath-query instead of an abs. Xpath.
   *       Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param  $xPathQuery (string) to the node(s) (See note above).
   * @param  $offset     (int)    (optional, default is 0) Starting offset. (Just like PHP's substr_replace())
   * @param  $count      (number) (optional, default is 0=ALL) Character count.  (Just like PHP's substr_replace())
   * @param  $textPartNr (int)    (optional, default is 0) the text part to delete (see _getTextSet())
   * @return             (bool)   TRUE on success, otherwise FALSE
   * @see     _getTextSet()
   */
  function deleteData($xPathQuery, $offset=0, $count=0, $textPartNr=1) {
    if (!($textSet = $this->_getTextSet($xPathQuery, $textPartNr))) return FALSE;
    $tSize=sizeOf($textSet);
    for ($i=0; $i<$tSize; $i++) {
      if (!$count)
        $textSet[$i] = "";
      else
        $textSet[$i] = substr_replace($textSet[$i],'', $offset, $count);
    } 
    return TRUE;
  }
 
  //-----------------------------------------------------------------------------------------
  // XPath                      ------  Help Stuff  ------                                   
  //-----------------------------------------------------------------------------------------
   
  /**
   * Parse the XML to a node-tree. A so called 'document'
   *
   * @param  $xmlString (string) The string to turn into a document node.
   * @return            (&array)  a node-tree
   */
  function &_xml2Document($xmlString) {
    $xmlOptions = array(
                    XML_OPTION_CASE_FOLDING => $this->getProperties('caseFolding'), 
                    XML_OPTION_SKIP_WHITE   => $this->getProperties('skipWhiteSpaces')
                  );
    $xmlParser =& new XPathEngine($xmlOptions);
    $xmlParser->setVerbose($this->properties['verboseLevel']);
    // Parse the XML string
    if (!$xmlParser->importFromString($xmlString)) {
      $this->_displayError($xmlParser->getLastError(), __LINE__, __FILE__, FALSE);
      return FALSE;
    }
    return $xmlParser->getNode('/');
  }
  
  /**
   * Get a reference-list to node text part(s) or node attribute(s).
   * 
   * If the Xquery references an attribute(s) (Xquery ends with attribute::), 
   * then the text value of the node-attribute(s) is/are returned.
   * Otherwise the Xquery is referencing to text part(s) of node(s). This can be either a 
   * direct reference to text part(s) (Xquery ends with text()[<nr>]) or indirect reference 
   * (a simple Xquery to node(s)).
   * 1) Direct Reference (Xquery ends with text()[<part-number>]):
   *   If the 'part-number' is omitted, the first text-part is assumed; starting by 1.
   *   Negative numbers are allowed, where -1 is the last text-part a.s.o.
   * 2) Indirect Reference (a simple  Xquery to node(s)):
   *   Default is to return the first text part(s). Optionally you may pass a parameter 
   *   $textPartNr to define the text-part you want;  starting by 1.
   *   Negative numbers are allowed, where -1 is the last text-part a.s.o.
   *
   * NOTE I : The returned vector is a set of references to the text parts / attributes.
   *          This is handy, if you wish to modify the contents.
   * NOTE II: text-part numbers out of range will not be in the list
   * NOTE III:Instead of an absolute xpath you may also pass a xpath-query.
   *          Depending on setModMatch() one, none or multiple nodes are affected.
   *
   * @param   $xPathQuery (string) xpath to the node (See note above).
   * @param   $textPartNr (int)    String containing the content to be set.
   * @return              (mixed)  A vector of *references* to the text that match, or 
   *                               FALSE on error
   * @see XPathEngine::wholeText()
   */
  function _getTextSet($xPathQuery, $textPartNr=1) {
    $status = FALSE;

    $bDebugThisFunction = FALSE;  // Get diagnostic output for this function
    if ($bDebugThisFunction) {
      $aStartTime = $this->_beginDebugFunction('_getTextSet');
      echo "Node: $xPathQuery\n";
      echo "Text Part Number: $textPartNr\n";
      echo "<hr>";
    }
    
    $funcName = '_getTextSet';
    $textSet = array();
    
    do { // try-block
      // Check if it's a Xpath reference to an attribut(s). Xpath ends with attribute::)
      if (preg_match(";(.*)/(attribute::|@)([^/]*)$;U", $xPathQuery, $matches)) {
        $xPathQuery = $matches[1];
        $attribute = $matches[3];
        // Quick out
        if (isSet($this->nodeIndex[$xPathQuery])) {
          $xPathSet[] = $xPathQuery;
        } else {
          // Try to evaluate the absoluteXPath (since it seems to be an Xquery and not an abs. Xpath)
          $xPathSet = $this->_resolveXPathQuery("$xPathQuery/attribute::$attribute", $funcName);
        }
        foreach($xPathSet as $absoluteXPath) {
          preg_match(";(.*)/attribute::([^/]*)$;U", $xPathSet[0], $matches);
          $absoluteXPath = $matches[1];
          $attribute = $matches[2];
          if (!isSet($this->nodeIndex[$absoluteXPath]['attributes'][$attribute])) {
            $this->_displayError("The $absoluteXPath/attribute::$attribute value isn't a node in this document.", __LINE__, __FILE__, FALSE);
            continue;
          }
          $textSet[] =& $this->nodes[$absoluteXPath]['attributes'][$attribute];
        }
        $status = TRUE;
        break; // try-block
      }
      
      // Check if it's a Xpath reference direct to a text-part(s). (xpath ends with text()[<part-number>])
      if (preg_match(":(.*)/text\(\)(\[(.*)\])?$:U", $xPathQuery, $matches)) {
        $xPathQuery = $matches[1];
        // default to the first text node if a text node was not specified
        $textPartNr = isSet($matches[2]) ? substr($matches[2],1,-1) : 1;
        // Quick check
        if (isSet($this->nodeIndex[$xPathQuery])) {
          $xPathSet[] = $xPathQuery;
        } else {
          // Try to evaluate the absoluteXPath (since it seams to be an Xquery and not an abs. Xpath)
          $xPathSet = $this->_resolveXPathQuery("$xPathQuery/text()[$textPartNr]", $funcName);
        }
      }
      else {
        // At this point we have been given an xpath with neither a 'text()' or 'attribute::' axis at the end
        // So this means to get the text-part of the node. If parameter $textPartNr was not set, use the last
        // text-part.
        if (isSet($this->nodeIndex[$xPathQuery])) {
          $xPathSet[] = $xPathQuery;
        } else {
          // Try to evaluate the absoluteXPath (since it seams to be an Xquery and not an abs. Xpath)
          $xPathSet = $this->_resolveXPathQuery($xPathQuery, $funcName);
        }
      }

      if ($bDebugThisFunction) {
        echo "Looking up paths for:\n";
        print_r($xPathSet);
      }

      // Now fetch all text-parts that match. (May be 0,1 or many)
      foreach($xPathSet as $absoluteXPath) {
        unset($text);
        if ($text =& $this->wholeText($absoluteXPath, $textPartNr)) {
          $textSet[] =& $text;
        } else {
          // The node does not yet have any text, so we have to add a '' string so that
          // if we insert or replace to it, then we'll actually have something to op on.
          $this->nodeIndex[$absoluteXPath]['textParts'][$textPartNr-1] = '';
          $textSet[] =& $this->nodeIndex[$absoluteXPath]['textParts'][$textPartNr-1];
        }
      }

      $status = TRUE;
    } while (FALSE); // END try-block
    
    if (!$status) $result = FALSE;
    else          $result = $textSet;

    if ($bDebugThisFunction) $this->_closeDebugFunction($aStartTime, $result);

    return $result;
  }
  

  /**
   * Resolves an xPathQuery vector for a node op for modification
   *
   * It is possible to create a brand new object, and try to append and insert nodes
   * into it, so this is a version of _resolveXPathQuery() that will autocreate the
   * super root if it detects that it is not present and the $xPathQuery is empty.
   *
   * Also it demands that there be at least one node returned, and displays a suitable
   * error message if the returned xPathSet does not contain any nodes.
   * 
   * @param  $xPathQuery (string) An xpath query targeting a single node.  If empty() 
   *                              returns the root node and auto creates the root node
   *                              if it doesn't exist.
   * @param  $function   (string) The function in which this check was called
   * @return             (array)  Vector of $absoluteXPath's (May be empty)
   * @see    _resolveXPathQuery()
   */
  function _resolveXPathQueryForNodeMod($xPathQuery, $functionName) {
    $xPathSet = array();
    if (empty($xPathQuery)) {
      // You can append even if the root node doesn't exist.
      if (!isset($this->nodeIndex[$xPathQuery])) $this->_createSuperRoot();
      $xPathSet[] = '';
      // However, you can only append to the super root, if there isn't already a root entry.
      $rootNodes = $this->_resolveXPathQuery('/*','appendChild');
      if (count($rootNodes) !== 0) {
        $this->_displayError(sprintf($this->errorStrings['RootNodeAlreadyExists']), __LINE__, __FILE__, FALSE);
        return array();
      }
    } else {
      $xPathSet = $this->_resolveXPathQuery($xPathQuery,'appendChild');
      if (sizeOf($xPathSet) === 0) {
        $this->_displayError(sprintf($this->errorStrings['NoNodeMatch'], $xPathQuery), __LINE__, __FILE__, FALSE);
        return array();
      }
    }
    return $xPathSet;
  }

  /**
   * Resolves an xPathQuery vector depending on the property['modMatch']
   * 
   * To:
   *   - all matches, 
   *   - the first
   *   - none (If the query matches more then one node.)
   * see  setModMatch() for details
   * 
   * @param  $xPathQuery (string) An xpath query targeting a single node.  If empty() 
   *                              returns the root node (if it exists).
   * @param  $function   (string) The function in which this check was called
   * @return             (array)  Vector of $absoluteXPath's (May be empty)
   * @see    setModMatch()
   */
  function _resolveXPathQuery($xPathQuery, $function) {
    $xPathSet = array();
    do { // try-block
      if (isSet($this->nodeIndex[$xPathQuery])) {
        $xPathSet[] = $xPathQuery;
        break; // try-block
      }
      if (empty($xPathQuery)) break; // try-block
      if (substr($xPathQuery, -1) === '/') break; // If the xPathQuery ends with '/' then it cannot be a good query.
      // If this xPathQuery is not absolute then attempt to evaluate it
      $xPathSet = $this->match($xPathQuery);
      
      $resultSize = sizeOf($xPathSet);
      switch($this->properties['modMatch']) {
        case XPATH_QUERYHIT_UNIQUE : 
          if ($resultSize >1) {
            $xPathSet = array();
            if ($this->properties['verboseLevel']) $this->_displayError("Canceled function '{$function}'. The query '{$xPathQuery}' mached {$resultSize} nodes and 'modMatch' is set to XPATH_QUERYHIT_UNIQUE.", __LINE__, __FILE__, FALSE);
          }
          break;
        case XPATH_QUERYHIT_FIRST : 
          if ($resultSize >1) {
            $xPathSet = array($xPathSet[0]);
            if ($this->properties['verboseLevel']) $this->_displayError("Only modified first node in function '{$function}' because the query '{$xPathQuery}' mached {$resultSize} nodes and 'modMatch' is set to XPATH_QUERYHIT_FIRST.", __LINE__, __FILE__, FALSE);
          }
          break;
        default: ; // DO NOTHING
      }
    } while (FALSE);
    
    if ($this->properties['verboseLevel'] >= 2) $this->_displayMessage("'{$xPathQuery}' parameter from '{$function}' returned the following nodes: ".(count($xPathSet)?implode('<br>', $xPathSet):'[none]'), __LINE__, __FILE__);
    return $xPathSet;
  }
} // END OF CLASS XPath

// -----------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------

/**************************************************************************************************
// Usage Sample:
// -------------
// Following code will give you an idea how to work with PHP.XPath. It's a working sample
// to help you get started. :o)
// Take the comment tags away and run this file.
**************************************************************************************************/

/**
 * Produces a short title line.
 */
/*
function _title($title) { 
  echo "<br><hr><b>" . htmlspecialchars($title) . "</b><hr>\n";
}

$self = isSet($_SERVER) ? $_SERVER['PHP_SELF'] : $PHP_SELF;
if (basename($self) == 'XPath.class.php') {
  // The sampe source:
  $q = '?';
  $xmlSource = <<< EOD
  <{$q}Process_Instruction test="&copy;&nbsp;All right reserved" {$q}>
    <AAA foo="bar"> ,,1,,
      ..1.. <![CDATA[ bla  bla 
      newLine blo blo ]]>
      <BBB foo="bar">
        ..2..
      </BBB>..3..<CC/>   ..4..</AAA> 
EOD;
  
  // The sample code:
  $xmlOptions = array(XML_OPTION_CASE_FOLDING => TRUE, XML_OPTION_SKIP_WHITE => TRUE);
  $xPath =& new XPath(FALSE, $xmlOptions);
  //$xPath->bDebugXmlParse = TRUE;
  if (!$xPath->importFromString($xmlSource)) { echo $xPath->getLastError(); exit; }
  
  _title("Following was imported:");
  echo $xPath->exportAsHtml();
  
  _title("Get some content");
  echo "Last text part in &lt;AAA&gt;: '" . $xPath->wholeText('/AAA[1]', -1) ."'<br>\n";
  echo "All the text in  &lt;AAA&gt;: '" . $xPath->wholeText('/AAA[1]') ."'<br>\n";
  echo "The attibute value  in  &lt;BBB&gt; using getAttributes('/AAA[1]/BBB[1]', 'FOO'): '" . $xPath->getAttributes('/AAA[1]', 'FOO') ."'<br>\n";
  echo "The attibute value  in  &lt;BBB&gt; using getData('/AAA[1]/@FOO'): '" . $xPath->getData('/AAA[1]/@FOO') ."'<br>\n";
  
  _title("Append some additional XML below /AAA/BBB:");
  $xPath->appendChild('/AAA[1]/BBB[1]', '<CCC> Step 1. Append new node </CCC>', $afterText=FALSE);
  $xPath->appendChild('/AAA[1]/BBB[1]', '<CCC> Step 2. Append new node </CCC>', $afterText=TRUE);
  $xPath->appendChild('/AAA[1]/BBB[1]', '<CCC> Step 3. Append new node </CCC>', $afterText=TRUE);
  echo $xPath->exportAsHtml();
  
  _title("Insert some additional XML below <AAA>:");
  $xPath->reindexNodeTree();
  $xPath->insertChild('/AAA[1]/BBB[1]', '<BB> Step 1. Insert new node </BB>', $shiftRight=TRUE, $afterText=TRUE);
  $xPath->insertChild('/AAA[1]/BBB[1]', '<BB> Step 2. Insert new node </BB>', $shiftRight=FALSE, $afterText=TRUE);
  $xPath->insertChild('/AAA[1]/BBB[1]', '<BB> Step 3. Insert new node </BB>', $shiftRight=FALSE, $afterText=FALSE);
  echo $xPath->exportAsHtml();

  _title("Replace the last <BB> node with new XML data '&lt;DDD&gt; Replaced last BB &lt;/DDD&gt;':");
  $xPath->reindexNodeTree();
  $xPath->replaceChild('/AAA[1]/BB[last()]', '<DDD> Replaced last BB </DDD>', $afterText=FALSE);
  echo $xPath->exportAsHtml();
  
  _title("Replace second <BB> node with normal text");
  $xPath->reindexNodeTree();
  $xPath->replaceChildByData('/AAA[1]/BB[2]', '"Some new text"');
  echo $xPath->exportAsHtml();
}
*/
?>