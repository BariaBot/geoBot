package ru.gang.datingBot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class YandexPlaceResponse {
    private List<YandexFeature> features;
}

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class YandexFeature {
    private YandexProperties properties;
    private YandexGeometry geometry;
}

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class YandexProperties {
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

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class YandexCompanyMetaData {
    private String id;
    private String address;
    private Double rating;
    private List<YandexPhoto> Photos;
}

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class YandexPhoto {
    private String url;
}

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class YandexGeometry {
    private double[] coordinates;
}
