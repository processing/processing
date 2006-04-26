<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file attempts to ease conversions of PmWiki 1.x installations
    to PmWiki 2.  This is definitely a preliminary implementation and
    still probably needs some work.

    The major components are UseV1WikiD($path) and ConvertV1WikiD($path).
    UseV1WikiD tells PmWiki to make use of an existing PmWiki 1.x wiki.d/
    directory, converting PmWiki 1 markup into PmWiki 2 markup as the 
    page is read.  Pages are then saved in the PmWiki 2 installation's wiki.d/
    directory, which should be separate from the original wiki.d/.

    The intent is that a wiki administrator can install, configure, and
    test a PmWiki 2 installation on an existing set of PmWiki 1.x pages 
    without losing or modifying the 1.x page files.

    ConvertV1WikiD($path) is a function that allows pages to be converted
    from a 1.x wiki.d/ into the 2.0 directory all at once.

    Details on this are being maintained at the UpgradingFromPmWiki1 page 
    http://www.pmwiki.org/wiki/PmWiki/UpgradingFromPmWiki1 .
*/

SDVA($Compat1x,array(
  # [[para:]] markup from cookbook recipe
  '/\\[\\[para:(.*?)\\]\\]/' => '(:para $1:)',

  # [[tocauto]] from cookbook recipe
  '/\\[\\[tocauto(.*?)\\]\\]/' => '(:toc$1:)',

  # nolinebreaks
  "/\\[\\[((no)?linebreaks)\\]\\]/" => '(:$1:)',

  # noheader, nofooter, etc.
  "/\\[\\[(noheader|nofooter|nogroupheader|nogroupfooter|notitle|spacewikiwords)\\]\\]/" => '(:$1:)',

  # include, redirect
  "/\\[\\[(include|redirect):(.*?)\\]\\]/" => '(:$1 $2:)',

  # table, cell, cellnr, endtable
  "/\\[\\[(table|cell|cellnr|tableend)(\\s.*?)?\\]\\]\n?/" => "(:$1$2:)\n",

  # [[$Title]]
  "/\\[\\[\\\$Title\\]\\]/" => '{$Name}',
  "/\\[\\[\\\$Titlespaced\\]\\]/" => '{$Namespaced}',

  # [[$pagecount]], from SimplePageCount cookbook script
  "/\\[\\[\\\$pagecount\\]\\]/" => '{$PageCount}',

  # [[$Group]], [[$Version]], etc.
  "/\\[\\[\\$(Group|Version|Author|LastModified|LastModifiedBy|LastModifiedHost)\\]\\]/" => '{$$1}',

  # [[$Edit text]], [[$Diff text]]
  "/\\[\\[\\\$Edit\\s(.*?)\\]\\]/" => '[[{$Name}?action=edit |$1]]',
  "/\\[\\[\\\$Diff\\s(.*?)\\]\\]/" => '[[{$Name}?action=diff |$1]]',

  # [[$Search]], [[$SearchResults]], [[$Attachlist]]
  "/\\[\\[\\\$Search\\]\\]/" => '(:searchbox:)',
  "/\\[\\[\\\$Searchresults\\]\\]/" => '(:searchresults:)',
  "/\\[\\[\\\$Attachlist(\\s.*?)?\\]\\]/" => '(:attachlist$1:)',

  # [[Drawing:]] from PmWikiDraw (javajunky on #pmwiki)
  "/\\[\\[Drawing:(.*?)\\]\\]/" => '(:drawing $1:)',

  # [[target linktext]]
  "/\\[\\[((\\w|\\#)[^$UrlExcludeChars\\s]*)\\s((.|\\\n)*?)\\]\\]/" 
    => '[[$1 |$3]]',

  # [[target]]
  "/\\[\\[(\\w[^$UrlExcludeChars\\s]*)\\]\\]/" => '[[$1 |#]]',

  # [[Group.{{free link}} link text]]
  "/\\[\\[($GroupPattern([\\/.]))?\\{\\{(~?\\w[-\\w\\s.\\/]*)\\}\\}([-#\\w]*)\\s((.|\\\n)*?)\\]\\]/" => '[[$1$3$4 |$5]]',

  # [[Group.{{free link|s}} link text]]
  "/\\[\\[($GroupPattern([\\/.]))?\\{\\{(~?\\w[-\\w\\s.\\/]*)\\|([-\\w\\s]*)\\}\\}([-#\\w]*)\\s(.*?)\\]\\]/" => '[[$1$3$4$5 |$6]]',

  # Group.{{free link}}ext
  "/($GroupPattern([\\/.]))?\\{\\{(~?\\w[-\\w\\s.\\/]*)\\}\\}([-\\w]*)/" 
    => '[[$1$3]]$4',

  # Group.{{free link|s}}ext
  "/($GroupPattern([\\/.]))?\\{\\{(~?\\w[-\\w\\s.\\/]*)\\|([-\\w\\s]*)\\}\\}([-\\w]*)/" => '[[$1$3($4)]]$5',

  # :: lists
  "/^(:+)(:[^:\n]*)$/m" => '$1 $2',
));

