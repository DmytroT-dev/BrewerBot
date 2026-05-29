package com.brewerbot.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NewsItem {
    private String title;
    private String url;
    private String description;
    private int score;
    private String source;
}
