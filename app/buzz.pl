#!/usr/bin/perl

# needs to make a temporary directory, compile into that
# clear out contents of temporary directory at begin of compile
# create temp directory if it doesn't exist

# should take arguments for the compiler:
# jikes -d classes *.java
# instead looks like
# buzz "jikes -d classes" *.java
# maybe everything always goes in /tmp? 
# (no, don't want to leave code around)

$blank_line = "\n";


# tell me about this here system
if ($ENV{'WINDIR'} ne '') {
    $separator = "\\";
    $path_separator = ';';
# need path to mkdir for win32, because 'mkdir' defaults to the
# crappy dos version.. need to use cygwin instead
    $mkdir_path = 'd:\\cygwin\\bin\\mkdir';
    $platform = 'windows';
} else {
    $separator = '/';
    $path_separator = ':';
    $mkdir_path = '/bin/mkdir';
    $platform = 'unix';
}
# something else needed here for macosx
# maybe something with OSTYPE or HOSTTYPE (esp OSTYPE)
# e.g. OSTYPE=linux-gnu HOSTTYPE=i386


# create the temporary directory
$temp_dir = "buzztemp";
if (-d $temp_dir) {
    `rm -rf $temp_dir`;
}
mkdirs($temp_dir, 0777) || die $!;


#print "args = @ARGV\n";
$command = shift(@ARGV);
if ($command eq '') {
    print "buzz.pl: perl is misconfigured.. no args passed in.. can't run\n";
    print "         cygwin perl seems to have problems, use activestate\n";
    exit;
}

if ($command =~ /-classpath/) {
    die "cannot set classpath using this version of buzz";
}
$classpath = $ENV{"CLASSPATH"};
if ($classpath eq "") {
    # find java in the path
    if ($platform eq 'windows') {
	@elements = split(';', $ENV{"PATH"});
	foreach $element (@elements) {
	    #print "trying $element\\java.exe\n";
	    if (-f "$element\\java.exe") {
		$classpath = "$element\\..\\lib\\classes.zip";
		print "found java: $element\\java.exe\n";
		last;
	    }
	}
	if ($classpath eq "") {
	    die "java.exe is not in your path, and classpath not set";
	}

    } elsif ($platform eq 'unix') {
	die "code for searching path not written for unix\nset environment varilable for CLASSPATH before using.\n";

    } elsif ($platform eq 'macosx') {
    }
}


# if target directory, -d, option is used, add it to CLASSPATH
if ($command =~ /\-d\s(\S*)/) {
    $classpath = "$1$path_separator$classpath";
}


foreach $arg (@ARGV) {
    if ($arg =~ /^-d(.*)/) {
	$params{$1} = 1;
    #} elsif ($arg =~/^-c(.*)/) {
	#$compiler = $1;

    } elsif ($arg =~ /\.java$/) {

	# this only gets hit under windows, because *.java won't be
	# expanded under the command line.. 
	if ($arg =~ /(.*)\*\.java$/) {
	    # gotta expand * to all matching
	    #print "expanding *.java from \"$1\"\n";
	    $dir = $1;
	    if ($dir eq "") {
		$dir = '.';
	    } else {
		#print "creating dir $temp_dir$separator$dir\n";
		mkdirs("$temp_dir$separator$dir", 0777) || die "$temp_dir$separator$dir $!";
	    }
	    opendir(DIR, $dir) || die $!;
	    @dcontents = readdir(DIR);
	    closedir(DIR);
	    foreach $file (@dcontents) {
		if ($file =~ /\.java$/) {
		    if ($dir eq '.') {
			$fullname = "$file";
		    } else {
			$fullname = "$dir$file";
		    }
		    #print "adding $fullname\n";
		    unshift @file_list, "$fullname";
		}
	    }
	    
	} else {
	    # make the directory that this file is in, in case it's 
	    # buried more than one level deep. this is especially 
	    # important under unix, because the above mkdir stuff won't
	    # get hit due to *.java being expanded on the cmd line
	    $arg =~ /(.*)$separator([\w\d]+\.java)/;
	    $file_path = $1;
	    $file_name = $2;
	    if ($file_path ne '') {
		#print "_${file_path}_ and _${file_name}_\n";
		#if (!(-d $file_path)) {
		mkdirs("$temp_dir$separator$file_path", 0777);
		#}
	    }

	    unshift @file_list, $arg;	
	}
    }
}

