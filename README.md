Processing
==========

This is the official source code for the [Processing](http://processing.org) Development Environment (PDE), 
the “core” and the libraries that are included with the [download](http://processing.org/download). 

> Development of Processing 3 has started, so major changes are underway inside this repository. **If you need a stable version of the source, use the tag processing-0227-2.2.1.** Do not expect this code to be stable. Major changes include severe things like breaking libraries (due to chaining operations in PVector) or the removal of `Applet` as the base class for PApplet. Some of these will be sorted out before the release, others are simply being tested or are developments that are in-progress.
> Update 8 September 2014: Applet is being removed as the base class. You can use [the 3.0a3 tag](https://github.com/processing/processing/releases/tag/processing-0230-3.0a3) if you'd like the last “stable” alpha release.

If you have found a bug in the Processing software, you can file it here under the [“issues” tab](https://github.com/processing/processing/issues). 
If it relates to the [JavaScript](http://processingjs.org) version, please use [their issue tracker](https://processing-js.lighthouseapp.com/).
All Android-related development has moved to its own repository [here](https://github.com/processing/processing-android), 
so issues with Android Mode, or with the Android version of the core library should be posted there instead.

The issues list has been imported from Google Code, so there are many spurious references 
amongst them since the numbering changed. Basically, any time you see references to 
changes made by [processing-bugs](https://github.com/processing-bugs), it may be somewhat suspect.
Over time this will clean itself up as bugs are fixed and new issues are added from within Github.
Help speed this process along by helping us!

The [processing-docs](https://github.com/processing/processing-docs/) repository contains reference, examples, and the site. 
(Please use that link to file issues regarding the web site, the examples, or the reference.)

The instructions for building the source [are here](https://github.com/processing/processing/wiki/Build-Instructions).

Someday we'll also write code style guidelines, fix all these bugs, 
throw together hundreds of unit tests, 
and get rich off all this stuff that we're giving away for free.

But in the meantime, I ask for your patience, 
[participation](https://github.com/processing/processing/wiki/Project-List), 
and [patches](https://github.com/processing/processing/pulls).

Ben Fry, 3 February 2013  
Last updated 30 July 2014
