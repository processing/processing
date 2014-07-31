package processing.sound;
import processing.core.*;

public class SawOsc implements Oscillator{
	
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1,-1};
	
	private int[] m_freqId = {-1,-1};
	private int[] m_ampId = {-1,-1};
	private int[] m_addId = {-1,-1};
	private int[] m_posId = {-1,-1};
		
	private float m_freq = 440;
	private float m_amp = 0.5f;
	private float m_add = 0;
	private float m_pos = 0;
	
	public SawOsc(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();		
	}
	
	public void play(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(float freq, float amp, float add){
		m_freq=freq; m_amp=amp; m_add=add;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(float freq, float amp){
		m_freq=freq; m_amp=amp;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(){
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	private void set(){
		if(m_nodeId[0] != -1 ) {
			m_engine.oscSet(m_freq, m_amp, m_add, m_pos, m_nodeId);
		}	
	}

	private void audioSet(){
		if(m_nodeId[0] != -1 ) {
			m_engine.oscAudioSet(m_freqId, m_ampId, m_addId, m_posId, m_nodeId);
		}	
	}
	
	public void set(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		this.set();
	}
	
	public void freq(float freq){
		m_freq=freq;
		this.set();
	}

	public void freq(SoundObject freq){
		m_freqId=freq.returnId();
		this.audioSet();
	}	

	public void amp(float amp){
		m_amp=amp;
		this.set();
	}

	public void amp(SoundObject amp){
		m_ampId=amp.returnId();
		this.audioSet();
	}	
	
	public void add(float add){
		m_add=add;
		this.set();
	}
	
	public void add(SoundObject add){
		m_addId=add.returnId();
		this.audioSet();
	}	

	public void pan(float pos){
		m_pos=pos;
		this.set();
	}
	
	public void pan(SoundObject pos){
		m_posId=pos.returnId();
		this.audioSet();
	}

	public void stop(){
		m_engine.synthStop(m_nodeId);
	}
	
	public int[] returnId(){
		return m_nodeId;
	}

	public void dispose() {
		m_engine.synthStop(m_nodeId);
	}
}
