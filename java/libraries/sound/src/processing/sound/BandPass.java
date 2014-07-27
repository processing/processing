package processing.sound;

import processing.core.PApplet;

public class BandPass implements SoundObject{
	
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1,-1};
	private float m_freq = 4000;
	private float m_bw = 1000;
	
	public BandPass(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void process(SoundObject input, float freq, float bw){
		m_freq=freq; m_bw=bw;
		m_nodeId = m_engine.bandPassPlay(input.returnId(), m_freq, m_bw);
	}
	
	public void process(SoundObject input, float freq){
		m_freq=freq;
		m_nodeId = m_engine.bandPassPlay(input.returnId(), m_freq, m_bw);
	}
	
	public void process(SoundObject input){
		m_nodeId = m_engine.bandPassPlay(input.returnId(), m_freq, m_bw);
	}

	private void set(){
		m_engine.filterBwSet(m_freq, m_bw, m_nodeId[0]);
	}
	
	public void set(float freq, float bw){
		m_freq=freq; m_bw=bw;
		this.set();
	}
	
	public void freq(float freq){
		m_freq=freq;
		this.set();
	}

	public void bw(float bw){
		m_bw=bw;
		this.set();
	}
	
	public int[] returnId(){
		return m_nodeId;
	}
	
	public void stop(){
		m_engine.synthStop(m_nodeId);
	}

	public void dispose() {
		m_engine.synthStop(m_nodeId);
	}
}
