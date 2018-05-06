package rhinoDb.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *  rhinoDb.core.Block DTO
 */
public class Block {
    public String currenthash;
    public String parrentHash; // previous block hash.
    public int blockNumber;  //block identifier, begins with 1.
    public String dBName;   //Database name as specified by the user.
    public String blockCreator; //Creator of the current block.
    private long timeStamp; //epoch time.
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    public int nonce;
    public String merkleRoot;

    public Block(String dBName, String parrentHash){
        this.parrentHash = parrentHash;
        this.timeStamp = new Date().getTime();

        this.currenthash = calculateHash();
    }

    public String calculateHash() {
        String calculatedhash = hashUtil.applySha256(
                parrentHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot + dBName
        );
        return calculatedhash;
    }

    //Increases nonce value until hash target is reached.
    public void mineBlock(int difficulty) {
        merkleRoot = hashUtil.getMerkleRoot(transactions);
        String target = hashUtil.getDificultyString(difficulty); //Create a string with difficulty * "0"
        while(!currenthash.substring( 0, difficulty).equals(target)) {
            nonce ++;
            currenthash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + currenthash);
    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        if(transaction == null) return false;
        if((!"0".equals(parrentHash))) {
            if((transaction.processTransaction() != true)) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }

        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }
}

