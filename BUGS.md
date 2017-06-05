Priority | Description
---------|------------
Very High - Dangerous!!! | PApplet allows *anything* to be run on the command line via the PApplet.launch(String...) or PApplet.exec(String... args) methods. Both methods should be replaced asap with a call to Desktop.getDesktop().open(file) which accomplishes the same thing without exposing the user to command injection. Not going to post any code here, but it would be an easy matter to create a processing project that destroys a computer when it's run.
