// -*- Mode: JDE; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 2 -*-

// Different instances of PdeMessageStream need to do different things with
// messages.  In particular, a stream instance used for parsing output from
// the compiler compiler has to interpret its messages differently than one
// parsing output from the runtime.
//
// Classes which consume messages and do something with them should implement
// this interface.
//
public interface PdeMessageConsumer {

  public void message(String s);

}
