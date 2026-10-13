package com.municipal.portal;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintService;
import com.municipal.portal.service.ReviewerActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewerActionServiceTest {

    private ComplaintService complaintService;
    private ReviewerActionService reviewerService;
    private Complaint testComplaint;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService();
        reviewerService = new ReviewerActionService(complaintService);
        testComplaint = complaintService.submitComplaint(
                "Nitin Gadre", "9898765432", "Water Supply & Leakage", "Ward 10",
                "High pressure pipeline leakage flooding street.", null
        );
    }

    @Test
    void testApproveComplaint() {
        boolean approved = reviewerService.approveComplaint(testComplaint.getToken(), "WARD_ENG_10", "Verified on site");
        assertTrue(approved);
        assertEquals(ComplaintStatus.APPROVED, testComplaint.getStatus());
        assertEquals("WARD_ENG_10", testComplaint.getReviewerId());
        assertEquals("Verified on site", testComplaint.getReviewerRemarks());
    }

    @Test
    void testRejectComplaintWithValidRemarks() {
        boolean rejected = reviewerService.rejectComplaint(testComplaint.getToken(), "WARD_ENG_10", "Duplicate complaint lodged by resident");
        assertTrue(rejected);
        assertEquals(ComplaintStatus.REJECTED, testComplaint.getStatus());
        assertEquals("Duplicate complaint lodged by resident", testComplaint.getReviewerRemarks());
    }

    @Test
    void testRejectComplaintWithoutRemarksThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewerService.rejectComplaint(testComplaint.getToken(), "WARD_ENG_10", "");
        });
        assertTrue(exception.getMessage().contains("mandatory"));
    }

    @Test
    void testAssignToFieldCrew() {
        boolean assigned = reviewerService.assignToFieldCrew(testComplaint.getToken(), "WARD_ENG_10", "WATER_CREW_04");
        assertTrue(assigned);
        assertEquals(ComplaintStatus.IN_PROGRESS, testComplaint.getStatus());
        assertTrue(testComplaint.getReviewerRemarks().contains("WATER_CREW_04"));
    }

    @Test
    void testResolveComplaint() {
        reviewerService.assignToFieldCrew(testComplaint.getToken(), "WARD_ENG_10", "WATER_CREW_04");
        boolean resolved = reviewerService.resolveComplaint(testComplaint.getToken(), "WARD_ENG_10", "Valve replaced and pressure restored");
        assertTrue(resolved);
        assertEquals(ComplaintStatus.RESOLVED, testComplaint.getStatus());
        assertEquals("Valve replaced and pressure restored", testComplaint.getReviewerRemarks());
    }

    @Test
    void testFilterByWard() {
        complaintService.submitComplaint("Sanjay Mane", "9765432100", "Roads & Potholes", "Ward 14", "Pothole repair needed", null);
        List<Complaint> ward10Complaints = reviewerService.getComplaintsByWard("Ward 10");
        assertEquals(1, ward10Complaints.size());
        assertEquals("Ward 10", ward10Complaints.get(0).getWard());
    }
}
