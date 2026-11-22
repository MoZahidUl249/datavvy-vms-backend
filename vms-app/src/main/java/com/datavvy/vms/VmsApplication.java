package com.datavvy.vms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootable assembly of all modules. Lives in the root package so component,
 * entity and repository scanning covers every {@code com.datavvy.vms.*} module.
 * Add a module = add its Maven dependency to vms-app/pom.xml; nothing else here.
 */
@SpringBootApplication
public class VmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VmsApplication.class, args);
    }
}
