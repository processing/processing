<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2005 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file defines an alternate authentication scheme based on the
    HTTP Basic authentication protocol (i.e., the scheme used by default
    in PmWiki 1).
*/

## If the webserver has already authenticated someone, then use
## that identifier for our authorization id.  We also disable
## the use of the browser's Basic Auth form later, since it tends
## to confuse webservers.
if (IsEnabled($EnableRemoteUserAuth, 1) && @$_SERVER['REMOTE_USER']) {
  SDV($EnableHTTPBasicAuth, 0);
  SDV($AuthId, $_SERVER['REMOTE_USER']);
}

## If the browser supplied a password, add that password to the
## list of passwords used for authentication
if (@$_SERVER['PHP_AUTH_PW']) {
  @session_start();
  @$_SESSION['authpw'][$_SERVER['PHP_AUTH_PW']]++;
  $_REQUEST[session_name()] = 1;
}


## $EnableHTTPBasicAuth tells PmWikiAuth to use the browser's
## HTTP Basic protocol prompt instead of a form-based prompt.
if (IsEnabled($EnableHTTPBasicAuth, 1)) 
  SDV($AuthPromptFmt, 'function:HTTPBasicAuthPrompt');

## HTTPBasicAuthPrompt replaces PmWikiAuth's form-based password
## prompt with the browser-based HTTP Basic prompt.
function HTTPBasicAuthPrompt($pagename) {
  global $AuthRealmFmt, $AuthDeniedFmt;
  SDV($AuthRealmFmt,$GLOBALS['WikiTitle']);
  SDV($AuthDeniedFmt,'A valid password is required to access this feature.');
  $realm=FmtPageName($AuthRealmFmt,$pagename);
  header("WWW-Authenticate: Basic realm=\"$realm\"");
  header("Status: 401 Unauthorized");
  header("HTTP-Status: 401 Unauthorized");
  PrintFmt($pagename,$AuthDeniedFmt);
  exit;
}

