package com.nexuspms.config;

import com.nexuspms.identity.domain.LocalCredential;
import com.nexuspms.identity.domain.User;
import com.nexuspms.identity.repository.LocalCredentialRepository;
import com.nexuspms.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PRD S1.1's admin-governed philosophy means every account is created by an
 * admin -- but that creates a bootstrap problem for the very first admin. This
 * runner is the one deliberate exception: on startup, if no platform_admin
 * exists yet AND the NEXUS_BOOTSTRAP_ADMIN_* environment variables are set, it
 * creates exactly one. This is an operational/deploy-time action (set once by
 * whoever stands up the environment), not a runtime self-service signup path --
 * it never runs again once a platform admin exists.
 */
@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapEmail;
    private final String bootstrapName;
    private final String bootstrapPassword;

    public BootstrapAdminRunner(UserRepository userRepository,
                                 LocalCredentialRepository localCredentialRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${nexus.bootstrap.admin-email:}") String bootstrapEmail,
                                 @Value("${nexus.bootstrap.admin-name:Nexus Admin}") String bootstrapName,
                                 @Value("${nexus.bootstrap.admin-password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.localCredentialRepository = localCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapName = bootstrapName;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
            return;
        }
        if (userRepository.findByEmailIgnoreCase(bootstrapEmail).isPresent()) {
            return;
        }

        User admin = new User(bootstrapName, bootstrapEmail, null, null, "ADMIN");
        admin.grantPlatformAdmin();
        userRepository.save(admin);
        localCredentialRepository.save(new LocalCredential(admin.getId(), passwordEncoder.encode(bootstrapPassword)));
        log.warn("Bootstrapped initial platform admin account for {}. Set NEXUS_BOOTSTRAP_ADMIN_* only for first deploy.", bootstrapEmail);
    }
}
