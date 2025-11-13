package ru.practicum.repository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.model.entity.Event;
import ru.practicum.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventSpecifications {

    public static Specification<Event> withUsers(List<Long> users) {
        return (root, query, cb) -> {
            if (users == null || users.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("initiator").get("id").in(users);
        };
    }

    public static Specification<Event> withStates(List<EventState> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("state").in(states);
        };
    }

    public static Specification<Event> withCategories(List<Long> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("category").get("id").in(categories);
        };
    }

    public static Specification<Event> withRangeStart(LocalDateTime rangeStart) {
        return (root, query, cb) -> {
            if (rangeStart == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart);
        };
    }

    public static Specification<Event> withRangeEnd(LocalDateTime rangeEnd) {
        return (root, query, cb) -> {
            if (rangeEnd == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd);
        };
    }

    public static Specification<Event> forPublicSearch(String text, List<Long> categories, Boolean paid,
                                                       LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                       Boolean onlyAvailable) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("state"), EventState.PUBLISHED));

            if (text != null && !text.isBlank()) {
                String likePattern = "%" + text.toLowerCase() + "%";
                Predicate annotationPredicate = cb.like(cb.lower(root.get("annotation")), likePattern);
                Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), likePattern);
                predicates.add(cb.or(annotationPredicate, descriptionPredicate));
            }

            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categories));
            }

            if (paid != null) {
                predicates.add(cb.equal(root.get("paid"), paid));
            }

            if (rangeStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
            }
            if (rangeEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
            }

            if (rangeStart == null && rangeEnd == null) {
                predicates.add(cb.greaterThan(root.get("eventDate"), LocalDateTime.now()));
            }

            if (Boolean.TRUE.equals(onlyAvailable)) {

                var subquery = query.subquery(Long.class);
                var requestRoot = subquery.from(ru.practicum.model.entity.ParticipationRequest.class);

                subquery.select(cb.count(requestRoot))
                        .where(cb.and(
                                cb.equal(requestRoot.get("event").get("id"), root.get("id")),
                                cb.equal(requestRoot.get("status"), ru.practicum.model.enums.RequestStatus.CONFIRMED)
                        ));

                Predicate noLimit = cb.equal(root.get("participantLimit"), 0);
                Predicate hasAvailableSpots = cb.lessThan(subquery, root.get("participantLimit"));

                predicates.add(cb.or(noLimit, hasAvailableSpots));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}