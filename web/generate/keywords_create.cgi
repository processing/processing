#!/usr/bin/perl
#require 'globals_API.pl';
#require 'globals.pl';


# CURRENT BUGS
# Making a line with \t\tblank
# fixed: [] array access symbols are not appearing
# ??? there are overlaps when methods are included (blend() and image.blend())


#$dir = "API";
$dir = "../content/api_en/";
# This path is for the configuration on Casey's local machine
$path = "../../Processing/build/shared/lib/";

# Open base file and copy into array
open (BASE, "keywords_base.txt") || die "can't open keywords_base.txt: $!";
@baseinfo = <BASE>; 
close(BASE);
chomp(@baseinfo);

foreach $bi (@baseinfo) {
  push(@modfiles, $bi)
}

# Add a blank line to separate the data
push(@modfiles, "\n");

opendir(DIR, $dir) || die $!;
@tempfiles = readdir(DIR);
closedir(DIR);

foreach $temp (@tempfiles) {
  if($temp =~ ".xml" && !($temp =~ "~")) {
    get_data("$temp"); 
    $tempname = strip_name($name);
    $namelength = length($tempname);
    if ($tempname ne "x" && $tempname ne "y" && $tempname ne "z" && $tempname ne "") {
        push(@modfiles, strip_name($name) . "\t" . set_category() . "\t" . file_name_convert($temp));
    }
  }
 }


open(KEYWORDS, ">$path/keywords.txt");

foreach $temp (@modfiles) {
  # Add additional API to file
  print KEYWORDS "$temp\n";
  print "$temp\n";
}

close(KEYWORDS);





sub get_data
{
  open (CAT, "$dir/$_[0]") || die "can't open $_[0]: $!";
  @cat = "";
  while (<CAT>) { 
    chomp;
    push(@cat, $_);
    #print "$_\n";
  }
  close CAT;
  foreach $el (@cat) {
    if(grep{/^<type>/} $el) {
      $type_cat = $el;
      $type_cat =~ s/<type>//;
      $type_cat =~ s/<\/type>//; 
    }
    if(grep{/^<name>/} $el) {
      $name_cat = $el;
      $name_cat =~ s/<name>//;
      $name_cat =~ s/<\/name>//; 
    }
  }
  $type = $type_cat;
  $name = $name_cat;
}

sub strip_name
{
    local ($page) = @_[0];
    
    # Exceptions for constants with PI
    if($page =~ /PI/) {
	$_ = $page;
	$page =~ s/\(.*\)//g;   # Remove all between parenthesis
	$page =~ s/\s//g;       # Remove the spaces
        #print("$page\n");
    }

    $_ = $page;
    if(!($page =~ s/\(\)//g))  # Truncate all functions 
    {
      # Exception for the case of pixels[] and vpixels[]
      if(/pixels\[\]/) {
	  $page =~ s/\[\]//g;
      }

      # Exception for the case of all operators
      if(/\(.*\)/) {
      #$page = $&;        
      $tp = $&;             # Get the entire match
	$page =~ s/$tp//g;  # Remove the entire match
        $page =~ s/\(//g;  # Remove the left paren
	$page =~ s/\)//g;  # Remove the right paren
      }
    } 

    # Exception for the case of () (parenthesis)
    if($page =~ /paren/) {
      #$tp = $&;             # Get the entire match
      $page =~ s/\(.*\)/()/g;  # Remove the entire match
    }

    $page =~ s/\s//g;       # Remove the spaces
    $page =~ s/\&lt;/\</g;   # Replace <
    $page =~ s/\&gt;/\</g;   # Replace >
    $page =~ s/\&amp;/\&/g;  # Replace &
    $page =~ s/\*\///g;      # Replace */
 

    return $page;
}

sub set_category
{
    if($type =~ s/constant/constant/i) {
        $category = "LITERAL2";
    } elsif ($type =~ s/variable/variable/i || $type =~ s/field/field/i ) {
        $category = "LITERAL2";
    } elsif ($type =~ s/datatype/datatype/i || $type =~ s/structure/structure/i ||
             $type =~ s/keyword/keyword/i || $type =~ s/object/object/i ) {
        $category = "KEYWORD1";
    } elsif ($type =~ s/function/method/i || $type =~ s/method/method/i ) {
        $category = "KEYWORD2";
    } else {
        $category = "";
    }

    if($type =~ s/processing//i) {
      $category = "KEYWORD3";
    }

    return $category;
}

sub file_name_convert 
{
    local ($thisfile) = @_[0];

    # Remove the "xml" from the files
    $thisfile =~ s/\.xml//;
    
    # Remove the "converts" from the xml files
    $thisfile =~ s/convert//;

    # Remove the "_var" from the xml files
    $thisfile =~ s/_var//;
    if(($type =~ /function/i) || ($type =~ /structure/i) ||($type =~ /method/i)) {
      $thisfile = $thisfile . "_";
    }

    return $thisfile;
}
