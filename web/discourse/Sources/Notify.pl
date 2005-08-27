###############################################################################
# Notify.pl                                                                   #
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

$notifyplver = "1 Gold - SP 1.4";

sub Notify {
	if( $currentboard eq '' ) { &fatal_error($txt{'1'}); }
	if($username eq "Guest") { &fatal_error("$txt{'138'}"); }

	# Check, if User already gets a notification
	fopen(FILE, "$datadir/$INFO{'thread'}.mail");
	@mails = <FILE>;
	fclose(FILE);

	$isonlist = 0;
	foreach $curmail (@mails) {
		$curmail =~ s/[\n\r]//g;
		if($settings[2] eq "$curmail") { $isonlist = 1; }
	}

	if ($isonlist){
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'125'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    $txt{'212'}<br>
    <b><a href="$cgi;action=notify3;thread=$INFO{'thread'};start=$INFO{'start'}">$txt{'163'}</a> - <a href="$cgi;action=display;num=$INFO{'thread'};start=$INFO{'start'}">$txt{'164'}</a></b>
    </font></td>
  </tr>
</table>
~;
	} else
	{
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" bgcolor="$color{'bordercolor'}" class="bordercolor">
  <tr>
    <td class="titlebg" bgcolor="$color{'titlebg'}"><font size=2 class="text1" color="$color{'titletext'}"><b>$txt{'125'}</b></font></td>
  </tr><tr>
    <td class="windowbg" bgcolor="$color{'windowbg'}"><font size=2>
    $txt{'126'}<br>
    <b><a href="$cgi;action=notify2;thread=$INFO{'thread'};start=$INFO{'start'}">$txt{'163'}</a> - <a href="$cgi;action=display;num=$INFO{'thread'};start=$INFO{'start'}">$txt{'164'}</a></b>
    </font></td>
  </tr>
</table>
~;
	}
	$yytitle = "$txt{'125'}";
	&template;
	exit;
}

sub Notify2 {
	if( $currentboard eq '' ) { &fatal_error($txt{'1'}); }
	if($username eq 'Guest') { &fatal_error($txt{'138'}); }

	$thread = $INFO{'thread'};
	$start = $INFO{'start'} ne '' ? $INFO{'start'} : 9999999;
	fopen(FILE, "$datadir/$thread.mail");
	@mails = <FILE>;
	fclose(FILE);

	fopen(FILE, ">$datadir/$thread.mail", 1) || &fatal_error("$txt{'23'} $thread.mail");
	print FILE "$settings[2]\n";
	foreach $curmail (@mails) {
		$curmail =~ s/[\n\r]//g;
		if($settings[2] ne $curmail) { print FILE "$curmail\n"; }
	} 
	fclose(FILE);

	$yySetLocation = qq~$cgi;action=display;num=$thread;start=$start~;
	&redirectexit;
}

sub Notify3 {
	if( $currentboard eq '' ) { &fatal_error($txt{'1'}); }
	if($username eq "Guest") { &fatal_error("$txt{'138'}"); }
	$thread = $INFO{'thread'};
	$start = $INFO{'start'} ne '' ? $INFO{'start'} : 9999999;
	fopen(FILE, "$datadir/$thread.mail");
	@mails = <FILE>;
	fclose(FILE);

	# if there is only one entry and this is the address which is to remove, remove the file at all

	if( $#mails eq 0) {
		unlink("$datadir/$thread.mail");
	} else
	# make no change with the file
	{
	fopen(FILE, ">$datadir/$thread.mail", 1) || &fatal_error("$txt{'23'} $thread.mail");
	foreach $curmail (@mails) {
		$curmail =~ s/[\n\r]//g;
		if($settings[2] ne "$curmail") { print FILE "$curmail\n"; }
	}
	fclose(FILE);
	}
	$yySetLocation = qq~$cgi;action=display;num=$thread;start=$start~;
	&redirectexit;
}

sub Notify4 {
	if($username eq "Guest") { &fatal_error("$txt{'138'}"); }
	my( $variable, $dummy, $dummy2, $threadno, @mails, $curmail );

	foreach $variable (keys %FORM) {
	 	$dummy = $FORM{$variable};
		($dummy2,$threadno) = split(/-/,$variable);
		if ($dummy2 eq "thread") {

			fopen(FILE, "$datadir/$threadno.mail");
			@mails = <FILE>;
			fclose(FILE);


			# if there is only one entry and this is the address which is to remove, remove the file at all
			if( $#mails eq 0) {
				unlink("$datadir/$threadno.mail");
			} else
			# make no change with the file
			{
			fopen(FILE, ">$datadir/$threadno.mail") || &fatal_error("$txt{'23'} $threadno.mail");
			foreach $curmail (@mails) {
				$curmail =~ s/[\n\r]//g;
				if($settings[2] ne $curmail) { print FILE "$curmail\n"; }
			}
			fclose(FILE);
			}

		}

	}
	&ShowNotifications;
}

