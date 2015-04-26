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

package comingle.runtime;

import java.util.List;

import android.app.Activity;
import android.util.Log;
import comingle.android.directory.ui.dialogs.DirectoryWifiAdapterDialogBuilder;
import comingle.android.directory.ui.dialogsequences.DirectoryChoiceDialogSequence;
import comingle.android.directory.ui.dialogsequences.DirectoryChosenListener;
import comingle.comms.datapipe.DataPipeManager;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.identity.IdentityGenerator;
import comingle.comms.log.Logger;
import comingle.comms.message.Message;
import comingle.comms.sockets.SocketDataPipe;
import comingle.facts.SerializedFact;
import comingle.nodes.SendListener;
import comingle.rewrite.RewriteMachine;

public class CoMingleAndroidRuntime<RW extends RewriteMachine> extends DataPipeManager<SerializedFact,String> implements Logger {
	
	protected static final String TAG = "CoMingleAndroidRuntime";
	protected static final int DEFAULT_ADMIN_PORT = 8010;
	protected static final int DEFAULT_FACT_PORT  = 6565;
	
	protected final Activity activity;
	protected final Class<RW> rwClass;
	protected final int adminPort;
	protected final int factPort;
	protected final String defaultReqCode;
	protected final String runtimeID;
	protected final int location;
	
	protected RW rewriteMachine = null;

	protected CompositeDirectory directory;
	
	protected volatile boolean isRewriteReady = false;
	
	public CoMingleAndroidRuntime(Activity activity, Class<RW> rwClass, int adminPort, int factPort, String defaultReqCode) {
		super( new SocketDataPipe<SerializedFact>( factPort ));
		this.rwClass   = rwClass;
		this.activity  = activity;
		this.adminPort = adminPort;
		this.factPort  = factPort;
		this.defaultReqCode = defaultReqCode;
		this.runtimeID = IdentityGenerator.generateID();
		this.location  = runtimeID.hashCode();
	}
	
	public CoMingleAndroidRuntime(Activity activity, Class<RW> rwClass, String defaultReqCode) {
		this(activity, rwClass, DEFAULT_ADMIN_PORT, DEFAULT_FACT_PORT, defaultReqCode);
	}

	///////////////////////
	// Directory Methods //
	///////////////////////
	
	protected void initCompositeDirectory(BaseDirectory<Message> mainDir) {
		directory = new CompositeDirectory(mainDir);
		init();
		Log.i(TAG, "Runtime directory instantiated.");
	}
	
	protected DirectoryChoiceDialogSequence dirChoiceDiagSeq = null;
	
	public void initStandardDirectorySetup(int peer_list_row, int peer_name, int peer_loc, int peer_ip, 
			DirectoryChosenListener<Message> postDirChoiceListener) {
		Log.i(TAG, "Initiatizing standard directory setup routine...");
		dirChoiceDiagSeq = new DirectoryChoiceDialogSequence(activity, adminPort, defaultReqCode, runtimeID,
				                  peer_list_row, peer_name, peer_loc, peer_ip);
		dirChoiceDiagSeq.addDirectoryChosenListener(new DirectoryChosenListener<Message>() {
			@Override
			public void doDirectoryChosenAction(BaseDirectory<Message> directory) {
				directory.initNetworkNotifications();
				directory.resumeNetworkNotifications();
				initCompositeDirectory(directory);
			}
		});
		if (postDirChoiceListener != null) {
			dirChoiceDiagSeq.addDirectoryChosenListener(postDirChoiceListener);
		}
		dirChoiceDiagSeq.start();
	}
	
	public void initStandardDirectorySetup(int peer_list_row, int peer_name, int peer_loc, int peer_ip) {
		this.initStandardDirectorySetup(peer_list_row, peer_name, peer_loc, peer_ip, null);
	}
	
	
	public void handleOnActivityResults(int requestCode) {
		if (this.dirChoiceDiagSeq != null && requestCode == DirectoryWifiAdapterDialogBuilder.DEFAULT_ACT_REQ_CODE){
			Log.i(TAG, "Returning from network adapter setup, reopening directory setup");
			this.dirChoiceDiagSeq.getDirectorySetupSequence().start();
		}
	}
	
	public void resumeNetworkNotifications() {
		if (directory != null) {
			Log.i(TAG, "Resuming directory network notifications");
			directory.resumeNetworkNotifications();
		}
	}
	
	public void pauseNetworkNotifications() {
		if ( directory != null) {
			Log.i(TAG, "Pausing directory network notifications");
			directory.pauseNetworkNotifications();
		}
	}
	
	public CompositeDirectory getDirectory() {
		return directory;
	}
	
	/////////////////////////////
	// Rewrite Machine Methods //
	/////////////////////////////
	
	public boolean initRewriteMachine() {
		
		if (directory == null) { return false; }

		if(rewriteMachine != null) { 
			isRewriteReady = false;
			rewriteMachine.stop_rewrite(); 
			log("Rewrite machine stopped");
		}
		
		log("Initializing rewrite machine...");
		
		try {
			this.rewriteMachine = rwClass.newInstance();
		} catch (InstantiationException e) {
			err("Error while instantiating rewrite machine" + e.toString());
			return false;
		} catch (IllegalAccessException e) {
			err("Error while instantiating rewrite machine" + e.toString());
			return false;
		}
		
		SendListener rmSendListener = new SendListener() {
			@Override
			public void performSendAction(final String ipAddr, final List<SerializedFact> facts) {
				sendData(facts, ipAddr);
			}
		};

		rewriteMachine.setupNeighborhood(directory, rmSendListener);
		
		log("Successfully initialized rewrite machine");
		
		return true;
		
	}
	
	public boolean startRewriteMachine() {
		if (directory != null && rewriteMachine != null) {
			rewriteMachine.init();
			rewriteMachine.start();
			isRewriteReady = true;
			log("Rewrite machine started");
			return true;
		} else {
			log("Cannot start rewrite machine, either directory or machine is null");
			return false;
		}
	}
	
	public RW getRewriteMachine() {
		return rewriteMachine;
	}
	
	public void close() {
		log("Closing all connections");
		super.close();
		if(directory != null) {
			directory.close();
		} 
		if(rewriteMachine != null) {
			rewriteMachine.stop_rewrite();
		}
	}
	
	//////////////////////
	// Identity Methods //
	//////////////////////
	
	public int getLocation() {
		return directory.getLocation();
	}
	
	public boolean isOwner() {
		return directory.isOwner();
	}

	public boolean isMember() {
		return directory.isMember();
	}
	
	////////////////////////////
	// Runtime Status Methods //
	////////////////////////////
	
	public boolean isRewriteReady() {
		return isRewriteReady;
	}
	
	////////////////////////////
	// Fact Receiving Methods //
	////////////////////////////
	
	@Override
	protected void receiveData(List<SerializedFact> facts, String addr) {
		if(rewriteMachine != null) {
			log( String.format("%s facts received from %s", facts.size(), addr));
			rewriteMachine.addExternalGoals(facts);
		} else {
			err(String.format("Received pre-mature facts from %s, ignoring...", addr));
		}
	}

	@Override
	protected void handleReceiveException(final Exception e) {
		err("Error occurred while sending/receiving facts: " + e.toString() );	
	}

	@Override
	public void log(final String msg) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, msg);	
			}
		});
	}

	@Override
	public void err(final String msg) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.e(TAG, msg);	
			}
		});
	}

	@Override
	public void info(final String msg) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, msg);	
			}
		});
	}
	
}
