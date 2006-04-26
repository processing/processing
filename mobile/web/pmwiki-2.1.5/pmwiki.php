<?php
/*
    PmWiki
    Copyright 2001-2006 Patrick R. Michaud
    pmichaud@pobox.com
    http://www.pmichaud.com/

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

    ----
    Note from Pm:  Trying to understand the PmWiki code?  Wish it had 
    more comments?  If you want help with any of the code here,
    write me at <pmichaud@pobox.com> with your question(s) and I'll
    provide explanations (and add comments) that answer them.
*/
error_reporting(E_ALL ^ E_NOTICE);
StopWatch('PmWiki');
@ini_set('magic_quotes_runtime', 0);
@ini_set('magic_quotes_sybase', 0);
if (ini_get('register_globals')) 
  foreach($_REQUEST as $k=>$v) { 
    if (preg_match('/^(GLOBALS|_SERVER|_GET|_POST|_COOKIE|_FILES|_ENV|_REQUEST|_SESSION)$/i', $k)) exit();
    unset(${$k}); 
  }
$UnsafeGlobals = array_keys($GLOBALS); $GCount=0; $FmtV=array();
SDV($FarmD,dirname(__FILE__));
SDV($WorkDir,'wiki.d');
define('PmWiki',1);
@include_once("$FarmD/scripts/version.php");
$GroupPattern = '[[:upper:]][\\w]*(?:-\\w+)*';
$NamePattern = '[[:upper:]\\d][\\w]*(?:-\\w+)*';
$BlockPattern = 'form|div|table|t[rdh]|p|[uo]l|d[ltd]|h[1-6r]|pre|blockquote';
$WikiWordPattern = '[[:upper:]][[:alnum:]]*(?:[[:upper:]][[:lower:]0-9]|[[:lower:]0-9][[:upper:]])[[:alnum:]]*';
$WikiDir = new PageStore('wiki.d/{$FullName}');
$WikiLibDirs = array(&$WikiDir,new PageStore('$FarmD/wikilib.d/{$FullName}'));
$InterMapFiles = array("$FarmD/scripts/intermap.txt",
  "$FarmD/local/farmmap.txt", '$SiteGroup.InterMap', 'local/localmap.txt');
$Newline = "\263";                                 # deprecated, 2.0.0
$KeepToken = "\235\235";  
$Now=time();
define('READPAGE_CURRENT', $Now+604800);
$TimeFmt = '%B %d, %Y, at %I:%M %p';
$MessagesFmt = array();
$BlockMessageFmt = "<h3 class='wikimessage'>$[This post has been blocked by the administrator]</h3>";
$EditFields = array('text');
$EditFunctions = array('EditTemplate', 'RestorePage', 'ReplaceOnSave',
  'SaveAttributes', 'PostPage', 'PostRecentChanges', 'PreviewPage');
$EnablePost = 1;
$ChangeSummary = substr(stripmagic(@$_REQUEST['csum']), 0, 100);
$AsSpacedFunction = 'AsSpaced';
$SpaceWikiWords = 0;
$LinkWikiWords = 0;
$RCDelimPattern = '  ';
$RecentChangesFmt = array(
  '$SiteGroup.AllRecentChanges' => 
    '* [[{$Group}.{$Name}]]  . . . $CurrentTime $[by] $AuthorLink: [=$ChangeSummary=]',
  '$Group.RecentChanges' =>
    '* [[{$Group}/{$Name}]]  . . . $CurrentTime $[by] $AuthorLink: [=$ChangeSummary=]');
$ScriptUrl = 'http://'.$_SERVER['HTTP_HOST'].$_SERVER['SCRIPT_NAME'];
$PubDirUrl = preg_replace('#/[^/]*$#','/pub',$ScriptUrl,1);
$HTMLVSpace = "<p class='vspace'></p>";
$HTMLPNewline = '';
$MarkupFrame = array();
$MarkupFrameBase = array('cs' => array(), 'vs' => '', 'ref' => 0,
  'closeall' => array('block' => '<:block>'), 'is' => array(),
  'escape' => 1);
$WikiWordCountMax = 1000000;
$WikiWordCount['PmWiki'] = 1;
$UrlExcludeChars = '<>"{}|\\\\^`()[\\]\'';
$QueryFragPattern = "[?#][^\\s$UrlExcludeChars]*";
$SuffixPattern = '(?:-?[[:alnum:]]+)*';
$LinkPageSelfFmt = "<a class='selflink' href='\$LinkUrl'>\$LinkText</a>";
$LinkPageExistsFmt = "<a class='wikilink' href='\$LinkUrl'>\$LinkText</a>";
$LinkPageCreateFmt = 
  "<a class='createlinktext' rel='nofollow' 
    href='{\$PageUrl}?action=edit'>\$LinkText</a><a rel='nofollow' 
    class='createlink' href='{\$PageUrl}?action=edit'>?</a>";
$UrlLinkFmt = 
  "<a class='urllink' href='\$LinkUrl' rel='nofollow'>\$LinkText</a>";
umask(002);
$CookiePrefix = '';
$SiteGroup = 'Site';
$DefaultGroup = 'Main';
$DefaultName = 'HomePage';
$GroupHeaderFmt = '(:include {$Group}.GroupHeader self=0:)(:nl:)';
$GroupFooterFmt = '(:nl:)(:include {$Group}.GroupFooter self=0:)';
$PagePathFmt = array('{$Group}.$1','$1.$1','$1.{$DefaultName}');
$PageAttributes = array(
  'passwdread' => '$[Set new read password:]',
  'passwdedit' => '$[Set new edit password:]',
  'passwdattr' => '$[Set new attribute password:]');
$XLLangs = array('en');
if (preg_match('/^C$|\.UTF-?8/i',setlocale(LC_ALL,0)))
  setlocale(LC_ALL,'en_US');
$FmtP = array();
$FmtPV = array(
  # '$ScriptUrl'    => 'PUE($ScriptUrl)',   ## $ScriptUrl is special
  '$PageUrl'      => 
    'PUE(($EnablePathInfo) 
         ? "$ScriptUrl/$group/$name"
         : "$ScriptUrl?n=$group.$name")',
  '$FullName'     => '"$group.$name"',
  '$Groupspaced'  => '$AsSpacedFunction($group)',
  '$Namespaced'   => '$AsSpacedFunction($name)',
  '$Group'        => '$group',
  '$Name'         => '$name',
  '$Titlespaced'  => 
    '@$page["title"] ? $page["title"] : $AsSpacedFunction($name)',
  '$Title'        => 
    '@$page["title"] ? $page["title"] : ($GLOBALS["SpaceWikiWords"]
       ? $AsSpacedFunction($name) : $name)',
  '$LastModifiedBy' => '@$page["author"]',
  '$LastModifiedHost' => '@$page["host"]',
  '$LastModified' => 'strftime($GLOBALS["TimeFmt"], $page["time"])',
  '$LastModifiedSummary' => '@$page["csum"]',
  '$Description' => '@$page["description"]',
  '$SiteGroup'    => '$GLOBALS["SiteGroup"]',
  '$VersionNum'   => '$GLOBALS["VersionNum"]',
  '$Version'      => '$GLOBALS["Version"]',
  '$Author'       => 'NoCache($GLOBALS["Author"])',
  '$AuthId'       => 'NoCache($GLOBALS["AuthId"])',
  '$DefaultGroup' => '$GLOBALS["DefaultGroup"]',
  '$DefaultName'  => '$GLOBALS["DefaultName"]',
  '$Action'       => "'$action'",
  );
$SaveProperties = array('title', 'description', 'keywords');

$WikiTitle = 'PmWiki';
$Charset = 'ISO-8859-1';
$HTTPHeaders = array(
  "Expires: Tue, 01 Jan 2002 00:00:00 GMT",
  "Cache-Control: no-store, no-cache, must-revalidate",
  "Content-type: text/html; charset=ISO-8859-1;");
$CacheActions = array('browse','diff','print');
$EnableHTMLCache = 0;
$NoHTMLCache = 0;
$HTMLDoctypeFmt = 
  "<!DOCTYPE html 
    PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"
    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">
  <html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'><head>\n";
$HTMLStylesFmt['pmwiki'] = "
  ul, ol, pre, dl, p { margin-top:0px; margin-bottom:0px; }
  code.escaped { white-space: nowrap; }
  .vspace { margin-top:1.33em; }
  .indent { margin-left:40px; }
  .outdent { margin-left:40px; text-indent:-40px; }
  a.createlinktext { text-decoration:none; border-bottom:1px dotted gray; }
  a.createlink { text-decoration:none; position:relative; top:-0.5em;
    font-weight:bold; font-size:smaller; border-bottom:none; }
  img { border:0px; }
  ";
$HTMLHeaderFmt['styles'] = array(
  "<style type='text/css'><!--",&$HTMLStylesFmt,"\n--></style>");
$HTMLBodyFmt = "</head>\n<body>";
$HTMLStartFmt = array('headers:',&$HTMLDoctypeFmt,&$HTMLHeaderFmt,
  &$HTMLBodyFmt);
$HTMLEndFmt = "\n</body>\n</html>";
$PageStartFmt = array(&$HTMLStartFmt,"\n<div id='contents'>\n");
$PageEndFmt = array('</div>',&$HTMLEndFmt);

$HandleActions = array(
  'browse' => 'HandleBrowse',
  'edit' => 'HandleEdit', 'source' => 'HandleSource', 
  'attr' => 'HandleAttr', 'postattr' => 'HandlePostAttr',
  'logout' => 'HandleLogoutA', 'login' => 'HandleLoginA');
$HandleAuth = array(
  'browse' => 'read', 'source' => 'read',
  'edit' => 'edit', 'attr' => 'attr', 'postattr' => 'attr',
  'logout' => 'read', 'login' => 'login');
$ActionTitleFmt = array(
  'edit' => '| $[Edit]',
  'attr' => '| $[Attributes]');
$DefaultPasswords = array('admin'=>'*','read'=>'','edit'=>'','attr'=>'');
$AuthCascade = array('edit'=>'read', 'attr'=>'edit');
$AuthList = array('' => 1, 'nopass:' => 1, '@nopass' => 1);

$Conditions['enabled'] = '(boolean)@$GLOBALS[$condparm]';
$Conditions['false'] = 'false';
$Conditions['true'] = 'true';
$Conditions['group'] = 
  "(boolean)MatchPageNames(\$pagename, FixGlob(\$condparm, '$1$2.*'))";
