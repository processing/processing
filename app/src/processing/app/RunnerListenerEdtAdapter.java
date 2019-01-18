package processing.app;

import java.awt.EventQueue;

public class RunnerListenerEdtAdapter implements RunnerListener {

  private RunnerListener wrapped;

  public RunnerListenerEdtAdapter(RunnerListener wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public void statusError(String message) {
    EventQueue.invokeLater(() -> wrapped.statusError(message));
  }

  @Override
  public void statusError(Exception exception) {
    EventQueue.invokeLater(() -> wrapped.statusError(exception));
  }

  @Override
  public void statusNotice(String message) {
    EventQueue.invokeLater(() -> wrapped.statusNotice(message));
  }

  @Override
  public void startIndeterminate() {
    EventQueue.invokeLater(() -> wrapped.startIndeterminate());
  }

  @Override
  public void stopIndeterminate() {
    EventQueue.invokeLater(() -> wrapped.stopIndeterminate());
  }

  @Override
  public void statusHalt() {
    EventQueue.invokeLater(() -> wrapped.statusHalt());
  }

  @Override
  public boolean isHalted() {
    return wrapped.isHalted();
  }
}

