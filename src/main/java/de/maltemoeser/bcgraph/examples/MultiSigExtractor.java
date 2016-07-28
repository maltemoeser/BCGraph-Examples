package de.maltemoeser.bcgraph.examples;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.maltemoeser.bcgraph.constants.LabelType;
import de.maltemoeser.bcgraph.database.Database;
import de.maltemoeser.bcgraph.entities.BCOutput;
import de.maltemoeser.bcgraph.entities.BCTransaction;
import de.maltemoeser.bcgraph.examples.utils.CSVOutput;
import de.maltemoeser.bcgraph.injector.AnalysisInjector;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;

/**
 * Extract details about all multisignature outputs.
 */
public class MultiSigExtractor {

    private GraphDatabaseService graphDatabaseService;
    private CSVOutput csvOutput = new CSVOutput();
    public static String OUTPUT_FILENAME = "output/multisig.csv";

    @Inject
    public MultiSigExtractor(Database database) {
        this.graphDatabaseService = database.getGraphDatabaseService();
    }

    private void analyze() {
        csvOutput.initializeCSVWriter(OUTPUT_FILENAME);
        iterateOverMultisigOutputs();
        csvOutput.closeCSVWriter();
    }

    /**
     * Looks up all multisignature outputs based on their Label.
     * This includes raw multisig as well as P2SH multisig.
     */
    private void iterateOverMultisigOutputs() {
        try (org.neo4j.graphdb.Transaction ignored = graphDatabaseService.beginTx()) {
            for (Node node : IteratorUtil.asIterable(graphDatabaseService.findNodes(LabelType.MultiSig))) {
                BCOutput output = new BCOutput(node);
                extractOutputDetails(output);
            }
        }
    }

    /**
     * Extract all details of interest from the output.
     */
    private void extractOutputDetails(BCOutput output) {
        BCTransaction transaction = output.getTransaction();
        int p2sh = output.isPayToScriptHash() ? 1 : 0;

        String[] result = {
                transaction.getHash(),
                "" + transaction.getHeight(),
                "" + transaction.getNumberOfInputs(),
                "" + transaction.getNumberOfOutputs(),
                "" + output.getIndex(),
                "" + output.getValue(),
                "" + output.getNumberOfRequiredSignatures(),
                "" + output.getNumberOfTotalSignatures(),
                "" + p2sh
        };
        // write to CSV file
        csvOutput.appendToCSVFile(result);
    }

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AnalysisInjector());
        MultiSigExtractor multiSigExtractor = injector.getInstance(MultiSigExtractor.class);
        multiSigExtractor.analyze();
    }
}
