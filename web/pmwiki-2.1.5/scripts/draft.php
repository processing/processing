<?php if (!defined('PmWiki')) exit();
/*  Copyright 2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.
*/

array_unshift($EditFunctions, 'EditDraft');
SDV($DraftSuffix, '-Draft');
if ($DraftSuffix) $SearchPatterns['normal'][] = "!$DraftSuffix\$!";

if ($action == 'edit') 
  SDVA($InputTags['e_savedraftbutton'], array(
    ':html' => "<input type='submit' \$InputFormArgs />",
    'name' => 'postdraft', 'value' => ' '.XL('Save as draft').' ',
    'accesskey' => XL('ak_savedraft')));

function EditDraft(&$pagename, &$page, &$new) {
  global $WikiDir, $DraftSuffix, $DeleteKeyPattern;
  SDV($DeleteKeyPattern, "^\\s*delete\\s*$");
  $basename = preg_replace("/$DraftSuffix\$/", '', $pagename);
  $draftname = $basename . $DraftSuffix;
  if ($_POST['postdraft']) 
    { $pagename = $draftname; return; }
  if ($_POST['post'] && !preg_match("/$DeleteKeyPattern/", $new['text'])) { 
    $pagename = $basename; 
    $page = ReadPage($basename);
    $WikiDir->delete($draftname);
    return; 
  }
  if (PageExists($draftname) && $pagename != $draftname)
    { Redirect($draftname, '$PageUrl?action=edit'); exit(); }
}


