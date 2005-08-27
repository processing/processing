###############################################################################
# Register.pl                                                                 #
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

$registerplver = "1 Gold - SP 1.4";

sub Register {
	$yymain .= qq~
<form action="$cgi;action=register2" method="POST" name="creator">
<table border=0 width="100%" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" cellpadding="2">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/register.gif" alt="$txt{'97'}" border="0"> <font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'97'}</b> $txt{'517'}</font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" width="100%"><font size="2">
    <table cellpadding="3" cellspacing="0" border=0 width="100%">
      <tr>
        <td width="40%"><font size=2>* <b>$txt{'98'}:</b></font>
        <BR><font size="1">$txt{'520'}</font></td>
        <td><input type=text name=username size=20 maxlength="18"></td>
      </tr><tr>
        <td width="40%"><font size=2>* <b>$txt{'69'}:</b></font>
        <BR><font size="1">$txt{'679'}</font></td>
~;
if ($allow_hide_email == 1) { $yymain .= qq~
        <td><font size=2><input type=text maxlength="40" name=email size=30> <input type="checkbox" name="hideemail" value="checked" checked> $txt{'721'}</font></td>
~;
} else { $yymain .= qq~
        <td><input type=text name=email size=30>
        <BR><font size="1">$txt{'679'}</font></td>
~;
}
$yymain .= qq~
      </tr>
~;
	unless( $emailpassword ) {
		$yymain .= qq~
      <tr>
        <td width="40%"><font size=2>* <b>$txt{'81'}:</b></font></td>
        <td><font size=2><input type=password maxlength="30" name=passwrd1 size=30></font></td>
      </tr><tr>
        <td width="40%"><font size=2>* <b>$txt{'82'}:</b></font></td>
        <td><font size=2><input type=password maxlength="30" name=passwrd2 size=30></font></td>
      </tr>
~;
}
$yymain .= qq~
    </table>
    </font></td>
  </tr>
</table>
~;
if ($RegAgree) {
	fopen(FILE, "$vardir/agreement.txt");
	@agreement = <FILE>;
	fclose(FILE);
	$fullagree = join( "", @agreement );
	$fullagree =~ s/\n/<BR>/g;
	$yymain .= qq~
<table border=0 cellspacing=1 cellpadding="5" bgcolor="$color{'bordercolor'}" class="bordercolor" width="100%" align="center">
  <tr>
    <td bgcolor="$color{'windowbg2'}" class="windowbg2">
    <font size="2"><br>$fullagree<br><br></font>
    </td>
  </tr><tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg" align="center"><font size="2">
    <B>$txt{'585'}</B> <input type=radio name=regagree value="yes">
    &nbsp;&nbsp;&nbsp; <B>$txt{'586'}</B> <input type=radio name=regagree value="no" checked>
    </font></td>
  </tr>
</table>
~;
}
	$yymain .= qq~
<BR><center><input type=submit value="$txt{'97'}"></center>
</form>
<script language="JavaScript"> <!--
	document.creator.username.focus();
//--> </script>
~;
	$yytitle = "$txt{'97'}";
	&template;
	exit;
}

