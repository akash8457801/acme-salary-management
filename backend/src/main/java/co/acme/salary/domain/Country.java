package co.acme.salary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A country ACME employs people in. The country owns the payroll currency: a salary recorded for a
 * German employee defaults to EUR because that is what they are actually paid in, and letting the
 * two drift apart is a class of bug worth designing out.
 */
@Entity
@Table(name = "country")
public class Country {

    /** ISO 3166-1 alpha-2. */
    @Id
    @Column(length = 2)
    private String code;

    @Column(nullable = false)
    private String name;

    /** ISO 4217 payroll currency. */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    protected Country() {
        // for JPA
    }

    public Country(String code, String name, String currencyCode) {
        this.code = code;
        this.name = name;
        this.currencyCode = currencyCode;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
}
