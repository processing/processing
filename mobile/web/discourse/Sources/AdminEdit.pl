###############################################################################
# AdminEdit.pl                                                                #
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

$admineditplver = "1 Gold - SP 1.4";

sub EditNews {
	&is_admin;
	my($line);
	$yymain .= qq~
<form action="$cgi;action=editnews2" method="POST">
<table border="0" width="70%" cellspacing="1" cellpadding="3" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/xx.gif" alt="">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'7'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><BR><font size="1">$txt{'670'}</font><BR><BR></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><BR>
    <font size=2>
    <textarea cols=70 rows=8 name="news">
~;
	fopen(FILE, "$vardir/news.txt");
	while($line = <FILE>) { chomp $line; $yymain .= qq~$line~; }
	fclose(FILE);
	$yymain .= qq~
</textarea><br><input type="submit" value="$txt{'10'}"></font><BR></td>
  </tr>
</table>
</form>
~;
	$yytitle = "$txt{'7'}";
	&template;
	exit;
}

sub EditNews2 {
	&is_admin;
	fopen(FILE, ">$vardir/news.txt", 1);
	chomp $FORM{'news'};
	print FILE "$FORM{'news'}";
	fclose(FILE);
	$yySetLocation = qq~$cgi\;action=admin~;
	&redirectexit;
}

sub EditMemberGroups {
	&is_admin;
	my(@lines, $mgroups, $i);
	fopen(FILE, "$vardir/membergroups.txt");
	@lines = <FILE>;
	fclose(FILE);
	foreach $i (@lines) {
		$i =~ tr/\r//d;
		$i =~ tr/\n//d;
	}
	for( $i = 7; $i < @lines; ++$i ) {
		if( $lines[$i] =~ m~\A\s+\Z~ ) { next; }
		$mgroups .= "$lines[$i]\n";
	}
	$yymain .= qq~
<table border="0" width="600" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center" cellpadding="4">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/guest.gif" alt="" border="0">
    <font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'8'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    <form action="$cgi;action=modmemgr2" method="POST">
    <table border="0" cellpadding="1" cellspacing="0">
      <tr>
        <td align="right"><font size=2><b>$txt{'11'}:</b></font></td>
        <td><input type=text name="admin" size=30 value="$lines[0]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'12'}:</b></font></td>
        <td><input type=text name="moderator" size=30 value="$lines[1]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'569'}:</b></font></td>
        <td><input type=text name="newbie" size=30 value="$lines[2]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'13'}:</b></font></td>
        <td><input type=text name="junior" size=30 value="$lines[3]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'14'}:</b></font></td>
        <td><input type=text name="full" size=30 value="$lines[4]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'15'}:</b></font></td>
        <td><input type=text name="senior" size=30 value="$lines[5]"></td>
      </tr><tr>
        <td align="right"><font size=2><b>$txt{'570'}:</b></font></td>
        <td><input type=text name="god" size=30 value="$lines[6]"></td>
      </tr><tr>
        <td align="right"><font size=2><B>$txt{'16'}:</b></font></td>
        <td><textarea name="additional" cols=30 rows=5>$mgroups</textarea><BR>
        <center><input type="submit" value="$txt{'10'}"></center></td>
      </tr>
    </table>
    </form>
    </font></td>
  </tr>
</table>
~;
	$yytitle = $txt{'8'};
	&template;
	exit;
}

sub EditMemberGroups2 {
	&is_admin;
	my $additional = $FORM{'additional'};
	while( $groups = each(%FORM) ) {
		$FORM{$groups} =~ tr/\n//d;
		$FORM{$groups} =~ tr/\r//d;
	}
	fopen(FILE, ">$vardir/membergroups.txt", 1);
	print FILE "$FORM{'admin'}\n";
	print FILE "$FORM{'moderator'}\n";
	print FILE "$FORM{'newbie'}\n";
	print FILE "$FORM{'junior'}\n";
	print FILE "$FORM{'full'}\n";
	print FILE "$FORM{'senior'}\n";
	print FILE "$FORM{'god'}\n";
	print FILE "$additional";
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

sub SetCensor {
	&is_admin;
	my( @censored, $i );
	fopen(FILE, "$vardir/censor.txt");
	@censored = <FILE>;
	fclose(FILE);
	foreach $i (@censored) {
		$i =~ tr/\r//d;
		$i =~ tr/\n//d;
	}
	$yymain .= qq~
<table border="0" width="300" cellspacing="1" cellpadding="4" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/ban.gif" alt="" border="0">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'135'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" align="center"><font size=2>
    <form action="$cgi;action=setcensor2" method="POST">
    $txt{'136'}<br>
    <textarea cols=55 rows=15 name="censored">
~;
	foreach $i (@censored) {
		unless( $i && $i =~ m~.+\=.+~ ) { next; }
		$yymain .= "$i\n";
	}
	$yymain .= qq~
</textarea><br><BR>
    <input type=submit value="$txt{'10'}"></form></font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'135'}";
	&template;
	exit;
}

