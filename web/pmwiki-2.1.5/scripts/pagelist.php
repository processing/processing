<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script implements (:pagelist:) and friends -- it's one
    of the nastiest scripts you'll ever encounter.  Part of the reason
    for this is that page listings are so powerful and flexible, so
    that adds complexity.  They're also expensive, so we have to
    optimize them wherever we can.

    The core function is FmtPageList(), which will generate a 
    listing according to a wide variety of options.  FmtPageList takes 
    care of initial option processing, and then calls a "FPL"
    (format page list) function to obtain the formatted output.
    The FPL function is chosen by the 'fmt=' option to (:pagelist:).

    Each FPL function calls MakePageList() to obtain the list
    of pages, formats the list somehow, and returns the results
    to FmtPageList.  FmtPageList then returns the output to
    the caller, and calls Keep() (preserves HTML) or PRR() (re-evaluate
    as markup) as appropriate for the output being returned.
*/

## $PageIndexFile is the index file for term searches and link= option
if (IsEnabled($EnablePageIndex, 1)) {
  SDV($PageIndexFile, "$WorkDir/.pageindex");
  $EditFunctions[] = 'PostPageIndex';
}

## $SearchPatterns holds patterns for list= option
SDVA($SearchPatterns['all'], array());
$SearchPatterns['normal'][] = '!\.(All)?Recent(Changes|Uploads)$!';
$SearchPatterns['normal'][] = '!\.Group(Print)?(Header|Footer|Attributes)$!';
$SearchPatterns['normal'][] = str_replace('.', '\\.', "!^$pagename$!");

## $FPLFormatOpt is a list of options associated with fmt=
## values.  'default' is used for any undefined values of fmt=.
SDVA($FPLFormatOpt, array(
  'default' => array('fn' => 'FPLTemplate', 'fmt' => '#default', 
                     'class' => 'fpltemplate'),
  'bygroup' => array('fn' => 'FPLTemplate', 'template' => '#bygroup',
                     'class' => 'fplbygroup'),
  'simple'  => array('fn' => 'FPLTemplate', 'template' => '#simple',
                     'class' => 'fplsimple'),
  'group'   => array('fn' => 'FPLTemplate', 'template' => '#group',
                     'class' => 'fplgroup'),
  'title'   => array('fn' => 'FPLTemplate', 'template' => '#title',
                     'class' => 'fpltitle', 'order' => 'title'),
  ));

