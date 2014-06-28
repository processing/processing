package processing.sound;

import processing.core.PApplet;

public class Delay implements SoundObject{
	
	PApplet parent;
	private MethClaInterface m_engine;
	private int m_nodeId;
	private int[] m_m = {0,0};
	private float m_maxDelayTime = 2;
	private float m_delayTime = 0;
	private float m_feedBack = 0;
	
	public Delay(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine = new MethClaInterface();		
	}
	
	public void process(SoundObject input, float maxDelayTime, float delayTime, float feedBack){
		m_maxDelayTime=maxDelayTime; m_delayTime=delayTime; m_feedBack=feedBack;
		m_nodeId = m_engine.delayPlay(input.returnId(), m_maxDelayTime, m_delayTime, m_feedBack);
	}
	
	public void process(SoundObject input, float maxDelayTime, float delayTime){
		m_maxDelayTime=maxDelayTime; m_delayTime=delayTime; 
		m_nodeId = m_engine.delayPlay(input.returnId(), m_maxDelayTime, m_delayTime, m_feedBack);
	}

	public void process(SoundObject input, float maxDelayTime){
		m_maxDelayTime=maxDelayTime; 
		m_nodeId = m_engine.delayPlay(input.returnId(), m_maxDelayTime, m_delayTime, m_feedBack);
	}
	
	private void set(){
		m_engine.delaySet(m_delayTime, m_feedBack, m_nodeId);
	}
	
	public void set(float delayTime, float feedBack){
		m_delayTime=delayTime; m_feedBack=feedBack;
		this.set();
	}
	
	public void time(float delayTime){
		m_delayTime=delayTime;
		this.set();
	}

	public void feedback(float feedBack){
		m_feedBack=feedBack;
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
