<?php if (!defined('PmWiki')) exit();
/*  Copyright 2004 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script defines additional settings needed when the 'print'
    skin is loaded (usually in response to ?action=print, as controlled
    by the $ActionSkin['print'] setting.  See scripts/skins.php for
    more details.

    The changes made are:
      - Redefines the standard layout to a format suitable for printing
      - Redefines internal links to keep ?action=print
      - Changes the display of URL and mailto: links
      - Uses GroupPrintHeader and GroupPrintFooter pages instead
        of GroupHeader and GroupFooter
*/

global $LinkPageExistsFmt, $GroupPrintHeaderFmt, 
  $GroupPrintFooterFmt, $GroupHeaderFmt, $GroupFooterFmt;

$LinkPageExistsFmt = "<a class='wikilink' href='\$PageUrl?action=print'>\$LinkText</a>";
SDV($GroupPrintHeaderFmt,'(:include $Group.GroupPrintHeader:)(:nl:)');
SDV($GroupPrintFooterFmt,'(:nl:)(:include $Group.GroupPrintFooter:)');
$GroupHeaderFmt = $GroupPrintHeaderFmt;
$GroupFooterFmt = $GroupPrintFooterFmt;

