package com.neveni.rss;

import java.util.Date;

public class RssItem {
	private String title;
	private String link;
	private String description;
	private Date publicationDate;
	// private Long weight;
	private String source;
	private String imageUrl;
	private String keywords;

	public RssItem(String title, String link, String description) {
		this.title = title;
		this.link = link;
		this.description = description;
	}

	public RssItem(String title, String link, String description, Date publicationDate, String source, String imageUrl,
			String keywords) {
		this.title = title;
		this.link = link;
		this.description = description;
		this.publicationDate = publicationDate;
		// this.weight = weight;
		this.source = source;
		this.imageUrl = imageUrl;
		this.keywords = keywords;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String toJson() {
		return "{" + "\"name\":\"" + title + "\"" + ", \"link\":\"" + link + "\"" + ", \"description\":\"" + description
				+ "\"" + ", \"updated\":\"" + publicationDate + "\"" +
				// ", \"size\":\"" + weight + "\"" +
				", \"source\":\"" + source + "\"" + ", \"image\":\"" + imageUrl + "\"" + ", \"keywords\":\"" + keywords
				+ "\"" + '}';
	}

}