$Conditions['name'] = 
  "(boolean)MatchPageNames(\$pagename, FixGlob(\$condparm, '$1*.$2'))";
$Conditions['match'] = 'preg_match("!$condparm!",$pagename)';
$Conditions['auth'] =
  'NoCache(@$GLOBALS["PCache"][$GLOBALS["pagename"]]["=auth"][trim($condparm)])';
$Conditions['authid'] = 'NoCache(@$GLOBALS["AuthId"] > "")';
$Conditions['exists'] = 'PageExists(MakePageName(\$pagename, \$condparm))';
$Conditions['equal'] = 'CompareArgs($condparm) == 0';
function CompareArgs($arg) 
  { $arg = ParseArgs($arg); return strcmp(@$arg[''][0], @$arg[''][1]); }

## CondExpr handles complex conditions (expressions)
## Portions Copyright 2005 by D. Faure (dfaure@cpan.org)
function CondExpr($pagename, $condname, $condparm) {
  global $CondExprOps;
  SDV($CondExprOps, 'and|x?or|&&|\\|\\||[!()]');
  if ($condname == '(' || $condname == '[')
    $condparm = preg_replace('/[\\]\\)]\\s*$/', '', $condparm);
  $condparm = str_replace('&amp;&amp;', '&&', $condparm);
  $terms = preg_split("/(?<!\\S)($CondExprOps)(?!\\S)/i", $condparm, -1,
                      PREG_SPLIT_DELIM_CAPTURE | PREG_SPLIT_NO_EMPTY);
  foreach($terms as $i => $t) {
    $t = trim($t);
    if (preg_match("/^($CondExprOps)$/i", $t)) continue;
    if ($t) $terms[$i] = CondText($pagename, "if $t", 'TRUE') ? '1' : '0';
  }
  return @eval('return(' . implode(' ', $terms) . ');');
}
$Conditions['expr'] = 'CondExpr($pagename, $condname, $condparm)';
$Conditions['('] = 'CondExpr($pagename, $condname, $condparm)';
$Conditions['['] = 'CondExpr($pagename, $condname, $condparm)';

$MarkupTable['_begin']['seq'] = 'B';
$MarkupTable['_end']['seq'] = 'E';
Markup('fulltext','>_begin');
Markup('split','>fulltext',"\n",
  '$RedoMarkupLine=1; return explode("\n",$x);');
Markup('directives','>split');
Markup('inline','>directives');
Markup('links','>inline');
Markup('block','>links');
Markup('style','>block');
Markup('closeall', '_begin',
  '/^\\(:closeall:\\)$/e', 
  "implode('', (array)\$GLOBALS['MarkupFrame'][0]['closeall'])");

$ImgExtPattern="\\.(?:gif|jpg|jpeg|png|GIF|JPG|JPEG|PNG)";
$ImgTagFmt="<img src='\$LinkUrl' alt='\$LinkAlt' title='\$LinkAlt' />";

$BlockMarkups = array(
  'block' => array('','','',0),
  'ul' => array('<ul><li>','</li><li>','</li></ul>',1),
  'dl' => array('<dl>','</dd>','</dd></dl>',1),
  'ol' => array('<ol><li>','</li><li>','</li></ol>',1),
  'p' => array('<p>','','</p>',0),
  'indent' => 
     array("<div class='indent'>","</div><div class='indent'>",'</div>',1),
  'outdent' => 
     array("<div class='outdent'>","</div><div class='outdent'>",'</div>',1),
  'pre' => array('<pre>','','</pre>',0),
  'table' => array("<table width='100%'>",'','</table>',0));

foreach(array('http:','https:','mailto:','ftp:','news:','gopher:','nap:',
    'file:') as $m) 
  { $LinkFunctions[$m] = 'LinkIMap';  $IMap[$m]="$m$1"; }
$LinkFunctions['<:page>'] = 'LinkPage';

$q = preg_replace('/(\\?|%3f)([-\\w]+=)/', '&$2', @$_SERVER['QUERY_STRING']);
if ($q != @$_SERVER['QUERY_STRING']) {
  unset($_GET);
  parse_str($q, $_GET);
  $_REQUEST = array_merge($_REQUEST, $_GET, $_POST);
}

if (isset($_GET['action'])) $action = $_GET['action'];
elseif (isset($_POST['action'])) $action = $_POST['action'];
else $action = 'browse';

$pagename = $_REQUEST['n'];
if (!$pagename) $pagename = $_REQUEST['pagename'];
if (!$pagename && 
    preg_match('!^'.preg_quote($_SERVER['SCRIPT_NAME'],'!').'/?([^?]*)!',
      $_SERVER['REQUEST_URI'],$match))
  $pagename = urldecode($match[1]);
if (preg_match('/[\\x80-\\xbf]/',$pagename)) 
  $pagename=utf8_decode($pagename);
$pagename = preg_replace('![^[:alnum:]\\x80-\\xff]+$!','',$pagename);
$FmtPV['$RequestedPage'] = "'".htmlspecialchars($pagename, ENT_QUOTES)."'";

if (file_exists("$FarmD/local/farmconfig.php")) 
  include_once("$FarmD/local/farmconfig.php");
if (IsEnabled($EnableLocalConfig,1)) {
  if (file_exists('local/config.php')) 
    include_once('local/config.php');
  elseif (file_exists('config.php'))
    include_once('config.php');
}

SDV($CurrentTime,strftime($TimeFmt,$Now));

if (IsEnabled($EnableStdConfig,1))
  include_once("$FarmD/scripts/stdconfig.php");

foreach((array)$InterMapFiles as $f) {
  $f = FmtPageName($f, $pagename);
  if (($v = @file($f))) 
    $v = preg_replace('/^\\s*(?>\\w+)(?!:)/m', '$0:', implode('', $v));
  else if (PageExists($f)) {
    $p = ReadPage($f, READPAGE_CURRENT);
    $v = $p['text'];
  } else continue;
  if (!preg_match_all("/^\\s*(\\w+:)[^\\S\n]+(\\S*)/m", $v, 
                      $match, PREG_SET_ORDER)) continue;
  foreach($match as $m) {
    if (strpos($m[2], '$1') === false) $m[2] .= '$1';
    $LinkFunctions[$m[1]] = 'LinkIMap';
    $IMap[$m[1]] = FmtPageName($m[2], $pagename);
  }
}

$LinkPattern = implode('|',array_keys($LinkFunctions));
SDV($LinkPageCreateSpaceFmt,$LinkPageCreateFmt);

$Action = FmtPageName(@$ActionTitleFmt[$action],$pagename);
if (!function_exists(@$HandleActions[$action])) $action='browse';
SDV($HandleAuth[$action], 'read');
$HandleActions[$action]($pagename, $HandleAuth[$action]);
Lock(0);
return;

## helper functions
function stripmagic($x) 
  { return get_magic_quotes_gpc() ? stripslashes($x) : $x; }
function PSS($x) 
  { return str_replace('\\"','"',$x); }
function PVS($x) 
  { return preg_replace("/\n[^\\S\n]*(?=\n)/", "\n<:vspace>", $x); }
function PZZ($x,$y='') { return ''; }
function PRR($x=NULL) 
  { if ($x || is_null($x)) $GLOBALS['RedoMarkupLine']++; return $x; }
function PUE($x)
  { return preg_replace('/[\\x80-\\xff ]/e', "'%'.dechex(ord('$0'))", $x); }
function PQA($x) { 
  return preg_replace('/([a-zA-Z])\\s*=\\s*([^\'">][^\\s>]*)/', "$1='$2'", $x);
}
function SDV(&$v,$x) { if (!isset($v)) $v=$x; }
function SDVA(&$var,$val) 
  { foreach($val as $k=>$v) if (!isset($var[$k])) $var[$k]=$v; }
function IsEnabled(&$var,$f=0)
  { return (isset($var)) ? $var : $f; }
function SetTmplDisplay($var, $val) 
  { NoCache(); $GLOBALS['TmplDisplay'][$var] = $val; }
function NoCache($x = '') { $GLOBALS['NoHTMLCache'] |= 1; return $x; }
function ParseArgs($x) {
  $z = array();
  preg_match_all('/([-+]|(?>(\\w+)[:=]))?("[^"]*"|\'[^\']*\'|\\S+)/',
    $x, $terms, PREG_SET_ORDER);
  foreach($terms as $t) {
    $v = preg_replace('/^([\'"])?(.*)\\1$/', '$2', $t[3]);
    if ($t[2]) { $z['#'][] = $t[2]; $z[$t[2]] = $v; }
    else { $z['#'][] = $t[1]; $z[$t[1]][] = $v; }
    $z['#'][] = $v;
  }
  return $z;
}
function StopWatch($x) { 
  global $StopWatch, $EnableStopWatch;
  if (!$EnableStopWatch) return;
  static $wstart = 0, $ustart = 0;
  list($usec,$sec) = explode(' ',microtime());
  $wtime = ($sec+$usec); 
  if (!$wstart) $wstart = $wtime;
  if ($EnableStopWatch != 2) 
    { $StopWatch[] = sprintf("%05.2f %s", $wtime-$wstart, $x); return; }
  $dat = getrusage();
  $utime = ($dat['ru_utime.tv_sec']+$dat['ru_utime.tv_usec']/1000000);
  if (!$ustart) $ustart=$utime;
  $StopWatch[] = 
    sprintf("%05.2f %05.2f %s", $wtime-$wstart, $utime-$ustart, $x);
}

## AsSpaced converts a string with WikiWords into a spaced version
## of that string.  (It can be overridden via $AsSpacedFunction.)
function AsSpaced($text) {
  $text = preg_replace("/([[:lower:]\\d])([[:upper:]])/", '$1 $2', $text);
  $text = preg_replace('/(?<![-\\d])(\\d+( |$))/',' $1',$text);
  return preg_replace("/([[:upper:]])([[:upper:]][[:lower:]\\d])/",
    '$1 $2', $text);
}

