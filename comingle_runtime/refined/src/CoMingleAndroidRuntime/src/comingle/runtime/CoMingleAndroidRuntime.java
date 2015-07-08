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

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import comingle.android.directory.ui.dialogs.DirectoryWifiAdapterDialogBuilder;
import comingle.android.directory.ui.dialogsequences.DirectoryChoiceDialogSequence;
import comingle.android.directory.ui.dialogsequences.DirectoryChosenListener;
import comingle.android.sensors.nfc.NDefMessageCreator;
import comingle.android.sensors.nfc.NDefMessageListener;
import comingle.android.sensors.nfc.NfcSensor;
import comingle.comms.datapipe.DataPipeManager;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.identity.IdentityGenerator;
import comingle.comms.lntp.LNTPChannel;
import comingle.comms.lntp.LNTPClient;
import comingle.comms.lntp.LNTPServer;
import comingle.comms.log.Logger;
import comingle.comms.message.Message;
import comingle.comms.ntp.PseudoNTPClient;
import comingle.comms.receiver.ExceptionListener;
import comingle.comms.sockets.SocketDataPipe;
import comingle.facts.SerializedFact;
import comingle.lib.ExtLib;
import comingle.nodes.SendListener;
import comingle.rewrite.RewriteMachine;

/**
 * 
 * The top-level runtime entity that combines a CoMingle Runtime and a Composite Directory, in the context of an Android activity.
 * 
 * @author Edmund S.L. Lam
 *
 * @param <RW> the Rewrite Machine instance
 */
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
	
	/**
	 * Basic Constructor
	 * @param activity the activity that embeds this CoMingle runtime instance.
	 * @param rwClass the class of the CoMingle runtime.
	 * @param adminPort port number for administrative messages.
	 * @param factPort port number for actual payload data.
	 * @param defaultReqCode default request code of the application.
	 */
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
	
	/**
	 * Default constructor, setup runtime with admin port '8010' and data port '6565'.
	 * @param activity the activity that embeds this CoMingle runtime instance.
	 * @param rwClass the class of the CoMingle runtime.
	 * @param defaultReqCode default request code of the application.
	 */
	public CoMingleAndroidRuntime(Activity activity, Class<RW> rwClass, String defaultReqCode) {
		this(activity, rwClass, DEFAULT_ADMIN_PORT, DEFAULT_FACT_PORT, defaultReqCode);
	}

	/////////////////////////////
	// Android Activity Events //
	/////////////////////////////
	
	public void onResume() {
		this.resumeNetworkNotifications();
		this.resumeNFCSensorNotifications();
	}
	
	public void onPause() {
		this.pauseNetworkNotifications();
		this.pauseNFCSensorNotifications();
	}
	
	public void onNewIntent(Intent intent) {
		this.checkNewNFCIntent(intent);
	}
	
	///////////////////////
	// Directory Methods //
	///////////////////////
	
	/**
	 * Initializes with given BaseDirectory as the main directory
	 * @param mainDir the main directory
	 */
	protected void initCompositeDirectory(BaseDirectory<Message> mainDir) {
		directory = new CompositeDirectory(mainDir);
		init();
		Log.i(TAG, "Runtime directory instantiated.");
	}
	
	protected DirectoryChoiceDialogSequence dirChoiceDiagSeq = null;
	
	/**
	 * Initialize standard directory setup dialog sequence
	 * @param peer_list_row Resource ID of row view for each node entry.
	 * @param peer_name Resource ID of name field for a node.
	 * @param peer_loc Resource ID of location field for a node.
	 * @param peer_ip Resource ID of address field for a node.
	 * @param postDirChoiceListener the directory chosen listener to invoke after user has chosen directory.
	 */
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
	
	/**
	 * Initialize standard directory setup dialog sequence,  with directory chosen listener set to null.
	 * @param peer_list_row Resource ID of row view for each node entry.
	 * @param peer_name Resource ID of name field for a node.
	 * @param peer_loc Resource ID of location field for a node.
	 * @param peer_ip Resource ID of address field for a node.
	 */
	public void initStandardDirectorySetup(int peer_list_row, int peer_name, int peer_loc, int peer_ip) {
		this.initStandardDirectorySetup(peer_list_row, peer_name, peer_loc, peer_ip, null);
	}
	
	/**
	 * Handling operation for returning from wifi-adapter setup. To be called in 'onActivityResults' operation of the parent activity
	 * @param requestCode the request code of the activity result return.
	 */
	public void handleOnActivityResults(int requestCode) {
		if (this.dirChoiceDiagSeq != null && requestCode == DirectoryWifiAdapterDialogBuilder.DEFAULT_ACT_REQ_CODE){
			Log.i(TAG, "Returning from network adapter setup, reopening directory setup");
			this.dirChoiceDiagSeq.getDirectorySetupSequence().start();
		}
	}
	
	/**
	 * Resume all network notifications. Typically called in 'onResume' of the parent activity
	 */
	public void resumeNetworkNotifications() {
		if (directory != null) {
			Log.i(TAG, "Resuming directory network notifications");
			directory.resumeNetworkNotifications();
		}
	}

	/**
	 * Pause all network notifications. Typically called in 'onPause' of the parent activity
	 */
	public void pauseNetworkNotifications() {
		if ( directory != null) {
			Log.i(TAG, "Pausing directory network notifications");
			directory.pauseNetworkNotifications();
		}
	}
	
	/**
	 * Returns the composite directory of this runtime.
	 * @return the composite directory of this runtime.
	 */
	public CompositeDirectory getDirectory() {
		return directory;
	}
	
	/////////////////////////////
	// Rewrite Machine Methods //
	/////////////////////////////
	
	/**
	 * Initialize the rewrite machine
	 * @return true if the rewrite machine is initialized.
	 */
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
	
	/**
	 * Start the rewrite machine
	 * @return true if the rewrite machine is successfully started.
	 */
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
	
	/**
	 * Returns the rewrite machine
	 * @return the rewrite machine
	 */
	public RW getRewriteMachine() {
		return rewriteMachine;
	}
	
	/**
	 * Close all operations of the rewrite machine.
	 */
	public void close() {
		log("Closing all connections");
		super.close();
		if(directory != null) {
			directory.close();
		} 
		if(rewriteMachine != null) {
			rewriteMachine.stop_rewrite();
		}
		if(this.lntpChannel != null) {
			lntpChannel.close();
		}
	}
	
	//////////////////////
	// Identity Methods //
	//////////////////////
	
	/**
	 * Get the location ID of this rewrite machine.
	 * @return the location ID of this rewrite machine.
	 */
	public int getLocation() {
		return directory.getLocation();
	}
	
	/**
	 * Returns true if this runtime is an owner.
	 * @return true if this runtime is an owner.
	 */
	public boolean isOwner() {
		return directory.isOwner();
	}

	/**
	 * Returns true if this runtime is a member.
	 * @return true if this runtime is a member.
	 */
	public boolean isMember() {
		return directory.isMember();
	}
	
	///////////////////////////////////
	// Misc Activity Support Methods //
	///////////////////////////////////
	
	public void postAlert(final String title, final String msg) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder(activity);
				alert.setTitle(title);
				alert.setMessage(msg);
				alert.setPositiveButton("Ok", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alert.show();
			}
		});
	}
	
	public void postToast(final String msg) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}	
	
	////////////////////////////
	// Runtime Status Methods //
	////////////////////////////
	
	/**
	 * Returns true if rewrite runtime has been started.
	 * @return true if rewrite runtime has been started.
	 */
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
	protected void handleReceiveException(final String task, final Exception e) {
		err("Error occurred (" + task + "): " + e.toString() );	
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
	
	/////////////////////
	// Time Operations //
	/////////////////////
	
	/*
	public long getLocalTime(String date) {
		return ExtLib.getLocalTime(date);
	} */
	
	public long getLocalTime(String date) {
		long localOffset = getLocalTimeOffset();
		return ExtLib.parseDate(date) + localOffset;
	}
	
	public long getLocalTime(long date) {
		return date + getLocalTimeOffset();
	}
	
	/*
	PseudoNTPClient pntpClient = null;
	public void initPseudoNTPService() {
		pntpClient = new PseudoNTPClient( directory.mainDir.getOwnerIP() );
		if(directory.isOwner()) {
			pntpClient.servePseudoNTPTime();
		}
	}
	
	Long localTimeOffset = null;
	public long getLocalTimeOffset() {
		if(directory.isOwner()) {
			return 0;
		} else {
			if(localTimeOffset == null) {
				localTimeOffset = pntpClient.getTimeOffset();
			}
			return localTimeOffset;
		}
	}*/
	
	Handler handler = null;
	LNTPChannel lntpChannel = null;
	Timer timer = null;
	public void initTimeServices(Handler handler) {
		if(directory.isOwner()) {
			lntpChannel = new LNTPServer( directory.mainDir.getOwnerIP() );
		} else {
			lntpChannel = new LNTPClient( directory.mainDir.getOwnerIP() );
		}
		lntpChannel.setExceptionListener(new ExceptionListener() {
			@Override
			public void performExceptionAction(String task, Exception e) {
				err("Error Occured in Time Service Channel (" + task + ") : " + e.toString());
			}			
		});
		lntpChannel.init();
		this.handler = handler;
		this.timer = new Timer();
	}
	
	Long localTimeOffset = null;
	public long getLocalTimeOffset() {
		if(localTimeOffset == null) {
			localTimeOffset = lntpChannel.getTimeOffset();
		}
		return localTimeOffset;
	}
	
	public void clearLocalTimeOffset() {
		localTimeOffset = null;
	}
	
	
	
    private class CoMingleTimerTask extends TimerTask {
    	
    	final Handler handler;
    	final Runnable event;
    	
    	public CoMingleTimerTask(Handler handler, Runnable event) {
    		this.handler = handler;
    		this.event   = event;
    	}
    	
    	@Override
    	public void run() {
	        final Thread thread = new Thread(new Runnable() {
	            public void run() {
	                handler.post(event);
	            }
	        });
	        thread.start();
    	}
    	
    }
	
	public void scheduleAt(Runnable event, long eventTime) {
		timer.schedule(new CoMingleTimerTask(handler, event), new Date(getLocalTime(eventTime)) );
	}
	
	////////////////
	// NFC Sensor //
	////////////////
	
	protected NfcSensor nfcSensor = null;
	protected boolean nfcInited = false;
	
	protected String nfcMimeType = "application/comingle-runtime-nfc";
	
	public boolean initNFCSensor(String nfcMimeType, final RewriteMachineOperation<RW,Integer> ndefMsgListener) {
		
		if (nfcInited) { return false; }
		
		nfcSensor = new NfcSensor(activity, nfcMimeType);
		
		nfcSensor.setNDefMessageCreator(new NDefMessageCreator() {
			@Override
			public String onCreateNDefMessage() {
				return String.format("%s", rewriteMachine.getLocation());
			}
		});
		
		nfcSensor.setNDefMessageListener(new NDefMessageListener() {
			@Override
			public void onReceiveMessage(NdefMessage msg) {
				final String str = new String(msg.getRecords()[0].getPayload());
				ndefMsgListener.doAction(rewriteMachine, Integer.parseInt(str));
			}
		});
		
		nfcInited = true;
		
		boolean succ = nfcSensor.init();
		if(succ) {
			nfcSensor.resumeSensorNotifications();
		}
		return succ;
	}
	
	public boolean initNFCSensor(final RewriteMachineOperation<RW,Integer> ndefMsgListener) {
		return initNFCSensor(nfcMimeType, ndefMsgListener);
	}
	
	protected void resumeNFCSensorNotifications() {
		if (nfcSensor != null) {
			nfcSensor.resumeSensorNotifications();
			log("NFC Sensor resumed.");
		}
	}
	
	protected void pauseNFCSensorNotifications() {
		if (nfcSensor != null) {
			nfcSensor.pauseSensorNotifications();
			log("NFC Sensor paused.");
		}
	}	
	
	protected void checkNewNFCIntent(Intent intent) {
		if (nfcSensor != null) {
			nfcSensor.processIntent(intent);
		}
	}
	
}
