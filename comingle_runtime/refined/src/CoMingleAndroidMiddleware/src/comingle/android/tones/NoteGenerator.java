package comingle.android.tones;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

public class NoteGenerator {
	// private final int duration = 1; // seconds
	private final int sampleRate = 8000;
	// private final int numSamples = duration * sampleRate;
	
	private static String TAG = "NOTE_GEN";
	
	private Map<String,Double> noteArchive;
	private Map<String,byte[]> toneArchive;
	
	public NoteGenerator() {
		noteArchive = new HashMap<String,Double>();
		toneArchive = new HashMap<String,byte[]>();
		initNoteArchive();
	}
	
	public NoteGenerator(int duration, double[] freqOfTones) {
		this();
		for(double freqOfTone: freqOfTones) {
			genTone(duration, freqOfTone);
		}
	}
	
	public NoteGenerator(int[] durations, double[] freqOfTones) {
		this();
		for(int duration: durations) {
			for(double freqOfTone: freqOfTones) {
				genTone(duration, freqOfTone);
			}
		}
	}
	
	private void initNoteArchive() {
		noteArchive.put("A3",  220d);
		noteArchive.put("A#3", 233.08);
		noteArchive.put("B3",  246.94);
		noteArchive.put("C4",  261.63);
		noteArchive.put("C#4", 277.18);
		noteArchive.put("D4",  293.66);
		noteArchive.put("D#4", 311.13);
		noteArchive.put("E4",  329.63);
		noteArchive.put("F4",  349.23);
		noteArchive.put("F#4", 369.99);
		noteArchive.put("G4",  392d);
		noteArchive.put("G#4", 415.3);
		
		noteArchive.put("A4",  440d);
		noteArchive.put("A#4", 466.16);
		noteArchive.put("B4",  493.88);
		noteArchive.put("C5",  523.26);
		noteArchive.put("C#5", 554.36);
		noteArchive.put("D5",  587.32);
		noteArchive.put("D#5", 622.26);
		noteArchive.put("E5",  659.26);
		noteArchive.put("F5",  698.46);
		noteArchive.put("F#5", 739.98);
		noteArchive.put("G5",  784d);
		noteArchive.put("G#5", 830.6);
		
		noteArchive.put("A5",  880d);
		noteArchive.put("A#5", 932.32);
		noteArchive.put("B5",  987.76);
		noteArchive.put("C6",  1046.52);
		noteArchive.put("C#6", 1108.72);
		noteArchive.put("D6",  1174.64);
		noteArchive.put("D#6", 1244.52);
		noteArchive.put("E6",  1318.52);
		noteArchive.put("F6",  1396.92);
		noteArchive.put("F#6", 1479.96);
		noteArchive.put("G6",  1568d);
		noteArchive.put("G#6", 1661.2);
	}
	
	private Double getNoteToneFreq(String note) {
		if(noteArchive.containsKey(note)) {
			return noteArchive.get(note);
		} else {
			return null;
		}
	}
	
	private String getToneIndex(int duration, double freqOfTone) {
		return String.format("%s:%s", duration, Double.toHexString(freqOfTone));
	}
	
	/*
    private void genTone(int duration, double freqOfTone){
    	int numSamples = duration * sampleRate;
    	
        // fill out the array    	
    	final double[] sample = new double[numSamples];
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/freqOfTone));
        }

    	final byte generatedSnd[] = new byte[2 * numSamples];
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        
        toneArchive.put(getToneIndex(duration, freqOfTone), generatedSnd);
    } */

    private void genTone(int duration, double freqOfTone){
    	
    	double dnumSamples = duration * sampleRate;
    	dnumSamples = Math.ceil(dnumSamples);
    	int numSamples = (int) dnumSamples;
    	double sample[] = new double[numSamples];
    	byte generatedSnd[] = new byte[2 * numSamples];

    	for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
          sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
        }
    	
    	// convert to 16 bit pcm sound array
    	// assumes the sample buffer is normalised.
    	int idx = 0;
    	int i = 0 ;

    	int ramp = numSamples / 20 ;                                    // Amplitude ramp as a percent of sample count


    	for (i = 0; i< ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
    	   double dVal = sample[i];
    	                                                                    // Ramp up to maximum
    	   final short val = (short) ((dVal * 32767 * i/ramp));
    	                                                                    // in 16 bit wav PCM, first byte is the low order byte
    	   generatedSnd[idx++] = (byte) (val & 0x00ff);
    	   generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
    	}


