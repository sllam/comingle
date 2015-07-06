package comingle.mafiapartygame;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import comingle.tuple.Tuple3;
import mafia.Mafia;
import comingle.actuation.ActuatorAction;
import comingle.android.directory.ui.dialogsequences.DirectoryChosenListener;
import comingle.comms.directory.BaseDirectory;
import comingle.comms.directory.NodeInfo;
import comingle.comms.listeners.DirectoryChangedListener;
import comingle.comms.listeners.LocalNodeInfoAvailableListener;
import comingle.comms.message.Message;
import comingle.misc.Misc;
import comingle.mset.SimpMultiset;
import comingle.runtime.CoMingleAndroidRuntime;
import comingle.tuple.Tuple2;
import comingle.tuple.Unit;

import android.content.Context;
import android.os.Vibrator;
import android.app.Activity;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MafiaActivity extends Activity {

	private static final String MPG_REQ_CODE = "MAFIA_PARTY_GAME_COMINGLE";
	private static final int MPG_ADMIN_PORT  = 8181;
	private static final int MPG_FACT_PORT   = 8819;

	private static final int LENGTH_OF_CYCLES_SECS = 30;

	protected CoMingleAndroidRuntime<Mafia> mafiaRuntime;

	protected Menu options_menu;
	protected Handler handler = new Handler();
	Boolean Mafia=false; //value to indicate if mafia or not
	TextView cycle;
	int Night=-1; //value to control voting 1 means it's night 0 means it's day -1 means it's in transition so no one can vote
	boolean dead=false;
	Context context=this;
	Activity act=this;
	boolean started=false;//game started boolean for init button control
	ArrayList<VoteSlot> votes; //arrayList of votes which serves as the base for the arrayAdapter.
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mafia);

		mafiaRuntime = new CoMingleAndroidRuntime<Mafia>(this, Mafia.class, MPG_ADMIN_PORT, MPG_FACT_PORT, MPG_REQ_CODE);

		DirectoryChosenListener<Message> postDirChoiceListener = new DirectoryChosenListener<Message>() {
			@Override
			public void doDirectoryChosenAction(BaseDirectory<Message> directory) {
				setupDirectory();
			}
		};
		mafiaRuntime.initStandardDirectorySetup(R.layout.peer_list_row, R.id.peer_name, R.id.peer_loc, R.id.peer_ip, postDirChoiceListener);
		cycle=(TextView)findViewById(R.id.Cycle);
	}

	private void setupDirectory() {
		final MafiaActivity self = this;
		mafiaRuntime.getDirectory().addLocalNodeInfoAvailableListener(new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, final int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						self.startMafiaRewriteMachine();
						self.checkOffset();
					}
				});
			}
		});
		mafiaRuntime.getDirectory().addDirectoryChangedListener(new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(final List<NodeInfo> new_peers,
												 List<NodeInfo> added_nodes, final List<NodeInfo> dropped_nodes, int role) {
				self.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(dropped_nodes.size() > 0) {
							mafiaRuntime.postAlert("Player Dropped", "A player has dropped out! Please restart the app!");
						}
					}
				});
			}
		});
	}

	private void startMafiaRewriteMachine() {

		mafiaRuntime.initRewriteMachine();

		// Actuator setup

		ActuatorAction<Unit> notifyIsCitizen = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				mafiaRuntime.postAlert("You're a citizen!", "You are now a citizen, your objective is to find the Mafiosos before it's too late!");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final ListView lv=(ListView)findViewById(R.id.Mafiavotes);
						((ViewManager)lv.getParent()).removeView(lv);
						final ListView lv1=(ListView)findViewById(R.id.CombinedVotes);
						Mafia=false;
						Map<Integer,String> map=mafiaRuntime.getDirectory().getNames();
						Iterator<Integer> keys=map.keySet().iterator();
						votes=new ArrayList<VoteSlot>();
						while (keys.hasNext()){
							int Key=keys.next();
							votes.add(new VoteSlot(0,map.get(Key),Key));
						}
						arrayAdapter ad=new arrayAdapter(act,R.layout.array_adapter,R.id.name,R.id.Votes,votes);
						lv1.setAdapter(ad);
						lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (!dead) {
											if (Night == 0) {
												int myIndex = getSlotIndexByLoc(mafiaRuntime.getLocation());
												if (votes.get(myIndex).votedForLoc != -1) {
													votes.get(getSlotIndexByLoc(votes.get(myIndex).votedForLoc)).numberOfVotes--;
												}
												votes.get(myIndex).votedForLoc = votes.get(i).location;
												votes.get(i).numberOfVotes++;
												citizenChangedVote(votes.get(i).location);
												arrayAdapter adapter = new arrayAdapter(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
												lv1.setAdapter(adapter);
											}
										}
									}
								});
							}
						});
					}
				});

			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyIsCitizenActuator(notifyIsCitizen);

		ActuatorAction<SimpMultiset<Integer>> notifyIsMafia = new ActuatorAction<SimpMultiset<Integer>>() {
			@Override
			public void doAction(SimpMultiset<Integer> mafiaLocs) {
				mafiaRuntime.postAlert("You're a Mafioso!","You are now a member of the devious Mafia, your objective is to eliminate the citizens before they find you!");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						final ListView lv1=(ListView)findViewById(R.id.Mafiavotes);
						final ListView lv=(ListView)findViewById(R.id.CombinedVotes);
						Mafia=true;
						Map<Integer,String> map=mafiaRuntime.getDirectory().getNames();
						Iterator<Integer> keys=map.keySet().iterator();
						votes=new ArrayList<VoteSlot>();
						while (keys.hasNext()){
							int Key=keys.next();
							votes.add(new VoteSlot(0,map.get(Key),Key));
						}

						arrayAdapter ad=new arrayAdapter(act,R.layout.array_adapter,R.id.name,R.id.Votes,votes);
						arrayAdapter2 ad2=new arrayAdapter2(act,R.layout.array_adapter,R.id.name,R.id.Votes,votes);
						lv.setAdapter(ad);
						lv1.setAdapter(ad2);
						lv1.setOnItemClickListener(new AdapterView.OnItemClickListener(){
							@Override
							public void onItemClick(AdapterView<?> adapterView, View view,final int i, long l) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (Night == 0) {
											int myIndex = getSlotIndexByLoc(mafiaRuntime.getLocation());
											if (!dead) {
												if (votes.get(myIndex).votedForLoc2 != -1) {
													votes.get(getSlotIndexByLoc(votes.get(myIndex).votedForLoc2)).votees.remove(votes.get(myIndex));
												}
												votes.get(i).votees.add(votes.get(myIndex));
												votes.get(myIndex).votedForLoc2 = votes.get(i).location;
												mafiaChangedVote(votes.get(i).location);
												arrayAdapter2 adapter = new arrayAdapter2(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
												lv1.setAdapter(adapter);
											}
										}
									}
								});
							}
						});
						lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
							@Override
							public void onItemClick(AdapterView<?> adapterView, View view,final int i, long l) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										int myIndex = getSlotIndexByLoc(mafiaRuntime.getLocation());
										if (!dead &&(Night==1||Night==0)) {
											if (votes.get(myIndex).votedForLoc != -1) {
												votes.get(getSlotIndexByLoc(votes.get(myIndex).votedForLoc)).numberOfVotes--;
											}
											votes.get(myIndex).votedForLoc = votes.get(i).location;
											votes.get(i).numberOfVotes++;
											if (Night == 1) {
												mafiaChangedVote(votes.get(i).location);
											} else if (Night == 0) {
												citizenChangedVote(votes.get(i).location);
											}
											arrayAdapter adapter = new arrayAdapter(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
											lv.setAdapter(adapter);
										}
									}
								});
							}
						});
					}
				});

			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyIsMafiaActuator(notifyIsMafia);

		ActuatorAction<Tuple2<Integer,Integer>> notifyMafiaChangedVote = new ActuatorAction<Tuple2<Integer,Integer>>() {
			@Override
			public void doAction(Tuple2<Integer, Integer> input) {
				int mafiaLoc = input.t1;
				int voteLoc = input.t2;
				// TODO: Notify that <mafiaLoc> has voted for <voteLoc> to be murdered.
				if (mafiaLoc != mafiaRuntime.getLocation()) {
					if (Mafia) {
						if (Night == 0) {
							final ListView lv1 = (ListView) findViewById(R.id.Mafiavotes);
							int mafiaIndex = getSlotIndexByLoc(mafiaLoc);
							int victimIndex = getSlotIndexByLoc(voteLoc);
							if (votes.get(mafiaIndex).votedForLoc2 != -1) {
								votes.get(getSlotIndexByLoc(votes.get(mafiaIndex).votedForLoc2)).votees.remove(votes.get(mafiaIndex));
							}
							votes.get(victimIndex).votees.add(votes.get(mafiaIndex));
							votes.get(mafiaIndex).votedForLoc2 = votes.get(victimIndex).location;
							final arrayAdapter2 adapter = new arrayAdapter2(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
							Runnable updateVotees = new Runnable() {
								@Override
								public void run() {
									lv1.setAdapter(adapter);
								}
							};
							runOnUiThread(updateVotees);
						}
						final ListView lv = (ListView) findViewById(R.id.CombinedVotes);
						int mafiaIndex = getSlotIndexByLoc(mafiaLoc);
						int victimIndex = getSlotIndexByLoc(voteLoc);
						if (votes.get(mafiaIndex).votedForLoc != voteLoc) {
							if (votes.get(mafiaIndex).votedForLoc != -1) {
								int prevVictimIndex = getSlotIndexByLoc(votes.get(mafiaIndex).votedForLoc);
								votes.get(prevVictimIndex).numberOfVotes--;
							}
							votes.get(victimIndex).numberOfVotes++;
							votes.get(mafiaIndex).votedForLoc = voteLoc;
							final arrayAdapter ad = new arrayAdapter(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
							Runnable updateCounter = new Runnable() {
								@Override
								public void run() {
									lv.setAdapter(ad);
								}
							};
							runOnUiThread(updateCounter);
						}
					} else if (mafiaRuntime.isOwner()) {
						int mafiaIndex = getSlotIndexByLoc(mafiaLoc);
						int victimIndex = getSlotIndexByLoc(voteLoc);
						if (votes.get(mafiaIndex).votedForLoc != -1) {
							int prevVictimIndex = getSlotIndexByLoc(votes.get(mafiaIndex).votedForLoc);
							votes.get(prevVictimIndex).numberOfVotes--;
						}
						if (votes.get(mafiaIndex).votedForLoc != voteLoc) {
							votes.get(victimIndex).numberOfVotes++;
							votes.get(mafiaIndex).votedForLoc = voteLoc;
						}
					}
				}
			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyMafiaChangedVoteActuator(notifyMafiaChangedVote);

		ActuatorAction<Tuple2<Integer,Integer>> notifyCitizenChangedVote = new ActuatorAction<Tuple2<Integer,Integer>>() {
			@Override
			public void doAction(Tuple2<Integer, Integer> input) {
				int citizenLoc = input.t1;
				int voteLoc    = input.t2;
				// TODO: Notify that <citizenLoc> has voted for <voteLoc> to be executed.
				if(citizenLoc!=mafiaRuntime.getLocation()){
					final ListView lv = (ListView) findViewById(R.id.CombinedVotes);
					int citizenIndex=getSlotIndexByLoc(citizenLoc);
					int victimIndex=getSlotIndexByLoc(voteLoc);
					if(votes.get(citizenIndex).votedForLoc!=voteLoc) {
						if(votes.get(citizenIndex).votedForLoc!=-1){
							int prevVictimIndex=getSlotIndexByLoc(votes.get(citizenIndex).votedForLoc);
							votes.get(prevVictimIndex).numberOfVotes--;
						}
						votes.get(victimIndex).numberOfVotes++;
						votes.get(citizenIndex).votedForLoc = voteLoc;
						final arrayAdapter ad = new arrayAdapter(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
						Runnable updateCounter = new Runnable() {
							@Override
							public void run() {
								lv.setAdapter(ad);
							}
						};
						runOnUiThread(updateCounter);
					}
				}
			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyCitizenChangedVoteActuator(notifyCitizenChangedVote);

		ActuatorAction<Long> warnNight = new ActuatorAction<Long>() {
			@Override
			public void doAction(Long warnTime) {
				// warnTime: Time to warn of approaching night
				// TODO: Implement <nightTimeWarningEvent>: warning beep and count down UI display
				Runnable nightTimeWarningEvent = new Runnable() {
					@Override
					public void run() {
						try {
							Night=-1;
							Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
							r.play();
							new CountDownTimer(10000, 1000) {

								public void onTick(long millisUntilFinished) {
									cycle.setText("seconds remaining: " + millisUntilFinished / 1000);
								}

								public void onFinish() {
									cycle.setText("Night Time!");
								}
							}.start();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				mafiaRuntime.scheduleAt(nightTimeWarningEvent, warnTime);
			}
		};
		mafiaRuntime.getRewriteMachine().setWarnNightActuator(warnNight);

		ActuatorAction<Long> warnDay = new ActuatorAction<Long>() {
			@Override
			public void doAction(Long warnTime) {
				// warnTime: Time to warn of approaching night
				// TODO: Implement <dayTimeWarningEvent>: warning beep and count down UI display
				Runnable dayTimeWarningEvent =new Runnable() {
					@Override
					public void run() {
						try {
							Night=-1;
							Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
							r.play();
							new CountDownTimer(10000, 1000) {

								public void onTick(long millisUntilFinished) {
									cycle.setText("seconds remaining: " + millisUntilFinished / 1000);
								}

								public void onFinish() {
									cycle.setText("Day Time!");
								}
							}.start();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				mafiaRuntime.scheduleAt(dayTimeWarningEvent, warnTime);
			}
		};
		mafiaRuntime.getRewriteMachine().setWarnDayActuator(warnDay);

		final Runnable resetVotes =new Runnable() {
			@Override
			public void run() {//a runnable to reset the votes of everyone ONLY RUN ON UI THREAD
				for(int i=0;i<votes.size();i++){
					votes.get(i).numberOfVotes=0;
					votes.get(i).votedForLoc=-1;
				}
				arrayAdapter ad = new arrayAdapter(act, R.layout.array_adapter, R.id.name, R.id.Votes, votes);
				ListView lv = (ListView) findViewById(R.id.CombinedVotes);
				lv.setAdapter(ad);
			}
		};
		ActuatorAction<Long> signalNight = new ActuatorAction<Long>() {
			@Override
			public void doAction(Long signalTime) {
				// signalTime: Time to signal approaching night
				// TODO: Implement <nightTimeSignalEvent>: long vibrate

				Runnable nightTimeSignalEvent = new Runnable() {
					@Override
					public void run() {
						Night=1;
						Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
						runOnUiThread(resetVotes);
						v.vibrate(2000);

					}
				};
				mafiaRuntime.scheduleAt(nightTimeSignalEvent, signalTime);
			}
		};
		mafiaRuntime.getRewriteMachine().setSignalNightActuator(signalNight);

		ActuatorAction<Long> signalDay = new ActuatorAction<Long>() {
			@Override
			public void doAction(Long signalTime) {
				// signalTime: Time to signal approaching day
				// TODO: Implement <dayTimeSignalEvent>: long vibrate
				Runnable dayTimeSignalEvent = new Runnable() {
					@Override
					public void run() {
						Night=0;
						Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
						runOnUiThread(resetVotes);
						v.vibrate(2000);
					}
				};
				mafiaRuntime.scheduleAt(dayTimeSignalEvent, signalTime);
			}
		};
		mafiaRuntime.getRewriteMachine().setSignalDayActuator(signalDay);

		ActuatorAction<Long> wakeMafia = new ActuatorAction<Long>() {
			@Override
			public void doAction(Long wakeTime) {
				// signalTime: Time to wake mafia
				// TODO: Implement <wakeMafiaEvent>: long vibrate + UI display
				Runnable wakeMafiaEvent = new Runnable() {
					@Override
					public void run() {
						Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
						v.vibrate(2000);
						mafiaRuntime.postAlert("Wake up mafia","Time to be devious");
					}
				};
				mafiaRuntime.scheduleAt(wakeMafiaEvent, wakeTime);
			}
		};
		mafiaRuntime.getRewriteMachine().setWakeMafiaActuator(wakeMafia);

		ActuatorAction<Unit> notifyMarked= new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit unit) {
				Runnable mark = new Runnable() {
					@Override
					public void run() {
						mafiaRuntime.postAlert("You've been marked","the mafiosos are coming for you!\n They've marked you as a high value target on the first turn.");
					}
				};
				mafiaRuntime.scheduleAt(mark,0);
			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyMarkedActuator(notifyMarked);

		ActuatorAction<Tuple3<SimpMultiset<Integer>,Long,Boolean>> notifyCheckVotes = new ActuatorAction<Tuple3<SimpMultiset<Integer>,Long,Boolean>>() {
			@Override
			public void doAction(Tuple3<SimpMultiset<Integer>, Long, Boolean> input) {
				if(mafiaRuntime.isOwner()){
					final SimpMultiset<Integer> votingLocs = input.t1;
					long voteCheckingTime = input.t2;
					final Boolean isFirst= input.t3;
					Runnable checkVoteEvent=new Runnable() {
						@Override
						public void run() {
							ArrayList<Integer> votedFors=new ArrayList<Integer>();
							for (int i = 0; i < votes.size(); i++) {
								if (votingLocs.contains(votes.get(i).location)) {
									if(votes.get(i).votedForLoc!=-1) {
										votedFors.add(votes.get(i).votedForLoc);
									}
								}
							}
							int currentMax = -1;
							VoteSlot currentVote = null;
							for(int i=0;i<votedFors.size();i++){
								VoteSlot currentSlot=votes.get(getSlotIndexByLoc(votedFors.get(i)));
								if(currentSlot.numberOfVotes>=currentMax){
									currentVote=currentSlot;
									currentMax=currentSlot.numberOfVotes;
								}
							}
							if (isFirst) {
								markPlayer(currentVote.location);
							} else {
								killPlayer(currentVote.location);
							}
						}
					};
					mafiaRuntime.scheduleAt(checkVoteEvent, voteCheckingTime);
				}

			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyCheckVotesActuator(notifyCheckVotes);
		ActuatorAction<Integer> notifyDeath = new ActuatorAction<Integer>() {
			@Override
			public void doAction(final Integer killedLoc) {
				// killedLoc Location that was nominated to be killed.
				//fix stupid
				// TODO: Notify this activity of the death of <killedLoc>
				Runnable notifyDeath = new Runnable() {
					@Override
					public void run() {
						if(mafiaRuntime.getLocation()==killedLoc){
							String Message="You've been murdered.";
							dead=true;
							mafiaRuntime.postAlert("You are dead",Message);
						}else {
							String nameOfKilled="";
							for(int i=0;i<votes.size();i++){ //find the name of the person killed for announcement.
								if(votes.get(i).location==killedLoc){
									nameOfKilled=votes.get(i).name;
								}
							}
							mafiaRuntime.postAlert(nameOfKilled+" has died!",nameOfKilled+" has been killed.");
						}
						removeFromDisplay(killedLoc);//removing the location from display
					}
				};
				runOnUiThread(notifyDeath);
			}


		};
		mafiaRuntime.getRewriteMachine().setNotifyDeathActuator(notifyDeath);

		ActuatorAction<Unit> notifyMafiaWin = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				dead=true;
				runOnUiThread(resetVotes);
				mafiaRuntime.postAlert("Mafia wins!","The Mafias have eliminated more Citizens than the number of Mafiosos in the game!");

			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyMafiaWinActuator(notifyMafiaWin);

		ActuatorAction<Unit> notifyCitizensWin = new ActuatorAction<Unit>() {
			@Override
			public void doAction(Unit input) {
				dead=true;
				runOnUiThread(resetVotes);//resetting votes.
				mafiaRuntime.postAlert("Citizens win!","The Mafias have been eliminated!");
			}
		};
		mafiaRuntime.getRewriteMachine().setNotifyCitizensWinActuator(notifyCitizensWin);

		mafiaRuntime.startRewriteMachine();

		if(mafiaRuntime.isRewriteReady() && mafiaRuntime.isOwner()) {
			this.setMenuItemVisibility(R.id.action_start, true);
		}
		if(mafiaRuntime.isRewriteReady()) {
			mafiaRuntime.getRewriteMachine().init();
			mafiaRuntime.initTimeServices(handler);
		}

	}



	//supplementary functions
	private void removeFromDisplay(Integer killedLoc) {//ONLY RUN ON UI THREAD
		int killedIndex=getSlotIndexByLoc(killedLoc);
		votes.remove(killedIndex);
		arrayAdapter ad=new arrayAdapter(act,R.layout.array_adapter,R.id.name,R.id.Votes,votes);
		ListView lv=(ListView)findViewById(R.id.CombinedVotes);
		lv.setAdapter(ad);
	}
	private int getSlotIndexByLoc(int Loc){ //Safe (will not return -1 unexpectedly)
		for(int i=0;i<votes.size();i++){
			if(votes.get(i).location==Loc){
				return i;
			}
		}
		return -1;
	}
	// Trigger methods
	// Initialize the game. Only group owner / moderator should be allowed to run this.
	protected void initializeGame() {
		SimpMultiset<Integer> allPlayerLocs = Misc.to_mset( mafiaRuntime.getDirectory().getLocations() ) ;
		int duration = LENGTH_OF_CYCLES_SECS;
		mafiaRuntime.getRewriteMachine().addInitialize(allPlayerLocs, duration);
	}

	// Start the game. Only group owner / moderator should be allowed to run this.
	protected void startGame() {
		if(mafiaRuntime.isOwner()) {
			mafiaRuntime.getRewriteMachine().addStart();
		}
	}

	protected void markPlayer(int chosenLoc){
		if(mafiaRuntime.isOwner()) {
			mafiaRuntime.getRewriteMachine().addMark(chosenLoc);
		}
	}
	// Kill a player. Only group owner / moderator should be allowed to run this.
	protected void killPlayer(int chosenLoc) {
		if(mafiaRuntime.isOwner()) {
			mafiaRuntime.getRewriteMachine().addKill(chosenLoc);
		}
	}

	// This activity chosens <chosenLoc> to be murdered. Only a mafia should be allowed to run this.
	protected void mafiaChangedVote(int chosenLoc) {
			mafiaRuntime.getRewriteMachine().addMafiaChangedVote(chosenLoc);
	}

	// This activity chosens <chosenLoc> to be executed. Only a citizen should be allowed to run this.
	protected void citizenChangedVote(int chosenLoc) {
		mafiaRuntime.getRewriteMachine().addCitizenChangedVote(chosenLoc);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mafia, menu);

		options_menu = menu;
		return true;
	}

	public void setMenuItemVisibility(int id, boolean visible) {
		if (options_menu == null) {
			String msg = "Option menu is null";
			return;
		}
		MenuItem item = options_menu.findItem(id);
		if (item != null) {
			item.setVisible(visible);
		} else {
			String msg = "Failed to find item " + id;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
			case R.id.action_settings: return true;
			case R.id.action_start:
				if(!started) {
					if (mafiaRuntime.isOwner()) {
						initializeGame();
						startGame();
						started=true;
					}
					return true;
				}
		}
		return super.onOptionsItemSelected(item);
	}

	protected void checkOffset() {
		(new Thread() {
			@Override
			public void run() {
				mafiaRuntime.postToast( String.format("Offset: %s", mafiaRuntime.getLocalTimeOffset()) );
			}
		}).start();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mafiaRuntime.resumeNetworkNotifications();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mafiaRuntime.pauseNetworkNotifications();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mafiaRuntime.close();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mafiaRuntime.handleOnActivityResults(requestCode);
	}

}