sub SetCensor2 {
	&is_admin;
	$FORM{'censored'} =~ tr/\r//d;
	$FORM{'censored'} =~ s~\A[\s\n]+~~;
	$FORM{'censored'} =~ s~[\s\n]+\Z~~;
	$FORM{'censored'} =~ s~\n\s*\n~\n~g;
	my @lines = split( /\n/, $FORM{'censored'} );
	fopen(FILE, ">$vardir/censor.txt", 1);
	foreach my $i (@lines) {
		$i =~ tr/\n//d;
		unless( $i && $i =~ m~.+\=.+~ ) { next; }
		print FILE "$i\n";
	}
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

sub SetReserve {
	my( @reserved, @reservecfg, $i );
	&is_admin;
	fopen(FILE, "$vardir/reserve.txt");
	@reserved = <FILE>;
	fclose(FILE);
	fopen(FILE, "$vardir/reservecfg.txt");
	@reservecfg = <FILE>;
	fclose(FILE);
	for(my $i = 0; $i < @reservecfg; $i++) { chomp $reservecfg[$i]; }

	$yymain .= qq~
<table border=0 cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center" cellpadding="4" width="580">
  <tr>
    <td bgcolor="$color{'titlebg'}" class="titlebg">
    <img src="$imagesdir/profile.gif" alt="" border="0">
    <font size="2" class="text1" color="$color{'titletext'}"><b>$txt{'341'}</b></font></td>
  </tr><tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg">
    <font size="1"><BR>$txt{'699'}<BR><BR></font></td>
  </tr><tr>
    <td bgcolor="$color{'windowbg2'}" class="windowbg2"><font size=2>
    <form action="$cgi;action=setreserve2" method="POST">
    <center>$txt{'342'}<br>
    <textarea cols=30 rows=6 name="reserved">
~;
	foreach $i (@reserved) {
		chomp $i;
		$i =~ s~\t~~g;
		if( $i !~ m~\A[\S|\s]*[\n\r]*\Z~) { next; }
		$yymain .= "$i\n";
	}
	$yymain .= qq~</textarea></center><br>
	<font size=2><input type=checkbox name="matchword" value="checked" $reservecfg[0]></font>
	<font size=2>$txt{'726'}</font><br>
	<font size=2><input type=checkbox name="matchcase" value="checked" $reservecfg[1]></font>
	<font size=2>$txt{'727'}</font><br>
	<font size=2><input type=checkbox name="matchuser" value="checked" $reservecfg[2]></font>
	<font size=2>$txt{'728'}</font><br>
	<font size=2><input type=checkbox name="matchname" value="checked" $reservecfg[3]></font>
	<font size=2>$txt{'729'}</font><br>
	<center><input type=submit value="$txt{'10'}"></center></form></font></td>
</tr>
</table>
~;
	$yytitle = "$txt{'341'}";
	&template;
	exit;
}

sub SetReserve2 {
	&is_admin;
	$FORM{'reserved'} =~ tr/\r//d;
	$FORM{'reserved'} =~ s~\A[\s\n]+~~;
	$FORM{'reserved'} =~ s~[\s\n]+\Z~~;
	$FORM{'reserved'} =~ s~\n\s*\n~\n~g;
	fopen(FILE, ">$vardir/reserve.txt", 1);
	my $matchword = $FORM{'matchword'} eq 'checked' ? 'checked' : '';
	my $matchcase = $FORM{'matchcase'} eq 'checked' ? 'checked' : '';
	my $matchuser = $FORM{'matchuser'} eq 'checked' ? 'checked' : '';
	my $matchname = $FORM{'matchname'} eq 'checked' ? 'checked' : '';
	print FILE $FORM{'reserved'};
	fclose(FILE);
	fopen(FILE, "+>$vardir/reservecfg.txt");
	print FILE "$matchword\n";
	print FILE "$matchcase\n";
	print FILE "$matchuser\n";
	print FILE "$matchname\n";
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

sub ModifyTemplate {
	&is_admin;
	my( $fulltemplate, $line );
	fopen(FILE, "$boarddir/template.html");
	while( $line = <FILE> ) {
		$line =~ s~[\r\n]~~g;
		&FromHTML;
		$fulltemplate .= qq~$line\n~;
	}
	fclose(FILE);
	$yymain .= qq~
<table border=0 width="100%" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" cellpadding="4">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/xx.gif" alt="" border="0">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'216'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}">
    <BR><font size="1">$txt{'682'}</font><BR><BR></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><font size=2>
    <form action="$cgi;action=modtemp2" method="POST"><BR>
    <textarea rows=30 cols=95 wrap=virtual name="template" style="width:98%">$fulltemplate</textarea>
    <br><BR><input type=submit value="$txt{'10'}"></form></font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'216'}";
	&template;
	exit;
}

sub ModifyTemplate2 {
	&is_admin;
	$FORM{'template'} =~ tr/\r//d;
	$FORM{'template'} =~ s~\A\n~~;
	$FORM{'template'} =~ s~\n\Z~~;
	fopen(FILE, ">$boarddir/template.html");
	print FILE $FORM{'template'};
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

sub ModifyAgreement {
	&is_admin;
	my( $fullagreement, $line );
	fopen(FILE, "$vardir/agreement.txt");
	while( $line = <FILE> ) {
		$line =~ tr/[\r\n]//d;
		&FromHTML($line);
		$fullagreement .= qq~$line\n~;
	}
	fclose(FILE);
	$yymain .= qq~
<table border=0 width="100%" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" cellpadding="4">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}">
    <img src="$imagesdir/xx.gif" alt="" border="0">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'764'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}">
    <BR><font size="1">$txt{'765'}</font><BR><BR></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><font size=2>
    <form action="$cgi;action=modagreement2" method="POST"><BR>
    <textarea rows=30 cols=95 wrap=virtual name="agreement" style="width:98%">$fullagreement</textarea>
    <br><BR><input type=submit value="$txt{'10'}"></form></font></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'764'}";
	&template;
	exit;
}

