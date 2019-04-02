package processing.mode.java.preproc.issue.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Singleton with fallback error localizations.
 */
public class DefaultErrorLocalStrSet {

  private static final AtomicReference<DefaultErrorLocalStrSet> instance = new AtomicReference<>();

  private final Map<String, String> localizations = new HashMap<>();

  /**
   * Get shared copy of this singleton.
   *
   * @return Shared singleton copy.
   */
  public static DefaultErrorLocalStrSet get() {
    instance.compareAndSet(null, new DefaultErrorLocalStrSet());
    return instance.get();
  }

  /**
   * Private hidden constructor.
   */
  private DefaultErrorLocalStrSet() {
    localizations.put("editor.status.error", "Error");
    localizations.put("editor.status.error.syntax", "Syntax Error - %s");
    localizations.put("editor.status.bad.assignment", "Error on variable assignment near %s?");
    localizations.put("editor.status.bad.identifier", "Identifier cannot start with digits near %s?");
    localizations.put("editor.status.bad.parameter", "Error on parameter or method declaration near %s?");
    localizations.put("editor.status.extraneous", "Unexpected extra code near %s?");
    localizations.put("editor.status.mismatched", "Missing operator or semicolon near %s?");
    localizations.put("editor.status.missing.name", "Missing name near %s?");
    localizations.put("editor.status.missing.type", "Missing name or type near %s?");
    localizations.put("editor.status.missing.default", "Missing '%s'?");
  }

  /**
   * Lookup localization.
   *
   * @param key Name of string.
   * @return Value of string or empty if not given.
   */
  public Optional<String> get(String key) {
    return Optional.ofNullable(localizations.getOrDefault(key, null));
  }

}
