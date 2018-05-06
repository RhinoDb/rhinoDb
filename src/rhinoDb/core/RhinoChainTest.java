package rhinoDb.core;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is just a simulation to validate functionality of other components.
 */
public class RhinoChainTest {

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
    public static int difficulty = 3;
    public static float minimumTransaction = 0.1f;
    public static Wallet walletA;
    public static Wallet walletB;
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        //Setup Bouncey castle as a Security Provider
       Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        //Create the new wallets
        walletA = new Wallet(); // Simulated user.
        walletB = new Wallet(); // rhinoDb account address.
        Wallet tokenDistributor = new Wallet(); // the guy/Orgnization who distributes tokens.

        //Create genesis transaction, so that simulated user has money to do operations.
        genesisTransaction = new Transaction(tokenDistributor.publicKey, walletA.publicKey, 100f,null, null); // here db operation will be null.
        genesisTransaction.generateSignature(tokenDistributor.privateKey); //manually sign, validates transaction is by tokenDistributor.
        genesisTransaction.transactionId = "0"; // as it is genisis transaction.
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Add transaction output.
        UTXOs.put(genesisTransaction .outputs.get(0).id,genesisTransaction.outputs.get(0)); //Put first transaction in UTXOs list.

        System.out.println("Creating Genesis block");
        /**
         * Here we should be prompting for db name from user, for now lets use the name USERS.
         * I think we should just give dbName null for tokenDistributor transactions.
         */
        String dbName = "USERS";
        Block genesis = new Block(dbName,"0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis); //We can define the block size logic here only, keeping block class indipendent of it.


        /**
         * I think we should avoid passing dbName everywhere, we should define the block arraylist as the dbName, ad=nd append accordingly.
         */
        Block block1 = new Block(dbName, genesis.currenthash);
        System.out.println("walletA is trying do an operation, and his balance is:" + walletA.getBalance());
        block1.addTransaction(walletA.sendFundsandOperation(walletB.publicKey,20f, "in USER update key Color to Red"));
        block1.addTransaction(walletA.sendFundsandOperation(walletB.publicKey, 20f, "GET USER.some.type "));
        addBlock(block1);

        isChainValid();


//        //Test public and private keys
//        System.out.println("Private and public keys:");
//        System.out.println(hashUtil.getStringFromKey(walletA.privateKey));
//        System.out.println(hashUtil.getStringFromKey(walletA.publicKey));
//        //Create a test transaction from WalletA to walletB
//        Transaction transaction = new Transaction(walletA.publicKey, walletB.publicKey, 5, null);
//        transaction.generateSignature(walletA.privateKey);
//        //Verify the signature works and verify it from the public key
//        System.out.println("Is signature verified");
//        System.out.println(transaction.verifiySignature());
    }

    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }

    /**
     * Mines the block chain for validity
     * Miner node code [Can be made efficient.
     * @return
     */
    public static Boolean isChainValid()
    {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>();
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //traverse the blockchain and check hashes
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.currenthash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.currenthash.equals(currentBlock.parrentHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if(!currentBlock.currenthash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //Now go through all transactions in that block and verify them.
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }
                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Transaction(" + t + ") output recipient is not who it should be");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }
            }
        }
        System.out.println("Blockchain is valid");
        return true;
    }
}