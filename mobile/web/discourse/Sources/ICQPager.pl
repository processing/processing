###############################################################################
# ICQPager.pl                                                                 #
###############################################################################
#                                                                             #
# YaBB: Yet another Bulletin Board                                            #
# Open-Source Community Software for Webmasters                               #
#                                                                             #
# Version:        YaBB 1 Gold - SP 1.4                                        #
# Released:       December 2001; Updated November 25, 2004                    #
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

$icqpagerplver = "1 Gold - SP 1.4";

sub IcqPager
{
if ($realname eq '') {$settofield="from";} else {$settofield="body";}
	$uin = $INFO{'UIN'};
$yymain .= qq~
<form action="http://web.icq.com/whitepages/page_me/1,,,00.html" name="form" method="post">
<table border="0" width="600" align="center" cellspacing="1" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td width="100%" bgcolor="$color{'windowbg'}" class="windowbg">
    <table width="100%" border="0" cellspacing="0" cellpadding="3">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2">
        <font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'513'} $txt{'514'}</b></font></td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=left valign=top>
        <font size="2"><B>$txt{'324'}:</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=left valign=middle>
        <img src="http://web.icq.com/whitepages/online?icq=$uin&img=5" alt="$uin" border="0"> <font size="2">$uin</font>
        </td>
      </tr><tr>
      <td colspan="2" height="2" bgcolor="$color{'bordercolor'}" class="bordercolor"></td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="left" valign="top">
        <font size="2"><B>$txt{'335'}:</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="left" valign="middle">
        <input type="text" value="$realname" name="from" size="20" maxlength="40">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="left" valign="top">
        <font size="2"><B>$txt{'336'}:</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="left" valign="middle">
        <input type="text" value="$realemail" name="fromemail" size="20" maxlength="40">
        </td>
      </tr><tr>
      <td colspan="2" height="2" bgcolor="$color{'bordercolor'}" class="bordercolor"></td>
      </tr><tr>
        <td  class="windowbg2" bgcolor="$color{'windowbg2'}" align="left" valign="top">
        <font size="2"><B>$txt{'72'}:</B></font>
        </td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" align="left" valign="middle">
        <textarea name="body" rows="10" cols="50" wrap="Virtual"></textarea>
        </td>
      </tr><tr>
      <td colspan="2" height="2" bgcolor="$color{'bordercolor'}" class="bordercolor"></td>
      </tr><tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" align="center" valign="middle" colspan="2">
        <input type="hidden" name="subject" value="$mbname">
	<input type="hidden" name="to" value="$INFO{'UIN'}">
        <font size="1" class="text1" color="$color{'titletext'}"><font size="1">$txt{'770'}</font></font><BR>
        <input type="submit" name="Send" value="$txt{'339'}" accesskey="s">
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>
</form>
<script language="JavaScript"> <!--
	document.form.$settofield.focus();
//--> </script>
~;
	$yytitle = "$txt{'513'} $txt{'514'}";
	&template;
	exit;

}

1;