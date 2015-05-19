package comingle.comms.misc;

public class MailBox<T> {

	protected T data;
	protected Barrier barrier;
	
	public MailBox() {
		data = null;
		barrier = new Barrier();
	}
	
	public T getData() {
		barrier.await();
		return data;
	}
	
	public void putData(T data) {
		this.data = data;
		barrier.release();
	}
	
	public void reset() {
		barrier.reset();
	}
	
}
