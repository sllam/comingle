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
Nabeeha Fatima        nhaque@andrew.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
'''

import os

from string import split

import msrex.frontend.lex_parse.ast as ast
import msrex.frontend.lex_parse.parser as p

from msrex.frontend.analyze.inspectors import Inspector

import msrex.frontend.compile.join_ordering as join

from msrex.frontend.compile.lookup_context import HASH_LK, MEM_LK, ORD_LK, LOC_HASH_LK, LINEAR_LK, LookupTables
from msrex.frontend.compile.prog_compilation import ProgCompilation

import msrex.misc.visit as visit
from msrex.misc.template import compile_template, template, compact

BASE_IMPORT_LIST_WITH_LOG = template('''
import java.io.*;
import java.util.logging.*;

import java.util.LinkedList;

import comingle.goals.*;
import comingle.store.*;
import comingle.facts.*;
import comingle.rewrite.*;
import comingle.hash.*;
import comingle.tuple.*;
import comingle.misc.*;
import comingle.logging.CoLogger;
''')

BASE_IMPORT_LIST = template('''
import java.util.LinkedList;

import comingle.goals.*;
import comingle.store.*;
import comingle.facts.*;
import comingle.rewrite.*;
import comingle.hash.*;
import comingle.tuple.*;
import comingle.misc.*;
''')

BOILER_PLATE_CODES = template('''
	@Override
	protected boolean rewrite() {
		boolean done_something = false;
		while(goals.has_goals()) {
			goals.next().execute( this );
			done_something = true;
		}
		return done_something;
	}

	@Override
	protected boolean rewrite(int max_steps) {
		int count = 0;
		while(goals.has_goals() && count < max_steps) {
			goals.next().execute( this );
			count++;
		}
		return count > 0;
	}