## Lock is used to make sure only one instance of PmWiki is running when
## files are being written.  It does not "lock pages" for editing.
function Lock($op) { 
  global $WorkDir,$LockFile;
  SDV($LockFile,"$WorkDir/.flock");
  mkdirp(dirname($LockFile));
  static $lockfp,$curop;
  if (!$lockfp) $lockfp = @fopen($LockFile,"w");
  if (!$lockfp) { 
    @unlink($LockFile); 
    $lockfp = fopen($LockFile,"w") or
      Abort("Cannot acquire lockfile", "Lockfile");
    fixperms($LockFile);
  }
  if ($op<0) { flock($lockfp,LOCK_UN); fclose($lockfp); $lockfp=0; $curop=0; }
  elseif ($op==0) { flock($lockfp,LOCK_UN); $curop=0; }
  elseif ($op==1 && $curop<1) 
    { session_write_close(); flock($lockfp,LOCK_SH); $curop=1; }
  elseif ($op==2 && $curop<2) 
    { session_write_close(); flock($lockfp,LOCK_EX); $curop=2; }
}

## mkdirp creates a directory and its parents as needed, and sets
## permissions accordingly.
function mkdirp($dir) {
  global $ScriptUrl;
  if (file_exists($dir)) return;
  if (!file_exists(dirname($dir))) mkdirp(dirname($dir));
  if (mkdir($dir, 0777)) {
    fixperms($dir);
    if (@touch("$dir/xxx")) { unlink("$dir/xxx"); return; }
    rmdir($dir);
  }
  $parent = realpath(dirname($dir)); 
  $perms = decoct(fileperms($parent) & 03777);
  $msg = "PmWiki needs to have a writable <tt>$dir/</tt> directory 
    before it can continue.  You can create the directory manually 
    by executing the following commands on your server:
    <pre>    mkdir $parent/$dir\n    chmod 777 $parent/$dir</pre>
    Then, <a href='{$ScriptUrl}'>reload this page</a>.";
  $safemode = ini_get('safe_mode');
  if (!$safemode) $msg .= "<br /><br />Or, for a slightly more 
    secure installation, try executing <pre>    chmod 2777 $parent</pre> 
    on your server and following <a target='_blank' href='$ScriptUrl'>
    this link</a>.  Afterwards you can restore the permissions to 
    their current setting by executing <pre>    chmod $perms $parent</pre>.";
  Abort($msg);
}

## fixperms attempts to correct permissions on a file or directory
## so that both PmWiki and the account (current dir) owner can manipulate it
function fixperms($fname, $add = 0) {
  clearstatcache();
  if (!file_exists($fname)) Abort('no such file');
  $bp = 0;
  if (fileowner($fname)!=@fileowner('.')) $bp = (is_dir($fname)) ? 007 : 006;
  if (filegroup($fname)==@filegroup('.')) $bp <<= 3;
  $bp |= $add;
  if ($bp && (fileperms($fname) & $bp) != $bp)
    @chmod($fname,fileperms($fname)|$bp);
}

## MatchPageNames
function MatchPageNames($pagelist, $pat) {
  $pagelist = (array)$pagelist;
  foreach((array)$pat as $p) {
    if (count($pagelist) < 1) break;
    switch ($p{0}) {
      case '/': 
        $pagelist = preg_grep($p, $pagelist); 
        continue;
      case '!':
        $pagelist = array_diff($pagelist, preg_grep($p, $pagelist)); 
        continue;
      default:
        $p = preg_quote($p, '/');
        $p = str_replace(array('/', '\\*', '\\?', '\\[', '\\]', '\\^'),
                         array('.', '.*', '.', '[', ']', '^'), $p);
        $excl = array(); $incl = array();
        foreach(preg_split('/[\\s,]+/', $p, -1, PREG_SPLIT_NO_EMPTY) as $q) {
          if ($q{0} == '-' || $q{0} == '!') $excl[] = '^'.substr($q, 1).'$';
          else $incl[] = "^$q$";
        }
        if ($excl) 
          $pagelist = array_diff($pagelist, 
                          preg_grep('/' . join('|', $excl) . '/i', $pagelist));
        if ($incl)
          $pagelist = preg_grep('/' . join('|', $incl) . '/i', $pagelist);
    }
  }
  return $pagelist;
}
function FixGlob($x, $rep = '$1*.$2') {
  return preg_replace('/([\\s,][-!]?)([^.\\s,]+)(?=[\\s,])/', $rep, " $x ");
}
  
## ResolvePageName "normalizes" a pagename based on the current
## settings of $DefaultPage and $PagePathFmt.  It's normally used
## during initialization to fix up any missing or partial pagenames.
function ResolvePageName($pagename) {
  global $DefaultPage, $DefaultGroup, $DefaultName,
    $GroupPattern, $NamePattern, $EnableFixedUrlRedirect;
  SDV($DefaultPage, "$DefaultGroup.$DefaultName");
  $pagename = preg_replace('!([./][^./]+)\\.html$!', '$1', $pagename);
  if ($pagename == '') return $DefaultPage;
  $p = MakePageName($DefaultPage, $pagename);
  if (preg_match("/^($GroupPattern)[.\\/]($NamePattern)$/i", $pagename))
    return $p;
  if (IsEnabled($EnableFixedUrlRedirect, 1)
      && $p && (PageExists($p) || preg_match('/[\\/.]/', $pagename)))
    { Redirect($p); exit(); }
  return MakePageName($DefaultPage, "$pagename.$pagename");
}

## MakePageName is used to convert a string into a valid pagename.
## If no group is supplied, then it uses $PagePathFmt to look
## for the page in other groups, or else uses the group of the
## pagename passed as an argument.
function MakePageName($basepage,$x) {
  global $MakePageNameFunction, $PageNameChars, $PagePathFmt,
    $MakePageNamePatterns;
  if (@$MakePageNameFunction) return $MakePageNameFunction($basepage,$x);
  SDV($PageNameChars,'-[:alnum:]');
  SDV($MakePageNamePatterns, array(
    "/'/" => '',			   # strip single-quotes
    "/[^$PageNameChars]+/" => ' ',         # convert everything else to space
    "/((^|[^-\\w])\\w)/e" => "strtoupper('$1')",
    "/ /" => ''));
  if (!preg_match('/(?:([^.\\/]+)[.\\/])?([^.\\/]+)$/',$x,$m)) return '';
  $name = preg_replace(array_keys($MakePageNamePatterns),
            array_values($MakePageNamePatterns), $m[2]);
  if ($m[1]) {
    $group = preg_replace(array_keys($MakePageNamePatterns),
               array_values($MakePageNamePatterns), $m[1]);
    return "$group.$name";
  }
  foreach((array)$PagePathFmt as $pg) {
    $pn = FmtPageName(str_replace('$1',$name,$pg),$basepage);
    if (PageExists($pn)) return $pn;
  }
  $group=preg_replace('/[\\/.].*$/','',$basepage);
  return "$group.$name";
}

## PCache caches basic information about a page and its attributes--
## usually everything except page text and page history.  This makes
## for quicker access to certain values in PageVar below.
function PCache($pagename, $page) {
  global $PCache;
  foreach($page as $k=>$v) 
    if ($k!='text' && strpos($k,':')===false) $PCache[$pagename][$k]=$v;
}

## SetProperty saves a page property into $PCache.  For convenience
## it returns the $value of the property just set.  If $sep is supplied,
## then $value is appended to the current property (with $sep as
## as separator) instead of replacing it.
function SetProperty($pagename, $prop, $value, $sep = NULL) {
  global $PCache, $KeepToken;
  NoCache();
  $prop = "=p_$prop";
  $value = preg_replace("/$KeepToken(\\d.*?)$KeepToken/e", 
                        "\$GLOBALS['KPV']['$1']", $value);
  if (!is_null($sep) && isset($PCache[$pagename][$prop]))
    $value = $PCache[$pagename][$prop] . $sep . $value;
  $PCache[$pagename][$prop] = $value;
  return $value;
}


function PageVar($pagename, $var, $pn = '') {
  global $Cursor, $PCache, $FmtPV, $AsSpacedFunction, $ScriptUrl,
    $EnablePathInfo;
  if ($var == '$ScriptUrl') return PUE($ScriptUrl);
  if ($pn) {
    $pn = isset($Cursor[$pn]) ? $Cursor[$pn] : MakePageName($pagename, $pn);
  } else $pn = $pagename;
  if ($pn == '') return '';
  if (preg_match('/^(.+)[.\\/]([^.\\/]+)$/', $pn, $match)
      && !isset($PCache[$pn]['time']) 
      && (!@$FmtPV[$var] || strpos($FmtPV[$var], '$page') !== false)) 
    PCache($pn, ReadPage($pn, READPAGE_CURRENT));
  @list($d, $group, $name) = $match;
  $page = &$PCache[$pn];
  if (@$FmtPV[$var]) return eval("return ({$FmtPV[$var]});");
  return '';
}
  
## FmtPageName handles $[internationalization] and $Variable 
## substitutions in strings based on the $pagename argument.
function FmtPageName($fmt, $pagename) {
  # Perform $-substitutions on $fmt relative to page given by $pagename
  global $GroupPattern, $NamePattern, $EnablePathInfo, $ScriptUrl,
    $GCount, $UnsafeGlobals, $FmtV, $FmtP, $FmtPV, $PCache, $AsSpacedFunction;
  if (strpos($fmt,'$')===false) return $fmt;                  
  $fmt = preg_replace('/\\$([A-Z]\\w*Fmt)\\b/e','$GLOBALS[\'$1\']',$fmt);
  $fmt = preg_replace('/\\$\\[(?>([^\\]]+))\\]/e',"XL(PSS('$1'))",$fmt);
  $fmt = str_replace('{$ScriptUrl}', '$ScriptUrl', $fmt);
  $fmt = 
    preg_replace('/\\{(\\$[A-Z]\\w+)\\}/e', "PageVar(\$pagename, '$1')", $fmt);
  if (strpos($fmt,'$')===false) return $fmt;
  if ($FmtP) $fmt = preg_replace(array_keys($FmtP), array_values($FmtP), $fmt);
  static $pv, $pvpat;
  if ($pv != count($FmtPV)) {
    $pvpat = str_replace('$', '\\$', implode('|', array_keys($FmtPV)));
    $pv = count($FmtPV);
  }
  $fmt = preg_replace("/(?:$pvpat)\\b/e", "PageVar(\$pagename, '$0')", $fmt);
  $fmt = preg_replace('!\\$ScriptUrl/([^?#\'"\\s<>]+)!e', 
    (@$EnablePathInfo) ? "'$ScriptUrl/'.PUE('$1')" :
        "'$ScriptUrl?n='.str_replace('/','.',PUE('$1'))",
    $fmt);
  if (strpos($fmt,'$')===false) return $fmt;
  static $g;
  if ($GCount != count($GLOBALS)+count($FmtV)) {
    $g = array();
    foreach($GLOBALS as $n=>$v) {
      if (is_array($v) || is_object($v) ||
         isset($FmtV["\$$n"]) || in_array($n,$UnsafeGlobals)) continue;
      $g["\$$n"] = $v;
    }
    $GCount = count($GLOBALS)+count($FmtV);
    krsort($g); reset($g);
  }
  $fmt = str_replace(array_keys($g),array_values($g),$fmt);
  $fmt = preg_replace('/(?>(\\$[[:alpha:]]\\w+))/e', 
          "isset(\$FmtV['$1']) ? \$FmtV['$1'] : '$1'", $fmt); 
  return $fmt;
}

