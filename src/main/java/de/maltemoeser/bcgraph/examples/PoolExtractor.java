package de.maltemoeser.bcgraph.examples;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import de.maltemoeser.bcgraph.blockchain.BlockProvider;
import de.maltemoeser.bcgraph.injector.AnalysisInjector;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This program uses the known pools list of Blockchain.info to determine the pool that owned a specific block.
 */
public class PoolExtractor {

    private BlockProvider blockProvider;
    private Map<String, String> coinbaseTags = new HashMap<>();
    private Map<String, String> payoutAddresses = new HashMap<>();
    private List<String> results = new ArrayList<>(500000);

    @Inject
    public PoolExtractor(BlockProvider blockProvider) {
        this.blockProvider = blockProvider;
    }

    private void analyze() {
        loadPoolData();
        blockProvider.initialize();
        extractPools();
        exportResults();
    }

    /**
     * Load coinbase tags and known pool payout addresses from Blockchain's "pools.json".
     * You can find the latest version of pools.json at https://github.com/blockchain/Blockchain-Known-Pools.
     */
    private void loadPoolData() {
        JSONParser parser = new JSONParser();
        try {
            // read JSON file
            InputStream is = PoolExtractor.class.getResourceAsStream("/pools.json");
            String stringInput = IOUtils.toString(is);
            JSONObject jsonInput = (JSONObject) parser.parse(stringInput);
            JSONObject ct = (JSONObject) jsonInput.get("coinbase_tags");
            JSONObject pa = (JSONObject) jsonInput.get("payout_addresses");

            // payout addresses
            for (Object o : pa.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                JSONObject contents = (JSONObject) entry.getValue();
                payoutAddresses.put((String) entry.getKey(), (String) contents.get("name"));
            }

            // coinbase tags
            for (Object o : ct.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                JSONObject contents = (JSONObject) entry.getValue();
                coinbaseTags.put((String) entry.getKey(), (String) contents.get("name"));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Iterate over all blocks and extract the pool.
     */
    private void extractPools() {
        for (Block block : blockProvider.getBlocks()) {
            // All necessary information can be found in the coinbase transaction
            // The coinbase transaction is always the first transaction in a block
            Transaction coinbaseTransaction = block.getTransactions().get(0);

            // Identify pool by payout address
            String poolName = identifyPoolInPayoutAddress(coinbaseTransaction);

            // Look at coinbase only when payout address is unknown.
            // This is more reliable since it is easy to fake pool attribution in the coinbase.
            if (poolName == null) {
                poolName = identifyPoolInCoinbase(coinbaseTransaction);
            }

            // Set pool name to "NA" if we were not able to determine the pool
            if (poolName == null) {
                poolName = "NA";
            }
            results.add(poolName);
        }
    }

    /**
     * Identify a pool by its payout address.
     *
     * @return the name of the pool, null otherwise
     */
    private String identifyPoolInPayoutAddress(Transaction tx) {
        TransactionOutput output = tx.getOutput(0);
        Script outputScript = output.getScriptPubKey();
        if (outputScript.isSentToAddress() || outputScript.isPayToScriptHash()) {
            String payoutAddress = outputScript.getToAddress(MainNetParams.get()).toString();
            return getPoolNameFromAddress(payoutAddress);
        }
        return null;
    }

    /**
     * Compares the supplied address to all known pool payout addresses.
     */
    private String getPoolNameFromAddress(String addressHash) {
        for (Map.Entry<String, String> entry : payoutAddresses.entrySet()) {
            if (addressHash.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Identify a pool by its coinbase tag / special message in the coinbase
     *
     * @return the name of the pool, null otherwise
     */
    private String identifyPoolInCoinbase(Transaction tx) {
        TransactionInput txIn = tx.getInput(0);
        byte[] scriptBytes = txIn.getScriptBytes();
        String scriptMessage = new String(scriptBytes);
        return getPoolNameFromScriptMessage(scriptMessage);
    }

    /**
     * Compares the coinbase contents to all known coinbase tags of large pools.
     */
    private String getPoolNameFromScriptMessage(String inputString) {
        for (Map.Entry<String, String> entry : coinbaseTags.entrySet()) {
            if (StringUtils.containsIgnoreCase(inputString, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Write the results to a file.
     */
    private void exportResults() {
        try (FileWriter writer = new FileWriter("output/known-pools.txt")) {
            IOUtils.writeLines(results, null, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Injector injector = Guice.createInjector(new AnalysisInjector());
        PoolExtractor poolExtractor = injector.getInstance(PoolExtractor.class);
        poolExtractor.analyze();
    }
}
