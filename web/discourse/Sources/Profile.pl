###############################################################################
# Profile.pl                                                                  #
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

$profileplver = "1 Gold - SP 1.4";

sub ModifyProfile {
	if ($INFO{'username'} =~ m~/~){ &fatal_error($txt{'224'}); }
	if ($INFO{'username'} =~ m~\\~){ &fatal_error($txt{'225'}); }
	if($username ne $INFO{'username'} && $settings[7] ne 'Administrator') { &fatal_error($txt{'80'}); }
	if(!-e ("$memberdir/$INFO{'username'}.dat")){ &fatal_error("$txt{'453'} -- $INFO{'username'}"); }
	if($allowpics) {
		opendir(DIR, "$facesdir") || fatal_error("$txt{'230'} ($facesdir)! $txt{'681'}");
		closedir(DIR);
	}

	fopen(FILE, "$memberdir/$INFO{'username'}.dat");
	@memsettings=<FILE>;
	fclose(FILE);
	foreach (@memsettings) { $_ =~ s~[\n\r]~~g; }
	$dr = $memsettings[14] ? $memsettings[14] : $txt{'470'};
	if ($memsettings[11] eq 'Male') { $GenderMale = ' selected'; }
	if ($memsettings[11] eq 'Female') { $GenderFemale = ' selected'; }
	$signature = $memsettings[5];
	$signature =~ s/\&\&/\n/g;
	$signature =~ s/\&lt;/</g;
	$signature =~ s/\&gt;/>/g;
	&CalcAge("parse"); # Let's get the birthdate
	$memsettings[9] =~ tr/+/ /;
	$memsettings[10] =~ tr/+/ /;

	if ($memsettings[17] == 6) { $tsl6 = ' selected'; }
	elsif ($memsettings[17] == 5) { $tsl5 = ' selected'; }
	elsif($memsettings[17] == 4) { $tsl4 = ' selected'; }
	elsif ($memsettings[17] == 3) { $tsl3 = ' selected'; }
	elsif ($memsettings[17] == 2) { $tsl2 = ' selected'; }
	elsif ($memsettings[17] == 1) { $tsl1 = ' selected'; }
	elsif ($timeselected == 6) { $tsl6 = ' selected'; }
	elsif ($timeselected == 5) { $tsl5 = ' selected'; }
	elsif ($timeselected == 4) { $tsl4 = ' selected'; }
	elsif ($timeselected == 3) { $tsl3 = ' selected'; }
	elsif ($timeselected == 2) { $tsl2 = ' selected'; }
	else { $tsl1 = ' selected'; }

	$dayormonthm = qq~$txt{'564'}<input type="text" name="bday1" size="2" maxlength="2" value="$umonth">~; $dayormonthd = qq~$txt{'565'}<input type="text" name="bday2" size="2" maxlength="2" value="$uday">~;
	if ($tsl2 || $tsl3 || $tsl6) {$dayormonth=$dayormonthd.$dayormonthm;} else {$dayormonth=$dayormonthm.$dayormonthd;}

	$oldformat = $date;
	$oldmonth = substr($oldformat,0,2);
	$oldday = substr($oldformat,3,2);
	$oldyear = ("20".substr($oldformat,6,2)) - 1900;
	$oldhour = substr($oldformat,-8,2);
	$oldminute = substr($oldformat,-5,2);
	$oldsecond = substr($oldformat,-2,2);
	use Time::Local 'timelocal';
	eval { $oldtime = timelocal($oldsecond,$oldminute,$oldhour,$oldday,$oldmonth-1,$oldyear); };
	my ($psec,$pmin,$phour,$dummy,$dummy,$dummy,$dummy,$dummy,$dummy) = localtime($oldtime);
	if ($phour < 10) { $phour = "0$phour" };
	if ($pmin < 10) { $pmin = "0$pmin" };
	if ($psec < 10) { $psec = "0$psec" };
	$proftime = $phour.":".$pmin.":".$psec;
	$ampm = $phour > 11 ? 'pm' : 'am';
	$phour = $phour % 12 || 12;
	$proftime = qq~$phour:$pmin:$psec$ampm~;

	$yymain .= qq~
<form action="$cgi;action=profile2" method="POST" name="creator">
<table border=0 width=720 cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}" height="30">
    &nbsp;<img src="$imagesdir/profile.gif" alt="" border="0">&nbsp;
    <font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'79'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}" height="25"><BR><font size="1">$txt{'698'}</font><BR><BR></td>
  </tr><tr>
    <td class="catbg" bgcolor="$color{'catbg'}" height="25"><font size=2><b>$txt{'517'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    <table border=0 width="100%" cellpadding="3">
      <tr>
	<td width="320"><font size=2><b>$txt{'35'}: </b></font></td>
	<td><font size=2><input type="hidden" name="username" value="$INFO{'username'}">$INFO{'username'}</font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'81'}: </b></font><BR>
	<font size="1">$txt{'596'}</font></td>
	<td><input type="password" maxlength="30" name="passwrd1" size="20" value="$memsettings[0]"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'82'}: </b></font></td>
	<td><input type="password" maxlength="30" name="passwrd2" size="20" value="$memsettings[0]"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'68'}: </b></font><BR>
	<font size="1">$txt{'518'}</font></td>
	<td><input type="text" maxlength="30" name="name" size="30" value="$memsettings[1]"><input type="hidden" name="oldname" value="$memsettings[1]"></td>
      </tr><tr>
	<td width="320"><font size="2"><b>$txt{'69'}: </b></font><br>
        <font size="1">$txt{'679'}</font></td>
	<td><input type="text" maxlength="40" name="email" size="30" value="$memsettings[2]"><input type="hidden" name="oldemail" value="$memsettings[2]"></td>
      </tr>
    </table><br>
    </font></td>
  </tr><tr>
    <td class="catbg" bgcolor="$color{'catbg'}" height="25"><font size=2><b>$txt{'597'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}">
    <table border=0 width="100%" cellpadding="3">
      <tr>
	<td width="320"><font size=2><b>$txt{'231'}: </b></font></td>
	<td>
	<select name="gender" size="1">
	 <option value=""></option>
	 <option value="Male"$GenderMale>$txt{'238'}</option>
	 <option value="Female"$GenderFemale>$txt{'239'}</option>
	</select>
	</td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'563'}:</b></font></td>
	<td><font size="1">$dayormonth$txt{'566'}<input type="text" name="bday3" size="4" maxlength="4" value="$uyear"></font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'227'}: </b></font></td>
	<td><font size=2><input type="text" maxlength="30" name="location" size="50" value="$memsettings[15]"></font></td>
      </tr><tr>
	<td colspan=2>
	<BR><hr width="100%" size="1" class="hr"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'83'}: </b></font><BR>
	<font size="1">$txt{'598'}</font></td>
	<td><font size=2><input type=text maxlength="30" name=websitetitle size=50 value="$memsettings[3]"></font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'84'}: </b></font><BR>
	<font size="1">$txt{'599'}</font></td>
	<td><font size=2><input type=text name=websiteurl size=50 value="$memsettings[4]"></font></td>
      </tr><tr>
	<td colspan=2>
	<BR><hr width="100%" size="1" class="hr"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'513'}: </b></font><BR>
	<font size="1">$txt{'600'}</font></td>
	<td><font size=2><input type=text maxlength="10" name=icq size=20 value="$memsettings[8]"></font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'603'}: </b></font><BR>
	<font size="1">$txt{'601'}</font></td>
	<td><font size=2><input type=text maxlength="30" name=aim size=20 value="$memsettings[9]"></font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'604'}: </b></font><BR>
	<font size="1">$txt{'602'}</font></td>
	<td><font size=2><input type=text maxlength="30" name=yim size=20 value="$memsettings[10]"></font></td>
      </tr><tr>
	<td colspan=2>
	<BR><hr width="100%" size="1" class="hr"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'228'}: </b></font></td>
	<td><font size=2><input type=text name=usertext size=50 value="$memsettings[12]" maxlength="50"></font></td>
      </tr>