## The XL functions provide translation tables for $[i18n] strings
## in FmtPageName().
function XL($key) {
  global $XL,$XLLangs;
  foreach($XLLangs as $l) if (isset($XL[$l][$key])) return $XL[$l][$key];
  return $key;
}
function XLSDV($lang,$a) {
  global $XL;
  foreach($a as $k=>$v) { if (!isset($XL[$lang][$k])) $XL[$lang][$k]=$v; }
}
function XLPage($lang,$p) {
  global $TimeFmt,$XLLangs,$FarmD;
  $page = ReadPage($p, READPAGE_CURRENT);
  if (!$page) return;
  $text = preg_replace("/=>\\s*\n/",'=> ',@$page['text']);
  foreach(explode("\n",$text) as $l)
    if (preg_match('/^\\s*[\'"](.+?)[\'"]\\s*=>\\s*[\'"](.+)[\'"]/',$l,$match))
      $xl[stripslashes($match[1])] = stripslashes($match[2]);
  if (isset($xl)) {
    if (@$xl['xlpage-i18n']) {
      $i18n = preg_replace('/[^-\\w]/','',$xl['xlpage-i18n']);
      include_once("$FarmD/scripts/xlpage-$i18n.php");
    }
    if (@$xl['Locale']) setlocale(LC_ALL,$xl['Locale']);
    if (@$xl['TimeFmt']) $TimeFmt=$xl['TimeFmt'];
    array_unshift($XLLangs,$lang);
    XLSDV($lang,$xl);
  }
}

## CmpPageAttr is used with uksort to order a page's elements with
## the latest items first.  This can make some operations more efficient.
function CmpPageAttr($a, $b) {
  @list($x, $agmt) = explode(':', $a);
  @list($x, $bgmt) = explode(':', $b);
  if ($agmt != $bgmt) 
    return ($agmt==0 || $bgmt==0) ? $agmt - $bgmt : $bgmt - $agmt;
  return strcmp($a, $b);
}

## class PageStore holds objects that store pages via the native
## filesystem.
class PageStore {
  var $dirfmt;
  var $iswrite;
  function PageStore($d='$WorkDir/$FullName', $w=0) 
    { $this->dirfmt=$d; $this->iswrite=$w; }
  function pagefile($pagename) {
    global $FarmD;
    $dfmt = $this->dirfmt;
    if ($pagename > '') {
      $pagename = str_replace('/', '.', $pagename);
      if ($dfmt == 'wiki.d/{$FullName}')               # optimizations for
        return "wiki.d/$pagename";                     # standard locations
      if ($dfmt == '$FarmD/wikilib.d/{$FullName}')     # 
        return "$FarmD/wikilib.d/$pagename";           #
      if ($dfmt == 'wiki.d/{$Group}/{$FullName}')
        return preg_replace('/([^.]+).*/', 'wiki.d/$1/$0', $pagename);
    }
    return FmtPageName($dfmt, $pagename);
  }
  function read($pagename, $since=0) {
    $newline = '';
    $urlencoded = false;
    $pagefile = $this->pagefile($pagename);
    if ($pagefile && ($fp=@fopen($pagefile, "r"))) {
      while (!feof($fp)) {
        $line = fgets($fp, 4096);
        while (substr($line, -1, 1) != "\n" && !feof($fp)) 
          { $line .= fgets($fp, 4096); }
        $line = rtrim($line);
        if ($urlencoded) $line = urldecode(str_replace('+', '%2b', $line));
        @list($k,$v) = explode('=', $line, 2);
        if (!$k) continue;
        if ($k == 'version') { 
          $ordered = (strpos($v, 'ordered=1') !== false); 
          $urlencoded = (strpos($v, 'urlencoded=1') !== false); 
          if (strpos($v, 'pmwiki-0.')) $newline="\262";
        }
        if ($k == 'newline') { $newline = $v; continue; }
        if ($since > 0 && preg_match('/:(\\d+)/', $k, $m) && $m[1] < $since) {
          if ($ordered) break;
          continue;
        }
        if ($newline) $v = str_replace($newline, "\n", $v);
        $page[$k] = $v;
      }
      fclose($fp);
    }
    return @$page;
  }
  function write($pagename,$page) {
    global $Now, $Version;
    $page['name'] = $pagename;
    $page['time'] = $Now;
    $page['host'] = $_SERVER['REMOTE_ADDR'];
    $page['agent'] = @$_SERVER['HTTP_USER_AGENT'];
    $page['rev'] = @$page['rev']+1;
    unset($page['version']); unset($page['newline']);
    uksort($page, 'CmpPageAttr');
    $s = false;
    $pagefile = $this->pagefile($pagename);
    $dir = dirname($pagefile); mkdirp($dir);
    if (!file_exists("$dir/.htaccess") && $fp = @fopen("$dir/.htaccess", "w")) 
      { fwrite($fp, "Order Deny,Allow\nDeny from all\n"); fclose($fp); }
    if ($pagefile && ($fp=fopen("$pagefile,new","w"))) {
      $r0 = array('%', "\n", '<');
      $r1 = array('%25', '%0a', '%3c');
      $x = "version=$Version ordered=1 urlencoded=1\n";
      $s = true && fputs($fp, $x); $sz = strlen($x);
      foreach($page as $k=>$v) 
        if ($k > '' && $k{0} != '=') {
          $x = str_replace($r0, $r1, "$k=$v") . "\n";
          $s = $s && fputs($fp, $x); $sz += strlen($x);
        }
      $s = fclose($fp) && $s;
      $s = $s && (filesize("$pagefile,new") > $sz * 0.95);
      if (file_exists($pagefile)) $s = $s && unlink($pagefile);
      $s = $s && rename("$pagefile,new", $pagefile);
    }
    $s && fixperms($pagefile);
    if (!$s)
      Abort("Cannot write page to $pagename ($pagefile)...changes not saved");
    PCache($pagename, $page);
  }
  function exists($pagename) {
    if (!$pagename) return false;
    $pagefile = $this->pagefile($pagename);
    return ($pagefile && file_exists($pagefile));
  }
  function delete($pagename) {
    global $Now;
    $pagefile = $this->pagefile($pagename);
    @rename($pagefile,"$pagefile,del-$Now");
  }
  function ls($pats=NULL) {
    global $GroupPattern, $NamePattern;
    StopWatch("PageStore::ls begin {$this->dir}");
    $pats=(array)$pats; 
    array_push($pats, "/^$GroupPattern\.$NamePattern$/");
    $dir = $this->pagefile('$Group.$Name');
    $dirlist = array(preg_replace('!/*[^/]*\\$.*$!','',$dir));
    $out = array();
    while (count($dirlist)>0) {
      $dir = array_shift($dirlist);
      $dfp = opendir($dir); if (!$dfp) { continue; }
      $o = array();
      while ( ($pagefile = readdir($dfp)) !== false) {
        if ($pagefile{0} == '.') continue;
        if (is_dir("$dir/$pagefile"))
          { array_push($dirlist,"$dir/$pagefile"); continue; }
        $o[] = $pagefile;
      }
      closedir($dfp);
      StopWatch("PageStore::ls merge {$this->dir}");
      $out = array_merge($out, MatchPageNames($o, $pats));
    }
    StopWatch("PageStore::ls end {$this->dir}");
    return $out;
  }
}

function ReadPage($pagename, $since=0) {
  # read a page from the appropriate directories given by $WikiReadDirsFmt.
  global $WikiLibDirs,$Now;
  foreach ($WikiLibDirs as $dir) {
    $page = $dir->read($pagename, $since);
    if ($page) break;
  }
  if (@!$page) $page['ctime'] = $Now;
  if (@!$page['time']) $page['time'] = $Now;
  return $page;
}

function WritePage($pagename,$page) {
  global $WikiLibDirs,$WikiDir,$LastModFile;
  $WikiDir->iswrite = 1;
  for($i=0; $i<count($WikiLibDirs); $i++) {
    $wd = &$WikiLibDirs[$i];
    if ($wd->iswrite && $wd->exists($pagename)) break;
  }
  if ($i >= count($WikiLibDirs)) $wd = &$WikiDir;
  $wd->write($pagename,$page);
  if ($LastModFile && !@touch($LastModFile)) 
    { unlink($LastModFile); touch($LastModFile); fixperms($LastModFile); }
}

function PageExists($pagename) {
  global $WikiLibDirs;
  static $pe;
  if (!isset($pe[$pagename])) {
    $pe[$pagename] = false;
    foreach((array)$WikiLibDirs as $dir)
      if ($dir->exists($pagename)) { $pe[$pagename] = true; break; }
  }
  return $pe[$pagename];
}

function ListPages($pat=NULL) {
  global $WikiLibDirs;
  foreach((array)$WikiLibDirs as $dir) 
    $out = array_unique(array_merge($dir->ls($pat),(array)@$out));
  return $out;
}

function RetrieveAuthPage($pagename, $level, $authprompt=true, $since=0) {
  global $AuthFunction;
  SDV($AuthFunction,'PmWikiAuth');
  if (!function_exists($AuthFunction)) return ReadPage($pagename, $since);
  return $AuthFunction($pagename, $level, $authprompt, $since);
}

function Abort($msg) {
  # exit pmwiki with an abort message
  echo "<h3>PmWiki can't process your request</h3>
    <p>$msg</p><p>We are sorry for any inconvenience.</p>";
  exit;
}

