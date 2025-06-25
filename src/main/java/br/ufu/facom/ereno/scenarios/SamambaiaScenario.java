package br.ufu.facom.ereno.scenarios;

import br.ufu.facom.ereno.SubstationNetwork;
import br.ufu.facom.ereno.api.Attacks;
import br.ufu.facom.ereno.api.GooseFlow;
import br.ufu.facom.ereno.api.SetupIED;
import br.ufu.facom.ereno.attacks.uc01.devices.RandomReplayerIED;
import br.ufu.facom.ereno.attacks.uc02.devices.InverseReplayerIED;
import br.ufu.facom.ereno.attacks.uc03.devices.MasqueradeFakeFaultIED;
import br.ufu.facom.ereno.attacks.uc04.devices.MasqueradeFakeNormalED;
import br.ufu.facom.ereno.attacks.uc05.devices.InjectorIED;
import br.ufu.facom.ereno.attacks.uc06.devices.HighStNumInjectorIED;
import br.ufu.facom.ereno.attacks.uc07.devices.HighRateStNumInjectorIED;
import br.ufu.facom.ereno.attacks.uc08.devices.GrayHoleVictimIED;
import br.ufu.facom.ereno.benign.uc00.devices.LegitimateProtectionIED;
import br.ufu.facom.ereno.benign.uc00.devices.MergingUnit;
import br.ufu.facom.ereno.dataExtractors.ARFFWritter;
import br.ufu.facom.ereno.dataExtractors.CSVWritter;
import br.ufu.facom.ereno.dataExtractors.DebugWritter;
import br.ufu.facom.ereno.general.IED;
import br.ufu.facom.ereno.general.ProtectionIED;
import br.ufu.facom.ereno.messages.Goose;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public class SamambaiaScenario implements IScenario {

    public static void main(String[] args) throws Exception {
        SamambaiaScenario scenario = new SamambaiaScenario();
        scenario.run();
    }

    private static final String CONFIG_FILE = "params.properties";
    private static final Properties props = new Properties();
    SubstationNetwork substationNetwork;
    private static Boolean generateArff = false;
    private static Boolean debug = false;
    private static String path;
    private static String datasetName;
    private static int numberOfMessages;

    @Override
    public void run() {
        substationNetwork = new SubstationNetwork();
        loadAllConfigs();
        setupDevices();
        runDevices();
        exportDataset();
    }

    public void loadAllConfigs() {
        Attacks.loadConfigs();
        GooseFlow.loadConfigs();
        SetupIED.loadConfigs();
        Attacks.legitimate = true;
        Attacks.randomReplay = true;
        Attacks.masqueradeOutage = true;
        Attacks.masqueradeDamage = true;
        Attacks.randomInjection = true;
        Attacks.inverseReplay = true;
        Attacks.highStNum = true;
        Attacks.flooding = true;
        Attacks.grayhole = false;

        try (InputStream input = GooseFlow.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                throw new RuntimeException(CONFIG_FILE + " not found in classpath");
            }

            props.load(input);

            // Map properties to static fields
            generateArff = Boolean.parseBoolean(props.getProperty("scenario.generateArff", "false"));
            debug = Boolean.parseBoolean(props.getProperty("scenario.debug", "false"));
            path = props.getProperty("scenario.path");
            datasetName = props.getProperty("scenario.datasetName");
            numberOfMessages = Integer.parseInt(props.getProperty("goose.flow.numberOfMessages", "0"));

        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration from " + CONFIG_FILE, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in configuration", e);
        }

    }

    @Override
    public void setupDevices() {
        MergingUnit mu = new MergingUnit(InputFilesForSV.getElectricalSourceFiles());
//        mu.enableRandomOffsets(numberOfMessages);
        substationNetwork.processLevelDevices.add(mu);

        // Initializing Legitimate Protection IED
        System.out.println("-----------------");
        LegitimateProtectionIED uc00 = new LegitimateProtectionIED();

        // Initializing attackers
        RandomReplayerIED uc01 = new RandomReplayerIED(uc00);
        InverseReplayerIED uc02 = new InverseReplayerIED(uc00);
        MasqueradeFakeFaultIED uc03 = new MasqueradeFakeFaultIED(uc00);
        MasqueradeFakeNormalED uc04 = new MasqueradeFakeNormalED();
        InjectorIED uc05 = new InjectorIED(uc00);
        HighStNumInjectorIED uc06 = new HighStNumInjectorIED(uc00);
        HighRateStNumInjectorIED uc07 = new HighRateStNumInjectorIED(uc00);
        ProtectionIED uc00forGrayhole = new LegitimateProtectionIED();
        GrayHoleVictimIED uc08 = new GrayHoleVictimIED(uc00forGrayhole);

//        uc00.setInitialTimestamp(mu.getInitialTimestamp());
        uc00.setSubstationNetwork(substationNetwork);
//        uc01.setInitialTimestamp(mu.getInitialTimestamp());
        uc01.setSubstationNetwork(substationNetwork);
//        uc02.setInitialTimestamp(mu.getInitialTimestamp());
        uc02.setSubstationNetwork(substationNetwork);
//        uc03.setInitialTimestamp(mu.getInitialTimestamp());
        uc03.setSubstationNetwork(substationNetwork);
//        uc04.setInitialTimestamp(mu.getInitialTimestamp());
        uc04.setSubstationNetwork(substationNetwork);
//        uc05.setInitialTimestamp(mu.getInitialTimestamp());
        uc05.setSubstationNetwork(substationNetwork);
//        uc06.setInitialTimestamp(mu.getInitialTimestamp());
        uc06.setSubstationNetwork(substationNetwork);
//        uc07.setInitialTimestamp(mu.getInitialTimestamp());
        uc07.setSubstationNetwork(substationNetwork);
//        uc00forGrayhole.setInitialTimestamp(mu.getInitialTimestamp());
        uc00forGrayhole.setSubstationNetwork(substationNetwork);
//        uc08.setInitialTimestamp(mu.getInitialTimestamp());
        uc08.setSubstationNetwork(substationNetwork);

        substationNetwork.processLevelDevices.add(mu);
        substationNetwork.bayLevelDevices.add(uc00);
//        substationNetwork.bayLevelDevices.add(uc01);
//        substationNetwork.bayLevelDevices.add(uc02);
//        substationNetwork.bayLevelDevices.add(uc03);
//        substationNetwork.bayLevelDevices.add(uc04);
//        substationNetwork.bayLevelDevices.add(uc05);
//        substationNetwork.bayLevelDevices.add(uc06);
//        substationNetwork.bayLevelDevices.add(uc07);
//        substationNetwork.bayLevelDevices.add(uc00forGrayhole);
//        substationNetwork.bayLevelDevices.add(uc08);

        Logger.getLogger("SamambaiaScenario").info("Devices set up!");
    }

    @Override
    public void runDevices() {
        // Generating SV messages
        for (MergingUnit mu : substationNetwork.processLevelDevices) {
            mu.run(numberOfMessages);
            substationNetwork.processBusMessages.addAll(mu.getMessages());
        }

        // Generating GOOSE messages
        for (IED ied : substationNetwork.bayLevelDevices) {
            Logger.getLogger("SambaiaScenario").info(substationNetwork.bayLevelDevices.size() + " devices connected to the substation network.");
            ied.run(numberOfMessages);
            int numAddedMessages = 0;

            for (Goose goose : ((ProtectionIED) ied).getMessages()) {
                numAddedMessages++;
                if (numAddedMessages < numberOfMessages) {
                    substationNetwork.stationBusMessages.add(goose);
                } else {
                    break; // skipping additional messages
                }
            }
            Logger.getLogger("SamambaiaScenario").info("Generated " + numAddedMessages + " for IED " + ((ProtectionIED) ied).getLabel());
        }

        Logger.getLogger("SamambaiaScenario").info("Devices run successfully!");

    }

    @Override
    public void exportDataset() {
        try {
            if (!debug) {
                if (generateArff) {
                    ARFFWritter.startWriting(path + datasetName + ".arff");
                    ARFFWritter.processDataset(substationNetwork.stationBusMessages, substationNetwork.processBusMessages);
                    ARFFWritter.finishWriting();
                } else {
                    CSVWritter.startWriting(path+datasetName+".csv");
                    CSVWritter.processDataset(substationNetwork.stationBusMessages, substationNetwork.processBusMessages);
                    CSVWritter.finishWriting();
                }
            } else {
                DebugWritter.startWriting("debug.csv");
                DebugWritter.processDataset(substationNetwork.stationBusMessages, substationNetwork.processBusMessages);
                DebugWritter.finishWriting();
            }

            Logger.getLogger("SamambaiaScenario").info("Dataset exported!");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
