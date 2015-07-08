package comingle.runtime;

import comingle.rewrite.RewriteMachine;

public interface RewriteMachineOperation<RW extends RewriteMachine, T> {

	public void doAction(RW rewriteMachine, T input);
	
}
