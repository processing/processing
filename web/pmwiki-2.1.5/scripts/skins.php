<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004-2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This file implements the skin selection code for PmWiki.  Skin 
    selection is controlled by the $Skin variable, which can also
    be an array (in which case the first skin found is loaded).

    In addition, $ActionSkin[$action] specifies other skins to be
    searched based on the current action.

*/

SDV($Skin, 'pmwiki');
SDV($ActionSkin['print'], 'print');
SDV($FarmPubDirUrl, $PubDirUrl);
SDV($PageLogoUrl, "$FarmPubDirUrl/skins/pmwiki/pmwiki-32.gif");
SDVA($TmplDisplay, array('PageEditFmt' => 0));

# $PageTemplateFmt is deprecated
if (isset($PageTemplateFmt)) LoadPageTemplate($pagename,$PageTemplateFmt);
else {
  $x = array_merge((array)@$ActionSkin[$action], (array)$Skin);
  SetSkin($pagename, $x);
}

SDV($PageCSSListFmt,array(
  'pub/css/local.css' => '$PubDirUrl/css/local.css',
  'pub/css/{$Group}.css' => '$PubDirUrl/css/{$Group}.css',
  'pub/css/{$FullName}.css' => '$PubDirUrl/css/{$FullName}.css'));

foreach((array)$PageCSSListFmt as $k=>$v) 
  if (file_exists(FmtPageName($k,$pagename))) 
    $HTMLHeaderFmt[] = "<link rel='stylesheet' type='text/css' href='$v' />\n";

# SetSkin changes the current skin to the first available skin from 
# the $skin array.
function SetSkin($pagename, $skin) {
  global $Skin, $SkinDir, $SkinDirUrl, $IsTemplateLoaded, $PubDirUrl,
    $FarmPubDirUrl, $FarmD;
  unset($Skin);
  foreach((array)$skin as $s) {
    $sd = FmtPageName("./pub/skins/$s", $pagename);
    if (is_dir($sd)) 
      { $Skin=$s; $SkinDirUrl="$PubDirUrl/skins/$Skin"; break; }
    $sd = FmtPageName("$FarmD/pub/skins/$s", $pagename);
    if (is_dir($sd)) 
      { $Skin=$s; $SkinDirUrl="$FarmPubDirUrl/skins/$Skin"; break; }
  }
  if (!is_dir($sd)) 
    Abort("?unable to find skin from list ".implode(' ',(array)$skin));
  $SkinDir = $sd;
  $IsTemplateLoaded = 0;
  if (file_exists("$SkinDir/$Skin.php"))
    include_once("$SkinDir/$Skin.php");
  else if (file_exists("$SkinDir/skin.php"))
    include_once("$SkinDir/skin.php");
  if ($IsTemplateLoaded) return;
  if (file_exists("$SkinDir/$Skin.tmpl")) 
    LoadPageTemplate($pagename, "$SkinDir/$Skin.tmpl");
  else if (file_exists("$SkinDir/skin.tmpl"))
    LoadPageTemplate($pagename, "$SkinDir/skin.tmpl");
  else if (($dh = opendir($SkinDir))) {
    while (($fname = readdir($dh)) !== false) {
      if (substr($fname, -5) != '.tmpl') continue;
      if ($IsTemplateLoaded) 
        Abort("?unable to find unique template in $SkinDir");
      LoadPageTemplate($pagename, "$SkinDir/$fname");
    }
    closedir($dh);
  }
  if (!$IsTemplateLoaded) Abort("Unable to load $Skin template");
}


# LoadPageTemplate loads a template into $TmplFmt
function LoadPageTemplate($pagename,$tfilefmt) {
  global $PageStartFmt, $PageEndFmt, $HTMLHeaderFmt,
    $IsTemplateLoaded, $TmplFmt, $TmplDisplay,
    $PageTextStartFmt, $PageTextEndFmt;

  # $BasicLayoutVars is deprecated
  global $BasicLayoutVars;
  if (isset($BasicLayoutVars)) 
    foreach($BasicLayoutVars as $sw) $TmplDisplay[$sw] = 1;

  SDV($PageTextStartFmt, "\n<div id='wikitext'>\n");
  SDV($PageTextEndFmt, "</div>\n");

  $sddef = array('PageEditFmt' => 0);
  $k = implode('',file(FmtPageName($tfilefmt,$pagename)));
  $sect = preg_split('#[[<]!--(/?(?:Page[A-Za-z]+Fmt|PageText|HeaderText)\\s?.*?)--[]>]#',
    $k,0,PREG_SPLIT_DELIM_CAPTURE);
  $TmplFmt['Start'] = array_merge(array('headers:'),
    preg_split('/[[<]!--((?:wiki|file|function|markup):.*?)--[]>]/s',
      array_shift($sect),0,PREG_SPLIT_DELIM_CAPTURE));
  $TmplFmt['End'] = array($PageTextEndFmt);
  $ps = 'Start';
  while (count($sect)>0) {
    $k = array_shift($sect);
    $v = preg_split('/[[<]!--((?:wiki|file|function|markup):.*?)--[]>]/',
      array_shift($sect),0,PREG_SPLIT_DELIM_CAPTURE);
    $TmplFmt[$ps][] = "<!--$k-->";
    if ($k{0} == '/') 
      { $TmplFmt[$ps][] = (count($v) > 1) ? $v : $v[0]; continue; }
    @list($var, $sd) = explode(' ', $k, 2);
    $GLOBALS[$var] = (count($v) > 1) ? $v : $v[0];
    if ($sd > '') $sddef[$var] = $sd;
    if ($var == 'PageText') { $ps = 'End'; }
    if ($var == 'HeaderText') { $TmplFmt[$ps][] = &$HTMLHeaderFmt; }
    $TmplFmt[$ps][$var] =& $GLOBALS[$var];
  }
  array_push($TmplFmt['Start'], $PageTextStartFmt);
  $PageStartFmt = 'function:PrintSkin Start';
  $PageEndFmt = 'function:PrintSkin End';
  $IsTemplateLoaded = 1;
  SDVA($TmplDisplay, $sddef);
}

# This function is called to print a portion of the skin template
# according to the settings in $TmplDisplay.
function PrintSkin($pagename, $arg) {
  global $TmplFmt, $TmplDisplay;
  foreach ($TmplFmt[$arg] as $k => $v) 
    if (!isset($TmplDisplay[$k]) || $TmplDisplay[$k])
      PrintFmt($pagename, $v);
}