sub Register2 {
	if($FORM{'regagree'} eq "no") {
		$yySetLocation = qq~$scripturl~;
		&redirectexit;
	}
	my %member;
	while( ($key,$value) = each(%FORM) ) {
		$value =~ s~\A\s+~~;
		$value =~ s~\s+\Z~~;
		$value =~ s~[\n\r]~~g;
		$member{$key} = $value;
	}
	$member{'username'} =~ s/\s/_/g;
	if (length($member{'username'}) > 25) { $member{'username'} = substr($member{'username'},0,25); }
	&fatal_error("($member{'username'}) $txt{'37'}") if($member{'username'} eq '');
	&fatal_error("($member{'username'}) $txt{'99'}") if($member{'username'} eq '_' || $member{'username'} eq '|');
	&fatal_error("$txt{'244'} $member{'username'}") if($member{'username'} =~ /guest/i);
	&fatal_error("$txt{'240'} $txt{'35'} $txt{'241'}") if($member{'username'} !~ /\A[0-9A-Za-z#%+-\.@^_]+\Z/);
	&fatal_error("$txt{'240'}") if($member{'username'} =~ /,/);
	&fatal_error("($member{'username'}) $txt{'76'}") if($member{'email'} eq "");
	&fatal_error("($member{'username'}) $txt{'100'}") if(-e ("$memberdir/$member{'username'}.dat"));

	if( $emailpassword ) {
		srand();
		$member{'passwrd1'} = int( rand(100) );
		$member{'passwrd1'} =~ tr/0123456789/ymifxupbck/;
		$_ = int( rand(77) );
		$_ =~ tr/0123456789/q8dv7w4jm3/;
		$member{'passwrd1'} .= $_;
		$_ = int( rand(89) );
		$_ =~ tr/0123456789/y6uivpkcxw/;
		$member{'passwrd1'} .= $_;
		$_ = int( rand(188) );
		$_ =~ tr/0123456789/poiuytrewq/;
		$member{'passwrd1'} .= $_;
		$_ = int( rand(65) );
		$_ =~ tr/0123456789/lkjhgfdaut/;
		$member{'passwrd1'} .= $_;
	} else {
		&fatal_error("($member{'username'}) $txt{'213'}") if($member{'passwrd1'} ne $member{'passwrd2'});
		&fatal_error("($member{'username'}) $txt{'91'}") if($member{'passwrd1'} eq '');
		&fatal_error("$txt{'240'} $txt{'36'} $txt{'241'}") if($member{'passwrd1'} !~ /\A[\s0-9A-Za-z!@#$%\^&*\(\)_\+|`~\-=\\:;'",\.\/?\[\]\{\}]+\Z/);
	}
	&fatal_error("$txt{'240'} $txt{'69'} $txt{'241'}") if($member{'email'} !~ /[\w\-\.\+]+\@[\w\-\.\+]+\.(\w{2,4}$)/);
	&fatal_error("$txt{'500'}") if(($member{'email'} =~ /(@.*@)|(\.\.)|(@\.)|(\.@)|(^\.)|(\.$)/) || ($member{'email'} !~ /\A.+@\[?(\w|[-.])+\.[a-zA-Z]{2,4}|[0-9]{1,4}\]?\Z/));
	fopen(FILE, "$vardir/ban_email.txt");
	@banned = <FILE>;
	fclose(FILE);
	foreach $curban (@banned) {
		if($member{'email'} eq "$curban") { &fatal_error("$txt{'678'}$txt{'430'}!"); }
	}


	fopen(FILE, "$memberdir/memberlist.txt");
	@memberlist = <FILE>;
	fclose(FILE);
	$testname = lc $member{'username'};
	$testemail = lc $member{'email'};

	$doublecheck = &profilecheck($member{'username'},$testname,$testemail,"check");
	if($doublecheck eq "email_exists"){&fatal_error("$txt{'730'} ($member{'email'}) $txt{'731'}");}
	if($doublecheck eq "realname_exists"){&fatal_error("($member{'username'}) $txt{'473'}");}
	&ToHTML($member{'email'});

	fopen(FILE, "$vardir/reserve.txt") || &fatal_error("$txt{'23'} reserve.txt");
	@reserve = <FILE>;
	fclose(FILE);
	fopen(FILE, "$vardir/reservecfg.txt") || &fatal_error("$txt{'23'} reservecfg.txt");
	@reservecfg = <FILE>;
	fclose(FILE);
	for( $a = 0; $a < @reservecfg; $a++ ) {
		chomp $reservecfg[$a];
	}
	$matchword = $reservecfg[0] eq 'checked';
	$matchcase = $reservecfg[1] eq 'checked';
	$matchuser = $reservecfg[2] eq 'checked';
	$matchname = $reservecfg[3] eq 'checked';
	$namecheck = $matchcase eq 'checked' ? $member{'username'} : lc $member{'username'};

	foreach $reserved (@reserve) {
		chomp $reserved;
		$reservecheck = $matchcase ? $reserved : lc $reserved;
		if ($matchuser) {
			if ($matchword) {
				if ($namecheck eq $reservecheck) { &fatal_error("$txt{'244'} $reserved"); }
			}
			else {
				if ($namecheck =~ $reservecheck) { &fatal_error("$txt{'244'} $reserved"); }
			}
		}
	}

	&fatal_error("$txt{'100'})") if(-e ("$memberdir/$member{'username'}.dat"));
	fopen(FILE, ">$memberdir/$member{'username'}.dat");
	print FILE "$member{'passwrd1'}\n";
	print FILE "$member{'username'}\n";
	print FILE "$member{'email'}\n";
	print FILE "\n\n\n0\n\n\n\n\n\n$txt{'209'}\nblank.gif\n$date\n\n\n\n\n";
	if ($FORM{'hideemail'} ne "checked") { $FORM{'hideemail'} = ""; }
	print FILE "$FORM{'hideemail'}\n";
	fclose(FILE);
	## fixed original code writing empty lines and removing any if present
	fopen(FILE, ">$memberdir/memberlist.txt", 1);
	foreach (@memberlist){
		chomp $_;
		if ($_ ne ""){print FILE "$_\n"; }
	}
	print FILE "$member{'username'}\n";
	fclose(FILE);

	## add to profile checklist ##
	fopen(FILE, ">>$memberdir/profiles.txt", 1);
	print FILE "$member{'username'}|$member{'username'}|$member{'email'}\n";
	fclose(FILE);

	my $membershiptotal = @memberlist + 1;
	fopen(FILE, "+>$memberdir/members.ttl");
	print FILE qq~$membershiptotal|$member{'username'}~;
	fclose(FILE);
	&FormatUserName($member{'username'});

	if($emailpassword) {
		&sendmail($member{'email'},"$txt{'700'} $mbname", "$txt{'248'} $member{'username'}!\n\n$txt{'719'} $member{'username'}, $txt{'492'} $member{'passwrd1'}\n\n$txt{'701'}\n$scripturl?action=profile;username=$useraccount{$member{'username'}}\n\n$txt{'130'}");
		$yymain .= qq~<BR><table border=0 width=100% cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">~;
		require "$sourcedir/LogInOut.pl";
		$sharedLogin_title="$txt{'97'}";
		$sharedLogin_text="$txt{'703'}";
		&sharedLogin;
		$yymain .= qq~</table>~;
	}
	else {
	if($emailwelcome) {
		&sendmail($member{'email'},"$txt{'700'} $mbname", "$txt{'248'} $member{'username'}!\n\n$txt{'719'} $member{'username'}, $txt{'492'} $member{'passwrd1'}\n\n$txt{'701'}\n$scripturl?action=profile;username=$useraccount{$member{'username'}}\n\n$txt{'130'}");
	}
		$yymain .= qq~
<BR><BR>
<table border=0 width=300 cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/register.gif" alt="$txt{'97'}" border="0"> <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'97'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" align="center"><font size=2><form action="$cgi;action=login2" method="POST">
    <BR>$txt{'431'}<BR><BR>
    <input type=hidden name="username" value="$member{'username'}">
    <input type=hidden name="passwrd" value="$member{'passwrd1'}">
    <input type=hidden name="cookielength" value="$Cookie_Length">
    <input type=submit value="$txt{'34'}">
    </form></font></td>
  </tr>
</table>
<BR><BR>
~;
}
	$yytitle="$txt{'245'}";
	&template;
	exit;
}

1;