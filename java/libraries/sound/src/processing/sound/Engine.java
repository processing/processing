/**
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##Wilm Thoben##
 * 
 */

package processing.sound;
import processing.core.PApplet;

public class Engine {

	private static PApplet parent;
	static MethClaInterface methCla;
	private static int m_sampleRate=44100;
	private static int m_bufferSize=512;

    private Engine() {
    	welcome();
    	methCla = new MethClaInterface();
		methCla.engineNew(m_sampleRate, m_bufferSize);
		methCla.engineStart();
    }

    private static class LazyHolder {
        private static final Engine INSTANCE = new Engine();
    }
 
    public static Engine start() {
        return LazyHolder.INSTANCE;
    }

    public static void setPreferences(PApplet theParent, int bufferSize, int sampleRate){
    	parent = theParent;
    	m_bufferSize = bufferSize;
    	m_sampleRate = sampleRate;
    }
	 
	// general Synth methods  	  
	public static void synthStop(int[] nodeId){
		methCla.synthStop(nodeId);
	}	  
	  	  
	// general Oscillator methods
	    
	public static void oscSet(float freq, float amp, float add, float pos, int[] nodeId){
		methCla.oscSet(freq, amp, add, pos, nodeId);
	};
	  
	// Sine Wave Oscillator
	    
	public static int[] sinePlay(float freq, float amp, float add, float pos){
		return methCla.sinePlay(freq, amp, add, pos);
	};
	    
	//Saw Wave Oscillator
	  
	public static int[] sawPlay(float freq, float amp, float add, float pos){
		return methCla.sawPlay(freq, amp, add, pos);
	};

	//Square Wave Oscillator
	  
	public static int[] sqrPlay(float freq, float amp, float add, float pos){
		return methCla.sqrPlay(freq, amp, add, pos);
	};
	  
	public static void sqrSet(float freq, float amp, float add, float pos, int[] nodeId){
		methCla.sqrSet(freq, amp, add, pos, nodeId);
	}; 
	  
	// Triangle Wave Oscillator
	  
	public static int[] triPlay(float freq, float amp, float add, float pos){
		return methCla.triPlay(freq, amp, add, pos);
	};
	  
	public static int[] pulsePlay(float freq, float width, float amp, float add, float pos){
		return methCla.pulsePlay(freq, width, amp, add, pos);
	};
	  
	public static void pulseSet(float freq, float width, float amp, float add, float pos, int[] nodeId){
		methCla.pulseSet(freq, width, amp, add, pos, nodeId);
	}; 

	// AudioIn

	public static int[] audioInPlay(float amp, float add, float pos, boolean out){
		return methCla.audioInPlay(amp, add, pos, out);
	};

  	public static void audioInSet(float amp, float add, float pos, int[] nodeId){
  		methCla.audioInSet(amp, add, pos, nodeId);
  	};
	
	// SoundFile
	  
	public static int[] soundFileInfo(String path){
		return methCla.soundFileInfo(path);
	};
	    
	public static int[] soundFilePlayMono(float rate, float pos, float amp, float add, boolean loop, String path, float dur, int cue){
		return soundFilePlayMono(rate, pos, amp, add, loop, path, dur, cue);
	};
	  
	public static int[] soundFilePlayMulti(float rate, float amp, float add, boolean loop, String path, float dur, int cue){
		return methCla.soundFilePlayMulti(rate, amp, add, loop, path, dur, cue);
	};
	  
	public static void soundFileSetMono(float rate, float pos, float amp, float add, int[] nodeId){
		methCla.soundFileSetMono(rate, pos, amp, add, nodeId);
	};
	  
	public static void soundFileSetStereo(float rate, float amp, float add, int[] nodeId){
		methCla.soundFileSetStereo(rate, amp, add, nodeId);
	};  
	  
	// White Noise
	  
	public static int[] whiteNoisePlay(float amp, float add, float pos){
		return methCla.whiteNoisePlay(amp, add, pos);
	};
	  
	public static void whiteNoiseSet(float amp, float add, float pos, int[] nodeId){
		methCla.whiteNoiseSet(amp, add, pos, nodeId);	
	};

  	// Pink Noise
  
  	public static int[] pinkNoisePlay(float amp, float add, float pos){
  		return methCla.pinkNoisePlay(amp, add, pos);
  	};
  
  	public static void pinkNoiseSet(float amp, float add, float pos, int[] nodeId){
  		methCla.pinkNoiseSet(amp, add, pos, nodeId);
  	};
	  
  	// Brown Noise
    
  	public static int[] brownNoisePlay(float amp, float add, float pos){
  		return methCla.brownNoisePlay(amp, add, pos);
  	};
    
  	public static void brownNoiseSet(float amp, float add, float pos, int[] nodeId){
  		methCla.brownNoiseSet(amp, add, pos, nodeId);
  	};
    
	// Envelope
	    
	public static int envelopePlay(int[] input, float attackTime, float sustainTime, float sustainLevel, float releaseTime){
		return methCla.envelopePlay(input, attackTime, sustainTime, sustainLevel, releaseTime);
	};
	  
	public static int doneAfter(float seconds){
		return methCla.doneAfter(seconds);
	};
	    
	// Filters
	    
	public static int highPassPlay(int[] input, float freq, float res){
		return methCla.highPassPlay(input, freq, res);
	};

	public static int lowPassPlay(int[] input, float freq, float res){
		return methCla.lowPassPlay(input, freq, res);
	};
	  
	public static int bandPassPlay(int[] input, float freq, float res){
		return methCla.bandPassPlay(input, freq, res);
	};

	public static void filterSet(float freq, float res, int nodeId){
		methCla.filterSet(freq, res, nodeId);
	};

	// Delay

	public static int delayPlay(int[] input, float maxDelayTime, float delayTime, float feedBack){
		return methCla.delayPlay(input, maxDelayTime, delayTime, feedBack);
	};
	  
	public static void delaySet(float delayTime, float feedBack, int nodeId){
		methCla.delaySet(delayTime, feedBack, nodeId);
	};
	  
	// Amplitude Follower
	  
	public static long amplitude(int[] nodeId){
		return methCla.amplitude(nodeId);
	};
	  
	public static float poll_amplitude(long ptr){
		return methCla.poll_amplitude(ptr);
	};
	  
	public static void destroy_amplitude(long ptr){
		methCla.destroy_amplitude(ptr);
	};
	  
	// FFT
	  
	public static long fft(int[] nodeId, int fftSize){
		return methCla.fft(nodeId, fftSize);
	};
	  
	public static float[] poll_fft(long ptr){
		return methCla.poll_fft(ptr);
	};  
	  
	public static void destroy_fft(long ptr){
		methCla.destroy_fft(ptr);
	};

	public static void engineStop() {
		methCla.engineStop();
	}
	
	public void dispose() {
		methCla.engineStop();
	}

 	private void welcome() {
		System.out.println("processing.sound v.09 by Wilm Thoben");
	}
}