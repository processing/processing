package processing.sound;

import processing.core.PApplet;

public class HPF implements SoundObject{
	
	PApplet parent;
	private MethClaInterface m_engine;
	private int m_nodeId;
	private int[] m_m = {0,0};
	private float m_freq = 100;
	private float m_res = 1;
	
	public HPF(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine = new MethClaInterface();		
	}
	
	public void play(SoundObject input, float freq, float res){
		m_freq=freq; m_res=res;
		m_nodeId = m_engine.highPassPlay(input.returnId(), m_freq, m_res);
	}
	
	public void play(SoundObject input, float freq){
		m_freq=freq;
		m_nodeId = m_engine.highPassPlay(input.returnId(), m_freq, m_res);
	}
	
	private void set(){
		m_engine.filterSet(m_freq, m_res, m_nodeId);
	}
	
	public void set(float freq, float res){
		m_freq=freq; m_res=res;
		this.set();
	}
	
	public void freq(float freq){
		m_freq=freq;
		this.set();
	}

	public void res(float res){
		m_res=res;
		this.set();
	}
	
	public int[] returnId(){
		return m_m;
	}
	
	public void stop(){
		//m_engine.synthStop(m_nodeId);
	}

	public void dispose() {
		//m_engine.synthStop(m_nodeId);
	}
}