~;
	if($allowpics) {
		opendir(DIR, "$facesdir") || fatal_error("$txt{'230'} ($facesdir)!<BR>$txt{'681'}");
		@contents = readdir(DIR);
		closedir(DIR);
		$images = "";
		foreach $line (sort @contents){
			($name, $extension) = split (/\./, $line);
			$checked = "";
			if ($line eq $memsettings[13]) { $checked = ' selected'; }
			if ($memsettings[13] =~ m~\Ahttp://~ && $line eq 'blank.gif') { $checked = ' selected'; }
			if ($extension =~ /gif/i || $extension =~ /jpg/i || $extension =~ /jpeg/i || $extension =~ /png/i ){
				if ($line eq 'blank.gif') { $name = $txt{'422'}; }
				$images .= qq~<option value="$line"$checked>$name\n~;
			}
		}
		if ($memsettings[13] =~ m~\Ahttp://~) {
			$pic = 'blank.gif';
			$checked = ' checked';
			$tmp = $memsettings[13];
		}
		else {
			$pic = $memsettings[13];
			$tmp = 'http://';
		}

		$yymain .= qq~
      <tr>
	<td width="320"><font size="2"><b>$txt{'229'}:</b></font><br>
	<font size="1">$txt{'474'} $userpic_limits</font></td>
        <td>
        <script language="JavaScript1.2" type="text/javascript">
        function showimage()
        {
           if (!document.images) return;
           document.images.icons.src="$facesurl/"+document.creator.userpic.options[document.creator.userpic.selectedIndex].value;
        }
        </script>
        <select name="userpic" size=6 onChange="showimage()">$images</select>
        &nbsp;&nbsp;<img src="$facesurl/$pic" name="icons" border=0 hspace=15>
	</td>
      </tr><tr>
	<td width="320"><font size="2"><B>$txt{'475'}</B></font></td>
	<td><input type="checkbox" name="userpicpersonalcheck"$checked>
	<input type="text" name="userpicpersonal" size="45" value="$tmp"></td>
      </tr>
~;
	}
	$yymain .= qq~
    </table><BR>
    </td>
  </tr><tr>
    <td class="catbg" bgcolor="$color{'catbg'}" height="25"><font size=2><b>$txt{'605'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    <table border=0 cellpadding="3" width="100%" cellspacing="0">
~;
	if ($allow_hide_email) {
		$yymain .= qq~
    <tr>
	<td width="320"><font size=2><b>$txt{'721'}</b></font></td>
	<td><input type="checkbox" name="hideemail" value="checked" $memsettings[19]></td>
    </tr>
~;
	}
	$yymain .= qq~
      <tr>
	<td width="320"><font size=2><b>$txt{'486'}:</b></font><BR>
	<font size="1">$txt{'479'}</font></td>
	<td width="50">
	<select name="usertimeselect" size=1>
	 <option value="1"$tsl1>$txt{'480'}
	 <option value="5"$tsl5>$txt{'484'}
	 <option value="4"$tsl4>$txt{'483'}
	 <option value="2"$tsl2>$txt{'481'}
	 <option value="3"$tsl3>$txt{'482'}
	 <option value="6"$tsl6>$txt{'485'}
	</select>
	</td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'371'}:</b></font><BR>
	<font size="1">$txt{'519'}</font></td>
	<td><font size="1">
	<input name="usertimeoffset" size="5" maxlength="5" value="$memsettings[18]">
	<BR>$txt{'741'}: <i>$proftime</i></font></td>
      </tr><tr>
		<td colspan=2><br><hr width="100%" size="1" class="hr"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'85'}:</b></font><BR>
	<font size="1">$txt{'606'}</font></td>
	<td><font size=2><textarea name="signature" rows="4" cols="50" wrap="virtual">$signature</textarea><BR>
	<font size="1">$txt{'664'} <input value="$MaxSigLen" size="3" name="msgCL" disabled></FONT><BR><BR></font></td></tr>
	<script language="JavaScript">
	<!--
	var supportsKeys = false
	function tick() {
	  calcCharLeft(document.forms[0])
	  if (!supportsKeys) timerID = setTimeout("tick()",$MaxSigLen)
	}

	function calcCharLeft(sig) {
	  clipped = false
	  maxLength = $MaxSigLen
	  if (document.creator.signature.value.length > maxLength) {
		document.creator.signature.value = document.creator.signature.value.substring(0,maxLength)
		charleft = 0
		clipped = true
	  } else {
		charleft = maxLength - document.creator.signature.value.length
	  }
	  document.creator.msgCL.value = charleft
	  return clipped
	}

	tick();
	//-->
	</script>
