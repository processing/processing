PROCESSING DEVELOPMENT ENVIRONMENT

(c) 2001-03 Ben Fry and Casey Reas
Massachusetts Institute of Technology 
and Interaction Design Institute Ivrea


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


RELEASE NOTES & DEVELOPER SOAPBOX

herein follows lots of random notes about the alpha releases of
processing. more up-to-date details can be found in "revisions.txt"
which has notes about individual releases.

'revisions.txt' contains more information about the specific updates
and fixes in this release.

you'll have to pardon the chatty detail in some spots, as this will
also serve as a response to many of the 'frequently asked questions'
that we have.  


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


GETTING STARTED

double click the 'Processing' application, and select something from
the examples menu: File -> Open -> Examples. hit the 'run' button
(which looks like the play button on a vcr or tape deck). 

lather, rinse, repeat as necessary.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


THANKS TO...

thanks to the many people who have been helping us out. 
it's huge. i'll get a nice long list of y'all in here soon.


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

a cultured software elite such as yourself should use the gentleman's
term "issue."

first, be sure to check under the notes for your specific platform to
make sure it isn't a known issue or that there isn't a simple fix.

note! avoid the urge to just email us at processing@media.mit.edu, 
or sending mail to ben or casey directly. while you may prefer the
privacy of an email, it's much quicker for you to ask the whole gang,
who are super helpful. we also what we use to keep track of bugs, so
we may just ask you to use the bboard anyway.

ok where was i.. next, check the bboard to see if something related
has been reported, or if there is already a workaround.

best method is to post to the bulletin board at:
http://proce55ing.net/discourse/
we prefer for you to use the bboard for bugs, since:
- we like to use the bboard as a way to track bugs and get feedback
- casey and ben can't always respond quickly to email 
- and there are several knowledgeable people on the bboard
if you want to go straight to the bugs page, it's: 
http://proce55ing.net/discourse/yabb/YaBB.cgi?board=Proce55ing_software_bugs

when reporting this "bug" please include information about
1. the revision number (i.e. 0048)
2. what operating system you're using, on what kind of hardware
3. a copy of your code--the smallest possible piece of code that will
   produce the error you're having trouble with. 
4. details of the error, which may be the last few lines from 
   the files stdout.txt or stderr.txt from the 'lib' folder. 

for stranger errors during compile time, you can also look inside the
"build" folder inside "lib", which is an intermediate (translated into
java) version of your code.

the more details you can post, the better, because it helps us figure
out what's going on. useful things when reporting:

- we want the minimum amount of code that will still replicate the
  bug. the worst that can happen is we get a report that says
  "problem!" along with a three page program. sure, everyone likes a
  puzzle, but simpler code will be a faster response. 

- occasionally we may need you to pack up a copy of your sketchbook or
  something similar so that we can try and replicate the weirdness on
  our own machine. rest assured, we have no interest in messing with
  your fancy creations or stealing your ideas. the p5 team is a pair
  of straight-laced boys who hail from the midwestern u.s. who were
  brought up better than that. and as we often lack enough time to
  build our own projects, we have even less time to spend figuring out
  other peoples' projects to rip them off.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


GOODIES & SEMI-HIDDEN FEATURES

- shift-click on the 'run' button to go straight to 'present' mode

- for quick renaming, just click on the sketch title 

- inside the 'lib' folder is a 'pde.properties' file, which contains a
  handful of settings for your app and how it's set up. you can change
  the coloring of things, or even change your sketchbook location
  inside this file. a second file with a similar title but that
  includes "windows" or "macosx" etc in the name is for tweaks
  specific to your platform. for instance, we use the macosx-specific
  properties file to set the font size a little differently than on
  windows.


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
not because we like windows the best, (sorry to the zealots in all
other corners of the machine space) but that's just how it is. the vm
for mac os x is really quite good (especially when compared to apple's
previous efforts), but it's still a bit behind. we think os x will be
a great bet for the future, and apple is putting all their feeble
weight behind it, so hopefully it will evolve somewhere.

developing the version for mac os 9 is a big headache, but we think
lots of people still use the crusty operating system, so we're going
to keep supporting it for the meantime. the guess is that a lot of
schools are still using it in their labs, and since schools are a
significant target for the environment, we gotta play along. in the
short term, however, development for mac os 9 has been suspended.

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

the most current release has only been tested on Mac OS X 10.2.6. 
your mileage may vary if you're running something else. actually, your
mileage will vary no matter what, because who knows what this software
is gonna do. you're playing with free, alpha software. get psyched!

minimum requirements.. processing requires at least Mac OS X 10.1. 
if you're running anything older than 10.2, you'll need "Java 1.3.1
Update 1", the latter of which is available as a free update from 
the "Software Update" control panel. it can also be downloaded from
http://www.apple.com/downloads/macosx/apple/ or from:
http://www.apple.com/downloads/macosx/apple/java131.html
for what it's worth, we don't test processing under mac os x 10.1 
and we don't recommend it at all. 

