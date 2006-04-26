<?php if (!defined('PmWiki')) exit();
/*  Copyright 2005-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    The APR compatible MD5 encryption algorithm in _crypt() below is 
    based on code Copyright 2005 by D. Faure and the File::Passwd
    PEAR library module by Mike Wallner <mike@php.net>.

    This script enables simple authentication based on username and 
    password combinations.  At present this script can authenticate
    from passwords held in arrays or in .htpasswd-formatted files,
    but eventually it will support authentication via sources such
    as LDAP and Active Directory.

    To configure a .htpasswd-formatted file for authentication, do
        $AuthUser['htpasswd'] = '/path/to/.htpasswd';
    prior to including this script.  

    Individual username/password combinations can also be placed
    directly in the $AuthUser array, such as:
        $AuthUser['pmichaud'] = crypt('secret');

    To authenticate against an LDAP server, put the url for
    the server in $AuthUser['ldap'], as in:
        $AuthUser['ldap'] = 'ldap://ldap.example.com/ou=People,o=example?uid';
*/

# let Site.AuthForm know that we're doing user-based authorization
$EnableAuthUser = 1;

if (@$_POST['authid']) 
  AuthUserId($pagename, stripmagic(@$_POST['authid']), 
             stripmagic(@$_POST['authpw']));
else SessionAuth($pagename);

function AuthUserId($pagename, $id, $pw=NULL) {
  global $AuthUser, $AuthUserPageFmt, $AuthUserFunctions, 
    $AuthId, $MessagesFmt;
  $auth = $AuthUser; $authid = '';

  # load information from Site.AuthUser (or page in $AuthUserPageFmt)
  SDV($AuthUserPageFmt, '$SiteGroup.AuthUser');
  SDVA($AuthUserFunctions, array(
    'htpasswd' => 'AuthUserHtPasswd',
    'ldap' => 'AuthUserLDAP',
    'yabb' => 'AuthUserYabb',
#    'mysql' => 'AuthUserMySQL',
    $id => 'AuthUserConfig'));

  $pn = FmtPageName($AuthUserPageFmt, $pagename);
  $apage = ReadPage($pn, READPAGE_CURRENT);
  if ($apage && preg_match_all("/^\\s*([@\\w][^\\s:]*):(.*)/m", 
                               $apage['text'], $matches, PREG_SET_ORDER)) {
    foreach($matches as $m) {
      if (!preg_match_all('/\\bldap:\\S+|[^\\s,]+/', $m[2], $v))
        continue;
      if ($m[1]{0} == '@') 
        foreach($v[0] as $g) $auth[$g][] = $m[1];
      else $auth[$m[1]] = array_merge((array)@$auth[$m[1]], $v[0]);
    }
  }

  if (is_null($pw)) $authid = $id;
  else 
    foreach($AuthUserFunctions as $k => $fn) 
      if ($auth[$k] && $fn($pagename, $id, $pw, $auth[$k])) 
        { $authid = $id; break; }

  if (!$authid) { $GLOBALS['InvalidLogin'] = 1; return; }
  if (!isset($AuthId)) $AuthId = $authid;
  $authlist["id:$authid"] = 1;
  $authlist["id:-$authid"] = -1;
  foreach(preg_grep('/^@/', (array)@$auth[$authid]) as $g) 
    $authlist[$g] = 1;
  foreach(preg_grep('/^@/', (array)@$auth['*']) as $g) 
    $authlist[$g] = 1;
  SessionAuth($pagename, array('authid' => $authid, 'authlist' => $authlist));
}

function AuthUserConfig($pagename, $id, $pw, $pwlist) {
  foreach ((array)$pwlist as $chal) 
    if (_crypt($pw, $chal) == $chal) return true;
  return false;
}

function AuthUserYabb($pagename, $id, $pw, $pwlist) {
  $filename = $pwlist .'/'. $id .'.dat';
  if (file_exists($filename)) {
    $fp = fopen($filename, "r");
    if ($fp) {
      $storedpw = trim(fgets($fp));
      if ($pw == $storedpw) {
        return true;
      }
    }
  }
  return false;
}

