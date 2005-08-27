###############################################################################
# Memberlist.pl                                                               #
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

$memberlistplver = "1 Gold - SP 1.4";

if($username eq "Guest") { &fatal_error("$txt{'223'}"); }

# Load the membergroups list.
fopen(FILE, "$vardir/membergroups.txt") || &fatal_error("100 $txt{'106'}: $txt{'23'} membergroups.txt");
@membergroups = <FILE>;
fclose(FILE);

if($action eq "mlall") { $Sort .= qq($txt{'303'} | ); } else { $Sort .= qq(<a href="$cgi;action=mlall"><font size=2 class="text1" color="$color{'titletext'}">$txt{'303'}</font></a> | ); }
if($action eq "mlletter") { $Sort .= qq($txt{'304'} | ); } else { $Sort .= qq(<a href="$cgi;action=mlletter"><font size=2 class="text1" color="$color{'titletext'}">$txt{'304'}</font></a> | ); }
if($action eq "mltop") { $Sort .= qq($txt{'305'} $txt{'411'} $TopAmmount $txt{'306'}); } else { $Sort .= qq(<a href="$cgi;action=mltop"><font size=2 class="text1" color="$color{'titletext'}">$txt{'305'} $txt{'411'} $TopAmmount $txt{'306'}</font></a>); }

if($action eq "mlletter") {
	$page = "a"; $showpage = "A";
	while($page ne "z") {
		$LetterLinks .= qq(<a href="$scripturl?action=mlletter;letter=$page">$showpage&nbsp;</a> );
		$page++; $showpage++;
	}
	$LetterLinks .= qq(<a href="$scripturl?action=mlletter;letter=z">Z</a>  <a href="$scripturl?action=mlletter;letter=other">$txt{'800'}</a> );
}

$TableHeader .= qq(
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
<tr>
	<td class="titlebg" bgcolor="$color{'titlebg'}" colspan="7"><b><font size=2 class="text1" color="$color{'titletext'}">$Sort</font></b></td>
</tr>
);
if($LetterLinks ne "") {
	$TableHeader .= qq(<tr>
		<td class="catbg" bgcolor="$color{'catbg'}" colspan="7"><b><font size=2>$LetterLinks</td>
	</tr>
	);
}
$TableHeader .= qq(<tr>
	<td class="catbg" bgcolor="$color{'catbg'}" width="200"><b><font size=2>$txt{'35'}</font></b></td>
	<td class="catbg" bgcolor="$color{'catbg'}"><b><font size=2>$txt{'307'}</font></b></td>
	<td class="catbg" bgcolor="$color{'catbg'}"><b><font size=2>$txt{'96'}</font></b></td>
	<td class="catbg" bgcolor="$color{'catbg'}"><b><font size=2>$txt{'86'}</font></b></td>
	<td class="catbg" bgcolor="$color{'catbg'}"><b><font size=2>$txt{'87'}</font></b></td>
	<td class="catbg" bgcolor="$color{'catbg'}"><b><font size=2>$txt{'21'}</font></b></td>
</tr>
);

$TableFooter = qq~</table>~;