# support: define, ifdef, ifndef, else, endif
# no support: defined(x), elif, #define blah 12, nesting
#exit; # testing why bagel dir is getting created

print "processing...\n";
foreach $file (@file_list) {
    open(FILE, "$file") || die "error with $file, $!";
    @contents = <FILE>;
    close(FILE);

    @new_contents = ();
    &read_positive;

    #printf "try to make $temp_dir$separator$file\n";
    open(OUTPUT, ">$temp_dir$separator$file") || die $!;
    print OUTPUT reverse(@new_contents);
    close(OUTPUT);
    unshift(@new_file_list, "$temp_dir$separator$file");
}

print "compiling...\n";
$files = join(' ', @new_file_list);
$compile_command = "$command -classpath $classpath $files";
#print "$compile_command\n";
print `$compile_command`;

# clean up
print "cleaning...\n";
`rm -rf $temp_dir`;

# finished
print "done.\n";


# reads until else or endif, adding what it finds
# to the new output file
sub read_positive {
    my $line;
    while ($line = shift(@contents)) {
	if ($line =~ /$\#if(\w*)def\s+(\S+)/) {
	    unshift(@new_contents, $blank_line);
	    if ((($1 eq "") && ($params{$2} == 1)) ||   #ifdef found
		(($1 eq "n") && ($params{$2} != 1))) {  #ifndef found
		# include until endif/else
		&read_positive;
		#return;

	    } else {
		# exclude until endif/else
		&read_negative(0);
                #return;
	    }
	} elsif ($line =~ /$\#else/) {  
	    unshift(@new_contents, $blank_line);
	    &read_negative(0);
	    return;

        } elsif ($line =~ /$\#endif/) {
	    unshift(@new_contents, $blank_line);
	    return;

	} elsif ($line =~ /$\#define\s+(\S+)/) {
	    $params{$1} = 1;
	    unshift(@new_contents, $blank_line); # maintain lf count

	} else {
	    unshift(@new_contents, $line);  # no change
	}
    }
}


# excludes everything until an else or an endif
sub read_negative {
    my ($inside_negative) = @_[0];
    my $line;
    while ($line = shift(@contents)) {
	if ($line =~ /$\#if(\w*)def\s+(\S+)/) {
	    unshift(@new_contents, $blank_line);
	    if ((($1 eq "") && ($params{$2} == 1)) ||   #ifdef found
		(($1 eq "n") && ($params{$2} != 1))) {  #ifndef found
		#&read_positive;
		&read_negative(1);
		
	    } else {
		# exclude until endif/else
		&read_negative(1);
	    }
	} elsif ($line =~ /$\#else/) {
	    unshift(@new_contents, $blank_line);
	    if ($inside_negative) {
		&read_negative(1);
	    } else {
		&read_positive;
	    }
	    return;
		 
        } elsif ($line =~ /$\#endif/) {
	    unshift(@new_contents, $blank_line);
	    return;

	#} elsif ($line =~ /$\#define\s+(\S+)/) {
	#   unshift(@new_contents, $blank_line); # maintain lf count

	} else {
	    # blank line, maintain lf count
	    unshift(@new_contents, $blank_line);
	}
    }
}


# recursively make directories if they don't yet exist
# this turned into a hack because i was lazy
sub mkdirs {
    my $d = @_[0];
    #print "making dir $d\n";
    $d =~ s/\\/\//g; # make backslashes into fwd slashes
    my $result = `$mkdir_path -p $d`;
    return 1;
}