sub ModifyAgreement2 {
	&is_admin;
	$FORM{'agreement'} =~ tr/\r//d;
	$FORM{'agreement'} =~ s~\A\n~~;
	$FORM{'agreement'} =~ s~\n\Z~~;
	fopen(FILE, ">$vardir/agreement.txt");
	print FILE $FORM{'agreement'};
	fclose(FILE);
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

sub ModifySettings {
	&is_admin;
	my($mainchecked, $guestaccchecked, $forcechecked, $blankchecked, $agreechecked, $mailpasschecked, $newpasschecked, $welchecked);
	my($menuchecked, $ubbcchecked, $aluchecked, $cpchecked, $pbchecked, $insertchecked, $newschecked, $gpchecked, $notifchecked);
	my($ahmchecked, $slmchecked, $srbarchecked, $smbarchecked, $smreadchecked, $smodchecked, $supicchecked, $sutextchecked, $sgichecked);
	my($snfchecked, $fls1, $fls2, $fls3, $utfchecked, $truncchecked, $mts1, $mts2, $mts3, $tsl6, $tsl5, $tsl4, $tsl3, $tsl2, $tsl1);

	# figure out what to print
	if ($maintenance) { $mainchecked = ' checked'; }
	if ($guestaccess == 0) { $guestaccchecked = ' checked'; }
	if($RegAgree) { $agreechecked = " checked"; }
	if($emailpassword) { $mailpasschecked = " checked"; }
	if($emailnewpass) { $newpasschecked = " checked"; }
	if($emailwelcome) { $welchecked = " checked"; }
	if ($MenuType) { $menuchecked = ' checked'; }
	if ($enable_ubbc) { $ubbcchecked = ' checked'; }
	if ($autolinkurls) { $aluchecked = ' checked'; }
	if ($curposlinks) { $cpchecked = ' checked'; }
	if ($profilebutton) { $pbchecked = ' checked'; }
	if ($enable_news) { $newschecked = "checked" }
	if ($enable_guestposting) { $gpchecked = "checked" }
	if ($enable_notification) { $notifchecked = "checked" }
	if ($allow_hide_email) { $ahmchecked = "checked" }
	if ($showlatestmember) { $slmchecked = "checked" }
	if ($Show_RecentBar) { $srbarchecked = "checked" }
	if ($showmarkread) { $smreadchecked = "checked" }
	if ($showmodify) { $smodchecked = "checked" }
	if ($ShowBDescrip) { $bdescripchecked = "checked" }
	if ($showuserpic) { $supicchecked = "checked" }
	if ($showusertext) { $sutextchecked = "checked" }
	if ($showgenderimage) { $sgichecked = "checked" }
	if ($shownewsfader) { $snfchecked = "checked" }
	if ($showyabbcbutt) { $syabbcchecked = "checked" }
	if ($allowpics) { $allowpicschecked = "checked" }
	if ($use_flock == 0) { $fls1 = " selected" } elsif ($use_flock == 1) { $fls2 = " selected" } elsif ($use_flock == 2) { $fls3 = " selected" }
	$utfchecked = $usetempfile ? ' checked' : '';
	$truncchecked = $faketruncation ? ' checked' : '';
	if ($mailtype == 0) { $mts1 = ' selected'; } elsif ($mailtype == 1) { $mts2 = ' selected'; } elsif( $mailtype == 2 ) { $mts3 = ' selected'; }
	if ($timeselected == 6) { $tsl6 = " selected" } elsif ($timeselected == 5) { $tsl5 = " selected" } elsif ($timeselected == 4) { $tsl4 = " selected" } elsif ($timeselected == 3) { $tsl3 = " selected" } elsif ($timeselected == 2) { $tsl2 = " selected" } else { $tsl1 = " selected" }

	$yymain .= qq~
<form action="$cgi;action=modsettings2" method="POST">
<table width="90%" border="0" cellspacing="1" cellpadding="0" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
  <td>
  <table border="0" cellspacing="0" cellpadding="4" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}" colspan=2>
    <img src="$imagesdir/preferences.gif" alt="" border="0">
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'222'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" colspan=2><BR><font size="1">$txt{'347'}</font><BR><BR></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'350'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="mbname" size="35" value="$mbname"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'351'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="boardurl" size="45" value="$boardurl"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'349'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><select name="language">
~;
opendir(DIR, "$boarddir") || die "$txt{'230'} ($boarddir) :: $!";
@contents = readdir(DIR);
closedir(DIR);
foreach $line (@contents){
	($name, $extension) = split (/\./, $line);
	if ($extension eq "lng"){
		$selected = "";
		if ($line eq $language) { $selected = " selected" }
		$yymain .= "    <option value=\"$line\"$selected>$name\n";
	}
}
$yymain .= qq~
</select></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'348'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="maintenance"$mainchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'348Text'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text size="45" name="maintenancetext" value="$maintenancetext"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'632'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="guestaccess"$guestaccchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'380'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="enable_guestposting" $gpchecked></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'432'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="cookielength" size="5" value="$Cookie_Length"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'352'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="cookieusername" size="20" value="$cookieusername"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'353'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="cookiepassword" size="20" value="$cookiepassword"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'584'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="regagree"$agreechecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'702'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="emailpassword"$mailpasschecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'639'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="emailnewpass"$newpasschecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'619'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="emailwelcome"$welchecked></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'354'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="mailprog" size="20" value="$mailprog"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'407'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="smtp_server" size="20" value="$smtp_server"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'355'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="webmaster_email" size="35" value="$webmaster_email"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'404'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}">
    <select name="mailtype" size=1>
    <option value="0"$mts1>$txt{'405'}
    <option value="1"$mts2>$txt{'406'}
    <option value="2"$mts3>Net::SMTP
    </select></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'356'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="boarddir" size="30" value="$boarddir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'357'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="datadir" size="30" value="$datadir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'358'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="memberdir" size="30" value="$memberdir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'359'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="boardsdir" size="30" value="$boardsdir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'360'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="sourcedir" size="30" value="$sourcedir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'361'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="vardir" size="30" value="$vardir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'362'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="facesdir" size="30" value="$facesdir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'423'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="facesurl" size="45" value="$facesurl"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'363'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="imagesdir" size="45" value="$imagesdir"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'390'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="faderpath" size="45" value="$faderpath"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'506'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="ubbcjspath" size="45" value="$ubbcjspath"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'364'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="helpfile" size="45" value="$helpfile"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"><b><font size="1">$txt{'784'}</font></b></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'365'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="titlebg" size="10" value="$color{'titlebg'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'366'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="titletext" size="10" value="$color{'titletext'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'367'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="windowbg" size="10" value="$color{'windowbg'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'368'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="windowbg2" size="10" value="$color{'windowbg2'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'640'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="windowbg3" size="10" value="$color{'windowbg3'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'369'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="catbg" size="10" value="$color{'catbg'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'370'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="bordercolor" size="10" value="$color{'bordercolor'}"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'389'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="fadertext" size="10" value="$color{'fadertext'}"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'521'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="menutype"$menuchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'522'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="curposlinks"$cpchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'523'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="profilebutton"$pbchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'382'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showlatestmember" $slmchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'387'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="shownewsfader" $snfchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'388'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="fadertime" size="5" value="$fadertime"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'509'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showrecentbar" $srbarchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'618'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showmarkread" $smreadchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'732'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showbdescrip" $bdescripchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'383'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showmodify" $smodchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'384'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showuserpic" $supicchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'385'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showusertext" $sutextchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'386'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showgenderimage" $sgichecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'740'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="showyabbcbutt" $syabbcchecked></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'587'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}">
    <select name="timeselect" size=1>
    <option value="1"$tsl1>$txt{'480'}
    <option value="5"$tsl5>$txt{'484'}
    <option value="4"$tsl4>$txt{'483'}
    <option value="2"$tsl2>$txt{'481'}
    <option value="3"$tsl3>$txt{'482'}
    <option value="6"$tsl6>$txt{'485'}
    </select>
    </td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'371'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="timeoffset" size="5" value="$timeoffset"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'378'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="enable_ubbc"$ubbcchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'524'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="autolinkurls"$aluchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'379'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="enable_news" $newschecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'498'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="maxmesslen" size="5" value="$MaxMessLen"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'746'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="allowpics" $allowpicschecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'381'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="enable_notification" $notifchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'723'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="allow_hide_email" $ahmchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'689'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="maxsiglen" size="5" value="$MaxSigLen"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'408'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="timeout" size="5" value="$timeout"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'476'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="userpic_width" size="5" value="$userpic_width"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'477'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="userpic_height" size="5" value="$userpic_height"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'478'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="userpic_limits" size="45" value="$userpic_limits"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'588'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="jrmem" size="5" value="$JrPostNum"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'589'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="fullmem" size="5" value="$FullPostNum"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'590'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="srmem" size="5" value="$SrPostNum"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'591'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="godmem" size="5" value="$GodPostNum"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'372'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="TopAmmount" size="5" value="$TopAmmount"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'373'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="MembersPerPage" size="5" value="$MembersPerPage"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'374'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="maxdisplay" size="5" value="$maxdisplay"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'375'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="maxmessagedisplay" size="5" value="$maxmessagedisplay"></td>
 </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'690'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="clicklogtime" size="5" value="$ClickLogTime"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'376'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="max_log_days_old" size="5" value="$max_log_days_old"></td>
  </tr><tr>
    <td colspan=2 class="windowbg2" bgcolor="$color{'windowbg2'}">
    <HR size=1 width="100%" class="hr"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'392'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="LOCK_EX" size="5" value="$LOCK_EX"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'393'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="LOCK_UN" size="5" value="$LOCK_UN"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'607'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=text name="LOCK_SH" size="5" value="$LOCK_SH"></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'391'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}">
    <select name="use_flock" size=1>
    <option value="0"$fls1>$txt{'401'}
    <option value="1"$fls2>$txt{'402'}
    <option value="2"$fls3>$txt{'403'}
    </select></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'615'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="usetempfile"$utfchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size="2">$txt{'630'}</font></td>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}"><input type=checkbox name="faketruncation"$truncchecked></td>
  </tr><tr>
    <td class="windowbg2" bgcolor="$color{'windowbg2'}" colspan="2" align="center" valign="middle">
    <BR><input type=submit value="$txt{'10'}">
    </td>
  </tr>
