#!/usr/bin/perl


open(F, "../../bagel/Bagel.java") || die $!;
@contents = <F>;
close(F);

open(APPLET, "ProcessingApplet.java") || die $!;
@applet = <APPLET>;
close(APPLET);

$insert = 'public functions from bagel';

open(OUT, ">ProcessingApplet.java") || die $!;
foreach $line (@applet) {
    print OUT $line;
    last if ($line =~ /$insert/);
}


#open(OUT, ">>ProcessingApplet.java") || die $!;
select(OUT);

$comments = 0;

#print "\n\n";

#foreach $line (@contents) {
while ($line = shift(@contents)) {
    $decl = "";

    if ($line =~ /\/\*/) {
	$comments++;
    }
    if ($line =~ /\*\//) {
	$comments--;
    }
    next if ($comments > 0);

    if ($line =~ /^\s*public (\w+) [a-zA-z_]+\(.*$/) {
	#print "$1\n";
	#$decl .= $line;
	if ($1 ne 'void') {
	    $returns = 'return';
	} else {
	    $returns = '';
	}
	print "\n\n$line";
	$decl .= $line;
	while (!($line =~ /\)/)) {
	    $line = shift (@contents);
	    $decl .= $line;
	    print $line;
	    #print shift (@contents);
	}

	$decl =~ /\s(\S+)\(/;
	$decl_name = $1;
	#print "dec $decl_name\n";
	print "   $returns g.${decl_name}(";

	$decl =~ s/\s+/ /g; # smush onto a single line
	$decl =~ s/^.*\(//;
	$decl =~ s/\).*$//;

	$prev = 0;
	@parts = split(', ', $decl);
	foreach $part (@parts) {
	    ($the_type, $the_arg) = split(' ', $part);
	    $the_arg =~ s/[\[\]]//g;
	    #print "* $the_arg\n";
	    if ($prev != 0) {
		print ", ";
	    }
	    print "${the_arg}";
	    $prev = 1;
	}
	print ");\n";

	#print "$decl\r\n";

	print "  }\n"; #\n";
    }
}
print "}\n";
