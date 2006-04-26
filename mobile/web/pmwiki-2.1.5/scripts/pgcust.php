<?php if (!defined('PmWiki')) exit();
/*  Copyright 2002-2005 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

    This script enables per-page and per-group customizations in the
    local/ subdirectory.  For example, to create customizations for
    the 'Demo' group, place them in a file called local/Demo.php.
    To customize a single page, use the full page name (e.g., 
    local/Demo.MyPage.php).  Per-page/per-group customizations can be 
    handled at any time by adding
	include_once("scripts/pgcust.php");
    to config.php.  It is automatically included by scripts/stdconfig.php
    unless $EnablePGCust is set to zero in config.php.

    A page's customization is loaded first, followed by any group
    customization.  If no page or group customizations are loaded,
    then 'local/default.php' is loaded.  

    A per-page configuration file can prevent its group's config from 
    loading by setting $EnablePGCust=0;.  A per-page configuration file 
    can force group customizations to be loaded first by using include_once
    on the group customization file.
    
*/

SDV($DefaultPage,"$DefaultGroup.$DefaultName");
if ($pagename=='') $pagename=$DefaultPage;

$f = 1;
for($p=$pagename;$p;$p=preg_replace('/\\.*[^.]*$/','',$p)) {
  if (!IsEnabled($EnablePGCust,1)) return;
  if (file_exists("local/$p.php")) { include_once("local/$p.php"); $f=0; }
}

if ($f && IsEnabled($EnablePGCust,1) && file_exists('local/default.php'))
  include_once('local/default.php');

