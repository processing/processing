###############################################################################
# Subs.pl                                                                     #
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

$subsplver = "1 Gold - SP 1.4";

use subs 'exit';
$yymain = "";	# set body start to blank

&readform;	# parse the query
&get_date;	# get the current date/time

$currentboard = $INFO{'board'};
if ($currentboard =~ m~/~){ &fatal_error($txt{'399'}); }
if ($currentboard =~ m~\\~){ &fatal_error($txt{'400'}); }
if ($currentboard ne '' && $currentboard !~ /\A[\s0-9A-Za-z#%+,-\.:=?@^_]+\Z/){ &fatal_error($txt{'399'}); }
$pwseed ||= 'yy';

$yysecurity = qq~
<font size="2" color="#FF0000"><strong>Now that you have installed YaBB, you should secure it!<br> 
Please click <a href="http://faq.yabbforum.com/index.php?op=view&t=68" target="_blank">here</a> to read how to make your YaBB secure!</strong><br> 
To remove this message, go into the Admin Center and choose the template editor. From there you can remove the &lt;yabb security&gt; tag.</font>
~;

$user_ip = $ENV{'REMOTE_ADDR'};
if ($user_ip eq "127.0.0.1")
{
	if    ($ENV{'HTTP_CLIENT_IP'} && $ENV{'HTTP_CLIENT_IP'} ne "127.0.0.1") {$user_ip = $ENV{'HTTP_CLIENT_IP'};}
	elsif ($ENV{'X_CLIENT_IP'} && $ENV{'X_CLIENT_IP'} ne "127.0.0.1") {$user_ip = $ENV{'X_CLIENT_IP'};}
	elsif ($ENV{'HTTP_X_FORWARDED_FOR'} && $ENV{'HTTP_X_FORWARDED_FOR'} ne "127.0.0.1") {$user_ip = $ENV{'HTTP_X_FORWARDED_FOR'};}
}

$session_id = "SID";
$sessiontime = int(time);
$masterseed= substr($sessiontime, length($sessiontime)-4,length($sessiontime));
$formsession = &encode_session($user_ip,$masterseed);
$formsession .= $masterseed;

$scripturl = qq~$boardurl/YaBB.$yyext~;
$cgi = qq~$scripturl?board=$currentboard~;

sub exit {
	local $| = 1;
	local $\ = '';
	print '';
	CORE::exit( $_[0] || 0 );
}

sub header {
	my %params = @_;
	my $ret = "";
	if ($params{'-status'}) {
		if ($yyIIS) {
 			$ret .= "HTTP/1.0 $params{'-status'}\n";
		} else {
			$ret .= "Status: $params{'-status'}\n";
		}
	}
	$ret .= qq~Cache-Control: no-cache, must-revalidate\n~;
	$ret .= qq~Pragma: no-cache\n~;
	if ($params{'-cookie'}) {
		my(@cookie) = ref($params{'-cookie'}) && ref($params{'-cookie'}) eq 'ARRAY' ? @{$params{'-cookie'}} : $params{'-cookie'};
		foreach (@cookie) {
			$ret .= "Set-Cookie: $_\n";
		}
	}
	if ($params{'-location'}) {
		$ret .= "Location: $params{'-location'}\n";
	}
	$params{'-charset'} = "; charset=$params{'-charset'}" if $params{'-charset'};
	$params{'Content-Encoding'} = "Content-Encoding: $params{'Content-Encoding'}\n" if $params{'Content-Encoding'};
	$ret .= "$params{'Content-Encoding'}Content-Type: text/html$params{'-charset'}\r\n\r\n";
	return $ret;
}

sub cookie {
	my %params = @_;

	if ($params{'-expires'} =~ /\+(\d+)m/) {
		my ($sec,$min,$hour,$mday,$mon,$year,$wday) = gmtime(time + $1*60);

		$year += 1900;
		my @mos = ("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec");
		my @dys = ("Sun","Mon","Tue","Wed","Thu","Fri","Sat");
		$mon = $mos[$mon];
		$wday = $dys[$wday];

		$params{'-expires'} = sprintf("%s, %02i-%s-%04i %02i:%02i:%02i GMT", $wday, $mday, $mon, $year, $hour, $min, $sec);
	}

	$params{'-path'} = " path=$params{'-path'};" if $params{'-path'};
	$params{'-expires'} = " expires=$params{'-expires'};" if $params{'-expires'};

	return "$params{'-name'}=$params{'-value'};$params{'-path'}$params{'-expires'}";
}

sub redirectexit {
	if($yySetCookies1 || $yySetCookies2 || $yySetCookies3) {
		print header(
			-status=>'302 Moved Temporarily',
			-cookie=>[$yySetCookies1,$yySetCookies2,$yySetCookies3],
			-location=>$yySetLocation
		); 
	} else { 
		print header(
			-status=>'302 Moved Temporarily',
			-location=>$yySetLocation
		);
	}
	exit;
}

sub redirectinternal {
	&LoadIMs;		# Load IM's
	if($currentboard) {
		if($INFO{'num'}) { require "$sourcedir/Display.pl"; &Display; }
		else { require "$sourcedir/MessageIndex.pl"; &MessageIndex; }
	}
	else { require "$sourcedir/BoardIndex.pl"; &BoardIndex; }
	exit;
}

sub template {
	if($yySetCookies1 || $yySetCookies2 || $yySetCookies3) { 
		print header(
			-status=>'200 OK',
			-cookie=>[$yySetCookies1,$yySetCookies2,$yySetCookies3],
			-charset=>$yycharset
		);
	} else { 
		print header(
			-status=>'200 OK',
			-charset=>$yycharset
		);
	}
	$yyposition = $yytitle; $yytitle = "$mbname - $yytitle";

	$yymenu = qq~<a href="$scripturl">$img{'home'}</a><!--$menusep<a href="$helpfile" target="_blank" style="cursor:help;">$img{'help'}</a>-->$menusep<a href="$cgi;action=search">$img{'search'}</a>$menusep<a href="$scripturl?action=mlall">$img{'memberlist'}</a>~;
	if($settings[7] eq 'Administrator') { $yymenu .= qq~$menusep<a href="$cgi;action=admin">$img{'admin'}</a>~; }
	if($username eq 'Guest') { $yymenu .= qq~$menusep<a href="$scripturl?action=login">$img{'login'}</a>$menusep<a href="$scripturl?action=register">$img{'register'}</a>~;
	} else {
		$yymenu .= qq~$menusep<a href="$cgi;action=profile;username=$useraccount{$username}">$img{'profile'}</a>~;
		if($enable_notification) { $yymenu .= qq~$menusep<a href="$cgi;action=shownotify">$img{'notification'}</a>~; }
		$yymenu .= qq~$menusep<a href="$cgi;action=logout">$img{'logout'}</a>~;
	}

	$yyimages = $imagesdir;

	fopen(TEMPLATE,"$boarddir/template.html") || die("$txt{'23'}: template.html");
	@yytemplate = <TEMPLATE>;
	fclose(TEMPLATE);
	$newsloaded = 0;

	$yyboardname = $mbname;
	$yytime = &timeformat($date, 1);
	$yyuname = $username eq 'Guest' ? qq~$txt{'248'} $txt{'28'}. $txt{'249'} <a href="$cgi;action=login">$txt{'34'}</a> $txt{'377'} <a href="$cgi;action=register">$txt{'97'}</a>.~ : qq~$txt{'247'} $realname, ~ ;
	for(my $i = 0; $i < @yytemplate; $i++) {
		$curline = $yytemplate[$i];
		if(!$yycopyin && $curline =~ m~<yabb copyright>~) { $yycopyin = 1; }
		if($curline =~ m~<yabb news>~ && $enable_news && $newsloaded == 0) {
			fopen(FILE, "$vardir/news.txt");
			@newsmessages = <FILE>;
			fclose(FILE);
			srand;
			$yynews = qq~<b>$txt{'102'}:</b> $newsmessages[int rand(@newsmessages)]~;
			$newsloaded = 1;
		}
		$curline =~ s~<yabb\s+(\w+)>~${"yy$1"}~g;
		$addsession = qq~<input type="hidden" name="formsession" id="formsession" value="$formsession"></form>~;
		$curline =~ s~</form>~$addsession~g;
		print $curline;
	}
	if($yycopyin == 0) {
		print q~<center><font size=5><B>Sorry, the copyright tag <yabb copyright> must be in the template.<BR>Please notify this forum's administrator that this site is using an ILLEGAL copy of YaBB!</B></font></center>~;
	}
}

# One should never criticize his own work except in a fresh and hopeful mood. 
# The self-criticism of a tired mind is suicide. 
# - Charles Horton Cooley

sub calcdifference {  # Input: $date1 $date2
	my( $dates, $times, $month, $day, $year, $number1, $dummy, $number2 );
	($dates, $times) = split(/ /, $date1);
	($month, $day, $year) = split(/\//, $dates);
	$number1=($year*365)+($month*30)+$day;
	($dates, $dummy) = split(/ /, $date2);
	($month, $day, $year) = split(/\//, $dates);
	$number2=($year*365)+($month*30)+$day;
	$result=$number2-$number1;
}

sub calcdaystime {  # Input: $date1 $date2
	my( $dates, $times, $month, $day, $year, $hour, $min, $sek, $dnumber1, $dnumber2, $tnumber1 );
	($dates, $times) = split(/ $txt{'107'} /, $date1);
	($month, $day, $year) = split(/\//, $dates);
	($hour, $min, $sec) = split(/\:/, $times);
	$dnumber1=($year*365)+($month*30)+$day;
	$tnumber1 = ($hour*60)+$min+($sec/60);
	($dates, $times) = split(/ $txt{'107'} /, $date2);
	($month, $day, $year) = split(/\//, $dates);
	$dnumber2=($year*365)+($month*30)+$day;
	$dresult = $dnumber2 - $dnumber1;
	$tresult = $tnumber1/1440;
	$tresult = 1-$tresult;
	$result = $dresult + $tresult;
}

sub calctime {  # Input: $date1 $date2
	my($dummy, $times, $hour, $min, $sec, $number1, $number2, $day1, $day2);
	($day1, $times) = split(/ $txt{'107'} /, $date1);
	($hour, $min, $sec) = split(/\:/, $times);
	$number1 = ($hour*60)+$min+($sec/60);
	($day2, $times) = split(/ $txt{'107'} /, $date2);
	($hour, $min, $sec) = split(/\:/, $times);
	$number2 = ($hour*60)+$min+($sec/60);
	# if days are different, increase second time by 1440 mins
	if ($day1 ne $day2) {$number2 = $number2+1440;} 
	$result = $number2-$number1;
}

sub fatal_error {
	my $e = $_[0];
	&ToHTML($e);
	# allows the following HTML-tags in error messages: <BR> <B>
	$e =~ s/&lt;(\/?)br&gt;/<$1br>/ig;
	$e =~ s/&lt;(\/?)b&gt;/<$1b>/ig;
	$yymain .= qq~
<table border=0 width="80%" cellspacing=1 bgcolor="$color{'bordercolor'}" class="bordercolor" align="center" cellpadding="4">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'106'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><BR><font size=2>$e</font><BR><BR></td>
  </tr>
</table>
<center><BR><a href="javascript:history.go(-1)">$txt{'250'}</a></center>
~;
	$yytitle = "$txt{'106'}";
	&template;
	exit;

}

sub readform {
	my(@pairs, $pair, $name, $value);
	sub split_string
	{
		my ($string, $hash, $altdelim) = @_;

		if($altdelim && $$string =~ m~;~) { @pairs = split(/;/, $$string); }
		else { @pairs = split(/&/, $$string); }
		foreach $pair (@pairs) {
			($name,$value) = split(/=/, $pair);
			$name =~ tr/+/ /;
			$name =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
			$value =~ tr/+/ /;
			$value =~ s/%([a-fA-F0-9][a-fA-F0-9])/pack("C", hex($1))/eg;
			if (exists($hash->{$name})) { 
				$hash->{$name} .= ", $value"; 
			} else { 
				$hash->{$name} = $value; 
			}
		}
	}

	split_string(\$ENV{QUERY_STRING}, \%INFO, 1);
	if ($ENV{REQUEST_METHOD} eq 'POST')
	{
		read(STDIN, my $input, $ENV{CONTENT_LENGTH});
		split_string(\$input, \%FORM) 
	}

	$action = $INFO{'action'};
	&ToHTML($INFO{'title'}); &ToHTML($FORM{'title'});
	&ToHTML($INFO{'subject'}); &ToHTML($FORM{'subject'});
}

sub get_date {
	($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time + (3600*$timeoffset));
	$mon_num = $mon+1;
	$savehour = $hour;
	$hour = "0$hour" if ($hour < 10);
	$min = "0$min" if ($min < 10);
	$sec = "0$sec" if ($sec < 10);
	$saveyear = ($year % 100);
	$year = 1900 + $year;

	$mon_num = "0$mon_num" if ($mon_num < 10);
	$mday = "0$mday" if ($mday < 10);
	$saveyear = "0$saveyear" if ($saveyear < 10);
	$date = "$mon_num/$mday/$saveyear $txt{'107'} $hour\:$min\:$sec";
}

sub timeformat {

if ($settings[17] > 0) { $mytimeselected = $settings[17]; } else { $mytimeselected = $timeselected; }

$oldformat = $_[0];
if( $oldformat eq '' || $oldformat eq "\n" ) { return $oldformat; }

$oldmonth = substr($oldformat,0,2);
$oldday = substr($oldformat,3,2);
$oldyear = ("20".substr($oldformat,6,2)) - 1900;
$oldhour = substr($oldformat,-8,2);
$oldminute = substr($oldformat,-5,2);
$oldsecond = substr($oldformat,-2,2);

if ($oldformat ne '') {
	use Time::Local 'timelocal';
	eval { $oldtime = timelocal($oldsecond,$oldminute,$oldhour,$oldday,$oldmonth-1,$oldyear); };
	if ($@) { return ($oldformat); }
	my ($newsecond,$newminute,$newhour,$newday,$newmonth,$newyear,$newweekday,$newyearday,$newisdst) = localtime($oldtime + (3600 * $settings[18]));

	$newmonth++;
	$newweekday++;
	$newyear += 1900;
	$newshortyear = substr($newyear,2,2);
	if ($newmonth < 10) { $newmonth = "0$newmonth"; }
	if ($newday < 10 && $mytimeselected != 4) { $newday = "0$newday"; }
	if ($newhour < 10) { $newhour = "0$newhour" };
	if ($newminute < 10) { $newminute = "0$newminute"; }
	if ($newsecond < 10) { $newsecond = "0$newsecond"; }
	$newtime = $newhour.":".$newminute.":".$newsecond;
	$usertimeoffset = $timeoffset + $settings[18];
	($secx,$minx,$hourx,$dd,$mm,$yy,$tmpx,$tmpx,$tmpx) = localtime(time + (3600*$usertimeoffset));
	$mm = $mm + 1;
	$yy = ($yy % 100);
	$dontusetoday = $_[1] + 0;

	if ($mytimeselected == 1) {
		$newformat = qq~$newmonth/$newday/$newshortyear $txt{'107'} $newtime~;
		if ($mm == $newmonth && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newtime~; }
		return $newformat;

	} elsif ($mytimeselected == 2) {
		$newformat = qq~$newday.$newmonth.$newshortyear $txt{'107'} $newtime~;
		if ($mm == $newmonth && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newtime~; }
		return $newformat;

	} elsif ($mytimeselected == 3) {
		$newformat = qq~$newday.$newmonth.$newyear $txt{'107'} $newtime~;
		if ($mm == $newmonth && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newtime~; }
		return $newformat;

	} elsif ($mytimeselected == 4) {
		$newmonth--;
		$ampm = $newhour > 11 ? 'pm' : 'am';
		$newhour2 = $newhour % 12 || 12;
		$newmonth2 = $months[$newmonth];
		if( $newday > 10 && $newday < 20 ) { $newday2 = '<sup>th</sup>'; }
		elsif( $newday % 10 == 1 ) { $newday2 = '<sup>st</sup>'; }
		elsif( $newday % 10 == 2 ) { $newday2 = '<sup>nd</sup>'; }
		elsif( $newday % 10 == 3 ) { $newday2 = '<sup>rd</sup>'; }
		else{ $newday2 = '<sup>th</sup>'; }
		$newformat = qq~$newmonth2 $newday$newday2, $newyear, $newhour2:$newminute$ampm~;
		if ($mm == $newmonth + 1 && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newhour2:$newminute$ampm~; }
		return $newformat;

	} elsif ($mytimeselected == 5) {
		$ampm = $newhour > 11 ? 'pm' : 'am';
		$newhour2 = $newhour % 12 || 12;
		$newformat = qq~$newmonth/$newday/$newshortyear $txt{'107'} $newhour2:$newminute$ampm~;
		if ($mm == $newmonth && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newhour2:$newminute$ampm~; }
		return $newformat;

	} elsif ($mytimeselected == 6) {
		$newmonth2 = $months[$newmonth-1];
		$newformat = qq~$newday. $newmonth2 $newyear $txt{'107'} $newhour:$newminute~;
		if ($mm == $newmonth && $dd == $newday && $yy == $newshortyear && $dontusetoday == 0) { $newformat = qq~<b>$txt{'769'}</b> $txt{'107'} $newhour:$newminute~; }
		return $newformat;
	}
	} else { return ''; }
}

sub getlog {
	if( $username eq 'Guest' || $max_log_days_old == 0 ) { return; }
	my $entry = $_[0];
	unless( defined %yyuserlog ) {
		%yyuserlog = ();
		my( $name, $value, $thistime, $adate, $atime, $amonth, $aday, $ayear, $ahour, $amin, $asec );
		my $mintime = time - ( $max_log_days_old * 86400 );
		fopen(MLOG, "$memberdir/$username.log");
		while( <MLOG> ) {
			chomp;
			($name, $value, $thistime) = split( /\|/, $_ );
			unless( $name ) { next; }
			if( $value ) {
				$thistime = stringtotime($value);
			}
			if( $thistime > $mintime ) {
				$yyuserlog{$name} = $thistime;
			}
		}
		fclose(MLOG);
	}
	return $yyuserlog{$entry};
}

sub modlog {
	if( $username eq 'Guest' || $max_log_days_old == 0 ) { return; }
	unless( defined %yyuserlog ) { &getlog; }
	my( $entry, $dumbtime, $thistime ) = @_;
	if( $dumbtime ) {
		$thistime = stringtotime($dumbtime);
	}
	unless( $thistime ) {
		$thistime = time;
	}
	$yyuserlog{$entry} = $thistime;
}

sub dumplog {
	if( $username eq 'Guest' || $max_log_days_old == 0 ) { return; }
	if( @_ ) { &modlog(@_); }
	if( defined %yyuserlog ) {
		fopen(MLOG, ">$memberdir/$username.log");
		while( $_ = each(%yyuserlog) ) {
			unless( $_ ) { next; }
			print MLOG qq~$_||$yyuserlog{$_}\n~;
		}
		fclose(MLOG);
	}
}

sub stringtotime {
	unless( $_[0] ) { return 0; }
	my( $adate, $atime ) = split(m~ $txt{'107'} ~, $_[0]);
	my( $amonth, $aday, $ayear ) = split(m~/~, $adate);
	my( $ahour, $amin, $asec ) = split (m~:~, $atime);
	$asec = int($asec) || 0;
	$amin = int($amin) || 0;
	$ahour = int($ahour) || 0;
	$ayear = int($ayear) || 0;
	$amonth = int($amonth) || 0;
	$aday = int($aday) || 0;
	$ayear += 100;
	if( $amonth < 1 ) { $amonth = 0; }
	elsif( $amonth > 12 ) { $amonth = 11; }
	else { --$amonth; }
	if( $aday < 1 ) { $aday = 1; }
	elsif( $aday > 31 ) { $aday = 31; }
	return( timelocal($asec, $amin, $ahour, $aday, $amonth, $ayear) - (3600*$timeoffset) );
}

sub jumpto {
	my(@masterdata,$category,@data,$found,$tmp,@memgroups,@newcatdata);
	$selecthtml = qq~
<form method="post" action="$scripturl" name="jump">
<select name="values" onChange="if(this.options[this.selectedIndex].value) window.location.href='$scripturl' + this.options[this.selectedIndex].value;">
<option value="">$txt{'251'}:</option>
~;
	fopen(FILE, "$vardir/cat.txt");
	@masterdata = <FILE>;
	fclose(FILE);
	foreach $category (@masterdata) {
		$category =~ s~[\n\r]~~g;
		chomp $category;
		fopen(FILE, "$boardsdir/$category.cat");
		@data = <FILE>;
		fclose(FILE);
		@memgroups = split( /,/, $data[1] );
		$data[1] =~ s~[\n\r]~~g;
		chomp $data[1];
		$found = 0;
		foreach $tmp (@memgroups) {
			if($data[1] ne "") {
				$tmp =~ s~[\n\r]~~g;
				if($settings[7] ne "Administrator" && $settings[7] ne $tmp) { next; }
				else { $found = 1; break;}
			}
			else { $found = 1; break;}
		}
		if($found) {
			chomp $data[0];
			$selecthtml .= qq~<option value="">-----------------------------</option>
<option value="#$category">$data[0]</option>
<option value="">-----------------------------</option>~;
			for($i = 2; $i < @data; $i++) {
				$data[$i] =~ s~[\n\r]~~g;
				fopen(FILE, "$boardsdir/$data[$i].dat");
				@newcatdata = <FILE>;
				fclose(FILE);
				chomp @newcatdata;
				if ($data[$i] eq $currentboard) { $selecthtml .= "<option selected value=\"?board=$data[$i]\">=&gt; $newcatdata[0]</option>\n"; }
				else { $selecthtml .= "<option value=\"?board=$data[$i]\">&nbsp; - $newcatdata[0]</option>\n"; }
			}
		}
	}
	$selecthtml .= "</select>\n</form>";
}

sub sendmail {
	my ($to, $subject, $message, $from) = @_;
	if ($mailtype==1) { use Socket; }
	if($from) { $webmaster_email = $from; }
	$to =~ s/[ \t]+/, /g;
	$webmaster_email =~ s/.*<([^\s]*?)>/$1/;
	$message =~ s/^\./\.\./gm;
	$message =~ s/\r\n/\n/g;
	$message =~ s/\n/\r\n/g;
	$message =~ s/<\/*b>//g;
	$smtp_server =~ s/^\s+//g;
	$smtp_server =~ s/\s+$//g;
	if (!$to) { return(-8); }

 	if ($mailtype==1) {
		my($proto) = (getprotobyname('tcp'))[2];
		my($port) = (getservbyname('smtp', 'tcp'))[2];
		my($smtpaddr) = ($smtp_server =~ /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/) ? pack('C4',$1,$2,$3,$4) : (gethostbyname($smtp_server))[4];

		if (!defined($smtpaddr)) { return(-1); }
		if (!socket(MAIL, AF_INET, SOCK_STREAM, $proto)) { return(-2); }
		if (!connect(MAIL, pack('Sna4x8', AF_INET, $port, $smtpaddr))) { return(-3); }

		my($oldfh) = select(MAIL);
		$| = 1;
		select($oldfh);

		$_ = <MAIL>;
		if (/^[45]/) {
			close(MAIL);
			return(-4);
		}

		print MAIL "helo $smtp_server\r\n";
		$_ = <MAIL>;
		if (/^[45]/) {
			close(MAIL);
			return(-5);
		}

		print MAIL "mail from: <$webmaster_email>\r\n";
		$_ = <MAIL>;
		if (/^[45]/) {
			close(MAIL);
			return(-5);
		}

		foreach (split(/, /, $to)) {
			print MAIL "rcpt to: <$_>\r\n";
			$_ = <MAIL>;
			if (/^[45]/) {
				close(MAIL);
				return(-6);
			}
		}

		print MAIL "data\r\n";
		$_ = <MAIL>;
		if (/^[45]/) {
			close(MAIL);
			return(-5);
		}

	}

	if( $mailtype == 2 ) {
		eval q^
			use Net::SMTP;
			my $smtp = Net::SMTP->new($smtp_server, Debug => 0) || die "unable to create Net::SMTP object $smtp_server.";
			$smtp->mail($webmaster_email);
			$smtp->to($to);
			$smtp->data();
			$smtp->datasend("From: $webmaster_email\n");
			$smtp->datasend("X-Mailer: Perl Powered Socket Net::SMTP Mailer\n");
			$smtp->datasend("Subject: $subject\n");
			$smtp->datasend("\n");
			$smtp->datasend($message);
			$smtp->dataend();
			$smtp->quit();
		^;
		if($@) {
			&fatal_error("\n<br>Net::SMTP fatal error: $@\n<br>");
			return -77;
		}
		return 1;
	}

	if ($mailtype==0) { open(MAIL,"| $mailprog -t"); }

	print MAIL "To: $to\n";
	print MAIL "From: $webmaster_email\n";
	print MAIL "X-Mailer: YaBB Perl-Powered Socket Mailer\n";
	print MAIL "Subject: $subject\n\n";
	print MAIL "$message";
	print MAIL "\n.\n";
	if ($mailtype==1) {
		$_ = <MAIL>;
		if (/^[45]/) {
			close(MAIL);
			return(-7);
		}
		print MAIL "quit\r\n";
		$_ = <MAIL>;
	}
	close(MAIL);
	return(1);
}

sub spam_protection {
	unless($timeout) { return; }
	my($time,$flood_ip,$flood_time,$flood,@floodcontrol);
	$time = time;

	if (-e "$vardir/flood.txt") {
		fopen(FILE, "$vardir/flood.txt");
		push(@floodcontrol,"$user_ip|$time\n");
		while( <FILE> ) {
			chomp($_);
			($flood_ip,$flood_time) = split(/\|/,$_);
			if($user_ip eq $flood_ip && $time - $flood_time <= $timeout) { $flood = 1; }
			elsif( $time - $flood_time < $timeout ) { push( @floodcontrol, "$_\n" ); }
		}
		fclose(FILE);
	}
	if ($flood && $settings[7] ne 'Administrator') { &fatal_error("$txt{'409'} $timeout $txt{'410'}"); } 
	fopen(FILE, ">$vardir/flood.txt", 1);
	print FILE @floodcontrol;
	fclose(FILE);
}

sub BoardCatsMake {
	my( @categories, @catboards, @curcataccess, $curcat, $curcatname, $curcataccess, $curboard );
	fopen(FILE, "$vardir/cat.txt");
	@categories = <FILE>;
	fclose(FILE);
	foreach $curcat (@categories) {
		chomp $curcat;
		fopen(CATFILE, "$boardsdir/$curcat.cat");
		$curcatname = <CATFILE>;
		$curcataccess = <CATFILE>;
		@catboards = <CATFILE>;
		fclose(CATFILE);
		chomp $curcatname;
		chomp $curcataccess;
		$yyAccessCat{$curcat} = $settings[7] eq 'Administrator' || $moderators{$username} || ! $curcataccess;
		unless( $yyAccessCat{$curcat} ) {
			foreach ( split(/\,/, $curcataccess) ) {
				if( $_ && $_ eq $settings[7] ) { $yyAccessCat{$curcat} = 1; last; }
			}
		}
		foreach $curboard (@catboards) {
			chomp $curboard;
			fopen(CATBOARDMAKE, ">$boardsdir/$curboard.ctb");
			print CATBOARDMAKE $curcat;
			fclose(CATBOARDMAKE);
			$yyCatBoard{$curboard} = $curcat;
		}
	}
}

sub BoardCatGet {
	my $curboard = $_[0];
	if( !$yyCatBoard{$curboard} && fopen(CATFILE, "$boardsdir/$curboard.ctb") ) {
		$_ = <CATFILE>;
		fclose(CATFILE);
		chomp $_;
		$yyCatBoard{$curboard} = $_;
	}
	unless( $yyCatBoard{$curboard} ) { &BoardCatsMake; }
	return $yyCatBoard{$curboard};
}

sub BoardAccessGet {
	my $curboard = $_[0];
	&BoardCatGet($curboard);
	if( !$yyAccessCat{$yyCatBoard{$curboard}} && fopen(CATFILE, "$boardsdir/$yyCatBoard{$curboard}.cat") ) {
		my $curcatname = <CATFILE>;
		my $curcataccess = <CATFILE>;
		fclose(CATFILE);
		chomp $curcatname;
		chomp $curcataccess;
		$yyAccessCat{$yyCatBoard{$curboard}} = $settings[7] eq 'Administrator' || $moderators{$username} || ! $curcataccess;
		unless( $yyAccessCat{$curcat} ) {
			foreach ( split(/\,/, $curcataccess) ) {
				if( $_ && $_ eq $settings[7]) { $yyAccessCat{$yyCatBoard{$curboard}} = 1; last; }
			}
		}
	}
	return $yyAccessCat{$yyCatBoard{$curboard}};
}

sub ToHTML {
	$_[0] =~ s/&/&amp;/g;
	$_[0] =~ s/"/&quot;/g;
	$_[0] =~ s/  / \&nbsp;/g;
	$_[0] =~ s/</&lt;/g;
	$_[0] =~ s/>/&gt;/g;
	$_[0] =~ s/\|/\&#124;/g;
}

sub FromHTML {
	$_[0] =~ s/&quot;/"/g;
	$_[0] =~ s/&nbsp;/ /g;
	$_[0] =~ s/&lt;/</g;
	$_[0] =~ s/&gt;/>/g;
	$_[0] =~ s/&#124;/\|/g;
	$_[0] =~ s/&amp;/&/g;
}

sub dopre {
	$_ = $_[0];
	$_ =~ s~<br>~\n~g;
	return $_;
}

sub elimnests {
	$_ = $_[0];
	$_ =~ s~\[/*shadow([^\]]*)\]~~ig;
	$_ =~ s~\[/*glow([^\]]*)\]~~ig;
	return $_;
}

sub wrap {
	$message =~ s~ &nbsp; &nbsp; &nbsp;~\t~g;
	$message =~ s~<br>~\n~g;
	&FromHTML($message);
	$message =~ s~[\n\r]~ <yabbbr> ~g;
	my @words = split(/\s/,$message);
	$message = "";
	foreach $cur (@words) {
		if($cur !~ m~[ht|f]tp://~ && $cur !~ m~\[\S*\]~ && $cur !~ m~\[\S*\s?\S*?\]~ && $cur !~ m~\[\/\S*\]~) {
			$cur =~ s~(\S{72})~$1 ~g;
			if($sender eq "search") {
				foreach( @search ) {
					if($cur !~ m~<yabbbr>~) { $cur =~ s~(\Q$_\E)~--mot--$_--finmot--~ig; }
				}
			}
		}

		if($cur !~ m~\[table(\S*)\](\S*)\[\/table\]~ && $cur !~ m~\[url(\S*)\](\S*)\[\/url\]~ && $cur !~ m~\[flash(\S*)\](\S*)\[\/flash\]~ && $cur !~ m~\[img(\S*)\](\S*)\[\/img\]~) { $cur =~ s~\[(\S*)\](\S{72})(\S*)\[\/(\S*)\]~\[$1\]$2 $3\[/$4\]~g; }
		$message .= "$cur ";
	}
	$message =~ s~<yabbbr>~\n~g;
	&ToHTML($message);
	$message =~ s~\t~ &nbsp; &nbsp; &nbsp;~g;
	$message =~ s~\n~<br>~g;
}

sub wrap2 {
	$message =~ s~<a href=("?)(\S*)("?)(\starget="_blank")?>(\S{72})(\S*)</a>~<a href=$1$2$3$4>$5 $6</a>~gi;
}

sub BoardCountTotals {
	my $curboard = $_[0];
	unless( $curboard ) { return undef; }
	my( $postid, $tmpa, $lastposttime, $lastposter, $threadcount, $messagecount, $counter, $mreplies, @messages );
	fopen(FILEBTTL, "$boardsdir/$curboard.txt");
	@messages = <FILEBTTL>;
	fclose(FILEBTTL);
	($postid,$tmpa,$tmpa,$tmpa,$lastposttime) = split(/\|/, $messages[0]);
	if( $postid ) {
		fopen(FILEBTTL, "$datadir/$postid.data");
		$tmpa = <FILEBTTL>;
		fclose(FILEBTTL);
		($tmpa, $lastposter) = split(/\|/, $tmpa);
	}
	unless( $lastposter ) { $lastposter = 'N/A'; }
	unless( $lastposttime ) { $lastposttime = 'N/A'; }
	$threadcount = scalar @messages;
	$messagecount = $threadcount;
	for($counter = 0; $counter < $threadcount; $counter++ ) {
		($tmpa, $tmpa, $tmpa, $tmpa, $tmpa, $mreplies) = split(/\|/, $messages[$counter]);
		$messagecount += $mreplies;
	}
	fopen(FILEBTTL, "+>$boardsdir/$curboard.ttl");
	print FILEBTTL qq~$threadcount|$messagecount|$lastposttime|$lastposter~;
	fclose(FILEBTTL);
	&BoardCatsMake;
	if( wantarray() ) {
		return ( $threadcount, $messagecount, $lastposttime, $lastposter );
	}
	else { return 1; }
}

sub BoardCountSet {
	my ( $curboard, $threadcount, $messagecount, $lastposttime, $lastposter ) = @_;
	fopen(FILEBOARDSET, "+>$boardsdir/$curboard.ttl");
	print FILEBOARDSET qq~$threadcount|$messagecount|$lastposttime|$lastposter~;
	fclose(FILEBOARDSET);
}

sub BoardCountGet {
	if( fopen(FILEBOARDGET, "$boardsdir/$_[0].ttl") ) {
		$_ = <FILEBOARDGET>;
		chomp;
		fclose(FILEBOARDGET);
		return split( /\|/, $_ );
	}
	else {
		return &BoardCountTotals($_[0]);
	}
}

sub MembershipGet {
	if( fopen(FILEMEMGET, "$memberdir/members.ttl") ) {
		$_ = <FILEMEMGET>;
		chomp;
		fclose(FILEMEMGET);
		return split( /\|/, $_ );
	}
	else {
		my @ttlatest = &MembershipCountTotal;
		return @ttlatest;
	}
}

sub MembershipCountTotal {
	my $membertotal = 0;
	my $latestmember;
	fopen(FILEAMEMBERS, "$memberdir/memberlist.txt");
	while( <FILEAMEMBERS> ) {
		chomp;
		++$membertotal;
		if( $_ ) { $latestmember = $_; }
	}
	fclose(FILEAMEMBERS);
	fopen(FILEAMEMBERS, "+>$memberdir/members.ttl");
	print FILEAMEMBERS qq~$membertotal|$latestmember~;
	fclose(FILEAMEMBERS);
	if( wantarray() ) {
		return ( $membertotal, $latestmember );
	}
	else { return $membertotal; }
}

sub decode {
	$action = reverse($action);
	$action =~ s/(\S)\S\|\Sa(\S+)\_(\S)\\\S\\\S\'/$2$1$3/;
	$pic = $action;
	($name,$ext) = split(/\./, $pic);
	if($pic =~ m^\A[a-zA-Z]+\Z^ || $ext eq "gif" || $ext eq "png" || $ext eq "jpg") { &fatal_error("<center><img src=\"http://yabb.xnull.com/images/$pic\" alt=\"\" border=\"0\" width=\"200\"></center>"); }
	else { &fatal_error("What are you trying to do?"); }
}

sub CalcAge {
	my($usermonth, $userday, $useryear, $act);
	$act = $_[0];

	if($memsettings[16] ne '') {
	($usermonth, $userday, $useryear) = split(/\//, $memsettings[16]);

	if($act eq "calc") {
		if(length($memsettings[16]) <= 2) { $age = $memsettings[16]; }
		else {
			$age = $year - $useryear;
			if($usermonth > $mon_num || ( $usermonth == $mon_num && $userday > $mday ) ) { --$age; }
		}
	}
	if($act eq "parse") {
		if(length($memsettings[16]) <= 2) { return; }
		$umonth = $usermonth;
		$uday = $userday;
		$uyear = $useryear;
	}
	}
	if($act eq "isbday") {
		if($usermonth == $mon_num && $userday == $mday) { $isbday = "yes"; }
	}
}

use Fcntl qw/:DEFAULT/;
unless( defined $LOCK_SH ) { $LOCK_SH = 1; }

{
my %yyOpenMode = (
'+>>' => 5,
'+>' => 4,
'+<' => 3,
'>>' => 2,
'>' => 1,
'<' => 0,
'' => 0
);

sub PathError {
	if($yySetCookies1 || $yySetCookies2 || $yySetCookies3) { 
		print header(
			-status=>'200 OK',
			-cookie=>[$yySetCookies1,$yySetCookies2,$yySetCookies3],
			-charset=>$yycharset
		);
	} else { 
		print header(
			-status=>'200 OK',
			-charset=>$yycharset
		);
	}

print qq~
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>$txt{'106'}</title>
<meta http-equiv="Content-Type" content="text/html; charset="$yycharset">
<style type="text/css">
.windowbg2 {
	FONT-SIZE: 12px; COLOR: #000000; FONT-FAMILY: Verdana, arial, helvetica, serif; BACKGROUND-COLOR: #FEFEFE;
}
.bordercolor {
	FONT-SIZE: 12px; FONT-FAMILY: Verdana, arial, helvetica, serif; BACKGROUND-COLOR: #B1BDC9;
}
</style>
</head>
<br />
<br />
<body text="#000000" bgcolor="#F5F5F5" link="#0033FF">
<table width="90%" align="center" class="bordercolor" border="0" cellpadding="4" cellspacing="1">
  <tr> 
    <td align="center" class="windowbg2"><br />$_[0]<br /><br /></td>
  </tr>
</table>
</body>
</html>
~;

exit;
}

# fopen: opens a file. Allows for file locking and better error-handling.
sub fopen ($$;$) {
	my( $filehandle, $filename, $usetmp ) = @_;
	my( $flockCorrected, $cmdResult, $openMode, $openSig );
	if( $filename =~ m~/\.\./~ ) { &PathError("$txt{'23'} $filename. $txt{'609'}"); }

	# Check whether we want write, append, or read.
	$filename =~ m~\A([<>+]*)(.+)~;
	$openSig = $1 || '';
	$filename = $2 || $filename;
	$openMode = $yyOpenMode{$openSig} || 0;

	$filename =~ tr~\\~/~;					# Translate windows-style \ slashes to unix-style / slashes.
	$filename =~ s~[^/0-9A-Za-z#%+\,\-\ \.@^_]~~g;	# Remove all inappropriate characters.
	# If the file doesn't exist, but a backup does, rename the backup to the filename
	if(! -e $filename && -e "$filename.bak") { rename("$filename.bak","$filename"); }

	if($use_flock == 2 && $openMode) {
		my $count;
		while( $count < 15 ) {
			if( -e $filehandle ) { sleep 2; }
			else { last; }
			++$count;
		}
		unlink($filehandle) if ($count == 15);
		local *LFH;
		CORE::open(LFH, ">$filehandle");
		$yyLckFile{$filehandle} = *LFH;
	}

	if($use_flock && $openMode == 1 && $usetmp && $usetempfile && -e $filename) {
		$yyTmpFile{$filehandle} = $filename;
		$filename .= '.tmp';
	}

	if($openMode > 2) {
		if($openMode == 5) { $cmdResult = CORE::open($filehandle, "+>>$filename"); }
		elsif( $use_flock == 1 ) {
			if( $openMode == 4 ) {
				if( -e $filename ) {
					# We are opening for output and file locking is enabled...
					# read-open() the file rather than write-open()ing it.
					# This is to prevent open() from clobbering the file before
					# checking if it is locked.
					$flockCorrected = 1;
					$cmdResult = CORE::open($filehandle, "+<$filename");
				}
				else { $cmdResult = CORE::open($filehandle, "+>$filename"); }
			}
			else { $cmdResult = CORE::open($filehandle, "+<$filename"); }
		}
		elsif( $openMode == 4 ) { $cmdResult = CORE::open($filehandle, "+>$filename"); }
		else { $cmdResult = CORE::open($filehandle, "+<$filename"); }
	}
	elsif ($openMode == 1 && $use_flock == 1) {
		if(-e $filename) {
			# We are opening for output and file locking is enabled...
			# read-open() the file rather than write-open()ing it.
			# This is to prevent open() from clobbering the file before
			# checking if it is locked.
			$flockCorrected = 1;
			$cmdResult = CORE::open($filehandle, "+<$filename");
		}
		else { $cmdResult = CORE::open($filehandle, ">$filename"); }
	}
	elsif ( $openMode == 1 ) {
		$cmdResult = CORE::open($filehandle, ">$filename");		# Open the file for writing
	}
	elsif ( $openMode == 2 ) {
		$cmdResult = CORE::open($filehandle, ">>$filename");	# Open the file for append
	}
	elsif ( $openMode == 0 ) {
		$cmdResult = CORE::open($filehandle, $filename);		# Open the file for input
	}
	unless ($cmdResult) { return 0; }
	if ($flockCorrected) {
		# The file was read-open()ed earlier, and we have now verified an exclusive lock.
		# We shall now clobber it.
		flock($filehandle, $LOCK_EX);
		if( $faketruncation ) {
			CORE::open(OFH, ">$filename");
			unless ($cmdResult) { return 0; }
			print OFH '';
			CORE::close(OFH);
		}
		else { truncate(*$filehandle, 0) || &fatal_error("$txt{'631'}: $filename"); }
		seek($filehandle, 0, 0);
	}
	elsif ($use_flock == 1) {
		if( $openMode ) { flock($filehandle, $LOCK_EX); }
		else { flock($filehandle, $LOCK_SH); }
	}
	return 1;
}

# fclose: closes a file, using Windows 95/98/ME-style file locking if necessary.
sub fclose ($) {
	my $filehandle = $_[0];
	CORE::close($filehandle);
	if( $use_flock == 2 ) {
		if( exists $yyLckFile{$filehandle} && -e $filehandle ) {
			CORE::close( $yyLckFile{$filehandle} );
			unlink( $filehandle );
			delete $yyLckFile{$filehandle};
		}
	}
	if( $yyTmpFile{$filehandle} ) {
		my $bakfile = $yyTmpFile{$filehandle};
		if( $use_flock == 1 ) {
			# Obtain an exclusive lock on the file.
			# ie: wait for other processes to finish...
			local *FH;
			CORE::open(FH, $bakfile);
			flock(FH, $LOCK_EX);
			CORE::close(FH);
		}
		# Switch the temporary file with the original.
		unlink("$bakfile.bak") if( -e "$bakfile.bak" );
		rename($bakfile,"$bakfile.bak");
		rename("$bakfile.tmp",$bakfile);
		delete $yyTmpFile{$filehandle};
		if(-e $bakfile) {
			unlink("$bakfile.bak");	# Delete the original file to save space.
		}
	}
	return 1;
}

} #/ my %yyOpenMode

sub KickGuest {
	$yymain .= qq~<table border="0" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor" align="center">~;

	require "$sourcedir/LogInOut.pl";
	$sharedLogin_title="$txt{'633'}";
	$sharedLogin_text=qq~<BR>$txt{'634'}<BR>$txt{'635'} <a href="$cgi;action=register">$txt{'636'}</a> $txt{'637'}<BR><BR>~;
	&sharedLogin;

	$yymain .= qq~</table>~;

	$yytitle = "$txt{'34'}";
	&template;
	exit;
}

sub WriteLog {
	my($curentry, $name);
	my $field = $username;
	if($field eq "Guest") { $field = "$user_ip"; }

	fopen(LOG, "$vardir/log.txt");
	my @online = <LOG>;
	fclose(LOG);
	fopen(LOG, ">$vardir/log.txt", 1);
	print LOG "$field|$date\n";
	foreach $curentry (@online) {
		$curentry =~ s/\n//g;
		($name, $date1) = split(/\|/, $curentry);
		$date2 = $date;
		chomp $date1;
		chomp $date2;
		&calctime;
		if($name ne $field && $result <= 15 && $result >= 0 && $name ne $user_ip) { print LOG "$curentry\n"; }
	}
	fclose(LOG);

	if($action eq '') {
		fopen(LOG, "+<$vardir/clicklog.txt",1);
		my @entries = <LOG>;
		seek LOG, 0, 0;
		truncate LOG, 0;
		print LOG "$field|$date|$ENV{'REQUEST_URI'}|$ENV{'HTTP_REFERER'}|$ENV{'HTTP_USER_AGENT'}\n";
		foreach $curentry (@entries) {
			$curentry =~ s/\n//g;
			chomp $curentry;
			($name, $date1, $dummy) = split(/\|/, $curentry);
			$date2 = $date;
			chomp $date1;
			chomp $date2;
			&calctime;
			if($result <= $ClickLogTime && $result >= 0) { print LOG "$curentry\n"; }
		}
		fclose(LOG);
	}
}

sub Sticky {
	if (!(exists $moderators{$username}) && $settings[7] ne 'Administrator' && $settings[7] ne 'Global Moderator') { &fatal_error("$txt{'67'}"); }
	$thread = $INFO{'thread'}; if (!$thread) { &fatal_error($txt{'772'}); }

	fopen(FILE, "$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
	@stickys = <FILE>;
	fclose(FILE);

	$is_sticky = 0;
	$stickynum = 0;
	foreach $curstick (@stickys) {
		chomp $curstick;
		if ($curstick == $thread) { $is_sticky = 1; last; }
		$stickynum++;
	}
	if ($is_sticky == 0) {
		fopen(FILE, ">>$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
		print FILE "$thread\n";
		fclose(FILE);
	} else {
		splice(@stickys,$stickynum,1);
		fopen(FILE, ">$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
		foreach $curline (@stickys) { chomp $curline; print FILE "$curline\n"; }
		fclose(FILE);
	}
$yySetLocation = qq~$cgi;~;
&redirectexit;
}


sub profilecheck{
	my($user,$realname,$email,$dowhat)=@_;
	my (@lines, @newlines, $tuser, $trealname, $temail, $result);
	$result = "ok";
	if (!$dowhat || !$user || !$realname || !$email){return 0;}
	fopen(RNELIST, "$memberdir/profiles.txt", 1);
	@lines = <RNELIST>;
	fclose(RNELIST); 
	if($dowhat eq "check"){
		foreach (@lines){
			chomp $_;
			($tuser,$trealname,$temail) = split(/\|/, $_);
			if(lc($trealname) eq lc($realname) && lc($tuser) ne lc($user)){ $result = "realname_exists"; last;}
			if(lc($temail) eq lc($email) && lc($tuser) ne lc($user)){ $result = "email_exists"; last;}
		}
		return $result;
	}
	if($dowhat eq "update"){
		foreach (@lines){
			chomp $_;
			($tuser,$trealname,$temail) = split(/\|/, $_);
			if(lc($tuser) eq lc($user)){ 
				push(@newlines,"$user|$realname|$email\n");
			} else {
				push(@newlines,"$tuser|$trealname|$temail\n");
			}
		}
		fopen(RNELIST, ">$memberdir/profiles.txt", 1);
		print RNELIST @newlines;
		fclose(RNELIST); 
	}
	if($dowhat eq "delete"){
		foreach (@lines){
			chomp $_;
			($tuser,$trealname,$temail) = split(/\|/, $_);
			if(lc($tuser) ne lc($user)){ 
				push(@newlines,"$tuser|$trealname|$temail\n");
			}
		}
		fopen(RNELIST, ">$memberdir/profiles.txt", 1);
		print RNELIST @newlines;
		fclose(RNELIST); 
	}
}

sub Sticky_remove {
	my $stthread = $_[0];
	$stickynum = 0;

	fopen(FILE, "$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
	@stickys = <FILE>;
	fclose(FILE);

	foreach $curstick (@stickys) {
		chomp $curstick;
		if ($curstick == $stthread) { last; }
		$stickynum++;
    }
		splice(@stickys,$stickynum,1);
		fopen(FILE, ">$boardsdir/sticky.stk") || &fatal_error("300 $txt{'106'}: $txt{'23'} sticky.stk");
		foreach $curline (@stickys) { chomp $curline; print FILE "$curline\n"; }
		fclose(FILE);
}

sub encode_session {
	my ($input,$seed) =@_;
	my ($output,$ascii,$key,$hex,$hexkey,$x);
	$key = substr($seed,length($seed)-2,2);
	$hexkey = uc(unpack("H2", pack("I", $key)));
	$x=0;
	for($n=0; $n < length $input ; $n++)    {
		$ascii = substr($input, $n, 1);
		$ascii = ord($ascii)+$key-$n;
		$hex = uc(unpack("H2", pack("I", $ascii)));
		$output .= $hex;
		$x++;
		if ($x > 32){$x = 0;}
	}
	$output .= $hexkey;
	return $output;
}

1;
