/*
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

CoMingle Version 1.5, Prototype Alpha

Authors:
Edmund S. L. Lam      sllam@qatar.cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
*/

package comingle.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import comingle.comms.neighborhood.Neighborhood;
import comingle.facts.SerializedFact;
import comingle.misc.Beeper;
import comingle.nodes.SendListener;
import comingle.rewrite.QuiescenceEvent;
import comingle.rewrite.QuiescenceListener;
import comingle.rewrite.RewriteMachine;

public class RewriteCluster<RW extends RewriteMachine> extends Thread {

	protected HashMap<String,RW> rw_machines;
	protected HashMap<Integer,String> directory;
	protected int curr_loc = 0;
	protected Beeper terminated = new Beeper();
	protected boolean proceed = true;
	
	public RewriteCluster() {
		rw_machines = new HashMap<String,RW>();
		directory   = new HashMap<Integer,String>();
	}
	
	public RewriteCluster(List<RW> rw_machines) {
		this.rw_machines = new HashMap<String,RW>();
		directory   = new HashMap<Integer,String>();
		
		final RewriteCluster<RW> self = this;
		SendListener send_listener = new SendListener() {
			@Override
			public void performSendAction(String loc_name, List<SerializedFact> facts) {
				self.send(loc_name, facts);
			}
		};
		
		for(int loc=0; loc < rw_machines.size(); loc++) {
			String loc_name = String.format("L%s",loc);
			this.directory.put(loc, loc_name);
		}
		
		for(int loc=0; loc < rw_machines.size(); loc++) {
			String loc_name = String.format("L%s",loc);
			RW rw_machine = rw_machines.get(loc);
			Neighborhood neighborhood = new InternalNeighborhood(loc, directory);
			rw_machine.setupNeighborhood(neighborhood, send_listener);
			this.rw_machines.put(loc_name, rw_machine);
		}
	}
	
	protected void send(String loc_name, List<SerializedFact> facts) {
		RW rw_machine = this.rw_machines.get(loc_name);
		rw_machine.addExternalGoals(facts);
	}
	
	public void init() {
		for(RW rw_machine : rw_machines.values()) {
			rw_machine.init();
		}
	}
	
	public void initQuiescenceSnapShotFile(String file_format) {
		for(final RW rw_machine: rw_machines.values()) {
			final String file_name = String.format(file_format, rw_machine.getLocation());
			
			QuiescenceListener qlistener = new QuiescenceListener() {
				@Override
				public void performQuiescenceAction(QuiescenceEvent qe) {
					try {
						PrintWriter writer = new PrintWriter(file_name,"UTF-8");
						writer.println(rw_machine.getPretty());
						writer.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			rw_machine.addPersistentQuiescenceListener(qlistener);
			
		}
	}
	
	public RW getMachine(int loc) {
	 	return rw_machines.get( this.directory.get(loc) );
	}
	
	@Override
	public void run() {
		for(RW rw_machine: rw_machines.values()) {
			rw_machine.start();
		}
		terminated.wait_for_beep();
	}
	
	public void stopRewrite() {
		for(RW rw_machine: rw_machines.values()) {
			rw_machine.stop_rewrite();
		}		
	}
	
	public void restartRewrite() {
		for(RW rw_machine: rw_machines.values()) {
			rw_machine.restart_rewrite();
		}		
	}
	
	public void terminateRewrite() {
		for(RW rw_machine: rw_machines.values()) {
			rw_machine.terminate_rewrite();
		}
		terminated.beep();
	}
	
	public String getPrettyBrief() {
		String output = "######### Cluster State #########\n";
		for(Entry<String,RW> entry : rw_machines.entrySet() ) {
			output += "##### Location: " + entry.getKey() + "#####\n";
			output += entry.getValue().getPrettyBrief() + "\n";
		}
		return output;
	}

	public String getPretty() {
		String output = "######### Cluster State #########\n";
		for(Entry<String,RW> entry : rw_machines.entrySet() ) {
			output += "##### Location: " + entry.getKey() + "#####\n";
			output += entry.getValue().getPretty() + "\n";
		}
		return output;
	}
	
	abstract public class ConsoleInput {
		public final static String SHOWLOC = "ShowLoc";
		public final static String SHOWALL = "ShowAll";
		public final static String EXIT = "exit";
		abstract public void action(RewriteCluster<RW> machine);
	}
	
	public class ShowLoc extends ConsoleInput {
		protected int loc;
		public ShowLoc(int loc) {
			this.loc = loc;
		}
		
		public void action(RewriteCluster<RW> machine) {
			System.out.print( machine.getMachine(loc).getPretty() );
		}
	}
	
	public class ShowAll extends ConsoleInput {
		public void action(RewriteCluster<RW> machine) {
			System.out.print(machine.getPrettyBrief());
		}
	}
	
	public class Exit extends ConsoleInput {
		public void action(RewriteCluster<RW> machine) {
			machine.setEndConsole();
		}
	}
	
	public class Unknown extends ConsoleInput {
		public void action(RewriteCluster<RW> machine) {
			System.out.println("Unknown input.. please try again.");
		}
	}
	
	public ConsoleInput parseConsoleInput(String input) {
		try {
			StringTokenizer st = new StringTokenizer(input," ");
			String cmd_token = (String) st.nextElement();
			if (cmd_token.equals("show")) {
				if(!st.hasMoreElements()) {
					return new ShowAll();
				} else {
					String arg_token = (String) st.nextElement();
					return new ShowLoc(Integer.parseInt(arg_token));
				}
			} else if (cmd_token.equals("exit")) {
				return new Exit();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new Unknown();
	}
	
	public void setEndConsole() { proceed = false; }
	
	public void runConsole() {

		System.out.println(this.getPrettyBrief());
		System.out.println("Starting Rewrite Cluster");
		
		this.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while(this.proceed) {
			System.out.println("Enter command: ");
			try {
				String input = br.readLine();
				ConsoleInput cons = this.parseConsoleInput(input);
				cons.action(this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		this.stopRewrite();
		
		System.out.println("Terminating...");
	}
	
	public static <RW extends RewriteMachine> RewriteCluster<RW> createCluster(Class<RW> rw, int n) {
		try {
			List<RW> machines = new LinkedList<RW>();
			for(int x=0; x<n; x++) {
				machines.add( rw.newInstance() );
			}
			return new RewriteCluster<RW>(machines);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
