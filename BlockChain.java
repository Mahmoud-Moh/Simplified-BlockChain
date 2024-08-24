/*
I acknowledge that I am aware of the academic integrity guidelines of this
course, and that I worked on this assignment independently without any
unauthorized help with coding or testing.
Mahmoud Mohamed Abdelaziz

/*Additional exercise
The way the hash of the COINBASE transactions is computed in the provided code
could lead to an issue:
- The issue is that miner can spend coinbase immediately, despite his work can be disgarded 
in the future because its branch will be cancelled to the longest branch. 
- This is solved in Bitcoin by the concept of Coinbase Maturity, which prevents spending of 
funds provided by coinbase until a number of confirmations on block is achieved.
Another issue that couldn't find a resource for it online: 
- Another issue is that in typical bitcoin transaction, transaction hash includes signatures
and Hashes of previous transactions that produce claimed Input.
- So if block contains 2 transactions that claim output of one previous transaction, this
will be detected when 2 transactions try to use same UTXO.
- But in Coinbase transaction, it contains only value, address of miner, so if miner tries to
repeat transaction, this won't be detected, double spending.
Suggestion, that I found out that's not in Bitcoin: 
- To prevent that, Input to coinbase transaction could be the Hash of the previous
coinbase transaction, and that doesn't mean it actually takes output claimed by
coinbase of previous(parent) block, but it prevents miner from claiming two
coinbase transactions, because UTXO double spending check will prevent it.
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
	private ArrayList<Block> blocks;
	private TransactionPool txPool;

    private BChTreeNode BChTreeRoot;

    private BChTreeNode maxHeightBlockNode;

    private HashMap<ByteArrayWrapper, BChTreeNode> blockChainNodes;

    class BChTreeNode{
        private Block block;
        private int height;
        private BChTreeNode parent;
        private UTXOPool utxoPool;
        public UTXOPool getUtxoPool() {
            return utxoPool;
        }

        public void setUtxoPool(UTXOPool utxoPool) {
            this.utxoPool = utxoPool;
        }

        public BChTreeNode(Block block) {
            this.block = block;
            this.utxoPool = new UTXOPool();
        }


        public void setBlock(Block block) {
            this.block = block;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void setParent(BChTreeNode parent) {
            this.parent = parent;
        }

        public Block getBlock() {
            return block;
        }

        public int getHeight() {
            return height;
        }

        public BChTreeNode getParent() {
            return parent;
        }
    }

/**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */

    public BlockChain(Block genesisBlock) {
		this.blocks = new ArrayList<>();
		this.txPool = new TransactionPool();
        this.blockChainNodes = new HashMap<>();
        BChTreeNode root = new BChTreeNode(genesisBlock);
        root.setHeight(1);
        root.setParent(null);
        // The first block (genesis block) is always valid.
        blocks.add(genesisBlock);
        //Add coinbase to UTXO
        Transaction coinbase = genesisBlock.getCoinbase();
        UTXO utxo_coinbase = new UTXO(coinbase.getHash(), 0);
        root.utxoPool.addUTXO(utxo_coinbase, coinbase.getOutput(0));
        blockChainNodes.put(new ByteArrayWrapper(genesisBlock.getHash()), root);
        this.BChTreeRoot = root;
        this.maxHeightBlockNode = this.BChTreeRoot;
        this.addBlock(genesisBlock);
    }


/** Get the maximum height block */

    public Block getMaxHeightBlock() {
        return maxHeightBlockNode.getBlock();
    }

/** Get the UTXOPool for mining a new block on top of max height block */

    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlockNode.getUtxoPool();
    }


/** Get the transaction pool to mine a new block */

    public TransactionPool getTransactionPool() {
        return new TransactionPool(txPool);
    }


/**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */

    public boolean addBlock(Block block) {
        if (block == null) {
            return false;
        }
        if(block.getPrevBlockHash() == null){
            return false;
        }

        //Got Parent Node
        BChTreeNode parentBlockNode = blockChainNodes.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if(parentBlockNode == null)
            return false;
        int height = parentBlockNode.height + 1;
        //Height Check
        if(height <= maxHeightBlockNode.getHeight() - CUT_OFF_AGE)
            return false;

        //Transactions validity Check
        //UTXO used is the UTXO of parent
        UTXOPool utxoPool = new UTXOPool(parentBlockNode.getUtxoPool());
        TxHandler txHandler = new TxHandler(utxoPool);
        Transaction[] blockTxs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validSet = txHandler.handleTxs(blockTxs);
        if(validSet.length != blockTxs.length)
            return false;

        //Create BlockNode
        BChTreeNode blockNode = new BChTreeNode(block);
        blockNode.setHeight(parentBlockNode.height + 1);
        blockNode.setParent(parentBlockNode);

        utxoPool = txHandler.getUTXOPool();
        Transaction coinbase = block.getCoinbase();
        UTXO utxo = new UTXO(coinbase.getHash(), 0);
        utxoPool.addUTXO(utxo, coinbase.getOutput(0));

        blockNode.setUtxoPool(utxoPool);

        //Add Block To blockChainNodes
        blockChainNodes.put(new ByteArrayWrapper(block.getHash()), blockNode);

        //Update MaxHeightNode
        if(blockNode.getHeight() > maxHeightBlockNode.getHeight())
            maxHeightBlockNode = blockNode;

        for(Transaction tx : block.getTransactions())
            eraseTransaction(tx);

        addTransaction(block.getCoinbase());
        //Add block to blocks
        blocks.add(block);

        return true;
    }

    private void eraseTransaction(Transaction tx){
        txPool.removeTransaction(tx.getHash());
    }

/** Add a transaction to the transaction pool */

    public void addTransaction(Transaction tx) {
        this.txPool.addTransaction(tx);
    }
}
