package com.municipal.portal.service;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ComplaintService {

    private final Map<String, Complaint> complaintStore = new ConcurrentHashMap<>();
    private final AtomicInteger tokenSequence = new AtomicInteger(1001);
    private final ValidationService validationService = new ValidationService();

    public Complaint submitComplaint(String citizenName, String citizenContact, String category, String ward, String description, String attachmentName) {
        String safeName = validationService.sanitizeInput(citizenName);
        String safeDesc = validationService.sanitizeInput(description);
        String contact = citizenContact == null ? null : citizenContact.trim();
        String normalizedCategory = category == null ? null : category.trim();
        String normalizedWard = ward == null ? null : ward.trim();
        if (!validationService.validateComplaint(safeName, contact, normalizedCategory, normalizedWard, safeDesc)) {
            throw new IllegalArgumentException("Invalid complaint input data");
        }

        String token = "CMP-2026-" + tokenSequence.getAndIncrement();
        Complaint complaint = new Complaint(token, safeName, contact, normalizedCategory, normalizedWard, safeDesc);
        if (attachmentName != null && !attachmentName.trim().isEmpty()) {
            complaint.setAttachmentFileName(validationService.sanitizeInput(attachmentName));
        }
        complaint.setStatus(ComplaintStatus.PENDING);

        complaintStore.put(token, complaint);
        return complaint;
    }

    public Optional<Complaint> getComplaintByToken(String token) {
        if (token == null) return Optional.empty();
        return Optional.ofNullable(complaintStore.get(token.trim().toUpperCase(Locale.ROOT)));
    }

    public List<Complaint> getAllComplaints() {
        return new ArrayList<>(complaintStore.values());
    }

    public synchronized boolean updateStatus(String token, ComplaintStatus newStatus, String remarks, String reviewerId) {
        Complaint complaint = getComplaintByToken(token).orElse(null);
        if (complaint == null) return false;

        if (newStatus == null || !allowedTransitions(complaint.getStatus()).contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + complaint.getStatus() + " to " + newStatus);
        }
        if (newStatus == ComplaintStatus.REJECTED && (remarks == null || remarks.trim().length() < 5)) {
            throw new IllegalArgumentException("Rejection remarks are mandatory and must be at least 5 characters");
        }
        if (remarks != null && remarks.length() > 1500) {
            throw new IllegalArgumentException("Remarks must be at most 1500 characters");
        }

        complaint.setStatus(newStatus);
        complaint.setReviewerRemarks(remarks != null ? remarks.trim() : "");
        complaint.setReviewerId(reviewerId != null ? reviewerId.trim() : "ADMIN");
        complaint.recordEvent(complaint.getReviewerRemarks(), complaint.getReviewerId());
        return true;
    }

    public static Set<ComplaintStatus> allowedTransitions(ComplaintStatus status) {
        return switch (status) {
            case PENDING -> Set.of(ComplaintStatus.UNDER_REVIEW, ComplaintStatus.APPROVED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED);
            case UNDER_REVIEW -> Set.of(ComplaintStatus.APPROVED, ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED);
            case APPROVED -> Set.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED);
            case IN_PROGRESS -> Set.of(ComplaintStatus.RESOLVED);
            case RESOLVED, REJECTED -> Set.of();
        };
    }

    public int getTotalCount() {
        return complaintStore.size();
    }
}
