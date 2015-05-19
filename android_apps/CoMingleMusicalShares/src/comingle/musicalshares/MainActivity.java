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
Ali Elgazar           aee@cmu.edu

* This implementation was made possible by an JSREP grant (JSREP 4-003-2-001, Effective Distributed 
Programming via Join Patterns with Guards, Propagation and More) from the Qatar National Research Fund 
(a member of the Qatar Foundation). The statements made herein are solely the responsibility of the authors.
*/

package comingle.musicalshares;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import musicalshares.Musicalshares;
import comingle.actuation.ActuatorAction;
import comingle.android.directory.ui.dialogsequences.DirectoryChosenListener;
import comingle.android.tones.NoteGenerator;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.listeners.LocalNodeInfoAvailableListener;
import comingle.comms.message.Message;
import comingle.lib.ExtLib;
import comingle.misc.Misc;
import comingle.mset.SimpMultiset;
import comingle.pretty.PrettyPrinter;
import comingle.runtime.CoMingleAndroidRuntime;
import comingle.tuple.Tuple2;
import comingle.tuple.Tuples;
import comingle.tuple.Unit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "Musical Shares Activity";
	
	private static final String MS_REQ_CODE = "MUSICAL_SHARES_COMINGLE";
	private static final int MS_ADMIN_PORT  = 8181;
	private static final int MS_FACT_PORT   = 8819;
	
	private CoMingleAndroidRuntime<Musicalshares> musicRuntime;
	
	private final NoteGenerator noteGen = new NoteGenerator();
	private final Handler handler = new Handler();
	
	private Menu options_menu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	   	musicRuntime = new CoMingleAndroidRuntime<Musicalshares>(this, Musicalshares.class, MS_ADMIN_PORT, MS_FACT_PORT, MS_REQ_CODE);
	   	
    	DirectoryChosenListener<Message> postDirChoiceListener = new DirectoryChosenListener<Message>() {
			@Override
			public void doDirectoryChosenAction(BaseDirectory<Message> directory) {
				setupDirectory();	
			}
    	};
    	musicRuntime.initStandardDirectorySetup(R.layout.peer_list_row, R.id.peer_name, R.id.peer_loc, R.id.peer_ip, postDirChoiceListener);
	   	
	}

    private void setupDirectory() {
    	final MainActivity self = this;
    	musicRuntime.getDirectory().addLocalNodeInfoAvailableListener(new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, final int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
				    	self.startMusicalsharesRewriteMachine();
				    	self.checkOffset();
					}
				});
			}
    	});
    	musicRuntime.getDirectory().addDirectoryChangedListener(new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(final List<NodeInfo> new_peers,
					List<NodeInfo> added_nodes, final List<NodeInfo> dropped_nodes, int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(dropped_nodes.size() > 0) {
							self.postAlert("Player Dropped", "A player has dropped out! Please restart the app!");
						}
					}
				});
			}
    	});
    }
	
    private void startMusicalsharesRewriteMachine() {
    	
    	musicRuntime.initRewriteMachine();
    	
    	ActuatorAction<Unit> refreshAction = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				// TODO Add refresh screen routine
				// Refresh and display stuff from musicRuntime.getRewriteMachine().get_playlog();
			}
    	};
    	musicRuntime.getRewriteMachine().setRefreshActuator(refreshAction);
    	
    	ActuatorAction<Tuple2<String,Long>> playAction = new ActuatorAction<Tuple2<String,Long>>() {
			@Override
			public void doAction(Tuple2<String, Long> input) {
				final String note   = input.t1;
				final long playTime = input.t2;
				musicRuntime.scheduleAt(noteGen.getPlayNoteAction(1, note), playTime);
				// final long playTime = musicRuntime.getLocalTime(input.t2);
				// noteGen.schedulePlayNote(handler, playTime, 1, note);
				postToast(String.format("%s @ %s", note, input.t2));
			}
    	};
    	musicRuntime.getRewriteMachine().setPlayActuator(playAction);
    	
    	ActuatorAction<Unit> completedAction = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setMenuItemVisibility(R.id.action_start, true);
						}
					});
			}
    	};
    	musicRuntime.getRewriteMachine().setCompletedActuator(completedAction);
    	
    	musicRuntime.startRewriteMachine();
    	
    	if(musicRuntime.isRewriteReady()) {
    		musicRuntime.getRewriteMachine().init();
    		if (musicRuntime.isOwner()) {
    			this.setMenuItemVisibility(R.id.action_distribute, true);
    		}
    		musicRuntime.initTimeServices(handler);
    		// checkOffset();
    	}
    	
    }
    
	public void postAlert(final String title, final String msg) {
		final MainActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder(self);
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
		final MainActivity self = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(self, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}	
	
	public void setMenuItemVisibility(int id, boolean visible) {
		if (options_menu == null) {
			String msg = "Option menu is null";
			Log.e(TAG, msg);
			// postToast(msg);
			return; 
		}
		MenuItem item = options_menu.findItem(id);
		if (item != null) {
			item.setVisible(visible);
		} else {
			String msg = "Failed to find item " + id;
			Log.e(TAG, msg);
			// postToast(msg);
		}
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		options_menu = menu;
		
    	if(musicRuntime.isRewriteReady() && musicRuntime.isOwner()) {
    		this.setMenuItemVisibility(R.id.action_distribute, true);
    	}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.action_distribute) {
			// musicRuntime.getRewriteMachine().addDistribute();
			distributeScores();
			this.setMenuItemVisibility(R.id.action_distribute, false);
		} else if (id == R.id.action_start) {
			// DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
			long timeInMillis = Calendar.getInstance().getTimeInMillis() + (1*1000);
			musicRuntime.getRewriteMachine().addStart(timeInMillis);
			this.setMenuItemVisibility(R.id.action_start, false);
			this.setMenuItemVisibility(R.id.action_reset, true);
		} else if (id == R.id.action_reset) {
			musicRuntime.getRewriteMachine().addReset();
			this.setMenuItemVisibility(R.id.action_reset, false);
			this.setMenuItemVisibility(R.id.action_distribute, true);
		} else if (id == R.id.action_getoffset) {
			musicRuntime.clearLocalTimeOffset();
			checkOffset();
		}
		return super.onOptionsItemSelected(item);
	}
	
	private SimpMultiset<Tuple2<Integer,Integer>> linearGraph(SimpMultiset<Integer> locations) {
		SimpMultiset<Tuple2<Integer,Integer>> edges = new SimpMultiset<Tuple2<Integer,Integer>>();
		Iterator<Integer> it = locations.iterator();
		if(it.hasNext()) {
			int curr = (int) it.next();
			while(it.hasNext()) {
				int next = (int) it.next();
				edges.add(new Tuple2<Integer,Integer>(curr,next));
				curr = next;
			}
		}
		return edges;
	}

	private SimpMultiset<Tuple2<Integer,Integer>> helixGraph(SimpMultiset<Integer> locations) {
		SimpMultiset<Tuple2<Integer,Integer>> edges = new SimpMultiset<Tuple2<Integer,Integer>>();
		Iterator<Integer> it = locations.iterator();
		if(it.hasNext()) {
			Integer curr1 = (int) it.next();
			Integer curr2 = null;
			boolean hx = true;
			while(it.hasNext()) {
				Integer next1 = (Integer) it.next();
				edges.add(new Tuple2<Integer,Integer>(curr1,next1));
				if(hx) {
					if(it.hasNext()) {
						Integer next2 = (Integer) it.next();
						edges.add(new Tuple2<Integer,Integer>(curr1,next2));
						curr1 = next1;
						curr2 = next2;
					} else {
						curr1 = next1;
					}
				} else {
					edges.add(new Tuple2<Integer,Integer>(curr2,next1));
					curr1 = next1;
					curr2 = null;
				}
				hx = !hx;
			}
		}
		return edges;
	}
	
	private void distributeScores() {
		// Currently only this 5 notes.
		String[] noteArr = { "A5","B5","G5","G4","D5" };
		LinkedList<String> notes = Misc.to_list(noteArr);
		SimpMultiset<Integer> locations = Misc.to_mset( musicRuntime.getDirectory().getLocations());
		// SimpMultiset<Tuple2<Integer,Integer>> edges = new SimpMultiset<Tuple2<Integer,Integer>>();
		// edges.add( Tuples.make_tuple(0, 1) );
		musicRuntime.getRewriteMachine().addDistribute(notes, locations, linearGraph(locations));
	}
 	
	
	protected void checkOffset() {
		(new Thread() {
			@Override
			public void run() {
				postToast( String.format("Offset: %s", musicRuntime.getLocalTimeOffset()) );
			}
		}).start();
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	musicRuntime.resumeNetworkNotifications();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	musicRuntime.pauseNetworkNotifications();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	musicRuntime.close();
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		musicRuntime.handleOnActivityResults(requestCode);
	}
	
}
