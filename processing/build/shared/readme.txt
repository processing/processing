PROCE55ING D3VEL()PM3NT 3N\/1RONM3NT 

alpha release (revision XXXX)


>>>>>>>>>> THIS DOCUMENT IS IN PROGRESS FOR ALPHA <<<<<<<<<<<<


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


RELEASE NOTES & DEVELOPER SOAPBOX

herein follows lots of random notes about this release. you'll have to
pardon the chatty detail in some spots, as this will also serve as a
response to many of the 'frequently asked questions' that we have. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


SUMMARY

+ if you find issues (some call them 'bugs'), 

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


REVISIONS & ROADMAP

at least until the final "1.0" version, we'll be using four digit
numbers for the release. we're calling revision "XXXX" (this one)
alpha, which for us means "first publicly consumable app that can be
used by early adopters". 

there will be a few more numbered releases leading up to a beta
release. beta means that all the features are in, but not all the bugs
are out. there are several known issues with the alpha release (thin
lines, lack of alpha transparency, etc) that will need to be sorted
out for beta.

a few more numbered releases will follow, leading up to 1.0, a version
that we can actually proud of and that has a minimum number of
bugs. hopefully this is not a *long* ways off, but...


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


I FOUND A BUG!

what operating system, what revision

include release number, platform, 
and a copy of the code, preferably the folder from the sketchbook (see
sketchbook/default/SKETCHNAME) if there are images or other data being
used.

check out stdout.txt and stderr.txt in 'lib'


>>>>>>>>>>>>>> NEED TO WRITE THIS SECTION <<<<<<<<<<<<<<<<


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


PLATFORMS

the processing development environment runs best on:

1. windows
2. mac os x
3. linux
4. mac os 9

our priority for how well the beast runs looks like:

1. windows & mac os x (tied for first)
2. mac os 9
3. linux

windows is the superior platform for running java applications. it's
not because we like windows the best, so sorry to the zealots in all
other corners of the machine space, but that's just how it is. the vm
for mac os x is really quite good (especially when compared to apple's
previous efforts), but it's still a bit behind. we think os x will be
a great bet for the future, and apple is putting all their feeble
weight behind it, so hopefully it will evolve somewhere.

developing the version for mac os 9 is a big headache, but we think
lots of people still use the crusty operating system, so we're going
to keep supporting it for the meantime. the guess is that a lot of
schools are still using it in their labs, and since schools are a
significant target for the environment, we gotta play along.

for the linux version, you guys can support yourselves. if you're
enough of a hacker weenie to get a linux box setup, you oughta know
what's going on. for lack of time, we won't be testing extensively
under linux, but would be really happy to hear about any bugs or
issues you might run into. actually, we don't get happy that you're
having issues, but if you're going to have issues, we're happy that
you tell us about them, so we can fix them.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


MAC OS X

processing runs best with (and probably requires) Mac OS X 10.1 and 
"Java 1.3.1 Update 1", the latter of which is available as a free
update from the "Software Update" control panel. it can also be
downloaded from http://www.apple.com/downloads/macosx/apple/ or
from: http://www.apple.com/downloads/macosx/apple/java131.html

with os x 10.2 on its way, you'll no longer need the java update, 
but we'll have to see if we can afford being gouged $129 for the
upgrade in order to do some testing under this cat-themed operating
system. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


MAC OS 9

java applications on classic mac os are in a bad state, as apple has
decided (rightfully so) to abandon further development of their java
runtime.

SPEED: this version runs very slowly. the first time you hit the 'run'
button, it might take a while to bring up the actual
program. hopefully after that, things will improve.

VERSIONS: this version has only been tested under Mac OS 9.2.2. 

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHAT IS SKETCHBOOK?

>>>>>>>>>>>>>> NEED TO WRITE THIS SECTION <<<<<<<<<<<<<<<<


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


EXTERNAL FILES / FONTS / READING DATA FILES


things need to go in a folder called 'data' inside sketchbook/default

>>>>>>>>>>>>>> NEED TO WRITE THIS SECTION <<<<<<<<<<<<<<<<


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


SERIAL PORT

>>>>>>>>>>>>>> NEED TO WRITE THIS SECTION <<<<<<<<<<<<<<<<

the serial port is a useful way to hook things up to hardware
devices of your own devising. 

on macos9, works fairly well with my keyspan usb/serial adapter. thank
god for patrick beard and jdirect.

on macosx, need rxtx to be installed (dmg included with p5 download),
follow their bizarre instructions. on my machine, i'm using a keyspan
28X dual port adapter, and the selection i use on the serial port menu
reads "/dev/cu.USA28X21P1.1". your mileage may vary.

linux.. haven't tested but it's the ibm vm and their own
implementation, so it may just work.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


PROCE55ING IS FREE TO DOWNLOAD / FREE TO USE

we think it's important to have processing freely available, rather
than selling it for a million dollars under some godawful yearly
contract update scheme. to that end, we encourage people to distribute
it widely and refer back to the site: http://Proce55ing.net
on most of our own projects, we usually list them as "Built with
Proce55ing" or something similar, with a link back to the site. of
course this isn't a necessity, but it makes us happy when you do.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


SOURCE CODE / OPEN SOURCE / GPL BLAH BLAH

we plan for this project to be "open source", that trendy moniker
which means that you'll be able to look at all the code that's behind
the processing development environment and the graphics engine used in
tandem with it. 

the export libraries will probably be LGPL, which means they can be
used as a library and included in your project without you having to
open up your code (though we encourage people to share anyway). 

more information about the gnu public license can be found here:
http://www.gnu.org/copyleft/gpl.html

processing also includes other open projects, namely the oro matcher
and the kjc compiler. the oro tools are distributed under a bsd style
license as part of the apache jakarta project, and the kjc compiler is
part of the kopi suite of tools, which is released under the gpl. so
in fact, if the final, publicly available version of processing still
uses kjc, the code for processing will have to be released gpl.
more about the oro tools is at: http://www.savarese.org/oro/
and the home for kopi/kjc is here: http://www.dms.at/kopi/

we're sorry that the source code isn't available just yet, we're
cleaning and scrubbing it, it was a decision between getting the alpha
out to people to try versus taking a few more weeks to clean up the
project and deal with the technology licensing departments at mit and
ivrea. these things are more difficult and time consuming than they
would appear.