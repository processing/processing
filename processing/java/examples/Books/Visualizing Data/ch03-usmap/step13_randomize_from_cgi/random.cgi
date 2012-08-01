#!/usr/bin/perl

# An array of the 50 state abbreviations
@states = ('AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA', 
           'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
           'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
           'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC', 
           'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY');

# A CGI script must identify the type of data it's sending,
# this line specifies that plain text data will follow. 
print "Content-type: text/plain\n\n";

# Loop through each of the state abbreviations in the array
foreach $state (@states) {

    # Pick a random number between -10 and 10. (rand() returns a  
    # number between 0 and 1, multiply that by 20 and subtract 10)
    $r = (rand() * 20) - 10;
    
    # Print the state name, followed by a tab,
    # then the random value, followed by a new line.
    print "$state\t$r\n";
}