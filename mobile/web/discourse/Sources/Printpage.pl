###############################################################################
# Printpage.pl                                                                #
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

$printplver = "1 Gold - SP 1.4";


sub Print { 
	$num = $INFO{'num'};
	### Determine what category we are in. ###
	fopen(FILE, "$boardsdir/$currentboard.ctb") || &fatal_error("300 $txt{'106'}: $txt{'23'} $currentboard.ctb");
	$cat = <FILE>;
	fclose(FILE);
	$curcat = $cat;
	fopen(FILE, "$boardsdir/$cat.cat") || &fatal_error("300 $txt{'106'}: $txt{'23'} $cat.cat");
	$cat = <FILE>;
	fclose(FILE);

	&LoadCensorList;	# Load Censor List

	### Lets open up the thread file itself. ###
	fopen(THREADS, "$datadir/$num.txt") || &donoopen;
	@threads = <THREADS>;
	fclose(THREADS);
	$cat =~ s/\n//g;

	($messagetitle, $poster, $trash, $date, $trash, $trash, $trash, $trash, $trash) = split (/\|/,$threads[0]);
	$startedby = $poster;
	$startedon = timeformat($date);

	### Lets output all that info. ###
	print "Content-type: text/html\n\n";
	print qq~<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>$mbname - $txt{'668'}</title>
<meta http-equiv=Content-Type content="text/html; charset=$yycharset">
<script language=JavaScript1.2 type="text/javascript">
<!--
if ((navigator.appVersion.substring(0,1) == "5" && navigator.userAgent.indexOf('Gecko') != -1) || navigator.userAgent.search(/Opera/) != -1) {
   document.write('<META HTTP-EQUIV="pragma" CONTENT="no-cache">');
}
// -->
</script>
</head>

<body bgcolor="#FFFFFF" text="#000000">
<table width="90%" align="center">
  <tr>
    <td><pre><font size="3" face="Arial,Helvetica">
    <table width="100%">
      <tr>
        <td><font size="3" face="Arial,Helvetica"><b>$mbname</b></font>
        <font size="2" face="Arial,Helvetica">($scripturl)</font></td>
      </tr><tr>
        <td><font size="2" face="Arial,Helvetica">$cat &gt;&gt; $boardname &gt;&gt; $messagetitle</font>
        <br><font size="1" face="Arial,Helvetica">($txt{'195'}: $startedby $txt{'176'} $startedon)</font></td>
      </tr>
    </table>
    </font></pre>
    </td>
  </tr>~;

	### Split the threads up so we can print them.
	foreach $thread (@threads) { # start foreach
		($threadtitle, $threadposter, $trash, $threaddate, $trash, $trash, $trash, $trash, $threadpost) = split (/\|/,$thread);
		### Do/Undo YaBBC Stuff ###
		$threadpost =~ s~<br>~\n~ig;
		$threadpost =~ s~\[code\]\n*(.*?)\n*\[/code\]~&codemsg($1)~eisg;

		$threadpost =~ s~\[([^\]]{0,30})\n([^\]]{0,30})\]~\[$1$2\]~g;
		$threadpost =~ s~\[/([^\]]{0,30})\n([^\]]{0,30})\]~\[/$1$2\]~g;
		$threadpost =~ s~(\w+://[^<>\s\n\"\]\[]+)\n([^<>\s\n\"\]\[]+)~$1\n$2~g;
		$threadpost =~ s~\[b\](.+?)\[/b\]~<b>$1</b>~isg;
		$threadpost =~ s~\[i\](.+?)\[/i\]~<i>$1</i>~isg;
		$threadpost =~ s~\[u\](.+?)\[/u\]~<u>$1</u>~isg;
		$threadpost =~ s~\[s\](.+?)\[/s\]~<s>$1</s>~isg;
		$threadpost =~ s~\[move\](.+?)\[/move\]~$1~isg;

		$threadpost =~ s~\[glow(.*?)\](.*?)\[/glow\]~&elimnests($2)~eisg;
		$threadpost =~ s~\[shadow(.*?)\](.*?)\[/shadow\]~&elimnests($2)~eisg;

		$threadpost =~ s~\[shadow=(\S+?),(.+?),(.+?)\](.+?)\[/shadow\]~$4~eisg;
		$threadpost =~ s~\[glow=(\S+?),(.+?),(.+?)\](.+?)\[/glow\]~$4~eisg;

		$threadpost =~ s~\[color=([\w#]+)\](.*?)\[/color\]~$2~isg;
		$threadpost =~ s~\[black\](.*?)\[/black\]~$1~isg;
		$threadpost =~ s~\[white\](.*?)\[/white\]~$1~isg;
		$threadpost =~ s~\[red\](.*?)\[/red\]~$1~isg;
		$threadpost =~ s~\[green\](.*?)\[/green\]~$1~isg;
		$threadpost =~ s~\[blue\](.*?)\[/blue\]~$1~isg;

		$threadpost =~ s~\[font=(.+?)\](.+?)\[/font\]~<font face="$1">$2</font>~isg;
		$threadpost =~ s~\[size=(.+?)\](.+?)\[/size\]~<font size="$1">$2</font>~isg;

		$threadpost =~ s~\[quote\s+author=(.*?)\s+link=(.*?)\].*\/me\s+(.*?)\[\/quote\]~\[quote author=$1 link=$2\]<i>* $1 $3</i>\[/quote\]~isg;
		$threadpost =~  s~\[quote(.*?)\].*\/me\s+(.*?)\[\/quote\]~\[quote$1\]<i>* Me $2</i>\[/quote\]~isg;
		$threadpost =~ s~\/me\s+(.*)~<font color="#FF0000">* $displayname $1</font>~ig;

		$char_160 = chr(160);
		$threadpost =~ s~\[img\][\s*\t*\n*(&nbsp;)*($char_160)*]*(http\:\/\/)*(.+?)[\s*\t*\n*(&nbsp;)*($char_160)*]*\[/img\]~http://$2~isg;
		$threadpost =~ s~\[img width=(\d+) height=(\d+)\][\s*\t*\n*(&nbsp;)*($char_160)*]*(http\:\/\/)*(.+?)[\s*\t*\n*(&nbsp;)*($char_160)*]*\[/img\]~http://$4~isg;

		$threadpost =~ s~\[tt\](.*?)\[/tt\]~<tt>$1</tt>~isg;
		$threadpost =~ s~\[left\](.+?)\[/left\]~<p align=left>$1</p>~isg;
		$threadpost =~ s~\[center\](.+?)\[/center\]~<center>$1</center>~isg;
		$threadpost =~ s~\[right\](.+?)\[/right\]~<p align=right>$1</p>~isg;
		$threadpost =~ s~\[sub\](.+?)\[/sub\]~<sub>$1</sub>~isg;
		$threadpost =~ s~\[sup\](.+?)\[/sup\]~<sup>$1</sup>~isg;
		$threadpost =~ s~\[fixed\](.+?)\[/fixed\]~<font face="Courier New">$1</font>~isg;

		$threadpost =~ s~\[\[~\{\{~g;
		$threadpost =~ s~\]\]~\}\}~g;
		$threadpost =~ s~\|~\&#124;~g;
		$threadpost =~ s~\[hr\]\n~<hr width=40% align=left size=1>~g;
		$threadpost =~ s~\[hr\]~<hr width=40% align=left size=1>~g;
		$threadpost =~ s~\[br\]~\n~ig;

		$threadpost =~ s~\[url\]www\.\s*(.+?)\s*\[/url\]~www.$1~isg;
		$threadpost =~ s~\[url=\s*(\w+\://.+?)\](.+?)\s*\[/url\]~$2 ($1)~isg;
		$threadpost =~ s~\[url=\s*(.+?)\]\s*(.+?)\s*\[/url\]~$2 (http://$1)~isg;
		$threadpost =~ s~\[url\]\s*(.+?)\s*\[/url\]~$1~isg;

		$threadpost =~ s~\[email\]\s*(\S+?\@\S+?)\s*\[/email\]~$1~isg;
		$threadpost =~ s~\[email=\s*(\S+?\@\S+?)\]\s*(.*?)\s*\[/email\]~$2 ($1)~isg;

		$threadpost =~ s~\[news\](.+?)\[/news\]~$1~isg;
		$threadpost =~ s~\[gopher\](.+?)\[/gopher\]~$1~isg;
		$threadpost =~ s~\[ftp\](.+?)\[/ftp\]~$1~isg;

		$threadpost =~ s~\[quote\s+author=(.*?)link=(.*?)\s+date=(.*?)\s*\]\n*(.*?)\n*\[/quote\]~<br><i>on $3, <a href=$scripturl?action=display;$2>$1 wrote</a>:</i><table bgcolor="#000000" cellspacing="1" width="90%"><tr><td width="100%"><table cellpadding="2" cellspacing="0" width="100%" bgcolor="#FFFFFF"><tr><td width="100%"><font size="1" color="#000000">$4</font></td></tr></table></td></tr></table>~isg;
		$threadpost =~ s~\[quote\]\n*(.+?)\n*\[/quote\]~<br><i>Quote:</i><table bgcolor="#000000" cellspacing="1" width="90%"><tr><td width="100%"><table cellpadding="2" cellspacing="0" width="100%" bgcolor="#FFFFFF"><tr><td width="100%"><font face="Arial,Helvetica" size="1" color="#000000">$1</font></td></tr></table></td></tr></table>~isg;

		$threadpost =~ s~\[list\]~<ul>~isg;
		$threadpost =~ s~\[\*\]~<li>~isg;
		$threadpost =~ s~\[/list\]~</ul>~isg;

		$threadpost =~ s~\[pre\](.+?)\[/pre\]~'<pre>' . dopre($1) . '</pre>'~iseg;

		$threadpost =~ s~\[flash=(\S+?),(\S+?)\](\S+?)\[/flash\]~$3~isg;

		$threadpost =~ s~\{\{~\[~g;
		$threadpost =~ s~\}\}~\]~g;

		if( $threadpost =~ m~\[table\]~i ) {
			$threadpost =~ s~\n{0,1}\[table\]\n*(.+?)\n*\[/table\]\n{0,1}~<table>$1</table>~isg;
			while( $threadpost =~ s~\<table\>(.*?)\n*\[tr\]\n*(.*?)\n*\[/tr\]\n*(.*?)\</table\>~<table>$1<tr>$2</tr>$3</table>~is ) {}
			while( $threadpost =~ s~\<tr\>(.*?)\n*\[td\]\n{0,1}(.*?)\n{0,1}\[/td\]\n*(.*?)\</tr\>~<tr>$1<td>$2</td>$3</tr>~is ) {}
		}

		$threadpost =~ s~\[\&table(.*?)\]~<table$1>~g;
		$threadpost =~ s~\[/\&table\]~</table>~g;
		$threadpost =~ s~\n~<br>~g;

		### Censor it ###
		foreach (@censored) {
			($tmpa,$tmpb) = @{$_};
			$threadtitle =~ s~\Q$tmpa\E~$tmpb~gi;
			$threadpost =~ s~\Q$tmpa\E~$tmpb~gi;
		}

		$threaddate = timeformat($threaddate);

		print qq~<tr>
    <td><font size="2" face="Arial,Helvetica">
    <hr size="2" width="100%">
    $txt{'196'}: <b>$threadtitle</b><BR>
    $txt{'197'} <b>$threadposter</b> $txt{'176'} <b>$threaddate</b>
    <hr width="100%" size="1">
    $threadpost</font></td>
  </tr>~;
	}

	print qq~<tr>
    <td align="center"><font size="1" face="Arial,Helvetica">
    <hr width="100%" size="1"><BR><BR>
    $yycopyright</font></td>
  </tr>
</table>
</body>
</html>~;
	exit;
}

{
	my %killhash = (
	';' => '&#059;',
	'!' => '&#33;',
	'(' => '&#40;',
	')' => '&#41;',
	'-' => '&#45;',
	'.' => '&#46;',
	'/' => '&#47;',
	':' => '&#58;',
	'?' => '&#63;',
	'[' => '&#91;',
	'\\' => '&#92;',
	']' => '&#93;',
	'^' => '&#94;'
	);
	sub codemsg {
		my $code = $_[0];
		if($code !~ /&\S*;/) { $code =~ s/;/&#059;/g; }
		$code =~ s~([\(\)\-\:\\\/\?\!\]\[\.\^])~$killhash{$1}~g;
		$_ = qq~<br><b>Code:</b><br><table bgcolor="#000000" cellspacing="1" width="90%"><tr><td width="100%"><table width="100%" cellpadding="2" cellspacing="0" bgcolor="#FFFFFF"><tr><td><font face="Courier" size="1" color="#000000">CODE</font></td></tr></table></td></tr></table>~;
		$_ =~ s~CODE~$code~g;
		return $_;
	}
}

sub donoopen {
	print qq~
<html><head><title>$txt{'199'}</title></head>
<body bgcolor=#ffffff>
<font size="2" face="Arial,Helvetica"><center>$txt{'199'}</center></font>
</body></html>~;
	exit;
}

1;
