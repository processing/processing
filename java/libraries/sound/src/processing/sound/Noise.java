package processing.sound;
 
interface Noise extends SoundObject {

	public void play(float amp, float add, float pos);
	public void play(float amp, float pos);
	public void play(float amp);	
	public void play();

	public void amp(float amp);
	public void add(float add);
	public void pan(float pos);
	public void set();	
	public void set(float amp, float add, float pan);
	public void stop();
	public void dispose();
	
	public int[] returnId();
}
