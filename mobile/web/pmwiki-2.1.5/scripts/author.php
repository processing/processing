<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script handles author tracking.
*/

SDV($AuthorNameChars, "- '\\w\\x80-\\xff");
SDV($AuthorCookie, $CookiePrefix.'author');
SDV($AuthorCookieExpires,$Now+60*60*24*30);
SDV($AuthorCookieDir,'/');
SDV($AuthorGroup,'Profiles');
SDV($AuthorRequiredFmt,
  "<h3 class='wikimessage'>$[An author name is required.]</h3>");
Markup('[[~','<[[','/\\[\\[~(.*?)\\]\\]/',"[[$AuthorGroup/$1]]");

$LogoutCookies[] = $AuthorCookie;

if (!isset($Author)) {
  if (isset($_POST['author'])) {
    $x = stripmagic($_POST['author']);
    setcookie($AuthorCookie, $x, $AuthorCookieExpires, $AuthorCookieDir);
  } elseif (@$_COOKIE[$AuthorCookie]) {
    $x = stripmagic(@$_COOKIE[$AuthorCookie]);
  } else $x = @$AuthId;
  $Author = htmlspecialchars(preg_replace("/[^$AuthorNameChars]/", '', $x), 
                ENT_QUOTES);
}
if (!isset($AuthorPage)) $AuthorPage = 
    FmtPageName('$AuthorGroup/$Name', MakePageName($pagename, $Author));
SDV($AuthorLink,($Author) ? "[[~$Author]]" : '?');

if (IsEnabled($EnableAuthorSignature,1)) {
  $ROSPatterns['/(?<!~)~~~~(?!~)/'] = '[[~$Author]] $CurrentTime';
  $ROSPatterns['/(?<!~)~~~(?!~)/'] = '[[~$Author]]';
  Markup('~~~~','<links','/(?<!~)~~~~(?!~)/',"[[~$Author]] $CurrentTime");
  Markup('~~~','>~~~~','/(?<!~)~~~(?!~)/',"[[~$Author]]");
}
if (IsEnabled($EnablePostAuthorRequired,0))
  array_unshift($EditFunctions,'RequireAuthor');

## RequireAuthor forces an author to enter a name before posting.
function RequireAuthor($pagename, &$page, &$new) {
  global $Author, $MessagesFmt, $AuthorRequiredFmt, $EnablePost;
  if (!$Author) {
    $MessagesFmt[] = $AuthorRequiredFmt;
    $EnablePost = 0;
  }
}
