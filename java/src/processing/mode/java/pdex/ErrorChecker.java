package processing.mode.java.pdex;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.compiler.IProblem;

import com.google.classpath.ClassPath;
import com.google.classpath.ClassPathFactory;
import com.google.classpath.RegExpResourceFilter;

import processing.app.Language;
import processing.app.Problem;
import processing.mode.java.JavaEditor;
import processing.mode.java.JavaMode;
import processing.mode.java.pdex.PreprocessedSketch.SketchInterval;


class ErrorChecker {
  // Delay delivering error check result after last sketch change #2677
  private final static long DELAY_BEFORE_UPDATE = 650;

  private ScheduledExecutorService scheduler;
  private volatile ScheduledFuture<?> scheduledUiUpdate = null;
  private volatile long nextUiUpdate = 0;
  private volatile boolean enabled = true;

  private final Consumer<PreprocessedSketch> errorHandlerListener = this::handleSketchProblems;

  private JavaEditor editor;
  private PreprocessingService pps;


  public ErrorChecker(JavaEditor editor, PreprocessingService pps) {
    this.editor = editor;
    this.pps = pps;
    scheduler = Executors.newSingleThreadScheduledExecutor();
    this.enabled = JavaMode.errorCheckEnabled;
    if (enabled) {
      pps.registerListener(errorHandlerListener);
    }
  }


  public void notifySketchChanged() {
    nextUiUpdate = System.currentTimeMillis() + DELAY_BEFORE_UPDATE;
  }


  public void preferencesChanged() {
    if (enabled != JavaMode.errorCheckEnabled) {
      enabled = JavaMode.errorCheckEnabled;
      if (enabled) {
        pps.registerListener(errorHandlerListener);
      } else {
        pps.unregisterListener(errorHandlerListener);
        editor.setProblemList(Collections.emptyList());
        nextUiUpdate = 0;
      }
    }
  }


  public void dispose() {
    if (scheduler != null) {
      scheduler.shutdownNow();
    }
  }


