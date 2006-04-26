<?php if (!defined('PmWiki')) exit();
/*  Copyright 2006 Patrick R. Michaud (pmichaud@pobox.com)
    This file is part of PmWiki; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published
    by the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.  See pmwiki.php for full details.

*/

## Browser cache-control.  If this is a cacheable action (e.g., browse,
## diff), then set the Last-Modified header to the time the site was 
## last modified.  If the browser has provided us with a matching 
## If-Modified-Since request header, we can return 304 Not Modified.
SDV($LastModFile,"$WorkDir/.lastmod");
if (!$LastModFile) return;

$LastModTime = @filemtime($LastModFile);
foreach(get_included_files() as $f) 
  { $v = @filemtime($f); if ($v > $LastModTime) $LastModTime = $v; }

if (@$EnableIMSCaching && in_array($action, (array)$CacheActions)) {
  $HTTPLastMod = gmdate('D, d M Y H:i:s \G\M\T',$LastModTime);
  $HTTPHeaders[] = "Cache-Control: no-cache";
  $HTTPHeaders[] = "Last-Modified: $HTTPLastMod";
  if (@$_SERVER['HTTP_IF_MODIFIED_SINCE']==$HTTPLastMod)
    { header("HTTP/1.0 304 Not Modified"); exit(); }
}

if ($NoHTMLCache 
    || !@$PageCacheDir
    || count($_POST) > 0
    || count($_GET) > 2
    || (count($_GET) == 1 && !$_GET['n'])) { $NoHTMLCache |= 1; return; }

mkdirp($PageCacheDir);
if (!file_exists("$PageCacheDir/.htaccess") 
    && $fp = @fopen("$PageCacheDir/.htaccess", "w"))
  { fwrite($fp, "Order Deny,Allow\nDeny from all\n"); fclose($fp); }
$PageCacheFile = "$PageCacheDir/$pagename,cache";
if (file_exists($PageCacheFile) && @filemtime($PageCacheFile) < $LastModTime) 
  @unlink($PageCacheFile);

