import java.security.PublicKey;
import java.util.*;

public class TxHandler {



    private UTXOPool utxoPool;

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<Transaction.Input> claimedOutputs = tx.getInputs();
        ArrayList<UTXO> allUTXO = this.utxoPool.getAllUTXO();
        Set<UTXO> preventsameUTXOMultipleTimes = new HashSet<>();
        for (Transaction.Input claimedOutput : claimedOutputs) {
            UTXO InputUtxo = new UTXO(claimedOutput.prevTxHash, claimedOutput.outputIndex);
            //(1)
            if(!utxoPool.contains(InputUtxo))
                return false;

            //(3)
            if(preventsameUTXOMultipleTimes.contains(InputUtxo))
                return false;
            preventsameUTXOMultipleTimes.add(InputUtxo);
            Transaction.Output output = utxoPool.getTxOutput(InputUtxo);

            PublicKey address = output.address;
            byte[] signature = claimedOutput.signature;
            byte[] message = tx.getRawDataToSign(claimedOutputs.indexOf(claimedOutput));
            if(address == null || signature == null || message == null)
                return false;
            //(2)
            boolean signatureValid = Crypto.verifySignature(address, message, signature);
            if(!signatureValid)
                return false;

        }

        //(4)
        for(Transaction.Output output : tx.getOutputs()){
            if(output.value < 0)
                return false;
        }

        //(5)
        double sum_inputs = 0L;
        double sum_outputs = 0L;
        for(Transaction.Input claimedOutput : tx.getInputs()) {
            UTXO outputUtxo = new UTXO(claimedOutput.prevTxHash, claimedOutput.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(outputUtxo);
            sum_inputs += output.value;
        }
        for(Transaction.Output output: tx.getOutputs()){
            sum_outputs += output.value;
        }
        if(sum_outputs > sum_inputs)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        HashMap<byte[], Integer> map;
        ArrayList<Transaction> transactions = new ArrayList<>();

        for (Transaction transaction : possibleTxs) {
            transactions.add(transaction);
        }
        boolean allValid = false;
        while(!allValid){
            allValid = true;
            map = new HashMap<>();
            HashSet<Integer> toBeRemoved = new HashSet<>();
            for(int i=0; i<transactions.size(); i++){
                for(Transaction.Input input : transactions.get(i).getInputs()){
                    if(!map.containsKey(input.prevTxHash))
                        map.put(input.prevTxHash, i);
                    else {
                        allValid = false;
                        toBeRemoved.add(i);
                    }
                }
            }
            ArrayList<Transaction> allowedTransactions = new ArrayList<>();
            for(int i=0; i<transactions.size(); i++){
                if(!toBeRemoved.contains(i))
                    allowedTransactions.add(transactions.get(i));
            }
            transactions = allowedTransactions;
        }

        ArrayList<Transaction> acceptedTxs = new ArrayList<>();
        HashMap<UTXO, Integer> usedUTXOs = new HashMap<UTXO, Integer>();
        for (Transaction tx : transactions) {
            if (isValidTx(tx)) {
                acceptedTxs.add(tx);
                updateUTXOPool(tx, usedUTXOs);
            }
        }

        // Convert the accepted transactions to an array and return it
        return acceptedTxs.toArray(new Transaction[0]);
    }

    private void updateUTXOPool(Transaction tx, HashMap<UTXO, Integer> usedUTXOs) {
        // Remove spent UTXOs from the UTXO pool
        for (Transaction.Input input : tx.getInputs()) {
            UTXO inputUtxo = new UTXO(input.prevTxHash, input.outputIndex);
            utxoPool.removeUTXO(inputUtxo);
        }

        // Add new UTXOs created by the transaction's outputs to the UTXO pool
        int index = 0;
        for (Transaction.Output output : tx.getOutputs()) {
            UTXO utxo = new UTXO(tx.getHash(), index);
            utxoPool.addUTXO(utxo, output);
            index++;
        }
    }

}