~;
	fopen(FILE, "$vardir/membergroups.txt");
	@lines = <FILE>;
	fclose(FILE);
	if($settings[7] eq 'Administrator') {
		$position='';
		foreach $curl (@lines) {
			if($curl ne $lines[1] && $curl ne $lines[2] && $curl ne $lines[3] && $curl ne $lines[4] && $curl ne $lines[5] && $curl ne $lines[6]) {
				if($curl ne $lines[0]) { $position= qq~$position<option>$curl~; }
				else { $position= qq~$position<option value="Administrator">$curl~; }
			}
		}
		if($memsettings[7] eq 'Administrator') { $tt = $lines[0]; }
		else { $tt = $memsettings[7]; }
		$yymain .= qq~
      <tr>
	<td colspan=2><hr width="100%" size="1" class="hr"></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'86'}: </b></font></td>
	<td><font size=2><input type=text name=settings6 size=4 value="$memsettings[6]"></font></td>
      </tr><tr>
	<td width="320"><font size=2><b>$txt{'87'}: </b></font></td>
	<td><font size=2><select name="settings7">
	 <option value="$memsettings[7]">$tt
	 <option value="$memsettings[7]">---------------
	 <option value="">
	 $position
	</select></font></td>
      </tr><tr>
        <td width="320"><font size="2"><b>$txt{'233'}:</b></font><br>
        <font size="1">$txt{'421'}</font><br><br></td>
        <td><input type="text" name="dr" size="20" value="$dr"><br><br></td>
      </tr>~;
	}
	if($settings[7] eq 'Administrator') { $confdel_text = "$txt{'775'} $txt{'777'} $INFO{'username'} $txt{'778'}"; }
	else { $confdel_text = "$txt{'775'} $txt{'776'} $txt{'778'}"; }
	$yymain .= qq~
    </table>
    </font></td>
  </tr><tr>
    <td class="catbg" bgcolor="$color{'catbg'}" height="25" align="center"><font size=2><BR>
    <input type=submit name=moda value="$txt{'88'}">
    <input type=submit name=moda value="$txt{'89'}" onClick="return confirm('$confdel_text')"><BR><BR>
    </font></td>
  </tr>
</table>
</form>
~;
	$yytitle = $txt{'79'};
	&template;
	exit;
}