  private void handleSketchProblems(PreprocessedSketch ps) {

    Map<String, String[]> suggCache =
        JavaMode.importSuggestEnabled ? new HashMap<>() : Collections.emptyMap();

    final List<Problem> problems = new ArrayList<>();

    IProblem[] iproblems;
    if (ps.compilationUnit == null) {
      iproblems = new IProblem[0];
    } else {
      iproblems = ps.compilationUnit.getProblems();
    }

    problems.addAll(ps.otherProblems);

    if (problems.isEmpty()) { // Check for curly quotes
      List<JavaProblem> curlyQuoteProblems = checkForCurlyQuotes(ps);
      problems.addAll(curlyQuoteProblems);
    }

    if (problems.isEmpty()) { // Check for missing braces
      List<JavaProblem> missingBraceProblems = checkForMissingBraces(ps);
      problems.addAll(missingBraceProblems);
    }

    if (problems.isEmpty()) {
      AtomicReference<ClassPath> searchClassPath = new AtomicReference<>(null);

      List<Problem> cuProblems = Arrays.stream(iproblems)
          // Filter Warnings if they are not enabled
          .filter(iproblem -> !(iproblem.isWarning() && !JavaMode.warningsEnabled))
          // Hide a useless error which is produced when a line ends with
          // an identifier without a semicolon. "Missing a semicolon" is
          // also produced and is preferred over this one.
          // (Syntax error, insert ":: IdentifierOrNew" to complete Expression)
          // See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=405780
          .filter(iproblem -> !iproblem.getMessage()
              .contains("Syntax error, insert \":: IdentifierOrNew\""))
          // Transform into our Problems
          .map(iproblem -> {
            JavaProblem p = convertIProblem(iproblem, ps);

            // Handle import suggestions
            if (p != null && JavaMode.importSuggestEnabled && isUndefinedTypeProblem(iproblem)) {
              ClassPath cp = searchClassPath.updateAndGet(prev -> prev != null ?
                  prev : new ClassPathFactory().createFromPaths(ps.searchClassPathArray));
              String[] s = suggCache.computeIfAbsent(iproblem.getArguments()[0],
                                                     name -> getImportSuggestions(cp, name));
              p.setImportSuggestions(s);
            }

            return p;
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      problems.addAll(cuProblems);
    }

    if (scheduledUiUpdate != null) {
      scheduledUiUpdate.cancel(true);
    }
    // Update UI after a delay. See #2677
    long delay = nextUiUpdate - System.currentTimeMillis();
    Runnable uiUpdater = () -> {
      if (nextUiUpdate > 0 && System.currentTimeMillis() >= nextUiUpdate) {
        EventQueue.invokeLater(() -> editor.setProblemList(problems));
      }
    };
    scheduledUiUpdate =
      scheduler.schedule(uiUpdater, delay, TimeUnit.MILLISECONDS);
  }


  static private JavaProblem convertIProblem(IProblem iproblem, PreprocessedSketch ps) {
    SketchInterval in = ps.mapJavaToSketch(iproblem);
    if (in != SketchInterval.BEFORE_START) {
      String badCode = ps.getPdeCode(in);
      int line = ps.tabOffsetToTabLine(in.tabIndex, in.startTabOffset);
      JavaProblem p = JavaProblem.fromIProblem(iproblem, in.tabIndex, line, badCode);
      p.setPDEOffsets(in.startTabOffset, in.stopTabOffset);
      return p;
    }
    return null;
  }


  static private boolean isUndefinedTypeProblem(IProblem iproblem) {
    int id = iproblem.getID();
    return id == IProblem.UndefinedType ||
        id == IProblem.UndefinedName ||
        id == IProblem.UnresolvedVariable;
  }


  static private boolean isMissingBraceProblem(IProblem iproblem) {
    if (iproblem.getID() == IProblem.ParsingErrorInsertToComplete) {
      char brace = iproblem.getArguments()[0].charAt(0);
      return brace == '{' || brace == '}';

    } else if (iproblem.getID() == IProblem.ParsingErrorInsertTokenAfter) {
      char brace = iproblem.getArguments()[1].charAt(0);
      return brace == '{' || brace == '}';
    }
    return false;
  }


  static private final Pattern CURLY_QUOTE_REGEX =
    Pattern.compile("([“”‘’])", Pattern.UNICODE_CHARACTER_CLASS);

  static private List<JavaProblem> checkForCurlyQuotes(PreprocessedSketch ps) {
    if (ps.compilationUnit == null) {
      return new ArrayList<>();
    }

    List<JavaProblem> problems = new ArrayList<>(0);

    // Go through the scrubbed code and look for curly quotes (they should not be any)
    Matcher matcher = CURLY_QUOTE_REGEX.matcher(ps.scrubbedPdeCode);
    while (matcher.find()) {
      int pdeOffset = matcher.start();
      String q = matcher.group();

      int tabIndex = ps.pdeOffsetToTabIndex(pdeOffset);
      int tabOffset = ps.pdeOffsetToTabOffset(tabIndex, pdeOffset);
      int tabLine = ps.tabOffsetToTabLine(tabIndex, tabOffset);

      String message = Language.interpolate("editor.status.bad_curly_quote", q);
      JavaProblem problem = new JavaProblem(message, JavaProblem.ERROR, tabIndex, tabLine);
      problem.setPDEOffsets(tabOffset, tabOffset+1);

      problems.add(problem);
    }


    // Go through iproblems and look for problems involving curly quotes
    List<JavaProblem> problems2 = new ArrayList<>(0);
    IProblem[] iproblems = ps.compilationUnit.getProblems();

    for (IProblem iproblem : iproblems) {
      switch (iproblem.getID()) {
        case IProblem.ParsingErrorDeleteToken:
        case IProblem.ParsingErrorDeleteTokens:
        case IProblem.ParsingErrorInvalidToken:
        case IProblem.ParsingErrorReplaceTokens:
        case IProblem.UnterminatedString:
          SketchInterval in = ps.mapJavaToSketch(iproblem);
          if (in == SketchInterval.BEFORE_START) continue;
          String badCode = ps.getPdeCode(in);
          matcher.reset(badCode);
          while (matcher.find()) {
            int offset = matcher.start();
            String q = matcher.group();
            int tabStart = in.startTabOffset + offset;
            int tabStop = tabStart + 1;
            // Prevent duplicate problems
            if (problems.stream().noneMatch(p -> p.getStartOffset() == tabStart)) {
              int line = ps.tabOffsetToTabLine(in.tabIndex, tabStart);
              String message;
              if (iproblem.getID() == IProblem.UnterminatedString) {
                message = Language.interpolate("editor.status.unterm_string_curly", q);
              } else {
                message = Language.interpolate("editor.status.bad_curly_quote", q);
              }
              JavaProblem p = new JavaProblem(message, JavaProblem.ERROR, in.tabIndex, line);
              p.setPDEOffsets(tabStart, tabStop);
              problems2.add(p);
            }
          }
      }
    }

    problems.addAll(problems2);

    return problems;
  }


  static private List<JavaProblem> checkForMissingBraces(PreprocessedSketch ps) {
    List<JavaProblem> problems = new ArrayList<>(0);
    for (int tabIndex = 0; tabIndex < ps.tabStartOffsets.length; tabIndex++) {
      int tabStartOffset = ps.tabStartOffsets[tabIndex];
      int tabEndOffset = (tabIndex < ps.tabStartOffsets.length - 1) ?
          ps.tabStartOffsets[tabIndex + 1] : ps.scrubbedPdeCode.length();
      int[] braceResult = SourceUtils.checkForMissingBraces(ps.scrubbedPdeCode, tabStartOffset, tabEndOffset);
      if (braceResult[0] != 0) {
        JavaProblem problem =
            new JavaProblem(braceResult[0] < 0
                                ? Language.interpolate("editor.status.missing.left_curly_bracket")
                                : Language.interpolate("editor.status.missing.right_curly_bracket"),
                            JavaProblem.ERROR, tabIndex, braceResult[1]);
        problem.setPDEOffsets(braceResult[3], braceResult[3] + 1);
        problems.add(problem);
      }
    }

    if (problems.isEmpty()) {
      return problems;
    }

    int problemTabIndex = problems.get(0).getTabIndex();

    IProblem missingBraceProblem = Arrays.stream(ps.compilationUnit.getProblems())
        .filter(ErrorChecker::isMissingBraceProblem)
        // Ignore if it is at the end of file
        .filter(p -> p.getSourceEnd() + 1 < ps.javaCode.length())
        // Ignore if the tab number does not match our detected tab number
        .filter(p -> problemTabIndex == ps.mapJavaToSketch(p).tabIndex)
        .findFirst()
        .orElse(null);

    // Prefer ECJ problem, shows location more accurately
    if (missingBraceProblem != null) {
      JavaProblem p = convertIProblem(missingBraceProblem, ps);
      if (p != null) {
        problems.clear();
        problems.add(p);
      }
    }

    return problems;
  }


  static public String[] getImportSuggestions(ClassPath cp, String className) {
    className = className.replace("[", "\\[").replace("]", "\\]");
    RegExpResourceFilter regf = new RegExpResourceFilter(
        Pattern.compile(".*"),
        Pattern.compile("(.*\\$)?" + className + "\\.class",
                        Pattern.CASE_INSENSITIVE));

    String[] resources = cp.findResources("", regf);
    return Arrays.stream(resources)
        // remove ".class" suffix
        .map(res -> res.substring(0, res.length() - 6))
        // replace path separators with dots
        .map(res -> res.replace('/', '.'))
        // replace inner class separators with dots
        .map(res -> res.replace('$', '.'))
        // sort, prioritize clases from java. package
        .sorted((o1, o2) -> {
          // put java.* first, should be prioritized more
          boolean o1StartsWithJava = o1.startsWith("java");
          boolean o2StartsWithJava = o2.startsWith("java");
          if (o1StartsWithJava != o2StartsWithJava) {
            if (o1StartsWithJava) return -1;
            return 1;
          }
          return o1.compareTo(o2);
        })
        .toArray(String[]::new);
  }
}
