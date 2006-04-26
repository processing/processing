<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script provides a URL-approval capability.  To enable this
    script, add the following line to a configuration file:

        include_once('scripts/urlapprove.php');

    The URL prefixes to be allowed are stored as patterns in 
    $WhiteUrlPatterns.  This array can be loaded from config.php, or 
    from the wiki pages given by the $ApprovedUrlPagesFmt[] array.  
    Any http: or https: URL that isn't in $WhiteUrlPatterns is rendered 
    using $UnapprovedLinkFmt.

    The script also provides ?action=approveurls and ?action=approvesites, 
    which scan the current page for any new URLs to be automatically added
    the first page of $UrlApprovalPagesFmt.

    Finally, the script will block any post containing more than
    $UnapprovedLinkCountMax unapproved urls in it.  By default this
    is set to a very large number, leaving the posting of unapproved
    urls wide open, but by setting $UnapprovedLinkCountMax to a smaller
    number you can limit the number of unapproved urls that make it into
    a page.  (Wikispammers seem to like to post long lists of urls, while
    more "normal" authors tend to only post a few.)
*/

$LinkFunctions['http:'] = 'LinkHTTP';
$LinkFunctions['https:'] = 'LinkHTTP';
SDV($ApprovedUrlPagesFmt, array('$SiteGroup.ApprovedUrls'));
SDV($UnapprovedLinkFmt,
  "\$LinkText<a class='apprlink' href='{\$PageUrl}?action=approvesites'>$[(approve sites)]</a>");
$HTMLStylesFmt['urlapprove'] = '.apprlink { font-size:smaller; }';
SDV($ApproveUrlPattern,
  "\\bhttps?:[^\\s$UrlExcludeChars]*[^\\s.,?!$UrlExcludeChars]");
$WhiteUrlPatterns = (array)$WhiteUrlPatterns;
SDV($HandleActions['approveurls'], 'HandleApprove');
SDV($HandleAuth['approveurls'], 'edit');
SDV($HandleActions['approvesites'], 'HandleApprove');
SDV($HandleAuth['approvesites'], 'edit');
SDV($UnapprovedLinkCountMax, 1000000);
array_splice($EditFunctions, array_search('PostPage', $EditFunctions),
  0, 'BlockUnapprovedPosts');

function LinkHTTP($pagename,$imap,$path,$title,$txt,$fmt=NULL) {
  global $EnableUrlApprovalRequired, $IMap, $WhiteUrlPatterns, $FmtV,
    $UnapprovedLinkCount, $UnapprovedLinkFmt;
  if (!IsEnabled($EnableUrlApprovalRequired,1))
    return LinkIMap($pagename,$imap,$path,$title,$txt,$fmt);
  static $havereadpages;
  if (!$havereadpages) { ReadApprovedUrls($pagename); $havereadpages=true; }
  $p = str_replace(' ','%20',$path);
  $url = str_replace('$1',$p,$IMap[$imap]);
  foreach((array)$WhiteUrlPatterns as $pat) {
    if (preg_match("!^$pat(/|$)!i",$url))
      return LinkIMap($pagename,$imap,$path,$title,$txt,$fmt);
  }
  $FmtV['$LinkUrl'] = PUE(str_replace('$1',$path,$IMap[$imap]));
  $FmtV['$LinkText'] = $txt;
  $FmtV['$LinkAlt'] = str_replace(array('"',"'"),array('&#34;','&#39;'),$title);
  @$UnapprovedLinkCount++;
  return FmtPageName($UnapprovedLinkFmt,$pagename);
}

function ReadApprovedUrls($pagename) {
  global $ApprovedUrlPagesFmt,$ApproveUrlPattern,$WhiteUrlPatterns;
  foreach((array)$ApprovedUrlPagesFmt as $p) {
    $apage = ReadPage(FmtPageName($p,$pagename));
    preg_match_all("/$ApproveUrlPattern/",@$apage['text'],$match);
    foreach($match[0] as $a) {
      $urlp = preg_quote($a,'!');
      if (!in_array($urlp,$WhiteUrlPatterns))
        $WhiteUrlPatterns[] = $urlp;
    }
  }
}

function HandleApprove($pagename, $auth='edit') {
  global $ApproveUrlPattern,$WhiteUrlPatterns,$ApprovedUrlPagesFmt,$action;
  Lock(2);
  $page = ReadPage($pagename);
  $text = preg_replace('/[()]/','',$page['text']);
  preg_match_all("/$ApproveUrlPattern/",$text,$match);
  ReadApprovedUrls($pagename);
  $addpat = array();
  foreach($match[0] as $a) {
    if ($action=='approvesites') 
      $a=preg_replace("!^([^:]+://[^/]+).*$!",'$1',$a);
    $addpat[] = $a;
  }
  if (count($addpat)>0) {
    $aname = FmtPageName($ApprovedUrlPagesFmt[0],$pagename);
    $apage = RetrieveAuthPage($aname, $auth);
    if (!$apage) Abort("?cannot edit $aname");
    $new = $apage;
    if (substr($new['text'],-1,1)!="\n") $new['text'].="\n";
    foreach($addpat as $a) {
      foreach((array)$WhiteUrlPatterns as $pat)
        if (preg_match("!^$pat(/|$)!i",$a)) continue 2;
      $urlp = preg_quote($a,'!');
      $WhiteUrlPatterns[] = $urlp;
      $new['text'].="  $a\n";
    }
    $_POST['post'] = 'y';
    PostPage($aname,$apage,$new);
  }
  Redirect($pagename);
}

function BlockUnapprovedPosts($pagename, &$page, &$new) {
  global $EnableUrlApprovalRequired, $UnapprovedLinkCount, 
    $UnapprovedLinkCountMax, $EnablePost, $MessagesFmt, $BlockMessageFmt;
  if (!IsEnabled($EnableUrlApprovalRequired, 1)) return;
  if ($UnapprovedLinkCount <= $UnapprovedLinkCountMax) return;
  if ($page['=auth']['admin']) return;
  $EnablePost = 0;
  $MessagesFmt[] = $BlockMessageFmt;
}
    
