package processing.app.tools.android;

public class AndroidProcess {
  public final String pid;
  public final String name;

  public AndroidProcess(final String pid, final String name) {
    if (pid == null) {
      throw new IllegalArgumentException("null pid");
    }
    if (name == null) {
      throw new IllegalArgumentException("null name");
    }
    this.pid = pid;
    this.name = name;
  }

  @Override
  public String toString() {
    return name + " (" + pid + ")";
  }

  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof AndroidProcess)) {
      return false;
    }
    final AndroidProcess o = (AndroidProcess) obj;
    return pid.equals(o.pid) && name.equals(o.name);
  }

  @Override
  public int hashCode() {
    return pid.hashCode() * 17 + name.hashCode();
  }
}
