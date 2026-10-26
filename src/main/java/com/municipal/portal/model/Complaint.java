package com.municipal.portal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Complaint {
    private String token;
    private String citizenName;
    private String citizenContact;
    private String category;
    private String ward;
    private String description;
    private String attachmentFileName;
    private ComplaintStatus status;
    private String reviewerRemarks;
    private String reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<ComplaintEvent> history = new ArrayList<>();

    public Complaint() {
        this.status = ComplaintStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        history.add(new ComplaintEvent(ComplaintStatus.PENDING, "Complaint registered", "CITIZEN", createdAt));
    }

    public Complaint(String token, String citizenName, String citizenContact, String category, String ward, String description) {
        this();
        this.token = token;
        this.citizenName = citizenName;
        this.citizenContact = citizenContact;
        this.category = category;
        this.ward = ward;
        this.description = description;
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }

    public String getCitizenContact() { return citizenContact; }
    public void setCitizenContact(String citizenContact) { this.citizenContact = citizenContact; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttachmentFileName() { return attachmentFileName; }
    public void setAttachmentFileName(String attachmentFileName) { this.attachmentFileName = attachmentFileName; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { 
        this.status = status; 
        this.updatedAt = LocalDateTime.now();
    }

    public String getReviewerRemarks() { return reviewerRemarks; }
    public void setReviewerRemarks(String reviewerRemarks) { this.reviewerRemarks = reviewerRemarks; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public synchronized List<ComplaintEvent> getHistory() { return List.copyOf(history); }

    public synchronized void recordEvent(String remarks, String actor) {
        history.add(new ComplaintEvent(status, remarks, actor, updatedAt));
    }
}