function Redirect($pagename,$urlfmt='$PageUrl') {
  # redirect the browser to $pagename
  global $EnableRedirect,$RedirectDelay;
  SDV($RedirectDelay,0);
  clearstatcache();
  #if (!PageExists($pagename)) $pagename=$DefaultPage;
  $pageurl = FmtPageName($urlfmt,$pagename);
  if (IsEnabled($EnableRedirect,1) && 
      (!isset($_REQUEST['redirect']) || $_REQUEST['redirect'])) {
    header("Location: $pageurl");
    header("Content-type: text/html");
    echo "<html><head>
      <meta http-equiv='Refresh' Content='$RedirectDelay; URL=$pageurl' />
     <title>Redirect</title></head><body></body></html>";
  } else echo "<a href='$pageurl'>Redirect to $pageurl</a>";
  exit;
}

function PrintFmt($pagename,$fmt) {
  global $HTTPHeaders,$FmtV;
  if (is_array($fmt)) 
    { foreach($fmt as $f) PrintFmt($pagename,$f); return; }
  if ($fmt == 'headers:') {
    foreach($HTTPHeaders as $h) (@$sent++) ? @header($h) : header($h);
    return;
  }
  $x = FmtPageName($fmt,$pagename);
  if (strncmp($fmt, 'function:', 9) == 0 &&
      preg_match('/^function:(\S+)\s*(.*)$/s', $x, $match) &&
      function_exists($match[1]))
    { $match[1]($pagename,$match[2]); return; }
  if (strncmp($fmt, 'file:', 5) == 0 && preg_match("/^file:(.+)/s",$x,$match)) {
    $filelist = preg_split('/[\\s]+/',$match[1],-1,PREG_SPLIT_NO_EMPTY);
    foreach($filelist as $f) {
      if (file_exists($f)) { include($f); return; }
    }
    return;
  }
  if (preg_match("/^markup:(.*)$/",$x,$match))
    { print MarkupToHTML($pagename,$match[1]); return; }
  if (preg_match('/^wiki:(.+)$/', $x, $match)) 
    { PrintWikiPage($pagename, $match[1], 'read'); return; }
  if (preg_match('/^page:(.+)$/', $x, $match)) 
    { PrintWikiPage($pagename, $match[1], ''); return; }
  echo $x;
}

function PrintWikiPage($pagename, $wikilist=NULL, $auth='read') {
  if (is_null($wikilist)) $wikilist=$pagename;
  $pagelist = preg_split('/\s+/',$wikilist,-1,PREG_SPLIT_NO_EMPTY);
  foreach($pagelist as $p) {
    if (PageExists($p)) {
      $page = ($auth) ? RetrieveAuthPage($p, $auth, false, READPAGE_CURRENT)
              : ReadPage($p, READPAGE_CURRENT);
      if ($page['text']) 
        echo MarkupToHTML($pagename,$page['text']);
      return;
    }
  }
}

function Keep($x, $pool=NULL) {
  # Keep preserves a string from being processed by wiki markups
  global $BlockPattern, $KeepToken, $KPV, $KPCount;
  $x = preg_replace("/$KeepToken(\\d.*?)$KeepToken/e", "\$KPV['\$1']", $x);
  if (is_null($pool) && preg_match("/<($BlockPattern)\\b/", $x)) $pool = 'B';
  $KPCount++; $KPV[$KPCount.$pool]=$x;
  return $KeepToken.$KPCount.$pool.$KeepToken;
}

function CondText($pagename,$condspec,$condtext) {
  global $Conditions;
  if (!preg_match("/^(\\S+)\\s*(!?)\\s*(\\S+)?\\s*(.*?)\\s*$/",
    $condspec,$match)) return '';
  @list($condstr,$condtype,$not,$condname,$condparm) = $match;
  if (isset($Conditions[$condname])) {
    $tf = @eval("return (".$Conditions[$condname].");");
    if (!$tf xor $not) $condtext='';
  }
  return $condtext;
}


function IncludeText($pagename, $inclspec) {
  global $MaxIncludes, $InclCount;
  SDV($MaxIncludes,50);
  $npat = '[[:alpha:]][-\\w]*';
  if ($InclCount++>=$MaxIncludes) return Keep($inclspec);
  $args = array_merge(array('self' => 1), ParseArgs($inclspec));
  while (count($args['#'])>0) {
    $k = array_shift($args['#']); $v = array_shift($args['#']);
    if ($k=='') {
      preg_match('/^([^#\\s]*)(.*)$/', $v, $match);
      if ($match[1]) {                                 # include a page
        if (isset($itext)) continue;
        $iname = MakePageName($pagename, $match[1]);
        if (!$args['self'] && $iname == $pagename) continue;
        if (!PageExists($iname)) continue;
        $ipage = RetrieveAuthPage($iname, 'read', false, READPAGE_CURRENT);
        $itext = @$ipage['text'];
      }
      if (preg_match("/^#($npat)?(\\.\\.)?(#($npat)?)?$/", $match[2], $m)) {
        @list($x, $aa, $dots, $b, $bb) = $m;
        if (!$dots && !$b) $bb = $npat;
        if ($aa)
          $itext=preg_replace("/^.*?\n([^\n]*\\[\\[#$aa\\]\\])/s",
                              '$1', $itext, 1);
        if ($bb)
          $itext=preg_replace("/(\n)[^\n]*\\[\\[#$bb\\]\\].*$/s",
                              '$1', $itext, 1);
      }
      continue;
    }
    if (in_array($k, array('line', 'lines', 'para', 'paras'))) {
      preg_match('/^(\\d*)(\\.\\.(\\d*))?$/', $v, $match);
      @list($x, $a, $dots, $b) = $match;
      $upat = ($k{0} == 'p') ? ".*?(\n\\s*\n|$)" : "[^\n]*(?:\n|$)";
      if (!$dots) { $b=$a; $a=0; }
      if ($a>0) $a--;
      $itext=preg_replace("/^(($upat){0,$b}).*$/s",'$1',$itext,1);
      $itext=preg_replace("/^($upat){0,$a}/s",'',$itext,1);
      continue;
    }
  }
  return PVS(htmlspecialchars(@$itext, ENT_NOQUOTES));
}


function RedirectMarkup($pagename, $opt) {
  $k = Keep("(:redirect $opt:)");
  global $MarkupFrame;
  if (!@$MarkupFrame[0]['redirect']) return $k;
  $opt = ParseArgs($opt);
  $to = @$opt['to']; if (!$to) $to = @$opt[''][0];
  if (!$to) return $k;
  if (preg_match('/^([^#]+)(#[A-Za-z][-\\w]*)$/', $to, $match)) 
    { $to = $match[1]; $anchor = $match[2]; }
  $to = MakePageName($pagename, $to);
  if (!PageExists($to)) return $k;
  if ($to == $pagename) return '';
  if (@$opt['from'] 
      && !MatchPageNames($pagename, FixGlob($opt['from'], '$1*.$2')))
    return '';
  if (preg_match('/^30[1237]$/', @$opt['status'])) 
     header("HTTP/1.1 {$opt['status']}");
  Redirect($to, "{\$PageUrl}?from=$pagename$anchor");
  exit();
}
   

function Block($b) {
  global $BlockMarkups,$HTMLVSpace,$HTMLPNewline,$MarkupFrame;
  $mf = &$MarkupFrame[0]; $cs = &$mf['cs']; $vspaces = &$mf['vs'];
  $out = '';
  if ($b == 'vspace') {
    $vspaces .= "\n";
    while (count($cs)>0 && @end($cs)!='pre' && @$BlockMarkups[@end($cs)][3]==0) 
      { $c = array_pop($cs); $out .= $BlockMarkups[$c][2]; }
    return $out;
  }
  @list($code, $depth, $icol) = explode(',', $b);
  if (!$code) $depth = 1;
  if ($depth == 0) $depth = strlen($depth);
  if ($icol == 0) $icol = strlen($icol);
  if ($depth > 0) $depth += @$mf['idep'];
  if ($icol > 0) $mf['is'][$depth] = $icol + @$mf['icol'];
  @$mf['idep'] = @$mf['icol'] = 0;
  while (count($cs)>$depth) 
    { $c = array_pop($cs); $out .= $BlockMarkups[$c][2]; }
  if (!$code) {
    if (@end($cs) == 'p') { $out .= $HTMLPNewline; $code = 'p'; }
    else if ($depth < 2) { $code = 'p'; $mf['is'][$depth] = 0; }
    else { $out .= $HTMLPNewline; $code = 'block'; }
  }
  if ($depth>0 && $depth==count($cs) && $cs[$depth-1]!=$code)
    { $c = array_pop($cs); $out .= $BlockMarkups[$c][2]; }
  while (count($cs)>0 && @end($cs)!=$code &&
      @$BlockMarkups[@end($cs)][3]==0)
    { $c = array_pop($cs); $out .= $BlockMarkups[$c][2]; }
  if ($vspaces) { 
    $out .= (@end($cs) == 'pre') ? $vspaces : $HTMLVSpace; 
    $vspaces=''; 
  }
  if ($depth==0) { return $out; }
  if ($depth==count($cs)) { return $out.$BlockMarkups[$code][1]; }
  while (count($cs)<$depth-1) { 
    array_push($cs, 'dl'); $mf['is'][count($cs)] = 0;
    $out .= $BlockMarkups['dl'][0].'<dd>'; 
  }
  if (count($cs)<$depth) {
    array_push($cs,$code);
    $out .= $BlockMarkups[$code][0];
  }
  return $out;
}

function FormatTableRow($x) {
  global $Block, $TableCellAttrFmt, $MarkupFrame, $TableRowAttrFmt, 
    $TableRowIndexMax, $FmtV;
  static $rowcount;
  $x = preg_replace('/\\|\\|\\s*$/','',$x);
  $td = explode('||',$x); $y='';
  for($i=0;$i<count($td);$i++) {
    if ($td[$i]=='') continue;
    $FmtV['$TableCellCount'] = $i;
    $attr = FmtPageName($TableCellAttrFmt, '');
    $td[$i] = preg_replace('/^(!?)\\s+$/', '$1&nbsp;', $td[$i]);
    if (preg_match('/^!(.*?)!$/',$td[$i],$match))
      { $td[$i]=$match[1]; $t='caption'; $attr=''; }
    elseif (preg_match('/^!(.*)$/',$td[$i],$match)) 
      { $td[$i]=$match[1]; $t='th'; }
    else $t='td';
    if (preg_match('/^\\s.*\\s$/',$td[$i])) { $attr .= " align='center'"; }
    elseif (preg_match('/^\\s/',$td[$i])) { $attr .= " align='right'"; }
    elseif (preg_match('/\\s$/',$td[$i])) { $attr .= " align='left'"; }
    for ($colspan=1;$i+$colspan<count($td);$colspan++) 
      if ($td[$colspan+$i]!='') break;
    if ($colspan>1) { $attr .= " colspan='$colspan'"; }
    $y .= "<$t $attr>".trim($td[$i])."</$t>";
  }
  if ($t=='caption') return "<:table,1>$y";
  if (@$MarkupFrame[0]['cs'][0] != 'table') $rowcount = 0; else $rowcount++;
  $FmtV['$TableRowCount'] = $rowcount + 1;
  $FmtV['$TableRowIndex'] = ($rowcount % $TableRowIndexMax) + 1;
  $trattr = FmtPageName($TableRowAttrFmt, '');
  return "<:table,1><tr $trattr>$y</tr>";
}

