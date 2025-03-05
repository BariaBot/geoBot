package ru.gang.datingBot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexProperties {
    private String name;
    private String description;
    private YandexCompanyMetaData CompanyMetaData;

    public String getAddress() {
        return CompanyMetaData != null ? CompanyMetaData.getAddress() : null;
    }
    
    public Double getRating() {
        return CompanyMetaData != null ? CompanyMetaData.getRating() : null;
    }
    
    public String getPhotoUrl() {
        if (CompanyMetaData != null && CompanyMetaData.getPhotos() != null && !CompanyMetaData.getPhotos().isEmpty()) {
            return CompanyMetaData.getPhotos().get(0).getUrl();
        }
        return null;
    }
    
    public String getCompanyId() {
        return CompanyMetaData != null ? CompanyMetaData.getId() : null;
    }
}