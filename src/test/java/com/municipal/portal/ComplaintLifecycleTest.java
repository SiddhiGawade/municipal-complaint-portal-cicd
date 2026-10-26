package com.municipal.portal;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;
import com.municipal.portal.service.ComplaintService;
import com.municipal.portal.service.ReviewerActionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintLifecycleTest {
    private final ComplaintService service = new ComplaintService();

    private Complaint complaint() {
        return service.submitComplaint("Demo Citizen", "citizen@example.com", "Roads & Potholes",
                "Ward 07", "A damaged road surface requires inspection.", null);
    }

    @Test
    void pendingComplaintCannotSkipDirectlyToResolved() {
        Complaint complaint = complaint();
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(complaint.getToken(), ComplaintStatus.RESOLVED, "Completed", "DEMO"));
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
    }

    @Test
    void resolvedComplaintCannotBeReopenedByAnotherReviewerPath() {
        Complaint complaint = complaint();
        ReviewerActionService reviewer = new ReviewerActionService(service);
        reviewer.assignToFieldCrew(complaint.getToken(), "DEMO", "CREW-01");
        reviewer.resolveComplaint(complaint.getToken(), "DEMO", "Road repaired");
        assertThrows(IllegalArgumentException.class, () ->
                reviewer.approveComplaint(complaint.getToken(), "DEMO", "Approve again"));
    }

    @Test
    void allReviewerPathsRequireMeaningfulRejectionRemarks() {
        Complaint complaint = complaint();
        assertThrows(IllegalArgumentException.class, () ->
                service.updateStatus(complaint.getToken(), ComplaintStatus.REJECTED, "no", "DEMO"));
        assertEquals(ComplaintStatus.PENDING, complaint.getStatus());
    }

    @Test
    void normalizedTrackingTokenAlsoWorksForStatusUpdates() {
        Complaint complaint = complaint();
        assertTrue(service.updateStatus(" " + complaint.getToken().toLowerCase() + " ",
                ComplaintStatus.UNDER_REVIEW, "Inspection started", "DEMO"));
        assertEquals(ComplaintStatus.UNDER_REVIEW, complaint.getStatus());
    }
}
