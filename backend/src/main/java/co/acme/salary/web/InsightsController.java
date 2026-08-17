package co.acme.salary.web;

import co.acme.salary.query.BreakdownDimension;
import co.acme.salary.query.InsightModels.BreakdownRow;
import co.acme.salary.query.InsightModels.InsightsDashboard;
import co.acme.salary.service.InsightsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The reporting half of the product: what ACME spends on payroll, and how that spend is
 * distributed across departments, countries, levels and salary bands.
 */
@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insights;

    public InsightsController(InsightsService insights) {
        this.insights = insights;
    }

    /** Everything the dashboard renders, in one round trip. */
    @GetMapping("/dashboard")
    public InsightsDashboard dashboard() {
        return insights.dashboard();
    }

    @GetMapping("/breakdown")
    public List<BreakdownRow> breakdown(@RequestParam BreakdownDimension dimension) {
        return insights.breakdown(dimension);
    }
}
