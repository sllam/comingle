package comingle.mobile.ui;

import java.util.List;
import java.util.LinkedList;

import android.support.v4.app.ListFragment;

import comingle.rewrite.RewriteMachine;
import comingle.goals.Goals;
import comingle.store.Store;
import comingle.facts.Fact;

public class UISoup<F extends Fact> {

	private List<UIFragment<F>> uisoup;

	public UISoup() {
		uisoup = new LinkedList<UIFragment<F>>();
	}

	public void registerRewriteMachine(RewriteMachine rm) {
		add( rm.getGoals() );
		LinkedList<Store> stores = rm.getStores();
		for(int i=0; i<stores.size(); i++) {
			add( stores.get(i) );
		}

	}

	public String getCategory(int index) {
		return uisoup.get(index).getName();
	}

	public UIFragment<F> getFragment(int index) {
		return uisoup.get(index);
	} 

	public void add(Goals goals) {
		uisoup.add( (UIFragment) new UIGoalsFragment(goals) );
	}

	public void add(Store<F> store) {
		uisoup.add( (UIFragment) new UIStoreFragment<F>(store) );
	}

	public int size() {
		return uisoup.size();
	}

	public void refresh() {
		for(int x=0; x<uisoup.size(); x++) {
			uisoup.get(x).refresh();
		}
	}

} 
