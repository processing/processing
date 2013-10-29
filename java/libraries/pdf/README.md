This library uses iText 2.1.7, which is the last LGPL/MPL version of the iText project. 

We've used iText for several years. The license for iText has changed for subsequent versions and is no longer compatible with Processing, so we're stuck at 2.x. 

At the iText site, there's also some [vague wording](http://lowagie.com/itext2) about legal liability for commercial projects using the 2.x series. It's not clear where this leaves us.

Bruno Lowagie did an enormous amount of (free) work with the iText project, and we certainly don't fault him for the new commercial license. 

We're using iText in a very limited way--drawing to it like it's a Java Graphics2D object. There might be other options for us in this space, but it's not much of a priority.

Ben Fry, 12 October 2013
