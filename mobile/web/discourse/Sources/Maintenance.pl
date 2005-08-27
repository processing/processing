###############################################################################
# Maintenance.pl                                                              #
###############################################################################
#                                                                             #
# YaBB: Yet another Bulletin Board                                            #
# Open-Source Community Software for Webmasters                               #
#                                                                             #
# Version:        YaBB 1 Gold - SP 1.3.2                                      #
# Released:       December 2001; Updated August 17, 2004                      #
# Distributed by: http://www.yabbforum.com                                    #
#                                                                             #
# =========================================================================== #
#                                                                             #
# Copyright (c) 2000-2004 YaBB (www.yabbforum.com) - All Rights Reserved.     #
#                                                                             #
# Software by:  The YaBB Development Team                                     #
#               with assistance from the YaBB community.                      #
# Sponsored by: Xnull Internet Media, Inc. - http://www.ximinc.com            #
#               Your source for web hosting, web design, and domains.         #
#                                                                             #
###############################################################################

$maintenanceplver = "1 Gold - SP 1.3.2";

sub InMaintenance
{
	if ($maintenancetext ne "") { $txt{'157'} = $maintenancetext; }
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
 <tr>
  <td class="titlebg" bgcolor="$color{'titlebg'}">
   <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'156'}</b></font>
  </td>
 </tr><tr>
  <td class="windowbg" bgcolor="$color{'windowbg'}">
   <font size=2>
    <br>
    $txt{'157'}
    <br>&nbsp;
   </font>
  </td>
 </tr>
</table>

<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
~;
	require "$sourcedir/LogInOut.pl";
	$sharedLogin_title="$txt{'114'}";
	&sharedLogin;

	$yymain .= qq~</table>~;
	$yytitle = "$txt{'155'}";
	&template;
	exit;
}

1;