mouse wheel support only works if you're using java 1.4. the latest
version of java will be available via the software update control
panel.

(actually this paragraph is only relevant if you want to try java 1.4,
 since we wound up using 1.3 as the default for release 58)
if you're having random troubles (exceptions being thrown, 
screen painting weirdness, general confusion) you might want to 
try running processing with java 1.3.1 instead of java 1.4. to do so, 
right-click or control-click the processing application and select 
"Show Package Contents". go to Contents -> Resources -> and then 
open MRJApp.properties in a text editor. remove the # from this line:
com.apple.mrj.application.JVMVersion=1.3.1
and add a # in front of this line:
com.apple.mrj.application.JVMVersion=1.3+

serial port.. we use rxtx (version 2.1_6) to handle serial i/o, which
is included with the processing release. unlike previous releases
(anything before 57), it no longer requires separate installation. 
however, if this is the first time you're using rxtx, you'll need to
run serial_setup.command (double-click it and follow the instructions)
to make sure that things are properly set up (a few permissions
need to be changed). if you're getting a "serial port is already in
use by another application" it's possible that you haven't run this
script. you may also need to reboot after running the script. on my
machine, i installed the keyspan driver for my usb-serial converter,
ran the script, and then rebooted in order for things to work. in the
past, i've used a keyspan 28X dual port adapter, and the selection i
use on the serial port menu reads "/dev/cu.USA28X21P1.1". you'll
probably have something similar. don't mind the frightening names.

another note on serial port.. tom igoe was kind enough to note that
you'll be in a world of hurt if you disconnect your serial adapter
while a sketch is running--it'll prolly freeze the machine and require
a forced reboot. (while this may seem nutty, you might run into it if
your adapter is plugged into your usb keyboard, and you have the
keyboard plugged into a monitor/keyboard switcher).

quitting presentation mode.. on other platforms, hitting the 
'escape' key will quickly get you out of presentation mode. however, 
there seems to be some key event weirdness under osx. we hope to find 
a fix someday.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


MAC OS 9

we have temporarily suspended development for mac os 9, because we
don't have time to fight with this dying os before beta. we hope to
resume mac os 9 development before releasing the final 1.0 version.

for releases earlier than 57:

java applications on classic mac os are in a bad state, as apple has
decided (rightfully so) to abandon further development of their java
runtime under OS 9.

serial works fairly well with my keyspan usb/serial adapter. thank god
for patrick beard and jdirect.

versions: we only test under Mac OS 9.2.2, all others.. who knows?


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WINDOWS

win2k and winxp are used as the primary development platforms, so the
release will likely work best on either platform.

win95/98/me seem to have some trouble, but we think it's just with
the .exe that we use, so that'll get fixed in the future. you can try
using the 'run.bat' file instead, and see if that works better.

the release is now split into 'standard' and 'expert' versions. the 
basic release includes a working java vm, and is all set up and ready 
to go. the advanced version is for people who already have java 
installed (and don't want to deal with the 20MB download), and know 
what they're doing enough that they can also install the serial port 
code by hand. instructions on installing the serial code are in the 
'serial' folder inside the 'expert' release.

out of memory? try adjusting the parameters in the file 'run.bat' and
use that to run instead of Processing.exe. short instructions can be
found inside that file.

mouse issues: by default, windows seems to skip every other pixel on
screen, causing weirdness for some drawing applications done with
p5. if you're seeing this, you can fix it by going to the windows
"mouse" control panel, the "pointer options" tab, and select "enhance
pointer precision." (this was actually tracked down by someone else in
the p5 community, whose name i have misplaced. if it was you, please
drop me a line so you can be properly cited. this kind of help is huge
for us, since we're such a small group!)

"hs_err_pid10XX.txt" error.. this is something within the java vm that
we can't fix. it's not clear what the problem is, but it seems to have
show up with java 1.4.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


LINUX

the processing application is just a shell script, you can use this
as a guide to getting p5 to run with your specific configuration,
because who knows what sort of setup you have. this release was tested
on a redhat 9 box, and sun's jre 1.4.2 is included with the
download. replacing (or making a symlink to) the contents of the
'java' folder will let you tie in a preferred jvm for your machine.

jikes.. just as 58 was being released, we ran into a problem where
jikes (the java compiler used by p5) couldn't be found by the
application on linux. faced with the deadline, we decided to put up an
error message saying it wasn't found. you should make sure jikes
version 1.18 (we strongly recommend this specific version!) is
installed on your machine and in your path.

serial.. this release uses rxtx-2.1_6 (just like macosx). you may get
error message spew to the console when starting the application saying
"Permission denied" and "No permission to create lock file" and to
read "INSTALL". this is because you need to add yourself to either the
uucp or lock group so that processing can write to /var/lock so it
doesn't get in a fight with other applications talking on the serial
port. supposedly, adding yourself to one of these groups will work
(didn't for me, but i'm a little clueless) or running processing as
root will get rid of the errors (not a great solution). 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHAT IS SKETCHBOOK?

we think most "integrated development environments" (microsoft visual
studio, codewarrior, jbuilder) tend to be overkill for the type of
audience we're targeting with Processing. for this reason, we've
introduced the 'sketchbook' which is a more lightweight way to
organize projects. as trained designers, we'd like the process of
coding to be a lot more like sketching. the sketchbook and the
'history' menu under 'sketch', are attempts in that direction.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHY JAVA? OR WHY SUCH A JAVA-ESQUE LANGUAGE?


We didn't set out to make the ultimate language for visual
programming, we set out to make something that was:

1) a sketchbook for our own work, simplifying the majority 
   of tasks that we undertake, 