sub ModifyProfile2 {
	my( %member, $key, $value, $newpassemail, @memberlist, $a, @check_settings, @reserve, $matchword, $matchcase, $matchuser, $matchname, $namecheck, $reserved, $reservecheck, @dirdata, $filename, @entries, $entry, $umail, @members, $tempname );
	$FORM{'signature'} =~ s~\n~\&\&~g;
	while( ($key,$value) = each(%FORM) ) {
		$value =~ s~\A\s+~~;
		$value =~ s~\s+\Z~~;
		$value =~ s~[\n\r]~~g;
		$member{$key} = $value;
	}

	# make sure this person has access to this profile
	if($username ne $member{'username'} && $settings[7] ne 'Administrator') { &fatal_error($txt{'80'}); }
	if( $settings[7] ne 'Administrator' ) {
		$member{'username'} = $username;
		$member{'settings6'} = $settings[6];
		$member{'settings7'} = $settings[7];
	}
	if($member{'settings6'} !~ /\A[0-9]+\Z/) { &fatal_error("$txt{'749'}"); }
	if ($member{'username'} =~ /\//){ &fatal_error($txt{'224'}); }
	if ($member{'username'} =~ /\\/){ &fatal_error($txt{'225'}); }
	$INFO{'username'} = $member{'username'};
	if(length($member{'usertext'}) > 51) { &fatal_error("$txt{'757'}"); }
	if( $member{'userpicpersonalcheck'} && ( $member{'userpicpersonal'} =~ m/\.gif\Z/i || $member{'userpicpersonal'} =~ m/\.jpg\Z/i || $member{'userpicpersonal'} =~ m/\.jpeg\Z/i || $member{'userpicpersonal'} =~ m/\.png\Z/i ) ) {
		$member{'userpic'} = $member{'userpicpersonal'};
	}
	if($member{'userpic'} eq "") { $member{'userpic'} = "blank.gif"; }
	&fatal_error("$txt{'592'}") if($member{'userpic'} !~ m^\A[0-9a-zA-Z_\.\#\%\-\:\+\?\$\&\~\.\,\@/]+\Z^);
	if(!$allowpics) { $member{'userpic'} = "blank.gif"; }
	if( $emailnewpass && lc $member{'email'} ne lc $settings[2] && $settings[7] ne 'Administrator') {
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
		$newpassemail = 1;
	} else {
		&fatal_error("($member{'username'}) $txt{'213'}") if($member{'passwrd1'} ne $member{'passwrd2'});
		&fatal_error("($member{'username'}) $txt{'91'}") if($member{'passwrd1'} eq '');
		&fatal_error("$txt{'240'} $txt{'36'} $txt{'241'}") if($member{'passwrd1'} !~ /\A[\s0-9A-Za-z!@#$%\^&*\(\)_\+|`~\-=\\:;'",\.\/?\[\]\{\}]+\Z/);
	}
	&fatal_error("$txt{'75'}") if($member{'name'} eq '');
	&fatal_error("$txt{'240'} $txt{'68'} $txt{'241'}") if($member{'name'} !~ /^[\s0-9A-Za-zöäüÖÄÜßñ\[\]#%+,-|\.:=?@^_]+$/);
	&fatal_error("$txt{'75'}") if($member{'name'} eq '|');
	&fatal_error("$txt{'568'}") if(length($member{'name'}) > 30);
	&fatal_error("$txt{'76'}") if($member{'email'} eq '');
	&fatal_error("$txt{'240'} $txt{'69'} $txt{'241'}") if($member{'email'} !~ /[\w\-\.\+]+\@[\w\-\.\+]+\.(\w{2,4}$)/);
	&fatal_error("$txt{'500'}") if(($member{'email'} =~ /(@.*@)|(\.\.)|(@\.)|(\.@)|(^\.)|(\.$)/) || ($member{'email'} !~ /^.+@\[?(\w|[-.])+\.[a-zA-Z]{2,4}|[0-9]{1,4}\]?$/));
	if( $member{'bday1'} ne "" || $member{'bday2'} ne "" || $member{'bday3'} ne "" ) {
		&fatal_error("$txt{'567'}") if( $member{'bday1'} !~ /^[0-9]+$/ || $member{'bday2'} !~ /^[0-9]+$/ || $member{'bday3'} !~ /^[0-9]+$/ || length($member{'bday3'}) < 4 );
		&fatal_error("$txt{'567'}") if( $member{'bday1'} < 1 || $member{'bday1'} > 12 || $member{'bday2'} < 1 || $member{'bday2'} > 31 || $member{'bday3'} < 1901 || $member{'bday3'} > $year-5 );
	}
	&fatal_error("$txt{'680'}") if ($member{'username'} eq "admin" && $member{'settings7'} ne "Administrator");

	if($member{'moda'} eq $txt{'88'}) {
		if (length($member{'signature'}) > $MaxSigLen) { $member{'signature'} = substr($member{'signature'},0,$MaxSigLen); }
	        $member{'icq'} =~ s/[^0-9]//g;
		$member{'bday1'} =~ s/[^0-9]//g;
	        $member{'bday2'} =~ s/[^0-9]//g;
		$member{'bday3'} =~ s/[^0-9]//g;
		if($member{'bday1'}) { $member{'bday'} = "$member{'bday1'}/$member{'bday2'}/$member{'bday3'}"; }
		else { $member{'bday'} = ''; }
		$member{'signature'} =~ s/</&lt;/g;
	        $member{'signature'} =~ s/</&gt;/g;
	        $member{'aim'} =~ s/ /\+/g;
	        $member{'yim'} =~ s/ /\+/g;
		if($settings[7] ne "Administrator") { $member{'dr'} = $settings[14]; }
		# store the name temorarily so we can restore any _'s later
		$tempname = $member{'name'};
		$member{'name'} =~ s/\_/ /g;

		&ToHTML($member{'location'});
		&ToHTML($member{'aim'});
		&ToHTML($member{'yim'});
		&ToHTML($member{'gender'});
		&ToHTML($member{'usertext'});
		&ToHTML($member{'websiteurl'});
		&ToHTML($member{'websitetitle'});
		&ToHTML($member{'email'});
		&ToHTML($FORM{'hideemail'});
		&ToHTML($member{'name'});

		&FromHTML($member{'usertext'});
		$member{'usertext'} =~ s~(\S{15})(?=\S)~$1 ~g;
		&ToHTML($member{'usertext'});

		if ( length $member{'signature'} > 1000 ) { $member{'signature'} = substr( $member{'signature'}, 0, 1000 ); }
		$member{'usertimeoffset'} =~ tr/,/./;
		$member{'usertimeoffset'} =~ s/[^\d*|\.|\-|w*]//g;
		if (( $member{'usertimeoffset'} < -23.5) || ( $member{'usertimeoffset'} > 23.5)) { &fatal_error($txt{'487'}); }

		$testname = lc $member{'name'};
		$testemail = lc $member{'email'};

		if($member{'name'} ne $member{'oldname'} || $member{'email'} ne $member{'oldemail'}){
			$doublecheck = &profilecheck($member{'username'},$testname,$testemail,"check");
			if($doublecheck eq "email_exists"){&fatal_error("$txt{'730'} ($member{'email'}) $txt{'731'}");}
			if($doublecheck eq "realname_exists"){&fatal_error("($member{'name'}) $txt{'473'}");}
		}

		fopen(FILE, "$vardir/reserve.txt") || &fatal_error("$txt{'23'} reserve.txt");
		@reserve = <FILE>;
		fclose(FILE);
		fopen(FILE, "$vardir/reservecfg.txt") || &fatal_error("$txt{'23'} reservecfg.txt");
		@reservecfg = <FILE>;
		fclose(FILE);
		for($a = 0; $a < @reservecfg; $a++) { chomp $reservecfg[$a]; }
		$matchword = $reservecfg[0] eq 'checked';
		$matchcase = $reservecfg[1] eq 'checked';
		$matchuser = $reservecfg[2] eq 'checked';
		$matchname = $reservecfg[3] eq 'checked';
		$namecheck = $matchcase eq 'checked' ? $member{'name'} : lc $member{'name'};

		foreach $reserved (@reserve) {
			chomp $reserved;
			$reservecheck = $matchcase ? $reserved : lc $reserved;
			if ($matchname) {
				if ($matchword) {
					if ($namecheck eq $reservecheck) { &fatal_error("$txt{'244'} $reserved"); }
				}
				else {
					if ($namecheck =~ $reservecheck) { &fatal_error("$txt{'244'} $reserved"); }
				}
			}
		}

		# let's restore the name now
		&ToHTML($tempname);
		$member{'name'} = $tempname;

		fopen(MEMBERFILEREAD,"$memberdir/$member{'username'}.dat");
		my @memset = <MEMBERFILEREAD>;
		fclose(MEMBERFILEREAD);
		chomp @memset;
		require "$sourcedir/Notify.pl";
		replace_notifications($memset[2],$member{'email'});

		fopen( FILE, ">$memberdir/$member{'username'}.dat", 1);
		print FILE "$member{'passwrd1'}\n";
		print FILE "$member{'name'}\n";
		print FILE "$member{'email'}\n";
		print FILE "$member{'websitetitle'}\n";
		print FILE "$member{'websiteurl'}\n";
		print FILE "$member{'signature'}\n";
		print FILE "$member{'settings6'}\n";
		print FILE "$member{'settings7'}\n";
		print FILE "$member{'icq'}\n";
		print FILE "$member{'aim'}\n";
		print FILE "$member{'yim'}\n";
		print FILE "$member{'gender'}\n";
		print FILE "$member{'usertext'}\n";
		print FILE "$member{'userpic'}\n";
		print FILE "$member{'dr'}\n";
		print FILE "$member{'location'}\n";
		print FILE "$member{'bday'}\n";
		print FILE "$member{'usertimeselect'}\n";
		print FILE "$member{'usertimeoffset'}\n";
		if ($FORM{'hideemail'} ne "checked") { $FORM{'hideemail'} = ""; }
		print FILE "$FORM{'hideemail'}\n";
		fclose(FILE);

		&profilecheck($member{'username'},$member{'name'},$testemail,"update");

	if($newpassemail) {

	# Write log
	fopen(LOG, "$vardir/log.txt");
	@entries = <LOG>;
	fclose(LOG);
	fopen(LOG, ">$vardir/log.txt", 1);
	$field="$username";
	foreach $curentry (@entries) {
	        $curentry =~ s/\n//g;
     		($name, $value) = split(/\|/, $curentry);
	        if($name ne "$field") {
	                print LOG "$curentry\n";
	        }
	}
	fclose(LOG);

	## usage: &UpdateCookie ("delete or write",<userid>,<password>,<session>,<path>,<expiration>); ##
	&UpdateCookie("delete");
	$username = 'Guest';
	$password = '';
	@settings = ();
	$realname = '';
	$realemail = '';
	$ENV{'HTTP_COOKIE'} = '';
	&FormatUserName($member{'username'});
	&sendmail($member{'email'},qq~$txt{'700'} $mbname~, "$txt{'733'} $member{'passwrd1'} $txt{'734'} $member{'username'}.\n\n$txt{'701'} $scripturl?action=profile;username=$useraccount{$member{'username'}}\n\n$txt{'130'}");
	$yymain .= qq~<br><table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">~;

	require "$sourcedir/LogInOut.pl";
	$sharedLogin_title="$txt{'34'}";
	$sharedLogin_text="$txt{'638'}";
	&sharedLogin;

	$yymain .= qq~</table>~;
	$yytitle="$txt{'245'}";
	&template;
	exit;
	}
	else {
		if ($member{'username'} eq $username) {
			$password = crypt("$member{'passwrd1'}",$masterseed);
			$password .= $masterseed;
			my $expiration = "Sunday, 17-Jan-2038 00:00:00 GMT";
			## usage: &UpdateCookie ("delete or write",<userid>,<password>,<session>,<path>,<expiration>); ##
			&UpdateCookie("write",$username,$password,$cookiesession,"/",$expiration);
			&LoadUserSettings;
			&WriteLog;
		}
		&ViewProfile;
	}
	} else {
	if($member{'username'} ne "admin") {
		require "$sourcedir/Notify.pl";
		remove_notifications($member{'email'});

	# For security, remove username from mod position
	fopen(FILE, "$vardir/cat.txt"); @categories = <FILE>; fclose(FILE);

	foreach $curcat (@categories) {
		$curcat =~ s/[\n\r]//g;
		fopen(CAT, "$boardsdir/$curcat.cat"); @catinfo = <CAT>; fclose(CAT);

		foreach $curboard (@catinfo) {
				$curboard =~ s/[\n\r]//g; chomp $curboard;

				fopen(BOARD, "$boardsdir/$curboard.dat"); @boardinfo = <BOARD>; fclose(BOARD);

				$boardinfo[2] =~ s/[\n\r]//g; $boardinfo[2] =~ /^\|(.*?)\|$/;
				$mods = $1 or $mods = $boardinfo[2];
                                $mods =~ s/(\s*)//g;              # remove all whitespaces
				$mods =~ s/(^(\|)+)?((\|)+$)?//;  # remove unnecessary front and back separator
				$mods =~ s/(\|)+/\|/g;            # replace multiple separators with one separator

				my @mod_ary = split(/\|/, $mods);
				my @new_mod_ary;
				my $mods_changed = 0;
				foreach my $mod( @mod_ary) {
					if( $settings[7] eq 'Administrator') {
						if( $member{'username'} ne $mod) {
							push( @new_mod_ary, $mod);
						} else {
							$mods_changed = 1;
						}
					} elsif( $member{'username'} eq $username) {
						if( $username ne $mod) {
							push( @new_mod_ary, $mod);
						} else {
							$mods_changed = 1;
						}
					}
				}				
				$mods = join( "|", @new_mod_ary);

				if ($mods_changed == 1)
				{
					$boardinfo[2] = $mods;
					fopen(BOARD, ">$boardsdir/$curboard.dat", 1);
					print BOARD @boardinfo;
					fclose(BOARD);
				}
		}
	}
		if($settings[7] eq 'Administrator') {
			unlink("$memberdir/$member{'username'}.dat");
			unlink("$memberdir/$member{'username'}.msg");
			unlink("$memberdir/$member{'username'}.log");
	                unlink("$memberdir/$member{'username'}.outbox");
	                unlink("$memberdir/$member{'username'}.imconfig");
			&profilecheck($member{'username'},"-","-","delete");
		}
		elsif( $member{'username'} eq $username ) {
			unlink("$memberdir/$username.dat");
			unlink("$memberdir/$username.msg");
			unlink("$memberdir/$username.log");
	                unlink("$memberdir/$username.outbox");
	                unlink("$memberdir/$username.imconfig");
			&profilecheck($username,"-","-","delete");
		}

		opendir (DIRECTORY,"$datadir");
		@dirdata = readdir(DIRECTORY);
		closedir (DIRECTORY);

		if($settings[7] eq 'Administrator') {
			$umail=$member{'email'};
		} else {
			$umail=$settings[2];
		}
		foreach $filename (@dirdata) {
			unless( $filename =~ m~mail\A~ ) { next; }
			fopen(FILE, "$datadir/$filename");
			@entries = <FILE>;
			fclose(FILE);

			fopen(FILE, ">$datadir/$filename");
			foreach $entry (@entries) {
				$entry =~ s/[\n\r]//g;
				if ($entry ne $umail) {
					print FILE "$entry\n";
				}
			}
			fclose(FILE);

		}

		fopen(FILE, "$memberdir/memberlist.txt");
		@members = <FILE>;
		fclose(FILE);
		fopen(FILE, ">$memberdir/memberlist.txt", 1);
		my $memberfound = 0;
		my $lastvalidmember = '';
		foreach $curmem (@members) {
			chomp $curmem;
			if($curmem ne $member{'username'}) { print FILE "$curmem\n"; $lastvalidmember = $curmem; }
			else { ++$memberfound; }
		}
		fclose(FILE);
		my $membershiptotal = @members - $memberfound;
		fopen(FILE, "+>$memberdir/members.ttl");
		print FILE qq~$membershiptotal|$lastvalidmember~;
		fclose(FILE);
		if($settings[7] ne 'Administrator') {
			require "$sourcedir/LogInOut.pl"; &Logout;
		}
		$yySetLocation = qq~$scripturl~;
		&redirectexit;
	}
	else { &fatal_error("$txt{'751'}"); }
	}
	exit;
}

sub ViewProfile {
	if($username eq "Guest") { &fatal_error("$txt{'223'}"); }
	if ($INFO{'username'} =~ /\//){ &fatal_error("$txt{'224'}" ); }
	if ($INFO{'username'} =~ /\\/){ &fatal_error("$txt{'225'}" ); }
	if(!-e ("$memberdir/$INFO{'username'}.dat")){ &fatal_error("$txt{'453'} -- $INFO{'username'}"); }

	my($memberinfo, $modify, $email, $gender, $pic);

	# get the member's info
	fopen(FILE, "$memberdir/$INFO{'username'}.dat");
	@memsettings=<FILE>;
	fclose(FILE);
	chomp @memsettings;
	$icq = $memsettings[8];
	if($memsettings[4] !~ m~\Ahttp://~) { $memsettings[4] = "http://$memsettings[4]"; }
	$memsettingsd[9] = $memsettings[9]; $memsettingsd[9] =~ tr/+/ /;
	$memsettingsd[10] = $memsettings[10]; $memsettingsd[10] =~ tr/+/ /;
	$dr = "";
	if ($memsettings[14] eq "") { $dr = "$txt{'470'}"; }
	else { $dr = "$memsettings[14]"; $dr = &timeformat($dr); }
	&CalcAge("calc"); # How old is he/she?
	&CalcAge("isbday"); # is it the bday?
	if($isbday) { $isbday = "<img src=\"$imagesdir/bdaycake.gif\" width=40>"; }

	fopen(FILE, "$vardir/membergroups.txt");
	@membergroups = <FILE>;
	fclose(FILE);
	if($memsettings[6] > $GodPostNum) { $memberinfo = "$membergroups[6]"; }
	elsif($memsettings[6] > $SrPostNum) { $memberinfo = "$membergroups[5]"; }
	elsif($memsettings[6] > $FullPostNum) { $memberinfo = "$membergroups[4]"; }
	elsif($memsettings[6] > $JrPostNum) { $memberinfo = "$membergroups[3]"; }
	else { $memberinfo = "$membergroups[2]"; }
	if($memsettings[7] ne "") { $memberinfo = "$memsettings[7]"; }
	if($memsettings[7] eq "Administrator") { $memberinfo = "$membergroups[0]"; }

	&FormatUserName($INFO{'username'});
	if ($username eq $INFO{'username'} || $settings[7] eq "Administrator") {
		$modify = qq~&#171; <a href="$cgi;action=profile;username=$useraccount{$INFO{'username'}}"><font size=2 class="text1" color="$color{'titletext'}">$txt{'17'}</font></a> &#187;~;
	}
	if ($memsettings[19] ne "checked" || $settings[7] eq "Administrator" || !$allow_hide_email) {
		$email = qq~<a href="mailto:$memsettings[2]">$memsettings[2]</a>~;
	}
	else { $email = qq~<i>$txt{'722'}</i>~; }
	$gender = "";
	if ($memsettings[11] eq "Male") { $gender = qq~$txt{'238'}~; }
	if ($memsettings[11] eq "Female") { $gender = qq~$txt{'239'}~; }
	if($allowpics) {
	if ($memsettings[13] =~ /^\http:\/\// ) {
		if ($userpic_width ne 0) { $tmp_width = "width=$userpic_width"; } else { $tmp_width=""; }
		if ($userpic_height ne 0) { $tmp_height = "height=$userpic_height"; } else { $tmp_height=""; }
		$pic = qq~<a href="$memsettings[13]" target="_blank" onClick="window.open('$memsettings[13]', 'ppic$INFO{username}', 'resizable,width=200,height=200'); return false;">~;
                $pic .= qq~<img src="$memsettings[13]" $tmp_width $tmp_height border="0" alt=""></a>~;
	}
	else {
		$pic = qq~<a href="$facesurl/$memsettings[13]" target="_blank" onClick="window.open('$facesurl/$memsettings[13]', 'ppic$INFO{username}', 'resizable,width=200,height=200'); return false;">~;
                $pic .= qq~<img src="$facesurl/$memsettings[13]" border="0" alt=""></a>~;
	}
	}

	$online = "$txt{'113'} ?";
	fopen(FILE, "$vardir/log.txt");
	@entries = <FILE>;
	fclose(FILE);
	foreach $curentry (@entries) {
		chomp $curentry;
		($name, $value) = split(/\|/, $curentry);
		if($name) {
			&LoadUser($name);
			if(lc $name eq lc $INFO{'username'}) {
				$online =~ s~\?~<i>$txt{'686'}</i>.~;
			}
		}
	}
	$online =~ s~\?~<i>$txt{'687'}</i>.~;
	if($memsettings[6] > 100000) { $memsettings[6] = "$txt{'683'}"; }

	$yymain .= qq~
<table border="0" cellpadding="4" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center" width="75%">
  <tr>
    <td class="titlebg" colspan="2" bgcolor="$color{'titlebg'}">
    <table border="0" cellspacing="0" cellpadding="0" width="100%">
      <tr height="30">
	<td width="50%">
        <img src="$imagesdir/profile.gif" alt="" border="0">&nbsp;
        <font size=2 class="text1" color="$color{'titletext'}"><B>$txt{'35'}: $INFO{'username'}</b></font></td>
	<td align="center" width="20%">
        <font size=2 class="text1" color="$color{'titletext'}">$modify</font></td>
	<td align="center" width="30%">
        <font size=2 class="text1" color="$color{'titletext'}">$txt{'232'}</font></td>
      </tr>
    </table>
    </td>
  </tr><tr>
    <td bgcolor="$color{'windowbg'}" class="windowbg" width="70%">
    <table border=0 cellspacing="0" cellpadding="2" width="100%">
      <tr>
	<td><font size=2><b>$txt{'68'}: </b></font></td>
	<td><font size=2>$memsettings[1]</font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'86'}: </b></font></td>
        <td><font size=2>$memsettings[6]</font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'87'}: </b></font></td>
        <td><font size=2>$memberinfo</font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'233'}: </b></font></td>
        <td><font size=2>$dr</font></td>
      </tr><tr>
	<td colspan="2"><hr size="1" width="100%" class="hr"></td>
      </tr><tr>
        <td><font size=2><b>$txt{'513'}:</b></font></td>
        <td><font size=2>~;
        if( $memsettings[8] ne "" && $memsettings[8] !~ m~\D~ ) {$yymain .= qq~ <img src="http://web.icq.com/whitepages/online?icq=$memsettings[8]&img=5" alt="$memsettings[8]" border="0"> <a href="$cgi;action=icqpager;UIN=$icq" target=_blank>$memsettings[8]</a>~;}
	$yymain .= qq~
        </font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'603'}: </b></font></td>
        <td><font size=2><a href="aim:goim?screenname=$memsettings[9]&message=Hi,+are+you+there?">$memsettingsd[9]</a></font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'604'}: </b></font></td>
        <td><font size=2>~;
        if( $memsettings[10] ne "") {$yymain .= qq~ <img src="http://opi.yahoo.com/online?u=$memsettings[10]&m=g&t=0" NOSAVE BORDER=0 alt="$memsettings[10]"> <a href="http://edit.yahoo.com/config/send_webmesg?.target=$memsettings[10]">$memsettingsd[10]</a></font></td>~;}
        else {$yymain .= qq~ </font></td>~;}
	$yymain .= qq~
      </tr><tr>
	<td><font size=2><b>$txt{'69'}: </b></font></td>
	<td><font size=2>$email</font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'96'}: </b></font></td>
        <td><font size=2><a href="$memsettings[4]" target=_blank>$memsettings[3]</a></font></td>
      </tr><tr>
	<td colspan="2"><hr size="1" width="100%" class="hr"></td>
      </tr><tr>
	<td><font size=2><b>$txt{'231'}: </b></font></td>
	<td><font size=2>$gender</font></td>
      </tr><tr>
        <td><font size=2><b>$txt{'420'}:</b></font></td>
        <td><font size=2>$age</font> &nbsp; $isbday</td>
      </tr><tr>
        <td><font size=2><b>$txt{'227'}: </b></font></td>
        <td><font size=2>$memsettings[15]</font></td>
      </tr>
    </table>
    </td>
    <td bgcolor="$color{'windowbg'}" class="windowbg" valign="middle" align="center" width="30%">
    $pic<BR><BR>
    <font size=1>$memsettings[12]</font></td>
  </tr><tr height="25">
    <td class="titlebg" bgcolor="$color{'titlebg'}" colspan="2">
    &nbsp;<font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'459'}:</b></font></td>
  </tr><tr>
    <td colspan="2" bgcolor="$color{'windowbg2'}" class="windowbg2" valign="middle" width="100%">
    <form action="$cgi;action=usersrecentposts;username=$useraccount{$INFO{'username'}}" method="POST">
    <font size=2>
    $online<BR><BR>
    <a href="$cgi;action=imsend;to=$useraccount{$INFO{'username'}}">$txt{'688'}</a>.<BR><BR>
    $txt{'460'} <select name="viewscount" size="1">
     <option value="5">5</option>
     <option value="10" selected>10</option>
     <option value="50">50</option>
     <option value="0">$txt{'190'}</option>
    </select> $txt{'461'}. <input type="submit" value="$txt{'462'}">
    </font></form></td>
  </tr>
</table>
~;
	$yytitle = "$txt{'92'} $INFO{'username'}";
	&template;
	exit;
}

sub usersrecentposts {
	my $curuser = $INFO{'username'};
	&FormatUserName($curuser);
	if ($curuser =~ m~/~){ &fatal_error($txt{'224'}); }
	if ($curuser =~ m~\\~){ &fatal_error($txt{'225'}); }
	my $display = $FORM{'viewscount'};
	if( $display =~ /\D/ ) { &fatal_error($txt{'337'}); }
	$display ||= 999999999;
	my( @memset, @categories, %data, %cat, $numfound, $oldestfound, $curcat, %catname, %cataccess, %catboards, $openmemgr, @membergroups, $tmpa, %openmemgr, $curboard, @threads, @boardinfo, $i, $c, @messages, $tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $mns, $mtime, $counter, $board, $notify );

	fopen(FILE, "$memberdir/$curuser.dat") || &fatal_error("$txt{'23'} $curuser.txt");
	@memset = <FILE>;
	fclose(FILE);

	@categories = ();
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);

	&LoadCensorList;	# Load Censor List

	$oldestfound = stringtotime("01/10/37 $txt{'107'} 00:00:00");

	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(FILE, "$boardsdir/$curcat.cat");
		$catname{$curcat} = <FILE>;
		chomp $catname{$curcat};
		$cataccess{$curcat} = <FILE>;
		chomp $cataccess{$curcat};
		@{$catboards{$curcat}} = <FILE>;
		fclose(FILE);
		$openmemgr{$curcat} = 0;
		@membergroups = split( /,/, $cataccess{$curcat} );
		foreach $tmpa (@membergroups) {
			if( $tmpa eq $settings[7]) { $openmemgr{$curcat} = 1; last; }
		}
		if( ! $cataccess{$curcat} || $settings[7] eq 'Administrator' ) {
			$openmemgr{$curcat} = 1;
		}
		unless( $openmemgr{$curcat} ) { next; }
		boardcheck: foreach $curboard (@{$catboards{$curcat}}) {
			chomp $curboard;
			fopen(FILE, "$boardsdir/$curboard.txt");
			@threads = <FILE>;
			fclose(FILE);

			fopen(FILE, "$boardsdir/$curboard.dat");
			@boardinfo = <FILE>;
			fclose(FILE);
			foreach (@boardinfo) {
				chomp;
			}
			@{$boardinfo{$curboard}} = @boardinfo;
			$cat{$curboard} = $curcat;

			threadcheck: for ($i = 0; $i < @threads; $i++) {
				chomp $threads[$i];
				($tnum, $tsub, $tname, $temail, $tdate, $treplies, $tusername, $ticon, $tstate) = split( /\|/, $threads[$i] );
				fopen(FILE, "$datadir/$tnum.txt") || next;
				@messages = <FILE>;
				fclose(FILE);

				for ($c = 0; $c < @messages; $c++) {
					chomp $messages[$c];
					($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns) = split(/\|/,$messages[$c]);
					if ($curuser eq $musername) {
						$mtime = stringtotime($mdate);
						if( $numfound >= $display && $mtime <= $oldestfound ) {
							next boardcheck;
						}
						else {
							$data{$mtime} = [$curboard, $tnum, $c, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns];
							if( $mtime < $oldestfound ) { $oldestfound = $mtime; }
							++$numfound;
						}
					}
				}
			}
		}
	}

	$yymain .= qq~
<p align=left><a href="$cgi;action=viewprofile;username=$useraccount{$curuser}"><font size=2><b>$txt{'92'} $memset[1]</b></font></a></p>
~;
	@messages = sort {$b <=> $a } keys %data;
	if( @messages > $display ) { $#messages = $display - 1; }
	$counter = 1;
	for( $i = 0; $i < @messages; $i++ ) {
		($board, $tnum, $c, $msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip, $message, $mns) = @{ $data{$messages[$i]} };
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$message =~ s~\Q$tmpa\E~$tmpb~gi;
			$msub =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		&wrap;
		$displayname = $mname;
		if($enable_ubbc) { $ns = $mns; if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
		&wrap2;
		if($enable_notification) { $notify = qq~$menusep<a href="$scripturl?board=$board;action=notify;thread=$tnum;start=$c">$img{'notify'}</a>~; }
		$mdate = timeformat($mdate);
		$yymain .= qq~
<table border="0" width="100%" cellspacing="1" $color{'bordercolor'}>
  <tr>
    <td align="left" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2">&nbsp;$counter&nbsp;</font></td>
    <td width="75%" bgcolor="$color{'titlebg'}" class="titlebg"><font class="text1" color="$color{'titletext'}" size="2"><b>&nbsp;$catname{$cat{$board}} / $boardinfo{$board}->[0] / <a href="$scripturl?board=$board;action=display;num=$tnum;start=$c#$c"><font class="text1" color="$color{'titletext'}" size=2><u>$msub</u></font></a></b></font></td>
    <td align="right" bgcolor="$color{'titlebg'}" class="titlebg" nowrap>&nbsp;<font class="text1" color="$color{'titletext'}" size=2>$mdate&nbsp;</font></td>
  </tr><tr height=80>
    <td colspan="3" bgcolor="$color{'windowbg2'}"  class="windowbg2" valign="top"><font size="2">$message</font></td>
  </tr><tr>
    <td colspan="3" bgcolor="$color{'catbg'}" class="catbg"><font size="2">
    &nbsp;<a href="$scripturl?board=$board;action=post;num=$tnum;start=$c;title=Post+reply">$img{'reply'}</a>$menusep<a href="$scripturl?board=$board;action=post;num=$tnum;quote=$c;title=Post+reply">$img{'replyquote2'}</a>$notify
    </font></td>
  </tr>
</table><br>
~;
		++$counter;
	}
if($counter <= 1) { $yymain .= "<font size=2><B>$txt{'755'}</B></font>"; }
else { $yymain .= qq~
<p align=left><a href="$cgi;action=viewprofile;username=$useraccount{$curuser}"><font size=2><b>$txt{'92'} $memset[1]</b></font></a></p></font>
~; }
	$yytitle = "$txt{'458'} $memset[1]";
	&template;
	exit;
}

1;