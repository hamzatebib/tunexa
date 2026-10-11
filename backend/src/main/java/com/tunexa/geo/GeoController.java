package com.tunexa.geo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
public class GeoController {

    public record GovernorateDto(Long id, String code, String nameFr, String nameAr, String nameEn, int shippingZone) {

        static GovernorateDto of(Governorate g) {
            return new GovernorateDto(g.getId(), g.getCode(), g.getNameFr(), g.getNameAr(), g.getNameEn(),
                    g.getShippingZone());
        }
    }

    private final GovernorateRepository repository;

    public GeoController(GovernorateRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/governorates")
    public List<GovernorateDto> governorates() {
        return repository.findAllByOrderByNameFrAsc().stream().map(GovernorateDto::of).toList();
    }
}
