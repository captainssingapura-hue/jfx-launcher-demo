package com.example.fxsuite.deployment;

import hue.captains.singapura.tao.ontology.enforcer.ContractViolation;
import hue.captains.singapura.tao.ontology.enforcer.OntologyEnforcer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeploymentUnitTest {

    record Ledger() implements Application {}
    record AcmeCore() implements Library {}
    record AcmeJson() implements ExternalBundle {}

    /** Not a valid unit: it holds state. */
    record NamedLibrary(String name) implements Library {}

    private static final OntologyEnforcer ENFORCER = new OntologyEnforcer();

    @Test
    void thereAreExactlyThreeKindsOfUnit() {
        assertTrue(DeploymentUnit.class.isSealed());
        assertEquals(Set.of(Application.class, Library.class, ExternalBundle.class),
                Set.of(DeploymentUnit.class.getPermittedSubclasses()));
    }

    @Test
    void eachKindIsOpenForTeamsToDeclareUnits() {
        for (Class<?> kind : DeploymentUnit.class.getPermittedSubclasses()) {
            assertTrue(kind.isInterface(), kind.getName());
            assertFalse(kind.isSealed(), kind.getName() + " should be non-sealed");
        }
    }

    @Test
    void aUnitsTypeIsItsIdentity() {
        assertEquals(new Ledger(), new Ledger());
        assertEquals(new Ledger().hashCode(), new Ledger().hashCode());
        assertFalse(new Ledger().equals(new AcmeCore()));
    }

    @Test
    void everySwitchOverUnitsCoversExactlyTheThreeKinds() {
        assertEquals("application", kind(new Ledger()));
        assertEquals("library", kind(new AcmeCore()));
        assertEquals("external bundle", kind(new AcmeJson()));
    }

    /** No default branch: a fourth kind of unit would stop this compiling. */
    private static String kind(DeploymentUnit unit) {
        return switch (unit) {
            case Application a -> "application";
            case Library l -> "library";
            case ExternalBundle b -> "external bundle";
        };
    }

    @Test
    void theModelAndStatelessUnitsMeetTheOntologyContracts() {
        for (Class<?> type : List.of(DeploymentUnit.class, Application.class, Library.class,
                ExternalBundle.class, Ledger.class, AcmeCore.class, AcmeJson.class)) {
            assertEquals(List.of(), ENFORCER.enforce(type), type.getName());
        }
    }

    @Test
    void aUnitThatHoldsStateIsRejected() {
        List<ContractViolation> violations = ENFORCER.enforce(NamedLibrary.class);
        assertTrue(violations.stream().anyMatch(v -> v.message().contains("instance field 'name'")),
                violations.toString());
    }
}
