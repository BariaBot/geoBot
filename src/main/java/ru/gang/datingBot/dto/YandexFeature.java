package ru.gang.datingBot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexFeature {
    private YandexProperties properties;
    private YandexGeometry geometry;
}