<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file does simple reference counting on pages in a PmWiki site.
    Simply activate this script using
        include_once('scripts/refcount.php');
    in the config.php file and then use ?action=refcount to bring up
    the reference count form.  The output is a table where each row
    of the table contains a page name or link reference, the number
    of (non-RecentChanges) pages that contain links to the page,
    the number of RecentChanges pages with links to the page, and the
    total number of references in all pages.
*/

SDV($PageRefCountFmt,"<h2 class='wikiaction'>Reference Count Results</h2>");
SDV($RefCountTimeFmt," <small>%Y-%b-%d %H:%M</small>");
SDV($HandleActions['refcount'], 'HandleRefCount');

function PrintRefCount($pagename) {
  global $GroupPattern,$NamePattern,$PageRefCountFmt,$RefCountTimeFmt;
  $pagelist = ListPages();
  $grouplist = array();
  foreach($pagelist as $pname) {
    if (!preg_match("/^($GroupPattern)[\\/.]($NamePattern)$/",$pname,$m))
      continue;
    $grouplist[$m[1]]=$m[1];
  }
  asort($grouplist);
  $grouplist = array_merge(array('all' => 'all groups'),$grouplist);

  $wlist = array('all','missing','existing','orphaned');
  $tlist = isset($_REQUEST['tlist']) ? $_REQUEST['tlist'] : array('all');
  $flist = isset($_REQUEST['flist']) ? $_REQUEST['flist'] : array('all');
  $whichrefs = @$_REQUEST['whichrefs'];
  $showrefs = @$_REQUEST['showrefs'];
  $submit = @$_REQUEST['submit'];

  echo FmtPageName($PageRefCountFmt,$pagename);
  echo "<form method='post'><input type='hidden' action='refcount'>
    <table cellspacing='10'><tr><td valign='top'>Show
    <br><select name='whichrefs'>";
  foreach($wlist as $w)
    echo "<option ",($whichrefs==$w) ? 'selected' : ''," value='$w'>$w\n";
  echo "</select></td><td valign='top'> page names in group<br>
    <select name='tlist[]' multiple size='4'>";
  foreach($grouplist as $g=>$t)
    echo "<option ",in_array($g,$tlist) ? 'selected' : ''," value='$g'>$t\n";
  echo "</select></td><td valign='top'> referenced from pages in<br>
    <select name='flist[]' multiple size='4'>";
  foreach($grouplist as $g=>$t)
    echo "<option ",in_array($g,$flist) ? 'selected' : ''," value='$g'>$t\n";
  echo "</select></td></tr></table>
    <p><input type='checkbox' name='showrefs' value='checked' $showrefs>
      Display referencing pages
    <p><input type='submit' name='submit' value='Search'></form><p><hr>";

  if ($submit) {
    foreach($pagelist as $pname) {
      $ref = array();
      $page = ReadPage($pname, READPAGE_CURRENT); 
      if (!$page) continue;
      $tref[$pname]['time'] = $page['time'];
      if (!in_array('all',$flist) &&
          !in_array(FmtPageName('$Group',$pname),$flist)) continue;
      $rc = preg_match('/RecentChanges$/',$pname);
      foreach(explode(',',@$page['targets']) as $r) {
        if ($r=='') continue;
        if ($rc) @$tref[$r]['rc']++;
        else { @$tref[$r]['page']++; @$pref[$r][$pname]++; }
      }
    }
    uasort($tref,'RefCountCmp');
    echo "<table >
      <tr><th></th><th colspan='2'>Referring pages</th></tr>
      <tr><th>Name / Time</th><th>All</th><th>R.C.</th></tr>";
    reset($tref);
    foreach($tref as $p=>$c) {
      if (!in_array('all',$tlist) &&
          !in_array(FmtPageName('$Group',$p),$tlist)) continue;
      if ($whichrefs=='missing' && PageExists($p)) continue;
      elseif ($whichrefs=='existing' && !PageExists($p)) continue;
      elseif ($whichrefs=='orphaned' &&
        (@$tref[$p]['page']>0 || !PageExists($p))) continue;
      echo "<tr><td valign='top'>",LinkPage($pagename, '', $p, '', $p);
      if (@$tref[$p]['time']) echo strftime($RefCountTimeFmt,$tref[$p]['time']);
      if ($showrefs && is_array(@$pref[$p])) {
        foreach($pref[$p] as $pr=>$pc) 
          echo "<dd>", LinkPage($pagename, '', $pr, '', $pr);
      }
      echo "</td>";
      echo "<td align='center' valign='top'>",@$tref[$p]['page']+0,"</td>";
      echo "<td align='center' valign='top'>",@$tref[$p]['rc']+0,"</td>";
      echo "</tr>";
    }
    echo "</table>";
  }
}


function RefCountCmp($ua,$ub) {
  if (@($ua['page']!=$ub['page'])) return @($ub['page']-$ua['page']);
  if (@($ua['rc']!=$ub['rc'])) return @($ub['rc']-$ua['rc']);
  return @($ub['time']-$ua['time']);
}



function HandleRefCount($pagename, $auth='read') {
  global $HandleRefCountFmt,$PageStartFmt,$PageEndFmt;
  $page = RetrieveAuthPage($pagename, $auth, true, READPAGE_CURRENT);
  if (!$page) Abort('?unauthorized');
  PCache($pagename, $page);
  SDV($HandleRefCountFmt,array(&$PageStartFmt,
    'function:PrintRefCount',&$PageEndFmt));
  PrintFmt($pagename,$HandleRefCountFmt);
}