function WikiLink($pagename, $word) {
  global $LinkWikiWords, $SpaceWikiWords, $AsSpacedFunction, 
    $MarkupFrame, $WikiWordCountMax;
  if (!$LinkWikiWords) return $word;
  $text = ($SpaceWikiWords) ? $AsSpacedFunction($word) : $word;
  $text = preg_replace('!.*/!', '', $text);
  if (!isset($MarkupFrame[0]['wwcount'][$word]))
    $MarkupFrame[0]['wwcount'][$word] = $WikiWordCountMax;
  if ($MarkupFrame[0]['wwcount'][$word]-- < 1) return $text;
  return MakeLink($pagename, $word, $text);
}
  
function LinkIMap($pagename,$imap,$path,$title,$txt,$fmt=NULL) {
  global $FmtV, $IMap, $IMapLinkFmt, $UrlLinkFmt;
  $FmtV['$LinkUrl'] = PUE(str_replace('$1',$path,$IMap[$imap]));
  $FmtV['$LinkText'] = $txt;
  $FmtV['$LinkAlt'] = str_replace(array('"',"'"),array('&#34;','&#39;'),$title);
  if (!$fmt) 
    $fmt = (isset($IMapLinkFmt[$imap])) ? $IMapLinkFmt[$imap] : $UrlLinkFmt;
  return str_replace(array_keys($FmtV),array_values($FmtV),$fmt);
}

function LinkPage($pagename,$imap,$path,$title,$txt,$fmt=NULL) {
  global $QueryFragPattern,$LinkPageExistsFmt,$LinkPageSelfFmt,
    $LinkPageCreateSpaceFmt,$LinkPageCreateFmt,$FmtV,$LinkTargets;
  if (!$fmt && $path{0} == '#') {
    $path = preg_replace("/[^-.:\\w]/", '', $path);
    return ($path) ? "<a href='#$path'>$txt</a>" : '';
  }
  if (!preg_match("/^\\s*([^#?]+)($QueryFragPattern)?$/",$path,$match))
    return '';
  $tgtname = MakePageName($pagename, $match[1]); 
  if (!$tgtname) return '';
  $qf = @$match[2];
  @$LinkTargets[$tgtname]++;
  if (!$fmt) {
    if (!PageExists($tgtname) && !preg_match('/[&?]action=/', $qf))
      $fmt = preg_match('/\\s/', $txt) 
             ? $LinkPageCreateSpaceFmt : $LinkPageCreateFmt;
    else
      $fmt = ($tgtname == $pagename && $qf == '') 
             ? $LinkPageSelfFmt : $LinkPageExistsFmt;
  }
  $fmt = str_replace(array('$LinkUrl', '$LinkText'),
           array(PageVar($tgtname, '$PageUrl').PUE($qf), $txt), $fmt);
  return FmtPageName($fmt,$tgtname);
}

function MakeLink($pagename,$tgt,$txt=NULL,$suffix=NULL,$fmt=NULL) {
  global $LinkPattern,$LinkFunctions,$UrlExcludeChars,$ImgExtPattern,$ImgTagFmt;
  $t = preg_replace('/[()]/','',trim($tgt));
  $t = preg_replace('/<[^>]*>/','',$t);
  preg_match("/^($LinkPattern)?(.+?)(\"(.*)\")?$/",$t,$m);
  if (!$m[1]) $m[1]='<:page>';
  if (preg_match("/(($LinkPattern)([^$UrlExcludeChars]+$ImgExtPattern))(\"(.*)\")?$/",$txt,$tm)) 
    $txt = $LinkFunctions[$tm[2]]($pagename,$tm[2],$tm[3],@$tm[5],
      $tm[1],$ImgTagFmt);
  else {
    if (is_null($txt)) {
      $txt = preg_replace('/\\([^)]*\\)/','',$tgt);
      if ($m[1]=='<:page>') $txt = preg_replace('!^.*[^<]/!','',$txt);
      $txt = $txt;
    }
    $txt .= $suffix;
  }
  $out = $LinkFunctions[$m[1]]($pagename,$m[1],$m[2],@$m[4],$txt,$fmt);
  return $out;
}

function Markup($id,$cmd,$pat=NULL,$rep=NULL) {
  global $MarkupTable,$MarkupRules;
  unset($MarkupRules);
  if (preg_match('/^([<>])?(.+)$/',$cmd,$m)) {
    $MarkupTable[$id]['cmd']=$cmd;
    $MarkupTable[$m[2]]['dep'][$id] = $m[1];
    if (!$m[1]) $m[1]='=';
    if (@$MarkupTable[$m[2]]['seq']) {
      $MarkupTable[$id]['seq'] = $MarkupTable[$m[2]]['seq'].$m[1];
      foreach((array)@$MarkupTable[$id]['dep'] as $i=>$m)
        Markup($i,"$m$id");
      unset($MarkupTable[$id]['dep']);
    }
  }
  if ($pat && !isset($MarkupTable[$id]['pat'])) {
    $MarkupTable[$id]['pat']=$pat;
    $MarkupTable[$id]['rep']=$rep;
  }
}

function DisableMarkup() {
  global $MarkupTable;
  $idlist = func_get_args();
  unset($MarkupRules);
  while (count($idlist)>0) {
    $id = array_shift($idlist);
    if (is_array($id)) { $idlist = array_merge($idlist, $id); continue; }
    $MarkupTable[$id] = array('cmd' => 'none', pat=>'');
  }
}
    
function mpcmp($a,$b) { return @strcmp($a['seq'].'=',$b['seq'].'='); }
function BuildMarkupRules() {
  global $MarkupTable,$MarkupRules,$LinkPattern;
  if (!$MarkupRules) {
    uasort($MarkupTable,'mpcmp');
    foreach($MarkupTable as $id=>$m) 
      if (@$m['pat']) 
        $MarkupRules[str_replace('\\L',$LinkPattern,$m['pat'])]=$m['rep'];
  }
  return $MarkupRules;
}


function MarkupToHTML($pagename, $text, $opt = NULL) {
  # convert wiki markup text to HTML output
  global $MarkupRules, $MarkupFrame, $MarkupFrameBase, $WikiWordCount,
    $K0, $K1, $RedoMarkupLine;

  StopWatch('MarkupToHTML begin');
  array_unshift($MarkupFrame, array_merge($MarkupFrameBase, (array)$opt));
  $MarkupFrame[0]['wwcount'] = $WikiWordCount;
  $markrules = BuildMarkupRules();
  foreach((array)$text as $l) 
    $lines[] = $MarkupFrame[0]['escape'] 
               ? PVS(htmlspecialchars($l, ENT_NOQUOTES)) : $l;
  $lines[] = '(:closeall:)';
  $out = '';
  while (count($lines)>0) {
    $x = array_shift($lines);
    $RedoMarkupLine=0;
    foreach($markrules as $p=>$r) {
      if ($p{0} == '/') $x=preg_replace($p,$r,$x); 
      elseif (strstr($x,$p)!==false) $x=eval($r);
      if (isset($php_errormsg)) { echo "pat=$p"; unset($php_errormsg); }
      if ($RedoMarkupLine) { $lines=array_merge((array)$x,$lines); continue 2; }
    }
    if ($x>'') $out .= "$x\n";
  }
  foreach((array)(@$MarkupFrame[0]['posteval']) as $v) eval($v);
  array_shift($MarkupFrame);
  StopWatch('MarkupToHTML end');
  return $out;
}
   
function HandleBrowse($pagename, $auth = 'read') {
  # handle display of a page
  global $DefaultPageTextFmt, $PageNotFoundHeaderFmt, $HTTPHeaders,
    $EnableHTMLCache, $NoHTMLCache, $PageCacheFile, $LastModTime, $IsHTMLCached,
    $FmtV, $HandleBrowseFmt, $PageStartFmt, $PageEndFmt, $PageRedirectFmt;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort('?cannot read $pagename');
  PCache($pagename,$page);
  if (PageExists($pagename)) $text = @$page['text'];
  else {
    SDV($DefaultPageTextFmt,'(:include $[{$SiteGroup}.PageNotFound]:)');
    $text = FmtPageName($DefaultPageTextFmt, $pagename);
    SDV($PageNotFoundHeaderFmt, 'HTTP/1.1 404 Not Found');
    SDV($HTTPHeaders['status'], $PageNotFoundHeaderFmt);
  }
  $opt = array();
  SDV($PageRedirectFmt,"<p><i>($[redirected from] <a rel='nofollow' 
    href='{\$PageUrl}?action=edit'>{\$FullName}</a>)</i></p>\$HTMLVSpace\n");
  if (@!$_GET['from']) { $opt['redirect'] = 1; $PageRedirectFmt = ''; }
  else $PageRedirectFmt = FmtPageName($PageRedirectFmt, $_GET['from']);
  if (@$EnableHTMLCache && !$NoHTMLCache && $PageCacheFile && 
      @filemtime($PageCacheFile) > $LastModTime) {
    list($ctext) = unserialize(file_get_contents($PageCacheFile));
    $FmtV['$PageText'] = "<!--cached-->$ctext";
    $IsHTMLCached = 1;
  } else {
    $IsHTMLCached = 0;
    $text = '(:groupheader:)'.@$text.'(:groupfooter:)';
    $t1 = time();
    $FmtV['$PageText'] = MarkupToHTML($pagename, $text, $opt);
    if (@$EnableHTMLCache > 0 && !$NoHTMLCache && $PageCacheFile
        && (time() - $t1 + 1) >= $EnableHTMLCache) {
      $fp = @fopen("$PageCacheFile,new", "x");
      if ($fp) { 
        fwrite($fp, serialize(array($FmtV['$PageText']))); fclose($fp);
        rename("$PageCacheFile,new", $PageCacheFile);
      }
    }
  }
  SDV($HandleBrowseFmt,array(&$PageStartFmt, &$PageRedirectFmt, '$PageText',
    &$PageEndFmt));
  PrintFmt($pagename,$HandleBrowseFmt);
}

