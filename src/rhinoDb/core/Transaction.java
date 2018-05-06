package rhinoDb.core;

import java.security.*;
import java.util.ArrayList;

public class Transaction {

    public String transactionId; // this is also the hash of the transaction.
    public PublicKey sender; // senders address/public key.
    public PublicKey reciepient; // Recipients address/public key, in our case it will be one static address.
    public float value; //Cost in Tokens to perform the operation. Todo: An algo to generate query cost based on complexity.
    public byte[] signature; // this is to prevent anybody else from spending funds in our wallet.
    public String dBOperation; // String of db query to be handled
    public String opStatus; //Operation status [1.Success, 2.Failed] Todo: implement status update method.
    /**
     * This is the context of data stored as an hash tree
     * A string of hashes, concatenated with InOrder tree traversal. Each hash(hex) is of 64 bit length.
     */
    public String hashString;


    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    private static int sequence = 0; // a rough count of how many transactions have been generated.


    public Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs, String dBOperation) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
        this.dBOperation = dBOperation;

        /**
         * ToDo: method to decode and encode hashTree to this string. Implement in rhinoDb.core.hashUtil called by QueryEngine. QueryEngine will do the required db operation.
         * Should this be invoked at the constructor or once the token is verified?
         */
        //this.hashString = QueryEngine.generateHashString(dBOperation);
        if (hashString == "1") {
            opStatus = "Success";
        } else if (hashString == "2") {
            opStatus = "Failed";
            hashString = null;
        }
    }

    /**
     * ToDo: Do we need to have dBOperation, opStatus and hashString also to calculate hash?
     * @return
     */
    // This Calculates the transaction hash (which will be used as its Id)
    private String calculateHash() {
        sequence++; //increase the sequence to avoid 2 identical transactions having the same hash
        return hashUtil.applySha256(
                hashUtil.getStringFromKey(sender) +
                        hashUtil.getStringFromKey(reciepient) +
                        Float.toString(value) + sequence
        );
    }



    /**
     * Signs all the data we don'tr wish to be tampered with.
     * Creates a signature of the transaction.
     * @param privateKey
     */
    public void generateSignature(PrivateKey privateKey) {
        String data = hashUtil.getStringFromKey(sender) + hashUtil.getStringFromKey(reciepient) + Float.toString(value) + hashString + dBOperation + opStatus;
        signature = hashUtil.applyECDSASig(privateKey,data);
    }
    //Verifies the data we signed hasnt been tampered with
    public boolean verifySignature() {
        String data = hashUtil.getStringFromKey(sender) + hashUtil.getStringFromKey(reciepient) + Float.toString(value) + hashString + dBOperation + opStatus;
        return hashUtil.verifyECDSASig(sender, data, signature);
    }


    /**
     * Wallet's balance is the sum of all the unspent transaction outputs addressed to the user.
     * @return
     */
    //Returns true if new transaction could be created.
    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#rhinoDb.core.Transaction Signature failed to verify");
            return false;
        }

        //gather transaction inputs (Make sure they are unspent):
        for(TransactionInput i : inputs) {
            i.UTXO = RhinoChainTest.UTXOs.get(i.transactionOutputId);
        }

        //check if transaction is valid:
        if(getInputsValue() < RhinoChainTest.minimumTransaction) {
            System.out.println("#rhinoDb.core.Transaction Inputs to small: " + getInputsValue());
            return false;
        }

        //generate transaction outputs:
        float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calculateHash();
        outputs.add(new TransactionOutput( this.reciepient, value,transactionId)); //send value to recipient
        outputs.add(new TransactionOutput( this.sender, leftOver,transactionId)); //send the left over 'change' back to sender

        //add outputs to Unspent list
        for(TransactionOutput o : outputs) {
            RhinoChainTest.UTXOs.put(o.id , o);
        }

        //remove transaction inputs from UTXO lists as spent:
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if rhinoDb.core.Transaction can't be found skip it
            RhinoChainTest.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    //returns sum of inputs(UTXOs) values
    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if rhinoDb.core.Transaction can't be found skip it
            total += i.UTXO.value;
        }
        return total;
    }

    //returns sum of outputs:
    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }
}