    	for (i = i; i< numSamples - ramp; ++i) {                        // Max amplitude for most of the samples
    	   double dVal = sample[i];
    	                                                                    // scale to maximum amplitude
    	   final short val = (short) ((dVal * 32767));
    	                                                                    // in 16 bit wav PCM, first byte is the low order byte
    	   generatedSnd[idx++] = (byte) (val & 0x00ff);
    	   generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
    	}

    	for (i = i; i< numSamples; ++i) {                               // Ramp amplitude down
    	   double dVal = sample[i];
    	                                                                    // Ramp down to zero
    	   final short val = (short) ((dVal * 32767 * (numSamples-i)/ramp ));
    	                                                                    // in 16 bit wav PCM, first byte is the low order byte
    	   generatedSnd[idx++] = (byte) (val & 0x00ff);
    	   generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
    	}
        
        toneArchive.put(getToneIndex(duration, freqOfTone), generatedSnd);
    }
	
    private void playNote(byte[] generatedSnd){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 
                generatedSnd.length, // AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
        Log.i(TAG, String.format( "Played %s", generatedSnd ) );
    }
    
    public Runnable getPlayNoteAction(int duration, double freqOfTone) {
    	Log.i(TAG, String.format( "Generating tone frequency %s for %s s", freqOfTone, duration ));
    	String toneIdx = getToneIndex(duration, freqOfTone);
    	if(!toneArchive.containsKey(toneIdx)) {
    		genTone(duration, freqOfTone);
    	}
    	final byte[] generatedSnd = toneArchive.get(toneIdx);
    	
    	Runnable action = new Runnable() {
    		@Override
    		public void run() {
    			playNote(generatedSnd);
    		}
    	};
    	
    	return action;
    }
    
    public Runnable getPlayNoteAction(int duration, String note) {
    	Double freqOfTone = getNoteToneFreq(note);
    	if (freqOfTone != null) {
    		return getPlayNoteAction(duration, freqOfTone);
    	} else {
    		return null;
    	}
    }
    
    private class Note {
    	
    	public int duration;
    	public double freqOfTone;
    	
    	public Note(int duration, double freqOfTone) {
    		this.duration = duration;
    		this.freqOfTone = freqOfTone;
    	}
    	
    }
    
    private List<Note> parseNoteSequence(String noteSeq) {
    	List<Note> ls = new LinkedList<Note>();
    	StringTokenizer st = new StringTokenizer(noteSeq, " ");
    	while (st.hasMoreTokens()) {
    		String[] args = st.nextToken().split(":");
    		int duration = Integer.parseInt( args[0] );
    		double freqOfTone = noteArchive.get( args[1] );
    		ls.add(new Note(duration, freqOfTone));
    	}
    	return ls;
    }
    
    public class TimedSoundTask extends TimerTask {
    	
    	final Handler handler;
    	final Runnable soundAction;
    	
    	public TimedSoundTask(Handler handler, Runnable soundAction) {
    		this.handler = handler;
    		this.soundAction = soundAction;
    	}
    	
    	@Override
    	public void run() {
	        final Thread thread = new Thread(new Runnable() {
	            public void run() {
	                handler.post(soundAction);
	            }
	        });
	        thread.start();
    	}
    	
    }
    
    public void schedulePlayNoteSequence(Handler handler, Calendar playTime, String noteSeq) {
    	
	    Timer timer = new Timer();
    	
    	for(Note note: parseNoteSequence(noteSeq)) {
    		Runnable soundAction = getPlayNoteAction(note.duration, note.freqOfTone);
    		timer.schedule(new TimedSoundTask(handler, soundAction), playTime.getTime());
    		playTime.add(Calendar.SECOND, note.duration);
    	}
    	
    }
    
    public void schedulePlayNote(Handler handler, Calendar playTime, int duration, String note) {
    	
    	Timer timer = new Timer();
    	Runnable soundAction = getPlayNoteAction(duration, this.getNoteToneFreq(note));
    	timer.schedule(new TimedSoundTask(handler, soundAction), playTime.getTime());
    	
    }
    
    public void schedulePlayNote(Handler handler, long playTimeInMillis, int duration, String note) {
    	Timer timer = new Timer();
    	Runnable soundAction = getPlayNoteAction(duration, this.getNoteToneFreq(note));
    	timer.schedule(new TimedSoundTask(handler, soundAction), new Date(playTimeInMillis));
    }
    
}
