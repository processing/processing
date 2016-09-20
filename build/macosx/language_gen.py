#!/usr/bin/env python

import os, re

BASEDIR = os.path.dirname(os.path.realpath(__file__))

def supported_languages():
    path = "../../build/shared/lib/languages/languages.txt"
    with open(os.path.join(BASEDIR, path)) as f:
        lines = f.read().splitlines()

    lines = filter(lambda l: re.match(r'^[a-z]{2}', l), lines)
    lines = map(lambda l: re.sub(r'#.*', '', l).strip(), lines)
    return lines

def lproj_directory(lang):
    path = "work/Processing.app/Contents/Resources/{}.lproj".format(lang)
    return os.path.join(BASEDIR, path)


if __name__ == "__main__":
    for lang in supported_languages():
        try:
            os.mkdir(lproj_directory(lang))
        except OSError:
            pass