</table>
</td>
</tr>
</table>
</form>
~;
	$yytitle = $txt{'222'};
	&template;
	exit;
}

sub GetBoardURL {
	my $url = 'http://' . ($ENV{'HTTP_HOST'} ? $ENV{'HTTP_HOST'} : $ENV{'SERVER_NAME'}) .
	($ENV{'SERVER_PORT'} != 80 ? ":$ENV{'SERVER_PORT'}" : '') .
	$ENV{'SCRIPT_NAME'};
	$url =~ s~/[^/]+\Z~/~;
	return $url;
}

# Gets our current absolute path. Needed for error messages.
sub GetDirPath {
	eval 'use Cwd; $cwd = cwd();';
	unless( $cwd ) { $cwd = `pwd`; chomp $cwd; }
	unless($cwd) { $cwd = $0 || $ENV{'PWD'} || $ENV{'CWD'} || ( $ENV{'DOCUMENT_ROOT'} . '/' . $ENV{'SCRIPT_NAME'} || $ENV{'PATH_INFO'} ); }
	$cwd =~ tr~\\~/~;
	$cwd =~ s~\A(.+)/\Z~$1~;
	$cwd =~ s~\A(.+)/YaBB\.\w+\Z~$1~i;
	return $cwd;
}

sub is_exe {
    my ($cmd,$name);
    foreach $cmd (@_) {
	$name = ($cmd =~ /^(\S+)/)[0];	# remove any options
	return ($cmd) if (-x $name and ! -d $name and $name =~ m:/:);	# check for absolute or relative path
	if (defined $ENV{PATH}) {
	    my $dir;
	    foreach $dir (split(/:/, $ENV{PATH})) {
		return "$dir/$cmd" if (-x "$dir/$name" && ! -d "$dir/$name");
	    }
	}
    }
    0;
}

