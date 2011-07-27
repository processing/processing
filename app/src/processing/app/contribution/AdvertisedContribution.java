package processing.app.contribution;

import java.util.List;

public class AdvertisedContribution extends AbstractContribution {
  
  protected Type type;
  
  public String link;

  public List<String> libraryNames;
  
  public AdvertisedContribution(Type type) {
    this.type = type;
  }
  
  public boolean isInstalled() {
    return false;
  }

  public Type getType() {
    return type;
  }
  
//  public void setName(String name) {
//    this.name = name;
//  }
//
//  public void setCategory(String category) {
//    this.category = category;
//  }
//
//  public void setAuthorList(List<Author> authorList) {
//    this.authorList = authorList;
//  }
//
//  public void setUrl(String url) {
//    this.url = url;
//  }
//
//  public void setSentence(String sentence) {
//    this.sentence = sentence;
//  }
//
//  public void setParagraph(String paragraph) {
//    this.paragraph = paragraph;
//  }
//
//  public void setVersion(int version) {
//    this.version = version;
//  }
//
//  public void setLatestVersion(int latestVersion) {
//    this.latestVersion = latestVersion;
//  }
//
//  public void setPrettyVersion(String prettyVersion) {
//    this.prettyVersion = prettyVersion;
//  }
  
}
