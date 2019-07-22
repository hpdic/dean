import java.sql.Timestamp;
import java.util.Random;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Iterator;
import java.util.HashMap;
import java.io.IOException;
import java.io.PrintWriter;

public class Simulator{
	public static void main(String[] args){
		try{
			
			
			long blockCreateTime=0;
			Timestamp start= new Timestamp(System.currentTimeMillis());
			long starttime= System.currentTimeMillis();
			System.out.println(start);
			
			PrintWriter transactionHistory = new PrintWriter("transactionHistory.txt", "UTF-8");
			PrintWriter blockChainHistory = new PrintWriter("blockChainHistory.txt", "UTF-8");
			PrintWriter blockChainHistoryTime = new PrintWriter("blockChainHistoryTime.txt", "UTF-8");
			PrintWriter blockChainHistoryReceiveTime = new PrintWriter("blockChainHistoryTime.txt", "UTF-8");
			PrintWriter masterChainHistoryTime= new PrintWriter("masterChainHistoryTime.txt","UTF-8");
			
			//int numPeers = Integer.parseInt(args[0]); //see WSN
			double minPropDelay = 10;
			double maxPropDelay = 500;
			double qDelayParameter = 12.0/1024.0;
			ArrayList<Node> nodeList = new ArrayList<Node>();
            
			
			
			//Genesys Block
			Timestamp genesisTime = new Timestamp(System.currentTimeMillis());
			Block genesisBlock = new Block("genesis", genesisTime);
			//Block mastergenesisBlock = new Block("genesis", genesisTime);
			//Generating numPeers number of nodes with randomly choosing fast and slow property
			//type true for fast nodes and false for lazy nodes
			Boolean[] nodeTypes = new Boolean[numPeers];
						
			Random randType = new Random(System.nanoTime());
			for(int i=0; i<numPeers; i++){
				String nodeID = "Node_"+i;
				boolean type = true;//true for fast node, false for slow node
				nodeTypes[i] = type;
				
			
			
			//Creating a array to store the bottle neck link between each pair of nodes
			Double[][] bottleNeck = new Double[numPeers][numPeers];
			for(int i=0; i<numPeers; i++){
				for(int j=0; j<numPeers; j++){
					if(connectionArray[i][j]){
						if(nodeList.get(i).getType() && nodeList.get(j).getType())
							bottleNeck[i][j] = 100.0;
						else
							bottleNeck[i][j] = 5.0;
					}								
				}
			}

			//Assigning mean to generate T_k later for each node from an exponential distribution
			Double[] cpuPower = new Double[numPeers];
			Random randCpu = new Random(System.nanoTime());
			for(int i=0; i<numPeers; i++){
				if(nodeList.get(i).wsnType!="storage")
				 {
					double cpuMean = 10 + randCpu.nextDouble()*1;
					cpuPower[i] = 1/cpuMean;
				 }
				
			}


			//Assigning mean to generate transaction later for each node from an exponential distribution
			Double[] txnMean = new Double[numPeers];
			Random randMean = new Random(System.nanoTime());
			for(int i=0; i<numPeers; i++){
				 if(nodeList.get(i).wsnType!="storage")
				 {
					 double tempTxnMean = 50 + randMean.nextDouble()*50;//deterministic: constant value for more experiments***************
				     txnMean[i] = 1/tempTxnMean;
				 }
								
			}

			//Priortiy Queue of events to be executed and finished
			PriorityQueue<Event> pendingEvents = new PriorityQueue<Event>();
			PriorityQueue<Event> finishedEvents = new PriorityQueue<Event>();

			long simTime =1000*1000; // 500000*10000; //execution time 
			 
			Timestamp currTime = new Timestamp(System.currentTimeMillis()); //****master node modification to make deterministic
			
			long currTimeOffset = currTime.getTime();
			
			Timestamp maxTime = new Timestamp(currTimeOffset + (long)(simTime)); //****master node modification to make deterministic

			//Every node here tries to generate a block on the genesis block
			for(int i=0; i<numPeers; i++){
				 if(nodeList.get(i).wsnType!="storage")
				 {
					    Random randBlock = new Random(System.nanoTime());
						double  nextTimeOffset = .8;//randBlock.nextDouble(); //*****master node change to make deterministic
						while(nextTimeOffset == 0.0){
							nextTimeOffset = randBlock.nextDouble();
						}
						double nextTimeGap = -1*Math.log(nextTimeOffset)/.001;//cpuPower[i]; //*****master node change to make deterministic
						Timestamp nextBlockTime = new Timestamp(currTimeOffset + (long)nextTimeGap*1000);// gap is 2.3 microseconds
						//register a new block generation event
						Block newBlock = nodeList.get(i).generateBlock(genesisBlock, nextBlockTime);
						Event newEvent = new Event(2, newBlock, nextBlockTime, i);
						nodeList.get(i).nextBlockTime = nextBlockTime;
						pendingEvents.add(newEvent);
				 }
				
				
			}

			//To generate initial set of transactions to start the simulator
			for(int i=0; i<numPeers; i++){
               if(nodeList.get(i).wsnType!="storage")
               {
            	// long nextTxnLong = (long)(10000*txnMean[i]);
   				Random randNext = new Random(System.nanoTime());
   				double nextTimeOffset = 0.5;//randNext.nextDouble();//****master node modification to make deterministic
   				while (nextTimeOffset == 0.0){
   					nextTimeOffset = 0.5;//randNext.nextFloat(); //****master node modification to make deterministic
   				}
   				
   				double nextTimeGap = -1*Math.log(nextTimeOffset)/50;  //****master node modification to make deterministic
   				Timestamp nextTxnTime = new Timestamp(currTimeOffset + (long)nextTimeGap*1000); //gap is 13 Nanoseconds
   				nodeList.get(i).nextTxnTime = nextTxnTime;
   				Random receiveRand = new Random(System.nanoTime());
   				int rcvNum = receiveRand.nextInt(numPeers); //This decides randomly which node will generate transaction
   				while(rcvNum == i){
   					rcvNum = receiveRand.nextInt(numPeers);
   				}
   				String receiverID = nodeList.get(rcvNum).getUID();
   				float receivedAmount = 0 ;
   				Transaction newTransaction = nodeList.get(i).generateTxn(receiverID, receivedAmount, nextTxnTime);
   				//register generate transcation event
   				Event newEvent = new Event(4, newTransaction, nextTxnTime);
   				pendingEvents.add(newEvent);   
               }
				
			}

			//Timestamp of the next event to be executed
			Timestamp nextEventTime = pendingEvents.peek().getEventTimestamp();
			Iterator<Event> eventItr = pendingEvents.iterator();
			while(nextEventTime.before(maxTime)){			
				if(eventItr.hasNext()){    //checks if any further blocks are needed to be handeled
					Event nextEvent = pendingEvents.poll();
					finishedEvents.add(nextEvent);

					if(nextEvent.getEventType()==1){ //receiveBlock = 1, generateBlock = 2, receiveTransaction = 3, generateTransaction = 4
						
						int currentNum = nextEvent.getReceiverNum();
						int creatorNum = nextEvent.getCreatorNum();
						Node currentNode = nodeList.get(currentNum);

						

						Block currentBlock = nextEvent.getEventBlock();
						String currentBlockID = currentBlock.getBlockID();
						if(!currentNode.checkForwarded(currentBlockID)){ //check the status of the block whether it is forwarded

							nodeList.get(currentNum).addForwarded(currentBlockID); //change the status of the block as forwarded
							//main add block code is here
							
							//*******Verification with master node chain**********************//
							
							
							 boolean addBlockSuccess = nodeList.get(currentNum).addBlock(currentBlock);
							//boolean addBlockSuccess = nodeList.get(currentNum).addBlockInMaster(currentBlock, singleNode);
							
							//**************adding to master node storage********************
							//if(addBlockSuccess) 
							//{																																																																																																				
								//blockChainHistoryReceiveTime.println("Block received "+currentBlockID+" by "+ currentNum+" Time:"+(System.currentTimeMillis() - currentBlock.createTime));	
								currentBlock.receiveTime= (System.currentTimeMillis() - currentBlock.createTime);
								currentBlock.setReceiverNode(currentNum);
								nodeList.get(currentNum).blockList.add(currentBlock);//Tracking all blocks that are received by this node
							//}	//long blockReceiveFinishTime =;
								 //blockChainHistoryTime.println("Block received and validated "+currentBlockID+" at depth "+ currentBlock.getDepth() +" by "+ currentNum+" Time:"+ (System.currentTimeMillis() - currentBlock.createTime));
								
								
								
							
								/*if(!singleNode.blockChain.containsKey(currentBlockID))
								{
									singleNode.addForwarded(currentBlockID);
									
									singleNode.addBlock(currentBlock);
									masterChainHistoryTime.println("During received "+currentBlock.getBlockID() +" Time: "+ (System.currentTimeMillis()-currentBlock.createTime));
									singleNode.probParentBlock= currentBlock;
									singleNode.calculateBTC();
									
								
								}*/
								
								
							//}
							
							
							//***********adding to master node storage ends*********************
							
							//blockChainHistoryTime.close();
							
							if(addBlockSuccess)
							{
								//check if any pending blocks can be added
								nodeList.get(currentNum).addPendingBlocks();
								singleNode.addPendingBlocks();
								
							}
							int currentDepth = nodeList.get(currentNum).probParentBlock.getDepth();
							//int currentDepth = singleNode.probParentBlock.getDepth();  //master node modification
							int blockDepth = currentBlock.getDepth();						
		                    //if received block has more valid chain than the receiver's chain, set this block as parent in other peers too !
							if(blockDepth > currentDepth){
								
								//System.out.println("Current block is more deep");
								
								//updating the probable parent block
								nodeList.get(currentNum).probParentBlock = currentBlock;
								nodeList.get(creatorNum).calculateBTC();

								
								//**************adding to master node storage********************
								//if(addBlockSuccess) 
								//{
									/*int masterDepth = singleNode.probParentBlock.getDepth();
									if(blockDepth > masterDepth)
									{
										singleNode.probParentBlock = currentBlock;
										singleNode.calculateBTC();
										
									}*/
																
								//**********master node ends******************************
								
									
								//to Generate the next transaction for the sending node
								Random randNext = new Random(System.nanoTime());
								double nextTimeOffset = randNext.nextDouble();
								while (nextTimeOffset == 0.0){
									nextTimeOffset = randNext.nextDouble();
								}
								double nextTimeGap = -1*Math.log(nextTimeOffset)/0.095;//cpuPower[currentNum];
								Timestamp newBlockTime = new Timestamp(nextEventTime.getTime() + (long)nextTimeGap*1000);//(long)nextTimeGap*1000);
								nodeList.get(currentNum).nextBlockTime = newBlockTime;
								Block newBlock = nodeList.get(currentNum).generateBlock(currentBlock, newBlockTime);
								Event newEvent = new Event(2, newBlock, newBlockTime, currentNum);
								pendingEvents.add(newEvent);			
							}

							for(int i=0; i<numPeers; i++){ //sending the block to other peers
								Node nextNode = currentNode.getNode(i);
								if(nextNode == null){
									break;
								}
								else{									
									int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);
									Random queingRandom = new Random(System.nanoTime());
									float qDelayP1 = queingRandom.nextFloat();
									while (qDelayP1 == 0.0){
										qDelayP1 = queingRandom.nextFloat();
									}
									long qDelay = 1000;//(long)((-1*Math.log(qDelayP1)*bottleNeck[currentNum][nextNodeNum])/qDelayParameter);
									long pDelay = Math.round(propagationDelay[currentNum][nextNodeNum]);
									long msgDelay = 0;
									if(bottleNeck[creatorNum][nextNodeNum]!=null){
										msgDelay = Math.round(1000.0/bottleNeck[creatorNum][nextNodeNum]);
									}
									Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay + msgDelay);									
									Event newEvent = new Event(1, currentBlock, receiveTime, nextNodeNum, currentNum);
									pendingEvents.add(newEvent);
								}							
							}
							//Timestamp of the next event to be executed
							blockChainHistory.println("Block received "+currentBlockID+" at depth "+ currentBlock.getDepth() +" by "+ currentNum);
							nextEventTime = pendingEvents.peek().getEventTimestamp();
							//blockChainHistoryTime.close();
						//}
						}
					}
					else if(nextEvent.getEventType()==2){
						//Code to execute generate Block
						//System.out.println("Event 2");
						int creatorNum = nextEvent.getCreatorNum();
											
						Node currentNode = nodeList.get(creatorNum); //pick the current node   // for master node
						
						if(currentNode.wsnType=="storage") continue; //new WSN code to cope up with present simulator
						System.out.println(currentNode.wsnType);
						
						Block currentBlock = nextEvent.getEventBlock();
						Timestamp nextBlockTime = currentNode.nextBlockTime;
										
						if(!(nextBlockTime.after(nextEventTime) || nextBlockTime.before(nextEventTime))){ //Only execute this if the node still decides to execute it
	
	
							//mining fee transaction 
							Transaction mfee = new Transaction(currentNode.getUID()+"_mining_fee","god",currentNode.getUID(),50,new Timestamp(System.currentTimeMillis()));
							currentBlock.addTxn(mfee); // adding mining fee at the current transaction in array list of current block
							
							//change block to include transactions of the current node
							Block parent = currentNode.probParentBlock;
							for(int i=0;i<currentNode.allTxns.size();i++){
								boolean alreadyIncluded = false;
								Transaction tmpTxn = currentNode.allTxns.get(i);
								
								
								
								/*if(!singleNode.checkValid(tmpTxn)){
									//continue;	//continue if invalid. It can turn valid after some time.
									//System.out.println("Master node validation fails event 2"+tmpTxn.getTxnID());
								}*/
								
								//check block validity
								if(!currentNode.checkValid(tmpTxn)){
									
									//System.out.println("current node validation fails event 2"+tmpTxn.getTxnID());
									continue;
								}
								
								while(parent!=null){ //check whether the transaction is already included in any block of current node
									if(parent.txnList.contains(tmpTxn)){// controlling double spending
										//System.out.println("Txn: "+tmpTxn.getTxnID()+" failed");
										alreadyIncluded = true;
										break;
									}
									parent = parent.getParentBlock();
								}
								if(!alreadyIncluded){ //add the pending transaction in the current block if not already added
									currentBlock.addTxn(tmpTxn); //Note: store each block's transaction
								}
							}
							//end of adding pending transaction to the new block
							
	
                           //adding a status message to keep track that the newly created block is going to be added in current node 
							nodeList.get(creatorNum).addForwarded(currentBlock.getBlockID()); //block is forwarded to be added
							
							//**************time tacking***********
							currentBlock.createTime = System.currentTimeMillis();
							Timestamp blockCreateTime2= new Timestamp(System.currentTimeMillis());
							String blockwisefile = "Create/"+currentBlock.getBlockID()+creatorNum+".txt"; 
							blockChainHistoryTime = new PrintWriter(blockwisefile, "UTF-8"); 
							
							//blockChainHistoryTime.println("Block created "+currentBlock.getBlockID()+" by "+ creatorNum+" Time:"+currentBlock.createTime);
							
							//*************time tracking sends******
							
							 //finally adding the block in the current node
							boolean addBlockSuccess = nodeList.get(creatorNum).addBlock(currentBlock);
							//*******Verification with master node chain**********************//
							 
							//boolean addBlockSuccess = nodeList.get(creatorNum).addBlockInMaster(currentBlock, singleNode);
							
							
							if(addBlockSuccess)
							{
								blockChainHistoryTime.println("Block created "+currentBlock.getBlockID()+" by "+ creatorNum+" Time:"+(System.currentTimeMillis()-currentBlock.createTime));
								
								nodeList.get(creatorNum).probParentBlock = currentBlock;
								nodeList.get(creatorNum).calculateBTC();//Note:calculate and store in DB
								
							}
							
							
													
							
							//if adding block in current node is successful, transfer the nodes to other peers
							if(addBlockSuccess){
								
								blockChainHistory.println("Node "+creatorNum+" created Block "+currentBlock.getBlockID()+ " at Depth "+ currentBlock.getDepth() + " ON "+currentBlock.getParentBlockID());
								for(int i=0; i<numPeers; i++){
									Node nextNode = currentNode.getNode(i);
									if(nextNode == null){
										break;
									}
									else{									
										int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);
										Random queingRandom = new Random(System.nanoTime());
										float qDelayP1 = queingRandom.nextFloat();
										while (qDelayP1 == 0.0){
											qDelayP1 = queingRandom.nextFloat();
										}
										long qDelay = 1000;//(long)((-1*Math.log(qDelayP1)*bottleNeck[creatorNum][nextNodeNum])/qDelayParameter);
										long pDelay = Math.round(propagationDelay[creatorNum][nextNodeNum]);
										System.out.println(pDelay +":\t"+creatorNum+"\t"+nextNodeNum);
										long msgDelay = 0;
										if(bottleNeck[creatorNum][nextNodeNum]!=null){
											msgDelay = Math.round(1000.0/bottleNeck[creatorNum][nextNodeNum]);
										}
										Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay+msgDelay);									
										//Prepare to send block to other peers
										Event newEvent = new Event(1, currentBlock, receiveTime, nextNodeNum, creatorNum);
										pendingEvents.add(newEvent);
										
									}							
								}						
							}
							//to Generate the next transaction for the sending node
							Random randNext = new Random(System.nanoTime());
							double nextTimeOffset = randNext.nextDouble();
							while (nextTimeOffset == 0.0){
								nextTimeOffset = randNext.nextDouble();
							}
							double nextTimeGap = -1*Math.log(nextTimeOffset)/0.095;//cpuPower[creatorNum];
							Timestamp newBlockTime = new Timestamp(nextEventTime.getTime() + (long)nextTimeGap*1000);

							Block newBlock = nodeList.get(creatorNum).generateBlock(currentBlock, newBlockTime);
							//Generate new block by the sending node for the purpose of next transaction
							Event newEvent = new Event(2, newBlock, newBlockTime, creatorNum); 
							pendingEvents.add(newEvent);
						}
						//Updating the time to execute next event
						nextEventTime = pendingEvents.peek().getEventTimestamp();	
						//blockChainHistoryTime.close();
					}
					else if(nextEvent.getEventType()==3){

						//Code to execute receive Transaction					
						int receiverNum = nextEvent.getReceiverNum();
						int senderNum = nextEvent.getSenderNum();
						Node tempSenderNode = nodeList.get(receiverNum);
						Transaction newTxn = nextEvent.getEventTransaction();
						String newTxnID = newTxn.getTxnID();
						if(!(tempSenderNode.checkForwarded(newTxnID))){//Only execute if it has not already forwarded the same transaction earlier
													
							//add transactions to allTxns list 
							Node currNode = nodeList.get(receiverNum);
							if(!currNode.allTxns.contains(newTxn)){
								currNode.allTxns.add(newTxn);
								
								//***********master************
								if(!singleNode.allTxns.contains(newTxn)){
								singleNode.allTxns.add(newTxn);
								
								}
								//***********master ends********
							}
							//end
							
							int txnReceiverNum = Integer.parseInt((newTxn.getReceiverID()).split("_")[1]);
							transactionHistory.print("Transaction Id "+ newTxnID+" Money receiver :"+txnReceiverNum+" "+"Message Receiver :"+receiverNum);
							if(txnReceiverNum == receiverNum){ //checking the transaction is meant for that node or not
								boolean addReceiveSuccess = nodeList.get(receiverNum).addTxn(newTxn);
								
								//************master******************
								if (addReceiveSuccess)
									{
									
									   boolean stat= singleNode.addTxn(newTxn);
									   singleNode.addForwarded(newTxnID);
									
									}
								//***********master ends**************
								
								transactionHistory.print(" Money Added!!");						
							}

							transactionHistory.println();
							nodeList.get(receiverNum).addForwarded(newTxnID);
							for(int i=0; i<numPeers; i++){
								Node nextNode = tempSenderNode.getNode(i);							
								if(nextNode == null){
									break;
								}							
								else{	
									int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);
									if (nextNodeNum != senderNum){

										Random queingRandom = new Random(System.nanoTime());
										double qDelayP1 = queingRandom.nextDouble();
										while (qDelayP1 == 0.0){
											qDelayP1 = queingRandom.nextFloat();
										}
										long qDelay = 1000;//(long)((-1*Math.log(qDelayP1)*bottleNeck[nextNodeNum][receiverNum])/qDelayParameter);
										// System.out.println(qDelay);
										long pDelay = Math.round(propagationDelay[receiverNum][nextNodeNum]);
										Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay);
										Event newEvent = new Event(3, newTxn, receiveTime, nextNodeNum, receiverNum);
										pendingEvents.add(newEvent);
									}
								}
							}				
							
							//Timestamp of the next event to be executed
							nextEventTime = nextEvent.getEventTimestamp();
						}
						
					}
					else if(nextEvent.getEventType()==4){

						//Code to handle generate Transaction event
						Transaction newTxn = nextEvent.getEventTransaction();
						String senderID = newTxn.getSenderID();
						int senderNum = Integer.parseInt(senderID.split("_")[1]);

						//Adding a temporary node to enhance efficiency
						Node tempSenderNode = nodeList.get(senderNum);

						if(tempSenderNode.wsnType=="storage") continue; //new WSN code to cope up with
						//random to generate an amount for the transaction
						Random updateRand = new Random(System.nanoTime());
						float newAmount = updateRand.nextFloat()*tempSenderNode.getCurrOwned();
						newTxn.updateAmount(newAmount);
						

						
						//add transactions to allTxns list 
						Node currNode = nodeList.get(senderNum);
						if(!currNode.allTxns.contains(newTxn)){
							currNode.allTxns.add(newTxn);
							
							//**********master ***************
							if(!singleNode.allTxns.contains(newTxn))
							 {
								singleNode.allTxns.add(newTxn);
							
							 }
							
							//**********master ends***********
						}	
						//end
						
						//Adding the transaction at the sender end.
						boolean addTxnSuccess = nodeList.get(senderNum).addTxn(newTxn);
						
						//************master******************
						if (addTxnSuccess) 
						{
							
							boolean stat = singleNode.addTxn(newTxn);
							singleNode.addForwarded(newTxn.getTxnID());
							
						}
						
						//***********master ends**************
						
						
						nodeList.get(senderNum).addForwarded(newTxn.getTxnID());
						if(addTxnSuccess){			//proceeding only when the transaction is successfully added
							if (newAmount!=0){
								transactionHistory.println(senderID + " sends " + newTxn.getAmount()+ " to " + newTxn.getReceiverID()+" a: "+ nodeList.get(senderNum).getCurrOwned());
								for(int i=0; i<numPeers; i++){
									Node nextNode = tempSenderNode.getNode(i);
									if(nextNode == null){
										break;
									}
									else{
										int nextNodeNum = Integer.parseInt(nextNode.getUID().split("_")[1]);

										Random queingRandom = new Random(System.nanoTime());
										float qDelayP1 = queingRandom.nextFloat();
										while (qDelayP1 == 0.0){
											qDelayP1 = queingRandom.nextFloat();
										}
										long qDelay =1000;//(long)((-1*Math.log(qDelayP1)*bottleNeck[senderNum][nextNodeNum])/qDelayParameter);
										long pDelay = Math.round(propagationDelay[senderNum][nextNodeNum]);
										Timestamp receiveTime = new Timestamp(nextEventTime.getTime()+ qDelay + pDelay);
										Event newEvent = new Event(3, newTxn, receiveTime, nextNodeNum, senderNum);
										pendingEvents.add(newEvent);

									}								
								}
							}						

							//to Generate the next transaction for the sending node
							Random randNext = new Random();
							double nextTimeOffset = randNext.nextDouble();
							while (nextTimeOffset == 0.0){
								nextTimeOffset = randNext.nextFloat();
							}
							double nextTimeGap = -1*Math.log(nextTimeOffset)/txnMean[senderNum];
							Timestamp nextTxnTime = new Timestamp(nextEventTime.getTime() + (long)nextTimeGap*1000);

							nodeList.get(senderNum).nextTxnTime = nextTxnTime;
							
							Random receiveRand = new Random(System.nanoTime());
							int rcvNum = receiveRand.nextInt(numPeers);
							while(rcvNum == senderNum){
								rcvNum = receiveRand.nextInt(numPeers);
							}

							String receiverID = nodeList.get(rcvNum).getUID();
							float receivedAmount = 0;

							Transaction newTransaction = nodeList.get(senderNum).generateTxn(receiverID, receivedAmount, nextTxnTime);
							Event newEvent = new Event(4, newTransaction, nextTxnTime);
							pendingEvents.add(newEvent);

							//Updating the time to execute next event
							nextEventTime = pendingEvents.peek().getEventTimestamp();
							
						}
						/*else{
							System.out.println("Add Transaction Failed!!");
						}*/	
					}
					else{
						System.out.println("Error: Wrong Eventtype Detected.");
					}
				}
				else{

				}
			}//All types of events execution finished here

			Timestamp finish= new Timestamp(System.currentTimeMillis());
			System.out.println(finish);
			System.out.println(System.currentTimeMillis() - starttime);			
			
			double sum = 0;
			for(int i=0; i<numPeers; i++){
				float value = nodeList.get(i).getCurrOwned();
				sum = sum + value;
				transactionHistory.println(value); //storing all the transactions by all peers
			}
			transactionHistory.println("Total :"+sum); //total transaction

			transactionHistory.close();
			blockChainHistory.close();
			masterChainHistoryTime.close();
			blockChainHistoryTime.close();
			blockChainHistoryReceiveTime.close();
			//*********master node logic************
			int longLength=0;
			int longNode=0;
			//*******master node logic ends**********
			
			
			for(int i=0; i<numPeers; i++){//Note: all node's block-chain is stored in node.blockchain arraylist
				HashMap<String, Block> tempBlockChain = nodeList.get(i).blockChain;//getting each node's chain
				
				String root = "genesis";
				String fileName = "Nodes/file_"+i+".txt";
				
							
				String vizFileNmae = "viz_"+i+".csv";
				try{
					PrintWriter writer = new PrintWriter(fileName,"UTF-8");
					
					
					writer.println("\nStored Tree:");

					printTree(writer ,root, tempBlockChain);//storing each node's chain in separate file
					
					writer.close();
					
				}
				catch (IOException e){
					e.printStackTrace();
				}			
			}
			
			//******************master node storage**********************
			String root = "genesis";
			
			
			HashMap<String, Block> tempBlockChain2 = singleNode.blockChain;
			String masterfileName = "master.txt";
			
			PrintWriter masterwriter = new PrintWriter(masterfileName,"UTF-8");
			printTree(masterwriter ,root,tempBlockChain2);
			masterwriter.close();
			
			
			//******************Master node storage ends*****************

			
			//**************all blocks creation time by each node***********
			for(int i=0; i<numPeers; i++){//Note: all node's block-chain is stored in node.blockchain arraylist
				try{
				String fileName = "CreateBlocks/Node_"+i+".txt";
				
				PrintWriter writer = new PrintWriter(fileName,"UTF-8");
				ArrayList<Block> blockList= nodeList.get(i).getBlockList();
				int blockListSize= blockList.size();
				int j=0;
				while (j<blockListSize)
				{
						
					    writer.println(blockList.get(j).getReceiverNode()+":"+blockList.get(j).createTime);
					    
						j++;
							
				}
				writer.close();
				}	
				catch (IOException e){
					e.printStackTrace();
				}
			}
			
			//**************all blocks received by each node***********
			for(int i=0; i<numPeers; i++){//Note: all node's block-chain is stored in node.blockchain arraylist
				try{
				String fileName = "Receive/Node_"+i+".txt";
				
				PrintWriter writer = new PrintWriter(fileName,"UTF-8");
				ArrayList<Block> blockList= nodeList.get(i).getBlockList();
				int blockListSize= blockList.size();
				int j=0;
				while (j<blockListSize)
				{
						
					    writer.println(blockList.get(j).getReceiverNode()+":"+blockList.get(j).receiveTime);
					    
						j++;
							
				}
				writer.close();
				}	
				catch (IOException e){
					e.printStackTrace();
				}
			}

			//************all blocks received by each node ends***********
			
			
		}catch(IOException ex){
			ex.printStackTrace();
		}
	}

	

	public static void printTree(PrintWriter writer,String root, HashMap<String, Block> blockChain){		
		Block rootBlock = blockChain.get(root);
		if(rootBlock != null){
			ArrayList<String> childList = blockChain.get(root).getChildList();
			int childListSize = childList.size();
			int i = 0;
			while(i<childListSize){
				String newRoot = childList.get(i);
				printTree(writer, newRoot, blockChain);
				i++;
			}
			Block parent = blockChain.get(root).getParentBlock();
			if(parent != null){
				writer.println(root+","+parent.getBlockID());
			}					
		}
	}
	
}
