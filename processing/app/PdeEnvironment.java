public interface PdeEnvironment {
  // stop the currently running thread (called by gui and others)
  public void terminate();

  // error being reported to gui (called by dbn)
  public void error(PdeException e);

  // successful finish reported to gui (called by dbn)
  public void finished();

  // message to write to gui (called by dbn)
  public void message(String msg);
}
