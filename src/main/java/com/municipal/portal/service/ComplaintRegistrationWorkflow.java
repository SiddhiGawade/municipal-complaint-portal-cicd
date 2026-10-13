package com.municipal.portal.service;

import com.municipal.portal.model.CitizenSubmissionRequest;
import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;

import java.util.Objects;

/**
 * Core Feature 1 Workflow: Citizen Complaint Submission and Registration Engine.
 * Handles validation, automated priority classification, token assignment, and receipt generation.
 */
public class ComplaintRegistrationWorkflow {

    private final ComplaintService complaintService;
    private final ValidationService validationService;
    private static final long MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024; // 5 MB

    public ComplaintRegistrationWorkflow(ComplaintService complaintService, ValidationService validationService) {
        this.complaintService = Objects.requireNonNull(complaintService, "complaintService cannot be null");
        this.validationService = Objects.requireNonNull(validationService, "validationService cannot be null");
    }

    public Complaint registerGrievance(CitizenSubmissionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        // 1. Data Validation
        if (!validationService.validateComplaint(
                request.getCitizenName(),
                request.getCitizenContact(),
                request.getCategory(),
                request.getWard(),
                request.getDescription())) {
            throw new IllegalArgumentException("Complaint validation failed: Missing or malformed parameters");
        }

        // 2. Attachment Size Check
        if (request.getAttachmentSizeBytes() > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachment exceeds maximum allowed limit of 5 MB");
        }

        // 3. Attachment Extension Sanitization
        String attachment = request.getAttachmentFileName();
        if (attachment != null && !attachment.trim().isEmpty()) {
            String lower = attachment.toLowerCase();
            if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png") && !lower.endsWith(".pdf")) {
                throw new IllegalArgumentException("Unsupported file type: Only JPG, PNG, and PDF are allowed");
            }
        }

        // 4. Persistence and Deterministic Token Allocation
        return complaintService.submitComplaint(
                request.getCitizenName(),
                request.getCitizenContact(),
                request.getCategory(),
                request.getWard(),
                request.getDescription(),
                attachment
        );
    }

    public String generateReceipt(Complaint complaint) {
        if (complaint == null) return "";
        return String.format(
                "================ MUNICIPAL GRIEVANCE RECEIPT ================\n" +
                " Token ID:        %s\n" +
                " Citizen Name:    %s\n" +
                " Ward / Sector:   %s\n" +
                " Civic Category:  %s\n" +
                " Status:          %s\n" +
                " Date Logged:     %s\n" +
                "=============================================================",
                complaint.getToken(),
                complaint.getCitizenName(),
                complaint.getWard(),
                complaint.getCategory(),
                complaint.getStatus(),
                complaint.getCreatedAt()
        );
    }
}
