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
	
}
