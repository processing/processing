###############################################################################
# Load.pl                                                                     #
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

$loadplver = "1 Gold - SP 1.4";

sub LoadIMs {
	if ($maintenance && $settings[7] ne 'Administrator') {$username="Guest";}
	if($username ne "Guest" && $username ne '' && $action ne "logout") {
		fopen(IM, "$memberdir/$username.msg");
		@immessages = <IM>;
		fclose(IM);
		$mnum = @immessages;
		if($mnum eq "1") { $yyim = qq~$txt{'152'} <a href="$cgi;action=im">$mnum $txt{'471'}</a>.~; }
		else { $yyim = qq~$txt{'152'} <a href="$cgi;action=im">$mnum $txt{'153'}</a>.~; }
		if($maintenance && $settings[7] eq 'Administrator') { $yyim .= qq~<BR><B>$txt{'616'}</B>~; }
		if (!$user_ip && $settings[7] eq 'Administrator') { $yyim .= qq~<br><b>$txt{'773'}</b>~; }
	}
}

sub LoadBoard {
	my $threadid = $INFO{'num'} || $INFO{'thread'} || $FORM{'threadid'};
	if( $threadid =~ /\D/ ) { &fatal_error($txt{'337'}); }
	if($currentboard ne '') {
		unless( &BoardAccessGet($currentboard) ) { &fatal_error( $txt{'1'} ); }

		fopen(FILE, "$boardsdir/$currentboard.dat") || &fatal_error("400 $txt{'106'}: $txt{'23'} $currentboard.dat");
		@yyBoardInfo =<FILE>;
		fclose(FILE);

		chomp @yyBoardInfo;
		$boardname = $yyBoardInfo[0];

		# Create Hash %moderators with all Moderators of the current board
		foreach(split(/\|/,$yyBoardInfo[2])) {
			fopen(MODERATOR, "$memberdir/$_.dat");
			@modprop = <MODERATOR>;
			fclose(MODERATOR);
			$modprop[1] =~ s/[\n\r]//g;
			$moderators{$_} = $modprop[1];
		}

		if ($threadid ne '' && ! $FORM{'caller'} && $action ne 'imsend') {
			$yyThreadPosition = -1;
			my $found;
			fopen(BOARDFILE, "$boardsdir/$currentboard.txt") || &fatal_error("401 $txt{'106'}: $txt{'23'} $currentboard.txt");
			while( $yyThreadLine = <BOARDFILE> ) {
				++$yyThreadPosition;
				if( $yyThreadLine =~ m~\A$threadid\|~o ) { $found = 1; last; }
			}
			fclose(BOARDFILE);
			unless( $found ) { &fatal_error("$txt{'472'} $threadid : $yyThreadPosition."); }
			chomp $yyThreadLine;
		}
	}
	elsif( $threadid ne '' && $action ne "imsend" ) { &fatal_error("$txt{'472'}"); }
}

sub LoadCensorList {
	fopen(FILE,"$vardir/censor.txt") || &fatal_error("205 $txt{'106'}: $txt{'23'} censor.txt");
	while( chomp( $buffer = <FILE> ) ) {
		($tmpa,$tmpb) = split(/=/,$buffer);
		push(@censored,[$tmpa,$tmpb]);
	}
	fclose(FILE);
}

sub LoadUserSettings {
	if($username ne 'Guest') {
		$validsession = 1;
		if( fopen(FILE, "$memberdir/$username.dat") ) {
			@settings=<FILE>;
			fclose(FILE);
			for( $_ = 0; $_ < @settings; $_++ ) {
				$settings[$_] =~ s~[\n\r]~~g;
			}
			$slavepwseed = substr($password, length($password)-4,length($password));
			$decryptpw = substr($password,0, length($password)-4);
			$spass = crypt($settings[0],$slavepwseed);
			$slaveseed = substr($cookiesession, length($cookiesession)-4,length($cookiesession));
			$decryptsession = substr($cookiesession,0, length($cookiesession)-4);
			if (&encode_session($user_ip,$slaveseed) ne $decryptsession && $cookiesession ne ""){$validsession = 0;}
			if(($spass ne $decryptpw && $action ne 'logout') || $validsession == 0) { $username = ''; }
			else {
				$realname = $settings[1];
				$realemail = $settings[2];
			}
		}
		else { $username = ''; }
	}
	unless($username) {
		&UpdateCookie("delete");
		$username = 'Guest';
		$password = '';
		@settings = ();
		$realname = '';
		$realemail = '';
		$ENV{'HTTP_COOKIE'} = '';
	}
	&FormatUserName($username);
}

