package com.municipal.portal.service;

import com.municipal.portal.model.Complaint;
import com.municipal.portal.model.ComplaintStatus;

public final class DemoData {
    private DemoData() {}

    public static void seed(ComplaintService service, boolean extended) {
        service.submitComplaint("Rahul Deshmukh", "citizen01@example.com", "Roads & Potholes", "Ward 07",
                "Deep pothole near SV Road junction causing hazard to two-wheelers.", null);
        service.submitComplaint("Sunita Kamble", "citizen02@example.com", "Solid Waste Management", "Ward 12",
                "Uncollected municipal garbage bins overflowing for 3 days.", null);
        if (!extended) return;
        String[] names = {"Aarav Shah", "Meera Joshi", "Kabir Patil", "Ananya Rao", "Vihaan Mehta",
                "Isha Kulkarni", "Arjun Nair", "Diya Desai", "Rohan Kale", "Sara Khan", "Nikhil More",
                "Tara Singh", "Reva Patel", "Dev Sethi", "Neha Das", "Yash Rao", "Aditi Kale", "Om Shah"};
        String[] categories = {"Water Supply & Leakage", "Streetlights & Electricity", "Roads & Potholes",
                "Solid Waste Management", "Drainage & Sewage", "Public Health & Sanitation"};
        String[] descriptions = {"Water pipeline leaking near the community garden entrance.",
                "Streetlights are not working along the main pedestrian crossing.",
                "Damaged road surface near the school requires urgent inspection.",
                "Waste collection is delayed near the neighborhood market.",
                "Blocked drainage is causing water accumulation near the bus stop.",
                "Public sanitation facilities require cleaning and maintenance."};
        for (int i = 0; i < names.length; i++) {
            Complaint complaint = service.submitComplaint(names[i], "citizen" + (i + 3) + "@example.com",
                    categories[i % 6], String.format("Ward %02d", 3 + i % 12), descriptions[i % 6], null);
            switch (i % 6) {
                case 0 -> service.updateStatus(complaint.getToken(), ComplaintStatus.UNDER_REVIEW, "Inspection scheduled", "DEMO_REVIEWER");
                case 1 -> service.updateStatus(complaint.getToken(), ComplaintStatus.APPROVED, "Verified for field work", "DEMO_REVIEWER");
                case 2 -> service.updateStatus(complaint.getToken(), ComplaintStatus.IN_PROGRESS, "Field crew assigned", "DEMO_REVIEWER");
                case 3 -> {
                    service.updateStatus(complaint.getToken(), ComplaintStatus.IN_PROGRESS, "Field crew assigned", "DEMO_REVIEWER");
                    service.updateStatus(complaint.getToken(), ComplaintStatus.RESOLVED, "Maintenance completed and checked", "DEMO_REVIEWER");
                }
                case 4 -> service.updateStatus(complaint.getToken(), ComplaintStatus.REJECTED, "Duplicate of an existing maintenance request", "DEMO_REVIEWER");
                default -> {}
            }
        }
    }
}
