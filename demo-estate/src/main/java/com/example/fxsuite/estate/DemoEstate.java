package com.example.fxsuite.estate;

import com.example.fxsuite.deployment.DeploymentUnit;

import hue.captains.singapura.tao.ontology.StatelessFunctionalObject;

import java.util.List;

/**
 * The demo estate: every unit it manages. Bundles first, then internal libraries, then
 * applications.
 */
public final class DemoEstate implements StatelessFunctionalObject {

    public List<Class<? extends DeploymentUnit>> units() {
        return List.of(
                AcmeOntology.class,
                AcmeFx.class,
                AcmeJson.class,
                AcmeGuava.class,
                AcmeJsonGuava.class,
                AcmeCore.class,
                AcmeText.class,
                AcmeNet.class,
                AcmeUi.class,
                AcmeCharts.class,
                AcmeContract.class,
                AcmeCodegen.class,
                Hello.class,
                Ledger.class,
                Atlas.class,
                Courier.class);
    }
}