sub FormatUserName {
	my $user = $_[0];
	if( $useraccount{$user} ) { return; }
	$useraccount{$user} = $user;
	$useraccount{$user} =~ s~\%~%25~g;
	$useraccount{$user} =~ s~\#~%23~g;
	$useraccount{$user} =~ s~\+~%2B~g;
	$useraccount{$user} =~ s~\,~%2C~g;
	$useraccount{$user} =~ s~\-~%2D~g;
	$useraccount{$user} =~ s~\.~%2E~g;
	$useraccount{$user} =~ s~\@~%40~g;
	$useraccount{$user} =~ s~\^~%5E~g;
}

sub LoadUser {
	my $user = $_[0];
	unless( exists $userprofile{$user} ) {
		fopen(FILEAUSER, "$memberdir/$user.dat") || return 0;
		@{$userprofile{$user}} = <FILEAUSER>;
		fclose(FILEAUSER);
		for( $_ = 0; $_ < @{$userprofile{$user}}; $_++ ) {
			chomp $userprofile{$user}->[$_];
		}
		&FormatUserName($user);
	}
	return 1;
}


sub LoadUserDisplay {
	my $user= $_[0];
	if(exists $userprofile{$user}) { if($yyUDLoaded{$user}) { return 1; } }
	else {
		&LoadUser($user);
		unless(exists $userprofile{$user}) { return 0 ; }
	}
	&LoadCensorList;	# Load censor list

	$userpic_tmpwidth ||= $userpic_width ? qq~ width="$userpic_width"~ : '';
	$userpic_tmpheight ||= $userpic_height ? qq~ height="$userpic_height"~ : '';

	if($userprofile{$user}->[4] !~ m~\Ahttp://~) { $userprofile{$user}->[4] = "http://$userprofile{$user}->[4]"; }
	if($sm) { $userprofile{$user}->[4] = $userprofile{$user}->[4] && $userprofile{$user}->[4] ne q~http://~ ? qq~ <a href="$userprofile{$user}->[4]" target="_blank">$img{'website_sm'}</a>$menusep~ : ''; }
	else { $userprofile{$user}->[4] = $userprofile{$user}->[4] && $userprofile{$user}->[4] ne q~http://~ ? qq~<a href="$userprofile{$user}->[4]" target="_blank">$img{'website'}</a>$menusep~ : ''; }

	$userprofile{$user}->[5] =~ s~\&\&~<br>~g;
	$userprofile{$user}->[5] = $userprofile{$user}->[5] ? qq~<hr width="100%" size="1" class="hr"><font size="1">$userprofile{$user}->[5]</font>~ : '';
	# do some ubbc on the signature
	$message = $userprofile{$user}->[5];
	$displayname = $userprofile{$user}->[1];
	if($enable_ubbc) { if(!$yyYaBBCloaded) { require "$sourcedir/YaBBC.pl"; } &DoUBBC; }
	$userprofile{$user}->[5] = $message;

	if($userprofile{$user}->[8] ne "" && $userprofile{$user}->[8] !~ m~\D~) {
		$icqad{$user} = qq~<a href="http://web.icq.com/$userprofile{$user}->[8]" target="_blank"><img src="$imagesdir/icqadd.gif" alt="$userprofile{$user}->[8]" BORDER=0></a>~;
		$userprofile{$user}->[8] = qq~<a href="$cgi;action=icqpager;UIN=$userprofile{$user}->[8]" target="_blank"><img src="http://web.icq.com/whitepages/online?icq=$userprofile{$user}->[8]&img=5" alt="$userprofile{$user}->[8]" border="0"></a>~;
	}
	else {
		$icqad{$user} = '';
		$userprofile{$user}->[8] = '';
	}

	$userprofile{$user}->[9] = $userprofile{$user}->[9] ? qq~<a href="aim:goim?screenname=$userprofile{$user}->[9]&message=Hi.+Are+you+there?"><img src="$imagesdir/aim.gif" alt="$userprofile{$user}->[9]" border="0"></a>~ : '';

	if($userprofile{$user}->[10] ne "") {
		$yimon{$user} = qq~<img SRC="http://opi.yahoo.com/online?u=$userprofile{$user}->[10]&m=g&t=0" BORDER=0 alt="">~;
		$userprofile{$user}->[10] = $userprofile{$user}->[10] ? qq~<a href="http://edit.yahoo.com/config/send_webmesg?.target=$userprofile{$user}->[10]" target="_blank"><img src="$imagesdir/yim.gif" alt="$userprofile{$user}->[10]" border="0"></a>~ : '';
	}

	if($showgenderimage && $userprofile{$user}->[11]) {
		$userprofile{$user}->[11] = $userprofile{$user}->[11] =~ m~Female~i ? 'female' : 'male';
		$userprofile{$user}->[11] = $userprofile{$user}->[11] ? qq~<font size="1">$txt{'231'}: <img src="$imagesdir/$userprofile{$user}->[11].gif" border="0" alt="$userprofile{$user}->[11]"><br></font>~ : '';
	}
	else { $userprofile{$user}->[11] = ''; }

	$userprofile{$user}->[12] = $showusertext ? qq~$userprofile{$user}->[12]<br>~ : '';

	if($showuserpic && $allowpics) {
		$userprofile{$user}->[13] ||= 'blank.gif';
		$userprofile{$user}->[13] = $userprofile{$user}->[13] =~ m~\A[\s\n]*http://~i ? qq~<br><img src="$userprofile{$user}->[13]"$userpic_tmpwidth$userpic_tmpheight border="0" alt=""><br><br>~ : qq~<br><img src="$facesurl/$userprofile{$user}->[13]" border="0" alt=""><br>~;
	}
	else { $userprofile{$user}->[13] = '<BR>'; }

	### Censor it ###
	foreach (@censored) {
		($tmpa,$tmpb) = @{$_};
		$userprofile{$user}->[5] =~ s~\Q$tmpa\E~$tmpb~gi;
		$userprofile{$user}->[12] =~ s~\Q$tmpa\E~$tmpb~gi;
	}
	
	if($userprofile{$user}->[6] > $GodPostNum) {
		$memberinfo{$user} = "$membergroups[6]";
		$memberstar{$user} = qq~<img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*">~;
	}
	elsif($userprofile{$user}->[6] > $SrPostNum) {
		$memberinfo{$user} = "$membergroups[5]";
		$memberstar{$user} = qq~<img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*">~;
	}
	elsif($userprofile{$user}->[6] > $FullPostNum) {
		$memberinfo{$user} = "$membergroups[4]";
		$memberstar{$user} = qq~<img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*">~;
	}
	elsif($userprofile{$user}->[6] > $JrPostNum) {
		$memberinfo{$user} = "$membergroups[3]";
		$memberstar{$user} = qq~<img src="$imagesdir/star.gif" border="0" alt="*"><img src="$imagesdir/star.gif" border="0" alt="*">~;
	}
	else {
		$memberinfo{$user} = "$membergroups[2]";
		$memberstar{$user} = qq~<img src="$imagesdir/star.gif" border="0" alt="*">~;
	}
	if(exists $moderators{$user} && $sender ne "im") {
		$modinfo{$user} = "<i>$membergroups[1]</i><BR>";
		$memberstar{$user} = qq~<img src="$imagesdir/starmod.gif" border="0" alt="*"><img src="$imagesdir/starmod.gif" border="0" alt="*"><img src="$imagesdir/starmod.gif" border="0" alt="*"><img src="$imagesdir/starmod.gif" border="0" alt="*"><img src="$imagesdir/starmod.gif" border="0" alt="*">~;
	}
	if($userprofile{$user}->[7] eq 'Administrator') {
		$memberstar{$user} = qq~<img src="$imagesdir/staradmin.gif" border="0" alt="*"><img src="$imagesdir/staradmin.gif" border="0" alt="*"><img src="$imagesdir/staradmin.gif" border="0" alt="*"><img src="$imagesdir/staradmin.gif" border="0" alt="*"><img src="$imagesdir/staradmin.gif" border="0" alt="*">~;
		$memberinfo{$user} = "<B>$membergroups[0]</B>";
	}
	if($userprofile{$user}->[7] && $userprofile{$user}->[7] ne 'Administrator') { $groupinfo{$user} = "$userprofile{$user}->[7]<BR>"; }
	if($userprofile{$user}->[7] ne 'Administrator') {
		$memberinfo{$user} = "$modinfo{$user}$groupinfo{$user}$memberinfo{$user}";
	}

	if($userprofile{$user}->[6] > 100000) { $userprofile{$user}->[6] = "$txt{'683'}"; }

	$yyUDLoaded{$user} = 1;
	return 1;
}

sub LoadCookie {
	foreach (split(/; /,$ENV{'HTTP_COOKIE'})) {
		$_ =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
		($cookie,$value) = split(/=/);
		$yyCookies{$cookie} = $value;
	}
	if($yyCookies{$cookiepassword}) {
		$password = $yyCookies{$cookiepassword};
		$username = $yyCookies{$cookieusername} || 'Guest';
		$cookiesession = $yyCookies{$session_id};
	} else {
		$password = '';
		$username = 'Guest';
		$cookiesession = '';
	}
}

sub UpdateCookie {
	my($what,$user,$passw,$sessionval,$pathval,$expire) =@_;
	my($valid,$expiration);
	$valid = 0;
	if ($what eq "delete"){
		$expiration = "Thursday, 01-Jan-1970 00:00:00 GMT";
		if($pathval eq "") {$pathval = qq~/~;}
		$valid = 1;
	} elsif ($what eq "write"){
		$expiration = $expire;
		if($pathval eq "") {$pathval = qq~/~;}
		$valid = 1;
	}
	if($expire eq "persistent"){$expiration = "Sunday, 17-Jan-2038 00:00:00 GMT";}

	if ($valid == 1){
		$yySetCookies1 = cookie(-name    =>   "$cookieusername",
					-value   =>   "$user",
					-path    =>   "$pathval",
					-expires =>   "$expiration");
		$yySetCookies2 = cookie(-name    =>   "$cookiepassword",
					-value   =>   "$passw",
					-path    =>   "$pathval",
					-expires =>   "$expiration");
		$yySetCookies3 = cookie(-name    =>   "$session_id",
					-value   =>   "$sessionval",
					-path    =>   "$pathval",
					-expires =>   "$expiration");
	}
}

sub LoadAdmins {
	&is_admin;
	my (@members, $curentry);

	$administrators = '';
	fopen(FILE, "$memberdir/memberlist.txt");
	@members = <FILE>;
	fclose(FILE);
	foreach $curentry (@members) {
		chomp $curentry;
		&LoadUser($curentry);
		if($userprofile{$curentry}->[7] eq 'Administrator') {
			$administrators .= qq~ <a href="$scripturl?action=viewprofile;username=$useraccount{$curentry}">$userprofile{$curentry}->[1]</a><font size="1">,</font> \n~;
		}
	}
	$administrators =~ s~<font size="1">,</font> \n\Z~~;
}

sub LoadLogCount {
	&is_admin;
	my(@log);
	fopen(LOG, "$vardir/clicklog.txt");
	@log = <LOG>;
	fclose(LOG);
	$yyclicks = @log;
}

sub loadfiles {
	require "$boarddir/$language";
	require "$sourcedir/AdminEdit.pl";
	require "$sourcedir/BoardIndex.pl";
	require "$sourcedir/Display.pl";
	require "$sourcedir/ICQPager.pl";
	require "$sourcedir/InstantMessage.pl";
	require "$sourcedir/LockThread.pl";
	require "$sourcedir/LogInOut.pl";
	require "$sourcedir/Maintenance.pl";
	require "$sourcedir/ManageBoards.pl";
	require "$sourcedir/ManageCats.pl";
	require "$sourcedir/Memberlist.pl";
	require "$sourcedir/MessageIndex.pl";
	require "$sourcedir/ModifyMessage.pl";
	require "$sourcedir/MoveThread.pl";
	require "$sourcedir/Notify.pl";
	require "$sourcedir/Post.pl";
	require "$sourcedir/Printpage.pl";
	require "$sourcedir/Profile.pl";
	require "$sourcedir/Recent.pl";
	require "$sourcedir/Register.pl";
	require "$sourcedir/RemoveOldThreads.pl";
	require "$sourcedir/RemoveThread.pl";
	require "$sourcedir/Search.pl";
	require "$sourcedir/Security.pl";
	require "$sourcedir/SendTopic.pl";
	require "$sourcedir/YaBBC.pl";
}

1;