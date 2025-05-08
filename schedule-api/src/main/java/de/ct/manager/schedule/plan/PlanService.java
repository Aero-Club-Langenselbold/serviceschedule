package de.ct.manager.schedule.plan;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.data.util.Pair;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import de.ct.manager.schedule.participant.DateAssignment;
import de.ct.manager.schedule.participant.DateParticipantAvailability;
import de.ct.manager.schedule.participant.DateParticipantAvailabilityRepository;
import de.ct.manager.schedule.participant.Participant;
import de.ct.manager.schedule.participant.ParticipantAssignment;
import de.ct.manager.schedule.participant.ParticipantRepository;
import de.ct.manager.schedule.participant.Status;
import de.ct.manager.schedule.service.Service;
import de.ct.manager.schedule.service.ServiceDate;
import de.ct.manager.schedule.service.ServiceDateRepository;
import de.ct.manager.schedule.service.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@Slf4j
public class PlanService {

    private static final int ADULT_AGE = 18;

    private final PlanRepository planRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceDateRepository serviceDateRepository;
    private final DateParticipantAvailabilityRepository dateParticipantAvailabilityRepository;
    private final ParticipantRepository participantRepository;

    public PlanService(PlanRepository planRepository, ServiceDateRepository serviceDateRepository, DateParticipantAvailabilityRepository dateParticipantAvailabilityRepository, ServiceRepository serviceRepository, ParticipantRepository participantRepository) {
        this.planRepository = planRepository;
        this.serviceDateRepository = serviceDateRepository;
        this.dateParticipantAvailabilityRepository = dateParticipantAvailabilityRepository;
        this.serviceRepository = serviceRepository;
        this.participantRepository = participantRepository;
    }

    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    public Plan getPlan(String id) {
        return planRepository.findById(id).orElse(null);
    }

    public Plan createOrUpdatePlan(Plan plan) {
        if (plan.getId().isEmpty()) {
            plan.setId(UUID.randomUUID().toString());
        }
        return planRepository.save(plan);
    }

    @Transactional
    public void deletePlan(String id) {
        Optional<Plan> p = planRepository.findById(id);
        if (p.isPresent()) {
            dateParticipantAvailabilityRepository.deleteByPlan(p.get());
        }
        planRepository.deleteById(id);
    }

    public List<ServiceDate> addServiceDates(String planId, List<ServiceDate> serviceDates) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        // Fetch existing service dates for the plan
        List<ServiceDate> existingServiceDates = serviceDateRepository.findByPlan(plan).orElse(new ArrayList<>());

        // Create sets for easy comparison
        Set<String> newServiceDateIds = serviceDates.stream().map(ServiceDate::getId).collect(Collectors.toSet());
        Set<String> existingServiceDateIds = existingServiceDates.stream().map(ServiceDate::getId).collect(Collectors.toSet());

        // Identify service dates to delete
        List<ServiceDate> serviceDatesToDelete = existingServiceDates.stream()
            .filter(sd -> !newServiceDateIds.contains(sd.getId()))
            .toList();

        // Delete missing service dates
        serviceDatesToDelete.forEach(sd -> serviceDateRepository.deleteById(sd.getId()));

        // Update existing service dates and add new ones
        for (ServiceDate sd : serviceDates) {
            if (existingServiceDateIds.contains(sd.getId())) {
                // Update existing service date
                ServiceDate existingServiceDate = existingServiceDates.stream()
                    .filter(esd -> esd.getId().equals(sd.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Service date not found"));
                existingServiceDate.setStartTime(sd.getStartTime());
                existingServiceDate.setEndTime(sd.getEndTime());
                existingServiceDate.setMandatoryAssignees(sd.getMandatoryAssignees());

                serviceDateRepository.save(existingServiceDate);
            } else {
                // Add new service date
                sd.setId(UUID.randomUUID().toString());
                sd.setService(serviceRepository.findById(sd.getService().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Service not found")));
                sd.setPlan(plan);
                serviceDateRepository.save(sd);
            }
        }
        return serviceDateRepository.findByPlan(plan).orElse(new ArrayList<>());
    }

    public void deleteServiceDate(String id) {
        serviceDateRepository.deleteById(id);
    }

    public void addDateParticipantAvailability(String planId, List<DateParticipantAvailability> dpa) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        // Get all participants from the provided DateParticipantAvailability list
        Set<String> participantIds = dpa.stream()
            .map(da -> da.getParticipant().getId())
            .collect(Collectors.toSet());

        // Delete all DateParticipantAvailability entries for those participants for the given plan
        List<DateParticipantAvailability> existingDpaList = dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>()).stream()
            .filter(existingDpa -> participantIds.contains(existingDpa.getParticipant().getId()))
            .toList();
        dateParticipantAvailabilityRepository.deleteAll(existingDpaList);

        dpa.forEach(da -> {
            da.setPlan(plan);
            da.setParticipant(participantRepository.findById(da.getParticipant().getId())
                .orElseThrow(() -> new IllegalArgumentException("Participant not found")));
        });

        dateParticipantAvailabilityRepository.saveAll(dpa);
    }

    public void deleteDateParticipantAvailability(String id) {
        dateParticipantAvailabilityRepository.deleteById(id);
    }

    public List<DateParticipantAvailability> getDateParticipantAvailability(String planId) {
        Plan plan = planRepository.findById(planId).orElseThrow();

        return dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>());
    }

