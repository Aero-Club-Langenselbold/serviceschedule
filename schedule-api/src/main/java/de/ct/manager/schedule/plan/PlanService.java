package de.ct.manager.schedule.plan;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import de.ct.manager.schedule.participant.DateAssignment;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
public class PlanService {

    private static final int ADULT_AGE = 18;

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    public Plan getPlan(String id) {
        return planRepository.findById(id).orElse(null);
    }

    public Plan createOrUpdatePlan(Plan plan) {
        return planRepository.save(plan);
    }

    public void deletePlan(String id) {
        planRepository.deleteById(id);
    }

    public File generatePlanPDF(String id) {
        Plan plan = planRepository.findById(id).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        // create PDF from plan
        try {
            PdfWriter writer = new PdfWriter("plan_" + id + ".pdf");
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Table table = new Table(plan.getAllServices().size() + 1);

            // add service headers
            table.addHeaderCell(new Cell().add(new Paragraph("Datum")));
            for (Service service : plan.getAllServices()) {
                table.addHeaderCell(new Cell().add(new Paragraph(service.getServicename())));
            }

            // add date rows and contents
            SimpleDateFormat df = new SimpleDateFormat("EE, dd.MM.yyyy", Locale.GERMAN);
            for (Date date : plan.getAllDates()) {
                table.addCell(new Cell().add(new Paragraph(df.format(date))));
                for (Service service : plan.getAllServices()) {
                    // Find the PlanEntry with the matching service and start date
                    List<PlanEntry> matchingEntries = plan.getEntries().stream()
                        .filter(entry -> entry.getService().equals(service) &&
                                Date.from(entry.getStartTime().atZone(ZoneId.systemDefault()).toInstant()).equals(date))
                        .toList();

                    // Add service data cell (you can customize this as needed)
                    if (matchingEntries != null) {
                        StringBuilder participantNames = new StringBuilder();
                        for (PlanEntry e : matchingEntries) {
                            participantNames.append(e.getParticipant().getLastname() + ", " + e.getParticipant().getFirstname() + "\r\n");
                        }
                        table.addCell(new Cell().add(new Paragraph(participantNames.toString())));
                    } else {
                        table.addCell(new Cell().add(new Paragraph("-")));
                    }
                }
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            log.error("Error creating PDF: {}", e.getMessage());
        }

        return new File("plan_" + id + ".pdf");
    }



    @Transactional
    public Plan generatePlan(String planId, List<ServiceDate> serviceDates, List<ParticipantAssignment> availability) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }
        if (serviceDates == null || serviceDates.isEmpty()) {
            throw new IllegalArgumentException("No services and dates provided");
        }

        Set<PlanEntry> planEntries = new HashSet<>();
        List<DateAssignment> dateAssignments = generateDateAssignments(serviceDates, availability);

        while (dateAssignments.stream().anyMatch(da -> da.getPossibleAssignees() > 0 || da.getAssignedParticipants().size() < da.getServiceDate().getMandatoryAssignees())) {
            try {
                // assign the next date given the smallest number of possible assignees
                planEntries.addAll(assignDate(availability, dateAssignments).orElseThrow(() ->
                    new IllegalArgumentException("Plan Entry is empty")));
                log.debug("Assignment made");
                dateAssignments.forEach(da -> log.debug(da.toString()));
            } catch (IllegalArgumentException e) {
                log.error("No assignment possible for date: {}", e.getMessage());
                break;
            }
        }

        plan.getEntries().addAll(planEntries);

