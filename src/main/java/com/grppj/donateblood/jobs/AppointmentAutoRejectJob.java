package com.grppj.donateblood.jobs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grppj.donateblood.repository.DonorAppointmentRepository;

@Component
public class AppointmentAutoRejectJob {

    @Autowired
    private DonorAppointmentRepository apptRepo;

    // run every minute; auto-reject anything pending >= 5 minutes
    @Scheduled(cron = "0 * * * * *")
    public void autoRejectPendingOlderThan5Minutes() {
        apptRepo.autoRejectStalePending(5);
    }
}
