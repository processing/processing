###############################################################################
# LogInOut.pl                                                                 #
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

$loginoutplver = "1 Gold - SP 1.4";

sub Login {
	$yymain .= qq~
<BR><BR>
<form action="$cgi;action=login2" method="POST" name="form">
<table border="0" width="60%" cellspacing="1" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" width="100%">
    <table width="100%" cellspacing="0" cellpadding="3">
      <tr>
        <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="3">
        <img src="$imagesdir/login.gif">
        <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'34'}</b></font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'35'}:</b></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><input type=text name="username" size=20 tabindex="1"></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><a href="$cgi;action=register"><font size="1">$txt{'753'}</font></a></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'36'}:</b></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><input type=password name="passwrd" size=20 tabindex="2"></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><a href="$cgi;action=reminder"><font size="1">$txt{'315'}</font></a></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'497'}:</b></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="2"><font size=2><input type=text name="cookielength" size=4 maxlength="4" value="$Cookie_Length" tabindex="3"></font></td>
      </tr><tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><b>$txt{'508'}:</b></font></td>
        <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="2"><font size=2><input type=checkbox name="cookieneverexp" tabindex="4" value="1" checked></font></td>
      </tr><tr>
        <td align=center colspan="3" class="windowbg" bgcolor="$color{'windowbg'}"><BR><input type=submit value="$txt{'34'}" tabindex="5" accesskey="l"></td>
      </tr><tr>
        <td align=center colspan="3" class="windowbg" bgcolor="$color{'windowbg'}"><BR></td>
      </tr>
    </table>
    </td>
  </tr>
</table>
</form>
<script language="JavaScript"> <!--
	document.form.username.focus();
//--> </script>
~;
	$yytitle = "$txt{'34'}";
	&template;
	exit;
}