''')

PRIME_NUMBERS = [7919,13259,31547,53173,72577,91099,103421,224737,350377,499979]

def mk_package_name( name ):
	package_name = ""
	for frag in split(name,'_'):
		package_name += frag
	return package_name

def mk_ensem_name( name ):
	ensem_name = ""
	for frag in split(name,'_'):
		ensem_name += frag[0].upper() + frag[1:]
	return ensem_name

def mk_pred_name( name ):
	pred_name = ""
	for frag in split(name,'_'):
		pred_name += frag[0].upper() + frag[1:]
	return pred_name

def mk_cpp_var_name( name ):
	return name.lower()

def mk_join_task_source_text( join_task ):
	return "// Join Task: %s" % join_task

def all_pat_vars(join_head_dict):
	pat_vars = set([])
	for idx in join_head_dict:
		pat_vars |= set(join_head_dict[idx]['pat_vars'])
	return pat_vars

def local_loc_var(join_head_dict):
	join_head_info = join_head_dict[0]
	return join_head_info['pat_vars'][0]

def req_collision_check(idx_var_eq):
	return len(idx_var_eq) > 1

class JavaCodeGenerator:

	def __init__(self, prog, fact_dir, incl_logs=True):
		self.prog = prog
		self.rule_names = prog.getRuleNames()
		self.fact_dir = fact_dir
		self.lookup_tables = prog.lookup_tables
		self.init_fact_dict()
		self.init_store_dict()
		self.join_ordering_dict = {}
		self.var_idx = 0
		self.tup_idx = 0
		self.incl_logs = incl_logs

	def next_temp_var_name(self):
		var_name = "temp%s" % self.var_idx
		self.var_idx += 1
		return var_name;

	def init_fact_dict(self):
		self.fact_dict = {}
		java_type_coerce = JavaTypeCoercion()
		for fact_idx,fact_dec in self.fact_dir.fact_idx_dict.items():
			arg_names = []
			for i in range(1,len(fact_dec.arg_types())+1):
				arg_names.append( "arg%s" % i )
			info = { 'fact_idx'   : fact_idx
                               , 'fact_name'  : mk_pred_name( fact_dec.name )
                               , 'var_name'   : mk_cpp_var_name( fact_dec.name )
                               , 'loc_name'   : 'loc'
                               , 'loc_type'   : 'int'
                               , 'arg_names'  : arg_names
                               , 'types'      : fact_dec.arg_types()
                               , 'type_codes' : map(lambda arg_type : java_type_coerce.coerce_type_codes(arg_type), fact_dec.arg_types())
                               , 'pretty_codes' : ["%s"] + map(lambda arg_type : java_type_coerce.coerce_pretty_codes(arg_type), fact_dec.arg_types())
                               , 'persistent' : fact_dec.persistent
                               , 'local'      : fact_dec.local
                               , 'monotone'   : fact_dec.monotone
                               , 'uses_priority' : fact_dec.uses_priority }
			self.fact_dict[fact_idx] = info
			# print "%s:%s:%s" % (fact_dec.name, map(str,fact_dec.arg_types()), info['pretty_codes'])

	def init_store_dict(self):
		self.store_dict = {}
		for fact_idx,fact_stores in self.lookup_tables.lookup_tables.items():
			fact_info = self.fact_dict[fact_idx]
			fact_name = fact_info['fact_name']
			for fact_store in fact_stores:
				store_idx = fact_store.lookup_idx
				store_name = "%s_store_%s" % ( mk_cpp_var_name(fact_name) ,store_idx)
				# print "Ho ho ho: %s" % fact_store.type
				if fact_store.type == LINEAR_LK:
					store_type = "ListStore<%s>" % fact_name
					iter_type  = "ListStoreIter<%s>" % fact_name
					has_index  = False
				elif fact_store.type == HASH_LK or fact_store.type == LOC_HASH_LK:
					store_type = "MultiMapStore<%s>" % fact_name
					iter_type  = "ListStoreIter<%s>" % fact_name
					has_index  = True
				else:
					print "Hahah"
					# TODO
					pass
				fact_arg_names  = [fact_info['loc_name']] + fact_info['arg_names']
				fact_type_codes = [fact_info['loc_type']] + fact_info['type_codes']
				idx_func_name = "index%s%s" % (store_idx,fact_name)

				idx_codes = []
				for idx in fact_store.lookupArgIndices():
					idx_codes.append( { 'idx':idx, 'arg_name':fact_arg_names[idx], 'type_code':fact_type_codes[idx] } )			
	
				store_info = { 'name':store_name, 'type':store_type, 'iter':iter_type, 'idx_func':idx_func_name, 'sort':fact_store.type
		                             , 'idx':idx_codes, 'collision_free':len(idx_codes)<=1, 'has_index':has_index, 'store_idx':store_idx }

				if fact_idx in self.store_dict:
					self.store_dict[fact_idx].append( store_info )
				else:
					self.store_dict[fact_idx] = [store_info]

	def generate(self):
		prog        = self.prog
		prog_name   = self.prog.prog_name
		extern_decs = self.prog.extern_decs
		# specs = self.msre_specs['specs']
		exec_decs   = [self.prog.exec_dec]

		extern_import_list = map(lambda e: self.generate_extern_import( e ), extern_decs)

		use_ordered_goals = prog.fact_dir.uses_priority()

		main_codes = ""
		if len(exec_decs) > 0:
			exec_call_codes = []
			for idx in range(0,len(exec_decs)):
				exec_call_codes.append( "execute_%s();" % idx )

			if self.incl_logs:
				init_log_codes = "CoLogger.addHandler(\"output.log\");"
				throw_io_codes = "throws IOException"
			else:
				init_log_codes = ""
				throw_io_codes = ""

			main_codes = compile_template( template('''
			public static void main(String[] args) {| throw_io_codes |} { 
				{| init_log_codes |}
				{| '\\n'.join( exec_call_codes ) |}
			}
			'''), throw_io_codes=throw_io_codes, init_log_codes=init_log_codes, exec_call_codes=exec_call_codes )

		exec_codes = []
		count = 0
		for exec_dec in exec_decs:
			exec_codes.append( self.generate_exec(exec_dec, count) )
			count += 1

		ensem_classes = map(lambda p: self.generate_ensemble( p, main_codes, exec_codes ), [prog])

		if self.incl_logs:
			base_import_list = BASE_IMPORT_LIST_WITH_LOG
		else:
			base_import_list = BASE_IMPORT_LIST

		package_code = "package %s;" % mk_package_name( prog_name )

		ensem_code = template('''
			{| package_code |}

			{| base_import_list |}

			{| '\\n'.join( extern_import_list ) |}

			{| '\\n'.join( ensem_classes ) |}

		''')		
		
		package_dir = mk_package_name( prog_name )

		file_name = "%s/%s.java" % (package_dir,mk_ensem_name( prog_name ))

		if not os.path.exists(package_dir):
			os.makedirs(package_dir)

		output = open(file_name, 'w')
		output.write( compile_template(ensem_code, package_code=package_code, base_import_list=base_import_list
                                              ,extern_import_list=extern_import_list, ensem_classes=ensem_classes) )

	def generate_extern_import(self, extern_dec):
		extern_import_codes = template('''
			import {| extern_module_name |};
		''')
		return compile_template( extern_import_codes, extern_module_name=extern_dec.name )

	def generate_fact_atom(self, fact_idx, loc_fact):
		fact_name = self.fact_dict[fact_idx]['fact_name']

		fact_args = []
		arg_contexts = []
		for fact_arg in [loc_fact.loc] + loc_fact.fact.terms:
			context_codes,f_arg_codes = self.generate_term( fact_arg )
			fact_args.append( f_arg_codes )
			arg_contexts += context_codes

		if loc_fact.priority != None:
			fact_args.append( str(loc_fact.priority) )

		return arg_contexts,fact_args

	def generate_exec(self, exec_dec, idx):

		ensem_name = mk_ensem_name( exec_dec.name )
		inspect = Inspector()
		java_type_coerce = JavaTypeCoercion()
		exist_decs    = inspect.filter_decs(exec_dec.decs, exist=True)
		loc_fact_decs = inspect.filter_decs(exec_dec.decs, loc_facts=True)

		# Generate exist codes
		exist_codes = []
		loc_count = 0
		exist_loc_names = []
		for exist_dec in exist_decs:
			for exist_var in exist_dec.exist_vars:
				exist_type = java_type_coerce.coerce_type_codes( exist_var.type )
				exist_name = mk_cpp_var_name( exist_var.name )
				exist_value = ""
				if exist_var.type.name == ast.LOC:
					exist_value = "%s" % loc_count
					loc_count += 1
					exist_loc_names.append( exist_name )
				elif exist_var.type.name == ast.DEST:
					exist_value = "next_exist_id(-1)"
				exist_codes.append( "%s %s = %s;" % (exist_type,exist_name,exist_value) )

		# Generate fact init codes
		init_fact_codes = []
		compre_idx = 0
		for loc_fact_dec in loc_fact_decs:
			for loc_fact in loc_fact_dec.loc_facts:
				if loc_fact.fact_type == ast.FACT_LOC:
					fact_idx,_ = self.fact_dir.getFactFromName( loc_fact.fact.name )
					arg_contexts,fact_args = self.generate_fact_atom(fact_idx, loc_fact)
					fact_name = self.fact_dict[fact_idx]['var_name']						
					init_fact_codes += arg_contexts
					init_fact_codes.append( "add_%s(%s);" % (fact_name,','.join(fact_args)) )
				else: # body_fact['type'] == FACT_COMPRE
					term_pat  = fact['term_pat']
					term_subj = fact['term_subj']
					facts     = fact['facts']

					subj_type    = java_type_coerce.coerce_type_codes( term_subj.type )
					subj_varname = "comp_%s" % compre_idx
					subj_context_codes,subj_exp   = self.generate_term( term_subj )
					term_pat_context,term_pat_var,term_pat_post = self.generate_left_pattern(term_pat, term_pat.type)

					fact_atom_codes = []
					for f in facts:
						arg_contexts,fact_args = self.generate_fact_atom(f)
						fact_atom_codes += arg_contexts
						fact_atom_codes.append( "add_%s(%s);" % (fact_name,','.join(fact_args)) )

					compre_codes = template('''
					{| '\\n'.join( subj_context_codes ) |}
					{| subj_type |} {| subj_varname |} = {| subj_exp |};
					for(int idx = 0; idx < {| subj_varname |}.size(); idx++) {
						{| term_pat_context |}
						{| term_pat_var |} = {| subj_varname |}.get(idx);
						{| term_pat_post |}
						{| '\\n'.join( fact_atom_codes ) |}
					}
					''')
					init_fact_codes.append( compile_template( compre_codes, subj_context_codes=subj_context_codes
                                                              , subj_type=subj_type, subj_varname=subj_varname, subj_exp=subj_exp
                                                              , term_pat_context=term_pat_context, term_pat_var=term_pat_var
                                                              , fact_atom_codes=fact_atom_codes, term_pat_post=term_pat_post ) )
					

		exec_codes = template('''
			@Override
			public void init() {
				{| '\\n'.join( exist_codes ) |}
				{| '\\n'.join( init_fact_codes ) |}
			}
		''')

		return compact( compile_template( exec_codes, idx=idx, ensem_name=ensem_name, exist_codes=exist_codes
                                                , init_fact_codes=init_fact_codes ) )

	def generate_ensemble(self, prog, main_codes, exec_codes):

		ensem_name = mk_ensem_name( prog.ensem_name )

		self.ensem_fact_name = "%sFact" % ensem_name

		# self.ensem_info[ensem_spec['ensem_name']] = self.fact_decs

		# Generate codes

		extern_imports = "" # map(lambda e: self.generate_extern_import( e ), ensem_spec['extern_specs'])

		fact_dec_codes = map(lambda fact_idx: self.generate_fact_decs(ensem_name, fact_idx), self.fact_dict)

		index_dec_codes = self.generate_store_index_decs(ensem_name)
		const_pred_id_decs = map(lambda (fact_idx,fact_info): "protected static final int %s_fact_idx = %s;" % (fact_info['var_name'],fact_idx) 
                                        ,self.fact_dict.items() )
		#fact_comm_decs = map(lambda (fact_idx,fact_info): "MSRE_MPICOMM_INSTANCE<%s> %s_comm;" % (fact_info['fact_name'],fact_info['var_name'])
                #                   ,filter(lambda (fact_idx,fact_info): not fact_info['local'], self.fact_dict.items()) )
		rule_app_counter_decs = map(lambda rule_name: "protected int %s_rule_count;" % rule_name, self.rule_names)
		store_dec_codes = []
		for (fact_idx,store_infos) in self.store_dict.items():
			for store_info in store_infos:
				store_dec_codes.append( "%s %s;" % (store_info['type'],store_info['name']) )
		constructor_codes    = self.generate_constructor( ensem_name )
		fact_member_codes    = self.generate_fact_members( ensem_name )
		receive_member_codes = self.generate_receive_member( ensem_name )

		join_exec_member_codes = []
		for fact_idx,join_orderings in self.prog.pred_rule_compilations.items():
			for i in range(0,len(join_orderings)):
				join_ordering = join_orderings[i]
				join_exec_member_codes.append( self.generate_join_ordering_member( ensem_name, join_ordering, i+1 ) )

		fact_exec_member_codes = map(lambda fact_idx: self.generate_fact_member(fact_idx), self.fact_dict.keys())

		package_code = "package %s;" % mk_package_name( prog.ensem_name )

		spec_code = template('''
			{| '\\n'.join( extern_imports ) |}

			public class {| ensem_name |} extends RewriteMachine {

				abstract class {| ef_name |} extends Fact {
					public {| ef_name |}(int l) { super(l); }
					public void execute({| ensem_name |} ensem) { }
				}

				{| '\\n'.join( fact_dec_codes ) |}

				{| index_dec_codes |}

				{| '\\n'.join( const_pred_id_decs ) |}

				protected Goals<{| ef_name |}> goals;

				{| '\\n'.join( store_dec_codes ) |}

				{| '\\n'.join( rule_app_counter_decs ) |}
				protected int rule_app_misses;

				{| constructor_codes |}

				{| boiler_plate_codes |}

				{| fact_member_codes |}

				{| '\\n'.join( join_exec_member_codes ) |}

				{| '\\n'.join( fact_exec_member_codes ) |}

				{| '\\n'.join( exec_codes ) |}

			}
		''')
		
		return compile_template( spec_code, package_code=package_code, ensem_name=ensem_name, ef_name=self.ensem_fact_name, extern_imports=extern_imports
                                       , fact_dec_codes=fact_dec_codes, index_dec_codes=index_dec_codes, const_pred_id_decs=const_pred_id_decs
                                       , rule_app_counter_decs=rule_app_counter_decs, store_dec_codes=store_dec_codes, constructor_codes=constructor_codes
                                       , boiler_plate_codes=BOILER_PLATE_CODES, fact_member_codes=fact_member_codes
                                       , join_exec_member_codes=join_exec_member_codes, fact_exec_member_codes=fact_exec_member_codes
                                       , exec_codes=exec_codes)

	def generate_fact_decs(self, ensem_name, fact_idx):

		fact_info = self.fact_dict[fact_idx]
		fact_name = fact_info['fact_name']
		arg_types = fact_info['types']
		num_of_args = len( arg_types )

		arg_dec_codes = [] # [ "public %s %s;" % (fact_info['loc_type'],fact_info['loc_name']) ]
		for (arg_name,type_code) in zip(fact_info['arg_names'],fact_info['type_codes']):
			arg_dec_codes.append( "public %s %s;" % (type_code,arg_name) )	

		constructor_args = [ "%s %s" % (fact_info['loc_type'],'l')  ]
		init_codes = [ "super(%s);" % 'l' ]
		for (arg_name,type_code,i) in zip(fact_info['arg_names'],fact_info['type_codes'],range(1,num_of_args+1)):
			constructor_args.append( "%s a%s" % (type_code,i) )
			init_codes.append( "%s=a%s;" % (arg_name,i) )

		type_codes     = [ fact_info['loc_type'] ] + fact_info['type_codes']
		arg_name_codes = [ fact_info['loc_name'] ] + fact_info['arg_names']

		str_subs = [ "%s" for i in range(num_of_args) ]

		arg_name_pretty_codes = map(lambda (pretty,name): pretty % name, zip(fact_info['pretty_codes'],arg_name_codes))

		# TODO: TDMONO

		to_string_codes = "String.format(\"[%s]%s(%s)\",%s);" % ("%s",fact_name,','.join(str_subs),','.join(arg_name_pretty_codes))

		fact_dec_code = template('''
			class {| fact_name |} extends {| ef_name |} {
				{| '\\n'.join( arg_dec_codes ) |}

				public {| fact_name |}({| ', '.join( constructor_args ) |}) { {| ' '.join( init_codes ) |} }
	
				public int fact_idx() { return {| fact_idx |}; }

				@Override
				public void execute({| ensem_name |} ensem) { ensem.execute( this ); }

				public String toString() { return {| to_string_codes |} }
			}
		''')

		return compile_template(fact_dec_code, fact_name=fact_name, ensem_name=ensem_name, arg_dec_codes=arg_dec_codes
                                       ,ef_name = self.ensem_fact_name, constructor_args=constructor_args, fact_idx=fact_idx
                                       ,init_codes=init_codes, type_codes=type_codes, arg_name_codes=arg_name_codes, to_string_codes=to_string_codes)

	def generate_store_index_decs(self, ensem_name):
		
		index_func_codes = []
		for fact_idx,store_infos in self.store_dict.items():
			fact_info = self.fact_dict[fact_idx]
			for idx in range(1,len(store_infos)+1):
				store_info = store_infos[idx-1]
				if store_info['sort'] == HASH_LK or store_info['sort'] == LOC_HASH_LK:
					if len( store_info['idx'] ) > 1:
						hash_values = map(lambda i: "Hash.hash(%s)" % i['arg_name'] , store_info['idx'])
						hash_codes  = "Hash.join(%s)" % (','.join(hash_values))
					else:
						hash_codes  = "Hash.hash(%s)" % store_info['idx'][0]['arg_name'] 
					index_func_code = template('''
						protected static int {| idx_func_name |}({| ', '.join( constr_args ) |}) {
							return {| hash_codes |};
						}
					''')
					constr_args = map(lambda i: "%s %s" % (i['type_code'],i['arg_name']) ,store_info['idx'])
					index_func_codes.append(
						compile_template(index_func_code, idx_func_name=store_info['idx_func'], constr_args=constr_args
                                                                ,hash_codes=hash_codes)
					)
		store_codes = template('''
			{| '\\n'.join( index_func_codes ) |}
		''')

		return compact( compile_template(store_codes, index_func_codes=index_func_codes) )

	def generate_constructor(self, ensem_name):
				
		rule_app_counter_codes = map(lambda rule_name: "%s_rule_count = 0;" % rule_name, self.rule_names)

		store_init_codes   = []
		store_pretty_codes = []
		for fact_idx in self.store_dict:
			fact_name = self.fact_dict[fact_idx]['fact_name']
			for store_info in self.store_dict[fact_idx]:
				store_init_code = template('''
					{| store_name |} = new {| store_type |}();
					{| store_name |}.set_name(\"{| fact_name |} Store\");
					// set_store_component( {| store_name |} );
				''')
				store_init_codes.append( compile_template(store_init_code, store_name=store_info['name'], fact_name=fact_name
                                                                         ,store_type=store_info['type']) )
				# store_logger_codes.append( "set_logger_child( &%s,\"%s Store\" );" % (store_info['name'], fact_name) )

			table_idx = self.lookup_tables.linear_lookup_index(fact_idx)
			store_pretty_codes.append( "set_store_component( %s );" % self.store_dict[fact_idx][table_idx]['name'] )

		constructor_code = template('''
			public {| ensem_name |}() {
				super();

				{| '\\n'.join( rule_app_counter_codes ) |}
				rule_app_misses = 0;
	
				goals = new {| goal_type |}<{| ensem_fact_name |}>();
				set_goal_component( goals );

				{| '\\n'.join( store_init_codes ) |}

				{| '\\n'.join( store_pretty_codes ) |}
			}
		''')

		goal_type = "ListGoals"

		return compile_template(constructor_code, ensem_name=ensem_name, rule_app_counter_codes=rule_app_counter_codes
                                       ,store_init_codes=store_init_codes, store_pretty_codes=store_pretty_codes
                                       ,ensem_fact_name=self.ensem_fact_name, goal_type=goal_type )

	def generate_fact_members(self, ensem_name):

		# TODO: TDMONO
	
		store_member_codes = []
		add_member_codes   = []
		send_member_codes  = []
		for fact_idx in self.fact_dict:
			fact_info = self.fact_dict[fact_idx]
			fact_name = fact_info['fact_name']
			var_name  = fact_info['var_name']
			add_to_store_codes = []
			for store_info in self.store_dict[fact_idx]:
				if store_info['has_index']:
					xs = (store_info['name'],var_name,store_info['idx_func']
                                             ,','.join(map(lambda idx: "%s.%s" % (var_name,idx['arg_name']) ,store_info['idx'])) )
					add_to_store_codes.append( "%s.add( %s, %s(%s) );" % xs )
				else:
					xs = (store_info['name'], var_name)
					add_to_store_codes.append( "%s.add( %s );" % xs )
			store_member_code = template('''
				protected void store({| fact_name |} {| var_name |}) {
					{| '\\n'.join( add_to_store_codes ) |}
				} 				
			''')			
			store_member_codes.append( compile_template(store_member_code, fact_name=fact_name, var_name=var_name
                                                                   ,add_to_store_codes=add_to_store_codes) )
			arg_decs  = map(lambda (t,n): "%s %s" % (t,n) ,zip(fact_info['type_codes'],fact_info['arg_names']))

			# monotone = "true"
			arg_calls = fact_info['arg_names'] # + [monotone]

			if fact_info['monotone']:
				add_cons_codes = compile_template(template('''
							goals.add( new {| fact_name |}(loc{| join_ext(',', arg_calls, prefix=',') |}) );
					         '''), fact_name=fact_name, arg_calls=arg_calls)
			else:
				template_args = { 'fact_name' : fact_name
                                                , 'arg_calls' : arg_calls }
				add_cons_codes = compile_template(template('''
							{| fact_name |} temp = new {| fact_name |}(loc{| join_ext(',', arg_calls, prefix=',') |});
							goals.add( temp );
							store( temp );
                                                 '''), **template_args)

			add_member_code = template('''
				public void add_{| var_name |}(int loc{| join_ext(',', arg_decs, prefix=',') |}) {
					{| add_cons_codes |}
					notify_new_goals();
				}
			''')
			add_member_codes.append( compile_template(add_member_code, var_name=var_name
                                                                 ,arg_decs=arg_decs, add_cons_codes=add_cons_codes) )
			if not self.fact_dict[fact_idx]['local']:
				# TODO: Send accounts for non-monotone constraints, but not receive! Fix soon.
				if fact_info['monotone']:
					send_member_code = template('''
						private: void send({| fact_name |} {| var_name |}) {
							int dest = lookup_dir( {| var_name |}.node_id() );
							if (dest != rank) {
								{| var_name |}_comm.send( dest, {| var_name |}); 
							} else {
								goals->add( {| var_name |}.clone() );
							}
						}
					''')
				else:
					send_member_code = template('''
						private: void send({| fact_name |} {| var_name |}) {
							int dest = lookup_dir( {| var_name |}.node_id() );
							if (dest != rank) {
								{| var_name |}_comm.send( dest, {| var_name |}); 
							} else {
								{| fact_name |}* temp = {| var_name |}.clone();
								goals->add( temp );
								store( temp );
							}
						}
					''')
				send_member_codes.append( compile_template(send_member_code, fact_name=fact_name, var_name=var_name) )

		fact_member_codes = template('''
			{| '\\n'.join( store_member_codes ) |}
			{| '\\n'.join( add_member_codes ) |}
		''')

		return compile_template( fact_member_codes, store_member_codes=store_member_codes, add_member_codes=add_member_codes)

	def generate_receive_member(self, ensem_name):
		receive_comm_codes = []
		send_codes = []
		recv_codes = []
		idx = 1
		for fact_idx in self.fact_dict:
			fact_info = self.fact_dict[fact_idx]
			var_name  = fact_info['var_name']
			fact_name = fact_info['fact_name']
			if not fact_info['local']:
				receive_comm_code = template('''
					optional<list<{| fact_name |}*> > opt_{| idx |} = {| var_name |}_comm.receive();
					if (opt_{| idx |}) {
						for (list<{| fact_name |}*>::iterator st = opt_{| idx |}->begin(); st != opt_{| idx |}->end(); st++) {
							goals->add( *st );
							received = true;
						}
					}				
				''')
				receive_comm_codes.append( compile_template(receive_comm_code, fact_name=fact_name, var_name=var_name, idx=idx) )
				idx += 1
				send_codes.append( "*(%s_comm.get_send_counter())" % var_name );
				recv_codes.append( "*(%s_comm.get_recv_counter())" % var_name );

		num_of_comm = idx - 1;

		receive_member_codes = template('''
			private: bool receive() {
				bool received = false;
				{| '\\n'.join( receive_comm_codes ) |}
				return received;
			}

			protected: bool globally_quiescence() {
				int ns1 [{| num_of_comm |}] = { {| ', '.join( send_codes ) |} };
				int ns2 [{| num_of_comm |}] = { {| ', '.join( recv_codes ) |} };
				return all_eq_sum<{| num_of_comm |}>(ns1, ns2);
			}
		''')
		return compile_template(receive_member_codes, receive_comm_codes=receive_comm_codes, num_of_comm=num_of_comm
                                       ,send_codes=send_codes, recv_codes=recv_codes)

	def generate_fact_lhs(self, fact_idx, loc_fact, fact_var_name, head_idx, var_ctxt):
		fact_info = self.fact_dict[fact_idx]
		orig_pat_vars  = [mk_cpp_var_name(loc_fact.loc.name)] + map(lambda t: mk_cpp_var_name(t.name), loc_fact.fact.terms)

		pat_types = [fact_info['loc_type']]+fact_info['type_codes']

		idx_var_eq = []
		mod_pat_vars = []
		mod_inits = []
		for i in range(0,len(orig_pat_vars)):
			if orig_pat_vars[i] in var_ctxt:
				mod_inits.append( "%s %s%s;" % (pat_types[i],orig_pat_vars[i],head_idx) )
				idx_var_eq.append( "Equality.is_eq(%s,%s%s)" % (orig_pat_vars[i],orig_pat_vars[i],head_idx) )
				mod_pat_vars.append( "%s%s" % (orig_pat_vars[i],head_idx) )
			else:
				mod_pat_vars.append( orig_pat_vars[i])

		pat_args = ['loc'] + map(lambda i: "arg%s" % i,range(1,len(mod_pat_vars)))
		arg_decs = map( lambda (t,v,a): "%s = %s.%s;" % (v,fact_var_name,a) 
                                , zip([fact_info['loc_type']]+fact_info['type_codes'], mod_pat_vars, pat_args))
		fact_lhs_codes = template('''
			{| '\\n'.join( mod_inits ) |}
			{| '\\n'.join( arg_decs ) |}
		''')
		return orig_pat_vars,compile_template(fact_lhs_codes, arg_decs=arg_decs, mod_inits=mod_inits),idx_var_eq

	def generate_join_ordering_member(self, ensem_name, join_ordering, join_idx):

		self.needs_final_escape= False

		fact_idx  = join_ordering.fact_idx
		fact_info = self.fact_dict[fact_idx]
		fact_type = fact_info['fact_name']
		fact_var  = fact_info['var_name']
		arg_types = fact_info['type_codes']

		join_member_name = "execute_%s_join_ordering_%s" % (fact_var, join_idx)

		if self.incl_logs:
			logging_code = "CoLogger.logger.info( String.format(\"Attempting occurrence %s on %s\", act) );" % (join_member_name,"%s")
		else:
			logging_code = ""	
	
		if fact_idx in self.join_ordering_dict:
			self.join_ordering_dict[fact_idx].append( { 'member_name':join_member_name, 'always_continue':join_ordering.is_active_prop } )
		else:
			self.join_ordering_dict[fact_idx] = [{ 'member_name':join_member_name, 'always_continue':join_ordering.is_active_prop }]

		source_text = join_ordering.__repr__()

		join_ordering_codes = self.generate_join_ordering(join_ordering, join_ordering.getJoinTasks(), {})

		init_head_vars_codes = []
		java_type_coerce = JavaTypeCoercion()
		for rule_var in join_ordering.rule_head_vars:
			init_head_vars_codes.append( "%s %s;" % (java_type_coerce.coerce_type_codes( rule_var.type ), mk_cpp_var_name(rule_var.name) ) )

		if self.needs_final_escape or join_ordering.is_active_prop:
			escape_code = "return true;"
		else:
			escape_code = ""

		join_member_codes = template('''
			/*
			{| source_text |}
			*/
			protected boolean {| join_member_name |}({| fact_type |} act) {
				{| logging_code |}
				{| '\\n'.join( init_head_vars_codes ) |}
				{| join_ordering_codes |}
				{| escape_code |}
			}
		''')

		return compile_template( join_member_codes, join_member_name=join_member_name, fact_type=fact_type, source_text=source_text
                                       , logging_code=logging_code, join_ordering_codes=join_ordering_codes, init_head_vars_codes=init_head_vars_codes
                                       , escape_code=escape_code)

	def generate_fact_member(self, fact_idx, join_ordering_info=None):
		if join_ordering_info == None:
			fact_info = self.fact_dict[fact_idx]
			fact_name = fact_info['fact_name']
			var_name  = fact_info['var_name']
			if fact_idx in self.join_ordering_dict:
				rest_exec_codes = compact( self.generate_fact_member(fact_idx, self.join_ordering_dict[fact_idx]) )
			else:
				rest_exec_codes = compact( self.generate_fact_member(fact_idx, []) )
			fact_exec_codes = template('''
				protected void execute({| fact_name |} {| var_name |}) {
					{| rest_exec_codes |}
				}
			''')
			return compile_template(fact_exec_codes, fact_name=fact_name, var_name=var_name, rest_exec_codes=rest_exec_codes)
		else:
			if len(join_ordering_info) == 0:				
				fact_info = self.fact_dict[fact_idx]
				if fact_info['monotone']:
					store_codes = template("store( {| var_name |} );")
				else:
					store_codes = ""
				return compact( compile_template(store_codes, var_name=self.fact_dict[fact_idx]['var_name']) )
			else:
				this_join_ordering_info = join_ordering_info[0]
				rest_join_ordering_info = join_ordering_info[1:]
				if this_join_ordering_info['always_continue']:
					fact_exec_codes = template('''
						{| member_name |}( {| var_name |} );
						{| rest_exec_codes |}
					''')
				else:
					fact_exec_codes = template('''
						if( {| member_name |}({| var_name |}) ) {
							{| rest_exec_codes |}
						}
					''')

				rest_exec_codes = compact( self.generate_fact_member(fact_idx, rest_join_ordering_info) )
				return compile_template(fact_exec_codes, member_name=this_join_ordering_info['member_name']
                                                       ,var_name=self.fact_dict[fact_idx]['var_name']
                                                       ,rest_exec_codes=rest_exec_codes )

	# ==========================
	# Generating Join Orderings
	# ==========================

	def generate_join_ordering(self, join_ordering, join_tasks, join_head_dict):
		if len(join_tasks) > 0:
			curr_task  = join_tasks[0]
			rest_tasks = join_tasks[1:]
			template_args,join_ordering_template = self.generate_join_task(curr_task, join_head_dict)
			rest_tasks_code = self.generate_join_ordering(join_ordering, rest_tasks, join_head_dict)
			template_args['rest_tasks_code'] = rest_tasks_code
			return compact( compile_template(join_ordering_template, **template_args) )
		else:
			if join_ordering.is_active_prop: # join_ordering.is_propagated:
				escape_code = ""
			else:
				escape_code = "return false;"

			counter_code = "%s_rule_count++;" % join_ordering.getName()
			
			pat_var_list = all_pat_vars(join_head_dict)
			ls = (join_ordering.getName(), ','.join( map(lambda _: "%s",pat_var_list) )
                             ,"" if len(pat_var_list)==0 else (', '.join( map(lambda v: "ToString.to_str(%s)" % v,pat_var_list) ) ))

			if self.incl_logs:
				log_code = "CoLogger.logger.info( String.format(\"Applying %s(%s)\", %s) );" % ls
			else:
				log_code = ""

			end_rule_codes = template('''
				{| counter_code |}
				{| log_code |}
				{| escape_code |}
			''')

			return compile_template( end_rule_codes, counter_code=counter_code, log_code=log_code, escape_code=escape_code)


	@visit.on('join_task')
	def generate_join_task(self, join_task, join_head_dict):
		pass

	@visit.when(join.ActiveAtom)
	def generate_join_task(self, join_task, join_head_dict):
		pat_vars,fact_head_codes,_ = self.generate_fact_lhs(join_task.head.fact_idx, join_task.head.fact, "act"
                                                                   ,join_task.head_idx, all_pat_vars(join_head_dict))
		join_head_dict[join_task.head_idx] = { 'fact_idx' : join_task.head.fact_idx
                                                     , 'pat_vars' : pat_vars
                                                     , 'fact_var' : "act"
                                                     , 'head'     : join_task.head
                                                     , 'is_atom'  : True }

		if self.fact_dict[join_task.head.fact_idx]['monotone']:
			join_ordering_template = template('''
				{| source_text |}
				{| curr_task_code |}
				{| rest_tasks_code |}
			''')
		else:
			join_ordering_template = template('''
				{| source_text |}
				if (act.is_alive()) {
					{| curr_task_code |}			
					{| rest_tasks_code |}
				}
			''')
			self.needs_final_escape= True
		template_args = { 'curr_task_code' : fact_head_codes
                                , 'source_text'    : mk_join_task_source_text( join_task ) }
		return template_args,join_ordering_template

	@visit.when(join.ActiveCompre)
	def generate_join_task(self, join_task, join_head_dict):
		pat_vars,fact_head_codes,_ = self.generate_fact_lhs(join_task.head.fact_idx, join_task.fact_pat, "act"
                                                                   ,join_task.head_idx, all_pat_vars(join_head_dict))
		join_head_dict[join_task.head_idx] = { 'fact_idx' : join_task.head.fact_idx
                                                     , 'pat_vars' : pat_vars
                                                     , 'fact_var' : "act"
                                                     , 'head'     : join_task.head
                                                     , 'is_atom'  : False }

		java_type_coerce = JavaTypeCoercion()
		init_binder_codes = map(lambda iv: "%s %s;" % (java_type_coerce.coerce_type_codes( iv.type ), mk_cpp_var_name(iv.name))
                                       ,join_task.compre_binders)

		if self.fact_dict[join_task.head.fact_idx]['monotone']:
			join_ordering_template = template('''
				{| source_text |}
				{| '\\n'.join(init_binder_codes) |}
				{| curr_task_code |}
				{| rest_tasks_code |}
			''')
		else:
			join_ordering_template = template('''
				{| source_text |}
				if (act.is_alive()) {
					{| '\\n'.join(init_binder_codes) |}
					{| curr_task_code |}			
					{| rest_tasks_code |}
				}
			''')
			self.needs_final_escape= True
		template_args = { 'curr_task_code'    : fact_head_codes
                                , 'init_binder_codes' : init_binder_codes
                                , 'source_text'       : mk_join_task_source_text( join_task ) }
		return template_args,join_ordering_template

	@visit.when(join.CheckGuard)
	def generate_join_task(self, join_task, join_head_dict):
		context_codes,cond_codes = self.generate_term( join_task.guard.term )
		join_ordering_template = template('''
			{| source_text |}
			{| '\\n'.join( context_codes ) |}
			if ({| cond_codes |}) {
				{| rest_tasks_code |}
			}
		''')
		self.needs_final_escape= True
		template_args = { 'context_codes' : context_codes
		                , 'cond_codes'    : cond_codes
                                , 'source_text'   : mk_join_task_source_text( join_task ) }
		return template_args,join_ordering_template

	@visit.when(join.LookupAtom)
	def generate_join_task(self, join_task, join_head_dict):

		# fact_pat = this_match_step['fact_pat']
		# sym_id   = fact_pat['sym_id']
		# store_info = self.store_decs[sym_id][this_match_step['lookup_index']]
		fact_pat = join_task.fact
		fact_idx = join_task.head.fact_idx
		lookup   = join_task.lookup
		store_info = self.store_dict[fact_idx][lookup.lookup_idx]
		cand_idx = join_task.head_idx

		# idx_vars   = map(mk_cpp_var_name,this_match_step['idx_vars'])
		idx_vars   = map(lambda iv: mk_cpp_var_name(iv.name), lookup.inputVars(join_task.head))

		if store_info['has_index']:
			index_func_app = "%s(%s)" % (store_info['idx_func'],','.join(idx_vars))
		else:
			index_func_app = ""

		fact_name  = self.fact_dict[fact_idx]['fact_name']
		cand_name  = "cand_%s" % cand_idx

		if self.fact_dict[fact_idx]['persistent']:
			get_next_code = "get_next()"
		else:
			get_next_code = "get_next_alive()"

		this_pat_vars,fact_lhs_codes,idx_var_eq = self.generate_fact_lhs(fact_idx, fact_pat, "%s" % cand_name, cand_idx, all_pat_vars(join_head_dict))
		join_head_dict[join_task.head_idx] = { 'fact_idx' : fact_idx
                                                     , 'pat_vars' : this_pat_vars
                                                     , 'fact_var' : cand_name
                                                     , 'head'     : join_task.head
                                                     , 'is_atom'  : True }

		# TODO: Might not be always collision free.
		if store_info['collision_free'] or not self.incl_logs:
			report_collision_code = ""
		else:
			msg_str = "\"Collision on %s found: %s is not a compatiable candidate.\"" % ("%s","%s")
			l_args = "cand_%s, %s" % (cand_idx,cand_name)
			report_collision_code = "CoLogger.logger.info( String.format(%s, %s) ); " % (msg_str, l_args)

		msg_str = "\"Candidate for %s found -> %s\"" % (cand_name,"%s")
		if self.incl_logs:
			logging_codes = "CoLogger.logger.info( String.format(%s, %s) );" % (msg_str, cand_name)
		else:
			logging_codes = ""

		if not store_info['collision_free']: # len(idx_var_eq) > 0:
			join_ordering_template = template('''
				{| source_text |}
				StoreIter<{| fact_name |}> candidates_{| cand_idx |} = {| store_name |}.lookup_candidates({| index_func_app |});
				{| fact_name |} {| cand_name |} = candidates_{| cand_idx |}.{| get_next_code |};
				while({| cand_name |} != null) {
					{| fact_lhs_codes |}
					{| logging_codes |}
					if ({| ' && '.join( idx_var_eq ) |}) {
						{| rest_tasks_code |}
					} else {
						{| report_collision_code |}
					}
					{| cand_name |} = candidates_{| cand_idx |}.{| get_next_code |};
				}
			''') 
		else:
			join_ordering_template = template('''
				{| source_text |}
				StoreIter<{| fact_name |}> candidates_{| cand_idx |} = {| store_name |}.lookup_candidates({| index_func_app |});
				{| fact_name |} {| cand_name |} = candidates_{| cand_idx |}.{| get_next_code |};
				while({| cand_name |} != null) {
					{| fact_lhs_codes |}
					{| logging_codes |}
					if (true) {
						{| rest_tasks_code |}
					}
					{| cand_name |} = candidates_{| cand_idx |}.{| get_next_code |};
				}
			''')
		self.needs_final_escape= True
		template_args = { 'iter_type'      : store_info['iter']
		                , 'cand_idx'       : cand_idx 
		                , 'store_name'     : store_info['name']
		                , 'index_func_app' : index_func_app
		                , 'fact_name'      : fact_name
		                , 'cand_name'      : cand_name
		                , 'fact_lhs_codes' : fact_lhs_codes 
		                , 'idx_var_eq'     : idx_var_eq
		                , 'get_next_code'  : get_next_code
		                , 'logging_codes'  : logging_codes
		                , 'report_collision_code' : report_collision_code
		                , 'source_text'    : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	@visit.when(join.LookupAll)
	def generate_join_task(self, join_task, join_head_dict):
		
		fact_pat = join_task.fact_pat
		fact_idx = join_task.head.fact_idx
		lookup   = join_task.lookup
		store_info = self.store_dict[fact_idx][lookup.lookup_idx]
		cand_idx = join_task.head_idx
		idx_vars   = map(lambda iv: mk_cpp_var_name(iv.name), lookup.inputVars(join_task.head))

		if store_info['has_index']:
			index_func_app = "%s(%s)" % (store_info['idx_func'],','.join(idx_vars))
		else:
			index_func_app = ""

		fact_name  = self.fact_dict[fact_idx]['fact_name']
		cand_name  = "cand_%s" % cand_idx

		if self.fact_dict[fact_idx]['persistent']:
			get_next_code = "get_next()"
		else:
			get_next_code = "get_next_alive()"

		this_pat_vars,fact_lhs_codes,idx_var_eq = self.generate_fact_lhs(fact_idx, fact_pat, "*%s" % cand_name, cand_idx, all_pat_vars(join_head_dict))
		iter_name = "candidates_%s" % cand_idx
		iter_mod  = 0

		compre_vars = map(lambda iv: mk_cpp_var_name(iv.name), join_task.term_vars)
		extern_pat_vars = []
		for pat_var in this_pat_vars:
			if pat_var not in compre_vars:
				extern_pat_vars.append( pat_var )

		join_head_dict[join_task.head_idx] = { 'fact_idx' : fact_idx
                                                     , 'pat_vars' : extern_pat_vars
                                                     , 'fact_var' : iter_name
                                                     , 'head'     : join_task.head
                                                     , 'is_atom'  : False
                                                     , 'fact_pat' : fact_pat
                                                     , 'iter_mod' : iter_mod }

		# TODO: Might not be always collision free.
		msg_str = "\"Collision on %s found: %s is not a compatiable candidate.\"" % ("%s","%s")
		report_collision_code = "CoLogger.logger.info( String.format(%s,%s,%s) );" % (msg_str, "cand_%s" % cand_idx, cand_name)

		template_args = { 'old_iter_type'  : store_info['iter']
                                , 'old_iter_name'  : iter_name
                                , 'fact_name'      : fact_name
                                , 'store_name'     : store_info['name']
                                , 'index_func_app' : index_func_app 
		                , 'source_text'    : mk_join_task_source_text( join_task ) }

		if store_info['collision_free']: # len(idx_var_eq) == 0:
			join_ordering_template = template('''
				{| source_text |}
				StoreIter<{| fact_name |}> {| old_iter_name |} = {| store_name |}.lookup_candidates({| index_func_app |});
				{| rest_tasks_code |}
			''')
			# join_head_dict[join_task.head_idx]['idx_var_eq'] = idx_var_eq
		else:
			new_iter_name = "%s_%s" % (iter_name,iter_mod)
			new_iter_type = "ListStoreIter<%s>" % fact_name
			join_head_dict[join_task.head_idx]['fact_var'] = new_iter_name
			join_head_dict[join_task.head_idx]['iter_mod'] = iter_mod + 1

			template_args['new_iter_type'] = new_iter_type
			template_args['new_iter_name'] = new_iter_name
			template_args['cand_name']  = cand_name
			template_args['fact_lhs_codes'] = fact_lhs_codes 
			template_args['idx_var_eq']     = idx_var_eq
			template_args['get_next_code']  = get_next_code
			# template_args['logging_codes']  = logging_codes
			# template_args['report_collision_code'] = report_collision_code

			join_ordering_template = template('''
				{| source_text |}
				StoreIter<{| fact_name |}> {| old_iter_name |} = {| store_name |}.lookup_candidates({| index_func_app |});
				{| new_iter_type |} {| new_iter_name |} = new {| new_iter_type |}();
				{| fact_name |} {| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				while({| cand_name |} != null) {
					{| fact_lhs_codes |}
					if ({| ' && '.join( idx_var_eq ) |}) {
						{| new_iter_name |}.add( {| cand_name |} );
					}
					{| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				}
				{| new_iter_name |}.init_iter();
				{| rest_tasks_code |}
			''')
			
		return template_args,join_ordering_template

	@visit.when(join.FilterHead)
	def generate_join_task(self, join_task, join_head_dict):

		head_idx1  = join_task.head_idx1
		head_idx2  = join_task.head_idx2
		head_info1 = join_head_dict[head_idx1]
		head_info2 = join_head_dict[head_idx2]
		
		cand_idx  = join_task.head_idx1
		iter_mod  = head_info1['iter_mod']
		cand_name = "cand_%s_%s" % (cand_idx,iter_mod)
		old_iter_name = head_info1['fact_var']
		fact_idx  = head_info1['fact_idx']
		fact_name = self.fact_dict[fact_idx]['fact_name']
		fact_pat  = head_info1['fact_pat']

		head_info1['iter_mod'] = iter_mod + 1

		if self.fact_dict[fact_idx]['persistent']:
			get_next_code = "get_next()"
		else:
			get_next_code = "get_next_alive()"

		_,fact_lhs_codes,_ = self.generate_fact_lhs(fact_idx, fact_pat, "%s" % cand_name, cand_idx, set([]))

		new_iter_name = "%s_%s" % (old_iter_name,iter_mod)
		new_iter_type = "ListStoreIter<%s>" % fact_name
		join_head_dict[cand_idx]['fact_var'] = new_iter_name
		join_head_dict[cand_idx]['iter_mod'] = iter_mod + 1

		template_args = { 'new_iter_type' : new_iter_type
                                , 'new_iter_name' : new_iter_name
                                , 'fact_name'     : fact_name
                                , 'cand_name'     : cand_name
                                , 'fact_lhs_codes': fact_lhs_codes
                                , 'old_iter_name' : old_iter_name
                                , 'get_next_code' : get_next_code
                                , 'source_text'   : mk_join_task_source_text( join_task ) }

		if head_info2['is_atom']:

			head2_name = head_info2['fact_var']

			join_ordering_template = template('''
				{| source_text |}
				{| new_iter_type |} {| new_iter_name |} = new {| new_iter_type |}();
				{| fact_name |} {| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				while({| cand_name |} != null) {
					{| fact_lhs_codes |}
					if (({| cand_name |}).identity() != ({| head2_name |}).identity() ) {
						{| new_iter_name |}.add( *{| cand_name |} );
					}
					{| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				}
				{| new_iter_name |}.init_iter();
				{| rest_tasks_code |}
			''')

			template_args['head2_name'] = head2_name

		else:

			join_ordering_template = template('''
				{| source_text |}
				{| new_iter_type |} {| new_iter_name |} = new {| new_iter_type |}();
				{| fact_name |} {| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				while({| cand_name |} != null) {
					{| fact_lhs_codes |}
					if ( !{| iter2_name |}.contains( {| cand_name |} ) ) {
						{| new_iter_name |}.add( {| cand_name |} );
					}
					{| cand_name |} = {| old_iter_name |}.{| get_next_code |};
				}
				{| new_iter_name |}.init_iter();
				{| rest_tasks_code |}
			''')

			template_args['iter2_name'] = head_info2['fact_var']

		return template_args,join_ordering_template

	@visit.when(join.FilterGuard)
	def generate_join_task(self, join_task, join_head_dict):

		cand_idx = join_task.head_idx
		head_info = join_head_dict[join_task.head_idx]
		iter_mod  = head_info['iter_mod']
		cand_name = "cand_%s_%s" % (cand_idx,iter_mod)
		old_iter_name = head_info['fact_var']
		fact_idx  = head_info['fact_idx']
		fact_name = self.fact_dict[fact_idx]['fact_name']
		fact_pat  = head_info['fact_pat']

		head_info['iter_mod'] = iter_mod + 1

		if self.fact_dict[fact_idx]['persistent']:
			get_next_code = "get_next()"
		else:
			get_next_code = "get_next_alive()"

		_,fact_lhs_codes,_ = self.generate_fact_lhs(fact_idx, fact_pat, "%s" % cand_name, cand_idx, set([]))

		new_iter_name = "%s_%s" % (old_iter_name,iter_mod)
		new_iter_type = "ListStoreIter<%s>" % fact_name
		join_head_dict[cand_idx]['fact_var'] = new_iter_name
		join_head_dict[cand_idx]['iter_mod'] = iter_mod + 1

		guard_context_codes = []
		guard_cond_codes    = [] # join_head_dict[cand_idx]['idx_var_eq']
		for guard in join_task.guards:
			guard_context_code,guard_cond_code = self.generate_term( guard.term )
			guard_context_codes += guard_context_code
			guard_cond_codes += [guard_cond_code]

		join_ordering_template = template('''
			{| source_text |}
			{| new_iter_type |} {| new_iter_name |} = new {| new_iter_type |}();
			{| fact_name |} {| cand_name |} = {| old_iter_name |}.{| get_next_code |};
			while({| cand_name |} != null) {
				{| fact_lhs_codes |}
				{| '\\n'.join( guard_context_codes ) |}
				if ({| ' && '.join( guard_cond_codes ) |}) {
					{| new_iter_name |}.add( {| cand_name |} );
				}
				{| cand_name |} = {| old_iter_name |}.{| get_next_code |};
			}
			{| new_iter_name |}.init_iter();
			{| rest_tasks_code |}
		''')

		template_args = { 'new_iter_type'  : new_iter_type
                                , 'new_iter_name'  : new_iter_name
                                , 'fact_name'      : fact_name
                                , 'cand_name'      : cand_name
                                , 'old_iter_name'  : old_iter_name
                                , 'get_next_code'  : get_next_code
                                , 'fact_lhs_codes' : fact_lhs_codes
                                , 'guard_context_codes' : guard_context_codes
                                , 'guard_cond_codes'    : guard_cond_codes
		                , 'source_text'    : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	@visit.when(join.CompreDomain)
	def generate_join_task(self, join_task, join_head_dict):

		cand_idx = join_task.head_idx
		head_info = join_head_dict[join_task.head_idx]
		iter_mod  = head_info['iter_mod']
		cand_name = "cand_%s_%s" % (cand_idx,iter_mod)
		iter_name = head_info['fact_var']
		fact_idx  = head_info['fact_idx']
		fact_name = self.fact_dict[fact_idx]['fact_name']
		fact_pat  = head_info['fact_pat']

		term_vars  = join_task.term_vars
		compre_dom = join_task.compre_dom

		init_binders = join_task.init_binders

		java_type_corece = JavaTypeCoercion()
		# print "Hurry! %s" % compre_dom.type
		compre_dom_type = java_type_corece.coerce_type_codes( compre_dom.type )
		# print compre_dom_type
		compre_dom_name = mk_cpp_var_name( compre_dom.name )
		if len(term_vars) > 1:
			# TODO
			compre_dom_elem = "Tuples.make_tuple(%s)" % (','.join(map(lambda tv: mk_cpp_var_name(tv.name),term_vars))) 
		else:
			compre_dom_elem = mk_cpp_var_name(term_vars[0].name)

		init_binders_codes = []
		if init_binders:
			init_binders_codes = map(lambda tv: "%s %s;" % (java_type_corece.coerce_type_codes( tv.type )
                                                ,mk_cpp_var_name(tv.name)) ,term_vars)			

		if self.fact_dict[fact_idx]['persistent']:
			get_next_code = "get_next()"
		else:
			get_next_code = "get_next_alive()"

		_,fact_lhs_codes,_ = self.generate_fact_lhs(fact_idx, fact_pat, "%s" % cand_name, cand_idx, set([]))

		head_info['iter_mod'] = iter_mod + 1

		head_info['pat_vars'].append( compre_dom_name )

		join_ordering_template = template('''
			{| source_text |}
			{| compre_dom_name |} = new {| compre_dom_type |}();
			{| fact_name |} {| cand_name |} = {| iter_name |}.{| get_next_code |};
			while({| cand_name |} != null) {
				{| '\\n'.join(init_binders_codes) |}
				{| fact_lhs_codes |}
				{| compre_dom_name |}.add( {| compre_dom_elem |} );
				{| cand_name |} = {| iter_name |}.{| get_next_code |};
			}
			{| iter_name |}.init_iter();
			{| rest_tasks_code |}
		''')

		template_args = { 'compre_dom_type' : compre_dom_type
                                , 'compre_dom_name' : compre_dom_name
                                , 'compre_dom_elem' : compre_dom_elem
                                , 'init_binders_codes' : init_binders_codes
                                , 'fact_name'       : fact_name
                                , 'cand_name'       : cand_name
                                , 'iter_name'       : iter_name
                                , 'get_next_code'   : get_next_code
                                , 'fact_lhs_codes'  : fact_lhs_codes
		                , 'source_text'     : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	@visit.when(join.NeqHead)
	def generate_join_task(self, join_task, join_head_dict):
		head_idx1  = join_task.head_idx1
		head_idx2  = join_task.head_idx2 
		head_info1 = join_head_dict[head_idx1]
		head_info2 = join_head_dict[head_idx2]
		monotone1 = self.fact_dict[head_info1['fact_idx']]['monotone']
		monotone2 = self.fact_dict[head_info2['fact_idx']]['monotone']
		if (head_idx1 != 0 or (head_idx1 == 0 and not monotone1)) and (head_idx2 != 0 or (head_idx2 == 0 and not monotone2)):
			fact_var1 = join_head_dict[head_idx1]['fact_var']
			fact_var2 = join_head_dict[head_idx2]['fact_var']
			neq_head_cond =	"(%s).identity() != (%s).identity()" % (fact_var1,fact_var2)
			# msg_str = "\"Collision on %s found: %s is not a compatiable candidate.\"" % ("%s","%s")
			# l_args = "cand_%s %s (**%s).pretty()" % (cand_idx," % ",cand_name)
			# report_collision_code = "LOG_RULE_APP( record((format(%s) %s %s).str(), THIS_SRC) );" % (msg_str,"%",l_args)
			join_ordering_template = template('''
				{| source_text |}
				if ({| neq_head_cond |}) {
					{| rest_tasks_code |}
				}
			''')
			self.needs_final_escape= True
			template_args = { 'neq_head_cond' : neq_head_cond
                                        , 'source_text'   : mk_join_task_source_text( join_task ) }
			return template_args,join_ordering_template
		else:
			join_ordering_template = template('''
				{| source_text |}
				{| rest_tasks_code |}
			''')
			return { 'source_text' : mk_join_task_source_text( join_task ) },join_ordering_template

	@visit.when(join.DeleteHead)
	def generate_join_task(self, join_task, join_head_dict):
		head_info = join_head_dict[join_task.head_idx]
		monotone = self.fact_dict[head_info['fact_idx']]['monotone']
		if (join_task.head_idx != 0) or (join_task.head_idx == 0 and not monotone):
			head      = head_info['head']
			store_name = self.store_dict[head.fact_idx][0]['name']
			if head_info['is_atom']:
				cand_name  = join_head_dict[join_task.head_idx]['fact_var']
				join_ordering_template = template('''
					{| source_text |}
					{| store_name |}.remove( {| cand_name |} );
					{| rest_tasks_code |}
				''')
				template_args = { 'store_name'  : store_name
		                                , 'source_text' : mk_join_task_source_text( join_task ) }
				if join_task.head_idx == 0:
					template_args['cand_name'] = cand_name
				else:
					template_args['cand_name'] = "%s" % cand_name
				return template_args,join_ordering_template
			else:
				
				cand_idx = join_task.head_idx
				head_info = join_head_dict[join_task.head_idx]
				iter_mod  = head_info['iter_mod']
				cand_name = "cand_%s_%s" % (cand_idx,iter_mod)
				iter_name = head_info['fact_var']
				fact_idx  = head_info['fact_idx']
				fact_name = self.fact_dict[fact_idx]['fact_name']
				fact_pat  = head_info['fact_pat']

				if self.fact_dict[fact_idx]['persistent']:
					get_next_code = "get_next()"
				else:
					get_next_code = "get_next_alive()"

				_,fact_lhs_codes,_ = self.generate_fact_lhs(fact_idx, fact_pat, "%s" % cand_name, cand_idx, set([]))

				join_ordering_template = template('''
					{| source_text |}
					{| fact_name |} {| cand_name |} = {| iter_name |}.{| get_next_code |};
					while({| cand_name |} != null) {
						{| store_name |}.remove( {| cand_name |} );
						{| cand_name |} = {| iter_name |}.{| get_next_code |};
					}
					{| rest_tasks_code |}
				''')
				template_args = { 'fact_name'     : fact_name
                                                , 'cand_name'     : cand_name
                                                , 'iter_name'     : iter_name
                                                , 'get_next_code' : get_next_code
                                                , 'store_name'    : store_name
		                                , 'source_text'   : mk_join_task_source_text( join_task ) }

				return template_args,join_ordering_template
		else:
			join_ordering_template = template('''
				{| source_text |}
				// H0 is active and monotone, no delete required
				{| rest_tasks_code |}
			''')
			return { 'source_text' : mk_join_task_source_text( join_task ) },join_ordering_template

	@visit.when(join.ExistDest)
	def generate_join_task(self, join_task, join_head_dict):
		java_type_coerce = JavaTypeCoercion()
		exist_term = join_task.exist_var
		exist_type = java_type_coerce.coerce_type_codes( exist_term.type )
		exist_name = mk_cpp_var_name(exist_term.name)
		loc_var = mk_cpp_var_name( local_loc_var(join_head_dict) )

		join_ordering_template = template('''
			{| source_text |}
			{| exist_type |} {| exist_name |} = next_exist_id({| loc_var |});
			{| rest_tasks_code |}
		''')
		template_args = { 'exist_type'  : exist_type
		                , 'exist_name'  : exist_name
		                , 'loc_var'     : loc_var 
                                , 'source_text' : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	# TODO: Update for Java
	@visit.when(join.ExistLoc)
	def generate_join_task(self, join_task, join_head_dict):
		java_type_coerce = JavaTypeCoercion()
		exist_loc = join_task.exist_var
		exist_loc_type = java_type_coerce.coerce_type_codes( exist_loc.type )
		exist_loc_name = mk_cpp_var_name(exist_loc.name)
		
		join_ordering_template = template('''
			{| source_text |}
			{| exist_loc_type |} {| exist_loc_name |} = dir->new_node();
		''')
		template_args = { 'exist_loc_type' : exist_loc_type
		                , 'exist_loc_name' : exist_loc_name
	                        , 'source_text'    : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	@visit.when(join.LetBind)
	def generate_join_task(self, join_task, join_head_dict):
		assign_dec = join_task.assign_dec
		left_pat_context,left_pat_codes,left_pat_post = self.generate_left_pattern(assign_dec.term_pat, assign_dec.term_pat.type)
		context_codes,assign_exp_codes = self.generate_term(assign_dec.builtin_exp)
		
		java_type_coerce = JavaTypeCoercion()
		init_pat_codes = "%s %s;" % (java_type_coerce.coerce_type_codes( assign_dec.term_pat.type )
                                            ,mk_cpp_var_name(assign_dec.term_pat.name))

		join_ordering_template = template('''
			{| source_text |}
			{| init_pat_codes |}
			{| left_pat_context |}
			{| '\\n'.join( context_codes ) |}
			{| left_pat_codes |} = {| assign_exp_codes |};
			{| left_pat_post |};
			{| rest_tasks_code |}
		''')

		template_args = { 'init_pat_codes'   : init_pat_codes
                                , 'left_pat_context' : left_pat_context
		                , 'left_pat_codes'   : left_pat_codes
                                , 'left_pat_post'    : left_pat_post
		                , 'context_codes'    : context_codes
		                , 'assign_exp_codes' : assign_exp_codes
		                , 'source_text'      : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	@visit.when(join.IntroAtom)
	def generate_join_task(self, join_task, join_head_dict):

		loc_fact = join_task.fact
		fact_idx = join_task.body.fact_idx
		body_idx = join_task.body_idx

		atom_body = self.generate_rhs_atom(loc_fact, fact_idx, join_task.priority, join_task.monotone, body_idx, join_head_dict)

		join_ordering_template = template('''
			{| source_text |}
			{| atom_body |}
			{| rest_tasks_code |}
		''')		

		template_args = { 'atom_body'   : atom_body
		                , 'source_text' : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	# TODO: Update for Java
	@visit.when(join.IntroCompre)
	def generate_join_task(self, join_task, join_head_dict):

		# TODO: Currently supports only one compre range

		compre_body = join_task.body.fact
		main_comp_range = compre_body.comp_ranges[0]
		compre_idx = join_task.compre_idx
		body_idx   = join_task.body_idx

		term_pat  = main_comp_range.term_vars  # body_fact['term_pat']
		term_subj = main_comp_range.term_range # body_fact['term_subj']
		facts     = compre_body.facts          # body_fact['facts']

		java_type_corece = JavaTypeCoercion()

		subj_type    = java_type_corece.coerce_type_codes( term_subj.type )
		subj_varname = "comp_%s" % compre_idx
		subj_context_codes,subj_exp   = self.generate_term( term_subj )
		term_pat_context,term_pat_var,term_pat_post = self.generate_left_pattern(term_pat, term_pat.type)

		init_binder_codes = map(lambda tv: "%s %s;" % (java_type_corece.coerce_type_codes( tv.type )
                                        ,mk_cpp_var_name(tv.name)) ,join_task.term_vars)

		fact_atom_codes = []
		for loc_fact in facts:
			fact_idx,_ = self.fact_dir.getFactFromName( loc_fact.fact.name )
			fact_atom_codes.append( self.generate_rhs_atom(loc_fact, fact_idx, loc_fact.fact.priority, loc_fact.fact.monotone
                                                                      ,body_idx, join_head_dict) )

		join_ordering_template = template('''
			{| source_text |}
			{| '\\n'.join( subj_context_codes ) |}
			{| subj_type |} {| subj_varname |} = {| subj_exp |};
			for(int idx=0; idx<{| subj_varname |}.size(); idx++) {
				{| '\\n'.join(init_binder_codes) |}
				{| term_pat_context |}
				{| term_pat_var |} = {| subj_varname |}.get(idx);
				{| term_pat_post |}
				{| '\\n'.join( fact_atom_codes ) |}
			}
			{| rest_tasks_code |}
		''')

		template_args = { 'subj_type'    : subj_type
		                , 'subj_varname' : subj_varname
		                , 'subj_exp'     : subj_exp
                                , 'init_binder_codes'  : init_binder_codes
		                , 'subj_context_codes' : subj_context_codes
		                , 'term_pat_context'   : term_pat_context
		                , 'term_pat_var'       : term_pat_var
                                , 'term_pat_post'      : term_pat_post
                                , 'fact_atom_codes'    : fact_atom_codes 
		                , 'source_text'        : mk_join_task_source_text( join_task ) }

		return template_args,join_ordering_template

	def generate_rhs_atom(self, loc_fact, fact_idx, priority, monotone, body_idx, join_head_dict):

		fact_info = self.fact_dict[fact_idx]
		fact_name = fact_info['fact_name']
		loc_var = mk_cpp_var_name( local_loc_var(join_head_dict) )

		fact_args = []
		arg_contexts = []
		for fact_arg in [loc_fact.loc] + loc_fact.fact.terms:
			context_codes,f_arg_codes = self.generate_term( fact_arg )
			fact_args.append( f_arg_codes )
			arg_contexts += context_codes

		# TODO: TDMONO

		# monotone = "true"
		# fact_args.append( monotone )
		# print ("Loc Vars: %s %s" % (body_fact['loc'].name,loc_var))
		if priority != None:
			fact_args.append( priority )

		if True:
			xs = (fact_name,','.join(fact_args))
			if monotone:
				body_codes = "goals.add( new %s(%s) );" % xs 
			else:
				template_args = { 'fact_name' : fact_name
						, 'fact_args' : ','.join(fact_args)
                                                , 'body_name' : "body_%s" % body_idx }
				non_monotone_template = template('''	
{| fact_name |} {| body_name |} = new {| fact_name |}({| fact_args |});
goals.add( {| body_name |} );
store( {| body_name |} );
				''')
				body_codes = compile_template(non_monotone_template, **template_args)
		else:
			xs = (fact_name,','.join(fact_args))
			body_codes = "send( %s(%s) );" % xs
		atom_template = template('''
			{| '\\n'.join( arg_contexts ) |}
			{| body_codes |}
		''')

		return compile_template(atom_template, arg_contexts=arg_contexts, body_codes=body_codes)

	def next_tup_idx(self):
		tidx = self.tup_idx
		self.tup_idx += 1
		return "tup%s" % tidx

	# Let binding left patterns
	def generate_left_pattern(self, term_pat, type_sig, boxed_type=False):
		# print term_pat
		# print type_sig
		java_type_coerce = JavaTypeCoercion()

		if type_sig.type_kind == ast.TYPE_CONS:
			var_name = mk_cpp_var_name( term_pat.name )
			return "",var_name,""
			# return "%s %s;" % (java_type_coerce.coerce_type_codes( term_pat.type, boxed_type ), var_name),var_name,""
		elif type_sig.type_kind == ast.TYPE_TUP:
			left_pat_contexts = []
			left_pat_codes    = []
			left_pat_posts    = []
			# Err: What if term_pat is not a tuple, but a variable of tuple type!!
			for (term,ty_sig) in zip(term_pat.terms,type_sig.types):
				left_pat_context,left_pat_code,left_pat_post = self.generate_left_pattern(term, ty_sig, True)
				left_pat_contexts.append( left_pat_context )
				left_pat_codes.append( left_pat_code )
				left_pat_posts.append( left_pat_post )
			tup_type = java_type_coerce.coerce_type_codes( term.type )
			tup_name = next_tup_idx()
			lp_contexts = compile_template( template('''
{| '\\n'.join( left_pat_contexts ) |}
{| tup_type |} {| tup_name |};
			'''), left_pat_contexts=left_pat_contexts, tup_type=tup_type, tup_name=tup_name )
			lp_codes = tup_name
			tup_binds = map(lambda (lp,tid): "%s = %s.t%s;" % (lp,tup_name,tid) , zip(left_pat_codes,range(1,len(left_pat_codes))) )
			lp_posts = compile_template( template('''
{| '\\n'.join( left_pat_posts ) |}
{| '\\n'.join( tup_binds ) |}
			'''), left_pat_posts=left_pat_posts, tup_binds=tup_binds )
			return lp_contexts,lp_codes,lp_posts
		elif type_sig.type_kind in [ast.TYPE_LIST,ast.TYPE_MSET]:
			if term_pat.term_type == ast.TERM_VAR:
				var_name = mk_cpp_var_name( term_pat.name )
				return "",var_name,""
				# return "%s %s;" % (java_type_coerce.coerce_type_codes( term_pat.type, True ), var_name),var_name,""
			else:
				# TODO
				pass
		else:
			# TODO
			pass

	# TODO: handle all types of terms
	def generate_term(self, t):
		java_type_coerce = JavaTypeCoercion()
		if t.term_type == ast.TERM_BINOP:
			cx1,t1_codes = self.generate_term( t.term1 )
			cx2,t2_codes = self.generate_term( t.term2 )
			context_codes = cx1 + cx2
			term_codes = "%s %s %s" % (t1_codes,t.op,t2_codes)
		elif t.term_type == ast.TERM_VAR:
			context_codes = []
			term_codes = mk_cpp_var_name( t.name )
		elif t.term_type == ast.TERM_LIT:
			context_codes = []
			term_codes = str(t.literal)
		elif t.term_type == ast.TERM_CONS:
			context_codes = []
			term_codes = t.name
		elif t.term_type == ast.TERM_APP:
			cx1,t1_codes = self.generate_term( t.term1 )
			cx2,t2_codes = self.generate_term( t.term2 )
			context_codes = cx1 + cx2
			if t.term2.term_type == ast.TERM_TUPLE:
				# TODO: ugly hack! Need to make uncurried function app first class!
				term_codes = "%s%s" % (t1_codes,t2_codes[10:])
			else:
				term_codes = "%s(%s)" % (t1_codes,t2_codes)
		elif t.term_type == ast.TERM_TUPLE:
			context_codes = []
			t_codes = []
			for t in t.terms:
				cx,t_code = self.generate_term( t )
				context_codes += cx
				t_codes.append( t_code )
			term_codes = "Tuples.make_tuple(%s)" % ','.join(t_codes)
		elif t.term_type in [ast.TERM_LIST,ast.TERM_MSET]:
			context_codes = []
			t_codes = []
			for term in t.terms:
				cx,t_code = self.generate_term( term )
				context_codes += cx
				t_codes.append( t_code )
			term_var = self.next_temp_var_name()
			inspect = Inspector()
			# print "%s" % t.inferred_type
			context_codes.append( "%s[] %s = { %s };" % (java_type_coerce.coerce_type_codes(t.type.type, True),term_var,','.join(t_codes)) )
			term_codes = "Misc.to_list(%s)" % (term_var)
		return context_codes,term_codes

class JavaTypeCoercion:

	def __init__(self):
		pass

	# Coercion from MSRE types to Java types.
	# TODO: Currently works only for simple types.
	@visit.on('arg_type')
	def coerce_type_codes(self, arg_type, boxed=False):
		pass

	@visit.when(ast.TypeVar)
	def coerce_type_codes(self, arg_type, boxed=False):
		return "T"

	@visit.when(ast.TypeCons)
	def coerce_type_codes(self, arg_type, boxed=False):
		const_name = arg_type.name
		if const_name == ast.LOC:
			return 'int' if not boxed else 'Integer'
		elif const_name == ast.INT:
			return 'int' if not boxed else 'Integer'
		elif const_name == ast.FLOAT:
			return 'float' if not boxed else 'Float'
		elif const_name == ast.CHAR:
			return 'char' if not boxed else 'Character'
		elif const_name == ast.STRING:
			return 'String'
		elif const_name == ast.BOOL:
			return 'boolean' if not boxed else 'Boolean'
		elif const_name == ast.DEST:
			return 'String'

	@visit.when(ast.TypeList)
	def coerce_type_codes(self, arg_type, boxed=False):
		java_type = self.coerce_type_codes( arg_type.type, True )
		return "LinkedList<%s> " % java_type

	@visit.when(ast.TypeMSet)
	def coerce_type_codes(self, arg_type, boxed=False):
		# print "Here! %s" % arg_type.type
		java_type = self.coerce_type_codes( arg_type.type, True )
		return "LinkedList<%s> " % java_type

	@visit.when(ast.TypeTuple)
	def coerce_type_codes(self, arg_type, boxed=False):
		java_types = map(lambda t: self.coerce_type_codes(t, True), arg_type.types) 
		return "Tuple%s<%s> " % (len(java_types),','.join( java_types ))

	# Coercion from MSRE type to required C++ pretty codes
	@visit.on( 'arg_type' )
	def coerce_pretty_codes(self, arg_type):
		pass

	@visit.when(ast.TypeCons)
	def coerce_pretty_codes(self, arg_type):
		const_name = arg_type.name
		if const_name in [ast.LOC,ast.INT,ast.FLOAT,ast.CHAR,ast.STRING,ast.BOOL,ast.DEST]:
			return "%s"
		else:
			# TODO: Support other pretty base types
			return "%s"

	@visit.when(ast.TypeTuple)
	def coerce_pretty_codes(self, arg_type):
		return "%s"
	
	@visit.when(ast.TypeList)
	def coerce_pretty_codes(self, arg_type):
		return "%s"

	@visit.when(ast.TypeMSet)
	def coerce_pretty_codes(self, arg_type):
		return "%s"

