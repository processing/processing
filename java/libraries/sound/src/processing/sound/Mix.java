package processing.sound;

import processing.core.PApplet;

public class Mix implements SoundObject{
	
	PApplet parent;
	private Engine m_engine;
	private int m_nodeId[] = {-1,-1};
	private int[] m_nodeInIds;
	private float[] m_amps;
	
	public Mix(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void play(SoundObject[] input, float[] amps){
		m_amps=amps; 
		for (int i=0; i<input.length; i++) {
			int[] returnId = input[i].returnId();
			m_nodeInIds[i*2]=returnId[0];
			m_nodeInIds[(i*2)+1]=returnId[1];			
		} 
		//m_nodeId = m_engine.mixPlay(input.length, m_nodeInIds, amps);
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
