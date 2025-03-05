package ru.gang.datingBot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexCompanyMetaData {
    private String id;
    private String address;
    private Double rating;
    private List<YandexPhoto> Photos;
}