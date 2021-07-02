/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import java.util.Calendar;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


/*import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.controler.AbstractModule;*/

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public final static boolean DECONGESTION = false;
	
	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/padang/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		
		// add current date in format "yyyy-mm-dd-hh-mm-ss"
					Calendar cal = Calendar.getInstance();
					// this class counts months from 0, but days from 1
					int month = cal.get(Calendar.MONTH) + 1;
					String monthStr = month + "";
					if (month < 10)
						monthStr = "0" + month;
					String date = cal.get(Calendar.YEAR) + "-" + monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH)	+ "-" + cal.get(Calendar.HOUR_OF_DAY) + "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND);
		
		
		if(DECONGESTION) {
			// einbau der decongestion sachen
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setToleratedAverageDelaySec(30.);
			decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0); //Vllt auch auf 0.8, damit sich alles stabilisert?
			decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0); //nach 50% wird die Maut eingesetzt
			decongestionSettings.setUpdatePriceInterval(1);
			decongestionSettings.setMsa(true);
			decongestionSettings.setTollBlendFactor(1.0);
			
//			decongestionSettings.setDecongestionApproach(DecongestionApproach.P_MC);
			
			decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
			decongestionSettings.setKd(0);
			decongestionSettings.setKi(0);
			decongestionSettings.setKp(0.001);
			decongestionSettings.setIntegralApproach(IntegralApproach.UnusedHeadway);
			decongestionSettings.setIntegralApproachUnusedHeadwayFactor(10.0);
			decongestionSettings.setIntegralApproachAverageAlpha(0.0);
			decongestionSettings.setWriteOutputIteration(500);
		
			config.addModule(decongestionSettings);
			
		//Änderung Output directory
			config.controler().setOutputDirectory("./output/"+date+"_decongestion");
		} else {
			config.controler().setOutputDirectory("./output/"+date+"_wodecongestion");
		}
		
		
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves ); //
		config.strategy().setFractionOfIterationsToDisableInnovation(0.6); //nach x% der Iterationen Rerouting ausschalten
		
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		Controler controler = new Controler( scenario ) ;
		
		if(DECONGESTION) {
			
			// congestion toll computation
			
			controler.addOverridingModule(new DecongestionModule(scenario));
			
			// toll-adjusted routing
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
	                                this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
				}
			});
		}
		
	
		
		controler.run();
		
	
	}
	
}
