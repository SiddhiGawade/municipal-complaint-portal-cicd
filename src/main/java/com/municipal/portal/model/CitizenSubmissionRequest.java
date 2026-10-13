package com.municipal.portal.model;

public class CitizenSubmissionRequest {
    private String citizenName;
    private String citizenContact;
    private String category;
    private String ward;
    private String description;
    private String attachmentFileName;
    private long attachmentSizeBytes;

    public CitizenSubmissionRequest() {}

    public CitizenSubmissionRequest(String citizenName, String citizenContact, String category, String ward, String description) {
        this.citizenName = citizenName;
        this.citizenContact = citizenContact;
        this.category = category;
        this.ward = ward;
        this.description = description;
    }

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

    public long getAttachmentSizeBytes() { return attachmentSizeBytes; }
    public void setAttachmentSizeBytes(long attachmentSizeBytes) { this.attachmentSizeBytes = attachmentSizeBytes; }
}