sub MLAll {
	if($username eq "Guest") { &fatal_error("$txt{'223'}"); }
	# Get the number of members
	fopen(FILE, "$memberdir/memberlist.txt");
	@memberlist = <FILE>;
	$memcount = @memberlist;
	@membername = @memberlist;
	fclose(FILE);

	if($INFO{'start'} eq "") { $start=0; } else { $start="$INFO{'start'}"; }
	$numshown=0;
	$numbegin = ($start + 1);
	$numend = ($start + $MembersPerPage);
	if($numend > $memcount) { $numend = $memcount; }
	$b = $start;

	$yymain .= qq~
		<center><font size="2" class="nav"><B>$txt{'308'} $numbegin $txt{'311'} $numend ($txt{'309'} $memcount $txt{'310'})</B></font></center><BR>
	~;
	$yymain .= qq~$TableHeader~;

	while(($numshown < $MembersPerPage)) {
		$numshown++;
		$c=0;
		$pages="";
		chomp(@membername);
		$tempname = $membername[$b];
		$membername[$b] =~ s/ //gi;
		$membername[$b] =~ s/\n//gi;
		$name = $membername[$b];
		$b++;

		@member = ();
		$Bar = "";
		$ICQ = "";
		fopen(MEMBERFILEREAD,"$memberdir/$name.dat");
		@member = <MEMBERFILEREAD>;
		fclose(MEMBERFILEREAD);
		chomp @member;
		&FormatUserName($name);
	if($member[4] ne "" && $member[4] !~ m~\Ahttp://\S*~) { $member[4] = "<a href=\"http://$member[4]\" target=\"_blank\">$member[3]</a>"; }
	elsif($member[4] ne "") { $member[4] = "<a href=\"$member[4]\" target=\"_blank\">$member[3]</a>"; }

		$barchart = ($member[6] / 5);
		if ($barchart < 1) {$Bar = "$Bar";}
		elsif ($barchart > 100) {
			$Bar = qq~<img src="$imagesdir/bar.gif" width=100 height=15 alt="" border="0">~;
		}
		else {
			$Bar = qq~<img src="$imagesdir/bar.gif" width=$barchart height=15 alt="" border="0">~;
		}

		if ($Bar eq "") { $Bar="&nbsp;"; }
		if($member[6] > 100000) { $member[6] = "$txt{'683'}"; }

		if($member[7] eq "Administrator") { $member[7] = "$membergroups[0]"; }

		if($tempname)
		{
			$yymain .= qq~
			<tr>
				<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><a href="$cgi;action=viewprofile;username=$useraccount{$name}">$member[1]</a></font></td>
				~;
				if ($member[19] eq "checked" && $settings[7] ne "Administrator" && $allow_hide_email eq 1) { $yymain .= qq~
					<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><i>$txt{'722'}</i></font></td>
				~; } else { $yymain .= qq~
					<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><a href="mailto:$member[2]">$member[2]</a></font></td>
				~; }
				$yymain .= qq~
				<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[4]</font>&nbsp;</td>
				<td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><font size=2>$member[6]</font>&nbsp;</td>
				<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[7]</font>&nbsp;</td>
				<td class="windowbg" bgcolor="$color{'windowbg'}">$Bar</td>
			</tr>
			~;
		}
	}
	$yymain .= qq~$TableFooter~;

	# Build the page links list.
	$postdisplaynum = 8;	# max number of pages to display
	$max = $memcount;
	$start = $INFO{'start'} || 0;
	$start = ( int( $start / $MembersPerPage ) ) * $MembersPerPage;
	$tmpa = 1;
	$tmpx = int( $max / $MembersPerPage );
	if ($start >= (($postdisplaynum-1) * $MembersPerPage)) { $startpage = $start - (($postdisplaynum-1) * $MembersPerPage); $tmpa = int( $startpage /$MembersPerPage ) + 1; }
	if ($max >= $start + ($postdisplaynum * $MembersPerPage)) { $endpage = $start + ($postdisplaynum * $MembersPerPage); } else { $endpage = $max }
	if ($startpage > 0) { $pageindex = qq~<a href="$cgi;action=mlall;start=0">1</a>&nbsp;...&nbsp;~; }
	if ($startpage == $MembersPerPage) { $pageindex = qq~<a href="$cgi;action=mlall;start=0">1</a>&nbsp;~;}
	for( $counter = $startpage; $counter < $endpage; $counter += $MembersPerPage ) {
		$pageindex .= $start == $counter ? qq~<b>$tmpa</b>&nbsp;~ : qq~<a href="$cgi;action=mlall;start=$counter">$tmpa</a>&nbsp;~;
		$tmpa++;
	}
	$tmpx = $max - $MembersPerPage;
	$outerpn = int($tmpx / $MembersPerPage) + 0;
	$lastpn = int($memcount / $MembersPerPage) + 1;
	$lastptn = ($lastpn - 1) * $MembersPerPage;
	if ($endpage < $max - ($MembersPerPage) ) {$pageindexadd = qq~&nbsp;...&nbsp;~;}
	if ($endpage != $max) {$pageindexadd .= qq~&nbsp;<a href="$cgi;action=mlall;start=$lastptn">$lastpn</a>~;}
	$pageindex .= $pageindexadd;

	$yymain .= qq~
	<table border="0" width="100%" cellpadding="0" cellspacing="0">
	<tr>
		<td><font size="2"><b>$txt{'139'}:</b>
		$pageindex
		</font></td>
	</tr>
	</table>
	~;

	$yytitle = "$txt{'308'} $numbegin $txt{'311'} $numend";
	&template;
	exit;
}

sub MLByLetter {
	if($username eq "Guest") { &fatal_error("$txt{'223'}"); }
	$yymain .= qq~$TableHeader~;

	$letter = $INFO{'letter'};
	if($INFO{'start'} eq "") { $start=0; } else { $start="$INFO{'start'}"; }

	unless(!$letter)
	{
		fopen(MEMBERSLISTREAD,"$memberdir/memberlist.txt");
		while(chomp($memberfile=<MEMBERSLISTREAD>)) {
			fopen(MEMBERFILEREAD,"$memberdir/$memberfile.dat");
			@member = <MEMBERFILEREAD>;
			fclose(MEMBERFILEREAD);
			chomp @member;

			$SearchName = $member[1];
			$SearchName = substr $SearchName,0,1;
			$SearchName = lc $SearchName;
			if($letter eq "other" && (($SearchName lt "a") || ($SearchName gt "z"))) {
				push(@ToShow,$memberfile);
			} elsif($SearchName eq $letter) {
				push(@ToShow,$memberfile);
			}
		}
		fclose(MEMBERSLISTREAD);
		@ToShow = sort { uc($a) cmp uc($b)} @ToShow;
		$memcount=@ToShow;
		$numshown=0; $b=$start;

		unless ($memcount == 0)	{
		while(($numshown < $MembersPerPage))
		{
			$membername=@ToShow[$b];
			if ($membername ne "")
			{
			@member = ();
			$Bar = "";
			$ICQ = "";
			fopen(MEMBERFILEREAD,"$memberdir/$membername.dat");
			@member = <MEMBERFILEREAD>;
			fclose(MEMBERFILEREAD);
			chomp @member;
			&FormatUserName($membername);
			if($member[4] ne "" && $member[4] !~ m~\Ahttp://\S*~) { $member[4] = "<a href=\"http://$member[4]\" target=\"_blank\">$member[3]</a>"; }
			elsif($member[4] ne "") { $member[4] = "<a href=\"$member[4]\" target=\"_blank\">$member[3]</a>"; }
			$barchart = ($member[6] / 5);
			if ($barchart < 1) {$Bar = "$Bar";}
			elsif ($barchart > 100) {$Bar = qq~<img src="$imagesdir/bar.gif" width=100 height=15 alt="" border="0">~;}
			else {
			$Bar = qq~<img src="$imagesdir/bar.gif" width=$barchart height=15 alt="" border="0">~;
			}
			$member[8] =~ s/[\n\r]//g;
			if ($Bar eq "") { $Bar="&nbsp;"; }

			if($member[6] > 100000) { $member[6] = "$txt{'683'}"; }

			if($member[7] eq "Administrator") { $member[7] = "$membergroups[0]"; }

			$yymain .= qq~
				<tr>
					<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><a href="$cgi;action=viewprofile;username=$useraccount{$membername}">$member[1]</a></font></td>
			~;
			if ($member[19] eq "checked" && $settings[7] ne "Administrator" && $allow_hide_email eq 1) { $yymain .= qq~
				<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><i>$txt{'722'}</i></font></td>
			~; } else { $yymain .= qq~
				<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><a href="mailto:$member[2]">$member[2]</a></font></td>
			~; }
			$yymain .= qq~
			<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[4]</font>&nbsp;</td>
			<td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><font size=2>$member[6]</font>&nbsp;</td>
			<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[7]</font>&nbsp;</td>
			<td class="windowbg" bgcolor="$color{'windowbg'}">$Bar</td>
			</tr>
			~;
			}
			$numshown++;
			$b++;
		}
		}
	}
	if(!$letter) {$yymain .= qq~ <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="7" align="center"><br><b>$txt{'759'}</b><br><br></td>~;}
	if($memcount == 0 && $letter) {$yymain .= qq~ <td class="windowbg" bgcolor="$color{'windowbg'}" colspan="7" align="center"><br><b>$txt{'760'}</b><br><br></td>~;}
	$yymain .= qq~$TableFooter~;

unless ($memcount == 0)
{
	# Build the page links list.
	$postdisplaynum = 4;	# max number of pages to display
	$max = $memcount;
	$start = $INFO{'start'} || 0;
	$start = ( int( $start / $MembersPerPage ) ) * $MembersPerPage;
	$tmpa = 1;
	$tmpx = int( $max / $MembersPerPage );
	if ($start >= (($postdisplaynum-1) * $MembersPerPage)) { $startpage = $start - (($postdisplaynum-1) * $MembersPerPage); $tmpa = int( $startpage /$MembersPerPage ) + 1; }
	if ($max >= $start + ($postdisplaynum * $MembersPerPage)) { $endpage = $start + ($postdisplaynum * $MembersPerPage); } else { $endpage = $max }
	if ($startpage > 0) { $pageindex = qq~<a href="$cgi;action=mlletter;letter=$letter;start=0">1</a>&nbsp;...&nbsp;~; }
	if ($startpage == $MembersPerPage) { $pageindex = qq~<a href="$cgi;action=mlletter;letter=$letter;start=0">1</a>&nbsp;~;}
	for( $counter = $startpage; $counter < $endpage; $counter += $MembersPerPage ) {
		$pageindex .= $start == $counter ? qq~<b>$tmpa</b>&nbsp;~ : qq~<a href="$cgi;action=mlletter;letter=$letter;start=$counter">$tmpa</a>&nbsp;~;
		$tmpa++;
	}
	$tmpx = $max - $MembersPerPage;
	$outerpn = int($tmpx / $MembersPerPage) + 0;
	$lastpn = int($memcount / $MembersPerPage) + 1;
	$lastptn = ($lastpn - 1) * $MembersPerPage;
	if ($endpage < $max - ($MembersPerPage) ) {$pageindexadd = qq~&nbsp;...&nbsp;~;}
	if ($endpage != $max) {$pageindexadd .= qq~&nbsp;<a href="$cgi;action=mlletter;letter=$letter;start=$lastptn">$lastpn</a>~;}
	$pageindex .= $pageindexadd;

	$yymain .= qq~
	<table border="0" width="100%" cellpadding="0" cellspacing="0">
	<tr>
		<td><font size="2"><b>$txt{'139'}:</b>
		$pageindex
		</font></td>
	</tr>
	</table>
	~;
}
	$yytitle = "$txt{'312'}";
	&template;
	exit;
}

sub MLTop {
	if($username eq "Guest") { &fatal_error("$txt{'223'}"); }
	$yymain .= qq~$TableHeader~;
	%TopMembers = ();
	fopen(MEMBERLISTREAD,"$memberdir/memberlist.txt");
		@member = ();
		while(chomp($membername=<MEMBERLISTREAD>)) {
			fopen(MEMBERFILE,"$memberdir/$membername.dat");
				@member = <MEMBERFILE>;
			fclose(MEMBERFILE);
			chomp @member;

			$TopMembers{$membername} = $member[6];
		}
	fclose(MEMBERLISTREAD);

	my @toplist = sort {$TopMembers{$a} <=> $TopMembers{$b}} keys %TopMembers;
	@toplist = reverse @toplist;
	$TopListNum = $TopAmmount - 1;

for ($i=0;$i<=$TopListNum;$i++) {
		@member = ();
		$Bar = "";
		$ICQ = "";
		$membername = @toplist[$i];
		fopen(MEMBERFILEREAD,"$memberdir/$membername.dat");
			@member = <MEMBERFILEREAD>;
		fclose(MEMBERFILEREAD);
		&FormatUserName($membername);
		if($member[4] ne "" && $member[4] !~ m~\Ahttp://\S*~) { $member[4] = "<a href=\"http://$member[4]\" target=\"_blank\">$member[3]</a>"; }
		elsif($member[4] ne "") { $member[4] = "<a href=\"$member[4]\" target=\"_blank\">$member[3]</a>"; }
		chomp @member;
		if($member[1] ne "") {
			$barchart = ($member[6] / 5);
			if ($barchart < 1) {$Bar = "$Bar";}
			elsif ($barchart > 100) {$Bar = qq~<img src="$imagesdir/bar.gif" width=100 height=15 alt="" border="0">~;}
			else {
			$Bar = qq~<img src="$imagesdir/bar.gif" width=$barchart height=15 alt="" border="0">~;
			}
			$member[8] =~ s/[\n\r]//g;

			if($member[8] ne "") { $ICQ = qq~<a href="$cgi;action=icqpager;UIN=$memset[8]" target="_blank"><img src="http://web.icq.com/whitepages/online?icq=$member[8]&img=5" alt ="$member[8]" border=0></a>~; }
			if ($Bar eq "") { $Bar="&nbsp;"; }
			if($member[6] > 100000) { $member[6] = "$txt{'683'}"; }

			if($member[7] eq "Administrator") { $member[7] = "$membergroups[0]"; }

			$yymain .= qq~
	<tr>
		<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2><a href="$cgi;action=viewprofile;username=$useraccount{$membername}">$member[1]</a></font></td>
		~;
		if ($member[19] eq "checked" && $settings[7] ne "Administrator" && $allow_hide_email eq 1) { $yymain .= qq~
			<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><i>$txt{'722'}</i></font></td>
		~; } else { $yymain .= qq~
			<td class="windowbg2" bgcolor="$color{'windowbg2'}"><font size=2><a href="mailto:$member[2]">$member[2]</a></font></td>
		~; }
		$yymain .= qq~
		<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[4]</font>&nbsp;</td>
		<td class="windowbg2" bgcolor="$color{'windowbg2'}" align="center"><font size=2>$member[6]</font>&nbsp;</td>
		<td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>$member[7]</font>&nbsp;</td>
		<td class="windowbg" bgcolor="$color{'windowbg'}">$Bar</td>
	</tr>
			~;
		}
	}
	$yymain .= qq~$TableFooter~;
	$yytitle = "$txt{'313'} $TopAmmount $txt{'314'}";
	&template;
	exit;
}

1;