package com.municipal.portal.model;

import java.time.LocalDateTime;

public record ComplaintEvent(ComplaintStatus status, String remarks, String actor, LocalDateTime at) {}
