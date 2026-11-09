package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.BundleRelease;
import com.example.fxsuite.deployment.Dependencies;
import com.example.fxsuite.deployment.ExternalArtifact;
import com.example.fxsuite.deployment.ExternalBundle;
import com.example.fxsuite.deployment.Members;
import com.example.fxsuite.deployment.Monotonic;
import com.example.fxsuite.deployment.Patch;
import com.example.fxsuite.deployment.PhysicalVersion;
import com.example.fxsuite.deployment.ReleaseDate;
import com.example.fxsuite.deployment.VirtualVersion;

import java.time.LocalDate;

/**
 * Jackson's Guava datatypes. It needs both Jackson and Guava, so it depends on AcmeJson and
 * AcmeGuava instead of bringing them itself. Its own pom names an older Guava, which a
 * build tool's nearest-wins resolution would happily use; depending on AcmeGuava pins the right one.
 */
public record AcmeJsonGuava() implements ExternalBundle {

    /** jackson-datatype-guava 3.2.3. */
    public record V1_20261006_0() implements PhysicalVersion<AcmeJsonGuava>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 10, 6)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public Dependencies dependencies() { return new Dependencies(
                new AcmeJson.V2_20261005_0(),
                new AcmeGuava.V1_20261001_0()); }
        @Override public Members members() { return new Members(new ExternalArtifact("tools.jackson.datatype", "jackson-datatype-guava", "3.2.3")); }
    }

    /** jackson-datatype-guava 3.2.4, on the newer Jackson and Guava. */
    public record V1_20261105_0() implements VirtualVersion<AcmeJsonGuava, V1_20261006_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 5)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261006_0 base() { return new V1_20261006_0(); }
        @Override public Dependencies changes() { return new Dependencies(
                new AcmeJson.V2_20261105_0(),
                new AcmeGuava.V1_20261103_0()); }
        @Override public Members members() { return new Members(new ExternalArtifact("tools.jackson.datatype", "jackson-datatype-guava", "3.2.4")); }
    }

    /** Moves to the fixed Guava. */
    public record V1_20261124_0() implements VirtualVersion<AcmeJsonGuava, V1_20261105_0>, BundleRelease {
        @Override public Monotonic monotonic() { return new Monotonic(1); }
        @Override public ReleaseDate date() { return new ReleaseDate(LocalDate.of(2026, 11, 24)); }
        @Override public Patch patch() { return new Patch(0); }
        @Override public V1_20261105_0 base() { return new V1_20261105_0(); }
        @Override public Dependencies changes() { return new Dependencies(new AcmeGuava.V1_20261124_0()); }
        @Override public Members members() { return new Members(new ExternalArtifact("tools.jackson.datatype", "jackson-datatype-guava", "3.2.4")); }
    }
}