class PageStore1x extends PageStore {
  function read($pagename) {
    global $Compat1x,$KeepToken;
    $page = parent::read($pagename);
    if ($page) {
      $page['text'] = preg_replace('/(\\[([=@]).*?\\2\\])/se',"Keep(PSS('$1'))",
        @$page['text']);
      $page['text'] = preg_replace(array_keys($Compat1x),
        array_values($Compat1x), $page['text']);
      $page['text'] = preg_replace("/$KeepToken(\\d.*?)$KeepToken/e",
        '$GLOBALS[\'KPV\'][\'$1\']',$page['text']);
    }
    return $page;
  }
}

function UseV1WikiD($path) {
  global $WikiLibDirs;
  if (!is_dir($path)) {
    Abort("?$path is not an accessible directory");
    exit();
  }
  array_splice($WikiLibDirs,1,0,
               array(new PageStore1x("$path/\$FullName")));
}


function ConvertV1WikiD($path) {
  global $WikiDir;
  Lock(2);
  if (!is_dir($path)) {
    Abort("?$path is not an accessible directory");
    exit();
  }
  $WikiV1Dir = new PageStore1x("$path/\$FullName");
  $oldlist = $WikiV1Dir->ls();
  $newlist = ListPages();
  $bothlist = array_intersect($oldlist,$newlist); sort($bothlist);
  $difflist = array_diff($oldlist,$newlist); sort($difflist);
  $bcount = count($bothlist);
  $dcount = count($difflist); 

  echo "
    <html>
    <head>
    <title>Convert v1 pages to v2</title>
    </head>
    <body>
    <h2>Convert and Copy PmWiki v1.x pages into v2.x</h2>
  ";

  $copy = array();
  if (@$_POST['copydiff']) $copy = $difflist;
  if (@$_POST['copyboth']) $copy = array_merge($copy,$bothlist);
  if (@$_POST['copy']) $copy = array_merge($copy,$_POST['copy']);

  if (@$copy) { 
    echo "<p>Okay, I'm now converting the pages you've requested.
       When this is finished, you can see if anything else needs to
       be converted, otherwise you can get rid of the 
       <tt>include_once('scripts/compat1x.php');</tt> and
       <tt>ConvertV1WikiD()</tt> lines that are in your 
       local/config.php file.</p>";
    $copy = array_unique($copy);
    foreach($copy as $p) { 
      echo "<li>Converting $p</li>\n"; 
      $page = $WikiV1Dir->read($p);
      WritePage($p,$page);
    }
    echo "<p>Converted ", count($copy), " pages.</p>\n";
  } else {
    echo "
      <p>This function will migrate pages from a 1.x wiki.d/ directory ($path)
      into your 2.x wiki.d/ directory, converting markups as it proceeds. 
      Note that the files in your 1.x wiki.d/ directory are not affected
      by this script, so if the conversion doesn't work out for any reason
      you still have your original pages lying around.</p>
    ";
  }

  /* now rebuild the lists */
  $oldlist = $WikiV1Dir->ls();
  $newlist = ListPages();
  $bothlist = array_intersect($oldlist,$newlist); sort($bothlist);
  $difflist = array_diff($oldlist,$newlist); sort($difflist);
  $bcount = count($bothlist);
  $dcount = count($difflist); 

 
  echo " <form method='post'> ";

  echo "<h3>Migrate pages from v1 to v2 (don't overwrite existing 
    v2 pages)</h3>";

  if ($difflist) {
    echo "
      <p>The following $dcount pages exist only in the version 1
      wiki.d/ directory.  </p>
      <dd><input type='submit' name='copydiff' value='Copy and convert all
        pages that do not already exist' /></dd>
      <p>or</p><dd><input type='submit' name='copyindv' value='Copy and convert
        pages checked in the list below' /><p></p></dd>
    ";
    foreach($difflist as $p) 
      echo "<dd><input type='checkbox' name='copy[]' value='$p' /> $p</dd>\n";
  } else {
    echo "<p>There aren't any pages in your version 1 wiki.d/ directory that 
      are not already in your version 2 directory.</p>";
  }
  
  echo "<h3>Migrate pages from v1 to v2 (overwrite existing v2 pages)</h3>
    <p>The following $bcount pages exist in <em>both</em> the version 1 and
    version 2 wiki.d/ directories.  If you use one of the buttons below,
    then your converted version 1 pages will <em>overwrite</em> the existing 
    version 2 pages, and you will lose any edits that you might have made
    in the version 2 installation (it's possible that
    this is what you want).</p>
    <dd><input type='submit' name='copyboth' value='Convert and overwrite
      pages that do already exist' /></dd>
    <p>or</p><dd><input type='submit' name='copyindv' value='Convert and
      overwrite pages checked in the list below' /><p></p></dd>
  ";
  foreach($bothlist as $p) 
    echo "<dd><input type='checkbox' name='copy[]' value='$p' /> $p</dd>\n";

  echo "</form></body></html>\n";
  exit();
}

