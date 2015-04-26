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

package comingle.actuation;

import java.util.HashMap;

public class Actuators {

	public static String ACT_DELAY     = "delay";
	public static String ACT_BEEP_TONE = "beep";
	public static String ACT_TOAST     = "toast";
	
	protected HashMap<String,ActuatorAction> actuators;
	
	public Actuators() {
		actuators = new HashMap<String,ActuatorAction>();
	}
	
	public void setActuator(String act_name, ActuatorAction action) {
		actuators.put(act_name, action);
	}
	
	public <T> boolean invokeActuator(String act_name, T input) {
		if( actuators.containsKey(act_name) ) {
			ActuatorAction<T> actuator = (ActuatorAction<T>) actuators.get(act_name);
			actuator.doAction(input);
			return true;
		} else {
			return false;
		}
	}
	
	public void setDelayActuator(ActuatorAction<Integer> delay) {
		actuators.put(ACT_DELAY, delay);
	}
	
	public boolean delay(int milliseconds) {
		return this.invokeActuator(ACT_DELAY, milliseconds);
	}
	
	public void setBeepToneActuator(ActuatorAction<String> beeptone) {
		actuators.put(ACT_BEEP_TONE, beeptone);
	}
	
	public boolean beepTone(String tone) {
		return this.invokeActuator(ACT_BEEP_TONE, tone);
	}
	
	public void setToastActuator(ActuatorAction<String> toast) {
		actuators.put(ACT_TOAST, toast);
	}
	
	public boolean toast(String msg) {
		return this.invokeActuator(ACT_TOAST, msg);
	}
	
}
