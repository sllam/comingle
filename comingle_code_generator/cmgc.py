'''
This file is part of CoMingle.

CoMingle is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

CoMingle is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with CoMingle. If not, see <http://www.gnu.org/licenses/>.

CoMingle Version 0.8, Prototype Alpha

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
'''

from msrex.frontend.process import process_msre
from msrex.misc.msr_logging import init_logger, log_info

import msrex.frontend.lex_parse.ast as ast
from msrex.frontend.builtin.predicates import BuiltinPred

from comingle.code_generator import JavaCodeGenerator

from string import split
from argparse import ArgumentParser

import shlex
import subprocess

comingle_builtin_preds = [BuiltinPred("delay",[ast.TypeCons(ast.INT)],act_name="delay",role=ast.ACTUATOR_FACT)
                         ,BuiltinPred("beep",[ast.TypeCons(ast.STRING)],act_name="beep",role=ast.ACTUATOR_FACT)
                         ,BuiltinPred("toast",[ast.TypeCons(ast.STRING)],act_name="toast",role=ast.ACTUATOR_FACT)]

arg_parser = ArgumentParser(prog='comingle.py')
arg_parser.add_argument('filename')
# arg_parser.add_argument('-o', dest="output")
args = arg_parser.parse_args()

output = process_msre(args.filename, builtin_preds=comingle_builtin_preds)

PYTHON_CC_PATH   = ""
COMINGLE_PY_PATH = ""
JAVAC_PATH       = ""
JAR_PATH         = ""
BIN_PATH         = "."
TEMP_PATH        = "/tmp"
DX_PATH          = ""
CURR_PATH        = ".."

BUILD_JAR = False

if output["valid"]:
	prog = output["prog"]
	print prog.fact_dir
	print prog

	print "Generating Java Codes..."
	javaGen = JavaCodeGenerator(prog, prog.fact_dir, False, builtin_preds=comingle_builtin_preds)
	main_prog,extern_mods = javaGen.generate()

	'''
	for extern_mod in extern_mods:
		if len(extern_mod['DirNames']) > 0:
			extern_ref = "%s" % extern_mod['DirNames'][0]
		else:
			extern_ref = "%s.java" % extern_mod['ClassName']
		print "Moving \'%s\' package to bin path" % extern_ref
		cmd2 = "cp -r %s/%s %s/." % (CURR_PATH,extern_ref,BIN_PATH) 
		subprocess.Popen(shlex.split(cmd2))

	for extern_mod in extern_mods:
		if len(extern_mod['DirNames']) > 0:
			extern_java = "%s/%s.java" % ('/'.join(extern_mod['DirNames']),extern_mod['ClassName'])
		else:
			extern_java = "%s.java" % extern_mod['ClassName']
		print "Compiling %s" % extern_java
		cmd3 = "javac -cp comingle.jar %s" % extern_java
		subprocess.call(shlex.split(cmd3))
	'''

	'''
	print "Compiling %s" % main_prog['DirNames'][0]
	cmd5 = "javac -cp comingle.jar:. %s/%s.java" % (main_prog['DirNames'][0],main_prog['ClassName'])
	subprocess.call(shlex.split(cmd5))

	includes = []
	for extern_mod in extern_mods:
		if len(extern_mod['DirNames']) > 0:
			includes.append( "%s" % (extern_mod['DirNames'][0]) )
		else:
			includes.append( "%s.class" % extern_mod['ClassName'] )

	print "Jar-ing %s" % main_prog['DirNames'][0]
	cmd6 = "jar cf %s_undex.jar %s %s" % (main_prog['DirNames'][0],main_prog['DirNames'][0]," ".join(includes))
	subprocess.call(shlex.split(cmd6))

	print "Cleaning bin path"
	cmd9 = "rm -r %s" % main_prog['DirNames'][0]
	subprocess.call(shlex.split(cmd9))
	for extern_mod in extern_mods:
		cmd10 = "rm -r %s" % extern_mod['DirNames'][0]
		subprocess.call(shlex.split(cmd10))
		# cmd11 = "rm %s.jar" % extern_mod['DirNames'][0]
		# subprocess.call(shlex.split(cmd11))
	'''
else:
	for report in output['error_reports']:
		print "\n"
		print report
		print "\n"

# mpiCC mergesort.cpp -lboost_mpi -lboost_serialization -o mergesort