        return planRepository.save(plan);
    }

    private Optional<List<PlanEntry>> assignDate(List<ParticipantAssignment> availability, List<DateAssignment> dateAssignments) {
        List<PlanEntry> pe = new ArrayList<>();

        // get the smallest number > 0 of possible assignees
        Optional<DateAssignment> daMin = dateAssignments.stream()
            .filter(da -> da.getPossibleAssignees() > 0)
            .min(Comparator.comparingInt(DateAssignment::getPossibleAssignees));

        DateAssignment da;
        List<ParticipantAssignment> availableParticipants;
        boolean didViolateThreshold = false;

        if (!daMin.isEmpty()) {
            da = daMin.get();
            availableParticipants = getAvailableParticipantsForServiceAndDate(da, availability, false);
        } else {
            // if any unassigned date has no more assignable candidates, ignore threshold
            daMin = dateAssignments.stream()
                .filter(d -> d.getPossibleAssignees() == 0 && d.getAssignedParticipants().size() < d.getServiceDate().getMandatoryAssignees())
                .findFirst();
            da = daMin.get();
            availableParticipants = getAvailableParticipantsForServiceAndDate(da, availability, true);
            didViolateThreshold = true;
        }

        for (int i = 0;i < da.getServiceDate().getMandatoryAssignees();i++) {
            if (availableParticipants.isEmpty() && !didViolateThreshold) {
                // violate threshold if no participant is available
                availableParticipants = getAvailableParticipantsForServiceAndDate(da, availability, true);
            }
            if (availableParticipants.isEmpty()) {
                // if even after a threshold violation no participant is available, don't assign anybody
                log.warn("No more participants available for date: {}", da.getServiceDate().getDate());
                break;
            }
            pe.add(generatePlanEntry(da.getServiceDate(), availableParticipants, da));
        }

        // reset counters for all date assignments and recalculate
        dateAssignments.forEach(d -> {
            d.setPossibleAssignees(0);
            calculateAvailability(availability, d);
        });

        if (pe.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pe);
    }

    private List<DateAssignment> generateDateAssignments(List<ServiceDate> serviceDates, List<ParticipantAssignment> availability) {
        List<DateAssignment> dateAssignments = new ArrayList<>();

        for (ServiceDate serviceDate : serviceDates) {
            DateAssignment da = new DateAssignment(serviceDate);

            calculateAvailability(availability, da);

            dateAssignments.add(da);
        }

        // sort list by smallest possible number of assignees
        return dateAssignments.stream()
            .sorted(Comparator.comparingInt(da -> da.getPossibleAssignees()))
            .toList();
    }

    private void calculateAvailability(List<ParticipantAssignment> availability, DateAssignment da) {
        // count all participants available at that date for that service
        for (ParticipantAssignment pa : availability) {
            if (pa.getDatesAvailable().contains(da.getServiceDate().getDate()) && pa.getParticipant().getServices().contains(da.getServiceDate().getService())) {
                da.setPossibleAssignees(da.getPossibleAssignees() + 1);
            }
        }

        // set available participants to zero if assignment has already been made
        if (da.getAssignedParticipants().size() == da.getServiceDate().getMandatoryAssignees()) {
            da.setPossibleAssignees(0);
        }
    }

    private List<ParticipantAssignment> getAvailableParticipantsForServiceAndDate(DateAssignment dateAssignment, List<ParticipantAssignment> availability, boolean includeThreshold) {
        List<ParticipantAssignment> availableParticipants = new CopyOnWriteArrayList<>();

        for (ParticipantAssignment pa : availability) {
            if (isParticipantAvailableForServiceAndDate(pa, dateAssignment) ||
                (includeThreshold && isParticipantAvailableForServiceAndDateOutsideThreshold(pa, dateAssignment))) {
                availableParticipants.add(pa);
            }
        }

        return availableParticipants;
    }

    private boolean isParticipantAvailableForServiceAndDate(ParticipantAssignment pa, DateAssignment dateAssignment) {
        if (!mayAssignMinor(pa, dateAssignment)) {
            return false;
        }

        // participant is available if: service is available, date is available, and next service to assign is the same as the service of the date
        return pa.getParticipant().getServices().contains(dateAssignment.getServiceDate().getService()) &&
            pa.getDatesAvailable().contains(dateAssignment.getServiceDate().getDate()) &&
            pa.canAssignService(dateAssignment.getServiceDate().getService());
    }

    private boolean isParticipantAvailableForServiceAndDateOutsideThreshold(ParticipantAssignment pa, DateAssignment dateAssignment) {
        if (!mayAssignMinor(pa, dateAssignment)) {
            return false;
        }

        return pa.getParticipant().getServices().contains(dateAssignment.getServiceDate().getService()) && (
            pa.getDatesAvailable().contains(dateAssignment.getServiceDate().getDate()) ||
            pa.getDatesWithinAssignmentThreshold().contains(dateAssignment.getServiceDate().getDate()));
    }

    private boolean mayAssignMinor(ParticipantAssignment pa, DateAssignment dateAssignment) {
        boolean adultAssignedOrMoreThanOneSlotsAvailable = dateAssignment.getAssignedParticipants().stream()
            .anyMatch(p -> p.getAgeAtDate(dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) >= ADULT_AGE) ||
            dateAssignment.getAssignedParticipants().isEmpty();

        if (dateAssignment.getServiceDate().getMandatoryAssignees() == 1 && pa.getParticipant().getAgeAtDate(dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) < ADULT_AGE) {
            return false;
        }
        // if either the other slot is still open or taken by an adult, the participant is available
        if (dateAssignment.getServiceDate().getMandatoryAssignees() >= 2 && !adultAssignedOrMoreThanOneSlotsAvailable) {
            return false;
        }

        log.debug("Participant {} may be assigned to date {}, age at date: {}", pa.getParticipant().getEmail(), dateAssignment.getServiceDate().getDate(), pa.getParticipant().getAgeAtDate(dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));

        return true;
    }

    private PlanEntry generatePlanEntry(ServiceDate serviceDate, List<ParticipantAssignment> possibleParticipants, DateAssignment dateAssignment) {
        PlanEntry planEntry = new PlanEntry();
        planEntry.setService(serviceDate.getService());
        planEntry.setStartTime(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        planEntry.setEndTime(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().plusHours(8)); // Assuming an 8-hour service duration

        // pick the participant with the least possible assignment dates
        // and fewest assignments so far
        ParticipantAssignment selectedParticipant = possibleParticipants.stream()
            .min(Comparator.comparingInt((ParticipantAssignment pa) -> pa.getDatesAssigned().size())
                .thenComparingInt(pa -> pa.getDatesAvailable().size()))
            .orElseThrow(() -> new IllegalArgumentException("No available participants"));

        planEntry.setParticipant(selectedParticipant.getParticipant());

        selectedParticipant.assignDate(serviceDate.getDate());
        dateAssignment.getAssignedParticipants().add(selectedParticipant.getParticipant());

        // remove all other minors from availability list if one minor is assigned
        if (selectedParticipant.getParticipant().getAgeAtDate(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) < ADULT_AGE) {
            possibleParticipants.removeIf(pa -> pa.getParticipant().getAgeAtDate(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) < ADULT_AGE);
        }
        possibleParticipants.remove(selectedParticipant);

        return planEntry;
    }
}
