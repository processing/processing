package processing.sound;

import processing.core.PApplet;

public class LowPass implements SoundObject{
	
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1, -1};
	private float m_freq = 100;
	
	public LowPass(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void process(SoundObject input, float freq){
		m_freq=freq;
		m_nodeId = m_engine.lowPassPlay(input.returnId(), m_freq);
	}
	
	public void process(SoundObject input){
		m_nodeId = m_engine.lowPassPlay(input.returnId(), m_freq);
	}
	
	private void set(){
		m_engine.filterSet(m_freq, m_nodeId[0]);
	}
	
	public void set(float freq){
		m_freq=freq; 
		this.set();
	}
	
	public void freq(float freq){
		m_freq=freq;
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
