package com.municipal.portal.service;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Core Feature 2 Workflow: Reviewer Action, Triage and Decision Engine.
 * Supports filtering, state machine transitions (Approve/Reject/In-Progress/Resolve),
 * and mandatory official remark validation.
 */
public class ReviewerActionService {

    private final ComplaintService complaintService;

    public ReviewerActionService(ComplaintService complaintService) {
        this.complaintService = Objects.requireNonNull(complaintService, "complaintService cannot be null");
    }

    public List<Complaint> getComplaintsByStatus(ComplaintStatus status) {
        return complaintService.getAllComplaints().stream()
                .filter(c -> c.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<Complaint> getComplaintsByWard(String ward) {
        if (ward == null || ward.trim().isEmpty()) {
            return complaintService.getAllComplaints();
        }
        return complaintService.getAllComplaints().stream()
                .filter(c -> c.getWard() != null && c.getWard().equalsIgnoreCase(ward.trim()))
                .collect(Collectors.toList());
    }

    public boolean approveComplaint(String token, String reviewerId, String remarks) {
        getComplaint(token);
        return complaintService.updateStatus(token, ComplaintStatus.APPROVED,
                remarks != null ? remarks : "Approved for field execution", reviewerId);
    }

    public boolean rejectComplaint(String token, String reviewerId, String mandatoryRemarks) {
        if (mandatoryRemarks == null || mandatoryRemarks.trim().length() < 5) {
            throw new IllegalArgumentException("Rejection remarks are mandatory and must be at least 5 characters");
        }
        getComplaint(token);
        return complaintService.updateStatus(token, ComplaintStatus.REJECTED, mandatoryRemarks, reviewerId);
    }

    public boolean assignToFieldCrew(String token, String reviewerId, String crewId) {
        getComplaint(token);
        return complaintService.updateStatus(token, ComplaintStatus.IN_PROGRESS,
                "Assigned to field maintenance unit: " + (crewId != null ? crewId.trim() : "CREW-01"), reviewerId);
    }

    public boolean resolveComplaint(String token, String reviewerId, String resolutionNotes) {
        getComplaint(token);
        return complaintService.updateStatus(token, ComplaintStatus.RESOLVED,
                resolutionNotes != null ? resolutionNotes : "Civic issue successfully resolved on ground", reviewerId);
    }

    private Complaint getComplaint(String token) {
        return complaintService.getComplaintByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Complaint token not found: " + token));
    }
}
