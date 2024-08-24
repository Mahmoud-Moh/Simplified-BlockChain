# Simplified BlockChain

## Overview

This project involves managing transactions and blocks within a blockchain system. Key components include:

- **Transaction**: Manages transaction data and signatures with `Input` and `Output` inner classes.
- **UTXO**: Represents an unspent transaction output.
- **UTXOPool**: Handles the current set of UTXOs.
- **BlockChain**: Maintains the blockchain and processes blocks and transactions.
- **TransactionPool**: Manages transactions that are pending inclusion in blocks.

## Components

### Transaction

- **Output**: Contains a value and a public key.
- **Input**: Includes the hash and index of the output being spent and a digital signature. Signature validity is verified using `Crypto.verifySignature()`.

### UTXO and UTXOPool

- **UTXO**: Represents an unspent transaction output with its transaction hash and index.
- **UTXOPool**: Manages UTXOs with methods to add, remove, and query UTXOs.

### Blockchain

- **BlockChain**: Manages the chain of blocks with methods to add new blocks and handle transactions.
  - **BChTreeNode**: Represents a node in the blockchain with methods for managing height, parent, and UTXO pool.
  - **Block**: Stores the block data and handles coinbase transactions.

### TransactionPool

- **TransactionPool**: Manages transactions waiting to be added to a block.

## Usage

1. **Initialize Blockchain**: Create a `BlockChain` instance with a genesis block.
2. **Add Blocks**: Use `addBlock()` to add new blocks, ensuring all transactions are valid and adhere to blockchain height rules.
3. **Manage Transactions**: Add transactions to the pool using `addTransaction()`.
4. **Verify Transactions**: Use `Transaction.Input.verifySignature()` to validate transaction inputs.

For more details, refer to the source code and accompanying JavaDoc.