sub ModifySettings2 {
	&is_admin;

	my @onoff = qw/
		allowpics showyabbcbutt showbdescrip maintenance guestaccess insert_original enable_ubbc enable_news enable_guestposting enable_notification showlatestmember showrecentbar showmarkread showmodify showuserpic showusertext showgenderimage shownewsfader MenuType curposlinks profilebutton autolinkurls emailpassword RegAgree emailwelcome allow_hide_email usetempfile faketruncation emailnewpass/;

	# Set as 0 or 1 if box was checked or not
	my $fi;
	map { $fi = lc $_; ${$_} = $FORM{$fi} eq 'on' ? 1 : 0; } @onoff;
	$guestaccess = $guestaccess ? 0 : 1;

	# If empty fields are submitted, set them to default-values to save yabb from crashing
	$maintenancetext = $FORM{'maintenancetext'} || "";
	&ToHTML($maintenancetext);
	$FORM{'timeout'} =~ s/\D//g;
	$timeout = $FORM{'timeout'} || 5;
	$FORM{'fadertime'} =~ s/\D//g;
	$fadertime = $FORM{'fadertime'} || 5000;
	$FORM{'timeselect'} =~ s/\D//g;
	$timeselected = $FORM{'timeselect'} || 0;
	$FORM{'timeoffset'} =~ tr/,/./;
	$FORM{'timeoffset'} =~ s/[^\d\.\-]//g;
	$timeoffset = $FORM{'timeoffset'} || 0;
	$FORM{'TopAmmount'} =~ s/\D//g;
	$TopAmmount = $FORM{'TopAmmount'} || 25;
	$FORM{'MembersPerPage'} =~ s/\D//g;
	$MembersPerPage = $FORM{'MembersPerPage'} || 20;
	$FORM{'maxdisplay'} =~ s/\D//g;
	$maxdisplay = $FORM{'maxdisplay'} || 20;
	$FORM{'maxmessagedisplay'} =~ s/\D//g;
	$maxmessagedisplay = $FORM{'maxmessagedisplay'} || 20;
	$FORM{'max_log_days_old'} =~ s/\D//g;
	$max_log_days_old = $FORM{'max_log_days_old'} || 21;
	$FORM{'clicklogtime'} =~ s/\D//g;
	$clicklogtime = $FORM{'clicklogtime'} || 1440;
	if($clicklogtime >= 1440) { $clicklogtime = 1439; }
	$FORM{'use_flock'} =~ s/\D//g;
	$use_flock = $FORM{'use_flock'} || 0;
	$FORM{'LOCK_EX'} =~ s/\D//g;
	$LOCK_EX = $FORM{'LOCK_EX'} || 2;
	$FORM{'LOCK_UN'} =~ s/\D//g;
	$LOCK_UN = $FORM{'LOCK_UN'} || 8;
	$FORM{'LOCK_SH'} =~ s/\D//g;
	$LOCK_SH = $FORM{'LOCK_SH'} || 1;
	$FORM{'cookielength'} =~ s/\D//g;
	$Cookie_Length = $FORM{'cookielength'} || 60;
	$FORM{'cookieusername'} =~ s/\W//g;
	$cookieusername = $FORM{'cookieusername'} || 'YaBBusername';
	$FORM{'cookiepassword'} =~ s/\W//g;
	$cookiepassword = $FORM{'cookiepassword'} || 'YaBBpassword';
	if ($cookieusername eq $cookiepassword) {$cookieusername = 'YaBBusername'; $cookiepassword = 'YaBBpassword';}
	$FORM{'maxmesslen'} =~ s/\D//g;
	$maxmesslen = $FORM{'maxmesslen'} || 5000;
	$FORM{'maxsiglen'} =~ s/\D//g;
	$maxsiglen = $FORM{'maxsiglen'} || 200;
	$FORM{'jrmem'} =~ s/\D//g;
	$jrmem = $FORM{'jrmem'} || 50;
	$FORM{'fullmem'} =~ s/\D//g;
	$fullmem = $FORM{'fullmem'} || 100;
	$FORM{'srmem'} =~ s/\D//g;
	$srmem = $FORM{'srmem'} || 250;
	$FORM{'godmem'} =~ s/\D//g;
	$godmem = $FORM{'godmem'} || 500;
	$language = $FORM{'language'} || 'english.lng';
	if($language =~ /[^\w\-\.]/) {$language = 'english.lng';}
	$mbname = $FORM{'mbname'} || 'My YaBB 1 Gold - SP1';
	$mbname =~ s/\"/\'/g;
	$mbname =~ s/\^//g;
	$boardurl = $FORM{'boardurl'} || &GetBoardURL;
	$boardurl =~ s/\"/\'/g;
	$boarddir = $FORM{'boarddir'} || &GetDirPath;
	$boarddir =~ s/\"/\'/g;
	$boardsdir = $FORM{'boardsdir'} || "$boarddir/boards";
	$boardsdir =~ s/\"/\'/g;
	$datadir = $FORM{'datadir'} || "$boarddir/posts";
	$datadir =~ s/\"/\'/g;
	$memberdir = $FORM{'memberdir'} || "$boarddir/members";
	$memberdir =~ s/\"/\'/g;
	$sourcedir = $FORM{'sourcedir'} || "$boarddir/source";
	$sourcedir =~ s/\"/\'/g;
	$vardir = $FORM{'vardir'} || "$boarddir/vars";
	$facesdir = $FORM{'facesdir'} || "$boarddir/yabb/images/avatars";
	$facesdir =~ s/\"/\'/g;
	$facesurl = $FORM{'facesurl'} || "$boardurl/yabb/images/avatars";
	$facesurl =~ s/\"/\'/g;
	$imagesdir = $FORM{'imagesdir'} || "$boardurl/yabb/images";
	$imagesdir =~ s/\"/\'/g;
	$helpfile = $FORM{'helpfile'} || "$boardurl/yabb/help/index.html";
	$helpfile =~ s/\"/\'/g;
	$mailprog = $FORM{'mailprog'} || &is_exe('/usr/lib/sendmail','/usr/sbin/sendmail','/usr/ucblib/sendmail','sendmail','mailx','Mail','mail');
	$mailprog =~ s/\"/\'/g;
	$smtp_server = $FORM{'smtp_server'} || '127.0.0.1';
	$smtp_server =~ s/\"/\'/g;
	$webmaster_email = $FORM{'webmaster_email'} || 'webmaster@mysite.com';
	$webmaster_email =~ s/\^//g;
	$webmaster_email =~ s/\"/\'/g;
	$FORM{'mailtype'} =~ s/\D//g;
	$mailtype = $FORM{'mailtype'} || 0;
	$color{'titlebg'} = $FORM{'titlebg'} || '#6E94B7';
	$color{'titlebg'} =~ s/\"/\'/g;
	$color{'titletext'} = $FORM{'titletext'} || '#FFFFFF';
	$color{'titletext'} =~ s/\"/\'/g;
	$color{'windowbg'} = $FORM{'windowbg'} || '#AFC6DB';
	$color{'windowbg'} =~ s/\"/\'/g;
	$color{'windowbg2'} = $FORM{'windowbg2'} || '#F8F8F8';
	$color{'windowbg2'} =~ s/\"/\'/g;
	$color{'windowbg3'} = $FORM{'windowbg3'} || '#6394BD';
	$color{'windowbg3'} =~ s/\"/\'/g;
	$color{'catbg'} = $FORM{'catbg'} || '#DEE7EF';
	$color{'catbg'} =~ s/\"/\'/g;
	$color{'bordercolor'} = $FORM{'bordercolor'} || '#6394BD';
	$color{'bordercolor'} =~ s/\"/\'/g;
	$color{'fadertext'} = $FORM{'fadertext'} || '#D4AD00';
	$color{'fadertext'} =~ s/\"/\'/g;
	$faderpath = $FORM{'faderpath'} || "$boardurl/fader.js";
	$faderpath =~ s/\"/\'/g;
	$ubbcjspath = $FORM{'ubbcjspath'} || "$boardurl/ubbc.js";
	$ubbcjspath =~ s/\"/\'/g;
	if ($FORM{'userpic_width'} =~ /^\d+$/)  { $userpic_width  = $FORM{'userpic_width'};  }
	else { $userpic_width = 65; }
	if ($FORM{'userpic_height'} =~ /^\d+$/) {	$userpic_height = $FORM{'userpic_height'}; }
	else { $userpic_height = 65; }
	$userpic_limits = $FORM{'userpic_limits'} || 'Please note that your image has to be <b>gif</b> or <b>jpg</b> and that it will be resized!';
	$userpic_limits =~ s/\~//g;
	$userpic_limits =~ s/\"/\'/g;
	my $filler = q~                                                                               ~;
	my $setfile = << "EOF";
###############################################################################
# Settings.pl                                                                 #
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

########## Board Info ##########
# Note: these settings must be properly changed for YaBB to work

\$maintenance = $maintenance;				# Set to 1 to enable Maintenance mode
\$guestaccess = $guestaccess;				# Set to 0 to disallow guests from doing anything but login or register

\$language = "$language";				# Change to language pack you wish to use
\$mbname = q^$mbname^;					# The name of your YaBB forum
\$boardurl = "$boardurl";				# URL of your board's folder (without trailing '/')

\$Cookie_Length = $Cookie_Length;			# Default minutes to set login cookies to stay for
\$cookieusername = "$cookieusername";			# Name of the username cookie
\$cookiepassword = "$cookiepassword";			# Name of the password cookie

\$RegAgree = $RegAgree;					# Set to 1 to display the registration agreement when registering
\$emailpassword = $emailpassword;			# 0 - instant registration. 1 - password emailed to new members
\$emailnewpass = $emailnewpass;				# Set to 1 to email a new password to members if they change their email address
\$emailwelcome = $emailwelcome;				# Set to 1 to email a welcome message to users even when you have mail password turned off

\$mailprog = "$mailprog";				# Location of your sendmail program
\$smtp_server = "$smtp_server";				# Address of your SMTP-Server
\$webmaster_email = q^$webmaster_email^;		# Your email address. (eg: \$webmaster_email = q^admin\@host.com^;)
\$mailtype = $mailtype;					# Mail program to use: 0 = sendmail, 1 = SMTP, 2 = Net::SMTP


########## Directories/Files ##########
# Note: directories other than \$imagesdir do not have to be changed unless you move things

\$boarddir = "$boarddir"; 				# The server path to the board's folder (usually can be left as '.')
\$boardsdir = "$boardsdir";         			# Directory with board data files
\$datadir = "$datadir";         			# Directory with messages
\$memberdir = "$memberdir";        			# Directory with member files
\$sourcedir = "$sourcedir";        			# Directory with YaBB source files
\$vardir = "$vardir";         				# Directory with variable files
\$facesdir = "$facesdir";				# The server path to your avatars (userpics) folder
\$facesurl = "$facesurl";				# URL to your avatars folder
\$imagesdir = "$imagesdir";				# URL to your images folder
\$ubbcjspath = "$ubbcjspath";	                        # URL to your 'ubbc.js' (REQUIRED for post/modify to work properly)
\$faderpath = "$faderpath";				# URL to your 'fader.js'
\$helpfile = "$helpfile";				# URL to your help file


########## Colors ##########
# Note: equivalent to colors in CSS tag of template.html, so set to same colors preferrably
# for browsers without CSS compatibility and for some items that don't use the CSS tag

\$color{'titlebg'} = "$color{'titlebg'}";		# Background color of the 'title-bar'
\$color{'titletext'} = "$color{'titletext'}";		# Color of text in the 'title-bar' (above each 'window')
\$color{'windowbg'} = "$color{'windowbg'}";		# Background color for messages/forms etc.
\$color{'windowbg2'} = "$color{'windowbg2'}";		# Background color for messages/forms etc.
\$color{'windowbg3'} = "$color{'windowbg3'}";		# Color of horizontal rules in posts
\$color{'catbg'} = "$color{'catbg'}";			# Background color for category (at Board Index)
\$color{'bordercolor'} = "$color{'bordercolor'}";	# Table Border color for some tables
\$color{'fadertext'}  = "$color{'fadertext'}";		# Color of text in the NewsFader (news color)

########## Layout ##########

\$maintenancetext = "$maintenancetext";			# User-defined text for Maintenance mode (leave blank for default text)
\$MenuType = $MenuType;					# 1 for text menu or anything else for images menu
\$curposlinks = $curposlinks;				# 1 for links in navigation on current page, or 0 for text without link
\$profilebutton = $profilebutton;			# 1 to show view profile button under post, or 0 for blank
\$timeselected = $timeselected;				# Select your preferred output Format of Time and Date
\$allow_hide_email = $allow_hide_email;			# Allow users to hide their email from public. Set 0 to disable
\$showlatestmember = $showlatestmember;			# Set to 1 to display "Welcome Newest Member" on the Board Index
\$shownewsfader = $shownewsfader;			# 1 to allow or 0 to disallow NewsFader javascript on the Board Index
							# If 0, you'll have no news at all unless you put <yabb news> tag
							# back into template.html!!!
\$Show_RecentBar = $showrecentbar;			# Set to 1 to display the Recent Post on Board Index
\$showmarkread = $showmarkread;				# Set to 1 to display and enable the mark as read buttons
\$showmodify = $showmodify;				# Set to 1 to display "Last modified: Realname - Date" under each message
\$ShowBDescrip = $showbdescrip;				# Set to 1 to display board descriptions on the topic (message) index for each board
\$showuserpic = $showuserpic;				# Set to 1 to display each member's picture in the message view (by the ICQ.. etc.)
\$showusertext = $showusertext;				# Set to 1 to display each member's personal text in the message view (by the ICQ.. etc.)
\$showgenderimage = $showgenderimage;			# Set to 1 to display each member's gender in the message view (by the ICQ.. etc.)
\$showyabbcbutt = $showyabbcbutt;                       # Set to 1 to display the yabbc buttons on Posting and IM Send Pages

########## Feature Settings ##########

\$enable_ubbc = $enable_ubbc;				# Set to 1 if you want to enable UBBC (Uniform Bulletin Board Code)
\$enable_news = $enable_news;				# Set to 1 to turn news on, or 0 to set news off
\$allowpics = $allowpics;				# set to 1 to allow members to choose avatars in their profile
\$enable_guestposting = $enable_guestposting;		# Set to 0 if do not allow 1 is allow.
\$enable_notification = $enable_notification;		# Allow e-mail notification
\$autolinkurls = $autolinkurls;				# Set to 1 to turn URLs into links, or 0 for no auto-linking.

\$timeoffset = $timeoffset;				# Time Offset (so if your server is EST, this would be set to -1 for CST)
\$TopAmmount = $TopAmmount;				# No. of top posters to display on the top members list
\$MembersPerPage = $MembersPerPage;			# No. of members to display per page of Members List - All
\$maxdisplay = $maxdisplay;				# Maximum of topics to display
\$maxmessagedisplay = $maxmessagedisplay;		# Maximum of messages to display
\$MaxMessLen = $maxmesslen;  				# Maximum Allowed Characters in a Posts
\$MaxSigLen = $maxsiglen;				# Maximum Allowed Characters in Signatures
\$ClickLogTime = $clicklogtime;				# Time in minutes to log every click to your forum (longer time means larger log file size)
\$max_log_days_old = $max_log_days_old;			# If an entry in the user's log is older than ... days remove it
							# Set to 0 if you want it disabled
\$fadertime = $fadertime;				# Length in milliseconds to delay between each item in the news fader
\$timeout = $timeout;					# Minimum time between 2 postings from the same IP


########## Membergroups ##########

\$JrPostNum = $jrmem;					# Number of Posts required to show person as 'junior' membergroup
\$FullPostNum = $fullmem;				# Number of Posts required to show person as 'full' membergroup
\$SrPostNum = $srmem;					# Number of Posts required to show person as 'senior' membergroup
\$GodPostNum = $godmem;					# Number of Posts required to show person as 'god' membergroup


########## MemberPic Settings ##########

\$userpic_width = $userpic_width;			# Set pixel size to which the selfselected userpics are resized, 0 disables this limit
\$userpic_height = $userpic_height;			# Set pixel size to which the selfselected userpics are resized, 0 disables this limit
\$userpic_limits = qq~$userpic_limits~;			# Text To Describe The Limits


########## File Locking ##########

\$LOCK_EX = $LOCK_EX;					# You can probably keep this as it is set now.
\$LOCK_UN = $LOCK_UN;					# You can probably keep this as it is set now.
\$LOCK_SH = $LOCK_SH;					# You can probably keep this as it is set now.

\$use_flock = $use_flock;				# Set to 0 if your server doesn't support file locking,
							# 1 for Unix/Linux and WinNT, and 2 for Windows 95/98/ME

\$usetempfile = $usetempfile;				# Write to a temporary file when updating large files.
							# This can potentially save your board index files from
							# being corrupted if a process aborts unexpectedly.
							# 0 to disable, 1 to enable.

\$faketruncation = $faketruncation;			# Enable this option only if YaBB fails with the error:
							# "truncate() function not supported on this platform."
							# 0 to disable, 1 to enable.

1;
EOF

	$setfile =~ s~(.+\;)\s+(\#.+$)~$1 . substr( $filler, 0, (70-(length $1)) ) . $2 ~gem;
	$setfile =~ s~(.{64,}\;)\s+(\#.+$)~$1 . "\n   " . $2~gem;
	$setfile =~ s~^\s\s\s+(\#.+$)~substr( $filler, 0, 70 ) . $1~gem;

	fopen(FILE, ">$boarddir/Settings.pl");
	print FILE $setfile;
	fclose(FILE);

	$Cookie_Exp_Date = 'Sunday, 17-Jan-2038 00:00:00 GMT';
	$cryptsession = &encode_session($user_ip,$masterseed);
	$cryptsession .= $masterseed;
	$password = crypt($settings[0],$masterseed);
	$password .= $masterseed;
	## usage: &UpdateCookie ("delete or write",<userid>,<password>,<session>,<path>,<expiration>); ##
	&UpdateCookie("write","$username","$password","$cryptsession","/","$Cookie_Exp_Date");
	&LoadIMs;
	&LoadUserSettings;
	&WriteLog;
	$yySetLocation = qq~$cgi;action=admin~;
	&redirectexit;
}

1;