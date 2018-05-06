package rhinoDb.core;

/**
 * Wallet's balance is the sum of all the unspent transaction outputs addressed to the user.
 * Unlike conventional we don't subtract and add token values for users.
 */
public class TransactionInput {
    public String transactionOutputId; //Reference to TransactionOutputs -> transactionId
    public TransactionOutput UTXO; //Contains the Unspent transaction output

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}
