package co.acme.salary.web;

import co.acme.salary.domain.ChangeReason;
import co.acme.salary.domain.Country;
import co.acme.salary.domain.Department;
import co.acme.salary.domain.EmploymentStatus;
import co.acme.salary.domain.Gender;
import co.acme.salary.domain.JobLevel;
import co.acme.salary.repository.CountryRepository;
import co.acme.salary.repository.DepartmentRepository;
import co.acme.salary.web.dto.ApiDtos.CountryDto;
import co.acme.salary.web.dto.ApiDtos.DepartmentDto;
import co.acme.salary.web.dto.ApiDtos.OptionDto;
import co.acme.salary.web.dto.ApiDtos.ReferenceDataDto;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Everything the UI needs to populate a dropdown, in one call.
 *
 * <p>One endpoint rather than six: the client fetches this once at start-up, and the alternative
 * is a waterfall of tiny requests before the first screen can render.
 */
@RestController
@RequestMapping("/api/reference-data")
public class ReferenceDataController {

    private final DepartmentRepository departments;
    private final CountryRepository countries;

    public ReferenceDataController(DepartmentRepository departments, CountryRepository countries) {
        this.departments = departments;
        this.countries = countries;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ReferenceDataDto referenceData() {
        return new ReferenceDataDto(
                departments.findAll().stream()
                        .sorted(Comparator.comparing(Department::getName))
                        .map(department -> new DepartmentDto(department.getId(), department.getName()))
                        .toList(),
                countries.findAll().stream()
                        .sorted(Comparator.comparing(Country::getName))
                        .map(country -> new CountryDto(country.getCode(), country.getName(),
                                country.getCurrencyCode()))
                        .toList(),
                Arrays.stream(JobLevel.values())
                        .map(level -> new OptionDto(level.name(), level.name() + " · " + level.title()))
                        .toList(),
                options(EmploymentStatus.values(), EmploymentStatus::name),
                Arrays.stream(ChangeReason.values())
                        .filter(reason -> reason != ChangeReason.HIRE) // only the timeline may open one
                        .map(reason -> new OptionDto(reason.name(), reason.label()))
                        .toList(),
                options(Gender.values(), Gender::name));
    }

    private <T> List<OptionDto> options(T[] values, java.util.function.Function<T, String> name) {
        return Arrays.stream(values)
                .map(value -> new OptionDto(name.apply(value), humanise(name.apply(value))))
                .toList();
    }

    /** ON_LEAVE reads as "On leave" in a dropdown; the enum name is the value, not the label. */
    private String humanise(String enumName) {
        String spaced = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
