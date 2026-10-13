package com.municipal.portal.service;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ComplaintService {

    private final Map<String, Complaint> complaintStore = new ConcurrentHashMap<>();
    private final AtomicInteger tokenSequence = new AtomicInteger(1001);
    private final ValidationService validationService = new ValidationService();

    public Complaint submitComplaint(String citizenName, String citizenContact, String category, String ward, String description, String attachmentName) {
        if (!validationService.validateComplaint(citizenName, citizenContact, category, ward, description)) {
            throw new IllegalArgumentException("Invalid complaint input data");
        }

        String token = "CMP-2026-" + tokenSequence.getAndIncrement();
        String safeName = validationService.sanitizeInput(citizenName);
        String safeDesc = validationService.sanitizeInput(description);

        Complaint complaint = new Complaint(token, safeName, citizenContact, category, ward, safeDesc);
        if (attachmentName != null && !attachmentName.trim().isEmpty()) {
            complaint.setAttachmentFileName(validationService.sanitizeInput(attachmentName));
        }
        complaint.setStatus(ComplaintStatus.PENDING);

        complaintStore.put(token, complaint);
        return complaint;
    }

    public Optional<Complaint> getComplaintByToken(String token) {
        if (token == null) return Optional.empty();
        return Optional.ofNullable(complaintStore.get(token.trim().toUpperCase()));
    }

    public List<Complaint> getAllComplaints() {
        return new ArrayList<>(complaintStore.values());
    }

    public boolean updateStatus(String token, ComplaintStatus newStatus, String remarks, String reviewerId) {
        Complaint complaint = complaintStore.get(token);
        if (complaint == null) return false;

        if (newStatus == ComplaintStatus.REJECTED && (remarks == null || remarks.trim().isEmpty())) {
            throw new IllegalArgumentException("Remarks are mandatory when rejecting a complaint");
        }

        complaint.setStatus(newStatus);
        complaint.setReviewerRemarks(remarks != null ? remarks.trim() : "");
        complaint.setReviewerId(reviewerId != null ? reviewerId.trim() : "ADMIN");
        return true;
    }

    public int getTotalCount() {
        return complaintStore.size();
    }
}