2) a teaching environment for that kind of process, and
3) a point of transition to more complicated or difficult 
   languages like full-blown Java or C++. (a gateway drug)

At the intersection of these points is a tradeoff between speed and
simplicity of use. i.e. if we didnt' care about speed, python or other
scripting languages would make far more sense. if we didn't care about
transition to more advanced languages, we'd get rid of the crummy
c-style (well, algol, really) syntax. etc etc.

Processing is not intended as the ultimate environment/language (in
fact, the language is just Java, but with another graphics api and
some simplifications), it's just putting together several years of
experience in building things, and trying to simplify the parts that
should be easier. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


EXTERNAL FILES / FONTS / READING DATA FILES


if you want to use external files, like images or text files 
or fonts, they should be placed in a folder called 'data' inside:
sketchbook -> default -> SKETCH_NAME

starting with version 44, there are several functions that make
dealing with data in files much easier (loadFile, loadStrings,
splitStrings, etc) so file i/o should be fun!


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


WHY IS IT CALLED "PROCESSING"?


at their core, computers are processing machines. they modify, move,
and combine symbols at a low level to construct higher level
representations. Processing allows people to control these actions and
representations through writing their own programs. 

the project also focuses on the "process" of creation rather than end
results. the design of the software supports and encourages sketching
and the website presents fragments of projects and exposes the
concepts behind finished software.

"Proce55ing" is the spelling we use for the url (processing.net being
unavailable) and while it's a combination of numbers and letters but
is simply pronounced "processing." you also might see "p5" used as a
shortened version of the name. 


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


PROCESSING IS FREE TO DOWNLOAD / FREE TO USE

we think it's important to have Processing freely available, rather
than selling it for a million dollars under some godawful yearly
contract update scheme. to that end, we encourage people to distribute
the word widely and refer them to the site: http://Proce55ing.net

on most of our own projects, we usually list them as "Built with
Processing" or something similar, with a link back to the site. of
course this isn't a necessity, but it makes us happy when you do.


. . . . . . . . . . . . . . . . . . . . . . . . . . . . . 


SOURCE CODE / OPEN SOURCE / GPL BLAH BLAH

we plan for this project to be "open source", everyone's favorite
phrase that means that you'll be able to get your grubby little mitts
all over our code (all the code that's behind the processing
development environment and the graphics engine used in tandem with
it). we can't promise, since we're still working on getting the
licensing taken care of with our employers, but we think this should
likely happen soon.

the export libraries (internally known as 'bagel') will probably be
LGPL, which means they can be used as a library and included in your
project without you having to open up your code (though we encourage
people to share anyway). 

more information about the gnu public license can be found here:
http://www.gnu.org/copyleft/gpl.html

processing also includes other open projects, namely the oro matcher, 
the kjc compiler, and the jedit syntax package. the oro tools are
distributed under a bsd style license as part of the apache jakarta
project, and the kjc compiler is part of the kopi suite of tools,
which is released under the gpl. so in fact, if the final, publicly
available version of processing still uses kjc, the code for
processing will have to be released gpl. more about the oro tools is
at: http://www.savarese.org/oro/ and the home for kopi/kjc is here:
http://www.dms.at/kopi/

kjc is being phased out in favor of the jikes compiler from ibm:
http://oss.software.ibm.com/developerworks/opensource/jikes/
which is covered by the ibm public license.

we're sorry that our source code isn't available just yet, we're
cleaning and scrubbing it, it was a decision between getting the alpha
out to people to try versus taking a few more weeks to clean up the
project and deal with the technology licensing departments at mit and
ivrea. these things are far more difficult and time consuming than
they would appear.

our plan is to have the code available with the first "beta" release,
which will be the first release that is publicly available and
downloadable from the site.
