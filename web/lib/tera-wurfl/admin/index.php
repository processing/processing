<?php
require_once('../tera_wurfl_config.php');
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<title>Tera-WURFL Administration</title>
<link href="style.css" rel="stylesheet" type="text/css" />
</head>

<body>
<table width="800">
	<tr><td>
<div align="center" class="titlediv">
	<p>		Tera-WURFL Administration<br />
		<span class="version">Version <?php echo "$branch $version"; ?></span></p>
</div>
</td></tr><tr><td>
	<p>&nbsp;		</p>
	<table width="800" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<th colspan="2" scope="col">Administration</th>
		</tr>
		<tr>
			<td width="16" class="lightrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td width="744" class="lightrow"><a href="tera_wurfl_updatedb.php?source=local&type=main">Update database from local file<br />
			</a><strong>Location</strong>: <?php echo WURFL_FILE; ?><br />
			Updates your WURFL database from a local file. The location of this file is defined in <strong>tera_wurfl_config.php</strong>.</td>
		</tr>
		<tr>
			<td class="darkrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td class="darkrow"><a href="tera_wurfl_updatedb.php?source=remote&type=main">Update database from wurfl.sourceforge.net<br />
			</a><strong>Location</strong>: <?php echo WURFL_DL_URL; ?><br />			Updates your WURFL database with the <strong>current stable release</strong> from the <a href="http://wurfl.sourceforge.net">official WURFL website</a>.</td>
		</tr>
		<tr>
			<td class="lightrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td class="lightrow"><p><a href="tera_wurfl_updatedb.php?source=remote_cvs&amp;type=main">Update database from wurfl.sourceforge.net CVS<br />
			</a><strong>Location</strong>: <?php echo urldecode(htmlspecialchars(WURFL_CVS_URL)); ?><br />
			Updates your WURFL database with the <strong>current development release (CVS) </strong> from the <a href="http://wurfl.sourceforge.net">official WURFL website</a>.</p>
				</td>
		</tr>
		<tr>
			<td class="darkrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td class="darkrow"><a href="tera_wurfl_updatedb.php?source=local&type=patch">Update PATCH database from your local patch file<br />
			</a><strong>Location</strong>: <?php echo WURFL_PATCH_FILE; ?><br />
			Updates your <strong>patch</strong> database from your patch file. After you make changes to your local patch file, you need to run this task to check your local patch file and load it into the patch database.</td>
		</tr>
	</table>
	<br/>
	<br/>
	<table width="800" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<th colspan="2" scope="col">Diagnostics</th>
		</tr>
		<tr>
			<td width="16" class="lightrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td width="744" class="lightrow"><p><a href="../check_wurfl.php">Tera-WURFL  test script</a><br />
				This is	
			very similar to the <strong>check_wurfl.php</strong> script included with the <a href="http://wurfl.sourceforge.net/php/">PHP Tools</a> package. This is a good way to test your installation of Tera-WURFL and see how the class handles different user agents. </p>				</td>
		</tr>
		<tr>
			<td class="darkrow"><span class="lightrow"><img src="triangle.gif" width="10" height="11" /></span></td>
			<td class="darkrow"><a href="stats.php">Statistics, Settings, Log File </a><br />
				See statistics about your database tables with detailed descriptions,your current settings and the last errors in your log file.</td>
		</tr>
	</table>
	<br/><br/>
	<table width="800" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<th colspan="2" scope="col">Support</th>
		</tr>
		<tr>
			<td width="16" class="lightrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td width="744" class="lightrow"><a href="http://devel.teratechnologies.net/tera-wurfl/doc/tera_wurfl/tera_wurfl.html">Online Documentation</a></td>
		</tr>
		<tr>
			<td class="darkrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td class="darkrow"><a href="http://groups.yahoo.com/group/wmlprogramming/">WML Programming Mailing List</a></td>
		</tr>
		<tr>
			<td class="lightrow"><img src="triangle.gif" width="10" height="11" /></td>
			<td class="lightrow"><a href="http://www.tera-wurfl.com">www.Tera-Wurfl.com</a></td>
		</tr>
		<tr>
			<td class="darkrow"><span class="lightrow"><img src="triangle.gif" width="10" height="11" /></span></td>
			<td class="darkrow"><a href="http://www.teratechnologies.net/stevekamerman">Steve Kamerman's Blog</a> (the author :D) </td>
		</tr>
	</table>
	<p>&nbsp;</p>
	</td>
</tr></table>
</body>
</html>