SDV($SearchResultsFmt, "<div class='wikisearch'>\$[SearchFor]
  $HTMLVSpace\$MatchList
  $HTMLVSpace\$[SearchFound]$HTMLVSpace</div>");
SDV($SearchQuery, str_replace('$', '&#036;', 
  htmlspecialchars(stripmagic(@$_REQUEST['q']), ENT_NOQUOTES)));
XLSDV('en', array(
  'SearchFor' => 'Results of search for <em>$Needle</em>:',
  'SearchFound' => 
    '$MatchCount pages found out of $MatchSearched pages searched.'));

Markup('pagelist', 'directives',
  '/\\(:pagelist(\\s+.*?)?:\\)/ei',
  "FmtPageList('\$MatchList', \$pagename, array('o' => PSS('$1 ')))");
Markup('searchbox', 'directives',
  '/\\(:searchbox(\\s.*?)?:\\)/e',
  "SearchBox(\$pagename, ParseArgs(PSS('$1')))");
Markup('searchresults', 'directives',
  '/\\(:searchresults(\\s+.*?)?:\\)/ei',
  "FmtPageList(\$GLOBALS['SearchResultsFmt'], \$pagename, 
       array('req' => 1, 'o' => PSS('$1')))");

SDV($SaveAttrPatterns['/\\(:(searchresults|pagelist)(\\s+.*?)?:\\)/i'], ' ');

SDV($HandleActions['search'], 'HandleSearchA');
SDV($HandleAuth['search'], 'read');
SDV($ActionTitleFmt['search'], '| $[Search Results]');

## SearchBox generates the output of the (:searchbox:) markup.
## If $SearchBoxFmt is defined, that is used, otherwise a searchbox
## is generated.  Options include group=, size=, label=.
function SearchBox($pagename, $opt) {
  global $SearchBoxFmt, $SearchBoxOpt, $SearchQuery, $EnablePathInfo;
  if (isset($SearchBoxFmt)) return Keep(FmtPageName($SearchBoxFmt, $pagename));
  SDVA($SearchBoxOpt, array('size' => '40', 
    'label' => FmtPageName('$[Search]', $pagename),
    'value' => str_replace("'", "&#039;", $SearchQuery)));
  $opt = array_merge((array)$SearchBoxOpt, @$_GET, (array)$opt);
  $opt['action'] = 'search';
  $target = ($opt['target']) 
            ? MakePageName($pagename, $opt['target']) : $pagename;
  $out = FmtPageName(" class='wikisearch' action='\$PageUrl' method='get'>",
                     $target);
  $opt['n'] = IsEnabled($EnablePathInfo, 0) ? '' : $target;
  $out .= "<input type='text' name='q' value='{$opt['value']}' 
    class='inputbox searchbox' size='{$opt['size']}' /><input type='submit' 
    class='inputbutton searchbutton' value='{$opt['label']}' />";
  foreach($opt as $k => $v) {
    if ($v == '') continue;
    if ($k == 'q' || $k == 'label' || $k == 'value' || $k == 'size') continue;
    $k = str_replace("'", "&#039;", $k);
    $v = str_replace("'", "&#039;", $v);
    $out .= "<input type='hidden' name='$k' value='$v' />";
  }
  return '<form '.Keep($out).'</form>';
}

## FmtPageList combines options from markup, request form, and url,
## calls the appropriate formatting function, and returns the string.
function FmtPageList($outfmt, $pagename, $opt) {
  global $GroupPattern, $FmtV, $FPLFormatOpt, $FPLFunctions;
  # get any form or url-submitted request
  $rq = htmlspecialchars(stripmagic(@$_REQUEST['q']), ENT_NOQUOTES);
  # build the search string
  $FmtV['$Needle'] = $opt['o'] . ' ' . $rq;
  # Handle "group/" at the beginning of the form-submitted request
  if (preg_match("!^($GroupPattern(\\|$GroupPattern)*)?/!i", $rq, $match)) {
    $opt['group'] = @$match[1];
    $rq = substr($rq, strlen(@$match[1])+1);
  }
  # merge markup options with form and url
  $opt = array_merge($opt, ParseArgs($opt['o'] . ' ' . $rq), @$_REQUEST);
  # non-posted blank search requests return nothing
  if (@($opt['req'] && !$opt['-'] && !$opt[''] && !$opt['+'] && !$opt['q']))
    return '';
  # terms and group to be included and excluded
  $GLOBALS['SearchIncl'] = array_merge((array)@$opt[''], (array)@$opt['+']);
  $GLOBALS['SearchExcl'] = (array)@$opt['-'];
  $GLOBALS['SearchGroup'] = @$opt['group'];
  $fmt = @$opt['fmt']; if (!$fmt) $fmt = 'default';
  $fmtopt = @$FPLFormatOpt[$fmt];
  if (!is_array($fmtopt)) {
    if ($fmtopt) $fmtopt = array('fn' => $fmtopt);
    elseif (@$FPLFunctions[$fmt]) 
      $fmtopt = array('fn' => $FPLFunctions[$fmt]);
    else $fmtopt = $FPLFormatOpt['default'];
  }
  $fmtfn = @$fmtopt['fn'];
  if (!is_callable($fmtfn)) $fmtfn = $FPLFormatOpt['default']['fn'];
  $matches = array();
  $opt = array_merge($fmtopt, $opt);
  $out = $fmtfn($pagename, $matches, $opt);
  $FmtV['$MatchCount'] = count($matches);
  if ($outfmt != '$MatchList') 
    { $FmtV['$MatchList'] = $out; $out = FmtPageName($outfmt, $pagename); }
  $out = preg_replace('/^(<[^>]+>)(.*)/esm', "PSS('$1').Keep(PSS('$2'))", $out);
  return PRR($out);
}

## MakePageList generates a list of pages using the specifications given
## by $opt.
function MakePageList($pagename, $opt, $retpages = 1) {
  global $MakePageListOpt, $SearchPatterns, $EnablePageListProtect, $PCache,
    $FmtV;
  StopWatch('MakePageList begin');
  SDVA($MakePageListOpt, array('list' => 'default'));

  $opt = array_merge((array)$MakePageListOpt, $opt);
  $readf = @$opt['readf'];
  # we have to read the page if order= is anything but name
  $order = @$opt['order'];
  $readf |= $order && ($order!='name') && ($order!='-name');

  $pats = @(array)$SearchPatterns[$opt['list']];
  if (@$opt['group']) $pats[] = FixGlob($opt['group'], '$1$2.*');
  if (@$opt['name']) $pats[] = FixGlob($opt['name'], '$1*.$2');

  # inclp/exclp contain words to be included/excluded.  
  $incl = array(); $inclp = array(); $inclx = false;
  $excl = array(); $exclp = ''; 
  foreach((array)@$opt[''] as $i) { $incl[] = $i; }
  foreach((array)@$opt['+'] as $i) { $incl[] = $i; }
  foreach((array)@$opt['-'] as $i) { $excl[] = $i; }
  foreach($incl as $i) {
    $inclp[] = '$'.preg_quote($i).'$i';
    $inclx |= preg_match('[^\\w\\x80-\\xff]', $i);
  }
  if ($excl) $exclp = '$'.implode('|', array_map('preg_quote', $excl)).'$i';

  $searchterms = count($incl) + count($excl);
  $readf += $searchterms;                         # forced read if incl/excl

  if (@$opt['trail']) {
    $trail = ReadTrail($pagename, $opt['trail']);
    $list = array();
    foreach($trail as $tstop) {
      $pn = $tstop['pagename'];
      $list[] = $pn;
      $tstop['parentnames'] = array();
      PCache($pn, $tstop);
    }
    foreach($trail as $tstop) 
      $PCache[$tstop['pagename']]['parentnames'][] =
        @$trail[$tstop['parent']]['pagename'];
  } else $list = ListPages($pats);

  if (IsEnabled($EnablePageListProtect, 1)) $readf = 1000;
  $matches = array();
  $FmtV['$MatchSearched'] = count($list);

  $terms = ($incl) ? PageIndexTerms($incl) : array();
  if (@$opt['link']) { 
    $link = MakePageName($pagename, $opt['link']);
    $linkp = "/(^|,)$link(,|$)/i";
    $terms[] = " $link ";
    $readf++;
  }
  if ($terms) {
    $xlist = PageIndexGrep($terms, true);
    $a = count($list);
    $list = array_diff($list, $xlist);
    $a -= count($list);
    StopWatch("MakePageList: PageIndex filtered $a pages");
  }
  $xlist = array();

  StopWatch('MakePageList scanning '.count($list)." pages, readf=$readf");
  foreach((array)$list as $pn) {
    if ($readf) {
      $page = ($readf >= 1000) 
              ? RetrieveAuthPage($pn, 'read', false, READPAGE_CURRENT)
              : ReadPage($pn, READPAGE_CURRENT);
      if (!$page) continue;
      if (@$linkp && !preg_match($linkp, @$page['targets']))
        { $xlist[] = $pn; continue; }
      if ($searchterms) {
        $text = $pn."\n".@$page['targets']."\n".@$page['text'];
        if ($exclp && preg_match($exclp, $text)) continue;
        foreach($inclp as $i) 
          if (!preg_match($i, $text)) 
            { if ($inclx) $xlist[] = $pn; continue 2; }
      }
      $page['size'] = strlen(@$page['text']);
    } else $page = array();
    $page['pagename'] = $page['name'] = $pn;
    PCache($pn, $page);
    $matches[] = $pn;
  }
  StopWatch('MakePageList sort');
  if ($order) SortPageList($matches, $order);
  if ($xlist) {
    register_shutdown_function('flush');
    register_shutdown_function('PageIndexUpdate', $xlist, getcwd());
  }
  StopWatch('MakePageList end');
  if ($retpages) 
    for($i=0; $i<count($matches); $i++)
      $matches[$i] = &$PCache[$matches[$i]];
  return $matches;
}

function SortPageList(&$matches, $order) {
  global $PCache;
  $code = '';
  foreach(preg_split("/[\\s,|]+/", $order, -1, PREG_SPLIT_NO_EMPTY) as $o) {
    if ($o{0}=='-') { $r = '-'; $o = substr($o, 1); }
    else $r = '';
    switch ($o) {
      case 'random':
        foreach($matches as $pn) $PCache[$pn]['random'] = rand();
        /* fall through */
      case 'size':
      case 'time':
      case 'ctime':
        $code .= "\$c = @(\$PCache[\$x]['$o']-\$PCache[\$y]['$o']); ";
        break;
      default:
        if ($o == 'title') 
          foreach($matches as $pn) 
            if (!isset($PCache[$pn]['title'])) 
              $PCache[$pn]['title'] = PageVar($pn, '$Title');
        if ($o == 'group') 
          foreach($matches as $pn) 
            $PCache[$pn]['group'] = PageVar($pn, '$Group');
        $code .= "\$c = @strcasecmp(\$PCache[\$x]['$o'],\$PCache[\$y]['$o']); ";
        break;
    }
    $code .= "if (\$c) return $r\$c;\n";
  }
  if ($code) 
    uasort($matches, 
           create_function('$x,$y', "global \$PCache; $code return 0;"));
}

## HandleSearchA performs ?action=search.  It's basically the same
## as ?action=browse, except it takes its contents from Site.Search.
function HandleSearchA($pagename, $level = 'read') {
  global $PageSearchForm, $FmtV, $HandleSearchFmt, 
    $PageStartFmt, $PageEndFmt;
  SDV($HandleSearchFmt,array(&$PageStartFmt, '$PageText', &$PageEndFmt));
  SDV($PageSearchForm, '$[{$SiteGroup}/Search]');
  $form = RetrieveAuthPage($pagename, $level, true, READPAGE_CURRENT);
  if (!$form) Abort("?unable to read $pagename");
  PCache($pagename, $form);
  $text = preg_replace('/\\[([=@])(.*?)\\1\\]/s', ' ', $form['text']);
  if (!preg_match('/\\(:searchresults(\\s.*?)?:\\)/', $text))
    foreach((array)$PageSearchForm as $formfmt) {
      $form = ReadPage(FmtPageName($formfmt, $pagename), READPAGE_CURRENT);
      if ($form['text']) break;
    }
  $text = @$form['text'];
  if (!$text) $text = '(:searchresults:)';
  $FmtV['$PageText'] = MarkupToHTML($pagename,$text);
  PrintFmt($pagename, $HandleSearchFmt);
}

########################################################################
## The functions below provide different formatting options for
## the output list, controlled by the fmt= parameter and the
## $FPLFormatOpt hash.
########################################################################

function FPLTemplate($pagename, &$matches, $opt) {
  global $Cursor, $FPLFormatOpt, $FPLTemplatePageFmt;
  SDV($FPLTemplatePageFmt, '{$SiteGroup}.PageListTemplates');

  $template = @$opt['template'];
  if (!$template) $template = @$opt['fmt'];

  list($tname, $qf) = explode('#', $template, 2);
  if ($tname) $tname = MakePageName($pagename, $tname);
  else $tname = FmtPageName($FPLTemplatePageFmt, $pagename);
  if ($qf) $tname .= "#$qf";
  $ttext = IncludeText($pagename, $tname, true);
  $ttext = preg_replace('/\\[\\[#[A-Za-z][-.:\\w]*\\]\\]/', '', $ttext);

  if (!@$opt['order'] && !@$opt['trail']) $opt['order'] = 'name';
  $matches = array_values(MakePageList($pagename, $opt, 0));
  if (@$opt['count']) array_splice($matches, $opt['count']);

  $savecursor = $Cursor;
  $pagecount = 0; $groupcount = 0; $grouppagecount = 0;
  $vk = array('{$PageCount}', '{$GroupCount}', '{$GroupPageCount}');
  $vv = array(&$pagecount, &$groupcount, &$grouppagecount);

  $lgroup = ''; $out = '';
  foreach($matches as $i => $pn) {
    $prev = (string)@$matches[$i-1];
    $next = (string)@$matches[$i+1];
    $Cursor['<'] = $Cursor['&lt;'] = $prev;
    $Cursor['='] = $pn;
    $Cursor['>'] = $Cursor['&gt;'] = $next;
    $group = PageVar($pn, '$Group');
    if ($group != $lgroup) { $groupcount++; $grouppagecount = 0; }
    $grouppagecount++; $pagecount++;

    $item = str_replace($vk, $vv, $ttext);
    $item = preg_replace('/\\{(=|&[lg]t;)(\\$\\w+)\\}/e',
                "PageVar(\$pn, '$2', '$1')", $item);
    $out .= $item;
    $lgroup = $group;
  }
  $class = preg_replace('/[^-a-zA-Z0-9\\x80-\\xff]/', ' ', @$opt['class']);
  $div = ($class) ? "<div class='$class'>" : '<div>';
  return $div.MarkupToHTML($pagename, $out, array('escape' => 0)).'</div>';
}


########################################################################
## The functions below optimize searches by maintaining a file of
## words and link cross references (the "page index").
########################################################################

## PageIndexTerms($terms) takes an array of strings and returns a
## normalized list of associated search terms.  This reduces the
## size of the index and speeds up searches.
function PageIndexTerms($terms) {
  $w = array();
  foreach((array)$terms as $t) {
    $w = array_merge($w, preg_split('/[^\\w\\x80-\\xff]+/', 
                                    strtolower($t), -1, PREG_SPLIT_NO_EMPTY));
  }
 return $w;
}

## The PageIndexUpdate($pagelist) function updates the page index
## file with terms and target links for the pages in $pagelist.
## The optional $dir parameter allows this function to be called
## via register_shutdown_function (which sometimes changes directories
## on us).
function PageIndexUpdate($pagelist, $dir = '') {
  global $PageIndexFile, $PageIndexTime, $Now;
  $abort = ignore_user_abort(true);
  if ($dir) chdir($dir);
  SDV($PageIndexTime, 10);
  if (!$pagelist || !$PageIndexFile) return;
  $c = count($pagelist);
  StopWatch("PageIndexUpdate begin ($c pages to update)");
  $pagelist = (array)$pagelist;
  $timeout = time() + $PageIndexTime;
  $cmpfn = create_function('$a,$b', 'return strlen($b)-strlen($a);');
  Lock(2);
  $ofp = fopen("$PageIndexFile,new", 'w');
  foreach($pagelist as $pn) {
    if (time() > $timeout) break;
    $page = ReadPage($pn, READPAGE_CURRENT);
    if ($page) {
      $targets = str_replace(',', ' ', @$page['targets']);
      $terms = PageIndexTerms(array(@$page['text'], $targets, $pn));
      usort($terms, $cmpfn);
      $x = '';
      foreach($terms as $t) { if (strpos($x, $t) === false) $x .= " $t"; }
      fputs($ofp, "$pn:$Now: $targets :$x\n");
    }
    $updated[$pn]++;
  }
  $ifp = @fopen($PageIndexFile, 'r');
  if ($ifp) {
    while (!feof($ifp)) {
      $line = fgets($ifp, 4096);
      while (substr($line, -1, 1) != "\n" && !feof($ifp)) 
        $line .= fgets($ifp, 4096);
      $i = strpos($line, ':');
      if ($i === false) continue;
      $n = substr($line, 0, $i);
      if (@$updated[$n]) continue;
      fputs($ofp, $line);
    }
    fclose($ifp);
  }
  fclose($ofp);
  if (file_exists($PageIndexFile)) unlink($PageIndexFile); 
  rename("$PageIndexFile,new", $PageIndexFile);
  fixperms($PageIndexFile);
  $c = count($updated);
  StopWatch("PageIndexUpdate end ($c updated)");
  ignore_user_abort($abort);
}

## PageIndexGrep returns a list of pages that match the strings
## provided.  Note that some search terms may need to be normalized
## in order to get the desired results (see PageIndexTerms above).
## Also note that this just works for the index; if the index is
## incomplete, then so are the results returned by this list.
## (MakePageList above already knows how to deal with this.)
function PageIndexGrep($terms, $invert = false) {
  global $PageIndexFile;
  if (!$PageIndexFile) return array();
  StopWatch('PageIndexGrep begin');
  $pagelist = array();
  $fp = @fopen($PageIndexFile, 'r');
  if ($fp) {
    $terms = (array)$terms;
    while (!feof($fp)) {
      $line = fgets($fp, 4096);
      while (substr($line, -1, 1) != "\n" && !feof($fp))
        $line .= fgets($fp, 4096);
      $i = strpos($line, ':');
      if (!$i) continue;
      $add = true;
      foreach($terms as $t) 
        if (strpos($line, $t) === false) { $add = false; break; }
      if ($add xor $invert) $pagelist[] = substr($line, 0, $i);
    }
    fclose($fp);
  }
  StopWatch('PageIndexGrep end');
  return $pagelist;
}
  
## PostPageIndex is inserted into $EditFunctions to update
## the linkindex whenever a page is saved.
function PostPageIndex($pagename, &$page, &$new) {
  global $IsPagePosted;
  if ($IsPagePosted) PageIndexUpdate($pagename);
}