function AuthUserHtPasswd($pagename, $id, $pw, $pwlist) {
  foreach ((array)$pwlist as $f) {
    $fp = fopen($f, "r"); if (!$fp) continue;
    while ($x = fgets($fp, 1024)) {
      $x = rtrim($x);
      list($i, $c, $r) = explode(':', $x, 3);
      if ($i == $id && _crypt($pw, $c) == $c) { fclose($fp); return true; }
    }
    fclose($fp);
  }
  return false;
}


function AuthUserLDAP($pagename, $id, $pw, $pwlist) {
  global $AuthLDAPBindDN, $AuthLDAPBindPassword;
  if (!$pw) return false;
  if (!function_exists('ldap_connect')) return false;
  foreach ((array)$pwlist as $ldap) {
    if (!preg_match('!ldap://([^:]+)(?::(\\d+))?/(.+)$!', $ldap, $match))
      continue;
    list($z, $server, $port, $path) = $match;
    list($basedn, $attr, $sub) = explode('?', $path);
    if (!$port) $port = 389;
    if (!$attr) $attr = 'uid';
    if (!$sub) $sub = 'one';
    $binddn = @$AuthLDAPBindDN;
    $bindpw = @$AuthLDAPBindPassword;
    $ds = ldap_connect($server, $port);
    ldap_set_option($ds, LDAP_OPT_PROTOCOL_VERSION, 3);
    if (ldap_bind($ds, $binddn, $bindpw)) {
      $fn = ($sub == 'sub') ? 'ldap_search' : 'ldap_list';
      $sr = $fn($ds, $basedn, "($attr=$id)", array($attr));
      $x = ldap_get_entries($ds, $sr);
      if ($x['count'] == 1) {
        $dn = $x[0]['dn'];
        if (ldap_bind($ds, $dn, $pw)) { ldap_close($ds); return true; }
      }
    }
    ldap_close($ds);
  }
  return false;
}


#  The _crypt function provides support for SHA1 encrypted passwords 
#  (keyed by '{SHA}') and Apache MD5 encrypted passwords (keyed by 
#  '$apr1$'); otherwise it just calls PHP's crypt() for the rest.
#  The APR MD5 encryption code was contributed by D. Faure.

function _crypt($plain, $salt=null) {
  if (strncmp($salt, '{SHA}', 5) == 0) 
    return '{SHA}'.base64_encode(pack('H*', sha1($plain)));
  if (strncmp($salt, '$apr1$', 6) == 0) {
    preg_match('/^\\$apr1\\$([^$]+)/', $salt, $match);
    $salt = $match[1];
    $length = strlen($plain);
    $context = $plain . '$apr1$' . $salt;
    $binary = pack('H32', md5($plain . $salt . $plain));
    for($i = $length; $i > 0; $i -= 16) 
      $context .= substr($binary, 0, min(16, $i));
    for($i = $length; $i > 0; $i >>= 1)
      $context .= ($i & 1) ? chr(0) : $plain{0};
    $binary = pack('H32', md5($context));
    for($i = 0; $i < 1000; $i++) {
      $new = ($i & 1) ? $plain : $binary;
      if ($i % 3) $new .= $salt;
      if ($i % 7) $new .= $plain;
      $new .= ($i & 1) ? $binary : $plain;
      $binary = pack('H32', md5($new));
    }
    $q = '';
    for ($i = 0; $i < 5; $i++) {
      $k = $i + 6;
      $j = $i + 12;
      if ($j == 16) $j = 5;
      $q = $binary{$i}.$binary{$k}.$binary{$j} . $q;
    }
    $q = chr(0).chr(0).$binary{11} . $q;
    $q = strtr(strrev(substr(base64_encode($q), 2)),
           'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/',
           './0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz');
    return "\$apr1\$$salt\$$q";
  }
  if (md5($plain) == $salt) return $salt;
  return crypt($plain, $salt);
}
