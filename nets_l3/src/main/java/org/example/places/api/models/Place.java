package org.example.places.api.models;

public class Place {
    private String title;
    private String description;
    private String pageId;

    public Place() {}

    public Place(String title, String description, String pageId) {
        this.title = title;
        this.description = description;
        this.pageId = pageId;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }
}