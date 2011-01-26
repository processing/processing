// this is a really straightforward effect that just reverses the order of the samples it receives
// it doesn't sound like how you think ;-)
class ReverseEffect implements AudioEffect
{
  void process(float[] samp)
  {
    float[] reversed = new float[samp.length];
    int i = samp.length - 1;
    for (int j = 0; j < reversed.length; i--, j++)
    {
      reversed[j] = samp[i];
    }
    // we have to copy the values back into samp for this to work
    arraycopy(reversed, samp);
  }
  
  void process(float[] left, float[] right)
  {
    process(left);
    process(right);
  }
}
  