# EditTemplate allows a site administrator to pre-populate new pages
# with the contents of another page.
function EditTemplate($pagename, &$page, &$new) {
  global $EditTemplatesFmt;
  if (@$new['text'] > '') return;
  if (@$_REQUEST['template'] && PageExists($_REQUEST['template'])) {
    $p = RetrieveAuthPage($_REQUEST['template'], 'read', false,
             READPAGE_CURRENT);
    if ($p['text'] > '') $new['text'] = $p['text']; 
    return;
  }
  foreach((array)$EditTemplatesFmt as $t) {
    $p = RetrieveAuthPage(FmtPageName($t,$pagename), 'read', false,
             READPAGE_CURRENT);
    if (@$p['text'] > '') { $new['text'] = $p['text']; return; }
  }
}

# RestorePage handles returning to the version of text as of
# the version given by $restore or $_REQUEST['restore'].
function RestorePage($pagename,&$page,&$new,$restore=NULL) {
  if (is_null($restore)) $restore=@$_REQUEST['restore'];
  if (!$restore) return;
  $t = $page['text'];
  $nl = (substr($t,-1)=="\n");
  $t = explode("\n",$t);
  if ($nl) array_pop($t);
  krsort($page); reset($page);
  foreach($page as $k=>$v) {
    if ($k<$restore) break;
    if (strncmp($k, 'diff:', 5) != 0) continue;
    foreach(explode("\n",$v) as $x) {
      if (preg_match('/^(\\d+)(,(\\d+))?([adc])(\\d+)/',$x,$match)) {
        $a1 = $a2 = $match[1];
        if ($match[3]) $a2=$match[3];
        $b1 = $match[5];
        if ($match[4]=='d') array_splice($t,$b1,$a2-$a1+1);
        if ($match[4]=='c') array_splice($t,$b1-1,$a2-$a1+1);
        continue;
      }
      if (strncmp($x,'< ',2) == 0) { $nlflag=true; continue; }
      if (preg_match('/^> (.*)$/',$x,$match)) {
        $nlflag=false;
        array_splice($t,$b1-1,0,$match[1]); $b1++;
      }
      if ($x=='\\ No newline at end of file') $nl=$nlflag;
    }
  }
  if ($nl) $t[]='';
  $new['text']=implode("\n",$t);
  return $new['text'];
}

## ReplaceOnSave performs any text replacements (held in $ROSPatterns)
## on the new text prior to saving the page.
function ReplaceOnSave($pagename,&$page,&$new) {
  global $EnablePost, $ROSPatterns;
  if (!$EnablePost) return;
  foreach((array)$ROSPatterns as $pat=>$repfmt) 
    $new['text'] = 
      preg_replace($pat,FmtPageName($repfmt,$pagename),$new['text']);
}

function SaveAttributes($pagename,&$page,&$new) {
  global $EnablePost, $LinkTargets, $SaveAttrPatterns, $PCache,
    $SaveProperties;
  if (!$EnablePost) return;
  $text = preg_replace(array_keys($SaveAttrPatterns), 
                       array_values($SaveAttrPatterns), $new['text']);
  $html = MarkupToHTML($pagename,$text);
  $new['targets'] = implode(',',array_keys((array)$LinkTargets));
  $p = & $PCache[$pagename];
  foreach((array)$SaveProperties as $k) {
    if (@$p["=p_$k"]) $new[$k] = $p["=p_$k"];
    else unset($new[$k]);
  }
  unset($new['excerpt']);
}

function PostPage($pagename, &$page, &$new) {
  global $DiffKeepDays, $DiffFunction, $DeleteKeyPattern, $EnablePost,
    $Now, $Author, $WikiDir, $IsPagePosted;
  SDV($DiffKeepDays,3650);
  SDV($DeleteKeyPattern,"^\\s*delete\\s*$");
  $IsPagePosted = false;
  if ($EnablePost) {
    $new["author"]=@$Author;
    $new["author:$Now"] = @$Author;
    $new["host:$Now"] = $_SERVER['REMOTE_ADDR'];
    $diffclass = preg_replace('/\\W/','',@$_POST['diffclass']);
    if ($page["time"]>0 && function_exists(@$DiffFunction)) 
      $new["diff:$Now:{$page['time']}:$diffclass"] =
        $DiffFunction($new['text'],@$page['text']);
    $keepgmt = $Now-$DiffKeepDays * 86400;
    $keys = array_keys($new);
    foreach($keys as $k)
      if (preg_match("/^\\w+:(\\d+)/",$k,$match) && $match[1]<$keepgmt)
        unset($new[$k]);
    if (preg_match("/$DeleteKeyPattern/",$new['text']))
      $WikiDir->delete($pagename);
    else WritePage($pagename,$new);
    $IsPagePosted = true;
  }
}

function PostRecentChanges($pagename,&$page,&$new) {
  global $IsPagePosted, $RecentChangesFmt, $RCDelimPattern, $RCLinesMax;
  if (!$IsPagePosted) return;
  foreach($RecentChangesFmt as $rcfmt=>$pgfmt) {
    $rcname = FmtPageName($rcfmt,$pagename);  if (!$rcname) continue;
    $pgtext = FmtPageName($pgfmt,$pagename);  if (!$pgtext) continue;
    if (@$seen[$rcname]++) continue;
    $rcpage = ReadPage($rcname);
    $rcelim = preg_quote(preg_replace("/$RCDelimPattern.*$/",' ',$pgtext),'/');
    $rcpage['text'] = preg_replace("/[^\n]*$rcelim.*\n/","",@$rcpage['text']);
    if (!preg_match("/$RCDelimPattern/",$rcpage['text'])) 
      $rcpage['text'] .= "$pgtext\n";
    else
      $rcpage['text'] = preg_replace("/([^\n]*$RCDelimPattern.*\n)/",
        "$pgtext\n$1", $rcpage['text'], 1);
    if (@$RCLinesMax > 0) 
      $rcpage['text'] = implode("\n", array_slice(
          explode("\n", $rcpage['text'], $RCLinesMax + 1), 0, $RCLinesMax));
    WritePage($rcname, $rcpage);
  }
}

function PreviewPage($pagename,&$page,&$new) {
  global $IsPageSaved, $FmtV;
  if (@$_POST['preview']) {
    $text = '(:groupheader:)'.$new['text'].'(:groupfooter:)';
    $FmtV['$PreviewText'] = MarkupToHTML($pagename,$text);
  } 
}
  
