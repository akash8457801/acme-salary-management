package co.acme.salary.service;

import co.acme.salary.domain.CompensationRecord;
import co.acme.salary.query.CompensationEntry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw compensation records into the timeline the UI renders, filling in the one thing the
 * database cannot: how big each change was relative to the salary it replaced.
 */
final class CompensationEntryAssembler {

    private CompensationEntryAssembler() {
    }

    /** @param records ordered oldest first */
    static List<CompensationEntry> assemble(List<CompensationRecord> records) {
        List<CompensationEntry> entries = new ArrayList<>(records.size());
        CompensationRecord previous = null;

        for (CompensationRecord record : records) {
            entries.add(new CompensationEntry(
                    record.getId(),
                    record.getAmount(),
                    record.getCurrencyCode(),
                    record.getAnnualUsdAmount(),
                    record.getEffectiveFrom(),
                    record.getEffectiveTo(),
                    record.getChangeReason(),
                    record.getChangeReason().label(),
                    record.getNote(),
                    record.getRecordedAt(),
                    changePercent(previous, record),
                    record.isOpen()));
            previous = record;
        }
        return entries;
    }

    private static BigDecimal changePercent(CompensationRecord previous, CompensationRecord current) {
        if (previous == null) {
            return null;
        }
        return current.salary().percentageChangeFrom(previous.salary());
    }
}
