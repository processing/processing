<?php if (!defined('PmWiki')) exit();
/*  Copyright 2002-2005 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script enables email notifications to be sent whenever posts
    are made.  It is included by default from the stdconfig.php 
    script if $EnableMailPosts is set to non-zero.  Be sure to set
    the $MailPostsTo variable or the scratch file will grow without
    limit.

    Several variables control the functioning of this script:

    $MailPostsTo - comma separated list of email recipients
    $MailPostsFrom - return email address
    $MailPostsDelay - number of seconds to wait before sending mail after
      the first post.  Useful so that lots of small edits result are 
      batched together in a single email message.  However, mail
      won't be sent until the first execution of pmwiki.php after the
      delay has expired, which could be much longer than the delay period
      itself depending on how active your site is.
    $MailPostsSquelch - minimum number of seconds between sending mail
      messages.  Useful when $MailPostsDelay is set to a small value
      to prevent large numbers of mail notifications to be sent.
    $MailPostsFile - scratch file used to keep track of recent posts
    $MailPostsMessage - body of message to be sent.  The sequence 
      '$MailPostsList' is replaced with the list of changes.
    $MailPostTimeFmt - the format for dates and times in $PostTime
    $MailPostItemFmt - the text to be sent for each changed item in the post.
      The string $PostTime contains the time of the post formatted
      according to $MailPostTimeFmt.
    $MailPostsSubject - subject line for mail to be sent
    $MailPostsHeaders - string of extra headers for mail (passed to PHP
       mail() function, some headers may not work for PHP < 4.3).
    $MailPostsFunction - If the default PHP mail function isn't working
       for you, then you can define your own mail function here or you
       can try "MailPostsSendmail".  
*/

SDV($MailPostsDelay,0);
SDV($MailPostsSquelch,7200);
SDV($MailPostsFile,"$WorkDir/.mailposts");
SDV($MailPostsMessage,"Recent wiki posts:\n"
  ."  ($ScriptUrl/$SiteGroup/AllRecentChanges)\n\n\$MailPostsList\n");
SDV($MailPostsSubject,"$WikiTitle recent wiki posts");
SDV($MailPostsFunction,"mail");
SDV($MailPostsTimeFmt,$TimeFmt);
SDV($MailPostsItemFmt,' * $FullName . . . $PostTime by $Author');
SDV($MailPostsHeaders,'');
if (@$MailPostsFrom) 
  $MailPostsHeaders = "From: $MailPostsFrom\r\n$MailPostsHeaders";

array_push($EditFunctions,'MailPosts');

function MailPosts($pagename, &$page, &$new) {
  global $IsPagePosted, $MailPostsFile, $MailPostsTimeFmt, $Now,
    $MailPostsItemFmt, $PostTime;
  if (!$IsPagePosted) return;
  $fp = @fopen($MailPostsFile, "a");
  if ($fp) { 
    $PostTime = strftime($MailPostsTimeFmt, $Now);
    fputs($fp,
        urlencode(FmtPageName("$Now $MailPostsItemFmt", $pagename))."\n"); 
    fclose($fp); 
  }
}

if (@$MailPostsTo == "") return;
$fp = @fopen($MailPostsFile, "r");
if (!$fp) return;
$oldestpost = $Now+1; $mailpost=array();
while (!feof($fp)) {
  $x = urldecode(rtrim(fgets($fp, 1024)));
  @(list($t,$p) = explode(' ', $x, 2));
  if (!$t) continue;
  if ($p=='#lastmailed') {
    if ($t > $Now-$MailPostsSquelch) { fclose($fp); return; }
    continue;
  }
  Lock(2);
  array_push($mailpost, $p."\n");
  if ($t<$oldestpost) $oldestpost=$t;
}
fclose($fp);

if ($oldestpost > $Now-$MailPostsDelay) { Lock(0); return; }
$MailPostsFunction($MailPostsTo,$MailPostsSubject,
  str_replace('$MailPostsList',join('',$mailpost),$MailPostsMessage),
  $MailPostsHeaders);

$fp = @fopen($MailPostsFile,"w");
if ($fp) { fputs($fp,"$Now #lastmailed\n"); fclose($fp); }
Lock(0);

function MailPostsSendmail($to,$subject,$msg,$headers) {
  if (preg_match('/From: .*?([-.\w]+@[-.\w]+)/',$headers,$match)) 
    $from="-f".$match[1];
  $mailer = popen("/usr/lib/sendmail -t -i $from","w");
  fwrite($mailer,"To: $to\nSubject: $subject\n$headers\n\n$msg");
  pclose($mailer);
}

