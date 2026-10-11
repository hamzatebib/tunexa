package com.tunexa.address;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tunexa.auth.PhoneNumbers;
import com.tunexa.common.ApiException;
import com.tunexa.config.AuthUser;
import com.tunexa.geo.GovernorateRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/me/addresses")
public class AddressController {

    public record AddressRequest(
            @NotBlank @Size(max = 40) String label,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank String phone,
            @NotNull Long governorateId,
            @NotBlank @Size(max = 80) String delegation,
            @Size(max = 80) String locality,
            @NotBlank @Pattern(regexp = "\\d{4}", message = "Code postal à 4 chiffres") String postalCode,
            @NotBlank @Size(max = 200) String street,
            @Size(max = 200) String landmark,
            boolean defaultAddress) {
    }

    public record AddressDto(Long id, String label, String fullName, String phone, Long governorateId,
            String governorateName, String delegation, String locality, String postalCode, String street,
            String landmark, boolean defaultAddress) {

        static AddressDto of(Address a) {
            return new AddressDto(a.getId(), a.getLabel(), a.getFullName(), a.getPhone(), a.getGovernorate().getId(),
                    a.getGovernorate().getNameFr(), a.getDelegation(), a.getLocality(), a.getPostalCode(),
                    a.getStreet(), a.getLandmark(), a.isDefaultAddress());
        }
    }

    private final AddressRepository addresses;
    private final GovernorateRepository governorates;

    public AddressController(AddressRepository addresses, GovernorateRepository governorates) {
        this.addresses = addresses;
        this.governorates = governorates;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<AddressDto> list(@AuthenticationPrincipal AuthUser auth) {
        return addresses.findByUserIdOrderByDefaultAddressDescIdAsc(auth.id()).stream().map(AddressDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AddressDto create(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody AddressRequest req) {
        Address address = new Address();
        address.setUserId(auth.id());
        boolean first = addresses.countByUserId(auth.id()) == 0;
        apply(address, req, auth.id(), first || req.defaultAddress());
        return AddressDto.of(addresses.save(address));
    }

    @PutMapping("/{id}")
    @Transactional
    public AddressDto update(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id,
            @Valid @RequestBody AddressRequest req) {
        Address address = addresses.findByIdAndUserId(id, auth.id()).orElseThrow(() -> ApiException.notFound("Adresse"));
        apply(address, req, auth.id(), address.isDefaultAddress() || req.defaultAddress());
        return AddressDto.of(address);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@AuthenticationPrincipal AuthUser auth, @PathVariable Long id) {
        Address address = addresses.findByIdAndUserId(id, auth.id()).orElseThrow(() -> ApiException.notFound("Adresse"));
        addresses.delete(address);
        if (address.isDefaultAddress()) {
            addresses.findByUserIdOrderByDefaultAddressDescIdAsc(auth.id()).stream()
                    .filter(a -> !a.getId().equals(id))
                    .findFirst()
                    .ifPresent(a -> a.setDefaultAddress(true));
        }
    }

    private void apply(Address address, AddressRequest req, Long userId, boolean makeDefault) {
        address.setLabel(req.label().trim());
        address.setFullName(req.fullName().trim());
        address.setPhone(PhoneNumbers.normalize(req.phone()));
        address.setGovernorate(governorates.findById(req.governorateId())
                .orElseThrow(() -> ApiException.badRequest("invalid_governorate", "Gouvernorat inconnu")));
        address.setDelegation(req.delegation().trim());
        address.setLocality(req.locality());
        address.setPostalCode(req.postalCode());
        address.setStreet(req.street().trim());
        address.setLandmark(req.landmark());
        if (makeDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescIdAsc(userId).forEach(a -> a.setDefaultAddress(false));
        }
        address.setDefaultAddress(makeDefault);
    }
}