function HandleEdit($pagename, $auth = 'edit') {
  global $IsPagePosted, $EditFields, $ChangeSummary, $EditFunctions, 
    $EnablePost, $FmtV, $Now, $EditRedirectFmt, 
    $PageEditForm, $HandleEditFmt, $PageStartFmt, $PageEditFmt, $PageEndFmt;
  SDV($EditRedirectFmt, '$FullName');
  if (@$_POST['cancel']) 
    { Redirect(FmtPageName($EditRedirectFmt, $pagename)); return; }
  Lock(2);
  $IsPagePosted = false;
  $page = RetrieveAuthPage($pagename, $auth, true);
  if (!$page) Abort("?cannot edit $pagename"); 
  PCache($pagename,$page);
  $new = $page;
  foreach((array)$EditFields as $k) 
    if (isset($_POST[$k])) $new[$k]=str_replace("\r",'',stripmagic($_POST[$k]));
  $new['csum'] = $ChangeSummary;
  if ($ChangeSummary) $new["csum:$Now"] = $ChangeSummary;
  $EnablePost &= preg_grep('/^post/', array_keys(@$_POST));
  foreach((array)$EditFunctions as $fn) $fn($pagename,$page,$new);
  Lock(0);
  if ($IsPagePosted && !@$_POST['postedit']) 
    { Redirect(FmtPageName($EditRedirectFmt, $pagename)); return; }
  $FmtV['$DiffClassMinor'] = 
    (@$_POST['diffclass']=='minor') ?  "checked='checked'" : '';
  $FmtV['$EditText'] = 
    str_replace('$','&#036;',htmlspecialchars(@$new['text'],ENT_NOQUOTES));
  $FmtV['$EditBaseTime'] = $Now;
  if (@$PageEditForm) {
    $form = ReadPage(FmtPageName($PageEditForm, $pagename), READPAGE_CURRENT);
    $FmtV['$EditForm'] = MarkupToHTML($pagename, $form['text']);
  }
  SDV($PageEditFmt, "<div id='wikiedit'>
    <h2 class='wikiaction'>$[Editing {\$FullName}]</h2>
    <form method='post' rel='nofollow' action='\$PageUrl?action=edit'>
    <input type='hidden' name='action' value='edit' />
    <input type='hidden' name='n' value='\$FullName' />
    <input type='hidden' name='basetime' value='\$EditBaseTime' />
    \$EditMessageFmt
    <textarea id='text' name='text' rows='25' cols='60'
      onkeydown='if (event.keyCode==27) event.returnValue=false;'
      >\$EditText</textarea><br />
    <input type='submit' name='post' value=' $[Save] ' />");
  SDV($HandleEditFmt, array(&$PageStartFmt, &$PageEditFmt, &$PageEndFmt));
  PrintFmt($pagename, $HandleEditFmt);
}

function HandleSource($pagename, $auth = 'read') {
  global $HTTPHeaders;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort("?cannot source $pagename");
  foreach ($HTTPHeaders as $h) {
    $h = preg_replace('!^Content-type:\\s+text/html!i',
             'Content-type: text/plain', $h);
    header($h);
  }
  echo @$page['text'];
}

## PmWikiAuth provides password-protection of pages using PHP sessions.
## It is normally called from RetrieveAuthPage.  Since RetrieveAuthPage
## can be called a lot within a single page execution (i.e., for every
## page accessed), we cache the results of site passwords and 
## GroupAttribute pages to be able to speed up subsequent calls.
function PmWikiAuth($pagename, $level, $authprompt=true, $since=0) {
  global $DefaultPasswords, $GroupAttributesFmt, $AllowPassword,
    $AuthCascade, $FmtV, $AuthPromptFmt, $PageStartFmt, $PageEndFmt, 
    $AuthId, $AuthList, $NoHTMLCache;
  static $acache;
  SDV($GroupAttributesFmt,'$Group/GroupAttributes');
  SDV($AllowPassword,'nopass');
  $page = ReadPage($pagename, $since);
  if (!$page) { return false; }
  if (!isset($acache)) 
    SessionAuth($pagename, (@$_POST['authpw']) 
                           ? array('authpw' => array($_POST['authpw'] => 1))
                           : '');
  if (@$AuthId) {
    $AuthList["id:$AuthId"] = 1;
    $AuthList["id:-$AuthId"] = -1;
    $AuthList["id:*"] = 1;
  }
  $gn = FmtPageName($GroupAttributesFmt, $pagename);
  if (!isset($acache[$gn])) $gp = ReadPage($gn, READPAGE_CURRENT);
  foreach($DefaultPasswords as $k => $v) {
    if (isset($gp)) {
      $x = array(2, array(), '');
      $acache['@site'][$k] = IsAuthorized($v, 'site', $x);
      $AuthList["@_site_$k"] = $acache['@site'][$k][0] ? 1 : 0;
      $acache[$gn][$k] = IsAuthorized($gp["passwd$k"], 'group', 
                                      $acache['@site'][$k]);
    }
    list($page['=auth'][$k], $page['=passwd'][$k], $page['=pwsource'][$k]) =
      IsAuthorized($page["passwd$k"], 'page', $acache[$gn][$k]);
  }
  foreach($AuthCascade as $k => $t) {
    if ($page['=auth'][$k]+0 == 2) {
      $page['=auth'][$k] = $page['=auth'][$t];
      if ($page['=passwd'][$k] = $page['=passwd'][$t])         # assign
        $page['=pwsource'][$k] = "cascade:$t";
    }
  }
  if (@$page['=auth']['admin']) 
    foreach($page['=auth'] as $lv=>$a) @$page['=auth'][$lv] = 3;
  if (@$page['=passwd']['read']) $NoHTMLCache |= 2;
  if (@$page['=auth'][$level]) return $page;
  if (!$authprompt) return false;
  $GLOBALS['AuthNeeded'] = (@$_POST['authpw']) 
    ? $page['=pwsource'][$level] . ' ' . $level : '';
  PCache($pagename, $page);
  $postvars = '';
  foreach($_POST as $k=>$v) {
    if ($k == 'authpw' || $k == 'authid') continue;
    $v = str_replace('$', '&#036;', 
             htmlspecialchars(stripmagic($v), ENT_COMPAT));
    $postvars .= "<input type='hidden' name='$k' value=\"$v\" />\n";
  }
  $FmtV['$PostVars'] = $postvars;
  SDV($AuthPromptFmt,array(&$PageStartFmt,
    "<p><b>$[Password required]</b></p>
      <form name='authform' action='{$_SERVER['REQUEST_URI']}' method='post'>
        $[Password]: <input tabindex='1' type='password' name='authpw' 
          value='' />
        <input type='submit' value='OK' />\$PostVars</form>
        <script language='javascript' type='text/javascript'><!--
          document.authform.authpw.focus() //--></script>", &$PageEndFmt));
  PrintFmt($pagename,$AuthPromptFmt);
  exit;
}

function IsAuthorized($chal, $source, &$from) {
  global $AuthList, $AuthPw, $AllowPassword;
  if (!$chal) return $from;
  $auth = 0; 
  $passwd = array();
  foreach((array)$chal as $c) {
    $x = '';
    $pwchal = preg_split('/([, ]|\\w+:)/', $c, -1, PREG_SPLIT_DELIM_CAPTURE);
    foreach($pwchal as $pw) {
      if ($pw == ',') continue;
      else if ($pw == ' ') { $x = ''; continue; }
      else if (substr($pw, -1, 1) == ':') { $x = $pw; continue; }
      else if ($pw{0} != '@' && $x > '') $pw = $x . $pw;
      if (!$pw) continue;
      $passwd[] = $pw;
      if ($auth < 0) continue;
      if ($x || $pw{0} == '@') {
        if (@$AuthList[$pw]) $auth = $AuthList[$pw];
        continue;
      }
      if (crypt($AllowPassword, $pw) == $pw)           # nopass
        { $auth=1; continue; }
      foreach((array)$AuthPw as $pwresp)                       # password
        if (crypt($pwresp, $pw) == $pw) { $auth=1; continue; }
    }
  }
  if (!$passwd) return $from;
  if ($auth < 0) $auth = 0;
  return array($auth, $passwd, $source);
}


## SessionAuth works with PmWikiAuth to manage authorizations
## as stored in sessions.  First, it can be used to set session
## variables by calling it with an $auth argument.  It then
## uses the authid, authpw, and authlist session variables
## to set the corresponding values of $AuthId, $AuthPw, and $AuthList
## as needed.
function SessionAuth($pagename, $auth = NULL) {
  global $AuthId, $AuthList, $AuthPw;
  static $called;

  @$called++;
  if (!$auth && ($called > 1 || !@$_REQUEST[session_name()])) return;

  $sid = session_id();
  @session_start();
  foreach((array)$auth as $k => $v)
    if ($k) $_SESSION[$k] = array_merge((array)$_SESSION[$k], (array)$v);

  if (!isset($AuthId)) $AuthId = @end($_SESSION['authid']);
  $AuthPw = array_keys((array)@$_SESSION['authpw']);
  $AuthList = array_merge($AuthList, (array)@$_SESSION['authlist']);
  if (!$sid) session_write_close();
}


function PrintAttrForm($pagename) {
  global $PageAttributes, $PCache, $FmtV;
  echo FmtPageName("<form action='\$PageUrl' method='post'>
    <input type='hidden' name='action' value='postattr' />
    <input type='hidden' name='n' value='\$FullName' />
    <table>",$pagename);
  $page = $PCache[$pagename];
  foreach($PageAttributes as $attr=>$p) {
    if (!$p) continue;
    $setting = @$page[$attr];
    $value = @$page[$attr];
    if (strncmp($attr, 'passwd', 6) == 0) {
      $a = substr($attr, 6);
      $value = '';
      $setting = implode(' ', 
        preg_replace('/^(?!@|\\w+:).+$/', '****', (array)$page['=passwd'][$a]));
      $pwsource = $page['=pwsource'][$a];
      $FmtV['$PWSource'] = $pwsource;
      $FmtV['$PWCascade'] = substr($pwsource, 8);
      if ($pwsource == 'group' || $pwsource == 'site')
        $setting = FmtPageName('$[(set by $PWSource)]', $pagename)." $setting";
      if (strncmp($pwsource, 'cascade:', 8) == 0)
        $setting = FmtPageName('$[(using $PWCascade password)]', $pagename);
     }
    $prompt = FmtPageName($p,$pagename);
    echo "<tr><td>$prompt</td>
      <td><input type='text' name='$attr' value='$value' /></td>
      <td>$setting</td></tr>";
  }
  echo FmtPageName("</table><input type='submit' value='$[Save]' /></form>",
         $pagename);
}

function HandleAttr($pagename, $auth = 'attr') {
  global $PageAttrFmt,$PageStartFmt,$PageEndFmt;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) { Abort("?unable to read $pagename"); }
  PCache($pagename,$page);
  XLSDV('en', array('EnterAttributes' =>
    "Enter new attributes for this page below.  Leaving a field blank
    will leave the attribute unchanged.  To clear an attribute, enter
    'clear'."));
  SDV($PageAttrFmt,"<div class='wikiattr'>
    <h2 class='wikiaction'>$[{\$FullName} Attributes]</h2>
    <p>$[EnterAttributes]</p></div>");
  SDV($HandleAttrFmt,array(&$PageStartFmt,&$PageAttrFmt,
    'function:PrintAttrForm',&$PageEndFmt));
  PrintFmt($pagename,$HandleAttrFmt);
}

function HandlePostAttr($pagename, $auth = 'attr') {
  global $PageAttributes, $EnablePostAttrClearSession;
  Lock(2);
  $page = RetrieveAuthPage($pagename, $auth, true);
  if (!$page) { Abort("?unable to read $pagename"); }
  foreach($PageAttributes as $attr=>$p) {
    $v = stripmagic(@$_POST[$attr]);
    if ($v == '') continue;
    if ($v=='clear') unset($page[$attr]);
    else if (strncmp($attr, 'passwd', 6) != 0) $page[$attr] = $v;
    else {
      $a = array();
      preg_match_all('/"[^"]*"|\'[^\']*\'|\\S+/', $v, $match);
      foreach($match[0] as $pw) 
        $a[] = preg_match('/^(@|\\w+:)/', $pw) ? $pw 
                   : crypt(preg_replace('/^([\'"])(.*)\\1$/', '$2', $pw));
      if ($a) $page[$attr] = implode(' ',$a);
    }
  }
  WritePage($pagename,$page);
  Lock(0);
  if (IsEnabled($EnablePostAttrClearSession, 1)) {
    @session_start();
    unset($_SESSION['authid']);
    unset($_SESSION['authlist']);
    $_SESSION['authpw'] = array();
  }
  Redirect($pagename);
  exit;
} 


function HandleLogoutA($pagename, $auth = 'read') {
  global $LogoutRedirectFmt, $LogoutCookies;
  SDV($LogoutRedirectFmt, '$FullName');
  SDV($LogoutCookies, array());
  @session_start();
  $_SESSION = array();
  if (isset($_COOKIE[session_name()]))
    setcookie(session_name(), '', time()-43200, '/');
  foreach ($LogoutCookies as $c)
    if (isset($_COOKIE[$c])) setcookie($c, '', time()-43200, '/');
  session_destroy();
  Redirect(FmtPageName($LogoutRedirectFmt, $pagename));
}


function HandleLoginA($pagename, $auth = 'login') {
  global $AuthId, $DefaultPasswords;
  unset($DefaultPasswords['admin']);
  $prompt = @(!$_POST['authpw'] || ($AuthId != $_POST['authid']));
  $page = RetrieveAuthPage($pagename, $auth, $prompt, READPAGE_CURRENT);
  Redirect($pagename);
}

