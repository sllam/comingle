package comingle.dragracing;

import java.util.LinkedList;

import comingle.mset.SimpMultiset;
import comingle.tuple.Tuple2;

public class RacerLib {

	public static final int SPEED = 600;
	
	public static int newPos(int pos, int time_start, int time_end) {
		int time_diff = time_end - time_start;
		return pos + (SPEED * time_diff / 1000);
	}
	
	public static <T> Tuple2<SimpMultiset<Tuple2<T,T>>,T> makeChain(T first, LinkedList<T> ls) {
		SimpMultiset<Tuple2<T,T>> new_ls = new SimpMultiset<Tuple2<T,T>>();
		for(int x=1; x<ls.size(); x++) {
			new_ls.addLast(new Tuple2<T,T>(ls.get(x-1),ls.get(x)));
		}
		T last = null;
		if(ls.size() > 0) {
			last = ls.getLast();
		}
		return new Tuple2<SimpMultiset<Tuple2<T,T>>,T>(new_ls,last);
	}
	
}
