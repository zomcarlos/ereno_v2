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
import br.ufu.facom.ereno.attacks.uc09.devices.OrientedGrayHoleIED;
import br.ufu.facom.ereno.benign.uc00.devices.LegitimateProtectionIED;
import br.ufu.facom.ereno.benign.uc00.devices.MergingUnit;
import br.ufu.facom.ereno.dataExtractors.ARFFWritter;
import br.ufu.facom.ereno.dataExtractors.CSVWritter;
import br.ufu.facom.ereno.dataExtractors.DebugWritter;
import br.ufu.facom.ereno.general.IED;
import br.ufu.facom.ereno.general.ProtectionIED;
import br.ufu.facom.ereno.messages.EthernetFrame;
import br.ufu.facom.ereno.messages.Goose;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
        substationNetwork.processLevelDevices.add(mu);

        System.out.println("-----------------");
        LegitimateProtectionIED uc00 = new LegitimateProtectionIED();

        RandomReplayerIED uc01 = new RandomReplayerIED(uc00);
        InverseReplayerIED uc02 = new InverseReplayerIED(uc00);
        MasqueradeFakeFaultIED uc03 = new MasqueradeFakeFaultIED(uc00);
        MasqueradeFakeNormalED uc04 = new MasqueradeFakeNormalED();
        InjectorIED uc05 = new InjectorIED(uc00);
        HighStNumInjectorIED uc06 = new HighStNumInjectorIED(uc00);
        HighRateStNumInjectorIED uc07 = new HighRateStNumInjectorIED(uc00);
        GrayHoleVictimIED uc08 = new GrayHoleVictimIED(uc00);
        OrientedGrayHoleIED uc09 = new OrientedGrayHoleIED(uc00);

        uc00.setSubstationNetwork(substationNetwork);
        uc01.setSubstationNetwork(substationNetwork);
        uc02.setSubstationNetwork(substationNetwork);
        uc03.setSubstationNetwork(substationNetwork);
        uc04.setSubstationNetwork(substationNetwork);
        uc05.setSubstationNetwork(substationNetwork);
        uc06.setSubstationNetwork(substationNetwork);
        uc07.setSubstationNetwork(substationNetwork);
        uc08.setSubstationNetwork(substationNetwork);
        uc09.setSubstationNetwork(substationNetwork);

        substationNetwork.processLevelDevices.add(mu);
        substationNetwork.bayLevelDevices.add(uc00);
        substationNetwork.bayLevelDevices.add(uc09);

        Logger.getLogger("SamambaiaScenario").info("Devices set up!");
    }

    @Override
    public void runDevices() {
        // Clear previous messages
        substationNetwork.processBusMessages.clear();
        substationNetwork.stationBusMessages.clear();

        // Generate SV messages (only from MUs)
        for (MergingUnit mu : substationNetwork.processLevelDevices) {
            mu.getMessages().clear();
            mu.run(numberOfMessages);
            substationNetwork.processBusMessages.addAll(
                    new ArrayList<>(mu.getMessages()) // Create new list to avoid sharing
            );
        }

        // Generate GOOSE messages with deduplication check
        Set<String> messageSignatures = new HashSet<>();
        for (IED ied : substationNetwork.bayLevelDevices) {
            if (ied instanceof ProtectionIED) {
                ProtectionIED protectionIED = (ProtectionIED) ied;
                protectionIED.getMessages().clear();
                protectionIED.run(numberOfMessages);

                for (Goose goose : protectionIED.getMessages()) {
                    String signature = createMessageSignature(goose);
                    if (!messageSignatures.contains(signature) && !(ied instanceof LegitimateProtectionIED)) {
                        messageSignatures.add(signature);
                        substationNetwork.stationBusMessages.add(goose);
                    }
                }
            }
        }

        Logger.getLogger("SamambaiaScenario").info(
                String.format("Generated %d unique SV and %d unique GOOSE messages",
                        substationNetwork.processBusMessages.size(),
                        substationNetwork.stationBusMessages.size())
        );
    }

    private String createMessageSignature(Goose goose) {
        return String.format("%.5f-%.5f-%d-%d-%d",
                goose.getTimestamp(),
                goose.getT(),
                goose.getStNum(),
                goose.getSqNum(),
                goose.getCbStatus()
        );
    }
    @Override
    public void exportDataset() {
        verifyMessageUniqueness();
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

    private void verifyMessageUniqueness() {
        Set<String> gooseSignatures = new HashSet<>();
        for (EthernetFrame goose : substationNetwork.stationBusMessages) {
            String sig = createMessageSignature((Goose) goose);
            if (gooseSignatures.contains(sig)) {
                Logger.getLogger("SamambaiaScenario").warning("Duplicate GOOSE message: " + sig);
            }
            gooseSignatures.add(sig);
        }
    }
}
