package com.municipal.portal;

import com.municipal.portal.model.CitizenSubmissionRequest;
import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintRegistrationWorkflow;
import com.municipal.portal.service.ComplaintService;
import com.municipal.portal.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComplaintRegistrationWorkflowTest {

    private ComplaintRegistrationWorkflow workflow;
    private ComplaintService complaintService;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService();
        ValidationService validationService = new ValidationService();
        workflow = new ComplaintRegistrationWorkflow(complaintService, validationService);
    }

    @Test
    void testSuccessfulGrievanceRegistration() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Ananya Kulkarni", "9812345678", "Roads & Potholes", "Ward 05",
                "Severe road cave-in and potholes on SV Road signal."
        );
        request.setAttachmentFileName("pothole_evidence.png");
        request.setAttachmentSizeBytes(1024 * 500); // 500 KB

        Complaint complaint = workflow.registerGrievance(request);

        assertNotNull(complaint);
        assertNotNull(complaint.getToken());
        assertTrue(complaint.getToken().startsWith("CMP-2026-"));
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
        assertEquals("Ananya Kulkarni", complaint.getCitizenName());
        assertEquals("pothole_evidence.png", complaint.getAttachmentFileName());
    }

    @Test
    void testAttachmentExceedsSizeLimitThrowsException() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Vikas Shinde", "9823456789", "Solid Waste Management", "Ward 11",
                "Commercial garbage dumping near school gate."
        );
        request.setAttachmentFileName("dump_video.mp4");
        request.setAttachmentSizeBytes(10 * 1024 * 1024); // 10 MB (limit is 5 MB)

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            workflow.registerGrievance(request);
        });
        assertTrue(exception.getMessage().contains("exceeds maximum allowed limit"));
    }

    @Test
    void testInvalidAttachmentExtensionThrowsException() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Deepak Patil", "9834567890", "Drainage & Sewage", "Ward 09",
                "Sewage line backflow during heavy rainfall."
        );
        request.setAttachmentFileName("malicious_script.sh");
        request.setAttachmentSizeBytes(1024);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            workflow.registerGrievance(request);
        });
        assertTrue(exception.getMessage().contains("Unsupported file type"));
    }

    @Test
    void testInvalidMobileNumberFailsValidation() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Kavita Nair", "12345", "Water Supply & Leakage", "Ward 03",
                "Potable water pipeline damaged by road contractors."
        );

        assertThrows(IllegalArgumentException.class, () -> {
            workflow.registerGrievance(request);
        });
    }

    @Test
    void testShortDescriptionFailsValidation() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Suresh More", "9876501234", "Streetlights & Electricity", "Ward 01",
                "Dark" // Less than 10 characters
        );

        assertThrows(IllegalArgumentException.class, () -> {
            workflow.registerGrievance(request);
        });
    }

    @Test
    void testReceiptGenerationContainsCorrectMetadata() {
        CitizenSubmissionRequest request = new CitizenSubmissionRequest(
                "Tanvi Salve", "9920112233", "Public Health & Sanitation", "Ward 15",
                "Mosquito breeding site near stagnant rainwater pond."
        );
        Complaint complaint = workflow.registerGrievance(request);

        String receipt = workflow.generateReceipt(complaint);
        assertNotNull(receipt);
        assertTrue(receipt.contains("MUNICIPAL GRIEVANCE RECEIPT"));
        assertTrue(receipt.contains(complaint.getToken()));
        assertTrue(receipt.contains("Tanvi Salve"));
        assertTrue(receipt.contains("Ward 15"));
    }
}
