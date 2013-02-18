package processing.app.contrib;

interface ContributionFilter {
  boolean matches(Contribution contrib);
}