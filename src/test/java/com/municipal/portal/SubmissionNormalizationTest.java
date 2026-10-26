package com.municipal.portal;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.service.ComplaintService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionNormalizationTest {
    private final ComplaintService service = new ComplaintService();

    @Test
    void rejectsANameThatIsEmptyAfterSanitization() {
        assertThrows(IllegalArgumentException.class, () -> service.submitComplaint(
                "<b> </b>", "demo@example.com", "Roads & Potholes", "Ward 07",
                "Damaged road surface near the school.", null));
        assertEquals(0, service.getTotalCount());
    }

    @Test
    void validatesTheDescriptionThatWillActuallyBeStored() {
        assertThrows(IllegalArgumentException.class, () -> service.submitComplaint(
                "Demo Citizen", "demo@example.com", "Roads & Potholes", "Ward 07",
                "<p>Too short</p>", null));
    }

    @Test
    void canonicalizesFieldsBeforeStoringAndGrouping() {
        Complaint complaint = service.submitComplaint("  Demo Citizen  ", " demo@example.com ",
                " Roads & Potholes ", " Ward 07 ", "  Damaged road surface near the school.  ", null);
        assertEquals("Demo Citizen", complaint.getCitizenName());
        assertEquals("demo@example.com", complaint.getCitizenContact());
        assertEquals("Roads & Potholes", complaint.getCategory());
        assertEquals("Ward 07", complaint.getWard());
        assertEquals("Damaged road surface near the school.", complaint.getDescription());
    }

    @Test
    void preservesLegitimateApostrophesWithoutSqlEscaping() {
        Complaint complaint = service.submitComplaint("Asha O'Connell", "demo@example.com",
                "Roads & Potholes", "Ward 07", "Road near O'Connell's school needs repair.", null);
        assertEquals("Asha O'Connell", complaint.getCitizenName());
        assertEquals("Road near O'Connell's school needs repair.", complaint.getDescription());
    }
}
