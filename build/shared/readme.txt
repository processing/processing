PROCE55ING DEVELOPMENT ENVIRONMENT

RELEASE 0044 - 2 SEPTEMBER 2002

(c) 2002, 2001 Massachusetts Institute of Technology 
and Interaction Design Institute Ivrea


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


RELEASE NOTES & DEVELOPER SOAPBOX

herein follows lots of random notes about this release. you'll have to
pardon the chatty detail in some spots, as this will also serve as a
response to many of the 'frequently asked questions' that we have. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


GETTING STARTED

double click the 'Proce55ing' application, and select something from
the examples menu: File -> Open -> Examples. hit the 'run' button
(which looks like the play button on a vcr or tape deck). 

lather, rinse, repeat as necessary.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


THANKS TO...

all the people who reported bugs for this release: jes, adrien,
eviltyler, fdb, ik0, chrism, jeremy, mKoser, michaONE.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


REVISIONS & ROADMAP

at least until the final "1.0" version, we'll be using four digit
numbers for the release. we're calling revision "0043" the first
"alpha", which for us means "first publicly consumable app that can 
be used by early adopters". later revisions (like this one) will
simply be numbered.

the numbered releases aren't heavily tested, so don't be surprised
if/when something breaks.. just report the problem and go back to the
previous numbered release until there's a fix.

there will be a few more numbered releases leading up to a beta
release. beta means that all the features are in, but not all the bugs
are out. there are several known issues with the alpha release (thin
lines, lack of alpha transparency, etc) that will need to be sorted
out for beta.

additional numbered releases will follow, leading up to 1.0, a 
version that we can actually proud of and that has a minimum number 
of bugs. hopefully this is not a *long* ways off, but...


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


I FOUND A BUG!

we prefer to call them "issues."

first, be sure to check under the notes for your specific platform to
make sure it isn't a known issue or that there isn't a simple fix.

you can either post to the bulletin board at:
http://proce55ing.net/discourse/
or send email to bugs@proce55ing.net. the bboard is probably the
better way to go, because more people will be watching it. the email
goes straight to the developers, but their schedules are erratic and
it could be anywhere from two minutes to two weeks before you receive
a response. if you want to go straight to the bugs page, it's:
http://proce55ing.net/discourse/yabb/YaBB.cgi?board=Proce55ing_software_bugs

when reporting this "bug" please include information about
1. the revision number (i.e. 0043)
2. what operating system you're using, on what kind of hardware
3. a copy of your code
4. details of the error, which may be the last few lines from 
   the files stdout.txt or stderr.txt from the 'lib' folder. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


GENERAL NOTES / COMMON MISTAKES

- size() must use numbers, not variables. this is because of how
  the size command is interpreted by proce55ing. 

- when using draw() mode, background() must also use only numbers, and
  no variables. this is similar to the issue with the size command,
  because in both cases, Proce55ing needs to know the size and
  background color of the app before it starts, so since variables
  are determined while the program is running, things break. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


GOODIES & SEMI-HIDDEN FEATURES

- shift-click on the 'run' button to go straight to 'present' mode

- for quick renaming, just click on the sketch title 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


PLATFORMS

the processing development environment runs best on:

1. windows 2000/XP
2. mac os x
3. linux
4. mac os 9
5. windows 95/98/ME

our priority for how well the beast runs looks like:

1. windows & mac os x (tied for first)
3. mac os 9
4. windows 95/98/ME (because we must)
5. linux

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

windows 95/98/ME is a piece of crap, but since lots of people (are
often forced to) use it, we'll try and support. early alpha versions
seem to be having trouble with 95/98/ME, but it'll run better in the
future.

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

another issue under osx.. it appears that there's a bug in their java
vm with text areas, so as a result, the arrow keys won't work in the
editor. we think this really stinks and are searching for options for
a (near?) future release.

"Caught java.lang.UnsatisfiedLinkError" on startup...
in order to use the serial port under macosx, you'll need to install
RXTX, the serial port driver. this is for more advanced users, and the
package is included with the p5 download, and includes its own
instructions. 

naming of sketches.. on other platforms, you aren't allowed to type
characters besides letters, numbers, and underscores for the names of
sketches. because of what looks like a bug in osx java, this feature
is disabled, and the file is simply renamed (bad characters are
replaced with underscores) after you hit 'ok'. boo apple.. i'm getting
sick of all these workarounds.

quitting presentation mode.. on other platforms, hitting the 'escape'
key will quickly get you out of presentation mode. however, there
seems to be some key event weirdness under osx. we hope to find a fix
someday.

arrow keys work (they didn't under 43) using a hokey algorithm to do
placement of the cursor. this is a workaround for an apple bug. things
might be a little funny, but maybe someone else will be able to repair
this in the future.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


MAC OS 9

java applications on classic mac os are in a bad state, as apple has
decided (rightfully so) to abandon further development of their java
runtime.

speed: this version runs very slowly. the first time you hit the 'run'
button, it might take a while to bring up the actual
program. hopefully after that, things will improve.

versions: this version has only been tested under Mac OS 9.2.2. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WINDOWS

win2k works well, reports point to xp working well also. win95/98/me
seems to have lots of trouble, but it's just with the .exe that we
use, so that'll get fixed in the future.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHAT IS SKETCHBOOK?

we think most "integrated development environments" (microsoft visual
studio, codewarrior, jbuilder) tend to be overkill for the type of
audience we're targeting with Proce55ing. for this reason, we've
introduced the 'sketchbook' which is a more lightweight way to
organize projects. as trained designers, we'd like the process of
coding to be a lot more like sketching. the sketchbook and the
'history' menu under 'sketch', are attempts in that direction.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


EXTERNAL FILES / FONTS / READING DATA FILES


if you want to use external files, like images or text files 
or fonts, they should be placed in a folder called 'data' inside:
sketchbook -> default -> SKETCH_NAME

starting with version 44, there are several functions that make
dealing with data in files much easier (loadFile, loadStrings,
splitStrings, etc) so file i/o should be fun!


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


SERIAL PORT

the serial port is a useful way to hook things up to hardware
devices of your own devising. the reference describes the specifics 
of how to use the serial port.

the windows version works well, much better than in previous releases.

on macos9, works fairly well with my keyspan usb/serial adapter. thank
god for patrick beard and jdirect.

on macosx, need rxtx to be installed (pkg included with p5 download),
follow their bizarre instructions. on my machine, i'm using a keyspan
28X dual port adapter, and the selection i use on the serial port menu
reads "/dev/cu.USA28X21P1.1". your mileage may vary.

linux.. haven't tested but it's the ibm vm and their own
implementation, so it may just work.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHY IS IT CALLED "PROCE55ING"?

"Proce55ing" is a combination of numbers and letters but is simply
pronounced "processing." 

at their core, computers are processing machines. they modify, move,
and combine symbols at a low level to construct higher level
representations. Proce55ing allows people to control these actions and
representations through writing their own programs. The spelling
"Proce55ing" makes reference to the encoding that is necessary for
transferring ideas into a machine readable form. 

the project also focuses on the "process" of creation rather than end
results. the design of the software supports and encourages sketching
and the website presents fragments of projects and exposes the
concepts behind finished software.

honestly, had the URL "www.processing.net" been available, the project
would have been called "Processing" and not "Proce55ing." 


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
tandem with it. we can't promise, since we're still working on getting
the licensing taken care of with our employers, but we think this
should likely happen soon.

the export libraries (also known as 'bagel') will probably be LGPL,
which means they can be used as a library and included in your project
without you having to open up your code (though we encourage people to
share anyway). 

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
ivrea. these things are far more difficult and time consuming than
they would appear.
