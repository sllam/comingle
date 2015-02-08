package comingle.directory;

import java.util.List;

import com.example.wifidirectmiddlewaretest.R;

import p2pdirectory.WifiDirectComingleDirectory;

import sllam.extras.admin.NodeInfo;
import sllam.extras.directory.BaseDirectory;
import sllam.extras.ui.PeerInfoListFragment;
import sllam.extras.wifidirect.WifiDirectBroadcastReceiver;
import sllam.extras.wifidirect.listeners.DirectoryChangedListener;
import sllam.extras.wifidirect.listeners.LocalNodeInfoAvailableListener;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {

	private final static int ADMIN_PORT = 8988;
	private final static String REQ_CODE = "comingle.directory";
	
	private final IntentFilter intentFilter = new IntentFilter();
	private Channel mChannel;
	private WifiP2pManager mManager;
	private WifiDirectBroadcastReceiver mReceiver;
	private BaseDirectory wifiDir;
	private PeerInfoListFragment wifiDirList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
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
		
		// wifiDir = new WifiDirectDirectory(this, ADMIN_PORT, REQ_CODE);
		wifiDir = new WifiDirectComingleDirectory(this, ADMIN_PORT, REQ_CODE);
		
		
		wifiDir.addDefaultRoleEstablishedAction();
		wifiDir.addDefaultConnectionEstablishedAction();
		
		final MainActivity self = this;
		DirectoryChangedListener dir_listener = new DirectoryChangedListener() {
			@Override
			public void doDirectoryChangedAction(List<NodeInfo> peers, List<NodeInfo> added
					                            ,List<NodeInfo> dropped, int role) {
				self.refresh();	
			}
		};
		wifiDir.addDirectoryChangedListener(dir_listener);
		
		wifiDirList = new PeerInfoListFragment(R.layout.device_list, R.layout.row_devices, R.id.row_name
				                              ,R.id.row_location, R.id.row_ip, R.id.row_role
				                              ,R.id.my_name, R.id.my_location, R.id.my_ip 
				                              ,R.id.my_role, wifiDir);
		
		LocalNodeInfoAvailableListener node_listener = new LocalNodeInfoAvailableListener() {
			@Override
			public void doLocalNodeInfoAvailableAction(NodeInfo local, int role) {
				wifiDirList.refreshMyDevice();	
			}
		};
		wifiDir.addLocalNodeInfoAvailableListener(node_listener);
		
		getSupportFragmentManager().beginTransaction().replace(R.id.frag_list, wifiDirList).commit();
		
	}
	
	public void refresh() { 
		if (wifiDirList != null) {
			wifiDirList.refreshPeers();			
		}
	}
	
    @Override
    protected void onResume() {
    	super.onResume();
    	registerReceiverIfNeeded();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	unregisterReceiverIfNeeded();
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if(wifiDir != null) {
        	wifiDir.close();	
    	}
    }
	
	private void registerReceiverIfNeeded() {
		if (mReceiver == null) {
			mReceiver = new WifiDirectBroadcastReceiver(mManager, mChannel, this, wifiDir);
			registerReceiver(mReceiver, intentFilter);			
		}
	}
	
	private void unregisterReceiverIfNeeded() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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
		}
		return super.onOptionsItemSelected(item);
	}
}