    public List<DateParticipantAvailability> getDateParticipantAvailability(String planId, String userId) {
        Plan plan = planRepository.findById(planId).orElseThrow();

        return dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>()).stream()
            .filter(dpa -> dpa.getParticipant().getId().equals(userId))
            .toList();
    }

    public List<DateParticipantAvailability> getDateParticipantAvailability(String planId, String date, String serviceId) {
        Plan plan = planRepository.findById(planId).orElseThrow();

        return dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>()).stream()
            .filter(dpa -> dpa.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString().equals(date) &&
                dpa.getParticipant().getServices().stream().anyMatch(service -> service.getId().equals(serviceId)))
            .toList();
    }

    public List<ServiceDate> listServiceDatesByPlan(String planId) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        return serviceDateRepository.findByPlan(plan).orElse(new ArrayList<>());
    }

    public List<PlanStatistics> getPlanStatistics(String planId) {
        Plan plan = planRepository.findById(planId).orElseThrow();

        Map<Participant, Map<String, Long>> participantServiceAssignments = plan.getEntries().stream()
            .collect(
                Collectors.groupingBy(PlanEntry::getParticipant, Collectors.groupingBy(entry -> entry.getService().getId(), Collectors.counting()))
            );

        return participantServiceAssignments.entrySet().stream()
            .map(participantEntry -> {
                Participant participant = participantEntry.getKey();
                Map<String, Long> serviceCounts = participantEntry.getValue();

                PlanStatistics stat = new PlanStatistics();
                stat.setParticipant(participant);
                stat.setServiceCount(serviceCounts);
                return stat;
            })
            .sorted(Comparator.comparing(stat -> stat.getParticipant().getFullname()))
            .toList();
    }

    public byte[] generatePlanPDF(String id) {
        Plan plan = planRepository.findById(id).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        // create PDF from plan
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            PageSize pageSize = new PageSize(PageSize.A4.rotate());
            Document document = new Document(pdf, pageSize);

            Table table = new Table(plan.getAllServices().size() + 1);
            table.setWidth(pageSize.getWidth() - document.getLeftMargin() - document.getRightMargin());
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
                        .filter(entry -> {
                            boolean serviceMatches = entry.getService().getId().equals(service.getId());
                            boolean dateMatches = entry.getStartTime().toLocalDate().toString().equals(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString());
                            return serviceMatches && dateMatches;
                        })
                        .toList();

                    // Add service data cell
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

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error creating PDF: {}", e.getMessage());
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    public byte[] generatePlanCSV(String planId, String serviceId) {
        String csvSeparator = ";";
        Plan plan = planRepository.findById(planId).orElseThrow();
        Service service = serviceRepository.findById(serviceId).orElseThrow();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Dienstbezeichnung" + csvSeparator + "Dienstbeginn" + csvSeparator + "Dienstende" + csvSeparator + "Max. Personen" + csvSeparator + "Person" + csvSeparator + "Person2" + csvSeparator + csvSeparator + "\r\n");

        // Group plan entries by startTime, endTime, and service
        Map<LocalDateTime, List<PlanEntry>> groupedEntries = plan.getEntries().stream()
            .filter(planEntry -> planEntry.getService().getId().equals(serviceId))
            .collect(Collectors.groupingBy(PlanEntry::getStartTime));

        groupedEntries = groupedEntries.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldValue, newValue) -> oldValue,
                LinkedHashMap::new
            ));
        groupedEntries.forEach((key, entries) -> {
            PlanEntry firstEntry = entries.get(0);
            String startTime = firstEntry.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String endTime = firstEntry.getEndTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            // Add the service name, start time, end time, and number of participants
            csvBuilder
                .append(service.getServicename()).append(csvSeparator)
                .append(startTime).append(csvSeparator)
                .append(endTime).append(csvSeparator)
                .append(entries.size()).append(csvSeparator); // Number of participants

            // Add participants' names
            for (int i = 0; i < entries.size(); i++) {
                csvBuilder.append(entries.get(i).getParticipant().getFullname());
                csvBuilder.append(csvSeparator);
            }

            csvBuilder.append("\r\n");
        });

        return csvBuilder.toString().getBytes();
    }

    @Transactional
    public Plan generatePlan(String planId) {
        Plan plan = planRepository.findById(planId).orElse(null);

        if (plan == null) {
            throw new IllegalArgumentException("Plan not found");
        }

        List<ServiceDate> serviceDates = serviceDateRepository.findByPlan(plan).orElse(new ArrayList<>());

        if (serviceDates == null || serviceDates.isEmpty()) {
            throw new IllegalArgumentException("No services and dates provided");
        }

        // delete existing planEntries for this plan
        plan.getEntries().clear();

        // if a participant has no availability set (they did not report any), all serviceDates are available by default
        generateAvailabilitiesForPlan(plan, serviceDates);

        List<ParticipantAssignment> availability = getAvailabilityForPlan(plan);

        Set<PlanEntry> planEntries = new HashSet<>();
        List<DateAssignment> dateAssignments = generateDateAssignments(serviceDates, availability);
        List<Service> services = serviceRepository.findAll();

        // to avoid wanting to assign services that are not even in the plan, remove those from the participants
        services.forEach(service -> {
            if (!doesServiceHaveAvailableDates(service, dateAssignments)) {
                removeFromAvailableServices(availability, service);
            }
        });

        while (dateAssignments.stream().anyMatch(da -> da.getPossibleAssignees() > 0 || da.getAssignedParticipants().size() < da.getServiceDate().getMandatoryAssignees())) {
            try {
                // assign the next date given the smallest number of possible assignees
                planEntries.addAll(assignDate(availability, dateAssignments).orElseThrow(() ->
                    new IllegalArgumentException("Plan Entry is empty")));
                log.trace("Assignment made");
                dateAssignments.forEach(da -> log.trace(da.toString()));
            } catch (IllegalArgumentException e) {
                log.error("No assignment possible for date: {}", e.getMessage());
                break;
            }
        }

        plan.getEntries().addAll(planEntries);
        planEntries.forEach(pe -> pe.setPlan(plan));
        plan.setPublished(true);

        return planRepository.save(plan);
    }

    @Transactional
    public List<PlanEntry> updatePlanEntry(String planId, List<PlanEntry> planEntries) {
        Plan plan = planRepository.findById(planId).orElseThrow();
        List<PlanEntry> newEntries = new ArrayList<>();

        planEntries.forEach(planEntry -> {
            serviceRepository.findById(planEntry.getService().getId())
                    .ifPresentOrElse(planEntry::setService, () -> {
                        throw new IllegalArgumentException("Service not found");
                    });
            // delete plan entries for given date and service
            List<PlanEntry> existingEntries = plan.getEntries().stream()
                .filter(pe -> pe.getService().getId().equals(planEntry.getService().getId()) && pe.getDate().equals(planEntry.getDate()))
                .toList();

            plan.getEntries().removeAll(existingEntries);
        });

        planEntries.forEach(planEntry -> {
            planEntry.setId(UUID.randomUUID().toString());
            planEntry.setPlan(plan);

            plan.getEntries().add(planEntry);
            newEntries.add(planEntry);
        });

        planRepository.save(plan);

        return newEntries;
    }

    public List<Service> getAvailableServices(String planId, String date, String currentUsername) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Plan plan = planRepository.findById(planId).orElseThrow();
        List<DateParticipantAvailability> dpas = dateParticipantAvailabilityRepository.findByPlan(plan).orElseThrow().stream()
            .filter(d -> df.format(d.getDate()).equals(date))
            .toList();
        List<ServiceDate> serviceDates = serviceDateRepository.findByPlan(plan).orElseThrow().stream()
            .filter(sd -> df.format(sd.getDate()).equals(date))
            .toList();
        List<Service> availableServices = new ArrayList<>();

        for (ServiceDate serviceDate : serviceDates) {
            long requestedCount = dpas.stream()
                .filter(dpa -> dpa.getRequestedService() != null && dpa.getRequestedService().getId().equals(serviceDate.getService().getId()))
                .count();

            if (requestedCount < serviceDate.getMandatoryAssignees() || dpas.stream().filter(dpa -> dpa.getParticipant().getEmail().equals(currentUsername) && dpa.getRequestedService() != null).count() > 0) {
                availableServices.add(serviceDate.getService());
            }
        }

        return availableServices.stream()
            .distinct()
            .sorted(Comparator.comparing(Service::getServicename))
            .toList();
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

        if (!doesServiceHaveAvailableDates(da.getServiceDate().getService(), dateAssignments)) {
            removeFromAvailableServices(availability, da.getServiceDate().getService());
        }

        return Optional.of(pe);
    }

    private boolean doesServiceHaveAvailableDates(Service service, List<DateAssignment> dateAssignments) {
        return dateAssignments.stream()
            .anyMatch(da -> da.getServiceDate().getService().getId().equals(service.getId()) && da.getPossibleAssignees() > 0);
    }

    private void removeFromAvailableServices(List<ParticipantAssignment> availability, Service service) {
        for (ParticipantAssignment pa : availability) {
            pa.removeFromAvailableServices(service);
        }
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
            LocalDate serviceDate = da.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            boolean isAvailable = pa.getDatesAvailable().stream()
                .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .anyMatch(date -> date.equals(serviceDate));
            boolean isRequested = pa.getDatesRequested().stream()
                .map(pair -> Map.entry(
                    pair.getFirst().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    pair.getSecond()))
                .anyMatch(pair -> pair.getKey().equals(serviceDate) && pair.getValue().getId().equals(da.getServiceDate().getService().getId()));
            if ((isAvailable || isRequested)
                && pa.getParticipant().getServices().contains(da.getServiceDate().getService())) {
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

        LocalDate serviceDate = dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        boolean isAvailable = pa.getDatesAvailable().stream()
            .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            .anyMatch(date -> date.equals(serviceDate));
        boolean isRequested = pa.getDatesRequested().stream()
            .map(pair -> Map.entry(
                pair.getFirst().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                pair.getSecond()))
            .anyMatch(pair -> pair.getKey().equals(serviceDate) && pair.getValue().getId().equals(dateAssignment.getServiceDate().getService().getId()));

        // participant is available if: service is available, date is available, and next service to assign is the same as the service of the date
        return pa.getParticipant().getServices().contains(dateAssignment.getServiceDate().getService()) &&
            (isAvailable || isRequested) &&
            pa.canAssignService(dateAssignment.getServiceDate());
    }

    private boolean isParticipantAvailableForServiceAndDateOutsideThreshold(ParticipantAssignment pa, DateAssignment dateAssignment) {
        if (!mayAssignMinor(pa, dateAssignment)) {
            return false;
        }

        LocalDate serviceDate = dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        boolean isAvailable = pa.getDatesAvailable().stream()
            .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            .anyMatch(date -> date.equals(serviceDate));
            boolean isRequested = pa.getDatesRequested().stream()
            .map(pair -> Map.entry(
                pair.getFirst().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                pair.getSecond()))
            .anyMatch(pair -> pair.getKey().equals(serviceDate) && pair.getValue().getId().equals(dateAssignment.getServiceDate().getService().getId()));
        boolean isInThreshold = pa.getDatesWithinAssignmentThreshold().stream()
            .map(date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            .anyMatch(date -> date.equals(serviceDate));

        return pa.getParticipant().getServices().contains(dateAssignment.getServiceDate().getService()) && (
            isAvailable || isRequested || isInThreshold);
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

        log.trace("Participant {} may be assigned to date {}, age at date: {}", pa.getParticipant().getEmail(), dateAssignment.getServiceDate().getDate(), pa.getParticipant().getAgeAtDate(dateAssignment.getServiceDate().getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));

        return true;
    }

    private PlanEntry generatePlanEntry(ServiceDate serviceDate, List<ParticipantAssignment> availableParticipants, DateAssignment dateAssignment) {
        PlanEntry planEntry = new PlanEntry();
        planEntry.setService(serviceDate.getService());

        LocalDate localDate = serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        planEntry.setStartTime(localDate.atTime(serviceDate.getStartTime()));
        planEntry.setEndTime(localDate.atTime(serviceDate.getEndTime()));

        // pick candidates with special requests for this date first
        ParticipantAssignment selectedParticipant = availableParticipants.stream()
            .filter(pa -> !pa.getDatesRequested().isEmpty() && pa.getDatesRequested().stream()
                .anyMatch(pair -> pair.getFirst().equals(serviceDate.getDate()) && pair.getSecond().getId().equals(serviceDate.getService().getId())))
            .min(Comparator.comparingInt(pa -> pa.getDatesRequested().size()))
            .orElse(null);

        // pick the participant with the least possible assignment dates
        // and fewest assignments so far
        log.trace("Selected participant as requested {}", selectedParticipant == null ? "null" : selectedParticipant.getParticipant().getEmail());
        if (selectedParticipant == null) {
            selectedParticipant = availableParticipants.stream()
                .min(Comparator.comparingInt((ParticipantAssignment pa) -> pa.getDatesAssigned().size())
                    .thenComparingInt(pa -> pa.getDatesAvailable().size()))
                .orElseThrow(() -> new IllegalArgumentException("No available participants"));
        }

        if (selectedParticipant.assignDate(serviceDate.getDate())) {
            planEntry.setParticipant(selectedParticipant.getParticipant());
            dateAssignment.getAssignedParticipants().add(selectedParticipant.getParticipant());
            log.trace("Selected participant: {}, assignments available: {}, assignments made: {}", selectedParticipant.getParticipant().getEmail(), selectedParticipant.getDatesAvailable().size(), selectedParticipant.getDatesAssigned().size());

            // remove all other minors from availability list if one minor is assigned
            if (selectedParticipant.getParticipant().getAgeAtDate(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) < ADULT_AGE) {
                availableParticipants.removeIf(pa -> pa.getParticipant().getAgeAtDate(serviceDate.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()) < ADULT_AGE);
            }
            availableParticipants.remove(selectedParticipant);
        }

        return planEntry;
    }

    private List<ParticipantAssignment> getAvailabilityForPlan(Plan plan) {
        List<DateParticipantAvailability> availabilities = this.dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>());
        List<ParticipantAssignment> availability = new ArrayList<>();

        for (DateParticipantAvailability dpa : availabilities) {
            Optional<ParticipantAssignment> existingAssignment = availability.stream()
                .filter(pa -> pa.getParticipant().equals(dpa.getParticipant()))
                .findFirst();

            if (existingAssignment.isPresent()) {
                if (dpa.getRequestedService() != null) {
                    existingAssignment.get().getDatesRequested().add(Pair.of(dpa.getDate(), dpa.getRequestedService()));
                } else {
                    existingAssignment.get().getDatesAvailable().add(dpa.getDate());
                }
            } else {
                ParticipantAssignment pa = new ParticipantAssignment(dpa.getParticipant(), new ArrayList<>(), new ArrayList<>());
                if (dpa.getRequestedService() != null) {
                    pa.getDatesRequested().add(Pair.of(dpa.getDate(), dpa.getRequestedService()));
                } else {
                    pa.getDatesAvailable().add(dpa.getDate());
                }
                availability.add(pa);
            }
        }

        return availability;
    }

    private void generateAvailabilitiesForPlan(Plan plan, List<ServiceDate> serviceDates) {
        List<Participant> participants = participantRepository.findAll();
        List<DateParticipantAvailability> allAvailabilities = dateParticipantAvailabilityRepository.findByPlan(plan).orElse(new ArrayList<>());

        participants.forEach(p -> {
            if (p.getStatus() == Status.INACTIVE) {
                return;
            }

            List<DateParticipantAvailability> availabilities = allAvailabilities.stream()
                .filter(dpa -> dpa.getParticipant().equals(p))
                .toList();

            if (!availabilities.isEmpty()) {
                return;
            }

            availabilities = new ArrayList<>();
            for (ServiceDate sd : serviceDates) {
                DateParticipantAvailability dpa = new DateParticipantAvailability();
                dpa.setParticipant(p);
                dpa.setPlan(plan);
                dpa.setDate(sd.getDate());
                availabilities.add(dpa);
            }
            dateParticipantAvailabilityRepository.saveAll(availabilities);
        });
    }
}
