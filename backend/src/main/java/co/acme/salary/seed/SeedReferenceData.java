package co.acme.salary.seed;

import co.acme.salary.domain.JobLevel;
import java.util.List;
import java.util.Map;

/**
 * The shape of ACME as an organisation: where it operates, what it does, and roughly what it pays.
 *
 * <p>These numbers are what make the insights screens worth looking at. Uniformly random salaries
 * would produce a dashboard where every department costs the same and every chart is flat, which
 * would demo nothing. A believable level pyramid, real cost-of-labour differences between
 * countries, and a premium for engineering give the reports something true to say.
 */
final class SeedReferenceData {

    private SeedReferenceData() {
    }

    record CountrySeed(String code, String name, String currency, double costFactor, int rounding) {
    }

    record DepartmentSeed(String name, double payFactor, double headcountWeight, List<String> titles) {
    }

    /** Salary midpoints in USD for each level, before country and department adjustment. */
    static final Map<JobLevel, Integer> LEVEL_MIDPOINT_USD = Map.of(
            JobLevel.L1, 48_000,
            JobLevel.L2, 78_000,
            JobLevel.L3, 112_000,
            JobLevel.L4, 152_000,
            JobLevel.L5, 198_000,
            JobLevel.L6, 262_000,
            JobLevel.L7, 355_000);

    /** A realistic pyramid: many engineers, few executives. Weights need not sum to one. */
    static final Map<JobLevel, Double> LEVEL_WEIGHTS = Map.of(
            JobLevel.L1, 0.13,
            JobLevel.L2, 0.29,
            JobLevel.L3, 0.26,
            JobLevel.L4, 0.15,
            JobLevel.L5, 0.09,
            JobLevel.L6, 0.06,
            JobLevel.L7, 0.02);

    static final List<CountrySeed> COUNTRIES = List.of(
            new CountrySeed("US", "United States", "USD", 1.00, 500),
            new CountrySeed("GB", "United Kingdom", "GBP", 0.82, 500),
            new CountrySeed("DE", "Germany", "EUR", 0.80, 500),
            new CountrySeed("PL", "Poland", "PLN", 0.46, 1_000),
            new CountrySeed("IN", "India", "INR", 0.30, 10_000),
            new CountrySeed("SG", "Singapore", "SGD", 0.83, 500),
            new CountrySeed("AU", "Australia", "AUD", 0.86, 500),
            new CountrySeed("CA", "Canada", "CAD", 0.81, 500),
            new CountrySeed("BR", "Brazil", "BRL", 0.34, 1_000),
            new CountrySeed("JP", "Japan", "JPY", 0.74, 100_000));

    /** How headcount is spread across countries — engineering-heavy in the US, India and Poland. */
    static final List<Double> COUNTRY_WEIGHTS = List.of(
            0.30, 0.09, 0.10, 0.11, 0.18, 0.04, 0.05, 0.06, 0.04, 0.03);

    static final List<DepartmentSeed> DEPARTMENTS = List.of(
            new DepartmentSeed("Engineering", 1.12, 0.34,
                    List.of("Software Engineer", "Platform Engineer", "QA Engineer", "SRE", "Engineering Manager")),
            new DepartmentSeed("Data", 1.10, 0.07,
                    List.of("Data Analyst", "Data Engineer", "Data Scientist", "Analytics Manager")),
            new DepartmentSeed("Product", 1.06, 0.07,
                    List.of("Product Manager", "Product Analyst", "Group Product Manager")),
            new DepartmentSeed("Design", 0.96, 0.05,
                    List.of("Product Designer", "UX Researcher", "Design Lead")),
            new DepartmentSeed("Sales", 1.02, 0.16,
                    List.of("Account Executive", "Sales Development Rep", "Solutions Engineer", "Sales Manager")),
            new DepartmentSeed("Marketing", 0.92, 0.07,
                    List.of("Marketing Manager", "Content Strategist", "Growth Marketer")),
            new DepartmentSeed("Customer Success", 0.86, 0.10,
                    List.of("Customer Success Manager", "Support Engineer", "Onboarding Specialist")),
            new DepartmentSeed("Finance", 1.00, 0.04,
                    List.of("Financial Analyst", "Accountant", "Finance Manager")),
            new DepartmentSeed("People", 0.90, 0.04,
                    List.of("Recruiter", "HR Business Partner", "People Operations Specialist")),
            new DepartmentSeed("Legal", 1.08, 0.02,
                    List.of("Counsel", "Compliance Manager", "Paralegal")),
            new DepartmentSeed("Operations", 0.88, 0.04,
                    List.of("Operations Analyst", "Program Manager", "Workplace Coordinator")));

    static final List<String> FIRST_NAMES = List.of(
            "Aarav", "Ada", "Adam", "Aisha", "Akira", "Alejandro", "Alice", "Amara", "Amelia", "Ana",
            "Andrea", "Aniket", "Anna", "Antoine", "Arjun", "Beatriz", "Ben", "Bruno", "Camila", "Carlos",
            "Caroline", "Chen", "Chloe", "Daniel", "Dario", "David", "Diego", "Divya", "Ella", "Emeka",
            "Emma", "Erik", "Fatima", "Felix", "Gabriel", "Grace", "Hannah", "Hiroshi", "Ines", "Isabel",
            "Ivan", "Jack", "James", "Jana", "Jasmine", "Javier", "Jonas", "Julia", "Kai", "Karolina",
            "Kavya", "Kenji", "Lars", "Laura", "Leon", "Lin", "Lucas", "Lucia", "Maya", "Mateusz",
            "Mei", "Michael", "Mika", "Nadia", "Nina", "Noah", "Olivia", "Omar", "Oscar", "Paula",
            "Priya", "Rahul", "Rafael", "Rebecca", "Rohan", "Sara", "Sebastian", "Sofia", "Sven", "Takumi",
            "Tara", "Thomas", "Tomasz", "Vikram", "Wei", "Yuki", "Zainab", "Zoe");

    static final List<String> LAST_NAMES = List.of(
            "Almeida", "Andersson", "Bauer", "Becker", "Bianchi", "Brown", "Chandra", "Chen", "Costa", "Davis",
            "Dubois", "Dvorak", "Eriksen", "Fernandes", "Fischer", "Garcia", "Gupta", "Hansen", "Hayashi", "Hoffmann",
            "Iyer", "Jackson", "Jensen", "Johnson", "Kaczmarek", "Kaur", "Kimura", "Kowalski", "Krishnan", "Kumar",
            "Lambert", "Lee", "Lewandowski", "Lima", "Lopez", "Martin", "Mehta", "Meyer", "Miller", "Mori",
            "Nakamura", "Nguyen", "Nowak", "Oliveira", "Patel", "Pereira", "Petrov", "Rao", "Reddy", "Ribeiro",
            "Rossi", "Sato", "Schmidt", "Sharma", "Silva", "Singh", "Smith", "Sorensen", "Suzuki", "Tanaka",
            "Taylor", "Thompson", "Torres", "Verma", "Wagner", "Walker", "Wang", "Weber", "Williams", "Wilson",
            "Wojcik", "Yadav", "Yamamoto", "Zhang", "Zielinski");
}
