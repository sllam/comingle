package comingle.mobile.ui;

import comingle.mobile.ui.fileselector.*;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;

import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

import android.view.View;
import android.app.Activity;
import android.widget.Button;

import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.IntentFilter;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import comingle.facts.Fact;
import comingle.rewrite.RewriteMachine;
import comingle.rewrite.QuiescenceEvent;
import comingle.rewrite.QuiescenceListener;
import comingle.rewrite.StopEvent;
import comingle.rewrite.StopListener;

public class CoMingleUI extends FragmentActivity implements ActionBar.TabListener {

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	private UISoup<Fact> uisoup;
	private AlternativeView alt_view;
	private RewriteMachine rm;

	private final IntentFilter intentFilter = new IntentFilter();

	private Channel mChannel;
	private WifiP2pManager mManager;

	private boolean loaded;
	private boolean started;
	private boolean paused;

	private ActionBar actionBar;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		loaded  = false;
		started = false;
		paused  = false;

		// Set up the action bar.
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		loadRewriteMachineDialog();

		// Initialization stuff for P2P WiFi
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		// Indicates a change in the list of available peers.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		// Indicates the state of Wi-Fi P2P connectivity has changed.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		// Indicates this device's details have changed.
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);

	}

	protected void loadRewriteMachineDialog() {

		final String[] mFileFilter = { "*.*", "*.jar" };

		final CoMingleUI self = this;
		OnHandleFileListener mLoadFileListener = new OnHandleFileListener() {
			@Override
			public void handleFile(final String filePath) {	 
				if(self.loadRewriteMachine(filePath)) {
					self.initRewriteMachine();
					self.refreshDisplays();
				}
			}
		};

		FileSelector fileSelect = new FileSelector(this, FileOperation.LOAD, mLoadFileListener, mFileFilter, "Select a CoMingle Program");
		fileSelect.show();

	}

	public void initRewriteMachine() {

		uisoup = new UISoup<Fact>();

		uisoup.registerRewriteMachine( rm );

		final CoMingleUI self = this;
		rm.addPersistentQuiescenceListener(
			new QuiescenceListener() {
				public void performQuiescenceAction(QuiescenceEvent qe) {
					self.runOnUiThread(
						new Runnable() {
							@Override
							public void run() { self.refreshDisplays(); }
						});
				}
			}
		);
		rm.addPersistentStopListener(
			new StopListener() {
				public void performStopAction(StopEvent qe) {
					self.runOnUiThread(
						new Runnable() {
							@Override
							public void run() { 
								self.finalizeStopRewrite();
							}
						});
				}
			}
		);

		rm.init();

	        // For each of the sections in the app, add a tab to the action bar.
		for(int i=0; i<uisoup.size(); i++) {
			actionBar.addTab(actionBar.newTab().setText(uisoup.getCategory(i)).setTabListener(this));
		}

	        actionBar.addTab(actionBar.newTab().setText("All").setTabListener(this));
	}

	public boolean loadRewriteMachine(final String rmPath) {

		if (loaded) {
			actionBar.removeAllTabs();
			rm.terminate_rewrite();
			paused = false;
			started = false;
			Button button = (Button) findViewById(R.id.rewrite_toggle);
			button.setText("Start");
			button.setClickable(true);
		}

		File f = new File(rmPath);
		final File optimizedDexOutputPath = getDir("outdex", 0);
		DexClassLoader classLoader = new DexClassLoader(f.getAbsolutePath(),
		optimizedDexOutputPath.getAbsolutePath(),null, getClassLoader());

		String[] pathcomps  = rmPath.split("/");
		String package_name = pathcomps[pathcomps.length-1].replaceAll(".jar","");
		char first        = Character.toUpperCase(package_name.charAt(0));
		String class_name = first + package_name.substring(1);

		String completeClassName = String.format("%s.%s", package_name, class_name); // "merger.Merger";

		boolean succ = false;
		String msg = "";

		try {
			Class<?> rm_class = classLoader.loadClass(completeClassName);
			rm = (RewriteMachine) rm_class.newInstance();
			msg += String.format("Successfully loaded: %s", completeClassName);
			succ = true;
			loaded = true;
		} catch (Exception e) { msg += String.format("Error occurred: %s", e.toString()); }

	        AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Load a Rewrite Machine..");
		alert.setMessage( msg );
		alert.setPositiveButton("OK", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
		});
		alert.show();

		return succ;

	}

	public void refreshDisplays() { 
		uisoup.refresh(); 
		if(alt_view != null) {
			alt_view.refresh();
		}
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_reload:
				loadRewriteMachineDialog();
				return true;
			case R.id.action_settings:
				// openSettings();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) { }

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		//extracting fragment depending on the tab clicked
		if (tab.getPosition()<uisoup.size()) {
			int i = tab.getPosition();
			ListFragment lfrag = uisoup.getFragment(i);
			getSupportFragmentManager().beginTransaction().replace(R.id.container, lfrag).commit();
		} else {
			alt_view = new AlternativeView(uisoup);
			getSupportFragmentManager().beginTransaction().replace(R.id.container, alt_view).commit();
		}
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) { }

	public void startRewrite(View view) {

		Button button = (Button) findViewById(R.id.rewrite_toggle);
		if(!paused) {
			if(!started) {
				rm.start();
				started = true;
			} else {
				rm.restart_rewrite();
			}
			paused = true;
			button.setText("Pause");
		} else {
			rm.stop_rewrite();
			button.setText("Stopping..");
			button.setClickable(false);
		}

	}

	public void finalizeStopRewrite() {
		Button button = (Button) findViewById(R.id.rewrite_toggle);
		paused = false;
		button.setText("Start");
		button.setClickable(true);
	}

	public void stopRewrite(View view) { 
		if(started) {
			rm.stop_rewrite();
			started = false;
		}
	}

}
