package processing.app.contrib;

public class UpdateListPanel extends ListPanel {

  Contribution.Filter contribFilter;

  public UpdateListPanel(ContributionTab contributionTab,
                         Contribution.Filter contribFilter) {
    super(contributionTab, contribFilter, true,
          ContributionColumn.STATUS_NO_HEADER,
          ContributionColumn.NAME,
          ContributionColumn.AUTHOR,
          ContributionColumn.INSTALLED_VERSION,
          ContributionColumn.AVAILABLE_VERSION);

    this.contribFilter = contribFilter;
    table.getTableHeader().setEnabled(false);
  }

  // Thread: EDT
  @Override
  public void contributionAdded(final Contribution contribution) {
    // Ensures contributionAdded in ListPanel is only run on LocalContributions
    if (contribFilter.matches(contribution)) {
      super.contributionAdded(contribution);
      ((UpdateStatusPanel) contributionTab.statusPanel).update(); // Enables update button
    }
  }

  // Thread: EDT
  @Override
  public void contributionRemoved(final Contribution contribution) {
    super.contributionRemoved(contribution);
    ((UpdateStatusPanel) contributionTab.statusPanel).update(); // Disables update button on last contribution
  }

  // Thread: EDT
  @Override
  public void contributionChanged(final Contribution oldContrib,
                                  final Contribution newContrib) {
    DetailPanel panel = panelByContribution.get(oldContrib);
    if (panel == null) {
      contributionAdded(newContrib);
    } else if (newContrib.isInstalled()) {
      panelByContribution.remove(oldContrib);
    }
    model.fireTableDataChanged();
  }

}
