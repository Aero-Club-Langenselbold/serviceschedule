package de.ct.manager.schedule.maintenance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryHistoryResponse;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryItemRequest;
import de.ct.manager.schedule.maintenance.MaintenanceDtos.InventoryItemResponse;
import de.ct.manager.schedule.user.User;
import de.ct.manager.schedule.user.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * Workshop stock. Every signed in user counts the stock, maintenance administrators manage the items
 * (enforced by {@link MaintenanceInventoryController}).
 */
@Service
@Slf4j
public class MaintenanceInventoryService {

    /** Counts of the same user within this time are merged into one history entry, so clicking a stepper does not flood the history. */
    static final Duration COUNT_MERGE_WINDOW = Duration.ofMinutes(15);

    private final MaintenanceInventoryItemRepository itemRepository;
    private final MaintenanceInventoryHistoryRepository historyRepository;
    private final MaintenanceAccess access;

    public MaintenanceInventoryService(MaintenanceInventoryItemRepository itemRepository, MaintenanceInventoryHistoryRepository historyRepository,
            MaintenanceAccess access) {
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> getItems() {
        return itemRepository.findAllByOrderByNameAsc().stream().map(MaintenanceInventoryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryHistoryResponse> getHistory(String id) {
        List<MaintenanceInventoryHistory> history = historyRepository.findAllByItemOrderByCreatedAtDesc(find(id));
        var names = access.names(history.stream().map(MaintenanceInventoryHistory::getActor).toList());
        return history.stream()
                .map(entry -> new InventoryHistoryResponse(entry.getId(), entry.getAction(), entry.getQuantity(), names.of(entry.getActor()), entry.getCreatedAt()))
                .toList();
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request, UserDetailsImpl currentUser) {
        MaintenanceInventoryItem item = new MaintenanceInventoryItem();
        apply(item, request);
        MaintenanceInventoryItem saved = itemRepository.save(item);
        addHistory(saved, InventoryHistoryAction.CREATED, access.currentUser(currentUser));
        log.info("User {} added inventory item {}", currentUser.getUsername(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public InventoryItemResponse update(String id, InventoryItemRequest request, UserDetailsImpl currentUser) {
        MaintenanceInventoryItem item = find(id);
        apply(item, request);
        MaintenanceInventoryItem saved = itemRepository.saveAndFlush(item);
        addHistory(saved, InventoryHistoryAction.UPDATED, access.currentUser(currentUser));
        log.info("User {} updated inventory item {}", currentUser.getUsername(), saved.getName());
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id, UserDetailsImpl currentUser) {
        MaintenanceInventoryItem item = find(id);
        itemRepository.delete(item);
        log.info("User {} deleted inventory item {}", currentUser.getUsername(), item.getName());
    }

    /** Sets the counted stock. */
    @Transactional
    public InventoryItemResponse count(String id, int quantity, UserDetailsImpl currentUser) {
        MaintenanceInventoryItem item = find(id);
        checkQuantity(item.getCounting(), quantity);
        if (item.getQuantity() == quantity) {
            return toResponse(item);
        }
        item.setQuantity(quantity);
        MaintenanceInventoryItem saved = itemRepository.saveAndFlush(item);

        User actor = access.currentUser(currentUser);
        Instant now = Instant.now();
        historyRepository.findFirstByItemOrderByCreatedAtDesc(saved)
                .filter(last -> last.getAction() == InventoryHistoryAction.COUNTED
                        && last.getActor() != null && Objects.equals(last.getActor().getId(), actor.getId())
                        && last.getCreatedAt().isAfter(now.minus(COUNT_MERGE_WINDOW)))
                .ifPresentOrElse(last -> {
                    last.setQuantity(quantity);
                    last.setCreatedAt(now);
                    historyRepository.save(last);
                }, () -> addHistory(saved, InventoryHistoryAction.COUNTED, actor));
        return toResponse(saved);
    }

    private MaintenanceInventoryItem find(String id) {
        return itemRepository.findById(id).orElseThrow(() -> MaintenanceAccess.notFound("Inventory item"));
    }

    private void addHistory(MaintenanceInventoryItem item, InventoryHistoryAction action, User actor) {
        MaintenanceInventoryHistory entry = new MaintenanceInventoryHistory();
        entry.setItem(item);
        entry.setActor(actor);
        entry.setAction(action);
        entry.setQuantity(item.getQuantity());
        historyRepository.save(entry);
    }

    private static void checkQuantity(InventoryCounting counting, int quantity) {
        if (quantity < 0 || counting == InventoryCounting.LEVEL && quantity > InventoryCounting.LEVEL_FULL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
        }
    }

    private static void apply(MaintenanceInventoryItem item, InventoryItemRequest request) {
        boolean level = request.counting() == InventoryCounting.LEVEL;
        int quantity = request.quantity() == null ? (level ? InventoryCounting.LEVEL_FULL : 0) : request.quantity();
        checkQuantity(request.counting(), quantity);

        item.setName(request.name().strip());
        item.setKind(request.kind());
        item.setLocation(blankToNull(request.location()));
        item.setCounting(request.counting());
        item.setQuantity(quantity);
        // a fill level has neither a unit nor a minimum
        item.setMinQuantity(level ? null : request.minQuantity());
        item.setUnit(level ? null : blankToNull(request.unit()));
        item.setComment(blankToNull(request.comment()));

        // only spare parts fit aircraft types; replace the content, Hibernate tracks the collection instance
        Set<String> types = request.kind() == InventoryKind.SPARE_PART && request.aircraftTypes() != null
                ? request.aircraftTypes().stream().map(String::strip).collect(Collectors.toSet())
                : Set.of();
        item.getAircraftTypes().clear();
        item.getAircraftTypes().addAll(types);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static InventoryItemResponse toResponse(MaintenanceInventoryItem item) {
        return new InventoryItemResponse(item.getId(), item.getName(), item.getKind(), item.getLocation(), item.getCounting(),
                item.getQuantity(), item.getMinQuantity(), item.getUnit(), item.getComment(),
                item.getAircraftTypes().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), item.getStatus());
    }
}
