###############################################################################
# SendTopic.pl                                                                #
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

$sendtopicplver = "1 Gold - SP 1.4";

sub SendTopic {
	$topic = $INFO{'topic'};
	$board = $INFO{'board'};
	&fatal_error($txt{'709'}) unless ($board ne '' && $board ne '_' && $board ne ' ');
	&fatal_error($txt{'710'}) unless ($topic ne '' && $topic ne '_' && $topic ne ' ');

	fopen(FILE, "$datadir/$topic.txt") || &fatal_error("201 $txt{'106'}: $txt{'23'} $topic.txt");
	@messages = <FILE>;
	fclose(FILE);
	($subject) = split(/\|/,$messages[0]);

	$yymain .= qq~
<form action="$cgi;action=sendtopic2" method="post">
<table border=0  align="center" cellspacing=1 cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td width="100%" bgcolor="$color{'windowbg'}" class="windowbg">
    <table width="100%" border="0" cellspacing="0" cellpadding="3">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2">
        <img src="$imagesdir/email.gif" alt="" border="0">
        <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'707'}&nbsp; &#171; $subject &#187; &nbsp;$txt{'708'}</b></font></td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=right valign=top>
        <font size=2><B>$txt{'715'}</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=left valign=middle>
        <input type="text" name="y_name" size="20" maxlength="40" value="$settings[1]">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=right valign=top>
        <font size=2><B>$txt{'716'}</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=left valign=middle>
        <input type="text" name="y_email" size="20" maxlength="40" value="$settings[2]">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=center valign=top colspan="2">
        <hr width="100%" size="1" class="hr">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="right" valign="top">
        <font size=2><B>$txt{'717'}</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align="left" valign="middle">
        <input type="text" name="r_name" size="20" maxlength="40">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=right valign=top>
        <font size=2><B>$txt{'718'}</B></font>
        </td>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=left valign=middle>
        <input type="text" name="r_email" size="20" maxlength="40">
        </td>
      </tr><tr>
        <td bgcolor="$color{'windowbg'}" class="windowbg" align=center valign=middle colspan=2>
	<INPUT TYPE="hidden" NAME="board" VALUE="$INFO{'board'}">
	<INPUT TYPE="hidden" NAME="topic" VALUE="$INFO{'topic'}">
        <input type="submit" name="Send" value="$txt{'339'}">
        </td>
      </tr>
    </table>
    </td>
  </tr>
</table>
</form>
~;
	$yytitle = "$txt{'707'}&nbsp; &#171; $subject &#187; &nbsp;$txt{'708'}";
	&template;
	exit;

}

sub SendTopic2 {
	$topic = $FORM{'topic'};
	$board = $FORM{'board'};
	&fatal_error($txt{'709'}) unless ($board ne '' && $board ne '_' && $board ne ' ');
	&fatal_error($txt{'710'}) unless ($topic ne '' && $topic ne '_' && $topic ne ' ');

	$yname = $FORM{'y_name'};
	$rname = $FORM{'r_name'};
	$yemail = $FORM{'y_email'};
	$remail = $FORM{'r_email'};
	$yname =~ s/\A\s+//;
	$yname =~ s/\s+\Z//;
	$rname =~ s/\A\s+//;
	$rname =~ s/\s+\Z//;

	&fatal_error($txt{'75'}) unless ($yname ne '' && $yname ne '_' && $yname ne ' ');
	&fatal_error($txt{'568'}) if(length($yname) > 25);
	&fatal_error("$txt{'76'}") if($yemail eq '');
	&fatal_error("$txt{'240'} $txt{'69'} $txt{'241'}") if($yemail !~ /[\w\-\.\+]+\@[\w\-\.\+]+\.(\w{2,4}$)/);
	&fatal_error("$txt{'500'}") if(($yemail =~ /(@.*@)|(\.\.)|(@\.)|(\.@)|(^\.)|(\.$)/) || ($yemail !~ /^.+@\[?(\w|[-.])+\.[a-zA-Z]{2,4}|[0-9]{1,4}\]?$/));
	&fatal_error($txt{'75'}) unless ($rname ne '' && yname ne '_' && $rname ne ' ');
	&fatal_error($txt{'568'}) if(length($rname) > 25);
	&fatal_error("$txt{'76'}") if($remail eq '');
	&fatal_error("$txt{'240'} $txt{'69'} $txt{'241'}") if($remail !~ /[\w\-\.\+]+\@[\w\-\.\+]+\.(\w{2,4}$)/);
	&fatal_error("$txt{'500'}") if(($remail =~ /(@.*@)|(\.\.)|(@\.)|(\.@)|(^\.)|(\.$)/) || ($remail !~ /^.+@\[?(\w|[-.])+\.[a-zA-Z]{2,4}|[0-9]{1,4}\]?$/));

	fopen(FILE, "$datadir/$topic.txt") || &fatal_error("201 $txt{'106'}: $txt{'23'} $topic.txt");
	@messages = <FILE>;
	fclose(FILE);
	($subject) = split(/\|/,$messages[0]);

	&sendmail($remail,"$txt{'118'}:  $subject ($txt{'318'} $yname)","$txt{'711'} $rname,\n\n$txt{'712'}: $subject, $txt{'30'} $mbname. $txt{'713'}:\n\n$cgi;action=display;num=$topic\n\n\n$txt{'714'},\n$yname",$yemail);

	$yySetLocation = qq~$cgi;action=display;num=$topic~;
	&redirectexit;
}

1;
