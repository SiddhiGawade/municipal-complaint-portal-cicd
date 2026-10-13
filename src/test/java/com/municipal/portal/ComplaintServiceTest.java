package com.municipal.portal;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ComplaintServiceTest {

    private ComplaintService service;

    @BeforeEach
    void setUp() {
        service = new ComplaintService();
    }

    @Test
    void testSubmitValidComplaint() {
        Complaint complaint = service.submitComplaint(
            "Amit Sharma", "9876543210", "Roads & Potholes", "Ward 12",
            "Severe pothole on Link Road causing traffic congestion.", "pothole.jpg"
        );

        assertNotNull(complaint);
        assertNotNull(complaint.getToken());
        assertTrue(complaint.getToken().startsWith("CMP-2026-"));
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
        assertEquals("Amit Sharma", complaint.getCitizenName());
    }

    @Test
    void testSubmitInvalidComplaintFails() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.submitComplaint("", "invalid-phone", "Unknown Category", "", "Short", null);
        });
    }

    @Test
    void testTrackComplaintByToken() {
        Complaint complaint = service.submitComplaint(
            "Priya Patel", "9123456789", "Solid Waste Management", "Ward 04",
            "Garbage overflow near community market area.", null
        );

        Optional<Complaint> tracked = service.getComplaintByToken(complaint.getToken());
        assertTrue(tracked.isPresent());
        assertEquals("Priya Patel", tracked.get().getCitizenName());
    }

    @Test
    void testReviewerApproveComplaint() {
        Complaint complaint = service.submitComplaint(
            "Rohan Verma", "9988776655", "Water Supply & Leakage", "Ward 08",
            "Drinking water pipeline burst near main circle.", null
        );

        boolean updated = service.updateStatus(complaint.getToken(), ComplaintStatus.APPROVED, "Verified by Ward 08 engineer", "WARD_OFFICER_08");
        assertTrue(updated);
        assertEquals(ComplaintStatus.APPROVED, complaint.getStatus());
        assertEquals("Verified by Ward 08 engineer", complaint.getReviewerRemarks());
    }

    @Test
    void testReviewerRejectComplaintRequiresRemarks() {
        Complaint complaint = service.submitComplaint(
            "Sneha Joshi", "9871234560", "Streetlights & Electricity", "Ward 02",
            "Streetlight failure along sector 5 junction.", null
        );

        assertThrows(IllegalArgumentException.class, () -> {
            service.updateStatus(complaint.getToken(), ComplaintStatus.REJECTED, "", "WARD_OFFICER_02");
        });
    }
}