sub Login2 {
	&fatal_error("$txt{'37'}") if($FORM{'username'} eq "");
	&fatal_error("$txt{'38'}") if($FORM{'passwrd'} eq "");
	$FORM{'username'} =~ s/\s/_/g;
	$username = $FORM{'username'};
	&fatal_error("$txt{'240'} $txt{'35'} $txt{'241'}") if($username !~ /^[\s0-9A-Za-z#%+,-\.:=?@^_]+$/);
	&fatal_error("$txt{'337'}") if($FORM{'cookielength'} !~ /^[0-9]+$/);

	if(-e("$memberdir/$username.dat")) {
		fopen(FILE, "$memberdir/$username.dat");
		@settings = <FILE>;
		fclose(FILE);
 		$settings[0] =~ s/[\n\r]//g;
		if($settings[0] ne "$FORM{'passwrd'}") { $username = "Guest"; &fatal_error("$txt{'39'}"); }
		$settings[0] = "$settings[0]\n";
	}
	else { $username = "Guest"; &fatal_error("$txt{'40'}"); }

	if($FORM{'cookielength'} < 1 || $FORM{'cookielength'} > 9999) { $FORM{'cookielength'} = $Cookie_Length; }
	if(!$FORM{'cookieneverexp'}) { $ck{'len'} = "\+$FORM{'cookielength'}m"; }
	else { $ck{'len'} = 'Sunday, 17-Jan-2038 00:00:00 GMT'; }
	$cryptsession = &encode_session($user_ip,$masterseed);
	$cryptsession .= $masterseed;
	$password = crypt("$FORM{'passwrd'}",$masterseed);
	$password .= $masterseed;

	## usage: &UpdateCookie ("delete or write",<userid>,<password>,<session>,<path>,<expiration>); ##
	&UpdateCookie("write","$username","$password","$cryptsession","/","$ck{'len'}");
	&LoadUserSettings;
	if ($maintenance && $settings[7] ne 'Administrator') {$username = 'Guest'; &fatal_error($txt{'774'});}
	&WriteLog;
	&redirectinternal;
}

sub Logout {
	# Write log
	fopen(LOG, "$vardir/log.txt");
	my @entries = <LOG>;
	fclose(LOG);
	fopen(LOG, ">$vardir/log.txt", 1);
	$field = $username;
	foreach $curentry (@entries) {
	        $curentry =~ s/\n//g;
     		($name, $value) = split(/\|/, $curentry);
	        if($name ne $field) { print LOG "$curentry\n"; }
	}
	fclose(LOG);

	## usage: &UpdateCookie ("delete or write",<userid>,<password>,<session>,<path>,<expiration>); ##
	&UpdateCookie("delete");
	$username = 'Guest';
	$password = '';
	@settings = ();
	@immessages = ();
	$yyim = "";
	$realname = '';
	$realemail = '';
	$ENV{'HTTP_COOKIE'} = '';
	&LoadIMs;	# Load IM's (to blank)
	$yyuname = "";
	&redirectinternal;
}

sub sharedLogin {
	if ($sharedLogin_title ne "") {$yymain .= qq~<tr><td class="catbg" bgcolor="$color{'catbg'}" colspan="2"><font size="2" class="catbg"><b>$sharedLogin_title</b></font></td></tr>~;}
	if ($sharedLogin_text ne "") {$yymain .= qq~<tr><td colspan="2" class="windowbg" bgcolor="$color{'windowbg'}" align="left" cellpadding=3><font size=2>$sharedLogin_text</font></td></tr>~;}
	$yymain .= qq~<tr>
        <td class="windowbg" bgcolor="$color{'windowbg'}" width="20" valign=middle align=center><img src="$imagesdir/login.gif" border="0" alt=""></td>
        <td class="windowbg2" bgcolor="$color{'windowbg2'}" valign="middle"><BR>
        <form action="$cgi;action=login2" method="POST">
        <table border="0" cellpadding="2" cellspacing="0" valign="middle" align="center" width="90%">
          <tr>
            <td valign="middle" align="left"><font size=2><b>$txt{'35'}:</b></font></td>
            <td valign="middle" align="left"><font size=2><input type=text name="username" size="15" tabindex="1"></font></td>
            <td valign="middle" align="left"><font size=2><b>$txt{'497'}:</b></font></td>
            <td valign="middle" align="left"><font size=2><input type=text name="cookielength" size="7" maxlength="4" value="$Cookie_Length" tabindex="3"> </font></td>
          </tr><tr>
            <td valign="middle" align="left"><font size=2><b>$txt{'36'}:</b></font></td>
            <td valign="middle" align="left"><font size=2><input type=password name="passwrd" size="15" tabindex="2"></font></td>
            <td valign="middle" align="left"><font size=2><b>$txt{'508'}:</b></font></td>
            <td valign="middle" align="left"><font size=2><input type=checkbox name="cookieneverexp" tabindex="4" checked>&nbsp;&nbsp;</font> <input type=submit value="$txt{'34'}" tabindex="5" accesskey="l"></font></td>
            <td valign="middle" align="left"><a href="$cgi;action=reminder"><font size="1">$txt{'315'}</font></a></td>
          </tr>
        </table>
        </form>
       </td>
      </tr>~;
	$sharedLogin_title=""; $sharedLogin_text="";
}

sub Reminder {
	$yymain .= qq~
<BR><BR><table border=0 width=400 cellspacing=1 bgcolor="$color{'bordercolor'}" align="center" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <font size="2" class="text1" color="$color{'titletext'}"><b>$mbname $txt{'36'} $txt{'194'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}">
    <form action="$cgi;action=reminder2" method="POST">
    <table border=0 align="center">
      <tr>
        <td><font size="2">$txt{'35'}: <input type="text" name="user">
        <input type="submit" value="$txt{'339'}"></font></td>
      </tr>
    </table>
    </form>
    </td>
  </tr>
</table>
~;
$yytitle = "$txt{'669'}";
&template;
exit;
}

sub Reminder2 {
$user = $FORM{'user'};
$user =~ s/\s/_/g;

fopen(FILE, "$memberdir/$user.dat") || &fatal_error("<b>$txt{'40'}</b>");
@member=<FILE>;
fclose(FILE);
$password = $member[0];
$name = $member[1];
$email = $member[2];
$status = $member[7];

chomp($name);
chomp($email);
chomp($password);
chomp($status);

$subject = "$txt{'36'} $mbname : $name";
&sendmail($email, $subject, qq~$txt{'711'} $name,\n\n$mbname ==>\n\n$txt{'35'}: $user\n$txt{'36'}: $password\n$txt{'87'}: $status\n\n$txt{'130'}~);

$yymain .= qq~
<br><br><table border="0" width="400" cellspacing="1" bgcolor="$color{'windowbg'}" class="windowbg" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <font size="2" class="text1" color="$color{'titletext'}"><b>$mbname $txt{'36'} $txt{'194'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}">
    <table border=0 align="center">
      <tr>
        <td align="center"><font size="2"><b>$txt{'192'}: $user</b></font></td>
      </tr>
    </table>
    </td>
  </tr>
</table>
<br><center><a href="javascript:history.back(-2)">$txt{'193'}</a></center><br>
~;
$yytitle = "$txt{'669'}";
&template;
exit;
}

1;