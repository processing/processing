<?php if (!defined('PmWiki')) exit();
/*  Copyright 2005-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script handles various "fixup transitions" that might need to 
    occur to help existing sites smoothly upgrade to newer releases of 
    PmWiki.  Rather than put the workarounds in the main code files, we 
    try to centralize them here so we can see what's deprecated and a
    simple switch (?trans=0 in the url) can tell the admin if his site 
    is relying on an outdated feature or way of doing things.

    Transitions defined in this script:

      $Transition['fplbygroup']         - restore FPLByGroup function

      $Transition['version'] < 2000915  - all transitions listed above
    
      $Transition['mainrc']             - keep using Main.AllRecentChanges
      $Transition['mainapprovedurls']   - keep using Main.ApprovedUrls
      $Transition['pageeditfmt']        - default $PageEditFmt value
      $Transition['mainpages']          - other default pages in Main

      $Transition['version'] < 1999944  - all transitions listed above

    To get all of the transitions for compatibility with a previous
    version of PmWiki, simply set $Transition['version'] in a local
    configuration file to the version number you want compatibility
    with.  All of the transitions associated with that version will
    then be enabled. Example:

            # Keep compatibility with PmWiki version 2.0.13
            $Transition['version'] = 2000013;

    To explicitly enable or disable specific transitions, set
    the corresponding $Transition[] element to 1 or 0.  This will 
    override the $Transition['version'] item listed above.  For 
    example, to enable just the 'pageeditfmt' transition, use

            $Transition['pageeditfmt'] = 1;

*/

## if ?trans=0 is specified, then we don't do any fixups.
if (@$_REQUEST['trans']==='0') return;

## Transitions from 2.1.beta15

if (@$Transition['version'] < 2000915) 
  SDVA($Transition, array('fplbygroup' => 1));

## fplbygroup:
##   The FPLByGroup function was removed in 2.1.beta15, this restores it.
if (@$Transition['fplbygroup'] && !function_exists('FPLByGroup')) {
  SDV($FPLFormatOpt['bygroup'], array('fn' => 'FPLByGroup'));
  function FPLByGroup($pagename, &$matches, $opt) {
    global $FPLByGroupStartFmt, $FPLByGroupEndFmt, $FPLByGroupGFmt,
      $FPLByGroupIFmt, $FPLByGroupOpt;
    SDV($FPLByGroupStartFmt,"<dl class='fplbygroup'>");
    SDV($FPLByGroupEndFmt,'</dl>');
    SDV($FPLByGroupGFmt,"<dt><a href='\$ScriptUrl/\$Group'>\$Group</a> /</dt>\n");
    SDV($FPLByGroupIFmt,"<dd><a href='\$PageUrl'>\$Name</a></dd>\n");
    SDVA($FPLByGroupOpt, array('readf' => 0, 'order' => 'name'));
    $matches = MakePageList($pagename, 
                            array_merge((array)$FPLByGroupOpt, $opt), 0);
    if (@$opt['count']) array_splice($matches, $opt['count']);
    if (count($matches)<1) return '';
    $out = '';
    foreach($matches as $pn) {
      $pgroup = FmtPageName($FPLByGroupGFmt, $pn);
      if ($pgroup != @$lgroup) { $out .= $pgroup; $lgroup = $pgroup; }
      $out .= FmtPageName($FPLByGroupIFmt, $pn);
    }
    return FmtPageName($FPLByGroupStartFmt, $pagename) . $out .
           FmtPageName($FPLByGroupEndFmt, $pagename);
  }
}

## Transitions from 2.0.beta44

if (@$Transition['version'] < 1999944) 
  SDVA($Transition, array('mainrc' => 1, 'mainapprovedurls' => 1,
    'pageeditfmt' => 1, 'mainpages' => 1));

## mainrc:
##   2.0.beta44 switched Main.AllRecentChanges to be 
##   $SiteGroup.AllRecentChanges.  This setting keeps Main.AllRecentChanges
##   if it exists.
if (@$Transition['mainrc'] && PageExists('Main.AllRecentChanges')) {
  SDV($RecentChangesFmt['Main.AllRecentChanges'],
    '* [[$Group.$Name]]  . . . $CurrentTime $[by] $AuthorLink');
}

## siteapprovedurls:
##   2.0.beta44 switched Main.ApprovedUrls to be $SiteGroup.ApprovedUrls .
##   This setting keeps using Main.ApprovedUrls if it exists.
if (@$Transition['mainapprovedurls'] && PageExists('Main.ApprovedUrls')) {
  $ApprovedUrlPagesFmt = (array)$ApprovedUrlPagesFmt;
  if (PageExists(FmtPageName($ApprovedUrlPagesFmt[0], $pagename))) 
    $ApprovedUrlPagesFmt[] = 'Main.ApprovedUrls';
  else array_unshift($ApprovedUrlPagesFmt, 'Main.ApprovedUrls');
}

## pageeditfmt:
##   2.0.beta44 switched to using wiki markup forms for page editing.
##   However, some sites and skins have customized values of $PageEdit.
##   This setting restores the default values.
if (@$Transition['pageeditfmt']) {
  SDV($PageEditFmt, "<div id='wikiedit'>
    <a id='top' name='top'></a>
    <h1 class='wikiaction'>$[Editing \$FullName]</h1>
    <form method='post' action='\$PageUrl?action=edit'>
    <input type='hidden' name='action' value='edit' />
    <input type='hidden' name='n' value='\$FullName' />
    <input type='hidden' name='basetime' value='\$EditBaseTime' />
    \$EditMessageFmt
    <textarea id='text' name='text' rows='25' cols='60'
      onkeydown='if (event.keyCode==27) event.returnValue=false;'
      >\$EditText</textarea><br />
    $[Author]: <input type='text' name='author' value='\$Author' />
    <input type='checkbox' name='diffclass' value='minor' \$DiffClassMinor />
      $[This is a minor edit]<br />
    <input type='submit' name='post' value=' $[Save] ' />
    <input type='submit' name='preview' value=' $[Preview] ' />
    <input type='reset' value=' $[Reset] ' /></form></div>");
  if (@$_POST['preview']) 
    SDV($PagePreviewFmt, "<div id='wikipreview'>
      <h2 class='wikiaction'>$[Preview \$FullName]</h2>
      <p><b>$[Page is unsaved]</b></p>
      \$PreviewText
      <hr /><p><b>$[End of preview -- remember to save]</b><br />
      <a href='#top'>$[Top]</a></p></div>");
  SDV($HandleEditFmt, array(&$PageStartFmt,
    &$PageEditFmt, 'wiki:$[PmWiki.EditQuickReference]', &$PagePreviewFmt,
    &$PageEndFmt));
  $EditMessageFmt = implode('', $MessagesFmt) . $EditMessageFmt;
  if ($action=='edit' && IsEnabled($EnableGUIButtons, 0)) 
    array_push($EditFunctions, 'GUIEdit');
} else $MessagesFmt[] = @$EditMessageFmt;

    
function GUIEdit($pagename, &$page, &$new) {
  global $EditMessageFmt;
  $EditMessageFmt .= GUIButtonCode($pagename);
}

## mainpages:
##   In 2.0.beta44 several utility pages change location to the new Site
##   group.  These settings cause some skins (that use translations)
##   to know to link to the new locations.
if ($Transition['mainpages']) {
  XLSDV('en', array(
    'Main/SearchWiki' => XL('Site/Search'),
    'PmWiki.EditQuickReference' => XL('Site/EditQuickReference'),
    'PmWiki.UploadQuickReference' => XL('Site/UploadQuickReference'),
    ));
}