sub ShowNotifications {
	if($username eq "Guest") { &fatal_error("$txt{'138'}"); }

	my(@dirdata,@datdata,$filename,$entry,@entries,$mnum,$dummy,$msub,$mname,$memail,$mdate,$musername,$micon,$mattach,$mip,$mmessage,@messages,@found_number,@found_subject,@found_date,@found_username);

	# Read all .mail-Files and search for username
	opendir (DIRECTORY,"$datadir");
	@dirdata = readdir(DIRECTORY);
	closedir (DIRECTORY);
	@datdata = grep(/mail/,@dirdata);

	# Load Censor List
	&LoadCensorList;
	
	foreach $filename (@datdata) {
		fopen(FILE, "$datadir/$filename");
		@entries = <FILE>;
		fclose(FILE);
	        foreach $entry (@entries) {
	        	$entry =~ s/[\n\r]//g;
	        	if ($entry eq $settings[2]) {
				($mnum, $dummy) = split(/\./,$filename);
				fopen(FILE, "$datadir/$mnum.txt");
				@messages = <FILE>;
				fclose(FILE);
				($msub, $mname, $memail, $mdate, $musername, $micon, $mattach, $mip,  $mmessage) = split(/\|/,$messages[0]);
				push(@found_number,$mnum);
				push(@found_subject,$msub);
				push(@found_date,$mdate);
				push(@found_username,$musername);
				push(@found_name,$mname);
			}
		}
	}

	# Display all Entries
	$yymain .= qq~
<table border="0" width="100%" cellspacing="1" cellpadding="6" bgcolor="$color{'bordercolor'}" class="bordercolor">
 <tr>
  <td bgcolor="$color{'titlebg'}" class="titlebg">
   <font size="2" color="$color{'titletext'}" class="text1"><b>$txt{'418'}</b></font>
  </td>
 </tr><tr>
  <td bgcolor="$color{'windowbg'}" class="windowbg">
   <font size=2>
    <br>
~;


	if (@found_number==0) {
		$yymain .= "$txt{'414'}<br><br>&nbsp;";
	} else {
		foreach (@censored) {
		($tmpa,$tmpb) = @{$_};
		$found_subject[$counter] =~ s~\Q$tmpa\E~$tmpb~gi;
		}
		$yymain .= qq~<form action="$cgi;action=notify4" method=post>
<table>
  <tr>
      <td colspan=2><font size=2>$txt{'415'}:</font><br>&nbsp;</td></tr>
~;
		$counter=0;
		foreach $entry (@found_number) {
			&FormatUserName($found_username[$counter]);
			$yymain .= "<tr><td><font size=2>";
			$yymain .= qq~<input type=checkbox name="thread-$found_number[$counter]" value="1"></font></td>~;
			$yymain .= qq~<td><font size=2><b><i>$found_subject[$counter]</i></b> $txt{'525'} <a href="$scripturl?board=;action=viewprofile;username=$useraccount{$found_username[$counter]}">$found_name[$counter]</a></font></td></tr>\n~;
			$counter++;
		}
		$yymain .= "<tr><td colspan=2><br><font size=2>$txt{'416'}</font><br>&nbsp;</td></tr>\n";
		$yymain .= qq~<tr><td>&nbsp;</td><td><input type=reset value="$txt{'278'}">&nbsp;&nbsp;&nbsp;<input type=submit value="$txt{'417'}"></td></tr>~;
		$yymain .= "</table></form><br>&nbsp;\n";
	}

	$yymain .= qq~
   </font>
  </td>
 </tr>
</table>
~;
	$yytitle = "$txt{'417'}";
	&template;
	exit;
}
sub remove_notifications {
	my (@deademails, @maildir, $filename, $content, $content_old) = (@_);

	opendir (DIRECTORY,"$datadir");
	@maildir = grep {/\.mail$/} readdir(DIRECTORY);
	closedir (DIRECTORY);

	foreach $filename (@maildir) {
		$content = '';

		fopen(MYFILE, "+<$datadir/$filename") || &fatal_error("$txt{'23'} $filename");
		while (<MYFILE>) { $content .= $_; }
		$content_old = $content;

		foreach (@deademails) { $content =~ s/$_(?:\r\n|\r|\n)//ig; }

		if (length($content) > 0) {
			if ($content ne $content_old) {
				truncate(MYFILE,0);
				seek(MYFILE,0,0);
				print MYFILE $content;
				fclose(MYFILE);
				$done .= "$filename\n";
			}
			else {
				fclose(MYFILE);
			}
		}
		else {
			fclose(MYFILE);
			unlink("$datadir/$filename");
		}
	}
}

sub replace_notifications {
	my ($old_email, $new_email, @maildir, $filename, $content, $content_old) = (shift, shift);

	opendir (DIRECTORY,"$datadir");
	@maildir = grep {/\.mail$/} readdir(DIRECTORY);
	closedir (DIRECTORY);

	foreach $filename (@maildir) {
		$content = '';

		fopen(MYFILE, "+<$datadir/$filename") || &fatal_error("$txt{'23'} $filename");
		while (<MYFILE>) { $content .= $_; }
		$content_old = $content;

		$content =~ s/$old_email/$new_email/ig;

		if (length($content) > 0) {
			if ($content ne $content_old) {
				truncate(MYFILE,0);
				seek(MYFILE,0,0);
				print MYFILE $content;
				fclose(MYFILE);
			}
			else {
				fclose(MYFILE);
			}
		}
		else {
			fclose(MYFILE);
			unlink("$datadir/$filename");
		}
	}
}


1;
