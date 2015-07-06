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

package comingle.comms.misc;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class RandGenerator {

	protected static Random rand = new Random();

	protected final static String DEFAULT_ALPHAS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	protected final static String DEFAULT_DIGITS = "1234567890";
	
	public static String randFrom(String str) {
		return String.format("%s", str.charAt( rand.nextInt(str.length()) ));
	}
	
	public static String randAlpha() {
		return randFrom(DEFAULT_ALPHAS);
	}
	
	public static String randDigit() {
		return randFrom(DEFAULT_DIGITS);
	}
	
	public static String randReqCode() {
		return String.format("%s%s%s%s%s", randAlpha(), randAlpha(), randDigit(), randAlpha(), randDigit());
	}
	
	public static long randLong() {
		return rand.nextLong();
	}
	
	public static <T> T randFrom(Collection<T> ts) {
		int randIdx = rand.nextInt(ts.size());
		Iterator<T> it = ts.iterator();
		T chosen = it.next();
		while(randIdx > 0) {
			chosen = it.next();
			randIdx--;
		}
		return chosen;
	}
	
	public static <T> T randFrom(Collection<T> ts, boolean remove) {
		int randIdx = rand.nextInt(ts.size());
		Iterator<T> it = ts.iterator();
		T chosen = it.next();
		while(randIdx > 0) {
			chosen = it.next();
			randIdx--;
		}
		if (remove) {
			ts.remove(chosen);
		}
		return chosen;
	}
	
	public static String randName() {
		int numOfWords = 2;
		String[] nameGrabBag = {
				"Super", "Larry", "John", "Uber", "Nuts", "Crap", "Funny", "Serious",
				"James", "Yoki", "Sally", "Spaz", "Dot", "Moo", "Rug", "Rex", "Silly",
				"Ox", "Or", "Roll", "Rock", "Paper", "Frag", "Smart", "Ping", "Pong",
				"Right", "Wrong", "First", "Second", "Third", "Happy", "Sad", "Lonely",
				"Max", "Mad", "Ok", "Fox", "Cat", "Robo", "Mojo", "Froyo", "Ice", "Fire",
				"Lame", "Quack", "Punch", "Rush", "Hack", "Argh", "Huh", "Duh", "Lol",
				"Dork", "Tool", "Rule", "Pawn", "King", "Queen", "Prince", "Price",
				"Not", "Nox", "Yo", "Hud", "Quick", "Pun", "Run", "Gun", "Mox", "Pox",
				"Lamb", "Ram", "Cool", "Mind", "Beans", "Pea", "Grab", "Bag", "Fax",
				"Log", "Pillow", "Jones", "Don", "Dan", "Sal", "Saul", "Sam", "Goof",
				"Duck", "Mick", "Mike", "Magic", "Misty", "Loop", "Pool", "Pop", "Pot",
				"Spoon", "Fork", "Razor", "Boot", "Random", "Hex", "Cook", "Fool", 
				"Axe", "Lock", "Wall", "Soft", "Hey", "Rookie", "Voom", "Doom", "Noon",
				"Soon", "Walk", "Jump", "Hop", "Stop", "Go", "Good", "Bad", "Nat", "Calm",
				"Nick", "Weak", "Dull", "Broke", "Hope", "Break", "Splat", "Lad", "Bane",
				"Kill", "Swing", "Wing", "Ding", "Dong", "Gone", "Ting", "Tong", "Hook",
				"Dart", "Jock", "Sack", "Luck", "Boo", "Sick", "Time", "Danger", "Even",
				"Kick", "Hide", "Top", "Down", "Mug", "Free", "Cosy", "Song", "Rite",
				"Back", "Hate", "Joker", "Quiet", "Hyper", "Best", "Worst", "Fact", 
				"Name", "Yes", "No", "Nope", "Yay", "Jail", "Box", "Teeth", "Zoo",
				"Hold", "Grab", "Kid", "Doc", "Zap", "Tap"
		};
		List<String> ls = new LinkedList<String>();
		for(String name: nameGrabBag) {
			ls.add(name);
		}
		String name = "";
		for(int x=0; x<numOfWords; x++) {
			name += randFrom(ls, true) + " ";
		}
		return name;
	}
	
}
