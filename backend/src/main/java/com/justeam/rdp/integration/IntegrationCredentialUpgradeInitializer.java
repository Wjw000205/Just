package com.justeam.rdp.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Completes the fail-closed credential validation prepared by Flyway V33. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IntegrationCredentialUpgradeInitializer implements ApplicationRunner {
    private final IntegrationService integrations;

    public IntegrationCredentialUpgradeInitializer(IntegrationService integrations) {
        this.integrations = integrations;
    }

    @Override
    public void run(ApplicationArguments args) {
        integrations.validateUpgradedHmacCredentials();
    }
}
