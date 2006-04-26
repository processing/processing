<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file enables merging of concurrent edits, using the "diff3"
    program available on most Unix systems to merge the edits.  If 
    diff3 is not available or you'd like to use a different command, 
    then set $SysMergeCmd accordingly.
*/

array_unshift($EditFunctions,'MergeSimulEdits');
$HTMLStylesFmt['simuledit'] = ".editconflict { color:green; 
  font-style:italic; margin-top:1.33em; margin-bottom:1.33em; }\n";

function Merge($newtext,$oldtext,$pagetext) {
  global $WorkDir,$SysMergeCmd;
  SDV($SysMergeCmd,"/usr/bin/diff3 -L '' -L '' -L '' -m -E");
  if (substr($newtext,-1,1)!="\n") $newtext.="\n";
  if (substr($oldtext,-1,1)!="\n") $oldtext.="\n";
  if (substr($pagetext,-1,1)!="\n") $pagetext.="\n";
  $tempnew = tempnam($WorkDir,"new");
  $tempold = tempnam($WorkDir,"old");
  $temppag = tempnam($WorkDir,"page");
  if ($newfp=fopen($tempnew,'w')) { fputs($newfp,$newtext); fclose($newfp); }
  if ($oldfp=fopen($tempold,'w')) { fputs($oldfp,$oldtext); fclose($oldfp); }
  if ($pagfp=fopen($temppag,'w')) { fputs($pagfp,$pagetext); fclose($pagfp); }
  $mergetext = '';
  $merge_handle = popen("$SysMergeCmd $tempnew $tempold $temppag",'r');
  if ($merge_handle) {
    while (!feof($merge_handle)) $mergetext .= fread($merge_handle,4096);
    pclose($merge_handle);
  }
  @unlink($tempnew); @unlink($tempold); @unlink($temppag);
  return $mergetext;
}

function MergeSimulEdits($pagename,&$page,&$new) {
  global $Now, $EnablePost, $MessagesFmt, $WorkDir;
  if (@!$_POST['basetime'] || !PageExists($pagename) 
      || $page['time'] >= $Now
      || $_POST['basetime']>=$page['time']
      || $page['text'] == $new['text']) return;
  $EnablePost = 0;
  $old = array();
  RestorePage($pagename,$page,$old,"diff:{$_POST['basetime']}");
  $text = Merge($new['text'],$old['text'],$page['text']);
  if ($text > '') { $new['text'] = $text; $ec = '$[EditConflict]'; }
  else $ec = '$[EditWarning]';
  XLSDV('en', array(
    'EditConflict' => "The page you are
      editing has been modified since you started editing it.
      The modifications have been merged into the text below, 
      you may want to verify the results of the merge before
      pressing save.  Conflicts the system couldn't resolve are
      bracketed by &lt;&lt;&lt;&lt;&lt;&lt;&lt; and
      &gt;&gt;&gt;&gt;&gt;&gt;&gt;.",
    'EditWarning' => "The page you are editing has been modified
      since you started editing it.  If you continue, your
      changes will overwrite any changes that others have made."));
  $MessagesFmt[] = "<p class='editconflict'>$ec
    (<a target='_blank' href='\$PageUrl?action=diff'>$[View changes]</a>)
    </p>\n";
}

