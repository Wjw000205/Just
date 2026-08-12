package com.justeam.rdp.device;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeviceHeartbeatMonitor {
    private final DeviceService devices;public DeviceHeartbeatMonitor(DeviceService devices){this.devices=devices;}
    @Scheduled(initialDelayString="${rdp.device.heartbeat-initial-delay-ms:15000}",fixedDelayString="${rdp.device.heartbeat-delay-ms:30000}")
    public void check(){devices.markTimedOutDevices(java.time.Instant.now());}
}
