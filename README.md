CoMingle
========

CoMingle: Distributed Logic Programming for Decentralized Mobile Ensembles

By Edmund S. L. Lam (sllam@qatar.cmu.edu) and Iliano Cervesato (iliano@cmu.edu), with
Ali Elgazar (aee@cmu.edu)

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation).

Alumni Developers:

* Zeeshan Hanif
* Nabeeha Fatima

News
====

* New paper to appear in preceedings of the 11th IEEE International Conference on Wireless and Mobile Computing, Network and Communications (Wimob2015)

* Added NFC sensing libraries.

* Added Mafia party game prototype (It currently looks a little ugly, but it works! =P Stay tuned for updates!). 

* Source codes for CoMingle Android libraries added!

* New CoMingle runtime now works with Wifi-Direct as well as Local-Area-Networks! 

* To appear in proceeding of the 10th International Federated Conference on Distributed Computing Techniques, Coordination'2015
(http://discotec2015.inria.fr/francais-coordination-2015-accepted-papers/).

What is CoMingle?
=================

CoMingle is a logic programming framework aimed at simplifying the development of applications distributed over
multiple mobile devices. Applications are written as a single declarative program (in a system-centric way) rather
than in the traditional node-centric manner, where separate communicating code is written for each participating node.
CoMingle is based on committed-choice multiset rewriting and is founded on linear logic.

CoMingle is greatly influenced by Linear Meld, a distributed logic programming for distributed graph algorithms. 
It is also a descendant of CHR (Constraint Handling Rules), a constraint programming language targeting traditional 
constraint solving problems. CoMingle extends CHR with multiset comprehension, explicit locations, and
mechanisms (known as triggers and actuators) that allows its decentralized multiset rewriting semantics to
interact with an Android application.

For details, please check out our technical report on Comingle: 
http://reports-archive.adm.cs.cmu.edu/anon/2015/CMU-CS-15-101.pdf

Contents
========

This repository contains a working copy of the Comingle compiler and runtime system, as well as some examples
to get you started Comingling. Here's a summary of what it contains:

   - comingle_code_generator: the source codes of the compiler
   - comingle_runtime: .jar libraries and source codes of the Comingle runtime
   - examples: basic examples of Comingle programs
   - android_apps: small repository of android programs (eclipse project + .apk), orchestrated by Comingle.
   - papers: a repository of CoMingle research papers and technical reports

To install and use Comingle, please follow the 'Getting Started' instructions below. If you want to go
straight into playing the Android Apps, follow the instructions in the Readme file at 
https://github.com/sllam/comingle/tree/master/android_apps .

Basic Requirements
==================

To run the CoMingle compiler and code generator, you will need:
   - Python 2.7 compiler and runtime
   - ply (Python Lex-Yacc http://www.dabeaz.com/ply/)
   - z3Py: Python APIs for z3 SMT Solver (Check https://z3.codeplex.com/ for installation details)
   - pysetcomp: Set Comprehension Extension for z3 in Python (Download at https://github.com/sllam/pysetcomp)
   - msre: Decentralized Multiset Rewriting for Ensembles (Compiler only, Download at https://github.com/sllam/msre)

To run the CoMingle generated codes, you will need:
   - Java Development Kit (JDK) 7 
   - Android SDK 

Getting Started
===============

To get Comingle's compiler working, you will first need to have Python 2.7 running. Next, you'll need a couple of
non-standard Python modules, ply and z3Py. Please proceed to http://www.dabeaz.com/ply/ and https://z3.codeplex.com/
to get the respective sources and install instructions. 

Once you have them, proceed to https://github.com/sllam/pysetcomp , and install:

> pysetcomp$ sudo python setup.py install

Next you'll need MSRE's compiler, proceed to https://github.com/sllam/msre, and install ONLY its compiler:

> msre$ cd compiler/msrex

> msre/compiler/msrex$ sudo python setup.py install

You are almost set, you should now be able to setup and install the Comingle compiler, do this by running the
make file:

> comingle$ sudo make install

This would do the following:
   - Installs another Python package 'comingle' into your Python distribution.
   - Copies executable script 'cmgc' to your /usr/local/bin
   - Copies Comingle library files to your /usr/local/lib
Feel free to customize the make file to adjust the installation paths.

To test Comingle, go to the example directory and try it out:

> comingle$ cd examples

> comingle/examples$ cmgc dragracing.cmg

You'll see a bunch of output, and hopefully, no error messages. If it has ran successfully, you should
see a directory 'dragracing' with a Java source file 'Dragracing.java'. This Java class implements
the Comingle runtime specified by the Comingle program 'dragracing.cmg'. See the eclipse project
in https://github.com/sllam/comingle/tree/master/android_apps/CoMingleDragRacing for an example of
how this CoMingle runtime can be intergrated with an actual Android game.

If you encountered errors in any of the above steps... please contact me (Edmund) at sllam@qatar.cmu.edu .
Also please accept my apologies, the Comingle prototype is still work in progress, but I'm working hard
to bring it to a stable and more usable state! =)

Acknowledgements
================

Special thanks to the following:

  - Nabeeha Fatima, for contributing in the development of the UI frontend of Android games, powered by 
    CoMingle.
  - Iliano Cervesato, for advise and technical discussions on the logic programming. They helped
    make CoMingle possible.
  - Francesco Azzola, for helpful tips on android list views, at
    http://www.javacodegeeks.com/2013/06/android-listview-tutorial-and-basic-example.html
  - Nithya Vasudevan, for helpful tips on android expandable list views, at
    http://theopentutorials.com/tutorials/android/listview/android-expandable-list-view-example/
  - People who made Chrome Racer (http://www.chrome.com/racer). It inspired our demo app Drag-Racing.
    But of course, Drag-Racing is no where as sleek as Chrome Racer.


